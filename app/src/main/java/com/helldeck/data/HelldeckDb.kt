package com.helldeck.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room database for HELLDECK
 * Manages all persistent data storage
 */
@Database(
    entities = [
        TemplateEntity::class,
        RoundEntity::class,
        CommentEntity::class,
        PlayerEntity::class,
        LexiconEntity::class,
        SettingEntity::class,
        GameSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
// @TypeConverters(Converters::class)
abstract class HelldeckDb : RoomDatabase() {

    abstract fun templates(): TemplateDao
    abstract fun rounds(): RoundDao
    abstract fun comments(): CommentDao
    abstract fun players(): PlayerDao
    abstract fun lexicons(): LexiconDao
    abstract fun settings(): SettingDao
    abstract fun sessions(): GameSessionDao

    companion object {
        @Volatile
        private var INSTANCE: HelldeckDb? = null

        @Volatile
        private var MIGRATION_TEST_DB: HelldeckDb? = null

        /**
         * Get singleton instance of the database
         */
        fun getInstance(context: Context): HelldeckDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Get a test database instance for migrations
         */
        fun getTestInstance(context: Context): HelldeckDb {
            return MIGRATION_TEST_DB ?: synchronized(this) {
                MIGRATION_TEST_DB ?: buildTestDatabase(context).also { MIGRATION_TEST_DB = it }
            }
        }

        /**
         * Build the main database instance
         */
        private fun buildDatabase(context: Context): HelldeckDb {
            return Room.databaseBuilder(
                context.applicationContext,
                HelldeckDb::class.java,
                "helldeck.db"
            )
                .addCallback(DatabaseCallback())
                .addMigrations() // Add migrations here when needed
                .fallbackToDestructiveMigration() // For development only
                .build()
        }

        /**
         * Build a test database instance for migration testing
         */
        private fun buildTestDatabase(context: Context): HelldeckDb {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                HelldeckDb::class.java
            )
                .addCallback(DatabaseCallback())
                .build()
        }

        /**
         * Reset database for testing or fresh start
         */
        suspend fun resetDatabase(context: Context) {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                context.deleteDatabase("helldeck.db")
            }
        }

        /**
         * Database callback for initialization and maintenance
         */
        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database is created, populate with initial data if needed
                CoroutineScope(Dispatchers.IO).launch {
                    // Initial data population can be added here
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Database is opened, perform any maintenance tasks
                // PRAGMA statements removed - they conflict with Room's query restrictions in onOpen callback
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                // Handle destructive migration if needed
            }
        }
    }

    /**
     * Database maintenance operations
     */
    suspend fun vacuum() {
        withTransaction {
            // Rebuild database to reclaim space
            val db = this
            // Note: VACUUM cannot be run within a transaction
        }
    }

    suspend fun integrityCheck(): Boolean {
        return withTransaction {
            val cursor = query("PRAGMA integrity_check", null)
            cursor.moveToFirst()
            val result = cursor.getString(0)
            cursor.close()
            result == "ok"
        }
    }

    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        return withTransaction {
            DatabaseStats(
                templateCount = templates().getCountForGame("*"), // This would need a custom query
                roundCount = rounds().getRoundCountSince(0),
                playerCount = players().getTotalPlayerCount(),
                lexiconCount = lexicons().getLexiconCount(),
                settingCount = settings().getSettingCount(),
                sessionCount = sessions().getTotalSessionCount(),
                databaseSize = getDatabaseSize(),
                lastMaintenance = System.currentTimeMillis()
            )
        }
    }

    private suspend fun getDatabaseSize(): Long {
        return try {
            val dbPath = openHelper.writableDatabase.path
            java.io.File(dbPath).length()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Clean up old data to maintain database performance
     */
    suspend fun cleanupOldData(retentionDays: Int = 30) {
        withTransaction {
            val cutoffTs = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

            // Clean old rounds
            rounds().deleteOldRounds(cutoffTs)

            // Clean old comments (handled by cascade delete)

            // Clean inactive players (older than retention period)
            players().deleteInactivePlayers(cutoffTs)

            // Clean old sessions
            sessions().deleteOldSessions(cutoffTs)

            // Clean stale lexicons (older than 7 days)
            val lexiconCutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            lexicons().getStaleLexicons(lexiconCutoff).forEach { lexicon ->
                lexicons().delete(lexicon)
            }
        }
    }
}

/**
 * Type converters for Room database
 */
class Converters {
    // Add any custom type converters here if needed
    // For example, for complex objects or enums
}

/**
 * Database statistics data class
 */
data class DatabaseStats(
    val templateCount: Int,
    val roundCount: Int,
    val playerCount: Int,
    val lexiconCount: Int,
    val settingCount: Int,
    val sessionCount: Int,
    val databaseSize: Long,
    val lastMaintenance: Long
)

/**
 * Database operations helper
 */
object DatabaseOperations {

    /**
     * Initialize database with default data
     */
    suspend fun initializeWithDefaults(context: Context) {
        val db = HelldeckDb.getInstance(context)

        // Check if we need to initialize
        if (db.templates().getCountForGame("*") == 0) {
            // Database is empty, would populate with default templates
            // This would be called from the repository layer
        }
    }

    /**
     * Export database for backup or migration
     */
    suspend fun exportDatabase(context: Context, outputPath: String): Boolean {
        return try {
            val db = HelldeckDb.getInstance(context)
            val dbPath = db.openHelper.writableDatabase.path
            val sourceFile = java.io.File(dbPath)

            val destinationFile = java.io.File(outputPath)
            destinationFile.parentFile?.mkdirs()

            sourceFile.copyTo(destinationFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Import database from backup
     */
    suspend fun importDatabase(context: Context, inputPath: String): Boolean {
        return try {
            val db = HelldeckDb.getInstance(context)
            val dbPath = db.openHelper.writableDatabase.path
            val destinationFile = java.io.File(dbPath)

            val sourceFile = java.io.File(inputPath)

            if (sourceFile.exists()) {
                // Close current database
                db.close()

                // Copy new database
                sourceFile.copyTo(destinationFile, overwrite = true)

                // Reinitialize singleton
                HelldeckDb.resetDatabase(context)

                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Optimize database performance
     */
    suspend fun optimizeDatabase(context: Context) {
        val db = HelldeckDb.getInstance(context)

        with(db) {
            // Run integrity check
            val isValid = integrityCheck()
            if (!isValid) {
                throw IllegalStateException("Database integrity check failed")
            }

            // Clean up old data
            cleanupOldData()

            // Run vacuum if needed (outside transaction)
            try {
                openHelper.writableDatabase.execSQL("VACUUM")
            } catch (e: Exception) {
                // VACUUM might fail in some cases, log but don't throw
            }
        }
    }
}