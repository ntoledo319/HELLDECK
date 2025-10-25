package com.helldeck.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.PlayerEntity
import com.helldeck.data.toEntity
import com.helldeck.ui.EmojiPicker
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.hdFieldColors
import com.helldeck.ui.theme.HelldeckColors
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun PlayersScene(vm: HelldeckVm) {
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var name by remember { mutableStateOf("") }
    val emojis = listOf("😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺")
    var emoji by remember { mutableStateOf(emojis.random()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Players") },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text("Name or emoji + name (e.g., 🦊 Pip)")
                    },
                    modifier = Modifier.weight(1f),
                    colors = hdFieldColors()
                )

                Spacer(modifier = Modifier.width(HelldeckSpacing.Small.dp))

                var showPicker by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showPicker = true }) { Text(text = emoji) }
                if (showPicker) {
                    EmojiPicker(show = showPicker, onDismiss = { showPicker = false }) { picked ->
                        emoji = picked
                    }
                }
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            val id = "p${Random.nextInt(100000)}"
                            repo.db.players().upsert(PlayerEntity(
                                id = id,
                                name = name,
                                avatar = emoji,
                                sessionPoints = 0
                            ))
                            vm.reloadPlayers()
                            name = ""
                            emoji = emojis.random()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Green
                )
            ) {
                Text(text = "Add Player")
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            LazyColumn {
                items(vm.players, key = { it.id }) { player ->
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
                            title = { Text("Delete player?") },
                            text = { Text("This will remove ${player.name}. You can still Undo right after.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    scope.launch {
                                        repo.db.players().delete(player.toEntity())
                                        vm.reloadPlayers()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Deleted ${player.name}",
                                            actionLabel = "Undo",
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            repo.db.players().upsert(player.toEntity())
                                            vm.reloadPlayers()
                                        }
                                    }
                                }) { Text("Delete") }
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .padding(horizontal = 12.dp)
                                    .background(Color(0xFFB00020).copy(alpha = fraction)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (fraction > 0f) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Delete", color = Color.White)
                                }
                            }
                        },
                        dismissContent = {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { vm.openProfile(player.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(HelldeckSpacing.Medium.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        var showEditPicker by remember { mutableStateOf(false) }
                                        Text(
                                            text = player.avatar,
                                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp),
                                            modifier = Modifier.clickable { showEditPicker = true }
                                        )
                                        if (showEditPicker) {
                                            EmojiPicker(
                                                show = true,
                                                onDismiss = { showEditPicker = false },
                                                onPick = { picked ->
                                                    scope.launch {
                                                        repo.db.players().update(player.toEntity().copy(avatar = picked))
                                                        vm.reloadPlayers()
                                                        snackbarHostState.showSnackbar("Updated avatar for ${player.name}")
                                                    }
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(HelldeckSpacing.Small.dp))
                                        var editName by remember { mutableStateOf(false) }
                                        var tempName by remember { mutableStateOf(player.name) }
                                        if (editName) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = tempName,
                                                    onValueChange = { tempName = it },
                                                    singleLine = true,
                                                    modifier = Modifier.widthIn(min = 140.dp, max = 240.dp),
                                                    colors = hdFieldColors()
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                TextButton(onClick = {
                                                    val newName = tempName.trim()
                                                    if (newName.isNotEmpty() && newName != player.name) {
                                                        scope.launch {
                                                            repo.db.players().update(player.toEntity().copy(name = newName))
                                                            vm.reloadPlayers()
                                                            snackbarHostState.showSnackbar("Renamed to $newName")
                                                        }
                                                    }
                                                    editName = false
                                                }) { Text("Save") }
                                                TextButton(onClick = { editName = false; tempName = player.name }) { Text("Cancel") }
                                            }
                                        } else {
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .clickable { editName = true }
                                                .widthIn(max = 220.dp)
                                        )
                                    }
                                    }

                                    Row {
                                        TextButton(onClick = { vm.openProfile(player.id) }) { Text("Profile") }
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    repo.db.players().update(
                                                        player.toEntity().copy(afk = if (player.afk == 0) 1 else 0)
                                                    )
                                                    vm.reloadPlayers()
                                                }
                                            }
                                        ) {
                                            Text(if (player.afk == 0) "AFK" else "Back")
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tip: 3–10 players best. 11–16 = teams (1 vote per team).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
