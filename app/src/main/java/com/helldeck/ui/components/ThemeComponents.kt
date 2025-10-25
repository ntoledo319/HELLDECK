package com.helldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Helldeck color palette
 */
object HelldeckColors {
    val Yellow = Color(0xFFFFD700)
    val Orange = Color(0xFFFF8C00)
    val Red = Color(0xFFFF4444)
    val Green = Color(0xFF4CAF50)
    val White = Color.White
    val DarkGray = Color(0xFF2B2B2B)
    val MediumGray = Color(0xFF4A4A4A)
    val LightGray = Color(0xFF9E9E9E)
    val Black = Color.Black
}

/**
 * Helldeck spacing constants
 */
object HelldeckSpacing {
    const val Small = 8
    const val Medium = 16
    const val Large = 24
    const val XLarge = 32
}

/**
 * Helldeck animation specifications
 */
object HelldeckAnimations {
    const val DefaultDurationMs = 300
    const val FastDurationMs = 150
    const val SlowDurationMs = 500
}

/**
 * Loading spinner component
 */
@Composable
fun HelldeckLoadingSpinner(
    modifier: Modifier = Modifier,
    text: String = "Loading..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = HelldeckColors.Orange
            )
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = HelldeckColors.LightGray
                )
            }
        }
    }
}

/**
 * Background pattern component
 */
@Composable
fun HelldeckBackgroundPattern(
    modifier: Modifier = Modifier
) {
    // Stub implementation - background pattern placeholder
    Box(modifier = modifier.fillMaxSize())
}

/**
 * Giant button component for primary actions
 */
@Composable
fun GiantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = HelldeckColors.Orange
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        colors = colors
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}