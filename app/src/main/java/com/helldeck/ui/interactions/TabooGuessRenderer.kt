package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders TABOO_GUESS interaction for Taboo Timer game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Forbidden words display with danger styling
 * - Urgent guess input
 * - Timer-driven pressure feel
 * - Clear visual hierarchy
 *
 * @ai_prompt Taboo word guessing with forbidden words display, HELLDECK neon styling
 */
@Composable
fun TabooGuessRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var guess by remember { mutableStateOf("") }

    // Pulsing effect for urgency
    val infiniteTransition = rememberInfiniteTransition(label = "taboo_pulse")
    val urgencyPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion) 1f else 1.02f,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Taboo header
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.colorAccentWarm.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, HelldeckColors.colorAccentWarm.copy(alpha = 0.6f)),
            modifier = Modifier.scale(urgencyPulse),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "🤐", fontSize = 24.sp)
                Text(
                    text = "GUESS THE WORD!",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorAccentWarm,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Instruction text
        Text(
            text = "Listen to the clues and guess!",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
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
                    text = "🎯 Your guess...",
                    color = HelldeckColors.colorMuted,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(HelldeckHeights.Input.dp + 8.dp),
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

        // Submit button
        TabooSubmitButton(
            label = "🎉 SUBMIT GUESS",
            isEnabled = guess.isNotBlank(),
            reducedMotion = reducedMotion,
            onClick = {
                onEvent(RoundEvent.SubmitTabooGuess(guess))
                guess = ""
            },
        )
    }
}

@Composable
private fun TabooSubmitButton(
    label: String,
    isEnabled: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (!isEnabled) 1f else if (isPressed) 0.95f else 1f,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "button_scale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                spotColor = HelldeckColors.colorSecondary.copy(alpha = if (isEnabled) 0.5f else 0.1f),
            ),
        enabled = isEnabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = HelldeckColors.colorSecondary,
            contentColor = HelldeckColors.background,
            disabledContainerColor = HelldeckColors.colorMuted.copy(alpha = 0.3f),
            disabledContentColor = HelldeckColors.colorMuted,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        )
    }
}
