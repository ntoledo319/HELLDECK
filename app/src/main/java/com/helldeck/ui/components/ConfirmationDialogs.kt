package com.helldeck.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import com.helldeck.ui.HelldeckColors

/**
 * Reusable confirmation dialogs for destructive actions.
 * 
 * Ensures users don't accidentally delete data or perform
 * irreversible operations without explicit confirmation.
 * 
 * @ai_prompt Confirmation dialogs prevent accidental data loss
 */

/**
 * Generic confirmation dialog for destructive actions.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(if (isDestructive) "⚠️" else "❓", fontSize = 48.sp) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (isDestructive) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HelldeckColors.Red,
                    ),
                ) {
                    Text(confirmText)
                }
            } else {
                GlowButton(
                    text = confirmText,
                    onClick = onConfirm,
                )
            }
        },
        dismissButton = {
            OutlineButton(
                text = cancelText,
                onClick = onDismiss,
            )
        },
    )
}

/**
 * Delete player confirmation.
 */
@Composable
fun DeletePlayerDialog(
    playerName: String,
    playerAvatar: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = "Delete Player?",
        message = "Are you sure you want to delete $playerAvatar $playerName?\n\nThis will remove all their stats, points, and game history. This action cannot be undone.",
        confirmText = "Delete",
        cancelText = "Keep Player",
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Delete all players confirmation.
 */
@Composable
fun DeleteAllPlayersDialog(
    playerCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = "Delete All Players?",
        message = "Are you sure you want to delete all $playerCount players?\n\nThis will remove all stats, points, and game history for everyone. This action cannot be undone.",
        confirmText = "Delete All",
        cancelText = "Cancel",
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Reset settings confirmation.
 */
@Composable
fun ResetSettingsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = "Reset to Defaults?",
        message = "This will reset all settings to their default values.\n\nYour players and game history will not be affected.",
        confirmText = "Reset",
        cancelText = "Cancel",
        isDestructive = false,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Delete crew brain confirmation.
 */
@Composable
fun DeleteCrewBrainDialog(
    brainName: String,
    brainEmoji: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = "Delete Crew Brain?",
        message = "Are you sure you want to delete $brainEmoji $brainName?\n\nThis will remove all players, stats, and game history for this crew brain. This action cannot be undone.",
        confirmText = "Delete",
        cancelText = "Keep",
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Exit game confirmation.
 */
@Composable
fun ExitGameDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = "End Game?",
        message = "Are you sure you want to end the current game?\n\nProgress will not be saved.",
        confirmText = "End Game",
        cancelText = "Keep Playing",
        isDestructive = false,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Clear favorites confirmation.
 */
@Composable
fun ClearFavoritesDialog(
    favoriteCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = "Clear All Favorites?",
        message = "Are you sure you want to delete all $favoriteCount favorite cards?\n\nThis action cannot be undone.",
        confirmText = "Clear All",
        cancelText = "Cancel",
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
