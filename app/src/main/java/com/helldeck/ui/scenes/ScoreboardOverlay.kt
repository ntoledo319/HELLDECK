package com.helldeck.ui.scenes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.helldeck.content.model.Player
import com.helldeck.ui.*

/**
 * Scoreboard overlay with enhanced animations and visual effects
 */
@Composable
fun ScoreboardOverlay(
    players: List<Player>,
    onClose: () -> Unit
) {
    val sorted = players.sortedByDescending { it.sessionPoints }
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        visible = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HelldeckColors.Yellow.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        radius = 1200f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(400)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 4 }
                ),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with pulsing glow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 SCOREBOARD",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        HelldeckColors.Yellow,
                                        HelldeckColors.Yellow,
                                        HelldeckColors.Orange,
                                        HelldeckColors.Yellow
                                    )
                                ),
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = HelldeckColors.Yellow.copy(alpha = 0.6f),
                                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                    blurRadius = 12f
                                )
                            )
                        )

                        TextButton(
                            onClick = onClose,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = HelldeckColors.Yellow
                            )
                        ) {
                            Text(
                                "✕ CLOSE",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Podium section with staggered animation
                    if (sorted.isNotEmpty()) {
                        EnhancedPodiumSection(
                            topPlayers = sorted.take(3),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Rest of players with fade-in animation
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sorted.drop(3)) { player ->
                            val position = sorted.indexOf(player) + 1
                            EnhancedPlayerScoreCard(
                                player = player,
                                position = position
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer with subtle glow
                    Surface(
                        color = HelldeckColors.Yellow.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🎯 Last place picks next game",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = HelldeckColors.Yellow,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedPodiumSection(
    topPlayers: List<Player>,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        visible = true
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // Second place with delayed entry
        if (topPlayers.size > 1) {
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(400, delayMillis = 200)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = spring(dampingRatio = 0.7f),
                    initialOffsetY = { it / 2 }
                )
            ) {
                EnhancedPodiumCard(
                    player = topPlayers[1],
                    position = 2,
                    height = 130.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // First place - center, tallest, first to appear
        if (topPlayers.isNotEmpty()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(400)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = spring(dampingRatio = 0.6f),
                    initialOffsetY = { it / 2 }
                ) + androidx.compose.animation.scaleIn(
                    animationSpec = spring(dampingRatio = 0.6f),
                    initialScale = 0.8f
                )
            ) {
                EnhancedPodiumCard(
                    player = topPlayers[0],
                    position = 1,
                    height = 170.dp,
                    isWinner = true,
                    modifier = Modifier.weight(1.1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Third place with delayed entry
        if (topPlayers.size > 2) {
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = tween(400, delayMillis = 400)
                ) + androidx.compose.animation.slideInVertically(
                    animationSpec = spring(dampingRatio = 0.7f),
                    initialOffsetY = { it / 2 }
                )
            ) {
                EnhancedPodiumCard(
                    player = topPlayers[2],
                    position = 3,
                    height = 110.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EnhancedPodiumCard(
    player: Player,
    position: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    isWinner: Boolean = false
) {
    val podiumColors = listOf(
        HelldeckColors.Yellow,     // Gold
        HelldeckColors.LightGray,  // Silver-ish
        HelldeckColors.Orange      // Bronze-ish
    )

    val cardColor = podiumColors.getOrElse(position - 1) { HelldeckColors.MediumGray }
    
    // Winner pulsing effect
    val infiniteTransition = rememberInfiniteTransition()
    val winnerGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "winner_glow"
    )

    ElevatedCard(
        modifier = modifier
            .height(height)
            .shadow(
                elevation = if (isWinner) (12.dp * winnerGlow) else 6.dp,
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                ambientColor = if (isWinner) cardColor.copy(alpha = 0.4f * winnerGlow) else Color.Transparent,
                spotColor = if (isWinner) cardColor.copy(alpha = 0.6f * winnerGlow) else Color.Transparent
            ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isWinner) 12.dp else 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardColor.copy(alpha = if (isWinner) 0.95f else 0.85f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            cardColor.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Position crown for winner
                if (isWinner) {
                    Text(
                        text = "👑",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                // Position number
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = position.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = cardColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Player avatar
                Text(
                    text = player.avatar,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (isWinner) 36.sp else 32.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Player name
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = if (isWinner) 16.sp else 14.sp
                    ),
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Score
                Text(
                    text = player.sessionPoints.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isWinner) 28.sp else 24.sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EnhancedPlayerScoreCard(
    player: Player,
    position: Int,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(position) {
        kotlinx.coroutines.delay((position * 50L).coerceAtMost(500))
        visible = true
    }
    
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(300)
        ) + androidx.compose.animation.slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.8f),
            initialOffsetX = { -it / 3 }
        )
    ) {
        ElevatedCard(
            modifier = modifier,
            colors = CardDefaults.elevatedCardColors(
                containerColor = HelldeckColors.DarkGray.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Position badge
                    Surface(
                        color = HelldeckColors.MediumGray,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = position.toString(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = HelldeckColors.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Player info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player.avatar,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = HelldeckColors.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Score
                Text(
                    text = player.sessionPoints.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = HelldeckColors.Yellow
                )
            }
        }
    }
}
