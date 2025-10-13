package com.helldeck.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.helldeck.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Repository class providing high-level data operations
 * Acts as a facade over the Room database and file operations
 */
class Repository private constructor(private val ctx: Context) {

    internal val db = HelldeckDb.getInstance(ctx)
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    // DAO accessors
    fun templates() = db.templates()
    fun rounds() = db.rounds()
    fun comments() = db.comments()
    fun players() = db.players()
    fun lexicons() = db.lexicons()
    fun settings() = db.settings()
    fun sessions() = db.sessions()

    // Template operations
    suspend fun loadTemplatesFromAssets(assetPath: String = "templates/templates.json") {
        return try {
            Logger.d("Loading templates from assets: $assetPath")
            val raw = ctx.assets.open(assetPath).bufferedReader().use { it.readText() }
            val templateDefs = gson.fromJson<List<TemplateDef>>(raw, object : TypeToken<List<TemplateDef>>() {}.type)

            if (templateDefs.isNullOrEmpty()) {
                Logger.w("No templates found in $assetPath")
                return
            }

            val entities = templateDefs.map { def ->
                TemplateEntity(
                    id = def.id,
                    game = def.game,
                    text = def.text,
                    family = def.family,
                    spice = def.spice,
                    locality = def.locality,
                    maxWords = def.maxWords
                )
            }

            db.templates().insertAll(entities)
            Logger.i("Successfully loaded ${entities.size} templates from $assetPath")
        } catch (e: Exception) {
            Logger.e("Failed to load templates from assets: $assetPath", e)
            throw RuntimeException("Failed to load templates from assets: $assetPath", e)
        }
    }

    suspend fun loadLexiconFromAssets(name: String, assetPath: String) {
        try {
            val raw = ctx.assets.open(assetPath).bufferedReader().use { it.readText() }
            val words = gson.fromJson<List<String>>(raw, object : TypeToken<List<String>>() {}.type)

            val entity = LexiconEntity(
                name = name,
                dataJson = gson.toJson(words),
                updatedTs = System.currentTimeMillis(),
                size = words.size
            )

            db.lexicons().insert(entity)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load lexicon from assets: $assetPath", e)
        }
    }

