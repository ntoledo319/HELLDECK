package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.*
import com.helldeck.ui.components.GamePickerSheet
import com.helldeck.ui.components.SpiceSlider
import kotlinx.coroutines.launch

/**
 * Modernized home screen with spice slider and direct game selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSceneModern(vm: HelldeckVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showGamePicker by remember { mutableStateOf(false) }
    var currentSpiceLevel by remember { mutableStateOf(3) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "HELLDECK",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                },
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
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = "Rules")
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App title card
            HellTitleCard()

            Spacer(modifier = Modifier.height(32.dp))

            // Spice slider
            SpiceSlider(
                spiceLevel = currentSpiceLevel,
                onSpiceLevelChanged = { newLevel ->
                    currentSpiceLevel = newLevel
                    // Update VM spice level
                    scope.launch {
                        // vm.updateSpiceLevel(newLevel) // If you have this method
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Primary CTA: Start Random Game
            Button(
                onClick = {
                    scope.launch {
                        vm.startRound(null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Rounded.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Start Random Game",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary CTA: Choose Game
            OutlinedButton(
                onClick = { showGamePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Rounded.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Choose Specific Game",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.People,
                    label = "Players",
                    modifier = Modifier.weight(1f),
                    onClick = { vm.navigateTo(Scene.PLAYERS) }
                )

                QuickActionCard(
                    icon = Icons.Rounded.HowToReg,
                    label = "Rollcall",
                    modifier = Modifier.weight(1f),
                    onClick = { vm.navigateTo(Scene.ROLLCALL) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI indicator (if available)
            if (com.helldeck.content.engine.ContentEngineProvider.isAIEnhancementAvailable()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI-Generated Cards Active ✨",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Game picker modal
    if (showGamePicker) {
        GamePickerSheet(
            onGameSelected = { gameId ->
                showGamePicker = false
                scope.launch {
                    vm.startRound(gameId)
                }
            },
            onDismiss = { showGamePicker = false }
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}
