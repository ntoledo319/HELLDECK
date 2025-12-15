package com.helldeck.ui.events

import com.helldeck.content.quality.Rating

/**
 * Sealed class representing all possible user events during a round.
 * Provides type-safe event handling for interactions.
 */
sealed class RoundEvent {

    // A/B Choice events
    data class PickAB(val choice: String) : RoundEvent() // "A" or "B"

    // Voting events
    data class VotePlayer(val playerIndex: Int) : RoundEvent()
    data class PreChoice(val choice: String) : RoundEvent() // For prediction games

    // Text input events
    data class EnterText(val text: String) : RoundEvent()

    // Player selection events
    data class SelectTarget(val playerIndex: Int) : RoundEvent()

    // Taboo events
    data class SubmitTabooGuess(val guess: String) : RoundEvent()
    object StartTabooTimer : RoundEvent()

    // Navigation/Phase events
    object ConfirmReveal : RoundEvent()
    object AdvancePhase : RoundEvent()
    object Skip : RoundEvent()

    // Feedback events
    data class RateCard(val rating: Rating) : RoundEvent()

    // Tone/option selection
    data class SelectOption(val option: String) : RoundEvent()

    // Judge pick
    data class JudgeSelect(val choiceIndex: Int) : RoundEvent()

    // Duel result
    data class DuelWinner(val winnerIndex: Int) : RoundEvent()

    // Lock-in for timed activities
    object LockIn : RoundEvent()
}
