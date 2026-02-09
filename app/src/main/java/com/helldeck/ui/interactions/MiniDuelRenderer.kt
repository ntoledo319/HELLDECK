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
 * Challenge types for Title Fight
 */
private enum class ChallengeType(val emoji: String, val displayName: String, val instruction: String) {
    BRAIN("🧠", "BRAIN", "Take turns naming items • Pause 3+ sec or repeat = LOSE"),
    BODY("💪", "BODY", "Race to complete the task • Second place = LOSE"),
    SOUL("👁️", "SOUL", "Endurance test • First to break = LOSE")
}

/**
 * Renders MINI_DUEL interaction for Title Fight game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Epic VS screen with dramatic head-to-head feel
 * - Red vs Blue fighter colors
 * - Spring physics with victory animations
 * - Champion crown effect on selection
 * - Contextual instructions based on challenge type
 *
 * @ai_prompt Epic VS battle screen, fighting game vibe, HELLDECK neon styling
 */
@Composable
fun MiniDuelRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var selectedWinner by remember { mutableStateOf<Int?>(null) }
    
    // Detect challenge type from card text
    val cardText = roundState.filledCard.text
    val challengeType = when {
        cardText.startsWith("Category:", ignoreCase = true) -> ChallengeType.BRAIN
        cardText.startsWith("Speed:", ignoreCase = true) -> ChallengeType.BODY
        cardText.startsWith("Guts:", ignoreCase = true) -> ChallengeType.SOUL
        else -> null
    }

    // VS pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "vs_pulse")
    val vsPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "vs_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Epic header
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.colorAccentWarm.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, HelldeckColors.colorAccentWarm.copy(alpha = 0.5f)),
        ) {
            Text(
                text = "⚔️ TITLE FIGHT ⚔️",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp,
                ),
                color = HelldeckColors.colorAccentWarm,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        
        // Contextual instructions based on challenge type
        if (challengeType != null) {
            Surface(
                shape = RoundedCornerShape(HelldeckRadius.Small),
                color = HelldeckColors.colorAccentCool.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, HelldeckColors.colorAccentCool.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HelldeckSpacing.Medium.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = challengeType.emoji,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = "${challengeType.displayName}: ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = HelldeckColors.colorAccentCool,
                    )
                    Text(
                        text = challengeType.instruction,
                        style = MaterialTheme.typography.labelMedium,
                        color = HelldeckColors.colorOnDark.copy(alpha = 0.9f),
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player 1 (Red corner)
            DuelFighterButton(
                label = "PLAYER 1",
                emoji = "🔴",
                cornerLabel = "RED",
                isSelected = selectedWinner == 0,
                accentColor = HelldeckColors.colorPrimary,
                reducedMotion = reducedMotion,
                onClick = {
                    selectedWinner = 0
                    onEvent(RoundEvent.DuelWinner(0))
                },
                modifier = Modifier.weight(1f),
            )

            // VS indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(if (reducedMotion) 1f else vsPulse),
            ) {
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                    ),
                    color = HelldeckColors.colorAccentWarm,
                )
            }

            // Player 2 (Blue corner)
            DuelFighterButton(
                label = "PLAYER 2",
                emoji = "🔵",
                cornerLabel = "BLUE",
                isSelected = selectedWinner == 1,
                accentColor = HelldeckColors.colorAccentCool,
                reducedMotion = reducedMotion,
                onClick = {
                    selectedWinner = 1
                    onEvent(RoundEvent.DuelWinner(1))
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Winner declaration
        if (selectedWinner != null) {
            val winnerColor = if (selectedWinner == 0) HelldeckColors.colorPrimary else HelldeckColors.colorAccentCool
            Surface(
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                color = winnerColor.copy(alpha = 0.2f),
                border = BorderStroke(2.dp, winnerColor),
            ) {
                Text(
                    text = "👑 WINNER: PLAYER ${selectedWinner!! + 1}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = winnerColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Individual fighter button with VS battle styling.
 */
@Composable
private fun DuelFighterButton(
    label: String,
    emoji: String,
    cornerLabel: String,
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
            isSelected && isPressed -> 0.92f
            isSelected -> 1.08f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh)
        },
        label = "fighter_scale",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.8f else if (isPressed) 0.4f else 0.2f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.3f) else HelldeckColors.surfacePrimary,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "background_color",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fighter_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(600, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "selected_pulse",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(130.dp)
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
            // Crown for winner
            if (isSelected) {
                Text(text = "👑", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (isSelected) 36.sp else 32.sp,
                ),
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            Text(
                text = cornerLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = accentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
            )
        }
    }
}
