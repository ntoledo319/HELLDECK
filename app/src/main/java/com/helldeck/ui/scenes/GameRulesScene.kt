package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                actions = { TextButton(onClick = { vm.goHome() }) { Text("Home") } },
            )
        },
    ) { padding ->
        if (game == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No game selected")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(HelldeckSpacing.Medium.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                        Text(text = game.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = game.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Timer: ${com.helldeck.engine.Config.getTimerForInteraction(game.interaction)} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Players: ${game.minPlayers}–${game.maxPlayers}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("How to Play", style = MaterialTheme.typography.titleMedium)
                        Text(text = gameHowTo(game), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green),
                ) { Text("Back to Game") }
            }
        }
    }
}

private fun gameHowTo(g: com.helldeck.engine.GameSpec): String = gameHowToDetailed(g)

private fun gameHowToDetailed(g: com.helldeck.engine.GameSpec): String {
    return when (g.id) {
        com.helldeck.engine.GameIds.ROAST_CONS -> "Read the roast prompt. Everyone taps the player that fits best. Majority target wins; active may score."
        com.helldeck.engine.GameIds.CONFESS_CAP -> "Speaker pre-picks TRUTH or BLUFF. Room votes T/F; points if majority matches the pre-pick."
        com.helldeck.engine.GameIds.POISON_PITCH -> "Would you rather A or B? Active pre-picks, then pitches. Room votes; bonus if majority matches."
        com.helldeck.engine.GameIds.FILLIN -> "Judge reads prompt aloud and fills in the first blank verbally. Others have 60s to write punchlines. Judge reads all answers aloud and picks the winner. +1 point to winner; judge rotates left."
        com.helldeck.engine.GameIds.RED_FLAG -> "Perk vs red flag. Room votes SMASH or PASS. Majority SMASH rewards."
        com.helldeck.engine.GameIds.HOTSEAT_IMP -> "Answer as the target player; judge picks the most on-brand response."
        com.helldeck.engine.GameIds.TEXT_TRAP -> "See an inbound text. Pick a reply vibe (Deadpan, Feral, etc.). Lock; feedback after."
        com.helldeck.engine.GameIds.TABOO -> "Start timer. Give clues without forbidden words. Lock when finished."
        com.helldeck.engine.GameIds.TITLE_FIGHT -> "Run a quick duel, then choose who won to keep or steal the crown."
        com.helldeck.engine.GameIds.ALIBI -> "Smuggle all secret words into an alibi without detection."
        com.helldeck.engine.GameIds.SCATTER -> "Given a category and letter, say three valid items quickly. No repeats."
        com.helldeck.engine.GameIds.UNIFYING_THEORY -> "Explain why three unrelated items are the same. Spice 4+ requires inappropriate connections."
        com.helldeck.engine.GameIds.REALITY_CHECK -> "Subject rates themselves 1-10 secretly; group rates subject 1-10; reveal both. Self-aware (gap 0-1) = +2; delusional/fisher = roast/drink."
        com.helldeck.engine.GameIds.OVER_UNDER -> "Group sets betting line; everyone bets OVER or UNDER on subject's number; reveal truth. Winners +1; losers drink."
        else -> when (g.interaction) {
            com.helldeck.engine.Interaction.VOTE_AVATAR -> "Everyone votes the most fitting player. Majority wins."
            com.helldeck.engine.Interaction.AB_VOTE -> "Room votes A or B; active may pre-pick."
            com.helldeck.engine.Interaction.TRUE_FALSE -> "Speaker sets TRUTH/BLUFF; room votes T/F."
            com.helldeck.engine.Interaction.JUDGE_PICK -> "Judge selects the best option."
            com.helldeck.engine.Interaction.SMASH_PASS -> "Room votes SMASH or PASS."
            com.helldeck.engine.Interaction.TARGET_PICK -> "Pick a target player and continue."
            com.helldeck.engine.Interaction.REPLY_TONE -> "Choose a reply vibe."
            com.helldeck.engine.Interaction.TABOO_CLUE -> "Give clues without forbidden words."
            com.helldeck.engine.Interaction.ODD_REASON -> "Pick the misfit and explain."
            com.helldeck.engine.Interaction.DUEL -> "Run a quick duel; choose who won."
            com.helldeck.engine.Interaction.SMUGGLE -> "Smuggle secret words into a story."
            com.helldeck.engine.Interaction.PITCH -> "Pitch your idea and lock."
            com.helldeck.engine.Interaction.SPEED_LIST -> "List items quickly until time."
        }
    }
}
