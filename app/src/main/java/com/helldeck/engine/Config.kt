package com.helldeck.engine

import android.content.Context
import com.helldeck.AppCtx
import com.helldeck.utils.Logger
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.yaml.snakeyaml.Yaml

/**
 * Configuration data classes for HELLDECK game settings
 */
data class Timers(
    val vote_binary_ms: Int,
    val vote_avatar_ms: Int,
    val judge_pick_ms: Int,
    val revote_ms: Int,
    val alibi_show_ms: Int = 3000,
    val title_duel_ms: Int = 15000,
    val scatter_time_ms: Int = 10000
) {
    fun getTimerForInteraction(interaction: Interaction): Int {
        return when (interaction) {
            Interaction.VOTE_AVATAR -> vote_avatar_ms
            Interaction.TRUE_FALSE -> vote_binary_ms
            Interaction.AB_VOTE -> vote_binary_ms
            Interaction.JUDGE_PICK -> judge_pick_ms
            Interaction.SMASH_PASS -> vote_binary_ms
            Interaction.TARGET_PICK -> 0 // No timer
            Interaction.REPLY_TONE -> vote_binary_ms
            Interaction.TABOO_CLUE -> vote_avatar_ms
            Interaction.ODD_REASON -> vote_avatar_ms
            Interaction.DUEL -> title_duel_ms
            Interaction.SMUGGLE -> alibi_show_ms
            Interaction.PITCH -> title_duel_ms
            Interaction.SPEED_LIST -> scatter_time_ms
        }
    }
}

data class Scoring(
    val win: Int,
    val room_heat_bonus: Int,
    val room_heat_threshold: Double,
    val trash_penalty: Int,
    val streak_cap: Int,
    val judge_bonus: Int = 1,
    val fast_laugh_bonus: Double = 0.5,
    val consensus_bonus: Int = 1
) {
    fun calculateScore(
        lol: Int,
        trash: Int,
        judgeWin: Boolean,
        fastLaugh: Boolean,
        streakBonus: Int,
        roomHeat: Boolean,
        roomTrash: Boolean
    ): Double {
        var score = 0.0

        // Base scoring
        score += 2.0 * lol
        score += trash_penalty * trash

        // Judge win bonus
        if (judgeWin) score += judge_bonus

        // Fast laugh bonus
        if (fastLaugh) score += fast_laugh_bonus

        // Streak bonus (capped)
        score += minOf(streakBonus, streak_cap)

        // Room heat bonus
        if (roomHeat) score += room_heat_bonus

        // Room trash penalty (additional)
        if (roomTrash) score += trash_penalty

        return score
    }
}

data class PlayersCfg(
    val sweet_spot_min: Int,
    val sweet_spot_max: Int,
    val party_mode_max: Int,
    val max_afk_rounds: Int = 3,
    val team_size_threshold: Int = 11
)

data class LearningCfg(
    val alpha: Double,
    val epsilon_start: Double,
    val epsilon_end: Double,
    val decay_rounds: Int,
    val diversity_window: Int,
    val minhash_threshold: Double,
    val min_plays_before_learning: Int = 3,
    val score_weight_recent: Double = 0.7,
    val score_weight_historical: Double = 0.3
)

data class MechanicsCfg(
    val comeback_last_place_picks_next: Boolean,
    val roast_consensus_guess_cap: Int,
    val alibi_secrets_per_player: Int = 2,
    val title_fight_rounds: Int = 3,
    val scatter_words_required: Int = 3,
    val majority_report_threshold: Double = 0.6
)

data class UiCfg(
    val show_timer_warning: Boolean = true,
    val timer_warning_threshold: Double = 0.3,
    val enable_animations: Boolean = true,
    val animation_duration_ms: Int = 300,
    val haptic_feedback_intensity: Int = 1, // 0-3
    val sound_effects_enabled: Boolean = true
)

data class DebugCfg(
    val enable_logging: Boolean = true,
    val log_level: String = "INFO", // DEBUG, INFO, WARN, ERROR
    val enable_performance_monitoring: Boolean = false,
    val enable_database_query_logging: Boolean = false,
    val enable_template_selection_logging: Boolean = false
)

data class HelldeckCfg(
    val scoring: Scoring,
    val timers: Timers,
    val players: PlayersCfg,
    val learning: LearningCfg,
    val mechanics: MechanicsCfg,
    val ui: UiCfg = UiCfg(),
    val debug: DebugCfg = DebugCfg(),
    val generator: GeneratorCfg = GeneratorCfg()
)

