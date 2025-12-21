package com.helldeck.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.util.SeededRng
import com.helldeck.content.model.Player
import com.helldeck.engine.*
import com.helldeck.engine.GameMetadata
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.random.Random
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.helldeck.data.PlayerEntity
import com.helldeck.data.computePlayerProfiles
import com.helldeck.data.toPlayer
import com.helldeck.data.toEntity
import com.helldeck.ui.scenes.HomeScene
import com.helldeck.ui.scenes.RollcallScene
import com.helldeck.ui.scenes.PlayersScene
import com.helldeck.ui.scenes.RoundScene
import com.helldeck.ui.scenes.FeedbackScene
import com.helldeck.ui.scenes.SettingsScene
import com.helldeck.ui.scenes.StatsScene
import com.helldeck.ui.components.GameRulesScene
import com.helldeck.ui.components.PlayerProfileScene
import com.helldeck.ui.components.ScoreboardOverlay
import com.helldeck.ui.components.RulesSheet
import com.helldeck.ui.components.HelldeckLoadingSpinner
import com.helldeck.ui.components.HelldeckBackgroundPattern
import com.helldeck.ui.components.HelldeckAnimations
import com.helldeck.ui.components.HelldeckSpacing
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.ui.LoadingWithErrorBoundary
import com.helldeck.ui.BackgroundPattern
import com.helldeck.ui.HelldeckError

/**
 * Scene enumeration for navigation
 */
enum class Scene {
    HOME, ROLLCALL, PLAYERS, ROUND, FEEDBACK, RULES, SCOREBOARD, STATS, SETTINGS, PROFILE, GAME_RULES, CARD_LAB
}

@Composable
internal fun hdFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

/**
 * Main HELLDECK app UI composable with error boundaries
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun HelldeckAppUI(
    vm: HelldeckVm = viewModel(),
    modifier: Modifier = Modifier
) {
    var error by remember { mutableStateOf<HelldeckError?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        Config.load()
        com.helldeck.utils.Logger.i("HelldeckAppUI: Initializing ViewModel")
        try {
            vm.initOnce()
            com.helldeck.utils.Logger.i("HelldeckAppUI: ViewModel initialized")
        } catch (e: Exception) {
            error = HelldeckError.UnknownError(
                message = "Failed to initialize app: ${e.message}",
                technicalDetails = e.stackTraceToString()
            )
        }
    }

    BackHandler(enabled = vm.scene != Scene.HOME) {
        try {
            vm.goBack()
        } catch (e: Exception) {
            error = HelldeckError.UnknownError(
                message = "Navigation failed: ${e.message}",
                technicalDetails = e.stackTraceToString()
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoadingWithErrorBoundary(
            isLoading = vm.isLoading,
            error = error,
            onRetry = {
                error = null
                coroutineScope.launch {
                    try {
                        vm.initOnce()
                    } catch (e: Exception) {
                        error = HelldeckError.UnknownError(
                            message = "Retry failed: ${e.message}",
                            technicalDetails = e.stackTraceToString()
                        )
                    }
                }
            },
            onDismiss = { 
                if (error?.recoverable == false) {
                    // For critical errors, close app
                    // In a real app, you might want to exit gracefully
                    error = null
                } else {
                    error = null 
                }
            },
            loadingContent = {
                // Background pattern for visual interest
                HelldeckBackgroundPattern(
                    pattern = BackgroundPattern.CIRCUIT,
                    opacity = 0.03f
                )

                // Scene transitions with smooth animations
                com.helldeck.utils.Logger.i("HelldeckAppUI: Current scene: ${vm.scene}")
                val reducedMotion = LocalReducedMotion.current
    AnimatedContent(
        targetState = vm.scene,
                    transitionSpec = {
                    // Hell’s Living Room: fast, directional, consistent.
                    if (reducedMotion) {
                        fadeIn(animationSpec = tween(HelldeckAnimations.Instant)) togetherWith
                            fadeOut(animationSpec = tween(HelldeckAnimations.Instant))
                    } else {
                        (fadeIn(animationSpec = tween(HelldeckAnimations.Fast)) +
                            slideInHorizontally(
                                animationSpec = tween(HelldeckAnimations.Normal, easing = EaseInOutSine),
                                initialOffsetX = { it / 8 }
                            )) togetherWith
                            (fadeOut(animationSpec = tween(HelldeckAnimations.Fast)) +
                                slideOutHorizontally(
                                    animationSpec = tween(HelldeckAnimations.Normal, easing = EaseInOutSine),
                                    targetOffsetX = { -it / 8 }
                                ))
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { targetScene ->
                when (targetScene) {
                    Scene.HOME -> HomeScene(vm)
                    Scene.ROLLCALL -> RollcallScene(vm)
                    Scene.PLAYERS -> PlayersScene(vm)
                    Scene.ROUND -> RoundScene(vm)
                    Scene.FEEDBACK -> FeedbackScene(vm)
                    Scene.RULES -> RulesSheet { vm.scene = Scene.HOME }
                    Scene.SCOREBOARD -> ScoreboardOverlay(vm.players) { vm.scene = Scene.HOME }
                    Scene.STATS -> StatsScene(onClose = { vm.scene = Scene.HOME }, vm = vm)
                    Scene.SETTINGS -> SettingsScene(onClose = { vm.scene = Scene.HOME }, vm = vm)
                    Scene.PROFILE -> PlayerProfileScene(vm = vm, onClose = { vm.scene = Scene.HOME })
                    Scene.GAME_RULES -> GameRulesScene(vm = vm, onClose = { vm.goBack() })
                    Scene.CARD_LAB -> com.helldeck.ui.scenes.CardLabScene(onClose = { vm.goBack() })
                }
            }
            }
        )
    }
}

/**
 * ViewModel for HELLDECK game state management.
 * Handles navigation, player data, game logic, and UI state.
 */
