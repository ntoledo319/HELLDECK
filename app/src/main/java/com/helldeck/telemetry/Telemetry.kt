package com.helldeck.telemetry

/**
 * Minimal no-op telemetry sink. Replace with real analytics later (e.g., Firebase, Segment).
 */
object Telemetry {
    fun log(
        @Suppress("UNUSED_PARAMETER") event: String,
        @Suppress("UNUSED_PARAMETER") properties: Map<String, Any?> = emptyMap(),
    ) {
        // no-op in unit tests and offline builds
    }
}
