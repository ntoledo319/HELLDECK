package com.helldeck.ui.interactions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion

/**
 * Empty renderer for NONE interaction type.
 * Displays an atmospheric spectator message when no interaction is required.
 * The void whispers. The game watches. You wait.
 *
 * @ai_prompt Menacing spectator state — the game is alive between turns.
 */

private val spectatorLines = listOf(
    "YOUR HANDS ARE CLEAN\u2026 FOR NOW",
    "SPECTATOR MODE: JUDGE SILENTLY",
    "SIT BACK. WATCH THEM SQUIRM.",
    "NOTHING TO DO BUT STARE INTO THE VOID",
    "THE VOID STARES BACK",
    "YOUR TURN WILL COME",
    "OBSERVE. REMEMBER. JUDGE.",
    "THE GAME SEES EVERYTHING",
    "SILENCE IS A WEAPON. USE IT.",
    "THEY DON'T KNOW YOU'RE WATCHING",
    "PATIENCE IS JUST DELAYED VIOLENCE",
    "EVERY WORD THEY SAY IS AMMUNITION",
    "YOU'RE NOT IDLE. YOU'RE LOADING.",
    "THE CHAOS DOESN'T NEED YOUR HELP YET",
    "BREATHE. PLOT. STRIKE LATER.",
)

@Composable
fun EmptyRenderer(
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val line = remember { spectatorLines.random() }

    // Slow breathing pulse: the void inhales and exhales
    val alpha = if (reducedMotion) {
        0.6f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "void-breath")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2500),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "void-breath-alpha",
        )
        animatedAlpha
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = HelldeckSpacing.ExtraLarge.dp,
                vertical = HelldeckSpacing.Huge.dp,
            )
            .semantics { contentDescription = "Waiting for other players" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = line,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            ),
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(alpha),
        )
    }
}
