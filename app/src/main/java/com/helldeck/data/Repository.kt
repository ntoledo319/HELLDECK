package com.helldeck.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.withTransaction
import com.helldeck.content.db.HelldeckDb
import com.helldeck.engine.Feedback
import com.helldeck.engine.SessionSummary
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository class providing high-level database operations for game sessions.
 *
 * This class wraps the Room database DAOs to provide a simpler API for
 * managing game sessions, rounds, and players.
 */
class Repository private constructor(private val db: HelldeckDb) {

    // Public APIs retain Long IDs while Room stores collision-resistant UUIDs.
    private val sessionIdMap = ConcurrentHashMap<Long, String>()

    companion object {
        @Volatile
        private var INSTANCE: Repository? = null

        fun get(context: Context): Repository {
            return INSTANCE ?: synchronized(this) {
                val instance = Repository(HelldeckDb.get(context))
                INSTANCE = instance
                instance
            }
        }

        @VisibleForTesting
        fun createForTesting(db: HelldeckDb): Repository = Repository(db)
    }

    /**
     * Creates a new game session with the given players.
     *
     * @param playerNames List of player names for the session
     * @return Session ID as a Long (converted from String UUID)
     */
    suspend fun createGameSession(playerNames: List<String>): Long {
        val sessionUuid = UUID.randomUUID()
        val sessionId = sessionUuid.toString()
        val session = SessionMetricsEntity(
            sessionId = sessionId,
            startedAtMs = System.currentTimeMillis(),
            totalRounds = 0,
            participatingPlayers = playerNames.joinToString(","),
        )
        db.withTransaction {
            db.sessionMetrics().upsert(session)

            playerNames.forEach { name ->
                val player = PlayerEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    avatar = "😀",
                    sessionPoints = 0,
                )
                db.players().upsert(player)
            }
        }

        val longId = sessionUuid.toPositiveLong()
        sessionIdMap[longId] = sessionId
        return longId
    }

    /**
     * Records a round in the database.
     *
     * @param sessionId Session ID (Long, will be converted to String)
     * @param templateId Template ID used for this round
     * @param game Game type
     * @param filledText The filled card text
     * @param feedback Feedback for this round
     * @param points Points awarded
     * @return Round ID as Long
     */
    suspend fun recordRound(
        sessionId: Long,
        templateId: String,
        game: String,
        filledText: String,
        feedback: Feedback,
        points: Int,
    ): Long {
        // Convert Long sessionId to String using the map
        val sessionIdStr = sessionIdMap[sessionId] ?: sessionId.toString()

        val roundUuid = UUID.randomUUID()
        val roundId = roundUuid.toString()
        val round = RoundMetricsEntity(
            roundId = roundId,
            sessionId = sessionIdStr,
            gameId = game,
            cardId = templateId,
            cardText = filledText,
            activePlayerId = "",
            lolCount = feedback.lol,
            mehCount = feedback.meh,
            trashCount = feedback.trash,
            points = points,
            startedAtMs = System.currentTimeMillis(),
            completedAtMs = System.currentTimeMillis(),
            durationMs = feedback.latencyMs.toLong(),
        )
        db.withTransaction {
            db.roundMetrics().upsert(round)
            db.sessionMetrics().incrementRounds(sessionIdStr)
            db.sessionMetrics().addLolCount(sessionIdStr, feedback.lol)
            db.sessionMetrics().addMehCount(sessionIdStr, feedback.meh)
            db.sessionMetrics().addTrashCount(sessionIdStr, feedback.trash)
        }

        return roundUuid.toPositiveLong()
    }

    /**
     * Gets a session by ID.
     *
     * @param sessionId Session ID (Long)
     * @return SessionSummary or null if not found
     */
    suspend fun getSessionById(sessionId: Long): SessionSummary? {
        val sessionIdStr = sessionIdMap[sessionId] ?: sessionId.toString()
        val session = db.sessionMetrics().getSession(sessionIdStr) ?: return null

        val playerNames = session.participatingPlayers.split(",").filter { it.isNotEmpty() }

        return SessionSummary(
            sessionId = sessionId,
            startTime = session.startedAtMs,
            endTime = session.endedAtMs,
            totalRounds = session.totalRounds,
            totalPoints = 0, // Would need to calculate from rounds
            playerCount = playerNames.size,
            gamesPlayed = emptyList(), // Would need to parse from gamesPlayed JSON
            averageScore = 0.0,
            highlights = emptyList(),
        )
    }

    /**
     * Gets all rounds for a session.
     *
     * @param sessionId Session ID (Long)
     * @return Flow of RoundMetricsEntity list
     */
    fun getRoundsForSession(sessionId: Long): Flow<List<RoundMetricsEntity>> {
        val sessionIdStr = sessionIdMap[sessionId] ?: sessionId.toString()
        return db.roundMetrics().getRoundsForSession(sessionIdStr)
    }

    /**
     * Gets all players.
     *
     * @return Flow of PlayerEntity list
     */
    fun getAllPlayers(): Flow<List<PlayerEntity>> {
        return db.players().getAllPlayers()
    }

    /**
     * Adds a new player.
     *
     * @param name Player name
     * @param avatar Player avatar emoji
     * @return Created PlayerEntity
     */
    suspend fun addPlayer(name: String, avatar: String): PlayerEntity {
        val player = PlayerEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            avatar = avatar,
            sessionPoints = 0,
        )
        db.players().upsert(player)
        return player
    }

    /**
     * Updates a player's score.
     *
     * @param playerId Player ID
     * @param points Points to add (can be negative)
     */
    suspend fun updatePlayerScore(playerId: String, points: Int) {
        db.withTransaction {
            db.players().addPointsToPlayer(playerId, points)
            db.players().addTotalPoints(playerId, points)
        }
    }
}

private fun UUID.toPositiveLong(): Long =
    (mostSignificantBits xor leastSignificantBits)
        .and(Long.MAX_VALUE)
        .coerceAtLeast(1L)
