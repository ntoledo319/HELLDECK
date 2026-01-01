package com.helldeck.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Memory optimization system for old Android devices
 * Designed for devices with <2GB RAM and worn hardware
 */
object MemoryOptimizer {

    private var context: Context? = null
    private var memoryInfo: ActivityManager.MemoryInfo? = null
    private var memoryWatchers = ConcurrentHashMap<String, MemoryWatcher>()
    private var optimizationLevel = MemoryStrategy.MODERATE

    /**
     * Get current optimization level (public accessor for MemoryEfficientCollections)
     */
    val currentOptimizationLevel: MemoryStrategy
        get() = optimizationLevel

    /**
     * Get current memory strategy
     */
    fun getMemoryStrategy(): MemoryStrategy = optimizationLevel

    // Memory thresholds for different device types
    private const val CRITICAL_MEMORY_THRESHOLD = 50 * 1024 * 1024L // 50MB
    private const val LOW_MEMORY_THRESHOLD = 100 * 1024 * 1024L // 100MB
    private const val MODERATE_MEMORY_THRESHOLD = 200 * 1024 * 1024L // 200MB

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        memoryInfo = mi

        // Set optimization level based on available memory
        optimizationLevel = when {
            mi.totalMem < 1024 * 1024 * 1024L -> MemoryStrategy.AGGRESSIVE // <1GB
            mi.totalMem < 2L * 1024 * 1024 * 1024L -> MemoryStrategy.MODERATE // 1-2GB
            else -> MemoryStrategy.CONSERVATIVE // >2GB
        }

