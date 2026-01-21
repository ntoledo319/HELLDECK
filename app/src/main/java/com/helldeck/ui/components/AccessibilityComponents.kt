package com.helldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.helldeck.ui.HelldeckColors

/**
 * Accessibility enhancement components for HELLDECK.
 * 
 * Features:
 * - Screen reader hints and labels
 * - High contrast mode support
 * - Semantic descriptions
 * - Accessible touch targets
 * 
 * @ai_prompt Accessibility components ensure inclusive design
 */

/**
 * Enhanced button with accessibility semantics.
 */
@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: String? = null,
    contentDescription: String? = null,
    isDestructive: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp) // Minimum touch target
            .semantics {
                this.contentDescription = contentDescription ?: text
                if (isDestructive) {
                    this.role = Role.Button
                    this.stateDescription = "Destructive action"
                }
            },
    ) {
        if (icon != null) {
            Text("$icon ", modifier = Modifier.semantics { this.contentDescription = "" })
        }
        Text(text)
    }
}

/**
 * Screen reader announcement component.
 */
@Composable
fun ScreenReaderAnnouncement(
    message: String,
    politeness: LiveRegionMode = LiveRegionMode.Polite,
) {
    Box(
        modifier = Modifier
            .size(0.dp)
            .semantics {
                liveRegion = politeness
                contentDescription = message
            }
    )
}

/**
 * Accessible card with proper semantics.
 */
@Composable
fun AccessibleCard(
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = buildString {
                append(title)
                if (description != null) {
                    append(". ")
                    append(description)
                }
            }
            if (onClick != null) {
                role = Role.Button
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * Accessible toggle with clear state announcement.
 */
@Composable
fun AccessibleSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(label)
                    if (description != null) {
                        append(". ")
                        append(description)
                    }
                    append(". ")
                    append(if (checked) "On" else "Off")
                }
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.colorMuted,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/**
 * Accessible slider with value announcements.
 */
@Composable
fun AccessibleSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { it.toString() },
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "$label: ${valueFormatter(value)}"
                },
        )
        Text(
            valueFormatter(value),
            style = MaterialTheme.typography.bodySmall,
            color = HelldeckColors.colorMuted,
        )
    }
}

/**
 * Accessible list item with proper semantics.
 */
@Composable
fun AccessibleListItem(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(title)
                    if (subtitle != null) {
                        append(". ")
                        append(subtitle)
                    }
                }
                if (onClick != null) {
                    role = Role.Button
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = HelldeckColors.colorMuted,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/**
 * Accessible text field with proper hints.
 */
@Composable
fun AccessibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            enabled = enabled,
            singleLine = singleLine,
            isError = error != null,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = buildString {
                        append(label)
                        if (hint != null) {
                            append(". ")
                            append(hint)
                        }
                        if (error != null) {
                            append(". Error: ")
                            append(error)
                        }
                    }
                },
        )
        if (hint != null && error == null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = HelldeckColors.colorMuted,
            )
        }
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * Status announcement for screen readers.
 */
@Composable
fun StatusAnnouncement(
    status: String,
    isImportant: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(0.dp)
            .semantics {
                liveRegion = if (isImportant) LiveRegionMode.Assertive else LiveRegionMode.Polite
                contentDescription = status
            }
    )
}

/**
 * High contrast color provider.
 */
object HighContrastColors {
    val background = androidx.compose.ui.graphics.Color.Black
    val onBackground = androidx.compose.ui.graphics.Color.White
    val primary = androidx.compose.ui.graphics.Color.Yellow
    val onPrimary = androidx.compose.ui.graphics.Color.Black
    val error = androidx.compose.ui.graphics.Color.Red
    val success = androidx.compose.ui.graphics.Color.Green
}

/**
 * Modifier extension for high contrast mode.
 */
fun Modifier.highContrastBorder(enabled: Boolean): Modifier {
    return if (enabled) {
        this.then(
            Modifier.semantics {
                // Border will be added via theme
            }
        )
    } else {
        this
    }
}
