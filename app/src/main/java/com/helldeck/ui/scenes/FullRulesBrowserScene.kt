package com.helldeck.ui.scenes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.Scene
import com.helldeck.ui.gameIconFor
import com.helldeck.ui.theme.HelldeckSpacing

/**
 * Full rules browser - allows players to explore all game rules before playing
 * Accessible from Home screen and Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullRulesBrowserScene(vm: HelldeckVm) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Rules Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { vm.goBack() }) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = { vm.goHome() }) {
                        Text("Home")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = HelldeckSpacing.Medium.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = HelldeckSpacing.Medium.dp)
        ) {
            item {
                Text(
                    text = "Browse all 14 HELLDECK games",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(GameMetadata.getAllGameIds()) { gameId ->
                GameRulesBrowserCard(
                    gameId = gameId,
                    onClick = {
                        vm.selectedGameId = gameId
                        vm.navigateTo(Scene.GAME_RULES)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))
            }
        }
    }
}

@Composable
private fun GameRulesBrowserCard(
    gameId: String,
    onClick: () -> Unit
) {
    val metadata = GameMetadata.getGameMetadata(gameId)
    val gameEmoji = gameIconFor(gameId)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HelldeckSpacing.Medium.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Game emoji
                Text(
                    text = gameEmoji,
                    style = MaterialTheme.typography.headlineMedium
                )

                // Game info
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = metadata?.title ?: gameId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    metadata?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "⏱️ ${it.timerSec}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "👥 ${it.minPlayers}-${it.maxPlayers}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "View full rules",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
