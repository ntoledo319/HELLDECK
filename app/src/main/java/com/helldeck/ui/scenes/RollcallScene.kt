package com.helldeck.ui.scenes

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.toEntity
import com.helldeck.settings.SettingsStore
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.Scene
import com.helldeck.ui.components.*
import com.helldeck.ui.theme.HelldeckSpacing
import com.helldeck.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Enhanced RollcallScene with design consistency and session memory.
 * 
 * Improvements:
 * - Remembers last attendance across sessions
 * - Uses centralized AddPlayerDialog
 * - Neon card styling
 * - Clear visual states for present/absent
 * - Better instructions and validation
 * - Bulk select/deselect options
 * - Quick add player without leaving scene
 * 
 * @ai_prompt Redesigned RollcallScene with HELLDECK aesthetic and UX improvements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollcallScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var present by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        present = SettingsStore.readLastAttendance()
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    val players = vm.players
    val presentPlayers = players.filter { it.id in present }
    val absentPlayers = players.filter { it.id !in present }
    
    // Validation
    val countWarning = ValidationUtils.getPlayerCountWarning(presentPlayers.size)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "📋 Rollcall",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${presentPlayers.size} present • ${absentPlayers.size} absent",
                            style = MaterialTheme.typography.labelMedium,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = { vm.goBack() }) {
                        Text("Back")
                    }
                },
                actions = {
                    if (players.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                // Select all
                                present = players.map { it.id }
                            },
                        ) {
                            Text("All")
                        }
                        TextButton(
                            onClick = {
                                // Deselect all
                                present = emptyList()
                            },
                        ) {
                            Text("None")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Quick skip — start immediately with current active players
            val activeCount = vm.players.filter { it.afk == 0 }.size
            if (activeCount >= 2) {
                GlowButton(
                    text = "Everyone's Here — Start Playing ($activeCount)",
                    icon = "\uD83D\uDE80",
                    onClick = {
                        scope.launch {
                            SettingsStore.writeLastAttendance(present)
                            vm.startRound()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HelldeckSpacing.Medium.dp)
                        .padding(top = HelldeckSpacing.Small.dp),
                    accentColor = HelldeckColors.colorSecondary,
                )
                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            }

            // Instructions
            InfoBanner(
                message = "Or tap players below to adjust who's playing",
                icon = "ℹ️",
                modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
            )

            // Validation warnings
            if (countWarning != null) {
                WarningBanner(
                    message = countWarning,
                    modifier = Modifier.padding(horizontal = HelldeckSpacing.Medium.dp),
                )
                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            }
            
            if (players.isEmpty()) {
                // Empty state
                EmptyState(
                    icon = "👥",
                    title = "No Players Added",
                    message = "Add players to your roster before starting rollcall",
                    actionLabel = "Add Players",
                    onActionClick = { vm.navigateTo(Scene.PLAYERS) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(HelldeckSpacing.Medium.dp),
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                ) {
                    // Present players
                    if (presentPlayers.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Present",
                                subtitle = "${presentPlayers.size} playing",
                            )
                        }

                        itemsIndexed(presentPlayers, key = { _, p -> p.id }) { index, player ->
                            RollcallSpringItem(index = index) {
                                RollcallPlayerCard(
                                    player = player,
                                    isPresent = true,
                                    onClick = {
                                        present = present - player.id
                                    },
                                )
                            }
                        }
                    }

                    // Absent players
                    if (absentPlayers.isNotEmpty()) {
                        item {
                            if (presentPlayers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                            }
                            SectionHeader(
                                title = "Absent",
                                subtitle = "${absentPlayers.size} not playing",
                            )
                        }

                        itemsIndexed(absentPlayers, key = { _, p -> p.id }) { index, player ->
                            RollcallSpringItem(index = index) {
                                RollcallPlayerCard(
                                    player = player,
                                    isPresent = false,
                                    onClick = {
                                        present = present + player.id
                                    },
                                )
                            }
                        }
                    }
                }
                
                // Bottom actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HelldeckSpacing.Large.dp),
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                ) {
                    // Quick add player
                    OutlineButton(
                        text = "Quick Add Player",
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = "➕",
                    )
                    
                    // Massive Start session CTA
                    GlowButton(
                        text = if (presentPlayers.size >= 2) {
                            "Start Session (${presentPlayers.size})"
                        } else {
                            "Need at least 2 players"
                        },
                        icon = if (presentPlayers.size >= 2) "\uD83C\uDFAE" else "\u26A0\uFE0F",
                        onClick = {
                            if (presentPlayers.size >= 2) {
                                scope.launch {
                                    SettingsStore.writeLastAttendance(present)
                                    vm.reloadPlayers()
                                    snackbarHostState.showSnackbar(
                                        "Session started with ${presentPlayers.size} players!"
                                    )
                                }
                                vm.goBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HelldeckHeights.Button.dp + 12.dp),
                        enabled = presentPlayers.size >= 2,
                        accentColor = if (presentPlayers.size >= 2) {
                            HelldeckColors.colorSecondary
                        } else {
                            HelldeckColors.colorMuted
                        },
                    )
                }
            }
        }
    }
    
    // Add player dialog
    if (showAddDialog) {
        AddPlayerDialog(
            existingPlayers = players,
            onDismiss = { showAddDialog = false },
            onPlayerCreated = { name, emoji ->
                scope.launch {
                    val id = ValidationUtils.generateUniquePlayerId(players)
                    repo.db.players().upsert(
                        com.helldeck.data.PlayerEntity(
                            id = id,
                            name = name,
                            avatar = emoji,
                            sessionPoints = 0,
                            afk = 0,
                        )
                    )
                    vm.reloadPlayers()
                    // Automatically mark new player as present
                    present = present + id
                    snackbarHostState.showSnackbar("Added ${name} to session")
                }
            },
        )
    }
}

