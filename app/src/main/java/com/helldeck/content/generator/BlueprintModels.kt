package com.helldeck.content.generator

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplateBlueprint(
    val id: String,
    val game: String,
    val family: String,
    val weight: Double = 1.0,
    val spice_max: Int = 3,
    val locality_max: Int = 3,
    val blueprint: List<BlueprintSegment>,
    val constraints: BlueprintConstraints = BlueprintConstraints(),
    val option_provider: BlueprintOptionProvider? = null,
)

@Serializable
sealed class BlueprintSegment {
    @Serializable
    @SerialName("text")
    data class Text(val value: String) : BlueprintSegment()

    @Serializable
    @SerialName("slot")
    data class Slot(
        val name: String,
        @SerialName("slot_type") val slotType: String,
        val mods: List<String> = emptyList(),
    ) : BlueprintSegment()
}

@Serializable
data class BlueprintConstraints(
    val max_words: Int = 28,
    val distinct_slots: Boolean = false,
    val min_players: Int = 0,
)

@Serializable
data class BlueprintOptionProvider(
    val type: OptionProviderType,
    val options: List<OptionMapping> = emptyList(),
) {
    @Serializable
    enum class OptionProviderType {
        @SerialName("PLAYER_VOTE")
        PLAYER_VOTE,

        @SerialName("AB")
        AB,

        @SerialName("JUDGE_PICK")
        JUDGE_PICK,

        @SerialName("RATING_1_10")
        RATING_1_10,

        @SerialName("OVER_UNDER")
        OVER_UNDER,

        @SerialName("None")
        NONE,
    }

    @Serializable
    data class OptionMapping(
        @SerialName("from_slot") val fromSlot: String? = null,
    )
}

@Serializable
data class LexiconFile(
    val slot_type: String,
    val entries: List<LexiconEntry>,
)

@Serializable
data class LexiconEntry(
    val text: String,
    val tags: List<String> = emptyList(),
    val tone: String = "neutral",
    val spice: Int = 1,
    val locality: Int = 1,
    val pluralizable: Boolean = false,
    val needs_article: String = "none",
)
