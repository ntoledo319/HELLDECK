package com.helldeck.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.DatabaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for RoundDao
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RoundDaoTest : DatabaseTest() {

    @Test
    fun `insert inserts round correctly`() = runTest {
        // Arrange
        val round = TestDataFactory.createRoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "test_template",
            game = "ROAST_CONSENSUS",
            filledText = "Test filled text",
            points = 2,
            timestamp = 1000L
        )

        // Act
        database.rounds().insert(round)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round should be retrievable", retrieved)
        assertEquals("Round ID should match", round.id, retrieved?.id)
        assertEquals("Session ID should match", round.sessionId, retrieved?.sessionId)
        assertEquals("Template ID should match", round.templateId, retrieved?.templateId)
        assertEquals("Game should match", round.game, retrieved?.game)
        assertEquals("Filled text should match", round.filledText, retrieved?.filledText)
        assertEquals("Points should match", round.points, retrieved?.points)
        assertEquals("Timestamp should match", round.timestamp, retrieved?.timestamp)
    }

    @Test
    fun `insertAll inserts multiple rounds correctly`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 2L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 3L, sessionId = 2L)
        )

        // Act
        database.rounds().insertAll(rounds)

        // Assert
        val allRounds = database.rounds().getAll()
        assertEquals("Should have all rounds", 3, allRounds.size)

        rounds.forEach { expectedRound ->
            val found = allRounds.find { it.id == expectedRound.id }
            assertNotNull("Round should be found: ${expectedRound.id}", found)
            assertEquals("Round session ID should match", expectedRound.sessionId, found?.sessionId)
        }
    }

    @Test
    fun `byId returns null for non-existent round`() = runTest {
        // Act
        val round = database.rounds().byId(999L)

        // Assert
        assertNull("Should return null for non-existent round", round)
    }

    @Test
    fun `getAll returns all rounds correctly`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 2L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 3L, sessionId = 2L)
        )
        database.rounds().insertAll(rounds)

        // Act
        val allRounds = database.rounds().getAll()

        // Assert
        assertEquals("Should return all rounds", 3, allRounds.size)
        rounds.forEach { expectedRound ->
            assertTrue("All rounds should be present",
                allRounds.any { it.id == expectedRound.id })
        }
    }

    @Test
    fun `getAll returns empty list when no rounds exist`() = runTest {
        // Act
        val allRounds = database.rounds().getAll()

        // Assert
        assertNotNull("Results should not be null", allRounds)
        assertTrue("Results should be empty when no rounds exist", allRounds.isEmpty())
    }

    @Test
    fun `getRoundsForSession returns correct rounds for session`() = runTest {
        // Arrange
        val session1Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 2L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 3L, sessionId = 1L)
        )

        val session2Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 4L, sessionId = 2L),
            TestDataFactory.createRoundEntity(id = 5L, sessionId = 2L)
        )

        val allRounds = session1Rounds + session2Rounds
        database.rounds().insertAll(allRounds)

        // Act
        val roundsForSession1 = database.rounds().getRoundsForSession(1L)
        val roundsForSession2 = database.rounds().getRoundsForSession(2L)

        // Assert
        assertEquals("Session 1 should have 3 rounds", 3, roundsForSession1.size)
        assertEquals("Session 2 should have 2 rounds", 2, roundsForSession2.size)

        roundsForSession1.forEach { round ->
            assertEquals("All rounds should be for session 1", 1L, round.sessionId)
        }

        roundsForSession2.forEach { round ->
            assertEquals("All rounds should be for session 2", 2L, round.sessionId)
        }
    }

    @Test
    fun `getRoundsForSession returns empty list for non-existent session`() = runTest {
        // Act
        val rounds = database.rounds().getRoundsForSession(999L)

        // Assert
        assertNotNull("Results should not be null", rounds)
        assertTrue("Results should be empty for non-existent session", rounds.isEmpty())
    }

    @Test
    fun `getRoundsByGame returns correct rounds for game type`() = runTest {
        // Arrange
        val roastRounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, game = "ROAST_CONSENSUS"),
            TestDataFactory.createRoundEntity(id = 2L, game = "ROAST_CONSENSUS")
        )

        val confessionRounds = listOf(
            TestDataFactory.createRoundEntity(id = 3L, game = "CONFESSION_OR_CAP"),
            TestDataFactory.createRoundEntity(id = 4L, game = "CONFESSION_OR_CAP"),
            TestDataFactory.createRoundEntity(id = 5L, game = "CONFESSION_OR_CAP")
        )

        val allRounds = roastRounds + confessionRounds
        database.rounds().insertAll(allRounds)

        // Act
        val roastResults = database.rounds().getRoundsByGame("ROAST_CONSENSUS")
        val confessionResults = database.rounds().getRoundsByGame("CONFESSION_OR_CAP")

        // Assert
        assertEquals("Should return correct number of roast rounds", 2, roastResults.size)
        assertEquals("Should return correct number of confession rounds", 3, confessionResults.size)

        roastResults.forEach { round ->
            assertEquals("All rounds should be ROAST_CONSENSUS", "ROAST_CONSENSUS", round.game)
        }

        confessionResults.forEach { round ->
            assertEquals("All rounds should be CONFESSION_OR_CAP", "CONFESSION_OR_CAP", round.game)
        }
    }

    @Test
    fun `getRoundsByGame returns empty list for game with no rounds`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, game = "ROAST_CONSENSUS"),
            TestDataFactory.createRoundEntity(id = 2L, game = "ROAST_CONSENSUS")
        )
        database.rounds().insertAll(rounds)

        // Act
        val results = database.rounds().getRoundsByGame("NON_EXISTENT_GAME")

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for non-existent game", results.isEmpty())
    }

    @Test
    fun `getRoundsByTemplate returns correct rounds for template`() = runTest {
        // Arrange
        val template1Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, templateId = "template1"),
            TestDataFactory.createRoundEntity(id = 2L, templateId = "template1")
        )

        val template2Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 3L, templateId = "template2"),
            TestDataFactory.createRoundEntity(id = 4L, templateId = "template2"),
            TestDataFactory.createRoundEntity(id = 5L, templateId = "template2")
        )

        val allRounds = template1Rounds + template2Rounds
        database.rounds().insertAll(allRounds)

        // Act
        val template1Results = database.rounds().getRoundsByTemplate("template1")
        val template2Results = database.rounds().getRoundsByTemplate("template2")

        // Assert
        assertEquals("Should return correct number of template1 rounds", 2, template1Results.size)
        assertEquals("Should return correct number of template2 rounds", 3, template2Results.size)

        template1Results.forEach { round ->
            assertEquals("All rounds should be for template1", "template1", round.templateId)
        }

        template2Results.forEach { round ->
            assertEquals("All rounds should be for template2", "template2", round.templateId)
        }
    }

    @Test
    fun `getRoundsByTemplate returns empty list for template with no rounds`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, templateId = "template1"),
            TestDataFactory.createRoundEntity(id = 2L, templateId = "template1")
        )
        database.rounds().insertAll(rounds)

        // Act
        val results = database.rounds().getRoundsByTemplate("non_existent_template")

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for non-existent template", results.isEmpty())
    }

    @Test
    fun `getRoundCount returns correct count`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 2L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 3L, sessionId = 2L)
        )
        database.rounds().insertAll(rounds)

        // Act
        val count = database.rounds().getRoundCount()

        // Assert
        assertEquals("Count should match inserted rounds", 3, count)
    }

    @Test
    fun `getRoundCount returns zero when no rounds exist`() = runTest {
        // Act
        val count = database.rounds().getRoundCount()

        // Assert
        assertEquals("Count should be zero when no rounds exist", 0, count)
    }

    @Test
    fun `getRoundCountForSession returns correct count for session`() = runTest {
        // Arrange
        val session1Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 2L, sessionId = 1L),
            TestDataFactory.createRoundEntity(id = 3L, sessionId = 1L)
        )

        val session2Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 4L, sessionId = 2L),
            TestDataFactory.createRoundEntity(id = 5L, sessionId = 2L)
        )

        val allRounds = session1Rounds + session2Rounds
        database.rounds().insertAll(allRounds)

        // Act
        val session1Count = database.rounds().getRoundCountForSession(1L)
        val session2Count = database.rounds().getRoundCountForSession(2L)

        // Assert
        assertEquals("Session 1 should have 3 rounds", 3, session1Count)
        assertEquals("Session 2 should have 2 rounds", 2, session2Count)
    }

    @Test
    fun `getRoundCountForSession returns zero for session with no rounds`() = runTest {
        // Act
        val count = database.rounds().getRoundCountForSession(999L)

        // Assert
        assertEquals("Count should be zero for session with no rounds", 0, count)
    }

    @Test
    fun `update updates round correctly`() = runTest {
        // Arrange
        val originalRound = TestDataFactory.createRoundEntity(
            id = 1L,
            filledText = "Original text",
            points = 1
        )
        database.rounds().insert(originalRound)

        val updatedRound = originalRound.copy(
            filledText = "Updated text",
            points = 3
        )

        // Act
        database.rounds().update(updatedRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round should still exist", retrieved)
        assertEquals("Filled text should be updated", "Updated text", retrieved?.filledText)
        assertEquals("Points should be updated", 3, retrieved?.points)
        assertEquals("Other fields should remain unchanged",
            originalRound.sessionId, retrieved?.sessionId)
    }

    @Test
    fun `delete removes round correctly`() = runTest {
        // Arrange
        val roundToDelete = TestDataFactory.createRoundEntity(id = 1L)
        val roundToKeep = TestDataFactory.createRoundEntity(id = 2L)

        database.rounds().insert(roundToDelete)
        database.rounds().insert(roundToKeep)

        // Act
        database.rounds().delete(roundToDelete)

        // Assert
        val deletedRound = database.rounds().byId(1L)
        val keptRound = database.rounds().byId(2L)

        assertNull("Deleted round should not exist", deletedRound)
        assertNotNull("Kept round should still exist", keptRound)
    }

    @Test
    fun `deleteById removes round by ID correctly`() = runTest {
        // Arrange
        val round = TestDataFactory.createRoundEntity(id = 1L)
        database.rounds().insert(round)

        // Act
        database.rounds().deleteById(1L)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNull("Round should be deleted", retrieved)
    }

    @Test
    fun `insert handles rounds with special characters correctly`() = runTest {
        // Arrange
        val specialRound = TestDataFactory.createRoundEntity(
            id = 1L,
            filledText = "Round with spécial çharácters and émojis 🚀!",
            game = "ROAST_CONSENSUS"
        )

        // Act
        database.rounds().insert(specialRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round with special characters should be stored", retrieved)
        assertEquals("Special characters should be preserved",
            "Round with spécial çharácters and émojis 🚀!", retrieved?.filledText)
    }

    @Test
    fun `insert handles very long filled text correctly`() = runTest {
        // Arrange
        val longText = "A".repeat(1000) + " Very Long Filled Text " + "B".repeat(1000)
        val longRound = TestDataFactory.createRoundEntity(
            id = 1L,
            filledText = longText
        )

        // Act
        database.rounds().insert(longRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round with long text should be stored", retrieved)
        assertEquals("Long text should be preserved", longText, retrieved?.filledText)
    }

    @Test
    fun `insert handles rounds with negative points correctly`() = runTest {
        // Arrange
        val negativePointsRound = TestDataFactory.createRoundEntity(
            id = 1L,
            points = -5
        )

        // Act
        database.rounds().insert(negativePointsRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round with negative points should be stored", retrieved)
        assertEquals("Negative points should be preserved", -5, retrieved?.points)
    }

    @Test
    fun `insert handles rounds with zero points correctly`() = runTest {
        // Arrange
        val zeroPointsRound = TestDataFactory.createRoundEntity(
            id = 1L,
            points = 0
        )

        // Act
        database.rounds().insert(zeroPointsRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round with zero points should be stored", retrieved)
        assertEquals("Zero points should be preserved", 0, retrieved?.points)
    }

    @Test
    fun `insert handles rounds with high points correctly`() = runTest {
        // Arrange
        val highPointsRound = TestDataFactory.createRoundEntity(
            id = 1L,
            points = 9999
        )

        // Act
        database.rounds().insert(highPointsRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round with high points should be stored", retrieved)
        assertEquals("High points should be preserved", 9999, retrieved?.points)
    }

    @Test
    fun `insert handles rounds with empty filled text correctly`() = runTest {
        // Arrange
        val emptyTextRound = TestDataFactory.createRoundEntity(
            id = 1L,
            filledText = ""
        )

        // Act
        database.rounds().insert(emptyTextRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Round with empty text should be stored", retrieved)
        assertEquals("Empty text should be preserved", "", retrieved?.filledText)
    }

    @Test
    fun `insert handles rounds with minimum valid values correctly`() = runTest {
        // Arrange
        val minimalRound = TestDataFactory.createRoundEntity(
            id = 1L,
            sessionId = 1L,
            templateId = "t",
            game = "G",
            filledText = "T",
            points = 0,
            timestamp = 0L
        )

        // Act
        database.rounds().insert(minimalRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Minimal round should be stored", retrieved)
        assertEquals("Minimal sessionId should be preserved", 1L, retrieved?.sessionId)
        assertEquals("Minimal templateId should be preserved", "t", retrieved?.templateId)
        assertEquals("Minimal game should be preserved", "G", retrieved?.game)
        assertEquals("Minimal filledText should be preserved", "T", retrieved?.filledText)
        assertEquals("Minimal points should be preserved", 0, retrieved?.points)
        assertEquals("Minimal timestamp should be preserved", 0L, retrieved?.timestamp)
    }

    @Test
    fun `insert handles rounds with maximum valid values correctly`() = runTest {
        // Arrange
        val maximalRound = TestDataFactory.createRoundEntity(
            id = 1L,
            sessionId = Long.MAX_VALUE,
            templateId = "A".repeat(100),
            game = "ROAST_CONSENSUS",
            filledText = "A".repeat(1000),
            points = Int.MAX_VALUE,
            timestamp = Long.MAX_VALUE
        )

        // Act
        database.rounds().insert(maximalRound)

        // Assert
        val retrieved = database.rounds().byId(1L)
        assertNotNull("Maximal round should be stored", retrieved)
        assertEquals("Maximal sessionId should be preserved", Long.MAX_VALUE, retrieved?.sessionId)
        assertEquals("Maximal templateId should be preserved", "A".repeat(100), retrieved?.templateId)
        assertEquals("Maximal filledText should be preserved", "A".repeat(1000), retrieved?.filledText)
        assertEquals("Maximal points should be preserved", Int.MAX_VALUE, retrieved?.points)
        assertEquals("Maximal timestamp should be preserved", Long.MAX_VALUE, retrieved?.timestamp)
    }

    @Test
    fun `getRoundsByPointsRange returns correct rounds for point range`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, points = 1),
            TestDataFactory.createRoundEntity(id = 2L, points = 2),
            TestDataFactory.createRoundEntity(id = 3L, points = 3),
            TestDataFactory.createRoundEntity(id = 4L, points = 4),
            TestDataFactory.createRoundEntity(id = 5L, points = 5)
        )
        database.rounds().insertAll(rounds)

        // Act
        val lowRangeResults = database.rounds().getRoundsByPointsRange(1, 3) // Points 1-3
        val highRangeResults = database.rounds().getRoundsByPointsRange(3, 5) // Points 3-5

        // Assert
        assertEquals("Low range should return 3 rounds", 3, lowRangeResults.size)
        assertEquals("High range should return 3 rounds", 3, highRangeResults.size)

        lowRangeResults.forEach { round ->
            assertTrue("All rounds should have points in range 1-3",
                round.points in 1..3)
        }

        highRangeResults.forEach { round ->
            assertTrue("All rounds should have points in range 3-5",
                round.points in 3..5)
        }
    }

    @Test
    fun `getRoundsByPointsRange returns empty list for range with no rounds`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, points = 1),
            TestDataFactory.createRoundEntity(id = 2L, points = 2)
        )
        database.rounds().insertAll(rounds)

        // Act
        val results = database.rounds().getRoundsByPointsRange(10, 20)

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for range with no rounds", results.isEmpty())
    }

    @Test
    fun `getRoundsByTimestampRange returns correct rounds for timestamp range`() = runTest {
        // Arrange
        val baseTime = 1000L
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, timestamp = baseTime),
            TestDataFactory.createRoundEntity(id = 2L, timestamp = baseTime + 1000),
            TestDataFactory.createRoundEntity(id = 3L, timestamp = baseTime + 2000),
            TestDataFactory.createRoundEntity(id = 4L, timestamp = baseTime + 3000),
            TestDataFactory.createRoundEntity(id = 5L, timestamp = baseTime + 4000)
        )
        database.rounds().insertAll(rounds)

        // Act
        val earlyRangeResults = database.rounds().getRoundsByTimestampRange(baseTime, baseTime + 2500)
        val lateRangeResults = database.rounds().getRoundsByTimestampRange(baseTime + 1500, baseTime + 4000)

        // Assert
        assertEquals("Early range should return 3 rounds", 3, earlyRangeResults.size)
        assertEquals("Late range should return 3 rounds", 3, lateRangeResults.size)

        earlyRangeResults.forEach { round ->
            assertTrue("All rounds should have timestamp in early range",
                round.timestamp in baseTime..(baseTime + 2500))
        }

        lateRangeResults.forEach { round ->
            assertTrue("All rounds should have timestamp in late range",
                round.timestamp in (baseTime + 1500)..(baseTime + 4000))
        }
    }

    @Test
    fun `getRoundsByTimestampRange returns empty list for range with no rounds`() = runTest {
        // Arrange
        val rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, timestamp = 1000L),
            TestDataFactory.createRoundEntity(id = 2L, timestamp = 2000L)
        )
        database.rounds().insertAll(rounds)

        // Act
        val results = database.rounds().getRoundsByTimestampRange(10000L, 20000L)

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for range with no rounds", results.isEmpty())
    }

    @Test
    fun `getTotalPointsForSession calculates total correctly`() = runTest {
        // Arrange
        val session1Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, sessionId = 1L, points = 2),
            TestDataFactory.createRoundEntity(id = 2L, sessionId = 1L, points = 3),
            TestDataFactory.createRoundEntity(id = 3L, sessionId = 1L, points = 1)
        )

        val session2Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 4L, sessionId = 2L, points = 5),
            TestDataFactory.createRoundEntity(id = 5L, sessionId = 2L, points = -2)
        )

        val allRounds = session1Rounds + session2Rounds
        database.rounds().insertAll(allRounds)

        // Act
        val session1Total = database.rounds().getTotalPointsForSession(1L)
        val session2Total = database.rounds().getTotalPointsForSession(2L)

        // Assert
        assertEquals("Session 1 total should be 6", 6, session1Total)
        assertEquals("Session 2 total should be 3", 3, session2Total)
    }

    @Test
    fun `getTotalPointsForSession returns zero for session with no rounds`() = runTest {
        // Act
        val total = database.rounds().getTotalPointsForSession(999L)

        // Assert
        assertEquals("Total should be zero for session with no rounds", 0, total)
    }

    @Test
    fun `getAveragePointsForSession calculates average correctly`() = runTest {
        // Arrange
        val session1Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 1L, sessionId = 1L, points = 2),
            TestDataFactory.createRoundEntity(id = 2L, sessionId = 1L, points = 4),
            TestDataFactory.createRoundEntity(id = 3L, sessionId = 1L, points = 6)
        )

        val session2Rounds = listOf(
            TestDataFactory.createRoundEntity(id = 4L, sessionId = 2L, points = 10),
            TestDataFactory.createRoundEntity(id = 5L, sessionId = 2L, points = 20)
        )

        val allRounds = session1Rounds + session2Rounds
        database.rounds().insertAll(allRounds)

        // Act
        val session1Average = database.rounds().getAveragePointsForSession(1L)
        val session2Average = database.rounds().getAveragePointsForSession(2L)

        // Assert
        assertEquals("Session 1 average should be 4.0", 4.0, session1Average, 0.01)
        assertEquals("Session 2 average should be 15.0", 15.0, session2Average, 0.01)
    }

    @Test
    fun `getAveragePointsForSession returns zero for session with no rounds`() = runTest {
        // Act
        val average = database.rounds().getAveragePointsForSession(999L)

        // Assert
        assertEquals("Average should be 0.0 for session with no rounds", 0.0, average, 0.01)
    }
}