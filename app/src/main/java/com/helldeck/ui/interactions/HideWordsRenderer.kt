package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
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
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders HIDE_WORDS interaction for Secret Slang game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Spy/secret agent vibe
 * - Classified mission briefing with NeonCard
 * - Scanning pulse for intrigue
 * - Uses NeonCard for header, GlowButton for accept mission
 *
 * @ai_prompt Secret agent mission briefing with hidden words, HELLDECK neon styling
 */
@Composable
fun HideWordsRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current

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
            .padding(HelldeckSpacing.Large.dp)
            .semantics { contentDescription = "Secret Slang hidden words mission" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mission briefing header - NeonCard with scanning effect
        NeonCard(
            accentColor = HelldeckColors.colorAccentCool,
            elevation = (10.dp * scanPulse),
            modifier = Modifier.scale(if (reducedMotion) 1f else (0.97f + 0.03f * scanPulse)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "CLASSIFIED MISSION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorAccentCool,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Instructions
        Text(
            text = "Hide these words in conversation without getting caught!",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = HelldeckSpacing.Large.dp),
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Huge.dp))

        // Ready button - GlowButton
        GlowButton(
            text = "ACCEPT MISSION",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEvent(RoundEvent.LockIn)
            },
            accentColor = HelldeckColors.colorSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Accept mission and start" },
        )
    }
}
