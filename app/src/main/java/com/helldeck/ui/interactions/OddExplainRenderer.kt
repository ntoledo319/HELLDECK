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
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders ODD_EXPLAIN interaction for Odd One Argues game.
 *
 * DESIGN PRINCIPLE (Hell's Living Room):
 * - Quirky/weird vibe with mystery feel
 * - NeonCard for header
 * - Text input for explanation
 * - GlowButton for submit
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
    val haptic = LocalHapticFeedback.current
    var explanation by remember { mutableStateOf("") }

    // Spring animation for submit readiness
    val submitScale by animateFloatAsState(
        targetValue = if (explanation.isNotBlank()) 1f else 0.95f,
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
            .semantics { contentDescription = "Explain why this is the odd one out" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mystery header using NeonCard
        NeonCard(
            accentColor = HelldeckColors.colorAccentWarm,
            elevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "EXPLAIN YOURSELF",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = HelldeckColors.colorAccentWarm,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Why is this the odd one out?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Explanation input wrapped in NeonCard for consistent styling
        NeonCard(
            accentColor = if (explanation.isNotBlank()) {
                HelldeckColors.colorAccentWarm
            } else {
                HelldeckColors.colorMuted
            },
            elevation = if (explanation.isNotBlank()) 8.dp else 4.dp,
        ) {
            OutlinedTextField(
                value = explanation,
                onValueChange = { explanation = it },
                label = {
                    Text(
                        text = "Your reasoning...",
                        color = HelldeckColors.colorMuted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .height(140.dp)
                    .semantics { contentDescription = "Type your explanation here" },
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
                    fontSize = 20.sp,
                ),
                maxLines = 4,
            )
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Submit button using GlowButton with spring animation
        GlowButton(
            text = "SUBMIT THEORY",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEvent(RoundEvent.EnterText(explanation))
                explanation = ""
            },
            enabled = explanation.isNotBlank(),
            accentColor = HelldeckColors.colorAccentWarm,
            modifier = Modifier
                .fillMaxWidth()
                .scale(submitScale)
                .then(
                    if (explanation.isNotBlank()) {
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(HelldeckRadius.Pill),
                            spotColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.6f),
                            ambientColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.3f),
                        )
                    } else {
                        Modifier
                    },
                )
                .semantics { contentDescription = "Submit your explanation" },
        )
    }
}
