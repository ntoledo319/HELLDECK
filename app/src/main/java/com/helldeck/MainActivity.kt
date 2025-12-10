package com.helldeck

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.engine.Config
import com.helldeck.ui.HelldeckTheme
import com.helldeck.ui.OnboardingWrapper
import com.helldeck.ui.HelldeckAppUI
import com.helldeck.utils.Logger
import kotlinx.coroutines.launch
import androidx.tracing.trace

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
                    enableRemoteLogging = false
                )
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
                .windowInsetsPadding(WindowInsets.systemBars)
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
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
                showRetry = false
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
     * Logs the destroy event for debugging.
     */
    override fun onDestroy() {
        super.onDestroy()
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
    androidx.compose.foundation.layout.Box(
        contentAlignment = androidx.compose.ui.Alignment.Center,
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
        )
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
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(androidx.compose.foundation.layout.PaddingValues(16.dp)),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "⚠️ Error",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.error
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

        androidx.compose.material3.Text(
            text = error,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (showRetry && onRetry != null) {
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            androidx.compose.material3.Button(onClick = onRetry) {
                androidx.compose.material3.Text("Retry")
            }
        }
    }
}
