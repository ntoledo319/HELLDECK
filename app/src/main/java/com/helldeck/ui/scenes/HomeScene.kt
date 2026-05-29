package com.helldeck.ui.scenes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.Scene
import com.helldeck.ui.components.GamePickerSheet
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.components.SpiceSlider
import com.helldeck.ui.components.WarningBanner
import com.helldeck.utils.ValidationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    var showGamePicker by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    val spiceLevel by vm.spiceLevel.collectAsState()

    val activePlayers = vm.players.filter { it.afk == 0 }
    val playerCountValidation = ValidationUtils.validatePlayerCount(activePlayers.size)
    val playerCountWarning = ValidationUtils.getPlayerCountWarning(activePlayers.size)
    val reducedMotion = LocalReducedMotion.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "HELLDECK",
                            fontWeight = FontWeight.Black,
                            color = HelldeckColors.colorPrimary,
                        )
                        if (activePlayers.isNotEmpty()) {
                            Text(
                                "${activePlayers.size} players ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = HelldeckColors.colorMuted,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.navigateTo(Scene.ROLLCALL) },
                        modifier = Modifier.semantics {
                            contentDescription = "Open rollcall"
                        },
                    ) {
                        Icon(Icons.Rounded.HowToReg, contentDescription = "Rollcall")
                    }
                    IconButton(
                        onClick = { vm.navigateTo(Scene.SETTINGS) },
                        modifier = Modifier.semantics {
                            contentDescription = "Open settings"
                        },
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        if (vm.players.isEmpty()) {
            HomeEmptyState(
                onAddPlayers = { vm.navigateTo(Scene.PLAYERS) },
                onRules = { vm.navigateTo(Scene.FULL_RULES_BROWSER) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(HelldeckSpacing.Large.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HomeHero(
                    activePlayerCount = activePlayers.size,
                    totalPlayerCount = vm.players.size,
                    gameCount = GameMetadata.getAllGames().size,
                    spiceLevel = spiceLevel,
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

                ActiveCrewStrip(activePlayers = activePlayers)

                Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

                // Spice Level Slider
                SpiceSlider(
                    spiceLevel = spiceLevel,
                    onSpiceLevelChanged = { vm.updateSpiceLevel(it) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

                // Player count validation warnings
                if (!playerCountValidation.isValid) {
                    WarningBanner(
                        message = playerCountValidation.errorMessage ?: "Need more players",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                } else if (playerCountWarning != null) {
                    InfoBanner(
                        message = playerCountWarning,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                }

                // Primary CTA: Pulsing play button
                val pulseScale = if (!reducedMotion && playerCountValidation.isValid) {
                    val infiniteTransition = rememberInfiniteTransition(label = "play_pulse")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.03f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "pulse_scale",
                    )
                    pulse
                } else {
                    1f
                }

                GlowButton(
                    text = if (playerCountValidation.isValid) {
                        "\uD83D\uDD25 Start the Chaos"
                    } else {
                        "\u26A0\uFE0F Add Players First"
                    },
                    onClick = {
                        if (playerCountValidation.isValid) {
                            scope.launch { vm.startRound(null) }
                        } else {
                            vm.navigateTo(Scene.PLAYERS)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(pulseScale),
                    accentColor = if (playerCountValidation.isValid) {
                        HelldeckColors.colorPrimary
                    } else {
                        HelldeckColors.colorMuted
                    },
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

                // Two secondary actions: rules and everything else.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryActionCard(
                            icon = "\uD83D\uDCD6",
                            title = "Game Rules",
                            subtitle = "Learn how to play",
                            accentColor = HelldeckColors.colorAccentCool,
                            onClick = { vm.navigateTo(Scene.FULL_RULES_BROWSER) },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SecondaryActionCard(
                            icon = "\u2699\uFE0F",
                            title = "More",
                            subtitle = "Games, stats & extras",
                            accentColor = HelldeckColors.colorMuted,
                            onClick = { showMoreSheet = true },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

                Text(
                    text = "Pass one phone. Keep the round moving. Let the room decide.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HelldeckColors.colorMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
            }
        }

        // "More" Bottom Sheet
        if (showMoreSheet) {
            MoreOptionsSheet(
                onDismiss = { showMoreSheet = false },
                onMiniGames = {
                    showMoreSheet = false
                    showGamePicker = true
                },
                onCrewBrain = {
                    showMoreSheet = false
                    vm.navigateTo(Scene.STATS)
                },
                onSafety = {
                    showMoreSheet = false
                    vm.navigateTo(Scene.SETTINGS)
                },
                onCustomCards = {
                    showMoreSheet = false
                    vm.navigateTo(Scene.CUSTOM_CARDS)
                },
                onFavorites = {
                    showMoreSheet = false
                    vm.navigateTo(Scene.FAVORITES)
                },
                onScores = {
                    showMoreSheet = false
                    vm.toggleScores()
                },
            )
        }

        // Game Picker Modal Sheet
        if (showGamePicker) {
            GamePickerSheet(
                onGameSelected = { gameId ->
                    showGamePicker = false
                    scope.launch { vm.startRound(gameId) }
                },
                onDismiss = { showGamePicker = false },
            )
        }

        // Scoreboard Overlay
        if (vm.showScores) {
            ScoreboardOverlay(vm.players) { vm.toggleScores() }
        }
    }
}

@Composable
private fun HomeEmptyState(
    onAddPlayers: () -> Unit,
    onRules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        HelldeckColors.background,
                        HelldeckColors.surfacePrimary,
                    ),
                ),
            )
            .padding(HelldeckSpacing.Large.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            Text(
                text = "HELLDECK",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                color = HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Set the table before the first round.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = HelldeckColors.colorMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HelldeckRadius.Large),
                color = HelldeckColors.surfaceElevated.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, HelldeckColors.colorPrimary.copy(alpha = 0.45f)),
            ) {
                Column(
                    modifier = Modifier.padding(HelldeckSpacing.Large.dp),
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "3-10 players is the sweet spot.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.colorOnDark,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Names stay local. Add seats, pick the heat, start playing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HelldeckColors.colorMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            GlowButton(
                text = "Add Players",
                onClick = onAddPlayers,
                modifier = Modifier.fillMaxWidth(),
                accentColor = HelldeckColors.colorSecondary,
            )

            com.helldeck.ui.components.OutlineButton(
                text = "Read Rules",
                onClick = onRules,
                modifier = Modifier.fillMaxWidth(),
                accentColor = HelldeckColors.colorAccentCool,
            )
        }
    }
}

@Composable
private fun HomeHero(
    activePlayerCount: Int,
    totalPlayerCount: Int,
    gameCount: Int,
    spiceLevel: Int,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 164.dp),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = HelldeckColors.surfaceElevated,
        border = BorderStroke(1.dp, HelldeckColors.colorPrimary.copy(alpha = 0.48f)),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            HelldeckColors.colorPrimary.copy(alpha = 0.26f),
                            HelldeckColors.surfaceElevated,
                            HelldeckColors.colorAccentCool.copy(alpha = 0.14f),
                        ),
                    ),
                )
                .padding(HelldeckSpacing.Large.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Tiny.dp)) {
                Text(
                    text = "HELLDECK",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = HelldeckColors.colorOnDark,
                )
                Text(
                    text = "Low cognitive load. High social stakes. Maximum chaos.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = HelldeckColors.colorMuted,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            ) {
                StatusMetric(
                    label = "Ready",
                    value = "$activePlayerCount/$totalPlayerCount",
                    color = HelldeckColors.colorSecondary,
                    modifier = Modifier.weight(1f),
                )
                StatusMetric(
                    label = "Games",
                    value = "$gameCount",
                    color = HelldeckColors.colorAccentCool,
                    modifier = Modifier.weight(1f),
                )
                StatusMetric(
                    label = "Heat",
                    value = "$spiceLevel",
                    color = HelldeckColors.colorAccentWarm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatusMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        color = HelldeckColors.background.copy(alpha = 0.44f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = HelldeckSpacing.Small.dp, vertical = HelldeckSpacing.Medium.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ActiveCrewStrip(activePlayers: List<com.helldeck.content.model.Player>) {
    if (activePlayers.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = HelldeckColors.surfacePrimary.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, HelldeckColors.colorMuted.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        ) {
            Text(
                text = "Crew",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorMuted,
            )

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy((-6).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                activePlayers.take(8).forEachIndexed { index, player ->
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = HelldeckColors.surfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            when (index % 3) {
                                0 -> HelldeckColors.colorPrimary
                                1 -> HelldeckColors.colorAccentCool
                                else -> HelldeckColors.colorSecondary
                            }.copy(alpha = 0.7f),
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = player.avatar,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            Text(
                text = "${activePlayers.size} active",
                style = MaterialTheme.typography.labelMedium,
                color = HelldeckColors.colorSecondary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SecondaryActionCard(
    icon: String,
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color = HelldeckColors.colorPrimary,
    onClick: () -> Unit,
) {
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title: $subtitle"
            },
        accentColor = accentColor,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorOnDark,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.colorMuted,
                )
            }
            Text(
                text = "\u203A",
                style = MaterialTheme.typography.headlineMedium,
                color = HelldeckColors.colorMuted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionsSheet(
    onDismiss: () -> Unit,
    onMiniGames: () -> Unit,
    onCrewBrain: () -> Unit,
    onSafety: () -> Unit,
    onCustomCards: () -> Unit,
    onFavorites: () -> Unit,
    onScores: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HelldeckColors.surfacePrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HelldeckSpacing.Large.dp)
                .padding(bottom = HelldeckSpacing.ExtraLarge.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        ) {
            Text(
                text = "More Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorPrimary,
                modifier = Modifier.padding(bottom = HelldeckSpacing.Small.dp),
            )

            SecondaryActionCard(
                icon = "\uD83C\uDFB2",
                title = "Mini Games",
                subtitle = "Quick party games",
                accentColor = HelldeckColors.colorAccentWarm,
                onClick = onMiniGames,
            )

            SecondaryActionCard(
                icon = "\uD83D\uDCCA",
                title = "Crew Brain",
                subtitle = "View statistics",
                accentColor = HelldeckColors.colorSecondary,
                onClick = onCrewBrain,
            )

            SecondaryActionCard(
                icon = "\uD83C\uDFC6",
                title = "Scoreboard",
                subtitle = "Current scores",
                accentColor = HelldeckColors.colorAccentCool,
                onClick = onScores,
            )

            SecondaryActionCard(
                icon = "\u2728",
                title = "Custom Cards",
                subtitle = "Create your own",
                accentColor = HelldeckColors.Lol,
                onClick = onCustomCards,
            )

            SecondaryActionCard(
                icon = "\u2B50",
                title = "Favorites",
                subtitle = "Saved moments",
                accentColor = HelldeckColors.colorPrimary,
                onClick = onFavorites,
            )

            SecondaryActionCard(
                icon = "\uD83D\uDEE1\uFE0F",
                title = "Safety & Settings",
                subtitle = "Controls & preferences",
                accentColor = HelldeckColors.colorMuted,
                onClick = onSafety,
            )
        }
    }
}
