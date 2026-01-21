package com.helldeck.content.data

import android.content.res.AssetManager
import com.helldeck.content.model.v2.TemplateV2
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedReader

class TemplateRepositoryV2(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private var templates: List<TemplateV2> = emptyList()

    fun loadAll(): List<TemplateV2> {
        if (templates.isNotEmpty()) return templates

        val loaded = mutableListOf<TemplateV2>()
        try {
            // Try to list all templates_v2/*.json files
            val v2Files = try {
                assets.list("templates_v2") ?: emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }

            v2Files.filter { it.endsWith(".json") }.forEach { filename ->
                try {
                    assets.open("templates_v2/$filename").use { ins ->
                        val text = ins.bufferedReader().use(BufferedReader::readText)
                        val batch = json.decodeFromString(ListSerializer(TemplateV2.serializer()), text)
                        loaded.addAll(batch)
                    }
                } catch (e: Exception) {
                    // Skip files that fail to parse
                }
            }
        } catch (e: Exception) {
            // No V2 templates available yet
        }

        // Fallback: merge legacy bundle at templates/templates.json to ensure coverage
        val legacyTemplates = loadLegacyTemplates()
        if (legacyTemplates.isNotEmpty()) {
            val existingIds = loaded.map { it.id }.toSet()
            legacyTemplates
                .filterNot { it.id in existingIds }
                .let { loaded.addAll(it) }
        }

        templates = loaded
        return templates
    }

    private fun loadLegacyTemplates(): List<TemplateV2> {
        return runCatching {
            assets.open("templates/templates.json").use { ins ->
                val text = ins.bufferedReader().use(BufferedReader::readText)
                json.decodeFromString(ListSerializer(TemplateV2.serializer()), text)
            }
        }.getOrDefault(emptyList())
    }
}
