package com.helldeck.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.content.model.Player
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.hdFieldColors
import com.helldeck.ui.theme.HelldeckSpacing
import com.helldeck.utils.ValidationUtils

/**
 * Centralized player creation/editing dialog.
 * Replaces duplicated player creation logic in PlayersScene, RollcallScene, and SettingsScene.
 * 
 * Features:
 * - Name validation with duplicate checking
 * - Emoji selection with quick picks and full picker
 * - Duplicate emoji warning
 * - Edit mode for existing players
 * - Consistent HELLDECK neon styling
 * 
 * @ai_prompt Use AddPlayerDialog for all player creation/editing
 * @context_boundary Single source of truth for player creation UX
 */

private val QUICK_PICK_EMOJIS = listOf(
    "😎", "🦊", "🐸", "🐼", "🦄", "🐙", "🐯", "🦁", "🐵", "🐧", "🦖", "🐺",
    "👑", "🔥", "💀", "👻", "🤖", "🦸", "🧙", "🧛", "🧚", "🥷", "🎭", "🎪",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    existingPlayers: List<Player>,
    onDismiss: () -> Unit,
    onPlayerCreated: (name: String, emoji: String) -> Unit,
    modifier: Modifier = Modifier,
    editingPlayer: Player? = null,
) {
    var name by remember { mutableStateOf(editingPlayer?.name ?: "") }
    var emoji by remember { mutableStateOf(editingPlayer?.avatar ?: QUICK_PICK_EMOJIS.random()) }
    var showFullEmojiPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmojiWarning by remember { mutableStateOf(false) }
    
    // Validate on emoji change
    LaunchedEffect(emoji) {
        showEmojiWarning = ValidationUtils.isEmojiDuplicate(
            emoji = emoji,
            existingPlayers = existingPlayers,
            excludePlayerId = editingPlayer?.id,
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = if (editingPlayer != null) "Edit Player" else "Add Player",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Emoji selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select Emoji",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.colorPrimary,
                    )
                    
                    // Quick pick emojis
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                    items(QUICK_PICK_EMOJIS) { quickEmoji ->
                        val isSelected = quickEmoji == emoji
                        Surface(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { emoji = quickEmoji },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                HelldeckColors.colorPrimary.copy(alpha = 0.2f)
                            } else {
                                HelldeckColors.surfaceElevated
                            },
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(
                                    3.dp,
                                    HelldeckColors.colorPrimary,
                                )
                            } else {
                                null
                            },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                            ) {
                                Text(
                                    text = quickEmoji,
                                    fontSize = 28.sp,
                                )
                            }
                        }
                    }
                    }
                    
                    // More emojis button
                    OutlineButton(
                        text = "More Emojis...",
                        onClick = { showFullEmojiPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = "🔍",
                    )
                    
                    // Emoji duplicate warning
                    if (showEmojiWarning) {
                        WarningBanner(
                            message = "This emoji is already used by another player",
                            icon = "⚠️",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                
                // Name input section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Player Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.colorPrimary,
                    )
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            errorMessage = null
                        },
                        placeholder = { Text("e.g., Jay, Pip, Mo") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = hdFieldColors(),
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let {
                            {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                    )
                }
                
                // Preview section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = HelldeckColors.colorMuted,
                    )
                    
                    NeonCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = HelldeckColors.colorSecondary,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp),
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 40.sp,
                            )
                            Text(
                                text = name.ifBlank { "(enter name)" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (name.isBlank()) {
                                    HelldeckColors.colorMuted
                                } else {
                                    HelldeckColors.colorOnDark
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlineButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                
                GlowButton(
                    text = if (editingPlayer != null) "Save" else "Add",
                    onClick = {
                    // Validate
                    val nameValidation = ValidationUtils.validatePlayerName(
                        name = name,
                        existingPlayers = existingPlayers,
                        excludePlayerId = editingPlayer?.id,
                    )
                    
                    if (!nameValidation.isValid) {
                        errorMessage = nameValidation.errorMessage
                        return@GlowButton
                    }
                    
                    val emojiValidation = ValidationUtils.validatePlayerEmoji(emoji)
                    if (!emojiValidation.isValid) {
                        errorMessage = emojiValidation.errorMessage
                        return@GlowButton
                    }
                    
                        // Success
                        onPlayerCreated(name.trim(), emoji.trim())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    icon = if (editingPlayer != null) "💾" else "➕",
                )
            }
        },
    )
    
    // Full emoji picker
    if (showFullEmojiPicker) {
        com.helldeck.ui.EmojiPicker(
            show = true,
            onDismiss = { showFullEmojiPicker = false },
            onPick = { picked ->
                emoji = picked
                showFullEmojiPicker = false
            },
        )
    }
}

/**
 * Quick add player button that opens the dialog.
 */
@Composable
fun QuickAddPlayerButton(
    existingPlayers: List<Player>,
    onPlayerAdded: (name: String, emoji: String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Add Player",
    icon: String = "➕",
) {
    var showDialog by remember { mutableStateOf(false) }
    
    GlowButton(
        text = label,
        onClick = { showDialog = true },
        modifier = modifier,
        icon = icon,
    )
    
    if (showDialog) {
        AddPlayerDialog(
            existingPlayers = existingPlayers,
            onDismiss = { showDialog = false },
            onPlayerCreated = onPlayerAdded,
        )
    }
}
