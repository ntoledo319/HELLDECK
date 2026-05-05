package com.helldeck.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun GroupDnaScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {},
) {
    val profiles = listOf(
        "Chaos Crew" to "Unhinged energy, zero boundaries",
        "Wholesome Gang" to "Good vibes, clean laughs",
        "Roast Masters" to "Savage humor, thick skin required",
        "Intellectual Squad" to "Witty banter, big brain plays",
    )

    val profileIcons = mapOf(
        "Chaos Crew" to "\uD83D\uDD25",
        "Wholesome Gang" to "\uD83E\uDD17",
        "Roast Masters" to "\uD83D\uDCA5",
        "Intellectual Squad" to "\uD83E\uDDE0",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Group DNA",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Define your crew's vibe",
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SectionHeader(
                title = "Choose Your Profile",
                subtitle = "Current: ${vm.groupDnaProfile ?: "Not set"}",
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            Column(verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp)) {
                profiles.forEach { (profile, description) ->
                    val isSelected = vm.groupDnaProfile == profile
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
                        label = "profile_scale",
                    )

                    NeonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .semantics {
                                contentDescription = "$profile profile: $description. ${if (isSelected) "Selected" else "Not selected"}"
                            },
                        accentColor = if (isSelected) HelldeckColors.colorSecondary else HelldeckColors.colorPrimary,
                        onClick = { vm.groupDnaProfile = profile },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = profileIcons[profile] ?: "\uD83C\uDFAD",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) HelldeckColors.colorSecondary else HelldeckColors.colorOnDark,
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HelldeckColors.colorMuted,
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "\u2705",
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                            }
                        }
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
