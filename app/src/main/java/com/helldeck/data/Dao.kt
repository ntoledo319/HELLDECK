package com.helldeck.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for template operations
 * Provides methods for template CRUD operations and queries
 */
@Dao
interface TemplateDao {

    // Query methods
    @Query("SELECT * FROM templates WHERE game = :game ORDER BY score DESC, draws ASC")
    fun getByGame(game: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE game = :game ORDER BY last_play_ts DESC LIMIT :limit")
    suspend fun getByGameRecent(game: String, limit: Int = 10): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE family = :family")
    suspend fun getByFamily(family: String): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE spice >= :minSpice")
    suspend fun getBySpiceLevel(minSpice: Int): List<TemplateEntity>

    @Query("SELECT * FROM templates ORDER BY score DESC LIMIT :limit")
    suspend fun getTopScoring(limit: Int = 50): List<TemplateEntity>

    @Query("SELECT * FROM templates")
    fun getAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE last_play_ts < :cutoffTs")
    suspend fun getStaleTemplates(cutoffTs: Long): List<TemplateEntity>

    @Query("SELECT DISTINCT game FROM templates")
    suspend fun getAllGames(): List<String>

    @Query("SELECT DISTINCT family FROM templates WHERE game = :game")
    suspend fun getFamiliesForGame(game: String): List<String>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: String): TemplateEntity?

    @Query("SELECT COUNT(*) FROM templates WHERE game = :game")
    suspend fun getCountForGame(game: String): Int

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun getTotalCount(): Int

    // Modification methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TemplateEntity>)

    @Update
    suspend fun update(template: TemplateEntity)

    @Update
    suspend fun updateAll(templates: List<TemplateEntity>)

    @Delete
    suspend fun delete(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE game = :game")
    suspend fun deleteByGame(game: String)

    @Query("DELETE FROM templates WHERE last_play_ts < :cutoffTs")
    suspend fun deleteStaleTemplates(cutoffTs: Long)

    @Query("DELETE FROM templates")
    suspend fun deleteAll()

    // Bulk operations
    @Transaction
    suspend fun upsert(template: TemplateEntity) {
        val existing = getById(template.id)
        if (existing != null) {
            update(template)
        } else {
            insert(template)
        }
    }

    @Transaction
    suspend fun upsertAll(templates: List<TemplateEntity>) {
        templates.forEach { upsert(it) }
    }

    // Analytics queries
    @Query("SELECT AVG(score) FROM templates WHERE game = :game")
    suspend fun getAverageScoreForGame(game: String): Double?

    @Query("SELECT MIN(score) FROM templates WHERE game = :game")
    suspend fun getMinScoreForGame(game: String): Double?

    @Query("SELECT MAX(score) FROM templates WHERE game = :game")
    suspend fun getMaxScoreForGame(game: String): Double?

    @Query("SELECT COUNT(*) FROM templates WHERE draws > 0")
    suspend fun getPlayedTemplateCount(): Int

    @Query("SELECT COUNT(*) FROM templates WHERE draws = 0")
    suspend fun getUnplayedTemplateCount(): Int
}

/**
 * Data Access Object for round operations
 * Manages game round data and history
 */
@Dao
interface RoundDao {

    // Query methods
    @Query("SELECT * FROM rounds ORDER BY ts DESC")
    fun getAllRounds(): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds ORDER BY ts DESC LIMIT :limit")
    suspend fun getLastRounds(limit: Int): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE game = :game ORDER BY ts DESC LIMIT :limit")
    suspend fun getRoundsByGame(game: String, limit: Int = 50): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE template_id = :templateId ORDER BY ts DESC")
    suspend fun getRoundsByTemplate(templateId: String): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE ts BETWEEN :startTime AND :endTime ORDER BY ts DESC")
    suspend fun getRoundsInTimeRange(startTime: Long, endTime: Long): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE points > 0 ORDER BY points DESC LIMIT :limit")
    suspend fun getHighScoringRounds(limit: Int = 10): List<RoundEntity>

    @Query("SELECT * FROM rounds WHERE judge_win = 1 ORDER BY ts DESC LIMIT :limit")
    suspend fun getJudgeWins(limit: Int = 20): List<RoundEntity>

    @Query("SELECT COUNT(*) FROM rounds WHERE game = :game")
    suspend fun getRoundCountForGame(game: String): Int

    @Query("SELECT COUNT(*) FROM rounds WHERE ts >= :since")
    suspend fun getRoundCountSince(since: Long): Int

