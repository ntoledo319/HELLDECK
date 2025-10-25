package com.helldeck.content.engine.augment

class Validator(
    private val profanity: Set<String>,
    private val maxSpice: Int
) {
    fun sanitize(s: String): String = s
        .replace(Regex("\\s+"), " ")
        .trim()
        .removePrefix("\"").removeSuffix("\"")

    fun accepts(s: String, maxWords: Int, spice: Int): Boolean {
        if (s.split(Regex("\\s+")).size > maxWords) return false
        if (spice <= 1 && containsProfanity(s)) return false
        return true
    }

    private fun containsProfanity(s: String): Boolean =
        profanity.any { bad -> s.contains(bad, ignoreCase = true) }
}