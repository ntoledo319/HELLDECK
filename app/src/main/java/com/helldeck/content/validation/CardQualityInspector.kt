package com.helldeck.content.validation

import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions

/**
 * Heuristic quality checks for filled cards before they ever reach players.
 *
 * These checks intentionally err on the side of caution: if a card looks risky
 * (too short, unresolved placeholders, repeated filler, etc.) we ask the engine
 * to draw another option instead of showing something that will land flat.
 */
object CardQualityInspector {

    enum class Issue {
        BLANK,
        PLACEHOLDER_LEFTOVER,
        TOO_SHORT,
        TOO_LONG,
        EXCESS_REPEAT,
        SLOT_MISSING_IN_TEXT,
        OPTIONS_UNUSABLE,
        REDUNDANT_OPTIONS,
        INSTRUCTION_LEAKAGE,
    }

    private val wordRegex = Regex("[\\p{L}\\p{N}'][\\p{L}\\p{N}'-]*")

    fun evaluate(card: FilledCard, options: GameOptions): Set<Issue> {
        val issues = mutableSetOf<Issue>()
        val text = card.text.trim()
        if (text.isBlank()) {
            issues += Issue.BLANK
            return issues
        }

        if ('{' in text || '}' in text) {
            issues += Issue.PLACEHOLDER_LEFTOVER
        }

        val words = wordRegex.findAll(text).map { it.value.lowercase() }.toList()
        if (words.size < 5) {
            issues += Issue.TOO_SHORT
        }
        if (words.size > 48) {
            issues += Issue.TOO_LONG
        }

        val repeats = words.groupingBy { it }.eachCount().filter { it.value >= 3 }
        if (repeats.isNotEmpty()) {
            issues += Issue.EXCESS_REPEAT
        }

        val slotMap = (card.metadata["slots"] as? Map<*, *>)?.mapNotNull {
            val name = it.key as? String ?: return@mapNotNull null
            val value = it.value as? String ?: return@mapNotNull null
            name to value
        } ?: emptyList()

        if (slotMap.isNotEmpty()) {
            val lowerText = text.lowercase()
            slotMap.filter { (_, value) ->
                val cleaned = value.trim().lowercase()
                cleaned.isNotEmpty() && cleaned !in lowerText
            }.takeIf { it.isNotEmpty() }?.let { issues += Issue.SLOT_MISSING_IN_TEXT }
        }

        if (!optionsAreUsable(options)) {
            issues += Issue.OPTIONS_UNUSABLE
        }

        // Reject cards where AB options are near-identical
        if (options is GameOptions.AB) {
            val a = options.optionA.trim().lowercase()
            val b = options.optionB.trim().lowercase()
            if (a == b || a.contains(b) || b.contains(a)) {
                issues += Issue.REDUNDANT_OPTIONS
            }
        }

        // Reject cards leaking meta-instructions into card text
        val instructionPatterns = listOf(
            "pick the perfect", "choose your reply", "choose the mood",
            "what vibe should", "pick the reply", "choose your energy",
        )
        val lowerForInstruction = text.lowercase()
        if (instructionPatterns.any { lowerForInstruction.contains(it) }) {
            issues += Issue.INSTRUCTION_LEAKAGE
        }

        return issues
    }

    fun isAcceptable(card: FilledCard, options: GameOptions): Boolean =
        evaluate(card, options).isEmpty()

    private fun optionsAreUsable(options: GameOptions): Boolean = when (options) {
        is GameOptions.AB -> options.optionA.isNotBlank() &&
            options.optionB.isNotBlank() &&
            !options.optionA.equals(options.optionB, ignoreCase = true)
        is GameOptions.SeatVote -> options.seatNumbers.distinct().size >= 2
        is GameOptions.Taboo -> options.word.isNotBlank() && options.forbidden.count { it.isNotBlank() } >= 3
        is GameOptions.Scatter -> options.category.isNotBlank() && options.letter.length == 1
        is GameOptions.TextInput -> options.prompt.isNotBlank()
        is GameOptions.SeatSelect -> options.seatNumbers.isNotEmpty()
        is GameOptions.ReplyTone -> options.tones.distinct().size >= 3
        is GameOptions.OddOneOut -> options.items.distinct().size >= 3
        is GameOptions.Challenge -> options.challenge.isNotBlank()
        is GameOptions.HiddenWords -> options.words.distinct().size >= 2
        is GameOptions.Product -> options.product.isNotBlank()
        is GameOptions.PredictVote -> options.optionA.isNotBlank() &&
            options.optionB.isNotBlank() &&
            !options.optionA.equals(options.optionB, ignoreCase = true)
        GameOptions.TrueFalse,
        GameOptions.SmashPass,
        GameOptions.None,
        -> true
    }
}
