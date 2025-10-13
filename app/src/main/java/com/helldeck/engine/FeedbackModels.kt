package com.helldeck.engine

/**
 * Feedback data class for collecting player reactions
 */
data class Feedback(
    val lol: Int = 0,
    val meh: Int = 0,
    val trash: Int = 0,
    val latencyMs: Int = 0,
    val tags: Set<String> = emptySet(),
    val comments: String = "",
    val intensity: FeedbackIntensity = FeedbackIntensity.NORMAL
)

/**
 * Feedback intensity levels
 */
enum class FeedbackIntensity(val multiplier: Double, val description: String) {
    LOW(0.5, "Subtle"),
    NORMAL(1.0, "Normal"),
    HIGH(1.5, "Enthusiastic"),
    EXTREME(2.0, "Over the top")
}

/**
 * Filled card data class representing a game card with filled slots
 */
data class FilledCard(
    val templateId: String,
    val game: String,
    val text: String,
    val options: List<String> = emptyList(),
    val meta: Map<String, String> = emptyMap(),
    val wordCount: Int = text.split(Regex("\\s+")).size,
    val estimatedReadTimeMs: Int = (wordCount * 200) // Assume 200ms per word
)

/**
 * Round phase enumeration for game state management
 */
enum class RoundPhase(val description: String, val allowsInput: Boolean) {
    IDLE("Waiting to start", false),
    DRAW("Drawing card", false),
    PERFORM("Players performing", true),
    RESOLVE("Resolving results", false),
    FEEDBACK("Collecting feedback", true)
}

/**
 * Round result data class
 */
data class RoundResult(
    val points: Int,
    val judgeWin: Int = 0,
    val roundScore: Double = 0.0,
    val roomHeat: Boolean = false,
    val roomTrash: Boolean = false,
    val streakBonus: Int = 0,
    val fastLaugh: Boolean = false,
    val consensusBonus: Boolean = false
)

/**
 * Vote data class for player votes
 */
data class Vote(
    val voterId: String,
    val targetId: String? = null,
    val choice: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Double = 1.0
)

/**
 * Game interaction data class
 */
