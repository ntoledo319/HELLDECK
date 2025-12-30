package com.helldeck.content.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "template_exposures", indices = [Index(value = ["timestamp"])])
data class TemplateExposureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: String,
    val timestamp: Long,
)
