package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.delay

/**
 * Quick Reaction Bar for REVEAL phase - Opt-in feedback system.
 * 
 * Design Philosophy:
 * - Feedback is opt-in, not mandatory
 * - Any tap = feedback logged (🔥 = LOL, ⭐ = save, 👎 = TRASH)
 * - No tap = auto-advance after delay (implicit MEH)
 * - Maintains party flow while still training AI
 * 
 * Reactions:
 * - 🔥 Fire: "This was great!" → LOL feedback
 * - ⭐ Star: "Save this one!" → LOL + add to favorites
 * - 👎 Thumbs Down: "Bad/offensive" → TRASH feedback
 */

enum class QuickReaction(
    val emoji: String,
    val label: String,
    val color: Color,
) {
    FIRE("🔥", "Great!", HelldeckColors.Lol),
    STAR("⭐", "Save", HelldeckColors.colorAccentWarm),
    THUMBS_DOWN("👎", "Bad", HelldeckColors.Trash),
}

/**
 * Floating reaction bar that appears during REVEAL phase.
 * 
 * @param onReaction Callback when user taps a reaction
 * @param onAutoAdvance Callback when auto-advance timer expires (no reaction)
 * @param autoAdvanceDelayMs Delay before auto-advancing (0 = disabled)
 * @param showAutoAdvanceTimer Whether to show the countdown timer
 */
@Composable
fun QuickReactionBar(
    onReaction: (QuickReaction) -> Unit,
    onAutoAdvance: () -> Unit,
    modifier: Modifier = Modifier,
    autoAdvanceDelayMs: Long = 4000L,
    showAutoAdvanceTimer: Boolean = true,
    enabled: Boolean = true,
) {
    var selectedReaction by remember { mutableStateOf<QuickReaction?>(null) }
    var timeRemainingMs by remember { mutableStateOf(autoAdvanceDelayMs) }
    var hasReacted by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val reducedMotion = LocalReducedMotion.current

    // Auto-advance countdown
    LaunchedEffect(autoAdvanceDelayMs, hasReacted) {
        if (autoAdvanceDelayMs > 0 && !hasReacted && enabled) {
            timeRemainingMs = autoAdvanceDelayMs
            while (timeRemainingMs > 0 && !hasReacted) {
                delay(100)
                timeRemainingMs -= 100
            }
            if (!hasReacted) {
                onAutoAdvance()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
    ) {
        // Reaction buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HelldeckSpacing.Medium.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickReaction.entries.forEach { reaction ->
                ReactionButton(
                    reaction = reaction,
                    isSelected = selectedReaction == reaction,
                    enabled = enabled && !hasReacted,
                    reducedMotion = reducedMotion,
                    onClick = {
                        if (!hasReacted) {
                            hasReacted = true
                            selectedReaction = reaction
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReaction(reaction)
                        }
                    },
                )
            }
        }

        // Auto-advance timer indicator
        if (showAutoAdvanceTimer && autoAdvanceDelayMs > 0 && !hasReacted && enabled) {
            AutoAdvanceIndicator(
                progress = timeRemainingMs.toFloat() / autoAdvanceDelayMs.toFloat(),
                secondsRemaining = (timeRemainingMs / 1000).toInt() + 1,
                reducedMotion = reducedMotion,
            )
        }

        // Status text
        Text(
            text = when {
                hasReacted && selectedReaction != null -> when (selectedReaction) {
                    QuickReaction.FIRE -> "🔥 Noted! Moving on..."
                    QuickReaction.STAR -> "⭐ Saved to favorites!"
                    QuickReaction.THUMBS_DOWN -> "👎 Got it, less like this"
                    else -> "Moving on..."
                }
                !enabled -> "Rate this card above"
                else -> "React or auto-advance in ${(timeRemainingMs / 1000).toInt() + 1}s"
            },
            style = MaterialTheme.typography.labelMedium,
            color = HelldeckColors.colorMuted,
        )
    }
}

@Composable
private fun ReactionButton(
    reaction: QuickReaction,
    isSelected: Boolean,
    enabled: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.2f
            !enabled -> 0.9f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "reaction_scale",
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            !enabled && !isSelected -> 0.4f
            else -> 1f
        },
        animationSpec = tween(200),
        label = "reaction_alpha",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 12.dp else 4.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = if (isSelected) reaction.color.copy(alpha = 0.6f) else Color.Transparent,
            ),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = if (isSelected) {
            reaction.color.copy(alpha = 0.2f)
        } else {
            HelldeckColors.surfaceElevated
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, reaction.color)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, HelldeckColors.colorMuted.copy(alpha = 0.3f))
        },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = reaction.emoji,
                fontSize = 32.sp,
                modifier = Modifier.alpha(alpha),
            )
            Text(
                text = reaction.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) reaction.color else HelldeckColors.colorMuted,
                modifier = Modifier.alpha(alpha),
            )
        }
    }
}

@Composable
private fun AutoAdvanceIndicator(
    progress: Float,
    secondsRemaining: Int,
    reducedMotion: Boolean,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reducedMotion) tween(0) else tween(100),
        label = "progress_animation",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HelldeckSpacing.ExtraLarge.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    color = HelldeckColors.surfaceElevated,
                    shape = RoundedCornerShape(2.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        color = HelldeckColors.colorMuted.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

/**
 * Compact reaction strip for inline use.
 */
@Composable
fun QuickReactionStrip(
    onReaction: (QuickReaction) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    var selectedReaction by remember { mutableStateOf<QuickReaction?>(null) }

    Row(
        modifier = modifier
            .background(
                color = HelldeckColors.surfaceElevated.copy(alpha = 0.8f),
                shape = RoundedCornerShape(HelldeckRadius.Pill),
            )
            .padding(horizontal = HelldeckSpacing.Medium.dp, vertical = HelldeckSpacing.Small.dp),
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Quick:",
            style = MaterialTheme.typography.labelSmall,
            color = HelldeckColors.colorMuted,
        )

        QuickReaction.entries.forEach { reaction ->
            val isSelected = selectedReaction == reaction

            Surface(
                onClick = {
                    if (enabled && selectedReaction == null) {
                        selectedReaction = reaction
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReaction(reaction)
                    }
                },
                enabled = enabled && selectedReaction == null,
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = if (isSelected) reaction.color.copy(alpha = 0.3f) else Color.Transparent,
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, reaction.color)
                } else {
                    null
                },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = reaction.emoji,
                        fontSize = 20.sp,
                        modifier = Modifier.alpha(if (enabled || isSelected) 1f else 0.4f),
                    )
                }
            }
        }
    }
}
