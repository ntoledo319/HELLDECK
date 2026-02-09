package com.helldeck.ui.interactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders TRUE/FALSE interaction.
 * Used for Confession or Cap game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Lie detector vibe with dramatic TRUE/FALSE buttons
 * - Green (truth) vs Red (lie) color coding
 * - Spring physics on selection
 * - Pulsing "scanning" effect
 *
 * @ai_prompt Lie detector vibe with green/red dramatic buttons, HELLDECK neon styling
 */
@Composable
fun TrueFalseRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
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
            .padding(HelldeckSpacing.ExtraLarge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Lie detector header with scanning effect
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.colorAccentCool.copy(alpha = scanPulse * 0.3f),
            border = BorderStroke(2.dp, HelldeckColors.colorAccentCool.copy(alpha = scanPulse)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "🔍",
                    fontSize = 20.sp,
                )
                Text(
                    text = "LIE DETECTOR ACTIVE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorAccentCool,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Question indicator
        Text(
            text = "Is this statement...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = HelldeckColors.colorMuted,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
        ) {
            TrueFalseButton(
                label = "TRUE",
                emoji = "✓",
                isSelected = selected == "T",
                accentColor = HelldeckColors.colorSecondary, // Lime green for truth
                reducedMotion = reducedMotion,
                onClick = {
                    selected = "T"
                    onEvent(RoundEvent.PickAB("T"))
                },
                modifier = Modifier.weight(1f),
            )

            TrueFalseButton(
                label = "FALSE",
                emoji = "✗",
                isSelected = selected == "F",
                accentColor = HelldeckColors.Error, // Red for false/lie
                reducedMotion = reducedMotion,
                onClick = {
                    selected = "F"
                    onEvent(RoundEvent.PickAB("F"))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual TRUE/FALSE button with lie detector styling.
 *
 * @ai_prompt Spring physics, dramatic glow, lie detector vibe
 */
@Composable
private fun TrueFalseButton(
    label: String,
    emoji: String,
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
            isSelected && isPressed -> 0.92f
            isSelected -> 1.08f
            isPressed -> 0.95f
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
        label = "tf_button_scale",
    )

    // Glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.8f else if (isPressed) 0.4f else 0.2f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    // Background color
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = 0.3f)
        } else {
            HelldeckColors.surfacePrimary
        },
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "background_color",
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

    Button(
        onClick = onClick,
        modifier = modifier
            .height(120.dp) // Tall for dramatic effect
            .scale(scale)
            .shadow(
                elevation = if (isSelected) (16.dp * selectedPulse) else if (isPressed) 8.dp else 4.dp,
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
            width = if (isSelected) 4.dp else 2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isSelected) 1f else 0.5f),
                    accentColor.copy(alpha = if (isSelected) 0.6f else 0.3f),
                ),
            ),
        ),
        contentPadding = PaddingValues(HelldeckSpacing.Medium.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (isSelected) 36.sp else 32.sp,
                ),
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (isSelected) 22.sp else 20.sp,
                    letterSpacing = 2.sp,
                ),
                color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
            )
        }
    }
}
