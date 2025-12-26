package com.helldeck.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.util.SeededRng
import com.helldeck.content.model.Player
import com.helldeck.engine.*
import com.helldeck.engine.GameMetadata
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.random.Random
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.helldeck.data.PlayerEntity
import com.helldeck.data.computePlayerProfiles
import com.helldeck.data.toPlayer
import com.helldeck.data.toEntity
import com.helldeck.ui.scenes.HomeScene
import com.helldeck.ui.scenes.RollcallScene
import com.helldeck.ui.scenes.PlayersScene
import com.helldeck.ui.scenes.RoundScene
import com.helldeck.ui.scenes.FeedbackScene
import com.helldeck.ui.scenes.SettingsScene
import com.helldeck.ui.scenes.StatsScene
import com.helldeck.ui.components.GameRulesScene
import com.helldeck.ui.components.PlayerProfileScene
import com.helldeck.ui.components.ScoreboardOverlay
import com.helldeck.ui.components.RulesSheet
import com.helldeck.ui.components.HelldeckLoadingSpinner
import com.helldeck.ui.components.HelldeckBackgroundPattern
import com.helldeck.content.engine.ContentEngineProvider
import com.helldeck.ui.LoadingWithErrorBoundary
import com.helldeck.ui.BackgroundPattern
import com.helldeck.ui.HelldeckError
import com.helldeck.ui.vm.GameNightViewModel

/**
 * Scene enumeration for navigation
 */
enum class Scene {
    HOME, ROLLCALL, PLAYERS, ROUND, FEEDBACK, RULES, SCOREBOARD, STATS, SETTINGS, PROFILE, GAME_RULES, CARD_LAB, ONBOARDING, FAVORITES, CUSTOM_CARDS
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
        unfocusedContainerColor = Color.Transparent
    )

/**
 * Main HELLDECK app UI composable with error boundaries
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun HelldeckAppUI(
    vm: HelldeckVm = viewModel(),
    modifier: Modifier = Modifier
) {
    var error by remember { mutableStateOf<HelldeckError?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        Config.load()
        com.helldeck.utils.Logger.i("HelldeckAppUI: Initializing ViewModel")
        try {
            vm.initOnce()
            com.helldeck.utils.Logger.i("HelldeckAppUI: ViewModel initialized")
        } catch (e: Exception) {
            error = HelldeckError.UnknownError(
                message = "Failed to initialize app: ${e.message}",
                technicalDetails = e.stackTraceToString()
            )
        }
    }

    BackHandler(enabled = vm.scene != Scene.HOME) {
        try {
            vm.goBack()
        } catch (e: Exception) {
            error = HelldeckError.UnknownError(
                message = "Navigation failed: ${e.message}",
                technicalDetails = e.stackTraceToString()
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                            technicalDetails = e.stackTraceToString()
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
                    opacity = 0.03f
                )

                // Scene transitions with smooth animations
                com.helldeck.utils.Logger.i("HelldeckAppUI: Current scene: ${vm.scene}")
                val reducedMotion = LocalReducedMotion.current
    AnimatedContent(
        targetState = vm.scene,
                    transitionSpec = {
                    // Hell’s Living Room: fast, directional, consistent.
                    if (reducedMotion) {
                        fadeIn(animationSpec = tween(HelldeckAnimations.Instant)) togetherWith
                            fadeOut(animationSpec = tween(HelldeckAnimations.Instant))
                    } else {
                        (fadeIn(animationSpec = tween(HelldeckAnimations.Fast)) +
                            slideInHorizontally(
                                animationSpec = tween(HelldeckAnimations.Normal, easing = EaseInOutSine),
                                initialOffsetX = { it / 8 }
                            )) togetherWith
                            (fadeOut(animationSpec = tween(HelldeckAnimations.Fast)) +
                                slideOutHorizontally(
                                    animationSpec = tween(HelldeckAnimations.Normal, easing = EaseInOutSine),
                                    targetOffsetX = { -it / 8 }
                                ))
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { targetScene ->
                when (targetScene) {
                    Scene.HOME -> HomeScene(vm)
                    Scene.ONBOARDING -> com.helldeck.ui.components.OnboardingFlow(
                        onComplete = {
                            coroutineScope.launch {
                                com.helldeck.settings.SettingsStore.writeHasSeenOnboarding(true)
                                vm.scene = Scene.HOME
                            }
                        }
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
                    Scene.CARD_LAB -> com.helldeck.ui.scenes.CardLabScene(onClose = { vm.goBack() })
                    Scene.FAVORITES -> com.helldeck.ui.scenes.FavoritesScene(vm = vm, onClose = { vm.goBack() })
                    Scene.CUSTOM_CARDS -> com.helldeck.ui.scenes.CustomCardCreatorScene(vm = vm)
                }
            }

                // Celebration dialog overlay (shows on top of any scene)
                vm.pendingMilestone?.let { milestone ->
                    com.helldeck.ui.components.CelebrationDialog(
                        milestone = milestone,
                        onDismiss = { vm.clearPendingMilestone() }
                    )
                }
            }
        )
    }
}

typealias HelldeckVm = GameNightViewModel