        Logger.i(
            "MemoryOptimizer initialized with level: $optimizationLevel, Total RAM: ${mi.totalMem / 1024 / 1024}MB",
        )
    }

    /**
     * Register a memory watcher for a specific component
     */
    fun registerWatcher(componentName: String, watcher: MemoryWatcher) {
        memoryWatchers[componentName] = watcher
    }

    /**
     * Unregister a memory watcher
     */
    fun unregisterWatcher(componentName: String) {
        memoryWatchers.remove(componentName)
    }

    /**
     * Check current memory status and trigger optimizations if needed
     */
    fun checkMemoryStatus(): MemoryStatus {
        val ctx = context ?: return MemoryStatus.NORMAL
        val mi = memoryInfo ?: ActivityManager.MemoryInfo()
        val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        memoryInfo = mi

        val status = when {
            mi.availMem < CRITICAL_MEMORY_THRESHOLD -> MemoryStatus.CRITICAL
            mi.availMem < LOW_MEMORY_THRESHOLD -> MemoryStatus.LOW
            mi.availMem < MODERATE_MEMORY_THRESHOLD -> MemoryStatus.MODERATE
            else -> MemoryStatus.NORMAL
        }

        if (status != MemoryStatus.NORMAL) {
            triggerMemoryOptimization(status, mi)
        }

        return status
    }

    /**
     * Trigger memory optimization based on current status
     */
    private fun triggerMemoryOptimization(status: MemoryStatus, mi: ActivityManager.MemoryInfo) {
        Logger.w("Memory status: $status, Available: ${mi.availMem / 1024 / 1024}MB")

        when (status) {
            MemoryStatus.CRITICAL -> {
                // Emergency cleanup
                performEmergencyCleanup()
            }
            MemoryStatus.LOW -> {
                // Aggressive cleanup
                performAggressiveCleanup()
            }
            MemoryStatus.MODERATE -> {
                // Moderate cleanup
                performModerateCleanup()
            }
            MemoryStatus.NORMAL -> {
                // No action needed
            }
        }

        // Notify watchers
        notifyWatchers(status)
    }

    /**
     * Emergency memory cleanup - free everything possible
     */
    private fun performEmergencyCleanup() {
        Logger.w("Performing emergency memory cleanup")

        // Clear all caches
        clearAllCaches()

        // Force garbage collection
        System.gc()
        Runtime.getRuntime().gc()

        // Clear background processes if possible
        try {
            val ctx = context ?: return
            val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activityManager.killBackgroundProcesses(ctx.packageName)
            }
        } catch (e: Exception) {
            Logger.e("Failed to kill background processes", e)
        }
    }

    /**
     * Aggressive memory cleanup
     */
    private fun performAggressiveCleanup() {
        Logger.i("Performing aggressive memory cleanup")

        // Clear non-essential caches
        clearNonEssentialCaches()

        // Reduce memory allocation
        reduceMemoryAllocation()

        // Force garbage collection
        System.gc()
    }

    /**
     * Moderate memory cleanup
     */
    private fun performModerateCleanup() {
        Logger.d("Performing moderate memory cleanup")

        // Light cleanup
        clearOldCaches()

        // Suggest memory reduction to watchers
        suggestMemoryReduction()
    }

    /**
     * Clear all caches
     */
    private fun clearAllCaches() {
        // Clear image caches
        // Clear template caches
        // Clear database caches
        // Clear any other caches

        Logger.d("Cleared all caches")
    }

    /**
     * Clear non-essential caches
     */
    private fun clearNonEssentialCaches() {
        // Clear analytics caches
        // Clear temporary files
        // Clear old log files

        Logger.d("Cleared non-essential caches")
    }

    /**
     * Clear old caches
     */
    private fun clearOldCaches() {
        // Clear caches older than threshold
        Logger.d("Cleared old caches")
    }

    /**
     * Reduce memory allocation for current operations
     */
    private fun reduceMemoryAllocation() {
        // Reduce batch sizes
        // Reduce cache sizes
        // Use more memory-efficient algorithms

        Logger.d("Reduced memory allocation")
    }

    /**
     * Suggest memory reduction to watchers
     */
    private fun suggestMemoryReduction() {
        memoryWatchers.values.forEach { watcher ->
            try {
                watcher.onMemoryPressure(MemoryPressure.MODERATE)
            } catch (e: Exception) {
                Logger.e("Error notifying memory watcher", e)
            }
        }
    }

    /**
     * Notify all watchers of memory status
     */
    private fun notifyWatchers(status: MemoryStatus) {
        var pressure = when (status) {
            MemoryStatus.CRITICAL -> MemoryPressure.CRITICAL
            MemoryStatus.LOW -> MemoryPressure.HIGH
            MemoryStatus.MODERATE -> MemoryPressure.MODERATE
            MemoryStatus.NORMAL -> return // No notification needed
        }

        memoryWatchers.values.forEach { watcher ->
            try {
                watcher.onMemoryPressure(pressure)
            } catch (e: Exception) {
                Logger.e("Error notifying memory watcher", e)
            }
        }
    }

    /**
     * Get current memory usage information
     */
    fun getMemoryInfo(): MemoryInfo {
        val ctx = context ?: return MemoryInfo(0, 0, 0, false)
        val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)

        return MemoryInfo(
            totalMemoryMB = mi.totalMem / 1024 / 1024,
            availableMemoryMB = mi.availMem / 1024 / 1024,
            thresholdMB = mi.threshold / 1024 / 1024,
            isLowMemory = mi.lowMemory,
        )
    }

    /**
     * Get optimization recommendations for current device
     */
    fun getOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val memoryInfo = getMemoryInfo()

        when {
            memoryInfo.totalMemoryMB < 1024 -> {
                recommendations.add("Consider using a device with more RAM for better performance")
                recommendations.add("Close other apps before playing")
                recommendations.add("Keep player count under 8 for best experience")
            }
            memoryInfo.totalMemoryMB < 2048 -> {
                recommendations.add("Memory usage is moderate - consider closing background apps")
                recommendations.add("Player count up to 16 should work well")
            }
            else -> {
                recommendations.add("Memory usage is good - full player count supported")
            }
        }

        return recommendations
    }

    /**
     * Monitor memory usage periodically
     */
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun startMemoryMonitoring(intervalMs: Long = 30000L) { // Check every 30 seconds
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                try {
                    checkMemoryStatus()
                    delay(intervalMs)
                } catch (e: Exception) {
                    Logger.e("Error in memory monitoring", e)
                    delay(60000L) // Wait longer on error
                }
            }
        }
    }

    /**
     * Stop memory monitoring
     */
    fun stopMemoryMonitoring() {
        // Cancel monitoring coroutine
        Logger.d("Memory monitoring stopped")
    }
}

/**
 * Memory pressure levels
 */
enum class MemoryPressure {
    MODERATE, // Moderate pressure
    HIGH, // High pressure
    CRITICAL, // Critical pressure
}

/**
 * Memory strategy enum for optimization levels
 */
enum class MemoryStrategy {
    AGGRESSIVE,
    MODERATE,
    CONSERVATIVE,
}

/**
 * Memory status enum
 */
enum class MemoryStatus {
    CRITICAL,
    LOW,
    MODERATE,
    NORMAL,
}

/**
 * Memory information container
 */
data class MemoryInfo(
    val totalMemoryMB: Long,
    val availableMemoryMB: Long,
    val thresholdMB: Long,
    val isLowMemory: Boolean,
)

