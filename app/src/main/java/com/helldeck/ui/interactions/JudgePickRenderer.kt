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
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders JUDGE_PICK interaction for Fill-In Finisher game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Judge spotlight feel with dramatic crown/gavel imagery
 * - Option selection with clear visual feedback
 * - Authority/power vibe for the judge role
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
            .padding(HelldeckSpacing.ExtraLarge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Judge header with spotlight effect
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Large),
            color = HelldeckColors.Lol.copy(alpha = 0.15f * spotlightPulse),
            border = BorderStroke(2.dp, HelldeckColors.Lol.copy(alpha = 0.6f * spotlightPulse)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    text = "👨‍⚖️",
                    fontSize = 48.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "THE JUDGE DECIDES",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.Lol,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Instruction
        Text(
            text = "Pick the winning answer:",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Option buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            options.forEachIndexed { index, option ->
                JudgeOptionButton(
                    label = option,
                    optionNumber = index + 1,
                    isSelected = selectedOption == index,
                    accentColor = if (index == 0) HelldeckColors.colorPrimary else HelldeckColors.colorAccentCool,
                    reducedMotion = reducedMotion,
                    onClick = {
                        selectedOption = index
                        onEvent(RoundEvent.SelectOption(option))
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Continue button (only shown when selection made)
        if (selectedOption != null) {
            JudgeContinueButton(
                reducedMotion = reducedMotion,
                onClick = { onEvent(RoundEvent.AdvancePhase) },
            )
        }
    }
}

@Composable
private fun JudgeOptionButton(
    label: String,
    optionNumber: Int,
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
            isSelected -> 1.03f
            isPressed -> 0.97f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "option_scale",
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

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 12.dp else if (isPressed) 6.dp else 4.dp,
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                spotColor = accentColor.copy(alpha = glowAlpha),
                ambientColor = accentColor.copy(alpha = glowAlpha * 0.5f),
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = HelldeckColors.colorOnDark,
        ),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 2.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isSelected) 1f else 0.5f),
                    accentColor.copy(alpha = if (isSelected) 0.7f else 0.3f),
                ),
            ),
        ),
        contentPadding = PaddingValues(HelldeckSpacing.Large.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
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
                    ),
                    color = if (isSelected) HelldeckColors.background else accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Option text
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )

            // Winner indicator
            if (isSelected) {
                Text(text = "🏆", fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun JudgeContinueButton(
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "continue_scale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 8.dp,
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                spotColor = HelldeckColors.colorSecondary.copy(alpha = 0.5f),
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = HelldeckColors.colorSecondary,
            contentColor = HelldeckColors.background,
        ),
    ) {
        Text(
            text = "⚖️ DELIVER VERDICT",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        )
    }
}
