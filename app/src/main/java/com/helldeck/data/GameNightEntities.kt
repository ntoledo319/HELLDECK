package com.helldeck.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * HELLDECK 2.0 Upgrade System Entities
 * Persistent storage for all 10 upgrade features
 */

// ========== 1. HOUSE RULES ==========
@Entity(tableName = "house_rules")
data class HouseRuleEntity(
    @PrimaryKey val ruleId: String,
    val sessionId: String,
    val ruleName: String,
    val enabled: Boolean,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Dao
interface HouseRulesDao {
    @Query("SELECT * FROM house_rules WHERE sessionId = :sessionId")
    fun getRulesForSession(sessionId: String): Flow<List<HouseRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: HouseRuleEntity)

    @Query("DELETE FROM house_rules WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)
}

// ========== 2. GROUP DNA ==========
@Entity(tableName = "group_dna")
data class GroupDnaEntity(
    @PrimaryKey val sessionId: String,
    val dnaProfile: String,
    val traits: String, // JSON list
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface GroupDnaDao {
    @Query("SELECT * FROM group_dna WHERE sessionId = :sessionId LIMIT 1")
    fun getDnaForSession(sessionId: String): Flow<GroupDnaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDna(dna: GroupDnaEntity)
}

// ========== 3. HIGHLIGHTS ==========
@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val cardId: String,
    val cardText: String,
    val rating: Int, // LOL count
    val timestampMs: Long = System.currentTimeMillis()
)

@Dao
interface HighlightsDao {
    @Query("SELECT * FROM highlights WHERE sessionId = :sessionId ORDER BY rating DESC, timestampMs DESC")
    fun getHighlightsForSession(sessionId: String): Flow<List<HighlightEntity>>

    @Insert
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)
}

// ========== 4. INSTANT REMIX ==========
@Entity(tableName = "remix_requests")
data class RemixRequestEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val originalText: String,
    val remixMode: String, // "more_spicy", "less_spicy", "funnier", etc.
    val timestampMs: Long = System.currentTimeMillis()
)

@Dao
interface RemixDao {
    @Insert
    suspend fun insertRemixRequest(request: RemixRequestEntity)

    @Query("SELECT * FROM remix_requests ORDER BY timestampMs DESC LIMIT 10")
    fun getRecentRemixes(): Flow<List<RemixRequestEntity>>
}

// ========== 5. PLAYER ROLES ==========
@Entity(tableName = "player_roles")
data class PlayerRoleEntity(
    @PrimaryKey val id: String,
    val playerId: String,
    val sessionId: String,
    val role: String,
    val assignedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface PlayerRolesDao {
    @Query("SELECT * FROM player_roles WHERE sessionId = :sessionId")
    fun getRolesForSession(sessionId: String): Flow<List<PlayerRoleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRole(role: PlayerRoleEntity)

    @Query("DELETE FROM player_roles WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)
}

// ========== 6-10: Additional Upgrades (Cinematic, Heat, Packs, Share, Fraud) ==========

@Entity(tableName = "pack_selections")
data class PackSelectionEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val packName: String,
    val enabled: Boolean
)

@Dao
interface PacksDao {
    @Query("SELECT * FROM pack_selections WHERE sessionId = :sessionId AND enabled = 1")
    fun getEnabledPacks(sessionId: String): Flow<List<PackSelectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPack(pack: PackSelectionEntity)
}

@Entity(tableName = "fraud_quarantine")
data class FraudQuarantineEntity(
    @PrimaryKey val cardId: String,
    val reason: String,
    val quarantinedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface FraudDao {
    @Query("SELECT * FROM fraud_quarantine")
    fun getAllQuarantined(): Flow<List<FraudQuarantineEntity>>

    @Insert
    suspend fun quarantineCard(card: FraudQuarantineEntity)

    @Query("SELECT COUNT(*) FROM fraud_quarantine WHERE cardId = :cardId")
    suspend fun isQuarantined(cardId: String): Int
}
