package com.helldeck.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.db.HelldeckDb
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.model.Player
import com.helldeck.content.quality.Rating
import com.helldeck.content.quality.Rewards
import com.helldeck.content.reporting.ContentReport
import com.helldeck.content.reporting.ContentReportStore
import com.helldeck.data.PlayerEntity
import com.helldeck.data.computePlayerProfiles
import com.helldeck.data.toPlayer
import com.helldeck.engine.*
import com.helldeck.settings.CrewBrain
import com.helldeck.settings.CrewBrainStore
import com.helldeck.ui.Scene
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundPhase
import com.helldeck.ui.state.RoundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val navStack = ArrayDeque<Scene>(10) // Bounded to 10 scenes max to prevent memory leak

    // ========== GAME CONFIGURATION ==========
    var spicy by mutableStateOf(false)
    var heatThreshold by mutableStateOf(Config.roomHeatThreshold().toFloat())
    private val _spiceLevel = MutableStateFlow(3) // 1-5 slider for content heat
    val spiceLevel: StateFlow<Int> = _spiceLevel.asStateFlow()

    // ========== PLAYER DATA ==========
    var players by mutableStateOf(listOf<Player>())
    var activePlayers by mutableStateOf(listOf<Player>())
    private var turnIdx by mutableStateOf(0)
    private var starterPicked = false

    // ========== SESSION MANAGEMENT ==========
    var gameNightSessionId by mutableStateOf("session_${System.currentTimeMillis()}")
    private var didRollcall = false
    private var askRollcallOnLaunch = true
    private var sessionStartTimeMs = System.currentTimeMillis()

    // ========== MILESTONE TRACKING ==========
    var pendingMilestone by mutableStateOf<com.helldeck.ui.components.Milestone?>(null)
    private var consecutiveWins = 0
    private val gamesPlayedThisSession = mutableSetOf<String>()
    private var totalRoundsThisSession = 0

    // ========== FEEDBACK TRACKING ==========
    private var feedbackSessionId: Long = System.currentTimeMillis()
    private var currentImpressionId: Long? = null
    private val sessionCardIds = mutableListOf<String>()
    private var quickFireTriggered = false

    // ========== ROUND STATE (AUTHORITATIVE) ==========
    var roundState by mutableStateOf<RoundState?>(null)

    // Last card for replay functionality
    private var lastCard: FilledCard? = null
    private var lastGameId: String? = null

    // ========== VOTING STATE ==========
    var preChoice by mutableStateOf<String?>(null)
    var votesAvatar by mutableStateOf<Map<String, String>>(emptyMap())
    var votesAB by mutableStateOf<Map<String, String>>(emptyMap())
    
    // ========== GAME-SPECIFIC STATE ==========
    // Taboo Timer: track successful guesses and forbidden word violations
    var tabooSuccessfulGuesses by mutableStateOf(0)
    var tabooForbiddenWordCount by mutableStateOf(0)
    
    // Reality Check: track ego and group ratings
    var realityCheckEgoRating by mutableStateOf<Int?>(null)
    var realityCheckGroupRating by mutableStateOf<Int?>(null)
    
    // Over/Under: track the betting line and actual value
    var overUnderLine by mutableStateOf<Int?>(null)
    var overUnderActualValue by mutableStateOf<Int?>(null)
    
    // Scatterblast: track turn ownership when bomb explodes
    var scatterblastVictimId by mutableStateOf<String?>(null)
    
    // Hot Seat Imposter: track target and imposter roles
    var hotSeatTargetId by mutableStateOf<String?>(null)
    var hotSeatImposterId by mutableStateOf<String?>(null)
    
    // Alibi Drop: track mandatory words and whether jury caught them
    var alibiMandatoryWords by mutableStateOf<List<String>>(emptyList())
    var alibiWordsDetected by mutableStateOf<List<String>>(emptyList())
    var alibiStoryBelievable by mutableStateOf<Boolean?>(null)
    
    // Title Fight: track duel participants and winner
    var titleFightChallengerId by mutableStateOf<String?>(null)
    var titleFightWinnerId by mutableStateOf<String?>(null)
    
    // Poison Pitch: track which players defend which option
    var poisonPitchDefenderA by mutableStateOf<String?>(null) // Player ID defending Option A
    var poisonPitchDefenderB by mutableStateOf<String?>(null) // Player ID defending Option B

    // ========== FEEDBACK STATE ==========
    private var lol = 0
    private var meh = 0
    private var trash = 0
    private var tags: MutableSet<String> = mutableSetOf()
    private var judgeWin = false
    private var points = 0
    private var t0 = 0L

    // Undo state for feedback ratings
    private data class FeedbackSnapshot(
        val lol: Int,
        val meh: Int,
        val trash: Int,
    )
    private var feedbackSnapshot: FeedbackSnapshot? = null
    var canUndoFeedback by mutableStateOf(false)

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

    // ========== CREW BRAIN MANAGEMENT ==========
    var crewBrains by mutableStateOf<List<CrewBrain>>(emptyList())
    var activeCrewBrainId by mutableStateOf<String?>(null)

    // ========== CONTENT REPORTING ==========
    var showReportDialog by mutableStateOf(false)
    private var reportStore: ContentReportStore = ContentReportStore()

    private suspend fun buildCoreSystems() {
        val context = AppCtx.ctx
        repo = ContentRepository(context)
        repo.initialize()
        engine = ContentEngineProvider.get(context)
        cardBuffer = com.helldeck.content.engine.CardBuffer(engine, bufferSize = 3)
        metricsTracker = com.helldeck.analytics.MetricsTracker(repo)
    }

    /**
     * Initializes the ViewModel systems on first use.
     * Sets up the content repository, game engine, and loads initial data.
     * Can be called multiple times safely.
     */
    suspend fun initOnce() {
        if (isInitialized) return
        isLoading = true

        val context = AppCtx.ctx
        val (brains, activeId) = CrewBrainStore.ensureInitialized()
        crewBrains = brains
        activeCrewBrainId = activeId

        try {
            buildCoreSystems()
            isInitialized = true

            // Initialize sound system
            val soundEnabled = com.helldeck.settings.SettingsStore.readSoundEnabled()
            val soundManager = com.helldeck.audio.SoundManager.get(context)
            soundManager.enabled = soundEnabled

            // Load content reports
            reportStore = ContentReportStore.load(context)
            com.helldeck.utils.Logger.i("Loaded ${reportStore.getReportCount()} content reports")

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
        } finally {
            isLoading = false
        }
    }

    /**
     * DEPRECATED: UI should use options from RoundState instead of recomputing.
     */
    @Deprecated("Use options from RoundState instead", ReplaceWith("roundState?.options"))
    fun getOptionsFor(
        card: FilledCard,
        req: GameEngine.Request,
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
                "🐸 Mo" to "🐸",
            )
            defaultPlayers.forEach { (name, avatar) ->
                val id = "p${Random.nextInt(100000)}"
                repo.db.players().upsert(
                    PlayerEntity(
                        id = id,
                        name = name,
                        avatar = avatar,
                        sessionPoints = 0,
                    ),
                )
            }

            players = repo.db.players().getAllPlayers().first().map { it.toPlayer() }
            activePlayers = players.filter { p -> p.afk == 0 }
        }
    }

    suspend fun switchCrewBrain(brainId: String) {
        isLoading = true
        try {
            CrewBrainStore.setActiveBrain(brainId)
            crewBrains = CrewBrainStore.getBrains()
            activeCrewBrainId = brainId

            if (::cardBuffer.isInitialized) {
                cardBuffer.stop()
            }
            ContentEngineProvider.reset()
            HelldeckDb.clearCache()
            buildCoreSystems()
            isInitialized = true

            roundState = null
            preChoice = null
            votesAvatar = emptyMap()
            votesAB = emptyMap()

            reloadPlayers()
            startNewGameNight()
            navStack.clear()
            scene = Scene.HOME
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to switch crew brain", e)
        } finally {
            isLoading = false
        }
    }

    suspend fun createCrewBrain(name: String, emoji: String): CrewBrain? {
        return try {
            val brain = CrewBrainStore.createBrain(name, emoji)
            switchCrewBrain(brain.id)
            brain
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to create crew brain", e)
            null
        }
    }

    // ========== NAVIGATION ==========

    fun openProfile(playerId: String) {
        selectedPlayerId = playerId
        navigateTo(Scene.PROFILE)
    }

    fun openRulesForCurrentGame() {
        selectedGameId = roundState?.gameId
        navigateTo(Scene.GAME_RULES)
    }

    fun navigateTo(target: Scene) {
        if (target != scene) {
            if (navStack.size >= 10) {
                navStack.removeFirst() // Remove oldest if at capacity
            }
            navStack.addLast(scene)
            scene = target
        }
    }

    fun canGoBack(): Boolean = navStack.isNotEmpty()

    fun goBack() {
        if (navStack.isNotEmpty()) {
            scene = navStack.removeLast()
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

    fun updateSpiceLevel(level: Int) {
        _spiceLevel.value = level.coerceIn(1, 5)
        spicy = _spiceLevel.value >= 3
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
        sessionStartTimeMs = System.currentTimeMillis()
        totalRoundsThisSession = 0
        consecutiveWins = 0
        gamesPlayedThisSession.clear()
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
        Config.spicyMode = _spiceLevel.value >= 3

        // Pick next game (random or selected)
        val nextGame = gameId ?: pickNextGame()
        val currentGameMeta = GameMetadata.getGameMetadata(nextGame)

        // Pick starter if not already picked
        if (!starterPicked) {
            turnIdx = if (activePlayers.isNotEmpty()) Random.nextInt(activePlayers.size) else 0
            starterPicked = true
        }

        // Generate card - use seat numbers instead of player names for anonymity
        val playersList = activePlayers.mapIndexed { idx, _ -> "Seat ${idx + 1}" }
        val targetPlayerId = if (currentGameMeta?.interaction == Interaction.TARGET_PICK) {
            activePlayers.randomOrNull()?.id
        } else {
            null
        }

        // Poison Pitch: Randomly assign two players to defend Option A and Option B
        if (nextGame == GameIds.POISON_PITCH && activePlayers.size >= 2) {
            val debaters = activePlayers.shuffled().take(2)
            poisonPitchDefenderA = debaters[0].id
            poisonPitchDefenderB = debaters[1].id
            com.helldeck.utils.Logger.d("Poison Pitch debaters assigned: A=Seat ${activePlayers.indexOf(debaters[0]) + 1}, B=Seat ${activePlayers.indexOf(debaters[1]) + 1}")
        }

        val sessionId = gameNightSessionId

        try {
            // Create request for card generation
            val gameMeta = GameMetadata.getGameMetadata(nextGame)

            val request = GameEngine.Request(
                gameId = nextGame,
                sessionId = sessionId,
                spiceMax = _spiceLevel.value,
                players = playersList,
            )

            // Start buffering for this game if not already started
            if (!::cardBuffer.isInitialized || gameId != null) {
                // New game or game change - restart buffer
                cardBuffer.stop()
                cardBuffer.start(request)
            }

            // Get card from buffer
            val gameResult = cardBuffer.getNext()

            // Create authoritative RoundState from engine result
            roundState = RoundState(
                gameId = nextGame,
                filledCard = gameResult.filledCard,
                options = gameResult.options,
                timerSec = gameResult.timer,
                interactionType = gameResult.interactionType,
                activePlayerIndex = turnIdx.coerceAtMost(activePlayers.size - 1).coerceAtLeast(0),
                judgePlayerIndex = if (gameMeta?.interaction == Interaction.JUDGE_PICK && activePlayers.isNotEmpty()) {
                    (turnIdx + 1) % activePlayers.size
                } else {
                    null
                },
                targetPlayerIndex = targetPlayerId?.let { id ->
                    activePlayers.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                },
                phase = com.helldeck.ui.state.RoundPhase.INTRO,
                sessionId = sessionId,
            )

            // Save for replay
            lastCard = gameResult.filledCard
            lastGameId = nextGame

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
                    spiceLevel = _spiceLevel.value,
                )
            }

            // Track card impression for feedback system
            try {
                val impression = com.helldeck.data.CardImpressionEntity(
                    sessionId = feedbackSessionId,
                    cardId = gameResult.filledCard.id,
                    gameId = nextGame,
                )
                currentImpressionId = repo.db.cardFeedback().insertImpression(impression)
                if (!sessionCardIds.contains(gameResult.filledCard.id)) {
                    sessionCardIds.add(gameResult.filledCard.id)
                }
                quickFireTriggered = false
            } catch (e: Exception) {
                com.helldeck.utils.Logger.e("Failed to track card impression", e)
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
                // Comeback games - easy voting games for last place
                listOf(GameIds.ROAST_CONS, GameIds.CONFESS_CAP, GameIds.REALITY_CHECK).random()
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
        
        // Reset all game-specific state
        resetGameSpecificState()
    }
    
    /**
     * Resets all game-specific state variables between rounds.
     * Called at the end of each round to ensure clean slate for next game.
     */
    private fun resetGameSpecificState() {
        // Taboo Timer
        tabooSuccessfulGuesses = 0
        tabooForbiddenWordCount = 0
        
        // Reality Check
        realityCheckEgoRating = null
        realityCheckGroupRating = null
        
        // Over/Under
        overUnderLine = null
        overUnderActualValue = null
        
        // Scatterblast
        scatterblastVictimId = null
        
        // Hot Seat Imposter
        hotSeatTargetId = null
        hotSeatImposterId = null
        
        // Alibi Drop
        alibiMandatoryWords = emptyList()
        alibiWordsDetected = emptyList()
        alibiStoryBelievable = null
        
        // Title Fight
        titleFightChallengerId = null
        titleFightWinnerId = null
        
        // Poison Pitch
        poisonPitchDefenderA = null
        poisonPitchDefenderB = null
        
        // Reset general voting state
        preChoice = null
        votesAvatar = emptyMap()
        votesAB = emptyMap()
    }
    
    // ========== GAME-SPECIFIC STATE SETTERS ==========
    
    fun setTabooGuess(success: Boolean) {
        if (success) {
            tabooSuccessfulGuesses++
        }
    }
    
    fun setTabooForbiddenWord() {
        tabooForbiddenWordCount++
    }
    
    fun setRealityCheckRatings(ego: Int, group: Int) {
        realityCheckEgoRating = ego
        realityCheckGroupRating = group
    }
    
    fun setOverUnderData(line: Int, actualValue: Int) {
        overUnderLine = line
        overUnderActualValue = actualValue
    }
    
    fun setScatterblastVictim(playerId: String) {
        scatterblastVictimId = playerId
    }
    
    fun setHotSeatRoles(targetId: String, imposterId: String) {
        hotSeatTargetId = targetId
        hotSeatImposterId = imposterId
    }
    
    fun setAlibiData(mandatoryWords: List<String>, detectedWords: List<String>, believable: Boolean) {
        alibiMandatoryWords = mandatoryWords
        alibiWordsDetected = detectedWords
        alibiStoryBelievable = believable
    }
    
    fun setTitleFightResult(challengerId: String, winnerId: String) {
        titleFightChallengerId = challengerId
        titleFightWinnerId = winnerId
    }
    
    fun setPoisonPitchDefenders(defenderAId: String, defenderBId: String) {
        poisonPitchDefenderA = defenderAId
        poisonPitchDefenderB = defenderBId
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
        val gameId = state?.gameId
        
        if (state != null) {
            when (state.interactionType) {
                InteractionType.VOTE_PLAYER -> resolveRoastConsensus()
                InteractionType.TRUE_FALSE -> resolveConfession()
                InteractionType.A_B_CHOICE -> resolveAB()
                InteractionType.PREDICT_VOTE -> resolveAB()
                InteractionType.SMASH_PASS -> resolveSmashPass()
                InteractionType.REPLY_TONE -> resolveTextThreadTrap()
                InteractionType.TABOO_GUESS -> resolveTabooTimer()
                InteractionType.ODD_EXPLAIN -> resolveUnifyingTheory()
                InteractionType.MINI_DUEL -> resolveTitleFight()
                InteractionType.HIDE_WORDS -> resolveAlibiDrop()
                InteractionType.TARGET_SELECT -> resolveRealityCheck()
                InteractionType.SPEED_LIST -> resolveScatterblast()
                InteractionType.JUDGE_PICK -> {
                    // Hot Seat Imposter has specific resolution logic
                    if (gameId == GameIds.HOTSEAT_IMP) {
                        resolveHotSeatImposter()
                    } else {
                        // Fill-In Finisher awards +1 point per HDRealRules.md, other judge games use default
                        val pointsToAward = if (gameId == GameIds.FILLIN) 1 else Config.current.scoring.win
                        judgeWin = true
                        points = pointsToAward
                        awardActive(pointsToAward)
                    }
                }
                else -> {
                    judgeWin = true
                    points = Config.current.scoring.win
                    awardActive(points)
                }
            }
        } else {
            val r = roundState ?: return
            val game = GameMetadata.getGameMetadata(r.gameId) ?: return
            when (game.interaction) {
                Interaction.VOTE_AVATAR -> resolveRoastConsensus()
                Interaction.TRUE_FALSE -> resolveConfession()
                Interaction.AB_VOTE -> resolveAB()
                Interaction.SMASH_PASS -> resolveSmashPass()
                Interaction.REPLY_TONE -> resolveTextThreadTrap()
                Interaction.TABOO_CLUE -> resolveTabooTimer()
                Interaction.ODD_REASON -> resolveUnifyingTheory()
                Interaction.DUEL -> resolveTitleFight()
                Interaction.SMUGGLE -> resolveAlibiDrop()
                Interaction.TARGET_PICK -> resolveRealityCheck()
                Interaction.SPEED_LIST -> resolveScatterblast()
                Interaction.JUDGE_PICK -> {
                    // Hot Seat Imposter has specific resolution logic
                    if (game.id == GameIds.HOTSEAT_IMP) {
                        resolveHotSeatImposter()
                    } else {
                        // Fill-In Finisher awards +1 point per HDRealRules.md, other judge games use default
                        val pointsToAward = if (game.id == GameIds.FILLIN) 1 else Config.current.scoring.win
                        judgeWin = true
                        points = pointsToAward
                        awardActive(pointsToAward)
                    }
                }
                else -> {
                    judgeWin = true
                    points = Config.current.scoring.win
                    awardActive(points)
                }
            }
        }

        roundState = roundState?.copy(phase = RoundPhase.FEEDBACK)
        scene = Scene.FEEDBACK
    }

    private fun resolveRoastConsensus() {
        val voteCounts = votesAvatar.values.groupBy { it }
        val targetId = voteCounts.maxByOrNull { it.value.size }?.key
        val totalVotes = votesAvatar.size
        val majorityVotes = targetId?.let { voteCounts[it]?.size ?: 0 } ?: 0
        val roomHeatThreshold = (totalVotes * 0.8).toInt()

        // Award points to voters who picked the majority target (+2 for majority pick)
        targetId?.let { target ->
            votesAvatar.forEach { (voterId, votedTarget) ->
                if (votedTarget == target) {
                    var pointsToAward = 2 // Majority pick: +2 points
                    // Room Heat Bonus: If 80%+ of the room agrees, +1 bonus
                    if (majorityVotes >= roomHeatThreshold) {
                        pointsToAward += 1
                    }
                    addPoints(voterId, pointsToAward)
                }
            }
        }
    }

    private fun resolveConfession() {
        val tVotes = votesAB.values.count { it == "T" }
        val fVotes = votesAB.values.count { it == "F" }
        val majority = if (tVotes >= fVotes) "T" else "F"
        val totalVoters = votesAB.size
        val ap = activePlayer() ?: return

        // Confessor wins if they fool the majority (everyone guessed wrong)
        val confessorFooledMajority = when (preChoice) {
            "TRUTH" -> majority == "F" // Confessor said truth, but majority voted false (wrong)
            "BLUFF" -> majority == "T" // Confessor said bluff, but majority voted true (wrong)
            else -> false
        }

        if (confessorFooledMajority) {
            // Confessor successfully fooled the majority: +2 points
            addPoints(ap.id, 2)
        } else {
            // Voters win: If you correctly called the lie (or truth), you get +1 point
            val correctAnswer = when (preChoice) {
                "TRUTH" -> "T"
                "BLUFF" -> "F"
                else -> null
            }
            correctAnswer?.let { correct ->
                players.forEach { p ->
                    if (votesAB[p.id] == correct) {
                        addPoints(p.id, 1)
                    }
                }
            }

            // Room Heat Bonus: If the ENTIRE room agrees (100%) and gets it right, everyone gets +1 bonus
            // This means all voters voted the same correct answer
            val allVotedCorrect = votesAB.values.isNotEmpty() && votesAB.values.all { it == correctAnswer }
            if (allVotedCorrect && correctAnswer != null && totalVoters > 0) {
                players.forEach { p ->
                    if (votesAB.containsKey(p.id)) {
                        addPoints(p.id, 1) // Bonus point for unanimous correct vote
                    }
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
        val currentGameId = roundState?.gameId

        when (currentGameId) {
            GameIds.POISON_PITCH -> {
                // Poison Pitch: Winning Pitcher gets +2 points (per HDRealRules.md)
                // Players are ASSIGNED their side randomly, not by choice
                val defenderA = poisonPitchDefenderA
                val defenderB = poisonPitchDefenderB
                
                if (defenderA != null && defenderB != null && majority != "TIE") {
                    // Award to the player who defended the winning option
                    val winnerId = when (majority) {
                        "A" -> defenderA
                        "B" -> defenderB
                        else -> null
                    }
                    winnerId?.let { 
                        addPoints(it, 2)
                        // Track for feedback display
                        if (activePlayer()?.id == winnerId) {
                            judgeWin = true
                            points = 2
                        }
                    }
                }
            }
            // MAJORITY_REPORT removed - not in official 14 games
            GameIds.OVER_UNDER -> {
                // Over/Under: Proper implementation per HDRealRules.md
                val line = overUnderLine
                val actualValue = overUnderActualValue
                
                if (line != null && actualValue != null) {
                    // Check for exact match first (everyone drinks except Subject who is "god")
                    if (actualValue == line) {
                        // Exact match: everyone drinks, Subject gets 0 points (but is celebrated)
                        judgeWin = false
                        points = 0
                    } else {
                        // Determine correct bet
                        val correctBet = if (actualValue > line) "OVER" else "UNDER"
                        
                        // Winners: Those who bet correctly get +1
                        players.forEach { p ->
                            if (p.id != ap.id) {
                                val playerBet = votesAB[p.id]
                                if (playerBet == correctBet || 
                                    (playerBet == "A" && correctBet == "OVER") ||
                                    (playerBet == "B" && correctBet == "UNDER")) {
                                    addPoints(p.id, 1)
                                }
                            }
                        }
                        
                        // Subject (The House): Gets points equal to number of wrong guesses
                        val wrongGuesses = votesAB.values.count { bet ->
                            bet != correctBet && 
                            !((bet == "A" && correctBet == "OVER") || (bet == "B" && correctBet == "UNDER"))
                        }
                        if (wrongGuesses > 0) {
                            addPoints(ap.id, wrongGuesses)
                        }
                    }
                }
            }
            else -> {
                // Default: Active wins if pre-choice matches majority
                if (preChoice != null && preChoice == majority) {
                    awardActive(Config.current.scoring.win)
                }
            }
        }
    }

    private fun resolveSmashPass() {
        val smash = votesAB.values.count { it.equals("SMASH", ignoreCase = true) || it == "A" }
        val pass = votesAB.values.count { it.equals("PASS", ignoreCase = true) || it == "B" }
        if (smash > pass) {
            // Red Flag Rally: Defender (Majority SMASH) gets +2 points (per HDRealRules.md)
            awardActive(2)
            judgeWin = true
            points = 2
        }
    }

    /**
     * Hot Seat Imposter (Game 6) - HDRealRules.md compliant
     * Scoring:
     * - Imposter (Fools Majority): +2 Points
     * - Target (Group Sees Through): +1 Point
     * - Voters (Correct Guess): +1 Point each
     */
    private fun resolveHotSeatImposter() {
        val targetId = hotSeatTargetId ?: return
        val imposterId = hotSeatImposterId ?: return
        
        val realVotes = votesAB.values.count { it == "REAL" }
        val fakeVotes = votesAB.values.count { it == "FAKE" }
        val majorityBelieved = realVotes > fakeVotes
        
        if (majorityBelieved) {
            // Imposter fooled majority: +2 points to imposter
            addPoints(imposterId, 2)
            judgeWin = true
            points = 2
        } else {
            // Group saw through it: +1 to target
            addPoints(targetId, 1)
            // All voters who correctly voted FAKE get +1
            players.forEach { p ->
                if (votesAB[p.id] == "FAKE") {
                    addPoints(p.id, 1)
                }
            }
        }
    }

    /**
     * Text Thread Trap (Game 7) - HDRealRules.md compliant
     * Scoring:
     * - Success (Majority Vote): +2 Points
     * - Failure (Breaking Character): -1 Point
     * - Room Heat Bonus: +1 for perfect improvisation
     */
    private fun resolveTextThreadTrap() {
        val successVotes = votesAB.values.count { it == "SUCCESS" || it == "A" }
        val failureVotes = votesAB.values.count { it == "FAILURE" || it == "B" }
        val totalVotes = votesAB.size
        
        if (successVotes > failureVotes) {
            // Success: +2 points
            var pointsToAward = 2
            
            // Room Heat Bonus: if everyone voted success (perfect improvisation)
            if (successVotes == totalVotes && totalVotes > 0) {
                pointsToAward += 1
            }
            
            awardActive(pointsToAward)
            judgeWin = true
            points = pointsToAward
        } else {
            // Failure: -1 point penalty
            awardActive(-1)
            points = -1
        }
    }

    /**
     * Taboo Timer (Game 8) - HDRealRules.md compliant
     * Scoring:
     * - +2 per successful guess (within 60 seconds)
     * - -1 for each forbidden word spoken
     * - Bonus: +1 if team guesses 5+ words in one round
     */
    private fun resolveTabooTimer() {
        var totalPoints = 0
        
        // +2 per successful guess
        totalPoints += tabooSuccessfulGuesses * 2
        
        // -1 per forbidden word
        totalPoints -= tabooForbiddenWordCount
        
        // Bonus if 5+ words guessed
        if (tabooSuccessfulGuesses >= 5) {
            totalPoints += 1
        }
        
        if (totalPoints > 0) {
            awardActive(totalPoints)
            judgeWin = true
            points = totalPoints
        } else if (totalPoints < 0) {
            awardActive(totalPoints)
            points = totalPoints
        }
    }

    /**
     * The Unifying Theory (Game 9) - HDRealRules.md compliant
     * Scoring:
     * - +2 for most convincing/hilarious theory
     * - -1 for stating fact that only applies to two items
     */
    private fun resolveUnifyingTheory() {
        val successVotes = votesAB.values.count { it == "SUCCESS" || it == "A" }
        val failureVotes = votesAB.values.count { it == "FAILURE" || it == "B" }
        
        if (successVotes > failureVotes) {
            // Theory holds water: +2 points
            awardActive(2)
            judgeWin = true
            points = 2
        } else {
            // Theory failed (or only applies to 2 items): -1 penalty
            awardActive(-1)
            points = -1
        }
    }

    /**
     * Title Fight (Game 10) - HDRealRules.md compliant
     * Scoring:
     * - Winner: +1 Point
     * - Loser: -1 Point (and penalty/drink)
     */
    private fun resolveTitleFight() {
        val challengerId = titleFightChallengerId ?: return
        val winnerId = titleFightWinnerId ?: return
        val ap = activePlayer() ?: return
        
        // Winner gets +1
        addPoints(winnerId, 1)
        
        // Loser gets -1
        val loserId = if (winnerId == ap.id) challengerId else ap.id
        addPoints(loserId, -1)
        
        if (winnerId == ap.id) {
            judgeWin = true
            points = 1
        }
    }

    /**
     * Alibi Drop (Game 11) - HDRealRules.md compliant
     * Scoring:
     * - Innocent (Success): +2 Points (good story AND group failed to guess words)
     * - Guilty (Failure): -1 Point (bad story OR group caught words)
     */
    private fun resolveAlibiDrop() {
        val believable = alibiStoryBelievable ?: false
        val wordsDetectedCount = alibiWordsDetected.size
        val mandatoryWordsCount = alibiMandatoryWords.size
        
        // Success requires: believable story AND group didn't catch all mandatory words
        val innocent = believable && wordsDetectedCount < mandatoryWordsCount
        
        if (innocent) {
            // Innocent: +2 points
            awardActive(2)
            judgeWin = true
            points = 2
        } else {
            // Guilty: -1 penalty
            awardActive(-1)
            points = -1
        }
    }

    /**
     * Reality Check (Game 12) - HDRealRules.md compliant
     * Scoring:
     * - Self-Aware (Gap 0-1): +2 Points
     * - Delusional (Ego Higher): Roasted + Drink (0 points)
     * - The Fisher (Ego Lower): Booed + Drink (0 points)
     * - Critical Delusion (Gap 6+): Must finish drink
     */
    private fun resolveRealityCheck() {
        val egoRating = realityCheckEgoRating ?: return
        val groupRating = realityCheckGroupRating ?: return
        val gap = kotlin.math.abs(egoRating - groupRating)
        
        when {
            gap <= 1 -> {
                // Self-aware: +2 points
                awardActive(2)
                judgeWin = true
                points = 2
            }
            gap >= 6 -> {
                // Critical delusion: must finish drink, 0 points
                judgeWin = false
                points = 0
            }
            else -> {
                // Delusional or Fisher: roasted/booed, 0 points
                judgeWin = false
                points = 0
            }
        }
    }

    /**
     * Scatterblast (Game 13) - HDRealRules.md compliant
     * Scoring:
     * - The Casualty: Penalty (whoever's turn when bomb explodes)
     * - The Survivors: Safe (0 points change)
     */
    private fun resolveScatterblast() {
        val victimId = scatterblastVictimId
        if (victimId != null) {
            // Victim takes penalty (no points awarded, just marked for penalty)
            judgeWin = false
            points = 0
        }
    }

    fun goToFeedbackNoPoints() {
        judgeWin = false
        points = 0
        scene = Scene.FEEDBACK
    }

    fun commitDirectWin(pts: Int = Config.current.scoring.win) {
        awardActive(pts)
        judgeWin = false
        points = 0
        roundState = roundState?.copy(phase = RoundPhase.FEEDBACK)
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

    private fun saveSnapshot() {
        feedbackSnapshot = FeedbackSnapshot(lol, meh, trash)
    }

    fun feedbackLol() {
        saveSnapshot()
        lol++
        canUndoFeedback = true
    }

    fun feedbackMeh() {
        saveSnapshot()
        meh++
        canUndoFeedback = true
    }

    fun feedbackTrash() {
        saveSnapshot()
        trash++
        canUndoFeedback = true
    }

    /**
     * Marks current card as skipped and advances to next round.
     */
    fun skipCurrentCard() {
        viewModelScope.launch {
            currentImpressionId?.let { impId ->
                try {
                    repo.db.cardFeedback().markSkipped(impId)
                    com.helldeck.utils.Logger.d("Marked card as skipped: impressionId=$impId")
                } catch (e: Exception) {
                    com.helldeck.utils.Logger.e("Failed to mark card skipped", e)
                }
            }
            commitFeedbackAndNext()
        }
    }

    /**
     * Marks current card with a quick fire (🔥) reaction.
     */
    fun triggerQuickFire() {
        if (quickFireTriggered) return
        quickFireTriggered = true

        viewModelScope.launch {
            currentImpressionId?.let { impId ->
                try {
                    repo.db.cardFeedback().markQuickFire(impId)
                    com.helldeck.utils.Logger.d("Quick fire triggered: impressionId=$impId")
                } catch (e: Exception) {
                    com.helldeck.utils.Logger.e("Failed to mark quick fire", e)
                }
            }
        }
    }

    /**
     * Handle quick reaction from REVEAL phase.
     * This is the opt-in feedback system that maintains party flow.
     * 
     * @param reaction The quick reaction type (FIRE, STAR, THUMBS_DOWN)
     */
    fun handleQuickReaction(reaction: com.helldeck.ui.components.QuickReaction) {
        viewModelScope.launch {
            when (reaction) {
                com.helldeck.ui.components.QuickReaction.FIRE -> {
                    // Fire = LOL feedback
                    feedbackLol()
                    triggerQuickFire()
                    com.helldeck.utils.Logger.d("Quick reaction: FIRE (LOL)")
                }
                com.helldeck.ui.components.QuickReaction.STAR -> {
                    // Star = LOL + Save to favorites
                    feedbackLol()
                    toggleFavorite()
                    com.helldeck.utils.Logger.d("Quick reaction: STAR (LOL + Favorite)")
                }
                com.helldeck.ui.components.QuickReaction.THUMBS_DOWN -> {
                    // Thumbs down = TRASH feedback
                    feedbackTrash()
                    com.helldeck.utils.Logger.d("Quick reaction: THUMBS_DOWN (TRASH)")
                }
            }
            // Auto-advance to next round after reaction
            commitFeedbackAndNext()
        }
    }

    /**
     * Handle auto-advance when no reaction given (implicit MEH).
     * Called when the quick reaction timer expires without user input.
     */
    fun handleAutoAdvanceNoReaction() {
        viewModelScope.launch {
            // No reaction = implicit MEH (neutral feedback)
            feedbackMeh()
            com.helldeck.utils.Logger.d("Auto-advance: No reaction (implicit MEH)")
            commitFeedbackAndNext()
        }
    }

    /**
     * Gets list of unique card IDs shown in current session for MVP/Dud selection.
     */
    suspend fun getSessionCardsForVoting(): List<String> {
        return try {
            repo.db.cardFeedback().getSessionCardIds(feedbackSessionId)
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to get session cards", e)
            sessionCardIds.toList()
        }
    }

    /**
     * Marks a card as MVP for current session.
     */
    suspend fun markCardAsMvp(cardId: String) {
        try {
            repo.db.cardFeedback().markMvp(feedbackSessionId, cardId)
            com.helldeck.utils.Logger.d("Marked MVP: cardId=$cardId")
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to mark MVP", e)
        }
    }

    /**
     * Marks a card as Dud for current session.
     */
    suspend fun markCardAsDud(cardId: String) {
        try {
            repo.db.cardFeedback().markDud(feedbackSessionId, cardId)
            com.helldeck.utils.Logger.d("Marked Dud: cardId=$cardId")
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to mark Dud", e)
        }
    }

    // ========== END GAME SUMMARY ==========
    var showEndGameSummary by mutableStateOf(false)
        private set

    fun showEndGameSummary() {
        showEndGameSummary = true
    }

    fun dismissEndGameSummary() {
        showEndGameSummary = false
    }

    fun finishGameAndGoHome() {
        viewModelScope.launch {
            // Recompute scores for cards played this session
            sessionCardIds.forEach { cardId ->
                recomputeCardScore(cardId)
            }
        }

        showEndGameSummary = false
        sessionCardIds.clear()
        feedbackSessionId = System.currentTimeMillis()
        scene = Scene.HOME
    }

    /**
     * Recomputes quality score for a single card from its aggregate stats.
     */
    private suspend fun recomputeCardScore(cardId: String) {
        try {
            val stats = repo.db.cardFeedback().getAggregatedStats(cardId) ?: return
            val score = com.helldeck.data.CardScoreCalculator.toScoreEntity(stats)
            repo.db.cardFeedback().upsertScore(score)
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to recompute score for $cardId", e)
        }
    }

    /**
     * Recomputes quality scores for all tracked cards.
     */
    suspend fun recomputeAllCardScores() {
        try {
            val cardIds = repo.db.cardFeedback().getAllTrackedCardIds()
            for (cardId in cardIds) {
                recomputeCardScore(cardId)
            }
            com.helldeck.utils.Logger.d("Recomputed scores for ${cardIds.size} cards")
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to recompute card scores", e)
        }
    }

    /**
     * Gets cards that have fallen below quality threshold.
     */
    suspend fun getCardsNeedingReview(): List<com.helldeck.data.CardScoreEntity> {
        return try {
            repo.db.cardFeedback().getLowScoringCards(threshold = 0.2f)
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to get low scoring cards", e)
            emptyList()
        }
    }

    /**
     * Exports card quality data for manual review.
     */
    suspend fun exportCardQualityReport(): String {
        return try {
            val allScores = mutableListOf<com.helldeck.data.CardScoreEntity>()
            val gameIds = listOf(
                "roast_consensus", "poison_pitch", "red_flag_rally",
                "hot_seat_imposter", "scatterblast", "taboo_timer",
                "alibi_drop", "text_thread_trap", "over_under",
                "reality_check", "fill_in_finisher", "confession_or_cap",
                "title_fight", "the_unifying_theory",
            )

            for (gameId in gameIds) {
                allScores.addAll(repo.db.cardFeedback().getScoresByGame(gameId))
            }

            buildString {
                appendLine("=== HELLDECK Card Quality Report ===")
                appendLine("Generated: ${java.time.LocalDateTime.now()}")
                appendLine("Total tracked cards: ${allScores.size}")
                appendLine()

                if (allScores.isEmpty()) {
                    appendLine("No cards tracked yet.")
                    return@buildString
                }

                val avgScore = allScores.map { it.computedScore }.average()
                val lowScoring = allScores.filter { it.computedScore < 0.35f }
                val highScoring = allScores.filter { it.computedScore > 0.7f }

                appendLine("Average score: %.2f".format(avgScore))
                appendLine("High performers (>0.7): ${highScoring.size}")
                appendLine("Low performers (<0.35): ${lowScoring.size}")
                appendLine()

                appendLine("=== TOP 10 CARDS ===")
                allScores.sortedByDescending { it.computedScore }.take(10).forEach { score ->
                    appendLine("${score.cardId.take(40)} | ${score.gameId} | %.2f".format(score.computedScore))
                }
                appendLine()

                appendLine("=== BOTTOM 10 CARDS ===")
                allScores.sortedBy { it.computedScore }.take(10).forEach { score ->
                    appendLine("${score.cardId.take(40)} | ${score.gameId} | %.2f".format(score.computedScore))
                }
            }
        } catch (e: Exception) {
            "Error generating report: ${e.message}"
        }
    }

    /**
     * Undoes the last feedback rating
     */
    fun undoLastRating() {
        feedbackSnapshot?.let { snapshot ->
            lol = snapshot.lol
            meh = snapshot.meh
            trash = snapshot.trash
            canUndoFeedback = false
            feedbackSnapshot = null
            com.helldeck.utils.Logger.d("Undid last rating: LOL=$lol, MEH=$meh, TRASH=$trash")
        }
    }

    fun addComment(text: String, t: Set<String>) {
        if (text.isNotBlank()) {
            tags.add("note")
        }
        tags.addAll(t)
    }

    suspend fun commitFeedbackAndNext() {
        val card = roundState?.filledCard ?: return

        // Mark round as completed in feedback tracking
        currentImpressionId?.let { impId ->
            try {
                repo.db.cardFeedback().markRoundCompleted(impId)
            } catch (e: Exception) {
                com.helldeck.utils.Logger.e("Failed to mark round completed", e)
            }
        }

        val laughsScore = Rewards.fromCounts(lol, meh, trash)

        try {
            if (!isInitialized) {
                com.helldeck.utils.Logger.w("Skipping outcome recording: not initialized")
            } else {
                engine.recordOutcome(
                    templateId = card.id,
                    reward01 = laughsScore,
                )
                com.helldeck.utils.Logger.i(
                    "Recorded outcome for ${card.id}: reward=$laughsScore (LOL:$lol, MEH:$meh, TRASH:$trash)",
                )
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
                    points = points,
                )
            } catch (e: Exception) {
                com.helldeck.utils.Logger.e("Failed to record round metrics", e)
            }
        }

        // Track milestones
        totalRoundsThisSession++
        roundState?.gameId?.let { gamesPlayedThisSession.add(it) }

        // Update win streak
        if (judgeWin && points > 0) {
            consecutiveWins++
        } else {
            consecutiveWins = 0
        }

        // Check for milestone achievements
        checkMilestones(
            isFirstWin = judgeWin && activePlayer()?.wins == 0,
            isPerfectScore = lol > 0 && meh == 0 && trash == 0,
        )

        // Reset feedback state
        lol = 0
        meh = 0
        trash = 0
        tags.clear()
        judgeWin = false
        points = 0
        feedbackSnapshot = null
        canUndoFeedback = false
        quickFireTriggered = false
        currentImpressionId = null

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
        val card = roundState?.filledCard ?: return false
        val game = GameMetadata.getGameMetadata(roundState?.gameId ?: return false) ?: return false
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
                val seatNumber = player?.let { p -> activePlayers.indexOfFirst { it.id == p.id } + 1 }
                val favorite = com.helldeck.data.FavoriteCardEntity(
                    id = "fav_${System.currentTimeMillis()}_${card.id}",
                    cardId = card.id,
                    cardText = card.text,
                    gameId = game.id,
                    gameName = game.title,
                    sessionId = gameNightSessionId,
                    playerId = player?.id,
                    playerName = seatNumber?.let { "Seat $it" }, // Anonymized
                    lolCount = lol,
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
        val card = roundState?.filledCard ?: return false
        return try {
            repo.db.favorites().isFavorited(card.id, gameNightSessionId) != null
        } catch (e: Exception) {
            false
        }
    }

    // ========== MILESTONES ==========

    /**
     * Checks for milestone achievements and queues celebration
     */
    private fun checkMilestones(isFirstWin: Boolean, isPerfectScore: Boolean) {
        try {
            val sessionDuration = System.currentTimeMillis() - sessionStartTimeMs
            val favoritesCount = if (isInitialized) {
                viewModelScope.launch {
                    repo.db.favorites().getFavoriteCount()
                }
                0 // TODO: Get actual count
            } else {
                0
            }

            val milestones = com.helldeck.ui.components.MilestoneTracker.checkMilestones(
                totalRounds = totalRoundsThisSession,
                consecutiveWins = consecutiveWins,
                gamesPlayedSet = gamesPlayedThisSession,
                favoritesCount = favoritesCount,
                sessionDurationMs = sessionDuration,
                isFirstWin = isFirstWin,
                isPerfectScore = isPerfectScore,
            )

            // Show first milestone if any
            if (milestones.isNotEmpty() && pendingMilestone == null) {
                pendingMilestone = milestones.first()
                com.helldeck.utils.Logger.i("Milestone achieved: ${milestones.first().title}")
            }
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to check milestones", e)
        }
    }

    /**
     * Clears the pending milestone (after showing celebration)
     */
    fun clearPendingMilestone() {
        pendingMilestone = null
    }

    // ========== REPLAY ==========

    /**
     * Replays the last card without advancing
     */
    suspend fun replayLastCard() {
        // Store in local variables to enable smart cast
        val replayCard = lastCard
        val replayGameId = lastGameId

        if (replayCard == null || replayGameId == null) {
            com.helldeck.utils.Logger.w("No last card to replay")
            return
        }

        // Safety check: ensure we have required data
        if (!::engine.isInitialized) {
            com.helldeck.utils.Logger.e("Cannot replay: engine not initialized")
            return
        }

        // Reset scene to round with same card
        scene = Scene.ROUND
        val replayGameMeta = GameMetadata.getGameMetadata(replayGameId)

        // Create new round state with same card
        val playersList = activePlayers.mapIndexed { idx, _ -> "Seat ${idx + 1}" }
        val targetPlayerId = if (replayGameMeta?.interaction == Interaction.TARGET_PICK) {
            activePlayers.randomOrNull()?.id
        } else {
            null
        }

        try {
            roundState = RoundState(
                gameId = replayGameId,
                filledCard = replayCard,
                options = engine.getOptionsFor(
                    replayCard,
                    GameEngine.Request(
                        gameId = replayGameId,
                        sessionId = gameNightSessionId,
                        spiceMax = _spiceLevel.value,
                        players = playersList,
                    ),
                ),
                timerSec = replayGameMeta?.timerSec ?: 6,
                interactionType = replayGameMeta?.interactionType ?: InteractionType.JUDGE_PICK,
                activePlayerIndex = turnIdx.coerceAtMost(activePlayers.size - 1).coerceAtLeast(0),
                judgePlayerIndex = if (replayGameMeta?.interaction == Interaction.JUDGE_PICK && activePlayers.isNotEmpty()) {
                    (turnIdx + 1) % activePlayers.size
                } else {
                    null
                },
                targetPlayerIndex = if (replayGameMeta?.interaction == Interaction.TARGET_PICK && targetPlayerId != null) {
                    activePlayers.indexOfFirst { it.id == targetPlayerId }.takeIf { it >= 0 }
                } else {
                    null
                },
                phase = com.helldeck.ui.state.RoundPhase.INTRO,
                sessionId = gameNightSessionId,
            )
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to replay card", e)
            scene = Scene.HOME
        }

        t0 = System.currentTimeMillis()

        // Reset voting state
        preChoice = null
        votesAvatar = emptyMap()
        votesAB = emptyMap()

        com.helldeck.utils.Logger.i("Replaying last card: ${lastCard?.text}")
    }

    /**
     * Checks if replay is available
     */
    fun canReplay(): Boolean = lastCard != null && lastGameId != null

    // ========== CUSTOM CARDS ==========

    /**
     * Saves a custom card to the database
     */
    suspend fun saveCustomCard(gameId: String, cardText: String) {
        if (!isInitialized) return

        val player = activePlayer()
        val seatNumber = player?.let { p -> activePlayers.indexOfFirst { it.id == p.id } + 1 }
        val customCard = com.helldeck.data.CustomCardEntity(
            id = "custom_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}",
            gameId = gameId,
            cardText = cardText,
            createdBy = player?.id,
            creatorName = seatNumber?.let { "Seat $it" }, // Anonymized
            createdAtMs = System.currentTimeMillis(),
        )

        try {
            repo.db.customCards().insert(customCard)
            com.helldeck.utils.Logger.i("Saved custom card: ${customCard.id} for game $gameId")
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to save custom card", e)
            throw e
        }
    }

    /**
     * Gets all active custom cards
     */
    suspend fun getAllCustomCards(): List<com.helldeck.data.CustomCardEntity> {
        if (!isInitialized) return emptyList()

        return try {
            repo.db.customCards().getAllActiveCardsSnapshot()
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to load custom cards", e)
            emptyList()
        }
    }

    /**
     * Deletes a custom card
     */
    suspend fun deleteCustomCard(cardId: String) {
        if (!isInitialized) return

        try {
            repo.db.customCards().deleteById(cardId)
            com.helldeck.utils.Logger.i("Deleted custom card: $cardId")
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to delete custom card", e)
            throw e
        }
    }

    /**
     * Gets custom cards for a specific game
     */
    suspend fun getCustomCardsForGame(gameId: String): List<com.helldeck.data.CustomCardEntity> {
        if (!isInitialized) return emptyList()

        return try {
            repo.db.customCards().getCardsForGame(gameId)
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to load custom cards for game", e)
            emptyList()
        }
    }

    // ========== CONTENT REPORTING ==========

    /**
     * Reports offensive AI-generated content.
     * Per Google Play policy: apps with AI content must provide in-app reporting.
     */
    fun reportOffensiveContent(reason: ContentReport.ReportReason) {
        val currentRound = roundState ?: return
        val context = AppCtx.ctx

        val report = ContentReport(
            cardText = currentRound.filledCard.text,
            blueprintId = currentRound.filledCard.id,
            gameId = currentRound.gameId,
            reportReason = reason,
            sessionId = gameNightSessionId,
        )

        reportStore = reportStore.withReport(report)
        ContentReportStore.save(context, reportStore)

        com.helldeck.utils.Logger.i("Content reported: ${reason.name} for card: ${currentRound.filledCard.text}")
        
        // Update content filtering to avoid showing this card again
        viewModelScope.launch {
            try {
                ContentEngineProvider.reportOffensiveContent(currentRound.filledCard.id)
            } catch (e: Exception) {
                com.helldeck.utils.Logger.e("Failed to update content filter", e)
            }
        }

        showReportDialog = false
    }

    /**
     * Opens the report dialog
     */
    fun openReportDialog() {
        showReportDialog = true
    }

    /**
     * Closes the report dialog
     */
    fun closeReportDialog() {
        showReportDialog = false
    }

    /**
     * Gets the total number of reports submitted
     */
    fun getReportCount(): Int = reportStore.getReportCount()
}
