package com.helldeck.content.model

/**
 * Anonymous session participant - replaces Player for privacy-focused gameplay.
 * No personal identifiers are stored; participants are identified by seat number only.
 * 
 * @property seatNumber The seat position (1-based) in the current session
 * @property avatar Visual identifier (emoji or icon) for UI display
 * @property sessionPoints Points accumulated in the current game session
 * @property isActive Whether this seat is currently occupied
 */
data class SessionParticipant(
    val seatNumber: Int,
    val avatar: String = getDefaultAvatar(seatNumber),
    val sessionPoints: Int = 0,
    val isActive: Boolean = true,
) {
    companion object {
        private val DEFAULT_AVATARS = listOf(
            "🔴", "🔵", "🟢", "🟡", "🟣", "🟠", "⚪", "🟤",
            "❤️", "💙", "💚", "💛", "💜", "🧡", "🤍", "🤎",
            "🌟", "⭐", "💫", "✨", "🌙", "☀️", "🌈", "🔥"
        )
        
        fun getDefaultAvatar(seatNumber: Int): String {
            return DEFAULT_AVATARS.getOrElse(seatNumber - 1) { "🎮" }
        }
        
        /**
         * Create a list of participants for a given seat count
         */
        fun createSeats(count: Int): List<SessionParticipant> {
            return (1..count).map { seatNumber ->
                SessionParticipant(seatNumber = seatNumber)
            }
        }
    }
    
    /**
     * Display label for UI (e.g., "Seat 1" or "Seat 3")
     */
    val displayLabel: String get() = "Seat $seatNumber"
    
    /**
     * Short label for compact UI
     */
    val shortLabel: String get() = "#$seatNumber"
}

/**
 * Extension to add points to a participant
 */
fun SessionParticipant.withAddedPoints(points: Int): SessionParticipant {
    return this.copy(sessionPoints = this.sessionPoints + points)
}

/**
 * Extension to reset points
 */
fun SessionParticipant.withResetPoints(): SessionParticipant {
    return this.copy(sessionPoints = 0)
}