    // Analytics queries
    @Query("SELECT AVG(points) FROM rounds WHERE game = :game")
    suspend fun getAveragePointsForGame(game: String): Double?

    @Query("SELECT AVG(latency_ms) FROM rounds WHERE game = :game")
    suspend fun getAverageLatencyForGame(game: String): Double?

    @Query("SELECT SUM(lol) FROM rounds WHERE game = :game")
    suspend fun getTotalLolsForGame(game: String): Int?

    @Query("SELECT SUM(trash) FROM rounds WHERE game = :game")
    suspend fun getTotalTrashForGame(game: String): Int?

    @Query("SELECT game, COUNT(*) as count FROM rounds GROUP BY game ORDER BY count DESC")
    suspend fun getGamePlayCounts(): List<GamePlayCount>

    // Insertion
    @Insert
    suspend fun insert(round: RoundEntity): Long

    @Insert
    suspend fun insertAll(rounds: List<RoundEntity>)

    @Update
    suspend fun update(round: RoundEntity)

    @Delete
    suspend fun delete(round: RoundEntity)

    @Query("DELETE FROM rounds WHERE ts < :cutoffTs")
    suspend fun deleteOldRounds(cutoffTs: Long)

    @Query("DELETE FROM rounds")
    suspend fun deleteAll()

    // Transaction helpers
    @Transaction
    suspend fun insertWithComments(round: RoundEntity, comments: List<CommentEntity>): Long {
        val roundId = insert(round)
        // Comments should be inserted via CommentDao
        return roundId
    }
}

/**
 * Data class for game play statistics
 */
data class GamePlayCount(
    @ColumnInfo(name = "game")
    val game: String,

    @ColumnInfo(name = "count")
    val count: Int
)

/**
 * Data Access Object for comment operations
 */
@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE round_id = :roundId ORDER BY created_at ASC")
    suspend fun getCommentsForRound(roundId: Long): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getCommentsWithTag(tag: String): List<CommentEntity>

    @Query("SELECT DISTINCT tags FROM comments WHERE tags IS NOT NULL AND tags != ''")
    suspend fun getAllTags(): List<String>

    @Insert
    suspend fun insert(comment: CommentEntity): Long

    @Insert
    suspend fun insertAll(comments: List<CommentEntity>)

    @Update
    suspend fun update(comment: CommentEntity)

    @Delete
    suspend fun delete(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE round_id = :roundId")
    suspend fun deleteCommentsForRound(roundId: Long)
}

/**
 * Data Access Object for player operations
 */
@Dao
interface PlayerDao {

    // Query methods
    @Query("SELECT * FROM players ORDER BY session_points DESC, name ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE afk = 0 ORDER BY session_points DESC")
    fun getActivePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE name = :name")
    suspend fun getPlayerByName(name: String): PlayerEntity?

    @Query("SELECT * FROM players ORDER BY elo DESC LIMIT :limit")
    suspend fun getTopPlayersByElo(limit: Int = 10): List<PlayerEntity>

    @Query("SELECT * FROM players ORDER BY total_points DESC LIMIT :limit")
    suspend fun getTopPlayersByTotalPoints(limit: Int = 10): List<PlayerEntity>

    @Query("SELECT COUNT(*) FROM players WHERE afk = 0")
    suspend fun getActivePlayerCount(): Int

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getTotalPlayerCount(): Int

    // Modification methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Update
    suspend fun update(player: PlayerEntity)

    @Update
    suspend fun updateAll(players: List<PlayerEntity>)

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("DELETE FROM players WHERE last_seen < :cutoffTs")
    suspend fun deleteInactivePlayers(cutoffTs: Long)

    @Query("UPDATE players SET afk = :afk WHERE id = :playerId")
    suspend fun setPlayerAfk(playerId: String, afk: Int)

    @Query("UPDATE players SET session_points = session_points + :points WHERE id = :playerId")
    suspend fun addPointsToPlayer(playerId: String, points: Int)

    @Query("UPDATE players SET elo = :elo WHERE id = :playerId")
    suspend fun updatePlayerElo(playerId: String, elo: Int)

    // Long-term stats
    @Query("UPDATE players SET total_points = total_points + :points WHERE id = :playerId")
    suspend fun addTotalPoints(playerId: String, points: Int)

    @Query("UPDATE players SET wins = wins + :wins WHERE id = :playerId")
    suspend fun addWins(playerId: String, wins: Int = 1)

    @Query("UPDATE players SET games_played = games_played + 1 WHERE id = :playerId")
    suspend fun incGamesPlayed(playerId: String)

