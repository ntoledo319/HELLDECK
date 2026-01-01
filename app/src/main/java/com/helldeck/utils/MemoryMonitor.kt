package com.helldeck.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.helldeck.AppCtx

/**
 * Monitors memory pressure and provides warnings for low-memory conditions.
 * 
 * Helps prevent OOM crashes by tracking heap usage and triggering graceful degradation.
 * 
 * ## Concept: Memory Pressure Management
 * HELLDECK loads large assets (models, lexicons, gold cards). On low-end devices,
 * we need to detect pressure early and reduce quality gracefully rather than crash.
 * 
 * @context_boundary Memory state affects generator quality settings but not game logic
 */
object MemoryMonitor {
    
    enum class MemoryPressure {
        LOW,        // <50% heap used
        MODERATE,   // 50-70% heap used
        HIGH,       // 70-85% heap used
        CRITICAL    // >85% heap used
    }
    
    data class MemoryStats(
        val heapSizeBytes: Long,
        val heapAllocatedBytes: Long,
        val heapFreeBytes: Long,
        val nativeHeapBytes: Long,
        val pressureLevel: MemoryPressure,
        val percentUsed: Double,
        val availableMemoryMB: Long
    )
    
    /**
     * Get current memory statistics.
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val heapSize = runtime.maxMemory()
        val heapAllocated = runtime.totalMemory()
        val heapFree = runtime.freeMemory()
        val heapUsed = heapAllocated - heapFree
        val nativeHeap = Debug.getNativeHeapAllocatedSize()
        
        val percentUsed = (heapUsed.toDouble() / heapSize.toDouble()) * 100.0
        
        val pressure = when {
            percentUsed < 50.0 -> MemoryPressure.LOW
            percentUsed < 70.0 -> MemoryPressure.MODERATE
            percentUsed < 85.0 -> MemoryPressure.HIGH
            else -> MemoryPressure.CRITICAL
        }
        
        val availableMB = getAvailableMemoryMB()
        
        return MemoryStats(
            heapSizeBytes = heapSize,
            heapAllocatedBytes = heapAllocated,
            heapFreeBytes = heapFree,
            nativeHeapBytes = nativeHeap,
            pressureLevel = pressure,
            percentUsed = percentUsed,
            availableMemoryMB = availableMB
        )
    }
    
    /**
     * Get available system memory in MB.
     */
    private fun getAvailableMemoryMB(): Long {
        return try {
            val activityManager = AppCtx.ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            memInfo.availMem / (1024 * 1024)
        } catch (e: Exception) {
            Logger.e("Failed to get available memory", e)
            0L
        }
    }
    
    /**
     * Check if we're in a low memory situation that requires degradation.
     */
    fun shouldDegradeQuality(): Boolean {
        val stats = getMemoryStats()
        return stats.pressureLevel == MemoryPressure.HIGH || 
               stats.pressureLevel == MemoryPressure.CRITICAL ||
               stats.availableMemoryMB < 100 // Less than 100MB available
    }
    
    /**
     * Request garbage collection if pressure is high.
     * Note: This is a hint to the VM, not a guarantee.
     */
    fun requestGCIfNeeded() {
        val stats = getMemoryStats()
        if (stats.pressureLevel == MemoryPressure.HIGH || stats.pressureLevel == MemoryPressure.CRITICAL) {
            Logger.w("High memory pressure detected (${stats.percentUsed.toInt()}%), requesting GC")
            System.gc()
        }
    }
    
    /**
     * Log current memory status for debugging.
     */
    fun logMemoryStatus() {
        val stats = getMemoryStats()
        Logger.i("=== Memory Status ===")
        Logger.i("Heap Used: ${stats.heapAllocatedBytes / (1024 * 1024)}MB / ${stats.heapSizeBytes / (1024 * 1024)}MB")
        Logger.i("Heap Free: ${stats.heapFreeBytes / (1024 * 1024)}MB")
        Logger.i("Native Heap: ${stats.nativeHeapBytes / (1024 * 1024)}MB")
        Logger.i("Pressure: ${stats.pressureLevel} (${stats.percentUsed.toInt()}%)")
        Logger.i("Available System: ${stats.availableMemoryMB}MB")
        Logger.i("==================")
    }
    
    /**
     * Get recommended LLM quality settings based on memory pressure.
     */
    fun getRecommendedQualitySettings(): QualitySettings {
        val stats = getMemoryStats()
        
        return when (stats.pressureLevel) {
            MemoryPressure.LOW -> QualitySettings(
                enableLLM = true,
                llmMaxTokens = 150,
                goldCardBufferSize = 50,
                enableParaphrase = true,
                cacheSize = 10
            )
            MemoryPressure.MODERATE -> QualitySettings(
                enableLLM = true,
                llmMaxTokens = 120,
                goldCardBufferSize = 30,
                enableParaphrase = true,
                cacheSize = 5
            )
            MemoryPressure.HIGH -> QualitySettings(
                enableLLM = true,
                llmMaxTokens = 100,
                goldCardBufferSize = 20,
                enableParaphrase = false,
                cacheSize = 3
            )
            MemoryPressure.CRITICAL -> QualitySettings(
                enableLLM = false, // Disable LLM to save memory
                llmMaxTokens = 0,
                goldCardBufferSize = 10,
                enableParaphrase = false,
                cacheSize = 1
            )
        }
    }
    
    data class QualitySettings(
        val enableLLM: Boolean,
        val llmMaxTokens: Int,
        val goldCardBufferSize: Int,
        val enableParaphrase: Boolean,
        val cacheSize: Int
    )
}
