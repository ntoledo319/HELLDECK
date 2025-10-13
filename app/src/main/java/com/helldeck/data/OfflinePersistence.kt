package com.helldeck.data

import com.helldeck.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Offline-first data persistence for HELLDECK
 * Designed for old devices with limited storage and no network
 */
object OfflinePersistence {

    private lateinit var context: android.content.Context
    private lateinit var appDirectory: File
    private lateinit var brainpackDirectory: File
    private lateinit var tempDirectory: File

    fun initialize(ctx: android.content.Context) {
        context = ctx.applicationContext
        appDirectory = File(context.getExternalFilesDir(null), "Helldeck").apply { mkdirs() }
        brainpackDirectory = File(appDirectory, "Brainpacks").apply { mkdirs() }
        tempDirectory = File(context.cacheDir, "helldeck_temp").apply { mkdirs() }

        OfflineLearning.initialize(ctx)

        Logger.i("OfflinePersistence initialized: ${appDirectory.absolutePath}")
    }

    /**
     * Export complete brainpack (learned data)
     */
    suspend fun exportBrainpack(filename: String? = null): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val actualFilename = filename ?: "helldeck_brainpack_${timestamp}.hhdb"

                val brainpackFile = File(brainpackDirectory, actualFilename)

                Logger.i("Exporting brainpack to: ${brainpackFile.absolutePath}")

                ZipOutputStream(FileOutputStream(brainpackFile)).use { zip ->
                    // Export database
                    exportDatabaseToZip(zip)

                    // Export player data
                    exportPlayerDataToZip(zip)

                    // Export settings
                    exportSettingsToZip(zip)

                    // Export metadata
                    exportMetadataToZip(zip, timestamp)
                }

