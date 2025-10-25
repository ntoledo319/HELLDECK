package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.Config
import com.helldeck.engine.GameFeedback
import com.helldeck.engine.HapticEvent
import com.helldeck.engine.Interaction
import com.helldeck.ui.*
import com.helldeck.ui.theme.HelldeckColors
import com.helldeck.ui.theme.HelldeckSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoundScene(vm: HelldeckVm) {
    val card = vm.currentCard ?: return
    val game = vm.currentGame ?: return
    val context = LocalContext.current

    val repo = remember { ContentRepository(AppCtx.ctx) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(card.id) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_START, useHaptics = hapticsEnabled)
    }

    val totalTimeMs = remember(card.id) { Config.getTimerForInteraction(game.interaction) }
    var timeRemaining by remember(card.id) { mutableIntStateOf(totalTimeMs) }
    var lastStage by remember(card.id) { mutableIntStateOf(3) }
    val autoResolveOnTimeUp = remember(card.id) {
        when (game.interaction) {
            Interaction.AB_VOTE, Interaction.TRUE_FALSE, Interaction.VOTE_AVATAR -> true
            else -> false
        }
    }
    var timerRunning by remember(card.id) {
        mutableStateOf(game.interaction != Interaction.TABOO_CLUE && totalTimeMs > 0)
    }
    var timeUp by remember(card.id) { mutableStateOf(false) }

    LaunchedEffect(totalTimeMs, card.id, timerRunning) {
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
            CardFace(
                title = card.text,
                subtitle = "(${game.title})",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(HelldeckSpacing.Medium.dp),
                
            )

        when (game.interaction) {
        Interaction.VOTE_AVATAR -> AvatarVoteFlow(
            players = vm.activePlayers,
            onVote = vm::onAvatarVote,
            onDone = { vm.resolveInteraction() },
            onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
        )
        Interaction.AB_VOTE -> {
            val options = vm.currentCard?.let { vm.getOptionsFor(it, com.helldeck.content.engine.GameEngine.Request(
                gameId = game.id,
                sessionId = "current",
                players = vm.activePlayers.map { it.name }
            )) }
            val abOptions = if (options is GameOptions.AB) listOf(options.optionA, options.optionB) else listOf("A", "B")
            ABVoteFlow(
                players = vm.activePlayers,
                preChoiceLabel = "Active picks A/B before votes",
                preChoices = abOptions,
                preChoice = vm.preChoice,
                onPreChoice = vm::onPreChoice,
                leftLabel = abOptions.getOrNull(0) ?: "A",
                rightLabel = abOptions.getOrNull(1) ?: "B",
                onVote = vm::onABVote,
                onDone = { vm.resolveInteraction() },
                onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
            )
        }
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
            Interaction.SMASH_PASS -> {
                val options = vm.currentCard?.let { vm.getOptionsFor(it, com.helldeck.content.engine.GameEngine.Request(
                    gameId = game.id,
                    sessionId = "current",
                    players = vm.activePlayers.map { it.name }
                )) }
                val abOptions = if (options is GameOptions.AB) listOf(options.optionA, options.optionB) else listOf("SMASH", "PASS")
                ABVoteFlow(
                    players = vm.activePlayers,
                    preChoiceLabel = "",
                    preChoices = emptyList(),
                    preChoice = null,
                    onPreChoice = {},
                    leftLabel = abOptions.getOrNull(0) ?: "SMASH",
                    rightLabel = abOptions.getOrNull(1) ?: "PASS",
                    onVote = vm::onABVote,
                    onDone = { vm.resolveInteraction() },
                    onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
                )
            }
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
            Interaction.REPLY_TONE -> {
                val options = vm.currentCard?.let { vm.getOptionsFor(it, com.helldeck.content.engine.GameEngine.Request(
                    gameId = game.id,
                    sessionId = "current",
                    players = vm.activePlayers.map { it.name }
                )) }
                val replyOptions = if (options is GameOptions.ReplyTone) options.tones else listOf("Deadpan", "Feral", "Chaotic", "Wholesome")
                OptionsPickFlow(
                    title = "Pick a reply vibe",
                    options = replyOptions,
                    onPick = { vm.resolveInteraction() }
                )
            }
            Interaction.TABOO_CLUE -> {
                val options = vm.currentCard?.let { vm.getOptionsFor(it, com.helldeck.content.engine.GameEngine.Request(
                    gameId = game.id,
                    sessionId = "current",
                    players = vm.activePlayers.map { it.name }
                )) }
                val tabooOptions = if (options is GameOptions.Taboo) options.forbidden else emptyList()
                TabooFlow(
                    clue = card.text,
                    taboos = tabooOptions,
                    running = timerRunning,
                    onStart = {
                        timeRemaining = totalTimeMs
                        timeUp = false
                        timerRunning = true
                    },
                    onDone = { vm.resolveInteraction() }
                )
            }
            Interaction.ODD_REASON -> {
                val options = vm.currentCard?.let { vm.getOptionsFor(it, com.helldeck.content.engine.GameEngine.Request(
                    gameId = game.id,
                    sessionId = "current",
                    players = vm.activePlayers.map { it.name }
                )) }
                val oddOptions = if (options is GameOptions.OddOneOut) options.items else listOf("Option 1", "Option 2", "Option 3")
                OptionsPickFlow(
                    title = "Pick the misfit",
                    options = oddOptions,
                    onPick = { vm.resolveInteraction() }
                )
            }
            Interaction.JUDGE_PICK -> {
                val options = vm.currentCard?.let { vm.getOptionsFor(it, com.helldeck.content.engine.GameEngine.Request(
                    gameId = game.id,
                    sessionId = "current",
                    players = vm.activePlayers.map { it.name }
                )) }
                val judgeOptions = if (options is GameOptions.AB) listOf(options.optionA, options.optionB) else listOf("Option 1", "Option 2")
                JudgePickFlow(
                    judge = vm.players.getOrNull((vm.players.indexOf(vm.activePlayer()) + 1) % vm.players.size),
                    options = judgeOptions,
                    onPick = { vm.resolveInteraction() }
                )
            }
            else -> {
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
