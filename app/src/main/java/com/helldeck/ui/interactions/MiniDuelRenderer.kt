package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckAnimations
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Challenge types for Title Fight
 */
private enum class ChallengeType(val emoji: String, val displayName: String, val instruction: String) {
    BRAIN("🧠", "BRAIN", "Take turns naming items. Pause 3+ sec or repeat = LOSE"),
    BODY("💪", "BODY", "Race to complete the task. Second place = LOSE"),
    SOUL("👁️", "SOUL", "Endurance test. First to break = LOSE")
}

/**
 * Renders MINI_DUEL interaction for Title Fight game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Epic VS screen with dramatic head-to-head feel
 * - NeonCards for fighter buttons with glow selection
 * - Red vs Blue fighter colors
 * - Spring physics with victory animations
 * - Champion crown effect on selection
 *
 * @ai_prompt Epic VS battle screen, fighting game vibe, HELLDECK neon styling
 */
@Composable
fun MiniDuelRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var selectedWinner by remember { mutableStateOf<Int?>(null) }

    // Detect challenge type from card text
    val cardText = roundState.filledCard.text
    val challengeType = when {
        cardText.startsWith("Category:", ignoreCase = true) -> ChallengeType.BRAIN
        cardText.startsWith("Speed:", ignoreCase = true) -> ChallengeType.BODY
        cardText.startsWith("Guts:", ignoreCase = true) -> ChallengeType.SOUL
        else -> null
    }

    // VS pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "vs_pulse")
    val vsPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "vs_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp)
            .semantics { contentDescription = "Title Fight duel, pick the winner" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Epic header - NeonCard
        NeonCard(
            accentColor = HelldeckColors.colorAccentWarm,
            elevation = 6.dp,
        ) {
            Text(
                text = "TITLE FIGHT",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                ),
                color = HelldeckColors.colorAccentWarm,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Contextual instructions based on challenge type
        if (challengeType != null) {
            NeonCard(
                accentColor = HelldeckColors.colorAccentCool,
                elevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = challengeType.emoji,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = HelldeckSpacing.Small.dp),
                    )
                    Text(
                        text = "${challengeType.displayName}: ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                        color = HelldeckColors.colorAccentCool,
                    )
                    Text(
                        text = challengeType.instruction,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 18.sp,
                        ),
                        color = HelldeckColors.colorOnDark.copy(alpha = 0.9f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player 1 (Red corner)
            DuelFighterCard(
                label = "PLAYER 1",
                emoji = "🔴",
                cornerLabel = "RED",
                isSelected = selectedWinner == 0,
                isRejected = selectedWinner != null && selectedWinner != 0,
                accentColor = HelldeckColors.colorPrimary,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedWinner = 0
                    onEvent(RoundEvent.DuelWinner(0))
                },
                modifier = Modifier.weight(1f),
            )

            // VS indicator - huge glowing magenta
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(if (reducedMotion) 1f else vsPulse)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(HelldeckRadius.Pill),
                        spotColor = HelldeckColors.colorPrimary.copy(alpha = 0.8f),
                        ambientColor = HelldeckColors.colorPrimary.copy(alpha = 0.4f),
                    ),
            ) {
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp,
                        letterSpacing = 4.sp,
                    ),
                    color = HelldeckColors.colorPrimary,
                )
            }

            // Player 2 (Blue corner)
            DuelFighterCard(
                label = "PLAYER 2",
                emoji = "🔵",
                cornerLabel = "BLUE",
                isSelected = selectedWinner == 1,
                isRejected = selectedWinner != null && selectedWinner != 1,
                accentColor = HelldeckColors.colorAccentCool,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedWinner = 1
                    onEvent(RoundEvent.DuelWinner(1))
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Winner declaration
        if (selectedWinner != null) {
            val winnerColor = if (selectedWinner == 0) HelldeckColors.colorPrimary else HelldeckColors.colorAccentCool
            NeonCard(
                accentColor = winnerColor,
                elevation = 8.dp,
            ) {
                Text(
                    text = "WINNER: PLAYER ${selectedWinner!! + 1}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = winnerColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Individual fighter card using NeonCard with VS battle styling.
 * Selected fighter gets crown, rejected dims.
 */
@Composable
private fun DuelFighterCard(
    label: String,
    emoji: String,
    cornerLabel: String,
    isSelected: Boolean,
    isRejected: Boolean,
    accentColor: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.1f
            isRejected -> 0.9f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh)
        },
        label = "fighter_scale",
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isRejected) 0.35f else 1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "card_alpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fighter_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(600, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "selected_pulse",
    )

    val effectiveElevation = when {
        isSelected -> 20.dp * selectedPulse
        isRejected -> 2.dp
        else -> 6.dp
    }

    NeonCard(
        modifier = modifier
            .heightIn(min = 56.dp)
            .height(150.dp)
            .scale(scale)
            .alpha(cardAlpha)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 24.dp * selectedPulse,
                        shape = RoundedCornerShape(HelldeckRadius.Large),
                        spotColor = accentColor.copy(alpha = 0.9f),
                        ambientColor = accentColor.copy(alpha = 0.5f),
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription = "$cornerLabel corner $label${if (isSelected) ", winner" else ""}"
            },
        accentColor = if (isRejected) HelldeckColors.colorMuted else accentColor,
        elevation = effectiveElevation,
        onClick = onClick,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Crown for winner
            if (isSelected) {
                Text(
                    text = "👑",
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            }

            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (isSelected) 38.sp else 32.sp,
                ),
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            Text(
                text = cornerLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp,
                ),
                color = when {
                    isSelected -> accentColor
                    isRejected -> HelldeckColors.colorMuted
                    else -> accentColor
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 18.sp,
                ),
                color = when {
                    isSelected -> accentColor
                    isRejected -> HelldeckColors.colorMuted
                    else -> HelldeckColors.colorOnDark
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}
