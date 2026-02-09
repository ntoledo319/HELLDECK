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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass
import org.robolectric.annotation.Config as RoboConfig

@RunWith(AndroidJUnit4::class)
@RoboConfig(sdk = [33])
class GameFamilyIntegrationTest {

    private data class Expectation(
        val gameId: String,
        val expectedOptionType: KClass<out GameOptions>,
    )

    // Test expectations for all 14 official games from HDRealRules.md
    private val expectations = listOf(
        Expectation(GameIds.ROAST_CONS, GameOptions.SeatVote::class),
        Expectation(GameIds.CONFESS_CAP, GameOptions.TrueFalse::class),
        Expectation(GameIds.POISON_PITCH, GameOptions.AB::class),
        Expectation(GameIds.FILLIN, GameOptions.Challenge::class),
        Expectation(GameIds.RED_FLAG, GameOptions.AB::class),
        Expectation(GameIds.HOTSEAT_IMP, GameOptions.Challenge::class),
        Expectation(GameIds.TEXT_TRAP, GameOptions.ReplyTone::class),
        Expectation(GameIds.TABOO, GameOptions.Taboo::class),
        Expectation(GameIds.UNIFYING_THEORY, GameOptions.OddOneOut::class),
        Expectation(GameIds.TITLE_FIGHT, GameOptions.Challenge::class),
        Expectation(GameIds.ALIBI, GameOptions.HiddenWords::class),
        Expectation(GameIds.REALITY_CHECK, GameOptions.SeatSelect::class),
        Expectation(GameIds.SCATTER, GameOptions.Scatter::class),
        Expectation(GameIds.OVER_UNDER, GameOptions.AB::class),
        // Legacy games removed: MAJORITY, ODD_ONE, HYPE_YIKE
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
                        spiceMax = 2,
                    ),
                )

                val metadata = GameMetadata.getGameMetadata(expectation.gameId)
                assertNotNull("Metadata available for ${expectation.gameId}", metadata)
                if (metadata != null) {
                    assertEquals("Timer should match metadata", metadata.timerSec, result.timer)
                    assertEquals(
                        "Interaction type should match metadata",
                        metadata.interactionType,
                        result.interactionType,
                    )
                }

                val card = result.filledCard
                assertEquals("Returned card game should match request", expectation.gameId, card.game)
                assertTrue("Card text should be non-blank", card.text.isNotBlank())
                assertFalse(
                    "Card text should not contain placeholders",
                    card.text.contains('{') || card.text.contains('}'),
                )
                val words = card.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                assertTrue("Word count should be reasonable", words.size in 2..60)

                val options = result.options
                val optionMatches = expectation.expectedOptionType.isInstance(options) ||
                    options is GameOptions.SeatVote ||
                    options is GameOptions.None
                assertTrue(
                    "Unexpected options type for ${expectation.gameId}: ${options::class.simpleName}",
                    optionMatches,
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
                    is GameOptions.ReplyTone -> {
                        assertTrue(options.tones.isNotEmpty())
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
                    is GameOptions.OddOneOut -> {
                        assertTrue(options.items.isNotEmpty())
                    }
                    is GameOptions.SeatVote -> {
                        assertTrue(options.seatNumbers.isNotEmpty())
                    }
                    is GameOptions.SeatSelect -> {
                        assertTrue(options.seatNumbers.isNotEmpty())
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
