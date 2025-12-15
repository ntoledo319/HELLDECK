package com.helldeck.ui.state

import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.InteractionType

/**
 * Round phase progression
 */
enum class RoundPhase {
    INTRO,      // Show card + game title
    INPUT,      // Collect user input (votes, choices, etc.)
    REVEAL,     // Show results
    FEEDBACK,   // LOL/MEH/TRASH rating
    DONE        // Complete, ready for next round
}

/**
 * Authoritative state for a single game round.
 * Contains ALL data needed to render the round correctly.
 * Generated once by GameEngine, never recomputed.
 */
data class RoundState(
    val gameId: String,
    val filledCard: FilledCard,
    val options: GameOptions,
    val timerSec: Int,
    val interactionType: InteractionType,
    val activePlayerIndex: Int,
    val judgePlayerIndex: Int? = null,
    val targetPlayerIndex: Int? = null,
    val phase: RoundPhase = RoundPhase.INTRO,
    val createdAtMs: Long = System.currentTimeMillis(),
    val sessionId: String
) {
    /**
     * Update phase without recreating entire state
     */
    fun withPhase(newPhase: RoundPhase): RoundState =
        copy(phase = newPhase)

    /**
     * Check if timer should be running
     */
    fun isTimerActive(): Boolean =
        timerSec > 0 && phase == RoundPhase.INPUT
}
