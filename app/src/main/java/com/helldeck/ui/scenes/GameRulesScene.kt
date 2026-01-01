package com.helldeck.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.engine.*
import com.helldeck.ui.*
import com.helldeck.ui.theme.HelldeckSpacing

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun GameRulesScene(vm: HelldeckVm, onClose: () -> Unit) {
    val gid = vm.selectedGameId
    val game = if (gid != null) com.helldeck.engine.GameRegistry.getGameById(gid) else null
    val detailedRules = if (gid != null) DetailedGameRules.getRulesForGame(gid) else null
    
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                ) {
                    Column(modifier = Modifier.padding(HelldeckSpacing.Large.dp)) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = game.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InfoChip(
                                label = "⏱️ ${com.helldeck.engine.Config.getTimerForInteraction(game.interaction) / 1000}s"
                            )
                            InfoChip(
                                label = "👥 ${game.minPlayers}–${game.maxPlayers}"
                            )
                        }
                    }
                }

                if (detailedRules != null) {
                    RulesSection(
                        title = "📖 How to Play",
                        icon = Icons.AutoMirrored.Rounded.Rule,
                        items = detailedRules.howToPlay,
                        color = HelldeckColors.Yellow
                    )
                    
                    RulesSection(
                        title = "⚙️ The Mechanics",
                        icon = Icons.Rounded.Psychology,
                        items = detailedRules.mechanics,
                        color = HelldeckColors.Blue
                    )
                    
                    RulesSection(
                        title = "🏆 Scoring",
                        icon = Icons.Rounded.EmojiEvents,
                        items = detailedRules.scoring,
                        color = HelldeckColors.Green
                    )
                    
                    RulesSection(
                        title = "🎭 The Vibe",
                        icon = Icons.Rounded.Star,
                        items = detailedRules.theVibe,
                        color = HelldeckColors.Orange
                    )
                    
                    RulesSection(
                        title = "💡 Pro Tips",
                        icon = Icons.Rounded.Lightbulb,
                        items = detailedRules.tips,
                        color = HelldeckColors.Purple
                    )
                } else {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                    ) {
                        Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                            Text(
                                text = "Quick Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = gameHowToDetailed(game),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green),
                ) { Text("Back to Game") }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RulesSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>,
    color: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(HelldeckSpacing.Large.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                RuleItem(text = item)
            }
        }
    }
}

@Composable
private fun RuleItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f
        )
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

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