                Logger.i("Brainpack exported successfully: ${brainpackFile.length()} bytes")
                Result.success(brainpackFile)

            } catch (e: Exception) {
                Logger.e("Failed to export brainpack", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Import brainpack from file
     */
    suspend fun importBrainpack(file: File): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.i("Importing brainpack from: ${file.absolutePath}")

                // Create temp directory for extraction
                val extractDir = File(tempDirectory, "import_${System.currentTimeMillis()}")
                extractDir.mkdirs()

                // Extract zip contents
                ZipInputStream(FileInputStream(file)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val entryFile = File(extractDir, entry.name)
                        entryFile.parentFile?.mkdirs()

                        FileOutputStream(entryFile).use { output ->
                            zip.copyTo(output)
                        }

                        entry = zip.nextEntry
                    }
                }

                // Import database
                val dbFile = File(extractDir, "database/helldeck.db")
                if (dbFile.exists()) {
                    DatabaseOperations.importDatabase(context, dbFile.absolutePath)
                }

                // Import player data
                importPlayerData(extractDir)

                // Import settings
                importSettings(extractDir)

                // Clean up temp files
                extractDir.deleteRecursively()

                Logger.i("Brainpack imported successfully")
                Result.success(true)

            } catch (e: Exception) {
                Logger.e("Failed to import brainpack", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Export database to zip
     */
    private suspend fun exportDatabaseToZip(zip: ZipOutputStream) {
        val db = com.helldeck.data.HelldeckDb.getInstance(context)
        val dbPath = db.openHelper.writableDatabase.path
        val dbFile = File(dbPath)

        if (dbFile.exists()) {
            zip.putNextEntry(ZipEntry("database/helldeck.db"))
            FileInputStream(dbFile).use { it.copyTo(zip) }
            zip.closeEntry()

            Logger.d("Exported database: ${dbFile.length()} bytes")
        }
    }

    /**
     * Export player data to zip
     */
    private suspend fun exportPlayerDataToZip(zip: ZipOutputStream) {
        val playerData = com.helldeck.engine.PlayerManager.exportPlayerData()
        val playerJson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            .toJson(playerData)

        zip.putNextEntry(ZipEntry("players/players.json"))
        zip.write(playerJson.toByteArray())
        zip.closeEntry()

        Logger.d("Exported player data: ${playerJson.length} characters")
    }

    /**
     * Export settings to zip
     */
    private suspend fun exportSettingsToZip(zip: ZipOutputStream) {
        val db = com.helldeck.data.HelldeckDb.getInstance(context)
        val settings = db.settings().getAllSettings()
        val settingsJson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            .toJson(settings)

        zip.putNextEntry(ZipEntry("settings/settings.json"))
        zip.write(settingsJson.toByteArray())
        zip.closeEntry()

        Logger.d("Exported settings: ${settingsJson.length} characters")
    }

    /**
     * Export metadata to zip
     */
    private suspend fun exportMetadataToZip(zip: ZipOutputStream, timestamp: Long) {
        val metadata = mapOf(
            "exportTime" to timestamp,
            "appVersion" to getAppVersion(),
            "deviceModel" to android.os.Build.MODEL,
            "androidVersion" to android.os.Build.VERSION.SDK_INT,
            "playerCount" to com.helldeck.engine.PlayerManager.getPlayerCount(),
            "brainSize" to estimateBrainSize()
        )

        val metadataJson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            .toJson(metadata)

        zip.putNextEntry(ZipEntry("metadata.json"))
        zip.write(metadataJson.toByteArray())
        zip.closeEntry()

        Logger.d("Exported metadata: ${metadataJson.length} characters")
    }

    /**
     * Import player data from extracted directory
     */
    private suspend fun importPlayerData(extractDir: File) {
        val playerFile = File(extractDir, "players/players.json")
        if (playerFile.exists()) {
            try {
                val playerJson = playerFile.readText()
                val playerData = com.google.gson.GsonBuilder().create()
                    .fromJson(playerJson, Map::class.java) as Map<String, Any>

                com.helldeck.engine.PlayerManager.importPlayerData(playerData)
                Logger.d("Imported player data from brainpack")
            } catch (e: Exception) {
                Logger.e("Failed to import player data", e)
            }
        }
    }

    /**
     * Import settings from extracted directory
     */
    private suspend fun importSettings(extractDir: File) {
        val settingsFile = File(extractDir, "settings/settings.json")
        if (settingsFile.exists()) {
            try {
                val settingsJson = settingsFile.readText()
                val settings = com.google.gson.GsonBuilder().create()
                    .fromJson(settingsJson, Map::class.java) as Map<String, Any>

                // Import settings into database
                val db = com.helldeck.data.HelldeckDb.getInstance(context)
                settings.forEach { (key, value) ->
                    if (value is String) {
                        db.settings().putString(key, value)
                    } else if (value is Number) {
                        db.settings().putInt(key, value.toInt())
                    } else if (value is Boolean) {
                        db.settings().putBoolean(key, value)
                    }
                }

                Logger.d("Imported settings from brainpack")
            } catch (e: Exception) {
                Logger.e("Failed to import settings", e)
            }
        }
    }

    /**
     * Get app version for metadata
     */
    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Estimate brain size for metadata
     */
    private suspend fun estimateBrainSize(): Long {
        return try {
            val db = com.helldeck.data.HelldeckDb.getInstance(context)
            val dbPath = db.openHelper.writableDatabase.path
            File(dbPath).length()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * List all available brainpacks
     */
    fun listBrainpacks(): List<File> {
        return brainpackDirectory.listFiles { file ->
            file.extension.lowercase() == "hhdb" || file.extension.lowercase() == "zip"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Delete a brainpack file
     */
    fun deleteBrainpack(file: File): Boolean {
        return try {
            val deleted = file.delete()
            if (deleted) {
                Logger.i("Deleted brainpack: ${file.name}")
            } else {
                Logger.w("Failed to delete brainpack: ${file.name}")
            }
            deleted
        } catch (e: Exception) {
            Logger.e("Error deleting brainpack", e)
            false
        }
    }

    /**
     * Get brainpack file info
     */
    fun getBrainpackInfo(file: File): BrainpackInfo? {
        return try {
            val metadataFile = File(file.parent, "metadata.json") // This would need proper extraction

            BrainpackInfo(
                filename = file.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                isValid = file.length() > 0
            )
        } catch (e: Exception) {
            Logger.e("Error getting brainpack info", e)
            null
        }
    }

    /**
     * Clean up old temporary files
     */
    fun cleanupTempFiles() {
        try {
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
            tempDirectory.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.deleteRecursively()
                }
            }
            Logger.d("Cleaned up temporary files")
        } catch (e: Exception) {
            Logger.e("Error cleaning up temp files", e)
        }
    }

    /**
     * Get storage usage information
     */
    fun getStorageInfo(): StorageInfo {
        val totalSpace = appDirectory.totalSpace
        val freeSpace = appDirectory.freeSpace
        val usedSpace = totalSpace - freeSpace

        return StorageInfo(
            totalBytes = totalSpace,
            usedBytes = usedSpace,
            freeBytes = freeSpace,
            brainpackCount = listBrainpacks().size,
            brainpackTotalSize = listBrainpacks().sumOf { it.length() }
        )
    }

    /**
     * Optimize storage usage
     */
    suspend fun optimizeStorage() {
        withContext(Dispatchers.IO) {
            try {
                Logger.i("Optimizing storage")

                // Clean up old brainpacks (keep last 10)
                val brainpacks = listBrainpacks()
                if (brainpacks.size > 10) {
                    brainpacks.takeLast(brainpacks.size - 10).forEach { file ->
                        deleteBrainpack(file)
                    }
                }

                // Clean up temp files
                cleanupTempFiles()

                // Optimize database
                com.helldeck.data.DatabaseOperations.optimizeDatabase(context)

                Logger.i("Storage optimization completed")
            } catch (e: Exception) {
                Logger.e("Storage optimization failed", e)
            }
        }
    }
}

/**
 * Brainpack information
 */
data class BrainpackInfo(
    val filename: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isValid: Boolean
)

/**
 * Storage information
 */
data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val brainpackCount: Int,
    val brainpackTotalSize: Long
)

/**
 * Offline analytics for learning system
 */
object OfflineAnalytics {

    private lateinit var context: android.content.Context
    private lateinit var analyticsFile: File
    private val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()

    fun initialize(ctx: android.content.Context) {
        context = ctx.applicationContext
        analyticsFile = File(ctx.getExternalFilesDir(null), "Helldeck/analytics.json")
        analyticsFile.parentFile?.mkdirs()
    }

    /**
     * Record a game event for learning
     */
    fun recordGameEvent(event: com.helldeck.engine.GameEvent) {
        try {
            val analytics = loadAnalytics()
            // Convert engine GameEvent to AnalyticsEvent
            val analyticsEvent = AnalyticsEvent(
                type = event.eventType.name,
                data = event.data,
                timestamp = event.timestamp
            )
            analytics.events.add(analyticsEvent)

            // Keep only last 1000 events to manage file size
            if (analytics.events.size > 1000) {
                val recentEvents = analytics.events.takeLast(1000)
                analytics.events.clear()
                analytics.events.addAll(recentEvents)
            }

            saveAnalytics(analytics)
        } catch (e: Exception) {
            Logger.e("Failed to record game event", e)
        }
    }

    /**
     * Record template performance
     */
    fun recordTemplatePerformance(templateId: String, score: Double, feedback: String) {
        val event = AnalyticsEvent(
            type = "template_performance",
            data = mapOf(
                "templateId" to templateId,
                "score" to score,
                "feedback" to feedback,
                "timestamp" to System.currentTimeMillis()
            )
        )
        // Convert to engine GameEvent for recording
        val engineEvent = com.helldeck.engine.GameEvent(
            eventType = com.helldeck.engine.GameEventType.TEMPLATE_PERFORMANCE,
            gameId = "analytics",
            playerId = null,
            data = event.data,
            timestamp = event.timestamp
        )
        recordGameEvent(engineEvent)
    }

    /**
     * Record player action
     */
    fun recordPlayerAction(playerId: String, action: String, data: Map<String, Any> = emptyMap()) {
        val event = AnalyticsEvent(
            type = "player_action",
            data = mapOf(
                "playerId" to playerId,
                "action" to action,
                "timestamp" to System.currentTimeMillis()
            ) + data
        )
        // Convert to engine GameEvent
        val engineEvent = com.helldeck.engine.GameEvent(
            eventType = com.helldeck.engine.GameEventType.PLAYER_JOINED,
            gameId = "analytics",
            playerId = playerId,
            data = event.data,
            timestamp = event.timestamp
        )
        recordGameEvent(engineEvent)
    }

    /**
     * Get analytics summary
     */
    fun getAnalyticsSummary(): AnalyticsSummary {
        return try {
            val analytics = loadAnalytics()

            val templatePerformances = analytics.events
                .filter { it.type == "template_performance" }
                .groupBy { it.data["templateId"] as String }
                .mapValues { (_, events) ->
                    events.mapNotNull { it.data["score"] as? Double }.average()
                }

            val playerActions = analytics.events
                .filter { it.type == "player_action" }
                .groupBy { it.data["playerId"] as String }
                .mapValues { (_, events) ->
                    events.groupBy { it.data["action"] as String }.mapValues { it.value.size }
                }

            AnalyticsSummary(
                totalEvents = analytics.events.size,
                templatePerformances = templatePerformances,
                playerActions = playerActions,
                dateRange = getDateRange(analytics.events)
            )
        } catch (e: Exception) {
            Logger.e("Failed to get analytics summary", e)
            AnalyticsSummary()
        }
    }

    /**
     * Export analytics data
     */
    internal fun exportAnalytics(): Map<String, Any> {
        return try {
            val analytics = loadAnalytics()
            mapOf(
                "events" to analytics.events,
                "exportTime" to System.currentTimeMillis(),
                "totalEvents" to analytics.events.size
            )
        } catch (e: Exception) {
            Logger.e("Failed to export analytics", e)
            emptyMap()
        }
    }

    /**
     * Clear old analytics data
     */
    fun clearOldAnalytics(daysToKeep: Int = 30) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            val analytics = loadAnalytics()

            val filteredEvents = analytics.events.filter { event ->
                (event.data["timestamp"] as? Number)?.toLong() ?: 0L > cutoffTime
            }
            analytics.events.clear()
            analytics.events.addAll(filteredEvents)

            saveAnalytics(analytics)
            Logger.d("Cleared analytics older than $daysToKeep days")
        } catch (e: Exception) {
            Logger.e("Failed to clear old analytics", e)
        }
    }

    /**
     * Load analytics from file
     */
    private fun loadAnalytics(): AnalyticsData {
        return try {
            if (analyticsFile.exists()) {
                val json = analyticsFile.readText()
                gson.fromJson(json, AnalyticsData::class.java) ?: AnalyticsData()
            } else {
                AnalyticsData()
            }
        } catch (e: Exception) {
            Logger.e("Failed to load analytics", e)
            AnalyticsData()
        }
    }

    /**
     * Save analytics to file
     */
    private fun saveAnalytics(analytics: AnalyticsData) {
        try {
            val json = gson.toJson(analytics)
            analyticsFile.writeText(json)
        } catch (e: Exception) {
            Logger.e("Failed to save analytics", e)
        }
    }

    /**
     * Get date range from events
     */
    private fun getDateRange(events: List<AnalyticsEvent>): DateRange? {
        val timestamps = events.mapNotNull { event ->
            (event.data["timestamp"] as? Number)?.toLong()
        }

        return if (timestamps.isNotEmpty()) {
            DateRange(
                startTime = timestamps.min(),
                endTime = timestamps.max()
            )
        } else {
            null
        }
    }
}

