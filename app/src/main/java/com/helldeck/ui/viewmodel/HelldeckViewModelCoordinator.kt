package com.helldeck.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.Player
import com.helldeck.engine.Config
import com.helldeck.AppCtx
import com.helldeck.utils.Logger
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Coordinator ViewModel that manages the three focused ViewModels.
 * Provides a unified interface for the UI while maintaining separation of concerns.
 */
class HelldeckViewModelCoordinator : ViewModel() {
    
    // Loading state
    var isLoading by mutableStateOf(true)
    
    // Focused ViewModels
    val gameViewModel = GameViewModel()
    val navigationViewModel = NavigationViewModel()
    val playerViewModel = PlayerViewModel()
    
    // Core systems
    private lateinit var repo: ContentRepository
    
    /**
     * Initializes all ViewModels and coordinates their setup.
     */
    suspend fun initOnce() {
        if (::repo.isInitialized) return
        isLoading = true
        
        try {
            val context = AppCtx.ctx
            repo = ContentRepository(context)
            
            // Initialize all ViewModels
            gameViewModel.initialize(repo)
            playerViewModel.initialize(repo)
            playerViewModel.initOnce()
            
            Logger.i("HelldeckViewModelCoordinator: All ViewModels initialized")
        } catch (e: Exception) {
            Logger.e("HelldeckViewModelCoordinator: Failed to initialize", e)
        } finally {
            isLoading = false
        }
    }
    
    /**
     * Starts a new game round with proper coordination between ViewModels.
     */
    suspend fun startRound(gameId: String? = null) {
        if (!playerViewModel.hasEnoughActivePlayers()) {
            navigationViewModel.goPlayers()
            return
        }
        
        gameViewModel.startRound(
            gameId = gameId,
            activePlayers = playerViewModel.activePlayers,
            currentTurnIndex = gameViewModel.getCurrentTurnIndex()
        )
    }
    
    /**
     * Resolves the current game interaction with proper coordination.
     */
    fun resolveInteraction() {
        val activePlayer = gameViewModel.activePlayer(playerViewModel.activePlayers)
        gameViewModel.resolveInteraction(playerViewModel.players, activePlayer)
        
        // Navigate to feedback scene
        navigationViewModel.navigateTo(com.helldeck.ui.Scene.FEEDBACK)
    }
    
    /**
     * Commits feedback and advances to the next round.
     */
    suspend fun commitFeedbackAndNext() {
        val activePlayer = gameViewModel.activePlayer(playerViewModel.activePlayers)
        
        gameViewModel.commitFeedbackAndNext(
            players = playerViewModel.players,
            activePlayer = activePlayer
        ) {
            // Advance turn and start next round
            viewModelScope.launch {
                gameViewModel.endRoundAdvanceTurn(playerViewModel.activePlayers)
                startRound()
            }
        }
    }
    
    /**
     * Opens profile for a player with proper navigation coordination.
     */
    fun openProfile(playerId: String) {
        navigationViewModel.openProfile(playerId)
    }
    
    /**
     * Opens rules for the current game.
     */
    fun openRulesForCurrentGame() {
        gameViewModel.currentGame?.id?.let { gameId ->
            navigationViewModel.openGameRules(gameId)
        }
    }
    
    /**
     * Handles back navigation with proper coordination.
     */
    fun goBack() {
        navigationViewModel.goBack()
    }
    
    /**
     * Handles home navigation with proper coordination.
     */
    fun goHome() {
        navigationViewModel.goHome()
    }
    
    /**
     * Toggles scores with proper coordination.
     */
    fun toggleScores() {
        navigationViewModel.toggleScores()
    }
    
    /**
     * Gets the current scene from navigation ViewModel.
     */
    fun getCurrentScene(): com.helldeck.ui.Scene {
        return navigationViewModel.getCurrentScene()
    }
    
    /**
     * Gets the loading state.
     */
    fun getLoadingState(): Boolean = isLoading
    
    /**
     * Reloads players with proper coordination.
     */
    suspend fun reloadPlayers() {
        playerViewModel.reloadPlayers()
    }
    
    /**
     * Gets all players from player ViewModel.
     */
    fun getPlayers(): List<Player> {
        return playerViewModel.players
    }
    
