package com.helldeck.ui.scenes

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.content.model.GameOptions
import com.helldeck.ui.*
import com.helldeck.ui.components.ModernCardDisplay
import com.helldeck.ui.components.UndoSnackbarHost
import com.helldeck.ui.components.rememberUndoState
import kotlinx.coroutines.launch

/**
 * Modernized round scene with new card display and undo functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSceneModern(vm: HelldeckVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = vm.roundState.collectAsState()
    val undoState = rememberUndoState()

    // Auto-dismiss undo
    undoState.AutoDismiss()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.value.currentGameTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Skip button
                    IconButton(
                        onClick = {
                            scope.launch {
                                vm.skipCard()
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Skip Card")
                    }

                    // End round
                    IconButton(
                        onClick = {
                            scope.launch {
                                vm.endRound()
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "End Round")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Undo snackbar
                UndoSnackbarHost(
                    undoState = undoState.currentState,
                    onUndo = { undoState.undo() }
                )

                // Game-specific interactions
                state.value.currentOptions?.let { options ->
                    Surface(
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            GameInteractionSection(
                                options = options,
                                onInteraction = { interaction ->
                                    // Handle interaction
                                    scope.launch {
                                        // vm.handleInteraction(interaction)
                                    }
                                }
                            )
                        }
                    }
                }

                // Feedback buttons
                FeedbackButtonsModern(
                    onFeedback = { rating ->
                        val cardText = state.value.currentCard?.text ?: "this card"
                        scope.launch {
                            vm.submitFeedback(rating)

                            // Show undo option
                            undoState.show("Rated \"$cardText\"") {
                                scope.launch {
                                    // vm.undoLastFeedback()
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Card display
            state.value.currentCard?.let { card ->
                val metadata = GameMetadataProvider.getMetadata(card.game)

                ModernCardDisplay(
                    text = card.text,
                    gameTitle = metadata?.name ?: card.game,
                    spiceLevel = card.spice,
                    isGenerating = state.value.isGenerating,
                    generatedByLLM = card.metadata["generated_by"] == "llm_v2" || card.family == "llm_generated_v2",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Loading state when no card
            if (state.value.currentCard == null && state.value.isGenerating) {
                ModernCardDisplay(
                    text = "",
                    gameTitle = "Loading...",
                    spiceLevel = 3,
                    isGenerating = true,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedbackButtonsModern(
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeedbackButton(
                emoji = "😂",
                label = "LOL",
                onClick = { onFeedback("lol") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            )

            FeedbackButton(
                emoji = "😐",
                label = "MEH",
                onClick = { onFeedback("meh") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            )

            FeedbackButton(
                emoji = "🗑️",
                label = "TRASH",
                onClick = { onFeedback("trash") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
private fun FeedbackButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = colors,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GameInteractionSection(
    options: GameOptions,
    onInteraction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        when (options) {
            is GameOptions.AB -> {
                Text(
                    text = "Choose one:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onInteraction("a") },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text(options.a, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onInteraction("b") },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text(options.b, fontWeight = FontWeight.Bold)
                    }
                }
            }

            is GameOptions.PlayerVote -> {
                Text(
                    text = "Vote for a player:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Player grid would go here
            }

            is GameOptions.Taboo -> {
                Text(
                    text = "Word: ${options.word}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Forbidden: ${options.forbidden.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> {
                // Other game types
            }
        }
    }
}

// Placeholder for game metadata
private object GameMetadataProvider {
    data class Metadata(val name: String)

    fun getMetadata(gameId: String): Metadata? {
        return com.helldeck.engine.GameMetadata.getGameMetadata(gameId)?.let {
            Metadata(it.name)
        }
    }
}
