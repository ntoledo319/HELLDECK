package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.vm.GameNightViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCardCreatorScene(vm: GameNightViewModel) {
    val scope = rememberCoroutineScope()

    var cardText by remember { mutableStateOf("") }
    var selectedGameId by remember { mutableStateOf("") }
    var showGamePicker by remember { mutableStateOf(false) }
    var customCards by remember { mutableStateOf<List<com.helldeck.data.CustomCardEntity>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load existing custom cards
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                customCards = vm.getAllCustomCards()
            } catch (e: Exception) {
                errorMessage = "Failed to load custom cards"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Cards") },
                navigationIcon = {
                    IconButton(onClick = { vm.goBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions
            Text(
                text = "Create personalized cards for your game nights! Use {PLAYER} for player names.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Game type selector
            OutlinedButton(
                onClick = { showGamePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedGameId.isEmpty()) {
                        "Select Game Type"
                    } else {
                        GameMetadata.getGameMetadata(selectedGameId)?.title ?: "Select Game Type"
                    }
                )
            }

            // Card text input
            OutlinedTextField(
                value = cardText,
                onValueChange = { cardText = it },
                label = { Text("Card Text") },
                placeholder = { Text("Enter your custom card...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4,
                supportingText = {
                    Text(
                        text = "${cardText.length}/300",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            // Add card button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            if (cardText.isBlank()) {
                                errorMessage = "Please enter card text"
                                return@launch
                            }
                            if (selectedGameId.isEmpty()) {
                                errorMessage = "Please select a game type"
                                return@launch
                            }

                            vm.saveCustomCard(selectedGameId, cardText)
                            cardText = ""
                            errorMessage = null

                            // Reload custom cards
                            customCards = vm.getAllCustomCards()
                        } catch (e: Exception) {
                            errorMessage = "Failed to save card: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = cardText.isNotBlank() && selectedGameId.isNotEmpty()
            ) {
                Text("ADD TO DECK")
            }

            // Error message
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Existing cards list
            Text(
                text = "Your Custom Cards (${customCards.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (customCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No custom cards yet.\nCreate your first one above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customCards) { card ->
                        CustomCardItem(
                            card = card,
                            onDelete = {
                                scope.launch {
                                    try {
                                        vm.deleteCustomCard(card.id)
                                        customCards = vm.getAllCustomCards()
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to delete card"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Game picker dialog
    if (showGamePicker) {
        AlertDialog(
            onDismissRequest = { showGamePicker = false },
            title = { Text("Select Game Type") },
            text = {
                LazyColumn {
                    items(GameMetadata.getAllGameIds()) { gameId ->
                        val game = GameMetadata.getGameMetadata(gameId)
                        TextButton(
                            onClick = {
                                selectedGameId = gameId
                                showGamePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = game?.title ?: gameId,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGamePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomCardItem(
    card: com.helldeck.data.CustomCardEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = card.cardText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${GameMetadata.getGameMetadata(card.gameId)?.title ?: card.gameId} • Used ${card.timesUsed} times",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete card",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
