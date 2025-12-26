package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.engine.Config
import com.helldeck.ui.*
import com.helldeck.ui.components.GamePickerSheet
import com.helldeck.ui.components.GameTile
import com.helldeck.ui.components.SpiceSlider
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.tween

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScene(vm: HelldeckVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val games = remember { (0 until com.helldeck.engine.Games.size).map { com.helldeck.engine.Games[it] } }
    var showGamePicker by remember { mutableStateOf(false) }
    val spiceLevel by vm.spiceLevel.collectAsState()
    val isAIAvailable = remember { ContentEngineProvider.isAIEnhancementAvailable() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HELLDECK", fontWeight = FontWeight.Black) },
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
            HellTitleCard()

            Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

            // AI Enhancement Indicator
            if (isAIAvailable) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HelldeckRadius.Small),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "AI Enhancement Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
            }

            // Spice Level Slider
            SpiceSlider(
                spiceLevel = spiceLevel,
                onSpiceLevelChanged = { vm.updateSpiceLevel(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            // Primary CTA: Start random game
            Button(
                onClick = { scope.launch { vm.startRound(null) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HelldeckHeights.Button.dp),
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.colorPrimary)
            ) {
                Text(
                    text = "Start Chaos",
                    style = MaterialTheme.typography.labelLarge,
                    color = HelldeckColors.colorOnDark
                )
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryActionButton(
                    icon = "🎮",
                    title = "Mini Games",
                    subtitle = "Pick a specific game",
                    onClick = { showGamePicker = true }
                )
                SecondaryActionButton(
                    icon = "🧠",
                    title = "Crew Brain",
                    subtitle = "Stats, highlights, and learning",
                    onClick = { vm.navigateTo(Scene.STATS) }
                )
                SecondaryActionButton(
                    icon = "🛟",
                    title = "Safety & Filters",
                    subtitle = "Chaos level, reduced motion, high contrast",
                    onClick = { vm.navigateTo(Scene.SETTINGS) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Single-phone party game • 3–12 players • 14 mini-games",
                style = MaterialTheme.typography.labelSmall,
                color = HelldeckColors.colorMuted
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
        }

        // Game Picker Modal Sheet
        if (showGamePicker) {
            GamePickerSheet(
                onGameSelected = { gameId ->
                    showGamePicker = false
                    scope.launch { vm.startRound(gameId) }
                },
                onDismiss = { showGamePicker = false }
            )
        }

        // Scoreboard Overlay
        if (vm.showScores) {
            ScoreboardOverlay(vm.players) { vm.toggleScores() }
        }
    }
}

@Composable
private fun HellTitleCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            HelldeckColors.colorPrimary.copy(alpha = 0.95f),
                            HelldeckColors.colorAccentCool.copy(alpha = 0.45f),
                            HelldeckColors.colorSecondary.copy(alpha = 0.20f)
                        )
                    )
                )
                .padding(vertical = 22.dp, horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "HELLDECK",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = HelldeckColors.colorOnDark
            )
            Text(
                text = "One possessed phone. The room is the controller.",
                style = MaterialTheme.typography.bodyMedium,
                color = HelldeckColors.colorOnDark.copy(alpha = 0.9f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryActionButton(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = icon, style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = HelldeckColors.colorOnDark)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = HelldeckColors.colorMuted)
                }
                Text(text = "›", style = MaterialTheme.typography.headlineMedium, color = HelldeckColors.colorMuted)
            )
        }
    }
}
