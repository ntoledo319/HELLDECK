package com.helldeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.*
import com.helldeck.ui.theme.HelldeckColors
import com.helldeck.ui.theme.HelldeckSpacing

/**
 * HELLDECK Design System Components
 * 
 * Unified neon-soaked aesthetic with consistent styling, animations, and interactions.
 * All components follow the "Hell's Living Room" design philosophy.
 * 
 * Design Principles:
 * - Neon glow effects on interactive elements
 * - Smooth spring-based animations
 * - High contrast with dark backgrounds
 * - Gradient accents for hierarchy
 * - Generous tap targets (min 48dp)
 * 
 * @ai_prompt Use these components for consistent HELLDECK styling
 * @context_boundary Single source of truth for design patterns
 */

/**
 * Standard HELLDECK neon card with gradient border and shadow glow.
 * Use for player cards, settings sections, content displays.
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    accentColor: Color = HelldeckColors.colorPrimary,
    elevation: androidx.compose.ui.unit.Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "neon_card_scale",
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) elevation / 2 else elevation,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = accentColor.copy(alpha = 0.5f),
                ambientColor = accentColor.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        colors = CardDefaults.cardColors(
            containerColor = HelldeckColors.surfaceElevated,
        ),
        border = BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.6f),
                    accentColor.copy(alpha = 0.3f),
                ),
            ),
        ),
        onClick = onClick ?: {},
        interactionSource = if (onClick != null) interactionSource else remember { MutableInteractionSource() },
        enabled = onClick != null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HelldeckColors.surfaceElevated,
                            HelldeckColors.surfacePrimary.copy(alpha = 0.8f),
                        ),
                    ),
                )
                .padding(HelldeckSpacing.Large.dp),
            content = content,
        )
    }
}

/**
 * HELLDECK primary CTA button with neon glow and spring physics.
 */
@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = HelldeckColors.colorPrimary,
    icon: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "glow_button_scale",
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                spotColor = accentColor.copy(alpha = 0.6f),
                ambientColor = accentColor.copy(alpha = 0.4f),
            ),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = HelldeckColors.background,
            disabledContainerColor = HelldeckColors.surfaceElevated,
            disabledContentColor = HelldeckColors.colorMuted,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Secondary outline button with HELLDECK styling.
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = HelldeckColors.colorPrimary,
    icon: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessHigh)
        },
        label = "outline_button_scale",
    )
    
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(HelldeckHeights.Button.dp)
            .scale(scale),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        border = BorderStroke(2.dp, if (enabled) accentColor else HelldeckColors.colorMuted),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (enabled) accentColor else HelldeckColors.colorMuted,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Standard empty state component with icon, message, and optional CTA.
 */
@Composable
fun EmptyState(
    icon: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = icon,
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = HelldeckColors.colorOnDark,
            textAlign = TextAlign.Center,
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(32.dp))
            
            GlowButton(
                text = actionLabel,
                onClick = onActionClick,
                modifier = Modifier.widthIn(min = 200.dp),
            )
        }
    }
}

/**
 * Info banner with icon and message for tips and warnings.
 */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: String = "💡",
    backgroundColor: Color = HelldeckColors.colorSecondary.copy(alpha = 0.12f),
    textColor: Color = HelldeckColors.colorOnDark,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Warning banner for critical information.
 */
@Composable
fun WarningBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: String = "⚠️",
) {
    InfoBanner(
        message = message,
        icon = icon,
        backgroundColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.15f),
        textColor = HelldeckColors.colorAccentWarm,
        modifier = modifier,
    )
}

/**
 * Section header with HELLDECK styling.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorPrimary,
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.colorMuted,
                )
            }
        }
        
        action?.invoke()
    }
}

/**
 * Stat display with label and value.
 */
@Composable
fun StatDisplay(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    valueColor: Color = HelldeckColors.colorPrimary,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = HelldeckColors.colorMuted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}

/**
 * Loading indicator with HELLDECK styling.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = HelldeckColors.colorPrimary,
            strokeWidth = 4.dp,
        )
        
        message?.let {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = HelldeckColors.colorMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
