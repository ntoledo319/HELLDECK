package com.helldeck.content.model

/**
 * @deprecated Use [SessionParticipant] instead for anonymous seat-based sessions.
 * This class is retained for backward compatibility during migration.
 * @see SessionParticipant
 */
@Deprecated("Use SessionParticipant for anonymous seat-based sessions")
data class Player(
    val id: String,
    val name: String,
    val avatar: String,
    val sessionPoints: Int = 0,
    val totalPoints: Int = 0,
    val elo: Int = 1000,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val afk: Int = 0,
)
