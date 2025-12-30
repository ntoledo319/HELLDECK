package com.helldeck.engine

import com.helldeck.content.model.Player
import com.helldeck.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Scalable player management system for 2-25 players
 * Handles team formation, voting efficiency, and performance optimization
 */
object PlayerManager {

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _teams = MutableStateFlow<Map<String, Team>>(emptyMap())
    val teams: StateFlow<Map<String, Team>> = _teams.asStateFlow()

    private val playerVotes = ConcurrentHashMap<String, MutableMap<String, Any>>()
    private val turnOrder = mutableListOf<String>()
    private var currentTurnIndex = 0

    /**
     * Initialize player manager
     */
    fun initialize() {
        Logger.i("PlayerManager initialized")
    }

    /**
     * Add a player to the game
     */
    fun addPlayer(player: Player) {
        val currentPlayers = _players.value.toMutableList()

        if (currentPlayers.none { it.id == player.id }) {
            currentPlayers.add(player)
            _players.value = currentPlayers

            // Reorganize teams if needed
            reorganizeTeams()

            Logger.d("Added player: ${player.name}, Total players: ${currentPlayers.size}")
        }
    }

    /**
     * Remove a player from the game
     */
    fun removePlayer(playerId: String) {
        val currentPlayers = _players.value.toMutableList()
        val removed = currentPlayers.removeIf { it.id == playerId }

        if (removed) {
            _players.value = currentPlayers

            // Clean up player data
            playerVotes.remove(playerId)
            turnOrder.remove(playerId)

            // Reorganize teams
            reorganizeTeams()

            Logger.d("Removed player: $playerId, Total players: ${currentPlayers.size}")
        }
    }

    /**
     * Update player information
     */
    fun updatePlayer(player: Player) {
        val currentPlayers = _players.value.toMutableList()
        val index = currentPlayers.indexOfFirst { it.id == player.id }

        if (index >= 0) {
            currentPlayers[index] = player
            _players.value = currentPlayers

            // Update team membership if needed
            updateTeamMembership(player)
        }
    }

    /**
     * Get player by ID
     */
    fun getPlayer(playerId: String): Player? {
        return _players.value.find { it.id == playerId }
    }

    /**
     * Get player by name
     */
    fun getPlayerByName(name: String): Player? {
        return _players.value.find { it.name == name }
    }

    /**
     * Get current player count
     */
    fun getPlayerCount(): Int = _players.value.size

    /**
     * Check if player count is in optimal range
     */
    fun isOptimalPlayerCount(): Boolean {
        val count = getPlayerCount()
        return count in 3..10
    }

    /**
     * Get recommended max players for current device
     */
    fun getRecommendedMaxPlayers(): Int {
        return when (com.helldeck.utils.MemoryOptimizer.getMemoryStrategy()) {
            com.helldeck.utils.MemoryStrategy.AGGRESSIVE -> 8
            com.helldeck.utils.MemoryStrategy.MODERATE -> 12
            com.helldeck.utils.MemoryStrategy.CONSERVATIVE -> 25
        }
    }

    /**
     * Reorganize teams based on player count
     */
    private fun reorganizeTeams() {
        val currentPlayers = _players.value
        val playerCount = currentPlayers.size

        when {
            playerCount <= 7 -> {
                // No teams for small groups
                _teams.value = emptyMap()
            }
            playerCount <= 16 -> {
                // Two teams for medium groups
                createTwoTeams(currentPlayers)
            }
            else -> {
                // Multiple teams for large groups
                createMultipleTeams(currentPlayers)
            }
        }
    }

    /**
     * Create two balanced teams
     */
    private fun createTwoTeams(players: List<Player>) {
        val sortedPlayers = players.sortedByDescending { it.sessionPoints }
        val midPoint = players.size / 2

        val teamA = sortedPlayers.take(midPoint)
        val teamB = sortedPlayers.drop(midPoint)

        val teams = mapOf(
            "Team A" to Team("Team A", teamA.map { it.id }, teamA.sumOf { it.sessionPoints }),
            "Team B" to Team("Team B", teamB.map { it.id }, teamB.sumOf { it.sessionPoints }),
        )

        _teams.value = teams
        Logger.d("Created two teams: Team A (${teamA.size} players), Team B (${teamB.size} players)")
    }

