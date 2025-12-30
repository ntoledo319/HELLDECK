package com.helldeck.engine

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class ConfigYamlTest {

    private lateinit var defaults: HelldeckCfg

    @Before
    fun snapshotDefaults() {
        Config.loadFromString("")
        defaults = Config.current
    }

    @After
    fun restoreDefaults() {
        Config.loadFromString("")
    }

    @Test
    fun `loadFromString applies overrides`() {
        val yaml = """
            scoring:
              win: 7
              room_heat_threshold: 0.72
            timers:
              vote_binary_ms: 9200
            debug:
              enable_logging: false
              log_level: "WARN"
            ui:
              haptic_feedback_intensity: 2
        """.trimIndent()

        Config.loadFromString(yaml)

        assertEquals(7, Config.current.scoring.win)
        assertEquals(0.72, Config.current.scoring.room_heat_threshold, 0.0001)
        assertEquals(9200, Config.current.timers.vote_binary_ms)
        assertFalse(Config.current.debug.enable_logging)
        assertEquals("WARN", Config.current.debug.log_level)
        assertEquals(2, Config.current.ui.haptic_feedback_intensity)
    }

    @Test
    fun `loadFromString falls back on parse failure`() {
        Config.loadFromString("scoring: [oops]")
        assertEquals(defaults.scoring.win, Config.current.scoring.win)
    }
}
