package com.helldeck.ui.theme

// Bridge aliases so files importing com.helldeck.ui.theme.* compile
object HelldeckColors {
    // Hell's Living Room token surface (new)
    val background get() = com.helldeck.ui.HelldeckColors.background
    val surfacePrimary get() = com.helldeck.ui.HelldeckColors.surfacePrimary
    val surfaceElevated get() = com.helldeck.ui.HelldeckColors.surfaceElevated
    val colorPrimary get() = com.helldeck.ui.HelldeckColors.colorPrimary
    val colorPrimaryVariant get() = com.helldeck.ui.HelldeckColors.colorPrimaryVariant
    val colorSecondary get() = com.helldeck.ui.HelldeckColors.colorSecondary
    val colorSecondaryVariant get() = com.helldeck.ui.HelldeckColors.colorSecondaryVariant
    val colorAccentWarm get() = com.helldeck.ui.HelldeckColors.colorAccentWarm
    val colorAccentCool get() = com.helldeck.ui.HelldeckColors.colorAccentCool
    val colorOnDark get() = com.helldeck.ui.HelldeckColors.colorOnDark
    val colorMuted get() = com.helldeck.ui.HelldeckColors.colorMuted
    val colorDangerText get() = com.helldeck.ui.HelldeckColors.colorDangerText

    // Compatibility aliases (old names)
    val Yellow get() = com.helldeck.ui.HelldeckColors.Yellow
    val Green get() = com.helldeck.ui.HelldeckColors.Green
    val Orange get() = com.helldeck.ui.HelldeckColors.Orange
    val Red get() = com.helldeck.ui.HelldeckColors.Red
    val Black get() = com.helldeck.ui.HelldeckColors.Black
    val DarkGray get() = com.helldeck.ui.HelldeckColors.DarkGray
    val MediumGray get() = com.helldeck.ui.HelldeckColors.MediumGray
    val LightGray get() = com.helldeck.ui.HelldeckColors.LightGray
    val White get() = com.helldeck.ui.HelldeckColors.White
    val OffWhite get() = com.helldeck.ui.HelldeckColors.OffWhite
    val DarkWhite get() = com.helldeck.ui.HelldeckColors.DarkWhite
    val Success get() = com.helldeck.ui.HelldeckColors.Success
    val Warning get() = com.helldeck.ui.HelldeckColors.Warning
    val Error get() = com.helldeck.ui.HelldeckColors.Error
    val Info get() = com.helldeck.ui.HelldeckColors.Info
    val Lol get() = com.helldeck.ui.HelldeckColors.Lol
    val Meh get() = com.helldeck.ui.HelldeckColors.Meh
    val Trash get() = com.helldeck.ui.HelldeckColors.Trash
    val VoteSelected get() = com.helldeck.ui.HelldeckColors.VoteSelected
    val VoteUnselected get() = com.helldeck.ui.HelldeckColors.VoteUnselected
    val TimerNormal get() = com.helldeck.ui.HelldeckColors.TimerNormal
    val TimerWarning get() = com.helldeck.ui.HelldeckColors.TimerWarning
    val TimerCritical get() = com.helldeck.ui.HelldeckColors.TimerCritical
}

object HelldeckSpacing {
    val None get() = com.helldeck.ui.HelldeckSpacing.None
    val Tiny get() = com.helldeck.ui.HelldeckSpacing.Tiny
    val Small get() = com.helldeck.ui.HelldeckSpacing.Small
    val Medium get() = com.helldeck.ui.HelldeckSpacing.Medium
    val Large get() = com.helldeck.ui.HelldeckSpacing.Large
    val ExtraLarge get() = com.helldeck.ui.HelldeckSpacing.ExtraLarge
    val Huge get() = com.helldeck.ui.HelldeckSpacing.Huge
    val Massive get() = com.helldeck.ui.HelldeckSpacing.Massive
}

object HelldeckHeights {
    val Button get() = com.helldeck.ui.HelldeckHeights.Button
    val Card get() = com.helldeck.ui.HelldeckHeights.Card
    val AppBar get() = com.helldeck.ui.HelldeckHeights.AppBar
    val BottomBar get() = com.helldeck.ui.HelldeckHeights.BottomBar
    val Input get() = com.helldeck.ui.HelldeckHeights.Input
}
