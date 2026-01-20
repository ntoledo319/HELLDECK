package com.helldeck.ui.interactions

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
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
 * Renders A/B choice interaction (A_B_CHOICE).
 * Used for binary decision games like Poison Pitch.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Split screen debate feel
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
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.ExtraLarge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Stakes label
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.colorAccentWarm.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, HelldeckColors.colorAccentWarm.copy(alpha = 0.4f)),
        ) {
            Text(
                text = "⚔️ PICK YOUR SIDE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = HelldeckColors.colorAccentWarm,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Get option labels
        val (optA, optB) = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.AB -> opts.optionA to opts.optionB
            else -> "A" to "B"
        }

        // VS indicator
        Text(
            text = "VS",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
            ),
            color = HelldeckColors.colorMuted,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            ABChoiceButton(
                label = optA,
                isSelected = selected == "A",
                accentColor = HelldeckColors.colorPrimary, // Magenta for A
                reducedMotion = reducedMotion,
                onClick = {
                    selected = "A"
                    onEvent(RoundEvent.PickAB("A"))
                },
                modifier = Modifier.weight(1f),
            )

            ABChoiceButton(
                label = optB,
                isSelected = selected == "B",
                accentColor = HelldeckColors.colorAccentCool, // Cyan for B
                reducedMotion = reducedMotion,
                onClick = {
                    selected = "B"
                    onEvent(RoundEvent.PickAB("B"))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual A/B choice button with HELLDECK styling.
 *
 * @ai_prompt Spring physics, glow on selection, split-screen debate vibe
 */
@Composable
private fun ABChoiceButton(
    label: String,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring physics for selection
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected && isPressed -> 0.93f
            isSelected -> 1.05f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessHigh,
            )
        },
        label = "ab_button_scale",
    )

    // Glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.7f else if (isPressed) 0.3f else 0.15f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    // Background color
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = 0.25f)
        } else {
            HelldeckColors.surfacePrimary
        },
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "background_color",
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

    Button(
        onClick = onClick,
        modifier = modifier
            .height(100.dp) // Taller for dramatic effect
            .scale(scale)
            .shadow(
                elevation = if (isSelected) (12.dp * selectedPulse) else if (isPressed) 6.dp else 4.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse else glowAlpha),
                ambientColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse * 0.5f else glowAlpha * 0.5f),
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Large),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = HelldeckColors.colorOnDark,
        ),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isSelected) 1f else 0.5f),
                    accentColor.copy(alpha = if (isSelected) 0.7f else 0.3f),
                ),
            ),
        ),
        contentPadding = PaddingValues(HelldeckSpacing.Large.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (isSelected) 24.sp else 22.sp,
            ),
            color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
