package com.helldeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual spice level slider for home screen
 */
@Composable
fun SpiceSlider(
    spiceLevel: Int,
    onSpiceLevelChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spice Level",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = getSpiceColor(spiceLevel).copy(alpha = 0.2f)
            ) {
                Text(
                    text = getSpiceLabel(spiceLevel),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = getSpiceColor(spiceLevel),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Slider with custom design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(spiceLevel / 5f)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                getSpiceColor(1),
                                getSpiceColor(spiceLevel.coerceAtLeast(2))
                            )
                        )
                    )
            )

            // Spice level buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val level = index + 1
                    val isActive = level <= spiceLevel
                    val isSelected = level == spiceLevel

                    SpiceLevelButton(
                        level = level,
                        isActive = isActive,
                        isSelected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSpiceLevelChanged(level)
                        }
                    )
                }
            }
        }

        // Description
        Text(
            text = getSpiceDescription(spiceLevel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SpiceLevelButton(
    level: Int,
    isActive: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "spice_scale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 36.dp else 32.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) {
                        if (isSelected) getSpiceColor(level)
                        else getSpiceColor(level).copy(alpha = 0.6f)
                    } else {
                        Color.White.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.toString(),
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = if (isSelected) 18.sp else 14.sp
            )
        }
    }
}

private fun getSpiceColor(level: Int): Color = when (level) {
    1 -> Color(0xFF667eea) // Wholesome purple
    2 -> Color(0xFFf093fb) // Playful pink
    3 -> Color(0xFFfa709a) // Edgy orange
    4 -> Color(0xFFff0844) // Wild red
    5 -> Color(0xFF00f2fe) // Chaos cyan
    else -> Color.Gray
}

private fun getSpiceLabel(level: Int): String = when (level) {
    1 -> "😇 Wholesome"
    2 -> "😄 Playful"
    3 -> "😈 Edgy"
    4 -> "🔥 Wild"
    5 -> "💀 Chaos"
    else -> "Unknown"
}

private fun getSpiceDescription(level: Int): String = when (level) {
    1 -> "Family-friendly, PG-13 humor"
    2 -> "Fun and playful with light edge"
    3 -> "Edgy and provocative, not mean-spirited"
    4 -> "Wild and unhinged, but not offensive"
    5 -> "Maximum chaos (keep it funny, not cruel)"
    else -> ""
}
