package com.helldeck.content.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        com.helldeck.data.FraudQuarantineEntity::class
    ],
    version = 5,
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

    companion object {
        @Volatile private var INSTANCE: HelldeckDb? = null

        fun get(context: Context): HelldeckDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HelldeckDb::class.java,
                    "helldeck.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
