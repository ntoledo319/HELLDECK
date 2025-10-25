package com.helldeck.content.db

import androidx.room.*

@Dao
interface TemplateStatDao {
    @Query("SELECT * FROM template_stats WHERE templateId = :id")
    suspend fun get(id: String): TemplateStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: TemplateStatEntity)

    @Query("SELECT * FROM template_stats")
    suspend fun getAll(): List<TemplateStatEntity>
}

@Dao
interface TemplateExposureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exposure: TemplateExposureEntity)

    @Query("SELECT DISTINCT templateId FROM template_exposures WHERE timestamp > :since ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentIds(since: Long, limit: Int): List<String>

    @Query("DELETE FROM template_exposures WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface GeneratedTextDao {
    @Query("SELECT * FROM generated_text WHERE key = :key")
    suspend fun get(key: String): com.helldeck.content.engine.augment.GeneratedTextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: com.helldeck.content.engine.augment.GeneratedTextEntity)

    @Query("DELETE FROM generated_text WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)
}