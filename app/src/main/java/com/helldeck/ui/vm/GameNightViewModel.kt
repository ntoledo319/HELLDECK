package com.helldeck.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.model.Player
import com.helldeck.content.quality.Rating
import com.helldeck.content.quality.Rewards
import com.helldeck.data.PlayerEntity
import com.helldeck.data.computePlayerProfiles
import com.helldeck.data.toEntity
import com.helldeck.data.toPlayer
import com.helldeck.engine.*
import com.helldeck.ui.Scene
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundPhase
import com.helldeck.ui.state.RoundState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * HELLDECK 2.0 Unified ViewModel
 *
 * Single source of truth for all game night state.
 * Manages navigation, players, rounds, and all 10 upgrades.
 *
 * CRITICAL: This is the ONLY ViewModel for game flow.
 * No other ViewModel should manage game state or call engine.next().
 */
class GameNightViewModel : ViewModel() {

    // ========== NAVIGATION ==========
    var scene by mutableStateOf(Scene.HOME)
    var showScores by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
    var selectedPlayerId by mutableStateOf<String?>(null)
    var selectedGameId by mutableStateOf<String?>(null)
    private val navStack = mutableListOf<Scene>()

    // ========== GAME CONFIGURATION ==========
    var spicy by mutableStateOf(false)
    var heatThreshold by mutableStateOf(Config.roomHeatThreshold().toFloat())

    // ========== PLAYER DATA ==========
    var players by mutableStateOf(listOf<Player>())
    var activePlayers by mutableStateOf(listOf<Player>())
    private var turnIdx by mutableStateOf(0)
    private var starterPicked = false

    // ========== SESSION MANAGEMENT ==========
    var gameNightSessionId by mutableStateOf("session_${System.currentTimeMillis()}")
    private var didRollcall = false
    private var askRollcallOnLaunch = true

    // ========== ROUND STATE (AUTHORITATIVE) ==========
    var roundState by mutableStateOf<RoundState?>(null)

    // Legacy fields (kept for backward compatibility during transition)
    var currentCard by mutableStateOf<FilledCard?>(null)
    var currentGame by mutableStateOf<GameInfo?>(null)
    var phase by mutableStateOf(RoundPhase.INTRO)

    // ========== VOTING STATE ==========
    var preChoice by mutableStateOf<String?>(null)
    var votesAvatar by mutableStateOf<Map<String, String>>(emptyMap())
    var votesAB by mutableStateOf<Map<String, String>>(emptyMap())

    // ========== FEEDBACK STATE ==========
    private var lol = 0
    private var meh = 0
    private var trash = 0
    private var tags: MutableSet<String> = mutableSetOf()
    private var judgeWin = false
    private var points = 0
    private var t0 = 0L

    // ========== CORE SYSTEMS ==========
    private lateinit var repo: ContentRepository
    private lateinit var engine: GameEngine
    private lateinit var cardBuffer: com.helldeck.content.engine.CardBuffer
    private lateinit var metricsTracker: com.helldeck.analytics.MetricsTracker
    private var isInitialized = false

    // ========== UPGRADE STATE (NEW FOR 2.0) ==========
    var enabledHouseRules by mutableStateOf(setOf<String>())
    var groupDnaProfile by mutableStateOf<String?>(null)
    var highlights by mutableStateOf(listOf<String>())
    var selectedPacks by mutableStateOf(setOf<String>())
    var playerRoles by mutableStateOf(mapOf<String, String>())

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
        cardBuffer = com.helldeck.content.engine.CardBuffer(engine, bufferSize = 3)
        metricsTracker = com.helldeck.analytics.MetricsTracker(repo)
        isInitialized = true

        // Load initial data
        reloadPlayers()

