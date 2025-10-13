package com.helldeck.ui

import androidx.compose.animation.AnimatedContentTransitionScope

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

/**
 * Scene enumeration for navigation
 */
enum class Scene {
    HOME, PLAYERS, ROUND, FEEDBACK, RULES, SCOREBOARD
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
        vm.initOnce()
    }

    HelldeckTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Background pattern for visual interest
            HelldeckBackgroundPattern(
                pattern = BackgroundPattern.CIRCUIT,
                opacity = 0.03f
            )

            // Scene transitions with smooth animations
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
                    Scene.PLAYERS -> PlayersScene(vm)
                    Scene.ROUND -> RoundScene(vm)
                    Scene.FEEDBACK -> FeedbackScene(vm)
                    Scene.RULES -> RulesSheet { vm.scene = Scene.HOME }
                    Scene.SCOREBOARD -> ScoreboardOverlay(vm.players) { vm.scene = Scene.HOME }
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

    // Game configuration
    var spicy by mutableStateOf(false)

    // Player data
    var players by mutableStateOf(listOf<PlayerEntity>())
    private var turnIdx by mutableStateOf(0)
    private var starterPicked = false

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
    private lateinit var repo: Repository
    private lateinit var templateEngine: TemplateEngine
    private lateinit var engine: GameEngine

    /**
     * Initialize systems on first use
     */
    suspend fun initOnce() {
        if (::repo.isInitialized) return

        repo = Repository.get(AppCtx.ctx)
        templateEngine = TemplateEngine(AppCtx.ctx)
        engine = GameEngine(AppCtx.ctx, repo, templateEngine)
        // Initialize game engine
        engine.initialize()

        // Load initial data
        reloadPlayers()
    }

    /**
     * Reload players from database
     */
    suspend fun reloadPlayers() {
        players = repo.db.players().getAllPlayers().first()

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

            players = repo.db.players().getAllPlayers().first()
        }
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
    suspend fun startRound() {
        scene = Scene.ROUND
        phase = RoundPhase.DRAW

        Config.spicyMode = spicy

        // Pick next game
        val nextGame = pickNextGame()
        currentGame = GameRegistry.getGameById(nextGame)

        // Pick starter if not already picked
        if (!starterPicked) {
            turnIdx = Random.nextInt(players.size)
            starterPicked = true
        }

        // Generate card
        currentCard = engine.nextFilledCard(nextGame)
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
        turnIdx = (turnIdx + 1) % players.size
    }

    /**
     * Get current active player
     */
    fun activePlayer(): PlayerEntity? {
        return if (players.isEmpty()) null else players[turnIdx]
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
    }

    /**
     * Handle LOL feedback
     */
    @androidx.compose.material3.ExperimentalMaterial3Api
    @Composable
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

        engine.commitRound(
            card = card,
            feedback = Feedback(lol, meh, trash, latency, tags),
            judgeWin = judgeWin,
            points = points,
            latencyMs = latency,
            notes = null
        )

        // Reset feedback state
        lol = 0
        meh = 0
        trash = 0
        tags.clear()
        judgeWin = false
        points = 0

        // Advance turn and start next round
        endRoundAdvanceTurn()
        startRound()
    }
}

/**
 * Home scene - main menu
 */
@Composable
fun HomeScene(vm: HelldeckVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main title card
        AnimatedCardFace(
            title = "Single phone. One card per round.",
            subtitle = "Long-press to draw • two-finger = back",
            delayMs = 200
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Start game button
        Button(
            onClick = { scope.launch { vm.startRound() } },
            modifier = Modifier
                .fillMaxWidth()
                .height(HelldeckHeights.Button.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HelldeckColors.Green
            )
        ) {
            Text(
                text = "Start Round",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

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

        // Settings row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (vm.spicy) "Spicy 70%" else "Chill 60%",
                style = MaterialTheme.typography.bodyMedium,
                color = HelldeckColors.LightGray
            )

            Spacer(modifier = Modifier.width(HelldeckSpacing.Small.dp))

            Switch(
                checked = vm.spicy,
                onCheckedChange = {
                    vm.spicy = it
                    Config.spicyMode = it
                }
            )
        }

        // Scoreboard overlay
        if (vm.showScores) {
            ScoreboardOverlay(vm.players) { vm.toggleScores() }
        }
    }
}

/**
 * Players management scene
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PlayersScene(vm: HelldeckVm) {
    val repo = remember { Repository.get(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    val emojis = listOf("😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺")
    var emoji by remember { mutableStateOf(emojis.random()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Players") },
                navigationIcon = {
                    TextButton(onClick = { vm.scene = Scene.HOME }) {
                        Text("Back")
                    }
                }
            )
        }
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

                Button(
                    onClick = { emoji = emojis.random() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HelldeckColors.Orange
                    )
                ) {
                    Text(text = emoji)
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
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
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
                                Text(
                                    text = player.avatar,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = 24.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(HelldeckSpacing.Small.dp))
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Row {
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

                                Spacer(modifier = Modifier.width(HelldeckSpacing.Tiny.dp))

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            repo.db.players().update(
                                                player.copy(afk = 1, name = player.name + " (off)")
                                            )
                                            vm.reloadPlayers()
                                        }
                                    }
                                ) {
                                    Text("Hide")
                                }
                            }
                        }
                    }
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                players = vm.players,
                onVote = vm::onAvatarVote,
                onDone = { vm.resolveInteraction() }
            )
            Interaction.AB_VOTE -> ABVoteFlow(
                players = vm.players,
                preChoiceLabel = "Active picks A/B before votes",
                preChoices = listOf("A", "B"),
                preChoice = vm.preChoice,
                onPreChoice = vm::onPreChoice,
                leftLabel = "A",
                rightLabel = "B",
                onVote = vm::onABVote,
                onDone = { vm.resolveInteraction() }
            )
            Interaction.TRUE_FALSE -> ABVoteFlow(
                players = vm.players,
                preChoiceLabel = "Speaker sets Truth or Bluff",
                preChoices = listOf("TRUTH", "BLUFF"),
                preChoice = vm.preChoice,
                onPreChoice = vm::onPreChoice,
                leftLabel = "T",
                rightLabel = "F",
                onVote = vm::onABVote,
                onDone = { vm.resolveInteraction() }
            )
            Interaction.JUDGE_PICK -> JudgePickFlow(
                judge = vm.players.getOrNull((vm.players.indexOf(vm.activePlayer()) + 1) % vm.players.size),
                options = listOf("Option 1", "Option 2", "Option 3", "Option 4"),
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

/**
 * Feedback collection scene
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun FeedbackScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
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
            onComment = { text, tags -> vm.addComment(text, tags) }
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

/**
 * Avatar voting flow
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun AvatarVoteFlow(
    players: List<PlayerEntity>,
    onVote: (voterId: String, targetId: String) -> Unit,
    onDone: () -> Unit
) {
    var idx by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }

    if (players.isEmpty()) {
        Text("Add players first.")
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
    onDone: () -> Unit
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
            Text("Add players first.")
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
                Text(leftLabel)
            }

            Button(
                onClick = { chosen = rightLabel },
                modifier = Modifier.width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (chosen == rightLabel) HelldeckColors.VoteSelected else HelldeckColors.MediumGray
                )
            ) {
                Text(rightLabel)
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
            TopAppBar(
                title = { Text("HELLDECK — Rules & How-To") },
                actions = {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                }
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

            // Game-specific rules would be added here
            item {
                Text(
                    text = "Tip: Big taps. Long-press draws. Two-finger = back/undo. Torch blink means lock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}