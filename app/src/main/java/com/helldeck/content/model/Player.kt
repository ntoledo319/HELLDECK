package com.helldeck.content.model

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
