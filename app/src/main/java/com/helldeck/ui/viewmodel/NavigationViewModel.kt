package com.helldeck.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helldeck.ui.Scene
import kotlinx.coroutines.launch

/**
 * ViewModel focused on navigation state management.
 * Handles scene transitions, navigation stack, and back navigation.
 */
class NavigationViewModel : ViewModel() {
    
    // Navigation state
    var scene by mutableStateOf(Scene.HOME)
    var showScores by mutableStateOf(false)
    
    // Simple navigation stack for back/home
    private val navStack = mutableListOf<Scene>()
    
    // Selection state
    var selectedPlayerId by mutableStateOf<String?>(null)
    var selectedGameId by mutableStateOf<String?>(null)
    
    /**
     * Navigates to the specified scene, adding the current scene to the navigation stack.
     *
     * @param target The target scene to navigate to.
     */
    fun navigateTo(target: Scene) {
        if (target != scene) {
            navStack.add(scene)
            scene = target
        }
    }
    
    /**
     * Checks if navigation back is possible.
     *
     * @return True if there are scenes in the navigation stack.
     */
    fun canGoBack(): Boolean = navStack.isNotEmpty()
    
    /**
     * Navigates back to the previous scene or home if no previous scene.
     */
    fun goBack() {
        if (navStack.isNotEmpty()) {
            scene = navStack.removeAt(navStack.lastIndex)
        } else {
            scene = Scene.HOME
        }
    }
    
    /**
     * Navigates directly to the home scene and clears the navigation stack.
     */
    fun goHome() {
        navStack.clear()
        scene = Scene.HOME
    }
    
    /**
     * Navigates to the profile scene for the specified player.
     *
     * @param playerId The ID of the player to view.
     */
    fun openProfile(playerId: String) {
        selectedPlayerId = playerId
        navigateTo(Scene.PROFILE)
    }
    
    /**
     * Navigates to the game rules scene for the current game.
     *
     * @param gameId The ID of the game to show rules for.
     */
    fun openGameRules(gameId: String) {
        selectedGameId = gameId
        navigateTo(Scene.GAME_RULES)
    }
    
    /**
     * Navigates to the players management scene.
     */
    fun goPlayers() {
        scene = Scene.PLAYERS
    }
    
    /**
     * Navigates to the settings scene.
     */
    fun goSettings() {
        scene = Scene.SETTINGS
    }
    
    /**
     * Navigates to the stats scene.
     */
    fun goStats() {
        scene = Scene.STATS
    }
    
    /**
     * Navigates to the rules scene.
     */
    fun goRules() {
        scene = Scene.RULES
    }
    
    /**
     * Toggles the visibility of the scoreboard overlay.
     */
    fun toggleScores() {
        showScores = !showScores
    }
    
    /**
     * Shows the scoreboard overlay.
     */
    fun showScoreboard() {
        showScores = true
    }
    
    /**
     * Hides the scoreboard overlay.
     */
    fun hideScoreboard() {
        showScores = false
    }
    
    /**
     * Gets the current scene.
     *
     * @return Current scene
     */
    fun getCurrentScene(): Scene = scene
    
    // Access selected IDs via properties (avoid duplicate JVM getters)
    
    /**
     * Clears the selected player ID.
     */
    fun clearSelectedPlayer() {
        selectedPlayerId = null
    }
    
    /**
     * Clears the selected game ID.
     */
    fun clearSelectedGame() {
        selectedGameId = null
    }
    
    /**
     * Gets the navigation stack size.
     *
     * @return Size of navigation stack
     */
    fun getNavigationStackSize(): Int = navStack.size
    
    /**
     * Clears the navigation stack.
     */
    fun clearNavigationStack() {
        navStack.clear()
    }
    
    /**
     * Restores navigation state.
     *
     * @param currentScene Current scene to restore
     * @param navigationStack Navigation stack to restore
     */
    fun restoreNavigationState(currentScene: Scene, navigationStack: List<Scene>) {
        scene = currentScene
        navStack.clear()
        navStack.addAll(navigationStack)
    }
}
