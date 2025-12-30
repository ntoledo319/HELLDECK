package com.helldeck.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * User-created custom cards for personalization
 */
@Entity(
    tableName = "custom_cards",
    indices = [Index(value = ["gameId"]), Index(value = ["createdAtMs"])],
)
data class CustomCardEntity(
    @PrimaryKey val id: String, // unique card ID
    val gameId: String, // Which game type this card is for
    val cardText: String, // The card content (with {PLAYER} placeholders)
    val createdBy: String?, // Player ID who created it
    val creatorName: String?,
    val createdAtMs: Long = System.currentTimeMillis(),
    val timesUsed: Int = 0, // Track how often it's been played
    val avgRating: Float = 0f, // Average LOL/MEH/TRASH rating
    val isActive: Boolean = true, // Can be disabled without deleting
)

@Dao
interface CustomCardsDao {
    @Query("SELECT * FROM custom_cards WHERE isActive = 1 ORDER BY createdAtMs DESC")
    fun getAllActiveCards(): Flow<List<CustomCardEntity>>

    @Query("SELECT * FROM custom_cards WHERE isActive = 1 ORDER BY createdAtMs DESC")
    suspend fun getAllActiveCardsSnapshot(): List<CustomCardEntity>

    @Query("SELECT * FROM custom_cards WHERE gameId = :gameId AND isActive = 1")
    suspend fun getCardsForGame(gameId: String): List<CustomCardEntity>

    @Query("SELECT * FROM custom_cards WHERE createdBy = :playerId ORDER BY createdAtMs DESC")
    fun getCardsForPlayer(playerId: String): Flow<List<CustomCardEntity>>

    @Query("SELECT * FROM custom_cards WHERE id = :cardId LIMIT 1")
    suspend fun getCard(cardId: String): CustomCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CustomCardEntity)

    @Update
    suspend fun update(card: CustomCardEntity)

    @Delete
    suspend fun delete(card: CustomCardEntity)

    @Query("DELETE FROM custom_cards WHERE id = :cardId")
    suspend fun deleteById(cardId: String)

    @Query("UPDATE custom_cards SET isActive = :active WHERE id = :cardId")
    suspend fun setActive(cardId: String, active: Boolean)

    @Query("UPDATE custom_cards SET timesUsed = timesUsed + 1 WHERE id = :cardId")
    suspend fun incrementUsage(cardId: String)

    @Query("SELECT COUNT(*) FROM custom_cards WHERE isActive = 1")
    suspend fun getActiveCardCount(): Int

    @Query("SELECT COUNT(*) FROM custom_cards WHERE gameId = :gameId AND isActive = 1")
    suspend fun getActiveCardCountForGame(gameId: String): Int

    @Query("DELETE FROM custom_cards")
    suspend fun deleteAll()
}
