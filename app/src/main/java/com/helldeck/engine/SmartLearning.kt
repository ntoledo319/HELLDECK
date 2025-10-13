package com.helldeck.engine


import android.os.VibrationEffect
import com.helldeck.data.TemplateEntity
import com.helldeck.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Smart learning system for HELLDECK
 * Epsilon-greedy selection with EMA scoring for old devices
 */
object SmartLearning {

    private val templateScores = mutableMapOf<String, TemplateScore>()
    private val recentFamilies = mutableListOf<String>()
    private var totalRounds = 0
    private var random = Random(System.currentTimeMillis())

    /**
     * Epsilon-greedy template selection
     */
    suspend fun selectTemplate(
        candidates: List<TemplateEntity>,
        gameType: String
    ): TemplateEntity = withContext(Dispatchers.Default) {
        if (candidates.isEmpty()) {
            throw IllegalArgumentException("No template candidates available")
        }

        totalRounds++

        // Calculate epsilon for exploration vs exploitation
        val epsilon = calculateEpsilon(totalRounds)

        return@withContext if (random.nextDouble() < epsilon) {
            // Exploration: pick random template
            Logger.d("Exploration: picking random template (epsilon: ${epsilon})")
            candidates.random(random)
        } else {
            // Exploitation: pick best template
            Logger.d("Exploitation: picking best template (epsilon: ${epsilon})")
            selectBestTemplate(candidates, gameType)
        }
    }

    /**
     * Calculate epsilon for epsilon-greedy algorithm
     */
    private fun calculateEpsilon(round: Int): Double {
        // Epsilon decreases over time: starts at 0.25, decays to 0.05
        val epsilonStart = 0.25
        val epsilonEnd = 0.05
        val decayRounds = 50.0

        val progress = min(1.0, round / decayRounds)
        return epsilonStart + (epsilonEnd - epsilonStart) * progress
    }

    /**
     * Select best template based on current scores
     */
    private fun selectBestTemplate(candidates: List<TemplateEntity>, gameType: String): TemplateEntity {
        var bestTemplate = candidates.first()
        var bestScore = Double.NEGATIVE_INFINITY

        for (template in candidates) {
            val score = calculateTemplateScore(template, gameType)

            if (score > bestScore) {
                bestScore = score
                bestTemplate = template
            }
        }

        return bestTemplate
    }

    /**
     * Calculate score for a template
     */
    private fun calculateTemplateScore(template: TemplateEntity, gameType: String): Double {
        val baseScore = getTemplateScore(template.id)

        // Add diversity bonus to avoid repetition
        val diversityBonus = calculateDiversityBonus(template.family)

        // Add recency bonus for templates not played recently
        val recencyBonus = calculateRecencyBonus(template.lastPlayTs)

        // Add exploration bonus for less-played templates
        val explorationBonus = calculateExplorationBonus(template.draws)

        return baseScore + diversityBonus + recencyBonus + explorationBonus
    }

    /**
     * Get current score for a template
     */
    private fun getTemplateScore(templateId: String): Double {
        return templateScores[templateId]?.emaScore ?: 1.0 // Default score
    }

    /**
     * Update template score using EMA (Exponential Moving Average)
     */
    fun updateTemplateScore(templateId: String, newScore: Double) {
        val currentScore = templateScores[templateId]?.emaScore ?: 1.0
        val alpha = 0.3 // Learning rate

        val updatedScore = alpha * newScore + (1 - alpha) * currentScore

        templateScores[templateId] = TemplateScore(
            templateId = templateId,
            emaScore = updatedScore,
            lastUpdated = System.currentTimeMillis()
        )

        Logger.d("Updated template score: $templateId = $updatedScore")
    }

    /**
     * Calculate diversity bonus to avoid family repetition
     */
    private fun calculateDiversityBonus(family: String): Double {
        val familyRecency = recentFamilies.indexOf(family)
        if (familyRecency == -1) return 0.0

        // Bonus decreases as family recency increases
        val recencyFactor = 1.0 - (familyRecency.toDouble() / recentFamilies.size)
        return recencyFactor * 0.3 // Max 0.3 bonus
    }

    /**
     * Calculate recency bonus for templates not played recently
     */
    private fun calculateRecencyBonus(lastPlayTs: Long): Double {
        if (lastPlayTs == 0L) return 0.5 // Never played bonus

        val hoursSinceLastPlay = (System.currentTimeMillis() - lastPlayTs) / (1000.0 * 3600.0)
        return min(0.3, hoursSinceLastPlay / 24.0) // Max bonus after 24 hours
    }

