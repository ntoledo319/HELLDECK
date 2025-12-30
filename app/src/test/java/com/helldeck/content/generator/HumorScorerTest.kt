package com.helldeck.content.generator

import com.helldeck.content.generator.HumorScorer.SlotData
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class HumorScorerTest {
    @Test
    fun `high spice taboo combo yields strong shock and benign violation`() {
        val lexRepo: LexiconRepositoryV2 = mockk(relaxed = true)
        val pairings: Map<String, Map<String, Double>> = emptyMap()
        val scorer = HumorScorer(lexRepo, pairings)

        val blueprint = TemplateBlueprint(
            id = "t1",
            game = "ROAST_CONSENSUS",
            family = "test",
            spice_max = 4,
            locality_max = 3,
            blueprint = listOf(
                BlueprintSegment.Text("A "),
                BlueprintSegment.Slot("a", "sexual_innuendo"),
                BlueprintSegment.Text(" and "),
                BlueprintSegment.Slot("b", "bodily_functions"),
            ),
        )

        val slots: Map<String, SlotData> = mapOf(
            "a" to SlotData(slotType = "sexual_innuendo", text = "suggestive stuff", spice = 4, tone = "wild"),
            "b" to SlotData(slotType = "bodily_functions", text = "gross stuff", spice = 4, tone = "wild"),
        )

        val score = scorer.evaluate("A test", blueprint, slots)

        assertTrue("shock should be high", score.shockValue >= 0.9)
        assertTrue("cringe should reflect sexual+bodily combo", score.cringeFactor >= 0.3)
        assertTrue("benign violation should be strong at spice 4", score.benignViolation >= 0.9)
        assertTrue("overall score within 0..1", score.overallScore in 0.0..1.0)
    }
}
