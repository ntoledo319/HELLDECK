package com.helldeck.e2e

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.data.*
import com.helldeck.engine.*
import com.helldeck.fixtures.TestDataFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for complete game flows
 */
@RunWith(AndroidJUnit4::class)
class CompleteGameFlowTest {

    private lateinit var context: Context
    private lateinit var database: HelldeckDb
    private lateinit var repository: Repository
    private lateinit var templateEngine: TemplateEngine
    private lateinit var gameEngine: GameEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            HelldeckDb::class.java
        ).build()
        repository = Repository.get(context)
        templateEngine = TemplateEngine(context)
        gameEngine = GameEngine(context, repository, templateEngine)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `complete game session from start to finish`() = runBlocking {
        // Arrange - Set up game
        val playerNames = listOf("Alice", "Bob", "Charlie")
        val templates = TestDataFactory.createTemplateEntityList(10, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act - Initialize game
        gameEngine.initialize()
        val sessionId = gameEngine.currentSessionId
        assertNotNull("Session should be created", sessionId)

        // Add players
        playerNames.forEach { name ->
            repository.addPlayer(name, "😀")
        }

        // Play multiple rounds
        repeat(5) { roundNum ->
            // Generate card
            val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
            assertNotNull("Card should be generated", card)
            assertNotNull("Card text should not be null", card.text)
            assertFalse("Card text should not be blank", card.text.isBlank())

            // Provide feedback
            val feedback = TestDataFactory.createFeedback(
                lol = if (roundNum % 2 == 0) 3 else 1,
                meh = if (roundNum % 2 == 1) 2 else 0,
                trash = if (roundNum == 4) 1 else 0,
                latencyMs = 1000 + roundNum * 200L
            )

            // Commit round
            val result = gameEngine.commitRound(
                card = card,
                feedback = feedback,
                judgeWin = roundNum != 4,
                points = if (roundNum != 4) 2 else 0,
                latencyMs = 1000 + roundNum * 200L
            )

            assertNotNull("Round result should not be null", result)
        }

        // Assert - Verify game state
        assertEquals("Should have completed 5 rounds", 5, gameEngine.roundIdx)

        val rounds = repository.getRoundsForSession(sessionId!!).first()
        assertEquals("Should have 5 rounds in database", 5, rounds.size)

        val players = repository.getAllPlayers().first()
        assertEquals("Should have all players", playerNames.size, players.size)

        // Verify learning occurred
        val stats = gameEngine.getGameStats()
        assertNotNull("Stats should not be null", stats)
        assertTrue("Should have totalRounds stat", stats.containsKey("totalRounds"))
        assertEquals("Total rounds should be 5", 5, stats["totalRounds"])
    }

    @Test
    fun `multi-round game with different game types`() = runBlocking {
        // Arrange
        val roastTemplates = TestDataFactory.createTemplateEntityList(5, "ROAST_CONSENSUS")
        val confessionTemplates = TestDataFactory.createTemplateEntityList(5, "CONFESSION_OR_CAP")
        database.templates().insertAll(roastTemplates + confessionTemplates)

        gameEngine.initialize()
        repository.addPlayer("Player 1", "😎")
        repository.addPlayer("Player 2", "🦊")

        // Act - Play rounds with different game types
        val gamesPlayed = mutableListOf<String>()

        repeat(10) { i ->
            val gameType = if (i % 2 == 0) "ROAST_CONSENSUS" else "CONFESSION_OR_CAP"
            val card = gameEngine.nextFilledCard(gameType)
            
            assertNotNull("Card should be generated", card)
            assertEquals("Card should be of requested game type", gameType, card.game)
            gamesPlayed.add(gameType)

            val feedback = TestDataFactory.createFeedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000)
            gameEngine.commitRound(
                card = card,
                feedback = feedback,
                judgeWin = true,
                points = 2,
                latencyMs = 1000
            )
        }

        // Assert
        assertEquals("Should have played 10 rounds", 10, gamesPlayed.size)
        assertTrue("Should have played ROAST_CONSENSUS", gamesPlayed.contains("ROAST_CONSENSUS"))
        assertTrue("Should have played CONFESSION_OR_CAP", gamesPlayed.contains("CONFESSION_OR_CAP"))

        val rounds = repository.getRoundsForSession(gameEngine.currentSessionId!!).first()
        assertEquals("Should have 10 rounds in database", 10, rounds.size)

        // Verify variety in game types
        val uniqueGames = rounds.map { it.game }.distinct()
        assertEquals("Should have 2 unique game types", 2, uniqueGames.size)
    }

    @Test
    fun `learning system adapts over multiple rounds`() = runBlocking {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(20, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        gameEngine.initialize()
        repository.addPlayer("Player 1", "😀")

        // Act - Play rounds with varied feedback
        val selectedTemplateIds = mutableListOf<String>()

        repeat(30) { roundNum ->
            val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
            selectedTemplateIds.add(card.templateId)

            // Provide feedback that favors certain templates
            val feedback = if (card.templateId.endsWith("1") || card.templateId.endsWith("2")) {
                // Positive feedback for templates ending in 1 or 2
                TestDataFactory.createFeedback(lol = 3, meh = 0, trash = 0, latencyMs = 800)
            } else {
                // Negative feedback for others
                TestDataFactory.createFeedback(lol = 0, meh = 1, trash = 2, latencyMs = 3000)
            }

            gameEngine.commitRound(
                card = card,
                feedback = feedback,
                judgeWin = card.templateId.endsWith("1") || card.templateId.endsWith("2"),
                points = if (card.templateId.endsWith("1") || card.templateId.endsWith("2")) 2 else 0,
                latencyMs = feedback.latencyMs
            )
        }

        // Assert - Learning should favor high-performing templates
        val uniqueTemplates = selectedTemplateIds.distinct()
        assertTrue("Should have explored multiple templates", uniqueTemplates.size > 5)

        // Later rounds should favor better-performing templates
        val earlySelections = selectedTemplateIds.take(10)
        val lateSelections = selectedTemplateIds.takeLast(10)

        val earlyFavoriteCount = earlySelections.count { it.endsWith("1") || it.endsWith("2") }
        val lateFavoriteCount = lateSelections.count { it.endsWith("1") || it.endsWith("2") }

        // Learning should increase selection of high-performing templates
        assertTrue("Late rounds should favor high-performing templates more than early rounds",
            lateFavoriteCount >= earlyFavoriteCount)
    }

    @Test
    fun `error recovery in game flow`() = runBlocking {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(5, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        gameEngine.initialize()
        repository.addPlayer("Player 1", "😀")

        // Act - Play successful round
        val card1 = gameEngine.nextFilledCard("ROAST_CONSENSUS")
        gameEngine.commitRound(
            card = card1,
            feedback = TestDataFactory.createFeedback(),
            judgeWin = true,
            points = 2,
            latencyMs = 1000
        )

        // Attempt invalid operation
        try {
            gameEngine.nextFilledCard("INVALID_GAME")
            fail("Should have thrown exception for invalid game")
        } catch (e: IllegalStateException) {
            // Expected exception
        }

        // Continue with valid round after error
        val card2 = gameEngine.nextFilledCard("ROAST_CONSENSUS")
        gameEngine.commitRound(
            card = card2,
            feedback = TestDataFactory.createFeedback(),
            judgeWin = true,
            points = 2,
            latencyMs = 1000
        )

        // Assert - Game should continue normally after error
        val rounds = repository.getRoundsForSession(gameEngine.currentSessionId!!).first()
        assertEquals("Should have 2 successful rounds", 2, rounds.size)
    }

    @Test
    fun `performance under sustained load`() = runBlocking {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(50, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        gameEngine.initialize()
        repository.addPlayer("Player 1", "😀")
        repository.addPlayer("Player 2", "🦊")

        // Act - Simulate long game session
        val startTime = System.currentTimeMillis()

        repeat(100) {
            val card = gameEngine.nextFilledCard("ROAST_CONSENSUS")
            val feedback = TestDataFactory.createFeedback(
                lol = (1..3).random(),
                meh = (0..1).random(),
                trash = (0..1).random(),
                latencyMs = (800L..3000L).random()
            )

            gameEngine.commitRound(
                card = card,
                feedback = feedback,
                judgeWin = true,
                points = 2,
                latencyMs = feedback.latencyMs
            )
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Assert
        assertEquals("Should have completed 100 rounds", 100, gameEngine.roundIdx)
        
        val rounds = repository.getRoundsForSession(gameEngine.currentSessionId!!).first()
        assertEquals("Should have 100 rounds in database", 100, rounds.size)

        // Performance should be acceptable for long sessions
        assertTrue("100 rounds should complete in <10s (actual: ${totalTime}ms)",
            totalTime < 10000)

        val avgTimePerRound = totalTime / 100
        assertTrue("Average time per round should be <100ms (actual: ${avgTimePerRound}ms)",
            avgTimePerRound < 100)
    }
}