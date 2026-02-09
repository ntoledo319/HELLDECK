package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckAnimations
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.theme.HelldeckSpacing
import com.helldeck.content.model.Player
import com.helldeck.utils.ValidationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Redesigned onboarding for first-time users.
 * 
 * Design Philosophy:
 * - Get users playing in <20 seconds
 * - 3 streamlined steps (Welcome → Gesture → Setup)
 * - Uses design system components (NeonCard, GlowButton, etc.)
 * - Proper Scaffold architecture with safe insets
 * - Progressive disclosure (advanced features taught contextually)
 * - Clear visual hierarchy with spacing tokens
 * 
 * Flow: Welcome (5s) → Core Gesture (10s) → Quick Setup (5-15s)
 * Total: 3 steps, ~20 seconds
 * 
 * @ai_prompt Redesigned onboarding using HELLDECK design system
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    onComplete: (players: List<Player>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var currentStep by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val totalSteps = 3
    var onboardingPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingTopBar(
                currentStep = currentStep,
                totalSteps = totalSteps,
                onSkip = { onComplete(onboardingPlayers) },
            )
        },
        containerColor = HelldeckColors.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HelldeckColors.background,
                            HelldeckColors.surfacePrimary,
                        ),
                    ),
                )
                .padding(padding),
        ) {
            // Main content with smooth transitions
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        ) + fadeOut(animationSpec = tween(300))
                },
                label = "onboarding_step",
            ) { step ->
                when (step) {
                    0 -> WelcomeStepV2(
                        reducedMotion = reducedMotion,
                        onNext = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            currentStep = 1
                        },
                    )
                    1 -> GestureDemoV2(
                        reducedMotion = reducedMotion,
                        onComplete = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            currentStep = 2
                        },
                    )
                    2 -> QuickSetupV2(
                        reducedMotion = reducedMotion,
                        players = onboardingPlayers,
                        onPlayersChanged = { onboardingPlayers = it },
                        onComplete = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onComplete(onboardingPlayers)
                        },
                    )
                }
            }

            // Step indicators at bottom
            StepIndicators(
                currentStep = currentStep,
                totalSteps = totalSteps,
                reducedMotion = reducedMotion,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = HelldeckSpacing.ExtraLarge.dp),
            )
        }
    }
}

/**
 * Top bar with progress indicator and skip button
 */
@Composable
private fun OnboardingTopBar(
    currentStep: Int,
    totalSteps: Int,
    onSkip: () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    
    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = (currentStep + 1).toFloat() / totalSteps,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "progress_animation",
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HelldeckColors.background.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HelldeckSpacing.Large.dp, vertical = HelldeckSpacing.Medium.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Progress indicator with glow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(4.dp),
                        spotColor = HelldeckColors.colorPrimary.copy(alpha = 0.3f),
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(HelldeckColors.surfaceElevated),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    HelldeckColors.colorPrimary,
                                    HelldeckColors.colorSecondary,
                                ),
                            ),
                        ),
                )
            }

            Spacer(Modifier.width(HelldeckSpacing.Large.dp))

            // Skip button with hover effect
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            
            val skipScale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = if (reducedMotion) {
                    tween(HelldeckAnimations.Instant)
                } else {
                    spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
                },
                label = "skip_scale",
            )
            
            TextButton(
                onClick = onSkip,
                interactionSource = interactionSource,
                modifier = Modifier.scale(skipScale),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HelldeckColors.colorOnDark,
                ),
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " →",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Step indicator dots at bottom
 */
