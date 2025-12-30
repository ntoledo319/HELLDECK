package com.helldeck.content.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Template statistics entity for tracking template performance.
 *
 * Stores aggregated statistics about template usage and user feedback.
 * Used by the ContextualSelector to inform template selection decisions.
 *
 * @property templateId Unique identifier for the template
 * @property visits Number of times this template has been shown
 * @property rewardSum Sum of normalized rewards (0.0-1.0) from user feedback
 */
@Entity(tableName = "template_stats", indices = [Index(value = ["templateId"], unique = true)])
data class TemplateStatEntity(
    @PrimaryKey val templateId: String,
    val visits: Int,
    val rewardSum: Double,
)
