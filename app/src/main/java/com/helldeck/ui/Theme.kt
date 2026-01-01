package com.helldeck.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.engine.Config
import com.helldeck.settings.SettingsStore

/**
 * Helldeck Theme (HELL'S LIVING ROOM)
 *
 * Purpose:
 * - Party-proof legibility (arm’s length, dim room, loud chaos)
 * - Dark-first surfaces with neon accents
 * - Minimal magic numbers via a stable token surface (`HelldeckColors`, `HelldeckSpacing`, etc.)
 *
 * Non-negotiables:
 * - Preserve engine/content behavior: theme is purely presentation + accessibility flags.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHighContrast = staticCompositionLocalOf { false }
val LocalNoFlash = staticCompositionLocalOf { true }

private val HellLivingRoomDarkColorSchemeBase = darkColorScheme(
    background = Color(0xFF060608),
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF101016),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF181824),
    onSurfaceVariant = Color(0xFFB0B0C0),

    primary = Color(0xFFFF2768), // neon magenta-red
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFB51A4A), // darker variant
    onPrimaryContainer = Color(0xFFFFFFFF),

    secondary = Color(0xFFCBFF4D), // radioactive lime
    onSecondary = Color(0xFF0C0C0F),
    secondaryContainer = Color(0xFF90C92C), // darker variant
    onSecondaryContainer = Color(0xFF0C0C0F),

    tertiary = Color(0xFF4DF2FF), // accent cool (cyan)
    onTertiary = Color(0xFF001013),
    tertiaryContainer = Color(0xFF1FA9B3),
    onTertiaryContainer = Color(0xFF001013),

    error = Color(0xFFD72638), // deep saturated red
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF5B0A12),
    onErrorContainer = Color(0xFFFFFFFF),

    outline = Color(0xFF3A3A4D),
    outlineVariant = Color(0xFF2A2A36),
    scrim = Color(0xB0000000),

    inverseSurface = Color(0xFFECECF6),
    inverseOnSurface = Color(0xFF0C0C0F),
    inversePrimary = Color(0xFFFF2768),
    surfaceTint = Color(0xFFFF2768),
)

/**
 * Light scheme is supported for accessibility/system preference, but the app is dark-first.
 * We keep contrast high and accents consistent.
 */
private val HellLivingRoomLightColorSchemeBase = lightColorScheme(
    background = Color(0xFFF8F8FF),
    onBackground = Color(0xFF0C0C0F),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C0C0F),
    surfaceVariant = Color(0xFFEDEDF8),
    onSurfaceVariant = Color(0xFF3B3B52),

    primary = Color(0xFFFF2768),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFC1D5),
    onPrimaryContainer = Color(0xFF2B0010),

    secondary = Color(0xFF6EA100),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCBFF4D),
    onSecondaryContainer = Color(0xFF0C0C0F),

    tertiary = Color(0xFF008C98),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF4DF2FF),
    onTertiaryContainer = Color(0xFF001013),

    error = Color(0xFFD72638),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF2B0006),

    outline = Color(0xFF6A6A86),
    outlineVariant = Color(0xFFC6C6DA),
    scrim = Color(0x80000000),

    inverseSurface = Color(0xFF101016),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFFF2768),
    surfaceTint = Color(0xFFFF2768),
)

/**
 * Party-readability typography:
 * - Big, bold titles for arm’s length
 * - Comfortable body sizes for instructions
 * - Large labels for buttons
 */
@Composable
fun HelldeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val reducedMotion by SettingsStore.reducedMotionFlow().collectAsState(initial = false)
    val highContrast by SettingsStore.highContrastFlow().collectAsState(initial = false)
    val noFlash by SettingsStore.noFlashFlow().collectAsState(initial = true)

    LaunchedEffect(reducedMotion, highContrast, noFlash) {
        // Keep backend flags in sync for non-Compose callers (e.g., feedback manager).
        Config.setReducedMotion(reducedMotion)
        Config.setHighContrast(highContrast)
        Config.setNoFlash(noFlash)
    }

    val base = if (darkTheme) HellLivingRoomDarkColorSchemeBase else HellLivingRoomLightColorSchemeBase
    val colorScheme = remember(base, highContrast) {
        if (!highContrast) return@remember base
        // High contrast mode: stronger outlines and brighter secondary text.
        base.copy(
            onSurfaceVariant = Color(0xFFE7E7F4),
            outline = Color(0xFF6A6A92),
            outlineVariant = Color(0xFF4A4A66),
        )
    }

    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalHighContrast provides highContrast,
        LocalNoFlash provides noFlash,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HellLivingRoomTypography,
            shapes = HellLivingRoomShapes,
            content = content,
        )
    }
}

/**
 * Preview theme for design system
 */
