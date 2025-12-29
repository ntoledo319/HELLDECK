package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import com.helldeck.content.model.Player
import com.helldeck.ui.*

/**
 * Avatar voting flow
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun AvatarVoteFlow(
    players: List<Player>,
    onVote: (voterId: String, targetId: String) -> Unit,
    onDone: () -> Unit,
    onManagePlayers: (() -> Unit)? = null
) {
    var idx by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }

    if (players.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No active players. Enable players in Settings.")
            Spacer(modifier = Modifier.height(8.dp))
            onManagePlayers?.let {
                OutlinedButton(onClick = it) { Text("Open Settings") }
            }
        }
        return
    }

    val voter = players[idx]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voter: ${voter.name}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Player grid
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 3
        ) {
            players.forEach { player ->
                VoteButton(
                    playerName = player.name,
                    playerAvatar = player.avatar,
                    isSelected = chosen == player.id,
                    onClick = { chosen = player.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    if (idx < players.lastIndex) {
                        idx++
                    } else {
                        onDone()
                    }
                    chosen = null
                }
            ) {
                Text("Skip")
            }

            Button(
                enabled = chosen != null,
                onClick = {
                    chosen?.let { targetId ->
                        onVote(voter.id, targetId)
                    }

                    if (idx < players.lastIndex) {
                        idx++
                    } else {
                        onDone()
                    }
                    chosen = null
                }
            ) {
                Text(if (idx < players.lastIndex) "Lock & Next" else "Finish Voting")
            }
        }
    }
}

/** Single pick of a target avatar (no iteration) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleAvatarPickFlow(
    players: List<Player>,
    onPick: (targetId: String) -> Unit,
    onManagePlayers: (() -> Unit)? = null,
    title: String = "Pick a target"
) {
    var chosen by remember { mutableStateOf<String?>(null) }

    if (players.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No active players. Enable players in Settings.")
            Spacer(modifier = Modifier.height(8.dp))
            onManagePlayers?.let {
                OutlinedButton(onClick = it) { Text("Open Settings") }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 3
        ) {
            players.forEach { player ->
                VoteButton(
                    playerName = player.name,
                    playerAvatar = player.avatar,
                    isSelected = chosen == player.id,
                    onClick = { chosen = player.id }
                )
            }
        }
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        Button(
            enabled = chosen != null,
            onClick = { chosen?.let { onPick(it) } },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
        ) { Text("Lock") }
    }
}

/** Simple options picker used by several interactions */
@Composable
fun OptionsPickFlow(
    title: String,
    options: List<String>,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        options.forEach { opt ->
            Button(
                onClick = { onPick(opt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Orange)
            ) { Text(opt) }
        }
    }
}

/** Taboo clue view with forbidden list */
@Composable
fun TabooFlow(
    clue: String,
    taboos: List<String>,
    running: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Clue: $clue", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            taboos.take(3).forEach { word ->
                AssistChip(word)
            }
        }
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        if (!running) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Yellow)
            ) { Text("Start Timer") }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HelldeckColors.Green)
            ) { Text("Lock") }
        }
    }
}

@Composable
private fun AssistChip(text: String) {
    Surface(
        color = HelldeckColors.MediumGray,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(HelldeckRadius.Medium)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = HelldeckColors.Yellow,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * A/B voting flow
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ABVoteFlow(
    players: List<Player>,
    preChoiceLabel: String,
    preChoices: List<String>,
    preChoice: String?,
    onPreChoice: (String) -> Unit,
    leftLabel: String,
    rightLabel: String,
    onVote: (voterId: String, choice: String) -> Unit,
    onDone: () -> Unit,
    onManagePlayers: (() -> Unit)? = null
) {
    var idx by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var lockPre by remember { mutableStateOf(preChoice != null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pre-choice selection
        if (!lockPre) {
            Text(
                text = preChoiceLabel,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    onPreChoice(preChoices[0])
                    lockPre = true
                }) {
                    Text(preChoices[0])
                }

                Button(onClick = {
                    onPreChoice(preChoices.getOrElse(1) { "B" })
                    lockPre = true
                }) {
                    Text(preChoices.getOrElse(1) { "B" })
                }
            }

            return@Column
        }

        // Voting interface
        if (players.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active players. Enable players in Settings.")
                Spacer(modifier = Modifier.height(8.dp))
                onManagePlayers?.let {
                    OutlinedButton(onClick = it) { Text("Open Settings") }
                }
            }
            return@Column
        }

        val voter = players[idx]

        Text(
            text = "Voter: ${voter.name}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // A/B choice buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { chosen = leftLabel },
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (chosen == leftLabel) HelldeckColors.VoteSelected else HelldeckColors.MediumGray
                    )
            ) {
                Text(leftLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

                Button(
                    onClick = { chosen = rightLabel },
                    modifier = Modifier.width(120.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (chosen == rightLabel) HelldeckColors.VoteSelected else HelldeckColors.MediumGray
                    )
            ) {
                Text(rightLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Vote button
        Button(
            enabled = chosen != null,
            onClick = {
                chosen?.let { choice ->
                    onVote(voter.id, choice)
                }

                if (idx < players.lastIndex) {
                    idx++
                } else {
                    onDone()
                }
                chosen = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (idx < players.lastIndex) "Lock & Next" else "Finish Voting")
        }
    }
}

/**
 * Judge pick flow
 */
@Composable
fun JudgePickFlow(
    judge: Player?,
    options: List<String>,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Judge: ${judge?.name ?: "—"}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Option buttons
        options.forEach { option ->
            Button(
                onClick = { onPick(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Orange
                )
            ) {
                Text(option)
            }
        }
    }
}
