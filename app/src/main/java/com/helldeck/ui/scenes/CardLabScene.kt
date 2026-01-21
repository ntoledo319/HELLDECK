package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.AppCtx
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.content.engine.GameEngine
import com.helldeck.content.generator.CardLabBanlist
import com.helldeck.engine.Config
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class GenerationStats(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val timings: List<Long>,
    val p50: Long,
    val p95: Long,
    val p99: Long,
)

data class LabResult(
    val text: String,
    val options: String,
    val slots: Map<String, String> = emptyMap(),
    val slotTypes: Map<String, String> = emptyMap(),
    val features: List<String> = emptyList(),
    val pairScore: Double? = null,
    val blueprintId: String? = null,
    // Humor metrics (if present)
    val spice: Int? = null,
    val humorScore: Double? = null,
    val absurdity: Double? = null,
    val shockValue: Double? = null,
    val relatability: Double? = null,
    val cringeFactor: Double? = null,
    val benignViolation: Double? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardLabScene(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf(GameMetadata.getAllGameIds().firstOrNull() ?: "ROAST_CONSENSUS") }
    var seedText by remember { mutableStateOf("12345") }
    var seedRangeEnabled by remember { mutableStateOf(false) }
    var seedRangeEnd by remember { mutableStateOf("12355") }
    var countText by remember { mutableStateOf("10") }
    var isGenerating by remember { mutableStateOf(false) }
    var generationStats by remember { mutableStateOf<GenerationStats?>(null) }

    var outputs by remember { mutableStateOf(listOf<LabResult>()) }
    var banlist by remember { mutableStateOf(CardLabBanlist.load(AppCtx.ctx)) }
    val expanded = remember { mutableStateOf(setOf<Int>()) }
    var filterLowPair by remember { mutableStateOf(false) }
    var filterAbEqual by remember { mutableStateOf(false) }
    var filterShort by remember { mutableStateOf(false) }
    var filterLong by remember { mutableStateOf(false) }
    var filterHighRepeat by remember { mutableStateOf(false) }
    var forceV3 by remember { mutableStateOf(false) }
    var localityCap by remember { mutableStateOf(Config.generatorLocalityCap()) }
    var spiceCap by remember { mutableStateOf(2) }
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Default") }
    val originalGoldOnly = remember { Config.current.generator.safe_mode_gold_only }
    val originalV3 = remember { Config.current.generator.enable_v3_generator }
    val originalLocalityCap = remember { Config.generatorLocalityCap() }
    var showHelp by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf(0f) }

    // Restore flags and save banlist when leaving the lab
    DisposableEffect(Unit) {
        onDispose {
            Config.setSafeModeGoldOnly(originalGoldOnly)
            Config.setEnableV3Generator(originalV3)
            Config.setLocalityCap(originalLocalityCap)
            CardLabBanlist.save(AppCtx.ctx, banlist)
            ContentEngineProvider.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧪 Card Lab", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Text("❓", fontSize = 20.sp)
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Controls
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = false, onExpandedChange = { }) {
                    OutlinedTextField(
                        value = selectedGame,
                        onValueChange = {},
                        label = { Text("Game ID") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Simple next/prev for games
                TextButton(onClick = {
                    val idx = GameMetadata.getAllGameIds().indexOf(selectedGame).coerceAtLeast(0)
                    selectedGame = GameMetadata.getAllGameIds()[(idx - 1 + GameMetadata.getAllGameIds().size) % GameMetadata.getAllGameIds().size]
                }) { Text("Prev") }
                TextButton(onClick = {
                    val idx = GameMetadata.getAllGameIds().indexOf(selectedGame).coerceAtLeast(0)
                    selectedGame = GameMetadata.getAllGameIds()[(idx + 1) % GameMetadata.getAllGameIds().size]
                }) { Text("Next") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = seedText,
                    onValueChange = { seedText = it },
                    label = { Text(if (seedRangeEnabled) "Start Seed" else "Seed") },
                    modifier = Modifier.weight(1f),
                )
                if (seedRangeEnabled) {
                    Text("→", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(
                        value = seedRangeEnd,
                        onValueChange = { seedRangeEnd = it },
                        label = { Text("End Seed") },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Count/Seed") },
                    modifier = Modifier.weight(0.8f),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip(label = "Seed range", selected = seedRangeEnabled) { seedRangeEnabled = it }
            }

            Text("Filters", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip("Low pair", filterLowPair) { filterLowPair = it }
                ToggleChip("A/B equal", filterAbEqual) { filterAbEqual = it }
                ToggleChip("Short", filterShort) { filterShort = it }
                ToggleChip("Long", filterLong) { filterLong = it }
                ToggleChip("High repeat", filterHighRepeat) { filterHighRepeat = it }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search text/blueprint…") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = {
                    sortMode = when (sortMode) {
                        "Default" -> "Humor"
                        "Humor" -> "Pair Asc"
                        "Pair Asc" -> "Word Count"
                        else -> "Default"
                    }
                }) { Text("Sort: $sortMode") }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {
                        forceV3 = !forceV3
                        if (forceV3) {
                            Config.setSafeModeGoldOnly(false)
                            Config.setEnableV3Generator(true)
                        } else {
                            Config.setSafeModeGoldOnly(originalGoldOnly)
                            Config.setEnableV3Generator(originalV3)
                        }
                        ContentEngineProvider.reset()
                    },
                    label = { Text(if (forceV3) "Force V3: ON" else "Force V3: OFF") },
                )
                val bannedCount = banlist.bannedBlueprints.size + banlist.bannedLexiconItems.values.sumOf { it.size }
                if (bannedCount > 0) {
                    AssistChip(
                        onClick = {
                            banlist = CardLabBanlist()
                            CardLabBanlist.save(AppCtx.ctx, banlist)
                            ContentEngineProvider.updateBanlist(banlist)
                        },
                        label = { Text("Clear Bans ($bannedCount)") },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Locality cap: $localityCap", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = localityCap.toFloat(),
                    onValueChange = { localityCap = it.roundToInt().coerceIn(1, 3) },
                    valueRange = 1f..3f,
                    steps = 1,
                    onValueChangeFinished = {
                        Config.setLocalityCap(localityCap)
                        ContentEngineProvider.reset()
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Spice max: $spiceCap", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = spiceCap.toFloat(),
                    onValueChange = { spiceCap = it.roundToInt().coerceIn(0, 5) },
                    valueRange = 0f..5f,
                    steps = 4,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlowButton(
                    text = if (isGenerating) "⌛ Generating..." else "⚡ Generate Cards",
                    onClick = {
                        if (!isGenerating) {
                            isGenerating = true
                            generationProgress = 0f
                            scope.launch {
                                try {
                                    // Simulate progress (real progress tracking would need VM updates)
                                    launch {
                                        while (isGenerating && generationProgress < 0.9f) {
                                            delay(100)
                                            generationProgress += 0.05f
                                        }
                                    }
                                    val startSeed = seedText.toLongOrNull() ?: 1234L
                                    val endSeed = if (seedRangeEnabled) seedRangeEnd.toLongOrNull() ?: (startSeed + 10) else startSeed
                                    val countPerSeed = countText.toIntOrNull()?.coerceIn(1, 100) ?: 10
                                    val seeds = if (seedRangeEnabled) {
                                        (startSeed..endSeed.coerceAtLeast(startSeed)).toList()
                                    } else {
                                        listOf(startSeed)
                                    }

                                    val results = mutableListOf<LabResult>()
                                    val timings = mutableListOf<Long>()
                                    var passCount = 0
                                    var failCount = 0

                                    seeds.forEach { seed ->
                                        val engine = ContentEngineProvider.get(context, seed)
                                        repeat(countPerSeed) { i ->
                                            val startTime = System.nanoTime()
                                            val req = GameEngine.Request(
                                                sessionId = "lab_${seed}_$i",
                                                gameId = selectedGame,
                                                players = listOf("Jay", "Pip", "Mo"),
                                                spiceMax = spiceCap,
                                                localityMax = localityCap,
                                            )

                                            val r = engine.next(req)
                                            val endTime = System.nanoTime()
                                            val durationMs = (endTime - startTime) / 1_000_000
                                            timings.add(durationMs)

                                            val meta = r.filledCard.metadata
                                            val slots = (meta["slots"] as? Map<*, *>)
                                                ?.mapNotNull { (k, v) ->
                                                    val key = k as? String ?: return@mapNotNull null
                                                    val value = v as? String ?: return@mapNotNull null
                                                    key to value
                                                }
                                                ?.toMap() ?: emptyMap()
                                            val slotTypes = (meta["slot_types"] as? Map<*, *>)
                                                ?.mapNotNull { (k, v) ->
                                                    val key = k as? String ?: return@mapNotNull null
                                                    val value = v as? String ?: return@mapNotNull null
                                                    key to value
                                                }
                                                ?.toMap() ?: emptyMap()
                                            val features = (meta["features"] as? List<*>)
                                                ?.mapNotNull { it as? String } ?: emptyList()
                                            val pairScore = (meta["pairScore"] as? Number)?.toDouble()
                                            val blueprintId = r.filledCard.id
                                            val humorScore = (meta["humorScore"] as? Number)?.toDouble()
                                            val absurdity = (meta["absurdity"] as? Number)?.toDouble()
                                            val shockValue = (meta["shockValue"] as? Number)?.toDouble()
                                            val relatability = (meta["relatability"] as? Number)?.toDouble()
                                            val cringeFactor = (meta["cringeFactor"] as? Number)?.toDouble()
                                            val benignViolation = (meta["benignViolation"] as? Number)?.toDouble()

                                            // Check if generation passed quality gates
                                            val hasPlaceholders = r.filledCard.text.contains('{') || r.filledCard.text.contains('}')
                                            val wordCount = r.filledCard.text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                                            val passed = !hasPlaceholders && wordCount in 5..32

                                            if (passed) passCount++ else failCount++

                                            results += LabResult(
                                                text = r.filledCard.text,
                                                options = r.options.toString(),
                                                slots = slots,
                                                slotTypes = slotTypes,
                                                features = features,
                                                pairScore = pairScore,
                                                blueprintId = blueprintId,
                                                spice = r.filledCard.spice,
                                                humorScore = humorScore,
                                                absurdity = absurdity,
                                                shockValue = shockValue,
                                                relatability = relatability,
                                                cringeFactor = cringeFactor,
                                                benignViolation = benignViolation,
                                            )
                                        }
                                    }

                                    // Calculate stats
                                    if (timings.isNotEmpty()) {
                                        val sorted = timings.sorted()
                                        val p50 = sorted[sorted.size / 2]
                                        val p95 = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)]
                                        val p99 = sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.lastIndex)]
                                        generationStats = GenerationStats(
                                            total = timings.size,
                                            passed = passCount,
                                            failed = failCount,
                                            timings = timings,
                                            p50 = p50,
                                            p95 = p95,
                                            p99 = p99,
                                        )
                                    }

                                    outputs = results
                                    generationProgress = 1f
                                } finally {
                                    isGenerating = false
                                    generationProgress = 0f
                                }
                            }
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = HelldeckColors.colorPrimary,
                )

                if (isGenerating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = generationProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = HelldeckColors.colorPrimary,
                    )
                    Text(
                        text = "${(generationProgress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = HelldeckColors.colorMuted,
                    )
                }
            }

            // Display generation stats
            generationStats?.let { stats ->
                HorizontalDivider(color = HelldeckColors.MediumGray.copy(alpha = 0.4f))
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📊 Generation Stats", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Total: ${stats.total} | ✅ Pass: ${stats.passed} | ❌ Fail: ${stats.failed}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        InfoBanner(
                            message = "💡 Tip: Use filters to test specific generation patterns. Tap ? for detailed help.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val passRate = if (stats.total > 0) (stats.passed * 100.0 / stats.total) else 0.0
                        Text(
                            "Pass Rate: ${"%.1f".format(passRate)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (passRate >= 95.0) HelldeckColors.Green else HelldeckColors.Red,
                        )
                    }
                }
            }

            HorizontalDivider(color = HelldeckColors.MediumGray.copy(alpha = 0.4f))
            Text("Results (${outputs.size})", style = MaterialTheme.typography.titleSmall)

            val q = query.trim().lowercase()
            val filtered = outputs.filter { item ->
                val matchesQuery = q.isBlank() ||
                    item.text.lowercase().contains(q) ||
                    (item.blueprintId?.lowercase()?.contains(q) == true)
                val words = item.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                val wc = words.size
                val counts = words.groupingBy { it.lowercase() }.eachCount()
                val top = counts.values.maxOrNull() ?: 0
                val repeat = if (wc == 0) 0.0 else top.toDouble() / wc
                val abEqual = item.options.contains("optionA=") && Regex("optionA=([^,]+), optionB=([^\\)]+)").find(item.options)?.let { m ->
                    m.groupValues[1].trim().equals(m.groupValues[2].trim(), ignoreCase = true)
                } ?: false

                val lowPairOk = !filterLowPair || (item.pairScore != null && item.pairScore < 0.0)
                val abEqualOk = !filterAbEqual || abEqual
                val shortOk = !filterShort || wc < 6
                val longOk = !filterLong || wc > 30
                val repeatOk = !filterHighRepeat || repeat > 0.4
                matchesQuery && lowPairOk && abEqualOk && shortOk && longOk && repeatOk
            }

            val sorted = when (sortMode) {
                "Humor" -> filtered.sortedByDescending { it.humorScore ?: -1.0 }
                "Pair Asc" -> filtered.sortedBy { it.pairScore ?: Double.MAX_VALUE }
                "Word Count" -> filtered.sortedByDescending {
                    it.text.split(Regex("\\s+")).count { w -> w.isNotBlank() }
                }
                else -> filtered
            }

            val clipboard = LocalClipboardManager.current
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sorted.indices.toList()) { idx ->
                    val item = sorted[idx]
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.text, style = MaterialTheme.typography.bodyLarge)
                            Text(item.options, style = MaterialTheme.typography.labelMedium)
                            val isOpen = expanded.value.contains(idx)
                            TextButton(onClick = {
                                expanded.value = if (isOpen) expanded.value - idx else expanded.value + idx
                            }) { Text(if (isOpen) "Hide details" else "Show details") }

                            if (isOpen) {
                                if (item.slots.isNotEmpty()) {
                                    Text("Slots:", style = MaterialTheme.typography.titleSmall)
                                    item.slots.forEach { (k, v) ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "- $k: $v",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f),
                                            )
                                            val slotType = item.slotTypes[k]
                                            if (slotType != null && !banlist.isLexiconItemBanned(slotType, v)) {
                                                TextButton(
                                                    onClick = {
                                                        banlist = banlist.withBannedLexiconItem(slotType, v)
                                                        CardLabBanlist.save(AppCtx.ctx, banlist)
                                                        ContentEngineProvider.updateBanlist(banlist)
                                                    },
                                                    contentPadding = PaddingValues(4.dp),
                                                ) {
                                                    Text("🚫 Ban", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                                if (item.features.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Features:", style = MaterialTheme.typography.titleSmall)
                                    Text(item.features.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                }
                                item.pairScore?.let { ps ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Pair score: ${"%.2f".format(ps)}", style = MaterialTheme.typography.bodySmall)
                                }
                                // Humor metrics
                                if (item.humorScore != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Humor: ${"%.2f".format(item.humorScore)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HelldeckColors.Yellow,
                                    )
                                    val details = listOfNotNull(
                                        item.absurdity?.let { "absurd ${"%.2f".format(it)}" },
                                        item.shockValue?.let { "shock ${"%.2f".format(it)}" },
                                        item.relatability?.let { "relate ${"%.2f".format(it)}" },
                                        item.cringeFactor?.let { "cringe ${"%.2f".format(it)}" },
                                        item.benignViolation?.let { "benign ${"%.2f".format(it)}" },
                                    ).joinToString(", ")
                                    if (details.isNotBlank()) {
                                        Text(details, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { clipboard.setText(AnnotatedString(item.text)) },
                                        contentPadding = PaddingValues(4.dp),
                                    ) {
                                        Text("Copy Text")
                                    }
                                    OutlinedButton(
                                        onClick = { clipboard.setText(AnnotatedString(item.options)) },
                                        contentPadding = PaddingValues(4.dp),
                                    ) {
                                        Text("Copy Options")
                                    }
                                }
                                item.blueprintId?.let { bpId ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "Blueprint: $bpId",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (!banlist.isBlueprintBanned(bpId)) {
                                            TextButton(
                                                onClick = {
                                                    banlist = banlist.withBannedBlueprint(bpId)
                                                    CardLabBanlist.save(AppCtx.ctx, banlist)
                                                    ContentEngineProvider.updateBanlist(banlist)
                                                },
                                                contentPadding = PaddingValues(4.dp),
                                            ) {
                                                Text(
                                                    "🚫 Ban Blueprint",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    
    // Help dialog
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            icon = { Text("🧪", fontSize = 48.sp) },
            title = { Text("Card Lab Guide") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "The Card Lab is a developer tool for testing and debugging card generation.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    
                    item {
                        Text("🎮 Game Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Choose which game to generate cards for. Use Prev/Next to cycle through games.", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    item {
                        Text("🎲 Seeds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Seed numbers control randomness. Same seed = same cards. Enable Range to test multiple seeds.", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    item {
                        Text("🔍 Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Test specific generation patterns:\n• Low Pair: Cards with weak word relationships\n• A/B Equal: Cards with similar option lengths\n• Short/Long: Test text length extremes\n• High Repeat: Cards with repetitive words", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    item {
                        Text("📊 Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Pass/Fail counts show quality gate success. Timing shows generation performance (p50/p95/p99).", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    item {
                        Text("🚫 Banning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Ban problematic blueprints or lexicon items to prevent them from appearing in games.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                com.helldeck.ui.components.GlowButton(
                    text = "Got It",
                    onClick = { showHelp = false },
                )
            },
        )
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Text("✓", color = HelldeckColors.Yellow, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = HelldeckColors.MediumGray,
            selectedContainerColor = HelldeckColors.DarkGray,
            selectedLabelColor = HelldeckColors.Yellow,
            labelColor = Color.White,
        ),
    )
}
