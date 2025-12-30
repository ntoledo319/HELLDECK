package com.helldeck.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.helldeck.AppCtx
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class CrewBrain(
    val id: String,
    val name: String,
    val emoji: String,
    val createdAtMs: Long,
    val lastUsedMs: Long,
)

object CrewBrainStore {
    const val MAX_BRAINS = 4
    const val DEFAULT_BRAIN_ID = "main"

    private val KEY_BRAINS = stringPreferencesKey("crew_brains_json")
    private val KEY_ACTIVE = stringPreferencesKey("crew_brains_active")
    private val json = Json { ignoreUnknownKeys = true }

    private fun defaultBrain(): CrewBrain {
        val now = System.currentTimeMillis()
        return CrewBrain(
            id = DEFAULT_BRAIN_ID,
            name = "Main Crew",
            emoji = "🧠",
            createdAtMs = now,
            lastUsedMs = now,
        )
    }

    private fun decodeBrains(raw: String?): List<CrewBrain> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<CrewBrain>>(raw) }.getOrDefault(emptyList())
    }

    private suspend fun persist(brains: List<CrewBrain>, activeId: String) {
        AppCtx.ctx.helldeckDataStore.edit { prefs ->
            prefs[KEY_BRAINS] = json.encodeToString(brains)
            prefs[KEY_ACTIVE] = activeId
        }
    }

    suspend fun ensureInitialized(): Pair<List<CrewBrain>, String> {
        val prefs = AppCtx.ctx.helldeckDataStore.data.firstOrNull()
        val storedBrains = decodeBrains(prefs?.get(KEY_BRAINS))
        val brains = if (storedBrains.isEmpty()) listOf(defaultBrain()) else storedBrains

        val active = prefs?.get(KEY_ACTIVE)
        val activeId = if (brains.any { it.id == active }) {
            active!!
        } else {
            brains.first().id
        }

        if (storedBrains.isEmpty() || active != activeId) {
            persist(brains, activeId)
        }
        return brains to activeId
    }

    suspend fun getBrains(): List<CrewBrain> = ensureInitialized().first

    fun brainsFlow(): Flow<List<CrewBrain>> {
        return AppCtx.ctx.helldeckDataStore.data.map { prefs ->
            val brains = decodeBrains(prefs[KEY_BRAINS])
            if (brains.isEmpty()) listOf(defaultBrain()) else brains
        }
    }

    fun activeBrainIdFlow(): Flow<String> {
        return AppCtx.ctx.helldeckDataStore.data.map { prefs ->
            val brains = decodeBrains(prefs[KEY_BRAINS])
            val active = prefs[KEY_ACTIVE]
            val fallback = (if (brains.isEmpty()) listOf(defaultBrain()) else brains).first()
            active?.takeIf { id -> brains.any { it.id == id } } ?: fallback.id
        }
    }

    fun activeBrainIdSync(): String = runBlocking { getActiveBrainId() }

    suspend fun getActiveBrainId(): String = ensureInitialized().second

    suspend fun setActiveBrain(brainId: String) {
        val (brains, _) = ensureInitialized()
        val target = brains.find { it.id == brainId } ?: return
        val updated = brains.map { brain ->
            if (brain.id == brainId) brain.copy(lastUsedMs = System.currentTimeMillis()) else brain
        }
        persist(updated, target.id)
    }

    suspend fun createBrain(name: String, emoji: String): CrewBrain {
        val (brains, active) = ensureInitialized()
        if (brains.size >= MAX_BRAINS) {
            throw IllegalStateException("Maximum of $MAX_BRAINS crew brains reached")
        }
        val trimmed = name.trim().ifBlank { "Crew ${brains.size + 1}" }
        val brain = CrewBrain(
            id = UUID.randomUUID().toString(),
            name = trimmed.take(30),
            emoji = if (emoji.isBlank()) "🧠" else emoji,
            createdAtMs = System.currentTimeMillis(),
            lastUsedMs = System.currentTimeMillis(),
        )
        val newBrains = brains + brain
        persist(newBrains, brain.id.ifBlank { active })
        return brain
    }
}