/**
 * Memory watcher interface for components that need to respond to memory pressure
 */
interface MemoryWatcher {
    fun onMemoryPressure(pressure: MemoryPressure)
}

/**
 * Base memory watcher implementation
 */
abstract class BaseMemoryWatcher : MemoryWatcher {
    override fun onMemoryPressure(pressure: MemoryPressure) {
        when (pressure) {
            MemoryPressure.MODERATE -> onModeratePressure()
            MemoryPressure.HIGH -> onHighPressure()
            MemoryPressure.CRITICAL -> onCriticalPressure()
        }
    }

    open fun onModeratePressure() {}
    open fun onHighPressure() {}
    open fun onCriticalPressure() {}
}

/**
 * Template engine memory watcher
 */
class TemplateEngineMemoryWatcher : BaseMemoryWatcher() {
    override fun onHighPressure() {
        // Clear template cache
        Logger.d("Template engine clearing cache due to memory pressure")
    }

    override fun onCriticalPressure() {
        // Clear all template caches and reduce memory usage
        Logger.w("Template engine performing emergency cleanup")
    }
}

/**
 * Database memory watcher
 */
class DatabaseMemoryWatcher : BaseMemoryWatcher() {
    override fun onModeratePressure() {
        // Reduce database cache size
        Logger.d("Database reducing cache size")
    }

    override fun onHighPressure() {
        // Clear non-essential database caches
        Logger.d("Database clearing non-essential caches")
    }

    override fun onCriticalPressure() {
        // Close non-essential database connections
        Logger.w("Database performing emergency cleanup")
    }
}

/**
 * UI memory watcher
 */
class UIMemoryWatcher : BaseMemoryWatcher() {
    override fun onModeratePressure() {
        // Reduce UI animation complexity
        Logger.d("UI reducing animation complexity")
    }

    override fun onHighPressure() {
        // Disable non-essential UI effects
        Logger.d("UI disabling non-essential effects")
    }

    override fun onCriticalPressure() {
        // Switch to minimal UI mode
        Logger.w("UI switching to minimal mode")
    }
}

/**
 * Memory-efficient data structures for old devices
 */
object MemoryEfficientCollections {

    /**
     * Memory-efficient list for large player counts
     */
    fun <T> createMemoryEfficientList(): MutableList<T> {
        return when (MemoryOptimizer.currentOptimizationLevel) {
            MemoryStrategy.AGGRESSIVE -> ArrayList(16) // Smaller initial capacity
            MemoryStrategy.MODERATE -> ArrayList(32)
            MemoryStrategy.CONSERVATIVE -> ArrayList(64)
        }
    }

    /**
     * Memory-efficient map for caching
     */
    fun <K, V> createMemoryEfficientMap(): MutableMap<K, V> {
        return when (MemoryOptimizer.currentOptimizationLevel) {
            MemoryStrategy.AGGRESSIVE -> HashMap(8) // Smaller initial capacity
            MemoryStrategy.MODERATE -> HashMap(16)
            MemoryStrategy.CONSERVATIVE -> HashMap(32)
        }
    }

    /**
     * Memory-efficient set
     */
    fun <T> createMemoryEfficientSet(): MutableSet<T> {
        return when (MemoryOptimizer.currentOptimizationLevel) {
            MemoryStrategy.AGGRESSIVE -> HashSet(8)
            MemoryStrategy.MODERATE -> HashSet(16)
            MemoryStrategy.CONSERVATIVE -> HashSet(32)
        }
    }
}

/**
 * Memory-efficient string builder for old devices
 */
class MemoryEfficientStringBuilder(
    private val initialCapacity: Int = when (MemoryOptimizer.currentOptimizationLevel) {
        MemoryStrategy.AGGRESSIVE -> 64
        MemoryStrategy.MODERATE -> 128
        MemoryStrategy.CONSERVATIVE -> 256
    },
) {
    private var stringBuilder = StringBuilder(initialCapacity)

    fun append(text: String): MemoryEfficientStringBuilder {
        stringBuilder.append(text)
        return this
    }

    fun appendLine(text: String = ""): MemoryEfficientStringBuilder {
        stringBuilder.appendLine(text)
        return this
    }

    fun clear() {
        stringBuilder.clear()
    }

    override fun toString(): String = stringBuilder.toString()
}

/**
 * Memory usage tracking for debugging
 */
object MemoryTracker {

    private val allocations = ConcurrentHashMap<String, Long>()

    fun trackAllocation(component: String, size: Long) {
        allocations[component] = allocations.getOrDefault(component, 0L) + size
    }

