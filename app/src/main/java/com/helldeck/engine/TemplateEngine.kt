package com.helldeck.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.helldeck.AppCtx
import com.helldeck.data.TemplateDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Template engine for filling card templates with dynamic content
 */
class TemplateEngine(private val ctx: Context) {

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val slotPattern = Pattern.compile("\\{([a-zA-Z_]+)}")
    private val templates by lazy { loadTemplates() }

    /**
     * Load templates from assets
     */
    private fun loadTemplates(): List<TemplateDef> {
        return try {
            val raw = ctx.assets.open("templates/templates.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<TemplateDef>>() {}.type
            gson.fromJson(raw, type)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load templates", e)
        }
    }

    /**
     * Fill a template with dynamic content
     */
    suspend fun fill(
        template: TemplateDef,
        slotProvider: suspend (String) -> String,
        maxWords: Int = template.maxWords
    ): String = withContext(Dispatchers.Default) {
        var result = template.text

        // Find all slots in the template
        val matcher = slotPattern.matcher(template.text)
        val slots = mutableMapOf<String, String>()

        while (matcher.find()) {
            val slotName = matcher.group(1)
            if (slotName != null && !slots.containsKey(slotName)) {
                slots[slotName] = slotProvider(slotName)
            }
        }

        // Replace slots in the template
        slots.forEach { (slotName, value) ->
            val placeholder = "{$slotName}"
            result = result.replace(placeholder, value)
        }

        // Apply word limit if specified
        if (maxWords > 0) {
            result = applyWordLimit(result, maxWords)
        }

        result.trim()
    }

    /**
     * Fill a template by ID
     */
    suspend fun fillById(
        templateId: String,
        slotProvider: suspend (String) -> String,
        maxWords: Int? = null
    ): String? {
        val template = templates.find { it.id == templateId }
        return template?.let { fill(it, slotProvider, maxWords ?: it.maxWords) }
    }

    /**
     * Get candidates for a specific game
     */
    fun getCandidatesForGame(game: String): List<TemplateDef> {
        return templates.filter { it.game == game }
    }

    /**
     * Get candidates for a specific game with filters
     */
    fun getFilteredCandidatesForGame(
        game: String,
        minSpice: Int = 1,
        maxSpice: Int = 3,
        excludeFamilies: Set<String> = emptySet()
    ): List<TemplateDef> {
        return templates.filter { template ->
            template.game == game &&
            template.spice in minSpice..maxSpice &&
            template.family !in excludeFamilies
        }
    }

    /**
     * Get template by ID
     */
    fun getTemplateById(id: String): TemplateDef? {
        return templates.find { it.id == id }
    }

    /**
     * Get all available games
     */
    fun getAvailableGames(): Set<String> {
        return templates.map { it.game }.toSet()
    }

    /**
     * Get families for a game
     */
    fun getFamiliesForGame(game: String): Set<String> {
        return templates.filter { it.game == game }.map { it.family }.toSet()
    }

    /**
     * Validate template syntax
     */
    fun validateTemplate(template: TemplateDef): List<String> {
        val errors = mutableListOf<String>()

        // Check for unmatched braces
        val openBraces = template.text.count { it == '{' }
        val closeBraces = template.text.count { it == '}' }

        if (openBraces != closeBraces) {
            errors.add("Unmatched braces in template")
        }

        if (openBraces % 2 != 0) {
            errors.add("Odd number of braces in template")
        }

        // Check for valid slot names
        val matcher = slotPattern.matcher(template.text)
        while (matcher.find()) {
            val slotName = matcher.group(1)
            if (slotName.isNullOrBlank()) {
                errors.add("Empty slot name")
            }
        }

        // Check word limit
        if (template.maxWords <= 0) {
            errors.add("Max words must be positive")
        }

        // Check spice level
        if (template.spice !in 1..3) {
            errors.add("Spice level must be between 1 and 3")
        }

        return errors
    }

    /**
     * Apply word limit to text
     */
    private fun applyWordLimit(text: String, maxWords: Int): String {
        val words = text.split(Regex("\\s+"))
        return if (words.size <= maxWords) {
            text
        } else {
            words.take(maxWords).joinToString(" ") + "..."
        }
    }

    /**
     * Get template statistics
     */
    fun getTemplateStats(): Map<String, Any> {
        val gameCounts = templates.groupBy { it.game }
            .mapValues { it.value.size }

        val spiceDistribution = templates.groupBy { it.spice }
            .mapValues { it.value.size }

        val familyCounts = templates.groupBy { it.family }
            .mapValues { it.value.size }

        return mapOf(
            "totalTemplates" to templates.size,
            "games" to gameCounts.keys.size,
            "families" to familyCounts.size,
            "gameDistribution" to gameCounts,
            "spiceDistribution" to spiceDistribution,
            "averageWords" to templates.map { it.text.split(Regex("\\s+")).size }.average(),
            "maxWords" to templates.maxOf { it.maxWords },
            "minWords" to templates.minOf { it.maxWords }
        )
    }

    /**
     * Preload and cache templates for better performance
     */
    fun preloadTemplates(): Int {
        // Force lazy loading
        val count = templates.size
        return count
    }

    /**
     * Clear template cache (for testing or memory management)
     */
    fun clearCache() {
        // In a more complex implementation, this might clear caches
        // For now, this is a no-op since we're using lazy loading
    }
}

/**
 * Default slot provider implementation
 */
class DefaultSlotProvider(private val context: Context) {

