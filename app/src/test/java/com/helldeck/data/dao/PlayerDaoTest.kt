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
 * Comprehensive unit tests for PlayerDao
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PlayerDaoTest : DatabaseTest() {

    @Test
    fun `insert inserts player correctly`() = runTest {
        // Arrange
        val player = TestDataFactory.createPlayerEntity(
            id = "test_player_1",
            name = "Test Player",
            avatar = "😀",
            sessionPoints = 10
        )

        // Act
        database.players().insert(player)

        // Assert
        val retrieved = database.players().byId(player.id)
        assertNotNull("Player should be retrievable", retrieved)
        assertEquals("Player ID should match", player.id, retrieved?.id)
        assertEquals("Player name should match", player.name, retrieved?.name)
        assertEquals("Player avatar should match", player.avatar, retrieved?.avatar)
        assertEquals("Player sessionPoints should match", player.sessionPoints, retrieved?.sessionPoints)
    }

    @Test
    fun `insertAll inserts multiple players correctly`() = runTest {
        // Arrange
        val players = TestDataFactory.createPlayerEntityList(5)

        // Act
        database.players().insertAll(players)

        // Assert
        val allPlayers = database.players().getAll()
        assertEquals("Should have all players", 5, allPlayers.size)

        players.forEach { expectedPlayer ->
            val found = allPlayers.find { it.id == expectedPlayer.id }
            assertNotNull("Player should be found: ${expectedPlayer.id}", found)
            assertEquals("Player name should match", expectedPlayer.name, found?.name)
        }
    }

    @Test
    fun `byId returns null for non-existent player`() = runTest {
        // Act
        val player = database.players().byId("non_existent_id")

        // Assert
        assertNull("Should return null for non-existent player", player)
    }

    @Test
    fun `getAll returns all players correctly`() = runTest {
        // Arrange
        val players = TestDataFactory.createPlayerEntityList(4)
        database.players().insertAll(players)

        // Act
        val allPlayers = database.players().getAll()

        // Assert
        assertEquals("Should return all players", 4, allPlayers.size)
        players.forEach { expectedPlayer ->
            assertTrue("All players should be present",
                allPlayers.any { it.id == expectedPlayer.id })
        }
    }

    @Test
    fun `getAll returns empty list when no players exist`() = runTest {
        // Act
        val allPlayers = database.players().getAll()

        // Assert
        assertNotNull("Results should not be null", allPlayers)
        assertTrue("Results should be empty when no players exist", allPlayers.isEmpty())
    }

    @Test
    fun `update updates player correctly`() = runTest {
        // Arrange
        val originalPlayer = TestDataFactory.createPlayerEntity(
            id = "update_test",
            name = "Original Name",
            avatar = "😀",
            sessionPoints = 5
        )
        database.players().insert(originalPlayer)

        val updatedPlayer = originalPlayer.copy(
            name = "Updated Name",
            avatar = "🤠",
            sessionPoints = 15
        )

        // Act
        database.players().update(updatedPlayer)

        // Assert
        val retrieved = database.players().byId(originalPlayer.id)
        assertNotNull("Player should still exist", retrieved)
        assertEquals("Name should be updated", "Updated Name", retrieved?.name)
        assertEquals("Avatar should be updated", "🤠", retrieved?.avatar)
        assertEquals("SessionPoints should be updated", 15, retrieved?.sessionPoints)
    }

    @Test
    fun `delete removes player correctly`() = runTest {
        // Arrange
        val playerToDelete = TestDataFactory.createPlayerEntity(id = "delete_test")
        val playerToKeep = TestDataFactory.createPlayerEntity(id = "keep_test")

        database.players().insert(playerToDelete)
        database.players().insert(playerToKeep)

        // Act
        database.players().delete(playerToDelete)

        // Assert
        val deletedPlayer = database.players().byId("delete_test")
        val keptPlayer = database.players().byId("keep_test")

        assertNull("Deleted player should not exist", deletedPlayer)
        assertNotNull("Kept player should still exist", keptPlayer)
    }

    @Test
    fun `insert handles duplicate IDs correctly with REPLACE strategy`() = runTest {
        // Arrange
        val originalPlayer = TestDataFactory.createPlayerEntity(
            id = "duplicate_test",
            name = "Original Name",
            sessionPoints = 5
        )

        val duplicatePlayer = TestDataFactory.createPlayerEntity(
            id = "duplicate_test",
            name = "Duplicate Name",
            sessionPoints = 10
        )

        // Act
        database.players().insert(originalPlayer)
        database.players().insert(duplicatePlayer) // Should replace

        // Assert
        val retrieved = database.players().byId("duplicate_test")
        assertNotNull("Player should exist", retrieved)
        assertEquals("Should have duplicate player data", "Duplicate Name", retrieved?.name)
        assertEquals("Should have duplicate player sessionPoints", 10, retrieved?.sessionPoints)
    }

    @Test
    fun `getPlayerCount returns correct count`() = runTest {
        // Arrange
        val players = TestDataFactory.createPlayerEntityList(7)
        database.players().insertAll(players)

        // Act
        val count = database.players().getPlayerCount()

        // Assert
        assertEquals("Count should match inserted players", 7, count)
    }

    @Test
    fun `getPlayerCount returns zero when no players exist`() = runTest {
        // Act
        val count = database.players().getPlayerCount()

        // Assert
        assertEquals("Count should be zero when no players exist", 0, count)
    }

    @Test
    fun `insert handles players with special characters correctly`() = runTest {
        // Arrange
        val specialPlayer = TestDataFactory.createPlayerEntity(
            id = "special_chars_test",
            name = "José María García 🚀",
            avatar = "🌟"
        )

        // Act
        database.players().insert(specialPlayer)

        // Assert
        val retrieved = database.players().byId("special_chars_test")
        assertNotNull("Player with special characters should be stored", retrieved)
        assertEquals("Special characters should be preserved",
            "José María García 🚀", retrieved?.name)
        assertEquals("Special avatar should be preserved", "🌟", retrieved?.avatar)
    }

    @Test
    fun `insert handles very long player names correctly`() = runTest {
        // Arrange
        val longName = "A".repeat(100) + " Very Long Name " + "B".repeat(100)
        val longNamePlayer = TestDataFactory.createPlayerEntity(
            id = "long_name_test",
            name = longName
        )

        // Act
        database.players().insert(longNamePlayer)

        // Assert
        val retrieved = database.players().byId("long_name_test")
        assertNotNull("Player with long name should be stored", retrieved)
        assertEquals("Long name should be preserved", longName, retrieved?.name)
    }

    @Test
    fun `insert handles players with unicode avatars correctly`() = runTest {
        // Arrange
        val unicodeAvatars = listOf("🚀", "🌟", "🎯", "🎲", "🎪", "🎨", "🎭", "🎪")
        val players = unicodeAvatars.mapIndexed { index, avatar ->
            TestDataFactory.createPlayerEntity(
                id = "unicode_avatar_$index",
                name = "Player $index",
                avatar = avatar
            )
        }

        // Act
        database.players().insertAll(players)

        // Assert
        unicodeAvatars.forEachIndexed { index, expectedAvatar ->
            val retrieved = database.players().byId("unicode_avatar_$index")
            assertNotNull("Player with unicode avatar should be stored", retrieved)
            assertEquals("Unicode avatar should be preserved", expectedAvatar, retrieved?.avatar)
        }
    }

    @Test
    fun `insert handles players with zero session points correctly`() = runTest {
        // Arrange
        val zeroPointsPlayer = TestDataFactory.createPlayerEntity(
            id = "zero_points_test",
            name = "Zero Points Player",
            sessionPoints = 0
        )

        // Act
        database.players().insert(zeroPointsPlayer)

        // Assert
        val retrieved = database.players().byId("zero_points_test")
        assertNotNull("Player with zero points should be stored", retrieved)
        assertEquals("Zero points should be preserved", 0, retrieved?.sessionPoints)
    }

    @Test
    fun `insert handles players with high session points correctly`() = runTest {
        // Arrange
        val highPointsPlayer = TestDataFactory.createPlayerEntity(
            id = "high_points_test",
            name = "High Points Player",
            sessionPoints = 9999
        )

        // Act
        database.players().insert(highPointsPlayer)

        // Assert
        val retrieved = database.players().byId("high_points_test")
        assertNotNull("Player with high points should be stored", retrieved)
        assertEquals("High points should be preserved", 9999, retrieved?.sessionPoints)
    }

    @Test
    fun `insert handles players with negative session points correctly`() = runTest {
        // Arrange
        val negativePointsPlayer = TestDataFactory.createPlayerEntity(
            id = "negative_points_test",
            name = "Negative Points Player",
            sessionPoints = -5
        )

        // Act
        database.players().insert(negativePointsPlayer)

        // Assert
        val retrieved = database.players().byId("negative_points_test")
        assertNotNull("Player with negative points should be stored", retrieved)
        assertEquals("Negative points should be preserved", -5, retrieved?.sessionPoints)
    }

    @Test
    fun `insert handles players with empty name correctly`() = runTest {
        // Arrange
        val emptyNamePlayer = TestDataFactory.createPlayerEntity(
            id = "empty_name_test",
            name = "",
            avatar = "😀"
        )

        // Act
        database.players().insert(emptyNamePlayer)

        // Assert
        val retrieved = database.players().byId("empty_name_test")
        assertNotNull("Player with empty name should be stored", retrieved)
        assertEquals("Empty name should be preserved", "", retrieved?.name)
    }

    @Test
    fun `insert handles players with empty avatar correctly`() = runTest {
        // Arrange
        val emptyAvatarPlayer = TestDataFactory.createPlayerEntity(
            id = "empty_avatar_test",
            name = "No Avatar Player",
            avatar = ""
        )

        // Act
        database.players().insert(emptyAvatarPlayer)

        // Assert
        val retrieved = database.players().byId("empty_avatar_test")
        assertNotNull("Player with empty avatar should be stored", retrieved)
        assertEquals("Empty avatar should be preserved", "", retrieved?.avatar)
    }

    @Test
    fun `insert handles players with minimum valid values correctly`() = runTest {
        // Arrange
        val minimalPlayer = TestDataFactory.createPlayerEntity(
            id = "minimal_test",
            name = "A",
            avatar = "A",
            sessionPoints = 0
        )

        // Act
        database.players().insert(minimalPlayer)

        // Assert
        val retrieved = database.players().byId("minimal_test")
        assertNotNull("Minimal player should be stored", retrieved)
        assertEquals("Minimal name should be preserved", "A", retrieved?.name)
        assertEquals("Minimal avatar should be preserved", "A", retrieved?.avatar)
        assertEquals("Minimal sessionPoints should be preserved", 0, retrieved?.sessionPoints)
    }

    @Test
    fun `insert handles players with maximum valid values correctly`() = runTest {
        // Arrange
        val maximalPlayer = TestDataFactory.createPlayerEntity(
            id = "maximal_test",
            name = "A".repeat(100),
            avatar = "🚀",
            sessionPoints = Int.MAX_VALUE
        )

        // Act
        database.players().insert(maximalPlayer)

        // Assert
        val retrieved = database.players().byId("maximal_test")
        assertNotNull("Maximal player should be stored", retrieved)
        assertEquals("Maximal name should be preserved", "A".repeat(100), retrieved?.name)
        assertEquals("Maximal avatar should be preserved", "🚀", retrieved?.avatar)
        assertEquals("Maximal sessionPoints should be preserved", Int.MAX_VALUE, retrieved?.sessionPoints)
    }

    @Test
    fun `getPlayersBySessionPoints returns players in correct order`() = runTest {
        // Arrange
        val players = listOf(
            TestDataFactory.createPlayerEntity(id = "player_100", name = "Player 100", sessionPoints = 100),
            TestDataFactory.createPlayerEntity(id = "player_50", name = "Player 50", sessionPoints = 50),
            TestDataFactory.createPlayerEntity(id = "player_200", name = "Player 200", sessionPoints = 200),
            TestDataFactory.createPlayerEntity(id = "player_0", name = "Player 0", sessionPoints = 0)
        )
        database.players().insertAll(players)

        // Act
        val orderedPlayers = database.players().getPlayersBySessionPoints()

        // Assert
        assertEquals("Should return all players", 4, orderedPlayers.size)

        // Should be ordered by sessionPoints descending
        assertEquals("Highest points should be first", 200, orderedPlayers[0].sessionPoints)
        assertEquals("Second highest should be second", 100, orderedPlayers[1].sessionPoints)
        assertEquals("Third highest should be third", 50, orderedPlayers[2].sessionPoints)
        assertEquals("Lowest points should be last", 0, orderedPlayers[3].sessionPoints)
    }

    @Test
    fun `getPlayersBySessionPoints handles ties correctly`() = runTest {
        // Arrange
        val players = listOf(
            TestDataFactory.createPlayerEntity(id = "player_50_a", name = "Player 50 A", sessionPoints = 50),
            TestDataFactory.createPlayerEntity(id = "player_50_b", name = "Player 50 B", sessionPoints = 50),
            TestDataFactory.createPlayerEntity(id = "player_100", name = "Player 100", sessionPoints = 100)
        )
        database.players().insertAll(players)

        // Act
        val orderedPlayers = database.players().getPlayersBySessionPoints()

        // Assert
        assertEquals("Should return all players", 3, orderedPlayers.size)

        // Should be ordered by sessionPoints descending
        assertEquals("Highest points should be first", 100, orderedPlayers[0].sessionPoints)
        assertEquals("Tied players should follow", 50, orderedPlayers[1].sessionPoints)
        assertEquals("Tied players should follow", 50, orderedPlayers[2].sessionPoints)
    }

    @Test
    fun `getPlayersBySessionPoints returns empty list when no players exist`() = runTest {
        // Act
        val orderedPlayers = database.players().getPlayersBySessionPoints()

        // Assert
        assertNotNull("Results should not be null", orderedPlayers)
        assertTrue("Results should be empty when no players exist", orderedPlayers.isEmpty())
    }

    @Test
    fun `getTopPlayers returns correct number of top players`() = runTest {
        // Arrange
        val players = (1..10).map { i ->
            TestDataFactory.createPlayerEntity(
                id = "player_$i",
                name = "Player $i",
                sessionPoints = i * 10
            )
        }
        database.players().insertAll(players)

        // Act
        val top3Players = database.players().getTopPlayers(3)
        val top5Players = database.players().getTopPlayers(5)

        // Assert
        assertEquals("Should return 3 top players", 3, top3Players.size)
        assertEquals("Should return 5 top players", 5, top5Players.size)

        // Top 3 should be highest scoring players
        assertEquals("Top player should have 100 points", 100, top3Players[0].sessionPoints)
        assertEquals("Second player should have 90 points", 90, top3Players[1].sessionPoints)
        assertEquals("Third player should have 80 points", 80, top3Players[2].sessionPoints)
    }

    @Test
    fun `getTopPlayers handles request for more players than exist`() = runTest {
        // Arrange
        val players = TestDataFactory.createPlayerEntityList(3)
        database.players().insertAll(players)

        // Act
        val topPlayers = database.players().getTopPlayers(10) // Ask for more than exist

        // Assert
        assertEquals("Should return all available players", 3, topPlayers.size)
    }

    @Test
    fun `getTopPlayers handles zero count correctly`() = runTest {
        // Arrange
        val players = TestDataFactory.createPlayerEntityList(3)
        database.players().insertAll(players)

        // Act
        val topPlayers = database.players().getTopPlayers(0)

        // Assert
        assertNotNull("Results should not be null", topPlayers)
        assertTrue("Results should be empty for zero count", topPlayers.isEmpty())
    }

    @Test
    fun `getTopPlayers handles negative count correctly`() = runTest {
        // Arrange
        val players = TestDataFactory.createPlayerEntityList(3)
        database.players().insertAll(players)

        // Act
        val topPlayers = database.players().getTopPlayers(-1)

        // Assert
        assertNotNull("Results should not be null", topPlayers)
        assertTrue("Results should be empty for negative count", topPlayers.isEmpty())
    }
}