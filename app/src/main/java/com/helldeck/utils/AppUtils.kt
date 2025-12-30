package com.helldeck.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Application utility functions for HELLDECK
 */
object AppUtils {

    private var mainHandler = Handler(Looper.getMainLooper())

    /**
     * Get app version information
     */
    fun getAppVersion(context: Context): AppVersion {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            AppVersion(
                versionName = packageInfo.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    packageInfo.versionCode
                },
                buildTime = getBuildTime(),
                gitHash = getGitHash(),
            )
        } catch (e: Exception) {
            AppVersion.unknown()
        }
    }

    /**
     * Get device information
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            model = Build.MODEL,
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            screenResolution = getScreenResolution(context),
            totalMemory = getTotalMemory(),
            availableMemory = getAvailableMemory(),
            isEmulator = isEmulator(),
        )
    }

    /**
     * Format file size in human readable format
     */
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024.0
            unitIndex++
        }

        return "%.1f %s".format(size, units[unitIndex])
    }

    /**
     * Format duration in milliseconds to human readable format
     */
    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
            minutes > 0 -> "%d:%02d".format(minutes, seconds)
            else -> "%d seconds".format(seconds)
        }
    }

    /**
     * Format timestamp to readable date
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    /**
     * Debounce function for search and input
     */
    fun debounce(
        delayMs: Long = 300L,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        action: () -> Unit,
    ): () -> Unit {
        var debounceJob: Job? = null

        return {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(delayMs)
                action()
            }
        }
    }

    /**
     * Run code on main thread
     */
    fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Run code on background thread
     */
    fun runOnBackgroundThread(action: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            action()
        }
    }

    /**
     * Check if app has permission
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return try {
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get cache directory size
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDir = context.cacheDir
            cacheDir.walkTopDown().sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Clear cache directory
     */
    fun clearCache(context: Context): Boolean {
        return try {
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Generate random ID
     */
    fun generateId(prefix: String = "", length: Int = 8): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        val suffix = (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
        return if (prefix.isNotEmpty()) "$prefix$suffix" else suffix
    }

    /**
     * Get screen resolution
     */
    private fun getScreenResolution(context: Context): String {
        return try {
            val displayMetrics = context.resources.displayMetrics
            "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get total memory
     */
    private fun getTotalMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            runtime.totalMemory()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get available memory
     */
    private fun getAvailableMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            runtime.freeMemory()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Check if running on emulator
     */
    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk_google") ||
            Build.PRODUCT.contains("google_sdk") ||
            Build.PRODUCT.contains("sdk") ||
            Build.PRODUCT.contains("sdk_x86") ||
            Build.PRODUCT.contains("vbox86p") ||
            Build.PRODUCT.contains("emulator") ||
            Build.PRODUCT.contains("simulator")
    }

    /**
     * Get build time from build config
     */
    private fun getBuildTime(): String {
        return try {
            // This would be set in build.gradle as BuildConfig.BUILD_TIME
            "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Get git hash from build config
     */
    private fun getGitHash(): String {
        return try {
            // This would be set in build.gradle as BuildConfig.GIT_HASH
            "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}

/**
 * App version data class
 */
data class AppVersion(
    val versionName: String,
    val versionCode: Int,
    val buildTime: String,
    val gitHash: String,
) {
    companion object {
        fun unknown() = AppVersion("unknown", 0, "unknown", "unknown")
    }

    override fun toString(): String {
        return "$versionName ($versionCode)"
    }
}

/**
 * Device information data class
 */
data class DeviceInfo(
    val model: String,
    val brand: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val screenResolution: String,
    val totalMemory: Long,
    val availableMemory: Long,
    val isEmulator: Boolean,
) {
    override fun toString(): String {
        return "$manufacturer $model (Android $androidVersion, API $apiLevel)"
    }
}

/**
 * Performance monitoring utilities
 */
object PerformanceMonitor {

    private var metrics = mutableMapOf<String, Long>()
    private var counters = mutableMapOf<String, Int>()

    /**
     * Start timing an operation
     */
    fun startTiming(operation: String) {
        metrics[operation] = System.currentTimeMillis()
    }

    /**
     * End timing an operation and return duration
     */
    fun endTiming(operation: String): Long {
        val startTime = metrics[operation] ?: return 0L
        val duration = System.currentTimeMillis() - startTime
        metrics.remove(operation)
        return duration
    }

    /**
     * Increment counter
     */
    fun incrementCounter(counter: String) {
        counters[counter] = (counters[counter] ?: 0) + 1
    }

    /**
     * Get counter value
     */
    fun getCounter(counter: String): Int {
        return counters[counter] ?: 0
    }

    /**
     * Get all metrics
     */
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "timers" to metrics.toMap(),
            "counters" to counters.toMap(),
            "memory" to mapOf(
                "used" to (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()),
                "total" to Runtime.getRuntime().totalMemory(),
                "free" to Runtime.getRuntime().freeMemory(),
            ),
        )
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        metrics.clear()
        counters.clear()
    }

    /**
     * Get optimization level (for compatibility)
     */
    val optimizationLevel: Int
        get() = 1
}

/**
 * File utilities
 */
object FileUtils {

    /**
     * Safely delete file or directory
     */
    fun safeDelete(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get file extension
     */
    fun getExtension(filename: String): String {
        return filename.substringAfterLast('.', "")
    }

    /**
     * Get filename without extension
     */
    fun getNameWithoutExtension(filename: String): String {
        return filename.substringBeforeLast('.')
    }

    /**
     * Create temporary file
     */
    fun createTempFile(context: Context, prefix: String, extension: String): File {
        val tempDir = context.cacheDir
        val timestamp = System.currentTimeMillis()
        return File(tempDir, "${prefix}_$timestamp.$extension")
    }

    /**
     * Copy file with progress callback
     */
    fun copyFileWithProgress(
        source: File,
        destination: File,
        progressCallback: ((Long, Long) -> Unit)? = null,
    ): Boolean {
        return try {
            val totalSize = source.length()
            var copiedSize = 0L

            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        copiedSize += bytesRead

                        progressCallback?.invoke(copiedSize, totalSize)
                    }
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * String utilities
 */
object StringUtils {

    /**
     * Capitalize first letter
     */
    fun capitalize(str: String): String {
        return str.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Truncate string with ellipsis
     */
    fun truncate(str: String, maxLength: Int): String {
        return if (str.length <= maxLength) {
            str
        } else {
            str.take(maxLength - 3) + "..."
        }
    }

    /**
     * Remove extra whitespace
     */
    fun normalizeWhitespace(str: String): String {
        return str.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Check if string is empty or whitespace only
     */
    fun isBlank(str: String?): Boolean {
        return str.isNullOrBlank() || str.trim().isEmpty()
    }

    /**
     * Generate slug from string
     */
    fun slugify(str: String): String {
        return str
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }
}

/**
 * Math utilities
 */
object MathUtils {

    /**
     * Clamp value between min and max
     */
    fun clamp(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }

    /**
     * Clamp value between min and max (Double)
     */
    fun clamp(value: Double, min: Double, max: Double): Double {
        return value.coerceIn(min, max)
    }

    /**
     * Map value from one range to another
     */
    fun map(value: Double, fromMin: Double, fromMax: Double, toMin: Double, toMax: Double): Double {
        return toMin + (value - fromMin) * (toMax - toMin) / (fromMax - fromMin)
    }

    /**
     * Linear interpolation between two values
     */
    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * clamp(t, 0.0, 1.0)
    }

    /**
     * Calculate percentage
     */
    fun percentage(part: Number, whole: Number): Double {
        return (part.toDouble() / whole.toDouble()) * 100.0
    }

    /**
     * Round to specified decimal places
     */
    fun round(value: Double, decimals: Int): Double {
        var multiplier = Math.pow(10.0, decimals.toDouble())
        return kotlin.math.round(value * multiplier) / multiplier
    }
}

/**
 * Network utilities
 */
object NetworkUtils {

    /**
     * Check if device is connected to internet
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get network type
     */
    fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                when {
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                    capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                    else -> "Unknown"
                }
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo?.typeName ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

/**
 * Animation utilities
 */
object AnimationUtils {

    /**
     * Create spring animation spec
     */
    fun createSpringAnimation(
        dampingRatio: Float = 0.8f,
        stiffness: Float = androidx.compose.animation.core.Spring.StiffnessLow.toFloat(),
    ): androidx.compose.animation.core.SpringSpec<Float> {
        return androidx.compose.animation.core.spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
        )
    }

    /**
     * Create tween animation spec
     */
    fun createTweenAnimation(
        durationMillis: Int = 300,
        easing: androidx.compose.animation.core.Easing = androidx.compose.animation.core.EaseOutCubic,
    ): androidx.compose.animation.core.TweenSpec<Float> {
        return androidx.compose.animation.core.tween(
            durationMillis = durationMillis,
            easing = easing,
        )
    }

    /**
     * Create infinite repeatable animation spec
     */
    fun createInfiniteAnimation(
        animation: androidx.compose.animation.core.DurationBasedAnimationSpec<Float>,
        repeatMode: androidx.compose.animation.core.RepeatMode = androidx.compose.animation.core.RepeatMode.Restart,
    ): androidx.compose.animation.core.InfiniteRepeatableSpec<Float> {
        return androidx.compose.animation.core.infiniteRepeatable(
            animation = animation,
            repeatMode = repeatMode,
        )
    }
}