@Composable
fun HelldeckPreviewTheme(
    content: @Composable () -> Unit,
) {
    HelldeckTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            content = content,
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
 * Token surface used across the project.
 *
 * IMPORTANT:
 * - Keep legacy names (Yellow/Green/etc.) for compatibility.
 * - New "Hell's Living Room" tokens live here too.
 */
object HelldeckColors {
    // Hell’s Living Room core tokens
    val background = Color(0xFF060608)
    val surfacePrimary = Color(0xFF101016)
    val surfaceElevated = Color(0xFF181824)

    val colorPrimary = Color(0xFFFF2768)
    val colorPrimaryVariant = Color(0xFFB51A4A)
    val colorSecondary = Color(0xFFCBFF4D)
    val colorSecondaryVariant = Color(0xFF90C92C)

    val colorAccentWarm = Color(0xFFFF9F1C)
    val colorAccentCool = Color(0xFF4DF2FF)

    val colorOnDark = Color(0xFFFFFFFF)
    val colorMuted = Color(0xFFB0B0C0)
    val colorDangerText = Color(0xFFFF6B6B)

    // Semantic tokens (Helldeck specifics)
    val Success = colorSecondary
    val Warning = colorAccentWarm
    val Error = Color(0xFFD72638)

    // Keep/define LOL/MEH/TRASH
    val Lol = Color(0xFFFFD166) // bright gold
    val Meh = Color(0xFF8E8E9E) // medium gray
    val Trash = Color(0xFFD72638)

    val VoteSelected = Color(0xFFCBFF4D) // high-contrast lime
    val VoteUnselected = Color(0xFF60606E) // muted gray

    // Timer colors (legible on dark)
    val TimerNormal = colorAccentCool
    val TimerWarning = colorAccentWarm
    val TimerCritical = Error

    // Compatibility aliases (old names)
    val Yellow = Lol
    val Green = colorSecondary
    val Orange = colorAccentWarm
    val Red = Error
    val Blue = Color(0xFF4361EE) // Added for compatibility
    val Purple = Color(0xFF7209B7) // Added for compatibility

    val Black = background
    val DarkGray = surfacePrimary
    val MediumGray = surfaceElevated
    val LightGray = Color(0xFFB0B0C0)

    val White = colorOnDark
    val OffWhite = Color(0xFFF8F8FF)
    val DarkWhite = Color(0xFFE7E7F4)

    val Info = colorAccentCool
}

/**
 * Extended color roles for Material Design 3.
 * These are legacy extension properties kept for compilation stability.
 */
val ColorScheme.Yellow: Color get() = HelldeckColors.Lol
val ColorScheme.helldeckGreen: Color get() = HelldeckColors.colorSecondary
val ColorScheme.Orange: Color get() = HelldeckColors.colorAccentWarm
val ColorScheme.helldeckRed: Color get() = HelldeckColors.Error
val ColorScheme.helldeckBlack: Color get() = HelldeckColors.background
val ColorScheme.helldeckDarkGray: Color get() = HelldeckColors.surfacePrimary
val ColorScheme.MediumGray: Color get() = HelldeckColors.surfaceElevated
val ColorScheme.LightGray: Color get() = HelldeckColors.colorMuted
val ColorScheme.helldeckWhite: Color get() = HelldeckColors.colorOnDark
val ColorScheme.helldeckLol: Color get() = HelldeckColors.Lol
val ColorScheme.helldeckMeh: Color get() = HelldeckColors.Meh
val ColorScheme.helldeckTrash: Color get() = HelldeckColors.Trash

/**
 * Animation specifications for HELLDECK
 */
object HelldeckAnimations {
    // Global timing targets (ms): fast, consistent, party-friendly.
    const val Instant = 1
    const val Fast = 180
    const val Normal = 260
    const val Slow = 320
}

/**
 * Spacing values for HELLDECK
 */
object HelldeckSpacing {
    // Token scale: 4 / 8 / 12 / 16 / 24 / 32 dp
    val None = 0
    val Tiny = 4
    val Small = 8
    val Medium = 12
    val Large = 16
    val ExtraLarge = 24
    val Huge = 32

    // Back-compat: keep Massive, but avoid introducing a new scale value.
    val Massive = 32
}

/**
 * Component heights for HELLDECK
 */
object HelldeckHeights {
    // Comfortable tap targets for parties.
    val Button = 60
    val Card = 200
    val AppBar = 64
    val BottomBar = 88
    val Input = 56
}

/**
 * Border radius values for HELLDECK
 */
object HelldeckRadius {
    // Cards: 20dp, Secondary buttons: 12dp, Primary buttons: pill.
    val None = 0.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 20.dp
    val ExtraLarge = 24.dp
    val Pill = 999.dp
}

private val HellLivingRoomTypography = Typography(
    // Big headings / card prompts
    displayLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Black,
    ),
    displayMedium = TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
    ),

    // Body (instructions)
    bodyLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Normal,
    ),

    // Buttons / labels
    labelLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

private val HellLivingRoomShapes = Shapes(
    small = RoundedCornerShape(HelldeckRadius.Medium),
    medium = RoundedCornerShape(HelldeckRadius.Large),
    large = RoundedCornerShape(HelldeckRadius.ExtraLarge),
)
