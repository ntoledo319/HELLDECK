package com.helldeck.content.generator

import android.content.Context
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.validation.GameContractValidator
import com.helldeck.engine.GameMetadata
import com.helldeck.utils.Logger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Generation smoke tests.
 * Ensures card generation produces valid, contract-compliant cards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class GenerationSmokeTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `all games can generate at least one valid card`() = runBlocking {
        val allGameIds = GameMetadata.getAllGameIds()
        val failures = mutableListOf<String>()

        allGameIds.forEach { gameId ->
            try {
                val engine = com.helldeck.content.engine.ContentEngineProvider.get(context)
                val request = GameEngine.Request(
                    gameId = gameId,
                    sessionId = "test_session",
                    players = listOf("Player1", "Player2", "Player3"),
                    spiceMax = 2
                )

                val result = engine.next(request)

                // Validate contract
                val contractResult = GameContractValidator.validate(
                    gameId = result.filledCard.game,
                    interactionType = result.interactionType,
                    options = result.options,
                    filledCard = result.filledCard,
                    playersCount = 3
                )

                if (!contractResult.isValid) {
                    failures.add("$gameId: Contract failed - ${contractResult.reasons.joinToString(", ")}")
                }

                // Check for placeholders
                if (result.filledCard.text.contains("{") || result.filledCard.text.contains("}")) {
                    failures.add("$gameId: Card contains unresolved placeholders: ${result.filledCard.text}")
                }

                // Check for null text
                if (result.filledCard.text.contains("null", ignoreCase = true)) {
                    failures.add("$gameId: Card contains 'null' in text: ${result.filledCard.text}")
                }

            } catch (e: Exception) {
                failures.add("$gameId: Generation threw exception - ${e.message}")
            }
        }

        if (failures.isNotEmpty()) {
            fail("Generation failures:\n${failures.joinToString("\n")}")
        }
    }

    @Test
    fun `generated cards have reasonable length`() = runBlocking {
        val engine = com.helldeck.content.engine.ContentEngineProvider.get(context)
        val testGames = GameMetadata.getAllGameIds().take(5)

        testGames.forEach { gameId ->
            val request = GameEngine.Request(
                gameId = gameId,
                sessionId = "test_session",
                players = listOf("Player1", "Player2", "Player3"),
                spiceMax = 2
            )

            val result = engine.next(request)
            val wordCount = result.filledCard.text.split(Regex("\\s+")).filter { it.isNotBlank() }.size

            assertTrue("$gameId: Card too short ($wordCount words): ${result.filledCard.text}",
                wordCount >= 4)
            assertTrue("$gameId: Card too long ($wordCount words): ${result.filledCard.text}",
                wordCount <= 50)
        }
    }

    @Test
    fun `options match declared interactionType`() = runBlocking {
        val engine = com.helldeck.content.engine.ContentEngineProvider.get(context)
        val testGames = listOf(
            "POISON_PITCH", // A_B_CHOICE
            "ROAST_CONSENSUS", // VOTE_PLAYER
            "CONFESSION_OR_CAP", // TRUE_FALSE
            "TABOO_TIMER" // TABOO_GUESS
        )

        testGames.forEach { gameId ->
            val request = GameEngine.Request(
                gameId = gameId,
                sessionId = "test_session",
                players = listOf("Player1", "Player2", "Player3"),
                spiceMax = 2
            )

            val result = engine.next(request)
            val metadata = GameMetadata.getGameMetadata(gameId)

            assertNotNull("Game metadata should exist for $gameId", metadata)
            assertEquals("InteractionType should match metadata",
                metadata?.interactionType, result.interactionType)

            // Verify options type matches interaction type
            val contractResult = GameContractValidator.validate(
                gameId = result.filledCard.game,
                interactionType = result.interactionType,
                options = result.options,
                filledCard = result.filledCard,
                playersCount = 3
            )

            assertTrue("$gameId: Options should match interactionType - ${contractResult.reasons.joinToString(", ")}",
                contractResult.isValid)
        }
    }

    @Test
    fun `multiple generations produce different cards`() = runBlocking {
        val engine = com.helldeck.content.engine.ContentEngineProvider.get(context)
        val gameId = "POISON_PITCH"
        val generatedTexts = mutableSetOf<String>()

        repeat(10) {
            val request = GameEngine.Request(
                gameId = gameId,
                sessionId = "test_session_$it", // Different session to avoid anti-repeat
                players = listOf("Player1", "Player2", "Player3"),
                spiceMax = 2
            )

            val result = engine.next(request)
            generatedTexts.add(result.filledCard.text)
        }

        assertTrue("Should generate at least 3 different cards in 10 attempts, got ${generatedTexts.size}",
            generatedTexts.size >= 3)
    }
}
