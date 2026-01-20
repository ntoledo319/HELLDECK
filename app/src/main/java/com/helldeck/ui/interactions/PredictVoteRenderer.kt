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
import androidx.compose.ui.graphics.Color
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
 * Renders PREDICT_VOTE interaction for Mob Mentality game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Crystal ball / fortune teller vibe
 * - Betting/prediction feel with risk indicator
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
            .padding(HelldeckSpacing.ExtraLarge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Fortune teller header
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Large),
            color = HelldeckColors.colorPrimary.copy(alpha = 0.1f * mysticalPulse),
            border = BorderStroke(2.dp, HelldeckColors.colorPrimary.copy(alpha = 0.5f * mysticalPulse)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(text = "🔮", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PREDICT THE MOB",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorPrimary,
                )
            }
        }

        Text(
            text = "What will the majority choose?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
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
            PredictButton(
                label = optA,
                isSelected = prediction == "A",
                accentColor = HelldeckColors.colorSecondary,
                reducedMotion = reducedMotion,
                onClick = {
                    prediction = "A"
                    onEvent(RoundEvent.PreChoice("A"))
                },
                modifier = Modifier.weight(1f),
            )

            PredictButton(
                label = optB,
                isSelected = prediction == "B",
                accentColor = HelldeckColors.colorAccentCool,
                reducedMotion = reducedMotion,
                onClick = {
                    prediction = "B"
                    onEvent(RoundEvent.PreChoice("B"))
                },
                modifier = Modifier.weight(1f),
            )
        }

        // Prediction confirmation
        if (prediction != null) {
            Surface(
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                color = HelldeckColors.colorPrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, HelldeckColors.colorPrimary.copy(alpha = 0.4f)),
            ) {
                Text(
                    text = "🎲 Bet placed!",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = HelldeckColors.colorPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PredictButton(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

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
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "predict_scale",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.7f else if (isPressed) 0.3f else 0.15f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.25f) else HelldeckColors.surfacePrimary,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "background_color",
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

    Button(
        onClick = onClick,
        modifier = modifier
            .height(100.dp)
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
