package com.helldeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.helldeck.billing.PurchaseManager
import com.helldeck.engine.DetailedGameRules
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.gameIconFor

/**
 * Modal sheet for direct game selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePickerSheet(
    onGameSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var showRulesFor by remember { mutableStateOf<String?>(null) }
    var showUpgradeFor by remember { mutableStateOf<String?>(null) }
    val isPremiumUnlocked by PurchaseManager.isPremiumUnlocked.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Choose Your Game",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Upgrade banner (only show if not premium)
            if (!isPremiumUnlocked && !PurchaseManager.isUnlockAllMode()) {
                UpgradePromptBanner(
                    onUpgradeClick = {
                        // Show upgrade modal for first locked game
                        val firstLockedGame = GameMetadata.getAllGameIds().firstOrNull { 
                            !PurchaseManager.isGameUnlocked(it) 
                        }
                        firstLockedGame?.let { showUpgradeFor = it }
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            // Game grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(GameMetadata.getAllGameIds()) { gameId ->
                    val isUnlocked = PurchaseManager.isGameUnlocked(gameId)
                    GameCard(
                        gameId = gameId,
                        isLocked = !isUnlocked,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isUnlocked) {
                                onGameSelected(gameId)
                            } else {
                                showUpgradeFor = gameId
                            }
                        },
                        onInfoClick = {
                            showRulesFor = gameId
                        }
                    )
                }
            }
        }
    }
    
    // Rules preview dialog
    showRulesFor?.let { gameId ->
        GameRulesDialog(
            gameId = gameId,
            onDismiss = { showRulesFor = null },
            onStartGame = {
                showRulesFor = null
                val isUnlocked = PurchaseManager.isGameUnlocked(gameId)
                if (isUnlocked) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onGameSelected(gameId)
                } else {
                    showUpgradeFor = gameId
                }
            }
        )
    }

    // Upgrade modal for locked games
    showUpgradeFor?.let { gameId ->
        UpgradeModal(
            gameId = gameId,
            onDismiss = { showUpgradeFor = null },
            onPurchaseComplete = {
                showUpgradeFor = null
                // Optionally auto-select the game after purchase
            },
        )
    }
}

@Composable
private fun GameCard(
    gameId: String,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    val metadata = GameMetadata.getGameMetadata(gameId)
    val gameName = metadata?.title ?: gameId
    val gameDescription = metadata?.description ?: ""
    val gameEmoji = gameIconFor(gameId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) 
                HelldeckColors.surfaceElevated.copy(alpha = 0.8f)
            else 
                MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = if (isLocked) {
            BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        HelldeckColors.colorPrimary.copy(alpha = 0.4f),
                        HelldeckColors.colorPrimary.copy(alpha = 0.15f),
                    ),
                ),
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLocked) 4.dp else 2.dp,
            pressedElevation = 6.dp,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main card content - clickable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Emoji with lock overlay for locked games
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = gameEmoji,
                        fontSize = 36.sp,
                        modifier = Modifier.alpha(if (isLocked) 0.4f else 1f),
                    )
                    if (isLocked) {
                        // Lock icon with subtle glow
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = HelldeckColors.surfacePrimary.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(50),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                tint = HelldeckColors.colorPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Game name
                Text(
                    text = gameName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    color = if (isLocked) HelldeckColors.colorMuted else MaterialTheme.colorScheme.onSurface,
                )

                // Description
                Text(
                    text = gameDescription,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = if (isLocked) 
                        HelldeckColors.colorMuted.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Premium badge - top left for locked games
            if (isLocked) {
                Surface(
                    shape = RoundedCornerShape(bottomEnd = HelldeckRadius.Medium),
                    color = HelldeckColors.colorPrimary,
                    shadowElevation = 4.dp,
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Text(
                        text = "✨ PRO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = HelldeckColors.background,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
            
            // Info button overlay - top right
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "View rules",
                    tint = if (isLocked) HelldeckColors.colorMuted else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameRulesDialog(
    gameId: String,
    onDismiss: () -> Unit,
    onStartGame: () -> Unit
) {
    val metadata = GameMetadata.getGameMetadata(gameId)
    val detailedRules = DetailedGameRules.getRulesForGame(gameId)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metadata?.title ?: "Rules",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    metadata?.let {
                        Text(
                            text = it.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    if (detailedRules != null) {
                        RulesPreviewSection("📖 How to Play", detailedRules.howToPlay)
                        RulesPreviewSection("⚙️ Mechanics", detailedRules.mechanics)
                        RulesPreviewSection("🏆 Scoring", detailedRules.scoring)
                        RulesPreviewSection("💡 Tips", detailedRules.tips)
                    } else {
                        Text(
                            text = "Tap 'Start Game' to see rules during gameplay!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = onStartGame,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Game")
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesPreviewSection(title: String, items: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        items.take(3).forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                )
            }
        }
        if (items.size > 3) {
            Text(
                text = "...and ${items.size - 3} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
