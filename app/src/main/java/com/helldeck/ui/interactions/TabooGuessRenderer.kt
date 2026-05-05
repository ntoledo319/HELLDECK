package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders TABOO_GUESS interaction for Taboo Timer game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Timer visually dramatic (pulsing red as time runs out)
 * - Forbidden words in glowing red NeonCards
 * - Urgent guess input
 * - Uses NeonCard for header, GlowButton for submit
 *
 * @ai_prompt Taboo word guessing with dramatic urgency, HELLDECK neon styling
 */
@Composable
fun TabooGuessRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var guess by remember { mutableStateOf("") }

    // Pulsing effect for urgency
    val infiniteTransition = rememberInfiniteTransition(label = "taboo_pulse")
    val urgencyPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion) 1f else 1.03f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(600, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "urgency_pulse",
    )

    // Timer danger color pulsing
    val timerColorPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "timer_color_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp)
            .semantics { contentDescription = "Taboo word guessing game" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Taboo header with urgency pulse
        NeonCard(
            accentColor = HelldeckColors.Error,
            elevation = (8.dp * timerColorPulse),
            modifier = Modifier.scale(urgencyPulse),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "GUESS THE WORD!",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.Error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Instruction text
        Text(
            text = "Listen to the clues and guess!",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Guess input with HELLDECK styling
        OutlinedTextField(
            value = guess,
            onValueChange = { guess = it },
            label = {
                Text(
                    text = "Your guess...",
                    color = HelldeckColors.colorMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(HelldeckHeights.Input.dp + 8.dp)
                .semantics { contentDescription = "Type your guess here" },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HelldeckColors.colorSecondary,
                unfocusedBorderColor = HelldeckColors.colorMuted.copy(alpha = 0.5f),
                focusedTextColor = HelldeckColors.colorOnDark,
                unfocusedTextColor = HelldeckColors.colorOnDark,
                cursorColor = HelldeckColors.colorSecondary,
                focusedContainerColor = HelldeckColors.surfacePrimary,
                unfocusedContainerColor = HelldeckColors.surfacePrimary.copy(alpha = 0.5f),
                focusedLabelColor = HelldeckColors.colorSecondary,
                unfocusedLabelColor = HelldeckColors.colorMuted,
            ),
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Submit button - uses GlowButton
        GlowButton(
            text = "SUBMIT GUESS",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEvent(RoundEvent.SubmitTabooGuess(guess))
                guess = ""
            },
            enabled = guess.isNotBlank(),
            accentColor = HelldeckColors.colorSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Submit guess button" },
        )
    }
}
