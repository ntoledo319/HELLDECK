package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.v2.SlotSpec
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import kotlin.random.Random

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
                SlotSpec("game_name", "lexicon", emptyList())
            ),
            max_words = 10
        )

        val context = TemplateEngine.Context(
            players = listOf("Alice", "Bob")
        )

        `when`(mockRepo.wordsFor("target_name")).thenReturn(listOf("Alice", "Bob"))
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("Chess", "Monopoly"))

        val result = engine.fill(template, context)

        assertEquals("Should fill player name", "Hello Alice, welcome to Chess!", result.text)
        assertTrue("Should contain filled slots", result.text.contains("Alice") && result.text.contains("Chess"))
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
            max_words = 10
        )

        val context = TemplateEngine.Context(
            players = listOf("alice", "bob")
        )

        `when`(mockRepo.wordsFor("target_name")).thenReturn(listOf("alice", "bob"))
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("chess", "monopoly"))

        val result = engine.fill(template, context)

        assertEquals("Should apply upper case", "Hello ALICE, welcome to Chess!", result.text)
        assertEquals("Should apply title case", "Monopoly", result.text.substringAfter("welcome to ").substringBefore("!"))
        assertTrue("Should contain filled slots", result.text.contains("ALICE") && result.text.contains("Chess"))
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
            max_words = 5
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
    fun `fill with unique modifier avoids duplicates`() {
        val template = TemplateV2(
            id = "unique_test",
            game = "test_game",
            text = "Word: {word::unique}",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("word", "lexicon", listOf("unique"))
            ),
            max_words = 10
        )

        val context = TemplateEngine.Context()

        // Mock lexicon with some words
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("apple", "banana", "apple", "cherry"))

        val result1 = engine.fill(template, context)
        val result2 = engine.fill(template, context)

        // Should avoid duplicates within same fill
        assertNotEquals("Should avoid duplicate in same fill", result1.text, result2.text)
        assertTrue("Each result should contain different words", 
            result1.text != result2.text)
    }

    @Test
    fun `fill with a_an modifier adds correct article`() {
        val template = TemplateV2(
            id = "article_test",
            game = "test_game",
            text = "This is {word::a_an} example",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("word", "lexicon", listOf("a_an"))
            ),
            max_words = 10
        )

        val context = TemplateEngine.Context()

        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("apple", "banana", "orange", "umbrella"))

        val result = engine.fill(template, context)

        assertTrue("Should add 'an' before vowel-starting words", 
            result.text.contains("an apple") || result.text.contains("an orange") || result.text.contains("an umbrella"))
        assertTrue("Should add 'a' before consonant-starting words", 
            result.text.contains("a banana"))
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
                SlotSpec("player_name", "target_name", emptyList())
            ),
            max_words = 10
        )

        val context = TemplateEngine.Context(
            players = emptyList()
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
                SlotSpec("inbound_text", "inbound_text", emptyList())
            ),
            max_words = 10
        )

        val context = TemplateEngine.Context(
            players = listOf("Player1"),
            inboundTexts = listOf("Hello from Alice", "Bob's message")
        )

        val result = engine.fill(template, context)

        assertTrue("Should use one of the inbound texts", 
            result.text.contains("Hello from Alice") || result.text.contains("Bob's message"))
    }

    @Test
    fun `fill with mixed case modifiers works correctly`() {
        val template = TemplateV2(
            id = "mixed_case_test",
            game = "test_game",
            text = "Word: {word::upper,lower,title}",
            family = "test_family",
            spice = 1,
            slots = listOf(
                SlotSpec("word", "lexicon", listOf("upper", "lower", "title"))
            ),
            max_words = 10
        )

        val context = TemplateEngine.Context()

        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("hello world"))

        val result = engine.fill(template, context)

        assertEquals("Should apply all case transformations", "Word: HELLO WORLD,hello world,Hello World", result.text)
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
                SlotSpec("word", "empty_lexicon", emptyList())
            ),
            max_words = 10
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
            max_words = 10
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
                SlotSpec("weapon", "lexicon", emptyList())
            ),
            max_words = 20
        )

        val context = TemplateEngine.Context(
            players = listOf("Alice", "Bob", "Charlie")
        )

        `when`(mockRepo.wordsFor("target_name")).thenReturn(listOf("Alice", "Bob", "Charlie"))
        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("Beach", "Forest", "Sword"))

        val result = engine.fill(template, context)

        assertTrue("Should fill all slots correctly", 
            result.text.contains("Alice") && result.text.contains("Bob") && 
            result.text.contains("Charlie") && result.text.contains("Beach") && 
            result.text.contains("Forest") && result.text.contains("Sword"))
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
            max_words = 10
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
                SlotSpec("special_word", "lexicon", emptyList())
            ),
            max_words = 10
        )

        val context = TemplateEngine.Context()

        `when`(mockRepo.wordsFor("lexicon")).thenReturn(listOf("@#$%", "test&*"))

        val result = engine.fill(template, context)

        assertTrue("Should handle special characters", result.text.contains("@#$%"))
    }
}