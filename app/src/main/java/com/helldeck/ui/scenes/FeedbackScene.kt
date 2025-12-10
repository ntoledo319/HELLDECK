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
import com.helldeck.ui.FeedbackStrip
import com.helldeck.ui.HelldeckVm
import com.helldeck.ui.components.HelldeckColors
import com.helldeck.ui.components.HelldeckSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackScene(vm: HelldeckVm) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repo = remember { ContentRepository(AppCtx.ctx) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(vm.currentCard?.id) {
        GameFeedback.triggerFeedback(context, HapticEvent.ROUND_END, useHaptics = hapticsEnabled)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback") },
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
                title = "Rate that card",
                subtitle = "😂 ≥60% = +1 heat bonus • 🚮 ≥60% = −2",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(HelldeckSpacing.Medium.dp)
            )

            FeedbackStrip(
                onLol = { vm.feedbackLol() },
                onMeh = { vm.feedbackMeh() },
                onTrash = { vm.feedbackTrash() },
                onComment = { text, tags -> vm.addComment(text, tags) },
                showComments = true
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            Button(
                onClick = { scope.launch { vm.commitFeedbackAndNext() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = HelldeckSpacing.Large.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Green
                )
            ) {
                Text(text = "Next Round")
            }
        }
    }
}
