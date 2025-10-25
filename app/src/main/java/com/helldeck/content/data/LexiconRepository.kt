package com.helldeck.content.data

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.io.BufferedReader

class LexiconRepository(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, List<String>>()

    fun wordsFor(slotName: String): List<String> {
        val file = slotToFile(slotName)
        return cache.getOrPut(file) { readArray("lexicons/$file") }
    }

    private fun readArray(path: String): List<String> {
        assets.open(path).use { ins ->
            val text = ins.bufferedReader().use(BufferedReader::readText)
            return json.decodeFromString<List<String>>(text)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    private fun slotToFile(slot: String): String = when (slot.lowercase()) {
        "friend" -> "friends.json"
        "place" -> "places.json"
        "meme","memes" -> "memes.json"
        "ick","icks" -> "icks.json"
        "perk","perks" -> "perks.json"
        "red_flag","red_flags" -> "red_flags.json"
        "gross" -> "gross.json"
        "social_disaster","social_disasters" -> "social_disasters.json"
        "sketchy_action","sketchy_actions" -> "sketchy_actions.json"
        "tiny_reward","tiny_rewards" -> "tiny_rewards.json"
        "guilty_prompt","guilty_prompts" -> "guilty_prompts.json"
        "category","categories" -> "categories.json"
        "letter","letters" -> "letters.json"
        "forbidden" -> "forbidden.json"
        "target_name" -> "friends.json" // dynamic preferred; fallback
        "inbound_text" -> "inbound_texts.json" // optional seed list
        else -> "$slot.json"
    }
}