package com.helldeck.ui.scenes

import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
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
import com.helldeck.settings.SettingsStore
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScene(onClose: () -> Unit, vm: HelldeckVm) {
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var learningEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var heat by remember { mutableStateOf(Config.roomHeatThreshold().toFloat()) }
    var soundEnabled by remember { mutableStateOf(true) }
    var rollcallOnLaunch by remember { mutableStateOf(true) }
    var reducedMotion by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }
    var noFlash by remember { mutableStateOf(true) }

    var expandedPlayers by remember { mutableStateOf(true) }
    var expandedGame by remember { mutableStateOf(false) }
    var expandedDevice by remember { mutableStateOf(false) }
    var expandedSafety by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Load all persisted settings from DataStore
        try {
            val (savedGoldOnly, savedV3) = SettingsStore.readFlags()
            // Enforce single mode: V3 on, gold-only off
            if (savedGoldOnly != false) scope.launch { SettingsStore.writeSafeGoldOnly(false) }
            if (savedV3 != true) scope.launch { SettingsStore.writeEnableV3(true) }
            Config.setSafeModeGoldOnly(false)
            Config.setEnableV3Generator(true)
            // Always-on learning
            learningEnabled = true
            Config.setLearningEnabled(true)
            
            rollcallOnLaunch = SettingsStore.readRollcallOnLaunch()
            
            hapticsEnabled = SettingsStore.readHapticsEnabled()
            Config.setHapticsEnabled(hapticsEnabled)
            
            soundEnabled = SettingsStore.readSoundEnabled()

            reducedMotion = SettingsStore.readReducedMotion()
            Config.setReducedMotion(reducedMotion)
            highContrast = SettingsStore.readHighContrast()
            Config.setHighContrast(highContrast)
            noFlash = SettingsStore.readNoFlash()
            Config.setNoFlash(noFlash)
            
            heat = Config.roomHeatThreshold().toFloat()
            // Simplified settings: no explicit performance toggle
            Config.setAttemptCap(null)
        } catch (e: Exception) {
            Log.w("SettingsScene", "Failed to load settings: ${e.message}")
        }
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
            // Safety & Filters (party-proof)
            item {
                SettingsSection(
                    title = "🛟 Safety & Filters",
                    isExpanded = expandedSafety,
                    onToggle = { expandedSafety = !expandedSafety }
                ) {
                    // Content filter: simple 3-step chaos slider (Soft / Mixed / Spicy).
                    Text(
                        text = "Chaos level",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val chaosLabel = when {
                        heat < 0.62f -> "Soft"
                        heat < 0.72f -> "Mixed"
                        else -> "Spicy"
                    }
                    Text(
                        text = "$chaosLabel (${(heat * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HelldeckColors.White
                    )
                    Slider(
                        value = heat.coerceIn(0.55f, 0.78f),
                        onValueChange = { v ->
                            // Snap to 3 stable positions.
                            val snapped = when {
                                v < 0.62f -> 0.58f
                                v < 0.72f -> 0.67f
                                else -> 0.76f
                            }
                            heat = snapped
                            vm.heatThreshold = snapped
                            Config.setRoomHeatThreshold(snapped.toDouble())
                            vm.spicy = snapped >= 0.72f
                            Config.spicyMode = vm.spicy
                        },
                        valueRange = 0.55f..0.78f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = HelldeckColors.Yellow,
                            activeTrackColor = HelldeckColors.Yellow,
                            inactiveTrackColor = HelldeckColors.LightGray.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingRow(
                        label = "High contrast",
                        description = "Bigger separation between text and surfaces",
                        isChecked = highContrast,
                        onCheckedChange = {
                            highContrast = it
                            Config.setHighContrast(it)
                            scope.launch { SettingsStore.writeHighContrast(it) }
                        }
                    )

                    SettingRow(
                        label = "Reduced motion",
                        description = "Disables non-essential animations",
                        isChecked = reducedMotion,
                        onCheckedChange = {
                            reducedMotion = it
                            Config.setReducedMotion(it)
                            scope.launch { SettingsStore.writeReducedMotion(it) }
                        }
                    )

                    SettingRow(
                        label = "No flash",
                        description = "Disables camera flash effects (safety)",
                        isChecked = noFlash,
                        onCheckedChange = {
                            noFlash = it
                            Config.setNoFlash(it)
                            scope.launch { SettingsStore.writeNoFlash(it) }
                        }
                    )
                }
            }

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

            // Game Section (simplified)
            item {
                SettingsSection(
                    title = "🎮 Game",
                    isExpanded = expandedGame,
                    onToggle = { expandedGame = !expandedGame }
                ) {
                    SettingRow(
                        label = "On-device AI (Auto)",
                        description = "Paraphrases cards using the bundled model when available.",
                        isChecked = com.helldeck.content.engine.ContentEngineProvider.isAIEnhancementAvailable(),
                        onCheckedChange = { /* no-op: AI is automatic by default */ },
                        enabled = false
                    )

                    SettingRow(
                        label = "Rollcall at Launch",
                        description = "Ask who's here when app starts",
                        isChecked = rollcallOnLaunch,
                        onCheckedChange = {
                            rollcallOnLaunch = it
                            scope.launch { SettingsStore.writeRollcallOnLaunch(it) }
                        }
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
                            scope.launch { SettingsStore.writeHapticsEnabled(it) }
                        }
                    )

                    SettingRow(
                        label = "Sound Effects",
                        description = "Audio feedback for ratings and milestones",
                        isChecked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            scope.launch {
                                SettingsStore.writeSoundEnabled(it)
                                // Update SoundManager
                                com.helldeck.audio.SoundManager.get(context).enabled = it
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                SettingsStore.resetToDefaults()
                                // Reload settings after reset
                                learningEnabled = SettingsStore.readLearningEnabled()
                                hapticsEnabled = SettingsStore.readHapticsEnabled()
                                soundEnabled = SettingsStore.readSoundEnabled()
                                rollcallOnLaunch = SettingsStore.readRollcallOnLaunch()
                                reducedMotion = SettingsStore.readReducedMotion()
                                highContrast = SettingsStore.readHighContrast()
                                noFlash = SettingsStore.readNoFlash()
                                // Enforce single-mode defaults after reset
                                Config.setSafeModeGoldOnly(false)
                                Config.setEnableV3Generator(true)
                                scope.launch {
                                    SettingsStore.writeSafeGoldOnly(false)
                                    SettingsStore.writeEnableV3(true)
                                }
                                Config.setLearningEnabled(learningEnabled)
                                Config.setHapticsEnabled(hapticsEnabled)
                                Config.setReducedMotion(reducedMotion)
                                Config.setHighContrast(highContrast)
                                Config.setNoFlash(noFlash)
                                com.helldeck.content.engine.ContentEngineProvider.reset()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HelldeckColors.Red
                        )
                    ) {
                        Text("🔄 Reset to Defaults")
                    }
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
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
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
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HelldeckColors.Yellow,
                checkedTrackColor = HelldeckColors.Yellow.copy(alpha = 0.5f),
                uncheckedThumbColor = HelldeckColors.LightGray,
                uncheckedTrackColor = HelldeckColors.MediumGray
            )
        )
    }
}