@Composable
private fun StepIndicators(
    currentStep: Int,
    totalSteps: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val isPast = index < currentStep
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.3f else 1f,
                animationSpec = if (reducedMotion) {
                    tween(HelldeckAnimations.Instant)
                } else {
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                },
                label = "step_indicator_scale",
            )

            val dotColor = when {
                isActive -> HelldeckColors.colorPrimary
                isPast -> HelldeckColors.colorSecondary
                else -> HelldeckColors.surfaceElevated
            }

            Box(
                modifier = Modifier
                    .size(if (isActive) 14.dp else 10.dp)
                    .scale(scale)
                    .shadow(
                        elevation = if (isActive) 8.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = HelldeckColors.colorPrimary.copy(alpha = 0.6f),
                    )
                    .clip(CircleShape)
                    .background(dotColor)
                    .then(
                        if (isActive) {
                            Modifier.border(
                                width = 2.dp,
                                color = HelldeckColors.colorPrimary.copy(alpha = 0.5f),
                                shape = CircleShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

/**
 * Step 1: Welcome screen with value proposition
 */
@Composable
private fun WelcomeStepV2(
    reducedMotion: Boolean,
    onNext: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            )
        },
        label = "welcome_scale",
    )

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        delay(200)
        showFeatures = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HelldeckSpacing.ExtraLarge.dp)
            .scale(scale),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val emojiScale by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = if (reducedMotion) {
                tween(HelldeckAnimations.Instant)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                )
            },
            label = "emoji_scale",
        )

        Text(
            text = "🔥",
            fontSize = (80 * emojiScale).sp,
            modifier = Modifier.scale(emojiScale),
        )

        Spacer(Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineSmall,
            color = HelldeckColors.colorMuted,
            fontWeight = FontWeight.Normal,
        )

        Spacer(Modifier.height(HelldeckSpacing.Small.dp))

        Text(
            text = "HELLDECK",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = HelldeckColors.colorPrimary,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "The party game that learns\nwhat your crew finds funny",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = HelldeckColors.colorOnDark.copy(alpha = 0.8f),
            lineHeight = 28.sp,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Feature banners with staggered entrance
        AnimatedVisibility(
            visible = showFeatures,
            enter = fadeIn(animationSpec = tween(HelldeckAnimations.Normal)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                InfoBanner(
                    message = "${GameMetadata.getAllGames().size} unique party games",
                    icon = "🎯",
                    backgroundColor = HelldeckColors.colorSecondary.copy(alpha = 0.12f),
                )
                InfoBanner(
                    message = "One phone, 3-16 players",
                    icon = "📱",
                    backgroundColor = HelldeckColors.colorAccentCool.copy(alpha = 0.12f),
                )
                InfoBanner(
                    message = "AI that adapts to your humor",
                    icon = "🧠",
                    backgroundColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.12f),
                )
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        GlowButton(
            text = "Let's Go",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            icon = "🔥",
        )
    }
}

/**
 * Step 2: Interactive gesture demo
 */
