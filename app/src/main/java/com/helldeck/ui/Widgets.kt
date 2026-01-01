package com.helldeck.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.content.model.Player
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    rightColor: Color = HelldeckColors.MediumGray,
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
                        onLongPress = { onLong() },
                    )
                },
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
                        onLongPress = { onLong() },
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
                        onLongPress = { onLong() },
                    )
                },
        )
    }
}

/**
 * Auto-resizing text that shrinks font until it fits within maxLines
 */
@Composable
private fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 8,
    maxFontSize: androidx.compose.ui.unit.TextUnit = 44.sp,
    minFontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    step: androidx.compose.ui.unit.TextUnit = 2.sp,
    color: Color = HelldeckColors.White,
    textAlign: TextAlign = TextAlign.Center,
    baseStyle: TextStyle = MaterialTheme.typography.displayMedium.copy(
        lineHeight = 50.sp,
        fontWeight = FontWeight.Bold,
    ),
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var ready by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        style = baseStyle.copy(fontSize = fontSize),
        modifier = modifier.fillMaxWidth(),
        onTextLayout = { result ->
            if (!ready && result.hasVisualOverflow && fontSize > minFontSize) {
                val next = (fontSize.value - step.value).coerceAtLeast(minFontSize.value)
                fontSize = next.sp
            } else {
                ready = true
            }
        },
    )
}

/**
 * Main card face for displaying game content with enhanced visual design
 * 
 * DESIGN PRINCIPLE (HDRealRules.md):
 * - "Low Cognitive Load. High Social Stakes. Maximum Chaos."
 * - Instant readability at arm's length in dim room
 * - Stakes must be immediately clear
 * - Pass the "Drunk Person Test" (3 drinks in, still comprehensible)
 */
@Composable
fun CardFace(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = HelldeckColors.DarkGray,
    borderColor: Color = HelldeckColors.Yellow,
    onClick: (() -> Unit)? = null,
    stakesLabel: String? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else if (isPressed) 0.98f else 1f,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessHigh,
            )
        },
        label = "card_scale",
    )

    val elevation by animateFloatAsState(
        targetValue = if (reducedMotion) 6f else if (isPressed) 4f else 8f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "card_elevation",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                ambientColor = borderColor.copy(alpha = 0.2f),
                spotColor = borderColor.copy(alpha = 0.3f),
            ),
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onClick() }
                    } else {
                        Modifier
                    },
                ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp,
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(HelldeckRadius.Large),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.95f),
                                backgroundColor.copy(alpha = 0.85f),
                                backgroundColor.copy(alpha = 0.9f),
                            ),
                        ),
                    )
                    .border(
                        BorderStroke(
                            width = if (isPressed) 3.dp else 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    borderColor.copy(alpha = 0.9f),
                                    borderColor.copy(alpha = 0.7f),
                                    borderColor,
                                    borderColor.copy(alpha = 0.7f),
                                    borderColor.copy(alpha = 0.9f),
                                ),
                            ),
                        ),
                        RoundedCornerShape(HelldeckRadius.Large),
                    ),
            ) {
                // Radial glow overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    borderColor.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                                radius = 600f,
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Stakes label (What's at risk?) - HDRealRules.md: "Stakes Must Be Clear"
                    stakesLabel?.let { stakes ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = borderColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = stakes,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                ),
                                color = borderColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    AutoResizeText(
                        text = title,
                        maxLines = 10,
                        maxFontSize = 44.sp,
                        minFontSize = 18.sp,
                        step = 2.sp,
                        color = HelldeckColors.White,
                        textAlign = TextAlign.Center,
                        baseStyle = MaterialTheme.typography.displayMedium.copy(
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 4f,
                            ),
                        ),
                        modifier = Modifier.animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ),
                    )

                    subtitle?.let { sub ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.2f),
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = 2f,
                                ),
                            ),
                            color = HelldeckColors.LightGray,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
    onClick: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(Unit) {
        delay((if (reducedMotion) 0 else delayMs).toLong())
        visible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) {
            androidx.compose.animation.fadeIn(animationSpec = tween(HelldeckAnimations.Instant))
        } else {
            androidx.compose.animation.fadeIn(
                animationSpec = tween(HelldeckAnimations.Normal),
            ) + androidx.compose.animation.slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = Spring.StiffnessLow,
                ),
                initialOffsetY = { it / 2 },
            )
        },
        modifier = modifier,
    ) {
        CardFace(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
        )
    }
}

