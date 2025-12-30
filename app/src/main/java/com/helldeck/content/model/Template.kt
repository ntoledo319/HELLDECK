package com.helldeck.content.model

import kotlinx.serialization.Serializable

@Serializable
data class Template(
    val id: String,
    val game: String, // Must match GamesRegistry id
    val text: String, // e.g., "Most likely to {sketchy_action} for {tiny_reward}."
    val family: String, // selection family
    val spice: Int = 1, // 1..3
    val locality: Int = 1, // 1..3
    val max_words: Int = 24,
)

data class TemplateCandidate(
    val template: Template,
    val reason: String = "",
)

data class FilledCard(
    val id: String,
    val game: String,
    val text: String,
    val family: String,
    val spice: Int,
    val locality: Int,
    val metadata: Map<String, Any?> = emptyMap(),
)
