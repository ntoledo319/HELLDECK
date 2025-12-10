package com.helldeck.content.generator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.Config
import com.helldeck.engine.GameIds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as RoboConfig

@RunWith(AndroidJUnit4::class)
@RoboConfig(sdk = [33])
class GeneratorV3InvariantsTest {

    private fun engineFor(seed: Long): com.helldeck.content.engine.GameEngine {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        return ContentEngineProvider.get(ctx, seed)
    }

    private val games = listOf(
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

    @Test
    fun abOptionsAreDistinct() = kotlinx.coroutines.runBlocking {
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)
        val abGames = listOf(GameIds.POISON_PITCH, GameIds.MAJORITY, GameIds.RED_FLAG, GameIds.TEXT_TRAP)
        var checks = 0
        abGames.forEach { gameId ->
            repeat(30) { idx ->
                val engine = engineFor(1_000L + idx + gameId.hashCode())
                val result = engine.next(GameEngine.Request("ab_${gameId}_$idx", gameId, listOf("A","B","C"), spiceMax = 2))
                val options = result.options
                if (options is GameOptions.AB) {
                    checks++
                    assertFalse(options.optionA.isBlank())
                    assertFalse(options.optionB.isBlank())
                    assertNotEquals(options.optionA.lowercase(), options.optionB.lowercase())
                }
            }
        }
        assertTrue("Expected to evaluate distinct AB options", checks > 0)
    }

    @Test
    fun generatedTextHasNoPlaceholders() = kotlinx.coroutines.runBlocking {
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)
        games.forEach { gameId ->
            repeat(15) { idx ->
                val engine = engineFor(2_000L + idx + gameId.hashCode())
                val result = engine.next(GameEngine.Request("txt_${gameId}_$idx", gameId, listOf("A","B","C"), spiceMax = 2))
                val text = result.filledCard.text
                assertTrue("Generated text should be non-empty", text.isNotBlank())
                assertFalse("Text should not contain placeholders", text.contains('{') || text.contains('}'))
                val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
                assertTrue("Word count should be within reasonable bounds", words.size in 4..36)
            }
        }
    }

    @Test
    fun repetitionRatioRemainsLow() = kotlinx.coroutines.runBlocking {
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)
        games.forEach { gameId ->
            repeat(10) { idx ->
                val engine = engineFor(3_000L + idx + gameId.hashCode())
                val result = engine.next(GameEngine.Request("rep_${gameId}_$idx", gameId, listOf("A","B","C"), spiceMax = 2))
                val words = result.filledCard.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                val counts = words.groupingBy { it.lowercase() }.eachCount()
                val maxCount = counts.values.maxOrNull() ?: 0
                val ratio = if (words.isNotEmpty()) maxCount.toDouble() / words.size else 0.0
                assertTrue("Repetition ratio should be ≤ 0.35 for $gameId", ratio <= 0.35 + 1e-6)
            }
        }
    }

    @Test
    fun tabooAndScatterOptionsContainData() = kotlinx.coroutines.runBlocking {
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)

        repeat(20) { idx ->
            val taboo = engineFor(4_000L + idx).next(
                GameEngine.Request("tab_$idx", GameIds.TABOO, listOf("A","B","C"), spiceMax = 2)
            ).options
            if (taboo is GameOptions.Taboo) {
                assertFalse(taboo.word.isBlank())
                assertTrue(taboo.forbidden.size == 3)
                assertTrue(taboo.forbidden.all { it.isNotBlank() })
            }

            val scatter = engineFor(4_500L + idx).next(
                GameEngine.Request("sc_$idx", GameIds.SCATTER, listOf("A","B","C"), spiceMax = 2)
            ).options
            if (scatter is GameOptions.Scatter) {
                assertFalse(scatter.category.isBlank())
                assertTrue(scatter.letter.length == 1)
            }
        }
    }
}
