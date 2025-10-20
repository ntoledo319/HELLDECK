package com.helldeck.ui

import androidx.compose.animation.AnimatedContentTransitionScope

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.AppCtx
import com.helldeck.data.PlayerEntity
import com.helldeck.data.Repository
import com.helldeck.data.TemplateEntity
import com.helldeck.engine.*
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.random.Random
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.helldeck.data.computePlayerProfiles

/**
 * Scene enumeration for navigation
 */
enum class Scene {
    HOME, ROLLCALL, PLAYERS, ROUND, FEEDBACK, RULES, SCOREBOARD, STATS, SETTINGS, PROFILE, GAME_RULES
}

/**
 * Main HELLDECK app UI composable
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun HelldeckAppUI(
    vm: HelldeckVm = viewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        Config.load()
        com.helldeck.utils.Logger.i("HelldeckAppUI: Initializing ViewModel")
        vm.initOnce()
        com.helldeck.utils.Logger.i("HelldeckAppUI: ViewModel initialized")
    }

    BackHandler(enabled = vm.scene != Scene.HOME) {
        vm.goBack()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
            if (vm.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Background pattern for visual interest
                HelldeckBackgroundPattern(
                    pattern = BackgroundPattern.CIRCUIT,
                    opacity = 0.03f
                )

                // Scene transitions with smooth animations
                com.helldeck.utils.Logger.i("HelldeckAppUI: Current scene: ${vm.scene}")
    AnimatedContent(
        targetState = vm.scene,
                    transitionSpec = {
                    // Fade and slide transitions between scenes
                    androidx.compose.animation.fadeIn(
                        animationSpec = tween(HelldeckAnimations.Normal)
                    ) + androidx.compose.animation.slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetX = { it / 2 }
                    ) togetherWith androidx.compose.animation.fadeOut(
                        animationSpec = tween(HelldeckAnimations.Normal / 2)
                    ) + androidx.compose.animation.slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        ),
                        targetOffsetX = { -it / 2 }
                    )
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
                    Scene.GAME_RULES -> com.helldeck.ui.GameRulesScene(vm = vm, onClose = { vm.goBack() })
                }
            }
        }
    }
}

/**
 * ViewModel for HELLDECK game state
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
    var players by mutableStateOf(listOf<PlayerEntity>())
    var activePlayers by mutableStateOf(listOf<PlayerEntity>())
    private var turnIdx by mutableStateOf(0)
    private var starterPicked = false

    // Rollcall / attendance
    private var didRollcall = false
    private var askRollcallOnLaunch = true

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

    // Core systems
    private var repo: Repository? = null
    private var templateEngine: TemplateEngine? = null
    private var engine: GameEngine? = null

    /**
     * Initialize systems on first use
     */
    suspend fun initOnce() {
        if (repo != null) return
        isLoading = true

        val context = AppCtx.ctx
        repo = Repository.get(context)
        try {
            templateEngine = TemplateEngine(context)
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to initialize TemplateEngine", e)
        }
        if (templateEngine == null) {
            com.helldeck.utils.Logger.e("TemplateEngine is null, cannot proceed")
            isLoading = false
            return
        }
        engine = GameEngine(context, repo!!, templateEngine!!)
        // Initialize game engine
        engine?.initialize()

        // Load initial data
        reloadPlayers()

        // Determine if we should ask for rollcall on launch
        runCatching {
            askRollcallOnLaunch = repo!!.db.settings().getBoolean("rollcall_on_launch", true)
        }
        if (askRollcallOnLaunch && !didRollcall && players.isNotEmpty()) {
            scene = Scene.ROLLCALL
        }
        isLoading = false
    }

    /**
     * Reload players from database
     */
    suspend fun reloadPlayers() {
        repo?.let {
            players = it.db.players().getAllPlayers().first()
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
                    it.db.players().upsert(PlayerEntity(
                        id = id,
                        name = name,
                        avatar = avatar,
                        sessionPoints = 0
                    ))
                }

                players = it.db.players().getAllPlayers().first()
                activePlayers = players.filter { p -> p.afk == 0 }
            }
        }
    }

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
    }

    fun markRollcallDone() {
        didRollcall = true
    }

    /**
     * Toggle scoreboard visibility
     */
    fun toggleScores() {
        showScores = !showScores
    }

    /**
     * Navigate to players scene
     */
    fun goPlayers() {
        scene = Scene.PLAYERS
    }

    /**
     * Start a new round
     */
    suspend fun startRound(gameId: String? = null) {
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
        currentGame = GameRegistry.getGameById(nextGame)

        // Pick starter if not already picked
        if (!starterPicked) {
            turnIdx = if (activePlayers.isNotEmpty()) Random.nextInt(activePlayers.size) else 0
            starterPicked = true
        }

        // Generate card
        engine?.let {
            currentCard = it.nextFilledCard(nextGame)
        }
        t0 = System.currentTimeMillis()

        // Reset voting state
        preChoice = null
        votesAvatar = emptyMap()
        votesAB = emptyMap()
    }

    /**
     * Pick next game based on mechanics
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
                GameRegistry.getAllGameIds().random()
            }
        } else {
            GameRegistry.getAllGameIds().random()
        }
    }

    /**
     * Get IDs of players in last place
     */
    private fun getLastPlaceIds(): List<String> {
        if (players.isEmpty()) return emptyList()

        val minPts = players.minOf { it.sessionPoints }
        return players.filter { it.sessionPoints == minPts }.map { it.id }
    }

    /**
     * Advance to next player's turn
     */
    fun endRoundAdvanceTurn() {
        val poolSize = activePlayers.size.coerceAtLeast(1)
        turnIdx = (turnIdx + 1) % poolSize
    }

    /**
     * Get current active player
     */
    fun activePlayer(): PlayerEntity? {
        return if (activePlayers.isEmpty()) null else activePlayers[turnIdx % activePlayers.size]
    }

    /**
     * Handle pre-choice selection (for games that need it)
     */
    fun onPreChoice(choice: String) {
        preChoice = choice
    }

    /**
     * Handle avatar vote
     */
    fun onAvatarVote(voterId: String, targetId: String) {
        votesAvatar = votesAvatar + (voterId to targetId)
    }

    /**
     * Handle A/B vote
     */
    fun onABVote(voterId: String, choice: String) {
        votesAB = votesAB + (voterId to choice)
    }

    /**
     * Resolve current interaction and move to feedback
     */
    fun resolveInteraction() {
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
                // Default resolution
                judgeWin = true
                points = Config.current.scoring.win
                awardActive(points)
            }
        }

        phase = RoundPhase.FEEDBACK
        scene = Scene.FEEDBACK
    }

    /**
     * Resolve roast consensus voting
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
     * Resolve confession/cap voting
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
     * Resolve A/B voting
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

    /**
     * Award points to active player
     */
    private fun awardActive(pts: Int) {
        activePlayer()?.let { addPoints(it.id, pts) }
    }

    /**
     * Add points to player
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
        viewModelScope.launch {
            try {
                repo?.db?.players()?.addPointsToPlayer(playerId, delta)
            } catch (_: Exception) {}
        }
    }

    /**
     * Handle LOL feedback
     */
    fun feedbackLol() {
        lol++
    }

    /**
     * Handle MEH feedback
     */
    fun feedbackMeh() {
        meh++
    }

    /**
     * Handle TRASH feedback
     */
    fun feedbackTrash() {
        trash++
    }

    /**
     * Add comment with tags
     */
    fun addComment(text: String, t: Set<String>) {
        if (text.isNotBlank()) {
            tags.add("note")
        }
        tags.addAll(t)
    }

    /**
     * Commit feedback and advance to next round
     */
    suspend fun commitFeedbackAndNext() {
        val card = currentCard ?: return
        val latency = (System.currentTimeMillis() - t0).toInt()

        engine?.commitRound(
            card = card,
            feedback = Feedback(lol, meh, trash, latency, tags),
            judgeWin = judgeWin,
            points = points,
            latencyMs = latency,
            notes = null,
            activePlayerId = activePlayer()?.id
        )

        // Reset feedback state
        lol = 0
        meh = 0
        trash = 0
        tags.clear()
        judgeWin = false
        points = 0

        // Persist long-term player stats
        activePlayer()?.id?.let { pid ->
            repo?.let { r ->
                r.db.players().incGamesPlayed(pid)
                if (judgeWin) r.db.players().addWins(pid, 1)
                if (points != 0) r.db.players().addTotalPoints(pid, points)
            }
        }

        // Advance turn and start next round
        endRoundAdvanceTurn()
        startRound()
    }

    suspend fun getGameStats(): Map<String, Any?> {
        return try {
            engine?.getGameStats() ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

/**
 * Home scene - main menu
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScene(vm: HelldeckVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val repo = remember { Repository.get(AppCtx.ctx) }

    // Launcher for importing brainpack
    val pickImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                Repository.get(ctx).importBrainpack(uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HELLDECK") },
                actions = {
                    IconButton(onClick = { vm.navigateTo(Scene.ROLLCALL) }) {
                        Icon(Icons.Rounded.HowToReg, contentDescription = "Rollcall")
                    }
                    IconButton(onClick = { vm.toggleScores() }) {
                        Icon(Icons.Rounded.Leaderboard, contentDescription = "Scores")
                    }
                    IconButton(onClick = { vm.navigateTo(Scene.STATS) }) {
                        Icon(Icons.Rounded.Insights, contentDescription = "Stats")
                    }
                    IconButton(onClick = { vm.navigateTo(Scene.RULES) }) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "Rules & How-To")
                    }
                    IconButton(onClick = { vm.navigateTo(Scene.SETTINGS) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main title card
        AnimatedCardFace(
            title = "Single phone. One card per round.",
            subtitle = "Long-press to draw • two-finger = back",
            delayMs = 200
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Quick access: Rules and Settings
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { vm.navigateTo(Scene.ROLLCALL) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.HowToReg, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Rollcall")
            }
            OutlinedButton(
                onClick = { vm.navigateTo(Scene.RULES) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Rules")
            }
            OutlinedButton(
                onClick = { vm.navigateTo(Scene.SETTINGS) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Settings")
            }
        }

        // Removed random start; select a game below.

        // Games grid
        Text(
            text = "Games",
            style = MaterialTheme.typography.titleMedium,
            color = HelldeckColors.LightGray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.helldeck.engine.Games.forEach { game ->
                GameTile(
                    title = game.title,
                    subtitle = game.description,
                    icon = gameIconFor(game.id),
                    onClick = { scope.launch { vm.startRound(game.id) } }
                )
            }
        }

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                    val uri = com.helldeck.engine.ExportImport.exportBrainpack(ctx)
                    val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(android.content.Intent.createChooser(share, "Share Brainpack"))
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Yellow
                )
            ) {
                Text(text = "Export Brain")
            }

            Button(
                onClick = { pickImport.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Orange
                )
            ) {
                Text(text = "Import Brain")
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Heat threshold slider
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        Text(
            text = "Heat threshold: ${(vm.heatThreshold * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = HelldeckColors.LightGray
        )
        Slider(
            value = vm.heatThreshold,
            onValueChange = { vm.heatThreshold = it.coerceIn(0.5f, 0.8f) },
            valueRange = 0.5f..0.8f,
            steps = 5,
            onValueChangeFinished = {
                Config.setRoomHeatThreshold(vm.heatThreshold.toDouble())
                vm.spicy = vm.heatThreshold >= 0.70f
                Config.spicyMode = vm.spicy
                scope.launch { repo.db.settings().putFloat("room_heat_threshold", vm.heatThreshold) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Scoreboard overlay
        if (vm.showScores) {
            ScoreboardOverlay(vm.players) { vm.toggleScores() }
        }
    }
    }
}

/**
 * Rollcall / Attendance scene
 * Select which players are present at the start of a session
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RollcallScene(vm: HelldeckVm) {
    val repo = remember { Repository.get(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var present by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(vm.players) {
        present = vm.players.filter { it.afk == 0 }.map { it.id }.toSet()
    }

    var name by remember { mutableStateOf("") }
    val emojis = listOf("😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺")
    var emoji by remember { mutableStateOf(emojis.random()) }
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Who's here?") },
                navigationIcon = { TextButton(onClick = { vm.goHome() }) { Text("Skip") } },
                actions = {
                    TextButton(onClick = { present = vm.players.map { it.id }.toSet() }) { Text("All") }
                    TextButton(onClick = { present = emptySet() }) { Text("None") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick add attendee
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Add player name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { showPicker = true }) { Text(emoji) }
                if (showPicker) {
                    EmojiPicker(show = showPicker, onDismiss = { showPicker = false }) { picked ->
                        emoji = picked
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val id = "p${Random.nextInt(100000)}"
                            scope.launch {
                                repo.db.players().upsert(
                                    PlayerEntity(id = id, name = name.trim(), avatar = emoji, sessionPoints = 0, afk = 0)
                                )
                                present = present + id
                                vm.reloadPlayers()
                                name = ""
                                emoji = emojis.random()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
                ) { Text("Add") }
            }

            // List of known players
            Text("Tap to mark present", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(vm.players, key = { it.id }) { p ->
                    val dismissState = rememberDismissState(confirmStateChange = { value ->
                        if (value == DismissValue.DismissedToStart) {
                            true
                        } else false
                    })

                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (dismissState.currentValue == DismissValue.DismissedToStart && !showDeleteConfirm) {
                        showDeleteConfirm = true
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete player?") },
                            text = { Text("This will remove ${p.name}. You can Undo immediately after.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    scope.launch {
                                        repo.db.players().delete(p)
                                        present = present - p.id
                                        vm.reloadPlayers()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted ${p.name}",
                                            actionLabel = "Undo",
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            repo.db.players().upsert(p)
                                            vm.reloadPlayers()
                                        }
                                    }
                                }) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                            }
                        )
                    }

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 12.dp)
                                    .background(Color(0xFFB00020)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Delete", color = Color.White)
                            }
                        },
                        dismissContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = p.avatar, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(8.dp))
                                    var editName by remember { mutableStateOf(false) }
                                    var tempName by remember { mutableStateOf(p.name) }
                                    if (editName) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = tempName,
                                                onValueChange = { tempName = it },
                                                singleLine = true,
                                                modifier = Modifier.widthIn(min = 120.dp, max = 240.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            TextButton(onClick = {
                                                val newName = tempName.trim()
                                                if (newName.isNotEmpty() && newName != p.name) {
                                                    scope.launch {
                                                        repo.db.players().update(p.copy(name = newName))
                                                        vm.reloadPlayers()
                                                        snackbarHostState.showSnackbar("Renamed to $newName")
                                                    }
                                                }
                                                editName = false
                                            }) { Text("Save") }
                                            TextButton(onClick = { editName = false; tempName = p.name }) { Text("Cancel") }
                                        }
                                    } else {
                                        Text(
                                            text = p.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .clickable { editName = true }
                                                .widthIn(max = 220.dp)
                                        )
                                    }
                                }
                                Switch(
                                    checked = present.contains(p.id),
                                    onCheckedChange = { checked ->
                                        present = if (checked) present + p.id else present - p.id
                                    }
                                )
                            }
                        }
                    )
                }
            }

            // Start session
            Button(
                enabled = present.size >= 2,
                onClick = {
                    scope.launch {
                        vm.players.forEach { p ->
                            val newAfk = if (present.contains(p.id)) 0 else 1
                            if (p.afk != newAfk) {
                                repo.db.players().update(p.copy(afk = newAfk))
                            }
                        }
                        vm.reloadPlayers()
                        vm.markRollcallDone()
                        vm.goHome()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Yellow)
            ) {
                Text("Start Session (${present.size} present)")
            }
        }
    }
}

/**
 * Players management scene
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayersScene(vm: HelldeckVm) {
    val repo = remember { Repository.get(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var name by remember { mutableStateOf("") }
    val emojis = listOf("😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺")
    var emoji by remember { mutableStateOf(emojis.random()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Players") },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
        ) {
            // Add player section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text("Name or emoji + name (e.g., 🦊 Pip)")
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HelldeckColors.Yellow,
                        unfocusedBorderColor = HelldeckColors.LightGray,
                        focusedTextColor = HelldeckColors.White,
                        unfocusedTextColor = HelldeckColors.White
                    )
                )

                Spacer(modifier = Modifier.width(HelldeckSpacing.Small.dp))

                var showPicker by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showPicker = true }) { Text(text = emoji) }
                if (showPicker) {
                    EmojiPicker(show = showPicker, onDismiss = { showPicker = false }) { picked ->
                        emoji = picked
                    }
                }
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            val id = "p${Random.nextInt(100000)}"
                            repo.db.players().upsert(PlayerEntity(
                                id = id,
                                name = name,
                                avatar = emoji,
                                sessionPoints = 0
                            ))
                            vm.reloadPlayers()
                            name = ""
                            emoji = emojis.random()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Green
                )
            ) {
                Text(text = "Add Player")
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            // Player list
            LazyColumn {
                items(vm.players, key = { it.id }) { player ->
                    val dismissState = rememberDismissState(confirmStateChange = { value ->
                        if (value == DismissValue.DismissedToStart) {
                            // Ask for confirmation; do not auto-delete
                            true // allow swipe visual, but we'll show dialog and recompose content
                        } else false
                    })

                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (dismissState.currentValue == DismissValue.DismissedToStart && !showDeleteConfirm) {
                        // Trigger confirmation once per swipe
                        showDeleteConfirm = true
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete player?") },
                            text = { Text("This will remove ${player.name}. You can still Undo right after.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    // Perform delete with undo snackbar
                                    scope.launch {
                                        repo.db.players().delete(player)
                                        vm.reloadPlayers()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted ${player.name}",
                                            actionLabel = "Undo",
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            repo.db.players().upsert(player)
                                            vm.reloadPlayers()
                                        }
                                    }
                                }) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                            }
                        )
                    }

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .padding(horizontal = 12.dp)
                                    .background(Color(0xFFB00020)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Delete", color = Color.White)
                            }
                        },
                        dismissContent = {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { vm.openProfile(player.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(HelldeckSpacing.Medium.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        var showEditPicker by remember { mutableStateOf(false) }
                                        Text(
                                            text = player.avatar,
                                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp),
                                            modifier = Modifier.clickable { showEditPicker = true }
                                        )
                                        if (showEditPicker) {
                                            EmojiPicker(
                                                show = true,
                                                onDismiss = { showEditPicker = false },
                                                onPick = { picked ->
                                                    scope.launch {
                                                        repo.db.players().update(player.copy(avatar = picked))
                                                        vm.reloadPlayers()
                                                        snackbarHostState.showSnackbar("Updated avatar for ${player.name}")
                                                    }
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(HelldeckSpacing.Small.dp))
                                        var editName by remember { mutableStateOf(false) }
                                        var tempName by remember { mutableStateOf(player.name) }
                                        if (editName) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = tempName,
                                                    onValueChange = { tempName = it },
                                                    singleLine = true,
                                                    modifier = Modifier.widthIn(min = 140.dp, max = 240.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                TextButton(onClick = {
                                                    val newName = tempName.trim()
                                                    if (newName.isNotEmpty() && newName != player.name) {
                                                        scope.launch {
                                                            repo.db.players().update(player.copy(name = newName))
                                                            vm.reloadPlayers()
                                                            snackbarHostState.showSnackbar("Renamed to $newName")
                                                        }
                                                    }
                                                    editName = false
                                                }) { Text("Save") }
                                                TextButton(onClick = { editName = false; tempName = player.name }) { Text("Cancel") }
                                            }
                                        } else {
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .clickable { editName = true }
                                                .widthIn(max = 220.dp)
                                        )
                                    }
                                    }

                                    Row {
                                        TextButton(onClick = { vm.openProfile(player.id) }) { Text("Profile") }
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    repo.db.players().update(
                                                        player.copy(afk = if (player.afk == 0) 1 else 0)
                                                    )
                                                    vm.reloadPlayers()
                                                }
                                            }
                                        ) {
                                            Text(if (player.afk == 0) "AFK" else "Back")
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tip: 3–10 players best. 11–16 = teams (1 vote per team).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Game round scene
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun RoundScene(vm: HelldeckVm) {
    val card = vm.currentCard ?: return
    val game = vm.currentGame ?: return
    val context = LocalContext.current

    // Respect haptics setting
    val repo = remember { Repository.get(AppCtx.ctx) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        hapticsEnabled = repo.db.settings().getBoolean("haptics_enabled", true)
    }

    LaunchedEffect(card.templateId) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_START, useHaptics = hapticsEnabled)
    }

    val totalTimeMs = remember(card.templateId) { Config.getTimerForInteraction(game.interaction) }
    var timeRemaining by remember(card.templateId) { mutableIntStateOf(totalTimeMs) }
    var lastStage by remember(card.templateId) { mutableIntStateOf(3) }
    val autoResolveOnTimeUp = remember(card.templateId) {
        when (game.interaction) {
            Interaction.AB_VOTE, Interaction.TRUE_FALSE, Interaction.VOTE_AVATAR -> true
            else -> false
        }
    }
    var timerRunning by remember(card.templateId) {
        mutableStateOf(game.interaction != Interaction.TABOO_CLUE && totalTimeMs > 0)
    }
    var timeUp by remember(card.templateId) { mutableStateOf(false) }

    LaunchedEffect(totalTimeMs, card.templateId, timerRunning) {
        if (totalTimeMs > 0 && timerRunning) {
            while (timeRemaining > 0 && timerRunning) {
                kotlinx.coroutines.delay(1000)
                timeRemaining = (timeRemaining - 1000).coerceAtLeast(0)
                val progress = timeRemaining.toFloat() / totalTimeMs.toFloat()
                val stage = when {
                    progress < 0.1f -> 0
                    progress < 0.3f -> 1
                    else -> 2
                }
                if (stage != lastStage) {
                    GameFeedback.triggerTimerFeedback(context, timeRemaining, totalTimeMs, useHaptics = hapticsEnabled)
                    lastStage = stage
                }
            }
            if (timeRemaining <= 0) {
                timeUp = true
                if (autoResolveOnTimeUp) {
                    vm.resolveInteraction()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game.title) },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = {
                    TextButton(onClick = { vm.openRulesForCurrentGame() }) { Text("Help") }
                    TextButton(onClick = { vm.goHome() }) { Text("Home") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.openRulesForCurrentGame() },
                containerColor = HelldeckColors.Yellow
            ) { Text("Help") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (totalTimeMs > 0) {
                GameTimer(
                    timeRemainingMs = timeRemaining,
                    totalTimeMs = totalTimeMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HelldeckSpacing.Medium.dp)
                )
            }
            // Game card
            CardFace(
                title = card.text,
                subtitle = "(${game.title})",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(HelldeckSpacing.Medium.dp)
            )

            // Game-specific interaction area
        when (game.interaction) {
        Interaction.VOTE_AVATAR -> AvatarVoteFlow(
            players = vm.activePlayers,
            onVote = vm::onAvatarVote,
            onDone = { vm.resolveInteraction() },
            onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
        )
        Interaction.AB_VOTE -> ABVoteFlow(
            players = vm.activePlayers,
            preChoiceLabel = "Active picks A/B before votes",
            preChoices = listOf("A", "B"),
            preChoice = vm.preChoice,
            onPreChoice = vm::onPreChoice,
            leftLabel = vm.currentCard?.options?.getOrNull(0) ?: "A",
            rightLabel = vm.currentCard?.options?.getOrNull(1) ?: "B",
            onVote = vm::onABVote,
            onDone = { vm.resolveInteraction() },
            onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
        )
            Interaction.TRUE_FALSE -> ABVoteFlow(
                players = vm.activePlayers,
                preChoiceLabel = "Speaker sets Truth or Bluff",
                preChoices = listOf("TRUTH", "BLUFF"),
                preChoice = vm.preChoice,
                onPreChoice = vm::onPreChoice,
                leftLabel = "T",
                rightLabel = "F",
                onVote = vm::onABVote,
                onDone = { vm.resolveInteraction() },
                onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
            )
            Interaction.SMASH_PASS -> ABVoteFlow(
                players = vm.activePlayers,
                preChoiceLabel = "",
                preChoices = emptyList(),
                preChoice = null,
                onPreChoice = {},
                leftLabel = vm.currentCard?.options?.getOrNull(0) ?: "SMASH",
                rightLabel = vm.currentCard?.options?.getOrNull(1) ?: "PASS",
                onVote = vm::onABVote,
                onDone = { vm.resolveInteraction() },
                onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
            )
            Interaction.TARGET_PICK -> SingleAvatarPickFlow(
                players = vm.activePlayers,
                onPick = { _ -> vm.goToFeedbackNoPoints() },
                onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
            )
            Interaction.DUEL -> OptionsPickFlow(
                title = "Who won the duel?",
                options = listOf("Active Player wins", "Other wins"),
                onPick = { choice ->
                    if (choice.startsWith("Active")) vm.commitDirectWin() else vm.goToFeedbackNoPoints()
                }
            )
            Interaction.PITCH -> OptionsPickFlow(
                title = "Done pitching?",
                options = listOf("Lock"),
                onPick = { vm.goToFeedbackNoPoints() }
            )
            Interaction.SPEED_LIST -> OptionsPickFlow(
                title = "Time's up?",
                options = listOf("Lock"),
                onPick = { vm.goToFeedbackNoPoints() }
            )
            Interaction.REPLY_TONE -> OptionsPickFlow(
                title = "Pick a reply vibe",
                options = card.options.ifEmpty { listOf("Deadpan", "Feral", "Chaotic", "Wholesome") },
                onPick = { vm.resolveInteraction() }
            )
            Interaction.TABOO_CLUE -> TabooFlow(
                clue = card.text,
                taboos = card.options,
                running = timerRunning,
                onStart = {
                    timeRemaining = totalTimeMs
                    timeUp = false
                    timerRunning = true
                },
                onDone = { vm.resolveInteraction() }
            )
            Interaction.ODD_REASON -> OptionsPickFlow(
                title = "Pick the misfit",
                options = card.options.ifEmpty { listOf("Option 1", "Option 2", "Option 3") },
                onPick = { vm.resolveInteraction() }
            )
            Interaction.JUDGE_PICK -> JudgePickFlow(
                judge = vm.players.getOrNull((vm.players.indexOf(vm.activePlayer()) + 1) % vm.players.size),
                options = vm.currentCard?.options?.ifEmpty { listOf("Option 1", "Option 2") } ?: listOf("Option 1", "Option 2"),
                onPick = { vm.resolveInteraction() }
            )
            else -> {
                // Default interaction
                BigZones(
                    onLeft = { /* Left action */ },
                    onCenter = { vm.resolveInteraction() },
                    onRight = { /* Right action */ },
                    onLong = { /* Long press action */ }
                )
            }
        }
        }
    }
}

