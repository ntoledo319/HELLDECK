package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders player voting interaction (VOTE_PLAYER).
 * Used for consensus voting games like Roast Consensus.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Pass the Drunk Person Test (3 drinks in, still comprehensible)
 * - Spring physics on selection with satisfying bounce
 * - Neon glow borders when selected, avatar pulse
 * - 60dp+ touch targets
 * - Uses NeonCard from design system
 *
 * @ai_prompt Player voting with neon glow, spring animations, HELLDECK styling
 */
@Composable
fun VotePlayerRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp)
            .semantics { contentDescription = "Vote for a player" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Stakes label - what's at risk
        NeonCard(
            accentColor = HelldeckColors.colorPrimary,
            elevation = 4.dp,
        ) {
            Text(
                text = "VOTE FOR ONE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = HelldeckColors.colorPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Get seat numbers from options
        val seatNumbers: List<Int> = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.SeatVote -> opts.seatNumbers
            else -> emptyList()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(HelldeckSpacing.Small.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            itemsIndexed(items = seatNumbers) { index, seatNumber ->
                PlayerVoteCard(
                    playerName = "Seat $seatNumber",
                    playerAvatar = getPlayerEmoji(seatNumber - 1),
                    isSelected = selectedIndex == index,
                    reducedMotion = reducedMotion,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedIndex = index
                        onEvent(RoundEvent.VotePlayer(index))
                    },
                )
            }
        }
    }
}

/**
 * Individual player vote card with HELLDECK NeonCard styling.
 *
 * @ai_prompt Spring physics, glow on selection, 60dp+ touch target, NeonCard wrapping
 */
@Composable
private fun PlayerVoteCard(
    playerName: String,
    playerAvatar: String,
    isSelected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (isSelected) HelldeckColors.colorSecondary else HelldeckColors.colorMuted

    // Spring physics for selection
    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.06f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = 0.5f,
                stiffness = Spring.StiffnessHigh,
            )
        },
        label = "vote_card_scale",
    )

    // Pulsing glow for selected avatar
    val infiniteTransition = rememberInfiniteTransition(label = "selection_pulse")
    val avatarPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected && !reducedMotion) 1.15f else 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(900, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "avatar_pulse",
    )

    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "selected_pulse",
    )

    NeonCard(
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.RecommendedTapTarget.dp + 20.dp)
            .scale(scale)
            .semantics {
                contentDescription = "$playerName${if (isSelected) ", selected" else ""}"
            },
        accentColor = accentColor,
        elevation = if (isSelected) (12.dp * selectedPulse) else 4.dp,
        onClick = onClick,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Player avatar emoji with pulse
            Text(
                text = playerAvatar,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (isSelected) 34.sp else 28.sp,
                ),
                modifier = Modifier.scale(avatarPulse),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))

            // Player name
            Text(
                text = playerName,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 18.sp,
                ),
                color = if (isSelected) HelldeckColors.colorSecondary else HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            // Confirmation indicator
            if (isSelected) {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
                Text(
                    text = "LOCKED IN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = HelldeckColors.colorSecondary.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/**
 * Get a consistent emoji for a player based on their index.
 * In a real implementation, this would come from player data.
 */
private fun getPlayerEmoji(index: Int): String {
    val emojis = listOf("😈", "🔥", "💀", "👹", "🎃", "👻", "🦇", "🌙", "⚡", "🎭")
    return emojis[index % emojis.size]
}
