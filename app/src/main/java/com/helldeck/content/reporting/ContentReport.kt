package com.helldeck.content.reporting

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ContentReport(
    val cardText: String,
    val blueprintId: String?,
    val gameId: String,
    val reportReason: ReportReason,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
) {
    @Serializable
    enum class ReportReason {
        OFFENSIVE_LANGUAGE,
        HATE_SPEECH,
        SEXUALLY_EXPLICIT,
        VIOLENCE,
        HARASSMENT,
        OTHER
    }
}

@Serializable
data class ContentReportStore(
    val reports: List<ContentReport> = emptyList(),
    val reportedBlueprints: Set<String> = emptySet(),
    val reportedCardTexts: Set<String> = emptySet(),
) {
    fun withReport(report: ContentReport): ContentReportStore {
        val updatedReports = reports + report
        val updatedBlueprints = if (report.blueprintId != null) {
            reportedBlueprints + report.blueprintId
        } else {
            reportedBlueprints
        }
        val updatedTexts = reportedCardTexts + report.cardText.lowercase()
        
        return copy(
            reports = updatedReports,
            reportedBlueprints = updatedBlueprints,
            reportedCardTexts = updatedTexts,
        )
    }
    
    fun isCardReported(text: String): Boolean = 
        text.lowercase() in reportedCardTexts
    
    fun isBlueprintReported(blueprintId: String): Boolean = 
        blueprintId in reportedBlueprints
    
    fun getReportCount(): Int = reports.size
    
    fun getRecentReports(limit: Int = 10): List<ContentReport> = 
        reports.sortedByDescending { it.timestamp }.take(limit)

    companion object {
        private const val FILENAME = "content_reports.json"
        private val json = Json { prettyPrint = true }

        fun load(context: Context): ContentReportStore {
            return try {
                val file = File(context.filesDir, FILENAME)
                if (file.exists()) {
                    val content = file.readText()
                    json.decodeFromString<ContentReportStore>(content)
                } else {
                    ContentReportStore()
                }
            } catch (e: Exception) {
                ContentReportStore()
            }
        }

        fun save(context: Context, store: ContentReportStore) {
            try {
                val file = File(context.filesDir, FILENAME)
                val content = json.encodeToString(store)
                file.writeText(content)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }

        fun clear(context: Context) {
            try {
                val file = File(context.filesDir, FILENAME)
                file.delete()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
