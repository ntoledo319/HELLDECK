package com.helldeck.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.engine.Config
import com.helldeck.ui.*
import com.helldeck.ui.components.GameTile
import com.helldeck.ui.theme.HelldeckColors
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScene(vm: HelldeckVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val games = remember { (0 until com.helldeck.engine.Games.size).map { com.helldeck.engine.Games[it] } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HELLDECK") },
                actions = {
                    IconButton(onClick = { vm.navigateTo(Scene.ROLLCALL) }) {
                        Icon(Icons.Rounded.HowToReg, contentDescription = "Rollcall")
                    }
                    IconButton(onClick = { vm.toggleScores() }) {
                        Icon(Icons.Rounded.Leaderboard, contentDescription = "Scores")
                    }
                    IconButton(onClick = { vm.navigateTo(Scene.STATS) }) {
                        Icon(Icons.Rounded.Insights, contentDescription = "Stats")
                    }
                    IconButton(onClick = { vm.navigateTo(Scene.RULES) }) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "Rules & How-To")
                    }
                    IconButton(onClick = { vm.navigateTo(Scene.SETTINGS) }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(HelldeckSpacing.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeroCard(
                title = "Single phone. One card per round.",
                subtitle = "Long-press to draw • two-finger tap = back",
                onRollcall = { vm.navigateTo(Scene.ROLLCALL) }
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            QuickActionsRow(
                onRollcall = { vm.navigateTo(Scene.ROLLCALL) },
                onRules = { vm.navigateTo(Scene.RULES) },
                onSettings = { vm.navigateTo(Scene.SETTINGS) },
                onStats = { vm.navigateTo(Scene.STATS) }
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            Text(
                text = "Games",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(games, key = { it.id }) { game ->
                    GameTile(
                        title = game.title,
                        subtitle = game.description,
                        icon = gameIconFor(game.id),
                        onClick = { scope.launch { vm.startRound(game.id) } },
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }

            HeatCard(
                heat = vm.heatThreshold,
                onHeatChanged = {
                    vm.heatThreshold = it
                    Config.setRoomHeatThreshold(it.toDouble())
                    vm.spicy = it >= 0.70f
                    Config.spicyMode = vm.spicy
                }
            )

            if (vm.showScores) {
                ScoreboardOverlay(vm.players) { vm.toggleScores() }
            }
        }
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String, onRollcall: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                        )
                    )
                )
                .padding(vertical = 24.dp, horizontal = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.Black, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black.copy(alpha = 0.8f))
                )
                AssistChip(
                    onClick = onRollcall,
                    label = { Text("Start rollcall", color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = {
                        Icon(Icons.Rounded.HowToReg, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color.White)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickActionsRow(
    onRollcall: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionChip(icon = Icons.Rounded.HowToReg, label = "Rollcall", onClick = onRollcall)
        QuickActionChip(icon = Icons.AutoMirrored.Rounded.MenuBook, label = "Rules", onClick = onRules)
        QuickActionChip(icon = Icons.Rounded.Settings, label = "Settings", onClick = onSettings)
        QuickActionChip(icon = Icons.Rounded.Insights, label = "Stats", onClick = onStats)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HeatCard(heat: Float, onHeatChanged: (Float) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Heat threshold: ${(heat * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = heat,
                onValueChange = { onHeatChanged(it.coerceIn(0.5f, 0.8f)) },
                valueRange = 0.5f..0.8f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}
