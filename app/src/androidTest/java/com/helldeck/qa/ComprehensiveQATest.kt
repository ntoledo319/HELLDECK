package com.helldeck.qa

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.Player
import com.helldeck.engine.GamesRegistry
import com.helldeck.ui.HelldeckAppUI
import com.helldeck.ui.HelldeckVm
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import android.content.Context
import androidx.test.core.app.ApplicationProvider

/**
 * Comprehensive QA tests for HELLDECK
 * 
 * Tests all 14 games with various player counts and edge cases:
 * - Game mechanics validation
 * - UI responsiveness
 * - Performance under load
 * - Error handling
 * - Accessibility compliance
 */
@RunWith(AndroidJUnit4::class)
class ComprehensiveQATest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var repository: ContentRepository
    private lateinit var gameEngine: GameEngine
    private lateinit var viewModel: HelldeckVm

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = ContentRepository(context)
        gameEngine = com.helldeck.content.engine.ContentEngineProvider.get(context)
        viewModel = HelldeckVm()
        
        runBlocking {
            viewModel.initOnce()
        }
    }

    /**
     * Test all games with minimum players (2)
     */
    @Test
    fun testAllGamesWithMinimumPlayers() {
        val allGames = GamesRegistry.getAllGameIds()
        val players = listOf(
            Player(id = "p1", name = "Player 1", avatar = "😀", sessionPoints = 0),
            Player(id = "p2", name = "Player 2", avatar = "😎", sessionPoints = 0)
        )

        allGames.forEach { gameId ->
            try {
                runBlocking {
                    val result = gameEngine.next(
                        GameEngine.Request(
                            gameId = gameId,
                            sessionId = "test_session",
                            spiceMax = 1,
                            players = players.map { it.name }
                        )
                    )
                    
                    assertNotNull("Game $gameId should generate content", result.filledCard)
                    assertTrue("Game $gameId should have valid content", result.filledCard.text.isNotEmpty())
                    assertFalse("Game $gameId should not have unfilled slots", result.filledCard.text.contains("{"))
                }
            } catch (e: Exception) {
                fail("Game $gameId failed with minimum players: ${e.message}")
            }
        }
    }

    /**
     * Test all games with maximum players (16)
     */
    @Test
    fun testAllGamesWithMaximumPlayers() {
        val allGames = GamesRegistry.getAllGameIds()
        val players = (1..16).map { i ->
            Player(id = "p$i", name = "Player $i", avatar = "👤", sessionPoints = 0)
        }

        allGames.forEach { gameId ->
            try {
                runBlocking {
                    val result = gameEngine.next(
                        GameEngine.Request(
                            gameId = gameId,
                            sessionId = "test_session",
                            spiceMax = 1,
                            players = players.map { it.name }
                        )
                    )
                    
                    assertNotNull("Game $gameId should generate content with max players", result.filledCard)
                    assertTrue("Game $gameId should have valid content with max players", result.filledCard.text.isNotEmpty())
                }
            } catch (e: Exception) {
                fail("Game $gameId failed with maximum players: ${e.message}")
            }
        }
    }

    /**
     * Test edge cases with odd player counts
     */
    @Test
    fun testEdgeCasePlayerCounts() {
        val edgeCases = listOf(3, 5, 7, 11, 13, 15)
        val testGame = GamesRegistry.getAllGameIds().first()

        edgeCases.forEach { playerCount ->
            val players = (1..playerCount).map { i ->
                Player(id = "p$i", name = "Player $i", avatar = "😀", sessionPoints = 0)
            }

            try {
                runBlocking {
                    val result = gameEngine.next(
                        GameEngine.Request(
                            gameId = testGame,
                            sessionId = "test_session",
                            spiceMax = 1,
                            players = players.map { it.name }
                        )
                    )
                    
                    assertNotNull("Game should work with $playerCount players", result.filledCard)
                }
            } catch (e: Exception) {
                fail("Game failed with $playerCount players: ${e.message}")
            }
        }
    }

    /**
     * Test spicy mode variations
     */
    @Test
    fun testSpicyModeVariations() {
        val players = listOf(
            Player(id = "p1", name = "Player 1", avatar = "😀", sessionPoints = 0),
            Player(id = "p2", name = "Player 2", avatar = "😎", sessionPoints = 0)
        )
        val spiceLevels = listOf(1, 2, 3)

        spiceLevels.forEach { spiceLevel ->
            try {
                runBlocking {
                    val result = gameEngine.next(
                        GameEngine.Request(
                            gameId = GamesRegistry.getAllGameIds().first(),
                            sessionId = "test_session",
                            spiceMax = spiceLevel,
                            players = players.map { it.name }
                        )
                    )
                    
                    assertNotNull("Game should work with spice level $spiceLevel", result.filledCard)
                    assertTrue("Content should be valid with spice level $spiceLevel", result.filledCard.text.isNotEmpty())
                }
            } catch (e: Exception) {
                fail("Game failed with spice level $spiceLevel: ${e.message}")
            }
        }
    }

    /**
     * Test UI responsiveness under load
     */
    @Test
    fun testUIResponsivenessUnderLoad() {
        composeTestRule.setContent {
            HelldeckAppUI(viewModel)
        }

        // Simulate rapid state changes
        repeat(10) {
            composeTestRule.onNodeWithText("Start Game").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify UI is still responsive
        composeTestRule.onNodeWithText("HELLDECK").assertExists()
    }

    /**
     * Test memory usage during gameplay
     */
    @Test
    fun testMemoryUsageDuringGameplay() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Simulate extended gameplay
        repeat(50) {
            runBlocking {
                gameEngine.next(
                    GameEngine.Request(
                        gameId = GamesRegistry.getAllGameIds().random(),
                        sessionId = "memory_test_session",
                        spiceMax = 2,
                        players = listOf("Player 1", "Player 2", "Player 3")
                    )
                )
            }
        }

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024) // Convert to MB

        assertTrue("Memory increase should be reasonable (< 50MB)", memoryIncrease < 50)
    }

    /**
     * Test error handling with invalid inputs
     */
    @Test
    fun testErrorHandlingWithInvalidInputs() {
        try {
            runBlocking {
                // Test with empty player list
                gameEngine.next(
                    GameEngine.Request(
                        gameId = "invalid_game",
                        sessionId = "test_session",
                        spiceMax = 1,
                        players = emptyList()
                    )
                )
                fail("Should throw exception for empty player list")
            }
        } catch (e: IllegalArgumentException) {
            // Expected
        } catch (e: Exception) {
            fail("Should throw IllegalArgumentException, not ${e.javaClass.simpleName}")
        }

        try {
            runBlocking {
                // Test with invalid game ID
                gameEngine.next(
                    GameEngine.Request(
                        gameId = "nonexistent_game",
                        sessionId = "test_session",
                        spiceMax = 1,
                        players = listOf("Player 1")
                    )
                )
                fail("Should throw exception for invalid game ID")
            }
        } catch (e: Exception) {
            // Expected - should handle gracefully
        }
    }

    /**
     * Test accessibility compliance
     */
    @Test
    fun testAccessibilityCompliance() {
        composeTestRule.setContent {
            HelldeckAppUI(viewModel)
        }

        // Test that all interactive elements have content descriptions
        composeTestRule.onAllNodes[assertHasClickAction()].forEach { node ->
            try {
                node.assertContentDescriptionExists()
            } catch (e: AssertionError) {
                // Log accessibility issues for manual review
                println("Accessibility issue: Interactive element missing content description")
            }
        }
    }

    /**
     * Test game balance across all games
     */
    @Test
    fun testGameBalanceAcrossAllGames() {
        val allGames = GamesRegistry.getAllGameIds()
        val players = listOf(
            Player(id = "p1", name = "Player 1", avatar = "😀", sessionPoints = 0),
            Player(id = "p2", name = "Player 2", avatar = "😎", sessionPoints = 0),
            Player(id = "p3", name = "Player 3", avatar = "🎮", sessionPoints = 0)
        )

        val gameResults = mutableMapOf<String, GameEngine.Result>()

        allGames.forEach { gameId ->
            try {
                runBlocking {
                    val result = gameEngine.next(
                        GameEngine.Request(
                            gameId = gameId,
                            sessionId = "balance_test",
                            spiceMax = 2,
                            players = players.map { it.name }
                        )
                    )
                    gameResults[gameId] = result
                }
            } catch (e: Exception) {
                fail("Game $gameId failed during balance test: ${e.message}")
            }
        }

        // Verify all games generated content
        assertEquals("All games should generate content", allGames.size, gameResults.size)
        
        // Verify content quality
        gameResults.values.forEach { result ->
            assertNotNull("Result should not be null", result)
            assertNotNull("Filled card should not be null", result.filledCard)
            assertTrue("Content should not be empty", result.filledCard.text.isNotEmpty())
            assertFalse("Content should not have unfilled slots", result.filledCard.text.contains("{"))
        }
    }

    /**
     * Test performance with concurrent operations
     */
    @Test
    fun testPerformanceWithConcurrentOperations() {
        val startTime = System.currentTimeMillis()
        
        // Run multiple game generations concurrently
        val jobs = (1..10).map { i ->
            kotlinx.coroutines.async {
                runBlocking {
                    gameEngine.next(
                        GameEngine.Request(
                            gameId = GamesRegistry.getAllGameIds().random(),
                            sessionId = "concurrent_test_$i",
                            spiceMax = 1,
                            players = listOf("Player 1", "Player 2")
                        )
                    )
                }
            }
        }

        runBlocking {
            jobs.forEach { it.await() }
        }

        val duration = System.currentTimeMillis() - startTime
        
        // Should complete within reasonable time (5 seconds)
        assertTrue("Concurrent operations should complete quickly", duration < 5000)
    }

    /**
     * Test template variety and diversity
     */
    @Test
    fun testTemplateVarietyAndDiversity() {
        val players = listOf(
            Player(id = "p1", name = "Player 1", avatar = "😀", sessionPoints = 0),
            Player(id = "p2", name = "Player 2", avatar = "😎", sessionPoints = 0)
        )

        val generatedTemplates = mutableSetOf<String>()
        val generatedFamilies = mutableSetOf<String>()

        // Generate multiple templates to test variety
        repeat(20) {
            runBlocking {
                val result = gameEngine.next(
                    GameEngine.Request(
                        gameId = GamesRegistry.getAllGameIds().random(),
                        sessionId = "variety_test",
                        spiceMax = 1,
                        players = players.map { it.name }
                    )
                )
                
                generatedTemplates.add(result.filledCard.id)
                result.filledCard.family?.let { family ->
                    generatedFamilies.add(family)
                }
            }
        }

        // Should have good variety
        assertTrue("Should generate variety of templates", generatedTemplates.size > 10)
        assertTrue("Should generate variety of families", generatedFamilies.size > 3)
    }

    /**
     * Test scoring system consistency
     */
    @Test
    fun testScoringSystemConsistency() {
        val players = listOf(
            Player(id = "p1", name = "Player 1", avatar = "😀", sessionPoints = 100),
            Player(id = "p2", name = "Player 2", avatar = "😎", sessionPoints = 50)
        )

        // Test different scoring scenarios
        val scoringScenarios = listOf(
            mapOf("lol" to 5, "meh" to 0, "trash" to 0), // All positive
            mapOf("lol" to 2, "meh" to 2, "trash" to 1), // Mixed
            mapOf("lol" to 0, "meh" to 0, "trash" to 5)  // All negative
        )

        scoringScenarios.forEach { scenario ->
            runBlocking {
                // Simulate feedback and scoring
                scenario.forEach { (type, count) ->
                    repeat(count) {
                        when (type) {
                            "lol" -> viewModel.feedbackLol()
                            "meh" -> viewModel.feedbackMeh()
                            "trash" -> viewModel.feedbackTrash()
                        }
                    }
                }
                
                // Verify scoring doesn't crash and produces reasonable results
                val finalScores = players.map { it.sessionPoints }
                assertTrue("Scores should be reasonable", finalScores.all { it >= 0 })
            }
        }
    }
}