    /**
     * Calculate exploration bonus for less-played templates
     */
    private fun calculateExplorationBonus(draws: Int): Double {
        if (draws < 3) {
            // Encourage exploration of new templates
            return 0.5 * (3 - draws).toDouble() / 3
        }

        // Slight bonus for templates with fewer plays
        return max(0.0, 0.2 - (draws.toDouble() / 100.0))
    }

    /**
     * Record template usage for diversity tracking
     */
    fun recordTemplateUsage(template: TemplateEntity) {
        // Add to recent families for diversity
        recentFamilies.add(0, template.family)
        if (recentFamilies.size > 10) {
            recentFamilies.removeAt(recentFamilies.lastIndex)
        }

        Logger.d("Recorded template usage: ${template.id}, family: ${template.family}")
    }

    /**
     * Process feedback and update learning
     */
    fun processFeedback(templateId: String, feedback: Feedback, gameResult: GameResult) {
        // Calculate score based on feedback and game result
        val score = calculateFeedbackScore(feedback, gameResult)

        // Update template score using EMA
        updateTemplateScore(templateId, score)

        Logger.d("Processed feedback for template $templateId: score = $score")
    }

    /**
     * Calculate score from feedback and game result
     */
    private fun calculateFeedbackScore(feedback: Feedback, gameResult: GameResult): Double {
        var score = 0.0

        // Base score from feedback
        score += 2.0 * feedback.lol      // LOL reactions
        score += 0.5 * feedback.meh      // MEH reactions
        score -= 1.0 * feedback.trash    // TRASH reactions

        // Game result bonuses
        if (gameResult.roomHeat) score += 1.0    // Room consensus bonus
        if (gameResult.judgeWin) score += 0.5    // Judge win bonus
        if (gameResult.fastLaugh) score += 0.3   // Fast laugh bonus

        // Streak bonus
        score += min(gameResult.streakBonus, 3) * 0.2

        return score
    }

    /**
     * Get learning statistics
     */
    fun getLearningStats(): LearningStats {
        val scores = templateScores.values.map { it.emaScore }

        return LearningStats(
            totalTemplates = templateScores.size,
            averageScore = if (scores.isNotEmpty()) scores.average() else 0.0,
            bestTemplate = templateScores.maxByOrNull { it.value.emaScore }?.key,
            worstTemplate = templateScores.minByOrNull { it.value.emaScore }?.key,
            totalRounds = totalRounds,
            currentEpsilon = calculateEpsilon(totalRounds),
            recentFamilies = recentFamilies.toList()
        )
    }

    /**
     * Reset learning data
     */
    fun resetLearning() {
        templateScores.clear()
        recentFamilies.clear()
        totalRounds = 0
        Logger.i("Learning data reset")
    }

    /**
     * Export learning data for brainpack
     */
    fun exportLearningData(): Map<String, Any> {
        return mapOf(
            "templateScores" to templateScores.map { (id, score) ->
                mapOf(
                    "templateId" to id,
                    "emaScore" to score.emaScore,
                    "lastUpdated" to score.lastUpdated
                )
            },
            "totalRounds" to totalRounds,
            "recentFamilies" to recentFamilies,
            "exportTime" to System.currentTimeMillis()
        )
    }

    /**
     * Import learning data from brainpack
     */
    fun importLearningData(data: Map<String, Any>) {
        try {
            @Suppress("UNCHECKED_CAST")
            val scoresData = data["templateScores"] as? List<Map<String, Any>> ?: emptyList()

            templateScores.clear()
            scoresData.forEach { scoreData ->
                val templateId = scoreData["templateId"] as String
                val emaScore = (scoreData["emaScore"] as Number).toDouble()
                val lastUpdated = (scoreData["lastUpdated"] as Number).toLong()

                templateScores[templateId] = TemplateScore(
                    templateId = templateId,
                    emaScore = emaScore,
                    lastUpdated = lastUpdated
                )
            }

            totalRounds = (data["totalRounds"] as? Number)?.toInt() ?: 0

            @Suppress("UNCHECKED_CAST")
            recentFamilies.clear()
            recentFamilies.addAll((data["recentFamilies"] as? List<String>) ?: emptyList())

            Logger.i("Imported learning data: ${templateScores.size} templates")
        } catch (e: Exception) {
            Logger.e("Failed to import learning data", e)
        }
    }
}

/**
 * Template score data class
 */
data class TemplateScore(
    val templateId: String,
    val emaScore: Double,
    val lastUpdated: Long
)

/**
 * Game result data class
 */
data class GameResult(
    val judgeWin: Boolean = false,
    val roomHeat: Boolean = false,
    val roomTrash: Boolean = false,
    val fastLaugh: Boolean = false,
    val streakBonus: Int = 0
)

/**
 * Learning statistics
 */
data class LearningStats(
    val totalTemplates: Int,
    val averageScore: Double,
    val bestTemplate: String?,
    val worstTemplate: String?,
    val totalRounds: Int,
    val currentEpsilon: Double,
    val recentFamilies: List<String>
)

