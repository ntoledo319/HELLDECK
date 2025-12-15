package com.helldeck.ui.nav

/**
 * Centralized screen route definitions for HELLDECK 2.0.
 * This is the single source of truth for all navigation routes.
 *
 * IMPORTANT: Any new screen MUST be added here and to the ALL list.
 * RouteAudit.kt validates that all routes are reachable and unique.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Lobby : Screen("lobby")
    data object HouseRules : Screen("house_rules")
    data object GroupDna : Screen("group_dna")
    data object Packs : Screen("packs")
    data object Roles : Screen("roles")
    data object Round : Screen("round")
    data object Feedback : Screen("feedback")
    data object Highlights : Screen("highlights")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object CardLab : Screen("card_lab")
    data object DebugHarness : Screen("debug_harness")

    // Additional screens for current implementation compatibility
    data object Rollcall : Screen("rollcall")
    data object Players : Screen("players")
    data object Rules : Screen("rules")
    data object Scoreboard : Screen("scoreboard")
    data object Profile : Screen("profile")
    data object GameRules : Screen("game_rules")

    companion object {
        /**
         * Complete list of all screens in the app.
         * Used by RouteAudit to verify navigation graph completeness.
         */
        val ALL = listOf(
            Home, Lobby, HouseRules, GroupDna, Packs, Roles,
            Round, Feedback, Highlights, Stats, Settings, CardLab, DebugHarness,
            Rollcall, Players, Rules, Scoreboard, Profile, GameRules
        )

        /**
         * Maps route string to Screen object.
         */
        fun fromRoute(route: String): Screen? {
            return ALL.firstOrNull { it.route == route }
        }
    }
}