        // Check if should show onboarding for first-time users
        val hasSeenOnboarding = com.helldeck.settings.SettingsStore.readHasSeenOnboarding()
        if (!hasSeenOnboarding) {
            scene = Scene.ONBOARDING
        } else if (askRollcallOnLaunch && !didRollcall && players.isNotEmpty()) {
            // Determine if we should ask for rollcall on launch
            askRollcallOnLaunch = true
            scene = Scene.ROLLCALL
        }
        isLoading = false
    }

    /**
     * DEPRECATED: UI should use options from RoundState instead of recomputing.
     */
    @Deprecated("Use options from RoundState instead", ReplaceWith("roundState?.options"))
    fun getOptionsFor(
        card: FilledCard,
        req: GameEngine.Request
    ): GameOptions {
        return try {
            if (!isInitialized) {
                com.helldeck.utils.Logger.w("getOptionsFor called before initialization")
                return GameOptions.None
            }
            engine.getOptionsFor(card, req)
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("getOptionsFor failed", e)
            GameOptions.None
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

    // ========== NAVIGATION ==========

    fun openProfile(playerId: String) {
        selectedPlayerId = playerId
        navigateTo(Scene.PROFILE)
    }

    fun openRulesForCurrentGame() {
        selectedGameId = currentGame?.id
        navigateTo(Scene.GAME_RULES)
    }

    fun navigateTo(target: Scene) {
        if (target != scene) {
            navStack.add(scene)
            scene = target
        }
    }

    fun canGoBack(): Boolean = navStack.isNotEmpty()

    fun goBack() {
        if (navStack.isNotEmpty()) {
            scene = navStack.removeAt(navStack.lastIndex)
        } else {
            scene = Scene.HOME
        }
    }

    fun goHome() {
        navStack.clear()
        scene = Scene.HOME

        // Stop card buffering when leaving game
        if (::cardBuffer.isInitialized) {
            cardBuffer.stop()
            com.helldeck.utils.Logger.d("CardBuffer stopped: ${cardBuffer.getStats()}")
        }

        // End metrics tracking when leaving game
        if (::metricsTracker.isInitialized) {
            viewModelScope.launch {
                try {
                    metricsTracker.endSession(gameNightSessionId)
                } catch (e: Exception) {
                    com.helldeck.utils.Logger.e("Failed to end session metrics", e)
                }
            }
        }
    }

    fun goPlayers() {
        scene = Scene.PLAYERS
    }

    fun toggleScores() {
        showScores = !showScores
    }

    // ========== SESSION MANAGEMENT ==========

    fun startNewGameNight() {
        gameNightSessionId = "session_${System.currentTimeMillis()}"
        turnIdx = 0
        starterPicked = false
        didRollcall = false
        com.helldeck.utils.Logger.i("New game night started: $gameNightSessionId")

        // Start metrics tracking for new session
        if (isInitialized && ::metricsTracker.isInitialized) {
            viewModelScope.launch {
                val playerIds = activePlayers.map { it.id }
                metricsTracker.startSession(gameNightSessionId, playerIds)
            }
        }
    }

    fun markRollcallDone() {
        didRollcall = true
    }

    // ========== ROUND MANAGEMENT ==========

    suspend fun startRound(gameId: String? = null) {
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
            scene = Scene.PLAYERS
            return
        }

        scene = Scene.ROUND
        phase = RoundPhase.INTRO

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

        val sessionId = gameNightSessionId

        try {
            // Create request for card generation
            val request = GameEngine.Request(
                gameId = nextGame,
                sessionId = sessionId,
                spiceMax = if (spicy) 3 else 1,
                players = playersList
            )

            // Start buffering for this game if not already started
            if (!::cardBuffer.isInitialized || gameId != null) {
                // New game or game change - restart buffer
                cardBuffer.stop()
                cardBuffer.start(request)
            }

            // Get card from buffer (instant if buffer ready, otherwise generates on-demand)
            val gameResult = cardBuffer.getNext()

            // Create authoritative RoundState from engine result
            roundState = RoundState(
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

            currentCard = gameResult.filledCard

            // Start metrics tracking for this round
            if (::metricsTracker.isInitialized) {
                val roundId = "round_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}"
                metricsTracker.startRound(
                    roundId = roundId,
                    sessionId = sessionId,
                    gameId = nextGame,
                    cardId = gameResult.filledCard.id,
                    cardText = gameResult.filledCard.text,
                    activePlayerId = activePlayer()?.id ?: "unknown",
                    spiceLevel = if (spicy) 3 else 1
                )
            }

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

    private fun pickNextGame(): String {
        val cfg = Config.current.mechanics

        return if (cfg.comeback_last_place_picks_next && players.size >= 3) {
            val lastPlaceIds = getLastPlaceIds()
            if (lastPlaceIds.isNotEmpty()) {
                listOf(GameIds.ROAST_CONS, GameIds.POISON_PITCH, GameIds.MAJORITY).random()
            } else {
                GameMetadata.getAllGameIds().random()
            }
        } else {
            GameMetadata.getAllGameIds().random()
        }
    }

    private fun getLastPlaceIds(): List<String> {
        if (players.isEmpty()) return emptyList()
        val minPts = players.minOf { it.sessionPoints }
        return players.filter { it.sessionPoints == minPts }.map { it.id }
    }

    fun endRoundAdvanceTurn() {
        val poolSize = activePlayers.size.coerceAtLeast(1)
        turnIdx = (turnIdx + 1) % poolSize
    }

    fun activePlayer(): Player? {
        return if (activePlayers.isEmpty()) null else activePlayers[turnIdx % activePlayers.size]
    }

    // ========== EVENT HANDLING ==========

    fun handleRoundEvent(event: RoundEvent) {
        when (event) {
            is RoundEvent.PickAB -> {
                onABVote(activePlayer()?.id ?: "", event.choice)
            }
            is RoundEvent.VotePlayer -> {
                val voterId = activePlayer()?.id ?: return
                val targetId = activePlayers.getOrNull(event.playerIndex)?.id ?: return
                onAvatarVote(voterId, targetId)
            }
            is RoundEvent.PreChoice -> {
                onPreChoice(event.choice)
            }
            is RoundEvent.SelectTarget -> {
                val targetId = activePlayers.getOrNull(event.playerIndex)?.id
                com.helldeck.utils.Logger.d("Target selected: $targetId")
            }
            is RoundEvent.RateCard -> {
                when (event.rating) {
                    Rating.LOL -> feedbackLol()
                    Rating.MEH -> feedbackMeh()
                    Rating.TRASH -> feedbackTrash()
                }
            }
            is RoundEvent.AdvancePhase -> {
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

    // ========== VOTING ==========

    fun onPreChoice(choice: String) {
        preChoice = choice
    }

    fun onAvatarVote(voterId: String, targetId: String) {
        votesAvatar = votesAvatar + (voterId to targetId)
    }

    fun onABVote(voterId: String, choice: String) {
        votesAB = votesAB + (voterId to choice)
    }

    // ========== RESOLUTION ==========

    fun resolveInteraction() {
        val state = roundState
        if (state != null) {
            when (state.interactionType) {
                InteractionType.VOTE_PLAYER -> resolveRoastConsensus()
                InteractionType.TRUE_FALSE -> resolveConfession()
                InteractionType.A_B_CHOICE -> resolveAB()
                InteractionType.PREDICT_VOTE -> resolveAB()
                InteractionType.SMASH_PASS -> resolveSmashPass()
                InteractionType.JUDGE_PICK -> {
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
        } else {
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

        phase = RoundPhase.FEEDBACK
        scene = Scene.FEEDBACK
    }

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

    private fun resolveConfession() {
        val tVotes = votesAB.values.count { it == "T" }
        val fVotes = votesAB.values.count { it == "F" }
        val majority = if (tVotes >= fVotes) "T" else "F"

        val ap = activePlayer() ?: return

        if (preChoice == "TRUTH" && majority == "T") {
            players.forEach { p ->
                if (votesAB[p.id] == "T") {
                    addPoints(p.id, 1)
                }
            }
        } else if (preChoice == "BLUFF" && majority == "F") {
            players.forEach { p ->
                if (votesAB[p.id] == "F") {
                    addPoints(p.id, 1)
                }
            }
        }
    }

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

    private fun resolveSmashPass() {
        val smash = votesAB.values.count { it.equals("SMASH", ignoreCase = true) || it == "A" }
        val pass = votesAB.values.count { it.equals("PASS", ignoreCase = true) || it == "B" }
        if (smash > pass) {
            awardActive(Config.current.scoring.win)
            judgeWin = true
            points = Config.current.scoring.win
        }
    }

    fun goToFeedbackNoPoints() {
        judgeWin = false
        points = 0
        phase = RoundPhase.FEEDBACK
        scene = Scene.FEEDBACK
    }

    fun commitDirectWin(pts: Int = Config.current.scoring.win) {
        awardActive(pts)
        judgeWin = true
        points = pts
        phase = RoundPhase.FEEDBACK
        scene = Scene.FEEDBACK
    }

    private fun awardActive(pts: Int) {
        activePlayer()?.let { addPoints(it.id, pts) }
    }

    private fun addPoints(playerId: String, delta: Int) {
        val updated = players.map {
            if (it.id == playerId) {
                it.copy(sessionPoints = it.sessionPoints + delta)
            } else {
                it
            }
        }
        players = updated
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

    // ========== FEEDBACK ==========

    fun feedbackLol() {
        lol++
    }

    fun feedbackMeh() {
        meh++
    }

    fun feedbackTrash() {
        trash++
    }

    fun addComment(text: String, t: Set<String>) {
        if (text.isNotBlank()) {
            tags.add("note")
        }
        tags.addAll(t)
    }

    suspend fun commitFeedbackAndNext() {
        val card = currentCard ?: return
        val latency = (System.currentTimeMillis() - t0).toInt()

        val laughsScore = Rewards.fromCounts(lol, meh, trash)
        val responseTimeMs = System.currentTimeMillis() - t0
        val heatPercentage = (lol + trash).toDouble() / (lol + meh + trash).coerceAtLeast(1).toDouble()
        val winnerId = if (judgeWin) activePlayer()?.id else null

        try {
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

        // Record round metrics
        if (::metricsTracker.isInitialized) {
            try {
                metricsTracker.completeRound(
                    lolCount = lol,
                    mehCount = meh,
                    trashCount = trash,
                    points = points
                )
            } catch (e: Exception) {
                com.helldeck.utils.Logger.e("Failed to record round metrics", e)
            }
        }

        // Reset feedback state
        lol = 0
        meh = 0
        trash = 0
        tags.clear()
        judgeWin = false
        points = 0

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
            reloadPlayers()
        }

        endRoundAdvanceTurn()
        startRound()
    }

    suspend fun getGameStats(): Map<String, Any?> {
        return emptyMap()
    }

    suspend fun computePlayerProfiles(): List<com.helldeck.data.PlayerProfile> {
        return if (isInitialized) {
            repo.computePlayerProfiles()
        } else {
            emptyList()
        }
    }

    // ========== FAVORITES ==========

    /**
     * Toggles favorite status for the current card.
     * Returns true if favorited, false if unfavorited.
     */
    suspend fun toggleFavorite(): Boolean {
        val card = currentCard ?: return false
        val game = currentGame ?: return false
        val player = activePlayer()

        return try {
            val existing = repo.db.favorites().isFavorited(card.id, gameNightSessionId)
            if (existing != null) {
                // Unfavorite
                repo.db.favorites().delete(existing)
                com.helldeck.utils.Logger.d("Unfavorited card: ${card.id}")
                false
            } else {
                // Favorite
                val favorite = com.helldeck.data.FavoriteCardEntity(
                    id = "fav_${System.currentTimeMillis()}_${card.id}",
                    cardId = card.id,
                    cardText = card.text,
                    gameId = game.id,
                    gameName = game.name,
                    sessionId = gameNightSessionId,
                    playerId = player?.id,
                    playerName = player?.name,
                    lolCount = lol
                )
                repo.db.favorites().insert(favorite)
                com.helldeck.utils.Logger.d("Favorited card: ${card.id}")
                true
            }
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to toggle favorite", e)
            false
        }
    }

    /**
     * Checks if the current card is favorited.
     */
    suspend fun isCurrentCardFavorited(): Boolean {
        val card = currentCard ?: return false
        return try {
            repo.db.favorites().isFavorited(card.id, gameNightSessionId) != null
        } catch (e: Exception) {
            false
        }
    }
}
