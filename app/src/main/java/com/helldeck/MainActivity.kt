package com.helldeck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.tracing.trace
import com.helldeck.billing.PurchaseManager
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.engine.Config
import com.helldeck.ui.HelldeckAppUI
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckTheme
import com.helldeck.ui.OnboardingWrapper
import com.helldeck.utils.Logger
import kotlinx.coroutines.launch

/**
 * Main Activity for HELLDECK application
 * Entry point for the game application
 */
class MainActivity : ComponentActivity() {

    private var isInitialized = false

    /**
     * Called when the activity is first created.
     * Sets up the splash screen, enables edge-to-edge display, initializes logging, and sets the content view.
     * Handles initialization errors gracefully.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            installSplashScreen()
            super.onCreate(savedInstanceState)

            // Enable edge-to-edge display
            enableEdgeToEdge()

            // Initialize logging
            Logger.initialize(
                context = this,
                config = com.helldeck.utils.LoggerConfig(
                    level = com.helldeck.utils.LogLevel.INFO,
                    enableFileLogging = true,
                    enableRemoteLogging = false,
                ),
            )

            Logger.i("MainActivity created successfully")

            // Set content
            setContent {
                HelldeckTheme {
                    HellDeckAppContent()
                }
            }
        } catch (e: Exception) {
            Logger.e("Failed to initialize MainActivity", e)
            // Show error screen or crash gracefully
            handleInitializationError(e)
        }
    }

    /**
     * Composable function that manages the main app content, including loading, error, and main UI states.
     * Handles app initialization asynchronously and displays appropriate screens based on the state.
     */
    @Composable
    private fun HellDeckAppContent() {
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        // Initialize app data
        LaunchedEffect(Unit) {
            try {
                if (!isInitialized) {
                    initializeApp()
                    isInitialized = true
                }
                isLoading = false
            } catch (e: Exception) {
                Logger.e("Failed to initialize app", e)
                error = e.message ?: "Unknown error occurred"
                isLoading = false
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            when {
                isLoading -> {
                    LoadingScreen()
                }
                error != null -> {
                    ErrorScreen(error = error!!) {
                        // Retry initialization
                        scope.launch {
                            try {
                                isLoading = true
                                error = null
                                initializeApp()
                                isLoading = false
                            } catch (e: Exception) {
                                error = e.message ?: "Unknown error occurred"
                                isLoading = false
                            }
                        }
                    }
                }
                else -> {
                    MainAppContent()
                }
            }
        }
    }

    /**
     * Composable function that displays the main application UI after initialization.
     * Wraps the core UI with onboarding components.
     */
    @OptIn(
        androidx.compose.material3.ExperimentalMaterial3Api::class,
        androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    )
    @Composable
    private fun MainAppContent() {
        OnboardingWrapper {
            HelldeckAppUI()
        }
    }

    /**
     * Suspend function to initialize the application asynchronously.
     * Loads configuration, initializes the content engine, and runs preflight validation.
     * Throws an exception if initialization fails.
     */
    private suspend fun initializeApp() {
        try {
            Logger.i("Starting app initialization")

            trace("Config.load") {
                Config.load(this)
            }

            trace("ContentEngineProvider.get") {
                ContentEngineProvider.get(this)
            }

            // Initialize billing/purchase manager
            trace("PurchaseManager.init") {
                PurchaseManager.initBilling(this)
            }

            // HELLDECK 2.0: Route validation in DEBUG builds
            if (BuildConfig.DEBUG) {
                val routeIssues = com.helldeck.ui.nav.RouteAudit.validate()
                if (routeIssues.isNotEmpty()) {
                    Logger.w("RouteAudit found issues: ${routeIssues.joinToString("; ")}")
                }
            }

            Logger.i("App initialization completed successfully")
        } catch (e: Exception) {
            Logger.e("App initialization failed", e)
            throw e
        }
    }

    /**
     * Handles initialization errors by logging and displaying an error screen.
     * Prevents the app from crashing and provides a fallback UI.
     *
     * @param error The exception that occurred during initialization.
     */
    private fun handleInitializationError(error: Exception) {
        // In a production app, you might want to show a crash dialog
        // or send error reports. For now, we'll just log and continue
        Logger.e("Critical initialization error", error)

        // Set minimal content to prevent complete crash
        setContent {
            ErrorScreen(
                error = "Failed to start HELLDECK: ${error.message}",
                showRetry = false,
            )
        }
    }

    /**
     * Called when the activity is resumed from a paused state.
     * Logs the resume event for debugging.
     */
    override fun onResume() {
        super.onResume()
        Logger.d("MainActivity resumed")
    }

    /**
     * Called when the activity is paused.
     * Logs the pause event for debugging.
     */
    override fun onPause() {
        super.onPause()
        Logger.d("MainActivity paused")
    }

    /**
     * Called when the activity is being destroyed.
     * Logs the destroy event for debugging and cleans up billing resources.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Clean up billing client connection
        if (isFinishing) {
            PurchaseManager.destroy()
        }
        Logger.d("MainActivity destroyed")
    }

    /**
     * Called when a new intent is delivered to the activity.
     * Logs the new intent for debugging.
     *
     * @param intent The new intent that was started for the activity.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("New intent received: ${intent.action}")
    }
}

/**
 * Composable function that displays a loading screen with a progress indicator.
 * Used while the app is initializing.
 */
@Composable
private fun LoadingScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        HelldeckColors.background,
                        HelldeckColors.surfacePrimary,
                    ),
                ),
            )
            .padding(HelldeckSpacing.Large.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Large),
            color = HelldeckColors.surfaceElevated.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, HelldeckColors.colorPrimary.copy(alpha = 0.38f)),
        ) {
            Column(
                modifier = Modifier.padding(HelldeckSpacing.ExtraLarge.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                Text(
                    text = "HELLDECK",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = HelldeckColors.colorOnDark,
                    textAlign = TextAlign.Center,
                )
                CircularProgressIndicator(
                    color = HelldeckColors.colorPrimary,
                    trackColor = HelldeckColors.colorPrimary.copy(alpha = 0.16f),
                )
                Text(
                    text = "Loading the next bad decision...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.colorMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Composable function that displays an error screen with the error message and optional retry button.
 * Used to show initialization or other errors to the user.
 *
 * @param error The error message to display.
 * @param showRetry Whether to show the retry button.
 * @param onRetry Callback to execute when the retry button is clicked.
 */
@Composable
private fun ErrorScreen(error: String, showRetry: Boolean = true, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HelldeckColors.background)
            .padding(PaddingValues(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HelldeckRadius.Large),
            color = HelldeckColors.surfaceElevated,
            border = BorderStroke(1.dp, HelldeckColors.Error.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier.padding(HelldeckSpacing.ExtraLarge.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                Text(
                    text = "Could not start HELLDECK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = HelldeckColors.Error,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.colorMuted,
                    textAlign = TextAlign.Center,
                )

                if (showRetry && onRetry != null) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HelldeckHeights.Button.dp),
                        shape = RoundedCornerShape(HelldeckRadius.Pill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HelldeckColors.colorPrimary,
                            contentColor = HelldeckColors.background,
                        ),
                    ) {
                        Text(
                            text = "Try Again",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