/**
 * Feedback collection scene
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun FeedbackScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repo = remember { Repository.get(AppCtx.ctx) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { hapticsEnabled = repo.db.settings().getBoolean("haptics_enabled", true) }
    LaunchedEffect(vm.currentCard?.templateId) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_END, useHaptics = hapticsEnabled)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback") },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = { TextButton(onClick = { vm.goHome() }) { Text("Home") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Feedback prompt
            CardFace(
                title = "Rate that card",
                subtitle = "😂 ≥60% = +1 heat bonus • 🚮 ≥60% = −2",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(HelldeckSpacing.Medium.dp)
            )

            // Feedback controls
            FeedbackStrip(
                onLol = { vm.feedbackLol() },
                onMeh = { vm.feedbackMeh() },
                onTrash = { vm.feedbackTrash() },
                onComment = { text, tags -> vm.addComment(text, tags) },
                showComments = true
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            // Next button
            Button(
                onClick = { scope.launch { vm.commitFeedbackAndNext() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HelldeckHeights.Button.dp)
                    .padding(horizontal = HelldeckSpacing.Large.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Green
                )
            ) {
                Text(text = "Next Round")
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScene(onClose: () -> Unit, vm: HelldeckVm = viewModel()) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var players by remember { mutableStateOf(0) }
    var templates by remember { mutableStateOf(0) }
    var profiles by remember { mutableStateOf<List<com.helldeck.data.PlayerProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        stats = vm.getGameStats()
        val repo = Repository.get(AppCtx.ctx)
        players = repo.db.players().getTotalPlayerCount()
        templates = repo.db.templates().getTotalCount()
        profiles = repo.computePlayerProfiles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                actions = {
                    TextButton(onClick = onClose) { Text("Close") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
                .fillMaxSize()
        ) {
            Text("Players: $players", style = MaterialTheme.typography.titleMedium)
            Text("Templates: $templates", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            Text("Recent Game Stats", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            SimpleStatRow("Total Rounds", (stats["totalRounds"] ?: 0).toString())
            SimpleStatRow("Avg Template Score", (stats["averageScore"] ?: 0.0).toString())
            SimpleStatRow("Most Played Game", (stats["mostPlayedGame"] ?: "—").toString())
            SimpleStatRow("Top Template", (stats["highestScoringTemplate"] ?: "—").toString())

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))
            Text("Player Profiles", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(profiles) { pr ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { vm.openProfile(pr.id) }
                    ) {
                        Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${pr.avatar} ${pr.name}", style = MaterialTheme.typography.titleMedium)
                                Text("${pr.totalPoints} pts", color = HelldeckColors.Yellow)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            SimpleStatRow("Wins", pr.wins.toString())
                            SimpleStatRow("Games", pr.gamesPlayed.toString())
                            SimpleStatRow("Heat Rounds", pr.heatRounds.toString())
                            SimpleStatRow("Quick Laughs", pr.quickLaughs.toString())
                            SimpleStatRow("Avg 😂", "${"%.2f".format(pr.avgLol)}")
                            SimpleStatRow("Avg 🚮", "${"%.2f".format(pr.avgTrash)}")
                            if (pr.awards.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    pr.awards.forEach { a -> AssistChip(a) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = HelldeckColors.Yellow)
    }
}

/**
 * Avatar voting flow
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun AvatarVoteFlow(
    players: List<PlayerEntity>,
    onVote: (voterId: String, targetId: String) -> Unit,
    onDone: () -> Unit,
    onManagePlayers: (() -> Unit)? = null
) {
    var idx by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }

    if (players.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No active players. Enable players in Settings.")
            Spacer(modifier = Modifier.height(8.dp))
            onManagePlayers?.let {
                OutlinedButton(onClick = it) { Text("Open Settings") }
            }
        }
        return
    }

    val voter = players[idx]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voter: ${voter.name}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Player grid
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 3
        ) {
            players.forEach { player ->
                VoteButton(
                    playerName = player.name,
                    playerAvatar = player.avatar,
                    isSelected = chosen == player.id,
                    onClick = { chosen = player.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    if (idx < players.lastIndex) {
                        idx++
                    } else {
                        onDone()
                    }
                    chosen = null
                }
            ) {
                Text("Skip")
            }

            Button(
                enabled = chosen != null,
                onClick = {
                    chosen?.let { targetId ->
                        onVote(voter.id, targetId)
                    }

                    if (idx < players.lastIndex) {
                        idx++
                    } else {
                        onDone()
                    }
                    chosen = null
                }
            ) {
                Text(if (idx < players.lastIndex) "Lock & Next" else "Finish Voting")
            }
        }
    }
}

/** Single pick of a target avatar (no iteration) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleAvatarPickFlow(
    players: List<PlayerEntity>,
    onPick: (targetId: String) -> Unit,
    onManagePlayers: (() -> Unit)? = null
) {
    var chosen by remember { mutableStateOf<String?>(null) }

    if (players.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No active players. Enable players in Settings.")
            Spacer(modifier = Modifier.height(8.dp))
            onManagePlayers?.let {
                OutlinedButton(onClick = it) { Text("Open Settings") }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pick a target", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 3
        ) {
            players.forEach { player ->
                VoteButton(
                    playerName = player.name,
                    playerAvatar = player.avatar,
                    isSelected = chosen == player.id,
                    onClick = { chosen = player.id }
                )
            }
        }
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        Button(
            enabled = chosen != null,
            onClick = { chosen?.let { onPick(it) } },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
        ) { Text("Lock") }
    }
}

/** Simple options picker used by several interactions */
@Composable
fun OptionsPickFlow(
    title: String,
    options: List<String>,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        options.forEach { opt ->
            Button(
                onClick = { onPick(opt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Orange)
            ) { Text(opt) }
        }
    }
}

