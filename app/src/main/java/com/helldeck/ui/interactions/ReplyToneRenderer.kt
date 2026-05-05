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
 * Renders REPLY_TONE interaction for Text Thread Trap game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Chat bubble style message display
 * - Tone options as glowing NeonCard chips
 * - Selected tone glows bright, others dim
 * - Spring physics on selection
 *
 * @ai_prompt Message bubble tone picker with texting vibe, HELLDECK neon styling
 */
@Composable
fun ReplyToneRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var selectedTone by remember { mutableStateOf<String?>(null) }

    val tones = when (val opts = roundState.options) {
        is com.helldeck.content.model.GameOptions.ReplyTone -> opts.tones
        else -> listOf("Deadpan", "Feral", "Chaotic", "Wholesome")
    }

    // Map tones to emojis and colors
    val toneData = mapOf(
        "Deadpan" to Pair("😐", HelldeckColors.colorMuted),
        "Feral" to Pair("🐺", HelldeckColors.colorPrimary),
        "Chaotic" to Pair("🌪️", HelldeckColors.colorAccentWarm),
        "Wholesome" to Pair("💖", HelldeckColors.colorSecondary),
        "Funny" to Pair("😂", HelldeckColors.Lol),
        "Serious" to Pair("😤", HelldeckColors.Error),
        "Sarcastic" to Pair("🙄", HelldeckColors.colorAccentCool),
        "Kind" to Pair("🥰", HelldeckColors.colorSecondary),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp)
            .semantics { contentDescription = "Pick a reply tone" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with chat bubble NeonCard
        NeonCard(
            accentColor = HelldeckColors.colorAccentCool,
            elevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PICK YOUR VIBE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp,
                    ),
                    color = HelldeckColors.colorAccentCool,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Instruction
        Text(
            text = "How should you reply?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Tone buttons in a 2x2 grid using NeonCard
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            tones.chunked(2).forEach { rowTones ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                ) {
                    rowTones.forEach { tone ->
                        val (emoji, color) = toneData[tone] ?: Pair("📝", HelldeckColors.colorMuted)
                        ToneChip(
                            tone = tone,
                            emoji = emoji,
                            isSelected = selectedTone == tone,
                            isRejected = selectedTone != null && selectedTone != tone,
                            accentColor = color,
                            reducedMotion = reducedMotion,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedTone = tone
                                onEvent(RoundEvent.SelectOption(tone))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Fill empty space if odd number of tones
                    if (rowTones.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Individual tone chip using NeonCard for consistent styling.
 * Selected chip glows bright, others dim when a selection is made.
 */
@Composable
private fun ToneChip(
    tone: String,
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
            isSelected -> 1.06f
            isRejected -> 0.95f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "tone_scale",
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (isRejected) 0.45f else 1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "card_alpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tone_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "selected_pulse",
    )

    val effectiveElevation = when {
        isSelected -> 12.dp * selectedPulse
        isRejected -> 2.dp
        else -> 4.dp
    }

    NeonCard(
        modifier = modifier
            .height(90.dp)
            .scale(scale)
            .alpha(cardAlpha)
            .semantics {
                contentDescription = "$tone tone${if (isSelected) ", selected" else ""}"
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
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (isSelected) 32.sp else 28.sp,
                ),
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            Text(
                text = tone.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp,
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