/**
 * Symmetric scoring system for HELLDECK
 */
object SymmetricScoring {

    /**
     * Calculate score for a round
     */
    fun calculateRoundScore(
        feedback: Feedback,
        gameResult: GameResult,
        playerCount: Int
    ): RoundScore {
        var points = 0
        var bonuses = 0

        // Base win points
        if (gameResult.judgeWin) {
            points += 2
        }

        // Room heat bonus (≥60% consensus)
        val lolPercentage = if (playerCount > 0) feedback.lol.toDouble() / playerCount else 0.0
        val isRoomHeat = lolPercentage >= 0.60

        if (isRoomHeat) {
            points += 1
            bonuses += 1
        }

        // Room trash penalty (≥60% consensus)
        val trashPercentage = if (playerCount > 0) feedback.trash.toDouble() / playerCount else 0.0
        val isRoomTrash = trashPercentage >= 0.60

        if (isRoomTrash) {
            points -= 2
        }

        // Fast laugh bonus (<1.2 seconds)
        if (gameResult.fastLaugh && feedback.lol > 0) {
            points += 1
            bonuses += 1
        }

        // Streak bonus (capped at 3)
        val streakPoints = min(gameResult.streakBonus, 3)
        points += streakPoints

        return RoundScore(
            totalPoints = points,
            basePoints = if (gameResult.judgeWin) 2 else 0,
            roomHeatBonus = if (isRoomHeat) 1 else 0,
            roomTrashPenalty = if (isRoomTrash) 2 else 0,
            fastLaughBonus = if (gameResult.fastLaugh) 1 else 0,
            streakBonus = streakPoints,
            totalBonuses = bonuses,
            lolPercentage = lolPercentage,
            trashPercentage = trashPercentage,
            isRoomHeat = isRoomHeat,
            isRoomTrash = isRoomTrash
        )
    }

    /**
     * Check if last place player picks next game
     */
    fun shouldLastPlacePickNext(players: List<com.helldeck.data.PlayerEntity>): Boolean {
        if (players.size < 3) return false // Need at least 3 players

        val sortedPlayers = players.sortedBy { it.sessionPoints }
        val lastPlacePlayers = sortedPlayers.filter { it.sessionPoints == sortedPlayers.first().sessionPoints }

        return lastPlacePlayers.size == 1 // Only one player in last place
    }

    /**
     * Get last place players
     */
    fun getLastPlacePlayers(players: List<com.helldeck.data.PlayerEntity>): List<com.helldeck.data.PlayerEntity> {
        if (players.isEmpty()) return emptyList()

        val minPoints = players.minOf { it.sessionPoints }
        return players.filter { it.sessionPoints == minPoints }
    }

    /**
     * Calculate tie-breaking strategy
     */
    fun calculateTieBreaker(
        tiedPlayers: List<com.helldeck.data.PlayerEntity>,
        gameType: String
    ): TieBreakStrategy {
        return when {
            tiedPlayers.size <= 2 -> TieBreakStrategy.REVOTE
            tiedPlayers.size <= 4 -> TieBreakStrategy.MICRO_DUEL
            else -> TieBreakStrategy.TEAM_VOTE
        }
    }

    /**
     * Get game mode recommendations based on player count
     */
    fun getGameModeRecommendations(playerCount: Int): List<String> {
        return when {
            playerCount < 3 -> listOf("Need at least 3 players for optimal experience")
            playerCount in 3..7 -> listOf("Individual play mode recommended")
            playerCount in 8..16 -> listOf("Team mode recommended", "Consider splitting into 2 teams")
            else -> listOf("Team mode required", "Split into teams of 4-5 players each")
        }
    }
}

/**
 * Round score data class
 */
data class RoundScore(
    val totalPoints: Int,
    val basePoints: Int,
    val roomHeatBonus: Int,
    val roomTrashPenalty: Int,
    val fastLaughBonus: Int,
    val streakBonus: Int,
    val totalBonuses: Int,
    val lolPercentage: Double,
    val trashPercentage: Double,
    val isRoomHeat: Boolean,
    val isRoomTrash: Boolean
)

/**
 * Tie-breaking strategies
 */
enum class TieBreakStrategy {
    REVOTE,      // Quick revote (3 seconds)
    MICRO_DUEL,  // Mini-game duel
    TEAM_VOTE    // Team-based voting
}

/**
 * Performance-optimized collections for old devices
 */
object OptimizedCollections {