    /**
     * Create multiple teams for large groups
     */
    private fun createMultipleTeams(players: List<Player>) {
        val sortedPlayers = players.sortedByDescending { it.sessionPoints }
        val teamSize = 4 // Optimal team size
        val numberOfTeams = (players.size + teamSize - 1) / teamSize // Ceiling division

        val teams = mutableMapOf<String, Team>()

        for (i in 0 until numberOfTeams) {
            val startIndex = i * teamSize
            val endIndex = minOf(startIndex + teamSize, players.size)
            val teamPlayers = sortedPlayers.subList(startIndex, endIndex)

            val teamName = "Team ${(65 + i).toChar()}" // A, B, C, etc.
            teams[teamName] = Team(
                name = teamName,
                playerIds = teamPlayers.map { it.id },
                totalPoints = teamPlayers.sumOf { it.sessionPoints },
            )
        }

        _teams.value = teams
        Logger.d("Created ${teams.size} teams for ${players.size} players")
    }

    /**
     * Update team membership for a player
     */
    private fun updateTeamMembership(player: Player) {
        val currentTeams = _teams.value.toMutableMap()

        // Find which team the player belongs to and update points
        currentTeams.forEach { (teamName, team) ->
            if (player.id in team.playerIds) {
                val updatedPlayerIds = team.playerIds.toMutableList()
                val playerIndex = updatedPlayerIds.indexOf(player.id)
                if (playerIndex >= 0) {
                    // Recalculate team points
                    val teamPlayers = _players.value.filter { it.id in team.playerIds }
                    currentTeams[teamName] = team.copy(totalPoints = teamPlayers.sumOf { it.sessionPoints })
                }
            }
        }

        _teams.value = currentTeams
    }

    /**
     * Get team for a player
     */
    fun getTeamForPlayer(playerId: String): Team? {
        return _teams.value.values.find { playerId in it.playerIds }
    }

    /**
     * Get all teams sorted by performance
     */
    fun getTeamsSortedByPerformance(): List<Team> {
        return _teams.value.values.sortedByDescending { it.totalPoints }
    }

    /**
     * Record a vote for a player
     */
    fun recordVote(playerId: String, voteType: String, voteData: Any) {
        val playerVotes = playerVotes.getOrPut(playerId) { ConcurrentHashMap() }
        playerVotes[voteType] = voteData

        Logger.d("Recorded vote for player $playerId: $voteType = $voteData")
    }

    /**
     * Get votes for a specific vote type
     */
    fun getVotes(voteType: String): Map<String, Any> {
        return playerVotes.mapNotNull { (playerId, votes) ->
            votes[voteType]?.let { playerId to it }
        }.toMap()
    }

    /**
     * Clear all votes
     */
    fun clearAllVotes() {
        playerVotes.clear()
        Logger.d("Cleared all votes")
    }

    /**
     * Initialize turn order
     */
    fun initializeTurnOrder() {
        turnOrder.clear()
        turnOrder.addAll(_players.value.shuffled().map { it.id })
        currentTurnIndex = 0

        Logger.d("Initialized turn order for ${turnOrder.size} players")
    }

    /**
     * Get next player in turn order
     */
    fun getNextPlayer(): Player? {
        if (turnOrder.isEmpty()) {
            initializeTurnOrder()
        }

        if (currentTurnIndex >= turnOrder.size) {
            currentTurnIndex = 0
        }

        val nextPlayerId = turnOrder[currentTurnIndex]
        currentTurnIndex++

        return getPlayer(nextPlayerId)
    }

    /**
     * Get current player
     */
    fun getCurrentPlayer(): Player? {
        if (turnOrder.isEmpty() || currentTurnIndex == 0) {
            return getNextPlayer()
        }

        val currentPlayerId = turnOrder[(currentTurnIndex - 1) % turnOrder.size]
        return getPlayer(currentPlayerId)
    }

    /**
     * Advance to next turn
     */
    fun advanceTurn() {
        if (turnOrder.isEmpty()) {
            initializeTurnOrder()
            return
        }

        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size
        Logger.d("Advanced to next turn: ${getCurrentPlayer()?.name}")
    }

    /**
     * Get players in current turn order
     */
    fun getPlayersInTurnOrder(): List<Player> {
        if (turnOrder.isEmpty()) {
            initializeTurnOrder()
        }

        return turnOrder.mapNotNull { getPlayer(it) }
    }