/**
 * Feedback strip for collecting player reactions with enhanced animations
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
    availableTags: List<String> = listOf("tame", "repeat", "inside", "long", "harsh"),
) {
    val reducedMotion = LocalReducedMotion.current
    var lolCount by remember { mutableIntStateOf(0) }
    var mehCount by remember { mutableIntStateOf(0) }
    var trashCount by remember { mutableIntStateOf(0) }
    var showCommentSection by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Feedback buttons with visual counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            EnhancedFeedbackButton(
                text = "😂",
                label = "BANGER",
                color = HelldeckColors.Lol,
                count = lolCount,
                onClick = {
                    lolCount++
                    onLol()
                },
            )

            EnhancedFeedbackButton(
                text = "😐",
                label = "MEH",
                color = HelldeckColors.Meh,
                count = mehCount,
                onClick = {
                    mehCount++
                    onMeh()
                },
            )

            EnhancedFeedbackButton(
                text = "🚮",
                label = "TRASH",
                color = HelldeckColors.Trash,
                count = trashCount,
                onClick = {
                    trashCount++
                    onTrash()
                },
            )

            EnhancedFeedbackButton(
                text = "✍️",
                label = "NOTE",
                color = HelldeckColors.Orange,
                count = null,
                onClick = { showCommentSection = !showCommentSection },
            )
        }

        // Comment section with smooth animation
        androidx.compose.animation.AnimatedVisibility(
            visible = showComments || showCommentSection,
            enter = if (reducedMotion) {
                androidx.compose.animation.fadeIn(animationSpec = tween(HelldeckAnimations.Instant))
            } else {
                androidx.compose.animation.expandVertically(
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ) + androidx.compose.animation.fadeIn()
            },
            exit = if (reducedMotion) {
                androidx.compose.animation.fadeOut(animationSpec = tween(HelldeckAnimations.Instant))
            } else {
                androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tag selection with better visual design
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableTags.forEach { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Surface(
                            color = if (isSelected) HelldeckColors.Yellow.copy(alpha = 0.2f) else Color.Transparent,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) HelldeckColors.Yellow else HelldeckColors.LightGray,
                            ),
                            modifier = Modifier.clickable {
                                selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                            },
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = if (isSelected) HelldeckColors.Yellow else HelldeckColors.LightGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                // Comment text field with improved styling
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Why was this card good/bad? (optional)",
                            color = HelldeckColors.LightGray.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HelldeckColors.Yellow,
                        unfocusedBorderColor = HelldeckColors.LightGray.copy(alpha = 0.5f),
                        focusedTextColor = HelldeckColors.White,
                        unfocusedTextColor = HelldeckColors.White,
                        cursorColor = HelldeckColors.Yellow,
                        focusedContainerColor = HelldeckColors.DarkGray.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                )

                // Save button
                Button(
                    onClick = {
                        onComment(commentText.trim(), selectedTags)
                        commentText = ""
                        selectedTags = emptySet()
                        showCommentSection = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HelldeckColors.Green,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Text(
                        text = "💾 Save Feedback",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedFeedbackButton(
    text: String,
    label: String,
    color: Color,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var clickCount by remember { mutableIntStateOf(0) }

    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else if (isPressed) 0.9f else 1f,
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(
                dampingRatio = 0.5f,
                stiffness = 400f,
            )
        },
        label = "feedback_scale",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (clickCount > 0) 0.6f else 0.2f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Normal),
        label = "glow_alpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Button(
            onClick = {
                clickCount++
                onClick()
            },
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .shadow(
                    elevation = if (clickCount > 0) 8.dp else 4.dp,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    spotColor = color.copy(alpha = glowAlpha),
                    ambientColor = color.copy(alpha = glowAlpha * 0.5f),
                ),
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.Black,
            ),
            shape = androidx.compose.foundation.shape.CircleShape,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isPressed) 2.dp else 6.dp,
                pressedElevation = 0.dp,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                    ),
                )
                if (count != null && count > 0) {
                    Surface(
                        color = Color.Black,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(20.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (clickCount > 0) color else HelldeckColors.LightGray,
        )
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
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f,
        ),
        label = "button_scale",
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
            contentColor = Color.Black,
        ),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isPressed) 2.dp else 4.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.animateContentSize(),
        )
    }
}

/**
 * Game timer display with enhanced animations and pulsing effects
 */
