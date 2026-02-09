package com.helldeck.engine

import com.helldeck.content.model.SessionParticipant
import com.helldeck.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Anonymous seat-based participant management system.
 * Replaces PlayerManager - no personal identifiers are stored or tracked.
 * Participants are identified solely by seat number (1-based).
 */
object SeatManager {

    private val _participants = MutableStateFlow<List<SessionParticipant>>(emptyList())
    val participants: StateFlow<List<SessionParticipant>> = _participants.asStateFlow()

    private val _teams = MutableStateFlow<Map<String, SeatTeam>>(emptyMap())
    val teams: StateFlow<Map<String, SeatTeam>> = _teams.asStateFlow()

    private val seatVotes = ConcurrentHashMap<Int, MutableMap<String, Any>>()
    private val turnOrder = mutableListOf<Int>()
    private var currentTurnIndex = 0

    /**
     * Initialize seat manager
     */
    fun initialize() {
        Logger.i("SeatManager initialized (anonymous mode)")
    }

    /**
     * Set up seats for a session with the given count
     */
    fun setupSeats(seatCount: Int) {
        require(seatCount in 2..25) { "Seat count must be between 2 and 25" }
        
        _participants.value = SessionParticipant.createSeats(seatCount)
        initializeTurnOrder()
        reorganizeTeams()
        
        Logger.d("Set up $seatCount seats for session")
    }

    /**
     * Add a seat to the session
     */
    fun addSeat(): SessionParticipant {
        val currentParticipants = _participants.value.toMutableList()
        val nextSeatNumber = (currentParticipants.maxOfOrNull { it.seatNumber } ?: 0) + 1
        
        val newParticipant = SessionParticipant(seatNumber = nextSeatNumber)
        currentParticipants.add(newParticipant)
        _participants.value = currentParticipants
        
        turnOrder.add(nextSeatNumber)
        reorganizeTeams()
        
        Logger.d("Added seat $nextSeatNumber, Total seats: ${currentParticipants.size}")
        return newParticipant
    }

    /**
     * Remove a seat from the session
     */
    fun removeSeat(seatNumber: Int) {
        val currentParticipants = _participants.value.toMutableList()
        val removed = currentParticipants.removeIf { it.seatNumber == seatNumber }

        if (removed) {
            _participants.value = currentParticipants
            seatVotes.remove(seatNumber)
            turnOrder.remove(seatNumber)
            reorganizeTeams()
            
            Logger.d("Removed seat $seatNumber, Total seats: ${currentParticipants.size}")
        }
    }

    /**
     * Update participant avatar
     */
    fun updateAvatar(seatNumber: Int, avatar: String) {
        val currentParticipants = _participants.value.toMutableList()
        val index = currentParticipants.indexOfFirst { it.seatNumber == seatNumber }

        if (index >= 0) {
            currentParticipants[index] = currentParticipants[index].copy(avatar = avatar)
            _participants.value = currentParticipants
        }
    }

    /**
     * Get participant by seat number
     */
    fun getParticipant(seatNumber: Int): SessionParticipant? {
        return _participants.value.find { it.seatNumber == seatNumber }
    }

    /**
     * Get current seat count
     */
    fun getSeatCount(): Int = _participants.value.size

    /**
     * Check if seat count is in optimal range
     */
    fun isOptimalSeatCount(): Boolean {
        val count = getSeatCount()
        return count in 3..10
    }

    /**
     * Get recommended max seats for current device
     */
    fun getRecommendedMaxSeats(): Int {
        return when (com.helldeck.utils.MemoryOptimizer.getMemoryStrategy()) {
            com.helldeck.utils.MemoryStrategy.AGGRESSIVE -> 8
            com.helldeck.utils.MemoryStrategy.MODERATE -> 12
            com.helldeck.utils.MemoryStrategy.CONSERVATIVE -> 25
        }
    }

