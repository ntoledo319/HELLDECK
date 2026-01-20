package com.helldeck.ui.interactions

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckAnimations
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders REPLY_TONE interaction for Text Thread Trap game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Message bubble aesthetic
 * - Tone selection with emoji indicators
 * - Chat/texting vibe
 * - Spring physics on selection
 *
 * @ai_prompt Message bubble tone picker with texting vibe, HELLDECK neon styling
 */
@Composable
fun ReplyToneRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var selectedTone by remember { mutableStateOf<String?>(null) }

    val tones = when (val opts = roundState.options) {
        is com.helldeck.content.model.GameOptions.ReplyTone -> opts.tones
        else -> listOf("Deadpan", "Feral", "Chaotic", "Wholesome")
    }

    // Map tones to emojis and colors
    val toneData = mapOf(
        "Deadpan" to Pair("😐", HelldeckColors.colorMuted),
        "Feral" to Pair("🐺", HelldeckColors.colorPrimary),
        "Chaotic" to Pair("🌪️", HelldeckColors.colorAccentWarm),
        "Wholesome" to Pair("💖", HelldeckColors.colorSecondary),
        "Funny" to Pair("😂", HelldeckColors.Lol),
        "Serious" to Pair("😤", HelldeckColors.Error),
        "Sarcastic" to Pair("🙄", HelldeckColors.colorAccentCool),
        "Kind" to Pair("🥰", HelldeckColors.colorSecondary),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with message bubble vibe
        Surface(
            shape = RoundedCornerShape(
                topStart = HelldeckRadius.Large,
                topEnd = HelldeckRadius.Large,
                bottomStart = HelldeckRadius.Small,
                bottomEnd = HelldeckRadius.Large,
            ),
            color = HelldeckColors.colorAccentCool.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, HelldeckColors.colorAccentCool.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "💬", fontSize = 24.sp)
                Text(
                    text = "PICK YOUR VIBE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                    ),
                    color = HelldeckColors.colorAccentCool,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Instruction
        Text(
            text = "How should you reply?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Tone buttons in a 2x2 grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            tones.chunked(2).forEach { rowTones ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                ) {
                    rowTones.forEach { tone ->
                        val (emoji, color) = toneData[tone] ?: Pair("📝", HelldeckColors.colorMuted)
                        ToneButton(
                            tone = tone,
                            emoji = emoji,
                            isSelected = selectedTone == tone,
                            accentColor = color,
                            reducedMotion = reducedMotion,
                            onClick = {
                                selectedTone = tone
                                onEvent(RoundEvent.SelectOption(tone))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Fill empty space if odd number of tones
                    if (rowTones.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToneButton(
    tone: String,
    emoji: String,
    isSelected: Boolean,
    accentColor: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isSelected && isPressed -> 0.92f
            isSelected -> 1.05f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "tone_scale",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.7f else if (isPressed) 0.3f else 0.15f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.25f) else HelldeckColors.surfacePrimary,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "background_color",
    )

    // Message bubble shape - rounded on all corners except bottom-left for "sent" feel
    val bubbleShape = RoundedCornerShape(
        topStart = HelldeckRadius.Large,
        topEnd = HelldeckRadius.Large,
        bottomStart = if (isSelected) HelldeckRadius.Small else HelldeckRadius.Medium,
        bottomEnd = HelldeckRadius.Large,
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tone_pulse")
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
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

    Button(
        onClick = onClick,
        modifier = modifier
            .height(90.dp)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) (10.dp * selectedPulse) else if (isPressed) 6.dp else 4.dp,
                shape = bubbleShape,
                spotColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse else glowAlpha),
                ambientColor = accentColor.copy(alpha = if (isSelected) glowAlpha * selectedPulse * 0.5f else glowAlpha * 0.5f),
            ),
        interactionSource = interactionSource,
        shape = bubbleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = HelldeckColors.colorOnDark,
        ),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isSelected) 1f else 0.4f),
                    accentColor.copy(alpha = if (isSelected) 0.6f else 0.2f),
                ),
            ),
        ),
        contentPadding = PaddingValues(HelldeckSpacing.Medium.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (isSelected) 32.sp else 28.sp,
                ),
            )
            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            Text(
                text = tone.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp,
                ),
                color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
            )
        }
    }
}
