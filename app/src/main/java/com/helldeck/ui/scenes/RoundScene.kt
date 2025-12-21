package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.Config
import com.helldeck.engine.GameFeedback
import com.helldeck.engine.HapticEvent
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.Interaction
import com.helldeck.engine.InteractionType
import com.helldeck.ui.*
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoundScene(vm: HelldeckVm) {
    val context = LocalContext.current
    val roundState = vm.roundState ?: return
    val game = remember(roundState.gameId) { GameMetadata.getGameMetadata(roundState.gameId) } ?: vm.currentGame

    val hapticsEnabled = Config.hapticsEnabled
    var timerGateStarted by remember(roundState.filledCard.id) {
        mutableStateOf(game?.interaction != Interaction.TABOO_CLUE)
    }

    LaunchedEffect(roundState.filledCard.id) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_START, useHaptics = hapticsEnabled)
    }

    val totalTimeMs = remember(roundState.filledCard.id) { (roundState.timerSec.coerceAtLeast(0) * 1000) }
    var timeRemainingMs by remember(roundState.filledCard.id) { mutableIntStateOf(totalTimeMs) }
    var lastStage by remember(roundState.filledCard.id) { mutableIntStateOf(3) }
    val autoResolveOnTimeUp = remember(roundState.filledCard.id, roundState.interactionType) {
        when (roundState.interactionType) {
            InteractionType.VOTE_PLAYER,
            InteractionType.A_B_CHOICE,
            InteractionType.TRUE_FALSE,
            InteractionType.SMASH_PASS,
            InteractionType.PREDICT_VOTE -> true
            else -> false
        }
    }

    // Timer is authoritative, but some games (e.g., Taboo) require a manual start gate.
    val timerRunning = roundState.isTimerActive() && totalTimeMs > 0 && timerGateStarted
    LaunchedEffect(totalTimeMs, roundState.filledCard.id, timerRunning) {
        if (totalTimeMs <= 0) return@LaunchedEffect
        if (!timerRunning) return@LaunchedEffect

        timeRemainingMs = totalTimeMs
        while (timeRemainingMs > 0 && timerRunning) {
            delay(1000)
            timeRemainingMs = (timeRemainingMs - 1000).coerceAtLeast(0)
            val progress = timeRemainingMs.toFloat() / totalTimeMs.toFloat()
            val stage = when {
                progress < 0.1f -> 0
                progress < 0.3f -> 1
                else -> 2
            }
            if (stage != lastStage) {
                GameFeedback.triggerTimerFeedback(context, timeRemainingMs, totalTimeMs, useHaptics = hapticsEnabled)
                lastStage = stage
            }
        }

        if (timeRemainingMs <= 0 && autoResolveOnTimeUp) {
            vm.resolveInteraction()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = when (roundState.phase) {
                                com.helldeck.ui.state.RoundPhase.INTRO -> "INTRO"
                                com.helldeck.ui.state.RoundPhase.INPUT -> "INPUT"
                                com.helldeck.ui.state.RoundPhase.REVEAL -> "REVEAL"
                                com.helldeck.ui.state.RoundPhase.FEEDBACK -> "FEEDBACK"
                                com.helldeck.ui.state.RoundPhase.DONE -> "DONE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = HelldeckColors.colorMuted
                        )
                        Text(
                            text = game?.title ?: "Round",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = {
                    TextButton(onClick = { vm.openRulesForCurrentGame() }) { Text("?") }
                    TextButton(onClick = { vm.goHome() }) { Text("Home") }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HelldeckSpacing.Large.dp, vertical = HelldeckSpacing.Medium.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { vm.openRulesForCurrentGame() },
                        modifier = Modifier.weight(1f).height(HelldeckHeights.Button.dp),
                        shape = RoundedCornerShape(HelldeckRadius.Medium)
                    ) {
                        Text("Help")
                    }

                    val primaryLabel = when (roundState.phase) {
                        com.helldeck.ui.state.RoundPhase.INTRO -> "START ROUND"
                        com.helldeck.ui.state.RoundPhase.INPUT -> "LOCK IN"
                        com.helldeck.ui.state.RoundPhase.REVEAL -> "NEXT"
                        com.helldeck.ui.state.RoundPhase.FEEDBACK -> "RATE"
                        com.helldeck.ui.state.RoundPhase.DONE -> "NEXT"
                    }
                    Button(
                        onClick = {
                            when (roundState.phase) {
                                com.helldeck.ui.state.RoundPhase.INTRO -> vm.handleRoundEvent(com.helldeck.ui.events.RoundEvent.AdvancePhase)
                                com.helldeck.ui.state.RoundPhase.INPUT -> vm.resolveInteraction()
                                com.helldeck.ui.state.RoundPhase.REVEAL -> vm.handleRoundEvent(com.helldeck.ui.events.RoundEvent.AdvancePhase)
                                com.helldeck.ui.state.RoundPhase.FEEDBACK -> vm.navigateTo(Scene.FEEDBACK)
                                com.helldeck.ui.state.RoundPhase.DONE -> vm.handleRoundEvent(com.helldeck.ui.events.RoundEvent.AdvancePhase)
                            }
                        },
                        modifier = Modifier.weight(2f).height(HelldeckHeights.Button.dp),
                        shape = RoundedCornerShape(HelldeckRadius.Pill),
                        colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.colorPrimary)
                    ) {
                        Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = HelldeckSpacing.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (totalTimeMs > 0 && roundState.isTimerActive()) {
                GameTimer(
                    timeRemainingMs = timeRemainingMs,
                    totalTimeMs = totalTimeMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HelldeckSpacing.Medium.dp, bottom = HelldeckSpacing.Small.dp)
                )
            }

            CardFace(
                title = roundState.filledCard.text,
                subtitle = game?.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                backgroundColor = HelldeckColors.surfacePrimary,
                borderColor = HelldeckColors.colorPrimary
            )

            // Interaction controls only during INPUT to keep the mental model stable.
            if (roundState.phase == com.helldeck.ui.state.RoundPhase.INPUT) {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                when (game?.interaction) {
                    Interaction.VOTE_AVATAR -> AvatarVoteFlow(
                        players = vm.activePlayers,
                        onVote = vm::onAvatarVote,
                        onDone = { vm.resolveInteraction() },
                        onManagePlayers = { vm.navigateTo(Scene.SETTINGS) }
                    )
                    Interaction.AB_VOTE -> {
                        val abOptions = (roundState.options as? GameOptions.AB)
                            ?.let { listOf(it.optionA, it.optionB) }
                            ?: listOf("A", "B")
                        ABVoteFlow(
                            players = vm.activePlayers,
                            preChoiceLabel = when (game.id) {
                                com.helldeck.engine.GameIds.POISON_PITCH -> "Active pre-picks A/B before votes"
                                com.helldeck.engine.GameIds.MAJORITY -> "Predict the room: pick A/B before votes"
                                else -> "Active picks A/B before votes"
                            },
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
                        val abOptions = (roundState.options as? GameOptions.AB)
                            ?.let { listOf(it.optionA, it.optionB) }
                            ?: listOf("SMASH", "PASS")
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
                        val replyOptions = (roundState.options as? GameOptions.ReplyTone)?.tones
                            ?: listOf("Deadpan", "Feral", "Chaotic", "Wholesome")
                        OptionsPickFlow(
                            title = "Pick a reply vibe",
                            options = replyOptions,
                            onPick = { vm.resolveInteraction() }
                        )
                    }
                    Interaction.TABOO_CLUE -> {
                        val tabooOptions = (roundState.options as? GameOptions.Taboo)?.forbidden ?: emptyList()
                        TabooFlow(
                            clue = roundState.filledCard.text,
                            taboos = tabooOptions,
                            running = timerRunning,
                            onStart = {
                                timerGateStarted = true
                                timeRemainingMs = totalTimeMs
                            },
                            onDone = { vm.resolveInteraction() }
                        )
                    }
                    Interaction.ODD_REASON -> {
                        val oddOptions = (roundState.options as? GameOptions.OddOneOut)?.items
                            ?: listOf("Option 1", "Option 2", "Option 3")
                        OptionsPickFlow(
                            title = "Pick the misfit",
                            options = oddOptions,
                            onPick = { vm.resolveInteraction() }
                        )
                    }
                    Interaction.JUDGE_PICK -> {
                        val judgeOptions = (roundState.options as? GameOptions.AB)
                            ?.let { listOf(it.optionA, it.optionB) }
                            ?: listOf("Option 1", "Option 2")
                        JudgePickFlow(
                            judge = vm.players.getOrNull((vm.players.indexOf(vm.activePlayer()) + 1) % vm.players.size),
                            options = judgeOptions,
                            onPick = { vm.resolveInteraction() }
                        )
                    }
                    else -> {
                        BigZones(
                            onLeft = { /* left */ },
                            onCenter = { vm.resolveInteraction() },
                            onRight = { /* right */ },
                            onLong = { /* long */ }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
            } else {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                Text(
                    text = "Tap START ROUND when the room is ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.colorMuted
                )
            }
    }
}