data class GameInteraction(
    val gameId: String,
    val interaction: Interaction,
    val playerId: String,
    val action: String,
    val data: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Player action data class
 */
data class PlayerAction(
    val playerId: String,
    val action: PlayerActionType,
    val data: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Player action types
 */
enum class PlayerActionType(val description: String) {
    JOIN_GAME("Joined game"),
    LEAVE_GAME("Left game"),
    VOTE("Cast vote"),
    FEEDBACK("Gave feedback"),
    SKIP("Skipped turn"),
    TIMEOUT("Timed out"),
    RECONNECT("Reconnected")
}

/**
 * Game event data class for logging
 */
data class GameEvent(
    val eventType: GameEventType,
    val gameId: String,
    val playerId: String?,
    val data: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Game event types
 */
enum class GameEventType(val description: String) {
    GAME_STARTED("Game session started"),
    GAME_ENDED("Game session ended"),
    ROUND_STARTED("Round started"),
    ROUND_ENDED("Round ended"),
    PLAYER_JOINED("Player joined"),
    PLAYER_LEFT("Player left"),
    CARD_DRAWN("Card drawn"),
    VOTING_STARTED("Voting started"),
    VOTING_ENDED("Voting ended"),
    FEEDBACK_COLLECTED("Feedback collected"),
    ERROR_OCCURRED("Error occurred"),
    LEARNING_UPDATED("Learning updated"),
    TEMPLATE_PERFORMANCE("Template performance recorded")
}

/**
 * Timer state data class
 */
data class TimerState(
    val durationMs: Int,
    val remainingMs: Int,
    val isRunning: Boolean = false,
    val isWarning: Boolean = false,
    val isCritical: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0) remainingMs.toFloat() / durationMs else 0f

    val isExpired: Boolean
        get() = remainingMs <= 0
}

/**
 * Game statistics data class
 */
data class GameStatistics(
    val totalRounds: Int,
    val totalPlayers: Int,
    val averageRoundTime: Long,
    val averagePointsPerRound: Double,
    val mostPlayedGame: String,
    val highestScore: Int,
    val roomHeatRate: Double,
    val learningProgress: Double
)

/**
 * Template performance data class
 */
data class TemplatePerformance(
    val templateId: String,
    val game: String,
    val totalPlays: Int,
    val averageScore: Double,
    val winRate: Double,
    val lastPlayed: Long,
    val trend: PerformanceTrend
)

/**
 * Performance trend enumeration
 */
enum class PerformanceTrend(val description: String) {
    IMPROVING("Getting better"),
    DECLINING("Getting worse"),
    STABLE("Consistent"),
    VOLATILE("Unpredictable"),
    NEW("Recently added")
}

/**
 * Player statistics data class
 */
data class PlayerStatistics(
    val playerId: String,
    val name: String,
    val totalPoints: Int,
    val roundsPlayed: Int,
    val averagePoints: Double,
    val winRate: Double,
    val favoriteGame: String,
    val lastActive: Long,
    val currentStreak: Int
)

/**
 * Session summary data class
 */
data class SessionSummary(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long?,
    val totalRounds: Int,
    val totalPoints: Int,
    val playerCount: Int,
    val gamesPlayed: List<String>,
    val averageScore: Double,
    val highlights: List<String>
)

/**
 * Validation result data class
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Game configuration validation
 */
fun validateGameConfiguration(): ValidationResult {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    try {
        Config.validate()
    } catch (e: Exception) {
        errors.add("Configuration validation failed: ${e.message}")
    }

    try {
        GameRegistry.validateGameConfiguration()
    } catch (e: Exception) {
        errors.add("Game registry validation failed: ${e.message}")
    }

    // Check for reasonable timer values
    val timers = Config.current.timers
    if (timers.vote_binary_ms < 3000) {
        warnings.add("Vote timers are very short (< 3s)")
    }
    if (timers.vote_avatar_ms < 5000) {
        warnings.add("Avatar vote timers are short (< 5s)")
    }

    // Check for reasonable scoring
    val scoring = Config.current.scoring
    if (scoring.win <= 0) {
        errors.add("Win points must be positive")
    }
    if (scoring.trash_penalty >= 0) {
        errors.add("Trash penalty should be negative")
    }

    return ValidationResult(
        isValid = errors.isEmpty(),
        errors = errors,
        warnings = warnings
    )
}

/**
 * Feedback validation
 */
fun validateFeedback(feedback: Feedback, maxPlayers: Int): ValidationResult {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    // Check vote counts
    val totalVotes = feedback.lol + feedback.meh + feedback.trash
    if (totalVotes > maxPlayers) {
        errors.add("Vote count ($totalVotes) exceeds player count ($maxPlayers)")
    }

    // Check latency
    if (feedback.latencyMs < 0) {
        errors.add("Latency cannot be negative")
    }
    if (feedback.latencyMs > 30000) {
        warnings.add("Very high latency detected (${feedback.latencyMs}ms)")
    }

    // Check tags
    if (feedback.tags.size > 10) {
        warnings.add("Too many feedback tags (${feedback.tags.size})")
    }

    return ValidationResult(
        isValid = errors.isEmpty(),
        errors = errors,
        warnings = warnings
    )
}

/**
 * Calculate feedback intensity based on response time and counts
 */
fun calculateFeedbackIntensity(
    feedback: Feedback,
    baselineLatency: Int = 2000
): FeedbackIntensity {
    val speedBonus = if (feedback.latencyMs < baselineLatency) 1.5 else 1.0
    val volumeBonus = when {
        feedback.lol + feedback.meh + feedback.trash >= 3 -> 1.5
        feedback.lol + feedback.meh + feedback.trash >= 1 -> 1.0
        else -> 0.5
    }

    val intensity = speedBonus * volumeBonus

    return when {
        intensity >= 2.0 -> FeedbackIntensity.EXTREME
        intensity >= 1.5 -> FeedbackIntensity.HIGH
        intensity >= 0.8 -> FeedbackIntensity.NORMAL
        else -> FeedbackIntensity.LOW
    }
}

/**
 * Merge multiple feedback instances
 */
fun mergeFeedback(feedbacks: List<Feedback>): Feedback {
    if (feedbacks.isEmpty()) return Feedback()

    val totalLol = feedbacks.sumOf { it.lol }
    val totalMeh = feedbacks.sumOf { it.meh }
    val totalTrash = feedbacks.sumOf { it.trash }
    val avgLatency = feedbacks.map { it.latencyMs }.average().toInt()
    val allTags = feedbacks.flatMap { it.tags }.toSet()

    return Feedback(
        lol = totalLol,
        meh = totalMeh,
        trash = totalTrash,
        latencyMs = avgLatency,
        tags = allTags
    )
}

/**
 * Calculate room consensus from feedback
 */
fun calculateRoomConsensus(feedback: Feedback, totalPlayers: Int): Double {
    if (totalPlayers == 0) return 0.0

    val maxVote = maxOf(feedback.lol, feedback.meh, feedback.trash)
    return maxVote.toDouble() / totalPlayers
}

/**
 * Determine if feedback indicates room heat
 */
fun isRoomHeat(feedback: Feedback, totalPlayers: Int, threshold: Double = 0.6): Boolean {
    if (totalPlayers == 0) return false
    return (feedback.lol.toDouble() / totalPlayers) >= threshold
}

/**
 * Determine if feedback indicates room trash
 */
fun isRoomTrash(feedback: Feedback, totalPlayers: Int, threshold: Double = 0.6): Boolean {
    if (totalPlayers == 0) return false
    return (feedback.trash.toDouble() / totalPlayers) >= threshold
}