    suspend fun getLexicon(name: String): List<String> {
        return try {
            val entity = db.lexicons().getLexicon(name)
            if (entity != null) {
                gson.fromJson(entity.dataJson, object : TypeToken<List<String>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRandomWordsFromLexicon(lexiconName: String, count: Int = 1): List<String> {
        val words = getLexicon(lexiconName)
        return if (words.isNotEmpty()) {
            words.shuffled().take(count)
        } else {
            emptyList()
        }
    }

    suspend fun upsertTemplates(templates: List<TemplateEntity>) {
        db.templates().insertAll(templates)
    }

    // Player operations
    suspend fun getActivePlayers(): Flow<List<PlayerEntity>> {
        return db.players().getActivePlayers()
    }

    suspend fun getAllPlayers(): Flow<List<PlayerEntity>> {
        return db.players().getAllPlayers()
    }

    suspend fun addPlayer(name: String, avatar: String): PlayerEntity {
        val player = PlayerEntity(
            id = generatePlayerId(),
            name = name,
            avatar = avatar,
            sessionPoints = 0,
            totalPoints = 0,
            elo = 1000,
            gamesPlayed = 0,
            wins = 0
        )
        db.players().insert(player)
        return player
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        db.players().update(player)
    }

    suspend fun addPointsToPlayer(playerId: String, points: Int) {
        db.players().addPointsToPlayer(playerId, points)
    }

    suspend fun setPlayerAfk(playerId: String, afk: Boolean) {
        db.players().setPlayerAfk(playerId, if (afk) 1 else 0)
    }

    // Round operations
    suspend fun recordRound(
        game: String,
        templateId: String,
        fillsJson: String,
        lol: Int,
        meh: Int,
        trash: Int,
        judgeWin: Boolean,
        points: Int,
        latencyMs: Int,
        notes: String? = null,
        playerCount: Int = 0,
        roomHeat: Double = 0.0
    ): Long {
        val round = RoundEntity(
            ts = System.currentTimeMillis(),
            game = game,
            templateId = templateId,
            fillsJson = fillsJson,
            lol = lol,
            meh = meh,
            trash = trash,
            judgeWin = if (judgeWin) 1 else 0,
            points = points,
            latencyMs = latencyMs,
            notes = notes,
            playerCount = playerCount,
            roomHeat = roomHeat
        )
        return db.rounds().insert(round)
    }

    suspend fun addComment(roundId: Long, text: String, tags: String) {
        val comment = CommentEntity(
            roundId = roundId,
            text = text,
            tags = tags
        )
        db.comments().insert(comment)
    }

    // Settings operations
    suspend fun getSetting(key: String, defaultValue: String = ""): String {
        return db.settings().getString(key, defaultValue)
    }

    suspend fun getSettingInt(key: String, defaultValue: Int = 0): Int {
        return db.settings().getInt(key, defaultValue)
    }

    suspend fun getSettingBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return db.settings().getBoolean(key, defaultValue)
    }

    suspend fun putSetting(key: String, value: String) {
        db.settings().putString(key, value)
    }

    suspend fun putSetting(key: String, value: Int) {
        db.settings().putInt(key, value)
    }

    suspend fun putSetting(key: String, value: Boolean) {
        db.settings().putBoolean(key, value)
    }

    // Session operations
    suspend fun startNewSession(): Long {
        val session = GameSessionEntity(
            startTime = System.currentTimeMillis(),
            playerCount = db.players().getActivePlayerCount()
        )
        return db.sessions().insert(session)
    }

    suspend fun endSession(sessionId: Long, roundsPlayed: Int, totalPoints: Int) {
        db.sessions().endSession(
            sessionId = sessionId,
            endTime = System.currentTimeMillis(),
            roundsPlayed = roundsPlayed,
            totalPoints = totalPoints
        )
    }

    // Analytics operations
    suspend fun getGameStatistics(): Map<String, Any> {
        val totalRounds = db.rounds().getRoundCountSince(0L)
        val totalPlayers = db.players().getTotalPlayerCount()
        val gameCounts = db.rounds().getGamePlayCounts()

        return mapOf(
            "totalRounds" to totalRounds,
            "totalPlayers" to totalPlayers,
            "gamePlayCounts" to gameCounts,
            "averagePoints" to (db.rounds().getAveragePointsForGame("ALL_GAMES") ?: 0.0),
            "topScoringRounds" to db.rounds().getHighScoringRounds(5)
        )
    }

    // Export/Import operations
    suspend fun exportBrainpack(filename: String = "helldeck_brainpack_${System.currentTimeMillis()}.zip"): Uri {
        return try {
            val base = File(ctx.getExternalFilesDir(null), "Helldeck/Exports").apply { mkdirs() }
            val outFile = File(base, filename)

            ZipOutputStream(outFile.outputStream()).use { zip ->
                // Export database
                exportDatabaseToZip(zip, "database/helldeck.db")

                // Export settings
                exportSettingsToZip(zip)

                // Export metadata
                exportMetadataToZip(zip)
            }

            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", outFile)
        } catch (e: Exception) {
            throw RuntimeException("Failed to export brainpack", e)
        }
    }

    suspend fun importBrainpack(uri: Uri): Boolean {
        return try {
            val tempDir = File(ctx.cacheDir, "brainpack_import_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            // Extract zip contents
            ctx.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val file = File(tempDir, entry.name)
                        file.parentFile?.mkdirs()
                        file.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                        entry = zip.nextEntry
                    }
                }
            }

            // Import database if present
            val dbFile = File(tempDir, "database/helldeck.db")
            if (dbFile.exists()) {
                DatabaseOperations.importDatabase(ctx, dbFile.absolutePath)
            }

            // Clean up temp files
            tempDir.deleteRecursively()

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun exportDatabaseToZip(zip: ZipOutputStream, entryName: String) {
        val dbPath = db.openHelper.writableDatabase.path
        val dbFile = File(dbPath)

        zip.putNextEntry(ZipEntry(entryName))
        dbFile.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private suspend fun exportSettingsToZip(zip: ZipOutputStream) {
        val settings = db.settings().getAllSettings()
        val settingsJson = gson.toJson(settings)

        zip.putNextEntry(ZipEntry("settings/settings.json"))
        zip.write(settingsJson.toByteArray())
        zip.closeEntry()
    }

    private fun exportMetadataToZip(zip: ZipOutputStream) {
        val metadata = mapOf(
            "exportTime" to System.currentTimeMillis(),
            "appVersion" to getAppVersion(),
            "databaseVersion" to 1,
            "templateCount" to 0, // Would need async context
            "playerCount" to 0  // Would need async context
        )

        val metadataJson = gson.toJson(metadata)
        zip.putNextEntry(ZipEntry("metadata.json"))
        zip.write(metadataJson.toByteArray())
        zip.closeEntry()
    }

    private fun getAppVersion(): String {
        return try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
    }

    // Utility operations
    suspend fun resetAllData() {
        db.templates().deleteAll() // Would need to add this method
        db.rounds().deleteAll() // Would need to add this method
        db.players().resetSession()
        db.settings().deleteAll() // Would need to add this method
    }

    suspend fun optimizeDatabase() {
        DatabaseOperations.optimizeDatabase(ctx)
    }

    suspend fun getDatabaseStats(): DatabaseStats {
        return db.getDatabaseStats()
    }

    // Initialization
    suspend fun initialize() {
        // Load default templates if database is empty
        if (db.templates().getCountForGame("*") == 0) {
            loadTemplatesFromAssets()
        }

        // Load default lexicons if not present
        val requiredLexicons = listOf("friends", "places", "memes", "icks", "perks", "red_flags", "categories", "letters", "forbidden")
        requiredLexicons.forEach { lexiconName ->
            if (db.lexicons().getLexicon(lexiconName) == null) {
                try {
                    loadLexiconFromAssets(lexiconName, "lexicons/$lexiconName.json")
                } catch (e: Exception) {
                    // Log error but don't fail initialization
                }
            }
        }

        // Set default settings if not present
        setDefaultSettings()
    }

    private suspend fun setDefaultSettings() {
        val defaults = mapOf(
            "spicy_mode" to "false",
            "sound_enabled" to "true",
            "haptics_enabled" to "true",
            "learning_enabled" to "true",
            "auto_cleanup_days" to "30"
        )

        defaults.forEach { (key, value) ->
            if (db.settings().getSetting(key) == null) {
                db.settings().putString(key, value)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: Repository? = null

        fun get(ctx: Context): Repository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Repository(ctx.applicationContext).also { INSTANCE = it }
        }

        private fun generatePlayerId(): String {
            return "p${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}"
        }
    }
}

// Extension functions for easier access
suspend fun Repository.getPlayerById(id: String): PlayerEntity? = players().getPlayerById(id)
suspend fun Repository.getPlayerByName(name: String): PlayerEntity? = players().getPlayerByName(name)
suspend fun Repository.getActivePlayerCount(): Int = players().getActivePlayerCount()
suspend fun Repository.getTotalPlayerCount(): Int = players().getTotalPlayerCount()

// Flow extensions
fun Repository.getActivePlayersFlow(): Flow<List<PlayerEntity>> {
    return players().getActivePlayers()
}

fun Repository.getAllPlayersFlow(): Flow<List<PlayerEntity>> {
    return players().getAllPlayers()
}

fun Repository.getAllRoundsFlow(): Flow<List<RoundEntity>> = rounds().getAllRounds()