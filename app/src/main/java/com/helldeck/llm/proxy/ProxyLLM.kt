package com.helldeck.llm.proxy

import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM

/**
 * Lightweight, deterministic LocalLLM implementation for JVM tests.
 * Uses simple text heuristics to classify humor and clarity without native models.
 */
class ProxyLLM : LocalLLM {
    override val modelId: String = "proxy-heuristic-0.1"
    override val isReady: Boolean = true

    override suspend fun generate(system: String, user: String, cfg: GenConfig): String {
        // No-op paraphrase; return user text to keep deterministic behavior in tests
        return user.take(cfg.maxTokens.coerceAtLeast(1))
    }

    override suspend fun classifyZeroShot(text: String, labels: List<String>, cfg: GenConfig): Int {
        if (labels.isEmpty()) return 0
        val lower = text.lowercase()
        val wc = lower.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val hasPunct = text.any { it == '.' || it == '!' || it == '?' }
        val hasDash = text.contains("—") || text.contains(" - ")
        val hasBecause = lower.contains("because")
        val hasWho = lower.contains("who ") || lower.contains("who's") || lower.contains("who would") || lower.contains("most likely")
        val hasAb = lower.contains(" would you rather ") || lower.contains(" or ") && lower.contains("?")
        val braces = text.contains('{') || text.contains('}')

        fun idxOf(label: String): Int = labels.indexOfFirst { it.equals(label, ignoreCase = true) }.takeIf { it >= 0 } ?: 0

        // Humor buckets
        if (labels.any { it.equals("hilarious", true) }) {
            val hilarious = idxOf("hilarious")
            val funny = idxOf("funny")
            val ok = idxOf("ok")
            val meh = idxOf("meh")
            val offensive = idxOf("offensive")
            val nonsensical = idxOf("nonsensical")

            if (braces || wc < 5) return if (wc < 3) nonsensical else meh
            var score = 0
            if (hasBecause) score += 2
            if (hasWho) score += 1
            if (hasAb) score += 1
            if (hasDash) score += 1
            if (hasPunct) score += 1
            score += (wc in 8..22).compareTo(false) // +1 if within good length window

            return when {
                score >= 5 -> hilarious
                score >= 3 -> funny
                score >= 2 -> ok
                score == 1 -> meh
                else -> meh
            }
        }

        // Sense-making
        if (labels.size == 2 && labels.any { it.equals("makes sense", true) }) {
            val sense = idxOf("makes sense")
            val nons = idxOf("nonsensical")
            return if (!braces && wc >= 5 && hasPunct) sense else nons
        }

        // Understandable clarity
        if (labels.size == 3 && labels.any { it.equals("clear", true) }) {
            val clear = idxOf("clear")
            val somewhat = idxOf("somewhat clear")
            val unclear = idxOf("unclear")
            return when {
                braces || wc < 5 -> unclear
                wc in 6..24 && hasPunct -> clear
                else -> somewhat
            }
        }

        // Fallback: choose first label deterministically
        return 0
    }
}
