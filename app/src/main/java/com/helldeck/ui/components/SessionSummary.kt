package com.helldeck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.analytics.MetricsTracker
import com.helldeck.data.SessionAnalytics
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Session summary component displaying end-of-session analytics
 *
 * Shows:
 * - Session duration and rounds played
 * - Average laugh score and reactions
 * - Top games played
 * - Heat moments (high-energy rounds)
 * - Player participation
 *
 * Supports export as text for sharing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryDialog(
    sessionId: String,
    metricsTracker: MetricsTracker,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var analytics by remember { mutableStateOf<SessionAnalytics?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sessionId) {
        analytics = metricsTracker.computeSessionAnalytics(sessionId)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (analytics != null) {
                SessionSummaryContent(analytics = analytics!!)
            } else {
                Text("No session data available")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        analytics?.let { summary ->
                            onExport(generateSummaryText(summary))
                        }
                    },
                    enabled = analytics != null
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}

@Composable
private fun SessionSummaryContent(analytics: SessionAnalytics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Duration and rounds
        SummaryCard(
            title = "Session Stats",
            items = listOf(
                "Duration" to formatDuration(analytics.duration),
                "Rounds played" to analytics.totalRounds.toString(),
                "Players" to analytics.participantCount.toString()
            )
        )

        // Reactions and laughs
        SummaryCard(
            title = "Reactions",
            items = listOf(
                "Total reactions" to analytics.totalReactions.toString(),
                "Laugh score" to "${(analytics.averageLaughScore * 100).toInt()}%",
                "Heat moments" to "${analytics.heatMoments} rounds 🔥"
            )
        )

        // Top game
        analytics.topGame?.let { (gameId, count) ->
            val gameName = GameMetadata.getGameMetadata(gameId)?.name ?: gameId
            SummaryCard(
                title = "Most Played",
                items = listOf(
                    gameName to "$count rounds"
                )
            )
        }

        // Round timings
        if (analytics.longestRound > 0 && analytics.shortestRound > 0) {
            SummaryCard(
                title = "Round Timings",
                items = listOf(
                    "Fastest round" to "${analytics.shortestRound / 1000}s",
                    "Longest round" to "${analytics.longestRound / 1000}s"
                )
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(HelldeckRadius.Medium)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Generates plain text summary for sharing
 */
private fun generateSummaryText(analytics: SessionAnalytics): String {
    val gameName = analytics.topGame?.let { (gameId, _) ->
        GameMetadata.getGameMetadata(gameId)?.name ?: gameId
    } ?: "Various games"

    return buildString {
        appendLine("🎮 HELLDECK Session Summary")
        appendLine()
        appendLine("Duration: ${formatDuration(analytics.duration)}")
        appendLine("Rounds: ${analytics.totalRounds}")
        appendLine("Players: ${analytics.participantCount}")
        appendLine()
        appendLine("📊 Reactions")
        appendLine("Total: ${analytics.totalReactions}")
        appendLine("Laugh score: ${(analytics.averageLaughScore * 100).toInt()}%")
        if (analytics.heatMoments > 0) {
            appendLine("Heat moments: ${analytics.heatMoments} 🔥")
        }
        appendLine()
        analytics.topGame?.let { (_, count) ->
            appendLine("🎯 Most played: $gameName ($count rounds)")
        }
        appendLine()
        appendLine("Made with HELLDECK")
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
