package com.helldeck.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.model.FilledCard
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.engine.*
import com.helldeck.content.model.Player
import kotlinx.coroutines.launch

/**
 * ViewModel focused on game logic and round management.
 * Handles game state, round progression, scoring, and feedback.
 */
class GameViewModel : ViewModel() {
    
    // Game configuration
    var spicy by mutableStateOf(false)
    var heatThreshold by mutableStateOf(Config.roomHeatThreshold().toFloat())
    
    // Current round state
    var currentCard by mutableStateOf<FilledCard?>(null)
    var currentGame by mutableStateOf<GameSpec?>(null)
    var phase by mutableStateOf(RoundPhase.DRAW)
    
    // Voting state
    var preChoice by mutableStateOf<String?>(null)
    var votesAvatar by mutableStateOf<Map<String, String>>(emptyMap())
    var votesAB by mutableStateOf<Map<String, String>>(emptyMap())
    
    // Feedback state
    private var lol = 0
    private var meh = 0
    private var trash = 0
    private var tags: MutableSet<String> = mutableSetOf()
    private var judgeWin = false
    private var points = 0
    private var t0 = 0L
    
    // Turn management
    private var turnIdx = 0
    private var starterPicked = false
    
    // Core systems
    private lateinit var repo: ContentRepository
    private lateinit var engine: GameEngine
    
    /**
     * Initializes the GameViewModel with required dependencies.
     */
    fun initialize(repo: ContentRepository) {
        this.repo = repo
        this.engine = ContentEngineProvider.get(com.helldeck.AppCtx.ctx)
    }
    
    /**
     * Starts a new game round with the specified or random game.
     * Ensures at least 2 players are active before proceeding.
     *
     * @param gameId The ID of the game to start, or null for random selection.
     * @param activePlayers List of active players for the round
     * @param currentTurnIndex Current turn index
     */
    suspend fun startRound(
        gameId: String? = null, 
        activePlayers: List<Player>, 
        currentTurnIndex: Int
    ) {
        if (activePlayers.size < 2) {
            return
        }
        
        phase = RoundPhase.DRAW
        Config.spicyMode = spicy
        
        // Pick next game (random or selected)
        val nextGame = gameId ?: pickNextGame()
        currentGame = GameRegistry.getGameById(nextGame)
        
        // Pick starter if not already picked
        if (!starterPicked) {
            turnIdx = if (activePlayers.isNotEmpty()) kotlin.random.Random.nextInt(activePlayers.size) else 0
            starterPicked = true
        }
        
        // Generate card
        val playersList = activePlayers.map { it.name }
        val sessionId = "session_${System.currentTimeMillis()}"
        
        val gameResult = engine.next(
            GameEngine.Request(
                gameId = nextGame,
                sessionId = sessionId,
                spiceMax = if (spicy) 3 else 1,
                players = playersList
            )
        )
        currentCard = gameResult.filledCard
        t0 = System.currentTimeMillis()
        
        // Reset voting state
        preChoice = null
        votesAvatar = emptyMap()
        votesAB = emptyMap()
    }
    
    /**
     * Selects the next game based on configuration mechanics, such as comeback mode.
     *
     * @param players List of all players for determining comeback logic
     * @return The ID of the selected game.
     */
    private fun pickNextGame(players: List<Player> = emptyList()): String {
        val cfg = Config.current.mechanics
        
        return if (cfg.comeback_last_place_picks_next && players.size >= 3) {
            // Last place picks next game
            val lastPlaceIds = getLastPlaceIds(players)
            if (lastPlaceIds.isNotEmpty()) {
                // In a real implementation, you'd ask the last place player
                // For now, pick randomly from comeback-friendly games
                listOf(GameIds.ROAST_CONS, GameIds.POISON_PITCH, GameIds.MAJORITY).random()
            } else {
                GameRegistry.getAllGameIds().random()
            }
        } else {
            GameRegistry.getAllGameIds().random()
        }
    }
    
    /**
     * Retrieves the IDs of players with the lowest session points.
     *
     * @param players List of players to analyze
     * @return List of player IDs in last place.
     */
    private fun getLastPlaceIds(players: List<Player>): List<String> {
        if (players.isEmpty()) return emptyList()
        
        val minPts = players.minOf { it.sessionPoints }
        return players.filter { it.sessionPoints == minPts }.map { it.id }
    }
    
    /**
     * Advances to the next player's turn in the rotation.
     *
     * @param activePlayers List of active players
     */
    fun endRoundAdvanceTurn(activePlayers: List<Player>) {
        val poolSize = activePlayers.size.coerceAtLeast(1)
        turnIdx = (turnIdx + 1) % poolSize
    }
    
