package com.helldeck.content.engine.augment

import com.helldeck.content.model.FilledCard
import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM

class Augmentor(
    private val llm: LocalLLM?, // nullable: when disabled or model not available
    private val cache: GenerationCache,
    private val validator: Validator
) {
    data class Plan(
        val allowParaphrase: Boolean,
        val maxWords: Int,
        val spice: Int,
        val tags: List<String>
    )

    suspend fun maybeParaphrase(card: FilledCard, plan: Plan, seed: Int, modelId: String): FilledCard {
        if (llm == null || !plan.allowParaphrase) return card
        val fillHash = Integer.toHexString((card.text + plan.tags.joinToString(",")).hashCode())
        val key = cache.key("paraphrase", modelId, card.id, fillHash, seed)
        cache.get(key)?.let { return card.copy(text = it) }

        val system = "You rewrite party game prompts safely. Respect constraints and stay SFW for spice<=1."
        val user = """
            Rewrite to be punchy, same meaning, ≤ ${plan.maxWords} words.
            Style: social, high-contrast. Keep placeholders or player names if present.
            Text: "${card.text}"
            Return only the rewritten line.
        """.trimIndent()

        val out = llm.generate(system, user, GenConfig(
            maxTokens = plan.maxWords * 2,
            temperature = if (plan.spice >= 3) 0.8f else 0.5f,
            topP = 0.9f,
            seed = seed
        )).trim()

        val cleaned = validator.sanitize(out)
        if (!validator.accepts(cleaned, plan.maxWords, plan.spice)) return card
        cache.put(key, cleaned)
        return card.copy(text = cleaned)
    }
}