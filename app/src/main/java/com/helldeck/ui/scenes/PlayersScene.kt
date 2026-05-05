package com.helldeck.ui.scenes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.toEntity
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.Scene
import com.helldeck.ui.components.*
import com.helldeck.ui.theme.HelldeckSpacing
import com.helldeck.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Enhanced PlayersScene with HELLDECK neon design, validation, and improved UX.
 * 
 * Improvements:
 * - Centralized player creation using AddPlayerDialog
 * - Full validation with duplicate checking
 * - Player count warnings and team mode suggestions
 * - Neon card styling throughout
 * - Empty state with quick setup
 * - Bulk operations (AFK all, clear all)
 * - Edit player functionality
 * - Better visual hierarchy
 * 
 * @ai_prompt Fully redesigned PlayersScene with consistent HELLDECK aesthetic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<com.helldeck.content.model.Player?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<com.helldeck.content.model.Player?>(null) }
    var showBulkMenu by remember { mutableStateOf(false) }
    
    val players = vm.players
    val activePlayers = players.filter { it.afk == 0 }
    
    // Player count validation
    val countValidation = ValidationUtils.validatePlayerCount(activePlayers.size)
    val countWarning = ValidationUtils.getPlayerCountWarning(activePlayers.size)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "👥 Players",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${activePlayers.size} active • ${players.size} total",
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
                        IconButton(onClick = { showBulkMenu = true }) {
                            Text("⋮", fontSize = 24.sp)
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            GlowButton(
                text = "Add Player",
                onClick = { showAddDialog = true },
                icon = "➕",
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Player count warning/info
            if (countWarning != null) {
                WarningBanner(
                    message = countWarning,
                    modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
                )
            } else if (!countValidation.isValid) {
                InfoBanner(
                    message = countValidation.errorMessage ?: "Add more players",
                    icon = "ℹ️",
                    modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
                )
            }
            
            if (players.isEmpty()) {
                // Empty state
                EmptyState(
                    icon = "👥",
                    title = "No Players Yet",
                    message = "Add players to start a game session.\nRecommended: 3-10 players for best experience.",
                    actionLabel = "Add First Player",
                    onActionClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(HelldeckSpacing.Medium.dp),
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                ) {
                    // Active players section
                    if (activePlayers.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Active Players",
                                subtitle = "${activePlayers.size} ready to play",
                            )
                        }
                        
                        itemsIndexed(activePlayers, key = { _, p -> p.id }) { index, player ->
                            SpringAnimatedItem(index = index) {
                                PlayerCard(
                                    player = player,
                                    onEdit = {
                                        editingPlayer = player
                                        showAddDialog = true
                                    },
                                    onDelete = { showDeleteConfirm = player },
                                    onToggleAFK = {
                                        scope.launch {
                                            repo.db.players().update(
                                                player.copy(afk = if (player.afk == 0) 1 else 0).toEntity()
                                            )
                                            vm.reloadPlayers()
                                        }
                                    },
                                )
                            }
                        }
                    }

                    // AFK players section
                    val afkPlayers = players.filter { it.afk > 0 }
                    if (afkPlayers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                            SectionHeader(
                                title = "Away (AFK)",
                                subtitle = "${afkPlayers.size} not participating",
                            )
                        }

                        itemsIndexed(afkPlayers, key = { _, p -> p.id }) { index, player ->
                            SpringAnimatedItem(index = index) {
                                PlayerCard(
                                    player = player,
                                    onEdit = {
                                        editingPlayer = player
                                        showAddDialog = true
                                    },
                                    onDelete = { showDeleteConfirm = player },
                                    onToggleAFK = {
                                        scope.launch {
                                            repo.db.players().update(
                                                player.copy(afk = if (player.afk == 0) 1 else 0).toEntity()
                                            )
                                            vm.reloadPlayers()
                                        }
                                    },
                                    isAFK = true,
                                )
                            }
                        }
                    }

                    // Start Game CTA when enough players
                    if (activePlayers.size >= 2) {
                        item {
                            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))
                            GlowButton(
                                text = "START GAME",
                                onClick = { vm.goBack() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(HelldeckHeights.Button.dp + 12.dp),
                                accentColor = HelldeckColors.colorSecondary,
                                icon = "\uD83C\uDFAE",
                            )
                        }
                    }

                    // Bottom spacing for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
    
    // Add/Edit player dialog
    if (showAddDialog) {
        AddPlayerDialog(
            existingPlayers = players,
            editingPlayer = editingPlayer,
            onDismiss = {
                showAddDialog = false
                editingPlayer = null
            },
            onPlayerCreated = { name, emoji ->
                scope.launch {
                    if (editingPlayer != null) {
                        // Edit existing
                        repo.db.players().update(
                            editingPlayer!!.copy(name = name, avatar = emoji).toEntity()
                        )
                        snackbarHostState.showSnackbar("Updated ${name}")
                    } else {
                        // Create new
                        val id = ValidationUtils.generateUniquePlayerId(players)
                        repo.db.players().upsert(
                            com.helldeck.data.PlayerEntity(
                                id = id,
                                name = name,
                                avatar = emoji,
                                sessionPoints = 0,
                            )
                        )
                        snackbarHostState.showSnackbar("Added ${name}")
                    }
                    vm.reloadPlayers()
                    editingPlayer = null
                }
            },
        )
    }
    
    // Delete confirmation
    showDeleteConfirm?.let { player ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = { Text("🗑️", fontSize = 48.sp) },
            title = { Text("Delete Player?") },
            text = {
                Text("Remove ${player.avatar}? This cannot be undone.")
            },
            confirmButton = {
                GlowButton(
                    text = "Delete",
                    onClick = {
                        scope.launch {
                            repo.db.players().delete(player.toEntity())
                            vm.reloadPlayers()
                            snackbarHostState.showSnackbar("Deleted ${player.avatar}")
                            showDeleteConfirm = null
                        }
                    },
                    accentColor = HelldeckColors.Red,
                    icon = "\uD83D\uDDD1\uFE0F",
                )
            },
            dismissButton = {
                OutlineButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirm = null },
                )
            },
        )
    }
    
    // Bulk actions menu
    if (showBulkMenu) {
        AlertDialog(
            onDismissRequest = { showBulkMenu = false },
            title = { Text("Bulk Actions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineButton(
                        text = "Mark All AFK",
                        onClick = {
                            scope.launch {
                                players.forEach { player ->
                                    repo.db.players().update(player.copy(afk = 1).toEntity())
                                }
                                vm.reloadPlayers()
                                snackbarHostState.showSnackbar("All players marked AFK")
                                showBulkMenu = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = "💤",
                    )
                    
                    OutlineButton(
                        text = "Mark All Active",
                        onClick = {
                            scope.launch {
                                players.forEach { player ->
                                    repo.db.players().update(player.copy(afk = 0).toEntity())
                                }
                                vm.reloadPlayers()
                                snackbarHostState.showSnackbar("All players marked active")
                                showBulkMenu = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = "✅",
                    )
                    
                    OutlineButton(
                        text = "Delete All Players",
                        onClick = {
                            scope.launch {
                                players.forEach { player ->
                                    repo.db.players().delete(player.toEntity())
                                }
                                vm.reloadPlayers()
                                snackbarHostState.showSnackbar("All players deleted")
                                showBulkMenu = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = "🗑️",
                        accentColor = HelldeckColors.Red,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBulkMenu = false }) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun SpringAnimatedItem(
    index: Int,
    content: @Composable () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "item_spring_$index",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = 200,
            delayMillis = (index * 40).coerceAtMost(200),
        ),
        label = "item_alpha_$index",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha),
    ) {
        content()
    }
}

@Composable
private fun PlayerCard(
    player: com.helldeck.content.model.Player,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAFK: () -> Unit,
    modifier: Modifier = Modifier,
    isAFK: Boolean = false,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        accentColor = if (isAFK) HelldeckColors.colorMuted else HelldeckColors.colorPrimary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = player.avatar,
                    fontSize = 40.sp,
                )
                Column {
                    Text(
                        text = "Seat", // Anonymized - no player names
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isAFK) HelldeckColors.colorMuted else HelldeckColors.colorOnDark,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "⭐ ${player.sessionPoints} pts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HelldeckColors.colorMuted,
                        )
                        if (player.gamesPlayed > 0) {
                            Text(
                                text = "🎮 ${player.gamesPlayed} games",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorMuted,
                            )
                        }
                    }
                }
            }
            
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onToggleAFK) {
                    Text(
                        text = if (isAFK) "✅" else "💤",
                        fontSize = 20.sp,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = HelldeckColors.colorPrimary,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = HelldeckColors.Red,
                    )
                }
            }
        }
    }
}
