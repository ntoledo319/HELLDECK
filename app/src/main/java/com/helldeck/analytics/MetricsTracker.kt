package com.helldeck.analytics

import com.helldeck.content.data.ContentRepository
import com.helldeck.data.RoundMetricsEntity
import com.helldeck.data.SessionAnalytics
import com.helldeck.data.SessionMetricsEntity
import com.helldeck.data.GameAnalytics
import com.helldeck.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tracks gameplay metrics for analytics and session summaries.
 *
 * Automatically records:
 * - Session-level metrics (duration, total rounds, laugh scores)
 * - Round-level metrics (per-game performance, timings)
 * - Player engagement patterns
 * - Game mix and preferences
 *
 * Used for:
 * - Session summaries and export
 * - Understanding what's working
 * - Celebrating milestones
 * - Improving content recommendations
 */
class MetricsTracker(
    private val repo: ContentRepository
) {
    private var currentRoundId: String? = null
    private var currentRoundStartMs: Long = 0L

    /**
     * Starts tracking a new game night session.
     * Creates initial session metrics entry.
     */
    suspend fun startSession(sessionId: String, playerIds: List<String>) = withContext(Dispatchers.IO) {
        try {
            val session = SessionMetricsEntity(
                sessionId = sessionId,
                startedAtMs = System.currentTimeMillis(),
                participatingPlayers = JSONArray(playerIds).toString()
            )
            repo.db.sessionMetrics().upsert(session)
            Logger.i("MetricsTracker: Started session $sessionId with ${playerIds.size} players")
        } catch (e: Exception) {
            Logger.e("MetricsTracker: Failed to start session", e)
        }
    }

    /**
     * Starts tracking a new round.
     * Records round start time and initial metadata.
     */
    suspend fun startRound(
        roundId: String,
        sessionId: String,
        gameId: String,
        cardId: String,
        cardText: String,
        activePlayerId: String,
        spiceLevel: Int
    ) = withContext(Dispatchers.IO) {
        try {
            currentRoundId = roundId
            currentRoundStartMs = System.currentTimeMillis()

            val round = RoundMetricsEntity(
                roundId = roundId,
                sessionId = sessionId,
                gameId = gameId,
                cardId = cardId,
                cardText = cardText,
                activePlayerId = activePlayerId,
                startedAtMs = currentRoundStartMs,
                spiceLevel = spiceLevel
            )
            repo.db.roundMetrics().upsert(round)
            Logger.d("MetricsTracker: Started round $roundId for game $gameId")
        } catch (e: Exception) {
            Logger.e("MetricsTracker: Failed to start round", e)
        }
    }

    /**
     * Completes the current round with feedback data.
     * Updates round metrics and session aggregates.
     */
    suspend fun completeRound(
        lolCount: Int,
        mehCount: Int,
        trashCount: Int,
        points: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val roundId = currentRoundId ?: run {
                Logger.w("MetricsTracker: No active round to complete")
                return@withContext
            }

            val round = repo.db.roundMetrics().getRound(roundId) ?: run {
                Logger.w("MetricsTracker: Round $roundId not found")
                return@withContext
            }

            val completedAtMs = System.currentTimeMillis()
            val durationMs = completedAtMs - round.startedAtMs

            // Update round with feedback and timing
            val updatedRound = round.copy(
                lolCount = lolCount,
                mehCount = mehCount,
                trashCount = trashCount,
                points = points,
                completedAtMs = completedAtMs,
                durationMs = durationMs
            )
            repo.db.roundMetrics().upsert(updatedRound)

            // Update session aggregates
            repo.db.sessionMetrics().incrementRounds(round.sessionId)
            repo.db.sessionMetrics().addLolCount(round.sessionId, lolCount)
            repo.db.sessionMetrics().addMehCount(round.sessionId, mehCount)
            repo.db.sessionMetrics().addTrashCount(round.sessionId, trashCount)

            // Update games played map
            updateGamesPlayedMap(round.sessionId, round.gameId)

            Logger.d("MetricsTracker: Completed round $roundId (${durationMs}ms, LOL:$lolCount, MEH:$mehCount, TRASH:$trashCount)")
            currentRoundId = null
        } catch (e: Exception) {
            Logger.e("MetricsTracker: Failed to complete round", e)
        }
    }

    /**
     * Ends the current session.
     * Calculates final duration and marks session as complete.
     */
    suspend fun endSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            val session = repo.db.sessionMetrics().getSession(sessionId) ?: run {
                Logger.w("MetricsTracker: Session $sessionId not found")
                return@withContext
            }

            val endedAtMs = System.currentTimeMillis()
            val durationMs = endedAtMs - session.startedAtMs

            repo.db.sessionMetrics().endSession(sessionId, endedAtMs, durationMs)
            Logger.i("MetricsTracker: Ended session $sessionId (duration: ${durationMs / 1000}s)")
        } catch (e: Exception) {
            Logger.e("MetricsTracker: Failed to end session", e)
        }
    }

    /**
     * Computes analytics for a session.
     * Returns aggregated metrics for UI display.
     */
    suspend fun computeSessionAnalytics(sessionId: String): SessionAnalytics? = withContext(Dispatchers.IO) {
        try {
            val session = repo.db.sessionMetrics().getSession(sessionId) ?: return@withContext null
            val rounds = repo.db.roundMetrics().getRoundsForSessionSnapshot(sessionId)

            if (rounds.isEmpty()) {
                return@withContext SessionAnalytics(
                    sessionId = sessionId,
                    duration = session.durationMs,
                    totalRounds = 0,
                    averageLaughScore = 0.0,
                    totalReactions = 0,
                    topGame = null,
                    participantCount = parsePlayerIds(session.participatingPlayers).size,
                    longestRound = 0,
                    shortestRound = 0,
                    heatMoments = 0
                )
            }

            val totalReactions = session.totalLolCount + session.totalMehCount + session.totalTrashCount
            val averageLaughScore = if (totalReactions > 0) {
                session.totalLolCount.toDouble() / totalReactions
            } else 0.0

            val gamePlayCounts = rounds.groupBy { it.gameId }.mapValues { it.value.size }
            val topGame = gamePlayCounts.maxByOrNull { it.value }?.toPair()

            val durations = rounds.mapNotNull { if (it.durationMs > 0) it.durationMs else null }
            val longestRound = durations.maxOrNull() ?: 0
            val shortestRound = durations.minOrNull() ?: 0

            val heatMoments = rounds.count { round ->
                val total = round.lolCount + round.mehCount + round.trashCount
                if (total > 0) {
                    val heatPercentage = (round.lolCount + round.trashCount).toDouble() / total
                    heatPercentage > 0.7
                } else false
            }

            SessionAnalytics(
                sessionId = sessionId,
                duration = session.durationMs,
                totalRounds = rounds.size,
                averageLaughScore = averageLaughScore,
                totalReactions = totalReactions,
                topGame = topGame,
                participantCount = parsePlayerIds(session.participatingPlayers).size,
                longestRound = longestRound,
                shortestRound = shortestRound,
                heatMoments = heatMoments
            )
        } catch (e: Exception) {
            Logger.e("MetricsTracker: Failed to compute session analytics", e)
            null
        }
    }

    /**
     * Computes analytics for a specific game across all sessions.
     */
    suspend fun computeGameAnalytics(gameId: String, limit: Int = 50): GameAnalytics? = withContext(Dispatchers.IO) {
        try {
            val rounds = repo.db.roundMetrics().getRoundsByGame(gameId, limit)
            if (rounds.isEmpty()) return@withContext null

            val totalLols = rounds.sumOf { it.lolCount }
            val totalMehs = rounds.sumOf { it.mehCount }
            val totalTrash = rounds.sumOf { it.trashCount }
            val totalReactions = totalLols + totalMehs + totalTrash

            val averageLaughScore = if (totalReactions > 0) {
                totalLols.toDouble() / totalReactions
            } else 0.0

            val durations = rounds.mapNotNull { if (it.durationMs > 0) it.durationMs else null }
            val averageDuration = if (durations.isNotEmpty()) {
                durations.average().toLong()
            } else 0L

            GameAnalytics(
                gameId = gameId,
                timesPlayed = rounds.size,
                averageLaughScore = averageLaughScore,
                averageDuration = averageDuration,
                totalLols = totalLols,
                totalMehs = totalMehs,
                totalTrash = totalTrash
            )
        } catch (e: Exception) {
            Logger.e("MetricsTracker: Failed to compute game analytics for $gameId", e)
            null
        }
    }

    /**
     * Updates the games played map for a session.
     * Tracks which games are being played most.
     */
    private suspend fun updateGamesPlayedMap(sessionId: String, gameId: String) {
        try {
            val session = repo.db.sessionMetrics().getSession(sessionId) ?: return
            val gamesMap = parseGamesPlayed(session.gamesPlayed).toMutableMap()
            gamesMap[gameId] = (gamesMap[gameId] ?: 0) + 1

            val updated = session.copy(
                gamesPlayed = JSONObject(gamesMap as Map<*, *>).toString()
            )
            repo.db.sessionMetrics().upsert(updated)
        } catch (e: Exception) {
            Logger.e("MetricsTracker: Failed to update games played map", e)
        }
    }

    /**
     * Parses player IDs from JSON array string.
     */
    private fun parsePlayerIds(json: String): List<String> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses games played map from JSON object string.
     */
    private fun parseGamesPlayed(json: String): Map<String, Int> {
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getInt(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
