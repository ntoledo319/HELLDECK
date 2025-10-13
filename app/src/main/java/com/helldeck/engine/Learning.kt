package com.helldeck.engine

import com.helldeck.data.TemplateEntity
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Learning system for HELLDECK
 * Adapts to player preferences and improves content selection over time
 */
object Learning {

    /**
     * Calculate score for a card based on feedback
     */
    fun scoreCard(
        lol: Int,
        trash: Int,
        judgeWin: Boolean,
        fastLaugh: Boolean,
        streakBonus: Int,
        roomHeat: Boolean = false,
        roomTrash: Boolean = false
    ): Double {
        val config = Config.current.scoring
        var score = 0.0

        // Base scoring
        score += 2.0 * lol
        score += config.trash_penalty * trash

        // Judge win bonus
        if (judgeWin) score += config.judge_bonus

        // Fast laugh bonus
        if (fastLaugh) score += config.fast_laugh_bonus

        // Streak bonus (capped)
        score += min(streakBonus, config.streak_cap)

        // Room heat bonus
        if (roomHeat) score += config.room_heat_bonus

        // Room consensus bonus
        if (roomHeat && lol > 0) score += config.consensus_bonus

        // Room trash penalty (additional)
        if (roomTrash) score += config.trash_penalty

        return score
    }

    /**
     * Update template score using exponential moving average
     */
    fun updateTemplateScore(
        currentScore: Double,
        newScore: Double,
        alpha: Double = Config.current.learning.alpha
    ): Double {
        return alpha * newScore + (1 - alpha) * currentScore
    }

    /**
     * Calculate diversity bonus to encourage variety
     */
    fun calculateDiversityBonus(
        template: TemplateEntity,
        recentFamilies: List<String>,
        diversityWindow: Int = Config.current.learning.diversity_window
    ): Double {
        val familyRecency = recentFamilies.indexOf(template.family)
        if (familyRecency == -1) return 0.0

        // Bonus decreases as family recency increases
        val recencyFactor = 1.0 - (familyRecency.toDouble() / diversityWindow)
        return recencyFactor * 0.5 // Max 0.5 bonus
    }

    /**
     * Calculate exploration bonus for less-played templates
     */
    fun calculateExplorationBonus(
        draws: Int,
        minPlays: Int = Config.current.learning.min_plays_before_learning
    ): Double {
        if (draws < minPlays) {
            // Encourage exploration of new templates
            return 2.0 * (minPlays - draws).toDouble() / minPlays
        }

        // Slight bonus for templates with fewer plays
        return max(0.0, 1.0 - (draws.toDouble() / 100.0))
    }

    /**
     * Calculate contextual score for template selection
     */
    fun calculateSelectionScore(
        template: TemplateEntity,
        recentFamilies: List<String>,
        roundIdx: Int,
        baseScoreMultiplier: Double = 1.0
    ): Double {
        val config = Config.current.learning

        // Base score from historical performance
        var score = template.score * baseScoreMultiplier

        // Add diversity bonus
        val diversityBonus = calculateDiversityBonus(template, recentFamilies)
        score += diversityBonus

        // Add exploration bonus
        val explorationBonus = calculateExplorationBonus(template.draws)
        score += explorationBonus

        // Add recency bonus (prefer templates not played recently)
        val recencyBonus = if (template.lastPlayTs > 0) {
            val hoursSinceLastPlay = (System.currentTimeMillis() - template.lastPlayTs) / (1000.0 * 3600.0)
            min(1.0, hoursSinceLastPlay / 24.0) // Max bonus after 24 hours
        } else {
            1.0 // Never played bonus
        }
        score += recencyBonus * 0.3

        return score
    }

    /**
     * Update template learning metrics after a round
     */
    fun updateTemplateAfterRound(
        template: TemplateEntity,
        roundScore: Double,
        wasJudgeWin: Boolean,
        wasRoomHeat: Boolean
    ): TemplateEntity {
        val config = Config.current.learning

        // Update score using EMA
        val newScore = updateTemplateScore(template.score, roundScore)

        // Update play statistics
        val newDraws = template.draws + 1
        val newWins = if (wasJudgeWin) template.wins + 1 else template.wins
        val newLastPlayTs = System.currentTimeMillis()

        // Calculate win rate
        val winRate = if (newDraws > 0) newWins.toDouble() / newDraws else 0.0

        return template.copy(
            score = newScore,
            draws = newDraws,
            wins = newWins,
            lastPlayTs = newLastPlayTs
        )
    }

    /**
     * Calculate player skill rating using Elo system
     */
    fun calculateEloChange(
        playerElo: Int,
        opponentElos: List<Int>,
        playerWon: Boolean,
        kFactor: Int = 32
    ): Int {
        if (opponentElos.isEmpty()) return 0

        val averageOpponentElo = opponentElos.average()
        val expectedScore = 1.0 / (1.0 + exp((averageOpponentElo - playerElo) / 400.0))

        val actualScore = if (playerWon) 1.0 else 0.0

        return (kFactor * (actualScore - expectedScore)).toInt()
    }

