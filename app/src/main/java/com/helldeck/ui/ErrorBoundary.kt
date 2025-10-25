package com.helldeck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.components.HelldeckColors
import com.helldeck.ui.components.HelldeckSpacing
import com.helldeck.ui.components.GiantButton
import kotlinx.coroutines.CancellationException

/**
 * Sealed class representing different types of errors that can occur in the app
 */
sealed class HelldeckError(
    val title: String,
    val message: String,
    val recoverable: Boolean = true,
    val technicalDetails: String? = null
) {
    class NetworkError(
        message: String,
        technicalDetails: String? = null
    ) : HelldeckError(
        title = "Connection Error",
        message = message,
        recoverable = true,
        technicalDetails = technicalDetails
    )

    class DatabaseError(
        message: String,
        technicalDetails: String? = null
    ) : HelldeckError(
        title = "Data Error",
        message = message,
        recoverable = true,
        technicalDetails = technicalDetails
    )

    class GameEngineError(
        message: String,
        technicalDetails: String? = null
    ) : HelldeckError(
        title = "Game Error",
        message = message,
        recoverable = true,
        technicalDetails = technicalDetails
    )

    class ValidationError(
        message: String,
        technicalDetails: String? = null
    ) : HelldeckError(
        title = "Validation Error",
        message = message,
        recoverable = true,
        technicalDetails = technicalDetails
    )

    class CriticalError(
        message: String,
        technicalDetails: String? = null
    ) : HelldeckError(
        title = "Critical Error",
        message = message,
        recoverable = false,
        technicalDetails = technicalDetails
    )

    class LLMError(
        message: String,
        technicalDetails: String? = null
    ) : HelldeckError(
        title = "AI Model Error",
        message = message,
        recoverable = true,
        technicalDetails = technicalDetails
    )

    class FileNotFoundError(
        fileName: String
    ) : HelldeckError(
        title = "File Not Found",
        message = "Required file '$fileName' could not be found",
        recoverable = true
    )

    class CorruptedDataError(
        dataType: String
    ) : HelldeckError(
        title = "Corrupted Data",
        message = "Some $dataType data appears to be corrupted",
        recoverable = true
    )

    class InsufficientResourcesError(
        resource: String
    ) : HelldeckError(
        title = "Insufficient Resources",
        message = "Not enough $resource available to complete this operation",
        recoverable = true
    )

    class TimeoutError(
        operation: String
    ) : HelldeckError(
        title = "Operation Timeout",
        message = "The $operation operation took too long to complete",
        recoverable = true
    )

    class PermissionError(
        permission: String
    ) : HelldeckError(
        title = "Permission Denied",
        message = "Permission '$permission' is required but was denied",
        recoverable = false
    )

    class UnknownError(
        message: String,
        technicalDetails: String? = null
    ) : HelldeckError(
        title = "Unexpected Error",
        message = message,
        recoverable = true,
        technicalDetails = technicalDetails
    )
}

/**
 * Error boundary state that captures and displays errors gracefully
 */
@Composable
fun ErrorBoundary(
    error: HelldeckError?,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (error == null) {
        return
    }

    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        colors = CardDefaults.cardColors(
            containerColor = HelldeckColors.DarkGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HelldeckSpacing.Large.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = HelldeckSpacing.Medium.dp)
                )
                
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.Red
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            // Error message
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyLarge,
                color = HelldeckColors.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Technical details (if available and in debug mode)
            if (error.technicalDetails != null) {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Technical Details:\n${error.technicalDetails}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = HelldeckColors.LightGray.copy(alpha = 0.7f),
                        modifier = Modifier.padding(HelldeckSpacing.Medium.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp)
            ) {
                if (error.recoverable && onRetry != null) {
                    GiantButton(
                        text = "Try Again",
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HelldeckColors.Green
                        )
                    )
                }

                if (onDismiss != null) {
                    GiantButton(
                        text = if (error.recoverable) "Dismiss" else "Close App",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (error.recoverable) HelldeckColors.Orange else HelldeckColors.Red
                        )
                    )
                }
            }

            // Recovery suggestions for recoverable errors
            if (error.recoverable) {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = HelldeckColors.MediumGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(HelldeckSpacing.Medium.dp)
                    ) {
                        Text(
                            text = "💡 Recovery Suggestions:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = HelldeckColors.Yellow
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                        
                        when (error) {
                            is HelldeckError.NetworkError -> {
                                Text(
                                    text = "• Check your internet connection\n• Try switching between Wi-Fi and mobile data\n• Restart the app if the problem persists",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HelldeckColors.LightGray
                                )
                            }
                            is HelldeckError.DatabaseError -> {
                                Text(
                                    text = "• Restart the app to refresh data\n• Clear app cache if issues persist\n• Contact support if data appears corrupted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HelldeckColors.LightGray
                                )
                            }
                            is HelldeckError.LLMError -> {
                                Text(
                                    text = "• Check if AI model is downloaded\n• Try restarting the app\n• Ensure sufficient device storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HelldeckColors.LightGray
                                )
                            }
                            is HelldeckError.FileNotFoundError -> {
                                Text(
                                    text = "• Reinstall the app to restore missing files\n• Check for app updates\n• Contact support if issue persists",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HelldeckColors.LightGray
                                )
                            }
                            else -> {
                                Text(
                                    text = "• Try the operation again\n• Restart the app\n• Check for available updates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HelldeckColors.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Error boundary wrapper that catches exceptions and converts them to HelldeckError
 * NOTE: Composable try-catch is not supported. Use error state management instead.
 * This function is kept for reference but should not be used around @Composable content.
 */
fun convertExceptionToHelldeckError(e: Exception): HelldeckError {
    return when {
        e is java.net.SocketException ||
        e is java.net.UnknownHostException ||
        e is java.net.ConnectException ||
        e.message?.contains("network", ignoreCase = true) == true -> {
            HelldeckError.NetworkError(
                message = "Unable to connect to servers. Please check your internet connection.",
                technicalDetails = e.message
            )
        }
        
        e is java.sql.SQLException ||
        e.message?.contains("database", ignoreCase = true) == true -> {
            HelldeckError.DatabaseError(
                message = "Database operation failed. Your data is safe, but the operation couldn't complete.",
                technicalDetails = e.message
            )
        }
        
        e is java.io.FileNotFoundException ||
        e.message?.contains("file not found", ignoreCase = true) == true -> {
            HelldeckError.FileNotFoundError(
                fileName = e.message?.substringAfter("file:") ?: "unknown"
            )
        }
        
        e is java.lang.OutOfMemoryError ||
        e.message?.contains("out of memory", ignoreCase = true) == true -> {
            HelldeckError.InsufficientResourcesError(
                resource = "memory"
            )
        }
        
        e is java.util.concurrent.TimeoutException ||
        e.message?.contains("timeout", ignoreCase = true) == true -> {
            HelldeckError.TimeoutError(
                operation = "data loading"
            )
        }
        
        e is SecurityException ||
        e.message?.contains("permission", ignoreCase = true) == true -> {
            HelldeckError.PermissionError(
                permission = e.message?.substringAfter("permission:") ?: "unknown"
            )
        }
        
        else -> {
            HelldeckError.UnknownError(
                message = e.message ?: "An unexpected error occurred",
                technicalDetails = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }
}

/**
 * Loading state with error handling
 */
@Composable
fun LoadingWithErrorBoundary(
    isLoading: Boolean,
    error: HelldeckError?,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    loadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                loadingContent()
            }
            error != null -> {
                ErrorBoundary(
                    error = error,
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
        }
    }
}