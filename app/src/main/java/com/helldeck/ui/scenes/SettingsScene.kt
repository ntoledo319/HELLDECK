package com.helldeck.ui.scenes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.PlayerEntity
import com.helldeck.data.toEntity
import com.helldeck.engine.Config
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.hdFieldColors
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScene(onClose: () -> Unit, vm: HelldeckVm) {
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    var learningEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var heat by remember { mutableStateOf(Config.roomHeatThreshold().toFloat()) }
    var soundEnabled by remember { mutableStateOf(true) }
    var rollcallOnLaunch by remember { mutableStateOf(true) }

    var expandedPlayers by remember { mutableStateOf(true) }
    var expandedGame by remember { mutableStateOf(false) }
    var expandedFeedback by remember { mutableStateOf(false) }
    var expandedDevice by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // NOTE: Settings DAO not implemented yet, using defaults
        learningEnabled = true
        hapticsEnabled = true
        soundEnabled = true
        heat = Config.roomHeatThreshold().toFloat()
        rollcallOnLaunch = true
        Config.setLearningEnabled(learningEnabled)
        Config.setHapticsEnabled(hapticsEnabled)
        Config.setRoomHeatThreshold(heat.toDouble())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Settings") },
                actions = { TextButton(onClick = onClose) { Text("Close") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Players Section
            item {
                SettingsSection(
                    title = "👥 Players",
                    isExpanded = expandedPlayers,
                    onToggle = { expandedPlayers = !expandedPlayers }
                ) {
                    var newName by remember { mutableStateOf("") }
                    val emojis = listOf("😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺")
                    var newEmoji by remember { mutableStateOf(emojis.random()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { newEmoji = emojis.random() },
                            modifier = Modifier.size(56.dp)
                        ) { Text(newEmoji, style = MaterialTheme.typography.headlineSmall) }

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Player name") },
                            placeholder = { Text("e.g., Jay") },
                            modifier = Modifier.weight(1f),
                            colors = hdFieldColors(),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                val id = "p${Random.nextInt(100000)}"
                                scope.launch {
                                    repo.db.players().upsert(
                                        PlayerEntity(id = id, name = newName.trim(), avatar = newEmoji, sessionPoints = 0)
                                    )
                                    vm.reloadPlayers()
                                    newName = ""
                                    newEmoji = emojis.random()
                                }
                            }
                        },
                        enabled = newName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
                    ) { Text("➕ Add Player") }

                    if (vm.players.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Active Players", style = MaterialTheme.typography.titleSmall, color = HelldeckColors.Yellow)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    vm.players.forEach { p ->
                        SettingRow(
                            label = "${p.avatar} ${p.name}",
                            isChecked = p.afk == 0,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    repo.db.players().update(p.copy(afk = if (checked) 0 else 1).toEntity())
                                    vm.reloadPlayers()
                                }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = { vm.goPlayers() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("📝 Manage Players") }
                }
            }

            // Game Section
            item {
                SettingsSection(
                    title = "🎮 Game",
                    isExpanded = expandedGame,
                    onToggle = { expandedGame = !expandedGame }
                ) {
                    SettingRow(
                        label = "Smart Learning",
                        description = "AI learns which cards players love",
                        isChecked = learningEnabled,
                        onCheckedChange = {
                            learningEnabled = it
                            Config.setLearningEnabled(it)
                            // NOTE: Settings DAO not implemented yet
                            // scope.launch { repo.db.settings().putBoolean("learning_enabled", it) }
                        }
                    )

                    SettingRow(
                        label = "Rollcall at Launch",
                        description = "Ask who's here when app starts",
                        isChecked = rollcallOnLaunch,
                        onCheckedChange = {
                            rollcallOnLaunch = it
                            // NOTE: Settings DAO not implemented yet
                            // scope.launch { repo.db.settings().putBoolean("rollcall_on_launch", it) }
                        }
                    )

                    SettingRow(
                        label = "AI Enhancements (Offline)",
                        description = "Use on-device AI to paraphrase cards",
                        isChecked = com.helldeck.content.engine.ContentEngineProvider.isAIEnhancementAvailable(),
                        onCheckedChange = {
                            // This would ideally trigger a restart of the engine or app
                        }
                    )
                }
            }

            // Feedback Threshold Section
            item {
                SettingsSection(
                    title = "🔥 Feedback Threshold",
                    isExpanded = expandedFeedback,
                    onToggle = { expandedFeedback = !expandedFeedback }
                ) {
                    Text(
                        "Room heat threshold: ${(heat * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HelldeckColors.Yellow
                    )
                    Text(
                        "Controls when spicy mode activates (≥70%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = HelldeckColors.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = heat,
                        onValueChange = { heat = it.coerceIn(0.5f, 0.8f) },
                        valueRange = 0.5f..0.8f,
                        steps = 5,
                        onValueChangeFinished = {
                            Config.setRoomHeatThreshold(heat.toDouble())
                            // NOTE: Settings DAO not implemented yet
                            // scope.launch { repo.db.settings().putFloat("room_heat_threshold", heat) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = HelldeckColors.Yellow,
                            activeTrackColor = HelldeckColors.Yellow,
                            inactiveTrackColor = HelldeckColors.MediumGray
                        )
                    )
                }
            }

            // Device Section
            item {
                SettingsSection(
                    title = "📱 Device",
                    isExpanded = expandedDevice,
                    onToggle = { expandedDevice = !expandedDevice }
                ) {
                    SettingRow(
                        label = "Haptic Feedback",
                        description = "Vibration for game events",
                        isChecked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            Config.setHapticsEnabled(it)
                            // NOTE: Settings DAO not implemented yet
                            // scope.launch { repo.db.settings().putBoolean("haptics_enabled", it) }
                        }
                    )

                    SettingRow(
                        label = "Sound Effects",
                        description = "Audio feedback (coming soon)",
                        isChecked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            // NOTE: Settings DAO not implemented yet
                            // scope.launch { repo.db.settings().putBoolean("sound_enabled", it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isExpanded) HelldeckColors.DarkGray else HelldeckColors.MediumGray
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isExpanded) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = HelldeckColors.Yellow
                )
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.titleMedium,
                    color = HelldeckColors.Yellow
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = HelldeckColors.White
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.LightGray
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HelldeckColors.Yellow,
                checkedTrackColor = HelldeckColors.Yellow.copy(alpha = 0.5f),
                uncheckedThumbColor = HelldeckColors.LightGray,
                uncheckedTrackColor = HelldeckColors.MediumGray
            )
        )
    }
}