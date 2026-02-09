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
 * Renders ODD_EXPLAIN interaction for Odd One Argues game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Quirky/weird vibe with question mark styling
 * - Text input for explanation
 * - Suspense and mystery feel
 *
 * @ai_prompt Quirky mystery explanation input, HELLDECK neon styling
 */
@Composable
fun OddExplainRenderer(
    @Suppress("UNUSED_PARAMETER") roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var explanation by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mystery header
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            color = HelldeckColors.colorAccentWarm.copy(alpha = 0.15f),
            border = BorderStroke(2.dp, HelldeckColors.colorAccentWarm.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "🤔", fontSize = 28.sp)
                Text(
                    text = "EXPLAIN YOURSELF",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorAccentWarm,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Why is this the odd one out?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Explanation input
        OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = {
                Text(
                    text = "🧠 Your reasoning...",
                    color = HelldeckColors.colorMuted,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HelldeckColors.colorAccentWarm,
                unfocusedBorderColor = HelldeckColors.colorMuted.copy(alpha = 0.5f),
                focusedTextColor = HelldeckColors.colorOnDark,
                unfocusedTextColor = HelldeckColors.colorOnDark,
                cursorColor = HelldeckColors.colorAccentWarm,
                focusedContainerColor = HelldeckColors.surfacePrimary,
                unfocusedContainerColor = HelldeckColors.surfacePrimary.copy(alpha = 0.5f),
                focusedLabelColor = HelldeckColors.colorAccentWarm,
                unfocusedLabelColor = HelldeckColors.colorMuted,
            ),
            shape = RoundedCornerShape(HelldeckRadius.Medium),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 4,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Submit button
        OddExplainSubmitButton(
            isEnabled = explanation.isNotBlank(),
            reducedMotion = reducedMotion,
            onClick = {
                onEvent(RoundEvent.EnterText(explanation))
                explanation = ""
            },
        )
    }
}

@Composable
private fun OddExplainSubmitButton(
    isEnabled: Boolean,
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
            .fillMaxWidth()
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 10.dp,
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                spotColor = HelldeckColors.colorAccentWarm.copy(alpha = if (isEnabled) 0.5f else 0.1f),
            ),
        enabled = isEnabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = HelldeckColors.colorAccentWarm,
            contentColor = HelldeckColors.background,
            disabledContainerColor = HelldeckColors.colorMuted.copy(alpha = 0.3f),
            disabledContentColor = HelldeckColors.colorMuted,
        ),
    ) {
        Text(
            text = "💡 SUBMIT THEORY",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        )
    }
}
