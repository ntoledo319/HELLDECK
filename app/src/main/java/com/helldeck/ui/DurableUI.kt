package com.helldeck.ui


import androidx.compose.ui.platform.LocalContext
import com.helldeck.engine.FlashIntensity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

/**
 * Durable UI components optimized for worn/cracked screens
 * Giant touch targets, high contrast, zero-glance design
 */

/**
 * Giant button for cracked screens - minimum 72dp touch target
 */
@Composable
fun GiantButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .height(72.dp) // Giant touch target
            .fillMaxWidth(),
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
    ) {
        content()
    }
}

/**
 * Giant icon button for cracked screens
 */
@Composable
fun GiantIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
    contentDescription: String? = null
) {
    val haptic = LocalHapticFeedback.current

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.size(72.dp), // Giant touch target
        enabled = enabled
    ) {
        icon()
    }
}

/**
 * High contrast card for zero-glance readability
 */
@Composable
fun DurableCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        colors = colors,
        shape = RoundedCornerShape(16.dp),
        content = content
    )
}

/**
 * Giant text display for worn screens
 */
@Composable
fun GiantText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 28.sp, // Large for worn screens
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign,
        maxLines = maxLines,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    )
}

/**
 * High contrast feedback buttons (😂/😐/🚮)
 */
@Composable
fun FeedbackButtons(
    onLol: () -> Unit,
    onMeh: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GiantButton(
            onClick = onLol,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50), // Green for LOL
                contentColor = Color.White
            )
        ) {
            Text(
                text = "😂",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        GiantButton(
            onClick = onMeh,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800), // Orange for MEH
                contentColor = Color.White
            )
        ) {
            Text(
                text = "😐",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        GiantButton(
            onClick = onTrash,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336), // Red for TRASH
                contentColor = Color.White
            )
        ) {
            Text(
                text = "🚮",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Player avatar button with giant touch target
 */
@Composable
fun PlayerAvatarButton(
    playerName: String,
    playerAvatar: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .size(120.dp) // Giant touch target
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = playerAvatar,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 36.sp // Very large for worn screens
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = playerName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * High contrast vote buttons for large groups
 */
@Composable
fun VoteButton(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(100.dp) // Giant touch target
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Instant feedback overlay for tap confirmation
 */
@Composable
fun InstantFeedbackOverlay(
    show: Boolean,
    modifier: Modifier = Modifier
) {
    if (show) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Zero-glance game card with high contrast
 */
@Composable
fun GameCard(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    DurableCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (onClick != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * High contrast theme for worn screens
 */
@Composable
fun DurableTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFFF6B35), // High contrast orange
            onPrimary = Color.Black,
            secondary = Color(0xFF00D4AA), // High contrast green
            onSecondary = Color.Black,
            surface = Color(0xFF1C1B1F), // Very dark for contrast
            onSurface = Color.White,
            background = Color(0xFF141218), // Maximum contrast
            onBackground = Color.White,
            error = Color(0xFFFFB4AB),
            onError = Color.Black
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF9C4150), // Dark red for contrast
            onPrimary = Color.White,
            secondary = Color(0xFF006A6A), // Dark teal
            onSecondary = Color.White,
            surface = Color.White,
            onSurface = Color(0xFF1C1B1F), // Very dark text
            background = Color(0xFFFFFBFF),
            onBackground = Color(0xFF1C1B1F)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(
                fontSize = 36.sp, // Larger for worn screens
                fontWeight = FontWeight.Bold
            ),
            displayMedium = MaterialTheme.typography.displayMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            ),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp // Larger for readability
            )
        ),
        content = content
    )
}

/**
 * Instant haptic feedback system
 */
object InstantFeedback {

    fun tap() {
        // Haptic feedback would be handled by caller with LocalHapticFeedback.current
    }

    fun success() {
        // Haptic feedback would be handled by caller with LocalHapticFeedback.current
    }

    fun error() {
        // Haptic feedback would be handled by caller with LocalHapticFeedback.current
    }

    fun phaseChange() {
        // Haptic feedback would be handled by caller with LocalHapticFeedback.current
    }
}

/**
 * Torch feedback system for instant visual confirmation
 */
object TorchFeedback {

    private var torchManager: com.helldeck.engine.HapticsTorch? = null

    fun initialize(context: android.content.Context) {
        torchManager = com.helldeck.engine.HapticsTorch
    }

    fun confirm(context: android.content.Context) {
        torchManager?.flash(
            context = context,
            durationMs = 100,
            intensity = com.helldeck.engine.FlashIntensity.QUICK
        )
    }

    fun success(context: android.content.Context) {
        torchManager?.flash(
            context = context,
            durationMs = 150,
            intensity = com.helldeck.engine.FlashIntensity.NORMAL
        )
    }

    fun error(context: android.content.Context) {
        kotlinx.coroutines.GlobalScope.launch {
            torchManager?.flash(context, 100, com.helldeck.engine.FlashIntensity.QUICK)
            delay(100)
            torchManager?.flash(context, 100, com.helldeck.engine.FlashIntensity.QUICK)
        }
    }
}

/**
 * Performance optimized layout for large player counts
 */
@Composable
fun ScalablePlayerGrid(
    players: List<com.helldeck.data.PlayerEntity>,
    onPlayerClick: (com.helldeck.data.PlayerEntity) -> Unit,
    selectedPlayerIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val chunkedPlayers = when {
        players.size <= 6 -> listOf(players) // Single row for small groups
        players.size <= 12 -> players.chunked(6) // Two rows for medium groups
        players.size <= 18 -> players.chunked(6) // Three rows for large groups
        else -> players.chunked(7) // Four+ rows for very large groups
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunkedPlayers.forEach { rowPlayers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPlayers.forEach { player ->
                    PlayerAvatarButton(
                        playerName = player.name,
                        playerAvatar = player.avatar,
                        isSelected = player.id in selectedPlayerIds,
                        onClick = { onPlayerClick(player) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Add empty spaces for uneven rows
                repeat(7 - rowPlayers.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Zero-glance timer display for worn screens
 */
@Composable
fun GiantTimer(
    timeRemaining: Int,
    totalTime: Int,
    modifier: Modifier = Modifier
) {
    val progress = timeRemaining.toFloat() / totalTime.toFloat()
    val isLow = progress < 0.3f // Warning at 30%

    Card(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeRemaining.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Text(
                    text = "seconds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Durable spacing constants for worn screens
 */
object DurableSpacing {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val Giant = 48.dp // Extra large for worn screens
}

/**
 * Durable heights for consistent touch targets
 */
object DurableHeights {
    val Button = 72.dp // Giant touch target
    val Card = 120.dp
    val Avatar = 120.dp
    val Timer = 200.dp
}