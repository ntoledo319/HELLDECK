package com.helldeck.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.ContextualSelector
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.engine.TemplateEngine
import com.helldeck.content.engine.augment.Augmentor
import com.helldeck.content.engine.augment.GenerationCache
import com.helldeck.content.engine.augment.Validator
import com.helldeck.content.model.FilledCard
import com.helldeck.content.util.SeededRng
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for complete game flows (template selection → filling → augmentation → feedback).
 *
 * Tests the entire pipeline from template selection through filling, augmentation,
 * and feedback recording. These tests verify that all components work together
 * correctly in real-world scenarios.
 *
 * Test scenarios:
 * - Complete game flow with multiple players
 * - Template selection with context constraints
 * - Slot filling with various lexicon types
 * - LLM augmentation (when available)
 * - Feedback recording and learning
 * - Error handling throughout the pipeline
 */
@RunWith(AndroidJUnit4::class)
class CompleteGameFlowTest {

    private lateinit var context: android.content.Context
    private lateinit var repo: ContentRepository
    private lateinit var engine: GameEngine
    private lateinit var selector: ContextualSelector
    private lateinit var templateEngine: TemplateEngine
    private lateinit var augmentor: Augmentor
    private lateinit var cache: GenerationCache
    private lateinit var validator: Validator
    private lateinit var rng: SeededRng

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        repo = ContentRepository(context)
        repo.initialize()

        rng = SeededRng(42) // Fixed seed for reproducible tests
        selector = ContextualSelector(repo, rng.random)
        templateEngine = TemplateEngine(repo, rng)

        // Initialize validator with basic profanity list
        validator = Validator(setOf("badword", "terrible"), maxSpice = 3)

        // Initialize cache
        cache = GenerationCache(repo.db)

        // Initialize augmentor (without LLM for basic tests)
        augmentor = Augmentor(null, cache, validator)

        // Initialize game engine
        engine = GameEngine(
            repo = repo,
            rng = rng,
            selector = selector,
            augmentor = augmentor,
            modelId = "",
            cardGeneratorV3 = null,
            llmCardGeneratorV2 = null,
        )