@Composable
fun GameTimer(
    timeRemainingMs: Int,
    totalTimeMs: Int,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true,
) {
    val reducedMotion = LocalReducedMotion.current
    val progress = timeRemainingMs.toFloat() / totalTimeMs.toFloat()
    val isWarning = progress < 0.3f
    val isCritical = progress < 0.1f

    val timerColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            isCritical -> HelldeckColors.Error
            isWarning -> HelldeckColors.Warning
            else -> HelldeckColors.Yellow
        },
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else 500),
        label = "timer_color",
    )

    // Pulsing animation for critical time
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion) 1f else if (isCritical) 1.15f else 1f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(600, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "pulse_scale",
    )

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (reducedMotion) 0.3f else if (isCritical) 0.8f else 0.3f,
        animationSpec = if (reducedMotion) {
            infiniteRepeatable(animation = tween(HelldeckAnimations.Instant), repeatMode = RepeatMode.Restart)
        } else {
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "glow_intensity",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Timer text with scale animation
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(if (reducedMotion) 1f else if (isCritical) pulseScale else 1f)
                .then(
                    if (reducedMotion) {
                        Modifier
                    } else {
                        Modifier.shadow(
                            elevation = if (isCritical) 12.dp else 4.dp,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            spotColor = timerColor.copy(alpha = glowIntensity),
                            ambientColor = timerColor.copy(alpha = glowIntensity * 0.5f),
                        )
                    },
                ),
        ) {
            Text(
                text = formatTime(timeRemainingMs),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (isCritical) 56.sp else 48.sp,
                    fontWeight = FontWeight.Bold,
                    brush = if (!reducedMotion && isCritical) {
                        Brush.linearGradient(
                            colors = listOf(
                                timerColor,
                                timerColor.copy(alpha = 0.8f),
                                timerColor,
                            ),
                        )
                    } else {
                        null
                    },
                ),
                color = if (!isCritical) timerColor else Color.Unspecified,
                textAlign = TextAlign.Center,
            )
        }

        // Progress indicator
        if (showProgress) {
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(if (isCritical) 6.dp else 4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shadow(
                        elevation = if (isCritical) 4.dp else 0.dp,
                        shape = RoundedCornerShape(3.dp),
                        spotColor = timerColor.copy(alpha = 0.5f),
                    ),
                color = timerColor,
                trackColor = timerColor.copy(alpha = 0.2f),
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
    onClick: (() -> Unit)? = null,
) {
    val avatarModifier = modifier
        .size(size)
        .clip(RoundedCornerShape(HelldeckRadius.Medium))
        .background(
            if (isActive) {
                HelldeckColors.MediumGray
            } else {
                HelldeckColors.LightGray
            },
        )
        .then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            },
        )
        .padding(4.dp)

    Column(
        modifier = avatarModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = avatar,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 32.sp,
            ),
        )

        if (showName) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = HelldeckColors.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * Vote button for player selection with enhanced interactions and glow effects
 */
