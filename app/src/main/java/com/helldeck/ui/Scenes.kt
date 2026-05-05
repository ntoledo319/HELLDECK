package com.helldeck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.data.toEntity
import com.helldeck.engine.*
import com.helldeck.ui.scenes.GameRulesScene
import com.helldeck.ui.scenes.FullRulesBrowserScene
import com.helldeck.ui.components.HelldeckBackgroundPattern
import com.helldeck.ui.components.PlayerProfileScene
import com.helldeck.ui.components.RulesSheet
import com.helldeck.ui.components.ScoreboardOverlay
import com.helldeck.ui.scenes.FeedbackScene
import com.helldeck.ui.scenes.HomeScene
import com.helldeck.ui.scenes.PlayersScene
import com.helldeck.ui.scenes.RollcallScene
import com.helldeck.ui.scenes.RoundScene
import com.helldeck.ui.scenes.SettingsScene
import com.helldeck.ui.scenes.StatsScene
import com.helldeck.ui.vm.GameNightViewModel
import kotlinx.coroutines.launch

/**
 * Scene enumeration for navigation
 */
enum class Scene {
    HOME, ROLLCALL, PLAYERS, ROUND, FEEDBACK, RULES, SCOREBOARD, STATS, SETTINGS, PROFILE, GAME_RULES, FULL_RULES_BROWSER, CARD_LAB, ONBOARDING, FAVORITES, CUSTOM_CARDS
}

@Composable
internal fun hdFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )

