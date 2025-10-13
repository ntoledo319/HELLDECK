package com.helldeck.data.entities

import com.helldeck.data.GameSessionEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GameSessionEntity validation and behavior
 */
class GameSessionEntityTest {

    @Test
    fun `GameSessionEntity with valid data creates successfully`() {
        // Arrange & Act
        val session = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"CONFESSION_OR_CAP\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Session should be created", session)
        assertEquals("ID should match", 1L, session.id)
        assertEquals("Start time should match", 1000L, session.startTime)
        assertEquals("End time should match", 2000L, session.endTime)
        assertEquals("Player count should match", 4, session.playerCount)
        assertEquals("Rounds played should match", 10, session.roundsPlayed)
        assertEquals("Games played should match", "[\"ROAST_CONSENSUS\",\"CONFESSION_OR_CAP\"]", session.gamesPlayed)
        assertEquals("Total points should match", 25, session.totalPoints)
        assertEquals("Brainpack exported should match", 1, session.brainpackExported)
    }

    @Test
    fun `GameSessionEntity with minimum valid values creates successfully`() {
        // Arrange & Act
        val session = GameSessionEntity(
            id = 0L,
            startTime = 0L,
            endTime = null,
            playerCount = 0,
            roundsPlayed = 0,
            gamesPlayed = "[]",
            totalPoints = 0,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Minimal session should be created", session)
        assertEquals("Minimal ID should match", 0L, session.id)
        assertEquals("Minimal start time should match", 0L, session.startTime)
        assertEquals("Minimal end time should be null", null, session.endTime)
        assertEquals("Minimal player count should match", 0, session.playerCount)
        assertEquals("Minimal rounds played should match", 0, session.roundsPlayed)
        assertEquals("Minimal games played should match", "[]", session.gamesPlayed)
        assertEquals("Minimal total points should match", 0, session.totalPoints)
        assertEquals("Minimal brainpack exported should match", 0, session.brainpackExported)
    }

    @Test
    fun `GameSessionEntity with maximum valid values creates successfully`() {
        // Arrange & Act
        val session = GameSessionEntity(
            id = Long.MAX_VALUE,
            startTime = Long.MAX_VALUE,
            endTime = Long.MAX_VALUE,
            playerCount = 16,
            roundsPlayed = Int.MAX_VALUE,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"CONFESSION_OR_CAP\",\"POISON_PITCH\",\"FILL_IN_FINISHER\"]",
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Maximal session should be created", session)
        assertEquals("Maximal ID should match", Long.MAX_VALUE, session.id)
        assertEquals("Maximal start time should match", Long.MAX_VALUE, session.startTime)
        assertEquals("Maximal end time should match", Long.MAX_VALUE, session.endTime)
        assertEquals("Maximal player count should match", 16, session.playerCount)
        assertEquals("Maximal rounds played should match", Int.MAX_VALUE, session.roundsPlayed)
        assertEquals("Maximal total points should match", Int.MAX_VALUE, session.totalPoints)
        assertEquals("Maximal brainpack exported should match", 1, session.brainpackExported)
    }

    @Test
    fun `GameSessionEntity copy creates correct copy with modifications`() {
        // Arrange
        val originalSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 0
        )

        // Act
        val copiedSession = originalSession.copy(
            endTime = 3000L,
            playerCount = 6,
            roundsPlayed = 15,
            totalPoints = 35,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Copied session should not be null", copiedSession)
        assertEquals("ID should remain same", originalSession.id, copiedSession.id)
        assertEquals("Start time should remain same", originalSession.startTime, copiedSession.startTime)
        assertEquals("Games played should remain same", originalSession.gamesPlayed, copiedSession.gamesPlayed)

        assertEquals("End time should be updated", 3000L, copiedSession.endTime)
        assertEquals("Player count should be updated", 6, copiedSession.playerCount)
        assertEquals("Rounds played should be updated", 15, copiedSession.roundsPlayed)
        assertEquals("Total points should be updated", 35, copiedSession.totalPoints)
        assertEquals("Brainpack exported should be updated", 1, copiedSession.brainpackExported)

        assertNotEquals("Original and copy should not be same object", originalSession, copiedSession)
    }

    @Test
    fun `GameSessionEntity handles special characters correctly`() {
        // Arrange & Act
        val specialSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"spécial_gâmé_ñ\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Special session should be created", specialSession)
        assertEquals("Special games played should be preserved", "[\"ROAST_CONSENSUS\",\"spécial_gâmé_ñ\"]", specialSession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles unicode characters correctly`() {
        // Arrange & Act
        val unicodeSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"ünícódé_gâmé\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Unicode session should be created", unicodeSession)
        assertEquals("Unicode games played should be preserved", "[\"ROAST_CONSENSUS\",\"ünícódé_gâmé\"]", unicodeSession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles empty strings correctly`() {
        // Arrange & Act
        val emptySession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Empty session should be created", emptySession)
        assertEquals("Empty games played should be preserved", "", emptySession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles whitespace strings correctly`() {
        // Arrange & Act
        val whitespaceSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "   ",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Whitespace session should be created", whitespaceSession)
        assertEquals("Whitespace games played should be preserved", "   ", whitespaceSession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles numeric boundaries correctly`() {
        // Arrange & Act
        val boundarySession = GameSessionEntity(
            id = 0L,
            startTime = 0L,
            endTime = null,
            playerCount = 0,
            roundsPlayed = 0,
            gamesPlayed = "[]",
            totalPoints = 0,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Boundary session should be created", boundarySession)
        assertEquals("Zero ID should be preserved", 0L, boundarySession.id)
        assertEquals("Zero start time should be preserved", 0L, boundarySession.startTime)
        assertEquals("Zero player count should be preserved", 0, boundarySession.playerCount)
        assertEquals("Zero rounds played should be preserved", 0, boundarySession.roundsPlayed)
        assertEquals("Zero total points should be preserved", 0, boundarySession.totalPoints)
        assertEquals("Zero brainpack exported should be preserved", 0, boundarySession.brainpackExported)
    }

    @Test
    fun `GameSessionEntity handles very long games played correctly`() {
        // Arrange & Act
        val longGamesPlayed = List(50) { i -> "\"GAME_$i\"" }.joinToString(",", prefix = "[", postfix = "]")
        val longGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 8,
            roundsPlayed = 100,
            gamesPlayed = longGamesPlayed,
            totalPoints = 200,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Long games session should be created", longGamesSession)
        assertEquals("Long games played should be preserved", longGamesPlayed, longGamesSession.gamesPlayed)
        assertEquals("Games count should be 50", 50, longGamesSession.gamesPlayed.split(",").size - 1)
    }

    @Test
    fun `GameSessionEntity handles negative total points correctly`() {
        // Arrange & Act
        val negativePointsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = -5,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Negative points session should be created", negativePointsSession)
        assertEquals("Negative total points should be preserved", -5, negativePointsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles zero total points correctly`() {
        // Arrange & Act
        val zeroPointsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 0,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Zero points session should be created", zeroPointsSession)
        assertEquals("Zero total points should be preserved", 0, zeroPointsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles high total points correctly`() {
        // Arrange & Act
        val highPointsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 9999,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("High points session should be created", highPointsSession)
        assertEquals("High total points should be preserved", 9999, highPointsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles extremely high total points correctly`() {
        // Arrange & Act
        val extremePointsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Extreme points session should be created", extremePointsSession)
        assertEquals("Extreme total points should be preserved", Int.MAX_VALUE, extremePointsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles extremely low total points correctly`() {
        // Arrange & Act
        val extremeLowPointsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = Int.MIN_VALUE,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Extreme low points session should be created", extremeLowPointsSession)
        assertEquals("Extreme low total points should be preserved", Int.MIN_VALUE, extremeLowPointsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles minimum player count correctly`() {
        // Arrange & Act
        val minPlayersSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 3, // Minimum for HELLDECK
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Min players session should be created", minPlayersSession)
        assertEquals("Minimum player count should be preserved", 3, minPlayersSession.playerCount)
    }

    @Test
    fun `GameSessionEntity handles maximum player count correctly`() {
        // Arrange & Act
        val maxPlayersSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 16, // Maximum for HELLDECK
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Max players session should be created", maxPlayersSession)
        assertEquals("Maximum player count should be preserved", 16, maxPlayersSession.playerCount)
    }

    @Test
    fun `GameSessionEntity handles zero rounds played correctly`() {
        // Arrange & Act
        val zeroRoundsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 0,
            gamesPlayed = "[]",
            totalPoints = 0,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Zero rounds session should be created", zeroRoundsSession)
        assertEquals("Zero rounds played should be preserved", 0, zeroRoundsSession.roundsPlayed)
        assertEquals("Empty games played should be preserved", "[]", zeroRoundsSession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles high rounds played correctly`() {
        // Arrange & Act
        val highRoundsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 9999,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"CONFESSION_OR_CAP\"]",
            totalPoints = 25000,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("High rounds session should be created", highRoundsSession)
        assertEquals("High rounds played should be preserved", 9999, highRoundsSession.roundsPlayed)
        assertEquals("High total points should be preserved", 25000, highRoundsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles extremely high rounds played correctly`() {
        // Arrange & Act
        val extremeRoundsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = Int.MAX_VALUE,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"CONFESSION_OR_CAP\"]",
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Extreme rounds session should be created", extremeRoundsSession)
        assertEquals("Extreme rounds played should be preserved", Int.MAX_VALUE, extremeRoundsSession.roundsPlayed)
    }

    @Test
    fun `GameSessionEntity handles brainpack exported flag correctly`() {
        // Arrange & Act
        val exportedSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1 // Exported
        )

        val notExportedSession = GameSessionEntity(
            id = 2L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 0 // Not exported
        )

        // Assert
        assertNotNull("Exported session should be created", exportedSession)
        assertNotNull("Not exported session should be created", notExportedSession)
        assertEquals("Exported flag should be 1", 1, exportedSession.brainpackExported)
        assertEquals("Not exported flag should be 0", 0, notExportedSession.brainpackExported)
    }

    @Test
    fun `GameSessionEntity handles null end time correctly`() {
        // Arrange & Act
        val activeSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = null, // Session still active
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Active session should be created", activeSession)
        assertEquals("End time should be null for active session", null, activeSession.endTime)
    }

    @Test
    fun `GameSessionEntity handles future start time correctly`() {
        // Arrange & Act
        val futureStartSession = GameSessionEntity(
            id = 1L,
            startTime = System.currentTimeMillis() + 1000000,
            endTime = null,
            playerCount = 4,
            roundsPlayed = 0,
            gamesPlayed = "[]",
            totalPoints = 0,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Future start session should be created", futureStartSession)
        assertTrue("Start time should be in future", futureStartSession.startTime > System.currentTimeMillis())
    }

    @Test
    fun `GameSessionEntity handles past end time correctly`() {
        // Arrange & Act
        val pastEndSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Past end session should be created", pastEndSession)
        assertTrue("End time should be after start time", pastEndSession.endTime!! > pastEndSession.startTime)
    }

    @Test
    fun `GameSessionEntity handles end time before start time correctly`() {
        // Arrange & Act
        val invalidTimeSession = GameSessionEntity(
            id = 1L,
            startTime = 2000L,
            endTime = 1000L, // End before start
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Invalid time session should be created", invalidTimeSession)
        assertTrue("End time should be before start time", invalidTimeSession.endTime!! < invalidTimeSession.startTime)
    }

    @Test
    fun `GameSessionEntity handles malformed JSON in games played correctly`() {
        // Arrange & Act
        val malformedJsonSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",]", // Trailing comma
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Malformed JSON session should be created", malformedJsonSession)
        assertEquals("Malformed JSON should be preserved", "[\"ROAST_CONSENSUS\",]", malformedJsonSession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles empty JSON array in games played correctly`() {
        // Arrange & Act
        val emptyGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Empty games session should be created", emptyGamesSession)
        assertEquals("Empty games array should be preserved", "[]", emptyGamesSession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles single game in games played correctly`() {
        // Arrange & Act
        val singleGameSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Single game session should be created", singleGameSession)
        assertEquals("Single game should be preserved", "[\"ROAST_CONSENSUS\"]", singleGameSession.gamesPlayed)
    }

    @Test
    fun `GameSessionEntity handles many games in games played correctly`() {
        // Arrange & Act
        val manyGames = List(14) { i -> "\"GAME_${i + 1}\"" }.joinToString(",", prefix = "[", postfix = "]")
        val manyGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 8,
            roundsPlayed = 100,
            gamesPlayed = manyGames,
            totalPoints = 200,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Many games session should be created", manyGamesSession)
        assertEquals("Many games should be preserved", manyGames, manyGamesSession.gamesPlayed)
        assertEquals("Games count should be 14", 14, manyGamesSession.gamesPlayed.split(",").size - 1)
    }

    @Test
    fun `GameSessionEntity handles games with special characters correctly`() {
        // Arrange & Act
        val specialGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"spécial_gâmé_ñ\",\"ünícódé_gâmé\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Special games session should be created", specialGamesSession)
        assertTrue("Games should contain special characters", specialGamesSession.gamesPlayed.contains("spécial_gâmé_ñ"))
        assertTrue("Games should contain unicode characters", specialGamesSession.gamesPlayed.contains("ünícódé_gâmé"))
    }

    @Test
    fun `GameSessionEntity handles games with emojis correctly`() {
        // Arrange & Act
        val emojiGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"🎲_GAME\",\"🎯_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Emoji games session should be created", emojiGamesSession)
        assertTrue("Games should contain dice emoji", emojiGamesSession.gamesPlayed.contains("🎲_GAME"))
        assertTrue("Games should contain dart emoji", emojiGamesSession.gamesPlayed.contains("🎯_GAME"))
    }

    @Test
    fun `GameSessionEntity handles extremely long games played correctly`() {
        // Arrange & Act
        val extremelyLongGames = List(100) { i -> "\"EXTREMELY_LONG_GAME_NAME_$i\"" }.joinToString(",", prefix = "[", postfix = "]")
        val extremelyLongGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 16,
            roundsPlayed = 1000,
            gamesPlayed = extremelyLongGames,
            totalPoints = 5000,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Extremely long games session should be created", extremelyLongGamesSession)
        assertEquals("Extremely long games should be preserved", extremelyLongGames, extremelyLongGamesSession.gamesPlayed)
        assertEquals("Games count should be 100", 100, extremelyLongGamesSession.gamesPlayed.split(",").size - 1)
    }

    @Test
    fun `GameSessionEntity handles all edge case combinations correctly`() {
        // Arrange & Act
        val edgeCaseSession = GameSessionEntity(
            id = Long.MAX_VALUE,
            startTime = Long.MAX_VALUE,
            endTime = Long.MAX_VALUE,
            playerCount = 16,
            roundsPlayed = Int.MAX_VALUE,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"spécial_gâmé_ñ\",\"ünícódé_gâmé\",\"🎲_GAME\"]",
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Edge case session should be created", edgeCaseSession)
        assertEquals("ID should be maximum", Long.MAX_VALUE, edgeCaseSession.id)
        assertEquals("Start time should be maximum", Long.MAX_VALUE, edgeCaseSession.startTime)
        assertEquals("End time should be maximum", Long.MAX_VALUE, edgeCaseSession.endTime)
        assertEquals("Player count should be maximum", 16, edgeCaseSession.playerCount)
        assertEquals("Rounds played should be maximum", Int.MAX_VALUE, edgeCaseSession.roundsPlayed)
        assertEquals("Total points should be maximum", Int.MAX_VALUE, edgeCaseSession.totalPoints)
        assertEquals("Brainpack exported should be maximum", 1, edgeCaseSession.brainpackExported)

        assertTrue("Games should contain normal game", edgeCaseSession.gamesPlayed.contains("ROAST_CONSENSUS"))
        assertTrue("Games should contain special characters", edgeCaseSession.gamesPlayed.contains("spécial_gâmé_ñ"))
        assertTrue("Games should contain unicode", edgeCaseSession.gamesPlayed.contains("ünícódé_gâmé"))
        assertTrue("Games should contain emoji", edgeCaseSession.gamesPlayed.contains("🎲_GAME"))
    }

    @Test
    fun `GameSessionEntity equality works correctly`() {
        // Arrange
        val session1 = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        val session2 = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        val session3 = GameSessionEntity(
            id = 2L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertEquals("Identical sessions should be equal", session1, session2)
        assertNotEquals("Different sessions should not be equal", session1, session3)
        assertEquals("Hash codes should be equal for identical sessions", session1.hashCode(), session2.hashCode())
        assertTrue("Hash codes should be different for different sessions", session1.hashCode() != session3.hashCode())
    }

    @Test
    fun `GameSessionEntity toString contains relevant information`() {
        // Arrange
        val session = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Act
        val toString = session.toString()

        // Assert
        assertNotNull("toString should not be null", toString)
        assertTrue("toString should contain ID", toString.contains("id=1"))
        assertTrue("toString should contain start time", toString.contains("startTime=1000"))
        assertTrue("toString should contain end time", toString.contains("endTime=2000"))
        assertTrue("toString should contain player count", toString.contains("playerCount=4"))
        assertTrue("toString should contain rounds played", toString.contains("roundsPlayed=10"))
        assertTrue("toString should contain games played", toString.contains("ROAST_CONSENSUS"))
        assertTrue("toString should contain total points", toString.contains("totalPoints=25"))
        assertTrue("toString should contain brainpack exported", toString.contains("brainpackExported=1"))
    }

    @Test
    fun `GameSessionEntity handles all boundary conditions correctly`() {
        // Arrange & Act
        val boundarySession = GameSessionEntity(
            id = 0L,
            startTime = 0L,
            endTime = null,
            playerCount = 0,
            roundsPlayed = 0,
            gamesPlayed = "[]",
            totalPoints = 0,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Boundary session should be created", boundarySession)
        assertEquals("Zero ID should be preserved", 0L, boundarySession.id)
        assertEquals("Zero start time should be preserved", 0L, boundarySession.startTime)
        assertEquals("Null end time should be preserved", null, boundarySession.endTime)
        assertEquals("Zero player count should be preserved", 0, boundarySession.playerCount)
        assertEquals("Zero rounds played should be preserved", 0, boundarySession.roundsPlayed)
        assertEquals("Empty games played should be preserved", "[]", boundarySession.gamesPlayed)
        assertEquals("Zero total points should be preserved", 0, boundarySession.totalPoints)
        assertEquals("Zero brainpack exported should be preserved", 0, boundarySession.brainpackExported)
    }

    @Test
    fun `GameSessionEntity handles all maximum boundary conditions correctly`() {
        // Arrange & Act
        val maxBoundarySession = GameSessionEntity(
            id = Long.MAX_VALUE,
            startTime = Long.MAX_VALUE,
            endTime = Long.MAX_VALUE,
            playerCount = 16,
            roundsPlayed = Int.MAX_VALUE,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"CONFESSION_OR_CAP\",\"POISON_PITCH\"]",
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Maximum boundary session should be created", maxBoundarySession)
        assertEquals("Maximum ID should be preserved", Long.MAX_VALUE, maxBoundarySession.id)
        assertEquals("Maximum start time should be preserved", Long.MAX_VALUE, maxBoundarySession.startTime)
        assertEquals("Maximum end time should be preserved", Long.MAX_VALUE, maxBoundarySession.endTime)
        assertEquals("Maximum player count should be preserved", 16, maxBoundarySession.playerCount)
        assertEquals("Maximum rounds played should be preserved", Int.MAX_VALUE, maxBoundarySession.roundsPlayed)
        assertEquals("Maximum total points should be preserved", Int.MAX_VALUE, maxBoundarySession.totalPoints)
        assertEquals("Maximum brainpack exported should be preserved", 1, maxBoundarySession.brainpackExported)
    }

    @Test
    fun `GameSessionEntity handles all minimum boundary conditions correctly`() {
        // Arrange & Act
        val minBoundarySession = GameSessionEntity(
            id = Long.MIN_VALUE,
            startTime = Long.MIN_VALUE,
            endTime = Long.MIN_VALUE,
            playerCount = 0,
            roundsPlayed = Int.MIN_VALUE,
            gamesPlayed = "[]",
            totalPoints = Int.MIN_VALUE,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Minimum boundary session should be created", minBoundarySession)
        assertEquals("Minimum ID should be preserved", Long.MIN_VALUE, minBoundarySession.id)
        assertEquals("Minimum start time should be preserved", Long.MIN_VALUE, minBoundarySession.startTime)
        assertEquals("Minimum end time should be preserved", Long.MIN_VALUE, minBoundarySession.endTime)
        assertEquals("Minimum rounds played should be preserved", Int.MIN_VALUE, minBoundarySession.roundsPlayed)
        assertEquals("Minimum total points should be preserved", Int.MIN_VALUE, minBoundarySession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles future end time correctly`() {
        // Arrange & Act
        val futureEndSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = System.currentTimeMillis() + 1000000,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Future end session should be created", futureEndSession)
        assertTrue("End time should be in future", futureEndSession.endTime!! > System.currentTimeMillis())
        assertTrue("End time should be after start time", futureEndSession.endTime!! > futureEndSession.startTime)
    }

    @Test
    fun `GameSessionEntity handles extremely large player count correctly`() {
        // Arrange & Act
        val largePlayerCountSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 100, // More than typical maximum
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Large player count session should be created", largePlayerCountSession)
        assertEquals("Large player count should be preserved", 100, largePlayerCountSession.playerCount)
    }

    @Test
    fun `GameSessionEntity handles extremely small player count correctly`() {
        // Arrange & Act
        val smallPlayerCountSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 1, // Less than typical minimum
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Small player count session should be created", smallPlayerCountSession)
        assertEquals("Small player count should be preserved", 1, smallPlayerCountSession.playerCount)
    }

    @Test
    fun `GameSessionEntity handles extremely large rounds played correctly`() {
        // Arrange & Act
        val largeRoundsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10000,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25000,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Large rounds session should be created", largeRoundsSession)
        assertEquals("Large rounds played should be preserved", 10000, largeRoundsSession.roundsPlayed)
        assertEquals("Large total points should be preserved", 25000, largeRoundsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles extremely small rounds played correctly`() {
        // Arrange & Act
        val smallRoundsSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = -1, // Negative rounds don't make sense but test boundary
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = -5,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Small rounds session should be created", smallRoundsSession)
        assertEquals("Small rounds played should be preserved", -1, smallRoundsSession.roundsPlayed)
        assertEquals("Small total points should be preserved", -5, smallRoundsSession.totalPoints)
    }

    @Test
    fun `GameSessionEntity handles brainpack exported with invalid values correctly`() {
        // Arrange & Act
        val invalidBrainpackSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 2 // Invalid value (should be 0 or 1)
        )

        // Assert
        assertNotNull("Invalid brainpack session should be created", invalidBrainpackSession)
        assertEquals("Invalid brainpack exported should be preserved", 2, invalidBrainpackSession.brainpackExported)
    }

    @Test
    fun `GameSessionEntity handles extremely large start time correctly`() {
        // Arrange & Act
        val largeStartTimeSession = GameSessionEntity(
            id = 1L,
            startTime = Long.MAX_VALUE,
            endTime = Long.MAX_VALUE,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Large start time session should be created", largeStartTimeSession)
        assertEquals("Large start time should be preserved", Long.MAX_VALUE, largeStartTimeSession.startTime)
    }

    @Test
    fun `GameSessionEntity handles extremely small start time correctly`() {
        // Arrange & Act
        val smallStartTimeSession = GameSessionEntity(
            id = 1L,
            startTime = Long.MIN_VALUE,
            endTime = Long.MIN_VALUE,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Small start time session should be created", smallStartTimeSession)
        assertEquals("Small start time should be preserved", Long.MIN_VALUE, smallStartTimeSession.startTime)
    }

    @Test
    fun `GameSessionEntity handles extremely large end time correctly`() {
        // Arrange & Act
        val largeEndTimeSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = Long.MAX_VALUE,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Large end time session should be created", largeEndTimeSession)
        assertEquals("Large end time should be preserved", Long.MAX_VALUE, largeEndTimeSession.endTime)
    }

    @Test
    fun `GameSessionEntity handles extremely small end time correctly`() {
        // Arrange & Act
        val smallEndTimeSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = Long.MIN_VALUE,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Small end time session should be created", smallEndTimeSession)
        assertEquals("Small end time should be preserved", Long.MIN_VALUE, smallEndTimeSession.endTime)
    }

    @Test
    fun `GameSessionEntity handles extremely large ID correctly`() {
        // Arrange & Act
        val largeIdSession = GameSessionEntity(
            id = Long.MAX_VALUE,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Large ID session should be created", largeIdSession)
        assertEquals("Large ID should be preserved", Long.MAX_VALUE, largeIdSession.id)
    }

    @Test
    fun `GameSessionEntity handles extremely small ID correctly`() {
        // Arrange & Act
        val smallIdSession = GameSessionEntity(
            id = Long.MIN_VALUE,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Small ID session should be created", smallIdSession)
        assertEquals("Small ID should be preserved", Long.MIN_VALUE, smallIdSession.id)
    }

    @Test
    fun `GameSessionEntity handles all edge case combinations correctly`() {
        // Arrange & Act
        val edgeCaseSession = GameSessionEntity(
            id = Long.MAX_VALUE,
            startTime = Long.MAX_VALUE,
            endTime = Long.MAX_VALUE,
            playerCount = 16,
            roundsPlayed = Int.MAX_VALUE,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"spécial_gâmé_ñ\",\"ünícódé_gâmé\",\"🎲_GAME\"]",
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Edge case session should be created", edgeCaseSession)
        assertEquals("ID should be maximum", Long.MAX_VALUE, edgeCaseSession.id)
        assertEquals("Start time should be maximum", Long.MAX_VALUE, edgeCaseSession.startTime)
        assertEquals("End time should be maximum", Long.MAX_VALUE, edgeCaseSession.endTime)
        assertEquals("Player count should be maximum", 16, edgeCaseSession.playerCount)
        assertEquals("Rounds played should be maximum", Int.MAX_VALUE, edgeCaseSession.roundsPlayed)
        assertEquals("Total points should be maximum", Int.MAX_VALUE, edgeCaseSession.totalPoints)
        assertEquals("Brainpack exported should be maximum", 1, edgeCaseSession.brainpackExported)

        assertTrue("Games should contain normal game", edgeCaseSession.gamesPlayed.contains("ROAST_CONSENSUS"))
        assertTrue("Games should contain special characters", edgeCaseSession.gamesPlayed.contains("spécial_gâmé_ñ"))
        assertTrue("Games should contain unicode", edgeCaseSession.gamesPlayed.contains("ünícódé_gâmé"))
        assertTrue("Games should contain emoji", edgeCaseSession.gamesPlayed.contains("🎲_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with extremely complex content correctly`() {
        // Arrange & Act
        val extremelyComplexGames = "[\"ROAST_CONSENSUS\",\"spécial_gâmé_ñ\",\"ünícódé_gâmé\",\"🎲_GAME\",\"∑_GAME\",\"∆_GAME\",\"π_GAME\",\"∞_GAME\"]"
        val extremelyComplexSession = GameSessionEntity(
            id = Long.MAX_VALUE,
            startTime = Long.MAX_VALUE,
            endTime = Long.MAX_VALUE,
            playerCount = 16,
            roundsPlayed = Int.MAX_VALUE,
            gamesPlayed = extremelyComplexGames,
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Extremely complex session should be created", extremelyComplexSession)
        assertEquals("Games should be extremely complex", extremelyComplexGames, extremelyComplexSession.gamesPlayed)

        assertTrue("Games should contain normal game", extremelyComplexSession.gamesPlayed.contains("ROAST_CONSENSUS"))
        assertTrue("Games should contain special characters", extremelyComplexSession.gamesPlayed.contains("spécial_gâmé_ñ"))
        assertTrue("Games should contain unicode", extremelyComplexSession.gamesPlayed.contains("ünícódé_gâmé"))
        assertTrue("Games should contain emoji", extremelyComplexSession.gamesPlayed.contains("🎲_GAME"))
        assertTrue("Games should contain sum symbol", extremelyComplexSession.gamesPlayed.contains("∑_GAME"))
        assertTrue("Games should contain delta symbol", extremelyComplexSession.gamesPlayed.contains("∆_GAME"))
        assertTrue("Games should contain pi symbol", extremelyComplexSession.gamesPlayed.contains("π_GAME"))
        assertTrue("Games should contain infinity symbol", extremelyComplexSession.gamesPlayed.contains("∞_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with zero-width joiner emojis correctly`() {
        // Arrange & Act
        val zwjEmojiGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"👨‍💻_GAME\",\"👩‍🎨_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("ZWJ emoji games session should be created", zwjEmojiGamesSession)
        assertTrue("Games should contain programmer emoji", zwjEmojiGamesSession.gamesPlayed.contains("👨‍💻_GAME"))
        assertTrue("Games should contain artist emoji", zwjEmojiGamesSession.gamesPlayed.contains("👩‍🎨_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with skin tone modifiers correctly`() {
        // Arrange & Act
        val skinToneGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"👋🏻_GAME\",\"👋🏼_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Skin tone games session should be created", skinToneGamesSession)
        assertTrue("Games should contain light skin tone", skinToneGamesSession.gamesPlayed.contains("👋🏻_GAME"))
        assertTrue("Games should contain medium-light skin tone", skinToneGamesSession.gamesPlayed.contains("👋🏼_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with country flags correctly`() {
        // Arrange & Act
        val countryFlagsGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"🇺🇸_GAME\",\"🇬🇧_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Country flags games session should be created", countryFlagsGamesSession)
        assertTrue("Games should contain US flag", countryFlagsGamesSession.gamesPlayed.contains("🇺🇸_GAME"))
        assertTrue("Games should contain UK flag", countryFlagsGamesSession.gamesPlayed.contains("🇬🇧_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with astronomical symbols correctly`() {
        // Arrange & Act
        val astronomicalGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"☀️_GAME\",\"🌙_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Astronomical games session should be created", astronomicalGamesSession)
        assertTrue("Games should contain sun", astronomicalGamesSession.gamesPlayed.contains("☀️_GAME"))
        assertTrue("Games should contain moon", astronomicalGamesSession.gamesPlayed.contains("🌙_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with musical notation correctly`() {
        // Arrange & Act
        val musicalGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"♪_GAME\",\"♫_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Musical games session should be created", musicalGamesSession)
        assertTrue("Games should contain eighth note", musicalGamesSession.gamesPlayed.contains("♪_GAME"))
        assertTrue("Games should contain beamed notes", musicalGamesSession.gamesPlayed.contains("♫_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with chess symbols correctly`() {
        // Arrange & Act
        val chessGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"♔_GAME\",\"♕_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Chess games session should be created", chessGamesSession)
        assertTrue("Games should contain white king", chessGamesSession.gamesPlayed.contains("♔_GAME"))
        assertTrue("Games should contain white queen", chessGamesSession.gamesPlayed.contains("♕_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with card symbols correctly`() {
        // Arrange & Act
        val cardGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"♠_GAME\",\"♥_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Card games session should be created", cardGamesSession)
        assertTrue("Games should contain spade", cardGamesSession.gamesPlayed.contains("♠_GAME"))
        assertTrue("Games should contain heart", cardGamesSession.gamesPlayed.contains("♥_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with dice and game symbols correctly`() {
        // Arrange & Act
        val gameSymbolsGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"🎲_GAME\",\"🎯_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Game symbols games session should be created", gameSymbolsGamesSession)
        assertTrue("Games should contain dice", gameSymbolsGamesSession.gamesPlayed.contains("🎲_GAME"))
        assertTrue("Games should contain dart", gameSymbolsGamesSession.gamesPlayed.contains("🎯_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with weather symbols correctly`() {
        // Arrange & Act
        val weatherGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"☀️_GAME\",\"🌧️_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Weather games session should be created", weatherGamesSession)
        assertTrue("Games should contain sun", weatherGamesSession.gamesPlayed.contains("☀️_GAME"))
        assertTrue("Games should contain rain", weatherGamesSession.gamesPlayed.contains("🌧️_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with food and drink symbols correctly`() {
        // Arrange & Act
        val foodGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"🍕_GAME\",\"🍔_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Food games session should be created", foodGamesSession)
        assertTrue("Games should contain pizza", foodGamesSession.gamesPlayed.contains("🍕_GAME"))
        assertTrue("Games should contain burger", foodGamesSession.gamesPlayed.contains("🍔_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with activity symbols correctly`() {
        // Arrange & Act
        val activityGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"⚽_GAME\",\"🏀_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Activity games session should be created", activityGamesSession)
        assertTrue("Games should contain soccer ball", activityGamesSession.gamesPlayed.contains("⚽_GAME"))
        assertTrue("Games should contain basketball", activityGamesSession.gamesPlayed.contains("🏀_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with travel and place symbols correctly`() {
        // Arrange & Act
        val travelGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"🚗_GAME\",\"✈️_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Travel games session should be created", travelGamesSession)
        assertTrue("Games should contain car", travelGamesSession.gamesPlayed.contains("🚗_GAME"))
        assertTrue("Games should contain airplane", travelGamesSession.gamesPlayed.contains("✈️_GAME"))
    }

    @Test
    fun `GameSessionEntity handles games with symbol combinations correctly`() {
        // Arrange & Act
        val symbolComboGamesSession = GameSessionEntity(
            id = 1L,
            startTime = 1000L,
            endTime = 2000L,
            playerCount = 4,
            roundsPlayed = 10,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"👨‍💻_GAME\",\"👩‍🎨_GAME\",\"🚀_GAME\"]",
            totalPoints = 25,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Symbol combo games session should be created", symbolComboGamesSession)
        assertTrue("Games should contain programmer", symbolComboGamesSession.gamesPlayed.contains("👨‍💻_GAME"))
        assertTrue("Games should contain artist", symbolComboGamesSession.gamesPlayed.contains("👩‍🎨_GAME"))
        assertTrue("Games should contain rocket", symbolComboGamesSession.gamesPlayed.contains("🚀_GAME"))
    }

    @Test
    fun `GameSessionEntity handles all boundary conditions correctly`() {
        // Arrange & Act
        val boundarySession = GameSessionEntity(
            id = 0L,
            startTime = 0L,
            endTime = null,
            playerCount = 0,
            roundsPlayed = 0,
            gamesPlayed = "[]",
            totalPoints = 0,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Boundary session should be created", boundarySession)
        assertEquals("Zero ID should be preserved", 0L, boundarySession.id)
        assertEquals("Zero start time should be preserved", 0L, boundarySession.startTime)
        assertEquals("Null end time should be preserved", null, boundarySession.endTime)
        assertEquals("Zero player count should be preserved", 0, boundarySession.playerCount)
        assertEquals("Zero rounds played should be preserved", 0, boundarySession.roundsPlayed)
        assertEquals("Empty games played should be preserved", "[]", boundarySession.gamesPlayed)
        assertEquals("Zero total points should be preserved", 0, boundarySession.totalPoints)
        assertEquals("Zero brainpack exported should be preserved", 0, boundarySession.brainpackExported)
    }

    @Test
    fun `GameSessionEntity handles all maximum boundary conditions correctly`() {
        // Arrange & Act
        val maxBoundarySession = GameSessionEntity(
            id = Long.MAX_VALUE,
            startTime = Long.MAX_VALUE,
            endTime = Long.MAX_VALUE,
            playerCount = 16,
            roundsPlayed = Int.MAX_VALUE,
            gamesPlayed = "[\"ROAST_CONSENSUS\",\"CONFESSION_OR_CAP\",\"POISON_PITCH\"]",
            totalPoints = Int.MAX_VALUE,
            brainpackExported = 1
        )

        // Assert
        assertNotNull("Maximum boundary session should be created", maxBoundarySession)
        assertEquals("Maximum ID should be preserved", Long.MAX_VALUE, maxBoundarySession.id)
        assertEquals("Maximum start time should be preserved", Long.MAX_VALUE, maxBoundarySession.startTime)
        assertEquals("Maximum end time should be preserved", Long.MAX_VALUE, maxBoundarySession.endTime)
        assertEquals("Maximum player count should be preserved", 16, maxBoundarySession.playerCount)
        assertEquals("Maximum rounds played should be preserved", Int.MAX_VALUE, maxBoundarySession.roundsPlayed)
        assertEquals("Maximum total points should be preserved", Int.MAX_VALUE, maxBoundarySession.totalPoints)
        assertEquals("Maximum brainpack exported should be preserved", 1, maxBoundarySession.brainpackExported)
    }

    @Test
    fun `GameSessionEntity handles all minimum boundary conditions correctly`() {
        // Arrange & Act
        val minBoundarySession = GameSessionEntity(
            id = Long.MIN_VALUE,
            startTime = Long.MIN_VALUE,
            endTime = Long.MIN_VALUE,
            playerCount = 0,
            roundsPlayed = Int.MIN_VALUE,
            gamesPlayed = "[]",
            totalPoints = Int.MIN_VALUE,
            brainpackExported = 0
        )

        // Assert
        assertNotNull("Minimum boundary session should be created", minBoundarySession)
        assertEquals("Minimum ID should be preserved", Long.MIN_VALUE, minBoundarySession.id)
        assertEquals("Minimum start time should be preserved", Long.MIN_VALUE, minBoundarySession.startTime)
        assertEquals("Minimum end time should be preserved", Long.MIN_VALUE, minBoundarySession.endTime)
        assertEquals("Minimum rounds played should be preserved", Int.MIN_VALUE, minBoundarySession.roundsPlayed)
        assertEquals("Minimum total points should be preserved", Int.MIN_VALUE, minBoundarySession.totalPoints)
    }
}