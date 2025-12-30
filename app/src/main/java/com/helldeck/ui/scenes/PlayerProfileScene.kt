package com.helldeck.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.data.computePlayerProfiles
import com.helldeck.ui.*
import com.helldeck.ui.HelldeckVm

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)
@Composable
fun PlayerProfileScene(vm: HelldeckVm, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val playerId = vm.selectedPlayerId
    var profile by remember { mutableStateOf<com.helldeck.data.PlayerProfile?>(null) }
    var summaryText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(playerId) {
        if (playerId != null) {
            val profiles = repo.computePlayerProfiles()
            profile = profiles.firstOrNull { it.id == playerId }
            profile?.let { pr ->
                summaryText = buildString {
                    append("${pr.avatar} ${pr.name} — HELLDECK Profile\n")
                    append("Total Points: ${pr.totalPoints}\n")
                    append("Wins: ${pr.wins} • Games: ${pr.gamesPlayed}\n")
                    append("Heat Rounds: ${pr.heatRounds} • Quick Laughs: ${pr.quickLaughs}\n")
                    append("Avg 😂: ${"%.2f".format(pr.avgLol)} • Avg 🚮: ${"%.2f".format(pr.avgTrash)}\n")
                    if (pr.awards.isNotEmpty()) {
                        append("Awards: ${pr.awards.joinToString(", ")}\n")
                    }
                    append("#HELLDECK")
                }
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${profile?.avatar ?: "👤"} Profile") },
                actions = {
                    profile?.let {
                        TextButton(onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, summaryText)
                            }
                            ctx.startActivity(android.content.Intent.createChooser(intent, "Share Profile"))
                        }) { Text("📤 Share") }
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                },
            )
        },
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                HelldeckLoadingSpinner()
            }
        } else {
            profile?.let { pr ->
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(HelldeckSpacing.Medium.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Hero card with gradient
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = HelldeckColors.DarkGray),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                HelldeckColors.Yellow.copy(alpha = 0.25f),
                                                HelldeckColors.Orange.copy(alpha = 0.1f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    )
                                    .padding(HelldeckSpacing.Large.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        pr.avatar,
                                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        pr.name,
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                        color = HelldeckColors.White,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        ProfileStatBadge("Total", pr.totalPoints.toString(), HelldeckColors.Yellow)
                                        ProfileStatBadge("Wins", pr.wins.toString(), HelldeckColors.Green)
                                        ProfileStatBadge("Games", pr.gamesPlayed.toString(), HelldeckColors.Orange)
                                    }
                                }
                            }
                        }
                    }

                    // Performance Stats
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = HelldeckColors.MediumGray),
                        ) {
                            Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                                Text(
                                    "📈 Performance",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = HelldeckColors.Yellow,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                ProfileStatWithBar("Heat Rounds", pr.heatRounds, pr.gamesPlayed, HelldeckColors.Red)
                                ProfileStatWithBar("Quick Laughs", pr.quickLaughs, pr.gamesPlayed, HelldeckColors.Lol)
                                EnhancedStatRow("Avg 😂", String.format("%.2f", pr.avgLol), "😂")
                                EnhancedStatRow("Avg 🚮", String.format("%.2f", pr.avgTrash), "🚮")
                            }
                        }
                    }

                    // Awards
                    if (pr.awards.isNotEmpty()) {
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = HelldeckColors.MediumGray),
                            ) {
                                Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                                    Text(
                                        "🏅 Awards",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = HelldeckColors.Yellow,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    androidx.compose.foundation.layout.FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        pr.awards.forEach { award ->
                                            Surface(
                                                color = HelldeckColors.Yellow.copy(alpha = 0.2f),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, HelldeckColors.Yellow),
                                            ) {
                                                Text(
                                                    text = award,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = HelldeckColors.Yellow,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shareable summary
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = HelldeckColors.DarkGray),
                        ) {
                            Column(modifier = Modifier.padding(HelldeckSpacing.Medium.dp)) {
                                Text(
                                    "📋 Shareable Summary",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = HelldeckColors.Yellow,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    summaryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HelldeckColors.LightGray,
                                )
                            }
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        HelldeckLoadingSpinner()
                    } else {
                        Text("No profile selected", color = HelldeckColors.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = color,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = HelldeckColors.LightGray,
        )
    }
}

@Composable
private fun ProfileStatWithBar(label: String, value: Int, total: Int, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = HelldeckColors.White)
            Text("$value / $total", style = MaterialTheme.typography.bodyMedium, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) value.toFloat() / total.toFloat() else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EnhancedStatRow(label: String, value: String, icon: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = HelldeckColors.White,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = HelldeckColors.Yellow,
        )
    }
}