    /**
     * Retrieves the currently active player based on turn index.
     *
     * @param activePlayers List of active players
     * @return The active player or null if no players.
     */
    fun activePlayer(activePlayers: List<Player>): Player? {
        return if (activePlayers.isEmpty()) null else activePlayers[turnIdx % activePlayers.size]
    }
    
    /**
     * Handles pre-choice selection for games that require it.
     *
     * @param choice The selected pre-choice.
     */
    fun onPreChoice(choice: String) {
        preChoice = choice
    }
    
    /**
     * Records an avatar vote from a voter to a target.
     *
     * @param voterId The ID of the voter.
     * @param targetId The ID of the target player.
     */
    fun onAvatarVote(voterId: String, targetId: String) {
        votesAvatar = votesAvatar + (voterId to targetId)
    }
    
    /**
     * Records an A/B vote from a voter.
     *
     * @param voterId The ID of the voter.
     * @param choice The chosen option ("A" or "B").
     */
    fun onABVote(voterId: String, choice: String) {
        votesAB = votesAB + (voterId to choice)
    }
    
    /**
     * Resolves the current game interaction, awards points, and transitions to feedback phase.
     *
     * @param players List of all players for scoring
     * @param activePlayer Current active player
     */
    fun resolveInteraction(players: List<Player>, activePlayer: Player?) {
        val game = currentGame ?: return
        
        when (game.interaction) {
            Interaction.VOTE_AVATAR -> resolveRoastConsensus(players)
            Interaction.TRUE_FALSE -> resolveConfession(players)
            Interaction.AB_VOTE -> resolveAB()
            Interaction.SMASH_PASS -> resolveSmashPass()
            Interaction.JUDGE_PICK -> {
                judgeWin = true
                points = Config.current.scoring.win
                activePlayer?.let { awardPlayer(it.id, points, players) }
            }
            else -> {
                // Default resolution
                judgeWin = true
                points = Config.current.scoring.win
                activePlayer?.let { awardPlayer(it.id, points, players) }
            }
        }
        
        phase = RoundPhase.FEEDBACK
    }
    
    /**
     * Resolves roast consensus voting by determining the majority target and awarding points.
     *
     * @param players List of all players
     */
    private fun resolveRoastConsensus(players: List<Player>) {
        val targetId = votesAvatar.values.groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key
        
        targetId?.let { target ->
            val targetPlayer = players.find { it.id == target }
            if (targetPlayer != null) {
                awardPlayer(target, Config.current.scoring.win, players)
            }
        }
    }
    
    /**
     * Resolves confession or cap voting based on majority and pre-choice.
     *
     * @param players List of all players
     */
    private fun resolveConfession(players: List<Player>) {
        val tVotes = votesAB.values.count { it == "T" }
        val fVotes = votesAB.values.count { it == "F" }
        val majority = if (tVotes >= fVotes) "T" else "F"
        
        if (preChoice == "TRUTH" && majority == "T") {
            // Truth wins - correct guessers get points
            players.forEach { p ->
                if (votesAB[p.id] == "T") {
                    awardPlayer(p.id, 1, players)
                }
            }
        } else if (preChoice == "BLUFF" && majority == "F") {
            // Bluff wins - correct guessers get points
            players.forEach { p ->
                if (votesAB[p.id] == "F") {
                    awardPlayer(p.id, 1, players)
                }
            }
        }
    }
    
    /**
     * Resolves A/B voting by checking if the pre-choice matches the majority.
     */
    private fun resolveAB() {
        val aVotes = votesAB.values.count { it == "A" }
        val bVotes = votesAB.values.count { it == "B" }
        val majority = when {
            aVotes > bVotes -> "A"
            bVotes > aVotes -> "B"
            else -> "TIE"
        }
        
        if (preChoice != null && preChoice == majority) {
            judgeWin = true
            points = Config.current.scoring.win
        }
    }
    
    /**
     * Resolves smash or pass voting and awards points if majority smashes.
     */
    private fun resolveSmashPass() {
        val smash = votesAB.values.count { it.equals("SMASH", ignoreCase = true) || it == "A" }
        val pass = votesAB.values.count { it.equals("PASS", ignoreCase = true) || it == "B" }
        if (smash > pass) {
            judgeWin = true
            points = Config.current.scoring.win
        }
    }
    
