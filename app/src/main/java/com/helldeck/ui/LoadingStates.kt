package com.helldeck.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info

/**
 * Loading state management for HELLDECK
 */
class LoadingStateManager {
    private val _loadingStates = MutableStateFlow<Map<String, LoadingState>>(emptyMap())
    val loadingStates: StateFlow<Map<String, LoadingState>> = _loadingStates.asStateFlow()

    private var autoClearScope: CoroutineScope = MainScope()

    fun setAutoClearScope(scope: CoroutineScope) {
        autoClearScope = scope
    }

    /**
     * Start a loading operation
     */
    fun startLoading(
        operationId: String,
        message: String = "Loading...",
        progress: Float? = null
    ) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates[operationId] = LoadingState.Loading(
            message = message,
            progress = progress,
            startTime = System.currentTimeMillis()
        )
        _loadingStates.value = currentStates
    }

    /**
     * Update loading progress
     */
    fun updateProgress(operationId: String, progress: Float, message: String? = null) {
        val currentStates = _loadingStates.value.toMutableMap()
        val currentState = currentStates[operationId]

        if (currentState is LoadingState.Loading) {
            currentStates[operationId] = currentState.copy(
                progress = progress,
                message = message ?: currentState.message
            )
            _loadingStates.value = currentStates
        }
    }

    /**
     * Complete a loading operation successfully
     */
    fun completeLoading(operationId: String, message: String? = null) {
        val currentStates = _loadingStates.value.toMutableMap()
        val currentState = currentStates[operationId]

        if (currentState is LoadingState.Loading) {
            currentStates[operationId] = LoadingState.Success(
                message = message ?: "Completed!",
                duration = System.currentTimeMillis() - currentState.startTime
            )
            _loadingStates.value = currentStates

            // Auto-clear success state after delay using provided scope
            autoClearScope.launch {
                delay(2000)
                clearLoadingState(operationId)
            }
        }
    }

    /**
     * Fail a loading operation
     */
    fun failLoading(operationId: String, error: String, canRetry: Boolean = true) {
        val currentStates = _loadingStates.value.toMutableMap()
        val currentState = currentStates[operationId]

        if (currentState is LoadingState.Loading) {
            currentStates[operationId] = LoadingState.Error(
                error = error,
                canRetry = canRetry,
                duration = System.currentTimeMillis() - currentState.startTime
            )
            _loadingStates.value = currentStates
        }
    }

    /**
     * Clear a loading state
     */
    fun clearLoadingState(operationId: String) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates.remove(operationId)
        _loadingStates.value = currentStates
    }

    /**
     * Check if any operations are loading
     */
    fun isLoading(): Boolean {
        return _loadingStates.value.values.any { it is LoadingState.Loading }
    }

    /**
     * Check if a specific operation is loading
     */
    fun isLoading(operationId: String): Boolean {
        return _loadingStates.value[operationId] is LoadingState.Loading
    }

    /**
     * Get loading state for operation
     */
    fun getLoadingState(operationId: String): LoadingState? {
        return _loadingStates.value[operationId]
    }
}

/**
 * Personality-filled loading messages
 */
object LoadingMessages {
    val funMessages = listOf(
        "Consulting the comedy AI...",
        "Generating the perfect roast...",
        "Shuffling the deck of chaos...",
        "Warming up the party machine...",
        "Finding the spiciest option...",
        "Summoning the entertainment gods...",
        "Mixing truth with mischief...",
        "Loading your next embarrassment...",
        "Crafting chaos with care...",
        "Stirring the drama pot...",
        "Charging the awkward meters...",
        "Preparing social destruction...",
        "Calculating maximum cringe...",
        "Unleashing controlled chaos..."
    )

    fun getRandomMessage(): String {
        return funMessages.random()
    }
}

/**
 * Loading state sealed class
 */
sealed class LoadingState {
    data class Loading(
        val message: String,
        val progress: Float?,
        val startTime: Long
    ) : LoadingState()

    data class Success(
        val message: String,
        val duration: Long
    ) : LoadingState()

    data class Error(
        val error: String,
        val canRetry: Boolean,
        val duration: Long
    ) : LoadingState()
}

/**
 * Global loading state manager instance
 */
val loadingStateManager = LoadingStateManager()

/**
 * Composable for displaying loading states
 */
@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier
) {
    val loadingStates by loadingStateManager.loadingStates.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        loadingStateManager.setAutoClearScope(scope)
    }

    if (loadingStates.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(0.9f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                loadingStates.values.forEach { state ->
                    when (state) {
                        is LoadingState.Loading -> LoadingContent(state)
                        is LoadingState.Success -> SuccessContent(state)
                        is LoadingState.Error -> ErrorContent(state)
                    }
                }
            }
        }
    }
}

/**
 * Loading content composable
 */
@Composable
private fun LoadingContent(state: LoadingState.Loading) {
    // Generate a fun message once and remember it
    val funMessage = remember { LoadingMessages.getRandomMessage() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated loading indicator
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .alpha((rotation / 360f).coerceIn(0.3f, 1f)),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show fun message if available, otherwise show state message
        Text(
            text = if (state.message == "Loading...") funMessage else state.message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        state.progress?.let { progress ->
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Success content composable
 */
@Composable
private fun SuccessContent(state: LoadingState.Success) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
            contentDescription = "Success",
            tint = Color.Green,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Green
        )
    }
}

/**
 * Error content composable
 */
@Composable
private fun ErrorContent(state: LoadingState.Error) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.Info,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )

        if (state.canRetry) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Retry logic would be handled by caller */ }
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Loading state extensions for common operations
 */
object LoadingOperations {
    const val INITIALIZING = "initializing"
    const val LOADING_TEMPLATES = "loading_templates"
    const val LOADING_PLAYERS = "loading_players"
    const val SAVING_GAME = "saving_game"
    const val EXPORTING_DATA = "exporting_data"
    const val IMPORTING_DATA = "importing_data"
    const val GENERATING_CARD = "generating_card"
}

/**
 * Composable wrapper that shows loading overlay when operations are in progress
 */
@Composable
fun WithLoadingOverlay(
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        LoadingOverlay()
    }
}

/**
 * Hook for using loading states in composables
 */
@Composable
fun rememberLoadingState(operationId: String): LoadingState? {
    val loadingStates by loadingStateManager.loadingStates.collectAsState()
    return loadingStates[operationId]
}

/**
 * Utility functions for common loading operations
 */
suspend fun withLoadingState(
    operationId: String,
    message: String = "Loading...",
    block: suspend () -> Unit
) {
    try {
        loadingStateManager.startLoading(operationId, message)
        block()
        loadingStateManager.completeLoading(operationId)
    } catch (e: Exception) {
        loadingStateManager.failLoading(operationId, e.message ?: "Unknown error")
        throw e
    }
}

suspend fun withProgressLoading(
    operationId: String,
    message: String = "Loading...",
    block: suspend (updateProgress: (Float, String?) -> Unit) -> Unit
) {
    try {
        loadingStateManager.startLoading(operationId, message)

        block { progress, progressMessage ->
            loadingStateManager.updateProgress(operationId, progress, progressMessage)
        }

        loadingStateManager.completeLoading(operationId)
    } catch (e: Exception) {
        loadingStateManager.failLoading(operationId, e.message ?: "Unknown error")
        throw e
    }
}
