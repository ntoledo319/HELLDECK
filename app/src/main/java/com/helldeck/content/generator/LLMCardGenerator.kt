package com.helldeck.content.generator

import android.content.Context
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.InteractionType
import com.helldeck.engine.GameIds
import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM
import com.helldeck.utils.Logger
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.random.Random

/**
 * UNIFIED LLM Card Generator - Combines best features from V1 and V2
 * 
 * Primary: On-device LLM generation with quality-first prompts
 * Fallback: Curated gold standard cards
 * 
 * Key Features:
 * - Enhanced reliability with smart retry strategy
 * - Gold standard examples in prompts
 * - Simplified quality validation (no over-engineering)
 * - Single fallback layer (gold cards only)
 * - Better error handling and JSON parsing
 */
class LLMCardGenerator(
    private val llm: LocalLLM?,
    private val context: Context
) {

    data class GenerationRequest(
        val gameId: String,
        val players: List<String>,
        val spiceMax: Int,
        val sessionId: String,
        val roomHeat: Double = 0.6
    )

    data class GenerationResult(
        val filledCard: FilledCard,
        val options: GameOptions,
        val timer: Int,
        val interactionType: InteractionType,
        val usedLLM: Boolean,
        val qualityScore: Double = 0.0
    )

    /**
     * Generate a card using LLM or fallback to gold cards
     */
    suspend fun generate(request: GenerationRequest): GenerationResult? {
        // Try LLM generation with smart retry strategy
        if (llm?.isReady == true) {
            repeat(3) { attempt ->
                try {
                    val candidate = withTimeout(4000) {  // 4 sec timeout (increased from 2.5s)
                        generateWithLLM(request, attempt)
                    }
                    if (candidate != null && validateBasicQuality(candidate)) {
                        Logger.d("LLM generated card (attempt ${attempt + 1}, quality: ${candidate.qualityScore})")
                        return candidate
                    } else if (candidate != null) {
                        Logger.d("LLM card failed quality check (attempt ${attempt + 1})")
                    }
                } catch (e: Exception) {
                    Logger.w("LLM generation attempt ${attempt + 1} failed: ${e.message}")
                }
            }
            Logger.w("All LLM attempts failed, falling back to gold cards")
        }

        // Fallback to gold cards (only fallback layer)
        return fallbackToGold(request)
    }

    /**
     * Generate card using LLM with temperature variation per attempt
     */
    private suspend fun generateWithLLM(request: GenerationRequest, attempt: Int): GenerationResult? {
        val prompt = buildQualityPrompt(request, attempt)
        
        // Vary temperature per attempt to increase variety
        val baseTemp = when (request.spiceMax) {
            1 -> 0.5f
            2 -> 0.6f
            3 -> 0.75f
            4 -> 0.85f
            else -> 0.9f
        }
        
        val attemptTemp = when (attempt) {
            0 -> baseTemp
            1 -> baseTemp + 0.1f
            else -> baseTemp + 0.2f
        }

        val config = GenConfig(
            maxTokens = 150,
            temperature = attemptTemp.coerceIn(0.3f, 1.2f),
            topP = 0.92f,
            seed = (request.sessionId + System.currentTimeMillis() + attempt).hashCode()
        )

        val response = llm?.generate(prompt.system, prompt.user, config) ?: return null
        return parseAndValidateResponse(response, request, attempt)
    }

    private data class Prompt(val system: String, val user: String)

    /**
     * Build quality-focused prompts with gold standard examples
     */
    private fun buildQualityPrompt(request: GenerationRequest, attempt: Int): Prompt {
        // Load gold examples for this game
        val goldExamples = GoldCardsLoader.getExamplesForGame(context, request.gameId, count = 5)

        val spiceGuidance = when (request.spiceMax) {
            1 -> "wholesome and PG-13 (family-friendly)"
            2 -> "fun and playful with light edge"
            3 -> "edgy and provocative but not mean-spirited"
            4 -> "wild and unhinged while avoiding slurs"
            else -> "maximum chaos (keep it funny, not cruel)"
        }
        
        // Add creativity boost on retry attempts
        val creativityNote = when (attempt) {
            0 -> ""
            1 -> "\n7. Be MORE creative than usual - avoid obvious patterns"
            else -> "\n7. COMPLETELY different approach - surprise me with originality"
        }

        val system = """You are an expert comedy writer for HELLDECK, a party game app.

CRITICAL RULES:
1. Generate EXACTLY ONE card in valid JSON format
2. Output ONLY the JSON - no markdown, no explanation, no extra text
3. Be SPECIFIC and UNEXPECTED - avoid clichés and generic phrases
4. Spice level: $spiceGuidance
5. NEVER use slurs, target protected groups, or be genuinely cruel
6. Every card must be UNIQUE - never repeat patterns$creativityNote"""

        val user = when (request.gameId) {
            GameIds.ROAST_CONS -> buildRoastPrompt(goldExamples)
            GameIds.POISON_PITCH -> buildPoisonPitchPrompt(goldExamples)
            GameIds.FILL_IN -> buildFillInPrompt(goldExamples)
            GameIds.RED_FLAG -> buildRedFlagPrompt(goldExamples)
            GameIds.HOTSEAT_IMP -> buildHotSeatPrompt(goldExamples)
            GameIds.TEXT_TRAP -> buildTextTrapPrompt(goldExamples)
            GameIds.TABOO -> buildTabooPrompt(goldExamples)
            GameIds.TITLE_FIGHT -> buildTitleFightPrompt(goldExamples)
            GameIds.ALIBI -> buildAlibiPrompt(goldExamples)
            GameIds.SCATTER -> buildScatterPrompt(goldExamples)
            GameIds.CONFESS_CAP -> buildConfessPrompt(goldExamples)
            GameIds.UNIFYING_THEORY -> buildUnifyingTheoryPrompt(goldExamples)
            GameIds.REALITY_CHECK -> buildRealityCheckPrompt(goldExamples)
            GameIds.OVER_UNDER -> buildOverUnderPrompt(goldExamples)
            // Legacy games removed: ODD_ONE, HYPE_YIKE, MAJORITY
            else -> """{"text": "Fallback card", "type": "unknown"}"""
        }

        return Prompt(system, user)
    }

    // ===== GAME-SPECIFIC PROMPTS =====

    private fun buildRoastPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate a roast card:

FORMAT:
{
  "text": "Most likely to [SPECIFIC ACTION] because [ABSURD BUT BELIEVABLE REASON]"
}

QUALITY CRITERIA:
✓ SPECIFICITY - Avoid generic (no "be late", "eat pizza", etc.)
✓ ABSURDITY - Exaggerated but relatable scenario
✓ VISUAL - Create a mental image
✓ PLAYFUL - Roast the behavior, not the person
✓ UNEXPECTED - Surprise with the reason

TOP EXAMPLES:
$exampleText

❌ AVOID:
- "Most likely to be late" (too generic)
- "Most likely to eat all the pizza" (cliché)
- Physical appearance attacks
- Anything genuinely hurtful

OUTPUT: Generate ONE unique roast card in JSON format."""
    }

    private fun buildPoisonPitchPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            val opts = if (it.optionA != null && it.optionB != null) {
                " | A: \"${it.optionA}\" vs B: \"${it.optionB}\""
            } else ""
            "✅ \"${it.text}\"$opts (quality: ${it.quality_score}/10)"
        }

        return """Generate a "Would You Rather" dilemma:

FORMAT:
{
  "text": "Would you rather...",
  "optionA": "first terrible option",
  "optionB": "second terrible option"
}

QUALITY CRITERIA:
✓ EQUAL DIFFICULTY - Both options equally bad/awkward
✓ SPECIFICITY - Vivid, concrete scenarios
✓ GENUINE DILEMMA - No obvious answer
✓ VISUAL - Easy to imagine both outcomes

TOP EXAMPLES:
$exampleText

❌ AVOID:
- One option obviously better
- Generic choices
- Too similar options

OUTPUT: Generate ONE unique dilemma in JSON format."""
    }

    private fun buildFillInPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate a double fill-in-the-blank prompt:

FORMAT:
{
  "text": "Prompt with _____ (first blank) and _____ (second blank)"
}

STRUCTURE: Judge fills first blank verbally (setup), players write punchlines for second blank.

QUALITY CRITERIA:
✓ FIRST BLANK - Invites setup/context
✓ SECOND BLANK - Invites punchline/completion
✓ OPEN-ENDED - Multiple funny answers possible
✓ CREATIVE POTENTIAL - Unexpected responses

TOP EXAMPLES:
$exampleText

❌ AVOID:
- Single blanks only
- Obvious answers
- Too limiting

OUTPUT: Generate ONE unique prompt in JSON format."""
    }

    private fun buildRedFlagPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate a dating red flag scenario:

