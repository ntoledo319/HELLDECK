package com.helldeck.content.engine.augment

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused unit tests for Validator only.
 * Legacy Augmentor + LLM integration tests are pending migration to current API.
 */
class AugmentorValidatorTest {

    @Test
    fun `sanitize normalizes whitespace and trims quotes`() {
        val v = Validator(setOf("badword"), maxSpice = 3)
        val input = "\"  Hello   world   \""
        val out = v.sanitize(input)
        assertTrue(out == "Hello world")
    }

    @Test
    fun `accepts enforces word limit and basic profanity rules`() {
        val v = Validator(setOf("badword"), maxSpice = 3)
        assertTrue(v.accepts("short text", maxWords = 3, spice = 1))
        assertFalse(v.accepts("this is too many words", maxWords = 3, spice = 1))
        assertFalse(v.accepts("has badword here", maxWords = 10, spice = 1))
        assertTrue(v.accepts("has badword here", maxWords = 10, spice = 3))
    }
}

