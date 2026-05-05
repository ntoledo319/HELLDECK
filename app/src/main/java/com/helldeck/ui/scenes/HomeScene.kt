package com.helldeck.ui.scenes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
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
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.Scene
import com.helldeck.ui.components.EmptyState
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
            EmptyState(
                icon = "\uD83D\uDC65",
                title = "Welcome to HELLDECK",
                message = "Add players to start your first game session.\n\nRecommended: 3-10 players for best experience.",
                actionLabel = "Add Players",
                onActionClick = { vm.navigateTo(Scene.PLAYERS) },
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
                HellTitleCard()

                Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

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

                // Two secondary action cards: Game Rules + More
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

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Pass one phone \u2022 Judge, roast, and betray your friends \u2022 ${GameMetadata.getAllGames().size} mini-games",
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
private fun HellTitleCard() {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = HelldeckColors.colorPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            HelldeckColors.colorPrimary.copy(alpha = 0.85f),
                            HelldeckColors.colorAccentCool.copy(alpha = 0.35f),
                            HelldeckColors.colorSecondary.copy(alpha = 0.15f),
                        ),
                    ),
                )
                .padding(vertical = HelldeckSpacing.ExtraLarge.dp, horizontal = HelldeckSpacing.Large.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        ) {
            Text(
                text = "HELLDECK",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = HelldeckColors.colorOnDark,
            )
            Text(
                text = "Low Cognitive Load. High Social Stakes. Maximum Chaos.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = HelldeckColors.colorOnDark.copy(alpha = 0.95f),
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
    val reducedMotion = LocalReducedMotion.current

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
