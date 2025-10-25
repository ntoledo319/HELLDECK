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
        com.helldeck.data.PlayerEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class HelldeckDb : RoomDatabase() {
    abstract fun templateStatDao(): TemplateStatDao
    abstract fun templateExposureDao(): TemplateExposureDao
    abstract fun generatedTextDao(): GeneratedTextDao
    abstract fun players(): com.helldeck.data.PlayerDao

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