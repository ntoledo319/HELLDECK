package com.helldeck.content.generator

import android.content.Context
import com.helldeck.engine.GameIds
import org.json.JSONObject

/**
 * Loads high-quality gold standard cards from assets.
 * These serve as examples for LLM prompts and emergency fallbacks.
 */
object GoldCardsLoader {

    data class GoldCard(
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

        val json = context.assets.open("gold_cards.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val games = root.getJSONObject("games")

        val loaded = mutableMapOf<String, List<GoldCard>>()

        games.keys().forEach { gameKey ->
            val gameObj = games.getJSONObject(gameKey)
            val cardsArray = gameObj.getJSONArray("cards")
            val cards = mutableListOf<GoldCard>()

            for (i in 0 until cardsArray.length()) {
                val cardObj = cardsArray.getJSONObject(i)
                cards.add(
                    GoldCard(
                        text = cardObj.optString("text", ""),
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
                    ),
                )
            }

            loaded[gameKey] = cards
        }

        goldCards = loaded
        return loaded
    }

    fun getExamplesForGame(
        context: Context,
        gameId: String,
        count: Int = 10, // Increased from 5 to 10
        seed: Int? = null, // Optional seed for deterministic rotation
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

        val eligible = allCards[gameKey]
            ?.filter { it.quality_score >= 7 } // Only gold-tier cards (7+)
            ?: return emptyList()

        if (eligible.isEmpty()) return emptyList()

        // If no seed provided, use top cards sorted by quality (for consistency/testing)
        if (seed == null) {
            return eligible
                .sortedByDescending { it.quality_score }
                .take(count)
        }

        // Weighted random selection: higher quality scores = higher probability
        // This ensures ALL cards can train the AI, but better cards appear more often
        val rng = kotlin.random.Random(seed)

        return eligible
            .map { card ->
                // Weight calculation: quality_score * (0.5 to 1.0 random factor)
                // Score 10: weight 5.0-10.0
                // Score 9:  weight 4.5-9.0
                // Score 8:  weight 4.0-8.0
                // Score 7:  weight 3.5-7.0
                val weight = card.quality_score * (0.5 + rng.nextDouble() * 0.5)
                card to weight
            }
            .sortedByDescending { it.second } // Sort by weight
            .take(count) // Take top N weighted cards
            .map { it.first } // Extract the cards
    }

    fun getRandomFallback(context: Context, gameId: String): GoldCard? {
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

        return allCards[gameKey]?.randomOrNull()
    }
}
