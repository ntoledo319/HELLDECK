package com.helldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rules sheet component
 */
@Composable
fun RulesSheet(
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📖 HELLDECK Rules",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    ),
                    color = HelldeckColors.Yellow,
                )

                TextButton(onClick = onClose) {
                    Text(
                        "✕",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Game Overview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = HelldeckColors.DarkGray,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = "🎮 Game Overview",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "HELLDECK is a party game where players take turns drawing cards " +
                            "and completing hilarious challenges. The goal is to make everyone laugh " +
                            "while navigating through different game modes and interactions.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HelldeckColors.White,
                        lineHeight = 1.4.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Core Rules
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = HelldeckColors.MediumGray,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = "📋 Core Rules",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val rules = listOf(
                        "• Take turns being the active player",
                        "• Read cards aloud and follow instructions",
                        "• Complete challenges honestly and creatively",
                        "• Vote or judge as required by the game",
                        "• Award points fairly",
                        "• Most importantly: have fun and be respectful",
                    )

                    rules.forEach { rule ->
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HelldeckColors.White,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Modes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = HelldeckColors.DarkGray,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = "🎲 Game Modes",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val gameModes = listOf(
                        "• **Roast Consensus** - Vote for the best roast target",
                        "• **Majority Report** - Guess what the majority thinks",
                        "• **Poison Pitch** - Creative pitch with a twist",
                        "• **Confession** - Truth or bluff voting game",
                        "• **Smash or Pass** - Rate the spicy content",
                        "• **Target Pick** - Select players for challenges",
                    )

                    gameModes.forEach { mode ->
                        Text(
                            text = mode,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HelldeckColors.White,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scoring
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = HelldeckColors.MediumGray,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = "🏆 Scoring System",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Win rounds to earn points\n" +
                            "• Get bonus points for creative answers\n" +
                            "• Streaks multiply your score\n" +
                            "• Last place gets to pick next game",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HelldeckColors.White,
                        lineHeight = 1.4.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tips
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = HelldeckColors.DarkGray,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = "💡 Pro Tips",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = HelldeckColors.Yellow,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Read the room and adapt your style\n" +
                            "• Don't be afraid to be bold (when appropriate)\n" +
                            "• Team up with unlikely allies\n" +
                            "• Remember: chaos creates the best memories\n" +
                            "• When in doubt, pick the funniest option",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HelldeckColors.White,
                        lineHeight = 1.4.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "🔥 Bring the heat or go home! 🔥",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.Orange,
                    ),
                )
            }
        }
    }
}
