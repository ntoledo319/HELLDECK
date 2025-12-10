package com.helldeck.content.generator

import android.content.res.AssetManager
import com.helldeck.content.generator.gold.GoldBank
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class BlueprintRepositoryV3(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val byGame: Map<String, List<TemplateBlueprint>> = load()

    private fun load(): Map<String, List<TemplateBlueprint>> {
        val map = mutableMapOf<String, MutableList<TemplateBlueprint>>()
        val files = assets.list(TEMPLATES_DIR)?.toList().orEmpty()
        files.filter { it.endsWith(".json") }.forEach { file ->
            val content = assets.open("$TEMPLATES_DIR/$file").bufferedReader().use { it.readText() }
            val blueprints: List<TemplateBlueprint> = json.decodeFromString(content)
            blueprints.forEach { blueprint ->
                map.getOrPut(blueprint.game) { mutableListOf() }.add(blueprint)
            }
        }
        return map
    }

    fun forGame(gameId: String): List<TemplateBlueprint> = byGame[gameId].orEmpty()

    companion object {
        private const val TEMPLATES_DIR = "templates_v3"
    }
}

class LexiconRepositoryV2(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val byType: Map<String, List<LexiconEntry>> = load()

    private fun load(): Map<String, List<LexiconEntry>> {
        val map = mutableMapOf<String, MutableList<LexiconEntry>>()
        val files = assets.list(LEXICONS_DIR)?.toList().orEmpty()
        files.filter { it.endsWith(".json") }.forEach { file ->
            val content = assets.open("$LEXICONS_DIR/$file").bufferedReader().use { it.readText() }
            val lexicon = json.decodeFromString<LexiconFile>(content)
            map.getOrPut(lexicon.slot_type) { mutableListOf() }.addAll(lexicon.entries)
        }
        return map
    }

    fun entriesFor(slotType: String): List<LexiconEntry> = byType[slotType].orEmpty()

    companion object {
        private const val LEXICONS_DIR = "lexicons_v2"
    }
}
