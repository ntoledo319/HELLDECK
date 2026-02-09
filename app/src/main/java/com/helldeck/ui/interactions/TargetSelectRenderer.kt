package com.helldeck.ui.interactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
 * Renders TARGET_SELECT interaction for games that require targeting a player.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Crosshair / target lock vibe
 * - Player spotlight feel
 * - Red danger accent for targeting
 *
 * @ai_prompt Target lock player selection with crosshair vibe, HELLDECK neon styling
 */
@Composable
fun TargetSelectRenderer(
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
        // Target lock header
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.Error.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, HelldeckColors.Error.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "🎯", fontSize = 24.sp)
                Text(
                    text = "SELECT TARGET",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.Error,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        val seatNumbers: List<Int> = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.SeatSelect -> opts.seatNumbers
            else -> emptyList()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(HelldeckSpacing.Small.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            itemsIndexed(items = seatNumbers) { index, seatNumber ->
                TargetPlayerCard(
                    playerName = "Seat $seatNumber",
                    playerEmoji = getTargetEmoji(seatNumber - 1),
                    isSelected = selectedIndex == index,
                    reducedMotion = reducedMotion,
                    onClick = {
                        selectedIndex = index
                        onEvent(RoundEvent.SelectTarget(index))
                    },
                )
            }
        }
    }
}

@Composable
private fun TargetPlayerCard(
    playerName: String,
    playerEmoji: String,
    isSelected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val accentColor = HelldeckColors.Error // Red for targeting

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
        label = "target_card_scale",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.7f else if (isPressed) 0.3f else 0.1f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

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

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else HelldeckColors.colorMuted.copy(alpha = 0.5f),
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "border_color",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "target_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "selected_pulse",
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.RecommendedTapTarget.dp + 20.dp)
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
            // Target indicator for selected
            if (isSelected) {
                Text(text = "🎯", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            }

            Text(
                text = playerEmoji,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (isSelected) 32.sp else 28.sp,
                ),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))

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

private fun getTargetEmoji(index: Int): String {
    val emojis = listOf("😈", "🔥", "💀", "👹", "🎃", "👻", "🦇", "🌙", "⚡", "🎭")
    return emojis[index % emojis.size]
}
