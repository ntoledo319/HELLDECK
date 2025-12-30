package com.helldeck.engine

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.helldeck.content.db.HelldeckDb
import com.helldeck.content.db.TemplateExposureEntity
import com.helldeck.content.db.TemplateStatEntity
import com.helldeck.data.PlayerEntity
import com.helldeck.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles export & import of "brainpacks" (player history + template performance).
 */
object ExportImport {

    private const val EXPORT_VERSION = 1
    private const val ENTRY_NAME = "brainpack.json"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportBrainpack(context: Context, filename: String): Uri? = runBlocking {
        try {
            val db = HelldeckDb.get(context.applicationContext)
            val payload = withContext(Dispatchers.IO) {
                val players = db.players().getAllSnapshot()
                val stats = db.templateStatDao().getAll()
                val exposures = db.templateExposureDao().getAll()
                BrainpackPayload(
                    version = EXPORT_VERSION,
                    exportedAt = System.currentTimeMillis(),
                    players = players,
                    templateStats = stats,
                    templateExposure = exposures,
                )
            }

            val exportDir = File(context.cacheDir, "brainpacks").apply { mkdirs() }
            val targetFile = File(exportDir, filename)
            withContext(Dispatchers.IO) {
                ZipOutputStream(targetFile.outputStream()).use { zip ->
                    zip.putNextEntry(ZipEntry(ENTRY_NAME))
                    val json = gson.toJson(payload)
                    zip.write(json.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
            Uri.fromFile(targetFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Brainpack export failed", e)
            throw e
        }
    }

    fun importBrainpack(context: Context, uri: Uri): ImportResult = runBlocking {
        try {
            val payload = readPayload(context, uri) ?: return@runBlocking ImportResult.Failure("Invalid brainpack payload")
            val db = HelldeckDb.get(context.applicationContext)
            withContext(Dispatchers.IO) {
                db.withTransaction {
                    val playerDao = db.players()
                    val statDao = db.templateStatDao()
                    val exposureDao = db.templateExposureDao()

                    payload.players.forEach { playerDao.upsert(it) }
                    payload.templateStats.forEach { statDao.upsert(it) }
                    if (payload.templateExposure.isNotEmpty()) {
                        val sanitized = payload.templateExposure.map { it.copy(id = 0) }
                        exposureDao.insertAll(sanitized)
                    }
                }
            }
            ImportResult.Success(
                templatesImported = payload.templateStats.size,
                playersImported = payload.players.size,
                roundsImported = payload.templateExposure.size,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Brainpack import failed", e)
            ImportResult.Failure(e.message ?: "Unknown error")
        }
    }

    private suspend fun readPayload(context: Context, uri: Uri): BrainpackPayload? = withContext(Dispatchers.IO) {
        val stream = openInputStream(context, uri) ?: return@withContext null
        stream.use { input ->
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name == ENTRY_NAME) {
                        val json = zip.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        return@withContext gson.fromJson(json, BrainpackPayload::class.java)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        null
    }

    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return when (uri.scheme) {
            null, "file" -> File(uri.path ?: return null).inputStream()
            else -> context.contentResolver.openInputStream(uri)
        }
    }

    private data class BrainpackPayload(
        val version: Int,
        val exportedAt: Long,
        val players: List<PlayerEntity>,
        val templateStats: List<TemplateStatEntity>,
        val templateExposure: List<TemplateExposureEntity>,
    )
}

/**
 * Result of a brainpack import operation.
 */
sealed class ImportResult {
    data class Success(val templatesImported: Int, val playersImported: Int, val roundsImported: Int) : ImportResult()
    data class Failure(val error: String) : ImportResult()
    object Cancelled : ImportResult()
}
