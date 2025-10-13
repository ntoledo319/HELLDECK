package com.helldeck.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.DatabaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for Repository
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RepositoryTest : DatabaseTest() {

    private lateinit var context: Context
    private lateinit var repository: Repository

    @Before
    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        repository = Repository.get(context)
    }

    @Test
    fun `get returns singleton instance correctly`() = runTest {
        // Act
        val repo1 = Repository.get(context)
        val repo2 = Repository.get(context)

        // Assert
        assertNotNull("Repository instance should not be null", repo1)
        assertNotNull("Repository instance should not be null", repo2)
        assertSame("Should return same singleton instance", repo1, repo2)
    }

    @Test
    fun `addPlayer inserts player correctly`() = runTest {
        // Arrange
        val playerName = "Test Player"
        val avatar = "😀"

        // Act
        val player = repository.addPlayer(playerName, avatar)

        // Assert
        assertNotNull("Player should not be null", player)
        assertEquals("Player name should match", playerName, player.name)
        assertEquals("Player avatar should match", avatar, player.avatar)
        assertNotNull("Player ID should not be null", player.id)
        assertTrue("Player should have valid session points", player.sessionPoints >= 0)

        // Verify in database
        val allPlayers = repository.getAllPlayers().first()
        assertTrue("Player should be in database", allPlayers.contains(player))
    }

    @Test
    fun `addPlayer generates unique IDs for different players`() = runTest {
        // Act
        val player1 = repository.addPlayer("Player 1", "😀")
        val player2 = repository.addPlayer("Player 2", "😎")

        // Assert
        assertNotNull("Player 1 should not be null", player1)
        assertNotNull("Player 2 should not be null", player2)
        assertNotEquals("Players should have different IDs", player1.id, player2.id)
    }

    @Test
    fun `getAllPlayers returns all players correctly`() = runTest {
        // Arrange
        val players = TestDataFactory.createPlayerEntityList(3)
        database.players().insertAll(players)

        // Act
        val allPlayers = repository.getAllPlayers().first()

        // Assert
        assertEquals("Should return all players", 3, allPlayers.size)
        players.forEach { expectedPlayer ->
            assertTrue("All expected players should be present",
                allPlayers.any { it.id == expectedPlayer.id })
        }
    }

    @Test
    fun `getAllPlayers returns empty list when no players exist`() = runTest {
        // Act
        val allPlayers = repository.getAllPlayers().first()

        // Assert
        assertNotNull("Player list should not be null", allPlayers)
        assertTrue("Player list should be empty", allPlayers.isEmpty())
    }

    @Test
    fun `recordRound persists round data correctly`() = runTest {
        // Arrange
        val sessionId = 1L
        val templateId = "test_template"
        val game = "ROAST_CONSENSUS"
        val filledText = "Test filled text"
        val feedback = TestDataFactory.createFeedback(lol = 2, meh = 1, trash = 0, latencyMs = 1500)
        val points = 2

        // Act
        val roundId = repository.recordRound(
            sessionId = sessionId,
            templateId = templateId,
            game = game,
            filledText = filledText,
            feedback = feedback,
            points = points
        )

        // Assert
        assertTrue("Round ID should be positive", roundId > 0)

        // Verify in database
        val rounds = repository.getRoundsForSession(sessionId).first()
        assertEquals("Should have one round recorded", 1, rounds.size)
        assertEquals("Round ID should match", roundId, rounds.first().id)
        assertEquals("Template ID should match", templateId, rounds.first().templateId)
        assertEquals("Game should match", game, rounds.first().game)
        assertEquals("Filled text should match", filledText, rounds.first().filledText)
        assertEquals("Points should match", points, rounds.first().points)
    }

    @Test
    fun `recordRound handles negative points correctly`() = runTest {
        // Arrange
        val sessionId = 1L
        val templateId = "test_template"
        val game = "ROAST_CONSENSUS"
        val filledText = "Test filled text"
        val feedback = TestDataFactory.createFeedback(lol = 0, meh = 1, trash = 3, latencyMs = 5000)
        val negativePoints = -1

        // Act
        val roundId = repository.recordRound(
            sessionId = sessionId,
            templateId = templateId,
            game = game,
            filledText = filledText,
            feedback = feedback,
            points = negativePoints
        )

        // Assert
        assertTrue("Round ID should be positive", roundId > 0)

        // Verify in database
        val rounds = repository.getRoundsForSession(sessionId).first()
        assertEquals("Should have one round recorded", 1, rounds.size)
        assertEquals("Points should be negative", negativePoints, rounds.first().points)
    }

    @Test
    fun `getRoundsForSession returns correct rounds`() = runTest {
        // Arrange
        val sessionId1 = 1L
        val sessionId2 = 2L

        val round1 = TestDataFactory.createRoundEntity(sessionId = sessionId1, id = 1L)
        val round2 = TestDataFactory.createRoundEntity(sessionId = sessionId1, id = 2L)
        val round3 = TestDataFactory.createRoundEntity(sessionId = sessionId2, id = 3L)

        database.rounds().insertAll(listOf(round1, round2, round3))

        // Act
        val session1Rounds = repository.getRoundsForSession(sessionId1).first()
        val session2Rounds = repository.getRoundsForSession(sessionId2).first()

        // Assert
        assertEquals("Session 1 should have 2 rounds", 2, session1Rounds.size)
        assertEquals("Session 2 should have 1 round", 1, session2Rounds.size)

        assertTrue("Session 1 rounds should contain round 1",
            session1Rounds.any { it.id == 1L })
        assertTrue("Session 1 rounds should contain round 2",
            session1Rounds.any { it.id == 2L })
        assertTrue("Session 2 rounds should contain round 3",
            session2Rounds.any { it.id == 3L })
    }

    @Test
    fun `getRoundsForSession returns empty list for non-existent session`() = runTest {
        // Act
        val rounds = repository.getRoundsForSession(999L).first()

        // Assert
        assertNotNull("Rounds list should not be null", rounds)
        assertTrue("Rounds list should be empty for non-existent session", rounds.isEmpty())
    }

    @Test
    fun `createGameSession creates valid session`() = runTest {
        // Arrange
        val playerNames = listOf("Player 1", "Player 2", "Player 3")

        // Act
        val sessionId = repository.createGameSession(playerNames)

        // Assert
        assertTrue("Session ID should be positive", sessionId > 0)

        // Verify session exists in database
        val session = repository.getSessionById(sessionId)
        assertNotNull("Session should exist in database", session)
        assertEquals("Session should have correct player count", playerNames.size, session.playerCount)
        assertTrue("Session should not be ended", session.endTime == null)
    }

    @Test
    fun `createGameSession with empty player list throws exception`() = runTest {
        // Arrange
        val emptyPlayerList = emptyList<String>()

        // Act & Assert
        try {
            repository.createGameSession(emptyPlayerList)
            fail("Should have thrown exception for empty player list")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention players",
                e.message?.contains("players") == true)
        }
    }

    @Test
    fun `createGameSession creates players correctly`() = runTest {
        // Arrange
        val playerNames = listOf("Alice", "Bob", "Charlie")

        // Act
        val sessionId = repository.createGameSession(playerNames)

        // Assert
        val allPlayers = repository.getAllPlayers().first()
        assertEquals("Should have created all players", playerNames.size, allPlayers.size)

        playerNames.forEach { expectedName ->
            assertTrue("Player should exist: $expectedName",
                allPlayers.any { it.name == expectedName })
        }
    }

    @Test
    fun `getSessionById returns correct session`() = runTest {
        // Arrange
        val playerNames = listOf("Player 1", "Player 2")
        val sessionId = repository.createGameSession(playerNames)

        // Act
        val session = repository.getSessionById(sessionId)

        // Assert
        assertNotNull("Session should not be null", session)
        assertEquals("Session ID should match", sessionId, session.id)
        assertEquals("Player count should match", playerNames.size, session.playerCount)
    }

    @Test
    fun `getSessionById returns null for non-existent session`() = runTest {
        // Act
        val session = repository.getSessionById(999L)

        // Assert
        assertNull("Should return null for non-existent session", session)
    }

    @Test
    fun `updatePlayerScore updates player points correctly`() = runTest {
        // Arrange
        val player = repository.addPlayer("Test Player", "😀")
        val additionalPoints = 5

        // Act
        repository.updatePlayerScore(player.id, additionalPoints)

        // Assert
        val updatedPlayer = repository.getAllPlayers().first().find { it.id == player.id }
        assertNotNull("Updated player should exist", updatedPlayer)
        assertEquals("Player points should be updated",
            player.sessionPoints + additionalPoints, updatedPlayer?.sessionPoints)
    }

    @Test
    fun `updatePlayerScore handles negative points correctly`() = runTest {
        // Arrange
        val player = repository.addPlayer("Test Player", "😀")
        val pointsToSubtract = -3

        // Act
        repository.updatePlayerScore(player.id, pointsToSubtract)

        // Assert
        val updatedPlayer = repository.getAllPlayers().first().find { it.id == player.id }
        assertNotNull("Updated player should exist", updatedPlayer)
        assertEquals("Player points should be reduced",
            player.sessionPoints + pointsToSubtract, updatedPlayer?.sessionPoints)
    }

    @Test
    fun `updatePlayerScore with non-existent player does nothing`() = runTest {
        // Act
        repository.updatePlayerScore("non_existent_id", 5)

        // Assert
        val allPlayers = repository.getAllPlayers().first()
        allPlayers.forEach { player ->
            assertEquals("Existing players should not be affected", 0, player.sessionPoints)
        }
    }

    @Test
    fun `multiple operations maintain data consistency`() = runTest {
        // Arrange
        val playerNames = listOf("Alice", "Bob", "Charlie")

        // Act - Create session and add rounds
        val sessionId = repository.createGameSession(playerNames)

        val templateId = "test_template"
        val game = "ROAST_CONSENSUS"

        // Add multiple rounds
        val roundIds = (1..3).map { roundNum ->
            repository.recordRound(
                sessionId = sessionId,
                templateId = templateId,
                game = game,
                filledText = "Round $roundNum text",
                feedback = TestDataFactory.createFeedback(lol = 2, meh = 0, trash = 0, latencyMs = 1000),
                points = roundNum * 2
            )
        }

        // Assert
        val allPlayers = repository.getAllPlayers().first()
        assertEquals("Should have all players", playerNames.size, allPlayers.size)

        val sessionRounds = repository.getRoundsForSession(sessionId).first()
        assertEquals("Should have all rounds", 3, sessionRounds.size)

        roundIds.forEachIndexed { index, expectedId ->
            assertEquals("Round ID should match", expectedId, roundIds[index])
        }

        // Verify round data integrity
        sessionRounds.forEachIndexed { index, round ->
            assertEquals("Template ID should be consistent", templateId, round.templateId)
            assertEquals("Game should be consistent", game, round.game)
            assertEquals("Points should match", (index + 1) * 2, round.points)
        }
    }

    @Test
    fun `concurrent operations handle correctly`() = runTest {
        // Arrange
        val playerNames = listOf("Player 1", "Player 2")

        // Act - Perform multiple operations
        val sessionId = repository.createGameSession(playerNames)

        // Add players and rounds concurrently (simulated)
        val player1 = repository.addPlayer("Extra Player 1", "🤖")
        val player2 = repository.addPlayer("Extra Player 2", "👻")

        val round1 = repository.recordRound(
            sessionId = sessionId,
            templateId = "template_1",
            game = "ROAST_CONSENSUS",
            filledText = "Concurrent round 1",
            feedback = TestDataFactory.createFeedback(),
            points = 2
        )

        val round2 = repository.recordRound(
            sessionId = sessionId,
            templateId = "template_2",
            game = "CONFESSION_OR_CAP",
            filledText = "Concurrent round 2",
            feedback = TestDataFactory.createFeedback(),
            points = 3
        )

        // Assert
        val allPlayers = repository.getAllPlayers().first()
        assertEquals("Should have all players", 5, allPlayers.size) // 3 session + 2 extra

        val sessionRounds = repository.getRoundsForSession(sessionId).first()
        assertEquals("Should have session rounds", 2, sessionRounds.size)

        assertTrue("Round 1 should exist", round1 > 0)
        assertTrue("Round 2 should exist", round2 > 0)
        assertNotEquals("Rounds should have different IDs", round1, round2)
    }
}