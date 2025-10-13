package com.helldeck.engine

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.data.*
import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for GameEngine
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GameEngineTest : DatabaseTest() {

    private lateinit var context: Context
    private lateinit var repository: Repository
    private lateinit var templateEngine: TemplateEngine
    private lateinit var gameEngine: GameEngine

    @Before
    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        repository = Repository.get(context)
        templateEngine = TemplateEngine(context)
        gameEngine = GameEngine(context, repository, templateEngine)
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun `testGameEngineInitialization creates valid session`() = runTest {
        // Act
        gameEngine.initialize()

        // Assert
        assertNotNull("Session ID should not be null", gameEngine.currentSessionId)
        assertEquals("Round index should start at 0", 0, gameEngine.roundIdx)

        // Verify session was created in database
        val session = repository.getSessionById(gameEngine.currentSessionId!!)
        assertNotNull("Session should exist in database", session)
    }

    @Test
    fun `testNextFilledCardGeneration with valid game returns filled card`() = runTest {
        // Arrange
        setupTestTemplates()
        gameEngine.initialize()

        // Act
        val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")

        // Assert
        assertNotNull("Card should not be null", card)
        assertEquals("Game should match requested", "ROAST_CONSENSUS", card.game)
        assertNotNull("Card text should not be null", card.text)
        assertFalse("Card text should not be blank", card.text.isBlank())
        assertNotNull("Template ID should not be null", card.templateId)
        assertTrue("Card text should contain filled slots", card.text.contains(" "))
    }

    @Test
    fun `testNextFilledCardGeneration with different games works correctly`() = runTest {
        // Arrange
        setupMultipleGameTemplates()
        gameEngine.initialize()

        // Act & Assert
        val roastCard = gameEngine.nextFilledCard("ROAST_CONSENSUS")
        assertNotNull("Roast card should not be null", roastCard)
        assertEquals("Should be roast consensus game", "ROAST_CONSENSUS", roastCard.game)

        val confessionCard = gameEngine.nextFilledCard("CONFESSION_OR_CAP")
        assertNotNull("Confession card should not be null", confessionCard)
        assertEquals("Should be confession game", "CONFESSION_OR_CAP", confessionCard.game)

        // Cards should be different
        assertNotEquals("Cards should be different", roastCard.text, confessionCard.text)
    }

    @Test
    fun `testCommitRoundWithValidFeedback updates game state correctly`() = runTest {
        // Arrange
        setupTestTemplates()
        setupTestPlayers()
        gameEngine.initialize()

        val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
        val feedback = TestDataFactory.createFeedback(lol = 2, meh = 1, trash = 0, latencyMs = 1500)

        // Act
        val result = gameEngine.commitRound(
            card = card,
            feedback = feedback,
            judgeWin = true,
            points = 2,
            latencyMs = 1500
        )

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Points should match", 2, result.points)
        assertTrue("Room heat should be true for positive feedback", result.roomHeat)
        assertEquals("Round index should increment", 1, gameEngine.roundIdx)

        // Verify round was recorded in database
        val rounds = repository.getRoundsForSession(gameEngine.currentSessionId!!).first()
        assertEquals("Should have one round recorded", 1, rounds.size)
        assertEquals("Template ID should match", card.templateId, rounds.first().templateId)
    }

    @Test
    fun `testCommitRoundWithNegativeFeedback handles correctly`() = runTest {
        // Arrange
        setupTestTemplates()
        setupTestPlayers()
        gameEngine.initialize()

        val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
        val feedback = TestDataFactory.createFeedback(lol = 0, meh = 1, trash = 3, latencyMs = 5000)

        // Act
        val result = gameEngine.commitRound(
            card = card,
            feedback = feedback,
            judgeWin = false,
            points = 0,
            latencyMs = 5000
        )

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Points should be 0 for negative feedback", 0, result.points)
        assertFalse("Room heat should be false for negative feedback", result.roomHeat)
        assertEquals("Round index should increment", 1, gameEngine.roundIdx)
    }

    @Test
    fun `testGameStatsCalculation after multiple rounds`() = runTest {
        // Arrange
        setupTestTemplates()
        setupTestPlayers()
        gameEngine.initialize()

        // Generate multiple test rounds
        repeat(5) { round ->
            val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
            val feedback = TestDataFactory.createFeedback(
                lol = if (round % 2 == 0) 3 else 1,
                meh = if (round % 2 == 1) 2 else 0,
                trash = if (round == 4) 1 else 0,
                latencyMs = 1000 + round * 200L
            )

            gameEngine.commitRound(
                card = card,
                feedback = feedback,
                judgeWin = round != 4,
                points = if (round != 4) 2 else 0,
                latencyMs = 1000 + round * 200L
            )
        }

        // Act
        val stats = gameEngine.getGameStats()

        // Assert
        assertNotNull("Stats should not be null", stats)
        assertTrue("Should contain totalRounds", stats.containsKey("totalRounds"))
        assertTrue("Should contain totalTemplates", stats.containsKey("totalTemplates"))
        assertTrue("Should contain averageScore", stats.containsKey("averageScore"))

        assertEquals("Total rounds should be 5", 5, stats["totalRounds"])
        assertTrue("Average score should be positive", (stats["averageScore"] as Double) > 0.0)
    }

    @Test
    fun `testErrorHandlingForInvalidGame throws appropriate exception`() = runTest {
        // Arrange
        gameEngine.initialize()

        // Act & Assert
        try {
            gameEngine.nextFilledCard("INVALID_GAME")
            fail("Should have thrown exception for invalid game")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention templates",
                e.message?.contains("No templates available") == true)
        }
    }

    @Test
    fun `testErrorHandlingForUninitializedEngine throws exception`() = runTest {
        // Arrange - Don't initialize the engine

        // Act & Assert
        try {
            gameEngine.nextFilledCard("ROAST_CONSENSUS")
            fail("Should have thrown exception for uninitialized engine")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention initialization",
                e.message?.contains("not initialized") == true)
        }
    }

    @Test
    fun `testMultipleRoundsMaintainCorrectState`() = runTest {
        // Arrange
        setupTestTemplates()
        setupTestPlayers()
        gameEngine.initialize()

        val initialRoundIdx = gameEngine.roundIdx

        // Act - Play multiple rounds
        repeat(3) {
            val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
            val feedback = TestDataFactory.createFeedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000)

            gameEngine.commitRound(
                card = card,
                feedback = feedback,
                judgeWin = true,
                points = 2,
                latencyMs = 1000
            )
        }

        // Assert
        assertEquals("Round index should increment correctly", initialRoundIdx + 3, gameEngine.roundIdx)

        // Verify all rounds were recorded
        val rounds = repository.getRoundsForSession(gameEngine.currentSessionId!!).first()
        assertEquals("Should have 3 rounds recorded", 3, rounds.size)
    }

    @Test
    fun `testResetClearsGameState`() = runTest {
        // Arrange
        setupTestTemplates()
        setupTestPlayers()
        gameEngine.initialize()

        // Play a few rounds
        repeat(2) {
            val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
            val feedback = TestDataFactory.createFeedback(lol = 2, meh = 1, trash = 0, latencyMs = 1000)

            gameEngine.commitRound(
                card = card,
                feedback = feedback,
                judgeWin = true,
                points = 2,
                latencyMs = 1000
            )
        }

        // Act
        gameEngine.reset()

        // Assert
        assertEquals("Round index should reset to 0", 0, gameEngine.roundIdx)
        assertNull("Session ID should be null after reset", gameEngine.currentSessionId)
    }

    // Helper methods

    private suspend fun setupTestTemplates() {
        val testTemplates = listOf(
            TestDataFactory.createTemplateEntity(
                id = "test_rc1",
                game = "ROAST_CONSENSUS",
                text = "Most likely to {sketchy_action} for {tiny_reward}.",
                family = "test_roast",
                spice = 1,
                locality = 1,
                maxWords = 16
            ),
            TestDataFactory.createTemplateEntity(
                id = "test_rc2",
                game = "ROAST_CONSENSUS",
                text = "Who would {embarrassing_action} in {public_place}?",
                family = "test_roast",
                spice = 2,
                locality = 1,
                maxWords = 14
            ),
            TestDataFactory.createTemplateEntity(
                id = "test_cc1",
                game = "CONFESSION_OR_CAP",
                text = "Confess: {guilty_prompt} — or bluff convincingly.",
                family = "test_confess",
                spice = 2,
                locality = 2,
                maxWords = 14
            )
        )

        database.templates().insertAll(testTemplates)
    }

    private suspend fun setupMultipleGameTemplates() {
        val testTemplates = listOf(
            TestDataFactory.createTemplateEntity(
                id = "test_rc1",
                game = "ROAST_CONSENSUS",
                text = "Most likely to {sketchy_action} for {tiny_reward}.",
                family = "test_roast",
                spice = 1,
                locality = 1,
                maxWords = 16
            ),
            TestDataFactory.createTemplateEntity(
                id = "test_cc1",
                game = "CONFESSION_OR_CAP",
                text = "Confess: {guilty_prompt} — or bluff convincingly.",
                family = "test_confess",
                spice = 2,
                locality = 2,
                maxWords = 14
            ),
            TestDataFactory.createTemplateEntity(
                id = "test_cc2",
                game = "CONFESSION_OR_CAP",
                text = "Would you rather {weird_choice_a} or {weird_choice_b}?",
                family = "test_confess",
                spice = 1,
                locality = 1,
                maxWords = 12
            )
        )

        database.templates().insertAll(testTemplates)
    }

    private suspend fun setupTestPlayers() {
        val testPlayers = TestDataFactory.createPlayerEntityList(3)
        database.players().insertAll(testPlayers)
    }
}