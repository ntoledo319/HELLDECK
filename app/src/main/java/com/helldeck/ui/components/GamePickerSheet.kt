package com.helldeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.engine.GameMetadata

/**
 * Modal sheet for direct game selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePickerSheet(
    onGameSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose Your Game",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Game grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(GameMetadata.getAllGameIds()) { gameId ->
                    GameCard(
                        gameId = gameId,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGameSelected(gameId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    gameId: String,
    onClick: () -> Unit
) {
    val metadata = GameMetadata.getGameMetadata(gameId)
    val gameName = metadata?.name ?: gameId
    val gameEmoji = getGameEmoji(gameId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = gameEmoji,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = gameName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 2
                )
            }
        }
    }
}

private fun getGameEmoji(gameId: String): String = when (gameId) {
    "roast_consensus" -> "🔥"
    "poison_pitch" -> "☠️"
    "fill_in_finisher" -> "✍️"
    "red_flag_rally" -> "🚩"
    "hotseat_imposter" -> "🎭"
    "text_thread_trap" -> "💬"
    "taboo_timer" -> "🤐"
    "odd_one_out" -> "🔍"
    "title_fight" -> "⚔️"
    "alibi_drop" -> "🕵️"
    "hype_or_yike" -> "📢"
    "scatterblast" -> "⚡"
    "majority_report" -> "📊"
    "confess_or_cap" -> "🤥"
    else -> "🎲"
}