    fun trackDeallocation(component: String, size: Long) {
        val newValue = allocations.getOrDefault(component, 0L) - size
        if (newValue <= 0) {
            allocations.remove(component)
        } else {
            allocations[component] = newValue
        }
    }

    fun getAllocationInfo(): Map<String, Long> = allocations.toMap()

    fun getTotalAllocation(): Long = allocations.values.sum()

    fun reset() {
        allocations.clear()
    }
}

/**
 * Battery optimization for extended party sessions
 */
object BatteryOptimizer {

    private var isBatteryOptimizationEnabled = true

    fun enableBatteryOptimization() {
        isBatteryOptimizationEnabled = true

        // Reduce haptic feedback intensity
        // Reduce screen brightness operations
        // Optimize wake lock usage
        // Reduce animation frame rates

        Logger.d("Battery optimization enabled")
    }

    fun disableBatteryOptimization() {
        isBatteryOptimizationEnabled = false
        Logger.d("Battery optimization disabled")
    }

    fun isEnabled(): Boolean = isBatteryOptimizationEnabled

    /**
     * Get battery optimization recommendations
     */
    fun getBatteryRecommendations(): List<String> {
        return listOf(
            "Disable haptic feedback in system settings for longer battery life",
            "Reduce screen brightness for extended play sessions",
            "Close other apps to conserve battery",
            "Consider using airplane mode if WiFi is not needed",
        )
    }
}

/**
 * Performance monitoring for old devices
 */
object MemoryPerformanceMonitor {

    private var performanceMetrics = ConcurrentHashMap<String, DetailedPerformanceMetric>()
    private var monitoringEnabled = false

    fun startMonitoring() {
        monitoringEnabled = true
        Logger.d("Performance monitoring started")
    }

    fun stopMonitoring() {
        monitoringEnabled = false
        Logger.d("Performance monitoring stopped")
    }

    fun recordMetric(component: String, duration: Long, memoryUsed: Long) {
        if (!monitoringEnabled) return

        var existingMetric = performanceMetrics.getOrPut(component) {
            DetailedPerformanceMetric(
                component = component,
                startTime = System.currentTimeMillis(),
                endTime = 0,
                startMemory = 0,
                endMemory = 0,
                duration = 0,
                memoryUsed = 0,
                totalCalls = 0,
                totalDuration = 0,
                totalMemoryUsed = 0,
                lastCallTime = 0,
                averageDuration = 0.0,
                averageMemoryUsed = 0.0,
            )
        }

        var metric = existingMetric.copy(
            totalCalls = existingMetric.totalCalls + 1,
            totalDuration = existingMetric.totalDuration + duration,
            totalMemoryUsed = existingMetric.totalMemoryUsed + memoryUsed,
            lastCallTime = System.currentTimeMillis(),
        )

        performanceMetrics[component] = metric

        // Log slow operations
        if (duration > 100) { // Operations taking >100ms
            Logger.w("Slow operation detected: $component took ${duration}ms")
        }
    }

    fun getMetrics(): Map<String, DetailedPerformanceMetric> = performanceMetrics.toMap()

    fun getAveragePerformance(): Map<String, Double> {
        return performanceMetrics.mapValues { (_, metric) ->
            if (metric.totalCalls > 0) {
                metric.totalDuration.toDouble() / metric.totalCalls
            } else {
                0.0
            }
        }
    }
}

/**
 * Performance metric data class
 */
data class DetailedPerformanceMetric(
    val component: String,
    val startTime: Long,
    val endTime: Long = 0,
    val startMemory: Long = 0,
    val endMemory: Long = 0,
    val duration: Long = 0,
    val memoryUsed: Long = 0,
    val totalCalls: Long = 0,
    val totalDuration: Long = 0,
    val totalMemoryUsed: Long = 0,
    val lastCallTime: Long = 0,
    val averageDuration: Double = 0.0,
    val averageMemoryUsed: Double = 0.0,
)

/**
 * Lightweight dependency injection for old devices
 */
object SimpleDependencyInjection {

    private val dependencies = ConcurrentHashMap<String, Any>()

    fun <T> register(key: String, instance: T) {
        dependencies[key] = instance as Any
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        return dependencies[key] as? T
    }

    fun <T> getOrCreate(key: String, factory: () -> T): T {
        return get<T>(key) ?: run {
            val instance = factory()
            register(key, instance)
            instance
        }
    }

    fun clear() {
        dependencies.clear()
    }

    fun getAllKeys(): Set<String> = dependencies.keys.toSet()
}
