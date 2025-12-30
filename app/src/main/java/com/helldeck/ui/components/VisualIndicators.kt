package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.helldeck.ui.HelldeckColors
import kotlin.math.*

/**
 * Visual indicators for game state including streaks, heat level, and comeback mode
 *
 * Provides animated visual feedback for:
 * - Win streaks with fire effects
 * - Room heat level with pulsing indicators
 * - Comeback mode with special animations
 * - Score multipliers with visual emphasis
 */
object VisualIndicators {

    /**
     * Streak indicator with fire animation
     */
    @Composable
    fun StreakIndicator(
        streakCount: Int,
        modifier: Modifier = Modifier,
    ) {
        if (streakCount <= 0) return

        val infiniteTransition = rememberInfiniteTransition()
        val fireColor by infiniteTransition.animateColor(
            initialValue = HelldeckColors.Orange,
            targetValue = HelldeckColors.Red,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(0),
            ),
        )

        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(0),
            ),
        )

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Streak count
            Text(
                text = "$streakCount",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                ),
                modifier = Modifier
                    .background(
                        color = fireColor,
                        shape = CircleShape,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(CircleShape),
            )

            // Fire flames for high streaks
            if (streakCount >= 3) {
                repeat(3) { index ->
                    val flameScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400 + index * 100, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 100),
                        ),
                    )

                    Canvas(
                        modifier = Modifier
                            .size(12.dp, 20.dp)
                            .scale(flameScale),
                    ) {
                        drawFlame(
                            color = fireColor,
                            alpha = 1f - (index * 0.2f),
                        )
                    }
                }
            }
        }
    }

    /**
     * Heat level indicator with pulsing effect
     */
    @Composable
    fun HeatIndicator(
        heatLevel: Double, // 0.0 to 1.0
        modifier: Modifier = Modifier,
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(0),
            ),
        )

        val heatColor = when {
            heatLevel >= 0.8 -> HelldeckColors.Red
            heatLevel >= 0.6 -> HelldeckColors.Orange
            heatLevel >= 0.4 -> HelldeckColors.Yellow
            else -> HelldeckColors.Green
        }

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            // Heat bar background
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp),
                    ),
            ) {
                // Heat level fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            color = heatColor.copy(alpha = pulseAlpha),
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Heat percentage text
            Text(
                text = "${(heatLevel * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                ),
                modifier = Modifier
                    .background(
                        color = heatColor,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }

    /**
     * Comeback mode indicator with special animation
     */
    @Composable
    fun ComebackIndicator(
        isActive: Boolean,
        modifier: Modifier = Modifier,
    ) {
        if (!isActive) return

        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(0),
            ),
        )

        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(0),
            ),
        )

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            // Comeback icon with rotation
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .background(
                        color = HelldeckColors.Yellow,
                        shape = CircleShape,
                    ),
            ) {
                Text(
                    text = "🔥",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier
                        .rotate(rotation)
                        .padding(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "COMEBACK MODE",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = HelldeckColors.Yellow,
                ),
            )
        }
    }

    /**
     * Score multiplier indicator with emphasis effect
     */
    @Composable
    fun ScoreMultiplier(
        multiplier: Float,
        modifier: Modifier = Modifier,
    ) {
        if (multiplier <= 1f) return

        val animatedScale by animateFloatAsState(
            targetValue = if (multiplier > 1f) 1.2f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "score_multiplier_scale",
        )

        val multiplierColor = when {
            multiplier >= 2f -> HelldeckColors.Red
            multiplier >= 1.5f -> HelldeckColors.Orange
            multiplier >= 1.2f -> HelldeckColors.Yellow
            else -> HelldeckColors.Green
        }

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "×",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = multiplierColor,
                ),
            )

            Text(
                text = String.format("%.1f", multiplier),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = multiplierColor,
                ),
                modifier = Modifier.scale(animatedScale),
            )
        }
    }

    /**
     * Draws a flame shape for streak indicator
     */
    private fun DrawScope.drawFlame(color: Color, alpha: Float = 1f) {
        val flamePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.8f)
            quadraticBezierTo(
                size.width * 0.2f,
                size.height * 0.4f,
                size.width * 0.5f,
                size.height * 0.2f,
            )
            quadraticBezierTo(
                size.width * 0.8f,
                size.height * 0.4f,
                size.width * 0.5f,
                size.height * 0.1f,
            )
            close()
        }

        drawPath(
            path = flamePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.6f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height,
            ),
        )
    }

    /**
     * Combined game state indicator
     */
    @Composable
    fun GameStateIndicators(
        streakCount: Int,
        heatLevel: Double,
        isComebackMode: Boolean,
        scoreMultiplier: Float = 1f,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Streak indicator
            StreakIndicator(streakCount = streakCount)

            // Heat level indicator
            HeatIndicator(heatLevel = heatLevel)

            // Comeback mode indicator
            ComebackIndicator(isActive = isComebackMode)

            // Score multiplier
            ScoreMultiplier(multiplier = scoreMultiplier)
        }
    }
}
