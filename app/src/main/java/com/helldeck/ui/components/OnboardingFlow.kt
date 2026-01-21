package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.theme.HelldeckSpacing
import com.helldeck.content.model.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Streamlined onboarding for first-time users.
 * 
 * Design Philosophy (Hell's Living Room):
 * - Get users playing in <30 seconds
 * - Focus on ONE core mechanic: long-press to draw
 * - Make skipping obvious and guilt-free
 * - Progressive disclosure (teach advanced features contextually later)
 * - NEON-SOAKED dark-first aesthetic
 * 
 * Flow: Welcome (5s) → Core Gesture (15s) → Add Players (20s) → Ready (10s)
 * Total: 4 steps, ~50 seconds
 * 
 * @ai_prompt Onboarding uses HELLDECK neon styling with glow effects
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: (players: List<Player>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var currentStep by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val totalSteps = 4
    var swipeOffset by remember { mutableStateOf(0f) }
    var onboardingPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HelldeckColors.background,
                        HelldeckColors.surfacePrimary,
                    ),
                ),
            )
            .pointerInput(currentStep) {
                detectDragGestures(
                    onDragEnd = {
                        if (swipeOffset.absoluteValue > 100f) {
                            if (swipeOffset < 0 && currentStep < totalSteps - 1) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                currentStep++
                            } else if (swipeOffset > 0 && currentStep > 0) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                currentStep--
                            }
                        }
                        swipeOffset = 0f
                    },
                    onDrag = { _, dragAmount ->
                        swipeOffset += dragAmount.x
                    },
                )
            },
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
                0 -> WelcomeStep(
                    reducedMotion = reducedMotion,
                    onNext = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        currentStep = 1
                    },
                )
                1 -> DrawCardDemo(
                    reducedMotion = reducedMotion,
                    onComplete = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        currentStep = 2
                    },
                )
                2 -> AddPlayersStep(
                    reducedMotion = reducedMotion,
                    players = onboardingPlayers,
                    onPlayersChanged = { onboardingPlayers = it },
                    onNext = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        currentStep = 3
                    },
                )
                3 -> ReadyToPlayStep(
                    reducedMotion = reducedMotion,
                    playerCount = onboardingPlayers.size,
                    onComplete = { onComplete(onboardingPlayers) },
                )
            }
        }

        // Top bar with progress and skip - HELLDECK styled
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(HelldeckSpacing.Medium.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Neon progress indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HelldeckColors.surfaceElevated),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((currentStep + 1).toFloat() / totalSteps)
                        .clip(RoundedCornerShape(3.dp))
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

            Spacer(Modifier.width(HelldeckSpacing.Medium.dp))

            // Skip button with HELLDECK styling
            TextButton(
                onClick = { onComplete(onboardingPlayers) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HelldeckColors.colorMuted,
                ),
            ) {
                Text(
                    text = "Skip →",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Step indicators (dots) with neon glow - bottom center
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
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
}

@Composable
private fun WelcomeStep(
    reducedMotion: Boolean,
    onNext: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }
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

        Column(
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FeatureHighlight("🎯", "${GameMetadata.getAllGames().size} unique games", HelldeckColors.colorSecondary)
            FeatureHighlight("📱", "One phone, 3-16 players", HelldeckColors.colorAccentCool)
            FeatureHighlight("🧠", "AI adapts to your humor", HelldeckColors.colorAccentWarm)
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // HELLDECK styled CTA button with glow
        OnboardingButton(
            text = "🔥 Get Started",
            reducedMotion = reducedMotion,
            onClick = onNext,
        )
    }
}

