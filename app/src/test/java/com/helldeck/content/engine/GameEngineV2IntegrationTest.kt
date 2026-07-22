package com.helldeck.content.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.GameOptions
import com.helldeck.content.util.SeededRng
import com.helldeck.content.validation.GameContractValidator
import com.helldeck.engine.GameIds
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.InteractionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class GameEngineV2IntegrationTest {

    private lateinit var context: Context
    private lateinit var repo: ContentRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = ContentRepository(context)
    }

    @Test
    fun `content repository initializes`() {
        repo.initialize()
        assertNotNull(repo)
    }

    @Test
    fun `gold fallbacks satisfy seat interaction contracts`() = kotlinx.coroutines.runBlocking {
        val cases = listOf(
            Triple(GameIds.ROAST_CONS, InteractionType.VOTE_SEAT, GameOptions.SeatVote::class),
            Triple(GameIds.REALITY_CHECK, InteractionType.SELF_RATE, GameOptions.SeatSelect::class),
        )

        cases.forEach { (gameId, expectedInteraction, expectedOptions) ->
            val players = listOf("A", "B", "C")
            val result = engineFor(41L).next(
                GameEngine.Request(
                    sessionId = "fallback_$gameId",
                    gameId = gameId,
                    players = players,
                    spiceMax = 0,
                ),
            )
            val contract = GameContractValidator.validate(
                gameId = result.filledCard.game,
                interactionType = result.interactionType,
                options = result.options,
                filledCard = result.filledCard,
                playersCount = players.size,
            )

            assertEquals("gold_fallback", result.filledCard.family)
            assertEquals(expectedInteraction, result.interactionType)
            assertTrue(expectedOptions.isInstance(result.options))
            assertTrue("Fallback should satisfy the contract: ${contract.reasons}", contract.isValid)
        }
    }

    @Test
    fun `unscoped gold fallback selection is seeded`() = kotlinx.coroutines.runBlocking {
        val request = GameEngine.Request(
            sessionId = "seeded_fallback",
            players = listOf("A", "B", "C"),
            spiceMax = 0,
        )

        val first = engineFor(90210L).next(request)
        val second = engineFor(90210L).next(request)

        assertEquals(first.filledCard.game, second.filledCard.game)
        assertEquals(first.interactionType, second.interactionType)
        assertEquals(first.options, second.options)
    }

    @Test
    fun `v2 generation satisfies every supported game contract`() = kotlinx.coroutines.runBlocking {
        val engine = engineFor(20260722L)

        GameMetadata.getAllGames().forEach { game ->
            setOf(game.minPlayers, game.maxPlayers).forEach { playerCount ->
                val players = (1..playerCount).map { "Player $it" }
                val result = engine.next(
                    GameEngine.Request(
                        sessionId = "contract_${game.id}_$playerCount",
                        gameId = game.id,
                        players = players,
                        spiceMax = 3,
                    ),
                )
                val contract = GameContractValidator.validate(
                    gameId = result.filledCard.game,
                    interactionType = result.interactionType,
                    options = result.options,
                    filledCard = result.filledCard,
                    playersCount = players.size,
                )

                assertEquals(game.id, result.filledCard.game)
                assertEquals(game.timerSec, result.timer)
                assertEquals(game.interactionType, result.interactionType)
                assertTrue(
                    "${game.id} failed with $playerCount players: ${contract.reasons}",
                    contract.isValid,
                )
            }
        }
    }

    private fun engineFor(seed: Long): GameEngine {
        val rng = SeededRng(seed)
        val selector = ContextualSelector(repo, rng.random)
        selector.seed(repo.templatesV2().associate { it.id to (1.0 to 1.0) })
        return GameEngine(
            repo = repo,
            rng = rng,
            selector = selector,
            augmentor = null,
            modelId = "",
            cardGeneratorV3 = null,
            llmCardGeneratorV2 = null,
        )
    }
}