    /**
     * Gets active players from player ViewModel.
     */
    fun getActivePlayers(): List<Player> {
        return playerViewModel.activePlayers
    }
    
    /**
     * Gets current game from game ViewModel.
     */
    fun getCurrentGame(): com.helldeck.engine.GameSpec? {
        return gameViewModel.currentGame
    }
    
    /**
     * Gets current card from game ViewModel.
     */
    fun getCurrentCard(): com.helldeck.content.model.FilledCard? {
        return gameViewModel.currentCard
    }
    
    /**
     * Gets current phase from game ViewModel.
     */
    fun getCurrentPhase(): com.helldeck.engine.RoundPhase {
        return gameViewModel.phase
    }
    
    /**
     * Gets spicy setting from game ViewModel.
     */
    fun getSpicySetting(): Boolean {
        return gameViewModel.spicy
    }
    
    /**
     * Sets spicy setting in game ViewModel.
     */
    fun setSpicySetting(spicy: Boolean) {
        gameViewModel.spicy = spicy
    }
    
    /**
     * Gets heat threshold from game ViewModel.
     */
    fun getHeatThreshold(): Float {
        return gameViewModel.heatThreshold
    }
    
    /**
     * Sets heat threshold in game ViewModel.
     */
    fun setHeatThreshold(threshold: Float) {
        gameViewModel.heatThreshold = threshold
    }
    
    /**
     * Gets show scores state from navigation ViewModel.
     */
    fun getShowScoresState(): Boolean {
        return navigationViewModel.showScores
    }
    
    /**
     * Gets selected player ID from navigation ViewModel.
     */
    fun getSelectedPlayerId(): String? = navigationViewModel.selectedPlayerId
    
    /**
     * Gets selected game ID from navigation ViewModel.
     */
    fun getSelectedGameId(): String? = navigationViewModel.selectedGameId
    
    /**
     * Handles pre-choice with delegation to game ViewModel.
     */
    fun onPreChoice(choice: String) {
        gameViewModel.onPreChoice(choice)
    }
    
    /**
     * Handles avatar vote with delegation to game ViewModel.
     */
    fun onAvatarVote(voterId: String, targetId: String) {
        gameViewModel.onAvatarVote(voterId, targetId)
    }
    
    /**
     * Handles A/B vote with delegation to game ViewModel.
     */
    fun onABVote(voterId: String, choice: String) {
        gameViewModel.onABVote(voterId, choice)
    }
    
    /**
     * Handles feedback LOL with delegation to game ViewModel.
     */
    fun feedbackLol() {
        gameViewModel.feedbackLol()
    }
    
    /**
     * Handles feedback MEH with delegation to game ViewModel.
     */
    fun feedbackMeh() {
        gameViewModel.feedbackMeh()
    }
    
    /**
     * Handles feedback TRASH with delegation to game ViewModel.
     */
    fun feedbackTrash() {
        gameViewModel.feedbackTrash()
    }
    
    /**
     * Adds comment with delegation to game ViewModel.
     */
    fun addComment(text: String, tags: Set<String>) {
        gameViewModel.addComment(text, tags)
    }
    
    /**
     * Gets feedback state from game ViewModel.
     */
    fun getFeedbackState(): Triple<Int, Int, Int> {
        return gameViewModel.getFeedbackState()
    }
    
    /**
     * Gets judge win state from game ViewModel.
     */
    fun getJudgeWinState(): Boolean {
        return gameViewModel.getJudgeWinState()
    }
    
    /**
     * Gets current points from game ViewModel.
     */
    fun getCurrentPoints(): Int {
        return gameViewModel.getCurrentPoints()
    }
    
    /**
     * Handles direct win with proper coordination.
     */
    fun commitDirectWin(pts: Int = Config.current.scoring.win) {
        val activePlayer = gameViewModel.activePlayer(playerViewModel.activePlayers)
        activePlayer?.let { player ->
            gameViewModel.commitDirectWin(player.id, playerViewModel.players, pts)
            navigationViewModel.navigateTo(com.helldeck.ui.Scene.FEEDBACK)
        }
    }
    