data class GeneratorCfg(
    val safe_mode_gold_only: Boolean = true,
    val enable_v3_generator: Boolean = false,
    val locality_cap: Int = 3
)

/**
 * Configuration manager for HELLDECK
 * Loads and manages game configuration from YAML files
 */
object Config {

    lateinit var current: HelldeckCfg
        private set

    var spicyMode: Boolean = false
        set(value) {
            field = value
            // Update threshold when spicy mode changes
            roomHeatThresholdCache = null
            if (value) {
                customRoomHeatThreshold = 0.70
            } else if (customRoomHeatThreshold == 0.70) {
                customRoomHeatThreshold = null
            }
        }

    private var customRoomHeatThreshold: Double? = null
    private var roomHeatThresholdCache: Double? = null
    // Optional attempt cap to speed up generation (set via Settings)
    private var attemptCapOverride: Int? = null

    // Runtime feature flags (synced with settings)
    var learningEnabled: Boolean = true
        private set

    var hapticsEnabled: Boolean = true
        private set

    /**
     * Accessibility/safety runtime flags (synced with SettingsStore).
     *
     * These flags are UI-facing and MUST NOT change engine/content semantics.
     * They are read by Compose UI to adapt rendering/animation and device effects.
     */
    var reducedMotion: Boolean = false
        private set

    var highContrast: Boolean = false
        private set

    /**
     * When true, any torch/flash-based feedback should be disabled.
     * (Currently torch feedback is a no-op in this build, but we keep the flag
     * to preserve a stable API and future-proof the behavior.)
     */
    var noFlash: Boolean = true
        private set

    /**
     * Load configuration from assets (settings/default.yaml) with graceful fallback.
     */
    fun load(context: Context = AppCtx.ctx) {
        val cfg = try {
            context.assets.open("settings/default.yaml").use { parseYaml(it) }
        } catch (e: Exception) {
            com.helldeck.utils.Logger.e("Failed to load settings/default.yaml, using defaults", e)
            null
        }
        current = cfg ?: getDefaultConfig()
        roomHeatThresholdCache = null
    }

    /**
     * Load configuration from custom input stream (useful for tests / overrides).
     */
    fun loadFromInputStream(inputStream: InputStream) {
        current = parseYaml(inputStream) ?: getDefaultConfig()
        roomHeatThresholdCache = null
    }

    /**
     * Load configuration from raw YAML string.
     */
    fun loadFromString(yamlContent: String) {
        if (yamlContent.isBlank()) {
            current = getDefaultConfig()
        } else {
            current = parseYaml(yamlContent.byteInputStream()) ?: getDefaultConfig()
        }
        roomHeatThresholdCache = null
    }

    private fun parseYaml(inputStream: InputStream): HelldeckCfg? {
        return try {
            inputStream.use { ins ->
                val reader = InputStreamReader(ins, StandardCharsets.UTF_8)
                val raw = Yaml().load<Map<String, Any?>>(reader) ?: return null
                buildConfigFromMap(raw, getDefaultConfig())
            }
        } catch (e: Exception) {
            Logger.e("Config YAML parse failed", e)
            null
        }
    }

