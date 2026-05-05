package com.helldeck.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.BackgroundPattern
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.LocalReducedMotion

/**
 * @context_boundary
 * Compatibility bridge for older imports under `com.helldeck.ui.components.*`.
 *
 * Canonical tokens live in `com.helldeck.ui.Theme.kt`.
 * This file must not define new colors/spacing/animation values.
 */
object HelldeckColors {
    val Yellow get() = com.helldeck.ui.HelldeckColors.Yellow
    val Orange get() = com.helldeck.ui.HelldeckColors.Orange
    val Red get() = com.helldeck.ui.HelldeckColors.Red
    val Green get() = com.helldeck.ui.HelldeckColors.Green
    val White get() = com.helldeck.ui.HelldeckColors.White
    val DarkGray get() = com.helldeck.ui.HelldeckColors.DarkGray
    val MediumGray get() = com.helldeck.ui.HelldeckColors.MediumGray
    val LightGray get() = com.helldeck.ui.HelldeckColors.LightGray
    val Black get() = com.helldeck.ui.HelldeckColors.Black
}

object HelldeckSpacing {
    val Small get() = com.helldeck.ui.HelldeckSpacing.Small
    val Medium get() = com.helldeck.ui.HelldeckSpacing.Medium
    val Large get() = com.helldeck.ui.HelldeckSpacing.Large
    val XLarge get() = com.helldeck.ui.HelldeckSpacing.Huge
}

object HelldeckAnimations {
    val DefaultDurationMs get() = com.helldeck.ui.HelldeckAnimations.Normal
    val FastDurationMs get() = com.helldeck.ui.HelldeckAnimations.Fast
    val SlowDurationMs get() = com.helldeck.ui.HelldeckAnimations.Slow
}

/** Sentinel value to detect when caller relies on the default text. */
private const val LOADING_DEFAULT_SENTINEL = "\u0000__HELLDECK_DEFAULT__"

private val chaosMessages = listOf(
    "Summoning chaos\u2026",
    "Warming up the roasts\u2026",
    "Shuffling the deck of depravity\u2026",
    "Preparing your downfall\u2026",
    "Loading the unhinged\u2026",
    "Fueling the dumpster fire\u2026",
    "Sharpening the insults\u2026",
    "Preheating hell\u2026",
    "Calibrating toxicity levels\u2026",
    "Unleashing bad decisions\u2026",
    "Assembling the chaos engine\u2026",
    "Weaponizing friendship\u2026",
)

@Composable
fun HelldeckLoadingSpinner(
    modifier: Modifier = Modifier,
    text: String = LOADING_DEFAULT_SENTINEL,
) {
    val reducedMotion = LocalReducedMotion.current
    val displayText = if (text == LOADING_DEFAULT_SENTINEL) {
        remember { chaosMessages.random() }
    } else {
        text
    }

    val colorPrimary = com.helldeck.ui.HelldeckColors.colorPrimary
    val colorSecondary = com.helldeck.ui.HelldeckColors.colorSecondary
    val colorAccentCool = com.helldeck.ui.HelldeckColors.colorAccentCool
    val colorMuted = com.helldeck.ui.HelldeckColors.colorMuted

    // --- Animations (all collapse to static values when reducedMotion is true) ---
    val infiniteTransition = rememberInfiniteTransition(label = "helldeck_loading")

    // Outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(
                animation = tween(com.helldeck.ui.HelldeckAnimations.Instant, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        } else {
            infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        },
        label = "outer_rotation",
    )

    // Middle ring: counter-rotate, different speed
    val middleRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(
                animation = tween(com.helldeck.ui.HelldeckAnimations.Instant, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        } else {
            infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        },
        label = "middle_rotation",
    )

    // Inner ring: fast and chaotic
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(
                animation = tween(com.helldeck.ui.HelldeckAnimations.Instant, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        } else {
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            )
        },
        label = "inner_rotation",
    )

    // Neon pulse (glow intensity oscillation)
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(
                animation = tween(com.helldeck.ui.HelldeckAnimations.Instant),
                repeatMode = RepeatMode.Restart,
            )
        } else {
            infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "glow_pulse",
    )

    // Text glow flicker
    val textGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(
                animation = tween(com.helldeck.ui.HelldeckAnimations.Instant),
                repeatMode = RepeatMode.Restart,
            )
        } else {
            infiniteRepeatable(
                animation = tween(900, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "text_glow",
    )

    val ringSize = 96.dp

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(com.helldeck.ui.HelldeckSpacing.ExtraLarge.dp),
        ) {
            // --- Neon chaos rings ---
            Canvas(
                modifier = Modifier.size(ringSize),
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer neon glow halo (diffuse)
                drawCircle(
                    color = colorPrimary.copy(alpha = 0.15f * glowPulse),
                    radius = size.minDimension / 2f,
                    center = center,
                )

                val strokeOuter = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val strokeMiddle = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val strokeInner = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Outer ring — primary (hot pink) — 240-degree arc
                val outerInset = 4.dp.toPx()
                rotate(degrees = outerRotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colorPrimary.copy(alpha = glowPulse),
                                colorPrimary.copy(alpha = 0.1f),
                                colorPrimary.copy(alpha = glowPulse),
                            ),
                            center = center,
                        ),
                        startAngle = 0f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset(outerInset, outerInset),
                        size = Size(
                            size.width - outerInset * 2,
                            size.height - outerInset * 2,
                        ),
                        style = strokeOuter,
                    )
                }

                // Middle ring — secondary (acid green) — 180-degree arc, counter-rotating
                val middleInset = 14.dp.toPx()
                rotate(degrees = middleRotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colorSecondary.copy(alpha = glowPulse * 0.8f),
                                colorSecondary.copy(alpha = 0.05f),
                                colorSecondary.copy(alpha = glowPulse * 0.8f),
                            ),
                            center = center,
                        ),
                        startAngle = 30f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(middleInset, middleInset),
                        size = Size(
                            size.width - middleInset * 2,
                            size.height - middleInset * 2,
                        ),
                        style = strokeMiddle,
                    )
                }

                // Inner ring — accent cool (cyan neon) — 120-degree arc, fast
                val innerInset = 24.dp.toPx()
                rotate(degrees = innerRotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colorAccentCool.copy(alpha = glowPulse * 0.9f),
                                colorAccentCool.copy(alpha = 0.05f),
                            ),
                            center = center,
                        ),
                        startAngle = 0f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(innerInset, innerInset),
                        size = Size(
                            size.width - innerInset * 2,
                            size.height - innerInset * 2,
                        ),
                        style = strokeInner,
                    )
                }

                // Center dot — pulsing hot core
                drawCircle(
                    color = colorPrimary.copy(alpha = glowPulse * 0.7f),
                    radius = 3.dp.toPx() * glowPulse,
                    center = center,
                )
            }

            // --- Chaos loading text ---
            if (displayText.isNotBlank()) {
                Text(
                    text = displayText.uppercase(),
                    color = colorPrimary.copy(alpha = textGlow),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun HelldeckBackgroundPattern(
    modifier: Modifier = Modifier,
    pattern: BackgroundPattern = BackgroundPattern.DOTS,
    opacity: Float = 0.05f,
) {
    com.helldeck.ui.HelldeckBackgroundPattern(
        modifier = modifier,
        pattern = pattern,
        opacity = opacity,
    )
}

@Composable
fun GiantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = com.helldeck.ui.HelldeckColors.colorPrimary),
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.Button.dp),
        enabled = enabled,
        colors = colors,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(HelldeckRadius.Pill),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
