package com.helldeck.ui.scenes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.PlayerEntity
import com.helldeck.data.toEntity
import com.helldeck.ui.EmojiPicker
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.Scene
import com.helldeck.ui.hdFieldColors
import com.helldeck.ui.theme.HelldeckColors
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RollcallScene(vm: HelldeckVm) {
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var present by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(vm.players) {
        present = vm.players.filter { it.afk == 0 }.map { it.id }.toSet()
    }

    var name by remember { mutableStateOf("") }
    val emojis = listOf("😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺")
    var emoji by remember { mutableStateOf(emojis.random()) }
    var showPicker by remember { mutableStateOf(false) }
    var showAddPlayer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Who's Here?")
                        Text(
                            "${present.size} of ${vm.players.size} present",
                            style = MaterialTheme.typography.bodySmall,
                            color = HelldeckColors.Yellow
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = { vm.goHome() }) { Text("Skip") } },
                actions = {
                    TextButton(onClick = { present = vm.players.map { it.id }.toSet() }) {
                        Text("All", color = HelldeckColors.Green)
                    }
                    TextButton(onClick = { present = emptySet() }) {
                        Text("None", color = HelldeckColors.Red)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!showAddPlayer) {
                ExtendedFloatingActionButton(
                    onClick = { showAddPlayer = true },
                    containerColor = HelldeckColors.Green,
                    contentColor = Color.Black
                ) {
                    Text("➕ Add Player")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showAddPlayer) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = HelldeckColors.DarkGray
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Add New Player",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = HelldeckColors.Yellow
                            )
                            TextButton(onClick = { showAddPlayer = false; name = "" }) {
                                Text("Cancel", color = HelldeckColors.LightGray)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPicker = true },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                            }
                            if (showPicker) {
                                EmojiPicker(show = showPicker, onDismiss = { showPicker = false }) { picked ->
                                    emoji = picked
                                }
                            }
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Player name") },
                                placeholder = { Text("e.g., Jay, Pip, Mo") },
                                modifier = Modifier.weight(1f),
                                colors = hdFieldColors(),
                                singleLine = true
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val id = "p${Random.nextInt(100000)}"
                                    scope.launch {
                                        repo.db.players().upsert(
                                            PlayerEntity(id = id, name = name.trim(), avatar = emoji, sessionPoints = 0, afk = 0)
                                        )
                                        present = present + id
                                        vm.reloadPlayers()
                                        name = ""
                                        emoji = emojis.random()
                                        showAddPlayer = false
                                        snackbarHostState.showSnackbar("Added ${name.trim()}")
                                    }
                                }
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
                        ) { Text("Add to Roster") }
                    }
                }
            }

            Surface(
                color = HelldeckColors.MediumGray.copy(alpha = 0.5f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Tap player cards to toggle attendance • Swipe left to delete",
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.LightGray,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.players, key = { it.id }) { p ->
                    val isPresent = present.contains(p.id)
                    val dismissState = rememberDismissState(confirmStateChange = { value ->
                        if (value == DismissValue.DismissedToStart) {
                            true
                        } else false
                    })

                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (dismissState.currentValue == DismissValue.DismissedToStart && !showDeleteConfirm) {
                        showDeleteConfirm = true
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete ${p.name}?") },
                            text = { Text("This will remove them from your roster. You can Undo immediately after.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteConfirm = false
                                        scope.launch {
                                            repo.db.players().delete(p.toEntity())
                                            present = present - p.id
                                            vm.reloadPlayers()
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Deleted ${p.name}",
                                                actionLabel = "Undo",
                                                withDismissAction = true,
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                repo.db.players().upsert(p.toEntity())
                                                vm.reloadPlayers()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = HelldeckColors.Red
                                    )
                                ) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                            }
                        )
                    }

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            val fraction = dismissState.progress.fraction
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HelldeckColors.Red.copy(alpha = fraction * 0.8f))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (fraction > 0.1f) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        dismissContent = {
                            RollcallPlayerCard(
                                player = p.toEntity(),
                                isPresent = isPresent,
                                onToggle = {
                                    present = if (isPresent) present - p.id else present + p.id
                                }
                            )
                        }
                    )
                }
            }

            Button(
                enabled = present.size >= 2,
                onClick = {
                    scope.launch {
                        vm.players.forEach { p ->
                            val newAfk = if (present.contains(p.id)) 0 else 1
                            if (p.afk != newAfk) {
                                repo.db.players().update(p.toEntity().copy(afk = newAfk))
                            }
                        }
                        vm.reloadPlayers()
                        vm.markRollcallDone()
                        vm.goHome()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (present.size >= 2) HelldeckColors.Yellow else HelldeckColors.MediumGray,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (present.size >= 2) 8.dp else 2.dp
                )
            ) {
                Text(
                    if (present.size >= 2) "🎮 Start Session (${present.size} present)"
                    else "⚠️ Need at least 2 players",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun RollcallPlayerCard(
    player: PlayerEntity,
    isPresent: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isPresent)
                HelldeckColors.Green.copy(alpha = 0.15f)
            else
                HelldeckColors.MediumGray
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isPresent) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isPresent) {
                        Brush.linearGradient(
                            colors = listOf(
                                HelldeckColors.Green.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
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
                    Surface(
                        color = if (isPresent) HelldeckColors.Green else HelldeckColors.LightGray,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(12.dp)
                    ) {}
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = player.avatar,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isPresent) HelldeckColors.White else HelldeckColors.LightGray
                        )
                        Text(
                            text = if (isPresent) "✓ Present" else "Absent",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPresent) HelldeckColors.Green else HelldeckColors.LightGray
                        )
                    }
                }
                
                Surface(
                    color = if (isPresent) HelldeckColors.Green else Color.Transparent,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(40.dp),
                    border = if (!isPresent) BorderStroke(2.dp, HelldeckColors.LightGray) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPresent) {
                            Text(
                                "✓",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