    // Transaction helpers
    @Transaction
    suspend fun upsert(player: PlayerEntity) {
        val existing = getPlayerById(player.id)
        if (existing != null) {
            update(player)
        } else {
            insert(player)
        }
    }

    @Transaction
    suspend fun resetSession() {
        // Reset session points for all players
        // This would need to be implemented with a custom query
        // For now, we'll skip this functionality
    }
}

/**
 * Data Access Object for lexicon operations
 */
@Dao
interface LexiconDao {

    @Query("SELECT * FROM lexicons WHERE name = :name")
    suspend fun getLexicon(name: String): LexiconEntity?

    @Query("SELECT * FROM lexicons ORDER BY name")
    suspend fun getAllLexicons(): List<LexiconEntity>

    @Query("SELECT name FROM lexicons ORDER BY name")
    suspend fun getAllLexiconNames(): List<String>

    @Query("SELECT COUNT(*) FROM lexicons")
    suspend fun getLexiconCount(): Int

    @Query("SELECT * FROM lexicons WHERE updated_ts < :cutoffTs")
    suspend fun getStaleLexicons(cutoffTs: Long): List<LexiconEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lexicon: LexiconEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lexicons: List<LexiconEntity>)

    @Update
    suspend fun update(lexicon: LexiconEntity)

    @Delete
    suspend fun delete(lexicon: LexiconEntity)

    @Query("DELETE FROM lexicons WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Transaction
    suspend fun upsert(lexicon: LexiconEntity) {
        insert(lexicon)
    }
}

/**
 * Data Access Object for settings operations
 */
@Dao
interface SettingDao {

    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getSetting(key: String): SettingEntity?

    @Query("SELECT * FROM settings ORDER BY key")
    suspend fun getAllSettings(): List<SettingEntity>

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT COUNT(*) FROM settings")
    suspend fun getSettingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: SettingEntity): Long

    @Update
    suspend fun update(setting: SettingEntity)

    @Delete
    suspend fun delete(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()

    // Type-safe getters
    suspend fun getString(key: String, defaultValue: String = ""): String {
        return getSettingValue(key) ?: defaultValue
    }

    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        return getSettingValue(key)?.toIntOrNull() ?: defaultValue
    }

    suspend fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getSettingValue(key)?.toLongOrNull() ?: defaultValue
    }

    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getSettingValue(key)?.toBooleanStrictOrNull() ?: defaultValue
    }

    suspend fun getFloat(key: String, defaultValue: Float = 0.0f): Float {
        return getSettingValue(key)?.toFloatOrNull() ?: defaultValue
    }

    // Type-safe setters
    suspend fun putString(key: String, value: String) {
        insert(SettingEntity(key, value))
    }

    suspend fun putInt(key: String, value: Int) {
        insert(SettingEntity(key, value.toString()))
    }

    suspend fun putLong(key: String, value: Long) {
        insert(SettingEntity(key, value.toString()))
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        insert(SettingEntity(key, value.toString()))
    }

    suspend fun putFloat(key: String, value: Float) {
        insert(SettingEntity(key, value.toString()))
    }
}

/**
 * Data Access Object for game session operations
 */
@Dao
interface GameSessionDao {

    @Query("SELECT * FROM game_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): GameSessionEntity?

    @Query("SELECT * FROM game_sessions ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 10): List<GameSessionEntity>

    @Query("SELECT * FROM game_sessions WHERE start_time BETWEEN :startTime AND :endTime")
    suspend fun getSessionsInTimeRange(startTime: Long, endTime: Long): List<GameSessionEntity>

    @Query("SELECT COUNT(*) FROM game_sessions")
    suspend fun getTotalSessionCount(): Int

    @Query("SELECT SUM(rounds_played) FROM game_sessions")
    suspend fun getTotalRoundsPlayed(): Int?

    @Query("SELECT AVG(player_count) FROM game_sessions")
    suspend fun getAveragePlayerCount(): Double?

    @Insert
    suspend fun insert(session: GameSessionEntity): Long

    @Update
    suspend fun update(session: GameSessionEntity)

    @Delete
    suspend fun delete(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE start_time < :cutoffTs")
    suspend fun deleteOldSessions(cutoffTs: Long)

    @Transaction
    suspend fun endSession(sessionId: Long, endTime: Long, roundsPlayed: Int, totalPoints: Int) {
        val session = getSessionById(sessionId)?.copy(
            endTime = endTime,
            roundsPlayed = roundsPlayed,
            totalPoints = totalPoints
        )
        session?.let { update(it) }
    }
}
