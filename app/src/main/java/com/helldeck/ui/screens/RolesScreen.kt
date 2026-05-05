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
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.EmptyState
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.components.SectionHeader
import com.helldeck.ui.vm.GameNightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolesScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {},
) {
    val roles = listOf(
        "Dealer" to Pair("\uD83C\uDCCF", "Controls the card deck"),
        "Judge" to Pair("\u2696\uFE0F", "Makes the final call"),
        "Wildcard" to Pair("\uD83C\uDFB2", "Chaos agent, unpredictable"),
        "Heckler" to Pair("\uD83D\uDCE2", "Professional roaster"),
        "Scorekeeper" to Pair("\uD83D\uDCCA", "Tracks the points"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Player Roles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Assign roles to players",
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
            if (vm.players.isEmpty()) {
                EmptyState(
                    icon = "\uD83C\uDFAD",
                    title = "No Players Yet",
                    message = "Add players before assigning roles.\nRoles add fun dynamics to your game!",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(HelldeckSpacing.Large.dp),
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                ) {
                    item {
                        SectionHeader(
                            title = "Assign Roles",
                            subtitle = "${vm.players.size} players",
                        )
                        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
                    }

                    items(vm.players) { player ->
                        val currentRole = vm.playerRoles[player.id] ?: "None"
                        val currentRoleInfo = roles.find { it.first == currentRole }
                        val reducedMotion = LocalReducedMotion.current

                        var appeared by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { appeared = true }

                        val scale by animateFloatAsState(
                            targetValue = if (appeared) 1f else 0.9f,
                            animationSpec = if (reducedMotion) {
                                spring(stiffness = Spring.StiffnessHigh)
                            } else {
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                )
                            },
                            label = "role_card_scale",
                        )

                        NeonCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .semantics {
                                    contentDescription = "${player.name}, current role: $currentRole"
                                },
                            accentColor = if (currentRole != "None") HelldeckColors.colorSecondary else HelldeckColors.colorPrimary,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = player.avatar,
                                        style = MaterialTheme.typography.headlineLarge,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = HelldeckColors.colorOnDark,
                                        )
                                        Text(
                                            text = if (currentRole != "None") {
                                                "${currentRoleInfo?.second?.first ?: ""} $currentRole"
                                            } else {
                                                "No role assigned"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (currentRole != "None") HelldeckColors.colorSecondary else HelldeckColors.colorMuted,
                                        )
                                    }
                                }

                                // Role selection chips
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded },
                                ) {
                                    OutlinedTextField(
                                        value = if (currentRole != "None") {
                                            "${currentRoleInfo?.second?.first ?: ""} $currentRole"
                                        } else {
                                            "Select a role..."
                                        },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = {
                                            Text(
                                                "Role",
                                                color = HelldeckColors.colorMuted,
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = HelldeckColors.colorPrimary,
                                            unfocusedBorderColor = HelldeckColors.colorMuted.copy(alpha = 0.5f),
                                            focusedTextColor = HelldeckColors.colorOnDark,
                                            unfocusedTextColor = HelldeckColors.colorOnDark,
                                        ),
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                    ) {
                                        roles.forEach { (role, iconAndDesc) ->
                                            val (icon, desc) = iconAndDesc
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(icon, style = MaterialTheme.typography.titleMedium)
                                                        Column {
                                                            Text(
                                                                role,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold,
                                                            )
                                                            Text(
                                                                desc,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = HelldeckColors.colorMuted,
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    vm.playerRoles = vm.playerRoles + (player.id to role)
                                                    expanded = false
                                                },
                                                modifier = Modifier.semantics {
                                                    contentDescription = "Assign $role to ${player.name}: $desc"
                                                },
                                            )
                                        }
                                    }
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
                        text = "Done",
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = HelldeckColors.colorSecondary,
                    )
                }
            }
        }
    }
}
