package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
 * Renders SMASH/PASS interaction.
 * Used for Red Flag Rally game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Tinder-style dating app vibe
 * - Big glowing SMASH button (lime/green) and PASS button (red/magenta)
 * - Satisfying press animation with spring physics
 * - Card-tilt effect on selection
 * - Uses GlowButton from design system for satisfying press
 *
 * @ai_prompt Tinder-style swipe vibe, hot/cold colors, HELLDECK neon styling
 */
@Composable
fun SmashPassRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var selected by remember { mutableStateOf<String?>(null) }

    // Floating heart animation for selection
    val infiniteTransition = rememberInfiniteTransition(label = "smash_pass_pulse")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 8f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "float_offset",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.ExtraLarge.dp)
            .semantics { contentDescription = "Smash or Pass decision" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Stakes label with dating vibe
        NeonCard(
            accentColor = HelldeckColors.colorPrimary,
            elevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SWIPE YOUR FATE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = HelldeckColors.colorPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
        ) {
            SmashPassCard(
                label = "SMASH",
                emoji = "🔥",
                isSelected = selected == "SMASH",
                isRejected = selected != null && selected != "SMASH",
                accentColor = HelldeckColors.colorSecondary, // Lime green for SMASH
                rotation = if (selected == "SMASH" && !reducedMotion) -5f else 0f,
                floatOffset = if (selected == "SMASH") floatOffset else 0f,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = "SMASH"
                    onEvent(RoundEvent.PickAB("A"))
                },
                modifier = Modifier.weight(1f),
            )

            SmashPassCard(
                label = "PASS",
                emoji = "❄️",
                isSelected = selected == "PASS",
                isRejected = selected != null && selected != "PASS",
                accentColor = HelldeckColors.colorPrimary, // Magenta-red for PASS
                rotation = if (selected == "PASS" && !reducedMotion) 5f else 0f,
                floatOffset = if (selected == "PASS") floatOffset else 0f,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = "PASS"
                    onEvent(RoundEvent.PickAB("B"))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual SMASH/PASS card with Tinder-style styling using NeonCard.
 *
 * @ai_prompt Spring physics, card-tilt effect, dating app vibe, dim on rejection
 */
@Composable
private fun SmashPassCard(
    label: String,
    emoji: String,
    isSelected: Boolean,
    isRejected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    rotation: Float,
    floatOffset: Float,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Spring physics for selection with card-tilt effect
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.12f
            isRejected -> 0.9f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = 0.5f,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "smash_pass_scale",
    )

    // Animated rotation for swipe feel
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
        },
        label = "card_rotation",
    )

    // Dim rejected card
    val cardAlpha by animateFloatAsState(
        targetValue = if (isRejected) 0.35f else 1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "card_alpha",
    )

    // Pulsing glow for selected state
    val infiniteTransition = rememberInfiniteTransition(label = "sp_selection_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(700, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "selected_pulse",
    )

    val effectiveElevation = when {
        isSelected -> 20.dp * selectedPulse
        isRejected -> 2.dp
        else -> 8.dp
    }

    NeonCard(
        modifier = modifier
            .height(160.dp)
            .offset(y = (-floatOffset).dp)
            .scale(scale)
            .rotate(animatedRotation)
            .alpha(cardAlpha)
            .semantics {
                contentDescription = "$label${if (isSelected) ", selected" else if (isRejected) ", rejected" else ""}"
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
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (isSelected) 52.sp else 42.sp,
                ),
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (isSelected) 26.sp else 22.sp,
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