/** Taboo clue view with forbidden list */
@Composable
fun TabooFlow(
    clue: String,
    taboos: List<String>,
    running: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Clue: $clue", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            taboos.take(3).forEach { word ->
                AssistChip(word)
            }
        }
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        if (!running) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Yellow)
            ) { Text("Start Timer") }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
            ) { Text("Lock") }
        }
    }
}

@Composable
private fun AssistChip(text: String) {
    Surface(
        color = HelldeckColors.MediumGray,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(HelldeckRadius.Medium)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = HelldeckColors.Yellow,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScene(onClose: () -> Unit, vm: HelldeckVm) {
    val repo = remember { Repository.get(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    var learningEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var heat by remember { mutableStateOf(Config.roomHeatThreshold().toFloat()) }
    var soundEnabled by remember { mutableStateOf(true) }
    var rollcallOnLaunch by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        learningEnabled = repo.db.settings().getBoolean("learning_enabled", true)
        hapticsEnabled = repo.db.settings().getBoolean("haptics_enabled", true)
        soundEnabled = repo.db.settings().getBoolean("sound_enabled", true)
        heat = repo.db.settings().getFloat("room_heat_threshold", Config.roomHeatThreshold().toFloat())
        rollcallOnLaunch = repo.db.settings().getBoolean("rollcall_on_launch", true)
        Config.setLearningEnabled(learningEnabled)
        Config.setHapticsEnabled(hapticsEnabled)
        Config.setRoomHeatThreshold(heat.toDouble())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = { TextButton(onClick = onClose) { Text("Close") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Players section
            Text("Players", style = MaterialTheme.typography.titleLarge)

            var newName by remember { mutableStateOf("") }
            val emojis = listOf("😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺")
            var newEmoji by remember { mutableStateOf(emojis.random()) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Add player name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { newEmoji = emojis.random() },
                    colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Orange)
                ) { Text(newEmoji) }
            }

            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        val id = "p${Random.nextInt(100000)}"
                        scope.launch {
                            repo.db.players().upsert(
                                PlayerEntity(id = id, name = newName, avatar = newEmoji, sessionPoints = 0)
                            )
                            vm.reloadPlayers()
                            newName = ""
                            newEmoji = emojis.random()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
            ) { Text("Add Player") }

            // Active toggles
            if (vm.players.isEmpty()) {
                Text("No players yet. Add a few above or Manage.")
            } else {
                vm.players.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${p.avatar} ${p.name}",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Switch(
                            checked = p.afk == 0,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    repo.db.players().update(p.copy(afk = if (checked) 0 else 1))
                                    vm.reloadPlayers()
                                }
                            }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { vm.goPlayers() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Manage Players") }

            Text("Game", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Learning Enabled")
                Switch(
                    checked = learningEnabled,
                    onCheckedChange = {
                        learningEnabled = it
                        Config.setLearningEnabled(it)
                        scope.launch { repo.db.settings().putBoolean("learning_enabled", it) }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ask 'Who's here?' at launch")
                Switch(
                    checked = rollcallOnLaunch,
                    onCheckedChange = {
                        rollcallOnLaunch = it
                        scope.launch { repo.db.settings().putBoolean("rollcall_on_launch", it) }
                    }
                )
            }

            Text("Feedback Threshold", style = MaterialTheme.typography.titleLarge)
            Text("Room heat threshold: ${(heat * 100).toInt()}%", color = HelldeckColors.LightGray)
            Slider(
                value = heat,
                onValueChange = { heat = it.coerceIn(0.5f, 0.8f) },
                valueRange = 0.5f..0.8f,
                steps = 5,
                onValueChangeFinished = {
                    Config.setRoomHeatThreshold(heat.toDouble())
                    scope.launch { repo.db.settings().putFloat("room_heat_threshold", heat) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Device", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Haptics Enabled")
                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = {
                        hapticsEnabled = it
                        Config.setHapticsEnabled(it)
                        scope.launch { repo.db.settings().putBoolean("haptics_enabled", it) }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sound Effects")
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        scope.launch { repo.db.settings().putBoolean("sound_enabled", it) }
                    }
                )
            }
        }
    }
}

/**
 * A/B voting flow
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ABVoteFlow(
    players: List<PlayerEntity>,
    preChoiceLabel: String,
    preChoices: List<String>,
    preChoice: String?,
    onPreChoice: (String) -> Unit,
    leftLabel: String,
    rightLabel: String,
    onVote: (voterId: String, choice: String) -> Unit,
    onDone: () -> Unit,
    onManagePlayers: (() -> Unit)? = null
) {
    var idx by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var lockPre by remember { mutableStateOf(preChoice != null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pre-choice selection
        if (!lockPre) {
            Text(
                text = preChoiceLabel,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    onPreChoice(preChoices[0])
                    lockPre = true
                }) {
                    Text(preChoices[0])
                }

                Button(onClick = {
                    onPreChoice(preChoices.getOrElse(1) { "B" })
                    lockPre = true
                }) {
                    Text(preChoices.getOrElse(1) { "B" })
                }
            }

            return@Column
        }

        // Voting interface
        if (players.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active players. Enable players in Settings.")
                Spacer(modifier = Modifier.height(8.dp))
                onManagePlayers?.let {
                    OutlinedButton(onClick = it) { Text("Open Settings") }
                }
            }
            return@Column
        }

        val voter = players[idx]

        Text(
            text = "Voter: ${voter.name}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // A/B choice buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { chosen = leftLabel },
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (chosen == leftLabel) HelldeckColors.VoteSelected else HelldeckColors.MediumGray
                    )
            ) {
                Text(leftLabel, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }

                Button(
                    onClick = { chosen = rightLabel },
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (chosen == rightLabel) HelldeckColors.VoteSelected else HelldeckColors.MediumGray
                    )
            ) {
                Text(rightLabel, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Vote button
        Button(
            enabled = chosen != null,
            onClick = {
                chosen?.let { choice ->
                    onVote(voter.id, choice)
                }

                if (idx < players.lastIndex) {
                    idx++
                } else {
                    onDone()
                }
                chosen = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (idx < players.lastIndex) "Lock & Next" else "Finish Voting")
        }
    }
}

/**
 * Judge pick flow
 */
@Composable
fun JudgePickFlow(
    judge: PlayerEntity?,
    options: List<String>,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Judge: ${judge?.name ?: "—"}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Option buttons
        options.forEach { option ->
            Button(
                onClick = { onPick(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Orange
                )
            ) {
                Text(option)
            }
        }
    }
}

/**
 * Scoreboard overlay with enhanced visual hierarchy
 */
@Composable
fun ScoreboardOverlay(
    players: List<PlayerEntity>,
    onClose: () -> Unit
) {
    val sorted = players.sortedByDescending { it.sessionPoints }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.85f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        radius = 1000f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with enhanced styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 SCOREBOARD",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    HelldeckColors.Yellow,
                                    HelldeckColors.Orange,
                                    HelldeckColors.Yellow.copy(alpha = 0.7f)
                                )
                            )
                        )
                    )

                    TextButton(
                        onClick = onClose,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = HelldeckColors.Yellow
                        )
                    ) {
                        Text(
                            "CLOSE",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Podium section for top 3 players
                if (sorted.isNotEmpty()) {
                    PodiumSection(
                        topPlayers = sorted.take(3),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Rest of players
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(sorted.drop(3)) { player ->
                        val position = sorted.indexOf(player) + 1
                        PlayerScoreCard(
                            player = player,
                            position = position,
                            isTopThree = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = HelldeckSpacing.Tiny.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer text with enhanced styling
                Text(
                    text = "Last place picks next game (comeback mechanic)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Rules sheet
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun RulesSheet(onClose: () -> Unit) {
    Scaffold(
        topBar = {
            val vmLocal: HelldeckVm = viewModel()
            TopAppBar(
                title = { Text("HELLDECK — Rules & How-To") },
                navigationIcon = { TextButton(onClick = { vmLocal.goBack() }) { Text("Back") } },
                actions = { TextButton(onClick = { vmLocal.goHome() }) { Text("Home") } }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
        ) {
            item {
                Text(
                    text = "Global Scoring & Flow",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Win +2 • Room Heat +1 (≥60%; 70% in Spicy Mode) • Trash −2 (≥60%) • Cross-game streaks +1→+3 • Sudden Death at 10% battery (first to +10)."
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Voting & Ties",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Binary 8s • Avatar 10s • Judge 6s (locks early at threshold). Tie → 3s revote → Torch RPS (judge decides in judge-games)."
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Turn Order & Fairness",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Random starter then clockwise. Last place picks next game. Late join enters next round."
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Per-game rules
            items(com.helldeck.engine.Games.size) { idx ->
                val g = com.helldeck.engine.Games[idx]
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                        Text(text = g.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = g.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Timer: ${com.helldeck.engine.Config.getTimerForInteraction(g.interaction)} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Players: ${g.minPlayers}–${g.maxPlayers}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("How to Play", style = MaterialTheme.typography.titleMedium)
                        Text(text = gameHowTo(g), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp)) }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun GameRulesScene(vm: HelldeckVm, onClose: () -> Unit) {
    val gid = vm.selectedGameId
    val game = if (gid != null) com.helldeck.engine.GameRegistry.getGameById(gid) else null
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Game Rules") },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = { TextButton(onClick = { vm.goHome() }) { Text("Home") } }
            )
        }
    ) { padding ->
        if (game == null) {
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No game selected")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(HelldeckSpacing.Medium.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                        Text(text = game.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = game.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Timer: ${com.helldeck.engine.Config.getTimerForInteraction(game.interaction)} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Players: ${game.minPlayers}–${game.maxPlayers}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("How to Play", style = MaterialTheme.typography.titleMedium)
                        Text(text = gameHowTo(game), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
                ) { Text("Back to Game") }
            }
        }
    }
}

private fun gameHowTo(g: com.helldeck.engine.GameSpec): String {
    return when (g.interaction) {
        com.helldeck.engine.Interaction.VOTE_AVATAR -> "Everyone votes the most fitting player. Majority wins; active gets points if applicable."
        com.helldeck.engine.Interaction.AB_VOTE -> "Room votes A or B. Active may pre-pick to earn bonus if they read the room correctly."
        com.helldeck.engine.Interaction.TRUE_FALSE -> "Speaker sets TRUTH/BLUFF. Room votes T/F. Points if majority matches the pre-pick."
        com.helldeck.engine.Interaction.JUDGE_PICK -> "Judge selects the best option. Lock to score."
        com.helldeck.engine.Interaction.SMASH_PASS -> "Room votes SMASH or PASS. Majority SMASH rewards the active player."
        com.helldeck.engine.Interaction.TARGET_PICK -> "Pick one target player. Lock to continue; feedback still counts."
        com.helldeck.engine.Interaction.REPLY_TONE -> "Choose reply vibe (Deadpan, Feral, etc.). Lock to continue; feedback after."
        com.helldeck.engine.Interaction.TABOO_CLUE -> "Start timer, give clues without forbidden words. Lock when finished."
        com.helldeck.engine.Interaction.ODD_REASON -> "Pick the misfit among three options and explain why."
        com.helldeck.engine.Interaction.DUEL -> "Run the mini-duel; choose who won."
        com.helldeck.engine.Interaction.SMUGGLE -> "Weave secret words into an alibi without detection; Lock when finished."
        com.helldeck.engine.Interaction.PITCH -> "Pitch your idea. Lock when finished."
        com.helldeck.engine.Interaction.SPEED_LIST -> "List items quickly until the timer ends. Lock when finished."
    }
}

@Composable
private fun GameTile(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columns = if (screenWidthDp < 420) 2 else 3
    val tileWidth = if (columns == 2) 180.dp else 200.dp
    ElevatedCard(
        modifier = Modifier
            .width(tileWidth)
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = HelldeckColors.DarkGray
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HelldeckColors.Yellow.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = icon, style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.LightGray,
                    maxLines = 2
                )
            }
        }
    }
}

private fun gameIconFor(id: String): String = when (id) {
    GameIds.ROAST_CONS -> "🔥"
    GameIds.CONFESS_CAP -> "🕵️"
    GameIds.POISON_PITCH -> "⚖️"
    GameIds.FILLIN -> "✍️"
    GameIds.RED_FLAG -> "🚩"
    GameIds.HOTSEAT_IMP -> "🎭"
    GameIds.TEXT_TRAP -> "💬"
    GameIds.TABOO -> "⛔️"
    GameIds.ODD_ONE -> "🧩"
    GameIds.TITLE_FIGHT -> "👑"
    GameIds.ALIBI -> "🕶️"
    GameIds.HYPE_YIKE -> "📣"
    GameIds.SCATTER -> "🔤"
    GameIds.MAJORITY -> "📊"
    else -> "🎮"
}
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerProfileScene(vm: HelldeckVm, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { Repository.get(AppCtx.ctx) }
    val playerId = vm.selectedPlayerId
    var profile by remember { mutableStateOf<com.helldeck.data.PlayerProfile?>(null) }
    var summaryText by remember { mutableStateOf("") }

    LaunchedEffect(playerId) {
        if (playerId != null) {
            val profiles = repo.computePlayerProfiles()
            profile = profiles.firstOrNull { it.id == playerId }
            profile?.let { pr ->
                summaryText = buildString {
                    append("${pr.avatar} ${pr.name} — HELLDECK Profile\n")
                    append("Total Points: ${pr.totalPoints}\n")
                    append("Wins: ${pr.wins} • Games: ${pr.gamesPlayed}\n")
                    append("Heat Rounds: ${pr.heatRounds} • Quick Laughs: ${pr.quickLaughs}\n")
                    append("Avg 😂: ${"%.2f".format(pr.avgLol)} • Avg 🚮: ${"%.2f".format(pr.avgTrash)}\n")
                    if (pr.awards.isNotEmpty()) {
                        append("Awards: ${pr.awards.joinToString(", ")}\n")
                    }
                    append("#HELLDECK")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    TextButton(onClick = {
                        // Share textual summary
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, summaryText)
                        }
                        ctx.startActivity(android.content.Intent.createChooser(intent, "Share Profile"))
                    }) { Text("Share") }
                    TextButton(onClick = onClose) { Text("Close") }
                }
            )
        }
    ) { padding ->
        profile?.let { pr ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(HelldeckSpacing.Medium.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = HelldeckColors.DarkGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        HelldeckColors.Yellow.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(HelldeckSpacing.Medium.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(pr.avatar, style = MaterialTheme.typography.displayMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(pr.name, style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SimpleStatRow("Total Points", pr.totalPoints.toString())
                                SimpleStatRow("Wins", pr.wins.toString())
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SimpleStatRow("Games", pr.gamesPlayed.toString())
                                SimpleStatRow("Session", pr.sessionPoints.toString())
                            }
                        }
                    }
                }

                // Insights
                Text("Insights", style = MaterialTheme.typography.titleLarge)
                SimpleStatRow("Heat Rounds", pr.heatRounds.toString())
                SimpleStatRow("Quick Laughs", pr.quickLaughs.toString())
                SimpleStatRow("Avg 😂", "${"%.2f".format(pr.avgLol)}")
                SimpleStatRow("Avg 🚮", "${"%.2f".format(pr.avgTrash)}")

                if (pr.awards.isNotEmpty()) {
                    Text("Awards", style = MaterialTheme.typography.titleLarge)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pr.awards.forEach { a -> AssistChip(a) }
                    }
                }

                Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))
                Text(
                    text = "Shareable summary",
                    style = MaterialTheme.typography.titleMedium,
                    color = HelldeckColors.LightGray
                )
                Text(summaryText, style = MaterialTheme.typography.bodyMedium)
            }
        } ?: run {
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No profile selected")
            }
        }
    }
}