    suspend fun provideSlot(slotName: String): String {
        val repository = com.helldeck.data.Repository.get(context)

        return when (slotName) {
            "friend" -> getRandomFromLexicon(repository, "friends")
            "target_name" -> getRandomFromLexicon(repository, "friends")
            "place" -> getRandomFromLexicon(repository, "places")
            "perk" -> getRandomFromLexicon(repository, "perks")
            "red_flag" -> getRandomFromLexicon(repository, "red_flags")
            "inbound_text" -> getRandomFromLexicon(repository, "memes")
            "category" -> getRandomFromLexicon(repository, "categories")
            "letter" -> getRandomFromLexicon(repository, "letters")
            "sketchy_action" -> getRandomFromLexicon(repository, "icks")
            "tiny_reward" -> getRandomFromLexicon(repository, "perks")
            "guilty_prompt" -> getRandomFromLexicon(repository, "memes")
            "gross" -> getRandomFromLexicon(repository, "icks")
            "social_disaster" -> getRandomFromLexicon(repository, "red_flags")
            else -> {
                // Fallback: try to get from lexicon with same name, or use slot name
                val fallback = getRandomFromLexicon(repository, slotName)
                fallback.ifEmpty { slotName.uppercase() }
            }
        }
    }

    private suspend fun getRandomFromLexicon(repository: com.helldeck.data.Repository, lexiconName: String): String {
        val words = repository.getLexicon(lexiconName)
        return if (words.isNotEmpty()) {
            words.random()
        } else {
            lexiconName.uppercase()
        }
    }
}

/**
 * Template filling result
 */
data class TemplateFillResult(
    val filledText: String,
    val template: TemplateDef,
    val slotsUsed: Map<String, String>,
    val wordCount: Int,
    val truncated: Boolean = false
)

/**
 * Batch template filling for performance
 */
suspend fun TemplateEngine.fillBatch(
    templates: List<TemplateDef>,
    slotProvider: suspend (String) -> String,
    maxConcurrency: Int = 4
): List<TemplateFillResult> = withContext(Dispatchers.Default) {
    templates.map { template ->
        try {
            val filled = fill(template, slotProvider)
            val slotsUsed = extractSlotsFromTemplate(template.text)
            TemplateFillResult(
                filledText = filled,
                template = template,
                slotsUsed = slotsUsed,
                wordCount = filled.split(Regex("\\s+")).size,
                truncated = filled.endsWith("...")
            )
        } catch (e: Exception) {
            // Return error result
            TemplateFillResult(
                filledText = "Error filling template: ${e.message}",
                template = template,
                slotsUsed = emptyMap(),
                wordCount = 0,
                truncated = false
            )
        }
    }
}

/**
 * Extract slot names from template text
 */
private fun extractSlotsFromTemplate(templateText: String): Map<String, String> {
    val pattern = Pattern.compile("\\{([a-zA-Z_]+)}")
    val matcher = pattern.matcher(templateText)
    val slots = mutableMapOf<String, String>()

    while (matcher.find()) {
        val slotName = matcher.group(1)
        if (slotName != null) {
            slots[slotName] = "{$slotName}"
        }
    }

    return slots
}