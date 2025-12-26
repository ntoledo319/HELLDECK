package com.helldeck.ui.scenes

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.PlayerProfile
import com.helldeck.data.computePlayerProfiles
import com.helldeck.settings.CrewBrain
import com.helldeck.settings.CrewBrainStore
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckLoadingSpinner
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckVm
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScene(onClose: () -> Unit, vm: HelldeckVm = viewModel()) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var players by remember { mutableStateOf(0) }
    var templates by remember { mutableStateOf(0) }
    var profiles by remember { mutableStateOf<List<PlayerProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val brains by CrewBrainStore.brainsFlow().collectAsState(initial = vm.crewBrains)
    val activeBrainId by CrewBrainStore.activeBrainIdFlow()
        .collectAsState(initial = vm.activeCrewBrainId ?: CrewBrainStore.DEFAULT_BRAIN_ID)
    var showBrainDialog by remember { mutableStateOf(false) }
    var newBrainName by remember { mutableStateOf("") }
    var newBrainEmoji by remember { mutableStateOf("🧠") }
    var brainError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeBrainId, brains) {
        vm.activeCrewBrainId = activeBrainId
        vm.crewBrains = brains
        isLoading = true
        stats = vm.getGameStats()
        val repo = ContentRepository(AppCtx.ctx)
        players = repo.statsDao.getAll().size
        templates = repo.templatesV2().size
        profiles = repo.computePlayerProfiles()
        isLoading = false
    }

    val busy = isLoading || vm.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Stats & Insights") },
                actions = {
                    TextButton(onClick = onClose) { Text("Close") }
                }
            )
        }
    ) { padding ->
        if (busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                HelldeckLoadingSpinner()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
                .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CrewBrainSelector(
                        brains = brains,
                        activeBrainId = activeBrainId,
                        isBusy = busy,
                        onSelect = { id ->
                            if (id != activeBrainId) {
                                isLoading = true
                                scope.launch { vm.switchCrewBrain(id) }
                            }
                        },
                        onAdd = { showBrainDialog = true }
                    )
                }
                // Overview Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            icon = "👥",
                            value = players.toString(),
                            label = "Players",
                            color = HelldeckColors.Green,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = "🎴",
                            value = templates.toString(),
                            label = "Templates",
                            color = HelldeckColors.Orange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Game Stats
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = HelldeckColors.DarkGray)
                    ) {
                        Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                            Text(
                                "🎮 Recent Game Stats",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = HelldeckColors.Yellow
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            EnhancedStatRow("Total Rounds", (stats["totalRounds"] ?: 0).toString(), "🎯")
                            EnhancedStatRow("Avg Score", String.format("%.1f", stats["averageScore"] ?: 0.0), "⭐")
                            EnhancedStatRow("Most Played", (stats["mostPlayedGame"] ?: "—").toString(), "🔥")
                            EnhancedStatRow("Top Template", (stats["highestScoringTemplate"] ?: "—").toString(), "👑")
                        }
                    }
                }

                // Player Leaderboard
                item {
                    Text(
                        "🏆 Player Leaderboard",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow
                    )
                }

                items(profiles) { pr ->
                    EnhancedPlayerCard(
                        profile = pr,
                        rank = profiles.indexOf(pr) + 1,
                        onClick = { vm.openProfile(pr.id) }
                    )
                }
            }
        }
    }

    if (showBrainDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!vm.isLoading) {
                    showBrainDialog = false
                    brainError = null
                }
            },
            title = { Text("New Crew Brain") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newBrainName,
                        onValueChange = { newBrainName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBrainEmoji,
                        onValueChange = { newBrainEmoji = it.take(2) },
                        label = { Text("Emoji") },
                        singleLine = true
                    )
                    Text(
                        text = "Keep up to ${CrewBrainStore.MAX_BRAINS} brains for different crews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    brainError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !vm.isLoading,
                    onClick = {
                        scope.launch {
                            brainError = null
                            isLoading = true
                            val created = vm.createCrewBrain(newBrainName, newBrainEmoji)
                            if (created == null) {
                                brainError = "Unable to add another crew brain right now."
                                isLoading = false
                            } else {
                                newBrainName = ""
                                newBrainEmoji = "🧠"
                                showBrainDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (!vm.isLoading) {
                        showBrainDialog = false
                        brainError = null
                    }
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CrewBrainSelector(
    brains: List<CrewBrain>,
    activeBrainId: String?,
    isBusy: Boolean,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Crew Brains",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = HelldeckColors.Yellow
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            brains.forEach { brain ->
                CrewBrainChip(
                    brain = brain,
                    selected = brain.id == activeBrainId,
                    isBusy = isBusy,
                    onClick = { onSelect(brain.id) }
                )
            }
            if (brains.size < CrewBrainStore.MAX_BRAINS) {
                AddCrewBrainChip(enabled = !isBusy, onClick = onAdd)
            }
        }
    }
}

@Composable
private fun CrewBrainChip(
    brain: CrewBrain,
    selected: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        color = container,
        tonalElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier.clickable(enabled = !isBusy, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(brain.emoji, style = MaterialTheme.typography.titleMedium)
            Column {
                Text(
                    brain.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
                Text(
                    if (selected) "Active crew brain" else "Tap to switch",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else HelldeckColors.LightGray
                )
            }
        }
    }
}

@Composable
private fun AddCrewBrainChip(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("➕", style = MaterialTheme.typography.titleMedium)
            Column {
                Text(
                    "Add Crew Brain",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Max ${CrewBrainStore.MAX_BRAINS}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HelldeckColors.LightGray
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = HelldeckColors.LightGray
            )
        }
    }
}

@Composable
private fun EnhancedStatRow(label: String, value: String, icon: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = HelldeckColors.White
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = HelldeckColors.Yellow
        )
    }
}