    /**
     * Shuffle turn order
     */
    fun shuffleTurnOrder() {
        if (turnOrder.isNotEmpty()) {
            turnOrder.shuffle()
            currentTurnIndex = 0
            Logger.d("Shuffled turn order")
        }
    }

    /**
     * Get last place players (for comeback mechanic)
     */
    fun getLastPlacePlayers(): List<Player> {
        if (_players.value.isEmpty()) return emptyList()

        val minPoints = _players.value.minOf { it.sessionPoints }
        return _players.value.filter { it.sessionPoints == minPoints }
    }

    /**
     * Get leaderboard
     */
    fun getLeaderboard(): List<Player> {
        return _players.value.sortedByDescending { it.sessionPoints }
    }

    /**
     * Get team leaderboard
     */
    fun getTeamLeaderboard(): List<Team> {
        return getTeamsSortedByPerformance()
    }

    /**
     * Add points to player
     */
    fun addPointsToPlayer(playerId: String, points: Int) {
        val currentPlayers = _players.value.toMutableList()
        val playerIndex = currentPlayers.indexOfFirst { it.id == playerId }

        if (playerIndex >= 0) {
            currentPlayers[playerIndex] = currentPlayers[playerIndex].copy(
                sessionPoints = currentPlayers[playerIndex].sessionPoints + points,
            )
            _players.value = currentPlayers

            // Update team points
            updateTeamMembership(currentPlayers[playerIndex])

            Logger.d("Added $points points to player $playerId")
        }
    }

    /**
     * Reset all player scores for new session
     */
    fun resetAllScores() {
        val currentPlayers = _players.value.toMutableList()
        currentPlayers.forEach { player ->
            currentPlayers[currentPlayers.indexOf(player)] = player.copy(sessionPoints = 0)
        }
        _players.value = currentPlayers

        // Reset team points
        reorganizeTeams()

        Logger.i("Reset all player scores")
    }

    /**
     * Get player statistics
     */
    fun getPlayerStatistics(): Map<String, Any> {
        val players = _players.value
        if (players.isEmpty()) return emptyMap()

        val totalPoints = players.sumOf { it.sessionPoints }
        val averagePoints = totalPoints.toDouble() / players.size
        val maxPoints = players.maxOf { it.sessionPoints }
        val minPoints = players.minOf { it.sessionPoints }

        return mapOf(
            "totalPlayers" to players.size,
            "totalPoints" to totalPoints,
            "averagePoints" to averagePoints,
            "maxPoints" to maxPoints,
            "minPoints" to minPoints,
            "teamsCount" to _teams.value.size,
            "optimalRange" to isOptimalPlayerCount(),
            "recommendedMax" to getRecommendedMaxPlayers(),
        )
    }

    /**
     * Check if player count requires team mode
     */
    fun requiresTeamMode(): Boolean {
        return getPlayerCount() > 7
    }

    /**
     * Get team mode recommendations
     */
    fun getTeamModeRecommendations(): String {
        val playerCount = getPlayerCount()

        return when {
            playerCount <= 3 -> "Individual play recommended"
            playerCount <= 7 -> "Individual play works well"
            playerCount <= 16 -> "Team mode recommended (2 teams)"
            else -> "Team mode required (${_teams.value.size} teams of ~4 players each)"
        }
    }

    /**
     * Export player data for brainpack
     */
    fun exportPlayerData(): Map<String, Any> {
        return mapOf(
            "players" to _players.value.map { player ->
                mapOf(
                    "id" to player.id,
                    "name" to player.name,
                    "avatar" to player.avatar,
                    "sessionPoints" to player.sessionPoints,
                    "totalPoints" to player.totalPoints,
                    "elo" to player.elo,
                    "gamesPlayed" to player.gamesPlayed,
                    "wins" to player.wins,
                )
            },
            "teams" to _teams.value.map { (teamName, team) ->
                mapOf(
                    "name" to team.name,
                    "playerIds" to team.playerIds,
                    "totalPoints" to team.totalPoints,
                )
            },
            "turnOrder" to turnOrder,
            "currentTurnIndex" to currentTurnIndex,
            "exportTime" to System.currentTimeMillis(),
        )
    }