    /**
     * Reorganize teams based on seat count
     */
    private fun reorganizeTeams() {
        val currentParticipants = _participants.value
        val seatCount = currentParticipants.size

        when {
            seatCount <= 7 -> {
                _teams.value = emptyMap()
            }
            seatCount <= 16 -> {
                createTwoTeams(currentParticipants)
            }
            else -> {
                createMultipleTeams(currentParticipants)
            }
        }
    }

    /**
     * Create two balanced teams
     */
    private fun createTwoTeams(participants: List<SessionParticipant>) {
        val sortedParticipants = participants.sortedByDescending { it.sessionPoints }
        val midPoint = participants.size / 2

        val teamA = sortedParticipants.take(midPoint)
        val teamB = sortedParticipants.drop(midPoint)

        val teams = mapOf(
            "Team A" to SeatTeam("Team A", teamA.map { it.seatNumber }, teamA.sumOf { it.sessionPoints }),
            "Team B" to SeatTeam("Team B", teamB.map { it.seatNumber }, teamB.sumOf { it.sessionPoints }),
        )

        _teams.value = teams
        Logger.d("Created two teams: Team A (${teamA.size} seats), Team B (${teamB.size} seats)")
    }

    /**
     * Create multiple teams for large groups
     */
    private fun createMultipleTeams(participants: List<SessionParticipant>) {
        val sortedParticipants = participants.sortedByDescending { it.sessionPoints }
        val teamSize = 4
        val numberOfTeams = (participants.size + teamSize - 1) / teamSize

        val teams = mutableMapOf<String, SeatTeam>()

        for (i in 0 until numberOfTeams) {
            val startIndex = i * teamSize
            val endIndex = minOf(startIndex + teamSize, participants.size)
            val teamParticipants = sortedParticipants.subList(startIndex, endIndex)

            val teamName = "Team ${(65 + i).toChar()}"
            teams[teamName] = SeatTeam(
                name = teamName,
                seatNumbers = teamParticipants.map { it.seatNumber },
                totalPoints = teamParticipants.sumOf { it.sessionPoints },
            )
        }

        _teams.value = teams
        Logger.d("Created ${teams.size} teams for ${participants.size} seats")
    }

    /**
     * Get team for a seat
     */
    fun getTeamForSeat(seatNumber: Int): SeatTeam? {
        return _teams.value.values.find { seatNumber in it.seatNumbers }
    }

    /**
     * Get all teams sorted by performance
     */
    fun getTeamsSortedByPerformance(): List<SeatTeam> {
        return _teams.value.values.sortedByDescending { it.totalPoints }
    }

    /**
     * Record a vote for a seat
     */
    fun recordVote(seatNumber: Int, voteType: String, voteData: Any) {
        val votes = seatVotes.getOrPut(seatNumber) { ConcurrentHashMap() }
        votes[voteType] = voteData
        Logger.d("Recorded vote for seat $seatNumber: $voteType = $voteData")
    }

    /**
     * Get votes for a specific vote type
     */
    fun getVotes(voteType: String): Map<Int, Any> {
        return seatVotes.mapNotNull { (seatNumber, votes) ->
            votes[voteType]?.let { seatNumber to it }
        }.toMap()
    }

    /**
     * Clear all votes
     */
    fun clearAllVotes() {
        seatVotes.clear()
        Logger.d("Cleared all votes")
    }

    /**
     * Initialize turn order
     */
    fun initializeTurnOrder() {
        turnOrder.clear()
        turnOrder.addAll(_participants.value.shuffled().map { it.seatNumber })
        currentTurnIndex = 0
        Logger.d("Initialized turn order for ${turnOrder.size} seats")
    }

    /**
     * Get next seat in turn order
     */
    fun getNextSeat(): SessionParticipant? {
        if (turnOrder.isEmpty()) {
            initializeTurnOrder()
        }

        if (currentTurnIndex >= turnOrder.size) {
            currentTurnIndex = 0
        }

        val nextSeatNumber = turnOrder[currentTurnIndex]
        currentTurnIndex++

        return getParticipant(nextSeatNumber)
    }