    /**
     * Transitions to feedback phase without awarding points.
     */
    fun goToFeedbackNoPoints() {
        judgeWin = false
        points = 0
        phase = RoundPhase.FEEDBACK
    }
    
    /**
     * Commits a direct win for the active player and transitions to feedback.
     *
     * @param activePlayerId The ID of the active player
     * @param players List of all players
     * @param pts The points to award (default: standard win points).
     */
    fun commitDirectWin(activePlayerId: String, players: List<Player>, pts: Int = Config.current.scoring.win) {
        awardPlayer(activePlayerId, pts, players)
        judgeWin = true
        points = pts
        phase = RoundPhase.FEEDBACK
    }
    
    /**
     * Awards points to the specified player and persists to the database.
     *
     * @param playerId The ID of the player.
     * @param delta The points to add (can be negative).
     * @param players List of all players for updating local state
     */
    private fun awardPlayer(playerId: String, delta: Int, players: List<Player>) {
        // This would update the players list in a real implementation
        // For now, just persist to DB
        viewModelScope.launch {
            try {
                repo.db.players().addPointsToPlayer(playerId, delta)
            } catch (_: Exception) {}
        }
    }
    
    /**
     * Increments the LOL feedback count.
     */
    fun feedbackLol() {
        lol++
    }
    
    /**
     * Increments the MEH feedback count.
     */
    fun feedbackMeh() {
        meh++
    }
    
    /**
     * Increments the TRASH feedback count.
     */
    fun feedbackTrash() {
        trash++
    }
    
    /**
     * Adds a comment with associated tags for feedback.
     *
     * @param text The comment text.
     * @param t The set of tags.
     */
    fun addComment(text: String, t: Set<String>) {
        if (text.isNotBlank()) {
            tags.add("note")
        }
        tags.addAll(t)
    }
    
    /**
     * Commits feedback, records outcome, and advances to the next round.
     *
     * @param players List of all players
     * @param activePlayer Current active player
     * @param onRoundComplete Callback when round is complete
     */
    suspend fun commitFeedbackAndNext(
        players: List<Player>, 
        activePlayer: Player?, 
        onRoundComplete: () -> Unit
    ) {
        val card = currentCard ?: return
        val latency = (System.currentTimeMillis() - t0).toInt()
        
        val laughsScore = calculateLaughsScore()
        val responseTimeMs = System.currentTimeMillis() - t0
        val heatPercentage = (lol + trash).toDouble() / (lol + meh + trash).coerceAtLeast(1).toDouble()
        val winnerId = if (judgeWin) activePlayer?.id else null
        
        engine.recordOutcome(
            templateId = card.id,
            reward01 = laughsScore
        )
        
        // Reset feedback state
        lol = 0
        meh = 0
        trash = 0
        tags.clear()
        judgeWin = false
        points = 0
        
        // Persist long-term player stats
        activePlayer?.id?.let { pid ->
            viewModelScope.launch {
                try {
                    repo.db.players().incGamesPlayed(pid)
                    if (judgeWin) repo.db.players().addWins(pid, 1)
                    if (points != 0) repo.db.players().addTotalPoints(pid, points)
                } catch (_: Exception) {}
            }
        }
        
        // Signal round completion
        onRoundComplete()
    }
    
    /**
     * Calculates laugh score based on feedback.
     *
     * @return Calculated laugh score
     */
    private fun calculateLaughsScore(): Double {
        // Simplified laugh score calculation
        val total = lol + meh + trash
        if (total == 0) return 0.0
        return (lol.toDouble() - trash.toDouble()) / total.toDouble()
    }
    
    /**
     * Gets the current turn index.
     *
     * @return Current turn index
     */
    fun getCurrentTurnIndex(): Int = turnIdx
    
    /**
     * Sets the turn index (used when restoring state).
     *
     * @param index The turn index to set
     */
    fun setTurnIndex(index: Int) {
        turnIdx = index
    }
    
    /**
     * Gets the current feedback state.
     *
     * @return Triple of (lol, meh, trash) counts
     */
    fun getFeedbackState(): Triple<Int, Int, Int> = Triple(lol, meh, trash)
    
    /**
     * Gets the current judge win state.
     *
     * @return True if judge won, false otherwise
     */
    fun getJudgeWinState(): Boolean = judgeWin
    
    /**
     * Gets the current points awarded.
     *
     * @return Points awarded in current round
     */
    fun getCurrentPoints(): Int = points

    /**
     * Retrieves aggregated game statistics (stub for now).
     */
    suspend fun getGameStats(): Map<String, Any?> {
        return emptyMap()
    }
}
