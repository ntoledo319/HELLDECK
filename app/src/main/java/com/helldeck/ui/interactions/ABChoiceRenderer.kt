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
 * Renders A/B choice interaction (A_B_CHOICE).
 * Used for binary decision games like Poison Pitch.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Two big glowing NeonCards side by side
 * - Selected card glows bright, rejected card dims
 * - Spring physics on selection
 * - Neon glow with accent colors
 * - 60dp+ touch targets
 *
 * @ai_prompt A/B choice with split-screen debate feel, HELLDECK neon styling
 */
@Composable
fun ABChoiceRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.ExtraLarge.dp)
            .semantics { contentDescription = "Choose between two options" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Stakes label
        NeonCard(
            accentColor = HelldeckColors.colorAccentWarm,
            elevation = 4.dp,
        ) {
            Text(
                text = "PICK YOUR SIDE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = HelldeckColors.colorAccentWarm,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Get option labels
        val (optA, optB) = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.AB -> opts.optionA to opts.optionB
            else -> "A" to "B"
        }

        // VS indicator with pulse
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

        Text(
            text = "VS",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
            ),
            color = HelldeckColors.colorAccentWarm,
            modifier = Modifier.scale(if (reducedMotion) 1f else vsPulse),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            ABChoiceCard(
                label = optA,
                isSelected = selected == "A",
                isRejected = selected != null && selected != "A",
                accentColor = HelldeckColors.colorPrimary,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = "A"
                    onEvent(RoundEvent.PickAB("A"))
                },
                modifier = Modifier.weight(1f),
            )

            ABChoiceCard(
                label = optB,
                isSelected = selected == "B",
                isRejected = selected != null && selected != "B",
                accentColor = HelldeckColors.colorAccentCool,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = "B"
                    onEvent(RoundEvent.PickAB("B"))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual A/B choice card using NeonCard.
 * Selected card glows bright, rejected card dims out.
 *
 * @ai_prompt Spring physics, glow on selection, dim on rejection
 */
@Composable
private fun ABChoiceCard(
    label: String,
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
            isSelected -> 1.06f
            isRejected -> 0.94f
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
        label = "ab_card_scale",
    )

    // Dim rejected card
    val cardAlpha by animateFloatAsState(
        targetValue = if (isRejected) 0.4f else 1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "card_alpha",
    )

    // Pulsing glow for selected state
    val infiniteTransition = rememberInfiniteTransition(label = "ab_selection_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "selected_pulse",
    )

    val effectiveElevation = when {
        isSelected -> 16.dp * selectedPulse
        isRejected -> 2.dp
        else -> 6.dp
    }

    NeonCard(
        modifier = modifier
            .height(120.dp)
            .scale(scale)
            .alpha(cardAlpha)
            .semantics {
                contentDescription = "$label${if (isSelected) ", selected" else if (isRejected) ", rejected" else ""}"
            },
        accentColor = if (isRejected) HelldeckColors.colorMuted else accentColor,
        elevation = effectiveElevation,
        onClick = onClick,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isSelected) 24.sp else 22.sp,
                ),
                color = when {
                    isSelected -> accentColor
                    isRejected -> HelldeckColors.colorMuted
                    else -> HelldeckColors.colorOnDark
                },
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}
