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
 * Renders PREDICT_VOTE interaction for Mob Mentality game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Crystal ball / fortune teller vibe
 * - Prediction NeonCards with neon gradient glow
 * - Selected prediction glows, other dims
 * - Mystical purple accent
 *
 * @ai_prompt Fortune teller prediction betting interface, HELLDECK neon styling
 */
@Composable
fun PredictVoteRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var prediction by remember { mutableStateOf<String?>(null) }

    // Mystical pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "mystical_pulse")
    val mysticalPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "mystical_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.ExtraLarge.dp)
            .semantics { contentDescription = "Predict what the majority will choose" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Fortune teller header - NeonCard with mystical pulse
        NeonCard(
            accentColor = HelldeckColors.colorPrimary,
            elevation = (10.dp * mysticalPulse),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "PREDICT THE MOB",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "What will the majority choose?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        val (optA, optB) = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.AB -> opts.optionA to opts.optionB
            is com.helldeck.content.model.GameOptions.PredictVote -> opts.optionA to opts.optionB
            else -> "A" to "B"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            PredictCard(
                label = optA,
                isSelected = prediction == "A",
                isRejected = prediction != null && prediction != "A",
                accentColor = HelldeckColors.colorSecondary,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    prediction = "A"
                    onEvent(RoundEvent.PreChoice("A"))
                },
                modifier = Modifier.weight(1f),
            )

            PredictCard(
                label = optB,
                isSelected = prediction == "B",
                isRejected = prediction != null && prediction != "B",
                accentColor = HelldeckColors.colorAccentCool,
                reducedMotion = reducedMotion,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    prediction = "B"
                    onEvent(RoundEvent.PreChoice("B"))
                },
                modifier = Modifier.weight(1f),
            )
        }

        // Prediction confirmation
        if (prediction != null) {
            NeonCard(
                accentColor = HelldeckColors.colorPrimary,
                elevation = 4.dp,
            ) {
                Text(
                    text = "Bet placed!",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = HelldeckColors.colorPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Individual prediction card using NeonCard.
 * Selected prediction glows, rejected dims.
 */
@Composable
private fun PredictCard(
    label: String,
    isSelected: Boolean,
    isRejected: Boolean,
    accentColor: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.06f
            isRejected -> 0.94f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "predict_scale",
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isRejected) 0.4f else 1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "card_alpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "predict_pulse")
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
        isSelected -> 14.dp * selectedPulse
        isRejected -> 2.dp
        else -> 6.dp
    }

    NeonCard(
        modifier = modifier
            .heightIn(min = 56.dp)
            .height(110.dp)
            .scale(scale)
            .alpha(cardAlpha)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 20.dp * selectedPulse,
                        shape = RoundedCornerShape(HelldeckRadius.Large),
                        spotColor = accentColor.copy(alpha = 0.8f),
                        ambientColor = accentColor.copy(alpha = 0.4f),
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription = "$label prediction${if (isSelected) ", selected" else ""}"
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
            // Neon gradient indicator for selected
            if (isSelected) {
                Text(
                    text = "LOCKED IN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                    color = accentColor,
                )
                Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            }
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
                maxLines = 2,
            )
        }
    }
}