class HelldeckVm : ViewModel() {

    // Navigation state
    var scene by mutableStateOf(Scene.HOME)
    var showScores by mutableStateOf(false)
    var isLoading by mutableStateOf(true)

    // Game configuration
    var spicy by mutableStateOf(false)
    var heatThreshold by mutableStateOf(Config.roomHeatThreshold().toFloat())

    // Selection / profile
    var selectedPlayerId by mutableStateOf<String?>(null)
    var selectedGameId by mutableStateOf<String?>(null)

    // Simple navigation stack for back/home
    private val navStack = mutableListOf<Scene>()

    // Player data
    var players by mutableStateOf(listOf<Player>())
    var activePlayers by mutableStateOf(listOf<Player>())
    private var turnIdx by mutableStateOf(0)
    private var starterPicked = false

    // Persistent session ID for a game night (prevents card repetition)
    var gameNightSessionId by mutableStateOf("session_${System.currentTimeMillis()}")

    // Rollcall / attendance
    private var didRollcall = false
    private var askRollcallOnLaunch = true

    // Current round state (LEGACY - being replaced by roundState)
    var currentCard by mutableStateOf<com.helldeck.content.model.FilledCard?>(null)
    var currentGame by mutableStateOf<GameInfo?>(null)
    var phase by mutableStateOf(RoundPhase.DRAW)

    // NEW: Authoritative round state from engine
    var roundState by mutableStateOf<com.helldeck.ui.state.RoundState?>(null)

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

    // Core systems
    private lateinit var repo: ContentRepository
    private lateinit var engine: com.helldeck.content.engine.GameEngine

    // Initialization guard
    private var isInitialized = false

    /**
     * Initializes the ViewModel systems on first use.
     * Sets up the content repository, game engine, and loads initial data.
     * Can be called multiple times safely.
     */
    suspend fun initOnce() {
        if (isInitialized) return
        isLoading = true

        val context = AppCtx.ctx
        repo = ContentRepository(context)
        engine = ContentEngineProvider.get(context)
        isInitialized = true

        // Load initial data
        reloadPlayers()

        // Determine if we should ask for rollcall on launch
        // NOTE: Settings DAO not implemented yet, using default
        askRollcallOnLaunch = true
        if (askRollcallOnLaunch && !didRollcall && players.isNotEmpty()) {
            scene = Scene.ROLLCALL
        }
        isLoading = false
    }

