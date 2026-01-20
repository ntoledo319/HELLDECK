package com.helldeck.ui.interactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
 * Renders player voting interaction (VOTE_PLAYER).
 * Used for consensus voting games like Roast Consensus.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Pass the Drunk Person Test™ (3 drinks in, still comprehensible)
 * - Spring physics on selection
 * - Glow effects with accent colors
 * - 60dp+ touch targets
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
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Stakes label - what's at risk
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.colorPrimary.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, HelldeckColors.colorPrimary.copy(alpha = 0.4f)),
        ) {
            Text(
                text = "🎯 VOTE FOR ONE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = HelldeckColors.colorPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Get player names from options
        val playerNames: List<String> = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.PlayerVote -> opts.players
            else -> emptyList()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(HelldeckSpacing.Small.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            itemsIndexed(items = playerNames) { index, playerName ->
                PlayerVoteCard(
                    playerName = playerName,
                    playerAvatar = getPlayerEmoji(index),
                    isSelected = selectedIndex == index,
                    reducedMotion = reducedMotion,
                    onClick = {
                        selectedIndex = index
                        onEvent(RoundEvent.VotePlayer(index))
                    },
                )
            }
        }
    }
}

/**
 * Individual player vote card with HELLDECK styling.
 *
 * @ai_prompt Spring physics, glow on selection, 60dp+ touch target
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val accentColor = HelldeckColors.colorSecondary // Lime for selection

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
        label = "vote_card_scale",
    )

    // Glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.7f else if (isPressed) 0.3f else 0.1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    // Background color animation
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = 0.2f)
        } else if (isPressed) {
            HelldeckColors.surfaceElevated
        } else {
            HelldeckColors.surfacePrimary
        },
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "background_color",
    )

    // Border color animation
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else HelldeckColors.colorMuted.copy(alpha = 0.5f),
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "border_color",
    )

    // Pulsing glow for selected state
    val infiniteTransition = rememberInfiniteTransition(label = "selection_pulse")
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

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.RecommendedTapTarget.dp + 20.dp) // 80dp for comfortable voting
            .scale(scale)
            .shadow(
                elevation = if (isSelected) (8.dp * selectedPulse) else if (isPressed) 4.dp else 2.dp,
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                spotColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse else glowAlpha),
                ambientColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse * 0.5f else glowAlpha * 0.5f),
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = if (isSelected) 1f else 0.6f),
                    borderColor.copy(alpha = if (isSelected) 0.8f else 0.4f),
                    borderColor.copy(alpha = if (isSelected) 1f else 0.6f),
                ),
            ),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = HelldeckColors.colorOnDark,
        ),
        contentPadding = PaddingValues(HelldeckSpacing.Medium.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Player avatar emoji
            Text(
                text = playerAvatar,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (isSelected) 32.sp else 28.sp,
                ),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))

            // Player name
            Text(
                text = playerName,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isSelected) 16.sp else 14.sp,
                ),
                color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
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
