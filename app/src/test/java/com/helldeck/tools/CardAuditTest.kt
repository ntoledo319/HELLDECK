@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "TYPE_MISMATCH_WARNING")
package com.helldeck.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.engine.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import org.robolectric.annotation.Config as RoboConfig

@RunWith(AndroidJUnit4::class)
@RoboConfig(sdk = [33])
class CardAuditTest {

    @Serializable
    data class AuditRow(
        val i: Int,
        val game: String,
        val text: String,
        val options: String,
        val generator_v3: Boolean,
        val pairScore: Double? = null,
        val slotsCount: Int = 0,
        val wordCount: Int = 0,
        val repeatRatio: Double = 0.0,
        val short: Boolean = false,
        val long: Boolean = false,
        val abEqual: Boolean = false,
        val hasPlaceholders: Boolean = false,
        val blueprintId: String = "",
        val slots: Map<String, String> = emptyMap(),
        val features: List<String> = emptyList(),
        // Added: spice + humor metrics + generation time
        val spice: Int = 0,
        val humorScore: Double? = null,
        val absurdity: Double? = null,
        val shockValue: Double? = null,
        val relatability: Double? = null,
        val cringeFactor: Double? = null,
        val benignViolation: Double? = null,
        val genMs: Long = 0L,
    )

    @Serializable
    data class BlueprintStats(
        val blueprintId: String,
        val totalGenerated: Int,
        val passed: Int,
        val failed: Int,
        val passRate: Double,
        val avgPairScore: Double,
        val avgWordCount: Double,
    )

    @Serializable
    data class AuditSummary(
        val game: String,
        val totalCards: Int,
        val passedCards: Int,
        val failedCards: Int,
        val passRate: Double,
        val blueprintStats: List<BlueprintStats>,
        val topFailureReasons: Map<String, Int>,
        val worstSamples: List<AuditRow>,
        // Added: performance + spice distribution
        val avgGenMs: Double,
        val p95GenMs: Double,
        val pctSpiceGte3: Double,
    )

    @Serializable
    data class AuditReport(
        val summary: AuditSummary,
        val rows: List<AuditRow>,
    )

    @Test
    fun runAudit() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        // Flags from system properties (defaults provided)
        val game = System.getProperty("game", "ROAST_CONSENSUS")
        val count = System.getProperty("count", "50").toIntOrNull()?.coerceIn(1, 2000) ?: 50
        val seed = System.getProperty("seed", "12345").toLongOrNull() ?: 12345L
        val spice = System.getProperty("spice", "2").toIntOrNull()?.coerceIn(0, 5) ?: 2

