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
 * Flow: Welcome (5s) → Core Gesture (15s) → Ready (10s)
 * Total: 3 steps, ~30 seconds
 * 
 * @ai_prompt Onboarding uses HELLDECK neon styling with glow effects
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    var currentStep by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val totalSteps = 3
    var swipeOffset by remember { mutableStateOf(0f) }

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
                2 -> ReadyToPlayStep(
                    reducedMotion = reducedMotion,
                    onComplete = onComplete,
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
                onClick = onComplete,
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
    onComplete: () -> Unit,
) {
    var showHint by remember { mutableStateOf(true) }
    var pulseScale by remember { mutableStateOf(1f) }
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(showHint) {
        if (showHint) {
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
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "Long-press anywhere to draw cards",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        val cardScale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else pulseScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "card_scale",
        )

        Card(
            modifier = Modifier
                .size(260.dp)
                .scale(cardScale)
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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isPressed) 4.dp else 12.dp,
            ),
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
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Long-press this card",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
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
                            color = HelldeckColors.Green,
                        )
                        Text(
                            text = "Perfect!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Text(
            text = "💡 That's it! This is how you'll play the game",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun ReadyToPlayStep(onComplete: () -> Unit) {
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
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
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
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "Add 3-16 players and jump in",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HelldeckRadius.Large),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(HelldeckSpacing.Large.dp),
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            ) {
                QuickTip("🎯", "Spice level in settings")
                QuickTip("🧠", "Game learns from votes")
                QuickTip("↩️", "Two-finger tap to undo")
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                "Let's Play!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FeatureHighlight(emoji: String, text: String) {
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
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun QuickTip(emoji: String, text: String) {
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
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
