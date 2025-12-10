package com.helldeck.macro

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup_cold() = benchmarkRule.measureRepeated(
        packageName = "com.helldeck",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.SpeedProfile(),
        startupMode = StartupMode.COLD,
        iterations = 3
    ) {
        pressHome()
        startActivityAndWait()
    }
}

