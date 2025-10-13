package com.helldeck.ui


import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.ButtonDefaults
import com.helldeck.data.PlayerEntity

/**
 * Big touch zones for easy interaction
 * Three large zones for left/center/right interactions
 */
@Composable
fun BigZones(
    modifier: Modifier = Modifier,
    onLeft: () -> Unit = {},
    onCenter: () -> Unit = {},
    onRight: () -> Unit = {},
    onLong: () -> Unit = {},
    leftColor: Color = HelldeckColors.MediumGray,
    centerColor: Color = HelldeckColors.DarkGray,
    rightColor: Color = HelldeckColors.MediumGray
) {
    Row(modifier.fillMaxSize()) {
        // Left zone
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(leftColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onLeft() },
                        onLongPress = { onLong() }
                    )
                }
            ) {
            // Optional: Add visual feedback for interaction
        }

        // Center zone
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(centerColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onCenter() },
                        onLongPress = { onLong() }
                    )
                },
        )

        // Right zone
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(rightColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onRight() },
                        onLongPress = { onLong() }
                    )
                },
        )
    }
}

/**
 * Main card face for displaying game content with enhanced visual design
 */
@Composable
fun CardFace(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = HelldeckColors.DarkGray,
    borderColor: Color = HelldeckColors.Yellow,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardModifier = modifier
        .clip(RoundedCornerShape(HelldeckRadius.Large))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.9f),
                    backgroundColor.copy(alpha = 0.8f)
                )
            )
        )
        .border(
            BorderStroke(
                width = if (isPressed) 3.dp else 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.8f),
                        borderColor,
                        borderColor.copy(alpha = 0.6f)
                    )
                )
            ),
            RoundedCornerShape(HelldeckRadius.Large)
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
            } else {
                Modifier
            }
        )
        .padding(16.dp)

    ElevatedCard(
        modifier = cardModifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isPressed) 12.dp else 8.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.1f),
                            backgroundColor.copy(alpha = 0.05f)
                        ),
                        radius = 800f
                    )
                )
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 44.sp,
                        lineHeight = 50.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    color = HelldeckColors.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.animateContentSize(animationSpec = spring())
                )

                subtitle?.let { sub ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        ),
                        color = HelldeckColors.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Animated card face with entrance animation
 */
@Composable
fun AnimatedCardFace(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    onClick: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(HelldeckAnimations.Normal)
        ) + androidx.compose.animation.slideInVertically(
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it / 2 }
        ),
        modifier = modifier
    ) {
        CardFace(
            title = title,
            subtitle = subtitle,
            onClick = onClick
        )
    }
}

/**
 * Feedback strip for collecting player reactions
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun FeedbackStrip(
    modifier: Modifier = Modifier,
    onLol: () -> Unit = {},
    onMeh: () -> Unit = {},
    onTrash: () -> Unit = {},
    onComment: (String, Set<String>) -> Unit = { _, _ -> },
    showComments: Boolean = false,
    commentText: String = "",
    onCommentTextChange: (String) -> Unit = {},
    selectedTags: Set<String> = emptySet(),
    availableTags: List<String> = listOf("tame", "repeat", "inside", "long", "harsh"),
    onTagToggle: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Feedback buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeedbackButton(
                text = "😂 BANGER",
                color = HelldeckColors.Lol,
                onClick = onLol
            )

            FeedbackButton(
                text = "😐 MEH",
                color = HelldeckColors.Meh,
                onClick = onMeh
            )

            FeedbackButton(
                text = "🚮 TRASH",
                color = HelldeckColors.Trash,
                onClick = onTrash
            )

            FeedbackButton(
                text = "✍️ WHY",
                color = HelldeckColors.Orange,
                onClick = { /* Toggle comment section */ }
            )
        }

        // Comment section
        if (showComments) {
            Spacer(modifier = Modifier.height(4.dp))

            // Tag selection
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                availableTags.forEach { tag ->
                    val isSelected = selectedTags.contains(tag)
                    Text(
                        text = "${if (isSelected) "■" else "□"} $tag",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) HelldeckColors.Yellow else HelldeckColors.LightGray,
                        modifier = Modifier
                            .clickable { onTagToggle(tag) }
                            .padding(horizontal = HelldeckSpacing.Tiny.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Comment text field
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "optional note…",
                        color = HelldeckColors.LightGray
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HelldeckColors.Yellow,
                    unfocusedBorderColor = HelldeckColors.LightGray,
                    focusedTextColor = HelldeckColors.White,
                    unfocusedTextColor = HelldeckColors.White,
                    cursorColor = HelldeckColors.Yellow
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Save button
            Button(
                onClick = { onComment(commentText, selectedTags) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HelldeckColors.Green
                )
            ) {
                Text(text = "Save Feedback")
            }
        }
    }
}

