package com.helldeck.ui.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.helldeck.AppCtx
import com.helldeck.content.data.ContentRepository
import com.helldeck.engine.GameFeedback
import com.helldeck.engine.HapticEvent
import com.helldeck.ui.CardFace
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.theme.HelldeckSpacing
import com.helldeck.engine.Config
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repo = remember { ContentRepository(AppCtx.ctx) }
    val hapticsEnabled = Config.hapticsEnabled
    val roundState = vm.roundState

    // Auto-advance countdown (5 seconds)
    var secondsRemaining by remember { mutableStateOf(5) }
    var isAutoAdvancing by remember { mutableStateOf(true) }

    LaunchedEffect(roundState?.filledCard?.id ?: vm.currentCard?.id) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_END, useHaptics = hapticsEnabled)
    }

    // Auto-advance timer
    LaunchedEffect(isAutoAdvancing) {
        if (isAutoAdvancing) {
            while (secondsRemaining > 0) {
                kotlinx.coroutines.delay(1000)
                secondsRemaining--
            }
            if (secondsRemaining == 0) {
                vm.commitFeedbackAndNext()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FEEDBACK", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Black) },
                navigationIcon = { TextButton(onClick = { vm.goBack() }) { Text("Back") } },
                actions = { TextButton(onClick = { vm.goHome() }) { Text("Home") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardFace(
                title = roundState?.filledCard?.text ?: (vm.currentCard?.text ?: "Rate that card"),
                subtitle = "Everyone taps once: LOL / MEH / TRASH",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(HelldeckSpacing.Medium.dp)
            )

            // Optional rating buttons (non-blocking)
            Text(
                text = "Rate this card (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            RatingRail(
                onLol = {
                    vm.feedbackLol()
                    GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                },
                onMeh = {
                    vm.feedbackMeh()
                    GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                },
                onTrash = {
                    vm.feedbackTrash()
                    GameFeedback.triggerFeedback(context, HapticEvent.VOTE_CONFIRM, useHaptics = hapticsEnabled)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HelldeckSpacing.Large.dp)
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            // Auto-advance countdown with skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HelldeckSpacing.Large.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Skip button (instant advance)
                OutlinedButton(
                    onClick = { scope.launch { vm.commitFeedbackAndNext() } },
                    modifier = Modifier
                        .weight(1f)
                        .height(HelldeckHeights.Button.dp)
                ) {
                    Text(text = "SKIP", style = MaterialTheme.typography.labelLarge)
                }

                // Auto-advance countdown button
                Button(
                    onClick = { scope.launch { vm.commitFeedbackAndNext() } },
                    modifier = Modifier
                        .weight(1f)
                        .height(HelldeckHeights.Button.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HelldeckColors.colorSecondary
                    )
                ) {
                    Text(
                        text = if (secondsRemaining > 0) "NEXT ($secondsRemaining)" else "NEXT",
                        style = MaterialTheme.typography.labelLarge,
                        color = HelldeckColors.Black
                    )
                }
            }

            // Small hint text
            Text(
                text = "Auto-advancing in ${secondsRemaining}s • Tap SKIP to go faster",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun RatingRail(
    onLol: () -> Unit,
    onMeh: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RatingButton(
            emoji = "😂",
            label = "LOL",
            color = HelldeckColors.Lol,
            onClick = onLol,
            modifier = Modifier.weight(1f)
        )
        RatingButton(
            emoji = "😐",
            label = "MEH",
            color = HelldeckColors.Meh,
            onClick = onMeh,
            modifier = Modifier.weight(1f)
        )
        RatingButton(
            emoji = "🚮",
            label = "TRASH",
            color = HelldeckColors.Trash,
            onClick = onTrash,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RatingButton(
    emoji: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = emoji, style = MaterialTheme.typography.headlineLarge)
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = HelldeckColors.Black)
        }
    }
}
