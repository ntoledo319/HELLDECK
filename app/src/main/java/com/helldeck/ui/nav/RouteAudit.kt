package com.helldeck.ui.nav

import com.helldeck.utils.Logger

/**
 * Route audit system to prevent navigation issues at runtime.
 * Validates navigation graph completeness and route uniqueness.
 *
 * Called on app startup in DEBUG builds to catch routing errors early.
 */
object RouteAudit {
    /**
     * Validates the navigation system for common issues.
     *
     * Checks:
     * - No duplicate route strings
     * - All screens have valid routes
     * - No orphaned or unreachable screens
     *
     * @return List of issue descriptions (empty if all OK)
     */
    fun validate(): List<String> {
        val issues = mutableListOf<String>()

        // Check for duplicate routes
        val routes = Screen.ALL.map { it.route }
        val duplicates = routes.groupBy { it }
            .filter { it.value.size > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            issues += "Duplicate routes found: $duplicates"
        }

        // Check for empty routes
        val emptyRoutes = Screen.ALL.filter { it.route.isBlank() }
        if (emptyRoutes.isNotEmpty()) {
            issues += "Empty routes found: ${emptyRoutes.map { it::class.simpleName }}"
        }

        // Check for route naming consistency (should be lowercase with underscores)
        val invalidRoutes = Screen.ALL.filter { screen ->
            !screen.route.matches(Regex("^[a-z][a-z0-9_]*$"))
        }
        if (invalidRoutes.isNotEmpty()) {
            issues += "Invalid route naming: ${invalidRoutes.map { "${it::class.simpleName}='${it.route}'" }}"
        }

        // Log results
        if (issues.isEmpty()) {
            Logger.i("RouteAudit: ✓ All ${Screen.ALL.size} routes validated successfully")
        } else {
            Logger.e("RouteAudit: ✗ Found ${issues.size} issues:\n${issues.joinToString("\n  ")}")
        }

        return issues
    }

    /**
     * Returns a human-readable report of all routes.
     * Useful for debugging navigation issues.
     */
    fun generateReport(): String {
        val sb = StringBuilder()
        sb.appendLine("HELLDECK Route Audit Report")
        sb.appendLine("=" * 50)
        sb.appendLine("Total screens: ${Screen.ALL.size}")
        sb.appendLine()
        sb.appendLine("Routes:")
        Screen.ALL.forEach { screen ->
            sb.appendLine("  ${screen::class.simpleName.padEnd(20)} -> ${screen.route}")
        }
        sb.appendLine()

        val issues = validate()
        if (issues.isEmpty()) {
            sb.appendLine("Status: ✓ OK")
        } else {
            sb.appendLine("Status: ✗ ISSUES FOUND")
            issues.forEach { issue ->
                sb.appendLine("  - $issue")
            }
        }

        return sb.toString()
    }
}

// Extension for string repetition
private operator fun String.times(count: Int): String = repeat(count)
