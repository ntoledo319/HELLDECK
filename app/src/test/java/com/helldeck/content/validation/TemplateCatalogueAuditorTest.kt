package com.helldeck.content.validation

import com.helldeck.content.data.LexiconRepository
import com.helldeck.content.model.v2.OptionProvider
import com.helldeck.content.model.v2.ABSource
import com.helldeck.content.model.v2.SlotSpec
import com.helldeck.content.model.v2.TemplateV2
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.ArgumentMatchers.anyString

class TemplateCatalogueAuditorTest {

    @Test
    fun `audit flags missing lexicon`() {
        val lexiconRepository = Mockito.mock(LexiconRepository::class.java).apply {
            Mockito.`when`(hasLexicon(anyString())).thenReturn(true)
            Mockito.`when`(hasLexicon("mythical_animals")).thenReturn(false)
        }

        val templates = listOf(
            TemplateV2(
                id = "test",
                game = "ROAST_CONSENSUS",
                family = "roast",
                text = "Which crew mate would adopt a {mythical_animals}?",
                slots = listOf(
                    SlotSpec(
                        name = "mythical_animals",
                        from = "mythical_animals"
                    )
                )
            )
        )

        val issues = TemplateCatalogueAuditor.audit(templates, lexiconRepository)
        assertTrue("Missing lexicon should surface as an issue", issues.any {
            it.templateId == "test" && it.message.contains("mythical_animals")
        })
    }

    @Test
    fun `audit checks option provider lexicons`() {
        val lexiconRepository = Mockito.mock(LexiconRepository::class.java).apply {
            Mockito.`when`(hasLexicon(anyString())).thenReturn(true)
            Mockito.`when`(hasLexicon("perks")).thenReturn(false)
        }

        val template = TemplateV2(
            id = "pitch",
            game = "POISON_PITCH",
            family = "pitch",
            text = "Would you rather enjoy {perk} or survive {gross}?",
            options = OptionProvider.AB(
                a = ABSource(from = "perks"),
                b = ABSource(from = "gross")
            )
        )

        val issues = TemplateCatalogueAuditor.audit(listOf(template), lexiconRepository)
        assertTrue("Missing option lexicon should be reported", issues.any {
            it.templateId == "pitch" && it.message.contains("perks")
        })
    }
}
