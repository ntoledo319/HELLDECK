package com.helldeck.utils

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Centralized error handling for HELLDECK
 * Provides consistent error handling across the application
 */
object ErrorHandler {

    private val errorListeners = mutableListOf<ErrorListener>()

    val listeners: List<ErrorListener> get() = errorListeners

    /**
     * Register an error listener
     */
    fun addErrorListener(listener: ErrorListener) {
        errorListeners.add(listener)
    }

    /**
     * Remove an error listener
     */
    fun removeErrorListener(listener: ErrorListener) {
        errorListeners.remove(listener)
    }

    /**
     * Handle an error with context information
     */
    fun handleError(
        error: Throwable,
        context: String? = null,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        userMessage: String? = null
    ) {
        var errorInfo = ErrorInfo(
            error = error,
            context = context,
            severity = severity,
            userMessage = userMessage,
            timestamp = System.currentTimeMillis()
        )

        // Log the error
        when (severity) {
            ErrorSeverity.LOW -> Logger.d("Error in $context: ${error.message}")
            ErrorSeverity.MEDIUM -> Logger.w("Error in $context: ${error.message}", error)
            ErrorSeverity.HIGH -> Logger.e("Error in $context: ${error.message}", error)
            ErrorSeverity.CRITICAL -> Logger.f("Critical error in $context: ${error.message}", error)
        }

        // Notify listeners
        errorListeners.forEach { listener ->
            try {
                listener.onError(errorInfo)
            } catch (e: Exception) {
                Logger.e("Error in error listener", e)
            }
        }
    }

    /**
     * Create a coroutine exception handler
     */
    fun createCoroutineExceptionHandler(
        context: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            handleError(throwable, context, severity)
        }
    }

    /**
     * Wrap a suspend function with error handling
     */
    suspend fun <T> withErrorHandling(
        context: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        block: suspend () -> T
    ): Result<T> {
        return try {
            var result = block()
            Result.success(result)
        } catch (e: Exception) {
            handleError(e, context, severity)
            Result.failure(e)
        }
    }

    /**
     * Execute a function with error handling
     */
    fun <T> executeWithErrorHandling(
        context: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        block: () -> T
    ): Result<T> {
        return try {
            var result = block()
            Result.success(result)
        } catch (e: Exception) {
            handleError(e, context, severity)
            Result.failure(e)
        }
    }
}

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    LOW,      // Minor issues, app continues to function
    MEDIUM,   // Moderate issues, some features may not work
    HIGH,     // Serious issues, app may be unstable
    CRITICAL  // Critical issues, app cannot continue
}

/**
 * Error information container
 */
data class ErrorInfo(
    val error: Throwable,
    val context: String?,
    val severity: ErrorSeverity,
    val userMessage: String?,
    val timestamp: Long
)

/**
 * Error listener interface
 */
interface ErrorListener {
    fun onError(errorInfo: ErrorInfo)
}

/**
 * Default error listener that logs errors
 */
class DefaultErrorListener : ErrorListener {
    override fun onError(errorInfo: ErrorInfo) {
        // Default implementation just logs
        Logger.i("Error handled: ${errorInfo.error.message} in ${errorInfo.context}")
    }
}

/**
 * Coroutine scope with error handling
 */
class ErrorHandlingScope(
    context: CoroutineContext = Dispatchers.Default,
    errorContext: String = "CoroutineScope"
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = context + ErrorHandler.createCoroutineExceptionHandler(errorContext)

    init {
        // Add default error listener if none exists
        if (ErrorHandler.listeners.isEmpty()) {
            ErrorHandler.addErrorListener(DefaultErrorListener())
        }
    }
}

/**
 * Extension functions for easier error handling
 */
suspend fun <T> withErrorHandling(
    context: String,
    severity: ErrorSeverity = ErrorSeverity.MEDIUM,
    block: suspend () -> T
): T? {
    return ErrorHandler.withErrorHandling(context, severity, block).getOrNull()
}

fun <T> executeWithErrorHandling(
    context: String,
    severity: ErrorSeverity = ErrorSeverity.MEDIUM,
    block: () -> T
): T? {
    return ErrorHandler.executeWithErrorHandling(context, severity, block).getOrNull()
}

/**
 * Try-catch extension for cleaner code
 */
inline fun <T> tryOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        Logger.w("Operation failed: ${e.message}")
        null
    }
}

/**
 * Try-catch extension with error handling
 */
inline fun <T> tryOrHandle(
    context: String,
    severity: ErrorSeverity = ErrorSeverity.MEDIUM,
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        ErrorHandler.handleError(e, context, severity)
        null
    }
}