package com.helldeck.ui.scenes

import androidx.compose.animation.core.*
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.PlayerProfile
import com.helldeck.data.computePlayerProfiles
import com.helldeck.settings.CrewBrain
import com.helldeck.settings.CrewBrainStore
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.EmptyState
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import com.helldeck.ui.components.LoadingIndicator
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.components.SectionHeader
import com.helldeck.ui.components.StatDisplay
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
    var newBrainEmoji by remember { mutableStateOf("\uD83E\uDDE0") }
    var brainError by remember { mutableStateOf<String?>(null) }

    var showCrewBrainHelp by remember { mutableStateOf(false) }

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
                title = {
                    Column {
                        Text(
                            "\uD83D\uDCCA Stats & Insights",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Arcade scoreboard",
                            style = MaterialTheme.typography.labelSmall,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCrewBrainHelp = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Learn about Crew Brains"
                        },
                    ) {
                        Text("\u2753", fontSize = 20.sp)
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                },
            )
        },
    ) { padding ->
        if (busy) {
            LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                message = "Loading stats...",
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(HelldeckSpacing.Medium.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
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
                        onAdd = { showBrainDialog = true },
                    )

                    if (brains.size == 1) {
                        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                        InfoBanner(
                            message = "Crew Brains let you track different friend groups separately. The AI learns each group's humor!",
                            icon = "\uD83D\uDCA1",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Overview Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                    ) {
                        StatCard(
                            icon = "\uD83D\uDC65",
                            value = players.toString(),
                            label = "Players",
                            color = HelldeckColors.colorSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            icon = "\uD83C\uDCB4",
                            value = templates.toString(),
                            label = "Templates",
                            color = HelldeckColors.colorAccentWarm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Game Stats
                item {
                    NeonCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = HelldeckColors.Lol,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp)) {
                            SectionHeader(
                                title = "\uD83C\uDFAE Recent Game Stats",
                            )
                            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                            StatDisplay(
                                label = "Total Rounds",
                                value = (stats["totalRounds"] ?: 0).toString(),
                                icon = "\uD83C\uDFAF",
                                valueColor = HelldeckColors.colorPrimary,
                            )
                            StatDisplay(
                                label = "Avg Score",
                                value = String.format("%.1f", stats["averageScore"] ?: 0.0),
                                icon = "\u2B50",
                                valueColor = HelldeckColors.Lol,
                            )
                            StatDisplay(
                                label = "Most Played",
                                value = (stats["mostPlayedGame"] ?: "\u2014").toString(),
                                icon = "\uD83D\uDD25",
                                valueColor = HelldeckColors.colorAccentWarm,
                            )
                            StatDisplay(
                                label = "Top Template",
                                value = (stats["highestScoringTemplate"] ?: "\u2014").toString(),
                                icon = "\uD83D\uDC51",
                                valueColor = HelldeckColors.colorSecondary,
                            )
                        }
                    }
                }

                // Player Leaderboard
                item {
                    SectionHeader(
                        title = "\uD83C\uDFC6 Player Leaderboard",
                    )
                }

                if (profiles.isEmpty()) {
                    item {
                        EmptyState(
                            icon = "\uD83C\uDFC6",
                            title = "No Players Yet",
                            message = "Play some rounds to see the leaderboard!",
                            modifier = Modifier.height(200.dp),
                        )
                    }
                } else {
                    itemsIndexed(profiles) { index, pr ->
                        LeaderboardSpringItem(index = index) {
                            EnhancedPlayerCard(
                                profile = pr,
                                rank = index + 1,
                                onClick = { vm.openProfile(pr.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Crew Brain help dialog
    if (showCrewBrainHelp) {
        AlertDialog(
            onDismissRequest = { showCrewBrainHelp = false },
            icon = { Text("\uD83E\uDDE0", fontSize = 48.sp) },
            title = { Text("What are Crew Brains?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp)) {
                    Text(
                        "Crew Brains let you track stats and AI learning for different friend groups separately.",
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    NeonCard(accentColor = HelldeckColors.colorSecondary) {
                        Column(verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp)) {
                            Text(
                                "\u2705 Separate player rosters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorOnDark,
                            )
                            Text(
                                "\u2705 Independent stats tracking",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorOnDark,
                            )
                            Text(
                                "\u2705 AI learns each group's humor",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorOnDark,
                            )
                            Text(
                                "\u2705 Switch between groups instantly",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorOnDark,
                            )
                        }
                    }

                    Text(
                        "Example: Create one for 'Work Friends' and another for 'College Crew' to keep their games separate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HelldeckColors.colorMuted,
                    )
                }
            },
            confirmButton = {
                GlowButton(
                    text = "Got It",
                    onClick = { showCrewBrainHelp = false },
                )
            },
        )
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
                Column(verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp)) {
                    OutlinedTextField(
                        value = newBrainName,
                        onValueChange = { newBrainName = it },
                        label = { Text("Name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = newBrainEmoji,
                        onValueChange = { newBrainEmoji = it.take(2) },
                        label = { Text("Emoji") },
                        singleLine = true,
                    )
                    Text(
                        text = "Keep up to ${CrewBrainStore.MAX_BRAINS} brains for different crews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HelldeckColors.colorMuted,
                    )
                    brainError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = HelldeckColors.Error,
                        )
                    }
                }
            },
            confirmButton = {
                GlowButton(
                    text = "Create",
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
                                newBrainEmoji = "\uD83E\uDDE0"
                                showBrainDialog = false
                            }
                        }
                    },
                )
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
            },
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
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp)) {
        SectionHeader(
            title = "Crew Brains",
            subtitle = "${brains.size} groups",
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        ) {
            brains.forEach { brain ->
                CrewBrainChip(
                    brain = brain,
                    selected = brain.id == activeBrainId,
                    isBusy = isBusy,
                    onClick = { onSelect(brain.id) },
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
    onClick: () -> Unit,
) {
    val accentColor = if (selected) HelldeckColors.colorSecondary else HelldeckColors.colorMuted
    Surface(
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) accentColor else HelldeckColors.colorMuted.copy(alpha = 0.4f),
        ),
        color = if (selected) accentColor.copy(alpha = 0.15f) else HelldeckColors.surfaceElevated,
        tonalElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier
            .clickable(enabled = !isBusy, onClick = onClick)
            .semantics {
                contentDescription = "${brain.name}. ${if (selected) "Active crew brain" else "Tap to switch"}"
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HelldeckSpacing.Medium.dp, vertical = HelldeckSpacing.Small.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(brain.emoji, style = MaterialTheme.typography.titleMedium)
            Column {
                Text(
                    brain.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) HelldeckColors.colorOnDark else HelldeckColors.colorOnDark,
                )
                Text(
                    if (selected) "Active crew brain" else "Tap to switch",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) accentColor else HelldeckColors.colorMuted,
                )
            }
        }
    }
}

@Composable
private fun AddCrewBrainChip(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        border = androidx.compose.foundation.BorderStroke(1.dp, HelldeckColors.colorMuted.copy(alpha = 0.4f)),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = "Add a new crew brain. Maximum ${CrewBrainStore.MAX_BRAINS}"
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HelldeckSpacing.Medium.dp, vertical = HelldeckSpacing.Small.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("\u2795", style = MaterialTheme.typography.titleMedium)
            Column {
                Text(
                    "Add Crew Brain",
                    style = MaterialTheme.typography.bodyLarge,
                    color = HelldeckColors.colorOnDark,
                )
                Text(
                    "Max ${CrewBrainStore.MAX_BRAINS}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HelldeckColors.colorMuted,
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
    modifier: Modifier = Modifier,
) {
    NeonCard(
        modifier = modifier,
        accentColor = color,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = HelldeckColors.colorMuted,
            )
        }
    }
}

@Composable
private fun LeaderboardSpringItem(
    index: Int,
    content: @Composable () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "leaderboard_spring_$index",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            tween(
                durationMillis = 200,
                delayMillis = (index * 60).coerceAtMost(300),
            )
        },
        label = "leaderboard_alpha_$index",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .graphicsLayer { this.alpha = alpha },
    ) {
        content()
    }
}

