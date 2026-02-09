package com.helldeck.content.generator.gold

import android.content.res.AssetManager
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.GameIds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

class GoldBank(assetManager: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cardsByGame: Map<String, List<GoldCard>> = load(assetManager)

    private fun load(assetManager: AssetManager): Map<String, List<GoldCard>> {
        val candidates = listOf("gold/gold_cards.json", "gold_cards.json")
        candidates.forEach { path ->
            val text = runCatching { assetManager.open(path).bufferedReader().use { it.readText() } }.getOrNull()
            if (text != null) {
                val parsed = parseGoldCards(text)
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return emptyMap()
    }

    private fun parseGoldCards(text: String): Map<String, List<GoldCard>> {
        runCatching {
            val goldCards: List<GoldCard> = json.decodeFromString(text)
            if (goldCards.isNotEmpty()) return goldCards.groupBy { it.game }
        }

        return runCatching {
            val root = JSONObject(text)
            val gamesObj = root.optJSONObject("games") ?: return@runCatching emptyMap<String, List<GoldCard>>()
            val byGame = mutableMapOf<String, MutableList<GoldCard>>()

            gamesObj.keys().forEach { gameKey ->
                val gameObj = gamesObj.optJSONObject(gameKey) ?: return@forEach
                val cardsArray = gameObj.optJSONArray("cards") ?: return@forEach

                for (i in 0 until cardsArray.length()) {
                    val cardObj = cardsArray.optJSONObject(i) ?: continue
                    val textValue = cardObj.optString("text").takeIf { it.isNotBlank() } ?: continue
                    val normalizedGame = gameKey.uppercase(Locale.US)
                    val card = GoldCard(
                        id = cardObj.optString("id").ifBlank { "${normalizedGame.lowercase(Locale.US)}_${i + 1}" },
                        game = normalizedGame,
                        family = cardObj.optString("family").ifBlank { "gold_${normalizedGame.lowercase(Locale.US)}" },
                        text = textValue.trim(),
                        spice = cardObj.optInt("spice", 1),
                        locality = cardObj.optInt("locality", 1),
                        options = null,
                    )
                    byGame.getOrPut(normalizedGame) { mutableListOf() }.add(card)
                }
            }
            byGame
        }.getOrDefault(emptyMap())
    }

    fun draw(game: String, random: Random, maxSpice: Int = 5): GoldCard? {
        val list = cardsByGame[game] ?: cardsByGame[game.uppercase(Locale.US)].orEmpty()
        val filtered = list.filter { it.spice <= maxSpice }
        if (filtered.isEmpty()) return null
        return filtered[random.nextInt(filtered.size)]
    }

    fun toFilledCard(card: GoldCard): FilledCard {
        return FilledCard(
            id = card.id,
            game = card.game,
            text = card.text,
            family = card.family,
            spice = card.spice,
            locality = card.locality,
        )
    }

    fun toGameOptions(card: GoldCard, filledSlots: Map<String, String>): GameOptions {
        val normalizedGame = card.game.uppercase(Locale.US)
        val default = defaultOptionsFor(normalizedGame)
        return when (val opt = card.options) {
            is GoldOptions.PlayerVote -> GameOptions.SeatVote(listOf(1, 2, 3))
            is GoldOptions.AB -> {
                val optionA = opt.optionA ?: filledSlots[opt.slotA ?: "perk"] ?: "Option A"
                val optionB = opt.optionB ?: filledSlots[opt.slotB ?: "gross"] ?: "Option B"
                GameOptions.AB(optionA, optionB)
            }
            null -> default
        }
    }

    private fun defaultOptionsFor(game: String): GameOptions {
        return when (game) {
            GameIds.ROAST_CONS -> GameOptions.SeatVote(listOf(1, 2, 3))
            GameIds.CONFESS_CAP -> GameOptions.TrueFalse
            GameIds.POISON_PITCH,
            GameIds.RED_FLAG,
            GameIds.OVER_UNDER,
            GameIds.TEXT_TRAP -> GameOptions.AB("Option A", "Option B")
            GameIds.FILLIN -> GameOptions.Challenge("Fill in the blanks")
            GameIds.HOTSEAT_IMP,
            GameIds.UNIFYING_THEORY,
            GameIds.TITLE_FIGHT -> GameOptions.Challenge("Freestyle challenge")
            GameIds.REALITY_CHECK -> GameOptions.SeatSelect(listOf(1, 2, 3), null)
            GameIds.TABOO -> GameOptions.Taboo("Secret word", listOf("forbidden 1", "forbidden 2", "forbidden 3"))
            GameIds.SCATTER -> GameOptions.Scatter("Category", "A")
            GameIds.ALIBI -> GameOptions.HiddenWords(listOf("word1", "word2", "word3"))
            else -> GameOptions.None
        }
    }
}

@Serializable
data class GoldCard(
    val id: String,
    val game: String,
    val family: String,
    val text: String,
    val spice: Int = 1,
    val locality: Int = 1,
    val options: GoldOptions? = null,
)

@Serializable
sealed class GoldOptions {
    @Serializable
    @SerialName("PLAYER_VOTE")
    object PlayerVote : GoldOptions()

    @Serializable
    @SerialName("AB")
    data class AB(
        @SerialName("optionA") val optionA: String? = null,
        @SerialName("optionB") val optionB: String? = null,
        @SerialName("slotA") val slotA: String? = null,
        @SerialName("slotB") val slotB: String? = null,
    ) : GoldOptions()
}
