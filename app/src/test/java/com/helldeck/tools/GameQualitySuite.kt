package com.helldeck.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.validation.GameQualityProfiles
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as RoboConfig
import java.io.File

@RunWith(AndroidJUnit4::class)
@RoboConfig(sdk = [33])
class GameQualitySuite {

    @Serializable
    data class Row(
        val game: String,
        val i: Int,
        val text: String,
        val score01: Double,
        val pass: Boolean,
        val issues: List<String>,
        val metrics: Map<String, String>
    )

    @Serializable
    data class Summary(
        val game: String,
        val total: Int,
        val passed: Int,
        val passRate: Double,
        val topIssues: Map<String, Int>,
        val avgScore: Double
    )

    @Serializable
    data class Report(
        val summary: Summary,
        val rows: List<Row>
    )

    @Test
    fun runQualitySweep() {
        // Enable proxy LLM during JVM tests so AI humor/sense metrics populate without a device/emulator
        System.setProperty("HELDECK_LLM_MODE", "proxy")
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val gameIds = GameMetadata.getAllGameIds()

        val seedsProp = System.getProperty("seeds")
        val seed = System.getProperty("seed", "12345").toLongOrNull() ?: 12345L
        val seeds: List<Long> = seedsProp?.split(',')?.mapNotNull { it.trim().toLongOrNull() } ?: listOf(seed)
        val count = System.getProperty("count", "50").toIntOrNull()?.coerceIn(1, 2000) ?: 50
        val spice = System.getProperty("spice", "2").toIntOrNull()?.coerceIn(0, 5) ?: 2

        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)

        val outDir = File("app/build/reports/cardlab/quality").apply { mkdirs() }
        val jsonCodec = Json { prettyPrint = true }

        seeds.forEach { seedVal ->
            // One engine instance per seed to preserve session anti-repetition
            val engine = ContentEngineProvider.get(ctx, seedVal)
            gameIds.forEach { game ->
                val rows = mutableListOf<Row>()
                repeat(count) { idx ->
                    val req = GameEngine.Request(
                        sessionId = "quality_${game}_$seedVal",
                        gameId = game,
                        players = listOf("Jay", "Pip", "Mo"),
                        spiceMax = spice
                    )
                    val result = try {
                        kotlinx.coroutines.runBlocking { engine.next(req) }
                    } catch (e: Exception) {
                        rows += Row(
                            game = game,
                            i = idx,
                            text = "[FILL_ERROR] ${e.message?.replace('\n',' ')?.take(160) ?: "unknown"}",
                            score01 = 0.0,
                            pass = false,
                            issues = listOf("FILL_ERROR"),
                            metrics = emptyMap()
                        )
                        return@repeat
                    }
                    val eval = kotlinx.coroutines.runBlocking {
                        GameQualityProfiles.evaluate(
                            gameId = game,
                            interaction = result.interactionType,
                            card = result.filledCard,
                            options = result.options
                        )
                    }
                    val metrics = eval.metrics.mapValues { it.value?.toString() ?: "" }
                    rows += Row(
                        game = game,
                        i = idx,
                        text = result.filledCard.text.trim(),
                        score01 = eval.score01,
                        pass = eval.pass,
                        issues = eval.issues.map { it.name },
                        metrics = metrics
                    )
                }
                val passed = rows.count { it.pass }
                val passRate = if (rows.isNotEmpty()) passed * 100.0 / rows.size else 0.0
                val avgScore = if (rows.isNotEmpty()) rows.map { it.score01 }.average() else 0.0
                val topIssues = rows.flatMap { it.issues }.groupingBy { it }.eachCount().toList()
                    .sortedByDescending { it.second }.toMap()
                val summary = Summary(
                    game = game,
                    total = rows.size,
                    passed = passed,
                    passRate = passRate,
                    topIssues = topIssues,
                    avgScore = avgScore
                )
                val base = "quality_${game}_${seedVal}_${count}"
                File(outDir, "$base.json").writeText(jsonCodec.encodeToString(Report(summary, rows)))
                File(outDir, "$base.csv").printWriter().use { pw ->
                    pw.println("i,score01,pass,issues,wordCount,repeatRatio,pairScore,humorScore,aiHumor,aiSense,aiUnderstandable,text")
                    rows.forEach { r ->
                        val m = r.metrics
                        pw.println(listOf(
                            r.i,
                            String.format("%.3f", r.score01),
                            r.pass,
                            r.issues.joinToString("|"),
                            m["wordCount"] ?: "",
                            m["repeatRatio"] ?: "",
                            m["pairScore"] ?: "",
                            m["humorScore"] ?: "",
                            m["aiHumor"] ?: "",
                            m["aiSense"] ?: "",
                            m["aiUnderstandable"] ?: "",
                            r.text.replace("\n", " ").replace(",", ";")
                        ).joinToString(","))
                    }
                }
                File(outDir, "$base.html").writeText("""
                    <html><head><meta charset=\"utf-8\"><title>Quality $game</title>
                    <style>body{background:#121212;color:#eee;font-family:system-ui,sans-serif;padding:24px} .ok{color:#7bd88f} .bad{color:#ff6b6b} table{border-collapse:collapse;width:100%} td,th{border:1px solid #333;padding:6px}</style>
                    </head><body>
                    <h1>Quality Summary: $game</h1>
                    <p>Pass rate: ${String.format("%.1f", passRate)}% | Avg score: ${String.format("%.2f", avgScore)}</p>
                    <h3>Top issues</h3>
                    <ul>${topIssues.entries.joinToString("") { "<li><code>${it.key}</code>: ${it.value}</li>" }}</ul>
                    <h3>Samples</h3>
                    <table><thead><tr><th>#</th><th>Score</th><th>Pass</th><th>Issues</th><th>Text</th></tr></thead><tbody>
                    ${rows.take(50).joinToString("") { r ->
                        val cls = if (r.pass) "ok" else "bad"
                        "<tr><td>${r.i}</td><td>${String.format("%.2f", r.score01)}</td><td class=\"$cls\">${r.pass}</td><td>${r.issues.joinToString(" ")}</td><td>${r.text.replace("<","&lt;")}</td></tr>"
                    }}
                    </tbody></table>
                    </body></html>
                """.trimIndent())
            }
        }
    }
}
