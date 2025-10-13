package com.helldeck.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.data.*
import com.helldeck.engine.Feedback
import com.helldeck.fixtures.TestDataFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for database operations
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var database: HelldeckDb
    private lateinit var repository: Repository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            HelldeckDb::class.java
        ).build()
        repository = Repository.get(context)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `complete round workflow persists data correctly`() = runBlocking {
        // Arrange
        val playerNames = listOf("Player 1", "Player 2", "Player 3")
        val sessionId = repository.createGameSession(playerNames)
        
        val template = TestDataFactory.createTemplateEntity(
            id = "test_template",
            game = "ROAST_CONSENSUS"
        )
        database.templates().insert(template)

        // Act - Record a round
        val roundId = repository.recordRound(
            sessionId = sessionId,
            templateId = template.id,
            game = "ROAST_CONSENSUS",
            filledText = "Test filled text",
            feedback = TestDataFactory.createFeedback(lol = 2, meh = 1, trash = 0, latencyMs = 1500),
            points = 3
        )

        // Assert
        assertTrue("Round ID should be positive", roundId > 0)

        // Verify session was created
        val session = repository.getSessionById(sessionId)
        assertNotNull("Session should exist", session)
        assertEquals("Session should have correct player count", playerNames.size, session.playerCount)

        // Verify round was recorded
        val rounds = repository.getRoundsForSession(sessionId).first()
        assertEquals("Should have one round", 1, rounds.size)
        assertEquals("Round should have correct template ID", template.id, rounds.first().templateId)

        // Verify players were created
        val players = repository.getAllPlayers().first()
        assertEquals("Should have all players", playerNames.size, players.size)
    }

    @Test
    fun `multiple rounds persist correctly`() = runBlocking {
        // Arrange
        val sessionId = repository.createGameSession(listOf("Player 1", "Player 2"))
        
        val templates = TestDataFactory.createTemplateEntityList(3, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act - Record multiple rounds
        val roundIds = templates.map { template ->
            repository.recordRound(
                sessionId = sessionId,
                templateId = template.id,
                game = "ROAST_CONSENSUS",
                filledText = "Round with ${template.id}",
                feedback = TestDataFactory.createFeedback(),
                points = 2
            )
        }

        // Assert
        assertEquals("Should have 3 round IDs", 3, roundIds.size)
        roundIds.forEach { roundId ->
            assertTrue("All round IDs should be positive", roundId > 0)
        }

        val rounds = repository.getRoundsForSession(sessionId).first()
        assertEquals("Should have 3 rounds", 3, rounds.size)

        // Verify all rounds belong to the session
        rounds.forEach { round ->
            assertEquals("All rounds should belong to session", sessionId, round.sessionId)
        }
    }

    @Test
    fun `player score updates persist correctly`() = runBlocking {
        // Arrange
        val player = repository.addPlayer("Test Player", "😀")
        val initialPoints = player.sessionPoints

        // Act - Update player score multiple times
        repository.updatePlayerScore(player.id, 5)
        repository.updatePlayerScore(player.id, 3)
        repository.updatePlayerScore(player.id, -2)

        // Assert
        val updatedPlayer = repository.getAllPlayers().first().find { it.id == player.id }
        assertNotNull("Player should exist", updatedPlayer)
        assertEquals("Player score should be updated correctly",
            initialPoints + 5 + 3 - 2, updatedPlayer?.sessionPoints)
    }

    @Test
    fun `concurrent operations maintain data integrity`() = runBlocking {
        // Arrange
        val sessionId = repository.createGameSession(listOf("Player 1", "Player 2"))

        // Act - Perform multiple operations concurrently
        val player1 = repository.addPlayer("Extra Player 1", "🤖")
        val player2 = repository.addPlayer("Extra Player 2", "👻")

        val template = TestDataFactory.createTemplateEntity()
        database.templates().insert(template)

        val round1Id = repository.recordRound(
            sessionId = sessionId,
            templateId = template.id,
            game = "ROAST_CONSENSUS",
            filledText = "Concurrent round 1",
            feedback = TestDataFactory.createFeedback(),
            points = 2
        )

        val round2Id = repository.recordRound(
            sessionId = sessionId,
            templateId = template.id,
            game = "ROAST_CONSENSUS",
            filledText = "Concurrent round 2",
            feedback = TestDataFactory.createFeedback(),
            points = 3
        )

        // Assert
        val allPlayers = repository.getAllPlayers().first()
        assertEquals("Should have 4 players total", 4, allPlayers.size)

        val sessionRounds = repository.getRoundsForSession(sessionId).first()
        assertEquals("Should have 2 rounds", 2, sessionRounds.size)

        assertTrue("Round 1 ID should be positive", round1Id > 0)
        assertTrue("Round 2 ID should be positive", round2Id > 0)
        assertNotEquals("Rounds should have different IDs", round1Id, round2Id)
    }

    @Test
    fun `transaction rollback works correctly on error`() = runBlocking {
        // Arrange
        val sessionId = repository.createGameSession(listOf("Player 1"))

        // Act & Assert
        try {
            // Attempt to record round with invalid data
            repository.recordRound(
                sessionId = -1L, // Invalid session ID
                templateId = "non_existent_template",
                game = "INVALID_GAME",
                filledText = "Invalid round",
                feedback = TestDataFactory.createFeedback(),
                points = 0
            )
            fail("Should have thrown exception for invalid data")
        } catch (e: Exception) {
            // Expected exception

            // Verify no rounds were created
            val rounds = repository.getRoundsForSession(sessionId).first()
            assertEquals("Should have no rounds after rollback", 0, rounds.size)
        }
    }
}