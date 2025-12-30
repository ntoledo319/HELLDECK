package com.helldeck.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Session-level metrics tracking for game night analytics
 */
@Entity(tableName = "session_metrics")
data class SessionMetricsEntity(
    @PrimaryKey val sessionId: String,
    val startedAtMs: Long = System.currentTimeMillis(),
    val endedAtMs: Long? = null,
    val totalRounds: Int = 0,
    val gamesPlayed: String = "{}", // JSON map of gameId -> count
    val totalLolCount: Int = 0,
    val totalMehCount: Int = 0,
    val totalTrashCount: Int = 0,
    val participatingPlayers: String = "[]", // JSON list of player IDs
    val durationMs: Long = 0,
)

/**
 * Individual round metrics for detailed analytics
 */
@Entity(
    tableName = "round_metrics",
    foreignKeys = [
        ForeignKey(
            entity = SessionMetricsEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"]), Index(value = ["gameId"])],
)
data class RoundMetricsEntity(
    @PrimaryKey val roundId: String,
    val sessionId: String,
    val gameId: String,
    val cardId: String,
    val cardText: String,
    val activePlayerId: String,
    val lolCount: Int = 0,
    val mehCount: Int = 0,
    val trashCount: Int = 0,
    val points: Int = 0,
    val startedAtMs: Long = System.currentTimeMillis(),
    val completedAtMs: Long? = null,
    val durationMs: Long = 0,
    val spiceLevel: Int = 1,
)

@Dao
interface SessionMetricsDao {
    @Query("SELECT * FROM session_metrics WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): SessionMetricsEntity?

    @Query("SELECT * FROM session_metrics ORDER BY startedAtMs DESC")
    fun getAllSessions(): Flow<List<SessionMetricsEntity>>

    @Query("SELECT * FROM session_metrics ORDER BY startedAtMs DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 10): List<SessionMetricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionMetricsEntity)

    @Query("UPDATE session_metrics SET totalRounds = totalRounds + 1 WHERE sessionId = :sessionId")
    suspend fun incrementRounds(sessionId: String)

    @Query("UPDATE session_metrics SET totalLolCount = totalLolCount + :count WHERE sessionId = :sessionId")
    suspend fun addLolCount(sessionId: String, count: Int)

    @Query("UPDATE session_metrics SET totalMehCount = totalMehCount + :count WHERE sessionId = :sessionId")
    suspend fun addMehCount(sessionId: String, count: Int)

    @Query("UPDATE session_metrics SET totalTrashCount = totalTrashCount + :count WHERE sessionId = :sessionId")
    suspend fun addTrashCount(sessionId: String, count: Int)

    @Query("UPDATE session_metrics SET endedAtMs = :endedAtMs, durationMs = :durationMs WHERE sessionId = :sessionId")
    suspend fun endSession(sessionId: String, endedAtMs: Long, durationMs: Long)

    @Query("DELETE FROM session_metrics WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

@Dao
interface RoundMetricsDao {
    @Query("SELECT * FROM round_metrics WHERE sessionId = :sessionId ORDER BY startedAtMs ASC")
    fun getRoundsForSession(sessionId: String): Flow<List<RoundMetricsEntity>>

    @Query("SELECT * FROM round_metrics WHERE sessionId = :sessionId ORDER BY startedAtMs ASC")
    suspend fun getRoundsForSessionSnapshot(sessionId: String): List<RoundMetricsEntity>

    @Query("SELECT * FROM round_metrics WHERE roundId = :roundId LIMIT 1")
    suspend fun getRound(roundId: String): RoundMetricsEntity?

    @Query("SELECT * FROM round_metrics WHERE gameId = :gameId ORDER BY startedAtMs DESC LIMIT :limit")
    suspend fun getRoundsByGame(gameId: String, limit: Int = 20): List<RoundMetricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(round: RoundMetricsEntity)

    @Query("DELETE FROM round_metrics WHERE sessionId = :sessionId")
    suspend fun deleteRoundsForSession(sessionId: String)

    @Query(
        """
        SELECT AVG(lolCount + mehCount + trashCount) as avgReactions
        FROM round_metrics
        WHERE gameId = :gameId AND (lolCount + mehCount + trashCount) > 0
    """,
    )
    suspend fun getAverageReactionsForGame(gameId: String): Double?

    @Query(
        """
        SELECT AVG(CAST(lolCount AS REAL) / (lolCount + mehCount + trashCount)) as avgLaughScore
        FROM round_metrics
        WHERE gameId = :gameId AND (lolCount + mehCount + trashCount) > 0
    """,
    )
    suspend fun getAverageLaughScoreForGame(gameId: String): Double?
}

/**
 * Computed session analytics for UI display
 */
data class SessionAnalytics(
    val sessionId: String,
    val duration: Long,
    val totalRounds: Int,
    val averageLaughScore: Double,
    val totalReactions: Int,
    val topGame: Pair<String, Int>?, // gameId to round count
    val participantCount: Int,
    val longestRound: Long,
    val shortestRound: Long,
    val heatMoments: Int, // rounds with >70% LOL+TRASH
)

/**
 * Per-game analytics for comparison
 */
data class GameAnalytics(
    val gameId: String,
    val timesPlayed: Int,
    val averageLaughScore: Double,
    val averageDuration: Long,
    val totalLols: Int,
    val totalMehs: Int,
    val totalTrash: Int,
)
