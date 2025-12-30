package com.helldeck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helldeck.ui.BackgroundPattern
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius

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

@Composable
fun HelldeckLoadingSpinner(
    modifier: Modifier = Modifier,
    text: String = "Loading…",
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            if (text.isNotBlank()) {
                Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