FORMAT:
{
  "text": "They're perfect: [ATTRACTIVE QUALITY], but [DEALBREAKER RED FLAG]"
}

QUALITY CRITERIA:
✓ CONTRAST - Green flag must be genuinely appealing
✓ ABSURDITY - Red flag is dealbreaker-level ridiculous
✓ DEFENSIBILITY - Could argue to overlook it (for comedy)
✓ SPECIFICITY - Vivid, concrete details

TOP EXAMPLES:
$exampleText

❌ AVOID:
- Weak red flags
- Undefensible (actual crimes)
- Generic qualities

OUTPUT: Generate ONE unique red flag scenario in JSON format."""
    }

    private fun buildHotSeatPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate a personal question:

FORMAT:
{
  "text": "Question everyone answers AS the target person"
}

QUALITY CRITERIA:
✓ PERSONAL - Reveals personality/habits
✓ FUN TO IMPERSONATE - Easy to exaggerate
✓ NOT TOO INVASIVE - Playful, not uncomfortable

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE unique question in JSON format."""
    }

    private fun buildTextTrapPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            val tones = it.tones?.joinToString(", ") ?: ""
            "✅ \"${it.text}\" | Tones: [$tones] (quality: ${it.quality_score}/10)"
        }

        return """Generate a text message scenario:

FORMAT:
{
  "text": "[PERSON] texts: '[MESSAGE]'",
  "tones": ["Tone1", "Tone2", "Tone3", "Tone4"]
}

QUALITY CRITERIA:
✓ RELATABLE - Common texting situation
✓ HIGH-STAKES - Creates tension/awkwardness
✓ MULTIPLE TONES - Many valid reply styles

TOP EXAMPLES:
$exampleText

Common tones: Flirty, Petty, Wholesome, Chaotic, Cold, Panicked, Professional, Gaslighting, Casual

OUTPUT: Generate ONE scenario in JSON with 4 tone options."""
    }

    private fun buildTabooPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") { card ->
            val forbidden = card.forbidden?.joinToString(", ") ?: ""
            "✅ Word: \"${card.word}\" | Forbidden: [$forbidden] (quality: ${card.quality_score}/10)"
        }

        return """Generate a Taboo card:

FORMAT:
{
  "word": "word to guess",
  "forbidden": ["word1", "word2", "word3"]
}

QUALITY CRITERIA:
✓ COMMON WORD - Everyone knows it
✓ CHALLENGING - Forbidden words are obvious clues
✓ ACHIEVABLE - Still possible to describe

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE Taboo card in JSON format."""
    }

    // LEGACY METHOD REMOVED - Game not in official 14
    /*
    private fun buildOddOnePrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            val items = it.items?.joinToString(", ") ?: ""
            "✅ [$items] | \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate "Odd One Out" challenge:

FORMAT:
{
  "items": ["item1", "item2", "item3"],
  "text": "Which one doesn't belong?"
}

QUALITY CRITERIA:
✓ ARGUABLE - All choices defensible
✓ NOT OBVIOUS - Multiple perspectives
✓ INTERESTING - Sparks debate

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE challenge in JSON format."""
    }

    */
    private fun buildTitleFightPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate a comparative challenge:

