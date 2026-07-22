@file:OptIn(ExperimentalMaterial3Api::class)

package com.helldeck.qa

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.db.HelldeckDb
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.ContextualSelector
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.util.SeededRng
import com.helldeck.content.validation.GameContractValidator
import com.helldeck.engine.GameMetadata
import com.helldeck.settings.SettingsStore
import com.helldeck.ui.HelldeckAppUI
import com.helldeck.ui.HelldeckTheme
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.Scene
import com.helldeck.ui.components.OnboardingFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-level acceptance tests for generation contracts and the two app entry paths.
 *
 * Timing and heap benchmarks intentionally live outside this functional CI suite so emulator
 * scheduling noise cannot turn product checks into flaky release gates.
 */
@RunWith(AndroidJUnit4::class)
class ComprehensiveQATest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: HelldeckDb
    private lateinit var gameEngine: GameEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            HelldeckDb::class.java,
        ).build()

        val repository = ContentRepository(context, database)
        val rng = SeededRng(20260722L)
        val selector = ContextualSelector(repository, rng.random)
        selector.seed(repository.templatesV2().associate { it.id to (1.0 to 1.0) })
        gameEngine = GameEngine(
            repo = repository,
            rng = rng,
            selector = selector,
            augmentor = null,
            modelId = "",
            cardGeneratorV3 = null,
            llmCardGeneratorV2 = null,
        )

        ContentEngineProvider.reset()
        runBlocking {
            SettingsStore.writeHasSeenOnboarding(true)
            SettingsStore.writeReducedMotion(true)
        }
    }

    @After
    fun teardown() {
        ContentEngineProvider.reset()
        database.close()
    }

    @Test
    fun allOfficialGamesHonorGenerationContractsAtSupportedPlayerBounds() = runBlocking {
        GameMetadata.getAllGames().forEach { game ->
            setOf(game.minPlayers, game.maxPlayers).forEach { playerCount ->
                val request = GameEngine.Request(
                    gameId = game.id,
                    sessionId = "contract_${game.id}_$playerCount",
                    spiceMax = 3,
                    players = playerNames(playerCount),
                )

                val result = gameEngine.next(request)
                val contract = GameContractValidator.validate(
                    gameId = result.filledCard.game,
                    interactionType = result.interactionType,
                    options = result.options,
                    filledCard = result.filledCard,
                    playersCount = playerCount,
                )

                assertEquals("Requested game should be preserved", game.id, result.filledCard.game)
                assertEquals("Timer should come from game metadata", game.timerSec, result.timer)
                assertEquals("Interaction should come from game metadata", game.interactionType, result.interactionType)
                assertTrue(
                    "${game.id} should satisfy its contract with $playerCount players: ${contract.reasons}",
                    contract.isValid,
                )
            }
        }
    }

    @Test
    fun unsupportedInputReturnsAnExplicitGoldFallback() = runBlocking {
        val result = gameEngine.next(
            GameEngine.Request(
                gameId = "NOT_A_REAL_GAME",
                sessionId = "fallback_contract",
                players = emptyList(),
                spiceMax = 1,
            ),
        )

        assertEquals("NOT_A_REAL_GAME", result.filledCard.game)
        assertEquals("gold_fallback", result.filledCard.family)
        assertEquals(true, result.filledCard.metadata["fallback"])
        assertFalse("Fallback text should be usable", result.filledCard.text.isBlank())
    }

    @Test
    fun onboardingSkipCompletesTheFirstRunFlow() {
        var completed = false

        composeTestRule.setContent {
            HelldeckTheme {
                OnboardingFlow(onComplete = { completed = true })
            }
        }

        composeTestRule.onNodeWithText("HELLDECK").assertExists()
        composeTestRule.onNodeWithText("Skip").assertHasClickAction().performClick()
        composeTestRule.runOnIdle {
            assertTrue("Skip should complete onboarding", completed)
        }
    }

    @Test
    fun homePrimaryActionStartsARound() {
        val viewModel = initializedViewModel()
        composeTestRule.setContent {
            HelldeckTheme {
                HelldeckAppUI(viewModel)
            }
        }

        waitForHome(viewModel)
        composeTestRule
            .onNodeWithText("🔥 Start the Chaos")
            .assertHasClickAction()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 60_000L) {
            viewModel.scene == Scene.ROUND && viewModel.roundState != null
        }
        composeTestRule.onNodeWithText("GET READY").assertExists()
    }

    @Test
    fun homePrimaryControlsExposeAccessibleLabels() {
        val viewModel = initializedViewModel()
        composeTestRule.setContent {
            HelldeckTheme {
                HelldeckAppUI(viewModel)
            }
        }

        waitForHome(viewModel)
        composeTestRule.onNodeWithContentDescription("Open rollcall").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Open settings").assertHasClickAction()
        composeTestRule.onNodeWithText("🔥 Start the Chaos").assertHasClickAction()
    }

    private fun initializedViewModel(): HelldeckVm {
        return HelldeckVm().also { viewModel ->
            runBlocking { viewModel.initOnce() }
        }
    }

    private fun waitForHome(viewModel: HelldeckVm) {
        composeTestRule.waitUntil(timeoutMillis = 30_000L) {
            viewModel.scene == Scene.HOME &&
                composeTestRule
                    .onAllNodesWithText("🔥 Start the Chaos")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
    }

    private fun playerNames(count: Int): List<String> =
        (1..count).map { index -> "Player $index" }
}
