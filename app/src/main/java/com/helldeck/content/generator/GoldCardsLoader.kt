package com.helldeck.content.generator

import android.content.Context
import com.helldeck.engine.GameIds
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

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
        val tones: List<String>? = null
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
                        }
                    )
                )
            }

            loaded[gameKey] = cards
        }

        goldCards = loaded
        return loaded
    }

    fun getExamplesForGame(context: Context, gameId: String, count: Int = 5): List<GoldCard> {
        val allCards = load(context)
        val gameKey = when (gameId) {
            GameIds.ROAST_CONS -> "roast_consensus"
            GameIds.POISON_PITCH -> "poison_pitch"
            GameIds.FILL_IN -> "fill_in_finisher"
            GameIds.RED_FLAG -> "red_flag_rally"
            GameIds.HOTSEAT_IMP -> "hotseat_imposter"
            GameIds.TEXT_TRAP -> "text_thread_trap"
            GameIds.TABOO -> "taboo_timer"
            GameIds.ODD_ONE -> "odd_one_out"
            GameIds.TITLE_FIGHT -> "title_fight"
            GameIds.ALIBI -> "alibi_drop"
            GameIds.HYPE_YIKE -> "hype_or_yike"
            GameIds.SCATTER -> "scatterblast"
            GameIds.MAJORITY -> "majority_report"
            GameIds.CONFESS_CAP -> "confess_or_cap"
            else -> return emptyList()
        }

        return allCards[gameKey]
            ?.sortedByDescending { it.quality_score }
            ?.take(count)
            ?: emptyList()
    }

    fun getRandomFallback(context: Context, gameId: String): GoldCard? {
        val allCards = load(context)
        val gameKey = when (gameId) {
            GameIds.ROAST_CONS -> "roast_consensus"
            GameIds.POISON_PITCH -> "poison_pitch"
            GameIds.FILL_IN -> "fill_in_finisher"
            GameIds.RED_FLAG -> "red_flag_rally"
            GameIds.HOTSEAT_IMP -> "hotseat_imposter"
            GameIds.TEXT_TRAP -> "text_thread_trap"
            GameIds.TABOO -> "taboo_timer"
            GameIds.ODD_ONE -> "odd_one_out"
            GameIds.TITLE_FIGHT -> "title_fight"
            GameIds.ALIBI -> "alibi_drop"
            GameIds.HYPE_YIKE -> "hype_or_yike"
            GameIds.SCATTER -> "scatterblast"
            GameIds.MAJORITY -> "majority_report"
            GameIds.CONFESS_CAP -> "confess_or_cap"
            else -> return null
        }

        return allCards[gameKey]?.randomOrNull()
    }
}
