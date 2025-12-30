package com.helldeck.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Favorite cards collection for saving and replaying best moments
 */
@Entity(
    tableName = "favorite_cards",
    indices = [Index(value = ["sessionId"]), Index(value = ["addedAtMs"])],
)
data class FavoriteCardEntity(
    @PrimaryKey val id: String, // unique favorite ID
    val cardId: String,
    val cardText: String,
    val gameId: String,
    val gameName: String,
    val sessionId: String,
    val playerId: String?, // Who favorited it
    val playerName: String?,
    val lolCount: Int = 0,
    val addedAtMs: Long = System.currentTimeMillis(),
    val note: String = "", // Optional user note
)

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_cards ORDER BY addedAtMs DESC")
    fun getAllFavorites(): Flow<List<FavoriteCardEntity>>

    @Query("SELECT * FROM favorite_cards ORDER BY addedAtMs DESC")
    suspend fun getAllFavoritesSnapshot(): List<FavoriteCardEntity>

    @Query("SELECT * FROM favorite_cards WHERE playerId = :playerId ORDER BY addedAtMs DESC")
    fun getFavoritesForPlayer(playerId: String): Flow<List<FavoriteCardEntity>>

    @Query("SELECT * FROM favorite_cards WHERE sessionId = :sessionId ORDER BY addedAtMs DESC")
    fun getFavoritesForSession(sessionId: String): Flow<List<FavoriteCardEntity>>

    @Query("SELECT * FROM favorite_cards WHERE gameId = :gameId ORDER BY addedAtMs DESC")
    fun getFavoritesForGame(gameId: String): Flow<List<FavoriteCardEntity>>

    @Query("SELECT * FROM favorite_cards WHERE id = :favoriteId LIMIT 1")
    suspend fun getFavorite(favoriteId: String): FavoriteCardEntity?

    @Query("SELECT * FROM favorite_cards WHERE cardId = :cardId AND sessionId = :sessionId LIMIT 1")
    suspend fun isFavorited(cardId: String, sessionId: String): FavoriteCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteCardEntity)

    @Delete
    suspend fun delete(favorite: FavoriteCardEntity)

    @Query("DELETE FROM favorite_cards WHERE id = :favoriteId")
    suspend fun deleteById(favoriteId: String)

    @Query("DELETE FROM favorite_cards WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM favorite_cards")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM favorite_cards")
    suspend fun getFavoriteCount(): Int

    @Query("SELECT COUNT(*) FROM favorite_cards WHERE playerId = :playerId")
    suspend fun getFavoriteCountForPlayer(playerId: String): Int
}

/**
 * Computed favorite card with additional metadata for display
 */
data class FavoriteCardDisplay(
    val favorite: FavoriteCardEntity,
    val sessionName: String?,
    val daysAgo: Int,
    val isRecent: Boolean, // Added within last 7 days
)