@Composable
private fun GestureDemoV2(
    reducedMotion: Boolean,
    onComplete: () -> Unit,
) {
    var showHint by remember { mutableStateOf(true) }
    var pulseScale by remember { mutableStateOf(1f) }
    var isPressed by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableStateOf(0f) }
    var showSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Pulsing animation for hint
    LaunchedEffect(showHint, reducedMotion) {
        if (showHint && !reducedMotion) {
            while (true) {
                animate(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                ) { value, _ -> pulseScale = value }
                animate(
                    initialValue = 1.08f,
                    targetValue = 1f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing),
                ) { value, _ -> pulseScale = value }
                delay(200)
            }
        }
    }
    
    // Success animation before advancing
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(600)
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HelldeckSpacing.ExtraLarge.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "One Simple Gesture",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = HelldeckColors.colorOnDark,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "Long-press to draw cards",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = HelldeckColors.colorMuted,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        val cardScale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else pulseScale,
            animationSpec = if (reducedMotion) {
                tween(HelldeckAnimations.Instant)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            },
            label = "card_scale",
        )

        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(cardScale),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    showHint = false
                                    isPressed = false
                                    showSuccess = true
                                    haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                                    )
                                },
                                onPress = {
                                    isPressed = true
                                    coroutineScope.launch {
                                        animate(
                                            initialValue = 0f,
                                            targetValue = 1f,
                                            animationSpec = tween(800),
                                        ) { value, _ ->
                                            pressProgress = value
                                        }
                                    }
                                    val released = tryAwaitRelease()
                                    isPressed = false
                                    if (released) {
                                        pressProgress = 0f
                                    }
                                },
                            )
                        },
                    accentColor = if (showHint) HelldeckColors.colorPrimary else HelldeckColors.colorSecondary,
                    elevation = if (isPressed) 4.dp else 16.dp,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showHint) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
                            ) {
                                Text(
                                    text = "👆",
                                    fontSize = 64.sp,
                                )
                                Text(
                                    text = "Try it!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HelldeckColors.colorPrimary,
                                )
                                Text(
                                    text = "Long-press here",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = HelldeckColors.colorMuted,
                                )
                            }
                        } else {
                            // Success celebration
                            val successScale by animateFloatAsState(
                                targetValue = if (showSuccess) 1f else 0.8f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                                label = "success_scale",
                            )
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                                modifier = Modifier.scale(successScale),
                            ) {
                                Text(
                                    text = "✓",
                                    fontSize = 56.sp,
                                    color = HelldeckColors.colorSecondary,
                                )
                                Text(
                                    text = "Perfect!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HelldeckColors.colorSecondary,
                                )
                            }
                        }
                    }
                }

                // Progress indicator when pressing
                if (isPressed && pressProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pressProgress)
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        HelldeckColors.colorPrimary,
                                        HelldeckColors.colorSecondary,
                                    ),
                                ),
                            )
                            .align(Alignment.BottomStart),
                    )
                }
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        InfoBanner(
            message = "That's it! This is how you'll play",
            icon = "💡",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


/**
 * Step 3: Quick setup - add players or skip
 */
@Composable
private fun QuickSetupV2(
    @Suppress("UNUSED_PARAMETER") reducedMotion: Boolean,
    players: List<Player>,
    onPlayersChanged: (List<Player>) -> Unit,
    onComplete: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val warning = ValidationUtils.getPlayerCountWarning(players.size)
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(HelldeckSpacing.Large.dp),
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Large.dp),
    ) {
        // Header with entrance animation
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(HelldeckAnimations.Normal)) +
                slideInVertically(
                    initialOffsetY = { -it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                Text(
                    text = "🎉",
                    fontSize = 64.sp,
                )

                Text(
                    text = "You're Ready!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = HelldeckColors.colorSecondary,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Add players now or explore first",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = HelldeckColors.colorMuted,
                )
            }
        }

        // Player count status
        if (players.isEmpty()) {
            EmptyState(
                icon = "👥",
                title = "No Players Yet",
                message = "Best with 3+ players, but you can add them later from the menu",
                actionLabel = "Add First Player",
                onActionClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = HelldeckSpacing.Large.dp),
            )
        } else {
            // Player count card
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = if (players.size >= 3) {
                    HelldeckColors.colorSecondary
                } else {
                    HelldeckColors.colorPrimary
                },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                ) {
                    Text(
                        text = "${players.size} ${if (players.size == 1) "Player" else "Players"}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (players.size >= 3) {
                            HelldeckColors.colorSecondary
                        } else {
                            HelldeckColors.colorOnDark
                        },
                    )
                    if (players.size < 3) {
                        Text(
                            text = "Add ${3 - players.size} more for best experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                }
            }

            // Validation warning
            if (warning != null) {
                InfoBanner(
                    message = warning,
                    icon = "ℹ️",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Player list with enter/exit animations
            Column(
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            ) {
                players.forEach { player ->
                    key(player.id) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(HelldeckAnimations.Normal)) +
                                expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                ),
                            exit = fadeOut(animationSpec = tween(HelldeckAnimations.Fast)) +
                                shrinkVertically(
                                    animationSpec = tween(HelldeckAnimations.Normal),
                                ),
                        ) {
                            NeonCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = HelldeckColors.colorAccentCool,
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
                                        Text(text = player.avatar, fontSize = 32.sp)
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = HelldeckColors.colorOnDark,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onPlayersChanged(players.filter { it.id != player.id })
                                        },
                                    ) {
                                        Text(
                                            text = "✕",
                                            fontSize = 20.sp,
                                            color = HelldeckColors.colorDangerText,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add another player button
            OutlineButton(
                text = "Add Another",
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                icon = "➕",
            )
        }

        Spacer(Modifier.weight(1f))

        // Quick tips
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            accentColor = HelldeckColors.colorAccentCool,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                Text(
                    text = "Quick Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorAccentCool,
                )
                InfoBanner(
                    message = "Long-press anywhere to draw cards",
                    icon = "👆",
                    backgroundColor = HelldeckColors.surfacePrimary,
                )
                InfoBanner(
                    message = "Two-finger tap to undo",
                    icon = "↩️",
                    backgroundColor = HelldeckColors.surfacePrimary,
                )
                InfoBanner(
                    message = "Adjust spice level in settings",
                    icon = "🌶️",
                    backgroundColor = HelldeckColors.surfacePrimary,
                )
            }
        }

        // Primary CTA
        GlowButton(
            text = if (players.size >= 3) "Start Playing" else "Explore App",
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            icon = "🎉",
            accentColor = if (players.size >= 3) {
                HelldeckColors.colorSecondary
            } else {
                HelldeckColors.colorPrimary
            },
        )
    }

    // Add player dialog
    if (showAddDialog) {
        AddPlayerDialog(
            existingPlayers = players,
            onDismiss = { showAddDialog = false },
            onPlayerCreated = { name, emoji ->
                val newPlayer = Player(
                    id = ValidationUtils.generateUniquePlayerId(players),
                    name = name,
                    avatar = emoji,
                )
                onPlayersChanged(players + newPlayer)
            },
        )
    }
}
