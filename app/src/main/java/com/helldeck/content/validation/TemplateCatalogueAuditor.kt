package com.helldeck.content.validation

import com.helldeck.content.data.LexiconRepository
import com.helldeck.content.model.v2.OptionProvider
import com.helldeck.content.model.v2.TemplateV2

object TemplateCatalogueAuditor {

    data class Issue(val templateId: String, val message: String)

    fun audit(templates: List<TemplateV2>, lexiconRepository: LexiconRepository): List<Issue> {
        val issues = mutableListOf<Issue>()
        val duplicates = templates.groupBy { it.id }.filterValues { it.size > 1 }
        duplicates.forEach { (id, dupes) ->
            issues += Issue(id, "Template id duplicated ${dupes.size} times")
        }

        templates.forEach { template ->
            if (template.text.isBlank()) {
                issues += Issue(template.id, "Template text is blank")
            }
            if (template.spice !in 0..3) {
                issues += Issue(template.id, "Spice ${template.spice} outside expected range 0..3")
            }
            template.slots.forEach slots@{ slot ->
                val from = slot.from
                if (from in SPECIAL_SLOTS) return@slots
                if (!lexiconRepository.hasLexicon(from)) {
                    issues += Issue(template.id, "Missing lexicon for slot '${slot.name}' -> '$from'")
                }
            }
            when (val opts = template.options) {
                is OptionProvider.AB -> {
                    if (!lexiconRepository.hasLexicon(opts.a.from)) {
                        issues += Issue(template.id, "Option A lexicon missing '${opts.a.from}'")
                    }
                    if (!lexiconRepository.hasLexicon(opts.b.from)) {
                        issues += Issue(template.id, "Option B lexicon missing '${opts.b.from}'")
                    }
                }
                is OptionProvider.Taboo -> {
                    if (!lexiconRepository.hasLexicon(opts.wordFrom)) {
                        issues += Issue(template.id, "Taboo word lexicon missing '${opts.wordFrom}'")
                    }
                    if (!lexiconRepository.hasLexicon(opts.forbiddenFrom)) {
                        issues += Issue(template.id, "Taboo forbidden lexicon missing '${opts.forbiddenFrom}'")
                    }
                }
                is OptionProvider.Scatter -> {
                    if (!lexiconRepository.hasLexicon(opts.categoryFrom)) {
                        issues += Issue(template.id, "Scatter category lexicon missing '${opts.categoryFrom}'")
                    }
                    if (!lexiconRepository.hasLexicon(opts.letterFrom)) {
                        issues += Issue(template.id, "Scatter letter lexicon missing '${opts.letterFrom}'")
                    }
                }
                else -> {
                    // no-op
                }
            }
        }
        return issues.distinct()
    }

    private val SPECIAL_SLOTS = setOf(
        "target_name",
        "inbound_text",
        "player_name",
        "player",
    )
}
