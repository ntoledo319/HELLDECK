package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.engine.GameIds
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.delay

/**
 * Game Action Hints - "Idiot-proof" one-liner instructions per game.
 * 
 * Shows a prominent, verb-first instruction at the start of INPUT phase.
 * Auto-hides after 3 seconds. Smaller/dimmer on repeat plays of same game type.
 * 
 * Design: Answer "What do I physically DO?" in under 5 seconds.
 */

data class ActionHint(
    val emoji: String,
    val instruction: String,
    val subtext: String? = null,
)

/**
 * Get the action hint for a specific game.
 * Returns a verb-first, action-oriented instruction.
 */
fun getActionHintForGame(gameId: String): ActionHint {
    return when (gameId) {
        GameIds.ROAST_CONS -> ActionHint(
            emoji = "👆",
            instruction = "Tap who fits best",
            subtext = "Everyone votes secretly, then reveal!",
        )
        GameIds.CONFESS_CAP -> ActionHint(
            emoji = "🤔",
            instruction = "TRUE or FALSE?",
            subtext = "Speaker: pick secretly. Others: guess!",
        )
        GameIds.POISON_PITCH -> ActionHint(
            emoji = "⚖️",
            instruction = "Pick your poison: A or B",
            subtext = "Listen to both pitches, then vote",
        )
        GameIds.FILLIN -> ActionHint(
            emoji = "✍️",
            instruction = "Write your punchline",
            subtext = "Judge picks their favorite answer",
        )
        GameIds.RED_FLAG -> ActionHint(
            emoji = "💚",
            instruction = "SMASH or PASS?",
            subtext = "Would you date them despite the red flag?",
        )
        GameIds.HOTSEAT_IMP -> ActionHint(
            emoji = "🎭",
            instruction = "Spot the imposter",
            subtext = "One player is faking—find them!",
        )
        GameIds.TEXT_TRAP -> ActionHint(
            emoji = "📱",
            instruction = "Reply in the right tone",
            subtext = "Match the vibe shown on screen",
        )
        GameIds.TABOO -> ActionHint(
            emoji = "🚫",
            instruction = "Describe WITHOUT forbidden words",
            subtext = "Tap START when ready, shout guesses!",
        )
        GameIds.UNIFYING_THEORY -> ActionHint(
            emoji = "🔗",
            instruction = "Connect all three items",
            subtext = "Find ONE thing they have in common",
        )
        GameIds.TITLE_FIGHT -> ActionHint(
            emoji = "🥊",
            instruction = "FIGHT! First to mess up loses",
            subtext = "Point at someone and battle!",
        )
        GameIds.ALIBI -> ActionHint(
            emoji = "🕵️",
            instruction = "Hide the secret words",
            subtext = "Weave them into your alibi naturally",
        )
        GameIds.REALITY_CHECK -> ActionHint(
            emoji = "📊",
            instruction = "Rate 1-10, match the group",
            subtext = "How self-aware is the subject?",
        )
        GameIds.SCATTER -> ActionHint(
            emoji = "💣",
            instruction = "List fast before BOOM!",
            subtext = "Don't be holding the phone when it explodes",
        )
        GameIds.OVER_UNDER -> ActionHint(
            emoji = "📈",
            instruction = "Bet OVER or UNDER",
            subtext = "Will the real number beat the line?",
        )
        else -> ActionHint(
            emoji = "🎮",
            instruction = "Follow the prompt",
            subtext = "Tap Help if you're stuck",
        )
    }
}

/**
 * Animated action hint banner that appears during INPUT phase.
 * 
 * @param gameId The current game being played
 * @param isFirstTimePlayingGame Whether this is the user's first time playing this game type
 * @param autoHideDelayMs How long to show the hint before auto-hiding (0 = never auto-hide)
 * @param onDismiss Callback when hint is dismissed
 */
@Composable
fun GameActionHintBanner(
    gameId: String,
    modifier: Modifier = Modifier,
    isFirstTimePlayingGame: Boolean = true,
    autoHideDelayMs: Long = 4000L,
    onDismiss: (() -> Unit)? = null,
) {
    val hint = remember(gameId) { getActionHintForGame(gameId) }
    var isVisible by remember { mutableStateOf(true) }
    val reducedMotion = LocalReducedMotion.current

    // Auto-hide after delay
    LaunchedEffect(gameId, autoHideDelayMs) {
        if (autoHideDelayMs > 0) {
            delay(autoHideDelayMs)
            isVisible = false
            onDismiss?.invoke()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = if (reducedMotion) {
            fadeIn(animationSpec = tween(0))
        } else {
            fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
        },
        exit = if (reducedMotion) {
            fadeOut(animationSpec = tween(0))
        } else {
            fadeOut(animationSpec = tween(200)) +
                slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(200),
                )
        },
        modifier = modifier,
    ) {
        ActionHintCard(
            hint = hint,
            isProminent = isFirstTimePlayingGame,
            onTap = {
                isVisible = false
                onDismiss?.invoke()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionHintCard(
    hint: ActionHint,
    isProminent: Boolean,
    onTap: () -> Unit,
) {
    val backgroundColor = if (isProminent) {
        Brush.horizontalGradient(
            colors = listOf(
                HelldeckColors.colorPrimary.copy(alpha = 0.95f),
                HelldeckColors.colorSecondary.copy(alpha = 0.85f),
            ),
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                HelldeckColors.surfaceElevated.copy(alpha = 0.95f),
                HelldeckColors.surfacePrimary.copy(alpha = 0.9f),
            ),
        )
    }

    Surface(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isProminent) 12.dp else 4.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = HelldeckColors.colorPrimary.copy(alpha = 0.4f),
            ),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(
                    horizontal = HelldeckSpacing.Large.dp,
                    vertical = if (isProminent) HelldeckSpacing.Large.dp else HelldeckSpacing.Medium.dp,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Emoji
                Text(
                    text = hint.emoji,
                    fontSize = if (isProminent) 36.sp else 28.sp,
                    modifier = Modifier.padding(end = HelldeckSpacing.Medium.dp),
                )

                // Text content
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = hint.instruction,
                        style = if (isProminent) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (isProminent) {
                            HelldeckColors.background
                        } else {
                            HelldeckColors.colorOnDark
                        },
                    )

                    hint.subtext?.let { subtext ->
                        if (isProminent) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subtext,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isProminent) {
                                    HelldeckColors.background.copy(alpha = 0.85f)
                                } else {
                                    HelldeckColors.colorMuted
                                },
                            )
                        }
                    }
                }
            }

            // Tap to dismiss hint (subtle)
            if (isProminent) {
                Text(
                    text = "tap to dismiss",
                    style = MaterialTheme.typography.labelSmall,
                    color = HelldeckColors.background.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Compact inline hint for repeat plays - less intrusive.
 */
@Composable
fun GameActionHintInline(
    gameId: String,
    modifier: Modifier = Modifier,
) {
    val hint = remember(gameId) { getActionHintForGame(gameId) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = HelldeckColors.surfaceElevated.copy(alpha = 0.7f),
                shape = RoundedCornerShape(HelldeckRadius.Small),
            )
            .padding(horizontal = HelldeckSpacing.Medium.dp, vertical = HelldeckSpacing.Small.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${hint.emoji} ${hint.instruction}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = HelldeckColors.colorOnDark.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
    }
}