    /**
     * Convenience wrapper to expose options for a filled card without leaking engine.
     * DEPRECATED: UI should use options from RoundState instead of recomputing.
     */
    @Deprecated("Use options from RoundState instead", ReplaceWith("roundState?.options"))
    fun getOptionsFor(
        card: com.helldeck.content.model.FilledCard,
        req: com.helldeck.content.engine.GameEngine.Request
    ): com.helldeck.content.model.GameOptions {
        return try {
            if (!isInitialized) {
                com.helldeck.utils.Logger.w("getOptionsFor called before initialization")
                return com.helldeck.content.model.GameOptions.None
            }
            engine.getOptionsFor(card, req)
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("getOptionsFor failed", e)
            // Return empty fallback to keep UI stable
            com.helldeck.content.model.GameOptions.None
        }
    }

    /**
     * Reloads players from the database and updates active players.
     * Adds default players if none exist.
     */
    suspend fun reloadPlayers() {
        if (!isInitialized) {
            com.helldeck.utils.Logger.w("reloadPlayers called before initialization")
            return
        }

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
     */
    fun openRulesForCurrentGame() {
        selectedGameId = currentGame?.id
        navigateTo(Scene.GAME_RULES)
    }

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
     * Starts a new game night session with fresh session ID.
     * This resets anti-repetition tracking and allows previously seen cards.
     */
    fun startNewGameNight() {
        gameNightSessionId = "session_${System.currentTimeMillis()}"
        turnIdx = 0
        starterPicked = false
        didRollcall = false
        com.helldeck.utils.Logger.i("New game night started: $gameNightSessionId")
    }

    /**
     * Marks the rollcall as completed.
     */
    fun markRollcallDone() {
        didRollcall = true
    }

    /**
     * Toggles the visibility of the scoreboard overlay.
     */
    fun toggleScores() {
        showScores = !showScores
    }

    /**
     * Navigates to the players management scene.
     */
    fun goPlayers() {
        scene = Scene.PLAYERS
    }

    /**
     * Starts a new game round with the specified or random game.
     * Ensures at least 2 players are active before proceeding.
     *
     * @param gameId The ID of the game to start, or null for random selection.
     */
    suspend fun startRound(gameId: String? = null) {
        // Ensure engine is ready before starting a round
        if (!::engine.isInitialized) {
            try {
                initOnce()
            } catch (e: Exception) {
                com.helldeck.utils.Logger.e("startRound: initialization failed", e)
                scene = Scene.HOME
                return
            }
        }

        if (activePlayers.size < 2) {
            // Ensure players exist before starting a round
            scene = Scene.PLAYERS
            return
        }

        scene = Scene.ROUND
        phase = RoundPhase.DRAW

        Config.spicyMode = spicy

        // Pick next game (random or selected)
        val nextGame = gameId ?: pickNextGame()
        currentGame = GameMetadata.getGameMetadata(nextGame)

        // Pick starter if not already picked
        if (!starterPicked) {
            turnIdx = if (activePlayers.isNotEmpty()) Random.nextInt(activePlayers.size) else 0
            starterPicked = true
        }

        // Generate card
        val playersList = activePlayers.map { it.name }
        val activePlayerId = activePlayer()?.id
        val targetPlayerId = if (currentGame?.interaction == Interaction.TARGET_PICK) {
            activePlayers.randomOrNull()?.id
        } else null

        // Use persistent session ID (will be fixed in STEP 4)
        val sessionId = gameNightSessionId

        try {
            val gameResult = engine.next(
                com.helldeck.content.engine.GameEngine.Request(
                    gameId = nextGame,
                    sessionId = sessionId,
                    spiceMax = if (spicy) 3 else 1,
                    players = playersList
                )
            )

            // Create authoritative RoundState from engine result
            roundState = com.helldeck.ui.state.RoundState(
                gameId = nextGame,
                filledCard = gameResult.filledCard,
                options = gameResult.options,
                timerSec = gameResult.timer,
                interactionType = gameResult.interactionType,
                activePlayerIndex = turnIdx,
                judgePlayerIndex = if (currentGame?.interaction == Interaction.JUDGE_PICK) {
                    (turnIdx + 1) % activePlayers.size
                } else null,
                targetPlayerIndex = if (currentGame?.interaction == Interaction.TARGET_PICK) {
                    activePlayers.indexOfFirst { it.id == targetPlayerId }
                } else null,
                phase = com.helldeck.ui.state.RoundPhase.INTRO,
                sessionId = sessionId
            )

            // Keep legacy fields for backward compatibility during transition
            currentCard = gameResult.filledCard

        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("startRound: engine.next failed", e)
            scene = Scene.HOME
            return
        }
        t0 = System.currentTimeMillis()

        // Reset voting state
        preChoice = null
        votesAvatar = emptyMap()
        votesAB = emptyMap()
    }

    /**
     * Selects the next game based on configuration mechanics, such as comeback mode.
     *
     * @return The ID of the selected game.
     */
    private fun pickNextGame(): String {
        val cfg = Config.current.mechanics

        return if (cfg.comeback_last_place_picks_next && players.size >= 3) {
            // Last place picks next game
            val lastPlaceIds = getLastPlaceIds()
            if (lastPlaceIds.isNotEmpty()) {
                // In a real implementation, you'd ask the last place player
                // For now, pick randomly from comeback-friendly games
                listOf(GameIds.ROAST_CONS, GameIds.POISON_PITCH, GameIds.MAJORITY).random()
            } else {
                GameMetadata.getAllGameIds().random()
            }
        } else {
            GameMetadata.getAllGameIds().random()
        }
    }

    /**
     * Retrieves the IDs of players with the lowest session points.
     *
     * @return List of player IDs in last place.
     */
    private fun getLastPlaceIds(): List<String> {
        if (players.isEmpty()) return emptyList()

        val minPts = players.minOf { it.sessionPoints }
        return players.filter { it.sessionPoints == minPts }.map { it.id }
    }

    /**
     * Advances to the next player's turn in the rotation.
     */
    fun endRoundAdvanceTurn() {
        val poolSize = activePlayers.size.coerceAtLeast(1)
        turnIdx = (turnIdx + 1) % poolSize
    }

    /**
     * Retrieves the currently active player based on turn index.
     *
     * @return The active player or null if no players.
     */
    fun activePlayer(): Player? {
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
     * NOW USES: InteractionType from roundState (not legacy Interaction enum)
     */
    fun resolveInteraction() {
        val state = roundState
        if (state != null) {
            // NEW PATH: Use InteractionType from authoritative roundState
            when (state.interactionType) {
                com.helldeck.engine.InteractionType.VOTE_PLAYER -> resolveRoastConsensus()
                com.helldeck.engine.InteractionType.TRUE_FALSE -> resolveConfession()
                com.helldeck.engine.InteractionType.A_B_CHOICE -> resolveAB()
                com.helldeck.engine.InteractionType.PREDICT_VOTE -> resolveAB() // Same as A/B
                com.helldeck.engine.InteractionType.SMASH_PASS -> resolveSmashPass()
                com.helldeck.engine.InteractionType.JUDGE_PICK -> {
                    judgeWin = true
                    points = Config.current.scoring.win
                    awardActive(points)
                }
                else -> {
                    // Default resolution for other types
                    judgeWin = true
                    points = Config.current.scoring.win
                    awardActive(points)
                }
            }
        } else {
            // LEGACY FALLBACK: Use old Interaction enum if roundState not available
            val game = currentGame ?: return
            when (game.interaction) {
                Interaction.VOTE_AVATAR -> resolveRoastConsensus()
                Interaction.TRUE_FALSE -> resolveConfession()
                Interaction.AB_VOTE -> resolveAB()
                Interaction.SMASH_PASS -> resolveSmashPass()
                Interaction.JUDGE_PICK -> {
                    judgeWin = true
                    points = Config.current.scoring.win
                    awardActive(points)
                }
                else -> {
                    judgeWin = true
                    points = Config.current.scoring.win
                    awardActive(points)
                }
            }
        }

        // Keep authoritative RoundState in sync with scene transitions.
        roundState = roundState?.withPhase(com.helldeck.ui.state.RoundPhase.FEEDBACK)
        phase = RoundPhase.FEEDBACK
        scene = Scene.FEEDBACK
    }

    /**
     * Handles round events from UI interactions.
     * Central event processor for type-safe interaction handling.
     */
    fun handleRoundEvent(event: com.helldeck.ui.events.RoundEvent) {
        when (event) {
            is com.helldeck.ui.events.RoundEvent.PickAB -> {
                onABVote(activePlayer()?.id ?: "", event.choice)
            }
            is com.helldeck.ui.events.RoundEvent.VotePlayer -> {
                val voterId = activePlayer()?.id ?: return
                val targetId = activePlayers.getOrNull(event.playerIndex)?.id ?: return
                onAvatarVote(voterId, targetId)
            }
            is com.helldeck.ui.events.RoundEvent.PreChoice -> {
                onPreChoice(event.choice)
            }
            is com.helldeck.ui.events.RoundEvent.SelectTarget -> {
                val targetId = activePlayers.getOrNull(event.playerIndex)?.id
                com.helldeck.utils.Logger.d("Target selected: $targetId")
            }
            is com.helldeck.ui.events.RoundEvent.RateCard -> {
                when (event.rating) {
                    com.helldeck.content.quality.Rating.LOL -> feedbackLol()
                    com.helldeck.content.quality.Rating.MEH -> feedbackMeh()
                    com.helldeck.content.quality.Rating.TRASH -> feedbackTrash()
                }
            }
            is com.helldeck.ui.events.RoundEvent.AdvancePhase -> {
                roundState?.let { state ->
                    when (state.phase) {
                        com.helldeck.ui.state.RoundPhase.INTRO -> {
                            roundState = state.withPhase(com.helldeck.ui.state.RoundPhase.INPUT)
                        }
                        com.helldeck.ui.state.RoundPhase.INPUT -> {
                            roundState = state.withPhase(com.helldeck.ui.state.RoundPhase.REVEAL)
                        }
                        com.helldeck.ui.state.RoundPhase.REVEAL -> {
                            roundState = state.withPhase(com.helldeck.ui.state.RoundPhase.FEEDBACK)
                            scene = Scene.FEEDBACK
                        }
                        com.helldeck.ui.state.RoundPhase.FEEDBACK -> {
                            roundState = state.withPhase(com.helldeck.ui.state.RoundPhase.DONE)
                        }
                        com.helldeck.ui.state.RoundPhase.DONE -> {
                            viewModelScope.launch { commitFeedbackAndNext() }
                        }
                    }
                }
            }
            else -> {
                com.helldeck.utils.Logger.w("Unhandled round event: $event")
            }
        }
    }

    /**
     * Resolves roast consensus voting by determining the majority target and awarding points.
     */
    private fun resolveRoastConsensus() {
        val targetId = votesAvatar.values.groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key

        targetId?.let { target ->
            val targetPlayer = players.find { it.id == target }
            if (targetPlayer != null) {
                addPoints(target, Config.current.scoring.win)
            }
        }
    }

    /**
     * Resolves confession or cap voting based on majority and pre-choice.
     */
    private fun resolveConfession() {
        val tVotes = votesAB.values.count { it == "T" }
        val fVotes = votesAB.values.count { it == "F" }
        val majority = if (tVotes >= fVotes) "T" else "F"

        val ap = activePlayer() ?: return

        if (preChoice == "TRUTH" && majority == "T") {
            // Truth wins - correct guessers get points
            players.forEach { p ->
                if (votesAB[p.id] == "T") {
                    addPoints(p.id, 1)
                }
            }
        } else if (preChoice == "BLUFF" && majority == "F") {
            // Bluff wins - correct guessers get points
            players.forEach { p ->
                if (votesAB[p.id] == "F") {
                    addPoints(p.id, 1)
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

        val ap = activePlayer() ?: return

        if (preChoice != null && preChoice == majority) {
            awardActive(Config.current.scoring.win)
        }
    }

    /**
     * Resolves smash or pass voting and awards points if majority smashes.
     */
    private fun resolveSmashPass() {
        val smash = votesAB.values.count { it.equals("SMASH", ignoreCase = true) || it == "A" }
        val pass = votesAB.values.count { it.equals("PASS", ignoreCase = true) || it == "B" }
        if (smash > pass) {
            awardActive(Config.current.scoring.win)
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
        roundState = roundState?.withPhase(com.helldeck.ui.state.RoundPhase.FEEDBACK)
        phase = RoundPhase.FEEDBACK
        scene = Scene.FEEDBACK
    }

    /**
     * Commits a direct win for the active player and transitions to feedback.
     *
     * @param pts The points to award (default: standard win points).
     */
    fun commitDirectWin(pts: Int = Config.current.scoring.win) {
        awardActive(pts)
        judgeWin = true
        points = pts
        roundState = roundState?.withPhase(com.helldeck.ui.state.RoundPhase.FEEDBACK)
        phase = RoundPhase.FEEDBACK
        scene = Scene.FEEDBACK
    }

    /**
     * Awards points to the currently active player.
     *
     * @param pts The points to award.
     */
    private fun awardActive(pts: Int) {
        activePlayer()?.let { addPoints(it.id, pts) }
    }

    /**
     * Adds points to the specified player and persists to the database.
     *
     * @param playerId The ID of the player.
     * @param delta The points to add (can be negative).
     */
    private fun addPoints(playerId: String, delta: Int) {
        val updated = players.map {
            if (it.id == playerId) {
                it.copy(sessionPoints = it.sessionPoints + delta)
            } else {
                it
            }
        }
        players = updated
        // Persist session points to DB
        if (isInitialized) {
            viewModelScope.launch {
                try {
                    repo.db.players().addPointsToPlayer(playerId, delta)
                } catch (e: Exception) {
                    com.helldeck.utils.Logger.e("Failed to persist points", e)
                }
            }
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
     */
    suspend fun commitFeedbackAndNext() {
        val card = currentCard ?: return
        val latency = (System.currentTimeMillis() - t0).toInt()

        // Use new Rewards system for consistent reward calculation
        val laughsScore = com.helldeck.content.quality.Rewards.fromCounts(lol, meh, trash)
        val responseTimeMs = System.currentTimeMillis() - t0
        val heatPercentage = (lol + trash).toDouble() / (lol + meh + trash).coerceAtLeast(1).toDouble()
        val winnerId = if (judgeWin) activePlayer()?.id else null

        try {
            // Record outcome with the engine if available
            if (!isInitialized) {
                com.helldeck.utils.Logger.w("Skipping outcome recording: not initialized")
            } else {
                engine.recordOutcome(
                    templateId = card.id,
                    reward01 = laughsScore
                )
                com.helldeck.utils.Logger.i("Recorded outcome for ${card.id}: reward=$laughsScore (LOL:$lol, MEH:$meh, TRASH:$trash)")
            }
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("commitFeedbackAndNext: recordOutcome failed", e)
        }

        // Reset feedback state
        lol = 0
        meh = 0
        trash = 0
        tags.clear()
        judgeWin = false
        points = 0

        // Persist long-term player stats
        // NOTE: Player scoring is now handled by GameEngine.recordOutcome, but we still need to update
        // the local player list and persist long-term stats like wins/games played.
        if (isInitialized) {
            activePlayer()?.id?.let { pid ->
                try {
                    repo.db.players().incGamesPlayed(pid)
                    if (judgeWin) repo.db.players().addWins(pid, 1)
                    if (points != 0) repo.db.players().addTotalPoints(pid, points)
                } catch (e: Exception) {
                    com.helldeck.utils.Logger.e("Failed to persist player stats", e)
                }
            }
            // Reload players to get updated scores
            reloadPlayers()
        }

        // Advance turn and start next round
        endRoundAdvanceTurn()
        startRound()
    }

    /**
     * Retrieves aggregated game statistics.
     * Currently returns an empty map as stats need re-implementation.
     *
     * @return Map of game statistics.
     */
    /**
     * Calculates laughs score based on feedback counts
     * @deprecated Use Rewards.fromCounts instead
     */
    @Deprecated("Use Rewards.fromCounts", ReplaceWith("com.helldeck.content.quality.Rewards.fromCounts(lol, meh, trash)"))
    private fun calculateLaughsScore(lol: Int, meh: Int, trash: Int): Double {
        return com.helldeck.content.quality.Rewards.fromCounts(lol, meh, trash)
    }

    /**
     * Retrieves aggregated game statistics.
     * Currently returns an empty map as stats need re-implementation.
     *
     * @return Map of game statistics.
     */
    suspend fun getGameStats(): Map<String, Any?> {
        // The new engine does not directly provide aggregated game stats in the same format.
        // This would need to be re-implemented based on the new TemplateStatEntity data.
        // For now, return an empty map or adapt to display available stats.
        return emptyMap()
    }

    suspend fun computePlayerProfiles(): List<com.helldeck.data.PlayerProfile> {
        return if (isInitialized) {
            repo.computePlayerProfiles()
        } else {
            emptyList()
        }
    }
}
