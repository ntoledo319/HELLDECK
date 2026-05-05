package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helldeck.content.model.Player
import com.helldeck.ui.HelldeckAnimations
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.vm.GameNightViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen end-game summary with dramatic scoreboard and optional MVP/Dud voting.
 *
 * Two-phase flow:
 * 1. SCOREBOARD — dramatic player rankings with session stats
 * 2. VOTING — optional MVP/Dud card voting (accessible via "Vote on Cards" button)
 *
 * This is the "screenshot moment" at the end of a game night.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndGameVotingDialog(
    vm: GameNightViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Phase: scoreboard first, then voting
    var showVoting by remember { mutableStateOf(false) }

    // Voting state
    var sessionCards by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMvp by remember { mutableStateOf<String?>(null) }
    var selectedDuds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var votingLoading by remember { mutableStateOf(false) }

    // Animation entrance
    var visible by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    // Compute session stats
    val sortedPlayers = remember(vm.activePlayers) {
        vm.activePlayers.sortedByDescending { it.sessionPoints }
    }
    val roundsPlayed = vm.totalRoundsThisSession
    val sessionMinutes = remember {
        val elapsed = System.currentTimeMillis() - vm.sessionStartTimeMs
        (elapsed / 60_000).toInt().coerceAtLeast(1)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                HelldeckColors.colorSecondary.copy(alpha = 0.08f),
                                HelldeckColors.background.copy(alpha = 0.97f),
                                HelldeckColors.background,
                            ),
                            radius = 1400f,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = if (reducedMotion) {
                        fadeIn(animationSpec = tween(1))
                    } else {
                        fadeIn(animationSpec = tween(400)) +
                            scaleIn(
                                animationSpec = spring(
                                    dampingRatio = 0.7f,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                                initialScale = 0.85f,
                            )
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.94f),
                ) {
                    AnimatedContent(
                        targetState = showVoting,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                            } else {
                                (fadeIn(tween(300)) + slideInHorizontally { it / 4 }) togetherWith
                                    (fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
                            }
                        },
                    ) { isVoting ->
                        if (!isVoting) {
                            EndGameScoreboardContent(
                                sortedPlayers = sortedPlayers,
                                roundsPlayed = roundsPlayed,
                                sessionMinutes = sessionMinutes,
                                onVoteCards = {
                                    scope.launch {
                                        votingLoading = true
                                        sessionCards = vm.getSessionCardsForVoting()
                                        votingLoading = false
                                        showVoting = true
                                    }
                                },
                                onPlayAgain = { vm.playAgain() },
                                onGoHome = { vm.finishGameAndGoHome() },
                            )
                        } else {
                            EndGameVotingContent(
                                sessionCards = sessionCards,
                                isLoading = votingLoading,
                                selectedMvp = selectedMvp,
                                selectedDuds = selectedDuds,
                                onMvpSelected = { selectedMvp = it },
                                onDudToggled = { cardId ->
                                    selectedDuds = if (selectedDuds.contains(cardId)) {
                                        selectedDuds - cardId
                                    } else {
                                        selectedDuds + cardId
                                    }
                                },
                                onSubmit = {
                                    scope.launch {
                                        selectedMvp?.let { vm.markCardAsMvp(it) }
                                        selectedDuds.forEach { vm.markCardAsDud(it) }
                                        vm.finishGameAndGoHome()
                                    }
                                },
                                onBack = { showVoting = false },
                                onSkip = { vm.finishGameAndGoHome() },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Phase 1: Dramatic Scoreboard
// ─────────────────────────────────────────────────────────

@Composable
private fun EndGameScoreboardContent(
    sortedPlayers: List<Player>,
    roundsPlayed: Int,
    sessionMinutes: Int,
    onVoteCards: () -> Unit,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current

    // Pulsing glow for the trophy
    val infiniteTransition = rememberInfiniteTransition(label = "trophy_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 1 else 1800,
                easing = EaseInOutSine,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Title: GAME OVER
        Text(
            text = "\uD83C\uDFC6 GAME OVER",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                letterSpacing = 2.sp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        HelldeckColors.colorSecondary,
                        HelldeckColors.colorSecondary.copy(alpha = glowAlpha),
                        HelldeckColors.colorAccentWarm,
                        HelldeckColors.colorSecondary,
                    ),
                ),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = HelldeckColors.colorSecondary.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                    blurRadius = 16f,
                ),
            ),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Session stats badge
        Surface(
            color = HelldeckColors.surfaceElevated,
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                HelldeckColors.colorMuted.copy(alpha = 0.3f),
            ),
        ) {
            Text(
                text = "$roundsPlayed rounds played  \u2022  $sessionMinutes min",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                ),
                color = HelldeckColors.colorMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Player rankings
        sortedPlayers.forEachIndexed { index, player ->
            val position = index + 1
            var itemVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay((index * 150L).coerceAtMost(900L))
                itemVisible = true
            }

            AnimatedVisibility(
                visible = itemVisible || reducedMotion,
                enter = if (reducedMotion) {
                    fadeIn(tween(1))
                } else {
                    fadeIn(tween(400)) + slideInVertically(
                        animationSpec = spring(dampingRatio = 0.7f),
                        initialOffsetY = { it / 3 },
                    )
                },
            ) {
                PlayerRankingCard(
                    player = player,
                    position = position,
                    isWinner = position == 1,
                )
            }

            if (index < sortedPlayers.lastIndex) {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            }
        }

        if (sortedPlayers.isEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "No players scored this session",
                style = MaterialTheme.typography.bodyLarge,
                color = HelldeckColors.colorMuted,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Vote on Cards button (tertiary)
        TextButton(
            onClick = onVoteCards,
            colors = ButtonDefaults.textButtonColors(
                contentColor = HelldeckColors.colorAccentCool,
            ),
        ) {
            Text(
                text = "\uD83C\uDFF7\uFE0F Vote on Cards",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlineButton(
                text = "Home",
                onClick = onGoHome,
                modifier = Modifier.weight(1f),
                icon = "\uD83C\uDFE0",
                accentColor = HelldeckColors.colorMuted,
            )
            GlowButton(
                text = "Play Again",
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f),
                icon = "\uD83D\uDD04",
                accentColor = HelldeckColors.colorSecondary,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Individual player ranking card used in the end-game scoreboard.
 */
@Composable
private fun PlayerRankingCard(
    player: Player,
    position: Int,
    isWinner: Boolean,
) {
    val rankEmoji = when (position) {
        1 -> "\uD83D\uDC51" // crown
        2 -> "\uD83E\uDD48" // silver medal
        3 -> "\uD83E\uDD49" // bronze medal
        else -> null
    }

    val accentColor = when (position) {
        1 -> HelldeckColors.colorSecondary
        2 -> HelldeckColors.colorAccentCool
        3 -> HelldeckColors.colorAccentWarm
        else -> HelldeckColors.colorMuted.copy(alpha = 0.5f)
    }

    NeonCard(
        accentColor = accentColor,
        elevation = if (isWinner) 12.dp else 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = HelldeckSpacing.Small.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = if (isWinner) 8.dp else 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = accentColor.copy(alpha = 0.5f),
                    )
                    .background(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rankEmoji ?: "#$position",
                    style = if (rankEmoji != null) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        )
                    },
                    color = if (rankEmoji == null) accentColor else Color.Unspecified,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Avatar
            Text(
                text = player.avatar,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Player name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (isWinner) FontWeight.Black else FontWeight.Bold,
                    ),
                    color = if (isWinner) HelldeckColors.colorSecondary else HelldeckColors.colorOnDark,
                )
                if (isWinner && position == 1) {
                    Text(
                        text = "CHAMPION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                        ),
                        color = HelldeckColors.colorSecondary.copy(alpha = 0.7f),
                    )
                }
            }

            // Score
            Text(
                text = "${player.sessionPoints}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                ),
                color = accentColor,
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "pts",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = HelldeckColors.colorMuted,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Phase 2: Card Voting
// ─────────────────────────────────────────────────────────

@Composable
private fun EndGameVotingContent(
    sessionCards: List<String>,
    isLoading: Boolean,
    selectedMvp: String?,
    selectedDuds: Set<String>,
    onMvpSelected: (String) -> Unit,
    onDudToggled: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header with back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HelldeckColors.colorMuted,
                ),
            ) {
                Text(
                    "\u2190 Back",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "\uD83C\uDFF7\uFE0F CARD VOTING",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                ),
                color = HelldeckColors.colorAccentCool,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Spacer to balance the back button
            Spacer(modifier = Modifier.width(72.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = HelldeckColors.colorPrimary)
            }
        } else if (sessionCards.isEmpty()) {
            Text(
                text = "No cards played this session",
                style = MaterialTheme.typography.bodyLarge,
                color = HelldeckColors.colorMuted,
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlowButton(
                text = "Done",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
        } else {
            // MVP Selection
            NeonCard(accentColor = HelldeckColors.colorSecondary) {
                Text(
                    text = "\uD83C\uDFC6 Best Card?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = HelldeckColors.colorSecondary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                sessionCards.take(10).forEach { cardId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMvpSelected(cardId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMvp == cardId,
                            onClick = { onMvpSelected(cardId) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HelldeckColors.colorSecondary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cardId.take(40) + if (cardId.length > 40) "..." else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HelldeckColors.colorOnDark,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dud Selection
            NeonCard(accentColor = HelldeckColors.colorMuted.copy(alpha = 0.4f)) {
                Text(
                    text = "\uD83D\uDCA9 Any Duds? (optional)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = HelldeckColors.colorMuted,
                )

                Spacer(modifier = Modifier.height(8.dp))

                sessionCards.take(10).forEach { cardId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDudToggled(cardId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedDuds.contains(cardId),
                            onCheckedChange = { onDudToggled(cardId) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = HelldeckColors.colorPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cardId.take(40) + if (cardId.length > 40) "..." else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HelldeckColors.colorOnDark,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlineButton(
                    text = "Skip",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    accentColor = HelldeckColors.colorMuted,
                )
                GlowButton(
                    text = "Submit",
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    icon = "\u2714\uFE0F",
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
