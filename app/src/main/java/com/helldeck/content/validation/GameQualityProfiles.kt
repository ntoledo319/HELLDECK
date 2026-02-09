package com.helldeck.content.validation

import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.GameIds
import com.helldeck.engine.InteractionType
import kotlin.math.min

/**
 * Per-game quality profiles + evaluator.
 * Combines structural checks, generator metadata, CardQualityInspector, and optional AI signals.
 */
object GameQualityProfiles {
    enum class Issue {
        TOO_SHORT,
        TOO_LONG,
        EXCESS_REPEAT,
        PLACEHOLDER,
        OPTIONS_BAD,
        TOO_VAGUE,
        LACKS_CONTRAST,
        NOT_TARGETED,
        LOW_PAIR_SCORE,
        LOW_HUMOR,
        LLM_NOT_FUNNY,
        UNCLEAR,
    }

    data class Evaluation(
        val pass: Boolean,
        val score01: Double, // 0..1 composite
        val issues: List<Issue>,
        val metrics: Map<String, Any?>,
    )

    data class QualityProfile(
        val minWords: Int = 5,
        val maxWords: Int = 28,
        val maxRepeatRatio: Double = 0.4,
        val minPairScore: Double? = null,
        val minHumor: Double? = null,
        val requireOptions: Boolean = false,
        val requireContrastAB: Boolean = false,
        val requireTargeting: Boolean = false,
    )

    private val profiles: Map<String, QualityProfile> = mapOf(
        GameIds.ROAST_CONS to QualityProfile(
            minWords = 6, minHumor = 0.35, requireTargeting = true,
        ),
        GameIds.CONFESS_CAP to QualityProfile(
            minWords = 5, minHumor = 0.35,
        ),
        GameIds.POISON_PITCH to QualityProfile(
            minWords = 6, minHumor = 0.30, requireOptions = true, requireContrastAB = true,
        ),
        GameIds.FILLIN to QualityProfile(
            minWords = 5, minHumor = 0.35,
        ),
        GameIds.RED_FLAG to QualityProfile(
            minWords = 6, minHumor = 0.40, requireOptions = true, requireContrastAB = true,
        ),
        GameIds.HOTSEAT_IMP to QualityProfile(
            minWords = 6, minHumor = 0.35,
        ),
        GameIds.TEXT_TRAP to QualityProfile(
            minWords = 5, minHumor = 0.35, requireOptions = true,
        ),
        GameIds.TABOO to QualityProfile(
            minWords = 3, minHumor = null, requireOptions = true,
        ),
        GameIds.TITLE_FIGHT to QualityProfile(
            minWords = 5, minHumor = 0.35,
        ),
        GameIds.ALIBI to QualityProfile(
            minWords = 5, minHumor = 0.35, requireOptions = true,
        ),
        GameIds.SCATTER to QualityProfile(
            minWords = 3, minHumor = null, requireOptions = true,
        ),
        GameIds.UNIFYING_THEORY to QualityProfile(
            minWords = 5, minHumor = 0.35, requireOptions = false,
        ),
        GameIds.REALITY_CHECK to QualityProfile(
            minWords = 5, minHumor = 0.30, requireOptions = false,
        ),
        GameIds.OVER_UNDER to QualityProfile(
            minWords = 5, minHumor = 0.30, requireOptions = false,
        ),
    )