    /**
     * Import player data from brainpack
     */
    fun importPlayerData(data: Map<String, Any>) {
        try {
            @Suppress("UNCHECKED_CAST")
            val playersData = data["players"] as? List<Map<String, Any>> ?: emptyList()

            val importedPlayers = playersData.map { playerData ->
                Player(
                    id = playerData["id"] as String,
                    name = playerData["name"] as String,
                    avatar = playerData["avatar"] as String,
                    sessionPoints = (playerData["sessionPoints"] as? Number)?.toInt() ?: 0,
                    totalPoints = (playerData["totalPoints"] as? Number)?.toInt() ?: 0,
                    elo = (playerData["elo"] as? Number)?.toInt() ?: 1000,
                    gamesPlayed = (playerData["gamesPlayed"] as? Number)?.toInt() ?: 0,
                    wins = (playerData["wins"] as? Number)?.toInt() ?: 0,
                )
            }

            _players.value = importedPlayers

            // Import turn order if available
            @Suppress("UNCHECKED_CAST")
            val turnOrderData = data["turnOrder"] as? List<String>
            if (turnOrderData != null) {
                turnOrder.clear()
                turnOrder.addAll(turnOrderData)
            }

            // Import current turn index if available
            currentTurnIndex = (data["currentTurnIndex"] as? Number)?.toInt() ?: 0

            // Reorganize teams
            reorganizeTeams()

            Logger.i("Imported ${importedPlayers.size} players from brainpack")
        } catch (e: Exception) {
            Logger.e("Failed to import player data", e)
            throw e
        }
    }
}

/**
 * Team data class
 */
data class Team(
    val name: String,
    val playerIds: List<String>,
    val totalPoints: Int,
)

/**
 * Player vote data class
 */
data class PlayerVote(
    val playerId: String,
    val voteType: String,
    val voteData: Any,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Voting efficiency helper for large groups
 */
object VotingEfficiency {

    /**
     * Get optimal voting strategy for player count
     */
    fun getOptimalVotingStrategy(playerCount: Int): VotingStrategy {
        return when {
            playerCount <= 4 -> VotingStrategy.INDIVIDUAL_SIMULTANEOUS
            playerCount <= 8 -> VotingStrategy.INDIVIDUAL_SEQUENTIAL
            playerCount <= 16 -> VotingStrategy.TEAM_BASED
            else -> VotingStrategy.BATCHED_TEAM
        }
    }

    /**
     * Calculate estimated voting time
     */
    fun calculateEstimatedVotingTime(playerCount: Int, strategy: VotingStrategy): Long {
        val baseTimePerVote = 3000L // 3 seconds per vote

        return when (strategy) {
            VotingStrategy.INDIVIDUAL_SIMULTANEOUS -> baseTimePerVote
            VotingStrategy.INDIVIDUAL_SEQUENTIAL -> playerCount * baseTimePerVote
            VotingStrategy.TEAM_BASED -> {
                val teamCount = (playerCount + 3) / 4 // ~4 players per team
                teamCount * baseTimePerVote
            }
            VotingStrategy.BATCHED_TEAM -> {
                val teamCount = (playerCount + 3) / 4
                (teamCount * baseTimePerVote) / 2 // Batched voting is faster
            }
        }
    }

    /**
     * Get voting UI layout for player count
     */
    fun getVotingLayout(playerCount: Int): VotingLayout {
        return when {
            playerCount <= 6 -> VotingLayout.SINGLE_ROW
            playerCount <= 12 -> VotingLayout.TWO_ROWS
            playerCount <= 18 -> VotingLayout.THREE_ROWS
            else -> VotingLayout.GRID
        }
    }
}

/**
 * Voting strategies for different group sizes
 */
enum class VotingStrategy {
    INDIVIDUAL_SIMULTANEOUS, // All vote at once (small groups)
    INDIVIDUAL_SEQUENTIAL, // Vote one by one (medium groups)
    TEAM_BASED, // Teams vote as units (large groups)
    BATCHED_TEAM, // Multiple teams vote in batches (very large groups)
}

/**
 * Voting layout options
 */
enum class VotingLayout {
    SINGLE_ROW, // All players in one row
    TWO_ROWS, // Players in two rows
    THREE_ROWS, // Players in three rows
    GRID, // Grid layout for very large groups
}

/**
 * Player management extensions
 */
fun List<Player>.toTeamMap(): Map<String, Team> {
    return PlayerManager.getTeamLeaderboard().associateBy { it.name }
}

fun Player.getTeam(): Team? {
    return PlayerManager.getTeamForPlayer(this.id)
}

fun Player.addPoints(points: Int) {
    PlayerManager.addPointsToPlayer(this.id, points)
}