/**
 * Analytics event for tracking
 */
internal data class AnalyticsEvent(
    val type: String,
    val data: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Analytics data container
 */
internal data class AnalyticsData(
    val events: MutableList<AnalyticsEvent> = mutableListOf()
)

/**
 * Analytics summary
 */
data class AnalyticsSummary(
    val totalEvents: Int = 0,
    val templatePerformances: Map<String, Double> = emptyMap(),
    val playerActions: Map<String, Map<String, Int>> = emptyMap(),
    val dateRange: DateRange? = null
)

/**
 * Date range for analytics
 */
data class DateRange(
    val startTime: Long,
    val endTime: Long
)

/**
 * Offline learning system using local analytics
 */
object OfflineLearning {

    private lateinit var context: android.content.Context

    fun initialize(ctx: android.content.Context) {
        context = ctx.applicationContext
    }

    /**
     * Update template scores based on local analytics
     */
    suspend fun updateTemplateScores() {
        try {
            val summary = OfflineAnalytics.getAnalyticsSummary()
            val templatePerformances = summary.templatePerformances

            if (templatePerformances.isNotEmpty()) {
                val db = com.helldeck.data.HelldeckDb.getInstance(context)
                // Update template scores in database
                // TODO: Implement actual template score updates
                Logger.d("Updated template scores from analytics")
            }
        } catch (e: Exception) {
            Logger.e("Failed to update template scores", e)
        }
    }

    /**
     * Get learning insights from local data
     */
    fun getLearningInsights(): LearningInsights {
        return try {
            val summary = OfflineAnalytics.getAnalyticsSummary()

            val bestTemplates = summary.templatePerformances
                .filter { it.value > 1.0 }
                .entries
                .sortedByDescending { it.value }
                .take(5)

            val worstTemplates = summary.templatePerformances
                .filter { it.value < 0.5 }
                .entries
                .sortedBy { it.value }
                .take(5)

            LearningInsights(
                bestPerformingTemplates = bestTemplates.map { it.key to it.value },
                worstPerformingTemplates = worstTemplates.map { it.key to it.value },
                totalFeedbackEvents = summary.totalEvents,
                learningProgress = calculateLearningProgress(summary)
            )
        } catch (e: Exception) {
            Logger.e("Failed to get learning insights", e)
            LearningInsights()
        }
    }

    /**
     * Calculate learning progress over time
     */
    private fun calculateLearningProgress(summary: AnalyticsSummary): Double {
        // Simple progress calculation based on feedback volume
        return (summary.totalEvents / 1000.0).coerceAtMost(1.0)
    }
}

/**
 * Learning insights
 */
data class LearningInsights(
    val bestPerformingTemplates: List<Pair<String, Double>> = emptyList(),
    val worstPerformingTemplates: List<Pair<String, Double>> = emptyList(),
    val totalFeedbackEvents: Int = 0,
    val learningProgress: Double = 0.0
)

/**
 * Offline backup system
 */
object OfflineBackup {

    /**
     * Create automatic backup
     */
    suspend fun createAutomaticBackup(): Result<File> {
        val timestamp = System.currentTimeMillis()
        val filename = "helldeck_auto_backup_${timestamp}.hhdb"
        return OfflinePersistence.exportBrainpack(filename)
    }

    /**
     * Restore from latest backup
     */
    suspend fun restoreFromLatestBackup(): Result<Boolean> {
        return try {
            val backups = OfflinePersistence.listBrainpacks()
            if (backups.isNotEmpty()) {
                Result.success(OfflinePersistence.importBrainpack(backups.first()).isSuccess)
            } else {
                Result.failure(Exception("No backups found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get backup recommendations
     */
    fun getBackupRecommendations(): List<String> {
        val storageInfo = OfflinePersistence.getStorageInfo()
        val recommendations = mutableListOf<String>()

        if (storageInfo.brainpackCount == 0) {
            recommendations.add("Create your first brainpack backup to preserve learned data")
        }

        if (storageInfo.freeBytes < 50 * 1024 * 1024) { // Less than 50MB free
            recommendations.add("Free up storage space for optimal performance")
        }

        if (storageInfo.brainpackCount > 10) {
            recommendations.add("Consider cleaning up old brainpack files")
        }

        return recommendations
    }
}