    private fun buildConfigFromMap(map: Map<String, Any?>, defaults: HelldeckCfg): HelldeckCfg {
        val scoringMap = map.section("scoring")
        val scoring = defaults.scoring.copy(
            win = scoringMap.int("win", defaults.scoring.win),
            room_heat_bonus = scoringMap.int("room_heat_bonus", defaults.scoring.room_heat_bonus),
            room_heat_threshold = scoringMap.double("room_heat_threshold", defaults.scoring.room_heat_threshold),
            trash_penalty = scoringMap.int("trash_penalty", defaults.scoring.trash_penalty),
            streak_cap = scoringMap.int("streak_cap", defaults.scoring.streak_cap),
            judge_bonus = scoringMap.int("judge_bonus", defaults.scoring.judge_bonus),
            fast_laugh_bonus = scoringMap.double("fast_laugh_bonus", defaults.scoring.fast_laugh_bonus),
            consensus_bonus = scoringMap.int("consensus_bonus", defaults.scoring.consensus_bonus)
        )

        val timersMap = map.section("timers")
        val timers = defaults.timers.copy(
            vote_binary_ms = timersMap.int("vote_binary_ms", defaults.timers.vote_binary_ms),
            vote_avatar_ms = timersMap.int("vote_avatar_ms", defaults.timers.vote_avatar_ms),
            judge_pick_ms = timersMap.int("judge_pick_ms", defaults.timers.judge_pick_ms),
            revote_ms = timersMap.int("revote_ms", defaults.timers.revote_ms),
            alibi_show_ms = timersMap.int("alibi_show_ms", defaults.timers.alibi_show_ms),
            title_duel_ms = timersMap.int("title_duel_ms", defaults.timers.title_duel_ms),
            scatter_time_ms = timersMap.int("scatter_time_ms", defaults.timers.scatter_time_ms)
        )

        val playersMap = map.section("players")
        val players = defaults.players.copy(
            sweet_spot_min = playersMap.int("sweet_spot_min", defaults.players.sweet_spot_min),
            sweet_spot_max = playersMap.int("sweet_spot_max", defaults.players.sweet_spot_max),
            party_mode_max = playersMap.int("party_mode_max", defaults.players.party_mode_max),
            max_afk_rounds = playersMap.int("max_afk_rounds", defaults.players.max_afk_rounds),
            team_size_threshold = playersMap.int("team_size_threshold", defaults.players.team_size_threshold)
        )

        val learningMap = map.section("learning")
        val learning = defaults.learning.copy(
            alpha = learningMap.double("alpha", defaults.learning.alpha),
            epsilon_start = learningMap.double("epsilon_start", defaults.learning.epsilon_start),
            epsilon_end = learningMap.double("epsilon_end", defaults.learning.epsilon_end),
            decay_rounds = learningMap.int("decay_rounds", defaults.learning.decay_rounds),
            diversity_window = learningMap.int("diversity_window", defaults.learning.diversity_window),
            minhash_threshold = learningMap.double("minhash_threshold", defaults.learning.minhash_threshold),
            min_plays_before_learning = learningMap.int("min_plays_before_learning", defaults.learning.min_plays_before_learning),
            score_weight_recent = learningMap.double("score_weight_recent", defaults.learning.score_weight_recent),
            score_weight_historical = learningMap.double("score_weight_historical", defaults.learning.score_weight_historical)
        )

        val mechanicsMap = map.section("mechanics")
        val mechanics = defaults.mechanics.copy(
            comeback_last_place_picks_next = mechanicsMap.bool("comeback_last_place_picks_next", defaults.mechanics.comeback_last_place_picks_next),
            roast_consensus_guess_cap = mechanicsMap.int("roast_consensus_guess_cap", defaults.mechanics.roast_consensus_guess_cap),
            alibi_secrets_per_player = mechanicsMap.int("alibi_secrets_per_player", defaults.mechanics.alibi_secrets_per_player),
            title_fight_rounds = mechanicsMap.int("title_fight_rounds", defaults.mechanics.title_fight_rounds),
            scatter_words_required = mechanicsMap.int("scatter_words_required", defaults.mechanics.scatter_words_required),
            majority_report_threshold = mechanicsMap.double("majority_report_threshold", defaults.mechanics.majority_report_threshold)
        )

        val uiMap = map.section("ui")
        val ui = defaults.ui.copy(
            show_timer_warning = uiMap.bool("show_timer_warning", defaults.ui.show_timer_warning),
            timer_warning_threshold = uiMap.double("timer_warning_threshold", defaults.ui.timer_warning_threshold),
            enable_animations = uiMap.bool("enable_animations", defaults.ui.enable_animations),
            animation_duration_ms = uiMap.int("animation_duration_ms", defaults.ui.animation_duration_ms),
            haptic_feedback_intensity = uiMap.int("haptic_feedback_intensity", defaults.ui.haptic_feedback_intensity),
            sound_effects_enabled = uiMap.bool("sound_effects_enabled", defaults.ui.sound_effects_enabled)
        )

        val debugMap = map.section("debug")
        val debug = defaults.debug.copy(
            enable_logging = debugMap.bool("enable_logging", defaults.debug.enable_logging),
            log_level = debugMap.string("log_level", defaults.debug.log_level),
            enable_performance_monitoring = debugMap.bool("enable_performance_monitoring", defaults.debug.enable_performance_monitoring),
            enable_database_query_logging = debugMap.bool("enable_database_query_logging", defaults.debug.enable_database_query_logging),
            enable_template_selection_logging = debugMap.bool("enable_template_selection_logging", defaults.debug.enable_template_selection_logging)
        )

        return defaults.copy(
            scoring = scoring,
            timers = timers,
            players = players,
            learning = learning,
            mechanics = mechanics,
            ui = ui,
            debug = debug
        )
    }

    private fun Map<String, Any?>.section(key: String): Map<*, *>? = this[key] as? Map<*, *>

