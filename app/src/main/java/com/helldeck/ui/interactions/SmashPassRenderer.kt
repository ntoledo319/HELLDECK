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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckAnimations
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders SMASH/PASS interaction.
 * Used for Red Flag Rally game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Tinder-style dating app vibe
 * - Hot pink (SMASH) vs Cool blue (PASS)
 * - Dramatic card-swipe feel
 * - Spring physics on selection
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
    var selected by remember { mutableStateOf<String?>(null) }

    // Floating heart animation for SMASH selection
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
            .padding(HelldeckSpacing.ExtraLarge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Stakes label with dating vibe
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            color = HelldeckColors.colorPrimary.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, HelldeckColors.colorPrimary.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "💘",
                    fontSize = 18.sp,
                )
                Text(
                    text = "SWIPE YOUR FATE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    color = HelldeckColors.colorPrimary,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
        ) {
            SmashPassButton(
                label = "SMASH",
                emoji = "🔥",
                isSelected = selected == "SMASH",
                accentColor = HelldeckColors.colorPrimary, // Hot magenta-red
                rotation = if (selected == "SMASH" && !reducedMotion) -5f else 0f,
                floatOffset = if (selected == "SMASH") floatOffset else 0f,
                reducedMotion = reducedMotion,
                onClick = {
                    selected = "SMASH"
                    onEvent(RoundEvent.PickAB("A"))
                },
                modifier = Modifier.weight(1f),
            )

            SmashPassButton(
                label = "PASS",
                emoji = "❄️",
                isSelected = selected == "PASS",
                accentColor = HelldeckColors.colorAccentCool, // Cool cyan
                rotation = if (selected == "PASS" && !reducedMotion) 5f else 0f,
                floatOffset = if (selected == "PASS") floatOffset else 0f,
                reducedMotion = reducedMotion,
                onClick = {
                    selected = "PASS"
                    onEvent(RoundEvent.PickAB("B"))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual SMASH/PASS button with Tinder-style styling.
 *
 * @ai_prompt Spring physics, card-tilt effect, dating app vibe
 */
@Composable
private fun SmashPassButton(
    label: String,
    emoji: String,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    rotation: Float,
    floatOffset: Float,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring physics for selection with card-tilt effect
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected && isPressed -> 0.9f
            isSelected -> 1.1f
            isPressed -> 0.95f
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

    // Glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.85f else if (isPressed) 0.4f else 0.2f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    // Background color
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = 0.35f)
        } else {
            HelldeckColors.surfacePrimary
        },
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "background_color",
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

    Button(
        onClick = onClick,
        modifier = modifier
            .height(140.dp) // Tall card-like button
            .offset(y = (-floatOffset).dp)
            .scale(scale)
            .rotate(animatedRotation)
            .shadow(
                elevation = if (isSelected) (20.dp * selectedPulse) else if (isPressed) 10.dp else 6.dp,
                shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
                spotColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse else glowAlpha),
                ambientColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse * 0.5f else glowAlpha * 0.5f),
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = HelldeckColors.colorOnDark,
        ),
        border = BorderStroke(
            width = if (isSelected) 4.dp else 2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isSelected) 1f else 0.5f),
                    accentColor.copy(alpha = if (isSelected) 0.5f else 0.25f),
                ),
            ),
        ),
        contentPadding = PaddingValues(HelldeckSpacing.Large.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (isSelected) 48.sp else 40.sp,
                ),
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (isSelected) 24.sp else 22.sp,
                ),
                color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
            )
        }
    }
}
