package com.helldeck.content.engine.augment

import com.helldeck.content.model.FilledCard
import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM

class Augmentor(
    private val llm: LocalLLM?, // nullable: when disabled or model not available
    private val cache: GenerationCache,
    private val validator: Validator,
) {
    data class Plan(
        val allowParaphrase: Boolean,
        val maxWords: Int,
        val spice: Int,
        val tags: List<String>,
        val gameId: String,
        val styleGuide: String,
    )

    suspend fun maybeParaphrase(card: FilledCard, plan: Plan, seed: Int, modelId: String): FilledCard {
        if (llm == null || llm.isReady.not() || !plan.allowParaphrase) return card
        val fillHash = Integer.toHexString((card.text + plan.tags.joinToString(",")).hashCode())
        val key = cache.key("paraphrase", modelId, card.id, fillHash, seed)
        cache.get(key)?.let { return card.copy(text = it) }

        val system = buildString {
            appendLine("You are a prompt rewriter for the mobile party game HELLDECK.")
            appendLine("- Never change game semantics or the number of items.")
            appendLine("- Preserve any names, quotes, and special tokens exactly.")
            appendLine("- Respect spice: keep SFW at spice<=1; avoid slurs and targeted harassment always.")
            appendLine("- Output a single line only; no quotes around the line.")
            appendLine("- Game: ${plan.gameId}")
            appendLine("Style guide: ${plan.styleGuide}")
        }

        val user = """
            Rewrite the following prompt to be punchy and clear while keeping the exact intent and structure.
            - Must be ≤ ${plan.maxWords} words
            - Keep placeholders, names, numbers, and emoji unchanged
            - Do not add or remove options/slots or change counts
            - Favor modern social tone; avoid filler and hedging
            Text:
            ${card.text}
        """.trimIndent()

        val out = llm.generate(
            system,
            user,
            GenConfig(
                maxTokens = plan.maxWords * 2,
                temperature = if (plan.spice >= 3) 0.8f else 0.5f,
                topP = 0.9f,
                seed = seed,
            ),
        ).trim()

        val cleaned = validator.sanitize(out)
        if (cleaned.isBlank() || cleaned.equals(card.text, ignoreCase = true)) return card
        if (!validator.accepts(cleaned, plan.maxWords, plan.spice)) return card
        cache.put(key, cleaned)
        return card.copy(text = cleaned)
    }
}