    /**
     * Handles feedback no points with proper coordination.
     */
    fun goToFeedbackNoPoints() {
        gameViewModel.goToFeedbackNoPoints()
        navigationViewModel.navigateTo(com.helldeck.ui.Scene.FEEDBACK)
    }
    
    /**
     * Gets game statistics with delegation to game ViewModel.
     */
    suspend fun getGameStats(): Map<String, Any?> {
        return gameViewModel.getGameStats()
    }
    
    /**
     * Marks rollcall as done with delegation to player ViewModel.
     */
    fun markRollcallDone() {
        playerViewModel.markRollcallDone()
    }
    
    /**
     * Checks if rollcall should be shown.
     */
    fun shouldShowRollcall(): Boolean {
        return playerViewModel.shouldShowRollcall()
    }
    
    /**
     * Gets player statistics with delegation to player ViewModel.
     */
    fun getPlayerStats(): Map<String, Any> {
        return playerViewModel.getPlayerStats()
    }
    
    /**
     * Adds a new player with delegation to player ViewModel.
     */
    suspend fun addPlayer(name: String, avatar: String): String {
        return playerViewModel.addPlayer(name, avatar)
    }
    
    /**
     * Updates a player with delegation to player ViewModel.
     */
    suspend fun updatePlayer(player: Player) {
        playerViewModel.updatePlayer(player)
    }
    
    /**
     * Removes a player with delegation to player ViewModel.
     */
    suspend fun removePlayer(playerId: String) {
        playerViewModel.removePlayer(playerId)
    }
    
    /**
     * Toggles player active status with delegation to player ViewModel.
     */
    suspend fun togglePlayerActive(playerId: String) {
        playerViewModel.togglePlayerActive(playerId)
    }
    
    /**
     * Gets player by ID with delegation to player ViewModel.
     */
    fun getPlayerById(playerId: String): Player? {
        return playerViewModel.getPlayerById(playerId)
    }
    
    /**
     * Gets active players count with delegation to player ViewModel.
     */
    fun getActivePlayersCount(): Int {
        return playerViewModel.getActivePlayersCount()
    }
    
    /**
     * Gets total players count with delegation to player ViewModel.
     */
    fun getTotalPlayersCount(): Int {
        return playerViewModel.getTotalPlayersCount()
    }
    
    /**
     * Gets players by score with delegation to player ViewModel.
     */
    fun getPlayersByScore(): List<Player> {
        return playerViewModel.getPlayersByScore()
    }
    
    /**
     * Gets last place players with delegation to player ViewModel.
     */
    fun getLastPlacePlayers(): List<Player> {
        return playerViewModel.getLastPlacePlayers()
    }
    
    /**
     * Gets top scoring player with delegation to player ViewModel.
     */
    fun getTopScoringPlayer(): Player? {
        return playerViewModel.getTopScoringPlayer()
    }
    
    /**
     * Checks if can go back with delegation to navigation ViewModel.
     */
    fun canGoBack(): Boolean {
        return navigationViewModel.canGoBack()
    }
    
    /**
     * Navigates to players scene with delegation to navigation ViewModel.
     */
    fun goPlayers() {
        navigationViewModel.goPlayers()
    }
    
    /**
     * Navigates to settings scene with delegation to navigation ViewModel.
     */
    fun goSettings() {
        navigationViewModel.goSettings()
    }
    
    /**
     * Navigates to stats scene with delegation to navigation ViewModel.
     */
    fun goStats() {
        navigationViewModel.goStats()
    }
    
    /**
     * Navigates to rules scene with delegation to navigation ViewModel.
     */
    fun goRules() {
        navigationViewModel.goRules()
    }
    
    /**
     * Shows scoreboard with delegation to navigation ViewModel.
     */
    fun showScoreboard() {
        navigationViewModel.showScoreboard()
    }
    
    /**
     * Hides scoreboard with delegation to navigation ViewModel.
     */
    fun hideScoreboard() {
        navigationViewModel.hideScoreboard()
    }
    
    /**
     * Resets session points with delegation to player ViewModel.
     */
    suspend fun resetSessionPoints() {
        playerViewModel.resetSessionPoints()
    }
    
    /**
     * Clears all players with delegation to player ViewModel.
     */
    suspend fun clearAllPlayers() {
        playerViewModel.clearAllPlayers()
    }
}