/**
 * Individual feedback button with enhanced interactions
 */
@Composable
fun FeedbackButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .width(100.dp)
            .height(48.dp)
            .scale(scale),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isPressed) 2.dp else 4.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.animateContentSize()
        )
    }
}

/**
 * Game timer display
 */
@Composable
fun GameTimer(
    timeRemainingMs: Int,
    totalTimeMs: Int,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true
) {
    val progress = timeRemainingMs.toFloat() / totalTimeMs.toFloat()
    val isWarning = progress < 0.3
    val isCritical = progress < 0.1

    val timerColor = when {
        isCritical -> HelldeckColors.Error
        isWarning -> HelldeckColors.Warning
        else -> HelldeckColors.Yellow
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer text
        Text(
            text = formatTime(timeRemainingMs),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            ),
            color = timerColor,
            textAlign = TextAlign.Center
        )

        // Progress indicator
        if (showProgress) {
            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = timerColor,
                trackColor = timerColor.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Player avatar display
 */
@Composable
fun PlayerAvatar(
    name: String,
    avatar: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showName: Boolean = true,
    isActive: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val avatarModifier = modifier
        .size(size)
        .clip(RoundedCornerShape(HelldeckRadius.Medium))
        .background(
            if (isActive) HelldeckColors.MediumGray
            else HelldeckColors.LightGray
        )
        .then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            }
        )
        .padding(4.dp)

    Column(
        modifier = avatarModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = avatar,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 32.sp
            )
        )

        if (showName) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = HelldeckColors.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Vote button for player selection with enhanced interactions
 */
@Composable
fun VoteButton(
    playerName: String,
    playerAvatar: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderColor = if (isSelected) {
        HelldeckColors.Yellow
    } else {
        HelldeckColors.MediumGray
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "vote_button_scale"
    )

    val glowIntensity by animateFloatAsState(
        targetValue = if (isSelected) 0.6f else if (isPressed) 0.3f else 0.1f,
        animationSpec = tween(200),
        label = "glow_intensity"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .width(120.dp)
            .height(72.dp)
            .scale(scale)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(HelldeckRadius.Medium),
                        ambientColor = borderColor.copy(alpha = glowIntensity),
                        spotColor = borderColor.copy(alpha = glowIntensity * 0.5f)
                    )
                } else {
                    Modifier
                }
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        border = BorderStroke(
            width = if (isSelected) 3.dp else if (isPressed) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.8f),
                    borderColor,
                    borderColor.copy(alpha = 0.6f)
                )
            )
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) {
                borderColor.copy(alpha = 0.2f)
            } else if (isPressed) {
                borderColor.copy(alpha = 0.1f)
            } else {
                Color.Transparent
            },
            contentColor = HelldeckColors.White
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = playerAvatar,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 24.sp
                ),
                modifier = Modifier.animateContentSize()
            )
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Pulsing animation for attention-grabbing elements
 */
@Composable
fun PulsingEffect(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier.scale(scale), contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Gradient background for special elements
 */
@Composable
fun GradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(colors),
                shape = RoundedCornerShape(HelldeckRadius.Large)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Loading spinner for async operations with enhanced animation
 */
@Composable
fun HelldeckLoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = HelldeckColors.Yellow,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size)
                .rotate(rotation)
                .scale(pulse),
            color = color,
            strokeWidth = (4.dp * pulse).coerceAtMost(6.dp)
        )

        // Inner glow effect
        CircularProgressIndicator(
            modifier = Modifier
                .size(size * 0.7f)
                .rotate(-rotation * 0.5f),
            color = color.copy(alpha = 0.3f * pulse),
            strokeWidth = 2.dp
        )
    }
}

