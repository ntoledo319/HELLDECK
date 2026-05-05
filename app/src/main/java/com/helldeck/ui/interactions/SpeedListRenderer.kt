package com.helldeck.ui.interactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders SPEED_LIST interaction for Scatterblast game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Bomb timer visual, frantic neon pulsing
 * - Countdown should feel URGENT
 * - Rapid-fire input with satisfying counter
 * - Uses NeonCard for counter display, GlowButton/OutlineButton for actions
 *
 * @ai_prompt Bomb timer urgency, rapid-fire input, HELLDECK neon styling
 */
@Composable
fun SpeedListRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var item by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<String>()) }

    // Urgency pulse based on item count - gets faster as items pile up
    val infiniteTransition = rememberInfiniteTransition(label = "urgency_pulse")
    val urgencyPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion) 1f else 1.06f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = (800 - (items.size * 50)).coerceAtLeast(300),
                    easing = EaseInOutCubic,
                ),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "urgency_pulse",
    )

    // Bomb glow pulse
    val bombGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = (500 - (items.size * 30)).coerceAtLeast(200),
                    easing = EaseInOutCubic,
                ),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "bomb_glow",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp)
            .semantics { contentDescription = "Speed list game, ${items.size} items listed" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Bomb timer header with frantic pulsing
        NeonCard(
            accentColor = HelldeckColors.Error,
            elevation = (12.dp * bombGlow),
            modifier = Modifier.scale(urgencyPulse),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SCATTERBLAST!",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.Error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Item counter with dramatic display - NeonCard
        NeonCard(
            accentColor = HelldeckColors.colorSecondary,
            elevation = 8.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = items.size.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                    ),
                    color = HelldeckColors.colorSecondary,
                )
                Text(
                    text = "ITEMS LISTED",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Input field with HELLDECK styling
        OutlinedTextField(
            value = item,
            onValueChange = { item = it },
            label = {
                Text(
                    text = "Next item...",
                    color = HelldeckColors.colorMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(HelldeckHeights.Input.dp + 8.dp)
                .semantics { contentDescription = "Type next item" },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HelldeckColors.colorSecondary,
                unfocusedBorderColor = HelldeckColors.colorMuted.copy(alpha = 0.5f),
                focusedTextColor = HelldeckColors.colorOnDark,
                unfocusedTextColor = HelldeckColors.colorOnDark,
                cursorColor = HelldeckColors.colorSecondary,
                focusedContainerColor = HelldeckColors.surfacePrimary,
                unfocusedContainerColor = HelldeckColors.surfacePrimary.copy(alpha = 0.5f),
                focusedLabelColor = HelldeckColors.colorSecondary,
                unfocusedLabelColor = HelldeckColors.colorMuted,
            ),
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Action buttons using design system components
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            GlowButton(
                text = "ADD",
                onClick = {
                    if (item.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        items = items + item
                        onEvent(RoundEvent.EnterText(item))
                        item = ""
                    }
                },
                enabled = item.isNotBlank(),
                accentColor = HelldeckColors.colorSecondary,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Add item to list" },
            )

            GlowButton(
                text = "DONE",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(RoundEvent.LockIn)
                },
                accentColor = HelldeckColors.colorPrimary,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Finish listing items" },
            )
        }
    }
}