    /**
     * Memory-efficient list for large player counts
     */
    fun <T> createOptimizedList(initialCapacity: Int = 16): MutableList<T> {
        return when (com.helldeck.utils.MemoryOptimizer.getMemoryStrategy()) {
            com.helldeck.utils.MemoryStrategy.AGGRESSIVE -> ArrayList(initialCapacity / 2)
            com.helldeck.utils.MemoryStrategy.MODERATE -> ArrayList(initialCapacity)
            com.helldeck.utils.MemoryStrategy.CONSERVATIVE -> ArrayList(initialCapacity * 2)
        }
    }

    /**
     * Memory-efficient map for caching
     */
    fun <K, V> createOptimizedMap(initialCapacity: Int = 16): MutableMap<K, V> {
        return when (com.helldeck.utils.MemoryOptimizer.getMemoryStrategy()) {
            com.helldeck.utils.MemoryStrategy.AGGRESSIVE -> HashMap(initialCapacity / 2)
            com.helldeck.utils.MemoryStrategy.MODERATE -> HashMap(initialCapacity)
            com.helldeck.utils.MemoryStrategy.CONSERVATIVE -> HashMap(initialCapacity * 2)
        }
    }

    /**
     * Circular buffer for recent items (memory efficient)
     */
    class CircularBuffer<T>(private val capacity: Int) {
        private val buffer = mutableListOf<T>()
        private var index = 0

        fun add(item: T) {
            if (buffer.size < capacity) {
                buffer.add(item)
            } else {
                buffer[index] = item
                index = (index + 1) % capacity
            }
        }

        fun toList(): List<T> = buffer.toList()

        fun size(): Int = buffer.size

        fun clear() {
            buffer.clear()
            index = 0
        }
    }
}

/**
 * Battery-efficient operations for extended play
 */
object BatteryEfficientOps {

    private var batteryOptimizationEnabled = true

    /**
     * Enable battery optimization
     */
    fun enableBatteryOptimization() {
        batteryOptimizationEnabled = true
        Logger.d("Battery optimization enabled")
    }

    /**
     * Disable battery optimization
     */
    fun disableBatteryOptimization() {
        batteryOptimizationEnabled = false
        Logger.d("Battery optimization disabled")
    }

    /**
     * Execute operation with battery optimization
     */
    suspend fun <T> withBatteryOptimization(block: suspend () -> T): T {
        if (!batteryOptimizationEnabled) {
            return block()
        }

        // Reduce haptic feedback intensity
        val originalIntensity = VibrationEffect.DEFAULT_AMPLITUDE

        try {
            return block()
        } finally {
            // Restore original intensity
            // Note: HapticsTorch intensity restoration handled by system
        }
    }

    /**
     * Get battery optimization tips
     */
    fun getBatteryTips(): List<String> {
        return listOf(
            "Disable haptic feedback in system settings for longer battery life",
            "Reduce screen brightness for extended play sessions",
            "Close other apps to conserve battery",
            "Consider using airplane mode if WiFi is not needed",
            "Take breaks during long play sessions to cool down the device"
        )
    }
}

/**
 * Instant feedback system for <100ms response time
 */
object InstantFeedback {

    private const val MAX_RESPONSE_TIME = 100L // 100ms requirement

    /**
     * Record operation timing
     */
    fun recordOperationTiming(operation: String, duration: Long) {
        if (duration > MAX_RESPONSE_TIME) {
            Logger.w("Slow operation detected: $operation took ${duration}ms (target: ${MAX_RESPONSE_TIME}ms)")
        } else {
            Logger.d("Fast operation: $operation took ${duration}ms")
        }
    }

    /**
     * Measure operation timing
     */
    suspend fun <T> measureTiming(
        operation: String,
        block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordOperationTiming(operation, duration)
        }
    }

    /**
     * Get performance recommendations
     */
    fun getPerformanceRecommendations(): List<String> {
        return listOf(
            "All UI operations should respond within 100ms",
            "Use lightweight data structures for better performance",
            "Minimize database queries during gameplay",
            "Cache frequently accessed data",
            "Use background threads for heavy operations"
        )
    }
}

/**
 * APK size optimization for old devices
 */
object APKSizeOptimizer {

    /**
     * Get APK size recommendations
     */
    fun getSizeRecommendations(): List<String> {
        return listOf(
            "Target APK size: <15MB for old devices",
            "Use vector drawables instead of PNGs",
            "Minimize native libraries",
            "Compress assets aggressively",
            "Remove unused resources",
            "Use ProGuard/R8 for code shrinking"
        )
    }

    /**
     * Estimate APK size impact of features
     */
    fun estimateSizeImpact(feature: String): String {
        return when (feature) {
            "analytics" -> "~50KB"
            "crash_reporting" -> "~100KB"
            "additional_game_modes" -> "~200KB per mode"
            "high_res_assets" -> "~2MB"
            "native_libraries" -> "~1MB+"
            else -> "Unknown"
        }
    }
}