package com.helldeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.vm.GameNightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseRulesScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "House Rules",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Customize the chaos",
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
                .padding(padding)
                .padding(HelldeckSpacing.Large.dp),
        ) {
            InfoBanner(
                message = "House rules override default game settings. Your crew, your way!",
                icon = "\uD83C\uDFE0",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

            // Spicy Mode Card
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Spicy mode toggle. Currently ${if (vm.spicy) "enabled" else "disabled"}"
                    },
                accentColor = if (vm.spicy) HelldeckColors.Error else HelldeckColors.colorPrimary,
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
                            text = if (vm.spicy) "\uD83C\uDF36\uFE0F" else "\uD83E\uDDCA",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Column {
                            Text(
                                text = "Spicy Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HelldeckColors.colorOnDark,
                            )
                            Text(
                                text = if (vm.spicy) "Adult content enabled" else "Safe for all audiences",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HelldeckColors.colorMuted,
                            )
                        }
                    }
                    Switch(
                        checked = vm.spicy,
                        onCheckedChange = { vm.spicy = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HelldeckColors.Error,
                            checkedTrackColor = HelldeckColors.Error.copy(alpha = 0.5f),
                            uncheckedThumbColor = HelldeckColors.colorMuted,
                            uncheckedTrackColor = HelldeckColors.surfaceElevated,
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Toggle spicy mode"
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            // Heat Threshold Card
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Heat threshold slider. Currently at ${vm.heatThreshold.toInt()} percent"
                    },
                accentColor = HelldeckColors.colorAccentWarm,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "\uD83C\uDF21\uFE0F",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Column {
                                Text(
                                    text = "Heat Threshold",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HelldeckColors.colorOnDark,
                                )
                                Text(
                                    text = "Controls content intensity",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HelldeckColors.colorMuted,
                                )
                            }
                        }
                        Text(
                            text = "${vm.heatThreshold.toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                vm.heatThreshold < 33f -> HelldeckColors.colorSecondary
                                vm.heatThreshold < 66f -> HelldeckColors.colorAccentWarm
                                else -> HelldeckColors.Error
                            },
                        )
                    }

                    Slider(
                        value = vm.heatThreshold,
                        onValueChange = { vm.heatThreshold = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = HelldeckColors.colorAccentWarm,
                            activeTrackColor = HelldeckColors.colorAccentWarm,
                            inactiveTrackColor = HelldeckColors.colorMuted.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Adjust heat threshold"
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "\uD83E\uDDCA Mild",
                            style = MaterialTheme.typography.labelSmall,
                            color = HelldeckColors.colorMuted,
                        )
                        Text(
                            text = "\uD83D\uDD25 Max Heat",
                            style = MaterialTheme.typography.labelSmall,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            GlowButton(
                text = "Done",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                accentColor = HelldeckColors.colorSecondary,
            )
        }
    }
}
