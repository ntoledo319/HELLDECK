package com.helldeck.content.generator.gold

import android.content.res.AssetManager
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class GoldBank(assetManager: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cardsByGame: Map<String, List<GoldCard>> = load(assetManager)

    private fun load(assetManager: AssetManager): Map<String, List<GoldCard>> {
        val path = "gold/gold_cards.json"
        return try {
            val text = assetManager.open(path).bufferedReader().use { it.readText() }
            val goldCards: List<GoldCard> = json.decodeFromString(text)
            goldCards.groupBy { it.game }
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    fun draw(game: String, random: Random): GoldCard? {
        val list = cardsByGame[game].orEmpty()
        if (list.isEmpty()) return null
        return list[random.nextInt(list.size)]
    }

    fun toFilledCard(card: GoldCard): FilledCard {
        return FilledCard(
            id = card.id,
            game = card.game,
            text = card.text,
            family = card.family,
            spice = card.spice,
            locality = card.locality
        )
    }

    fun toGameOptions(card: GoldCard, filledSlots: Map<String, String>): GameOptions {
        return when (val opt = card.options) {
            is GoldOptions.PlayerVote -> GameOptions.PlayerVote(emptyList())
            is GoldOptions.AB -> {
                val optionA = opt.optionA ?: filledSlots[opt.slotA ?: "perk"] ?: "Option A"
                val optionB = opt.optionB ?: filledSlots[opt.slotB ?: "gross"] ?: "Option B"
                GameOptions.AB(optionA, optionB)
            }
            null -> GameOptions.None
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
    val options: GoldOptions? = null
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
        @SerialName("slotB") val slotB: String? = null
    ) : GoldOptions()
}
