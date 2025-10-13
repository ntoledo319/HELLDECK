package com.helldeck.engine

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.helldeck.AppCtx
import com.helldeck.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Export/Import manager for HELLDECK brainpack files
 * Handles saving and loading game learning data
 */
object ExportImport {

    internal const val BRAINPACK_VERSION = 1
    internal const val METADATA_FILENAME = "metadata.json"
    internal const val DATABASE_FILENAME = "database/helldeck.db"
    private const val SETTINGS_FILENAME = "settings/settings.json"

    /**
     * Export brainpack to URI
     */
    suspend fun exportBrainpack(
        context: Context,
        filename: String = "helldeck_brainpack_${System.currentTimeMillis()}.zip"
    ): Uri {
        return try {
            val repo = Repository.get(context)
            val baseDir = File(context.getExternalFilesDir(null), "Helldeck/Exports").apply { mkdirs() }
            val outputFile = File(baseDir, filename)

            ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
                // Export database
                exportDatabase(context, zip)

                // Export settings
                exportSettings(repo, zip)

                // Export metadata
                exportMetadata(context, zip)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to export brainpack", e)
        }
    }

    /**
     * Import brainpack from URI
     */
    suspend fun importBrainpack(
        context: Context,
        uri: Uri
    ): ImportResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val repo = Repository.get(context)
            val tempDir = File(context.cacheDir, "brainpack_import_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                // Extract zip contents
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val file = File(tempDir, entry.name)
                            file.parentFile?.mkdirs()

                            FileOutputStream(file).use { output ->
                                zip.copyTo(output)
                            }

                            entry = zip.nextEntry
                        }
                    }
                }

                // Validate brainpack
                val metadata = validateBrainpack(tempDir)
                if (!metadata.isValid) {
                    return@withContext ImportResult.Failure("Invalid brainpack format: ${metadata.errors}")
                }

                // Import database
                val dbFile = File(tempDir, DATABASE_FILENAME)
                if (dbFile.exists()) {
                    importDatabase(context, dbFile)
                }

                // Import settings
                val settingsFile = File(tempDir, SETTINGS_FILENAME)
                if (settingsFile.exists()) {
                    importSettings(repo, settingsFile)
                }

                // Clean up temp files
                tempDir.deleteRecursively()

                ImportResult.Success(
                    templatesImported = metadata.templateCount,
                    playersImported = metadata.playerCount,
                    roundsImported = metadata.roundCount,
                    version = metadata.version
                )

            } catch (e: Exception) {
                tempDir.deleteRecursively()
                ImportResult.Failure("Import failed: ${e.message}")
            }

        } catch (e: Exception) {
            ImportResult.Failure("Import failed: ${e.message}")
        }
    }

    /**
     * Export database to zip
     */
    private fun exportDatabase(context: Context, zip: ZipOutputStream) {
        val dbPath = context.getDatabasePath("helldeck.db")
        if (!dbPath.exists()) return

        zip.putNextEntry(ZipEntry(DATABASE_FILENAME))
        FileInputStream(dbPath).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    /**
     * Export settings to zip
     */
    private suspend fun exportSettings(repo: Repository, zip: ZipOutputStream) {
        try {
            val settings = repo.db.settings().getAllSettings()
            val settingsJson = com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(settings)

            zip.putNextEntry(ZipEntry(SETTINGS_FILENAME))
            zip.write(settingsJson.toByteArray())
            zip.closeEntry()
        } catch (e: Exception) {
            // Settings export failed, continue without settings
        }
    }

    /**
     * Export metadata to zip
     */
    private fun exportMetadata(context: Context, zip: ZipOutputStream) {
        val metadata = BrainpackMetadata(
            version = BRAINPACK_VERSION,
            exportTime = System.currentTimeMillis(),
            appVersion = getAppVersion(context),
            databaseVersion = 1,
            templateCount = 0, // Would calculate from database
            playerCount = 0,   // Would calculate from database
            roundCount = 0,    // Would calculate from database
            gameVersion = "1.0.0",
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.SDK_INT.toString()
        )

        val metadataJson = com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(metadata)

        zip.putNextEntry(ZipEntry(METADATA_FILENAME))
        zip.write(metadataJson.toByteArray())
        zip.closeEntry()
    }

    /**
     * Import database from file
     */
    private suspend fun importDatabase(context: Context, dbFile: File) {
        val currentDb = context.getDatabasePath("helldeck.db")
        val backupDb = File(currentDb.parent, "helldeck_backup.db")

        try {
            // Backup current database
            if (currentDb.exists()) {
                currentDb.copyTo(backupDb, overwrite = true)
            }

            // Copy new database
            dbFile.copyTo(currentDb, overwrite = true)

            // Test new database
            if (!testDatabaseIntegrity(currentDb)) {
                throw Exception("Imported database is corrupted")
            }

        } catch (e: Exception) {
            // Restore backup if import failed
            if (backupDb.exists()) {
                backupDb.copyTo(currentDb, overwrite = true)
            }
            throw e
        } finally {
            // Clean up backup
            backupDb.delete()
        }
    }

    /**
     * Import settings from file
     */
    private suspend fun importSettings(repo: Repository, settingsFile: File) {
        try {
            val settingsJson = settingsFile.readText()
            val settings = com.google.gson.Gson().fromJson(
                settingsJson,
                Array<com.helldeck.data.SettingEntity>::class.java
            ).toList()

            // Import settings
            settings.forEach { setting ->
                repo.db.settings().putString(setting.key, setting.value)
            }
        } catch (e: Exception) {
            // Settings import failed, continue without settings
        }
    }

    /**
     * Validate brainpack structure and contents
     */
    private fun validateBrainpack(tempDir: File): BrainpackMetadata {
        return try {
            val metadataFile = File(tempDir, METADATA_FILENAME)
            if (!metadataFile.exists()) {
                return BrainpackMetadata(isValid = false, errors = listOf("Missing metadata file"))
            }

            val metadataJson = metadataFile.readText()
            val metadata = com.google.gson.Gson().fromJson(
                metadataJson,
                BrainpackMetadata::class.java
            )

            val errors = mutableListOf<String>()

            // Validate version compatibility
            if (metadata.version > BRAINPACK_VERSION) {
                errors.add("Brainpack version ${metadata.version} is newer than supported version $BRAINPACK_VERSION")
            }

            // Check required files
            val dbFile = File(tempDir, DATABASE_FILENAME)
            if (!dbFile.exists()) {
                errors.add("Missing database file")
            } else {
                // Validate database integrity
                if (!testDatabaseIntegrity(dbFile)) {
                    errors.add("Database file is corrupted")
                }
            }

            metadata.copy(
                isValid = errors.isEmpty(),
                errors = errors
            )

        } catch (e: Exception) {
            BrainpackMetadata(
                isValid = false,
                errors = listOf("Validation failed: ${e.message}")
            )
        }
    }

    /**
     * Test database file integrity
     */
    private fun testDatabaseIntegrity(dbFile: File): Boolean {
        return try {
            // Try to open database and run a simple query
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
                val isValid = cursor.moveToFirst()
                cursor.close()
                isValid
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get app version
     */
    private fun getAppVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Clean up old export files
     */
    fun cleanupOldExports(context: Context, retentionDays: Int = 7) {
        try {
            val exportDir = File(context.getExternalFilesDir(null), "Helldeck/Exports")
            if (!exportDir.exists()) return

            val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

            exportDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Cleanup failed, not critical
        }
    }

    /**
     * Get export directory size
     */
    fun getExportDirectorySize(context: Context): Long {
        return try {
            val exportDir = File(context.getExternalFilesDir(null), "Helldeck/Exports")
            if (!exportDir.exists()) return 0L

            exportDir.walkTopDown().sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * Brainpack metadata structure
 */
data class BrainpackMetadata(
    val version: Int = ExportImport.BRAINPACK_VERSION,
    val exportTime: Long = System.currentTimeMillis(),
    val appVersion: String = "unknown",
    val databaseVersion: Int = 1,
    val templateCount: Int = 0,
    val playerCount: Int = 0,
    val roundCount: Int = 0,
    val gameVersion: String = "1.0.0",
    val deviceModel: String = android.os.Build.MODEL,
    val androidVersion: String = android.os.Build.VERSION.SDK_INT.toString(),
    val isValid: Boolean = true,
    val errors: List<String> = emptyList()
)

/**
 * Import result sealed class
 */
sealed class ImportResult {
    data class Success(
        val templatesImported: Int,
        val playersImported: Int,
        val roundsImported: Int,
        val version: Int
    ) : ImportResult()

    data class Failure(
        val error: String
    ) : ImportResult()

    object Cancelled : ImportResult()
}

/**
 * Export progress callback
 */
interface ExportProgressCallback {
    fun onProgress(current: Long, total: Long)
    fun onComplete(success: Boolean, error: String?)
}

/**
 * Import progress callback
 */
interface ImportProgressCallback {
    fun onProgress(current: Long, total: Long)
    fun onComplete(result: ImportResult)
}

/**
 * Brainpack utilities
 */
object BrainpackUtils {

    /**
     * Generate brainpack filename with timestamp
     */
    fun generateBrainpackFilename(prefix: String = "helldeck_brainpack"): String {
        val timestamp = System.currentTimeMillis()
        return "${prefix}_${timestamp}.zip"
    }

    /**
     * Validate brainpack file
     */
    fun validateBrainpackFile(file: File): ValidationResult {
        return try {
            if (!file.exists()) {
                return ValidationResult(false, listOf("File does not exist"))
            }

            if (file.extension != "zip") {
                return ValidationResult(false, listOf("File must be a zip archive"))
            }

            val tempDir = kotlin.io.path.createTempDirectory().toFile()
            try {
                ZipInputStream(FileInputStream(file)).use { zip ->
                    var entry = zip.nextEntry
                    var hasMetadata = false
                    var hasDatabase = false

                    while (entry != null) {
                        when (entry.name) {
                            ExportImport.METADATA_FILENAME -> hasMetadata = true
                            ExportImport.DATABASE_FILENAME -> hasDatabase = true
                        }
                        entry = zip.nextEntry
                    }

                    val errors = mutableListOf<String>()
                    if (!hasMetadata) errors.add("Missing metadata file")
                    if (!hasDatabase) errors.add("Missing database file")

                    ValidationResult(
                        isValid = errors.isEmpty(),
                        errors = errors
                    )
                }
            } finally {
                tempDir.deleteRecursively()
            }

        } catch (e: Exception) {
            ValidationResult(false, listOf("Invalid file format: ${e.message}"))
        }
    }

    /**
     * Get brainpack file size in MB
     */
    fun getBrainpackSizeMB(file: File): Double {
        return if (file.exists()) {
            file.length().toDouble() / (1024 * 1024)
        } else {
            0.0
        }
    }

    /**
     * Compress brainpack if needed
     */
    fun compressBrainpack(inputFile: File, outputFile: File): Boolean {
        return try {
            // For now, just copy - in a real implementation you might want to compress further
            inputFile.copyTo(outputFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
}