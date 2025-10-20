package com.helldeck.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

/**
 * HELLDECK custom color scheme
 */
private val HelldeckDarkColorScheme = darkColorScheme(
    // Accents
    primary = Color(0xFFFFD166),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFCCAA44),
    onPrimaryContainer = Color(0xFF000000),

    secondary = Color(0xFF7CFC00),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF66CC00),
    onSecondaryContainer = Color(0xFF000000),

    tertiary = Color(0xFFFF8C00),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFCC7000),
    onTertiaryContainer = Color(0xFF000000),

    // Error
    error = Color(0xFFFF4444),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFFCC2222),
    onErrorContainer = Color(0xFFFFFFFF),

    // Neutrals tuned for readability in dark mode
    background = Color(0xFF121212),       // Slightly lifted from pure black
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF1E1E1E),          // Lighter card/containers for contrast
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2A2A2A),   // Used for secondary surfaces
    onSurfaceVariant = Color(0xFFD0D0D0), // Readable secondary text/icons

    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF4D4D4D),

    scrim = Color(0x80000000),

    inverseSurface = Color(0xFF2A2A2A),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFFFD166),

    surfaceTint = Color(0xFFFFD166)
)

/**
 * HELLDECK light color scheme (for accessibility)
 */
private val HelldeckLightColorScheme = lightColorScheme(
    primary = Color(0xFFCC9900),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD166),
    onPrimaryContainer = Color(0xFF000000),

    secondary = Color(0xFF66AA00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF7CFC00),
    onSecondaryContainer = Color(0xFF000000),

    tertiary = Color(0xFFCC7000),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFF8C00),
    onTertiaryContainer = Color(0xFF000000),

    error = Color(0xFFCC2222),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF4444),
    onErrorContainer = Color(0xFF000000),

    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF000000),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE9E9E9),
    onSurfaceVariant = Color(0xFF4D4D4D),

    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF999999),

    scrim = Color(0x80000000),

    inverseSurface = Color(0xFF000000),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFCC9900),

    surfaceTint = Color(0xFFCC9900)
)

/**
 * HELLDECK theme composable
 */
@Composable
fun HelldeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        HelldeckDarkColorScheme
    } else {
        HelldeckLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}

/**
 * Preview theme for design system
 */
@Composable
fun HelldeckPreviewTheme(
    content: @Composable () -> Unit
) {
    HelldeckTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

/**
 * Game-specific theme variations
 */
object HelldeckTheme {
    val colors: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes
}

/**
 * Color roles for HELLDECK
 */
object HelldeckColors {
    // Brand colors
    val Yellow = Color(0xFFFFD166)
    val Green = Color(0xFF7CFC00)
    val Orange = Color(0xFFFF8C00)
    val Red = Color(0xFFFF4444)

    // Background colors
    val Black = Color(0xFF000000)
    // Neutrals retuned for legibility
    val DarkGray = Color(0xFF1E1E1E)     // Card/containers on dark bg
    val MediumGray = Color(0xFF2C2C2C)   // Secondary containers
    val LightGray = Color(0xFFCFCFCF)    // Secondary text on dark surfaces

    // Text colors
    val White = Color(0xFFFFFFFF)
    val OffWhite = Color(0xFFF5F5F5)
    val DarkWhite = Color(0xFFE0E0E0)

    // State colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)

    // Game-specific colors
    val Lol = Color(0xFFFFD700)
    val Meh = Color(0xFF808080)
    val Trash = Color(0xFF8B0000)

    // Vote colors
    val VoteSelected = Color(0xFFFFD166)
    val VoteUnselected = Color(0xFF5A5A5A)

    // Timer colors
    val TimerNormal = Color(0xFFFFD166)
    val TimerWarning = Color(0xFFFF8C00)
    val TimerCritical = Color(0xFFFF4444)
}

/**
 * Extended color roles for Material Design 3
 */
val ColorScheme.Yellow: Color get() = Color(0xFFFFD166)
val ColorScheme.helldeckGreen: Color get() = Color(0xFF7CFC00)
val ColorScheme.Orange: Color get() = Color(0xFFFF8C00)
val ColorScheme.helldeckRed: Color get() = Color(0xFFFF4444)
val ColorScheme.helldeckBlack: Color get() = Color(0xFF000000)
val ColorScheme.helldeckDarkGray: Color get() = Color(0xFF1E1E1E)
val ColorScheme.MediumGray: Color get() = Color(0xFF2C2C2C)
val ColorScheme.LightGray: Color get() = Color(0xFFCFCFCF)
val ColorScheme.helldeckWhite: Color get() = Color(0xFFFFFFFF)
val ColorScheme.helldeckLol: Color get() = Color(0xFFFFD700)
val ColorScheme.helldeckMeh: Color get() = Color(0xFF808080)
val ColorScheme.helldeckTrash: Color get() = Color(0xFF8B0000)

/**
 * Animation specifications for HELLDECK
 */
object HelldeckAnimations {
    const val Fast = 150
    const val Normal = 300
    const val Slow = 500

    val CardEnter = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessLow
    )

    val CardExit = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessVeryLow
    )

    val ButtonPress = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessHigh
    )
}

/**
 * Spacing values for HELLDECK
 */
object HelldeckSpacing {
    val None = 0
    val Tiny = 4
    val Small = 8
    val Medium = 16
    val Large = 24
    val ExtraLarge = 32
    val Huge = 48
    val Massive = 64
}

/**
 * Component heights for HELLDECK
 */
object HelldeckHeights {
    val Button = 48
    val Card = 200
    val AppBar = 64
    val BottomBar = 80
    val Input = 56
}

/**
 * Border radius values for HELLDECK
 */
object HelldeckRadius {
    val None = 0
    val Small = 8
    val Medium = 12
    val Large = 16
    val ExtraLarge = 24
    val Full = Int.MAX_VALUE
}
