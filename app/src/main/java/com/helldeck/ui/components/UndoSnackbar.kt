package com.helldeck.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Undo snackbar that appears for 3 seconds after rating a card
 */
@Composable
fun UndoSnackbarHost(
    undoState: UndoState?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = undoState != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        undoState?.let { state ->
            UndoSnackbarContent(
                message = state.message,
                timeLeftMs = state.timeLeftMs,
                onUndo = onUndo
            )
        }
    }
}

@Composable
private fun UndoSnackbarContent(
    message: String,
    timeLeftMs: Long,
    onUndo: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // Undo button with countdown
            Button(
                onClick = onUndo,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "UNDO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Progress indicator
    LinearProgressIndicator(
        progress = (timeLeftMs / 3000f).coerceIn(0f, 1f),
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.Transparent
    )
    }
}

/**
 * State for undo snackbar
 */
data class UndoState(
    val message: String,
    val timeLeftMs: Long,
    val action: () -> Unit
)

/**
 * Helper to manage undo state with auto-dismiss
 */
@Composable
fun rememberUndoState(): UndoSnackbarState {
    return remember { UndoSnackbarState() }
}

class UndoSnackbarState {
    var currentState by mutableStateOf<UndoState?>(null)
        private set

    fun show(message: String, action: () -> Unit) {
        currentState = UndoState(message, 3000, action)
    }

    fun dismiss() {
        currentState = null
    }

    fun undo() {
        currentState?.action?.invoke()
        dismiss()
    }

    @Composable
    fun AutoDismiss() {
        val state = currentState
        LaunchedEffect(state) {
            if (state != null) {
                delay(3000)
                if (currentState == state) {
                    dismiss()
                }
            }
        }
    }
}