    /**
     * Get current seat
     */
    fun getCurrentSeat(): SessionParticipant? {
        if (turnOrder.isEmpty() || currentTurnIndex == 0) {
            return getNextSeat()
        }

        val currentSeatNumber = turnOrder[(currentTurnIndex - 1) % turnOrder.size]
        return getParticipant(currentSeatNumber)
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
        Logger.d("Advanced to next turn: Seat ${getCurrentSeat()?.seatNumber}")
    }

    /**
     * Get participants in current turn order
     */
    fun getParticipantsInTurnOrder(): List<SessionParticipant> {
        if (turnOrder.isEmpty()) {
            initializeTurnOrder()
        }
        return turnOrder.mapNotNull { getParticipant(it) }
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
     * Get last place participants (for comeback mechanic)
     */
    fun getLastPlaceParticipants(): List<SessionParticipant> {
        if (_participants.value.isEmpty()) return emptyList()

        val minPoints = _participants.value.minOf { it.sessionPoints }
        return _participants.value.filter { it.sessionPoints == minPoints }
    }

    /**
     * Get leaderboard by seat
     */
    fun getLeaderboard(): List<SessionParticipant> {
        return _participants.value.sortedByDescending { it.sessionPoints }
    }

    /**
     * Get team leaderboard
     */
    fun getTeamLeaderboard(): List<SeatTeam> {
        return getTeamsSortedByPerformance()
    }

    /**
     * Add points to seat
     */
    fun addPointsToSeat(seatNumber: Int, points: Int) {
        val currentParticipants = _participants.value.toMutableList()
        val participantIndex = currentParticipants.indexOfFirst { it.seatNumber == seatNumber }

        if (participantIndex >= 0) {
            currentParticipants[participantIndex] = currentParticipants[participantIndex].copy(
                sessionPoints = currentParticipants[participantIndex].sessionPoints + points,
            )
            _participants.value = currentParticipants
            
            // Update team points
            reorganizeTeams()
            
            Logger.d("Added $points points to seat $seatNumber")
        }
    }

    /**
     * Reset all scores for new session
     */
    fun resetAllScores() {
        val currentParticipants = _participants.value.map { it.copy(sessionPoints = 0) }
        _participants.value = currentParticipants
        reorganizeTeams()
        Logger.i("Reset all seat scores")
    }

    /**
     * Get session statistics (no personal data)
     */
    fun getSessionStatistics(): Map<String, Any> {
        val participants = _participants.value
        if (participants.isEmpty()) return emptyMap()

        val totalPoints = participants.sumOf { it.sessionPoints }
        val averagePoints = totalPoints.toDouble() / participants.size
        val maxPoints = participants.maxOf { it.sessionPoints }
        val minPoints = participants.minOf { it.sessionPoints }

        return mapOf(
            "totalSeats" to participants.size,
            "totalPoints" to totalPoints,
            "averagePoints" to averagePoints,
            "maxPoints" to maxPoints,
            "minPoints" to minPoints,
            "teamsCount" to _teams.value.size,
            "optimalRange" to isOptimalSeatCount(),
            "recommendedMax" to getRecommendedMaxSeats(),
        )
    }

    /**
     * Check if seat count requires team mode
     */
    fun requiresTeamMode(): Boolean {
        return getSeatCount() > 7
    }

    /**
     * Clear session (for privacy - removes all session data)
     */
    fun clearSession() {
        _participants.value = emptyList()
        _teams.value = emptyMap()
        seatVotes.clear()
        turnOrder.clear()
        currentTurnIndex = 0
        Logger.i("Session cleared - all participant data removed")
    }
}

/**
 * Team data class (seat-based)
 */
data class SeatTeam(
    val name: String,
    val seatNumbers: List<Int>,
    val totalPoints: Int,
)

/**
 * Seat vote data class
 */
data class SeatVote(
    val seatNumber: Int,
    val voteType: String,
    val voteData: Any,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Extension to add points
 */
fun SessionParticipant.addPoints(points: Int) {
    SeatManager.addPointsToSeat(this.seatNumber, points)
}

/**
 * Extension to get team
 */
fun SessionParticipant.getTeam(): SeatTeam? {
    return SeatManager.getTeamForSeat(this.seatNumber)
}