    /**
     * Evaluate a card against a per-game quality profile using:
     * - Word length and repetition
     * - Placeholder leftovers
     * - Options structure and AB contrast (if applicable)
     * - Generator metadata (pairScore, humorScore when present)
     * - Optional LLM signals (humor/sense/clarity)
     */
    suspend fun evaluate(
        gameId: String,
        @Suppress("UNUSED_PARAMETER") interaction: InteractionType,
        card: FilledCard,
        options: GameOptions,
    ): Evaluation {
        val profile = profiles[gameId] ?: QualityProfile()
        val issues = mutableListOf<Issue>()

        val text = card.text.trim()
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wc = words.size
        if (wc < profile.minWords) issues += Issue.TOO_SHORT
        if (wc > profile.maxWords) issues += Issue.TOO_LONG
        if ('{' in text || '}' in text) issues += Issue.PLACEHOLDER

        val counts = words.groupingBy { it.lowercase() }.eachCount()
        val top = counts.values.maxOrNull() ?: 0
        val repeatRatio = if (wc == 0) 0.0 else top.toDouble() / wc
        if (repeatRatio > profile.maxRepeatRatio) issues += Issue.EXCESS_REPEAT

        // Options structure
        val optionsOk = when (options) {
            is GameOptions.AB -> options.optionA.isNotBlank() && options.optionB.isNotBlank() &&
                !options.optionA.equals(options.optionB, true)
            is GameOptions.SeatVote -> options.seatNumbers.distinct().size >= 2
            is GameOptions.Taboo -> options.word.isNotBlank() && options.forbidden.count { it.isNotBlank() } >= 3
            is GameOptions.Scatter -> options.category.isNotBlank() && options.letter.length == 1
            is GameOptions.ReplyTone -> options.tones.distinct().size >= 3
            is GameOptions.OddOneOut -> options.items.distinct().size >= 3
            is GameOptions.HiddenWords -> options.words.distinct().size >= 2
            is GameOptions.Product -> options.product.isNotBlank()
            is GameOptions.PredictVote -> options.optionA.isNotBlank() && options.optionB.isNotBlank()
            is GameOptions.Challenge -> options.challenge.isNotBlank()
            is GameOptions.TextInput, GameOptions.TrueFalse, GameOptions.SmashPass, is GameOptions.SeatSelect, GameOptions.None -> true
        }
        if (profile.requireOptions && !optionsOk) issues += Issue.OPTIONS_BAD

        // AB contrast hint (length/character variety proxy)
        if (profile.requireContrastAB && options is GameOptions.AB) {
            val a = options.optionA.trim().lowercase()
            val b = options.optionB.trim().lowercase()
            if (!hasContrast(a, b)) issues += Issue.LACKS_CONTRAST
        }

        // Targeting hint for roast/vote games: stronger detection using keywords or noun-ish specificity
        if (profile.requireTargeting) {
            val targeted = isTargetedText(text)
            val nounsish = words.count { it.endsWith("er") || it.endsWith("ist") || it.endsWith("tion") }
            if (!targeted && nounsish < 1) issues += Issue.NOT_TARGETED
        }

        // Generator metadata
        val pairScore = (card.metadata["pairScore"] as? Number)?.toDouble()
        val humorScore = (card.metadata["humorScore"] as? Number)?.toDouble()
        if (profile.minPairScore != null && (pairScore ?: 0.0) < profile.minPairScore) issues += Issue.LOW_PAIR_SCORE
        if (profile.minHumor != null && humorScore != null && humorScore < profile.minHumor) issues += Issue.LOW_HUMOR

        // Optional LLM judge
        val ai = try { FunnyJudge.evaluate(card) } catch (_: Exception) { FunnyJudge.AiJudgment(null, null, null) }
        if (ai.humor01 != null && ai.humor01 < 0.35) issues += Issue.LLM_NOT_FUNNY
        if (ai.understandable01 != null && ai.understandable01 < 0.5) issues += Issue.UNCLEAR

        // Also fold CardQualityInspector issues to avoid gaps
        try {
            val inspectorIssues = CardQualityInspector.evaluate(card, options)
            if (inspectorIssues.isNotEmpty()) {
                if (inspectorIssues.contains(
                        CardQualityInspector.Issue.PLACEHOLDER_LEFTOVER,
                    )
                ) {
                    issues += Issue.PLACEHOLDER
                }
                if (inspectorIssues.contains(CardQualityInspector.Issue.TOO_SHORT)) issues += Issue.TOO_SHORT
                if (inspectorIssues.contains(CardQualityInspector.Issue.TOO_LONG)) issues += Issue.TOO_LONG
                if (inspectorIssues.contains(CardQualityInspector.Issue.EXCESS_REPEAT)) issues += Issue.EXCESS_REPEAT
                if (inspectorIssues.contains(CardQualityInspector.Issue.OPTIONS_UNUSABLE)) issues += Issue.OPTIONS_BAD
            }
        } catch (_: Exception) {}

        // Composite score: structure (0.3) + humor (0.5) + AI (0.2)
        val structIssues = listOf(
            Issue.TOO_SHORT,
            Issue.TOO_LONG,
            Issue.EXCESS_REPEAT,
            Issue.PLACEHOLDER,
            Issue.OPTIONS_BAD,
        )
            .count { issues.contains(it) }
        val structure = (1.0 - (structIssues.toDouble() / 5.0)).coerceIn(0.0, 1.0)
        val humorComp = humorScore?.let { if (it.isFinite()) it else 0.4 } ?: 0.4
        val aiComp = listOfNotNull(ai.humor01, ai.makesSense01, ai.understandable01).ifEmpty { listOf(0.6) }.average()
        val score = (0.35 * structure) + (0.45 * humorComp) + (0.2 * aiComp)

        val pass = issues.isEmpty() || score >= 0.60
        return Evaluation(
            pass = pass,
            score01 = score.coerceIn(0.0, 1.0),
            issues = issues,
            metrics = mapOf(
                "wordCount" to wc,
                "repeatRatio" to repeatRatio,
                "pairScore" to pairScore,
                "humorScore" to humorScore,
                "aiHumor" to ai.humor01,
                "aiSense" to ai.makesSense01,
                "aiUnderstandable" to ai.understandable01,
            ),
        )
    }

    private fun tokenize(s: String): Set<String> = s
        .replace(Regex("[^a-z0-9 ]"), " ")
        .split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val inter = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union == 0.0) 1.0 else inter / union
    }

    private fun hasContrast(a: String, b: String): Boolean {
        val ta = tokenize(a)
        val tb = tokenize(b)
        val sim = jaccard(ta, tb)
        if (sim > 0.6) return false
        // if the first 5 chars match (after strip), likely similar phrasing
        val sa = a.take(min(5, a.length))
        val sb = b.take(min(5, b.length))
        if (sa == sb) return false
        return true
    }

    private fun isTargetedText(text: String): Boolean {
        val t = text.lowercase()
        val keywords = listOf(
            "most likely", "who ", "who's", "whoever", "who would", "who'd", "who’d",
            "call out", "point at", "pick", "vote", "name the", "tag the",
            "among", "in the room", "because",
        )
        if (keywords.any { it in t }) return true
        val hasYou = t.contains("you ") || t.contains("you're") || t.contains("your ")
        val socialVerbs = listOf("accuse", "roast", "point at", "choose", "select", "call out", "name")
        if (hasYou && socialVerbs.any { it in t }) return true
        return false
    }
}
