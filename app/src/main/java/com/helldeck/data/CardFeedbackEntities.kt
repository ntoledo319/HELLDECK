package com.helldeck.data

import androidx.room.*

/**
 * Tracks every card impression (showing) during gameplay.
 * Used to collect implicit and explicit feedback signals.
 */
@Entity(
    tableName = "card_impressions",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["cardId"]),
        Index(value = ["gameId"]),
    ],
)
data class CardImpressionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cardId: String,
    val gameId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasSkipped: Boolean = false,
    val roundCompleted: Boolean = false,
    val quickFire: Boolean = false,
    val wasMvp: Boolean = false,
    val wasDud: Boolean = false,
)

/**
 * Aggregated quality scores per card.
 * Computed from card_impressions data.
 */
@Entity(
    tableName = "card_scores",
    indices = [Index(value = ["gameId"])],
)
data class CardScoreEntity(
    @PrimaryKey val cardId: String,
    val gameId: String,
    val impressions: Int = 0,
    val skips: Int = 0,
    val completions: Int = 0,
    val quickFires: Int = 0,
    val mvpVotes: Int = 0,
    val dudFlags: Int = 0,
    val computedScore: Float = 0.5f,
    val lastUpdated: Long = System.currentTimeMillis(),
)

@Dao
interface CardFeedbackDao {
    
    // ========== IMPRESSIONS ==========
    
    @Insert
    suspend fun insertImpression(impression: CardImpressionEntity): Long
    
    @Query("UPDATE card_impressions SET wasSkipped = 1 WHERE id = :impressionId")
    suspend fun markSkipped(impressionId: Long)
    
    @Query("UPDATE card_impressions SET roundCompleted = 1 WHERE id = :impressionId")
    suspend fun markRoundCompleted(impressionId: Long)
    
    @Query("UPDATE card_impressions SET quickFire = 1 WHERE id = :impressionId")
    suspend fun markQuickFire(impressionId: Long)
    
    @Query("UPDATE card_impressions SET wasMvp = 1 WHERE sessionId = :sessionId AND cardId = :cardId")
    suspend fun markMvp(sessionId: Long, cardId: String)
    
    @Query("UPDATE card_impressions SET wasDud = 1 WHERE sessionId = :sessionId AND cardId = :cardId")
    suspend fun markDud(sessionId: Long, cardId: String)
    
    @Query("SELECT * FROM card_impressions WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSessionCards(sessionId: Long): List<CardImpressionEntity>
    
    @Query("SELECT DISTINCT cardId FROM card_impressions WHERE sessionId = :sessionId")
    suspend fun getSessionCardIds(sessionId: Long): List<String>
    
    // ========== SCORES ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScore(score: CardScoreEntity)
    
    @Query("SELECT * FROM card_scores WHERE cardId = :cardId LIMIT 1")
    suspend fun getCardScore(cardId: String): CardScoreEntity?
    
    @Query("SELECT * FROM card_scores WHERE gameId = :gameId ORDER BY computedScore DESC")
    suspend fun getScoresByGame(gameId: String): List<CardScoreEntity>
    
    @Query("SELECT * FROM card_scores WHERE computedScore < :threshold")
    suspend fun getLowScoringCards(threshold: Float = 0.3f): List<CardScoreEntity>
    
    @Query("SELECT * FROM card_scores WHERE dudFlags >= :minFlags")
    suspend fun getFlaggedCards(minFlags: Int = 3): List<CardScoreEntity>
    
    // ========== AGGREGATION ==========
    
    @Query("""
        SELECT cardId, gameId,
            COUNT(*) as impressions,
            SUM(CASE WHEN wasSkipped = 1 THEN 1 ELSE 0 END) as skips,
            SUM(CASE WHEN roundCompleted = 1 THEN 1 ELSE 0 END) as completions,
            SUM(CASE WHEN quickFire = 1 THEN 1 ELSE 0 END) as quickFires,
            SUM(CASE WHEN wasMvp = 1 THEN 1 ELSE 0 END) as mvpVotes,
            SUM(CASE WHEN wasDud = 1 THEN 1 ELSE 0 END) as dudFlags
        FROM card_impressions
        WHERE cardId = :cardId
        GROUP BY cardId
    """)
    suspend fun getAggregatedStats(cardId: String): CardAggregateStats?
    
    @Query("SELECT DISTINCT cardId FROM card_impressions")
    suspend fun getAllTrackedCardIds(): List<String>
}

/**
 * Intermediate data class for aggregation query results.
 */
data class CardAggregateStats(
    val cardId: String,
    val gameId: String,
    val impressions: Int,
    val skips: Int,
    val completions: Int,
    val quickFires: Int,
    val mvpVotes: Int,
    val dudFlags: Int,
)

/**
 * Utility object for computing card quality scores.
 */
object CardScoreCalculator {
    
    /**
     * Compute quality score from aggregate stats.
     * Returns 0.0-1.0 score.
     */
    fun compute(stats: CardAggregateStats): Float {
        if (stats.impressions == 0) return 0.5f
        
        val raw = (
            (stats.mvpVotes * 1.0f) +
            (stats.quickFires * 0.2f) +
            (stats.completions * 0.1f) +
            (stats.impressions * 0.05f) -
            (stats.skips * 0.3f) -
            (stats.dudFlags * 1.0f)
        ) / stats.impressions
        
        return raw.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Create a CardScoreEntity from aggregate stats.
     */
    fun toScoreEntity(stats: CardAggregateStats): CardScoreEntity {
        return CardScoreEntity(
            cardId = stats.cardId,
            gameId = stats.gameId,
            impressions = stats.impressions,
            skips = stats.skips,
            completions = stats.completions,
            quickFires = stats.quickFires,
            mvpVotes = stats.mvpVotes,
            dudFlags = stats.dudFlags,
            computedScore = compute(stats),
            lastUpdated = System.currentTimeMillis(),
        )
    }
}