FORMAT:
{
  "text": "Who would win: [OPTION A] vs [OPTION B]?"
}

QUALITY CRITERIA:
✓ ABSURDITY - Ridiculous matchups
✓ DEBATABLE - Both sides defensible
✓ VISUAL - Easy to imagine

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE challenge in JSON format."""
    }

    private fun buildAlibiPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            val words = it.words?.joinToString(", ") ?: ""
            "✅ Words: [$words] | \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate an alibi challenge:

FORMAT:
{
  "words": ["random1", "random2", "random3"],
  "text": "Sneak these into your excuse:"
}

QUALITY CRITERIA:
✓ UNRELATED - Words have no connection
✓ CHALLENGING - Hard to work in naturally
✓ NOT IMPOSSIBLE - Still achievable

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE challenge with 3 random words in JSON format."""
    }

    // LEGACY METHOD REMOVED - Game not in official 14
    /*
    private fun buildHypePrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ Product: \"${it.product}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate a ridiculous product to pitch:

FORMAT:
{
  "product": "absurd product description",
  "text": "Pitch this product:"
}

QUALITY CRITERIA:
✓ ABSURD - Clearly ridiculous
✓ PITCH-ABLE - Can actually defend it
✓ CREATIVE - Unexpected concept

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE product in JSON format."""
    }

    */
    private fun buildScatterPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ Category: \"${it.category}\", Letter: ${it.letter} (quality: ${it.quality_score}/10)"
        }

        return """Generate a Scattergories challenge:

FORMAT:
{
  "category": "creative category",
  "letter": "X",
  "text": "Name 3"
}

QUALITY CRITERIA:
✓ CREATIVE CATEGORY - Not generic
✓ CHALLENGING - Requires thought
✓ ACHIEVABLE - Answers exist

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE challenge in JSON format."""
    }

    // LEGACY METHOD REMOVED - Game not in official 14
    /*
    private fun buildMajorityPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            val opts = if (it.optionA != null && it.optionB != null) {
                " | ${it.optionA} vs ${it.optionB}"
            } else ""
            "✅ \"${it.text}\"$opts (quality: ${it.quality_score}/10)"
        }

        return """Generate a prediction challenge:

FORMAT:
{
  "optionA": "first choice",
  "optionB": "second choice",
  "text": "What will the room choose?"
}

QUALITY CRITERIA:
✓ DIVISIVE - Should split the room
✓ NO OBVIOUS ANSWER - Genuine debate
✓ RELATABLE - Everyone has an opinion

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE prediction challenge in JSON format."""
    }

    */
    private fun buildConfessPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (quality: ${it.quality_score}/10)"
        }

        return """Generate a confession (truth or lie):

FORMAT:
{
  "text": "I once [confession]"
}

QUALITY CRITERIA:
✓ BELIEVABLE YET SUS - Could be true or false
✓ INTERESTING - Sparks curiosity
✓ BORDERLINE - Not obviously true/false

TOP EXAMPLES:
$exampleText

OUTPUT: Generate ONE confession in JSON format."""
    }

    private fun buildUnifyingTheoryPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleTexts = examples.take(3).joinToString("\n") { 
            """{"text": "${it.text}"}""" 
        }
        return """
Generate a card for The Unifying Theory game.
List three completely unrelated items. Players must explain why they're the same.

Examples:
$exampleTexts

Return JSON: {"text": "Item 1, Item 2, Item 3"}
Make items as random and unrelated as possible for maximum creativity.
        """.trimIndent()
    }

    private fun buildRealityCheckPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleTexts = examples.take(3).joinToString("\n") { 
            """{"text": "${it.text}"}""" 
        }
        return """
Generate a card for Reality Check game.
Ask the subject to rate themselves 1-10 on a specific trait or ability.

Examples:
$exampleTexts

Return JSON: {"text": "Rating: Your [trait/ability]"}
Make it specific, personal, and potentially revealing.
        """.trimIndent()
    }

    private fun buildOverUnderPrompt(examples: List<GoldCardsLoader.GoldCard>): String {
        val exampleTexts = examples.take(3).joinToString("\n") { 
            """{"text": "${it.text}"}""" 
        }
        return """
Generate a card for Over/Under game.
Ask for a specific number about the subject that others can bet on.

Examples:
$exampleTexts

Return JSON: {"text": "Number of [countable thing]"}
Make it verifiable and potentially embarrassing or revealing.
        """.trimIndent()
    }

    // ===== PARSING & VALIDATION =====

    /**
     * Parse LLM response with enhanced error handling
     */
    private fun parseAndValidateResponse(
        response: String, 
        request: GenerationRequest,
        attempt: Int
    ): GenerationResult? {
        return try {
            // Clean response - handle common formatting issues
            val cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .replace("\\n", " ")
                .trim()
                .let { 
                    // Extract JSON if embedded in text
                    val start = it.indexOf('{')
                    val end = it.lastIndexOf('}')
                    if (start != -1 && end != -1 && end > start) {
                        it.substring(start, end + 1)
                    } else {
                        it
                    }
                }

            val json = JSONObject(cleaned)
            val text = json.optString("text", "")

            // Basic validation
            if (text.isBlank() || text.length < 10) {
                Logger.d("Card text too short: ${text.length} chars")
                return null
            }

            val card = FilledCard(
                id = "llm_${request.gameId}_${System.currentTimeMillis()}",
                game = request.gameId,
                text = text,
                family = "llm_generated",
                spice = request.spiceMax,
                locality = 1,
                metadata = mapOf(
                    "generated_by" to "llm_unified",
                    "model" to (llm?.modelId ?: "unknown"),
                    "timestamp" to System.currentTimeMillis(),
                    "attempt" to attempt,
                    "prompt_version" to "unified_v1"
                )
            )

            val options = parseOptionsFromJson(json, request)
            val timer = getTimerForGame(request.gameId)
            val interactionType = getInteractionTypeForGame(request.gameId)
            val qualityScore = estimateQuality(card, options)

            GenerationResult(card, options, timer, interactionType, usedLLM = true, qualityScore = qualityScore)

        } catch (e: Exception) {
            Logger.w("Failed to parse LLM response: ${e.message}")
            null
        }
    }

    /**
     * Simplified quality validation (no over-engineering)
     */
    private fun validateBasicQuality(result: GenerationResult): Boolean {
        val text = result.filledCard.text

        // Minimum quality score check
        if (result.qualityScore < 0.6) {
            Logger.d("Quality score too low: ${result.qualityScore}")
            return false
        }

        // Check for game-specific clichés
        val badPhrases = when (result.filledCard.game) {
            GameIds.ROAST_CONS -> listOf("be late", "eat pizza", "eat all", "be the one")
            GameIds.POISON_PITCH -> listOf("would you rather have", "or would you")
            else -> emptyList()
        }

        if (badPhrases.any { text.contains(it, ignoreCase = true) }) {
            Logger.d("Contains cliché phrase")
            return false
        }

        // Minimum length check
        if (text.length < 15) {
            Logger.d("Text too short: ${text.length} chars")
            return false
        }

        // Maximum length check (avoid runaway generation)
        if (text.length > 200) {
            Logger.d("Text too long: ${text.length} chars")
            return false
        }

        return true
    }

    /**
     * Estimate quality score (simplified)
     */
    private fun estimateQuality(card: FilledCard, options: GameOptions): Double {
        var score = 0.7  // Base score

        // Length check (not too short, not too long)
        val wordCount = card.text.split("\\s+".toRegex()).size
        score += when {
            wordCount < 5 -> -0.3
            wordCount > 30 -> -0.2
            wordCount in 10..20 -> 0.2
            else -> 0.0
        }

        // Specificity (contains numbers, proper nouns, specific details)
        if (card.text.contains(Regex("\\d"))) score += 0.05
        if (card.text.contains(Regex("[A-Z][a-z]+"))) score += 0.05

        // Options quality (for applicable games)
        when (options) {
            is GameOptions.AB -> {
                if (options.optionA.isNotBlank() && options.optionB.isNotBlank()) {
                    score += 0.1
                    // Penalize if options are too similar
                    if (options.optionA.lowercase() == options.optionB.lowercase()) {
                        score -= 0.3
                    }
                }
            }
            is GameOptions.Taboo -> {
                if (options.forbidden.size == 3) score += 0.1
            }
            else -> {}
        }

        return score.coerceIn(0.0, 1.0)
    }

    // ===== OPTION PARSING =====

    private fun parseOptionsFromJson(json: JSONObject, request: GenerationRequest): GameOptions {
        return when (request.gameId) {
            GameIds.ROAST_CONS -> GameOptions.PlayerVote(request.players)

            GameIds.POISON_PITCH -> {
                val a = json.optString("optionA", "Option A")
                val b = json.optString("optionB", "Option B")
                GameOptions.AB(a, b)
            }

            GameIds.RED_FLAG -> GameOptions.AB("SMASH", "PASS")

            GameIds.TABOO -> {
                val word = json.optString("word", "word")
                val forbidden = json.optJSONArray("forbidden")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: listOf("forbidden1", "forbidden2", "forbidden3")
                GameOptions.Taboo(word, forbidden)
            }

            GameIds.ALIBI -> {
                val words = json.optJSONArray("words")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: listOf("word1", "word2", "word3")
                GameOptions.HiddenWords(words)
            }
            
            // Legacy games removed: MAJORITY, ODD_ONE, HYPE_YIKE

            GameIds.SCATTER -> {
                val category = json.optString("category", "Things")
                val letter = json.optString("letter", "A")
                GameOptions.Scatter(category, letter)
            }

            GameIds.TEXT_TRAP -> {
                val tones = json.optJSONArray("tones")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: listOf("Casual", "Formal", "Chaotic", "Petty")
                GameOptions.ReplyTone(tones.take(4))
            }

            GameIds.CONFESS_CAP -> GameOptions.TrueFalse

            GameIds.HOTSEAT_IMP, GameIds.TITLE_FIGHT -> {
                GameOptions.Challenge(json.optString("challenge", "Pick the best"))
            }

            GameIds.FILL_IN -> GameOptions.None

            else -> GameOptions.None
        }
    }

    // ===== GAME METADATA =====

    private fun getTimerForGame(gameId: String): Int = when (gameId) {
        GameIds.TABOO -> 60
        GameIds.SCATTER -> 30
        GameIds.ALIBI -> 45
        else -> 15
    }

    private fun getInteractionTypeForGame(gameId: String): InteractionType = when (gameId) {
        GameIds.ROAST_CONS -> InteractionType.VOTE_PLAYER
        GameIds.POISON_PITCH -> InteractionType.A_B_CHOICE
        GameIds.CONFESS_CAP -> InteractionType.TRUE_FALSE
        GameIds.RED_FLAG -> InteractionType.SMASH_PASS
        GameIds.TABOO -> InteractionType.TABOO_GUESS
        GameIds.ALIBI -> InteractionType.HIDE_WORDS
        GameIds.SCATTER -> InteractionType.SPEED_LIST
        GameIds.TITLE_FIGHT -> InteractionType.MINI_DUEL
        GameIds.TEXT_TRAP -> InteractionType.REPLY_TONE
        GameIds.HOTSEAT_IMP, GameIds.FILL_IN -> InteractionType.JUDGE_PICK
        GameIds.UNIFYING_THEORY -> InteractionType.ODD_EXPLAIN
        GameIds.REALITY_CHECK -> InteractionType.TARGET_SELECT
        GameIds.OVER_UNDER -> InteractionType.PREDICT_VOTE
        // Legacy games removed: MAJORITY, ODD_ONE, HYPE_YIKE
        else -> InteractionType.NONE
    }

    // ===== GOLD CARD FALLBACK =====

    /**
     * Fallback to curated gold standard cards
     */
    private fun fallbackToGold(request: GenerationRequest): GenerationResult? {
        val goldCard = GoldCardsLoader.getRandomFallback(context, request.gameId) ?: return null

        val card = FilledCard(
            id = "gold_${request.gameId}_${System.currentTimeMillis()}",
            game = request.gameId,
            text = goldCard.text,
            family = "gold_standard",
            spice = goldCard.spice,
            locality = 1,
            metadata = mapOf(
                "generated_by" to "gold_standard",
                "quality_score" to goldCard.quality_score
            )
        )

        val options = when (request.gameId) {
            GameIds.POISON_PITCH -> {
                GameOptions.AB(goldCard.optionA ?: "A", goldCard.optionB ?: "B")
            }
            GameIds.TABOO -> {
                GameOptions.Taboo(
                    goldCard.word ?: "word", 
                    goldCard.forbidden ?: listOf("1", "2", "3")
                )
            }
            GameIds.ALIBI -> {
                GameOptions.HiddenWords(goldCard.words ?: listOf("word1", "word2"))
            }
            // Legacy games removed: MAJORITY, ODD_ONE, HYPE_YIKE
            GameIds.SCATTER -> {
                GameOptions.Scatter(
                    goldCard.category ?: "Things", 
                    goldCard.letter ?: "A"
                )
            }
            GameIds.TEXT_TRAP -> {
                GameOptions.ReplyTone(
                    goldCard.tones ?: listOf("Casual", "Formal", "Chaotic", "Petty")
                )
            }
            GameIds.ROAST_CONS -> GameOptions.PlayerVote(request.players)
            GameIds.CONFESS_CAP -> GameOptions.TrueFalse
            GameIds.RED_FLAG -> GameOptions.AB("SMASH", "PASS")
            else -> GameOptions.None
        }

        return GenerationResult(
            card, 
            options,
            getTimerForGame(request.gameId),
            getInteractionTypeForGame(request.gameId),
            usedLLM = false,
            qualityScore = goldCard.quality_score / 10.0
        )
    }
}
