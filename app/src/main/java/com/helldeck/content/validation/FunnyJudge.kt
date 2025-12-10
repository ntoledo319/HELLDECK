package com.helldeck.content.validation

import com.helldeck.content.model.FilledCard
import com.helldeck.llm.LocalLLM
import com.helldeck.content.engine.ContentEngineProvider

/**
 * Optional AI-based judge that rates cards for humor and sense-making using a local LLM when available.
 * Falls back to nulls if no model is ready.
 */
object FunnyJudge {
    data class AiJudgment(
        val humor01: Double?,         // 0.0 .. 1.0, higher is funnier
        val makesSense01: Double?,    // 0.0 .. 1.0, higher makes more sense
        val understandable01: Double? // 0.0 .. 1.0, clarity/readability
    )

    /**
     * Attempt to evaluate with a local LLM. Returns nulls when model is unavailable.
     */
    suspend fun evaluate(card: FilledCard): AiJudgment {
        val llm: LocalLLM = ContentEngineProvider.getLocalLLM() ?: return AiJudgment(null, null, null)
        if (!llm.isReady) return AiJudgment(null, null, null)

        // Humor classification (6 buckets mapped to 0..1)
        val humorLabels = listOf("hilarious", "funny", "ok", "meh", "offensive", "nonsensical")
        val humorIdx = llm.classifyZeroShot(
            text = card.text,
            labels = humorLabels
        ).coerceIn(0, humorLabels.lastIndex)
        val humor01 = when (humorLabels[humorIdx]) {
            "hilarious" -> 1.0
            "funny" -> 0.85
            "ok" -> 0.6
            "meh" -> 0.35
            "offensive" -> 0.1
            else -> 0.0 // nonsensical
        }

        // Sense-making: binary classification
        val senseLabels = listOf("makes sense", "nonsensical")
        val senseIdx = llm.classifyZeroShot(card.text, senseLabels).coerceIn(0, 1)
        val makesSense01 = if (senseIdx == 0) 1.0 else 0.0

        // Understandable: proxy with a 3-class clarity rating
        val understandLabels = listOf("clear", "somewhat clear", "unclear")
        val understandIdx = llm.classifyZeroShot(card.text, understandLabels).coerceIn(0, 2)
        val understandable01 = when (understandIdx) {
            0 -> 1.0
            1 -> 0.65
            else -> 0.25
        }

        return AiJudgment(humor01, makesSense01, understandable01)
    }
}

