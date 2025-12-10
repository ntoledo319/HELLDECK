package com.helldeck.content.validation

import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardQualityInspectorTest {

    @Test
    fun `accepts lively card with balanced options`() {
        val card = FilledCard(
            id = "test_card",
            game = "POISON_PITCH",
            text = "Pitch the room on why midnight karaoke in crocs beats a sunrise spin class.",
            family = "pitch",
            spice = 2,
            locality = 1,
            metadata = mapOf(
                "slots" to mapOf(
                    "perk" to "midnight karaoke",
                    "gross" to "crocs"
                )
            )
        )
        val ok = CardQualityInspector.isAcceptable(
            card,
            GameOptions.AB("Midnight karaoke", "Sunrise spin class")
        )
        assertTrue("Card should pass quality heuristics", ok)
    }

    @Test
    fun `flags placeholder leakage and repetition`() {
        val card = FilledCard(
            id = "bad_card",
            game = "ROAST_CONSENSUS",
            text = "Most likely to {target_name} because because because.",
            family = "roast",
            spice = 1,
            locality = 1,
            metadata = emptyMap()
        )
        val issues = CardQualityInspector.evaluate(card, GameOptions.PlayerVote(listOf("A", "B")))
        assertTrue("Should flag leftover placeholder", issues.contains(CardQualityInspector.Issue.PLACEHOLDER_LEFTOVER))
        assertTrue("Should flag repeated filler", issues.contains(CardQualityInspector.Issue.EXCESS_REPEAT))
    }

    @Test
    fun `flags unusable A B options`() {
        val card = FilledCard(
            id = "bad_options",
            game = "POISON_PITCH",
            text = "Would you rather choose chaos or chaos?",
            family = "pitch",
            spice = 1,
            locality = 1,
            metadata = emptyMap()
        )
        val ok = CardQualityInspector.isAcceptable(
            card,
            GameOptions.AB("Chaos", "Chaos")
        )
        assertFalse("Identical options should fail quality checks", ok)
    }
}
