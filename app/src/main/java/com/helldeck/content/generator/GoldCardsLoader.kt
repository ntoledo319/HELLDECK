package com.helldeck.content.generator

import android.content.Context
import com.helldeck.engine.GameIds
import org.json.JSONArray

/**
 * Loads high-quality gold standard cards from assets.
 * These serve as examples for LLM prompts and emergency fallbacks.
 */
object GoldCardsLoader {

    // Feedback-based quality thresholds
    const val AUTO_EXCLUDE_THRESHOLD = 0.15f  // Cards below this score are never shown
    const val LOW_PRIORITY_THRESHOLD = 0.35f  // Cards below this are shown less often

    data class GoldCard(
        val id: String = "",  // Unique identifier for feedback tracking
        val text: String,
        val quality_score: Int,
        val spice: Int,
        val optionA: String? = null,
        val optionB: String? = null,
        val word: String? = null,
        val forbidden: List<String>? = null,
        val items: List<String>? = null,
        val words: List<String>? = null,
        val product: String? = null,
        val category: String? = null,
        val letter: String? = null,
        val tones: List<String>? = null,
    )

    private var goldCards: Map<String, List<GoldCard>>? = null

    fun load(context: Context): Map<String, List<GoldCard>> {
        if (goldCards != null) return goldCards!!

        val json = context.assets.open("gold/gold_cards_v2.json").bufferedReader().use { it.readText() }
        val cardsArray = JSONArray(json)

        val grouped = mutableMapOf<String, MutableList<GoldCard>>()

        for (i in 0 until cardsArray.length()) {
            val cardObj = cardsArray.getJSONObject(i)
            val text = cardObj.optString("text", "")
            val family = cardObj.optString("family", "")
            if (text.isBlank() || family.isBlank()) continue

            val cardId = cardObj.optString("id").takeIf { it.isNotEmpty() }
                ?: "${family}_${text.hashCode().toString(16)}"

            val card = GoldCard(
                id = cardId,
                text = text,
                quality_score = cardObj.optInt("quality_score", 7),
                spice = cardObj.optInt("spice", 2),
                optionA = cardObj.optString("optionA").takeIf { it.isNotEmpty() },
                optionB = cardObj.optString("optionB").takeIf { it.isNotEmpty() },
                word = cardObj.optString("word").takeIf { it.isNotEmpty() },
                forbidden = cardObj.optJSONArray("forbidden")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                items = cardObj.optJSONArray("items")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                words = cardObj.optJSONArray("words")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
                product = cardObj.optString("product").takeIf { it.isNotEmpty() },
                category = cardObj.optString("category").takeIf { it.isNotEmpty() },
                letter = cardObj.optString("letter").takeIf { it.isNotEmpty() },
                tones = cardObj.optJSONArray("tones")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                },
            )

            grouped.getOrPut(family) { mutableListOf() }.add(card)
        }

        goldCards = grouped
        return grouped
    }

    fun getExamplesForGame(
        context: Context,
        gameId: String,
        count: Int = 10, // Increased from 5 to 10
        seed: Int? = null, // Optional seed for deterministic rotation
        maxSpice: Int = 5, // Filter examples by spice level
        cardScores: Map<String, Float>? = null, // Optional feedback-based scores
    ): List<GoldCard> {
        val allCards = load(context)
        val gameKey = when (gameId) {
            GameIds.ROAST_CONS -> "roast_consensus"
            GameIds.CONFESS_CAP -> "confession_or_cap"
            GameIds.POISON_PITCH -> "poison_pitch"
            GameIds.FILL_IN -> "fill_in_finisher"
            GameIds.RED_FLAG -> "red_flag_rally"
            GameIds.HOTSEAT_IMP -> "hot_seat_imposter"
            GameIds.TEXT_TRAP -> "text_thread_trap"
            GameIds.TABOO -> "taboo_timer"
            GameIds.TITLE_FIGHT -> "title_fight"
            GameIds.ALIBI -> "alibi_drop"
            GameIds.SCATTER -> "scatterblast"

            // New games from HDRealRules.md
            GameIds.UNIFYING_THEORY -> "the_unifying_theory"
            GameIds.REALITY_CHECK -> "reality_check"
            GameIds.OVER_UNDER -> "over_under"

            else -> return emptyList()
        }

        // Filter by quality score, spice, and feedback-based exclusion
        val eligible = allCards[gameKey]
            ?.filter { card ->
                val feedbackScore = cardScores?.get(card.id) ?: 0.5f
                card.quality_score >= 7 &&
                    card.spice <= maxSpice &&
                    feedbackScore >= AUTO_EXCLUDE_THRESHOLD
            }
            ?: return emptyList()

        if (eligible.isEmpty()) return emptyList()

        // If no seed provided, use top cards sorted by quality (for consistency/testing)
        if (seed == null) {
            return eligible
                .sortedByDescending { it.quality_score }
                .take(count)
        }

        // Weighted random selection: combines quality_score with feedback-based scores
        val rng = kotlin.random.Random(seed)

        return eligible
            .map { card ->
                // Base weight from quality score
                val baseWeight = card.quality_score.toFloat()

                // Feedback multiplier: cards with good feedback get boosted
                val feedbackScore = cardScores?.get(card.id) ?: 0.5f
                val feedbackMultiplier = when {
                    feedbackScore >= 0.7f -> 1.5f  // High performers get 50% boost
                    feedbackScore >= LOW_PRIORITY_THRESHOLD -> 1.0f  // Normal
                    else -> 0.5f  // Low performers get 50% penalty
                }

                // Random factor for variety (0.7 to 1.0)
                val randomFactor = 0.7f + rng.nextFloat() * 0.3f

                val weight = baseWeight * feedbackMultiplier * randomFactor
                card to weight
            }
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }
    }

    fun getRandomFallback(context: Context, gameId: String, maxSpice: Int = 5): GoldCard? {
        val allCards = load(context)
        val gameKey = when (gameId) {
            GameIds.ROAST_CONS -> "roast_consensus"
            GameIds.CONFESS_CAP -> "confession_or_cap"
            GameIds.POISON_PITCH -> "poison_pitch"
            GameIds.FILL_IN -> "fill_in_finisher"
            GameIds.RED_FLAG -> "red_flag_rally"
            GameIds.HOTSEAT_IMP -> "hot_seat_imposter"
            GameIds.TEXT_TRAP -> "text_thread_trap"
            GameIds.TABOO -> "taboo_timer"
            GameIds.TITLE_FIGHT -> "title_fight"
            GameIds.ALIBI -> "alibi_drop"
            GameIds.SCATTER -> "scatterblast"

            // New games
            GameIds.UNIFYING_THEORY -> "the_unifying_theory"
            GameIds.REALITY_CHECK -> "reality_check"
            GameIds.OVER_UNDER -> "over_under"

            else -> return null
        }

        return allCards[gameKey]
            ?.filter { it.spice <= maxSpice }
            ?.randomOrNull()
    }
}