/**
 * Skeleton loading card for content placeholders
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    showAvatar: Boolean = true,
    showTitle: Boolean = true,
    showSubtitle: Boolean = false
) {
    val shimmer = rememberInfiniteTransition()

    val shimmerTranslate by shimmer.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = HelldeckColors.MediumGray.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAvatar) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(HelldeckRadius.Medium))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    HelldeckColors.LightGray.copy(alpha = 0.3f),
                                    HelldeckColors.LightGray.copy(alpha = 0.7f),
                                    HelldeckColors.LightGray.copy(alpha = 0.3f)
                                ),
                                start = androidx.compose.ui.geometry.Offset(
                                    shimmerTranslate * 200f,
                                    0f
                                ),
                                end = androidx.compose.ui.geometry.Offset(
                                    shimmerTranslate * 200f + 200f,
                                    100f
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (showTitle) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(20.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        HelldeckColors.LightGray.copy(alpha = 0.3f),
                                        HelldeckColors.LightGray.copy(alpha = 0.7f),
                                        HelldeckColors.LightGray.copy(alpha = 0.3f)
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(
                                        shimmerTranslate * 300f,
                                        0f
                                    ),
                                    end = androidx.compose.ui.geometry.Offset(
                                        shimmerTranslate * 300f + 300f,
                                        50f
                                    )
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (showSubtitle) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(16.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            HelldeckColors.LightGray.copy(alpha = 0.2f),
                                            HelldeckColors.LightGray.copy(alpha = 0.5f),
                                            HelldeckColors.LightGray.copy(alpha = 0.2f)
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(
                                            shimmerTranslate * 200f,
                                            0f
                                        ),
                                        end = androidx.compose.ui.geometry.Offset(
                                            shimmerTranslate * 200f + 200f,
                                            30f
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Score display component
 */
@Composable
fun ScoreDisplay(
    score: Int,
    label: String = "Points",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = score.toString(),
            style = style.copy(
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.Yellow
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = HelldeckColors.LightGray
        )
    }
}

/**
 * Podium section for top 3 players
 */
