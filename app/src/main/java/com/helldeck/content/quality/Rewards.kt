package com.helldeck.content.quality

/**
 * Rating enum for feedback
 */
enum class Rating {
    LOL, // Laughed out loud - great content
    MEH, // Mediocre - okay but not great
    TRASH, // Bad - don't show again
}

/**
 * Maps user feedback ratings to numeric rewards for learning.
 * Consistent reward mapping ensures predictable content improvement.
 */
object Rewards {

    /**
     * Convert a Rating to a 0.0-1.0 reward value.
     *
     * LOL  = 1.0  (best possible)
     * MEH  = 0.35 (below average but not banned)
     * TRASH = 0.0 (worst possible, effectively bans the template)
     */
    fun rewardFor(rating: Rating): Double = when (rating) {
        Rating.LOL -> 1.0
        Rating.MEH -> 0.35
        Rating.TRASH -> 0.0
    }

    /**
     * Calculate aggregated reward from multiple ratings.
     * Useful for group feedback scenarios.
     */
    fun aggregateReward(ratings: List<Rating>): Double {
        if (ratings.isEmpty()) return 0.5 // Neutral if no feedback
        return ratings.map { rewardFor(it) }.average()
    }

    /**
     * Calculate reward from vote counts (legacy compatibility).
     * Used when feedback is stored as counts instead of individual ratings.
     */
    fun fromCounts(lol: Int, meh: Int, trash: Int): Double {
        val total = lol + meh + trash
        if (total == 0) return 0.5

        val lolWeight = 1.0
        val mehWeight = 0.35
        val trashWeight = 0.0

        val weightedSum = (lol * lolWeight) + (meh * mehWeight) + (trash * trashWeight)
        return (weightedSum / total).coerceIn(0.0, 1.0)
    }

    /**
     * Convert a reward back to suggested rating (for display/debugging).
     */
    fun ratingFromReward(reward: Double): Rating = when {
        reward >= 0.7 -> Rating.LOL
        reward >= 0.2 -> Rating.MEH
        else -> Rating.TRASH
    }
}