@Composable
private fun EnhancedPlayerCard(
    profile: PlayerProfile,
    rank: Int,
    onClick: () -> Unit
) {
    // Calculate enhanced stats
    val winRate = if (profile.gamesPlayed > 0) {
        (profile.wins.toFloat() / profile.gamesPlayed * 100).toInt()
    } else 0

    val avgPointsPerRound = if (profile.gamesPlayed > 0) {
        (profile.totalPoints.toFloat() / profile.gamesPlayed)
    } else 0f

    val rankColor = when (rank) {
        1 -> HelldeckColors.Yellow     // Gold
        2 -> HelldeckColors.LightGray  // Silver-ish
        3 -> HelldeckColors.Orange     // Bronze-ish
        else -> HelldeckColors.MediumGray
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh),
        label = "player_card_scale"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (rank <= 3) rankColor.copy(alpha = 0.1f) else HelldeckColors.MediumGray
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (rank <= 3) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (rank <= 3) {
                        Brush.linearGradient(
                            colors = listOf(
                                rankColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                    }
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Rank badge
                    Surface(
                        color = rankColor.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                rank.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (rank <= 3) Color.Black else HelldeckColors.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            "${profile.avatar} ${profile.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HelldeckColors.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "🏆 ${profile.wins} wins",
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                            Text(
                                "🎮 ${profile.gamesPlayed} games",
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                            Text(
                                "📊 $winRate% win rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                        }
                        // Enhanced stats row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "⚡ ${String.format("%.1f", avgPointsPerRound)} avg pts",
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                            Text(
                                "😂 ${String.format("%.1f", profile.avgLol)} LOLs",
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                            Text(
                                "🔥 ${profile.heatRounds} heat",
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${profile.totalPoints}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow
                    )
                    Text(
                        "points",
                        style = MaterialTheme.typography.bodySmall,
                        color = HelldeckColors.LightGray
                    )
                }
            }
        }
    }
}
