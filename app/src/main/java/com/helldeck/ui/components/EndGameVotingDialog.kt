package com.helldeck.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.vm.GameNightViewModel
import kotlinx.coroutines.launch

/**
 * End game dialog with MVP/Dud voting.
 * Allows players to select their favorite card and flag any duds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndGameVotingDialog(
    vm: GameNightViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var sessionCards by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMvp by remember { mutableStateOf<String?>(null) }
    var selectedDuds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        sessionCards = vm.getSessionCardsForVoting()
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Game Over! 🎉",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (sessionCards.isEmpty()) {
                    Text(
                        text = "No cards played this session",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    // MVP Selection
                    Text(
                        text = "🏆 Best Card?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.colorPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    sessionCards.take(10).forEach { cardId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMvp = cardId }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedMvp == cardId,
                                onClick = { selectedMvp = cardId },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cardId.take(40) + if (cardId.length > 40) "..." else "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Dud Selection (optional)
                    Text(
                        text = "💩 Any Duds? (optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.colorMuted,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    sessionCards.take(10).forEach { cardId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDuds = if (selectedDuds.contains(cardId)) {
                                        selectedDuds - cardId
                                    } else {
                                        selectedDuds + cardId
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selectedDuds.contains(cardId),
                                onCheckedChange = { checked ->
                                    selectedDuds = if (checked) {
                                        selectedDuds + cardId
                                    } else {
                                        selectedDuds - cardId
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cardId.take(40) + if (cardId.length > 40) "..." else "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        selectedMvp?.let { vm.markCardAsMvp(it) }
                        selectedDuds.forEach { vm.markCardAsDud(it) }
                        vm.finishGameAndGoHome()
                    }
                },
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.finishGameAndGoHome() }) {
                Text("Skip Voting")
            }
        },
    )
}