@Composable
fun PodiumSection(
    topPlayers: List<PlayerEntity>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // Second place
        if (topPlayers.size > 1) {
            PodiumCard(
                player = topPlayers[1],
                position = 2,
                height = 120.dp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // First place (center, tallest)
        if (topPlayers.isNotEmpty()) {
            PodiumCard(
                player = topPlayers[0],
                position = 1,
                height = 160.dp,
                isWinner = true,
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Third place
        if (topPlayers.size > 2) {
            PodiumCard(
                player = topPlayers[2],
                position = 3,
                height = 100.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual podium card for top players
 */
@Composable
fun PodiumCard(
    player: PlayerEntity,
    position: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    isWinner: Boolean = false
) {
    val podiumColors = listOf(
        HelldeckColors.Yellow, // Gold for 1st
        HelldeckColors.MediumGray,   // Silver for 2nd
        HelldeckColors.Orange  // Bronze for 3rd
    )

    val positionColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFC0C0C0), // Silver
        Color(0xFFCD7F32)  // Bronze
    )

    val cardColor = if (isWinner) {
        podiumColors[0]
    } else {
        podiumColors.getOrElse(position - 1) { HelldeckColors.MediumGray }
    }

    ElevatedCard(
        modifier = modifier
            .height(height)
            .fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isWinner) 12.dp else 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardColor.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Position indicator
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Player avatar
            Text(
                text = player.avatar,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 28.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Player name
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Score
            ScoreDisplay(
                score = player.sessionPoints,
                label = "pts",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }
    }
}

/**
 * Player score card for remaining players
 */
@Composable
fun PlayerScoreCard(
    player: PlayerEntity,
    position: Int,
    modifier: Modifier = Modifier,
    isTopThree: Boolean = false
) {
    val positionColors = listOf(
        HelldeckColors.Yellow.copy(alpha = 0.8f),
        HelldeckColors.MediumGray.copy(alpha = 0.8f),
        HelldeckColors.Orange.copy(alpha = 0.8f)
    )

    val backgroundColor = when {
        isTopThree -> positionColors.getOrElse(position - 1) { HelldeckColors.MediumGray }
        position <= 3 -> positionColors.getOrElse(position - 1) { HelldeckColors.MediumGray }
        else -> HelldeckColors.MediumGray
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = backgroundColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Position badge
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (position <= 3) positionColors[position - 1] else HelldeckColors.LightGray,
                    modifier = Modifier
                        .background(
                            backgroundColor.copy(alpha = 0.2f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Player info
                Column {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = player.avatar,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HelldeckColors.LightGray
                    )
                }
            }

            // Score
            ScoreDisplay(
                score = player.sessionPoints,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * Helper function to format time in milliseconds to MM:SS
 */
@Composable
private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Celebration animation for scoring events
 */
@Composable
fun ScoreCelebration(
    score: Int,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition()

    val particles = remember { List(12) { index ->
        Particle(
            id = index,
            initialDelay = index * 100L,
            angle = (360f / 12) * index,
            distance = 100f + (index % 3) * 50f
        )
    } }

    val scale by animateFloatAsState(
        targetValue = 1.2f,
        animationSpec = tween(300, easing = EaseOutBack),
        finishedListener = null
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center
    ) {
        // Main score display
        Text(
            text = "+$score",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                brush = Brush.linearGradient(
                    colors = listOf(
                        HelldeckColors.Yellow,
                        HelldeckColors.Orange,
                        HelldeckColors.Yellow.copy(alpha = 0.8f)
                    )
                )
            ),
            modifier = Modifier.scale(scale),
            textAlign = TextAlign.Center
        )

        // Particle effects would go here if needed
        // particles.forEach { particle ->
        //     ParticleEffect(particle = particle)
        // }

        // Background glow
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HelldeckColors.Yellow.copy(alpha = 0.3f),
                            HelldeckColors.Yellow.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 100f
                    )
                )
        )
    }
}

/**
 * Individual particle for celebration effect
 */
data class Particle(
    val id: Int,
    val initialDelay: Long,
    val angle: Float,
    val distance: Float
)

/**
 * Particle animation effect
 */
@Composable
fun ParticleEffect(
    particle: Particle,
    color: Color = HelldeckColors.Yellow
) {
    val infiniteTransition = rememberInfiniteTransition()

    val animatedDistance by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = particle.distance,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                delayMillis = particle.initialDelay.toInt(),
                easing = EaseOutCubic
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                delayMillis = particle.initialDelay.toInt() + 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val x = animatedDistance * kotlin.math.cos(Math.toRadians(particle.angle.toDouble())).toFloat()
    val y = animatedDistance * kotlin.math.sin(Math.toRadians(particle.angle.toDouble())).toFloat()

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(x = x.toInt().dp, y = y.toInt().dp)
            .background(
                color = color.copy(alpha = alpha),
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .shadow(
                elevation = 4.dp,
                spotColor = color.copy(alpha = alpha * 0.5f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

/**
 * Background pattern types
 */
enum class BackgroundPattern {
    DOTS, GRID, HEXAGON, CIRCUIT
}

/**
 * Background pattern for visual interest
 */
@Composable
fun HelldeckBackgroundPattern(
    modifier: Modifier = Modifier,
    pattern: BackgroundPattern = BackgroundPattern.DOTS,
    opacity: Float = 0.05f
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (pattern) {
            BackgroundPattern.DOTS -> DotsPattern(opacity = opacity)
            BackgroundPattern.GRID -> GridPattern(opacity = opacity)
            BackgroundPattern.HEXAGON -> HexagonPattern(opacity = opacity)
            BackgroundPattern.CIRCUIT -> CircuitPattern(opacity = opacity)
        }
    }
}

/**
 * Dot pattern background
 */
@Composable
private fun DotsPattern(
    opacity: Float,
    dotSize: Dp = 2.dp,
    spacing: Dp = 32.dp
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = HelldeckColors.Yellow.copy(alpha = opacity)
            style = androidx.compose.ui.graphics.PaintingStyle.Fill
        }

        val canvasWidth = size.width
        val canvasHeight = size.height

        var y = 0f
        while (y < canvasHeight) {
            var x = 0f
            while (x < canvasWidth) {
                drawCircle(
                    center = Offset(x, y),
                    radius = dotSize.toPx() / 2,
                    color = Color.Black
                )
                x += spacing.toPx()
            }
            y += spacing.toPx()
        }
    }
}

/**
 * Grid pattern background
 */
@Composable
private fun GridPattern(
    opacity: Float,
    lineWidth: Dp = 1.dp,
    spacing: Dp = 24.dp
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = HelldeckColors.LightGray.copy(alpha = opacity)
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeWidth = lineWidth.toPx()
        }

        val canvasWidth = size.width
        val canvasHeight = size.height

        // Vertical lines
        var x = 0f
        while (x < canvasWidth) {
            drawLine(
                start = Offset(x, 0f),
                end = Offset(x, canvasHeight),
                color = Color.Gray
            )
            x += spacing.toPx()
        }

        // Horizontal lines
        var y = 0f
        while (y < canvasHeight) {
            drawLine(
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                color = Color.Gray
            )
            y += spacing.toPx()
        }
    }
}

/**
 * Hexagon pattern background
 */
@Composable
private fun HexagonPattern(
    opacity: Float,
    size: Dp = 20.dp
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = HelldeckColors.Orange.copy(alpha = opacity)
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeWidth = 1.dp.toPx()
        }

        val hexRadius = size.toPx() / 2
        val canvasWidth = size.toPx()
        val canvasHeight = size.toPx()

        val hexHeight = hexRadius * sqrt(3f)
        val hexWidth = hexRadius * 1.5f

        var row = 0
        var y = hexHeight

        while (y < canvasHeight + hexHeight) {
            var x = if (row % 2 == 0) hexWidth else hexWidth * 1.5f

            while (x < canvasWidth + hexWidth) {
                drawHexagon(
                    center = Offset(x, y),
                    radius = hexRadius,
                    
                )
                x += hexWidth * 2
            }

            y += hexHeight * 1.5f
            row++
        }
    }
}

/**
 * Circuit pattern background
 */
@Composable
private fun CircuitPattern(
    opacity: Float,
    complexity: Int = 8
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = HelldeckColors.Green.copy(alpha = opacity)
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeWidth = 1.dp.toPx()
        }

        val canvasWidth = size.width
        val canvasHeight = size.height
        val segments = complexity * 4

        // Draw circuit-like lines
        repeat(segments) { i ->
            val startX = (i * canvasWidth / segments).coerceIn(0f, canvasWidth)
            val endX = ((i + 1) * canvasWidth / segments).coerceIn(0f, canvasWidth)
            val y = (i * canvasHeight / segments).coerceIn(0f, canvasHeight)

            // Horizontal lines
            drawLine(
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                color = Color.Cyan
            )

            // Vertical lines with occasional branches
            if (i % 3 == 0) {
                drawLine(
                    start = Offset(startX, 0f),
                    end = Offset(startX, canvasHeight),
                    color = Color.Cyan
                )

                // Add some connection nodes
                drawCircle(
                    center = Offset(startX, y),
                    radius = 3.dp.toPx(),
                    color = HelldeckColors.Yellow.copy(alpha = opacity * 2)
                )
            }
        }
    }
}

/**
 * Draw hexagon shape
 */
private fun DrawScope.drawHexagon(
    center: Offset,
    radius: Float
) {
    val path = Path().apply {
        for (i in 0..5) {
            val angle = (Math.PI / 3 * i).toFloat()
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)

            if (i == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }

    drawPath(path = path, color = Color.Black)
}

/**
 * Flow row for tag layout
 */
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}