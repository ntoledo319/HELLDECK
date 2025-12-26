package com.helldeck.content.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.helldeck.settings.CrewBrainStore

@Database(
    entities = [
        TemplateStatEntity::class,
        TemplateExposureEntity::class,
        com.helldeck.content.engine.augment.GeneratedTextEntity::class,
        com.helldeck.data.PlayerEntity::class,
        com.helldeck.data.HouseRuleEntity::class,
        com.helldeck.data.GroupDnaEntity::class,
        com.helldeck.data.HighlightEntity::class,
        com.helldeck.data.RemixRequestEntity::class,
        com.helldeck.data.PlayerRoleEntity::class,
        com.helldeck.data.PackSelectionEntity::class,
        com.helldeck.data.FraudQuarantineEntity::class,
        com.helldeck.data.SessionMetricsEntity::class,
        com.helldeck.data.RoundMetricsEntity::class,
        com.helldeck.data.FavoriteCardEntity::class,
        com.helldeck.data.CustomCardEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class HelldeckDb : RoomDatabase() {
    abstract fun templateStatDao(): TemplateStatDao
    abstract fun templateExposureDao(): TemplateExposureDao
    abstract fun generatedTextDao(): GeneratedTextDao
    abstract fun players(): com.helldeck.data.PlayerDao
    abstract fun houseRules(): com.helldeck.data.HouseRulesDao
    abstract fun groupDna(): com.helldeck.data.GroupDnaDao
    abstract fun highlights(): com.helldeck.data.HighlightsDao
    abstract fun remix(): com.helldeck.data.RemixDao
    abstract fun playerRoles(): com.helldeck.data.PlayerRolesDao
    abstract fun packs(): com.helldeck.data.PacksDao
    abstract fun fraud(): com.helldeck.data.FraudDao
    abstract fun sessionMetrics(): com.helldeck.data.SessionMetricsDao
    abstract fun roundMetrics(): com.helldeck.data.RoundMetricsDao
    abstract fun favorites(): com.helldeck.data.FavoritesDao
    abstract fun customCards(): com.helldeck.data.CustomCardsDao

    companion object {
        @Volatile private var instances: MutableMap<String, HelldeckDb> = mutableMapOf()

        private fun dbNameFor(brainId: String): String {
            return if (brainId == CrewBrainStore.DEFAULT_BRAIN_ID) {
                "helldeck.db"
            } else {
                "helldeck_${brainId}.db"
            }
        }

        private fun currentBrainId(): String {
            return runCatching { CrewBrainStore.activeBrainIdSync() }
                .getOrDefault(CrewBrainStore.DEFAULT_BRAIN_ID)
                .ifBlank { CrewBrainStore.DEFAULT_BRAIN_ID }
        }

        fun get(context: Context): HelldeckDb = getForBrain(context, currentBrainId())

        fun getForBrain(context: Context, brainId: String): HelldeckDb {
            val key = brainId.ifBlank { CrewBrainStore.DEFAULT_BRAIN_ID }
            return instances[key] ?: synchronized(this) {
                instances[key] ?: Room.databaseBuilder(
                    context.applicationContext,
                    HelldeckDb::class.java,
                    dbNameFor(key)
                ).fallbackToDestructiveMigration().build().also { db ->
                    instances[key] = db
                }
            }
        }

        fun clearCache() {
            synchronized(this) {
                instances.values.forEach { it.close() }
                instances.clear()
            }
        }
    }
}