@Composable
fun VoteButton(
    playerName: String,
    playerAvatar: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderColor = if (isSelected) HelldeckColors.Yellow else HelldeckColors.MediumGray

    val scale by animateFloatAsState(
        targetValue = when {
            isSelected && isPressed -> 0.93f
            isSelected -> 1.05f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "vote_button_scale",
    )

    val glowIntensity by animateFloatAsState(
        targetValue = if (isSelected) 0.8f else if (isPressed) 0.3f else 0.1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "glow_intensity",
    )

    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) {
            borderColor.copy(alpha = 0.25f)
        } else if (isPressed) {
            borderColor.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "background_color",
    )

    // Pulsing glow effect for selected state
    val infiniteTransition = rememberInfiniteTransition()
    val selectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "selected_pulse",
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .width(120.dp)
            .height(72.dp)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) (8.dp * selectedPulse) else if (isPressed) 4.dp else 2.dp,
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                ambientColor = borderColor.copy(alpha = if (isSelected) glowIntensity * selectedPulse else 0.2f),
                spotColor = borderColor.copy(alpha = if (isSelected) glowIntensity * selectedPulse * 0.6f else 0.1f),
            ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        border = BorderStroke(
            width = if (isSelected) 3.dp else if (isPressed) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor.copy(alpha = if (isSelected) 1f else 0.6f),
                    borderColor.copy(alpha = if (isSelected) 0.9f else 0.8f),
                    borderColor.copy(alpha = if (isSelected) 1f else 0.6f),
                ),
            ),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = HelldeckColors.White,
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = if (isSelected) 6.dp else 0.dp,
            pressedElevation = 2.dp,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = playerAvatar,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (isSelected) 28.sp else 24.sp,
                ),
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isSelected) 13.sp else 12.sp,
                ),
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = if (isSelected) HelldeckColors.Yellow else HelldeckColors.White,
                modifier = Modifier.animateContentSize(),
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
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center,
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
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(colors),
                shape = RoundedCornerShape(HelldeckRadius.Large),
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
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
    size: Dp = 48.dp,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size)
                .rotate(rotation)
                .scale(pulse),
            color = color,
            strokeWidth = (4.dp * pulse).coerceAtMost(6.dp),
        )

        // Inner glow effect
        CircularProgressIndicator(
            modifier = Modifier
                .size(size * 0.7f)
                .rotate(-rotation * 0.5f),
            color = color.copy(alpha = 0.3f * pulse),
            strokeWidth = 2.dp,
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
    showSubtitle: Boolean = false,
) {
    val shimmer = rememberInfiniteTransition()

    val shimmerTranslate by shimmer.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = HelldeckColors.MediumGray.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                                    HelldeckColors.LightGray.copy(alpha = 0.3f),
                                ),
                                start = androidx.compose.ui.geometry.Offset(
                                    shimmerTranslate * 200f,
                                    0f,
                                ),
                                end = androidx.compose.ui.geometry.Offset(
                                    shimmerTranslate * 200f + 200f,
                                    100f,
                                ),
                            ),
                        ),
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
                                        HelldeckColors.LightGray.copy(alpha = 0.3f),
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(
                                        shimmerTranslate * 300f,
                                        0f,
                                    ),
                                    end = androidx.compose.ui.geometry.Offset(
                                        shimmerTranslate * 300f + 300f,
                                        50f,
                                    ),
                                ),
                            ),
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
                                            HelldeckColors.LightGray.copy(alpha = 0.2f),
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(
                                            shimmerTranslate * 200f,
                                            0f,
                                        ),
                                        end = androidx.compose.ui.geometry.Offset(
                                            shimmerTranslate * 200f + 200f,
                                            30f,
                                        ),
                                    ),
                                ),
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
    style: TextStyle = MaterialTheme.typography.headlineMedium,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = score.toString(),
            style = style.copy(
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.Yellow,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = HelldeckColors.LightGray,
        )
    }
}

/**
 * Podium section for top 3 players
 */
