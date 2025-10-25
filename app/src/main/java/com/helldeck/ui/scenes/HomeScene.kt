package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.engine.Config
import com.helldeck.ui.*
import com.helldeck.ui.components.GameTile
import com.helldeck.ui.theme.HelldeckColors
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScene(vm: HelldeckVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ContentRepository(AppCtx.ctx) }

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedCardFace(
                title = "Single phone. One card per round.",
                subtitle = "Long-press to draw • two-finger = back",
                delayMs = 200
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.navigateTo(Scene.ROLLCALL) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.HowToReg, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rollcall")
                }
                OutlinedButton(
                    onClick = { vm.navigateTo(Scene.RULES) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rules")
                }
                OutlinedButton(
                    onClick = { vm.navigateTo(Scene.SETTINGS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Settings")
                }
            }

            Text(
                text = "Games",
                style = MaterialTheme.typography.titleMedium,
                color = HelldeckColors.LightGray,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until com.helldeck.engine.Games.size) {
                    val game = com.helldeck.engine.Games[i]
                    GameTile(
                        title = game.title,
                        subtitle = game.description,
                        icon = gameIconFor(game.id),
                        onClick = { scope.launch { vm.startRound(game.id) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
            Text(
                text = "Heat threshold: ${(vm.heatThreshold * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = HelldeckColors.LightGray
            )
            Slider(
                value = vm.heatThreshold,
                onValueChange = { vm.heatThreshold = it.coerceIn(0.5f, 0.8f) },
                valueRange = 0.5f..0.8f,
                steps = 5,
                onValueChangeFinished = {
                    Config.setRoomHeatThreshold(vm.heatThreshold.toDouble())
                    vm.spicy = vm.heatThreshold >= 0.70f
                    Config.spicyMode = vm.spicy
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (vm.showScores) {
                ScoreboardOverlay(vm.players) { vm.toggleScores() }
            }
        }
    }
}