@Composable
private fun DrawCardDemo(
    reducedMotion: Boolean,
    onComplete: () -> Unit,
) {
    var showHint by remember { mutableStateOf(true) }
    var pulseScale by remember { mutableStateOf(1f) }
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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
            text = "Long-press anywhere to draw cards",
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

        // HELLDECK styled demo card with neon glow
        Card(
            modifier = Modifier
                .size(260.dp)
                .scale(cardScale)
                .shadow(
                    elevation = if (isPressed) 4.dp else 16.dp,
                    shape = RoundedCornerShape(HelldeckRadius.Large),
                    spotColor = HelldeckColors.colorPrimary.copy(alpha = 0.5f),
                    ambientColor = HelldeckColors.colorPrimary.copy(alpha = 0.3f),
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showHint = false
                            isPressed = true
                            haptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                            )
                            coroutineScope.launch {
                                delay(300)
                                onComplete()
                            }
                        },
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                    )
                },
            shape = RoundedCornerShape(HelldeckRadius.Large),
            colors = CardDefaults.cardColors(
                containerColor = HelldeckColors.surfaceElevated,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HelldeckColors.surfaceElevated,
                                HelldeckColors.surfacePrimary,
                            ),
                        ),
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HelldeckColors.colorPrimary.copy(alpha = 0.6f),
                                HelldeckColors.colorSecondary.copy(alpha = 0.4f),
                            ),
                        ),
                        shape = RoundedCornerShape(HelldeckRadius.Large),
                    ),
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
                            text = "Long-press this card",
                            style = MaterialTheme.typography.bodyLarge,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
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

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Text(
            text = "💡 That's it! This is how you'll play the game",
            style = MaterialTheme.typography.bodyMedium,
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun AddPlayersStep(
    reducedMotion: Boolean,
    players: List<Player>,
    onPlayersChanged: (List<Player>) -> Unit,
    onNext: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

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
        label = "players_scale",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HelldeckSpacing.ExtraLarge.dp),
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Spacer(Modifier.height(HelldeckSpacing.Large.dp)) }
        
        // Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                Text(
                    text = "👥",
                    fontSize = (64 * scale).sp,
                    modifier = Modifier.scale(scale),
                )

                Text(
                    text = "Add Your Crew",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = HelldeckColors.colorPrimary,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Need 3-16 players for the best experience",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = HelldeckColors.colorMuted,
                )
            }
        }

        // Player count indicator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HelldeckRadius.Large),
                colors = CardDefaults.cardColors(
                    containerColor = if (players.size >= 3) {
                        HelldeckColors.colorSecondary.copy(alpha = 0.2f)
                    } else {
                        HelldeckColors.surfaceElevated
                    },
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HelldeckSpacing.Large.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${players.size} Players Added",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (players.size >= 3) HelldeckColors.colorSecondary else HelldeckColors.colorMuted,
                    )
                    if (players.size < 3) {
                        Text(
                            text = "Add ${3 - players.size} more to continue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HelldeckColors.colorMuted,
                        )
                    }
                }
            }
        }

        // Add player button
        item {
            OnboardingButton(
                text = "➕ Add Player",
                reducedMotion = reducedMotion,
                onClick = { showAddDialog = true },
            )
        }

        // Show added players header
        if (players.isNotEmpty()) {
            item {
                Text(
                    text = "Your Crew",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HelldeckSpacing.Medium.dp),
                )
            }
        }
        
        // Player list
        items(players.size) { index ->
            val player = players[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = HelldeckColors.surfaceElevated,
                ),
                shape = RoundedCornerShape(HelldeckRadius.Medium),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HelldeckSpacing.Medium.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = player.avatar, fontSize = 32.sp)
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(
                        onClick = {
                            onPlayersChanged(players.filter { it.id != player.id })
                        },
                    ) {
                        Text("✕", fontSize = 20.sp, color = HelldeckColors.Red)
                    }
                }
            }
        }

        // Continue button
        item {
            Spacer(Modifier.height(HelldeckSpacing.Medium.dp))
        }
        
        item {
            OnboardingButton(
                text = if (players.size >= 3) "Continue →" else "Skip for Now",
                reducedMotion = reducedMotion,
                accentColor = if (players.size >= 3) HelldeckColors.colorSecondary else HelldeckColors.colorMuted,
                onClick = onNext,
            )
        }
        
        item { Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp)) }
    }

    // Add player dialog
    if (showAddDialog) {
        AddPlayerDialog(
            existingPlayers = players,
            onDismiss = { showAddDialog = false },
            onPlayerCreated = { name, emoji ->
                val newPlayer = Player(
                    id = com.helldeck.utils.ValidationUtils.generateUniquePlayerId(players),
                    name = name,
                    avatar = emoji,
                )
                onPlayersChanged(players + newPlayer)
            },
        )
    }
}

@Composable
private fun ReadyToPlayStep(
    reducedMotion: Boolean,
    playerCount: Int,
    onComplete: () -> Unit,
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HelldeckSpacing.ExtraLarge.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            label = "ready_scale",
        )

        Text(
            text = "🎉",
            fontSize = (80 * scale).sp,
            modifier = Modifier.scale(scale),
        )

        Spacer(Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Ready to Play!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = HelldeckColors.colorSecondary,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = if (playerCount > 0) {
                "You've added $playerCount players. Let's go!"
            } else {
                "You can add players anytime from the menu"
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = HelldeckColors.colorMuted,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Tips card with HELLDECK styling
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HelldeckColors.colorSecondary.copy(alpha = 0.3f),
                            HelldeckColors.colorAccentCool.copy(alpha = 0.2f),
                        ),
                    ),
                    shape = RoundedCornerShape(HelldeckRadius.Large),
                ),
            shape = RoundedCornerShape(HelldeckRadius.Large),
            colors = CardDefaults.cardColors(
                containerColor = HelldeckColors.surfaceElevated.copy(alpha = 0.8f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(HelldeckSpacing.Large.dp),
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                QuickTip("🎯", "Spice level in settings", HelldeckColors.colorSecondary)
                QuickTip("🧠", "Game learns from votes", HelldeckColors.colorAccentCool)
                QuickTip("↩️", "Two-finger tap to undo", HelldeckColors.colorAccentWarm)
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Final CTA button with HELLDECK styling
        OnboardingButton(
            text = "🎉 Let's Play!",
            reducedMotion = reducedMotion,
            accentColor = HelldeckColors.colorSecondary,
            onClick = onComplete,
        )
    }
}

/**
 * HELLDECK styled CTA button with glow and spring physics
 */
@Composable
private fun OnboardingButton(
    text: String,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = HelldeckColors.colorPrimary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "button_scale",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                spotColor = accentColor.copy(alpha = 0.5f),
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = HelldeckColors.background,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FeatureHighlight(emoji: String, text: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = accentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun QuickTip(emoji: String, text: String, accentColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = accentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}