@Composable
private fun RollcallSpringItem(
    index: Int,
    content: @Composable () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "rollcall_spring_$index",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = 200,
            delayMillis = (index * 50).coerceAtMost(250),
        ),
        label = "rollcall_alpha_$index",
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
private fun RollcallPlayerCard(
    player: com.helldeck.content.model.Player,
    isPresent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current

    NeonCard(
        modifier = modifier.fillMaxWidth(),
        accentColor = if (isPresent) {
            HelldeckColors.colorSecondary
        } else {
            HelldeckColors.colorMuted.copy(alpha = 0.5f)
        },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Big avatar with glow when present
                Box(
                    modifier = if (!reducedMotion && isPresent) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(HelldeckRadius.Large),
                            spotColor = HelldeckColors.colorSecondary.copy(alpha = 0.5f),
                            ambientColor = HelldeckColors.colorSecondary.copy(alpha = 0.3f),
                        )
                    } else {
                        Modifier
                    },
                ) {
                    Text(
                        text = player.avatar,
                        fontSize = 48.sp,
                    )
                }
                Column {
                    Text(
                        text = "Seat", // Anonymized - no player names
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPresent) {
                            HelldeckColors.colorOnDark
                        } else {
                            HelldeckColors.colorMuted
                        },
                    )
                    Text(
                        text = if (isPresent) "Playing" else "Sitting out",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isPresent) HelldeckColors.colorSecondary else HelldeckColors.colorMuted,
                    )
                }
            }

            // Status indicator
            Surface(
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                color = if (isPresent) {
                    HelldeckColors.colorSecondary.copy(alpha = 0.2f)
                } else {
                    HelldeckColors.surfaceElevated
                },
            ) {
                Text(
                    text = if (isPresent) "\u2705" else "\u274C",
                    fontSize = 36.sp,
                    modifier = Modifier.padding(horizontal = HelldeckSpacing.Large.dp, vertical = HelldeckSpacing.Small.dp),
                )
            }
        }
    }
}
