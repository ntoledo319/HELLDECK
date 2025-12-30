package com.helldeck.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enhanced logging utility for HELLDECK
 * Supports console logging, file logging, and remote logging
 */
object Logger {

    private const val TAG = "HELLDECK"
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    private const val MAX_FILES = 5

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var logLevel = LogLevel.INFO
    private var enableFileLogging = false
    private var enableRemoteLogging = false
    private var logDirectory: File? = null
    private var remoteEndpoint: String? = null

    /**
     * Initialize logger with configuration
     */
    fun initialize(
        context: android.content.Context,
        config: LoggerConfig = LoggerConfig(),
    ) {
        logLevel = config.level
        enableFileLogging = config.enableFileLogging
        enableRemoteLogging = config.enableRemoteLogging
        logDirectory = config.logDirectory?.let { File(it) } ?: context.getExternalFilesDir("logs")
        remoteEndpoint = config.remoteEndpoint

        if (enableFileLogging && logDirectory != null) {
            logDirectory?.mkdirs()
            cleanupOldLogFiles()
        }
    }

    /**
     * Log verbose message
     */
    fun v(message: String, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, message, throwable)
    }

    /**
     * Log debug message
     */
    fun d(message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, message, throwable)
    }

    /**
     * Log info message
     */
    fun i(message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, message, throwable)
    }

    /**
     * Log warning message
     */
    fun w(message: String, throwable: Throwable? = null) {
        log(LogLevel.WARNING, message, throwable)
    }

    /**
     * Log error message
     */
    fun e(message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, message, throwable)
    }

    /**
     * Log fatal message
     */
    fun f(message: String, throwable: Throwable? = null) {
        log(LogLevel.FATAL, message, throwable)
    }

    /**
     * Main logging method
     */
    private fun log(level: LogLevel, message: String, throwable: Throwable?) {
        if (level.priority < logLevel.priority) return

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message,
            throwable = throwable,
            thread = Thread.currentThread().name,
            tag = TAG,
        )

        // Add to queue for async processing
        logQueue.offer(entry)

        // Process log entry asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            processLogEntry(entry)
        }
    }

    /**
     * Process log entry (console, file, remote)
     */
    private fun processLogEntry(entry: LogEntry) {
        val formattedMessage = formatLogMessage(entry)

        // Console logging
        when (entry.level) {
            LogLevel.VERBOSE -> Log.v(entry.tag, formattedMessage, entry.throwable)
            LogLevel.DEBUG -> Log.d(entry.tag, formattedMessage, entry.throwable)
            LogLevel.INFO -> Log.i(entry.tag, formattedMessage, entry.throwable)
            LogLevel.WARNING -> Log.w(entry.tag, formattedMessage, entry.throwable)
            LogLevel.ERROR -> Log.e(entry.tag, formattedMessage, entry.throwable)
            LogLevel.FATAL -> Log.wtf(entry.tag, formattedMessage, entry.throwable)
        }

        // File logging
        if (enableFileLogging) {
            writeToFile(entry)
        }

        // Remote logging
        if (enableRemoteLogging && entry.level.priority >= LogLevel.WARNING.priority) {
            sendToRemote(entry)
        }
    }

    /**
     * Format log message
     */
    private fun formatLogMessage(entry: LogEntry): String {
        return "[${entry.level.displayName[0]}] ${dateFormat.format(
            Date(entry.timestamp),
        )} [${entry.thread}] ${entry.message}"
    }

    /**
     * Write log entry to file
     */
    private fun writeToFile(entry: LogEntry) {
        try {
            val logDir = logDirectory ?: return
            val date = fileDateFormat.format(Date(entry.timestamp))
            val logFile = File(logDir, "helldeck_$date.log")

            PrintWriter(FileWriter(logFile, true)).use { writer ->
                writer.println(formatLogMessage(entry))
                entry.throwable?.let { throwable ->
                    writer.println("Exception: ${throwable.message}")
                    throwable.printStackTrace(writer)
                }
            }

            // Check file size and rotate if necessary
            if (logFile.length() > MAX_FILE_SIZE) {
                rotateLogFiles()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }

    /**
     * Send log entry to remote endpoint
     */
    private fun sendToRemote(entry: LogEntry) {
        try {
            remoteEndpoint?.let { endpoint ->
                // In a real implementation, you would send to your logging service
                // For now, just log that we would send it
                Log.d(TAG, "Would send to remote: $endpoint - ${entry.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to remote", e)
        }
    }

    /**
     * Rotate log files when they get too large
     */
    private fun rotateLogFiles() {
        try {
            val logDir = logDirectory ?: return

            // Get all log files sorted by modification time
            val logFiles = logDir.listFiles { file ->
                file.name.startsWith("helldeck_") && file.name.endsWith(".log")
            }?.sortedBy { it.lastModified() } ?: return

            // Delete oldest files if we have too many
            if (logFiles.size >= MAX_FILES) {
                logFiles.take(logFiles.size - MAX_FILES + 1).forEach { it.delete() }
            }

            // Rename current file
            val currentFile = logFiles.lastOrNull() ?: return
            val newName = "helldeck_${fileDateFormat.format(Date())}_${System.currentTimeMillis()}.log"
            currentFile.renameTo(File(logDir, newName))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log files", e)
        }
    }

    /**
     * Cleanup old log files
     */
    private fun cleanupOldLogFiles() {
        try {
            val logDir = logDirectory ?: return
            val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days

            logDir.listFiles { file ->
                file.name.startsWith("helldeck_") && file.name.endsWith(".log") && file.lastModified() < cutoffTime
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old log files", e)
        }
    }

    /**
     * Get all log files
     */
    fun getLogFiles(): List<File> {
        return try {
            val logDir = logDirectory ?: return emptyList()
            logDir.listFiles { file ->
                file.name.startsWith("helldeck_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Export logs to file
     */
    fun exportLogs(context: android.content.Context, outputFile: File): Boolean {
        return try {
            val logFiles = getLogFiles()
            PrintWriter(FileWriter(outputFile)).use { writer ->
                writer.println("HELLDECK Log Export - ${Date()}")
                writer.println("=".repeat(50))

                logFiles.forEach { file ->
                    writer.println("\n--- ${file.name} ---")
                    file.forEachLine { line ->
                        writer.println(line)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear all logs
     */
    fun clearLogs(): Boolean {
        return try {
            getLogFiles().forEach { it.delete() }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get log statistics
     */
    fun getLogStats(): LogStats {
        val logFiles = getLogFiles()
        val totalSize = logFiles.sumOf { it.length() }
        val totalLines = logFiles.sumOf { it.readLines().size }

        return LogStats(
            totalFiles = logFiles.size,
            totalSizeBytes = totalSize,
            totalLines = totalLines,
            oldestFile = logFiles.lastOrNull()?.name,
            newestFile = logFiles.firstOrNull()?.name,
        )
    }
}

/**
 * Log level enumeration
 */
enum class LogLevel(val priority: Int, val displayName: String) {
    VERBOSE(0, "VERBOSE"),
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO"),
    WARNING(3, "WARNING"),
    ERROR(4, "ERROR"),
    FATAL(5, "FATAL"),
    ;

    override fun toString(): String = displayName
}

/**
 * Log entry data class
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val message: String,
    val throwable: Throwable?,
    val thread: String,
    val tag: String,
)

/**
 * Logger configuration
 */
data class LoggerConfig(
    val level: LogLevel = LogLevel.INFO,
    val enableFileLogging: Boolean = true,
    val enableRemoteLogging: Boolean = false,
    val logDirectory: String? = null,
    val remoteEndpoint: String? = null,
    val maxFileSizeMB: Int = 10,
    val maxFiles: Int = 5,
    val retentionDays: Int = 7,
)

/**
 * Log statistics
 */
data class LogStats(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val totalLines: Int,
    val oldestFile: String?,
    val newestFile: String?,
)

/**
 * Performance logging utility
 */
object PerformanceLogger {

    private val performanceMetrics = mutableMapOf<String, PerformanceMetric>()

    /**
     * Start performance measurement
     */
    fun startMeasurement(operation: String) {
        performanceMetrics[operation] = PerformanceMetric(
            operation = operation,
            startTime = System.currentTimeMillis(),
            startMemory = getUsedMemory(),
        )
    }

    /**
     * End performance measurement
     */
    fun endMeasurement(operation: String): PerformanceMetric? {
        return performanceMetrics.remove(operation)?.let { metric ->
            metric.copy(
                endTime = System.currentTimeMillis(),
                endMemory = getUsedMemory(),
                duration = System.currentTimeMillis() - metric.startTime,
                memoryUsed = getUsedMemory() - metric.startMemory,
            )
        }
    }

    /**
     * Log performance metric
     */
    fun logPerformance(operation: String) {
        endMeasurement(operation)?.let { metric ->
            Logger.i(
                "Performance: $operation took ${metric.duration}ms, used ${AppUtils.formatFileSize(
                    metric.memoryUsed,
                )} memory",
            )
        }
    }

    /**
     * Get all performance metrics
     */
    fun getMetrics(): Map<String, PerformanceMetric> {
        return performanceMetrics.toMap()
    }

    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}

/**
 * Performance metric data class
 */
data class PerformanceMetric(
    val operation: String,
    val startTime: Long,
    val endTime: Long = 0,
    val startMemory: Long = 0,
    val endMemory: Long = 0,
    val duration: Long = 0,
    val memoryUsed: Long = 0,
)

/**
 * Database query logging
 */
object DatabaseLogger {

    private val queryMetrics = mutableMapOf<String, QueryMetric>()

    /**
     * Log database query
     */
    fun logQuery(query: String, duration: Long, rowCount: Int = 0) {
        val normalizedQuery = normalizeQuery(query)

        val existingMetric = queryMetrics.getOrPut(normalizedQuery) {
            QueryMetric(normalizedQuery)
        }

        val metric = existingMetric.copy(
            executionCount = existingMetric.executionCount + 1,
            totalDuration = existingMetric.totalDuration + duration,
            totalRows = existingMetric.totalRows + rowCount,
            lastExecution = System.currentTimeMillis(),
        )

        queryMetrics[normalizedQuery] = metric

        if (duration > 100) { // Log slow queries
            Logger.w("Slow query (${duration}ms): $normalizedQuery")
        }
    }

    /**
     * Get query statistics
     */
    fun getQueryStats(): Map<String, QueryMetric> {
        return queryMetrics.toMap()
    }

    /**
     * Normalize query for grouping
     */
    private fun normalizeQuery(query: String): String {
        return query
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\d+"), "?")
            .trim()
    }
}

/**
 * Query metric data class
 */
data class QueryMetric(
    val query: String,
    val executionCount: Int = 0,
    val totalDuration: Long = 0,
    val totalRows: Int = 0,
    val lastExecution: Long = 0,
    val averageDuration: Long = if (executionCount > 0) totalDuration / executionCount else 0,
)

/**
 * Game event logging
 */
object GameEventLogger {

    /**
     * Log game event
     */
    fun logEvent(event: String, data: Map<String, Any> = emptyMap()) {
        val eventData = mapOf(
            "event" to event,
            "timestamp" to System.currentTimeMillis(),
            "session" to "current_session", // Would get from game engine
        ) + data

        Logger.i("Game Event: $event - $data")
    }

    /**
     * Log player action
     */
    fun logPlayerAction(playerId: String, action: String, data: Map<String, Any> = emptyMap()) {
        logEvent(
            "player_action",
            mapOf(
                "playerId" to playerId,
                "action" to action,
            ) + data,
        )
    }

    /**
     * Log game state change
     */
    fun logGameStateChange(oldState: String, newState: String, data: Map<String, Any> = emptyMap()) {
        logEvent(
            "game_state_change",
            mapOf(
                "oldState" to oldState,
                "newState" to newState,
            ) + data,
        )
    }

    /**
     * Log error with context
     */
    fun logError(error: String, context: Map<String, Any> = emptyMap()) {
        logEvent("error", mapOf("error" to error) + context)
    }
}

/**
 * Extension function for logging verbose messages with object context.
 *
 * @param message The message to log.
 */
fun Any.logv(message: String) = Logger.v("$this: $message")

/**
 * Extension function for logging debug messages with object context.
 *
 * @param message The message to log.
 */
fun Any.logd(message: String) = Logger.d("$this: $message")

/**
 * Extension function for logging info messages with object context.
 *
 * @param message The message to log.
 */
fun Any.logi(message: String) = Logger.i("$this: $message")

/**
 * Extension function for logging warning messages with object context.
 *
 * @param message The message to log.
 */
fun Any.logw(message: String) = Logger.w("$this: $message")

/**
 * Extension function for logging error messages with object context.
 *
 * @param message The message to log.
 * @param throwable Optional throwable to include.
 */
fun Any.loge(message: String, throwable: Throwable? = null) = Logger.e("$this: $message", throwable)

/**
 * Extension function for logging fatal messages with object context.
 *
 * @param message The message to log.
 * @param throwable Optional throwable to include.
 */
fun Any.logf(message: String, throwable: Throwable? = null) = Logger.f("$this: $message", throwable)
