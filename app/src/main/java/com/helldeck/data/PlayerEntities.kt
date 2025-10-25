package com.helldeck.data

import androidx.room.*
import com.helldeck.content.model.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Room entity for player persistence
 */
@Entity(tableName = "players", indices = [Index(value = ["name"], unique = true)])
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String,
    val sessionPoints: Int = 0,
    val totalPoints: Int = 0,
    val elo: Int = 1000,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val afk: Int = 0,  // 0 = active, 1 = away
    val heatRounds: Int = 0,
    val quickLaughs: Int = 0,
    val lolCount: Int = 0,
    val mehCount: Int = 0,
    val trashCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Convert PlayerEntity to Player model
 */
fun PlayerEntity.toPlayer(): Player = Player(
    id = id,
    name = name,
    avatar = avatar,
    sessionPoints = sessionPoints,
    totalPoints = totalPoints,
    elo = elo,
    gamesPlayed = gamesPlayed,
    wins = wins,
    afk = afk
)

/**
 * Convert Player model to PlayerEntity
 */
fun Player.toEntity(): PlayerEntity = PlayerEntity(
    id = id,
    name = name,
    avatar = avatar,
    sessionPoints = sessionPoints,
    totalPoints = totalPoints,
    elo = elo,
    gamesPlayed = gamesPlayed,
    wins = wins,
    afk = afk
)

/**
 * Player profile for statistics and sharing
 */
data class PlayerProfile(
    val id: String,
    val name: String,
    val avatar: String,
    val totalPoints: Int,
    val wins: Int,
    val gamesPlayed: Int,
    val heatRounds: Int,
    val quickLaughs: Int,
    val avgLol: Double,
    val avgTrash: Double,
    val awards: List<String>
)

/**
 * Data Access Object for Player operations
 */
@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY totalPoints DESC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>
    
    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayer(id: String): PlayerEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(player: PlayerEntity)
    
    @Update
    suspend fun update(player: PlayerEntity)
    
    @Delete
    suspend fun delete(player: PlayerEntity)
    
    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deleteById(playerId: String)
    
    @Query("DELETE FROM players")
    suspend fun deleteAll()
    
    @Query("UPDATE players SET sessionPoints = sessionPoints + :points WHERE id = :playerId")
    suspend fun addPointsToPlayer(playerId: String, points: Int)
    
    @Query("UPDATE players SET totalPoints = totalPoints + :points WHERE id = :playerId")
    suspend fun addTotalPoints(playerId: String, points: Int)
    
    @Query("UPDATE players SET wins = wins + :count WHERE id = :playerId")
    suspend fun addWins(playerId: String, count: Int)
    
    @Query("UPDATE players SET gamesPlayed = gamesPlayed + 1 WHERE id = :playerId")
    suspend fun incGamesPlayed(playerId: String)
    
    @Query("UPDATE players SET heatRounds = heatRounds + 1 WHERE id = :playerId")
    suspend fun incHeatRounds(playerId: String)
    
    @Query("UPDATE players SET quickLaughs = quickLaughs + 1 WHERE id = :playerId")
    suspend fun incQuickLaughs(playerId: String)
    
    @Query("UPDATE players SET lolCount = lolCount + :count WHERE id = :playerId")
    suspend fun addLolCount(playerId: String, count: Int)
    
    @Query("UPDATE players SET mehCount = mehCount + :count WHERE id = :playerId")
    suspend fun addMehCount(playerId: String, count: Int)
    
    @Query("UPDATE players SET trashCount = trashCount + :count WHERE id = :playerId")
    suspend fun addTrashCount(playerId: String, count: Int)
    
    @Query("UPDATE players SET sessionPoints = 0")
    suspend fun resetSessionPoints()
    
    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int
}

/**
 * Compute player profiles with statistics
 */
suspend fun com.helldeck.content.data.ContentRepository.computePlayerProfiles(): List<PlayerProfile> {
    val players = db.players().getAllPlayers().first()
    
    return players.map { playerEntity ->
        val totalFeedback = playerEntity.lolCount + playerEntity.mehCount + playerEntity.trashCount
        val avgLol = if (totalFeedback > 0) playerEntity.lolCount.toDouble() / totalFeedback else 0.0
        val avgTrash = if (totalFeedback > 0) playerEntity.trashCount.toDouble() / totalFeedback else 0.0
        
        // Compute awards
        val awards = mutableListOf<String>()
        if (playerEntity.wins > 0 && playerEntity.gamesPlayed > 0) {
            val winRate = playerEntity.wins.toDouble() / playerEntity.gamesPlayed
            when {
                winRate >= 0.75 -> awards.add("🏆 Champion")
                winRate >= 0.50 -> awards.add("🥇 Winner")
                winRate >= 0.25 -> awards.add("🥈 Competitor")
            }
        }
        
        if (playerEntity.heatRounds >= 10) awards.add("🔥 Heat Master")
        if (playerEntity.quickLaughs >= 5) awards.add("⚡ Quick Wit")
        if (avgLol >= 0.7) awards.add("😂 Crowd Pleaser")
        if (playerEntity.totalPoints >= 100) awards.add("💯 Century Club")
        if (playerEntity.gamesPlayed >= 50) awards.add("🎮 Veteran")
        
        PlayerProfile(
            id = playerEntity.id,
            name = playerEntity.name,
            avatar = playerEntity.avatar,
            totalPoints = playerEntity.totalPoints,
            wins = playerEntity.wins,
            gamesPlayed = playerEntity.gamesPlayed,
            heatRounds = playerEntity.heatRounds,
            quickLaughs = playerEntity.quickLaughs,
            avgLol = avgLol,
            avgTrash = avgTrash,
            awards = awards
        )
    }.sortedByDescending { profile -> profile.totalPoints }
}