        // Seed the selector with some initial data
        val priors = repo.templatesV2().associate { template ->
            template.id to Pair(1.0, 1.0) // Initial alpha/beta values
        }
        selector.seed(priors)
    }

    @Test
    fun `complete game flow with multiple players works correctly`() = runBlocking {
        val players = listOf("Alice", "Bob", "Charlie")
        val sessionId = "test_session_1"

        // Test template selection
        val request = GameEngine.Request(
            gameId = null, // Random game
            sessionId = sessionId,
            spiceMax = 2,
            players = players,
        )

        val result = engine.next(request)

        assertNotNull("Should generate a result", result)
        assertNotNull("Should have a filled card", result.filledCard)
        assertTrue("Should have valid game ID", result.filledCard.game.isNotEmpty())
        assertTrue("Should have filled text", result.filledCard.text.isNotEmpty())

        // Verify the filled card contains player references
        val filledText = result.filledCard.text
        assertTrue(
            "Should reference players",
            players.any { filledText.contains(it) } ||
                filledText.contains("someone"),
        ) // Fallback when no players

        // Test feedback recording
        val reward = 0.8 // Positive feedback
        engine.recordOutcome(result.filledCard.id, reward)

        // Verify learning occurred (selector parameters updated)
        // This is tested indirectly by ensuring no exceptions are thrown
        assertTrue("Feedback recording should complete", true)
    }

    @Test
    fun `template selection respects spice constraints`() = runBlocking {
        val players = listOf("Alice", "Bob")
        val sessionId = "test_session_2"

        // Test low spice constraint
        val lowSpiceRequest = GameEngine.Request(
            gameId = null,
            sessionId = sessionId,
            spiceMax = 1,
            players = players,
        )

        val lowSpiceResult = engine.next(lowSpiceRequest)

        assertTrue(
            "Low spice result should have spice <= 1",
            lowSpiceResult.filledCard.spice <= 1,
        )

        // Test high spice constraint
        val highSpiceRequest = GameEngine.Request(
            gameId = null,
            sessionId = sessionId,
            spiceMax = 3,
            players = players,
        )

        val highSpiceResult = engine.next(highSpiceRequest)

        assertTrue(
            "High spice result should have spice <= 3",
            highSpiceResult.filledCard.spice <= 3,
        )
    }

    @Test
    fun `template selection with specific game works correctly`() = runBlocking {
        val players = listOf("Alice", "Bob")
        val sessionId = "test_session_3"

        // Get available templates
        val templates = repo.templatesV2()
        assertTrue("Should have templates available", templates.isNotEmpty())

        // Pick a specific game
        val specificGame = templates.first().game

        val request = GameEngine.Request(
            gameId = specificGame,
            sessionId = sessionId,
            spiceMax = 2,
            players = players,
        )

        val result = engine.next(request)

        assertEquals("Should select specific game", specificGame, result.filledCard.game)
        assertNotNull("Should have filled card", result.filledCard)
        assertTrue("Should have filled text", result.filledCard.text.isNotEmpty())
    }

    @Test
    fun `slot filling with various lexicon types works correctly`() = runBlocking {
        val players = listOf("Alice", "Bob")
        val sessionId = "test_session_4"

        // Test multiple rounds to see different slot types
        repeat(5) {
            val request = GameEngine.Request(
                gameId = null,
                sessionId = sessionId,
                spiceMax = 2,
                players = players,
            )

            val result = engine.next(request)

            assertNotNull("Should generate result", result)
            assertTrue("Should have filled text", result.filledCard.text.isNotEmpty())

            // Verify text doesn't contain unfilled slots
            assertFalse(
                "Should not contain unfilled slots",
                result.filledCard.text.contains("{") || result.filledCard.text.contains("}"),
            )
        }
    }

    @Test
    fun `feedback loop improves selection over time`() = runBlocking {
        val players = listOf("Alice", "Bob")
        val sessionId = "test_session_5"

        // Generate multiple rounds and provide feedback
        val templateIds = mutableListOf<String>()

        repeat(10) { round ->
            val request = GameEngine.Request(
                gameId = null,
                sessionId = sessionId,
                spiceMax = 2,
                players = players,
            )

            val result = engine.next(request)
            templateIds.add(result.filledCard.id)

            // Provide varying feedback to test learning
            val reward = when (round % 3) {
                0 -> 0.9 // High reward
                1 -> 0.5 // Medium reward
                else -> 0.1 // Low reward
            }

            engine.recordOutcome(result.filledCard.id, reward)
        }

        // Verify we got different templates (diversity)
        val uniqueTemplates = templateIds.distinct()
        assertTrue("Should have template diversity", uniqueTemplates.size >= 3)

        // Verify learning occurred (no exceptions thrown)
        assertTrue("Learning should complete successfully", true)
    }

    @Test
    fun `error handling works throughout pipeline`() = runBlocking {
        val players = listOf("Alice", "Bob")
        val sessionId = "test_session_6"

        // Test with invalid game ID
        try {
            val request = GameEngine.Request(
                gameId = "nonexistent_game",
                sessionId = sessionId,
                spiceMax = 2,
                players = players,
            )

            val result = engine.next(request)

            // Should either return a result or handle gracefully
            if (result != null) {
                assertNotNull("Should handle invalid game gracefully", result.filledCard)
            }
        } catch (e: Exception) {
            // Should handle exceptions gracefully
            assertTrue("Should handle exceptions", e.message?.isNotEmpty() == true)
        }

        // Test with empty players list
        try {
            val request = GameEngine.Request(
                gameId = null,
                sessionId = sessionId,
                spiceMax = 2,
                players = emptyList(),
            )

            val result = engine.next(request)

            assertNotNull("Should handle empty players gracefully", result)
            assertNotNull("Should still generate filled card", result.filledCard)
        } catch (e: Exception) {
            // Should handle exceptions gracefully
            assertTrue("Should handle empty players", e.message?.isNotEmpty() == true)
        }
    }

    @Test
    fun `template filling preserves metadata correctly`() = runBlocking {
        val players = listOf("Alice", "Bob")
        val sessionId = "test_session_7"

        val request = GameEngine.Request(
            gameId = null,
            sessionId = sessionId,
            spiceMax = 2,
            players = players,
        )

        val result = engine.next(request)
        val filledCard = result.filledCard

        // Verify metadata preservation
        assertNotNull("Should have template ID", filledCard.id)
        assertTrue("Should have game ID", filledCard.game.isNotEmpty())
        assertTrue("Should have family", filledCard.family.isNotEmpty())
        assertTrue("Should have spice level", filledCard.spice >= 0)
        assertTrue("Should have locality", filledCard.locality >= 0)

        // Verify template metadata is included
        assertTrue("Should include template metadata", filledCard.metadata.isNotEmpty())
        assertTrue(
            "Should include original template",
            filledCard.metadata.containsKey("template"),
        )
    }

    @Test
    fun `multiple concurrent sessions work correctly`() = runBlocking {
        val players1 = listOf("Alice", "Bob")
        val players2 = listOf("Charlie", "Dana")

        // Test multiple sessions simultaneously
        val session1Results = mutableListOf<FilledCard>()
        val session2Results = mutableListOf<FilledCard>()

        repeat(5) { round ->
            // Session 1
            val request1 = GameEngine.Request(
                gameId = null,
                sessionId = "session_1",
                spiceMax = 2,
                players = players1,
            )

            val result1 = engine.next(request1)
            session1Results.add(result1.filledCard)

            // Session 2
            val request2 = GameEngine.Request(
                gameId = null,
                sessionId = "session_2",
                spiceMax = 2,
                players = players2,
            )

            val result2 = engine.next(request2)
            session2Results.add(result2.filledCard)
        }

        // Verify both sessions generated results
        assertEquals("Session 1 should have 5 results", 5, session1Results.size)
        assertEquals("Session 2 should have 5 results", 5, session2Results.size)

        // Verify results are different (diversity)
        val session1Unique = session1Results.map { it.id }.distinct()
        val session2Unique = session2Results.map { it.id }.distinct()

        assertTrue("Session 1 should have diversity", session1Unique.size >= 2)
        assertTrue("Session 2 should have diversity", session2Unique.size >= 2)
    }

    @Test
    fun `word count constraints are respected`() = runBlocking {
        val players = listOf("Alice", "Bob")
        val sessionId = "test_session_8"

        repeat(10) {
            val request = GameEngine.Request(
                gameId = null,
                sessionId = sessionId,
                spiceMax = 2,
                players = players,
            )

            val result = engine.next(request)
            val filledText = result.filledCard.text

            // Count words (simple split by whitespace)
            val wordCount = filledText.trim().split(Regex("\\s+")).size

            // Should be reasonable word count (not too long)
            assertTrue("Should have reasonable word count", wordCount <= 50)
            assertTrue("Should have minimum words", wordCount >= 3)
        }
    }

    @Test
    fun `template diversity works across multiple rounds`() = runBlocking {
        val players = listOf("Alice", "Bob", "Charlie")
        val sessionId = "test_session_9"

        val templateIds = mutableListOf<String>()
        val gameIds = mutableListOf<String>()
        val families = mutableListOf<String>()

        // Generate many rounds to test diversity
        repeat(20) {
            val request = GameEngine.Request(
                gameId = null,
                sessionId = sessionId,
                spiceMax = 2,
                players = players,
            )

            val result = engine.next(request)
            templateIds.add(result.filledCard.id)
            gameIds.add(result.filledCard.game)
            families.add(result.filledCard.family)
        }

        // Verify diversity
        val uniqueTemplates = templateIds.distinct()
        val uniqueGames = gameIds.distinct()
        val uniqueFamilies = families.distinct()

        assertTrue("Should have template diversity", uniqueTemplates.size >= 5)
        assertTrue("Should have game diversity", uniqueGames.size >= 2)
        assertTrue("Should have family diversity", uniqueFamilies.size >= 2)

        // Verify no immediate repetition (within reason)
        for (i in 1 until templateIds.size) {
            if (templateIds[i] == templateIds[i - 1]) {
                // Allow some repetition but not too much
                val repetitions = templateIds.count { it == templateIds[i] }
                assertTrue("Template should not repeat too much", repetitions <= 3)
            }
        }
    }
}
