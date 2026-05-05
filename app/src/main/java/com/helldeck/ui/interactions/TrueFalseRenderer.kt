package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders TRUE/FALSE interaction.
 * Used for Confession or Cap game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Lie detector vibe with dramatic TRUE/FALSE NeonCards
 * - Green (truth) vs Red (lie) color coding
 * - Spring physics on selection, rejected dims
 * - Pulsing "scanning" effect
 *
 * @ai_prompt Lie detector vibe with green/red dramatic cards, HELLDECK neon styling
 */
@Composable
fun TrueFalseRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var selected by remember { mutableStateOf<String?>(null) }

    // Scanning pulse effect for lie detector vibe
    val infiniteTransition = rememberInfiniteTransition(label = "lie_detector_pulse")
    val scanPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "scan_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.ExtraLarge.dp)
            .semantics { contentDescription = "True or False decision" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Lie detector header with scanning effect
        NeonCard(
            accentColor = HelldeckColors.colorAccentCool,
            elevation = (8.dp * scanPulse * 2f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "LIE DETECTOR ACTIVE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorAccentCool,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Question indicator
        Text(
            text = "Is this statement...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
        ) {
            TrueFalseCard(
                label = "TRUE",
                emoji = "✓",
                isSelected = selected == "T",
                isRejected = selected != null && selected != "T",
                accentColor = HelldeckColors.colorSecondary, // Lime green for truth
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = "T"
                    onEvent(RoundEvent.PickAB("T"))
                },
                modifier = Modifier.weight(1f),
            )

            TrueFalseCard(
                label = "FALSE",
                emoji = "✗",
                isSelected = selected == "F",
                isRejected = selected != null && selected != "F",
                accentColor = HelldeckColors.Error, // Red for false/lie
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = "F"
                    onEvent(RoundEvent.PickAB("F"))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual TRUE/FALSE card using NeonCard with lie detector styling.
 * Selected card glows dramatically, rejected dims.
 *
 * @ai_prompt Spring physics, dramatic glow, lie detector vibe, NeonCard
 */
@Composable
private fun TrueFalseCard(
    label: String,
    emoji: String,
    isSelected: Boolean,
    isRejected: Boolean,
    accentColor: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Spring physics for selection
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.1f
            isRejected -> 0.92f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessHigh,
            )
        },
        label = "tf_card_scale",
    )

    // Dim rejected card
    val cardAlpha by animateFloatAsState(
        targetValue = if (isRejected) 0.35f else 1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "card_alpha",
    )

    // Pulsing glow for selected state
    val infiniteTransition = rememberInfiniteTransition(label = "tf_selection_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
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
            .height(140.dp)
            .scale(scale)
            .alpha(cardAlpha)
            .semantics {
                contentDescription = "$label${if (isSelected) ", selected" else ""}"
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
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (isSelected) 40.sp else 34.sp,
                ),
                color = when {
                    isSelected -> accentColor
                    isRejected -> HelldeckColors.colorMuted
                    else -> HelldeckColors.colorOnDark
                },
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (isSelected) 24.sp else 22.sp,
                    letterSpacing = 2.sp,
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
