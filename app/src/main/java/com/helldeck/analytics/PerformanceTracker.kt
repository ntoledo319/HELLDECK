package com.helldeck.analytics

import android.os.SystemClock
import com.helldeck.utils.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks performance metrics for card generation and game operations.
 * 
 * Provides detailed metrics on:
 * - LLM generation times and success rates
 * - Fallback usage patterns
 * - Memory allocation during generation
 * - UI rendering performance
 * 
 * @ai_prompt Use `perfTracker.startGeneration()` before card generation,
 * then `perfTracker.recordGeneration()` with the result to track metrics.
 * 
 * @context_boundary Metrics do not affect game logic - purely observability
 */
object PerformanceTracker {
    
    // Generation method tracking
    enum class GenerationMethod {
        LLM_V2,
        TEMPLATE_V3,
        GOLD_CARD,
        FALLBACK
    }
    
    // Metrics storage
    private val generationCounts = ConcurrentHashMap<GenerationMethod, AtomicLong>()
    private val generationTimes = ConcurrentHashMap<GenerationMethod, MutableList<Long>>()
    private val gameGenerationCounts = ConcurrentHashMap<String, AtomicLong>()
    private val failureReasons = ConcurrentHashMap<String, AtomicLong>()
    
    // Active operation tracking
    private val activeGenerations = ConcurrentHashMap<String, Long>()
    
    init {
        GenerationMethod.values().forEach { method ->
            generationCounts[method] = AtomicLong(0)
            generationTimes[method] = mutableListOf()
        }
    }
    
    /**
     * Start tracking a generation operation.
     * Returns an operation ID for later recording.
     */
    fun startGeneration(gameId: String): String {
        val opId = "${gameId}_${System.nanoTime()}"
        activeGenerations[opId] = SystemClock.elapsedRealtimeNanos()
        return opId
    }
    
    /**
     * Record a completed generation operation.
     * 
     * @param opId Operation ID from startGeneration()
     * @param method Which generation method was used
     * @param gameId The game that was generated for
     * @param success Whether generation succeeded
     * @param failureReason If failed, the reason why
     */
    fun recordGeneration(
        opId: String,
        method: GenerationMethod,
        gameId: String,
        success: Boolean = true,
        failureReason: String? = null
    ) {
        val startTime = activeGenerations.remove(opId) ?: return
        val durationNs = SystemClock.elapsedRealtimeNanos() - startTime
        val durationMs = durationNs / 1_000_000
        
        // Record method-specific metrics
        generationCounts[method]?.incrementAndGet()
        synchronized(generationTimes[method] ?: mutableListOf<Long>()) {
            generationTimes[method]?.add(durationMs)
            // Keep only last 100 samples to avoid memory growth
            if (generationTimes[method]?.size ?: 0 > 100) {
                generationTimes[method]?.removeAt(0)
            }
        }
        
        // Record per-game metrics
        gameGenerationCounts.computeIfAbsent(gameId) { AtomicLong(0) }.incrementAndGet()
        
        // Record failure reasons
        if (!success && failureReason != null) {
            failureReasons.computeIfAbsent(failureReason) { AtomicLong(0) }.incrementAndGet()
        }
        
        // Log slow generations (>5s)
        if (durationMs > 5000) {
            Logger.w("Slow generation detected: ${durationMs}ms using $method for $gameId")
        }
    }
    
    /**
     * Get success rate for a specific generation method.
     */
    fun getSuccessRate(method: GenerationMethod): Double {
        val count = generationCounts[method]?.get() ?: 0
        if (count == 0L) return 0.0
        
        val total = generationCounts.values.sumOf { it.get() }
        return count.toDouble() / total.toDouble()
    }
    
    /**
     * Get average generation time for a method.
     */
    fun getAverageTime(method: GenerationMethod): Long {
        val times = synchronized(generationTimes[method] ?: mutableListOf<Long>()) {
            generationTimes[method]?.toList() ?: emptyList()
        }
        return if (times.isEmpty()) 0L else times.average().toLong()
    }

    /**
     * Get p95 generation time for a method.
     */
    fun getP95Time(method: GenerationMethod): Long {
        val times = synchronized(generationTimes[method] ?: mutableListOf<Long>()) {
            generationTimes[method]?.toList() ?: emptyList()
        }
        if (times.isEmpty()) return 0L
        
        val sorted = times.sorted()
        val index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
        return sorted[index]
    }
    
    /**
     * Get metrics summary for debugging/dashboard.
     */
    fun getMetricsSummary(): Map<String, Any> {
        return mapOf(
            "llm_v2" to mapOf(
                "count" to (generationCounts[GenerationMethod.LLM_V2]?.get() ?: 0),
                "avg_ms" to getAverageTime(GenerationMethod.LLM_V2),
                "p95_ms" to getP95Time(GenerationMethod.LLM_V2),
                "success_rate" to getSuccessRate(GenerationMethod.LLM_V2)
            ),
            "template_v3" to mapOf(
                "count" to (generationCounts[GenerationMethod.TEMPLATE_V3]?.get() ?: 0),
                "avg_ms" to getAverageTime(GenerationMethod.TEMPLATE_V3),
                "p95_ms" to getP95Time(GenerationMethod.TEMPLATE_V3),
                "success_rate" to getSuccessRate(GenerationMethod.TEMPLATE_V3)
            ),
            "gold_cards" to mapOf(
                "count" to (generationCounts[GenerationMethod.GOLD_CARD]?.get() ?: 0),
                "avg_ms" to getAverageTime(GenerationMethod.GOLD_CARD),
                "p95_ms" to getP95Time(GenerationMethod.GOLD_CARD),
                "success_rate" to getSuccessRate(GenerationMethod.GOLD_CARD)
            ),
            "fallback" to mapOf(
                "count" to (generationCounts[GenerationMethod.FALLBACK]?.get() ?: 0),
                "avg_ms" to getAverageTime(GenerationMethod.FALLBACK),
                "p95_ms" to getP95Time(GenerationMethod.FALLBACK),
                "success_rate" to getSuccessRate(GenerationMethod.FALLBACK)
            ),
            "active_operations" to activeGenerations.size,
            "top_failures" to failureReasons.entries
                .sortedByDescending { it.value.get() }
                .take(5)
                .associate { it.key to it.value.get() },
            "per_game" to gameGenerationCounts.entries
                .associate { it.key to it.value.get() }
        )
    }
    
    /**
     * Reset all metrics (useful for testing or new session).
     */
    fun reset() {
        generationCounts.values.forEach { it.set(0) }
        generationTimes.values.forEach { synchronized(it) { it.clear() } }
        gameGenerationCounts.clear()
        failureReasons.clear()
        activeGenerations.clear()
    }
    
    /**
     * Log current metrics to console for debugging.
     */
    fun logMetrics() {
        val summary = getMetricsSummary()
        Logger.i("=== Performance Metrics ===")
        Logger.i("LLM V2: ${summary["llm_v2"]}")
        Logger.i("Template V3: ${summary["template_v3"]}")
        Logger.i("Gold Cards: ${summary["gold_cards"]}")
        Logger.i("Fallback: ${summary["fallback"]}")
        Logger.i("Active Operations: ${summary["active_operations"]}")
        Logger.i("Top Failures: ${summary["top_failures"]}")
        Logger.i("========================")
    }
}