    private fun Map<*, *>?.int(key: String, fallback: Int): Int {
        val raw = this?.get(key) ?: return fallback
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toDoubleOrNull()?.toInt() ?: fallback
            else -> fallback
        }
    }

    private fun Map<*, *>?.double(key: String, fallback: Double): Double {
        val raw = this?.get(key) ?: return fallback
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun Map<*, *>?.bool(key: String, fallback: Boolean): Boolean {
        val raw = this?.get(key) ?: return fallback
        return when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", true) || raw.equals("yes", true) || raw == "1"
            else -> fallback
        }
    }

    private fun Map<*, *>?.string(key: String, fallback: String): String {
        val raw = this?.get(key) ?: return fallback
        return raw.toString()
    }
    
    /**
     * Get default configuration (fallback if YAML fails)
     */
    private fun getDefaultConfig(): HelldeckCfg {
        return HelldeckCfg(
            scoring = Scoring(
                win = 3,                    // Increased from 2 to 3 for better reward
                room_heat_bonus = 2,            // Increased from 1 to 2
                room_heat_threshold = 0.65,        // Increased from 0.60 to 0.65 for easier heat
                trash_penalty = -1,               // Reduced penalty from -2 to -1 (less harsh)
                streak_cap = 5,                   // Increased from 3 to 5 for longer streaks
                fast_laugh_bonus = 1.0,          // Increased from 0.5 to 1.0
                consensus_bonus = 2                 // Increased from 1 to 2
            ),
            timers = Timers(
                vote_binary_ms = 6000,             // Reduced from 8000 for faster voting
                vote_avatar_ms = 8000,             // Reduced from 10000 for faster avatar voting
                judge_pick_ms = 4000,             // Reduced from 6000 for faster judging
                revote_ms = 2000                  // Reduced from 3000 for faster revoting
            ),
            players = PlayersCfg(
                sweet_spot_min = 3,
                sweet_spot_max = 10,
                party_mode_max = 16
            ),
            learning = LearningCfg(
                alpha = 0.4,                     // Increased from 0.3 for faster learning
                epsilon_start = 0.30,               // Increased from 0.25 for more exploration
                epsilon_end = 0.10,                 // Increased from 0.05 for sustained exploration
                decay_rounds = 15,                 // Reduced from 20 for faster adaptation
                diversity_window = 3,                 // Reduced from 5 for more variety
                minhash_threshold = 0.80              // Reduced from 0.85 for more learning
            ),
            mechanics = MechanicsCfg(
                comeback_last_place_picks_next = true,
                roast_consensus_guess_cap = 2
            ),
            generator = GeneratorCfg(
                safe_mode_gold_only = false,
                enable_v3_generator = true,
                locality_cap = 2
            )
        )
    }

    /**
     * Get room heat threshold based on current mode
     */
    fun roomHeatThreshold(): Double {
        return roomHeatThresholdCache ?: run {
            val threshold = customRoomHeatThreshold ?: if (spicyMode) 0.70 else current.scoring.room_heat_threshold
            roomHeatThresholdCache = threshold
            threshold
        }
    }

    fun setRoomHeatThreshold(threshold: Double?) {
        customRoomHeatThreshold = threshold
        roomHeatThresholdCache = null
    }

    fun setLearningEnabled(enabled: Boolean) {
        learningEnabled = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticsEnabled = enabled
    }

    fun setReducedMotion(enabled: Boolean) {
        reducedMotion = enabled
    }

    fun setHighContrast(enabled: Boolean) {
        highContrast = enabled
    }

    fun setNoFlash(enabled: Boolean) {
        noFlash = enabled
    }

    fun setSafeModeGoldOnly(enabled: Boolean) {
        current = current.copy(generator = current.generator.copy(safe_mode_gold_only = enabled))
    }

    fun setEnableV3Generator(enabled: Boolean) {
        current = current.copy(generator = current.generator.copy(enable_v3_generator = enabled))
    }

    fun generatorLocalityCap(): Int = current.generator.locality_cap.coerceIn(1, 3)

    fun setLocalityCap(cap: Int) {
        current = current.copy(generator = current.generator.copy(locality_cap = cap.coerceIn(1, 3)))
    }

    fun setAttemptCap(cap: Int?) {
        attemptCapOverride = cap?.coerceAtLeast(1)
    }

    fun getAttemptCap(): Int? = attemptCapOverride

    /**
     * Get timer for specific interaction
     */
    fun getTimerForInteraction(interaction: Interaction): Int {
        return current.timers.getTimerForInteraction(interaction)
    }

    /**
     * Calculate if room heat is achieved
     */
    fun isRoomHeat(lolCount: Int, totalPlayers: Int): Boolean {
        if (totalPlayers == 0) return false
        val threshold = roomHeatThreshold()
        return (lolCount.toDouble() / totalPlayers) >= threshold
    }

    /**
     * Calculate if room trash is achieved
     */
    fun isRoomTrash(trashCount: Int, totalPlayers: Int): Boolean {
        if (totalPlayers == 0) return false
        val threshold = roomHeatThreshold()
        return (trashCount.toDouble() / totalPlayers) >= threshold
    }

    /**
     * Get player count recommendation
     */
    fun getPlayerCountRecommendation(): String {
        val cfg = current.players
        return when {
            cfg.sweet_spot_min <= 3 && 3 <= cfg.sweet_spot_max -> "3-${cfg.sweet_spot_max} players recommended"
            else -> "${cfg.sweet_spot_min}-${cfg.sweet_spot_max} players recommended"
        }
    }

    /**
     * Check if player count is in sweet spot
     */
    fun isInSweetSpot(playerCount: Int): Boolean {
        val cfg = current.players
        return playerCount in cfg.sweet_spot_min..cfg.sweet_spot_max
    }

    /**
     * Check if player count requires party mode (teams)
     */
    fun requiresPartyMode(playerCount: Int): Boolean {
        val cfg = current.players
        return playerCount > cfg.party_mode_max
    }

    /**
     * Get epsilon value for learning algorithm based on round
     */
    fun getEpsilonForRound(roundIdx: Int): Double {
        val cfg = current.learning
        val t = (roundIdx.toDouble() / cfg.decay_rounds).coerceIn(0.0, 1.0)
        return cfg.epsilon_start + (cfg.epsilon_end - cfg.epsilon_start) * t
    }

    /**
     * Validate configuration integrity
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        with(current) {
            // Validate scoring
            if (scoring.win <= 0) errors.add("Win points must be positive")
            if (scoring.room_heat_threshold !in 0.0..1.0) errors.add("Room heat threshold must be between 0 and 1")
            if (scoring.trash_penalty >= 0) errors.add("Trash penalty must be negative")

            // Validate timers
            if (timers.vote_binary_ms <= 0) errors.add("Vote binary timer must be positive")
            if (timers.vote_avatar_ms <= 0) errors.add("Vote avatar timer must be positive")
            if (timers.judge_pick_ms <= 0) errors.add("Judge pick timer must be positive")

            // Validate players
            if (players.sweet_spot_min <= 0) errors.add("Sweet spot minimum must be positive")
            if (players.sweet_spot_max < players.sweet_spot_min) errors.add("Sweet spot maximum must be >= minimum")
            if (players.party_mode_max < players.sweet_spot_max) errors.add("Party mode max must be >= sweet spot max")

            // Validate learning
            if (learning.alpha !in 0.0..1.0) errors.add("Learning alpha must be between 0 and 1")
            if (learning.epsilon_start !in 0.0..1.0) errors.add("Epsilon start must be between 0 and 1")
            if (learning.epsilon_end !in 0.0..1.0) errors.add("Epsilon end must be between 0 and 1")
            if (learning.epsilon_start < learning.epsilon_end) errors.add("Epsilon start must be >= epsilon end")
            if (learning.diversity_window <= 0) errors.add("Diversity window must be positive")
        }

        return errors
    }

    /**
     * Get configuration summary for debugging
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "spicyMode" to spicyMode,
            "roomHeatThreshold" to roomHeatThreshold(),
            "playerRecommendation" to getPlayerCountRecommendation(),
            "generator" to mapOf(
                "safeModeGoldOnly" to current.generator.safe_mode_gold_only,
                "enableV3" to current.generator.enable_v3_generator
            ),
            "timers" to mapOf(
                "voteBinary" to current.timers.vote_binary_ms,
                "voteAvatar" to current.timers.vote_avatar_ms,
                "judgePick" to current.timers.judge_pick_ms
            ),
            "scoring" to mapOf(
                "winPoints" to current.scoring.win,
                "heatBonus" to current.scoring.room_heat_bonus,
                "trashPenalty" to current.scoring.trash_penalty
            )
        )
    }

    /**
     * Reset configuration to defaults
     */
    fun resetToDefaults() {
        load() // Reload from assets
        spicyMode = false
        roomHeatThresholdCache = null
    }

    /**
     * Export current configuration as YAML
     * Currently disabled due to SnakeYAML compatibility issues
     */
    fun exportAsYaml(): String {
        return "# YAML export currently disabled\n# Configuration: ${current}"
    }
}
