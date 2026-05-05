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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.PlayerEntity
import com.helldeck.data.toEntity
import com.helldeck.engine.Config
import com.helldeck.settings.SettingsStore
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.components.OutlineButton
import com.helldeck.ui.components.SectionHeader
import com.helldeck.ui.hdFieldColors
import kotlinx.coroutines.launch

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
        try {
            val (savedGoldOnly, savedV3) = SettingsStore.readFlags()
            if (savedGoldOnly != false) scope.launch { SettingsStore.writeSafeGoldOnly(false) }
            if (savedV3 != true) scope.launch { SettingsStore.writeEnableV3(true) }
            Config.setSafeModeGoldOnly(false)
            Config.setEnableV3Generator(true)
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
            Config.setAttemptCap(null)
        } catch (e: Exception) {
            Log.w("SettingsScene", "Failed to load settings: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "\u2699\uFE0F Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Customize your experience",
                            style = MaterialTheme.typography.labelSmall,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.semantics { contentDescription = "Close settings" },
                    ) { Text("Close") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        ) {
            // Safety & Filters
            item {
                SettingsSection(
                    title = "\uD83D\uDEDF Safety & Filters",
                    isExpanded = expandedSafety,
                    onToggle = { expandedSafety = !expandedSafety },
                ) {
                    SectionHeader(
                        title = "Chaos Level",
                        subtitle = "Controls how spicy the content gets",
                    )
                    Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                    val chaosLabel = when {
                        heat < 0.62f -> "\uD83D\uDE0A Soft"
                        heat < 0.72f -> "\uD83C\uDF36\uFE0F Mixed"
                        else -> "\uD83D\uDD25 Spicy"
                    }
                    Text(
                        text = chaosLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            heat < 0.62f -> HelldeckColors.colorSecondary
                            heat < 0.72f -> HelldeckColors.Lol
                            else -> HelldeckColors.Error
                        },
                    )
                    Text(
                        text = when {
                            heat < 0.62f -> "Family-friendly, safe for all audiences"
                            heat < 0.72f -> "Mix of tame and edgy content"
                            else -> "Adult humor, not for the easily offended"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = HelldeckColors.colorMuted,
                    )
                    Slider(
                        value = heat.coerceIn(0.55f, 0.78f),
                        onValueChange = { v ->
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
                            thumbColor = HelldeckColors.colorPrimary,
                            activeTrackColor = HelldeckColors.colorPrimary,
                            inactiveTrackColor = HelldeckColors.colorMuted.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Chaos level slider"
                        },
                    )

                    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

                    SettingRow(
                        label = "High contrast",
                        description = "Bigger separation between text and surfaces",
                        isChecked = highContrast,
                        onCheckedChange = {
                            highContrast = it
                            Config.setHighContrast(it)
                            scope.launch { SettingsStore.writeHighContrast(it) }
                        },
                    )

                    SettingRow(
                        label = "Reduced motion",
                        description = "Disables non-essential animations",
                        isChecked = reducedMotion,
                        onCheckedChange = {
                            reducedMotion = it
                            Config.setReducedMotion(it)
                            scope.launch { SettingsStore.writeReducedMotion(it) }
                        },
                    )

                    SettingRow(
                        label = "No flash",
                        description = "Disables camera flash effects (safety)",
                        isChecked = noFlash,
                        onCheckedChange = {
                            noFlash = it
                            Config.setNoFlash(it)
                            scope.launch { SettingsStore.writeNoFlash(it) }
                        },
                    )
                }
            }

            // Players Section
            item {
                var showAddPlayerDialog by remember { mutableStateOf(false) }

                SettingsSection(
                    title = "\uD83D\uDC65 Players",
                    isExpanded = expandedPlayers,
                    onToggle = { expandedPlayers = !expandedPlayers },
                ) {
                    InfoBanner(
                        message = "Manage your player roster. Players persist across sessions.",
                        icon = "\u2139\uFE0F",
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                    GlowButton(
                        text = "Add Player",
                        onClick = { showAddPlayerDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Add a new player" },
                        icon = "\u2795",
                        accentColor = HelldeckColors.colorSecondary,
                    )

                    if (vm.players.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                        Text(
                            "Active Players",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = HelldeckColors.colorPrimary,
                        )
                        Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
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
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                    OutlineButton(
                        text = "Manage All Players",
                        onClick = { vm.goPlayers() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Manage all players" },
                        icon = "\uD83D\uDCDD",
                    )

                    if (showAddPlayerDialog) {
                        com.helldeck.ui.components.AddPlayerDialog(
                            existingPlayers = vm.players,
                            onDismiss = { showAddPlayerDialog = false },
                            onPlayerCreated = { name, emoji ->
                                scope.launch {
                                    val id = com.helldeck.utils.ValidationUtils.generateUniquePlayerId(vm.players)
                                    repo.db.players().upsert(
                                        PlayerEntity(
                                            id = id,
                                            name = name,
                                            avatar = emoji,
                                            sessionPoints = 0,
                                        ),
                                    )
                                    vm.reloadPlayers()
                                }
                            },
                        )
                    }
                }
            }

            // Game Section
            item {
                SettingsSection(
                    title = "\uD83C\uDFAE Game",
                    isExpanded = expandedGame,
                    onToggle = { expandedGame = !expandedGame },
                ) {
                    SettingRow(
                        label = "On-device AI (Auto)",
                        description = "Paraphrases cards using the bundled model when available.",
                        isChecked = com.helldeck.content.engine.ContentEngineProvider.isAIEnhancementAvailable(),
                        onCheckedChange = { },
                        enabled = false,
                    )

                    SettingRow(
                        label = "Rollcall at Launch",
                        description = "Automatically show player attendance screen when app starts",
                        isChecked = rollcallOnLaunch,
                        onCheckedChange = {
                            rollcallOnLaunch = it
                            scope.launch { SettingsStore.writeRollcallOnLaunch(it) }
                        },
                    )

                    InfoBanner(
                        message = "Tip: Enable this to quickly set who's playing each session",
                        icon = "\uD83D\uDCA1",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Device Section
            item {
                SettingsSection(
                    title = "\uD83D\uDCF1 Device",
                    isExpanded = expandedDevice,
                    onToggle = { expandedDevice = !expandedDevice },
                ) {
                    SettingRow(
                        label = "Haptic Feedback",
                        description = "Vibration for game events",
                        isChecked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            Config.setHapticsEnabled(it)
                            scope.launch { SettingsStore.writeHapticsEnabled(it) }
                        },
                    )

                    SettingRow(
                        label = "Sound Effects",
                        description = "Audio feedback for ratings and milestones",
                        isChecked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            scope.launch {
                                SettingsStore.writeSoundEnabled(it)
                                com.helldeck.audio.SoundManager.get(context).enabled = it
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                    OutlineButton(
                        text = "\uD83D\uDD04 Reset to Defaults",
                        onClick = {
                            scope.launch {
                                SettingsStore.resetToDefaults()
                                learningEnabled = SettingsStore.readLearningEnabled()
                                hapticsEnabled = SettingsStore.readHapticsEnabled()
                                soundEnabled = SettingsStore.readSoundEnabled()
                                rollcallOnLaunch = SettingsStore.readRollcallOnLaunch()
                                reducedMotion = SettingsStore.readReducedMotion()
                                highContrast = SettingsStore.readHighContrast()
                                noFlash = SettingsStore.readNoFlash()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Reset all settings to defaults" },
                        accentColor = HelldeckColors.Error,
                    )
                }
            }

            // Support Section (Tip Jar)
            item {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                com.helldeck.ui.components.TipJarSection(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Promo Code Section (if not premium)
            item {
                val isPremiumUnlocked = com.helldeck.billing.PurchaseManager.isPremiumUnlocked.collectAsState()
                if (!isPremiumUnlocked.value && !com.helldeck.billing.PurchaseManager.isUnlockAllMode()) {
                    Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                    com.helldeck.ui.components.PromoCodeEntry(
                        modifier = Modifier.fillMaxWidth(),
                        onRedeemed = { },
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = if (isExpanded) HelldeckColors.colorPrimary else HelldeckColors.colorMuted,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .semantics {
                        contentDescription = "$title section. ${if (isExpanded) "Expanded" else "Collapsed"}. Tap to toggle."
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = HelldeckColors.colorPrimary,
                )
                Text(
                    text = if (isExpanded) "\u25BC" else "\u25B6",
                    style = MaterialTheme.typography.titleMedium,
                    color = HelldeckColors.colorPrimary,
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
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
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HelldeckSpacing.Tiny.dp)
            .semantics {
                contentDescription = "$label. ${description ?: ""}. Currently ${if (isChecked) "enabled" else "disabled"}"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = HelldeckColors.colorOnDark,
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.colorMuted,
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HelldeckColors.colorPrimary,
                checkedTrackColor = HelldeckColors.colorPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = HelldeckColors.colorMuted,
                uncheckedTrackColor = HelldeckColors.surfaceElevated,
            ),
        )
    }
}
