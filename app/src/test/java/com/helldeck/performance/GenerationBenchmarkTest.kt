package com.helldeck.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.engine.Config
import com.helldeck.engine.GameIds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as RoboConfig

/**
 * Micro-benchmarks for Generator V3 performance targeting Moto G24.
 * Goal: p95 ≤ 12ms per card generation.
 */
@RunWith(AndroidJUnit4::class)
@RoboConfig(sdk = [33])
class GenerationBenchmarkTest {

    private data class BenchmarkResult(
        val game: String,
        val count: Int,
        val timings: List<Long>,
        val min: Long,
        val p50: Long,
        val p95: Long,
        val p99: Long,
        val max: Long,
        val mean: Double
    )

    @Test
    fun benchmarkAllGames() = kotlinx.coroutines.runBlocking {
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)
        
        val games = listOf(
            GameIds.ROAST_CONS,
            GameIds.POISON_PITCH,
            GameIds.MAJORITY,
            GameIds.RED_FLAG,
            GameIds.TEXT_TRAP,
            GameIds.ODD_ONE,
            GameIds.TITLE_FIGHT,
            GameIds.ALIBI,
            GameIds.HYPE_YIKE,
            GameIds.TABOO,
            GameIds.SCATTER,
            GameIds.HOTSEAT_IMP
        )
        
        val results = mutableListOf<BenchmarkResult>()
        val iterations = 100
        
        games.forEach { gameId ->
            val timings = mutableListOf<Long>()
            val ctx: Context = ApplicationProvider.getApplicationContext()
            
            repeat(iterations) { i ->
                val engine = ContentEngineProvider.get(ctx, 5000L + i)
                val req = GameEngine.Request(
                    sessionId = "bench_${gameId}_$i",
                    gameId = gameId,
                    players = listOf("A", "B", "C"),
                    spiceMax = 2
                )
                
                val start = System.nanoTime()
                engine.next(req)
                val end = System.nanoTime()
                
                val durationMs = (end - start) / 1_000_000
                timings.add(durationMs)
            }
            
            timings.sort()
            val p50 = timings[timings.size / 2]
            val p95 = timings[(timings.size * 0.95).toInt().coerceAtMost(timings.lastIndex)]
            val p99 = timings[(timings.size * 0.99).toInt().coerceAtMost(timings.lastIndex)]
            val mean = timings.average()
            
            results.add(BenchmarkResult(
                game = gameId,
                count = iterations,
                timings = timings,
                min = timings.first(),
                p50 = p50,
                p95 = p95,
                p99 = p99,
                max = timings.last(),
                mean = mean
            ))
        }
        
        // Print results in table format
        println("\n═══════════════════════════════════════════════════════════")
        println("HELLDECK Generator V3 - Performance Benchmark Results")
        println("Target Device: Moto G24")
        println("Iterations per Game: $iterations")
        println("═══════════════════════════════════════════════════════════")
        println()
        println("Game                  | p50   | p95   | p99   | Mean  | Max   | Status")
        println("----------------------|-------|-------|-------|-------|-------|--------")
        
        results.forEach { r ->
            val status = if (r.p95 <= 12) "✅ PASS" else "⚠️ SLOW"
            val gameName = r.game.padEnd(20)
            println("$gameName| ${r.p50.toString().padStart(5)}ms| ${r.p95.toString().padStart(5)}ms| ${r.p99.toString().padStart(5)}ms| ${"%.1f".format(r.mean).padStart(5)}ms| ${r.max.toString().padStart(5)}ms| $status")
        }
        
        println()
        println("═══════════════════════════════════════════════════════════")
        
        // Overall summary
        val overallP95 = results.map { it.p95 }.sorted()[(results.size * 0.95).toInt().coerceAtMost(results.lastIndex)]
        val passCount = results.count { it.p95 <= 12 }
        val passRate = (passCount * 100.0) / results.size
        
        println("Overall Performance:")
        println("  p95 across all games: ${overallP95}ms")
        println("  Games passing target (p95 ≤ 12ms): $passCount/${results.size} (${"%.1f".format(passRate)}%)")
        if (overallP95 <= 12) {
            println("  ✅ TARGET MET: p95 ≤ 12ms")
        } else {
            println("  ⚠️ TARGET MISSED: p95 = ${overallP95}ms (target: 12ms)")
        }
        println("═══════════════════════════════════════════════════════════\n")
    }
    
    @Test
    fun benchmarkSingleGameDeep() = kotlinx.coroutines.runBlocking {
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)
        
        val gameId = GameIds.ROAST_CONS
        val iterations = 500
        val timings = mutableListOf<Long>()
        val ctx: Context = ApplicationProvider.getApplicationContext()
        
        repeat(iterations) { i ->
            val engine = ContentEngineProvider.get(ctx, 10000L + i)
            val req = GameEngine.Request(
                sessionId = "deep_${gameId}_$i",
                gameId = gameId,
                players = listOf("Jay", "Pip", "Mo"),
                spiceMax = 2
            )
            
            val start = System.nanoTime()
            engine.next(req)
            val end = System.nanoTime()
            
            timings.add((end - start) / 1_000_000)
        }
        
        timings.sort()
        println("\n═══ Deep Benchmark: $gameId ($iterations iterations) ═══")
        println("Min: ${timings.first()}ms")
        println("p10: ${timings[(timings.size * 0.10).toInt()]}ms")
        println("p25: ${timings[(timings.size * 0.25).toInt()]}ms")
        println("p50: ${timings[timings.size / 2]}ms")
        println("p75: ${timings[(timings.size * 0.75).toInt()]}ms")
        println("p90: ${timings[(timings.size * 0.90).toInt()]}ms")
        println("p95: ${timings[(timings.size * 0.95).toInt()]}ms")
        println("p99: ${timings[(timings.size * 0.99).toInt()]}ms")
        println("Max: ${timings.last()}ms")
        println("Mean: ${"%.2f".format(timings.average())}ms")
        println("StdDev: ${"%.2f".format(calculateStdDev(timings))}ms")
        println("═════════════════════════════════════════════════════\n")
    }
    
    private fun calculateStdDev(values: List<Long>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
