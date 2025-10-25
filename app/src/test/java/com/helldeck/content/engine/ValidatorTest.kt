package com.helldeck.content.engine

import com.helldeck.content.engine.augment.Validator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {

    private val profanity = setOf("badword", "curse")
    private val validator = Validator(profanity, maxSpice = 3)

    @Test
    fun `accepts valid string`() {
        assertTrue(validator.accepts("This is a clean string", 10, 1))
    }

    @Test
    fun `rejects string with too many words`() {
        assertFalse(validator.accepts("This string has too many words", 5, 1))
    }

    @Test
    fun `rejects string with profanity at low spice`() {
        assertFalse(validator.accepts("This is a badword string", 10, 1))
    }

    @Test
    fun `accepts string with profanity at high spice`() {
        assertTrue(validator.accepts("This is a badword string", 10, 2))
    }

    @Test
    fun `sanitizes string correctly`() {
        val sanitized = validator.sanitize("\"  This is a   dirty string  \"")
        assertTrue(sanitized == "This is a dirty string")
    }
}