@Composable
private fun EnhancedPlayerCard(
    profile: PlayerProfile,
    rank: Int,
    onClick: () -> Unit,
) {
    val winRate = if (profile.gamesPlayed > 0) {
        (profile.wins.toFloat() / profile.gamesPlayed * 100).toInt()
    } else {
        0
    }

    val avgPointsPerRound = if (profile.gamesPlayed > 0) {
        (profile.totalPoints.toFloat() / profile.gamesPlayed)
    } else {
        0f
    }

    val rankColor = when (rank) {
        1 -> HelldeckColors.Lol
        2 -> HelldeckColors.colorMuted
        3 -> HelldeckColors.colorAccentWarm
        else -> HelldeckColors.surfaceElevated
    }

    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = if (reducedMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "player_card_scale",
    )

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics {
                contentDescription = "Rank $rank: ${profile.name}, ${profile.totalPoints} points, $winRate percent win rate"
            },
        accentColor = if (rank <= 3) rankColor else HelldeckColors.colorMuted,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (rank <= 3) {
                        Brush.linearGradient(
                            colors = listOf(
                                rankColor.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                        )
                    } else {
                        Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                    },
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    // Rank badge
                    Surface(
                        color = rankColor.copy(alpha = 0.3f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                rank.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (rank <= 3) HelldeckColors.background else HelldeckColors.colorOnDark,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(HelldeckSpacing.Medium.dp))

                    Column {
                        Text(
                            "${profile.avatar} ${profile.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HelldeckColors.colorOnDark,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp)) {
                            Text(
                                "\uD83C\uDFC6 ${profile.wins} wins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorMuted,
                            )
                            Text(
                                "\uD83C\uDFAE ${profile.gamesPlayed} games",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorMuted,
                            )
                            Text(
                                "\uD83D\uDCCA $winRate%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorMuted,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp)) {
                            Text(
                                "\u26A1 ${String.format("%.1f", avgPointsPerRound)} avg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorAccentCool,
                            )
                            Text(
                                "\uD83D\uDE02 ${String.format("%.1f", profile.avgLol)} LOLs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.Lol,
                            )
                            Text(
                                "\uD83D\uDD25 ${profile.heatRounds}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorAccentWarm,
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        "${profile.totalPoints}",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                        color = if (rank == 1) HelldeckColors.Lol else HelldeckColors.colorAccentCool,
                    )
                    Text(
                        "points",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HelldeckColors.colorMuted,
                    )
                }
            }
        }
    }
}