        // Force generator V3 for audit and allow non-gold generation
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)

        val rows = mutableListOf<AuditRow>()
        // Run synchronously in a simple loop (engine.next is suspend → use runBlocking)
        kotlinx.coroutines.runBlocking {
            // FIX: Create a single engine instance to enable anti-repetition tracking across all cards
            val engine = ContentEngineProvider.get(ctx, seed)
            repeat(count) { idx ->
                val req = GameEngine.Request(
                    sessionId = "audit_$seed", // Use same sessionId for all cards to track duplicates
                    gameId = game,
                    players = listOf("Jay", "Pip", "Mo"),
                    spiceMax = spice,
                )
                try {
                    val t0 = System.nanoTime()
                    val r = engine.next(req)
                    val t1 = System.nanoTime()
                    val meta = r.filledCard.metadata
                    val slots = (meta["slots"] as? Map<*, *>)
                        ?.mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { value -> key to value } } }
                        ?.toMap().orEmpty()
                    val features = (meta["features"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                    val pairScore = (meta["pairScore"] as? Number)?.toDouble()
                    val words = r.filledCard.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    val wc = words.size
                    val counts = words.groupingBy { it.lowercase() }.eachCount()
                    val top = counts.values.maxOrNull() ?: 0
                    val repeatRatio = if (wc == 0) 0.0 else top.toDouble() / wc
                    val abMatch = Regex("AB\\(optionA=([^,]+), optionB=([^\\)]+)\\)").find(r.options.toString())
                    val abEqual = abMatch?.let { m ->
                        val a = m.groupValues.getOrNull(1)?.trim()
                        val b = m.groupValues.getOrNull(2)?.trim()
                        a != null && b != null && a.equals(b, ignoreCase = true)
                    } ?: false

                    val hasPlaceholders = r.filledCard.text.contains('{') || r.filledCard.text.contains('}')
                    val blueprintId = r.filledCard.id

                    val row = AuditRow(
                        i = idx,
                        game = game,
                        text = r.filledCard.text,
                        options = r.options.toString(),
                        generator_v3 = features.isNotEmpty() || pairScore != null,
                        pairScore = pairScore,
                        slotsCount = slots.size,
                        wordCount = wc,
                        repeatRatio = repeatRatio,
                        short = wc < 5,
                        long = wc > 32,
                        abEqual = abEqual,
                        hasPlaceholders = hasPlaceholders,
                        blueprintId = blueprintId,
                        slots = slots,
                        features = features,
                        spice = r.filledCard.spice,
                        humorScore = (meta["humorScore"] as? Number)?.toDouble(),
                        absurdity = (meta["absurdity"] as? Number)?.toDouble(),
                        shockValue = (meta["shockValue"] as? Number)?.toDouble(),
                        relatability = (meta["relatability"] as? Number)?.toDouble(),
                        cringeFactor = (meta["cringeFactor"] as? Number)?.toDouble(),
                        benignViolation = (meta["benignViolation"] as? Number)?.toDouble(),
                        genMs = ((t1 - t0) / 1_000_000),
                    )
                    rows += row
                } catch (e: Exception) {
                    // Record exception as a failed row so audits never abort
                    val msg = e.message?.take(300) ?: e::class.simpleName ?: "Exception"
                    rows += AuditRow(
                        i = idx,
                        game = game,
                        text = "EXCEPTION: $msg",
                        options = "",
                        generator_v3 = false,
                        pairScore = null,
                        slotsCount = 0,
                        wordCount = 0,
                        repeatRatio = 0.0,
                        short = false,
                        long = false,
                        abEqual = false,
                        hasPlaceholders = false,
                        blueprintId = "exception",
                        slots = emptyMap(),
                        features = emptyList(),
                        spice = spice,
                        humorScore = null,
                        absurdity = null,
                        shockValue = null,
                        relatability = null,
                        cringeFactor = null,
                        benignViolation = null,
                        genMs = 0,
                    )
                }
            }
        }

        // Calculate blueprint statistics
        val blueprintGroups = rows.groupBy { it.blueprintId }
        val blueprintStats = blueprintGroups.map { (bpId, bpRows) ->
            val passed = bpRows.count { !it.hasPlaceholders && !it.abEqual && !it.short && !it.long && it.wordCount in 5..32 }
            val failed = bpRows.size - passed
            BlueprintStats(
                blueprintId = bpId,
                totalGenerated = bpRows.size,
                passed = passed,
                failed = failed,
                passRate = if (bpRows.isNotEmpty()) passed * 100.0 / bpRows.size else 0.0,
                avgPairScore = bpRows.mapNotNull { it.pairScore }.average().takeIf { !it.isNaN() } ?: 0.0,
                avgWordCount = bpRows.map { it.wordCount.toDouble() }.average(),
            )
        }.sortedByDescending { it.totalGenerated }

        // Analyze failure reasons
        val failureReasons = mutableMapOf<String, Int>()
        rows.forEach { row ->
            if (row.hasPlaceholders) failureReasons["placeholders"] = (failureReasons["placeholders"] ?: 0) + 1
            if (row.abEqual) failureReasons["ab_equal"] = (failureReasons["ab_equal"] ?: 0) + 1
            if (row.short) failureReasons["too_short"] = (failureReasons["too_short"] ?: 0) + 1
            if (row.long) failureReasons["too_long"] = (failureReasons["too_long"] ?: 0) + 1
            if (row.repeatRatio > 0.35) failureReasons["high_repetition"] = (failureReasons["high_repetition"] ?: 0) + 1
            if ((row.pairScore ?: 0.0) < 0.0) failureReasons["negative_pair_score"] = (failureReasons["negative_pair_score"] ?: 0) + 1
        }
        val topFailures = failureReasons.entries.sortedByDescending { it.value }.take(
            10,
        ).associate { it.key to it.value }

        // Get worst 20 samples (most issues)
        val worstSamples = rows
            .map { row ->
                val issueCount = listOf(
                    row.hasPlaceholders,
                    row.abEqual,
                    row.short,
                    row.long,
                    row.repeatRatio > 0.35,
                    (row.pairScore ?: 0.0) < 0.0,
                ).count { it }
                row to issueCount
            }
            .sortedByDescending { it.second }
            .take(20)
            .map { it.first }

        // Create summary
        val totalPassed = rows.count { !it.hasPlaceholders && !it.abEqual && !it.short && !it.long && it.wordCount in 5..32 }
        val genTimes = rows.map { it.genMs.toDouble() }.sorted()
        val avgGen = genTimes.average()
        val p95Idx = if (genTimes.isNotEmpty()) kotlin.math.floor(0.95 * (genTimes.size - 1)).toInt() else 0
        val p95 = genTimes.getOrElse(p95Idx) { 0.0 }
        val pctSpicy = if (rows.isNotEmpty()) rows.count { (it.spice) >= 3 } * 100.0 / rows.size else 0.0
        val summary = AuditSummary(
            game = game,
            totalCards = count,
            passedCards = totalPassed,
            failedCards = count - totalPassed,
            passRate = if (count > 0) totalPassed * 100.0 / count else 0.0,
            blueprintStats = blueprintStats,
            topFailureReasons = topFailures,
            worstSamples = worstSamples,
            avgGenMs = avgGen,
            p95GenMs = p95,
            pctSpiceGte3 = pctSpicy,
        )

        // Write under the module's build directory so paths are stable regardless of working dir
        val outDir = File("build/reports/cardlab").apply { mkdirs() }

        // Write CSV (enhanced with blueprint ID)
        val csvHeader = "i,game,blueprintId,text,options,generator_v3,spice,humorScore,absurdity,shockValue,relatability,cringeFactor,benignViolation,pairScore,lowPair,slotsCount,wordCount,repeatRatio,short,long,abEqual,hasPlaceholders,genMs\n"
        val csvLines = rows.map { r ->
            val safeText = r.text.replace('"', '\'').take(400).replace("\n", " ")
            val safeOptions = r.options.replace('"', '\'').take(200)
            val safeBpId = r.blueprintId.replace('"', '\'')
            val lowPair = r.pairScore?.let { it < 0.0 } ?: false
            "${r.i},${r.game},\"${safeBpId}\",\"${safeText}\",\"${safeOptions}\",${r.generator_v3},${r.spice},${r.humorScore ?: ""},${r.absurdity ?: ""},${r.shockValue ?: ""},${r.relatability ?: ""},${r.cringeFactor ?: ""},${r.benignViolation ?: ""},${r.pairScore ?: ""},$lowPair,${r.slotsCount},${r.wordCount},${"%.2f".format(
                r.repeatRatio,
            )},${r.short},${r.long},${r.abEqual},${r.hasPlaceholders},${r.genMs}"
        }
        File(outDir, "audit_${game}_${seed}_$count.csv").writeText(csvHeader + csvLines.joinToString("\n"))

        // Write detailed JSON with summary and full rows
        val json = Json { prettyPrint = true }
        val fullReport = AuditReport(summary, rows)
        File(outDir, "audit_${game}_${seed}_$count.json").writeText(json.encodeToString(fullReport))

        // Write simple HTML report
        val htmlReport = buildHtmlReport(summary, game, seed, count)
        File(outDir, "audit_${game}_${seed}_$count.html").writeText(htmlReport)

        // Print summary to console
        println("\n═══════════════════════════════════════════════════════════")
        println("Card Audit Report: $game")
        println("═══════════════════════════════════════════════════════════")
        println("Total Cards: ${summary.totalCards}")
        println("Passed: ${summary.passedCards} (${String.format("%.1f", summary.passRate)}%)")
        println("Failed: ${summary.failedCards}")
        println(
            "Avg gen: ${String.format(
                "%.2f",
                summary.avgGenMs,
            )} ms | p95: ${String.format("%.2f", summary.p95GenMs)} ms",
        )
        println("Spice >=3: ${String.format("%.1f", summary.pctSpiceGte3)}%")
        println("\nBlueprint Performance:")
        summary.blueprintStats.take(5).forEach { bp ->
            println("  ${bp.blueprintId}: ${bp.passed}/${bp.totalGenerated} (${String.format("%.1f", bp.passRate)}%)")
        }
        println("\nTop Failure Reasons:")
        summary.topFailureReasons.entries.take(5).forEach { (reason, count) ->
            println("  $reason: $count")
        }
        println("═══════════════════════════════════════════════════════════\n")

        // No assertions; this is a utility runner producing a report file
    }

    private fun buildHtmlReport(summary: AuditSummary, game: String, seed: Long, count: Int): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <title>HELLDECK Card Audit: ${summary.game}</title>
    <style>
        body { font-family: -apple-system, system-ui, sans-serif; padding: 20px; background: #1a1a1a; color: #e0e0e0; }
        .header { background: #2d2d2d; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 20px; }
        .stat-card { background: #2d2d2d; padding: 15px; border-radius: 8px; border-left: 4px solid #4CAF50; }
        .stat-card.warn { border-left-color: #FF9800; }
        .stat-card.fail { border-left-color: #f44336; }
        .stat-label { font-size: 12px; color: #999; text-transform: uppercase; }
        .stat-value { font-size: 28px; font-weight: bold; margin-top: 5px; }
        table { width: 100%; border-collapse: collapse; background: #2d2d2d; border-radius: 8px; overflow: hidden; }
        th { background: #3d3d3d; padding: 12px; text-align: left; font-weight: 600; }
        td { padding: 10px 12px; border-top: 1px solid #3d3d3d; }
        tr:hover { background: #333; }
        .pass { color: #4CAF50; }
        .fail { color: #f44336; }
        .samples { margin-top: 20px; }
        .sample { background: #2d2d2d; padding: 15px; margin: 10px 0; border-radius: 8px; border-left: 4px solid #f44336; }
        .sample-text { font-style: italic; margin-top: 8px; color: #ccc; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🃏 HELLDECK Card Audit Report</h1>
        <p><strong>Game:</strong> ${summary.game} | <strong>Seed:</strong> $seed | <strong>Count:</strong> $count</p>
        <p><strong>Generated:</strong> ${java.time.LocalDateTime.now()}</p>
    </div>
    
    <div class="stats">
        <div class="stat-card ${if (summary.passRate >= 95.0) "" else if (summary.passRate >= 80.0) "warn" else "fail"}">
            <div class="stat-label">Pass Rate</div>
            <div class="stat-value">${String.format("%.1f", summary.passRate)}%</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Total Cards</div>
            <div class="stat-value">${summary.totalCards}</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Passed</div>
            <div class="stat-value" style="color: #4CAF50">${summary.passedCards}</div>
        </div>
        <div class="stat-card ${if (summary.failedCards > 0) "fail" else ""}">
            <div class="stat-label">Failed</div>
            <div class="stat-value" style="color: #f44336">${summary.failedCards}</div>
        </div>
    </div>
    
    <h2>📊 Blueprint Performance</h2>
    <table>
        <thead>
            <tr>
                <th>Blueprint ID</th>
                <th>Generated</th>
                <th>Passed</th>
                <th>Failed</th>
                <th>Pass Rate</th>
                <th>Avg Pair Score</th>
                <th>Avg Words</th>
            </tr>
        </thead>
        <tbody>
            ${summary.blueprintStats.joinToString("") { bp ->
            """<tr>
                    <td><code>${bp.blueprintId}</code></td>
                    <td>${bp.totalGenerated}</td>
                    <td class="pass">${bp.passed}</td>
                    <td class="fail">${bp.failed}</td>
                    <td><strong>${String.format("%.1f", bp.passRate)}%</strong></td>
                    <td>${String.format("%.2f", bp.avgPairScore)}</td>
                    <td>${String.format("%.1f", bp.avgWordCount)}</td>
                </tr>"""
        }}
        </tbody>
    </table>
    
    <h2>⚠️ Top Failure Reasons</h2>
    <table>
        <thead>
            <tr><th>Reason</th><th>Count</th><th>Percentage</th></tr>
        </thead>
        <tbody>
            ${summary.topFailureReasons.entries.joinToString("") { (reason, count) ->
            val pct = if (summary.totalCards > 0) count * 100.0 / summary.totalCards else 0.0
            """<tr>
                    <td><code>${reason.replace("_", " ").uppercase()}</code></td>
                    <td class="fail"><strong>$count</strong></td>
                    <td>${String.format("%.1f", pct)}%</td>
                </tr>"""
        }}
        </tbody>
    </table>
    
    <div class="samples">
        <h2>🔍 Worst 20 Samples</h2>
        ${summary.worstSamples.take(20).joinToString("") { sample ->
            """<div class="sample">
                <strong>Blueprint:</strong> <code>${sample.blueprintId}</code> |
                <strong>Word Count:</strong> ${sample.wordCount} |
                <strong>Repeat Ratio:</strong> ${String.format("%.2f", sample.repeatRatio)} |
                <strong>Pair Score:</strong> ${sample.pairScore?.let { String.format("%.2f", it) } ?: "N/A"}
                <div class="sample-text">"${sample.text.replace("<", "&lt;").replace(">", "&gt;")}"</div>
            </div>"""
        }}
    </div>
    
    <hr style="margin: 40px 0; border: none; border-top: 1px solid #3d3d3d;">
    <p style="text-align: center; color: #666; font-size: 12px;">
        Generated by HELLDECK Card Audit Tool<br>
        <a href="audit_${game}_${seed}_$count.csv" style="color: #4CAF50; margin: 0 10px;">Download CSV</a>
        <a href="audit_${game}_${seed}_$count.json" style="color: #4CAF50; margin: 0 10px;">Download JSON</a>
    </p>
</body>
</html>
        """.trimIndent()
    }
}
