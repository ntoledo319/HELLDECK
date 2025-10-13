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
import androidx.lifecycle.lifecycleScope
import com.helldeck.data.Repository
import com.helldeck.engine.Config
import com.helldeck.ui.Scene
import com.helldeck.utils.Logger
import kotlinx.coroutines.launch

/**
 * Main Activity for HELLDECK application
 * Entry point for the game application
 */
class MainActivity : ComponentActivity() {

    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Install splash screen
            // installSplashScreen() // TODO: Add splash screen dependency

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
                HellDeckAppContent()
            }

        } catch (e: Exception) {
            Logger.e("Failed to initialize MainActivity", e)
            // Show error screen or crash gracefully
            handleInitializationError(e)
        }
    }

    @Composable
    private fun HellDeckAppContent() {
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

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

        when {
            isLoading -> {
                LoadingScreen()
            }
            error != null -> {
                ErrorScreen(error = error!!) {
                    // Retry initialization
                    lifecycleScope.launch {
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
    
    /**
     * Main app content composable
     */
    @Composable
    private fun MainAppContent() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "HELLDECK",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
    
            Spacer(modifier = Modifier.height(16.dp))
    
            Text(
                text = "Welcome to HELLDECK!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }

    private suspend fun initializeApp() {
        try {
            Logger.i("Starting app initialization")

            // Load configuration
            Config.load(this)

            // Initialize repository and database
            val repository = Repository.get(this)
            repository.initialize()

            Logger.i("App initialization completed successfully")

        } catch (e: Exception) {
            Logger.e("App initialization failed", e)
            throw e
        }
    }

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

    override fun onResume() {
        super.onResume()
        Logger.d("MainActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Logger.d("MainActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("MainActivity destroyed")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("New intent received: ${intent.action}")
    }
}

/**
 * Loading screen composable
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
 * Error screen composable
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