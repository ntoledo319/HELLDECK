package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.engine.*
import com.helldeck.ui.*

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun GameRulesScene(vm: HelldeckVm, onClose: () -> Unit) {
    val gid = vm.selectedGameId
    val game = if (gid != null) com.helldeck.engine.GameRegistry.getGameById(gid) else null
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Game Rules") },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = { TextButton(onClick = { vm.goHome() }) { Text("Home") } }
            )
        }
    ) { padding ->
        if (game == null) {
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No game selected")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(HelldeckSpacing.Medium.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                        Text(text = game.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = game.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Timer: ${com.helldeck.engine.Config.getTimerForInteraction(game.interaction)} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Players: ${game.minPlayers}–${game.maxPlayers}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("How to Play", style = MaterialTheme.typography.titleMedium)
                        Text(text = gameHowTo(game), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
                ) { Text("Back to Game") }
            }
        }
    }
}

private fun gameHowTo(g: com.helldeck.engine.GameSpec): String {
    return when (g.interaction) {
        com.helldeck.engine.Interaction.VOTE_AVATAR -> "Everyone votes the most fitting player. Majority wins; active gets points if applicable."
        com.helldeck.engine.Interaction.AB_VOTE -> "Room votes A or B. Active may pre-pick to earn bonus if they read the room correctly."
        com.helldeck.engine.Interaction.TRUE_FALSE -> "Speaker sets TRUTH/BLUFF. Room votes T/F. Points if majority matches the pre-pick."
        com.helldeck.engine.Interaction.JUDGE_PICK -> "Judge selects the best option. Lock to score."
        com.helldeck.engine.Interaction.SMASH_PASS -> "Room votes SMASH or PASS. Majority SMASH rewards the active player."
        com.helldeck.engine.Interaction.TARGET_PICK -> "Pick one target player. Lock to continue; feedback still counts."
        com.helldeck.engine.Interaction.REPLY_TONE -> "Choose reply vibe (Deadpan, Feral, etc.). Lock to continue; feedback after."
        com.helldeck.engine.Interaction.TABOO_CLUE -> "Start timer, give clues without forbidden words. Lock when finished."
        com.helldeck.engine.Interaction.ODD_REASON -> "Pick the misfit among three options and explain why."
        com.helldeck.engine.Interaction.DUEL -> "Run the mini-duel; choose who won."
        com.helldeck.engine.Interaction.SMUGGLE -> "Weave secret words into an alibi without detection; Lock when finished."
        com.helldeck.engine.Interaction.PITCH -> "Pitch your idea. Lock when finished."
        com.helldeck.engine.Interaction.SPEED_LIST -> "List items quickly until the timer ends. Lock when finished."
    }
}