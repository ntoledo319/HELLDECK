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
        // Capture full placeholder content; we parse name/mods/count/sep ourselves
        private val SLOT_REGEX = Regex("\\{([^}]+)\\}")
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
        val slotSequence = mutableListOf<Pair<String, String>>()
        var text = template.text
        val usedLexiconWords = mutableMapOf<String, MutableSet<String>>()
        val usedPlayers = mutableSetOf<String>()

        if (template.slots.isNotEmpty()) {
            // Use structured slot filling for modern templates
            template.slots.forEach { slotSpec ->
                val filled = fillSlot(
                    spec = slotSpec,
                    ctx = ctx,
                    used = filledSlots,
                    distinctAll = template.constraints.distinct_slots,
                    usedLexiconWords = usedLexiconWords,
                    usedPlayers = usedPlayers
                )
                filledSlots[slotSpec.name] = filled
                slotSequence += slotSpec.name to filled
                text = text.replace("{${slotSpec.name}}", filled)
            }
        } else {
            // Fallback to regex for legacy templates with enhanced syntax support
            text = SLOT_REGEX.replace(template.text) { m ->
                val raw = m.groupValues[1].trim()
                val spec = parseLegacySlotSpec(raw)
                val result = if (spec.count > 1) {
                    val picks = mutableListOf<String>()
                    repeat(spec.count) {
                        val avoid = (if (template.constraints.distinct_slots || spec.unique) picks.toSet() else null)
                        val base = when (spec.name) {
                            "target_name" -> pickTarget(ctx.players, usedPlayers)
                            "inbound_text" -> pickInbound(ctx) ?: pickFromLex(spec.name, avoid, usedLexiconWords)
                            else -> pickFromLex(spec.name, avoid, usedLexiconWords)
                        }
                        var v = applyCase(base, spec.transforms)
                        if ("a_an" in spec.transforms) v = withArticle(v)
                        picks += v
                    }
                    picks.joinToString(spec.separator ?: ", ")
                } else {
                    val base = when (spec.name) {
                        "target_name" -> pickTarget(ctx.players, usedPlayers)
                        "inbound_text" -> pickInbound(ctx) ?: pickFromLex(spec.name, null, usedLexiconWords)
                        else -> pickFromLex(
                            spec.name,
                            if (template.constraints.distinct_slots) filledSlots.values.toSet() else null,
                            usedLexiconWords
                        )
                    }
                    var v = applyCase(base, spec.transforms)
                    if ("a_an" in spec.transforms) v = withArticle(v)
                    v
                }

                // Track slot fills (for downstream option inference)
                slotSequence += spec.name to result
                filledSlots[spec.name] = result
                result
            }
        }

        text = sanitizePlain(text)
        val wordsList = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val maxWords = template.max_words ?: 100
        val wc = wordsList.size
        if (wc > maxWords) {
            throw IllegalArgumentException("Template ${template.id} exceeds max_words ($wc > $maxWords)")
        }

        return FilledCard(
            id = template.id, 
            game = template.game, 
            text = text,
            family = template.family, 
            spice = template.spice, 
            locality = template.locality,
            metadata = mapOf(
                "template" to template.text,
                "slots" to filledSlots.toMap(),
                "slot_sequence" to slotSequence.toList()
            )
        )
    }

    /**
     * Legacy slot spec supporting patterns like:
     * {meme:title}, {meme#3,sep=", ",unique}, {target_name}, {inbound_text}
     */
    private data class LegacySlotSpec(
        val name: String,
        val transforms: List<String> = emptyList(),
        val count: Int = 1,
        val separator: String? = null,
        val unique: Boolean = false
    )

    private fun parseLegacySlotSpec(content: String): LegacySlotSpec {
        // Split into name + remainder
        val nameMatch = Regex("^([a-zA-Z0-9_]+)").find(content) ?: return LegacySlotSpec(content)
        val name = nameMatch.value
        var rest = content.removePrefix(name).trim()

        var count = 1
        var separator: String? = null
        val transforms = mutableListOf<String>()
        var unique = false

        // Support optional #N right after name
        val cnt = Regex("^#(\\d+)").find(rest)
        if (cnt != null) {
            count = cnt.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            rest = rest.removePrefix(cnt.value).trim()
        }

        // If rest begins with ':' tokens (legacy syntax uses one or more), strip all leading ':'
        while (rest.startsWith(":")) {
            rest = rest.removePrefix(":")
        }

        // Tokenize on commas outside quotes
        val tokens = tokenizeLegacy(rest)
        tokens.forEach { tok ->
            val t = tok.trim()
            when {
                t.isBlank() -> {}
                t.equals("unique", true) -> unique = true
                t.equals("upper", true) || t.equals("lower", true) || t.equals("title", true) || t.equals("a_an", true) ->
                    transforms += t.lowercase()
                t.startsWith("sep=", true) -> {
                    val v = t.substringAfter("=").trim().removeSurrounding("'", "'").removeSurrounding("\"", "\"")
                    separator = v
                }
                else -> {
                    // Unrecognized tokens act as transforms for backward compatibility
                    transforms += t.lowercase()
                }
            }
        }

        return LegacySlotSpec(name, transforms, count, separator, unique)
    }

    private fun tokenizeLegacy(s: String): List<String> {
        if (s.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var quote: Char? = null
        s.forEach { ch ->
            when {
                quote != null -> {
                    sb.append(ch)
                    if (ch == quote) quote = null
                }
                ch == '\'' || ch == '"' -> {
                    quote = ch
                    sb.append(ch)
                }
                ch == ',' -> {
                    out += sb.toString()
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) out += sb.toString()
        return out
    }

    /**
     * Fills a specific slot with appropriate content.
     * 
     * @param spec The slot specification defining how to fill this slot
     * @param ctx Current filling context
     * @param used Map of already used slot values to avoid repetition
     * @return The filled content for this slot
     */
    private fun fillSlot(
        spec: SlotSpec,
        ctx: Context,
        used: Map<String, String>,
        distinctAll: Boolean,
        usedLexiconWords: MutableMap<String, MutableSet<String>>,
        usedPlayers: MutableSet<String>
    ): String {
        val raw = when (spec.from) {
            "target_name" -> pickTarget(ctx.players, usedPlayers)
            "inbound_text" -> pickInbound(ctx) ?: pickFromLex(spec.from, used.values.toSet(), usedLexiconWords)
            else -> {
                val avoid = when {
                    distinctAll -> used.values.toSet()
                    "unique" in spec.transform -> used.values.toSet()
                    else -> null
                }
                pickFromLex(spec.from, avoid, usedLexiconWords)
            }
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
    private fun pickTarget(players: List<String>, usedPlayers: MutableSet<String>): String {
        if (players.isEmpty()) return "someone"
        val unused = players.filterNot { usedPlayers.contains(it) }
        val pool = if (unused.isNotEmpty()) unused else players
        val choice = pool[rng.nextInt(pool.size)]
        usedPlayers += choice
        return choice
    }

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
    private fun pickFromLex(
        lexicon: String,
        used: Set<String>?,
        usedLexiconWords: MutableMap<String, MutableSet<String>>
    ): String {
        val pool = repo.wordsFor(lexicon)
        require(pool.isNotEmpty()) { "Empty or missing lexicon for slot '$lexicon'" }
        val normalizedAvoid = (used?.map { it.trim().lowercase() }?.toMutableSet() ?: mutableSetOf())
        val lexKey = lexicon.lowercase()
        val seen = usedLexiconWords.getOrPut(lexKey) { mutableSetOf() }
        normalizedAvoid += seen

        val candidate = pool
            .shuffled(rng.random)
            .firstOrNull { candidate ->
                val cleaned = candidate.trim().lowercase()
                cleaned.isNotEmpty() && cleaned !in normalizedAvoid
            }
            ?: pool.first()

        seen += candidate.trim().lowercase()
        return candidate
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

    private fun sanitizePlain(s: String): String {
        var t = s
        // Normalize spaces
        t = t.replace(Regex("\\s+"), " ").trim()
        // Fix spacing before punctuation
        t = t.replace(Regex("\\s+([,!?])"), "$1")
        // Ensure single space after punctuation when appropriate
        t = t.replace(Regex("([,!?.])(?=[^\\s])"), "$1 ")
        // Capitalize first letter
        t = t.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
        return t
    }
}
