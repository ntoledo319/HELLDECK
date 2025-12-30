package com.helldeck.content.generator

import android.content.Context
import androidx.test.core.app.ApplicationProvider  
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as RoboConfig
import org.yaml.snakeyaml.Yaml

/**
 * Regression tests to lock generator rules.
 * If someone loosens quality thresholds, tests fail fast.
 */
@RunWith(AndroidJUnit4::class)
@RoboConfig(sdk = [33])
class RuleRegressionTest {

    @Test
    fun rulesYamlMaintainsQualityThresholds() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val content = ctx.assets.open("model/rules.yaml").bufferedReader().use { it.readText() }
        val yaml = Yaml()
        val rules = yaml.load<Map<String, Any>>(content)
        
        // Lock minimum word count (should not be lowered)
        val minWords = (rules["min_word_count"] as? Number)?.toInt() ?: 0
        assertTrue("min_word_count must be ≥ 5 to prevent too-short cards", minWords >= 5)
        
        // Lock maximum word count (should not be dramatically raised)
        val maxWords = (rules["max_word_count"] as? Number)?.toInt() ?: 100
        assertTrue("max_word_count must be ≤ 35 for readability", maxWords <= 35)
        
        // Lock repetition ratio (should not be loosened)
        val maxRepeat = (rules["max_repetition_ratio"] as? Number)?.toDouble() ?: 1.0
        assertTrue("max_repetition_ratio must be ≤ 0.40 to prevent word spam", maxRepeat <= 0.40)
        
        // Lock max attempts (should not be dramatically increased for performance)
        val maxAttempts = (rules["max_attempts"] as? Number)?.toInt() ?: 10
        assertTrue("max_attempts should be ≤ 5 for performance", maxAttempts <= 5)
        
        // Lock coherence threshold (should not be dramatically lowered)
        val coherenceThreshold = (rules["coherence_threshold"] as? Number)?.toDouble() ?: 0.0
        assertTrue("coherence_threshold should be ≥ 0.10 for quality", coherenceThreshold >= 0.10)
        
        // Version check
        val version = (rules["version"] as? Number)?.toInt() ?: 0
        assertEquals("rules.yaml version should remain stable", 1, version)
    }
    
    @Test
    fun bannedTokensExist() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        try {
            val content = ctx.assets.open("model/banned.json").bufferedReader().use { it.readText() }
            assertFalse("banned.json should not be empty", content.trim().isEmpty())
            assertTrue("Should be valid JSON array or object", content.trim().startsWith("{") || content.trim().startsWith("["))
        } catch (e: Exception) {
            // Banned tokens are optional but if present must be valid
            println("Warning: banned.json not found or invalid: ${e.message}")
        }
    }
    
    @Test
    fun pairingsHaveReasonableWeights() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val content = ctx.assets.open("model/pairings.json").bufferedReader().use { it.readText() }
        
        // Check for extreme weights that could skew scoring
        assertFalse("Pairing weights should not exceed 1.0 (found in content)",
            content.contains(Regex(":\\s*[2-9]\\.")))  // Catches weights ≥ 2.0
        
        assertFalse("Pairing weights should not go below -1.0 (found in content)",
            content.contains(Regex(":\\s*-[2-9]\\.")))  // Catches weights ≤ -2.0
    }
    
    @Test
    fun goldBankHasMinimumCoverage() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val content = ctx.assets.open("gold/gold_cards.json").bufferedReader().use { it.readText() }
        
        // Count approximate number of gold cards
        val cardCount = content.count { it == '{' } - 1 // Subtract outer object
        assertTrue("Gold bank should have at least 30 cards for decent coverage", cardCount >= 30)
        
        // Check critical games have coverage - using official 14 games from HDRealRules.md
        val criticalGames = listOf(
            "ROAST_CONSENSUS", "CONFESSION_OR_CAP", "POISON_PITCH", "FILL_IN_FINISHER",
            "RED_FLAG_RALLY", "HOT_SEAT_IMPOSTER", "TEXT_THREAD_TRAP", "TABOO_TIMER"
        )
        criticalGames.forEach { game ->
            assertTrue("Gold bank must cover critical game: $game", content.contains("\"$game\""))
        }
    }
    
    @Test
    fun priorAlphaBetaArePositive() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val content = ctx.assets.open("model/priors.json").bufferedReader().use { it.readText() }
        
        // Alpha and Beta should be positive for valid Beta distribution
        assertFalse("Alpha values must be positive", content.contains(Regex("\"alpha\":\\s*0\\b")))
        assertFalse("Beta values must be positive", content.contains(Regex("\"beta\":\\s*0\\b")))
        assertFalse("Alpha should not be negative", content.contains(Regex("\"alpha\":\\s*-")))
        assertFalse("Beta should not be negative", content.contains(Regex("\"beta\":\\s*-")))
    }
}