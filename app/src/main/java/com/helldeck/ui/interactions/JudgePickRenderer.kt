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
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders JUDGE_PICK interaction for Fill-In Finisher game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Judge spotlight feel with dramatic crown/gavel imagery
 * - Cards fan out, selected card rises with glow
 * - Spotlight effect on judge's choice
 * - Uses NeonCard for option cards, GlowButton for verdict
 *
 * @ai_prompt Judge selection with spotlight/authority vibe, HELLDECK neon styling
 */
@Composable
fun JudgePickRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var selectedOption by remember { mutableStateOf<Int?>(null) }

    // Get options from round state
    val options: List<String> = when (val opts = roundState.options) {
        is com.helldeck.content.model.GameOptions.AB -> listOf(opts.optionA, opts.optionB)
        else -> listOf("Option A", "Option B")
    }

    // Spotlight pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "judge_spotlight")
    val spotlightPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "spotlight_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.ExtraLarge.dp)
            .semantics { contentDescription = "Judge picks the winning answer" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Judge header with spotlight effect
        NeonCard(
            accentColor = HelldeckColors.Lol,
            elevation = (8.dp * spotlightPulse),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "THE JUDGE DECIDES",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.Lol,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Instruction
        Text(
            text = "Pick the winning answer:",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Option cards - fan out with NeonCard
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedOption == index
                val isRejected = selectedOption != null && selectedOption != index
                val optionAccent = if (index == 0) HelldeckColors.colorPrimary else HelldeckColors.colorAccentCool

                JudgeOptionCard(
                    label = option,
                    optionNumber = index + 1,
                    isSelected = isSelected,
                    isRejected = isRejected,
                    accentColor = optionAccent,
                    reducedMotion = reducedMotion,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedOption = index
                        onEvent(RoundEvent.SelectOption(option))
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Continue button (only shown when selection made) - uses GlowButton
        if (selectedOption != null) {
            GlowButton(
                text = "DELIVER VERDICT",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(RoundEvent.AdvancePhase)
                },
                accentColor = HelldeckColors.colorSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Deliver verdict button" },
            )
        }
    }
}

@Composable
private fun JudgeOptionCard(
    label: String,
    optionNumber: Int,
    isSelected: Boolean,
    isRejected: Boolean,
    accentColor: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Spring physics: selected card rises up, rejected dims
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.04f
            isRejected -> 0.96f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "option_scale",
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isRejected) 0.4f else 1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "card_alpha",
    )

    val effectiveElevation = when {
        isSelected -> 16.dp
        isRejected -> 2.dp
        else -> 6.dp
    }

    NeonCard(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(scale)
            .alpha(cardAlpha)
            .semantics {
                contentDescription = "Option $optionNumber: $label${if (isSelected) ", selected as winner" else ""}"
            },
        accentColor = if (isRejected) HelldeckColors.colorMuted else accentColor,
        elevation = effectiveElevation,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Option number badge
            Surface(
                shape = RoundedCornerShape(HelldeckRadius.Small),
                color = accentColor.copy(alpha = if (isSelected) 0.8f else 0.3f),
            ) {
                Text(
                    text = "#$optionNumber",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = if (isSelected) HelldeckColors.background else accentColor,
                    modifier = Modifier.padding(horizontal = HelldeckSpacing.Small.dp, vertical = HelldeckSpacing.Tiny.dp),
                )
            }

            // Option text
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 20.sp,
                ),
                color = when {
                    isSelected -> accentColor
                    isRejected -> HelldeckColors.colorMuted
                    else -> HelldeckColors.colorOnDark
                },
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )

            // Winner trophy
            if (isSelected) {
                Text(
                    text = "🏆",
                    fontSize = 28.sp,
                )
            }
        }
    }
}