/**
 * Main HELLDECK app UI composable with error boundaries
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun HelldeckAppUI(
    vm: HelldeckVm = viewModel(),
    modifier: Modifier = Modifier,
) {
    var error by remember { mutableStateOf<HelldeckError?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show ViewModel error messages as snackbar
    val vmError = vm.errorMessage
    LaunchedEffect(vmError) {
        if (vmError != null) {
            snackbarHostState.showSnackbar(vmError)
            vm.clearError()
        }
    }

    LaunchedEffect(Unit) {
        com.helldeck.utils.Logger.i("HelldeckAppUI: Initializing ViewModel")
        try {
            vm.initOnce()
            com.helldeck.utils.Logger.i("HelldeckAppUI: ViewModel initialized")
        } catch (e: Exception) {
            error = HelldeckError.UnknownError(
                message = "Failed to initialize app: ${e.message}",
                technicalDetails = e.stackTraceToString(),
            )
        }
    }

    BackHandler(enabled = vm.scene != Scene.HOME) {
        try {
            vm.goBack()
        } catch (e: Exception) {
            error = HelldeckError.UnknownError(
                message = "Navigation failed: ${e.message}",
                technicalDetails = e.stackTraceToString(),
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadingWithErrorBoundary(
            isLoading = vm.isLoading,
            error = error,
            onRetry = {
                error = null
                coroutineScope.launch {
                    try {
                        vm.initOnce()
                    } catch (e: Exception) {
                        error = HelldeckError.UnknownError(
                            message = "Retry failed: ${e.message}",
                            technicalDetails = e.stackTraceToString(),
                        )
                    }
                }
            },
            onDismiss = {
                if (error?.recoverable == false) {
                    // For critical errors, close app
                    // In a real app, you might want to exit gracefully
                    error = null
                } else {
                    error = null
                }
            },
            loadingContent = {
                // Background pattern for visual interest
                HelldeckBackgroundPattern(
                    pattern = BackgroundPattern.CIRCUIT,
                    opacity = 0.03f,
                )

                // Scene transitions — card-slap with neon discharge
                com.helldeck.utils.Logger.i("HelldeckAppUI: Current scene: ${vm.scene}")
                val reducedMotion = LocalReducedMotion.current

                // Neon flash on scene change: electric discharge between scenes
                val flashAlpha = remember { Animatable(0f) }
                LaunchedEffect(vm.scene) {
                    if (!reducedMotion) {
                        flashAlpha.snapTo(0.07f)
                        flashAlpha.animateTo(
                            0f,
                            animationSpec = tween(100, easing = LinearEasing),
                        )
                    }
                }

                AnimatedContent(
                    targetState = vm.scene,
                    transitionSpec = {
                        // Card-slap: scene drops in hard from above with a scale pop.
                        // Outgoing scene punches down and out. No gentle fades here.
                        if (reducedMotion) {
                            fadeIn(animationSpec = tween(HelldeckAnimations.Instant)) togetherWith
                                fadeOut(animationSpec = tween(HelldeckAnimations.Instant))
                        } else {
                            (
                                fadeIn(animationSpec = tween(HelldeckAnimations.Fast, easing = LinearEasing)) +
                                    scaleIn(
                                        animationSpec = spring(
                                            dampingRatio = 0.7f,
                                            stiffness = Spring.StiffnessHigh,
                                        ),
                                        initialScale = 1.06f,
                                    ) +
                                    slideInVertically(
                                        animationSpec = tween(HelldeckAnimations.Fast, easing = EaseOutCubic),
                                        initialOffsetY = { -it / 5 },
                                    )
                                ) togetherWith
                                (
                                    fadeOut(animationSpec = tween(HelldeckAnimations.Fast / 2, easing = LinearEasing)) +
                                        scaleOut(
                                            animationSpec = tween(HelldeckAnimations.Fast, easing = EaseInCubic),
                                            targetScale = 0.92f,
                                        ) +
                                        slideOutVertically(
                                            animationSpec = tween(HelldeckAnimations.Fast, easing = EaseInCubic),
                                            targetOffsetY = { it / 6 },
                                        )
                                    )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) { targetScene ->
                    when (targetScene) {
                        Scene.HOME -> HomeScene(vm)
                        Scene.ONBOARDING -> com.helldeck.ui.components.OnboardingFlow(
                            onComplete = { players ->
                                coroutineScope.launch {
                                    // Save onboarding players to database
                                    if (players.isNotEmpty()) {
                                        val repo = com.helldeck.content.data.ContentRepository(com.helldeck.AppCtx.ctx)
                                        players.forEach { player ->
                                            repo.db.players().upsert(player.toEntity())
                                        }
                                        vm.reloadPlayers()
                                    }
                                    com.helldeck.settings.SettingsStore.writeHasSeenOnboarding(true)
                                    vm.scene = Scene.HOME
                                }
                            },
                        )
                        Scene.ROLLCALL -> RollcallScene(vm)
                        Scene.PLAYERS -> PlayersScene(vm)
                        Scene.ROUND -> RoundScene(vm)
                        Scene.FEEDBACK -> FeedbackScene(vm)
                        Scene.RULES -> RulesSheet { vm.scene = Scene.HOME }
                        Scene.SCOREBOARD -> ScoreboardOverlay(vm.players) { vm.scene = Scene.HOME }
                        Scene.STATS -> StatsScene(onClose = { vm.scene = Scene.HOME }, vm = vm)
                        Scene.SETTINGS -> SettingsScene(onClose = { vm.scene = Scene.HOME }, vm = vm)
                        Scene.PROFILE -> PlayerProfileScene(vm = vm, onClose = { vm.scene = Scene.HOME })
                        Scene.GAME_RULES -> GameRulesScene(vm = vm, onClose = { vm.goBack() })
                        Scene.FULL_RULES_BROWSER -> FullRulesBrowserScene(vm = vm)
                        Scene.CARD_LAB -> if (com.helldeck.BuildConfig.DEBUG_MODE) {
                            com.helldeck.ui.scenes.CardLabScene(onClose = { vm.goBack() })
                        } else {
                            HomeScene(vm)
                        }
                        Scene.FAVORITES -> com.helldeck.ui.scenes.FavoritesScene(vm = vm, onClose = { vm.goBack() })
                        Scene.CUSTOM_CARDS -> com.helldeck.ui.scenes.CustomCardCreatorScene(vm = vm)
                    }
                }

                // Neon flash overlay — renders on top of scene content
                if (flashAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(HelldeckColors.colorPrimary.copy(alpha = flashAlpha.value)),
                    )
                }

                // Celebration dialog overlay (shows on top of any scene)
                vm.pendingMilestone?.let { milestone ->
                    com.helldeck.ui.components.CelebrationDialog(
                        milestone = milestone,
                        onDismiss = { vm.clearPendingMilestone() },
                    )
                }

                // Session end prompt overlay — dramatic intermission
                if (vm.showSessionEndPrompt) {
                    Dialog(
                        onDismissRequest = { vm.dismissSessionEndPrompt() },
                        properties = DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                        ),
                    ) {
                        com.helldeck.ui.components.NeonCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = HelldeckColors.colorPrimary,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
                            ) {
                                // Round count badge
                                Text(
                                    text = "${vm.totalRoundsThisSession}",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Black,
                                    color = HelldeckColors.colorPrimary,
                                    textAlign = TextAlign.Center,
                                )

                                Text(
                                    text = "ROUNDS SURVIVED",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HelldeckColors.colorSecondary,
                                    letterSpacing = 3.sp,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                                Text(
                                    text = "You're still standing. Check the damage or get back in the pit.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = HelldeckColors.colorMuted,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                                // Primary: keep playing
                                com.helldeck.ui.components.GlowButton(
                                    text = "KEEP PLAYING",
                                    onClick = { vm.dismissSessionEndPrompt() },
                                    modifier = Modifier.fillMaxWidth(),
                                    accentColor = HelldeckColors.colorSecondary,
                                )

                                // Secondary: see scores
                                com.helldeck.ui.components.OutlineButton(
                                    text = "SEE THE DAMAGE",
                                    onClick = {
                                        vm.dismissSessionEndPrompt()
                                        vm.scene = Scene.STATS
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    accentColor = HelldeckColors.colorPrimary,
                                )
                            }
                        }
                    }
                }
            },
        )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )
        }
    }
}

typealias HelldeckVm = GameNightViewModel
