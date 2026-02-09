package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.Config
import com.helldeck.engine.GameFeedback
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.HapticEvent
import com.helldeck.engine.Interaction
import com.helldeck.engine.InteractionType
import com.helldeck.ui.*
import com.helldeck.ui.components.GameActionHintBanner
import com.helldeck.ui.components.QuickReactionBar
import com.helldeck.ui.components.ReportContentDialog
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoundScene(vm: HelldeckVm) {
    val context = LocalContext.current
    val roundState = vm.roundState ?: return
    val game = remember(roundState.gameId) { GameMetadata.getGameMetadata(roundState.gameId) }

    val hapticsEnabled = Config.hapticsEnabled
    var timerGateStarted by remember(roundState.filledCard.id) {
        mutableStateOf(game?.interaction != Interaction.TABOO_CLUE)
    }
    
    // Track if action hint has been dismissed
    var actionHintDismissed by remember(roundState.filledCard.id) { mutableStateOf(false) }

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
            InteractionType.PREDICT_VOTE,
            -> true
            else -> false
        }
    }

    // Timer is authoritative, but some games (e.g., Taboo) require a manual start gate.
    val timerRunning = roundState.isTimerActive() && totalTimeMs > 0 && timerGateStarted
    LaunchedEffect(totalTimeMs, roundState.filledCard.id, timerRunning, roundState.phase) {
        if (totalTimeMs <= 0) return@LaunchedEffect
        if (!timerRunning) return@LaunchedEffect

        timeRemainingMs = totalTimeMs
        while (timeRemainingMs > 0) {
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
                                com.helldeck.ui.state.RoundPhase.INTRO -> "GET READY"
                                com.helldeck.ui.state.RoundPhase.INPUT -> "VOTING"
                                com.helldeck.ui.state.RoundPhase.REVEAL -> "RESULTS"
                                com.helldeck.ui.state.RoundPhase.FEEDBACK -> "RATE IT"
                                com.helldeck.ui.state.RoundPhase.DONE -> "COMPLETE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = HelldeckColors.colorMuted,
                        )
                        Text(
                            text = game?.title ?: "Round",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = {
                    TextButton(onClick = { vm.openRulesForCurrentGame() }) { Text("?") }
                    TextButton(onClick = { vm.goHome() }) { Text("Home") }
                },
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HelldeckSpacing.Large.dp, vertical = HelldeckSpacing.Medium.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { vm.openRulesForCurrentGame() },
                        modifier = Modifier.weight(1f).height(HelldeckHeights.Button.dp),
                        shape = RoundedCornerShape(HelldeckRadius.Medium),
                    ) {
                        Text("Help")
                    }

                    // Quick Fire button during REVEAL phase
                    if (roundState.phase == com.helldeck.ui.state.RoundPhase.REVEAL) {
                        OutlinedButton(
                            onClick = { vm.triggerQuickFire() },
                            modifier = Modifier.height(HelldeckHeights.Button.dp),
                            shape = RoundedCornerShape(HelldeckRadius.Medium),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = androidx.compose.ui.graphics.Color(0xFFFF6B35),
                            ),
                        ) {
                            Text("🔥")
                        }
                    }

                    val primaryLabel = when (roundState.phase) {
                        com.helldeck.ui.state.RoundPhase.INTRO -> "🎯 START"
                        com.helldeck.ui.state.RoundPhase.INPUT -> "✅ LOCK IT"
                        com.helldeck.ui.state.RoundPhase.REVEAL -> "👀 SEE RESULTS"
                        com.helldeck.ui.state.RoundPhase.FEEDBACK -> "⭐ RATE"
                        com.helldeck.ui.state.RoundPhase.DONE -> "➡️ NEXT ROUND"
                    }
                    Button(
                        onClick = {
                            when (roundState.phase) {
                                com.helldeck.ui.state.RoundPhase.INTRO -> vm.handleRoundEvent(
                                    com.helldeck.ui.events.RoundEvent.AdvancePhase,
                                )
                                com.helldeck.ui.state.RoundPhase.INPUT -> vm.resolveInteraction()
                                com.helldeck.ui.state.RoundPhase.REVEAL -> vm.handleRoundEvent(
                                    com.helldeck.ui.events.RoundEvent.AdvancePhase,
                                )
                                com.helldeck.ui.state.RoundPhase.FEEDBACK -> vm.navigateTo(Scene.FEEDBACK)
                                com.helldeck.ui.state.RoundPhase.DONE -> vm.handleRoundEvent(
                                    com.helldeck.ui.events.RoundEvent.AdvancePhase,
                                )
                            }
                        },
                        modifier = Modifier.weight(2f).height(HelldeckHeights.Button.dp),
                        shape = RoundedCornerShape(HelldeckRadius.Pill),
                        colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.colorPrimary),
                    ) {
                        Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = HelldeckSpacing.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (totalTimeMs > 0 && roundState.isTimerActive()) {
                GameTimer(
                    timeRemainingMs = timeRemainingMs,
                    totalTimeMs = totalTimeMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HelldeckSpacing.Medium.dp, bottom = HelldeckSpacing.Small.dp),
                )
            }

            CardFace(
                title = roundState.filledCard.text,
                subtitle = game?.description,
                stakesLabel = when (game?.id) {
                    com.helldeck.engine.GameIds.ROAST_CONS -> "Majority pick: +2pts • Room heat bonus: +1"
                    com.helldeck.engine.GameIds.CONFESS_CAP -> "Fool everyone: +2pts • Guess right: +1pt"
                    com.helldeck.engine.GameIds.POISON_PITCH -> "Best pitch wins: +2pts"
                    com.helldeck.engine.GameIds.FILLIN -> "Judge's favorite: +1pt"
                    com.helldeck.engine.GameIds.RED_FLAG -> "Win vote: +2pts • Lose: Penalty"
                    com.helldeck.engine.GameIds.HOTSEAT_IMP -> "Fool them: +2pts • Catch them: +1pt"
                    com.helldeck.engine.GameIds.TEXT_TRAP -> "Nail the vibe: +2pts"
                    com.helldeck.engine.GameIds.TABOO -> "+2 per word • -1 per forbidden • 5+ bonus: +1"
                    com.helldeck.engine.GameIds.UNIFYING_THEORY -> "Best theory: +2pts • Weak theory: -1pt"
                    com.helldeck.engine.GameIds.TITLE_FIGHT -> "Winner: +1pt • Loser: -1pt"
                    com.helldeck.engine.GameIds.ALIBI -> "Innocent: +2pts • Caught: -1pt"
                    com.helldeck.engine.GameIds.REALITY_CHECK -> "Self-aware (gap 0-1): +2pts • Delusional: 0pts"
                    com.helldeck.engine.GameIds.SCATTER -> "Last one standing survives • Others: elimination"
                    com.helldeck.engine.GameIds.OVER_UNDER -> "Correct bet: +1pt • Subject gets wrong guesses"
                    else -> "Winner earns points • Everyone else: watch and laugh"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                backgroundColor = HelldeckColors.surfacePrimary,
                borderColor = HelldeckColors.colorPrimary,
            )

            TextButton(
                onClick = { vm.openReportDialog() },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = HelldeckSpacing.Small.dp),
            ) {
                Text(
                    text = "🚩 Report Offensive Content",
                    style = MaterialTheme.typography.labelSmall,
                    color = HelldeckColors.LightGray.copy(alpha = 0.7f),
                )
            }

            // Interaction controls only during INPUT to keep the mental model stable.
            if (roundState.phase == com.helldeck.ui.state.RoundPhase.INPUT) {
                // Action Hint Banner - shows "what do I do?" instruction
                if (!actionHintDismissed) {
                    GameActionHintBanner(
                        gameId = roundState.gameId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = HelldeckSpacing.Medium.dp),
                        isFirstTimePlayingGame = true, // TODO: Track per-game first play
                        autoHideDelayMs = 4000L,
                        onDismiss = { actionHintDismissed = true },
                    )
                }
                
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                when (game?.interaction) {
                    Interaction.VOTE_AVATAR -> AvatarVoteFlow(
                        players = vm.activePlayers,
                        onVote = vm::onAvatarVote,
                        onDone = { vm.resolveInteraction() },
                        onManagePlayers = { vm.navigateTo(Scene.PLAYERS) },
                    )
                    Interaction.AB_VOTE -> {
                        val abOptions = (roundState.options as? GameOptions.AB)
                            ?.let { listOf(it.optionA, it.optionB) }
                            ?: listOf("A", "B")
                        ABVoteFlow(
                            players = vm.activePlayers,
                            preChoiceLabel = when (game.id) {
                                com.helldeck.engine.GameIds.POISON_PITCH -> "Active pre-picks A/B before votes"
                                com.helldeck.engine.GameIds.OVER_UNDER -> "Bet OVER or UNDER on the subject's number"
                                else -> "Active picks A/B before votes"
                            },
                            preChoices = abOptions,
                            preChoice = vm.preChoice,
                            onPreChoice = vm::onPreChoice,
                            leftLabel = abOptions.getOrNull(0) ?: "A",
                            rightLabel = abOptions.getOrNull(1) ?: "B",
                            onVote = vm::onABVote,
                            onDone = { vm.resolveInteraction() },
                            onManagePlayers = { vm.navigateTo(Scene.SETTINGS) },
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
                        onManagePlayers = { vm.navigateTo(Scene.PLAYERS) },
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
                            onManagePlayers = { vm.navigateTo(Scene.SETTINGS) },
                        )
                    }
                    Interaction.TARGET_PICK -> {
                        val targetLabel = when (game.id) {
                            com.helldeck.engine.GameIds.REALITY_CHECK -> "Select the subject to rate"
                            else -> "Pick a target player"
                        }
                        SingleAvatarPickFlow(
                            players = vm.activePlayers,
                            title = targetLabel,
                            onPick = { _ -> vm.goToFeedbackNoPoints() },
                            onManagePlayers = { vm.navigateTo(Scene.PLAYERS) },
                        )
                    }
                    Interaction.DUEL -> OptionsPickFlow(
                        title = "Who won the duel?",
                        options = listOf("Active Player wins", "Other wins"),
                        onPick = { choice ->
                            // Title Fight: Winner +1, Loser -1 (per HDRealRules.md)
                            if (choice.startsWith("Active")) vm.commitDirectWin(pts = 1) else vm.goToFeedbackNoPoints()
                        },
                    )
                    Interaction.PITCH -> OptionsPickFlow(
                        title = "Done pitching?",
                        options = listOf("Lock"),
                        onPick = { vm.goToFeedbackNoPoints() },
                    )
                    Interaction.SPEED_LIST -> OptionsPickFlow(
                        title = "Time's up?",
                        options = listOf("Lock"),
                        onPick = { vm.goToFeedbackNoPoints() },
                    )
                    Interaction.REPLY_TONE -> {
                        val replyOptions = (roundState.options as? GameOptions.ReplyTone)?.tones
                            ?: listOf("Deadpan", "Feral", "Chaotic", "Wholesome")
                        OptionsPickFlow(
                            title = "Pick a reply vibe",
                            options = replyOptions,
                            onPick = { vm.resolveInteraction() },
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
                            onDone = { vm.resolveInteraction() },
                        )
                    }
                    Interaction.ODD_REASON -> {
                        val oddOptions = (roundState.options as? GameOptions.OddOneOut)?.items
                            ?: listOf("Option 1", "Option 2", "Option 3")
                        OptionsPickFlow(
                            title = "Pick the misfit",
                            options = oddOptions,
                            onPick = { vm.resolveInteraction() },
                        )
                    }
                    Interaction.JUDGE_PICK -> {
                        val judgeOptions = (roundState.options as? GameOptions.AB)
                            ?.let { listOf(it.optionA, it.optionB) }
                            ?: listOf("Option 1", "Option 2")
                        val activePlayer = vm.activePlayer()
                        val judgeIndex = if (activePlayer != null && vm.players.isNotEmpty()) {
                            (vm.players.indexOf(activePlayer) + 1) % vm.players.size
                        } else {
                            0
                        }
                        JudgePickFlow(
                            judge = vm.players.getOrNull(judgeIndex),
                            options = judgeOptions,
                            onPick = { vm.resolveInteraction() },
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(HelldeckSpacing.Medium.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Button(
                                onClick = { vm.resolveInteraction() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(HelldeckHeights.Button.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HelldeckColors.colorPrimary,
                                ),
                                shape = RoundedCornerShape(HelldeckRadius.Pill),
                            ) {
                                Text(
                                    text = "CONTINUE",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
            } else if (roundState.phase == com.helldeck.ui.state.RoundPhase.REVEAL) {
                // REVEAL phase: Quick Reaction Bar for opt-in feedback
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                
                QuickReactionBar(
                    onReaction = { reaction ->
                        vm.handleQuickReaction(reaction)
                    },
                    onAutoAdvance = {
                        vm.handleAutoAdvanceNoReaction()
                    },
                    autoAdvanceDelayMs = 5000L,
                    showAutoAdvanceTimer = true,
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                
                // Option to go to full feedback screen
                TextButton(
                    onClick = { vm.navigateTo(Scene.FEEDBACK) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "More options →",
                        style = MaterialTheme.typography.labelSmall,
                        color = HelldeckColors.colorMuted,
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                Surface(
                    shape = RoundedCornerShape(HelldeckRadius.Medium),
                    color = HelldeckColors.colorSecondary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, HelldeckColors.colorSecondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = when (roundState.phase) {
                            com.helldeck.ui.state.RoundPhase.INTRO -> "📱 Pass the phone around. When everyone's ready, hit START."
                            else -> "Waiting for the room..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = HelldeckColors.colorOnDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    if (vm.showReportDialog) {
        ReportContentDialog(
            cardText = roundState.filledCard.text,
            onDismiss = { vm.closeReportDialog() },
            onReport = { reason -> vm.reportOffensiveContent(reason) },
        )
    }
}
