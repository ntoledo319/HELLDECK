package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders SALES_PITCH interaction for Poison Pitch game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Infomercial / used car salesman vibe
 * - Money/deal aesthetic with gold NeonCard
 * - Persuasive energy
 * - Uses NeonCard for header, GlowButton for pitch submit
 *
 * @ai_prompt Infomercial sales pitch input with deal energy, HELLDECK neon styling
 */
@Composable
fun SalesPitchRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var pitch by remember { mutableStateOf("") }

    // Pulsing header animation for dramatic energy
    val infiniteTransition = rememberInfiniteTransition(label = "sell_pulse")
    val headerPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "header_pulse",
    )

    // Spring animation for submit readiness
    val submitScale by animateFloatAsState(
        targetValue = if (pitch.isNotBlank()) 1f else 0.95f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "submit_scale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp)
            .semantics { contentDescription = "Sales pitch input" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Salesman header - NeonCard with gold accent and dramatic pulse
        NeonCard(
            accentColor = HelldeckColors.Lol,
            elevation = 10.dp,
            modifier = Modifier.scale(if (reducedMotion) 1f else headerPulse),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SELL IT!",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.Lol,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Convince them to buy this garbage!",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Pitch input in NeonCard
        NeonCard(
            accentColor = if (pitch.isNotBlank()) HelldeckColors.Lol else HelldeckColors.colorMuted,
            elevation = if (pitch.isNotBlank()) 8.dp else 4.dp,
        ) {
            OutlinedTextField(
                value = pitch,
                onValueChange = { pitch = it },
                label = {
                    Text(
                        text = "Your pitch...",
                        color = HelldeckColors.colorMuted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .height(140.dp)
                    .semantics { contentDescription = "Type your sales pitch here" },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HelldeckColors.Lol,
                    unfocusedBorderColor = HelldeckColors.colorMuted.copy(alpha = 0.5f),
                    focusedTextColor = HelldeckColors.colorOnDark,
                    unfocusedTextColor = HelldeckColors.colorOnDark,
                    cursorColor = HelldeckColors.Lol,
                    focusedContainerColor = HelldeckColors.surfacePrimary,
                    unfocusedContainerColor = HelldeckColors.surfacePrimary.copy(alpha = 0.5f),
                    focusedLabelColor = HelldeckColors.Lol,
                    unfocusedLabelColor = HelldeckColors.colorMuted,
                ),
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                ),
                maxLines = 4,
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Pitch button - GlowButton with gold accent, spring animation, and glow
        GlowButton(
            text = "PITCH IT!",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEvent(RoundEvent.EnterText(pitch))
                pitch = ""
            },
            enabled = pitch.isNotBlank(),
            accentColor = HelldeckColors.Lol,
            modifier = Modifier
                .fillMaxWidth()
                .scale(submitScale)
                .then(
                    if (pitch.isNotBlank()) {
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(HelldeckRadius.Pill),
                            spotColor = HelldeckColors.Lol.copy(alpha = 0.6f),
                            ambientColor = HelldeckColors.Lol.copy(alpha = 0.3f),
                        )
                    } else {
                        Modifier
                    },
                )
                .semantics { contentDescription = "Submit your pitch" },
        )
    }
}
