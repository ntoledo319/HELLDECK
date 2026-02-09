package com.helldeck.content.validation

import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.InteractionType

/**
 * Validates that generated cards satisfy game interaction contracts.
 * Prevents rule-breaking cards from reaching the UI.
 */
object GameContractValidator {

    /**
     * Result of contract validation
     */
    data class ContractResult(
        val isValid: Boolean,
        val reasons: List<String> = emptyList(),
    ) {
        companion object {
            fun valid() = ContractResult(true, emptyList())
            fun invalid(vararg reasons: String) = ContractResult(false, reasons.toList())
        }
    }

    /**
     * Validates a card against game interaction contract.
     *
     * @param gameId The game identifier
     * @param interactionType The required interaction type
     * @param options The game options provided
     * @param filledCard The generated card
     * @param playersCount Number of players in the game
     * @return ContractResult indicating validity and reasons for failure
     */
    fun validate(
        gameId: String,
        interactionType: InteractionType,
        options: GameOptions,
        filledCard: FilledCard,
        playersCount: Int,
    ): ContractResult {
        val failures = mutableListOf<String>()

        if (gameId.isBlank()) {
            failures.add("Missing gameId")
        }

        // Global validation: card text must not contain unresolved placeholders
        if (filledCard.text.contains("{") || filledCard.text.contains("}")) {
            failures.add("Card contains unresolved placeholder: ${filledCard.text}")
        }

        // Global validation: card must not contain "null" as text
        if (filledCard.text.contains("null", ignoreCase = true)) {
            failures.add("Card contains 'null' text")
        }

        // Global validation: reasonable word count (4-50 words)
        val wordCount = filledCard.text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        if (wordCount < 4) {
            failures.add("Card too short: $wordCount words")
        }
        if (wordCount > 50) {
            failures.add("Card too long: $wordCount words")
        }

        // Interaction-specific validation
        when (interactionType) {
            InteractionType.A_B_CHOICE -> {
                when (options) {
                    is GameOptions.AB -> {
                        if (options.optionA.isBlank()) {
                            failures.add("A_B_CHOICE requires non-empty optionA")
                        }
                        if (options.optionB.isBlank()) {
                            failures.add("A_B_CHOICE requires non-empty optionB")
                        }
                        if (options.optionA.equals(options.optionB, ignoreCase = true)) {
                            failures.add(
                                "A_B_CHOICE options must be different: '${options.optionA}' vs '${options.optionB}'",
                            )
                        }
                    }
                    else -> failures.add("A_B_CHOICE requires GameOptions.AB, got ${options::class.simpleName}")
                }
            }

            InteractionType.VOTE_PLAYER, InteractionType.VOTE_SEAT -> {
                when (options) {
                    is GameOptions.SeatVote -> {
                        if (options.seatNumbers.size < 2) {
                            failures.add("VOTE requires at least 2 seats, got ${options.seatNumbers.size}")
                        }
                        if (playersCount < 2) {
                            failures.add("VOTE requires at least 2 participants in game, got $playersCount")
                        }
                    }
                    else -> failures.add(
                        "VOTE requires GameOptions.SeatVote, got ${options::class.simpleName}",
                    )
                }
            }

            InteractionType.TABOO_GUESS -> {
                when (options) {
                    is GameOptions.Taboo -> {
                        if (options.word.isBlank()) {
                            failures.add("TABOO_GUESS requires non-empty target word")
                        }
                        if (options.forbidden.isEmpty()) {
                            failures.add("TABOO_GUESS requires forbidden words list")
                        }
                        if (options.forbidden.any { it.isBlank() }) {
                            failures.add("TABOO_GUESS forbidden list contains empty entries")
                        }
                    }
                    else -> failures.add("TABOO_GUESS requires GameOptions.Taboo, got ${options::class.simpleName}")
                }
            }

            InteractionType.TRUE_FALSE -> {
                if (options !is GameOptions.TrueFalse) {
                    failures.add("TRUE_FALSE requires GameOptions.TrueFalse, got ${options::class.simpleName}")
                }
            }

            InteractionType.SMASH_PASS -> {
                // Can use AB or SmashPass options
                when (options) {
                    is GameOptions.AB -> {
                        // Valid: A/B options for smash/pass
                    }
                    is GameOptions.SmashPass -> {
                        // Valid: dedicated smash/pass
                    }
                    else -> failures.add(
                        "SMASH_PASS requires GameOptions.AB or SmashPass, got ${options::class.simpleName}",
                    )
                }
            }

            InteractionType.TARGET_SELECT, InteractionType.SELF_RATE -> {
                when (options) {
                    is GameOptions.SeatSelect -> {
                        if (options.seatNumbers.isEmpty()) {
                            failures.add("TARGET_SELECT requires seats list")
                        }
                    }
                    is GameOptions.SeatVote,
                    is GameOptions.Challenge,
                    GameOptions.None -> {
                        // These are all acceptable for target selection / self-rating games
                    }
                    else -> {
                        failures.add(
                            "TARGET_SELECT requires GameOptions.SeatSelect, SeatVote, Challenge, or None, got ${options::class.simpleName}",
                        )
                    }
                }
            }

            InteractionType.JUDGE_PICK -> {
                // Requires at least 2 players (1 judge + 1 participant)
                if (playersCount < 3) {
                    failures.add("JUDGE_PICK typically requires at least 3 players, got $playersCount")
                }
            }

            InteractionType.REPLY_TONE -> {
                when (options) {
                    is GameOptions.ReplyTone -> {
                        if (options.tones.isEmpty()) {
                            failures.add("REPLY_TONE requires tone options")
                        }
                    }
                    else -> failures.add("REPLY_TONE requires GameOptions.ReplyTone, got ${options::class.simpleName}")
                }
            }

            InteractionType.ODD_EXPLAIN -> {
                when (options) {
                    is GameOptions.OddOneOut -> {
                        if (options.items.size < 3) {
                            failures.add("ODD_EXPLAIN requires at least 3 items, got ${options.items.size}")
                        }
                    }
                    else -> failures.add("ODD_EXPLAIN requires GameOptions.OddOneOut, got ${options::class.simpleName}")
                }
            }

            InteractionType.HIDE_WORDS -> {
                when (options) {
                    is GameOptions.HiddenWords -> {
                        if (options.words.isEmpty()) {
                            failures.add("HIDE_WORDS requires hidden words list")
                        }
                    }
                    else -> failures.add(
                        "HIDE_WORDS requires GameOptions.HiddenWords, got ${options::class.simpleName}",
                    )
                }
            }

            InteractionType.SALES_PITCH -> {
                when (options) {
                    is GameOptions.Product -> {
                        if (options.product.isBlank()) {
                            failures.add("SALES_PITCH requires product name")
                        }
                    }
                    else -> {
                        // Can work with None or Challenge
                        if (options !is GameOptions.None && options !is GameOptions.Challenge) {
                            failures.add("SALES_PITCH requires GameOptions.Product, Challenge, or None")
                        }
                    }
                }
            }

            InteractionType.SPEED_LIST -> {
                when (options) {
                    is GameOptions.Scatter -> {
                        if (options.category.isBlank()) {
                            failures.add("SPEED_LIST requires category")
                        }
                        if (options.letter.isBlank()) {
                            failures.add("SPEED_LIST requires letter")
                        }
                    }
                    else -> failures.add("SPEED_LIST requires GameOptions.Scatter, got ${options::class.simpleName}")
                }
            }

            InteractionType.MINI_DUEL -> {
                // Requires at least 2 players for duel
                if (playersCount < 2) {
                    failures.add("MINI_DUEL requires at least 2 players, got $playersCount")
                }
            }

            InteractionType.PREDICT_VOTE -> {
                when (options) {
                    is GameOptions.AB, is GameOptions.PredictVote -> {
                        // Valid
                    }
                    else -> failures.add(
                        "PREDICT_VOTE requires GameOptions.AB or PredictVote, got ${options::class.simpleName}",
                    )
                }
            }

            InteractionType.NONE -> {
                // No special requirements
            }
        }

        return if (failures.isEmpty()) {
            ContractResult.valid()
        } else {
            ContractResult(false, failures)
        }
    }
}
