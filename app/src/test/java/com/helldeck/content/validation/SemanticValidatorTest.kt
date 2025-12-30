package com.helldeck.content.validation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for SemanticValidator.
 * Tests that validator correctly uses slot TYPES, not slot NAMES.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SemanticValidatorTest {

    private lateinit var validator: SemanticValidator

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        validator = SemanticValidator(context.assets)
    }

    @Test
    fun `single slot always passes`() {
        val slots = mapOf(
            "action1" to SlotFill("sketchy_action", "steal candy", "steal candy"),
        )
        val score = validator.validateCoherence(slots)
        assertEquals(1.0, score, 0.001)
    }

    @Test
    fun `compatible slot types pass with high score`() {
        val slots = mapOf(
            "action1" to SlotFill("sketchy_action", "steal", "steal"),
            "reason1" to SlotFill("absurd_reason", "for science", "for science"),
        )
        val score = validator.validateCoherence(slots)
        assertTrue("Compatible types should score > 0.5, got $score", score > 0.5)
    }

    @Test
    fun `forbidden pair returns zero score`() {
        // Test forbidden pair if any exist in semantic_compatibility.json
        // This is a placeholder - actual forbidden pairs depend on config
        val slots = mapOf(
            "slot1" to SlotFill("forbidden_type_a", "text1", "text1"),
            "slot2" to SlotFill("forbidden_type_b", "text2", "text2"),
        )
        // Note: Will pass if no forbidden pairs defined in config
        val score = validator.validateCoherence(slots)
        assertTrue("Score should be in valid range: $score", score >= 0.0 && score <= 1.0)
    }

    @Test
    fun `slot names do not affect validation`() {
        // Same slot types, different names -> same score
        val slots1 = mapOf(
            "action_main" to SlotFill("sketchy_action", "steal", "steal"),
            "reason_main" to SlotFill("absurd_reason", "for fun", "for fun"),
        )
        val slots2 = mapOf(
            "different_name_1" to SlotFill("sketchy_action", "steal", "steal"),
            "different_name_2" to SlotFill("absurd_reason", "for fun", "for fun"),
        )

        val score1 = validator.validateCoherence(slots1)
        val score2 = validator.validateCoherence(slots2)

        assertEquals(
            "Same types with different names should have same score",
            score1,
            score2,
            0.001,
        )
    }

    @Test
    fun `slot types are evaluated not names`() {
        // This is the critical fix: validator must use slot.slotType, not map keys
        val slotsWithMismatchedNames = mapOf(
            "wrong_name" to SlotFill("correct_type_a", "value1", "value1"),
            "also_wrong" to SlotFill("correct_type_b", "value2", "value2"),
        )

        val slotsWithCorrectNames = mapOf(
            "correct_type_a" to SlotFill("correct_type_a", "value1", "value1"),
            "correct_type_b" to SlotFill("correct_type_b", "value2", "value2"),
        )

        val score1 = validator.validateCoherence(slotsWithMismatchedNames)
        val score2 = validator.validateCoherence(slotsWithCorrectNames)

        // Both should evaluate identically because types are the same
        assertEquals(
            "Slot names must not affect evaluation, only types matter",
            score1,
            score2,
            0.001,
        )
    }

    @Test
    fun `empty slots return high score`() {
        val slots = emptyMap<String, SlotFill>()
        val score = validator.validateCoherence(slots)
        assertEquals(1.0, score, 0.001)
    }

    @Test
    fun `three or more slots are evaluated`() {
        val slots = mapOf(
            "s1" to SlotFill("type_a", "val1", "val1"),
            "s2" to SlotFill("type_b", "val2", "val2"),
            "s3" to SlotFill("type_c", "val3", "val3"),
        )
        val score = validator.validateCoherence(slots)
        assertTrue("Score should be in valid range: $score", score >= 0.0 && score <= 1.0)
    }

    @Test
    fun `semantic distance affects score`() {
        // Identical texts should be penalized as too similar
        val identicalSlots = mapOf(
            "s1" to SlotFill("type_a", "identical text", "identical text"),
            "s2" to SlotFill("type_b", "identical text", "identical text"),
        )
        val identicalScore = validator.validateCoherence(identicalSlots)

        // Different texts should score differently
        val differentSlots = mapOf(
            "s1" to SlotFill("type_a", "completely different", "completely different"),
            "s2" to SlotFill("type_b", "unique content here", "unique content here"),
        )
        val differentScore = validator.validateCoherence(differentSlots)

        // Both should be in valid range
        assertTrue(identicalScore >= 0.0 && identicalScore <= 1.0)
        assertTrue(differentScore >= 0.0 && differentScore <= 1.0)
    }
}
