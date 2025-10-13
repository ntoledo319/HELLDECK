package com.helldeck.data

import androidx.room.*
import com.google.gson.annotations.SerializedName

/**
 * Template entity representing a game card template
 * Stores the base template data and learning metrics
 */
@Entity(
    tableName = "templates",
    indices = [
        Index("game"),
        Index("family"),
        Index("last_play_ts")
    ]
)
data class TemplateEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "game")
    val game: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "family")
    val family: String,

    @ColumnInfo(name = "spice")
    val spice: Int = 1,

    @ColumnInfo(name = "locality")
    val locality: Int = 1,

    @ColumnInfo(name = "max_words")
    val maxWords: Int = 16,

    // Learning metrics
    @ColumnInfo(name = "score", defaultValue = "0.0")
    val score: Double = 0.0,

    @ColumnInfo(name = "draws", defaultValue = "0")
    val draws: Int = 0,

    @ColumnInfo(name = "wins", defaultValue = "0")
    val wins: Int = 0,

    @ColumnInfo(name = "last_play_ts", defaultValue = "0")
    val lastPlayTs: Long = 0L,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Round entity representing a single game round
 * Stores all the data about what happened in a round
 */
@Entity(
    tableName = "rounds",
    indices = [
        Index("ts"),
        Index("game"),
        Index("template_id"),
        Index("judge_win"),
        Index("points")
    ]
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "ts")
    val ts: Long,

    @ColumnInfo(name = "game")
    val game: String,

    @ColumnInfo(name = "template_id")
    val templateId: String,

    @ColumnInfo(name = "fills_json")
    val fillsJson: String, // JSON representation of filled slots

    @ColumnInfo(name = "lol")
    val lol: Int,

    @ColumnInfo(name = "meh")
    val meh: Int,

    @ColumnInfo(name = "trash")
    val trash: Int,

    @ColumnInfo(name = "judge_win", defaultValue = "0")
    val judgeWin: Int,

    @ColumnInfo(name = "points", defaultValue = "0")
    val points: Int,

    @ColumnInfo(name = "latency_ms", defaultValue = "0")
    val latencyMs: Int,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "player_count", defaultValue = "0")
    val playerCount: Int = 0,

    @ColumnInfo(name = "room_heat", defaultValue = "0.0")
    val roomHeat: Double = 0.0
)

/**
 * Comment entity for additional round feedback
 * Allows players to add tags and notes to rounds
 */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["round_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("round_id"),
        Index("tags")
    ]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "round_id")
    val roundId: Long,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "tags")
    val tags: String, // Comma-separated tags

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Player entity representing a game participant
 * Stores player info and persistent statistics
 */
@Entity(
    tableName = "players",
    indices = [
        Index("name"),
        Index("session_points"),
        Index("elo")
    ]
)
data class PlayerEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "avatar")
    val avatar: String, // Emoji or short identifier

    @ColumnInfo(name = "session_points", defaultValue = "0")
    val sessionPoints: Int = 0,

    @ColumnInfo(name = "total_points", defaultValue = "0")
    val totalPoints: Int = 0,

    @ColumnInfo(name = "elo", defaultValue = "1000")
    val elo: Int = 1000,

    @ColumnInfo(name = "games_played", defaultValue = "0")
    val gamesPlayed: Int = 0,

    @ColumnInfo(name = "wins", defaultValue = "0")
    val wins: Int = 0,

    @ColumnInfo(name = "titles_json")
    val titlesJson: String = "[]", // JSON array of earned titles

    @ColumnInfo(name = "afk", defaultValue = "0")
    val afk: Int = 0, // 0 = active, 1 = AFK

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_seen", defaultValue = "CURRENT_TIMESTAMP")
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * Lexicon entity for storing word lists
 * Used for template slot filling
 */
@Entity(
    tableName = "lexicons",
    indices = [
        Index("name"),
        Index("updated_ts")
    ]
)
data class LexiconEntity(
    @PrimaryKey
    val name: String,

    @ColumnInfo(name = "data_json")
    val dataJson: String, // JSON array of strings

    @ColumnInfo(name = "updated_ts")
    val updatedTs: Long,

    @ColumnInfo(name = "size", defaultValue = "0")
    val size: Int = 0
)

/**
 * Settings entity for app configuration
 * Key-value store for user preferences
 */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey
    val key: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "updated_at", defaultValue = "CURRENT_TIMESTAMP")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Game session entity for tracking game instances
 * Helps with analytics and session management
 */
@Entity(
    tableName = "game_sessions",
    indices = [
        Index("start_time"),
        Index("end_time"),
        Index("player_count")
    ]
)
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    @ColumnInfo(name = "player_count", defaultValue = "0")
    val playerCount: Int = 0,

    @ColumnInfo(name = "rounds_played", defaultValue = "0")
    val roundsPlayed: Int = 0,

    @ColumnInfo(name = "games_played")
    val gamesPlayed: String = "[]", // JSON array of game IDs

    @ColumnInfo(name = "total_points", defaultValue = "0")
    val totalPoints: Int = 0,

    @ColumnInfo(name = "brainpack_exported", defaultValue = "0")
    val brainpackExported: Int = 0 // 0 = no, 1 = yes
)

/**
 * Data class for template definitions (used for loading from assets)
 */
data class TemplateDef(
    @SerializedName("id")
    val id: String,

    @SerializedName("game")
    val game: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("family")
    val family: String,

    @SerializedName("spice")
    val spice: Int = 1,

    @SerializedName("locality")
    val locality: Int = 1,

    @SerializedName("max_words")
    val maxWords: Int = 16
)

/**
 * Data class for lexicon data (used for loading from assets)
 */
data class LexiconDef(
    @SerializedName("name")
    val name: String,

    @SerializedName("words")
    val words: List<String>
)