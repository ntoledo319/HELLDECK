package com.helldeck.ui.scenes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.*
import com.helldeck.ui.components.GamePickerSheet
import com.helldeck.ui.components.PrimaryButton
import com.helldeck.ui.components.SpiceSlider
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
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
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(HelldeckSpacing.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HellTitleCard()

            Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

            // AI Enhancement Indicator
            if (isAIAvailable) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HelldeckRadius.Small),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✨", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "AI Enhancement Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
            }

            // Spice Level Slider
            SpiceSlider(
                spiceLevel = spiceLevel,
                onSpiceLevelChanged = { vm.updateSpiceLevel(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            // Primary CTA: Start random game
            PrimaryButton(
                text = "🔥 Start the Chaos",
                onClick = { scope.launch { vm.startRound(null) } },
                modifier = Modifier.fillMaxWidth(),
                icon = null,
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            // Secondary actions grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryActionButton(
                        icon = "🎲",
                        title = "Mini Games",
                        subtitle = "Quick party games",
                        onClick = { showGamePicker = true },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryActionButton(
                        icon = "📖",
                        title = "Game Rules",
                        subtitle = "Learn how to play",
                        onClick = { vm.navigateTo(Scene.FULL_RULES_BROWSER) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryActionButton(
                        icon = "📊",
                        title = "Crew Brain",
                        subtitle = "View statistics",
                        onClick = { vm.navigateTo(Scene.STATS) },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryActionButton(
                        icon = "🛡️",
                        title = "Safety",
                        subtitle = "Settings & controls",
                        onClick = { vm.navigateTo(Scene.SETTINGS) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryActionButton(
                        icon = "✨",
                        title = "Custom Cards",
                        subtitle = "Create your own",
                        onClick = { vm.navigateTo(Scene.CUSTOM_CARDS) },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryActionButton(
                        icon = "⭐",
                        title = "Favorites",
                        subtitle = "Saved moments",
                        onClick = { vm.navigateTo(Scene.FAVORITES) },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Pass one phone • Judge, roast, and betray your friends • ${GameMetadata.getAllGames().size} mini-games",
                style = MaterialTheme.typography.labelSmall,
                color = HelldeckColors.colorMuted,
                textAlign = TextAlign.Center,
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
                onDismiss = { showGamePicker = false },
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
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            HelldeckColors.colorPrimary.copy(alpha = 0.95f),
                            HelldeckColors.colorAccentCool.copy(alpha = 0.45f),
                            HelldeckColors.colorSecondary.copy(alpha = 0.20f),
                        ),
                    ),
                )
                .padding(vertical = 22.dp, horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "HELLDECK",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = HelldeckColors.colorOnDark,
            )
            Text(
                text = "Low Cognitive Load. High Social Stakes. Maximum Chaos.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = HelldeckColors.colorOnDark.copy(alpha = 0.95f),
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
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = icon, style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = HelldeckColors.colorOnDark)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = HelldeckColors.colorMuted)
                }
                Text(text = "›", style = MaterialTheme.typography.headlineMedium, color = HelldeckColors.colorMuted)
            }
        }
    }
}
