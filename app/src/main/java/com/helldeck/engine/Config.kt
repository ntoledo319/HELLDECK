package com.helldeck.engine

import android.content.Context
import com.helldeck.AppCtx
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.InputStream

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
    val debug: DebugCfg = DebugCfg()
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
        }

    private var roomHeatThresholdCache: Double? = null

    /**
     * Load configuration from assets
     */
    fun load(context: Context = AppCtx.ctx) {
        try {
            val inputStream = context.assets.open("settings/default.yaml")
            loadFromInputStream(inputStream)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load configuration", e)
        }
    }

    /**
     * Load configuration from custom input stream
     */
    fun loadFromInputStream(inputStream: InputStream) {
        try {
            val yaml = Yaml()
            current = yaml.loadAs(inputStream, HelldeckCfg::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse configuration YAML", e)
        }
    }

    /**
     * Load configuration from string
     */
    fun loadFromString(yamlContent: String) {
        try {
            val yaml = Yaml()
            current = yaml.loadAs(yamlContent.byteInputStream(), HelldeckCfg::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse configuration YAML", e)
        }
    }

    /**
     * Get room heat threshold based on current mode
     */
    fun roomHeatThreshold(): Double {
        return roomHeatThresholdCache ?: run {
            val threshold = if (spicyMode) 0.70 else current.scoring.room_heat_threshold
            roomHeatThresholdCache = threshold
            threshold
        }
    }

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
     */
    fun exportAsYaml(): String {
        val yaml = Yaml()
        return yaml.dump(current)
    }
}