    /**
     * Calculate session statistics for learning insights
     */
    fun calculateSessionInsights(
        rounds: List<com.helldeck.data.RoundEntity>,
        players: List<com.helldeck.data.PlayerEntity>
    ): Map<String, Any> {
        if (rounds.isEmpty()) return emptyMap()

        val gameStats = rounds.groupBy { it.game }.mapValues { (_, gameRounds) ->
            mapOf(
                "count" to gameRounds.size,
                "averagePoints" to gameRounds.map { it.points }.average(),
                "averageLol" to gameRounds.map { it.lol }.average(),
                "averageTrash" to gameRounds.map { it.trash }.average(),
                "judgeWinRate" to gameRounds.count { it.judgeWin > 0 }.toDouble() / gameRounds.size
            )
        }

        val playerStats = players.associate { player ->
            val playerRounds = rounds // In a real implementation, you'd filter by player
            player.id to mapOf(
                "totalPoints" to player.sessionPoints,
                "averagePoints" to if (playerRounds.isNotEmpty()) playerRounds.map { it.points }.average() else 0.0,
                "participationRate" to 1.0 // Would calculate based on actual participation
            )
        }

        val timeStats = mapOf(
            "totalDuration" to if (rounds.size >= 2) {
                rounds.last().ts - rounds.first().ts
            } else 0L,
            "averageRoundDuration" to rounds.map { it.latencyMs }.average(),
            "fastestRound" to rounds.minOf { it.latencyMs },
            "slowestRound" to rounds.maxOf { it.latencyMs }
        )

        return mapOf(
            "gameStats" to gameStats,
            "playerStats" to playerStats,
            "timeStats" to timeStats,
            "overallStats" to mapOf(
                "totalRounds" to rounds.size,
                "averagePoints" to rounds.map { it.points }.average(),
                "averageLol" to rounds.map { it.lol }.average(),
                "averageTrash" to rounds.map { it.trash }.average(),
                "judgeWinRate" to rounds.count { it.judgeWin > 0 }.toDouble() / rounds.size,
                "roomHeatRate" to rounds.count { it.roomHeat > 0.6 }.toDouble() / rounds.size
            )
        )
    }

    /**
     * Detect patterns in player feedback
     */
    fun detectFeedbackPatterns(rounds: List<com.helldeck.data.RoundEntity>): Map<String, Any> {
        val patterns = mutableMapOf<String, Any>()

        // Game preference patterns
        val gamePreferences = rounds.groupBy { it.game }.mapValues { (_, gameRounds) ->
            val avgLol = gameRounds.map { it.lol }.average()
            val avgTrash = gameRounds.map { it.trash }.average()
            mapOf(
                "averageLol" to avgLol,
                "averageTrash" to avgTrash,
                "preferenceScore" to avgLol - avgTrash
            )
        }

        // Time-based patterns
        val timePatterns = rounds.groupBy {
            val hour = (it.ts / (1000 * 60 * 60)) % 24
            when {
                hour in 0..5 -> "late_night"
                hour in 6..11 -> "morning"
                hour in 12..17 -> "afternoon"
                hour in 18..23 -> "evening"
                else -> "unknown"
            }
        }.mapValues { (_, timeRounds) ->
            mapOf(
                "count" to timeRounds.size,
                "averageLol" to timeRounds.map { it.lol }.average(),
                "averageLatency" to timeRounds.map { it.latencyMs }.average()
            )
        }

        // Streak patterns
        var currentStreak = 0
        var maxStreak = 0
        var currentStreakType = ""

        rounds.sortedBy { it.ts }.forEach { round ->
            val isPositive = round.lol > round.trash
            val streakType = if (isPositive) "positive" else "negative"

            if (streakType == currentStreakType) {
                currentStreak++
                maxStreak = max(maxStreak, currentStreak)
            } else {
                currentStreak = 1
                currentStreakType = streakType
            }
        }

        patterns["gamePreferences"] = gamePreferences
        patterns["timePatterns"] = timePatterns
        patterns["streakInfo"] = mapOf(
            "maxStreak" to maxStreak,
            "currentStreak" to currentStreak,
            "currentStreakType" to currentStreakType
        )

        return patterns
    }

    /**
     * Generate learning recommendations
     */
    fun generateRecommendations(
        templates: List<TemplateEntity>,
        rounds: List<com.helldeck.data.RoundEntity>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Check for overplayed templates
        val overplayedThreshold = 50
        val overplayed = templates.filter { it.draws > overplayedThreshold }
        if (overplayed.isNotEmpty()) {
            recommendations.add("Consider adding new templates - ${overplayed.size} templates have been played $overplayedThreshold+ times")
        }

        // Check for underperforming templates
        val underperformers = templates.filter { it.draws > 5 && it.score < 1.0 }
        if (underperformers.isNotEmpty()) {
            recommendations.add("${underperformers.size} templates are underperforming (score < 1.0 after 5+ plays)")
        }

        // Check for stale templates
        val staleThreshold = 7 * 24 * 60 * 60 * 1000L // 7 days
        val stale = templates.filter { it.draws > 0 && (System.currentTimeMillis() - it.lastPlayTs) > staleThreshold }
        if (stale.isNotEmpty()) {
            recommendations.add("${stale.size} templates haven't been played in over a week")
        }

        // Check for learning effectiveness
        val recentRounds = rounds.takeLast(20)
        if (recentRounds.size >= 10) {
            val recentAverageScore = recentRounds.map { it.points }.average()
            val overallAverageScore = rounds.map { it.points }.average()

            if (recentAverageScore > overallAverageScore * 1.1) {
                recommendations.add("Learning is working well - recent rounds score 10% higher on average")
            } else if (recentAverageScore < overallAverageScore * 0.9) {
                recommendations.add("Consider adjusting learning parameters - recent rounds score 10% lower on average")
            }
        }

        return recommendations
    }
}