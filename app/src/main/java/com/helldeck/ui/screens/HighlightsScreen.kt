package com.helldeck.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.EmptyState
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.components.SectionHeader
import com.helldeck.ui.vm.GameNightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Session Highlights",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (vm.highlights.isNotEmpty()) {
                            Text(
                                "${vm.highlights.size} moments captured",
                                style = MaterialTheme.typography.labelMedium,
                                color = HelldeckColors.colorMuted,
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("Back") }
                },
            )
        },
    ) { padding ->
        if (vm.highlights.isEmpty()) {
            EmptyState(
                icon = "\u2728",
                title = "No Highlights Yet",
                message = "Keep playing to capture memorable moments!\nHighlights are created automatically during games.",
                actionLabel = "Close",
                onActionClick = onClose,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(HelldeckSpacing.Large.dp),
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                ) {
                    item {
                        SectionHeader(
                            title = "Best Moments",
                            subtitle = "From your latest session",
                        )
                        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                    }

                    itemsIndexed(vm.highlights) { index, highlight ->
                        val reducedMotion = LocalReducedMotion.current
                        var appeared by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) { appeared = true }

                        val scale by animateFloatAsState(
                            targetValue = if (appeared) 1f else 0.8f,
                            animationSpec = if (reducedMotion) {
                                spring(stiffness = Spring.StiffnessHigh)
                            } else {
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                )
                            },
                            label = "highlight_scale",
                        )

                        val medalEmoji = when (index) {
                            0 -> "\uD83E\uDD47"
                            1 -> "\uD83E\uDD48"
                            2 -> "\uD83E\uDD49"
                            else -> "\u2B50"
                        }

                        val accentColor = when (index) {
                            0 -> HelldeckColors.Lol
                            1 -> HelldeckColors.colorAccentCool
                            2 -> HelldeckColors.colorAccentWarm
                            else -> HelldeckColors.colorPrimary
                        }

                        NeonCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .semantics {
                                    contentDescription = "Highlight number ${index + 1}: $highlight"
                                },
                            accentColor = accentColor,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = medalEmoji,
                                    style = MaterialTheme.typography.headlineLarge,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Highlight #${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                    )
                                    Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = HelldeckColors.colorOnDark,
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HelldeckSpacing.Large.dp),
                ) {
                    GlowButton(
                        text = "Close",
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
