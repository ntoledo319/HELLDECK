package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Interactive onboarding tutorial for first-time users.
 *
 * Teaches:
 * 1. Long-press to draw cards
 * 2. Two-finger tap to undo
 * 3. Spice slider control
 * 4. Game selection
 *
 * Total time: ~40 seconds (with skip button)
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(0) }
    var canProceed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() with
                        slideOutHorizontally { -it } + fadeOut()
            }
        ) { step ->
            when (step) {
                0 -> WelcomeStep(onNext = { currentStep = 1 })
                1 -> LongPressDemo(
                    onComplete = {
                        canProceed = true
                        currentStep = 2
                    }
                )
                2 -> TwoFingerTapDemo(
                    onComplete = {
                        canProceed = true
                        currentStep = 3
                    }
                )
                3 -> SpiceSliderDemo(onNext = { currentStep = 4 })
                4 -> GamePickerDemo(onNext = { onComplete() })
            }
        }

        // Progress indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == currentStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }

        // Skip button
        TextButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Skip Tutorial")
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "HELLDECK",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "The party game that learns\nwhat your crew finds funny",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Let's Go!")
        }
    }

    LaunchedEffect(Unit) {
        delay(3000) // Auto-advance after 3 seconds
        onNext()
    }
}

@Composable
private fun LongPressDemo(onComplete: () -> Unit) {
    var showHint by remember { mutableStateOf(true) }
    var pulseAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            animate(1f, 0.3f, animationSpec = tween(1000)) { value, _ ->
                pulseAlpha = value
            }
            animate(0.3f, 1f, animationSpec = tween(1000)) { value, _ ->
                pulseAlpha = value
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Drawing Cards",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Long-press anywhere to draw a card",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        // Interactive demo area
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showHint = false
                            onComplete()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (showHint) {
                Text(
                    text = "👆\nLong-press here",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "✓ Perfect!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TwoFingerTapDemo(onComplete: () -> Unit) {
    var showHint by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Undo Action",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Tap with two fingers to undo",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        // Interactive demo area
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Accept any tap as "two-finger" for demo purposes
                        showHint = false
                        onComplete()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (showHint) {
                Text(
                    text = "✌️\nTap with two fingers",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Text(
                    text = "✓ Got it!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun SpiceSliderDemo(onNext: () -> Unit) {
    var sliderValue by remember { mutableStateOf(3f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Spice Control",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Adjust how wild the cards get",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        // Spice slider demo
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = when (sliderValue.toInt()) {
                    1 -> "😇 Wholesome"
                    2 -> "😄 Playful"
                    3 -> "😈 Edgy"
                    4 -> "🔥 Wild"
                    else -> "💀 Chaos"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (sliderValue.toInt()) {
                    1 -> "Family-friendly fun"
                    2 -> "Light roasting"
                    3 -> "Spicy content"
                    4 -> "No holds barred"
                    else -> "Maximum chaos"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue")
        }
    }

    LaunchedEffect(Unit) {
        delay(5000) // Auto-advance after 5 seconds
        onNext()
    }
}

@Composable
private fun GamePickerDemo(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Your Game",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "14 different party games\nto keep things fresh",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Sample game cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameSampleCard("🔥 Roast Consensus", "Vote who's most likely")
            GameSampleCard("☠️ Poison Pitch", "Sell impossible choices")
            GameSampleCard("🚩 Red Flag Rally", "Defend terrible dates")
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Start Playing!")
        }
    }
}

@Composable
private fun GameSampleCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
