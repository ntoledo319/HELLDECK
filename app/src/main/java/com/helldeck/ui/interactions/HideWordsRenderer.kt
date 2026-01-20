package com.helldeck.ui.interactions

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
 * Renders HIDE_WORDS interaction for Secret Slang game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Spy/secret agent vibe
 * - Hidden words display with redacted styling
 * - Mission briefing feel
 *
 * @ai_prompt Secret agent mission briefing with hidden words, HELLDECK neon styling
 */
@Composable
fun HideWordsRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current

    // Scanning effect for spy vibe
    val infiniteTransition = rememberInfiniteTransition(label = "spy_scan")
    val scanPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "scan_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mission briefing header
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.colorAccentCool.copy(alpha = 0.15f * scanPulse),
            border = BorderStroke(2.dp, HelldeckColors.colorAccentCool.copy(alpha = 0.6f * scanPulse)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(text = "🕵️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CLASSIFIED MISSION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorAccentCool,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Instructions
        Text(
            text = "Hide these words in conversation without getting caught!",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Ready button
        HideWordsReadyButton(
            reducedMotion = reducedMotion,
            onClick = { onEvent(RoundEvent.LockIn) },
        )
    }
}

@Composable
private fun HideWordsReadyButton(
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
        label = "button_scale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
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
            text = "🎯 ACCEPT MISSION",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        )
    }
}
