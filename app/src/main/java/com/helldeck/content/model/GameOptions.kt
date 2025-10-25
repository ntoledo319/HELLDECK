package com.helldeck.content.model

sealed class GameOptions {
    data class AB(val optionA: String, val optionB: String): GameOptions()
    data class Taboo(val word: String, val forbidden: List<String>): GameOptions()
    data class Scatter(val category: String, val letter: String): GameOptions()
    data class PlayerVote(val players: List<String>): GameOptions()
    object TrueFalse: GameOptions()
    data class TextInput(val prompt: String): GameOptions()
    object SmashPass: GameOptions()
    data class PlayerSelect(val players: List<String>, val targetPlayer: String?): GameOptions()
    data class ReplyTone(val tones: List<String>): GameOptions()
    data class OddOneOut(val items: List<String>): GameOptions()
    data class Challenge(val challenge: String): GameOptions()
    data class HiddenWords(val words: List<String>): GameOptions()
    data class Product(val product: String): GameOptions()
    data class PredictVote(val optionA: String, val optionB: String): GameOptions()
    object None: GameOptions()
}