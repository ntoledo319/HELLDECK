package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.engine.Config
import com.helldeck.engine.GameFeedback
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.HapticEvent
import com.helldeck.ui.CardFace
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.components.ReportContentDialog
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hapticsEnabled = Config.hapticsEnabled
    val roundState = vm.roundState

    // Auto-advance countdown (8 seconds for more discussion time)
    var secondsRemaining by remember { mutableStateOf(8) }
    var isAutoAdvancing by remember { mutableStateOf(true) }
    var hasAdvanced by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Favorite state
    var isFavorited by remember { mutableStateOf(false) }

    LaunchedEffect(roundState?.filledCard?.id) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_END, useHaptics = hapticsEnabled)
        isFavorited = vm.isCurrentCardFavorited()
        hasAdvanced = false // Reset guard on new card
    }

    // Auto-advance timer with pause support
    LaunchedEffect(isAutoAdvancing, hasAdvanced, isPaused) {
        if (isAutoAdvancing && !hasAdvanced && !isPaused) {
            while (secondsRemaining > 0 && !isPaused) {
                kotlinx.coroutines.delay(1000)
                if (!isPaused) {
                    secondsRemaining--
                }
            }
            if (secondsRemaining == 0 && !hasAdvanced && !isPaused) {
                hasAdvanced = true
                vm.commitFeedbackAndNext()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FEEDBACK",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    )
                },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = {
                    // Share card as image
                    IconButton(
                        onClick = {
                            val card = roundState?.filledCard
                            val game = roundState?.gameId?.let { GameMetadata.getGameMetadata(it) }
                            val player = vm.activePlayer()
                            if (card != null && game != null) {
                                com.helldeck.utils.ShareUtils.shareCardAsImage(
                                    context = context,
                                    cardText = card.text,
                                    gameName = game.title,
                                    playerName = player?.name,
                                )
                            }
                        },
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share card",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Favorite button
                    IconButton(
                        onClick = {
                            scope.launch {
                                isFavorited = vm.toggleFavorite()
                                GameFeedback.triggerFeedback(
                                    context,
                                    HapticEvent.VOTE_CONFIRM,
                                    useHaptics = hapticsEnabled,
                                )
                            }
                        },
                    ) {
                        Icon(
                            if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorited) HelldeckColors.Lol else MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    TextButton(onClick = { vm.goHome() }) { Text("Home") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CardFace(
                title = roundState?.filledCard?.text ?: "How was that?",
                subtitle = "Rate this card to help improve future games",
                stakesLabel = "Your feedback trains the AI to generate better cards for YOUR group's humor",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(HelldeckSpacing.Medium.dp),
            )
            
            // Explain AI learning
            com.helldeck.ui.components.InfoBanner(
                message = "💡 The AI learns: Loved cards = more like this. Trash cards = avoid similar content.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HelldeckSpacing.Medium.dp),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            RatingRail(
                onLol = {
                    vm.feedbackLol()
                    GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                    com.helldeck.audio.SoundManager.play(context, com.helldeck.audio.GameSound.LOL_RATING)
                },
                onMeh = {
                    vm.feedbackMeh()
                    GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                    com.helldeck.audio.SoundManager.play(context, com.helldeck.audio.GameSound.MEH_RATING)
                },
                onTrash = {
                    vm.feedbackTrash()
                    GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                    com.helldeck.audio.SoundManager.play(context, com.helldeck.audio.GameSound.TRASH_RATING)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HelldeckSpacing.Large.dp),
            )

            // Undo button (appears after rating)
            if (vm.canUndoFeedback) {
                OutlinedButton(
                    onClick = {
                        vm.undoLastRating()
                        GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    shape = RoundedCornerShape(HelldeckRadius.Medium),
                    border = BorderStroke(1.dp, HelldeckColors.colorMuted),
                ) {
                    Text(
                        text = "↶ UNDO",
                        style = MaterialTheme.typography.labelMedium,
                        color = HelldeckColors.colorOnDark,
                    )
                }
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            TextButton(
                onClick = { vm.openReportDialog() },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = "🚩 Report Offensive Content",
                    style = MaterialTheme.typography.labelSmall,
                    color = HelldeckColors.LightGray.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            // Replay button (if available)
            if (vm.canReplay()) {
                OutlinedButton(
                    onClick = { scope.launch { vm.replayLastCard() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HelldeckHeights.Button.dp)
                        .padding(horizontal = HelldeckSpacing.Large.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HelldeckColors.colorPrimary,
                    ),
                ) {
                    Text(text = "🔄 REPLAY THIS CARD", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Auto-advance controls with pause
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HelldeckSpacing.Large.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Pause/Resume button
                com.helldeck.ui.components.OutlineButton(
                    text = if (isPaused) "▶️" else "⏸️",
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier.weight(0.5f),
                    enabled = !hasAdvanced && secondsRemaining > 0,
                )
                
                // Skip button
                com.helldeck.ui.components.OutlineButton(
                    text = "Skip",
                    onClick = {
                        if (!hasAdvanced) {
                            hasAdvanced = true
                            scope.launch { vm.commitFeedbackAndNext() }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !hasAdvanced,
                )

                // Next/Continue button
                com.helldeck.ui.components.GlowButton(
                    text = when {
                        hasAdvanced -> "Next"
                        secondsRemaining > 0 && !isPaused -> "Next ($secondsRemaining)"
                        isPaused -> "Continue"
                        else -> "Next"
                    },
                    onClick = {
                        if (!hasAdvanced) {
                            hasAdvanced = true
                            scope.launch { vm.commitFeedbackAndNext() }
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    enabled = !hasAdvanced,
                    accentColor = HelldeckColors.colorSecondary,
                )
            }

            // Status hint
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isPaused -> "⏸️ Paused - Take your time to discuss"
                    secondsRemaining > 0 -> "⏱️ Auto-advancing in ${secondsRemaining}s (tap ⏸️ to pause)"
                    else -> "⏱️ Moving on..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = HelldeckColors.colorMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (vm.showReportDialog && roundState != null) {
        ReportContentDialog(
            cardText = roundState.filledCard.text,
            onDismiss = { vm.closeReportDialog() },
            onReport = { reason -> vm.reportOffensiveContent(reason) },
        )
    }
}

@Composable
private fun RatingRail(
    onLol: () -> Unit,
    onMeh: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingButton(
            emoji = "😂",
            label = "LOL",
            color = HelldeckColors.Lol,
            onClick = onLol,
            modifier = Modifier.weight(1f),
        )
        RatingButton(
            emoji = "😐",
            label = "MEH",
            color = HelldeckColors.Meh,
            onClick = onMeh,
            modifier = Modifier.weight(1f),
        )
        RatingButton(
            emoji = "🚮",
            label = "TRASH",
            color = HelldeckColors.Trash,
            onClick = onTrash,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RatingButton(
    emoji: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = emoji, style = MaterialTheme.typography.headlineLarge)
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = HelldeckColors.Black)
        }
    }
}
