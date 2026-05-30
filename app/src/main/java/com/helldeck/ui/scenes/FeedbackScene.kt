package com.helldeck.ui.scenes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helldeck.engine.Config
import com.helldeck.engine.GameFeedback
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.HapticEvent
import com.helldeck.ui.CardFace
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import com.helldeck.ui.components.OutlineButton
import com.helldeck.ui.components.ReportContentDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hapticsEnabled = Config.hapticsEnabled
    val roundState = vm.roundState

    // Auto-advance countdown (5 seconds for faster pacing)
    var secondsRemaining by remember { mutableStateOf(5) }
    var isAutoAdvancing by remember { mutableStateOf(true) }
    var hasAdvanced by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    // Advance guard
    val advanceLock = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val tryAdvance: () -> Boolean = { advanceLock.compareAndSet(false, true).also { if (it) hasAdvanced = true } }

    // Favorite state
    var isFavorited by remember { mutableStateOf(false) }

    LaunchedEffect(roundState?.filledCard?.id) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_END, useHaptics = hapticsEnabled)
        isFavorited = vm.isCurrentCardFavorited()
        hasAdvanced = false
        advanceLock.set(false)
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
                if (tryAdvance()) { scope.launch { vm.commitFeedbackAndNext() } }
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
                        fontWeight = FontWeight.Black,
                        color = HelldeckColors.colorPrimary,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { vm.goBack() }) { Text("Back") }
                },
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
                        modifier = Modifier.semantics {
                            contentDescription = "Share card as image"
                        },
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share card",
                            tint = HelldeckColors.colorOnDark,
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
                        modifier = Modifier.semantics {
                            contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites"
                        },
                    ) {
                        Icon(
                            if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorited) HelldeckColors.Lol else HelldeckColors.colorOnDark,
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
            InfoBanner(
                message = "The AI learns: Loved cards = more like this. Trash cards = avoid similar content.",
                icon = "\uD83D\uDCA1",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HelldeckSpacing.Medium.dp),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            // Rating buttons - massive, colorful, satisfying
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

            // Undo button
            if (vm.canUndoFeedback) {
                OutlineButton(
                    text = "\u21B6 UNDO",
                    onClick = {
                        vm.undoLastRating()
                        GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                    },
                    modifier = Modifier.padding(top = HelldeckSpacing.Small.dp),
                    accentColor = HelldeckColors.colorMuted,
                )
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            TextButton(
                onClick = { vm.openReportDialog() },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = HelldeckSpacing.Large.dp)
                    .semantics {
                        contentDescription = "Report offensive content"
                    },
            ) {
                Text(
                    text = "\uD83D\uDEA9 Report Offensive Content",
                    style = MaterialTheme.typography.labelSmall,
                    color = HelldeckColors.colorMuted.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            // Primary action: Next button (full width, prominent)
            GlowButton(
                text = when {
                    hasAdvanced -> "Next"
                    secondsRemaining > 0 && !isPaused -> "Next ($secondsRemaining)"
                    isPaused -> "Continue"
                    else -> "Next"
                },
                onClick = {
                    if (tryAdvance()) { scope.launch { vm.commitFeedbackAndNext() } }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HelldeckSpacing.Large.dp),
                enabled = !hasAdvanced,
                accentColor = HelldeckColors.colorSecondary,
            )

            // Status hint
            Text(
                text = when {
                    isPaused -> "\u23F8\uFE0F Paused - Take your time to discuss"
                    secondsRemaining > 0 -> "\u23F1\uFE0F Auto-advancing in ${secondsRemaining}s"
                    else -> "\u23F1\uFE0F Moving on..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = HelldeckColors.colorMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HelldeckSpacing.Tiny.dp),
            )

            // "More options" toggle
            TextButton(
                onClick = { showMoreOptions = !showMoreOptions },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        contentDescription = if (showMoreOptions) "Hide more options" else "Show more options"
                    },
            ) {
                Text(
                    text = if (showMoreOptions) "Less \u25B4" else "More \u25BE",
                    style = MaterialTheme.typography.labelMedium,
                    color = HelldeckColors.colorMuted,
                )
            }

            // Expandable secondary actions
            AnimatedVisibility(
                visible = showMoreOptions,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HelldeckSpacing.Large.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                    ) {
                        // End Game
                        OutlineButton(
                            text = "\uD83C\uDFC1 End Game",
                            onClick = {
                                if (tryAdvance()) { vm.showEndGameSummary() }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !hasAdvanced,
                        )

                        // Pause/Resume
                        OutlineButton(
                            text = if (isPaused) "\u25B6\uFE0F Resume" else "\u23F8\uFE0F Pause",
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.weight(1f),
                            enabled = !hasAdvanced && secondsRemaining > 0,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                    ) {
                        // Skip
                        OutlineButton(
                            text = "Skip",
                            onClick = {
                                if (tryAdvance()) { vm.skipCurrentCard() }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !hasAdvanced,
                        )

                        // Replay
                        if (vm.canReplay()) {
                            OutlineButton(
                                text = "\uD83D\uDD04 Replay",
                                onClick = { scope.launch { vm.replayLastCard() } },
                                modifier = Modifier.weight(1f),
                                accentColor = HelldeckColors.colorAccentCool,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
        }
    }

    if (vm.showReportDialog && roundState != null) {
        ReportContentDialog(
            cardText = roundState.filledCard.text,
            onDismiss = { vm.closeReportDialog() },
            onReport = { reason -> vm.reportOffensiveContent(reason) },
        )
    }

    if (vm.showEndGameSummary) {
        com.helldeck.ui.components.EndGameVotingDialog(
            vm = vm,
            onDismiss = { vm.dismissEndGameSummary() },
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
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingButton(
            emoji = "\uD83D\uDE02",
            label = "LOL",
            glowColor = HelldeckColors.colorSecondary,
            containerColor = HelldeckColors.colorSecondary.copy(alpha = 0.2f),
            labelColor = HelldeckColors.colorSecondary,
            onClick = onLol,
            modifier = Modifier.weight(1f),
        )
        RatingButton(
            emoji = "\uD83D\uDE10",
            label = "MEH",
            glowColor = HelldeckColors.colorAccentWarm,
            containerColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.2f),
            labelColor = HelldeckColors.colorAccentWarm,
            onClick = onMeh,
            modifier = Modifier.weight(1f),
        )
        RatingButton(
            emoji = "\uD83D\uDEAE",
            label = "TRASH",
            glowColor = HelldeckColors.Error,
            containerColor = HelldeckColors.Error.copy(alpha = 0.2f),
            labelColor = HelldeckColors.Error,
            onClick = onTrash,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RatingButton(
    emoji: String,
    label: String,
    glowColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    labelColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = if (reducedMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh)
        },
        label = "rating_scale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = glowColor.copy(alpha = 0.6f),
                ambientColor = glowColor.copy(alpha = 0.4f),
            )
            .semantics {
                contentDescription = "Rate card as $label"
            },
        shape = RoundedCornerShape(HelldeckRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = labelColor,
        ),
        border = BorderStroke(
            width = 2.dp,
            color = glowColor.copy(alpha = 0.5f),
        ),
        interactionSource = interactionSource,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Tiny.dp),
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = labelColor,
            )
        }
    }
}