@Composable
fun PodiumSection(
    topPlayers: List<Player>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        // Second place
        if (topPlayers.size > 1) {
            PodiumCard(
                player = topPlayers[1],
                position = 2,
                height = 120.dp,
                modifier = Modifier.weight(1f),
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
                modifier = Modifier.weight(1.2f),
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Third place
        if (topPlayers.size > 2) {
            PodiumCard(
                player = topPlayers[2],
                position = 3,
                height = 100.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Individual podium card for top players
 */
@Composable
fun PodiumCard(
    player: Player,
    position: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    isWinner: Boolean = false,
) {
    val podiumColors = listOf(
        HelldeckColors.Yellow, // Gold for 1st
        HelldeckColors.MediumGray, // Silver for 2nd
        HelldeckColors.Orange, // Bronze for 3rd
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
            defaultElevation = if (isWinner) 12.dp else 8.dp,
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardColor.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Position indicator
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Player avatar
            Text(
                text = player.avatar,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 28.sp,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Player name
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Score
            ScoreDisplay(
                score = player.sessionPoints,
                label = "pts",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                ),
            )
        }
    }
}

/**
 * Player score card for remaining players
 */
@Composable
fun PlayerScoreCard(
    player: Player,
    position: Int,
    modifier: Modifier = Modifier,
    isTopThree: Boolean = false,
) {
    val positionColors = listOf(
        HelldeckColors.Yellow.copy(alpha = 0.8f),
        HelldeckColors.MediumGray.copy(alpha = 0.8f),
        HelldeckColors.Orange.copy(alpha = 0.8f),
    )

    val backgroundColor = when {
        isTopThree -> positionColors.getOrElse(position - 1) { HelldeckColors.MediumGray }
        position <= 3 -> positionColors.getOrElse(position - 1) { HelldeckColors.MediumGray }
        else -> HelldeckColors.MediumGray
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = backgroundColor.copy(alpha = 0.1f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Position badge
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (position <= 3) positionColors[position - 1] else HelldeckColors.LightGray,
                    modifier = Modifier
                        .background(
                            backgroundColor.copy(alpha = 0.2f),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Player info
                Column {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )

                    Text(
                        text = player.avatar,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HelldeckColors.LightGray,
                    )
                }
            }

            // Score
            ScoreDisplay(
                score = player.sessionPoints,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
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
) {
    val scale by animateFloatAsState(
        targetValue = 1.2f,
        animationSpec = tween(300, easing = EaseOutBack),
        finishedListener = null,
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
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
                        HelldeckColors.Yellow.copy(alpha = 0.8f),
                    ),
                ),
            ),
            modifier = Modifier.scale(scale),
            textAlign = TextAlign.Center,
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
                            Color.Transparent,
                        ),
                        radius = 100f,
                    ),
                ),
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
    val distance: Float,
)

/**
 * Particle animation effect
 */
@Composable
fun ParticleEffect(
    particle: Particle,
    color: Color = HelldeckColors.Yellow,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val animatedDistance by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = particle.distance,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                delayMillis = particle.initialDelay.toInt(),
                easing = EaseOutCubic,
            ),
            repeatMode = RepeatMode.Restart,
        ),
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                delayMillis = particle.initialDelay.toInt() + 1000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
    )

    val x = animatedDistance * kotlin.math.cos(Math.toRadians(particle.angle.toDouble())).toFloat()
    val y = animatedDistance * kotlin.math.sin(Math.toRadians(particle.angle.toDouble())).toFloat()

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(x = x.toInt().dp, y = y.toInt().dp)
            .background(
                color = color.copy(alpha = alpha),
                shape = androidx.compose.foundation.shape.CircleShape,
            )
            .shadow(
                elevation = 4.dp,
                spotColor = color.copy(alpha = alpha * 0.5f),
                shape = androidx.compose.foundation.shape.CircleShape,
            ),
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
    opacity: Float = 0.05f,
) {
    Box(
        modifier = modifier.fillMaxSize(),
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
    spacing: Dp = 32.dp,
) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        var y = 0f
        while (y < canvasHeight) {
            var x = 0f
            while (x < canvasWidth) {
                drawCircle(
                    center = Offset(x, y),
                    radius = dotSize.toPx() / 2,
                    color = HelldeckColors.Yellow.copy(alpha = opacity),
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
    spacing: Dp = 24.dp,
) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val color = HelldeckColors.LightGray.copy(alpha = opacity)
        val strokeWidth = lineWidth.toPx()

        // Vertical lines
        var x = 0f
        while (x < canvasWidth) {
            drawLine(
                start = Offset(x, 0f),
                end = Offset(x, canvasHeight),
                color = color,
                strokeWidth = strokeWidth,
            )
            x += spacing.toPx()
        }

        // Horizontal lines
        var y = 0f
        while (y < canvasHeight) {
            drawLine(
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                color = color,
                strokeWidth = strokeWidth,
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
    size: Dp = 20.dp,
) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val hexRadius = size.toPx() / 2
        val canvasWidth = size.toPx()
        val canvasHeight = size.toPx()
        val color = HelldeckColors.Orange.copy(alpha = opacity)

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
                    color = color,
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
    complexity: Int = 8,
) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val segments = complexity * 4
        val color = HelldeckColors.Green.copy(alpha = opacity)
        val strokeWidth = 1.dp.toPx()

        // Draw circuit-like lines
        repeat(segments) { i ->
            val startX = (i * canvasWidth / segments).coerceIn(0f, canvasWidth)
            val y = (i * canvasHeight / segments).coerceIn(0f, canvasHeight)

            // Horizontal lines
            drawLine(
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                color = color,
                strokeWidth = strokeWidth,
            )

            // Vertical lines with occasional branches
            if (i % 3 == 0) {
                drawLine(
                    start = Offset(startX, 0f),
                    end = Offset(startX, canvasHeight),
                    color = color,
                    strokeWidth = strokeWidth,
                )

                // Add some connection nodes
                drawCircle(
                    center = Offset(startX, y),
                    radius = 3.dp.toPx(),
                    color = HelldeckColors.Yellow.copy(alpha = opacity * 2),
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
    radius: Float,
    color: Color,
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

    drawPath(path = path, color = color, style = Stroke(width = 1.dp.toPx()))
}

/**
 * Flow row for tag layout
 */
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
    ) {
        content()
    }
}
