package com.helldeck.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.engine.*
import com.helldeck.ui.*

/**
 * Rules sheet with enhanced design and "How HellDeck Works" section
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun RulesSheet(onClose: () -> Unit) {
    var expandedHowItWorks by remember { mutableStateOf(true) }
    var expandedCardGen by remember { mutableStateOf(false) }
    var expandedLearning by remember { mutableStateOf(false) }
    var expandedSpice by remember { mutableStateOf(false) }
    var expandedFeedback by remember { mutableStateOf(false) }
    var expandedGameRules by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    Scaffold(
        topBar = {
            val vmLocal: HelldeckVm = viewModel()
            TopAppBar(
                title = { Text("HELLDECK — Rules & How-To") },
                navigationIcon = { TextButton(onClick = { vmLocal.goBack() }) { Text("Back") } },
                actions = { TextButton(onClick = { vmLocal.goHome() }) { Text("Home") } }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(HelldeckSpacing.Medium.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // "How HellDeck Works" Section
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = HelldeckColors.DarkGray
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedHowItWorks = !expandedHowItWorks }
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        HelldeckColors.Yellow.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(HelldeckSpacing.Medium.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 How HellDeck Works",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            HelldeckColors.Yellow,
                                            HelldeckColors.Orange
                                        )
                                    )
                                )
                            )
                            Text(
                                text = if (expandedHowItWorks) "▼" else "▶",
                                style = MaterialTheme.typography.headlineSmall,
                                color = HelldeckColors.Yellow
                            )
                        }
                        
                        if (!expandedHowItWorks) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to learn about card generation, smart learning, spice levels, and scoring",
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                        }
                    }
                    
                    if (expandedHowItWorks) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = HelldeckSpacing.Medium.dp)
                                .padding(bottom = HelldeckSpacing.Medium.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Card Generation
                            HowItWorksCard(
                                icon = "🎴",
                                title = "Card Generation",
                                isExpanded = expandedCardGen,
                                onToggle = { expandedCardGen = !expandedCardGen },
                                summary = "Dynamic cards filled with random content",
                                content = {
                                    Text(
                                        text = "Templates have slots like {friend} or {place} that get filled with random words from lexicons. Each game gets fresh, unpredictable cards every round!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = HelldeckColors.MediumGray,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    ) {
                                       Column(modifier = Modifier.padding(12.dp)) {
                                           Text(
                                               text = "Template:",
                                               style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                               color = HelldeckColors.Yellow
                                           )
                                           Spacer(modifier = Modifier.height(2.dp))
                                           Text(
                                               text = "\"Roast {friend} for {red_flag}\"",
                                               style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                               color = Color.White
                                           )
                                           Spacer(modifier = Modifier.height(12.dp))
                                           Text(
                                               text = "Becomes:",
                                               style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                               color = HelldeckColors.Green
                                           )
                                           Spacer(modifier = Modifier.height(2.dp))
                                           Text(
                                               text = "\"Roast Jay for always being late\"",
                                               style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                               color = Color.White
                                           )
                                       }
                                    }
                                }
                            )
                            
                            // Smart Learning
                            HowItWorksCard(
                                icon = "🧠",
                                title = "Smart Learning",
                                isExpanded = expandedLearning,
                                onToggle = { expandedLearning = !expandedLearning },
                                summary = "AI learns which cards your group loves",
                                content = {
                                    Text(
                                        text = "HellDeck uses AI to track which cards make your group laugh. High-scoring cards appear more often, trash cards appear less. The system balances between playing proven favorites (exploitation) and trying new content (exploration) to keep sessions fresh.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("😂 Loved", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = HelldeckColors.Lol)
                                            Text("Score: 8.5", style = MaterialTheme.typography.bodyMedium, color = HelldeckColors.Green)
                                            Text("Plays ↑", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("😐 Meh", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                            Text("Score: 2.1", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                            Text("Plays →", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🚮 Trash", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = HelldeckColors.Trash)
                                            Text("Score: -1.2", style = MaterialTheme.typography.bodyMedium, color = HelldeckColors.Red)
                                            Text("Plays ↓", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                        }
                                    }
                                }
                            )
                            
                            // Spice System
                            HowItWorksCard(
                                icon = "🌶️",
                                title = "Spice System",
                                isExpanded = expandedSpice,
                                onToggle = { expandedSpice = !expandedSpice },
                                summary = "Control content intensity with spice levels",
                                content = {
                                    Text(
                                        text = "Every card has a spice rating (1-3 🌶️). The heat threshold slider controls which cards appear. Set to 50% for family-friendly content, 70%+ for savage mode. In spicy mode (≥70%), the room needs 70% consensus for heat bonuses instead of 60%.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SpiceLevelRow("🌶️", "Mild", "Safe for all audiences", HelldeckColors.Green)
                                        SpiceLevelRow("🌶️🌶️", "Medium", "Adult humor, some edge", HelldeckColors.Orange)
                                        SpiceLevelRow("🌶️🌶️🌶️", "Hot", "Savage roasts, NSFW", HelldeckColors.Red)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "💡 Heat threshold ${(Config.roomHeatThreshold() * 100).toInt()}% determines spicy mode activation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HelldeckColors.Yellow
                                    )
                                }
                            )
                            
                            // Feedback & Scoring
                            HowItWorksCard(
                                icon = "📊",
                                title = "Feedback & Scoring",
                                isExpanded = expandedFeedback,
                                onToggle = { expandedFeedback = !expandedFeedback },
                                summary = "How points and room heat work",
                                content = {
                                    Text(
                                        text = "After each round, everyone votes with feedback buttons. This both scores the current card AND teaches the AI what your group finds funny.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ScoringRow("Win", "+2 pts", HelldeckColors.Green)
                                        ScoringRow("Room Heat (≥60% 😂)", "+1 pt", HelldeckColors.Yellow)
                                        ScoringRow("Quick Laugh (<1.2s)", "+1 pt", HelldeckColors.Lol)
                                        ScoringRow("Streak Bonus", "+1 to +3 pts", HelldeckColors.Orange)
                                        ScoringRow("Room Trash (≥60% 🚮)", "-2 pts", HelldeckColors.Red)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "💡 Feedback helps the AI learn what your group finds funny!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HelldeckColors.Yellow
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            // Global Rules Section
            item {
                Text(
                    text = "⚖️ Global Rules",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = HelldeckColors.Yellow
                )
            }
            
            item {
                RuleCard(
                    title = "Voting & Ties",
                    content = "Binary votes (A/B, True/False): 8 second timer. Avatar votes: 10 seconds. Judge picks: 6 seconds (can lock early once threshold reached). If there's a tie, do a 3-second revote. Still tied? Use Torch RPS (Rock-Paper-Scissors). In judge-based games, the judge breaks ties."
                )
            }
            
            item {
                RuleCard(
                    title = "Turn Order & Fairness",
                    content = "First round: random starter. Then turns go clockwise. Comeback mechanic: when you have 3+ players, whoever's in last place gets to pick the next game (helps them catch up). New players joining late enter rotation at the start of the next round."
                )
            }
            
            // Game-Specific Rules Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎮 Game Rules",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = HelldeckColors.Yellow
                )
            }

            // Per-game rules with expandable design
            items(com.helldeck.engine.Games.size) { idx ->
                val g = com.helldeck.engine.Games[idx]
                val isExpanded = expandedGameRules.contains(g.id)
                
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedGameRules = if (isExpanded) {
                                expandedGameRules - g.id
                            } else {
                                expandedGameRules + g.id
                            }
                        },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isExpanded)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            HelldeckColors.MediumGray
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (isExpanded) 4.dp else 2.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = gameIconFor(g.id),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = g.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (!isExpanded) {
                                        Text(
                                            text = "${g.minPlayers}-${g.maxPlayers} players • ${Config.getTimerForInteraction(g.interaction)/1000}s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = HelldeckColors.LightGray
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isExpanded) "▼" else "▶",
                                style = MaterialTheme.typography.titleMedium,
                                color = HelldeckColors.Yellow
                            )
                        }
                        
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = g.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = HelldeckColors.DarkGray,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "⏱️ ${Config.getTimerForInteraction(g.interaction)/1000}s",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HelldeckColors.Yellow
                                    )
                                }
                                Surface(
                                    color = HelldeckColors.DarkGray,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "👥 ${g.minPlayers}-${g.maxPlayers} players",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HelldeckColors.Green
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "How to Play",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = HelldeckColors.Orange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = gameHowTo(g),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp)) }
        }
    }
}

@Composable
private fun HowItWorksCard(
    icon: String,
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    summary: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.elevatedCardColors(
            containerColor = HelldeckColors.MediumGray
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isExpanded) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HelldeckColors.Yellow
                        )
                        if (!isExpanded) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = HelldeckColors.LightGray
                            )
                        }
                    }
                }
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    style = MaterialTheme.typography.titleMedium,
                    color = HelldeckColors.Yellow
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun SpiceLevelRow(pepper: String, level: String, desc: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = pepper, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = level, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
        Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
private fun ScoringRow(label: String, points: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Text(
            text = points,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun RuleCard(title: String, content: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = HelldeckColors.MediumGray),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = HelldeckColors.Yellow
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                lineHeight = 22.sp
            )
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

private fun gameIconFor(id: String): String = when (id) {
    GameIds.ROAST_CONS -> "🔥"
    GameIds.CONFESS_CAP -> "🕵️"
    GameIds.POISON_PITCH -> "⚖️"
    GameIds.FILLIN -> "✍️"
    GameIds.RED_FLAG -> "🚩"
    GameIds.HOTSEAT_IMP -> "🎭"
    GameIds.TEXT_TRAP -> "💬"
    GameIds.TABOO -> "⛔️"
    GameIds.ODD_ONE -> "🧩"
    GameIds.TITLE_FIGHT -> "👑"
    GameIds.ALIBI -> "🕶️"
    GameIds.HYPE_YIKE -> "📣"
    GameIds.SCATTER -> "🔤"
    GameIds.MAJORITY -> "📊"
    else -> "🎮"
}