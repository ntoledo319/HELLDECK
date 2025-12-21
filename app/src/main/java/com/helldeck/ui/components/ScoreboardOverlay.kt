package com.helldeck.ui.components

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.content.model.Player
import com.helldeck.ui.components.HelldeckColors
import kotlinx.coroutines.delay

/**
 * Enhanced podium card for top players
 */
@Composable
private fun EnhancedPodiumCard(
    player: Player,
    position: Int,
    height: Dp,
    isWinner: Boolean = false,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(400, delayMillis = if (isWinner) 0 else 200)
        ) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.7f),
            initialOffsetY = { it / 2 }
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .shadow(
                    elevation = if (isWinner) 12.dp else 8.dp,
                    shape = RoundedCornerShape(8.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = when (position) {
                    1 -> HelldeckColors.Yellow.copy(alpha = 0.3f)
                    2 -> HelldeckColors.LightGray.copy(alpha = 0.2f)
                    3 -> HelldeckColors.Orange.copy(alpha = 0.2f)
                    else -> HelldeckColors.MediumGray
                }
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Position badge
                Surface(
                    color = when (position) {
                        1 -> HelldeckColors.Yellow
                        2 -> Color.LightGray
                        3 -> Color.DarkGray
                        else -> HelldeckColors.MediumGray
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when (position) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> position.toString()
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Player info
                Text(
                    text = player.avatar,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Bold
                    ),
                    color = if (isWinner) HelldeckColors.White else HelldeckColors.LightGray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Text(
                    text = "${player.sessionPoints} pts",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isWinner) HelldeckColors.White else HelldeckColors.LightGray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

/**
 * Enhanced player score card
 */
@Composable
private fun EnhancedPlayerScoreCard(
    player: Player,
    position: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = HelldeckColors.DarkGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position
            Text(
                text = "#$position",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = when (position) {
                    1 -> HelldeckColors.Yellow
                    2 -> HelldeckColors.LightGray
                    3 -> Color.DarkGray
                    else -> HelldeckColors.MediumGray
                },
                modifier = Modifier.width(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Player info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.avatar,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = HelldeckColors.White
                )
            }
            
            // Score
            Text(
                text = "${player.sessionPoints}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = when (position) {
                    1 -> HelldeckColors.Yellow
                    2 -> HelldeckColors.LightGray
                    3 -> Color.DarkGray
                    else -> HelldeckColors.MediumGray
                },
                modifier = Modifier.width(80.dp)
            )
        }
    }
}

/**
 * Scoreboard overlay component
 */
@Composable
fun ScoreboardOverlay(
    players: List<Player>,
    onClose: () -> Unit
) {
    val sorted = players.sortedByDescending { it.sessionPoints }
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
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
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(
                    animationSpec = tween(400)
                ) + slideInVertically(
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
                    LazyColumn(
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🎯 Last place picks next game",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
        delay(200)
        visible = true
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // Second place with delayed entry
        if (topPlayers.size > 1) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(
                    animationSpec = tween(400, delayMillis = 200)
                ) + slideInVertically(
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
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(
                    animationSpec = tween(400)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = 0.6f),
                    initialOffsetY = { it / 2 }
                ) + scaleIn(
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
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(
                    animationSpec = tween(400, delayMillis = 400)
                ) + slideInVertically(
                    animationSpec = spring(dampingRatio = 0.7f),
                    initialOffsetY = { it / 2 }
                )
            ) {
                EnhancedPodiumCard(
                    player = topPlayers[2],
                    position = 3,
                    height = 130.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}