package com.helldeck.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.Player
import com.helldeck.data.PlayerEntity
import com.helldeck.data.toPlayer
import com.helldeck.data.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel focused on player management and data operations.
 * Handles player CRUD operations, rollcall, and player state.
 */
class PlayerViewModel : ViewModel() {
    
    // Player data
    var players by mutableStateOf(listOf<Player>())
    var activePlayers by mutableStateOf(listOf<Player>())
    
    // Rollcall / attendance
    private var didRollcall = false
    private var askRollcallOnLaunch = true
    
    // Core systems
    private lateinit var repo: ContentRepository
    
    /**
     * Initializes the PlayerViewModel with required dependencies.
     */
    fun initialize(repo: ContentRepository) {
        this.repo = repo
    }
    
    /**
     * Initializes the PlayerViewModel and loads initial data.
     */
    suspend fun initOnce() {
        // Load initial data
        reloadPlayers()
        
        // Determine if we should ask for rollcall on launch
        // NOTE: Settings DAO not implemented yet, using default
        askRollcallOnLaunch = true
        if (askRollcallOnLaunch && !didRollcall && players.isNotEmpty()) {
            // Signal that rollcall should be shown
            didRollcall = true
        }
    }
    
    /**
     * Reloads players from the database and updates active players.
     * Adds default players if none exist.
     */
    suspend fun reloadPlayers() {
       players = repo.db.players().getAllPlayers().first().map { it.toPlayer() }
       activePlayers = players.filter { p -> p.afk == 0 }
       
        // Add default players if none exist
        if (players.isEmpty()) {
            val defaultPlayers = listOf(
                "😎 Jay" to "😎",
                "🦊 Pip" to "🦊",
                "🐸 Mo" to "🐸"
            )
            defaultPlayers.forEach { (name, avatar) ->
                val id = "p${Random.nextInt(100000)}"
                repo.db.players().upsert(PlayerEntity(
                    id = id,
                    name = name,
                    avatar = avatar,
                    sessionPoints = 0
                ))
            }
            
            players = repo.db.players().getAllPlayers().first().map { it.toPlayer() }
            activePlayers = players.filter { p -> p.afk == 0 }
        }
    }
    
    /**
     * Adds a new player to the database.
     *
     * @param name The name of the new player
     * @param avatar The avatar for the new player
     * @return The ID of the created player
     */
    suspend fun addPlayer(name: String, avatar: String): String {
        val id = "p${Random.nextInt(100000)}"
        val playerEntity = PlayerEntity(
            id = id,
            name = name,
            avatar = avatar,
            sessionPoints = 0
        )
        
        repo.db.players().upsert(playerEntity)
        reloadPlayers()
        return id
    }
    
    /**
     * Updates an existing player.
     *
     * @param player The player to update
     */
    suspend fun updatePlayer(player: Player) {
        repo.db.players().upsert(player.toEntity())
        reloadPlayers()
    }
    
    /**
     * Removes a player from the database.
     *
     * @param playerId The ID of the player to remove
     */
    suspend fun removePlayer(playerId: String) {
        repo.db.players().deleteById(playerId)
        reloadPlayers()
    }
    
    /**
     * Toggles a player's active status (AFK state).
     *
     * @param playerId The ID of the player to toggle
     */
    suspend fun togglePlayerActive(playerId: String) {
        val player = players.find { it.id == playerId }
        player?.let {
            val updatedPlayer = it.copy(afk = if (it.afk == 0) 1 else 0)
            repo.db.players().upsert(updatedPlayer.toEntity())
            reloadPlayers()
        }
    }
    
    /**
     * Sets a player's active status.
     *
     * @param playerId The ID of the player
     * @param active Whether the player should be active
     */
    suspend fun setPlayerActive(playerId: String, active: Boolean) {
        val player = players.find { it.id == playerId }
        player?.let {
            val updatedPlayer = it.copy(afk = if (active) 0 else 1)
            repo.db.players().upsert(updatedPlayer.toEntity())
            reloadPlayers()
        }
    }
    
    /**
     * Adds points to a player's session score.
     *
     * @param playerId The ID of the player
     * @param points The points to add
     */
    suspend fun addPointsToPlayer(playerId: String, points: Int) {
        repo.db.players().addPointsToPlayer(playerId, points)
        reloadPlayers()
    }
    
