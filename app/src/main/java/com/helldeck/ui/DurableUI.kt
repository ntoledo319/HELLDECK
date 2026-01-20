package com.helldeck.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.FlashIntensity
import com.helldeck.engine.HapticsTorch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced Durable UI components for HellDeck
 * Optimized for worn/cracked screens with game-specific components
 */

/**
 * Enhanced giant button with animation and better feedback
 * ACCESSIBILITY: Supports keyboard/D-pad navigation with focus indicators
 */
@Composable
fun GiantButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
    )

    Button(
        onClick = {
            if (!loading) {
                isPressed = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                TorchFeedback.confirm(context)
                onClick()
            }
        },
        modifier = modifier
            .height(DurableHeights.Button)
            .fillMaxWidth()
            .scale(scale)
            .semantics {
                // Keyboard navigation support
                focused = enabled && !loading
            },
        enabled = enabled && !loading,
        colors = colors,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            content()
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

/**
 * Game-specific option buttons based on GameOptions
 */
@Composable
fun GameOptionButtons(
    options: GameOptions,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (options) {
        is GameOptions.AB -> ABChoiceButtons(
            optionA = options.optionA,
            optionB = options.optionB,
            onChoice = onOptionSelected,
            modifier = modifier,
        )

        is GameOptions.PlayerVote -> PlayerVoteGrid(
            players = options.players,
            onVote = onOptionSelected,
            modifier = modifier,
        )

        is GameOptions.SmashPass -> SmashPassButtons(
            onChoice = onOptionSelected,
            modifier = modifier,
        )

        is GameOptions.TrueFalse -> TrueFalseButtons(
            onChoice = onOptionSelected,
            modifier = modifier,
        )

        is GameOptions.ReplyTone -> ReplyToneButtons(
            tones = options.tones,
            onChoice = onOptionSelected,
            modifier = modifier,
        )

        is GameOptions.Taboo -> TabooDisplay(
            word = options.word,
            forbidden = options.forbidden,
            modifier = modifier,
        )

        is GameOptions.Scatter -> ScatterDisplay(
            category = options.category,
            letter = options.letter,
            modifier = modifier,
        )

        is GameOptions.OddOneOut -> OddOneOutButtons(
            items = options.items,
            onChoice = onOptionSelected,
            modifier = modifier,
        )

        is GameOptions.PredictVote -> PredictVoteButtons(
            optionA = options.optionA,
            optionB = options.optionB,
            onChoice = onOptionSelected,
            modifier = modifier,
        )

        else -> {}
    }
}

/**
 * A/B choice buttons for games like Poison Pitch
 */
@Composable
fun ABChoiceButtons(
    optionA: String,
    optionB: String,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GiantButton(
            onClick = {
                selected = "A"
                onChoice("A")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "A") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    optionA,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        GiantButton(
            onClick = {
                selected = "B"
                onChoice("B")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "B") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("B", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    optionB,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Smash/Pass buttons for Red Flag Rally
 */
@Composable
fun SmashPassButtons(
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GiantButton(
            onClick = {
                selected = "SMASH"
                onChoice("SMASH")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "SMASH") {
                    Color(0xFF4CAF50)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Text("💚 SMASH", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        GiantButton(
            onClick = {
                selected = "PASS"
                onChoice("PASS")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "PASS") {
                    Color(0xFFF44336)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Text("❌ PASS", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * True/False buttons for Confession or Cap
 */
@Composable
fun TrueFalseButtons(
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GiantButton(
            onClick = {
                selected = "TRUE"
                onChoice("TRUE")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "TRUE") {
                    Color(0xFF2196F3)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Text("✓ TRUTH", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        GiantButton(
            onClick = {
                selected = "FALSE"
                onChoice("FALSE")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "FALSE") {
                    Color(0xFFFF9800)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Text("✗ CAP", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Reply tone selection for Text Thread Trap
 */
@Composable
fun ReplyToneButtons(
    tones: List<String>,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<String?>(null) }
    val toneEmojis = mapOf(
        "Deadpan" to "😐",
        "Feral" to "😈",
        "Chaotic" to "🤪",
        "Wholesome" to "🥰",
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tones) { tone ->
            GiantButton(
                onClick = {
                    selected = tone
                    onChoice(tone)
                },
                modifier = Modifier.width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == tone) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        toneEmojis[tone] ?: "😊",
                        fontSize = 24.sp,
                    )
                    Text(
                        tone,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Taboo word display
 */
@Composable
fun TabooDisplay(
    word: String,
    forbidden: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = word.uppercase(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "FORBIDDEN:",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Red,
            )

            forbidden.forEach { forbiddenWord ->
                Text(
                    text = "❌ $forbiddenWord",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                    ),
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * Scatterblast category/letter display
 */
@Composable
fun ScatterDisplay(
    category: String,
    letter: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("CATEGORY", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    category,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Card(
            modifier = Modifier.weight(0.5f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Odd one out selection buttons
 */
@Composable
fun OddOneOutButtons(
    items: List<String>,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            GiantButton(
                onClick = {
                    selected = item
                    onChoice(item)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == item) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Text(
                    item,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Predict vote buttons for Majority Report
 */
@Composable
fun PredictVoteButtons(
    optionA: String,
    optionB: String,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var prediction by remember { mutableStateOf<String?>(null) }
    var locked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "MAKE YOUR PREDICTION",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GiantButton(
                onClick = {
                    if (!locked) {
                        prediction = "A"
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !locked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prediction == "A") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        optionA,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            GiantButton(
                onClick = {
                    if (!locked) {
                        prediction = "B"
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !locked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prediction == "B") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("B", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        optionB,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (prediction != null && !locked) {
            GiantButton(
                onClick = {
                    locked = true
                    onChoice(prediction!!)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                ),
            ) {
                Text("🔒 LOCK PREDICTION", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Player vote grid for Roast Consensus
 */
@Composable
fun PlayerVoteGrid(
    players: List<String>,
    onVote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<String?>(null) }
    val playerEmojis = listOf("😎", "🤓", "😈", "🤡", "👻", "🦄", "🐸", "🔥")

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(players.chunked(3)) { rowPlayers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowPlayers.forEachIndexed { index, player ->
                    PlayerVoteButton(
                        playerName = player,
                        emoji = playerEmojis.getOrElse(players.indexOf(player)) { "👤" },
                        isSelected = selected == player,
                        onClick = {
                            selected = player
                            onVote(player)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Fill empty spaces
                repeat(3 - rowPlayers.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Individual player vote button
 */
@Composable
fun PlayerVoteButton(
    playerName: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.75f),
    )

    Card(
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(emoji, fontSize = 28.sp)
            Text(
                playerName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Enhanced feedback buttons with animations
 */
@Composable
fun FeedbackButtons(
    onLol: () -> Unit,
    onMeh: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
) {
    val context = LocalContext.current
    var lastPressed by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedFeedbackButton(
            emoji = "😂",
            label = if (showLabels) "LOL" else null,
            color = Color(0xFF4CAF50),
            isPressed = lastPressed == "LOL",
            onClick = {
                lastPressed = "LOL"
                TorchFeedback.success(context)
                onLol()
            },
            modifier = Modifier.weight(1f),
        )

        AnimatedFeedbackButton(
            emoji = "😐",
            label = if (showLabels) "MEH" else null,
            color = Color(0xFFFF9800),
            isPressed = lastPressed == "MEH",
            onClick = {
                lastPressed = "MEH"
                TorchFeedback.confirm(context)
                onMeh()
            },
            modifier = Modifier.weight(1f),
        )

        AnimatedFeedbackButton(
            emoji = "🚮",
            label = if (showLabels) "TRASH" else null,
            color = Color(0xFFF44336),
            isPressed = lastPressed == "TRASH",
            onClick = {
                lastPressed = "TRASH"
                TorchFeedback.error(context)
                onTrash()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Animated feedback button
 */
@Composable
fun AnimatedFeedbackButton(
    emoji: String,
    label: String?,
    color: Color,
    isPressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
    )

    GiantButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = emoji,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            label?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Enhanced timer with visual warnings
 */
@Composable
fun GiantTimer(
    timeRemaining: Int,
    totalTime: Int,
    modifier: Modifier = Modifier,
    onTimeUp: () -> Unit = {},
) {
    val progress = timeRemaining.toFloat() / totalTime.toFloat()
    val isLow = progress < 0.3f
    val isCritical = progress < 0.1f

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCritical -> Color(0xFFD32F2F)
            isLow -> Color(0xFFF44336)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300),
    )

    val scale by animateFloatAsState(
        targetValue = if (isCritical) 1.1f else 1f,
        animationSpec = if (isCritical) {
            infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse,
            )
        } else {
            spring()
        },
    )

    LaunchedEffect(timeRemaining) {
        if (timeRemaining == 0) {
            onTimeUp()
        }
    }

    Card(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                color = Color.White.copy(alpha = 0.3f),
                strokeWidth = 8.dp,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeRemaining.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (isCritical) 56.sp else 48.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )

                Text(
                    text = if (timeRemaining == 1) "second" else "seconds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/**
 * Score display with animations
 */
@Composable
fun ScoreDisplay(
    scores: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                "SCORES",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            scores.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
                ScoreRow(
                    rank = index + 1,
                    playerName = entry.key,
                    score = entry.value,
                    isLeader = index == 0,
                )
            }
        }
    }
}

/**
 * Individual score row
 */
@Composable
fun ScoreRow(
    rank: Int,
    playerName: String,
    score: Int,
    isLeader: Boolean,
) {
    val rankEmoji = when (rank) {
        1 -> "👑"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                rankEmoji,
                fontSize = if (isLeader) 24.sp else 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                playerName,
                fontSize = 18.sp,
                fontWeight = if (isLeader) FontWeight.Bold else FontWeight.Medium,
            )
        }

        Text(
            score.toString(),
            fontSize = if (isLeader) 24.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Enhanced torch feedback with patterns
 */
object TorchFeedback {
    private val hapticsTorch = HapticsTorch

    fun confirm(context: android.content.Context) {
        hapticsTorch.flash(
            context = context,
            durationMs = 100,
            intensity = FlashIntensity.QUICK,
        )
    }

    fun success(context: android.content.Context) {
        hapticsTorch.flash(
            context = context,
            durationMs = 150,
            intensity = FlashIntensity.NORMAL,
        )
    }

    fun error(context: android.content.Context) {
        kotlinx.coroutines.GlobalScope.launch {
            repeat(2) {
                hapticsTorch.flash(context, 100, FlashIntensity.QUICK)
                delay(100)
            }
        }
    }

    fun celebration(context: android.content.Context) {
        kotlinx.coroutines.GlobalScope.launch {
            repeat(3) {
                hapticsTorch.flash(context, 50, FlashIntensity.BRIGHT)
                delay(150)
            }
        }
    }
}

/**
 * Constants remain the same
 */
object DurableSpacing {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val Giant = 48.dp
}

object DurableHeights {
    val Button = 72.dp
    val Card = 120.dp
    val Avatar = 120.dp
    val Timer = 200.dp
    val CompactButton = 56.dp
}

/**
 * Game phase indicator with animations
 */
@Composable
fun GamePhaseIndicator(
    currentPhase: String,
    phasesComplete: Int,
    totalPhases: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                currentPhase.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = phasesComplete.toFloat() / totalPhases.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Round $phasesComplete of $totalPhases",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * Heat meter showing room energy
 */
@Composable
fun HeatMeter(
    heatLevel: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
) {
    val heatColor by animateColorAsState(
        targetValue = when {
            heatLevel < 0.3f -> Color(0xFF2196F3) // Cold - Blue
            heatLevel < 0.6f -> Color(0xFFFF9800) // Warm - Orange
            else -> Color(0xFFF44336) // Hot - Red
        },
        animationSpec = tween(500),
    )

    val heatEmoji = when {
        heatLevel < 0.3f -> "🧊"
        heatLevel < 0.6f -> "🔥"
        else -> "🌋"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                heatEmoji,
                fontSize = 32.sp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "ROOM HEAT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${(heatLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = heatColor,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = heatLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = heatColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

/**
 * Streak indicator with fire animation
 */
@Composable
fun StreakIndicator(
    streakCount: Int,
    modifier: Modifier = Modifier,
) {
    if (streakCount > 0) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse,
            ),
        )

        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF6B35),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "🔥",
                    fontSize = 24.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                )

                Text(
                    "$streakCount STREAK!",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                repeat(minOf(streakCount, 3)) {
                    Text("🔥", fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * Quick action floating buttons
 */
@Composable
fun QuickActionButtons(
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
    showSkip: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        FloatingActionButton(
            onClick = onPause,
            containerColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(56.dp),
        ) {
            Text("⏸", fontSize = 24.sp)
        }

        if (showSkip) {
            FloatingActionButton(
                onClick = onSkip,
                containerColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(56.dp),
            ) {
                Text("⏭", fontSize = 24.sp)
            }
        }

        FloatingActionButton(
            onClick = onHelp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp),
        ) {
            Text("❓", fontSize = 24.sp)
        }
    }
}

/**
 * Spice level selector
 */
@Composable
fun SpiceLevelSelector(
    currentLevel: Int,
    maxLevel: Int = 3,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "SPICE LEVEL",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (level in 1..maxLevel) {
                    SpiceLevelButton(
                        level = level,
                        isSelected = currentLevel >= level,
                        onClick = { onLevelChange(level) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                when (currentLevel) {
                    1 -> "Mild - Family Friendly"
                    2 -> "Medium - Some Edge"
                    3 -> "Hot - Savage Mode"
                    else -> "Select spice level"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Individual spice level button
 */
@Composable
fun SpiceLevelButton(
    level: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val emoji = when (level) {
        1 -> "🌶"
        2 -> "🌶🌶"
        3 -> "🌶🌶🌶"
        else -> ""
    }

    val color = when (level) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface,
        ),
        border = if (!isSelected) BorderStroke(2.dp, color) else null,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                emoji,
                fontSize = 20.sp,
            )
        }
    }
}

/**
 * Game icon display
 */
@Composable
fun GameIcon(
    gameId: String,
    size: Int = 48,
    modifier: Modifier = Modifier,
) {
    val icon = when (gameId) {
        "ROAST_CONSENSUS" -> "🎯" // 1. Roast Consensus
        "CONFESSION_OR_CAP" -> "🤥" // 2. Confession or Cap
        "POISON_PITCH" -> "💀" // 3. Poison Pitch
        "FILL_IN_FINISHER" -> "✍️" // 4. Fill-In Finisher
        "RED_FLAG_RALLY" -> "🚩" // 5. Red Flag Rally
        "HOT_SEAT_IMPOSTER" -> "🎭" // 6. Hot Seat Imposter
        "TEXT_THREAD_TRAP" -> "📱" // 7. Text Thread Trap
        "TABOO_TIMER" -> "⏱️" // 8. Taboo Timer
        "THE_UNIFYING_THEORY" -> "📐" // 9. The Unifying Theory
        "TITLE_FIGHT" -> "🥊" // 10. Title Fight
        "ALIBI_DROP" -> "🕵️" // 11. Alibi Drop
        "REALITY_CHECK" -> "🪞" // 12. Reality Check
        "SCATTERBLAST" -> "💣" // 13. Scatterblast
        "OVER_UNDER" -> "📉" // 14. Over / Under
        else -> "🎮"
    }

    Text(
        text = icon,
        fontSize = size.sp,
        modifier = modifier,
    )
}

/**
 * Loading overlay for async operations
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    message: String = "Loading...",
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center,
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * Enhanced durable theme with better contrast
 */
@Composable
fun DurableTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFFF6B35),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFFCC5628),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFF00D4AA),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF00A688),
            onSecondaryContainer = Color.White,
            tertiary = Color(0xFFFFD700),
            onTertiary = Color.Black,
            surface = Color(0xFF1C1B1F),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2D2C31),
            onSurfaceVariant = Color(0xFFE0E0E0),
            background = Color(0xFF141218),
            onBackground = Color.White,
            error = Color(0xFFFF5252),
            onError = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF9C4150),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDADE),
            onPrimaryContainer = Color(0xFF3F0011),
            secondary = Color(0xFF006A6A),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF9FF1F0),
            onSecondaryContainer = Color(0xFF002020),
            tertiary = Color(0xFF7E5700),
            onTertiary = Color.White,
            surface = Color.White,
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFF49454E),
            background = Color(0xFFFFFBFF),
            onBackground = Color(0xFF1C1B1F),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            displayMedium = MaterialTheme.typography.displayMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
            displaySmall = MaterialTheme.typography.displaySmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            ),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            labelLarge = MaterialTheme.typography.labelLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
        ),
        content = content,
    )
}
