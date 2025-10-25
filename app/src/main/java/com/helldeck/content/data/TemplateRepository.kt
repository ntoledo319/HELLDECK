package com.helldeck.content.data

import android.content.res.AssetManager
import com.helldeck.content.model.Template
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.io.BufferedReader

class TemplateRepository(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private var templates: List<Template> = emptyList()

    fun loadAll(): List<Template> {
        if (templates.isNotEmpty()) return templates
        assets.open("templates/templates.json").use { ins ->
            val text = ins.bufferedReader().use(BufferedReader::readText)
            templates = json.decodeFromString(ListSerializer(Template.serializer()), text)
        }
        return templates
    }
}