package com.helldeck

import com.helldeck.ui.nav.RouteAudit
import org.junit.Test
import org.junit.Assert.*

/**
 * Route audit validation test.
 * Ensures all navigation routes are valid and unique.
 */
class RouteAuditTest {
    @Test
    fun validate_noIssues() {
        val issues = RouteAudit.validate()
        assertTrue("Route audit found issues: $issues", issues.isEmpty())
    }

    @Test
    fun generateReport_succeeds() {
        val report = RouteAudit.generateReport()
        assertNotNull(report)
        assertTrue(report.isNotEmpty())
    }
}
