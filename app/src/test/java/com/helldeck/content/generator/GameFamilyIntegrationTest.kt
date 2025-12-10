package com.helldeck.content.generator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.Config
import com.helldeck.engine.GameIds
import com.helldeck.engine.GameMetadata
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as RoboConfig

@RunWith(AndroidJUnit4::class)
@RoboConfig(sdk = [33])
class GameFamilyIntegrationTest {

    private data class Expectation(
        val gameId: String,
        val expectedOptionType: KClass<out GameOptions>
    )

    private val expectations = listOf(
        Expectation(GameIds.ROAST_CONS, GameOptions.PlayerVote::class),
        Expectation(GameIds.POISON_PITCH, GameOptions.AB::class),
        Expectation(GameIds.MAJORITY, GameOptions.AB::class),
        Expectation(GameIds.RED_FLAG, GameOptions.AB::class),
        Expectation(GameIds.TEXT_TRAP, GameOptions.AB::class),
        Expectation(GameIds.ODD_ONE, GameOptions.OddOneOut::class),
        Expectation(GameIds.TITLE_FIGHT, GameOptions.Challenge::class),
        Expectation(GameIds.ALIBI, GameOptions.HiddenWords::class),
        Expectation(GameIds.HYPE_YIKE, GameOptions.Product::class),
        Expectation(GameIds.TABOO, GameOptions.Taboo::class),
        Expectation(GameIds.SCATTER, GameOptions.Scatter::class),
        Expectation(GameIds.HOTSEAT_IMP, GameOptions.Challenge::class)
    )

    private fun engineFor(seed: Long): com.helldeck.content.engine.GameEngine {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        return ContentEngineProvider.get(ctx, seed)
    }

    @Test
    fun generatorProducesSaneResultsAcrossGames() = kotlinx.coroutines.runBlocking {
        Config.setSafeModeGoldOnly(false)
        Config.setEnableV3Generator(true)

        expectations.forEach { expectation ->
            repeat(20) { idx ->
                val seed = 20_000L + expectation.gameId.hashCode() + idx
                val engine = engineFor(seed)
                val result = engine.next(
                    GameEngine.Request(
                        sessionId = "int_${expectation.gameId}_$idx",
                        gameId = expectation.gameId,
                        players = listOf("A", "B", "C"),
                        spiceMax = 2
                    )
                )

                val metadata = GameMetadata.getGameMetadata(expectation.gameId)
                assertNotNull("Metadata available for ${expectation.gameId}", metadata)
                if (metadata != null) {
                    assertEquals("Timer should match metadata", metadata.timerSec, result.timer)
                    assertEquals("Interaction type should match metadata", metadata.interactionType, result.interactionType)
                }

                val card = result.filledCard
                assertEquals("Returned card game should match request", expectation.gameId, card.game)
                assertTrue("Card text should be non-blank", card.text.isNotBlank())
                assertFalse("Card text should not contain placeholders", card.text.contains('{') || card.text.contains('}'))
                val words = card.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                assertTrue("Word count should be reasonable", words.size in 4..36)

                val options = result.options
                val optionMatches = expectation.expectedOptionType.isInstance(options) ||
                    options is GameOptions.PlayerVote ||
                    options is GameOptions.None
                assertTrue(
                    "Unexpected options type for ${expectation.gameId}: ${options::class.simpleName}",
                    optionMatches
                )

                when (options) {
                    is GameOptions.AB -> {
                        assertTrue(options.optionA.isNotBlank())
                        assertTrue(options.optionB.isNotBlank())
                        assertFalse(options.optionA.equals(options.optionB, ignoreCase = true))
                    }
                    is GameOptions.Taboo -> {
                        assertTrue(options.word.isNotBlank())
                        assertEquals(3, options.forbidden.size)
                        assertTrue(options.forbidden.all { it.isNotBlank() })
                    }
                    is GameOptions.HiddenWords -> {
                        assertTrue(options.words.isNotEmpty())
                        assertTrue(options.words.all { it.isNotBlank() })
                    }
                    is GameOptions.Scatter -> {
                        assertTrue(options.category.isNotBlank())
                        assertTrue(options.letter.length == 1)
                    }
                    is GameOptions.Product -> {
                        assertTrue(options.product.isNotBlank())
                    }
                    is GameOptions.PlayerVote -> {
                        assertTrue(options.players.isNotEmpty())
                    }
                    is GameOptions.Challenge -> {
                        assertTrue(options.challenge.isNotBlank())
                    }
                    else -> {
                        // Other options types not used in these expectations
                    }
                }
            }
        }
    }
}