    /**
     * Increments the games played count for a player.
     *
     * @param playerId The ID of the player
     */
    suspend fun incrementGamesPlayed(playerId: String) {
        repo.db.players().incGamesPlayed(playerId)
        reloadPlayers()
    }
    
    /**
     * Adds wins to a player's total.
     *
     * @param playerId The ID of the player
     * @param wins The number of wins to add
     */
    suspend fun addWinsToPlayer(playerId: String, wins: Int) {
        repo.db.players().addWins(playerId, wins)
        reloadPlayers()
    }
    
    /**
     * Adds total points to a player's career total.
     *
     * @param playerId The ID of the player
     * @param points The points to add
     */
    suspend fun addTotalPointsToPlayer(playerId: String, points: Int) {
        repo.db.players().addTotalPoints(playerId, points)
        reloadPlayers()
    }
    
    /**
     * Gets a player by ID.
     *
     * @param playerId The ID of the player to retrieve
     * @return The player or null if not found
     */
    fun getPlayerById(playerId: String): Player? {
        return players.find { it.id == playerId }
    }
    
    /**
     * Gets active players count.
     *
     * @return Number of active players
     */
    fun getActivePlayersCount(): Int = activePlayers.size
    
    /**
     * Gets total players count.
     *
     * @return Number of total players
     */
    fun getTotalPlayersCount(): Int = players.size
    
    /**
     * Checks if rollcall should be shown.
     *
     * @return True if rollcall should be shown
     */
    fun shouldShowRollcall(): Boolean {
        return askRollcallOnLaunch && !didRollcall && players.isNotEmpty()
    }
    
    /**
     * Marks the rollcall as completed.
     */
    fun markRollcallDone() {
        didRollcall = true
    }
    
    /**
     * Resets rollcall state.
     */
    fun resetRollcall() {
        didRollcall = false
    }
    
    /**
     * Gets players sorted by session points (descending).
     *
     * @return Players sorted by score
     */
    fun getPlayersByScore(): List<Player> {
        return players.sortedByDescending { it.sessionPoints }
    }
    
    /**
     * Gets players sorted by name (alphabetical).
     *
     * @return Players sorted by name
     */
    fun getPlayersByName(): List<Player> {
        return players.sortedBy { it.name }
    }
    
    /**
     * Gets active players sorted by name.
     *
     * @return Active players sorted by name
     */
    fun getActivePlayersByName(): List<Player> {
        return activePlayers.sortedBy { it.name }
    }
    
    /**
     * Clears all players from the database.
     */
    suspend fun clearAllPlayers() {
        repo.db.players().deleteAll()
        reloadPlayers()
    }
    
    /**
     * Resets all players' session points to zero.
     */
    suspend fun resetSessionPoints() {
        players.forEach { player ->
            val updatedPlayer = player.copy(sessionPoints = 0)
            repo.db.players().upsert(updatedPlayer.toEntity())
        }
        reloadPlayers()
    }
    
    /**
     * Gets the player with the highest session points.
     *
     * @return Player with highest score or null if no players
     */
    fun getTopScoringPlayer(): Player? {
        return players.maxByOrNull { it.sessionPoints }
    }
    
    /**
     * Gets players with the lowest session points.
     *
     * @return List of players in last place
     */
    fun getLastPlacePlayers(): List<Player> {
        if (players.isEmpty()) return emptyList()
        
        val minPts = players.minOf { it.sessionPoints }
        return players.filter { it.sessionPoints == minPts }
    }
    
    /**
     * Checks if there are enough active players for a game.
     *
     * @param minimumPlayers Minimum players required (default: 2)
     * @return True if enough active players
     */
    fun hasEnoughActivePlayers(minimumPlayers: Int = 2): Boolean {
        return activePlayers.size >= minimumPlayers
    }
    
    /**
     * Gets player statistics summary.
     *
     * @return Map with player statistics
     */
    fun getPlayerStats(): Map<String, Any> {
        return mapOf(
            "totalPlayers" to players.size,
            "activePlayers" to activePlayers.size,
            "averageScore" to if (players.isNotEmpty()) players.map { it.sessionPoints }.average() else 0.0,
            "highestScore" to if (players.isNotEmpty()) players.maxOf { it.sessionPoints } else 0,
            "totalGamesPlayed" to players.sumOf { it.gamesPlayed },
            "totalWins" to players.sumOf { it.wins }
        )
    }
}