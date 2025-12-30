package com.helldeck.content.model.v2

import kotlinx.serialization.Serializable

@Serializable
data class SlotSpec(
    val name: String,
    val from: String,
    val transform: List<String> = emptyList(),
    val distinct_from: List<String> = emptyList(),
)

@Serializable
data class ABSource(
    val from: String,
    val transform: List<String> = emptyList(),
    val avoid_same_as: String? = null,
)

@Serializable
sealed class OptionProvider {
    @Serializable
    data class AB(val a: ABSource, val b: ABSource) : OptionProvider()

    @Serializable
    data class PlayerVote(val scope: String? = null) : OptionProvider()

    @Serializable
    data class Taboo(val wordFrom: String, val forbiddenFrom: String, val count: Int = 3) : OptionProvider()

    @Serializable
    data class Scatter(val categoryFrom: String, val letterFrom: String) : OptionProvider()

    @Serializable
    object None : OptionProvider()
}

@Serializable
data class RepeatConstraint(val horizon: Int = 10)

@Serializable
data class Constraints(
    val distinct_slots: Boolean = false,
    val no_recent_repeats: RepeatConstraint? = null,
    val allow_spice_above_threshold: Boolean = false,
    val min_players: Int? = null,
)

@Serializable
data class PostFillRule(val rule: String, val limit: Int? = null)

@Serializable
data class TemplateV2(
    val id: String,
    val version: Int = 2,
    val game: String,
    val family: String,
    val tags: List<String> = emptyList(),
    val spice: Int = 1,
    val locality: Int = 1,
    val weight: Double = 1.0,
    val min_players: Int? = null,
    val max_words: Int? = null,
    val text: String,
    val options: OptionProvider = OptionProvider.None,
    val slots: List<SlotSpec> = emptyList(),
    val constraints: Constraints = Constraints(),
    val post_fill_validators: List<PostFillRule> = emptyList(),
)
