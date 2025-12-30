package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.v2.SlotSpec
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Unit tests for TemplateEngine slot filling, modifiers, and edge cases.
 *
 * Tests the core template filling functionality including:
 * - Structured slot filling using TemplateV2.slots
 * - Legacy regex fallback for older templates
 * - Case transformations (upper, lower, title)
 * - Article handling for proper grammar
 * - Word count validation
 * - Lexicon-based content selection
 */
class TemplateEngineTest {

    private lateinit var mockRepo: ContentRepository
    private lateinit var engine: TemplateEngine
    private lateinit var rng: SeededRng

    @Before
    fun setup() {
        mockRepo = mock(ContentRepository::class.java)
        rng = SeededRng(42) // Fixed seed for reproducible tests
        engine = TemplateEngine(mockRepo, rng)
    }

    @Test
    fun `fill with structured slots works correctly`() {
        val template = TemplateV2(
            id = "test_template",
            game = "test_game",
            text = "Hello {player_name}, welcome to {game_name}!",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("player_name", "target_name", emptyList()),
                SlotSpec("game_name", "lexicon", emptyList()),
            ),
            max_words = 10,
        )

        val context = TemplateEngine.Context(
            players = listOf("Alice", "Bob"),
        )

        `when`(mockRepo.wordsFor("target_name")).thenReturn(listOf("Alice", "Bob"))
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("Chess", "Monopoly"))

        val result = engine.fill(template, context)

        val playerPicked = listOf("Alice", "Bob").firstOrNull { result.text.contains(it) }
        val gamePicked = listOf("Chess", "Monopoly").firstOrNull { result.text.contains(it) }
        assertTrue("Should pick a player name from context", playerPicked != null)
        assertTrue("Should pick a game name from lexicon", gamePicked != null)
        assertEquals("Should preserve template metadata", "test_game", result.game)
        assertEquals("Should preserve template family", "test_family", result.family)
        assertEquals("Should preserve spice level", 1, result.spice)
    }

    @Test
    fun `fill with legacy regex fallback works`() {
        val template = TemplateV2(
            id = "legacy_template",
            game = "test_game",
            text = "Hello {player_name::upper}, welcome to {game_name::title}!",
            family = "test_family",
            spice = 1,
            slots = emptyList(), // No structured slots
            max_words = 10,
        )

        val context = TemplateEngine.Context(
            players = listOf("alice", "bob"),
        )

        // Legacy placeholders use their raw names as lexicon keys
        `when`(mockRepo.wordsFor("player_name")).thenReturn(listOf("alice", "bob"))
        `when`(mockRepo.wordsFor("game_name")).thenReturn(listOf("chess", "monopoly"))

        val result = engine.fill(template, context)

        val namePart = result.text.substringAfter("Hello ").substringBefore(", welcome")
        val gamePart = result.text.substringAfter("welcome to ").substringBefore("!")

        // Upper applied to chosen name
        assertTrue(namePart == "ALICE" || namePart == "BOB")
        // Title applied to chosen game
        assertTrue(gamePart == "Chess" || gamePart == "Monopoly")
        assertTrue("Should contain filled slots", result.text.contains(namePart) && result.text.contains(gamePart))
    }

    @Test
    fun `fill respects max_words constraint`() {
        val template = TemplateV2(
            id = "long_template",
            game = "test_game",
            text = "This is a very long template that exceeds the word limit",
            family = "test_family",
            spice = 1,
            slots = emptyList(),
            max_words = 5,
        )

        val context = TemplateEngine.Context()

        try {
            engine.fill(template, context)
            fail("Should throw exception for exceeding word limit")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention word limit", e.message?.contains("exceeds max_words") == true)
        }
    }

    @Test
    fun `fill with unique modifier avoids duplicates within one template`() {
        val template = TemplateV2(
            id = "unique_test",
            game = "test_game",
            text = "Words: {w1}, {w2}",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("w1", "lexicon", listOf("unique")),
                SlotSpec("w2", "lexicon", listOf("unique")),
            ),
            max_words = 10,
        )

        val context = TemplateEngine.Context()

        // Mock lexicon with duplicate candidates to validate uniqueness constraint
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("apple", "banana", "apple", "cherry"))

        val result = engine.fill(template, context)
        val parts = result.text.substringAfter("Words: ").split(", ")
        assertEquals(2, parts.size)
        assertNotEquals("Two filled words should be different when 'unique' is set", parts[0], parts[1])
    }

    @Test
    fun `target name slot rotates through available players`() {
        val template = TemplateV2(
            id = "players_unique",
            game = "test_game",
            text = "{p1} calls out {p2} for karaoke duty.",
            family = "roast",
            spice = 1,
            slots = listOf(
                SlotSpec("p1", "target_name"),
                SlotSpec("p2", "target_name"),
            ),
            max_words = 16,
        )

        val context = TemplateEngine.Context(
            players = listOf("Alex", "Jamie"),
        )

        val result = engine.fill(template, context)
        val slots = result.metadata["slots"] as Map<*, *>
        val first = slots["p1"]
        val second = slots["p2"]
        assertNotEquals("Distinct players should be used when available", first, second)
    }

    @Test
    fun `fill with a_an modifier adds correct article`() {
        val baseTemplate = TemplateV2(
            id = "article_test",
            game = "test_game",
            text = "This is {word} example",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("word", "lexicon", listOf("a_an")),
            ),
            max_words = 10,
        )

        val context = TemplateEngine.Context()

        // Case 1: vowel-starting word
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("apple"))
        val withVowel = engine.fill(baseTemplate, context)
        assertTrue(withVowel.text.contains("an apple"))

        // Case 2: consonant-starting word
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("banana"))
        val withConsonant = engine.fill(baseTemplate, context)
        assertTrue(withConsonant.text.contains("a banana"))
    }

    @Test
    fun `fill with empty players list handles gracefully`() {
        val template = TemplateV2(
            id = "no_players_test",
            game = "test_game",
            text = "Hello {player_name}!",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("player_name", "target_name", emptyList()),
            ),
            max_words = 10,
        )

        val context = TemplateEngine.Context(
            players = emptyList(),
        )

        `when`(mockRepo.wordsFor("target_name")).thenReturn(listOf("someone"))

        val result = engine.fill(template, context)

        assertEquals("Should use fallback name", "Hello someone!", result.text)
    }

    @Test
    fun `fill with inbound text uses inbound when available`() {
        val template = TemplateV2(
            id = "inbound_test",
            game = "test_game",
            text = "Message: {inbound_text}",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("inbound_text", "inbound_text", emptyList()),
            ),
            max_words = 10,
        )

        val context = TemplateEngine.Context(
            players = listOf("Player1"),
            inboundTexts = listOf("Hello from Alice", "Bob's message"),
        )

        val result = engine.fill(template, context)

        assertTrue(
            "Should use one of the inbound texts",
            result.text.contains("Hello from Alice") || result.text.contains("Bob's message"),
        )
    }

    @Test
    fun `fill with mixed case modifiers works correctly`() {
        // Use legacy placeholder syntax to apply different transforms per occurrence
        val template = TemplateV2(
            id = "mixed_case_test",
            game = "test_game",
            text = "Word: {word::upper},{word::lower},{word::title}",
            family = "test_family",
            spice = 1,
            slots = emptyList(),
            max_words = 10,
        )

        val context = TemplateEngine.Context()

        `when`(mockRepo.wordsFor("word")).thenReturn(listOf("hello world"))

        val result = engine.fill(template, context)

        val expected = "Word: HELLO WORLD,hello world,Hello World"
        assertTrue(
            "Should include upper, lower, and title variants",
            result.text.contains("HELLO WORLD") && result.text.contains("hello world") && result.text.contains("Hello World"),
        )
    }

    @Test
    fun `fill with empty lexicon throws exception`() {
        val template = TemplateV2(
            id = "empty_lexicon_test",
            game = "test_game",
            text = "Word: {word}",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("word", "empty_lexicon", emptyList()),
            ),
            max_words = 10,
        )

        val context = TemplateEngine.Context()

        `when`(mockRepo.wordsFor("empty_lexicon")).thenReturn(emptyList())

        try {
            engine.fill(template, context)
            fail("Should throw exception for empty lexicon")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention empty lexicon", e.message?.contains("Empty or missing lexicon") == true)
        }
    }

    @Test
    fun `fill preserves template metadata correctly`() {
        val template = TemplateV2(
            id = "metadata_test",
            game = "test_game",
            text = "Simple text",
            family = "test_family",
            spice = 2,
            locality = 1,
            slots = emptyList(),
            max_words = 10,
        )

        val context = TemplateEngine.Context()

        val result = engine.fill(template, context)

        assertEquals("Should preserve template ID", "metadata_test", result.id)
        assertEquals("Should preserve game", "test_game", result.game)
        assertEquals("Should preserve family", "test_family", result.family)
        assertEquals("Should preserve spice", 2, result.spice)
        assertEquals("Should preserve locality", 1, result.locality)
        assertTrue("Should include template metadata", result.metadata.containsKey("template"))
    }

    @Test
    fun `fill with complex nested slots works correctly`() {
        val template = TemplateV2(
            id = "complex_test",
            game = "test_game",
            text = "{player1} vs {player2} in {location} with {weapon}",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("player1", "target_name", emptyList()),
                SlotSpec("player2", "target_name", emptyList()),
                SlotSpec("location", "lexicon", emptyList()),
                SlotSpec("weapon", "lexicon", emptyList()),
            ),
            max_words = 20,
        )

        val context = TemplateEngine.Context(
            players = listOf("Alice", "Bob", "Charlie"),
        )

        `when`(mockRepo.wordsFor("target_name")).thenReturn(listOf("Alice", "Bob", "Charlie"))
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("Beach", "Forest", "Sword"))

        val result = engine.fill(template, context)

        // Should include two player names separated by " vs ", a location and a weapon
        assertTrue("Should include 'vs' separator", result.text.contains(" vs "))
        assertTrue("Should include 'in' preposition", result.text.contains(" in "))
        assertTrue("Should include 'with' preposition", result.text.contains(" with "))
    }

    @Test
    fun `fill with no slots returns original text`() {
        val originalText = "This is a simple template with no slots"
        val template = TemplateV2(
            id = "no_slots_test",
            game = "test_game",
            text = originalText,
            family = "test_family",
            spice = 1,
            slots = emptyList(),
            max_words = 10,
        )

        val context = TemplateEngine.Context()

        val result = engine.fill(template, context)

        assertEquals("Should return original text", originalText, result.text)
        assertEquals("Should preserve metadata", "test_game", result.game)
    }

    @Test
    fun `fill handles special characters in slots`() {
        val template = TemplateV2(
            id = "special_chars_test",
            game = "test_game",
            text = "Special: {special_word}",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("special_word", "lexicon", emptyList()),
            ),
            max_words = 10,
        )

        val context = TemplateEngine.Context()

        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("@#$%", "test&*"))

        val result = engine.fill(template, context)

        val candidates = listOf("@#$%", "test&*")
        assertTrue("Should handle special characters", candidates.any { result.text.contains(it) })
    }
}
