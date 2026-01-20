package com.helldeck.ui.interactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
 * Renders SPEED_LIST interaction for Scatterblast game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Bomb timer urgency feel
 * - Rapid-fire input with satisfying counter
 * - Pulsing urgency as items pile up
 * - High energy, chaotic vibe
 *
 * @ai_prompt Bomb timer urgency, rapid-fire input, HELLDECK neon styling
 */
@Composable
fun SpeedListRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var item by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<String>()) }

    // Urgency pulse based on item count
    val infiniteTransition = rememberInfiniteTransition(label = "urgency_pulse")
    val urgencyPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion) 1f else 1.05f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = (800 - (items.size * 50)).coerceAtLeast(300), // Gets faster as items pile up
                    easing = EaseInOutCubic,
                ),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "urgency_pulse",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Bomb timer header
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.Error.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, HelldeckColors.Error.copy(alpha = 0.5f)),
            modifier = Modifier.scale(urgencyPulse),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "💣", fontSize = 24.sp)
                Text(
                    text = "SCATTERBLAST!",
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

        // Item counter with dramatic display
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Large),
            color = HelldeckColors.colorSecondary.copy(alpha = 0.15f),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HelldeckColors.colorSecondary.copy(alpha = 0.8f),
                        HelldeckColors.colorSecondary.copy(alpha = 0.4f),
                    ),
                ),
            ),
            modifier = Modifier.shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = HelldeckColors.colorSecondary.copy(alpha = 0.4f),
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
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
                    text = "🚀 Next item...",
                    color = HelldeckColors.colorMuted,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(HelldeckHeights.Input.dp + 8.dp),
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
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            SpeedListButton(
                label = "➕ ADD",
                isEnabled = item.isNotBlank(),
                accentColor = HelldeckColors.colorSecondary,
                reducedMotion = reducedMotion,
                onClick = {
                    if (item.isNotBlank()) {
                        items = items + item
                        onEvent(RoundEvent.EnterText(item))
                        item = ""
                    }
                },
                modifier = Modifier.weight(1f),
            )

            SpeedListButton(
                label = "✅ DONE",
                isEnabled = true,
                accentColor = HelldeckColors.colorPrimary,
                reducedMotion = reducedMotion,
                onClick = { onEvent(RoundEvent.LockIn) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SpeedListButton(
    label: String,
    isEnabled: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (!isEnabled) 1f else if (isPressed) 0.95f else 1f,
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
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 8.dp,
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                spotColor = accentColor.copy(alpha = if (isEnabled) 0.4f else 0.1f),
            ),
        enabled = isEnabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = HelldeckColors.background,
            disabledContainerColor = HelldeckColors.colorMuted.copy(alpha = 0.3f),
            disabledContentColor = HelldeckColors.colorMuted,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
