package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.theme.HelldeckSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern, interactive onboarding tutorial for first-time users.
 *
 * Features:
 * - Smooth animations and transitions
 * - Interactive demos with haptic feedback
 * - Clear visual hierarchy
 * - Progress indicators
 * - Skip option
 *
 * Total steps: 5 (Welcome, Draw Cards, Spice Control, Game Selection, Ready)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val totalSteps = 5

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                ),
            ),
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
                    onNext = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        currentStep = 1
                    },
                )
                1 -> DrawCardDemo(
                    onComplete = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        currentStep = 2
                    },
                )
                2 -> SpiceControlDemo(
                    onNext = {
                        haptic.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove,
                        )
                        currentStep = 3
                    },
                )
                3 -> GameSelectionDemo(
                    onNext = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        currentStep = 4
                    },
                )
                4 -> ReadyToPlayStep(onComplete = onComplete)
            }
        }

        // Top progress bar
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Step indicators (dots)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = HelldeckSpacing.ExtraLarge.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
        ) {
            repeat(totalSteps) { index ->
                val isActive = index == currentStep
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "step_indicator_scale",
                )

                Box(
                    modifier = Modifier
                        .size(if (isActive) 12.dp else 8.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index <= currentStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
            }
        }

        // Skip button (top right)
        TextButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(HelldeckSpacing.Medium.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
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
        // Animated logo/emoji
        val emojiScale by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow,
            ),
            label = "emoji_scale",
        )

        Text(
            text = "🎮",
            fontSize = (80 * emojiScale).sp,
            modifier = Modifier.scale(emojiScale),
        )

        Spacer(Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
        )

        Spacer(Modifier.height(HelldeckSpacing.Small.dp))

        Text(
            text = "HELLDECK",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "The party game that learns\nwhat your crew finds funny",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Feature highlights
        Column(
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FeatureHighlight("🎯", "${GameMetadata.getAllGames().size} Unique Games")
            FeatureHighlight("🧠", "AI Learns Your Humor")
            FeatureHighlight("📱", "Single Phone, Everyone Plays")
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                "Let's Go!",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DrawCardDemo(onComplete: () -> Unit) {
    var showHint by remember { mutableStateOf(true) }
    var pulseScale by remember { mutableStateOf(1f) }
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showHint) {
        if (showHint) {
            while (true) {
                animate(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                ) { value, _ -> pulseScale = value }
                animate(
                    initialValue = 1.05f,
                    targetValue = 1f,
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                ) { value, _ -> pulseScale = value }
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
            text = "Drawing Cards",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "Long-press anywhere to draw a new card",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Interactive demo area
        val cardScale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else pulseScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "card_scale",
        )

        Card(
            modifier = Modifier
                .size(240.dp)
                .scale(cardScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showHint = false
                            isPressed = true
                            coroutineScope.launch {
                                delay(200)
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
                defaultElevation = if (isPressed) 2.dp else 8.dp,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (showHint) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                    ) {
                        Text(
                            text = "👆",
                            fontSize = 48.sp,
                        )
                        Text(
                            text = "Long-press here",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 48.sp,
                            color = HelldeckColors.Green,
                        )
                        Text(
                            text = "Perfect!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "💡 Tip: Cards appear instantly when you long-press",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpiceControlDemo(onNext: () -> Unit) {
    var sliderValue by remember { mutableStateOf(3f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HelldeckSpacing.ExtraLarge.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Spice Control",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "Adjust how wild the cards get",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Spice slider with visual feedback
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HelldeckRadius.Large),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(HelldeckSpacing.Large.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val spiceLevel = sliderValue.toInt()
                val spiceData = when (spiceLevel) {
                    1 -> SpiceData("😇", "Wholesome", "Family-friendly fun", HelldeckColors.Green)
                    2 -> SpiceData("😄", "Playful", "Light roasting", HelldeckColors.Orange)
                    3 -> SpiceData("😈", "Edgy", "Spicy content", MaterialTheme.colorScheme.primary)
                    4 -> SpiceData("🔥", "Wild", "No holds barred", HelldeckColors.Orange)
                    else -> SpiceData("💀", "Chaos", "Maximum chaos", HelldeckColors.Red)
                }

                Text(
                    text = spiceData.emoji,
                    fontSize = 64.sp,
                )

                Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

                Text(
                    text = spiceData.label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = spiceData.color,
                )

                Spacer(Modifier.height(HelldeckSpacing.Small.dp))

                Text(
                    text = spiceData.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(HelldeckSpacing.Large.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = spiceData.color,
                        activeTrackColor = spiceData.color,
                    ),
                )
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                "Continue",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private data class SpiceData(
    val emoji: String,
    val label: String,
    val description: String,
    val color: Color,
)

private data class GameSample(
    val emoji: String,
    val title: String,
    val description: String,
)

@Composable
private fun GameSelectionDemo(onNext: () -> Unit) {
    val sampleGames = remember {
        listOf(
            GameSample("🔥", "Roast Consensus", "Vote who fits best"),
            GameSample("☠️", "Poison Pitch", "Sell impossible choices"),
            GameSample("🚩", "Red Flag Rally", "Defend terrible dates"),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HelldeckSpacing.ExtraLarge.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Choose Your Game",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "${GameMetadata.getAllGames().size} different party games\nto keep things fresh",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        // Sample game cards
        Column(
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            sampleGames.forEach { game ->
                GameSampleCard(
                    emoji = game.emoji,
                    title = game.title,
                    description = game.description,
                )
            }
        }

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                "Continue",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
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
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "Add 3-16 players and start playing.\nThe game learns what your crew finds funny!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(HelldeckSpacing.ExtraLarge.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                "Start Playing!",
                style = MaterialTheme.typography.titleMedium,
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
private fun GameSampleCard(emoji: String, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
