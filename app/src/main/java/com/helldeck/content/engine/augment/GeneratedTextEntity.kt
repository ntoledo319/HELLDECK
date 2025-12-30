package com.helldeck.content.engine.augment

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_text")
data class GeneratedTextEntity(
    @PrimaryKey val key: String, // hash(task|model|template|fill|seed)
    val text: String,
    val createdAt: Long,
)
