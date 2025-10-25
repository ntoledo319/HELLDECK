package com.helldeck.content.engine.augment

import com.helldeck.content.model.FilledCard
import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import kotlin.random.Random

/**
 * Unit tests for Augmentor and Validator (LLM integration, validation rules).
 * 
 * Tests the LLM augmentation and validation pipeline including:
 * - Paraphrase generation with caching
 * - Content sanitization and validation
 * - Profanity filtering
 * - Word count and spice level constraints
 * - Error handling for LLM failures
 */
class AugmentorValidatorTest {

    private lateinit var mockLLM: LocalLLM
    private lateinit var mockCache: GenerationCache
    private lateinit var validator: Validator
    private lateinit var augmentor: Augmentor

    @Before
    fun setup() {
        mockLLM = mock(LocalLLM::class.java)
        mockCache = mock(GenerationCache::class.java)
        validator = Validator(setOf("badword", "terrible"), maxSpice = 3)
        augmentor = Augmentor(mockLLM, mockCache, validator)
    }

    @Test
    fun `maybeParaphrase returns original card when LLM is null`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        val result = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        assertEquals("Should return original card when LLM is null", card, result)
    }

    @Test
    fun `maybeParaphrase returns original card when paraphrase disabled`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = false,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        `when`(mockLLM.generate(anyString(), anyString(), any())).thenReturn("Paraphrased text")

        val result = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        assertEquals("Should return original card when paraphrase disabled", card, result)
        verify(mockLLM, never()).generate(anyString(), anyString(), any())
    }

    @Test
    fun `maybeParaphrase uses cache when available`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        val cachedText = "Cached paraphrased text"
        `when`(mockCache.key(anyString(), anyString(), anyString(), anyInt())).thenReturn(cachedText)

        val result = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        assertEquals("Should use cached text", cachedText, result.text)
        verify(mockLLM, never()).generate(anyString(), anyString(), any())
    }

    @Test
    fun `maybeParaphrase generates LLM request when not cached`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        `when`(mockCache.key(anyString(), anyString(), anyString(), anyInt())).thenReturn(null)
        `when`(mockLLM.generate(anyString(), anyString(), any())).thenReturn("Generated text")

        val result = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        assertEquals("Should use generated text", "Generated text", result.text)
        verify(mockLLM).generate(anyString(), anyString(), any())
    }

    @Test
    fun `maybeParaphrase uses correct LLM parameters`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        `when`(mockCache.key(anyString(), anyString(), anyString(), anyInt())).thenReturn(null)
        `when`(mockLLM.generate(anyString(), anyString(), any())).thenReturn("Generated text")

        augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        // Verify LLM was called with correct parameters
        verify(mockLLM).generate(
            argThat { contains("You rewrite party game prompts safely") },
            argThat { contains("Rewrite to be punchy, same meaning") },
            argThat { config: GenConfig ->
                config.maxTokens == 40 && // maxWords * 2
                config.temperature == 0.5f && // spice = 1 (low spice)
                config.topP == 0.9f &&
                config.seed == 42
            }
        )
    }

    @Test
    fun `maybeParaphrase uses higher temperature for spicy content`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 3
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 3,
            tags = listOf("funny")
        )

        `when`(mockCache.key(anyString(), anyString(), anyString(), anyInt())).thenReturn(null)
        `when`(mockLLM.generate(anyString(), anyString(), any())).thenReturn("Generated text")

        augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        // Verify LLM was called with higher temperature for spicy content
        verify(mockLLM).generate(
            anyString(),
            anyString(),
            argThat { config: GenConfig -> config.temperature == 0.8f }
        )
    }

    @Test
    fun `maybeParaphrase rejects invalid LLM output`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        val invalidOutput = "This contains badword which should be rejected"
        `when`(mockCache.key(anyString(), anyString(), anyString(), anyInt())).thenReturn(null)
        `when`(mockLLM.generate(anyString(), anyString(), any())).thenReturn(invalidOutput)

        val result = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        assertEquals("Should return original card when validation fails", card, result)
    }

    @Test
    fun `sanitize removes extra whitespace`() {
        val input = "  Hello   world  with   spaces  "
        val expected = "Hello world with spaces"

        val result = validator.sanitize(input)

        assertEquals("Should normalize whitespace", expected, result)
    }

    @Test
    fun `sanitize removes quotes`() {
        val input = "\"Hello world\""
        val expected = "Hello world"

        val result = validator.sanitize(input)

        assertEquals("Should remove quotes", expected, result)
    }

    @Test
    fun `accepts rejects content with profanity`() {
        val cleanText = "This is clean content"
        val profaneText = "This contains badword"

        assertTrue("Should accept clean text", validator.accepts(cleanText, 20, 1))
        assertFalse("Should reject profane text", validator.accepts(profaneText, 20, 1))
    }

    @Test
    fun `accepts rejects content with too many words`() {
        val shortText = "This is short"
        val longText = "This text has way too many words and should be rejected because it exceeds the maximum word limit for this particular test case"

        assertTrue("Should accept short text", validator.accepts(shortText, 20, 1))
        assertFalse("Should reject long text", validator.accepts(longText, 20, 1))
    }

    @Test
    fun `accepts allows profanity for high spice levels`() {
        val profaneText = "This contains badword"
        val cleanText = "This is clean content"

        val lowSpiceValidator = Validator(setOf("badword", "terrible"), maxSpice = 1)
        val highSpiceValidator = Validator(setOf("badword", "terrible"), maxSpice = 3)

        assertFalse("Low spice should reject profanity", lowSpiceValidator.accepts(profaneText, 20, 1))
        assertTrue("High spice should allow profanity", highSpiceValidator.accepts(profaneText, 20, 3))
    }

    @Test
    fun `containsProfanity detects bad words correctly`() {
        val cleanText = "This is clean"
        val profaneText1 = "This contains badword"
        val profaneText2 = "This contains terrible"
        val profaneText3 = "This contains BADWORD in different case"

        assertFalse("Should not detect clean text", validator.containsProfanity(cleanText))
        assertTrue("Should detect badword", validator.containsProfanity(profaneText1))
        assertTrue("Should detect terrible", validator.containsProfanity(profaneText2))
        assertTrue("Should detect case-insensitive", validator.containsProfanity(profaneText3))
    }

    @Test
    fun `maybeParaphrase caches results correctly`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        val generatedText = "Generated text"
        `when`(mockCache.key(anyString(), anyString(), anyString(), anyInt())).thenReturn(null)
        `when`(mockLLM.generate(anyString(), anyString(), any())).thenReturn(generatedText)

        // First call should cache the result
        val result1 = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        // Second call should use cached result
        val result2 = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        assertEquals("First call should use generated text", generatedText, result1.text)
        assertEquals("Second call should use cached text", generatedText, result2.text)
        
        // Verify cache was called once and LLM was called once
        verify(mockCache).key(anyString(), anyString(), anyString(), anyInt())
        verify(mockLLM, times(1)).generate(anyString(), anyString(), any())
    }

    @Test
    fun `maybeParaphrase handles LLM exceptions gracefully`() {
        val card = FilledCard(
            id = "test",
            game = "test_game",
            text = "Original text",
            family = "test_family",
            spice = 1
        )

        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = 20,
            spice = 1,
            tags = listOf("funny")
        )

        `when`(mockCache.key(anyString(), anyString(), anyString(), anyInt())).thenReturn(null)
        `when`(mockLLM.generate(anyString(), anyString(), any())).thenThrow(RuntimeException("LLM failed"))

        val result = augmentor.maybeParaphrase(card, plan, seed = 42, modelId = "test_model")

        assertEquals("Should return original card on LLM failure", card, result)
    }
}