package com.helldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.content.model.Player
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.theme.HelldeckSpacing

/**
 * Team mode UI components for HELLDECK.
 * 
 * Features:
 * - Team warnings at 8+ players
 * - Automatic team formation suggestions
 * - Team display with voting efficiency info
 * - Team picker for manual assignments
 * 
 * @ai_prompt Team mode components surface hidden PlayerManager logic
 */

/**
 * Warning banner suggesting team mode at 8+ players.
 */
@Composable
fun TeamModeWarning(
    playerCount: Int,
    @Suppress("UNUSED_PARAMETER") onEnableTeamMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playerCount >= 8) {
        WarningBanner(
            message = when {
                playerCount >= 17 -> "⚠️ Team mode required for $playerCount players (1 vote per team)"
                playerCount >= 11 -> "💡 Team mode recommended: $playerCount players = slow voting"
                else -> "💡 Tip: Enable team mode for faster voting with $playerCount players"
            },
            modifier = modifier,
        )
    }
}

/**
 * Team mode suggestion card with recommendations.
 */
@Composable
fun TeamModeSuggestion(
    playerCount: Int,
    onEnableTeamMode: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val teamCount = when {
        playerCount <= 16 -> 2
        else -> (playerCount + 3) / 4 // ~4 players per team
    }
    
    val votesPerRound = when {
        playerCount <= 10 -> playerCount // Individual
        playerCount <= 16 -> 2 // Team mode
        else -> teamCount
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("👥", fontSize = 48.sp) },
        title = { Text("Enable Team Mode?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "With $playerCount players, team mode speeds up voting:",
                    style = MaterialTheme.typography.bodyLarge,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                NeonCard(accentColor = HelldeckColors.colorSecondary) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Teams:", fontWeight = FontWeight.Bold)
                            Text("$teamCount teams")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Votes per round:", fontWeight = FontWeight.Bold)
                            Text("$votesPerRound (instead of $playerCount)")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Voting time:", fontWeight = FontWeight.Bold)
                            Text("~${votesPerRound * 3}s (instead of ~${playerCount * 3}s)")
                        }
                    }
                }
                
                Text(
                    "Teams are randomly assigned and rotate each game.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.colorMuted,
                )
            }
        },
        confirmButton = {
            GlowButton(
                text = "Enable Team Mode",
                onClick = {
                    onEnableTeamMode()
                    onDismiss()
                },
                icon = "✅",
            )
        },
        dismissButton = {
            OutlineButton(
                text = "Keep Individual",
                onClick = onDismiss,
            )
        },
        modifier = modifier,
    )
}

/**
 * Team display card showing team assignments.
 */
@Composable
fun TeamDisplay(
    teams: Map<String, List<Player>>,
    modifier: Modifier = Modifier,
) {
    if (teams.isEmpty()) return
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
    ) {
        SectionHeader(
            title = "Teams",
            subtitle = "${teams.size} teams • 1 vote per team",
        )
        
        teams.entries.forEachIndexed { index, (_, members) ->
            val teamName = "Team ${index + 1}"
            val teamColor = when (index % 4) {
                0 -> HelldeckColors.colorPrimary
                1 -> HelldeckColors.colorSecondary
                2 -> HelldeckColors.colorAccentWarm
                else -> HelldeckColors.colorAccentCool
            }
            
            NeonCard(
                accentColor = teamColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = teamName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = teamColor,
                        )
                        Text(
                            text = "${members.size} players",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                    
                    // Team members
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        members.forEach { player ->
                            Surface(
                                shape = RoundedCornerShape(HelldeckRadius.Small),
                                color = HelldeckColors.surfaceElevated,
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(player.avatar, fontSize = 20.sp)
                                    Text(
                                        player.name,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        InfoBanner(
            message = "💡 Teams vote together. Discuss and agree before the timer runs out!",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Team picker for manual team assignments.
 */
@Composable
fun TeamPickerDialog(
    players: List<Player>,
    currentTeams: Map<String, List<String>>,
    onTeamsAssigned: (Map<String, List<String>>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var teamAssignments by remember {
        mutableStateOf(
            if (currentTeams.isEmpty()) {
                // Auto-assign to 2 teams
                val half = players.size / 2
                mapOf(
                    "team1" to players.take(half).map { it.id },
                    "team2" to players.drop(half).map { it.id },
                )
            } else {
                currentTeams
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Teams") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                teamAssignments.entries.forEachIndexed { teamIndex, (teamId, memberIds) ->
                    item {
                        Text(
                            "Team ${teamIndex + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    
                    items(players.filter { it.id in memberIds }) { player ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(player.avatar)
                                Text(player.name)
                            }
                            
                            OutlineButton(
                                text = "Move",
                                onClick = {
                                    // Move to other team
                                    val otherTeamId = teamAssignments.keys.first { it != teamId }
                                    teamAssignments = teamAssignments.mapValues { (id, members) ->
                                        when (id) {
                                            teamId -> members - player.id
                                            otherTeamId -> members + player.id
                                            else -> members
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            GlowButton(
                text = "Save Teams",
                onClick = {
                    onTeamsAssigned(teamAssignments)
                    onDismiss()
                },
            )
        },
        dismissButton = {
            OutlineButton(
                text = "Cancel",
                onClick = onDismiss,
            )
        },
        modifier = modifier,
    )
}

/**
 * Inline team mode toggle with explanation.
 */
@Composable
fun TeamModeToggle(
    playerCount: Int,
    isTeamMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        accentColor = if (isTeamMode) HelldeckColors.colorSecondary else HelldeckColors.colorMuted,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Team Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isTeamMode) {
                        "Players vote as teams (faster)"
                    } else {
                        "Each player votes individually"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.colorMuted,
                )
                if (playerCount >= 8 && !isTeamMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ Recommended for $playerCount players",
                        style = MaterialTheme.typography.bodySmall,
                        color = HelldeckColors.colorAccentWarm,
                    )
                }
            }
            
            Switch(
                checked = isTeamMode,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HelldeckColors.colorSecondary,
                    checkedTrackColor = HelldeckColors.colorSecondary.copy(alpha = 0.5f),
                ),
            )
        }
    }
}
