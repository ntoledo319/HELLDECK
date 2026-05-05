package com.helldeck.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.components.SectionHeader
import com.helldeck.ui.vm.GameNightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacksScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {},
) {
    val availablePacks = listOf(
        "Core" to Pair("\uD83C\uDFB4", HelldeckColors.colorPrimary),
        "Party" to Pair("\uD83C\uDF89", HelldeckColors.colorSecondary),
        "Dark Humor" to Pair("\uD83D\uDDA4", HelldeckColors.colorAccentCool),
        "Wholesome" to Pair("\uD83D\uDC96", HelldeckColors.Lol),
        "Office" to Pair("\uD83D\uDCBC", HelldeckColors.colorAccentWarm),
        "NSFW" to Pair("\uD83D\uDD1E", HelldeckColors.Error),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Card Packs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${vm.selectedPacks.size} of ${availablePacks.size} active",
                            style = MaterialTheme.typography.labelMedium,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            InfoBanner(
                message = "Select card packs to customize your game. Each pack brings unique vibes!",
                icon = "\uD83D\uDCE6",
                modifier = Modifier.padding(HelldeckSpacing.Large.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(HelldeckSpacing.Large.dp),
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                item {
                    SectionHeader(
                        title = "Available Packs",
                        subtitle = "Tap to toggle",
                    )
                    Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                }

                items(availablePacks) { (pack, iconAndColor) ->
                    val (icon, accentColor) = iconAndColor
                    val isSelected = pack in vm.selectedPacks
                    val reducedMotion = LocalReducedMotion.current

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1f,
                        animationSpec = if (reducedMotion) {
                            spring(stiffness = Spring.StiffnessHigh)
                        } else {
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            )
                        },
                        label = "pack_scale",
                    )

                    NeonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .semantics {
                                contentDescription = "$pack pack. ${if (isSelected) "Selected" else "Not selected"}. Tap to toggle."
                            },
                        accentColor = if (isSelected) accentColor else HelldeckColors.colorMuted,
                        onClick = {
                            vm.selectedPacks = if (pack in vm.selectedPacks) {
                                vm.selectedPacks - pack
                            } else {
                                vm.selectedPacks + pack
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = icon,
                                    style = MaterialTheme.typography.headlineLarge,
                                )
                                Column {
                                    Text(
                                        text = pack,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) accentColor else HelldeckColors.colorOnDark,
                                    )
                                    Text(
                                        text = if (isSelected) "Active" else "Tap to enable",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HelldeckColors.colorMuted,
                                    )
                                }
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = HelldeckColors.colorMuted,
                                    checkmarkColor = HelldeckColors.background,
                                ),
                            )
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
                    text = "Done (${vm.selectedPacks.size} packs)",
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = HelldeckColors.colorSecondary,
                )
            }
        }
    }
}
