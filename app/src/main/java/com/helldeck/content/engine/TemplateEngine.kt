package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.v2.SlotSpec
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng

/**
 * TemplateEngine handles dynamic content generation by filling template slots.
 * 
 * This engine processes TemplateV2 objects and fills them with appropriate content
 * based on the current game context. It supports both structured slot filling
 * and legacy regex-based slot resolution for backward compatibility.
 * 
 * Key features:
 * - Structured slot filling using TemplateV2.slots
 * - Legacy regex fallback for older templates
 * - Case transformations (upper, lower, title)
 * - Article handling for proper grammar
 * - Word count validation
 * - Lexicon-based content selection
 * 
 * @param repo Content repository for accessing lexicons and word lists
 * @param rng Seeded random number generator for reproducible results
 */
class TemplateEngine(
    private val repo: ContentRepository,
    private val rng: SeededRng
) {
    
    companion object {
        private val SLOT_REGEX = Regex("\\{([a-zA-Z0-9_]+)(?::([^}]+))?\\}")
    }
    
    /**
     * Context data required for template filling operations.
     * 
     * @param players List of player names participating in the current session
     * @param spiceMax Maximum spice level allowed for content generation
     * @param localityMax Maximum number of recent words to avoid repetition
     * @param inboundTexts List of inbound text messages to potentially incorporate
     */
    data class Context(
        val players: List<String> = emptyList(),
        val spiceMax: Int = 3,
        val localityMax: Int = 3,
        val inboundTexts: List<String> = emptyList()
    )

    /**
     * Fills a template with appropriate content based on the provided context.
     * 
     * The filling process:
     * 1. Uses structured slots from TemplateV2.slots if available
     * 2. Falls back to regex-based parsing for legacy templates
     * 3. Applies case transformations and article handling
     * 4. Validates word count against template limits
     * 5. Selects appropriate content from lexicons
     * 
     * @param template The template to fill
     * @param ctx Context containing all constraints and preferences
     * @return FilledCard with generated content and metadata
     * @throws IllegalArgumentException if template constraints cannot be satisfied
     */
    fun fill(template: TemplateV2, ctx: Context): FilledCard {
        val filledSlots = mutableMapOf<String, String>()
        var text = template.text

        if (template.slots.isNotEmpty()) {
            // Use structured slot filling for modern templates
            template.slots.forEach { slotSpec ->
                val filled = fillSlot(slotSpec, ctx, filledSlots)
                filledSlots[slotSpec.name] = filled
                text = text.replace("{${slotSpec.name}}", filled)
            }
        } else {
            // Fallback to regex for legacy templates
            text = SLOT_REGEX.replace(template.text) { m ->
                val slotName = m.groupValues.getOrNull(1) ?: return@replace ""
                val mods = m.groupValues.getOrNull(2)?.split(",")?.map { it.trim() } ?: emptyList()
                val slotSpec = SlotSpec(slotName, slotName, mods)
                fillSlot(slotSpec, ctx, filledSlots)
            }
        }

        val wc = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        require(wc <= (template.max_words ?: 100)) {
            "Filled text for template ${template.id} exceeds max_words (${template.max_words}): \"$text\""
        }

        return FilledCard(
            id = template.id, 
            game = template.game, 
            text = text,
            family = template.family, 
            spice = template.spice, 
            locality = template.locality,
            metadata = mapOf("template" to template.text)
        )
    }

    /**
     * Fills a specific slot with appropriate content.
     * 
     * @param spec The slot specification defining how to fill this slot
     * @param ctx Current filling context
     * @param used Map of already used slot values to avoid repetition
     * @return The filled content for this slot
     */
    private fun fillSlot(spec: SlotSpec, ctx: Context, used: Map<String, String>): String {
        val raw = when (spec.from) {
            "target_name" -> pickTarget(ctx.players)
            "inbound_text" -> pickInbound(ctx) ?: pickFromLex(spec.from, used.values.toSet())
            else -> pickFromLex(spec.from, used.values.toSet().takeIf { "unique" in spec.transform })
        }
        val cased = applyCase(raw, spec.transform)
        return if ("a_an" in spec.transform) withArticle(cased) else cased
    }

    /**
     * Selects a target player from the available players.
     * 
     * @param players List of player names to choose from
     * @return Selected player name or "someone" if no players available
     */
    private fun pickTarget(players: List<String>): String =
        if (players.isNotEmpty()) players[rng.nextInt(players.size)] else "someone"

    /**
     * Selects an inbound text message from the context.
     * 
     * @param ctx Current filling context
     * @return Selected inbound text or null if none available
     */
    private fun pickInbound(ctx: Context): String? =
        ctx.inboundTexts.takeIf { it.isNotEmpty() }?.let { it[rng.nextInt(it.size)] }

    /**
     * Selects content from a lexicon while avoiding already used words.
     * 
     * @param lexicon The lexicon name to select from
     * @param used Set of already used words to avoid (if specified)
     * @return Selected word from the lexicon
     * @throws IllegalArgumentException if lexicon is empty or missing
     */
    private fun pickFromLex(lexicon: String, used: Set<String>?): String {
        val pool = repo.wordsFor(lexicon)
        require(pool.isNotEmpty()) { "Empty or missing lexicon for slot '$lexicon'" }
        val filtered = if (used != null) pool.filter { it.lowercase() !in used } else pool
        val src = if (filtered.isNotEmpty()) filtered else pool
        return src[rng.nextInt(src.size)]
    }

    /**
     * Applies case transformations to text based on specified modifiers.
     * 
     * Supported transformations:
     * - "upper": Convert to uppercase
     * - "lower": Convert to lowercase  
     * - "title": Convert to title case (first letter of each word capitalized)
     * - "a_an": Add appropriate article ("a" or "an")
     * 
     * @param s The input text to transform
     * @param mods List of transformation modifiers to apply
     * @return Transformed text with all specified modifiers applied
     */
    private fun applyCase(s: String, mods: List<String>): String {
        var w = s
        if ("upper" in mods) w = w.uppercase()
        if ("lower" in mods) w = w.lowercase()
        if ("title" in mods) {
            w = w.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
        }
        return w
    }

    /**
     * Adds appropriate article ("a" or "an") to a word based on its first letter.
     * 
     * @param s The word to add article to
     * @return The word with appropriate article prefix
     */
    private fun withArticle(s: String): String {
        val first = s.trim().firstOrNull()?.lowercaseChar() ?: return s
        val article = if (first in listOf('a','e','i','o','u')) "an" else "a"
        return "$article $s"
    }
}