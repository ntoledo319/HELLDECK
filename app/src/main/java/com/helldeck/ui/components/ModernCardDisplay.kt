package com.helldeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern card display with gradient backgrounds and smooth animations
 */
@Composable
fun ModernCardDisplay(
    text: String,
    gameTitle: String,
    spiceLevel: Int,
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false,
    generatedByLLM: Boolean = false
) {
    // Animate card entrance
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(text) {
        visible = false
        kotlinx.coroutines.delay(50)
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "card_alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    // Gradient based on spice level
    val gradientColors = when (spiceLevel) {
        1 -> listOf(Color(0xFF667eea), Color(0xFF764ba2)) // Wholesome purple
        2 -> listOf(Color(0xFFf093fb), Color(0xFFf5576c)) // Playful pink
        3 -> listOf(Color(0xFFfa709a), Color(0xFFfee140)) // Edgy orange
        4 -> listOf(Color(0xFFff0844), Color(0xFFffb199)) // Wild red
        5 -> listOf(Color(0xFF4facfe), Color(0xFF00f2fe)) // Chaos neon
        else -> listOf(Color(0xFF667eea), Color(0xFF764ba2))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = gradientColors[0].copy(alpha = 0.3f),
                spotColor = gradientColors[1].copy(alpha = 0.3f)
            )
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Game title header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gameTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )

                    if (generatedByLLM) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "✨ AI",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Card text (centered)
                if (isGenerating) {
                    ShimmerLoadingText()
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        fontSize = 28.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Spice indicator
                SpiceIndicator(spiceLevel)
            }
        }
    }
}

@Composable
private fun SpiceIndicator(level: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(5) { index ->
            val isActive = index < level
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) Color.White
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
fun ShimmerLoadingText() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) { index ->
            val alpha = ((shimmer + (index * 0.2f)) % 1f).coerceIn(0.3f, 0.8f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f - (index * 0.1f))
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }

    Text(
        text = "Generating...",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 16.dp)
    )
}
