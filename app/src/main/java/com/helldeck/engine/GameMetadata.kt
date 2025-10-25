package com.helldeck.engine

object GameMetadata {
    val GameDefinitions = emptyMap<String, GameInfo>()
    fun getGameMetadata(gameId: String): GameInfo? = null
    fun getAllGames(): List<GameInfo> = emptyList()
    fun getGamesByCategory(category: GameCategory): List<GameInfo> = emptyList()
    fun getGamesByDifficulty(difficulty: GameDifficulty): List<GameInfo> = emptyList()
    fun getGamesForPlayerCount(playerCount: Int): List<GameInfo> = emptyList()
    fun getGamesWithTags(tags: Set<String>): List<GameInfo> = emptyList()
    fun getRandomGame(playerCount: Int): GameInfo = GameInfo("", "", "", GameCategory.MAIN, GameDifficulty.EASY, 0, 0, 0, Interaction.JUDGE_PICK, InteractionType.NONE, emptySet(), 0)
    fun getAllGameIds(): List<String> = emptyList()
    fun getGameStats(): Map<String, Any> = emptyMap()
    fun validateAllGames(): List<String> = emptyList()
    fun suggestGameProgression(currentGameId: String, playerCount: Int): List<GameInfo> = emptyList()
}

data class GameInfo(
    val id: String,
    val title: String,
    val description: String,
    val category: GameCategory,
    val difficulty: GameDifficulty,
    val timerSec: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val interaction: Interaction,
    val interactionType: InteractionType,
    val tags: Set<String>,
    val spice: Int
)
