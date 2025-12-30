package com.helldeck.content.generator

import android.content.Context
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.GameIds
import com.helldeck.engine.InteractionType
import com.helldeck.llm.GenConfig
import com.helldeck.llm.LocalLLM
import com.helldeck.utils.Logger
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * Quality-first LLM card generator using gold standard examples.
 * Generates unique, high-quality cards on-the-fly.
 */
class LLMCardGeneratorV2(
    private val llm: LocalLLM?,
    private val context: Context,
    private val templateFallback: CardGeneratorV3,
) {

    data class GenerationRequest(
        val gameId: String,
        val players: List<String>,
        val spiceMax: Int,
        val sessionId: String,
        val roomHeat: Double = 0.6,
    )

    data class GenerationResult(
        val filledCard: FilledCard,
        val options: GameOptions,
        val timer: Int,
        val interactionType: InteractionType,
        val usedLLM: Boolean,
        val qualityScore: Double = 0.0,
    )

    suspend fun generate(request: GenerationRequest): GenerationResult? {
        var llmResult: GenerationResult? = null

        if (llm?.isReady == true) {
            repeat(3) { attempt ->
                try {
                    val candidate = withTimeout(2500) { // 2.5 sec timeout
                        generateWithLLM(request, attempt)
                    }
                    if (candidate != null && validateQuality(candidate)) {
                        llmResult = candidate
                        return@repeat
                    } else if (candidate != null) {
                        Logger.d("LLM card failed quality check (attempt ${attempt + 1})")
                    }
                } catch (e: Exception) {
                    Logger.w("LLM generation attempt ${attempt + 1} failed: ${e.message}")
                }
            }
        }

        // Fallback: try gold cards first, then templates
        return llmResult ?: fallbackToGold(request) ?: fallbackToTemplates(request)
    }

    private suspend fun generateWithLLM(request: GenerationRequest, attempt: Int): GenerationResult? {
        val prompt = buildQualityPrompt(request, attempt)
        val config = GenConfig(
            maxTokens = 150,
            temperature = when (request.spiceMax) {
                1 -> 0.5f
                2 -> 0.6f
                3 -> 0.75f
                4 -> 0.85f
                else -> 0.9f
            },
            topP = 0.92f,
            seed = (request.sessionId + System.currentTimeMillis() + attempt).hashCode(),
        )

        val response = llm?.generate(prompt.system, prompt.user, config) ?: return null
        return parseAndValidateResponse(response, request)
    }

    private data class Prompt(val system: String, val user: String)

    private fun buildQualityPrompt(request: GenerationRequest, attempt: Int): Prompt {
        // Generate seed for weighted rotation of examples
        val seed = (request.sessionId + System.currentTimeMillis() + attempt).hashCode()

        // Load 10 gold examples with weighted rotation
        // This ensures ALL gold cards can train the AI, but better cards appear more often
        val goldExamples = GoldCardsLoader.getExamplesForGame(
            context,
            request.gameId,
            count = 10, // Increased from 5 to 10
            seed = seed, // Enable rotation across generations
        )

        val spiceGuidance = when (request.spiceMax) {
            1 -> "wholesome and PG-13 (family-friendly)"
            2 -> "fun and playful with light edge"
            3 -> "edgy and provocative but not mean-spirited"
            4 -> "wild and unhinged while avoiding slurs"
            else -> "maximum chaos (keep it funny, not cruel)"
        }

        val system = """You are an expert comedy writer for HELLDECK, a party game app.

CRITICAL RULES:
1. Generate EXACTLY ONE card in valid JSON format
2. Output ONLY the JSON - no markdown, no explanation
3. Be SPECIFIC and UNEXPECTED - avoid clichés
4. Spice level: $spiceGuidance
5. NEVER use slurs, target protected groups, or be genuinely cruel
6. Every card must be UNIQUE - never repeat patterns"""

        val user = when (request.gameId) {
            GameIds.ROAST_CONS -> buildRoastPrompt(goldExamples, attempt)
            GameIds.CONFESS_CAP -> buildConfessPrompt(goldExamples, attempt)
            GameIds.POISON_PITCH -> buildPoisonPitchPrompt(goldExamples, attempt)
            GameIds.FILL_IN -> buildFillInPrompt(goldExamples, attempt)
            GameIds.RED_FLAG -> buildRedFlagPrompt(goldExamples, attempt)
            GameIds.HOTSEAT_IMP -> buildHotSeatPrompt(goldExamples, attempt)
            GameIds.TEXT_TRAP -> buildTextTrapPrompt(goldExamples, attempt)
            GameIds.TABOO -> buildTabooPrompt(goldExamples, attempt)
            GameIds.TITLE_FIGHT -> buildTitleFightPrompt(goldExamples, attempt)
            GameIds.ALIBI -> buildAlibiPrompt(goldExamples, attempt)
            GameIds.SCATTER -> buildScatterPrompt(goldExamples, attempt)

            // New games from HDRealRules.md
            GameIds.UNIFYING_THEORY -> buildUnifyingTheoryPrompt(goldExamples, attempt)
            GameIds.REALITY_CHECK -> buildRealityCheckPrompt(goldExamples, attempt)
            GameIds.OVER_UNDER -> buildOverUnderPrompt(goldExamples, attempt)

            else -> """{"text": "Fallback card", "type": "unknown"}"""
        }

        return Prompt(system, user)
    }

    private fun buildRoastPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.joinToString("\n") {
            "✅ GREAT: \"${it.text}\" (score: ${it.quality_score}/10)"
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

TOP-TIER EXAMPLES (emulate these):
$exampleText

❌ AVOID:
- "Most likely to be late" (too generic)
- "Most likely to eat all the pizza" (cliché)
- Physical appearance attacks
- Anything genuinely hurtful

OUTPUT:
Generate ONE unique roast card in JSON format. Be creative!"""
    }

    private fun buildPoisonPitchPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.joinToString("\n") {
            "✅ \"${it.text}\" | Options: \"${it.optionA}\" vs \"${it.optionB}\" (score: ${it.quality_score}/10)"
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
✓ CREATIVE - Unexpected combinations

TOP-TIER EXAMPLES:
$exampleText

❌ AVOID:
- One option obviously better
- Generic choices ("rich vs poor")
- Too similar options
- Physically harmful scenarios

OUTPUT:
Generate ONE unique dilemma in JSON format."""
    }

    private fun buildFillInPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
        }

        return """Generate a fill-in-the-blank prompt:

FORMAT:
{
  "text": "Prompt with _____ blank to complete"
}

QUALITY CRITERIA:
✓ OPEN-ENDED - Multiple funny answers possible
✓ RELATABLE - Players can connect to it
✓ NOT TOO OBVIOUS - Avoid easy/boring answers
✓ CREATIVE POTENTIAL - Invites unexpected responses

TOP-TIER EXAMPLES:
$exampleText

❌ AVOID:
- Blanks with obvious answers
- Too broad ("I like _____")
- Too narrow (only one answer works)

OUTPUT:
Generate ONE unique prompt in JSON format."""
    }

    private fun buildRedFlagPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
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

TOP-TIER EXAMPLES:
$exampleText

❌ AVOID:
- Weak red flags ("they snore")
- Undefensible (actual crimes)
- Generic qualities

OUTPUT:
Generate ONE unique red flag scenario in JSON format."""
    }

    private fun buildHotSeatPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
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
✓ REVEALING - Shows who they are

TOP-TIER EXAMPLES:
$exampleText

OUTPUT:
Generate ONE unique question in JSON format."""
    }

    private fun buildTextTrapPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
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
✓ MODERN - Current texting culture

TOP-TIER EXAMPLES:
$exampleText

Common tones: Flirty, Petty, Wholesome, Chaotic, Cold, Panicked, Professional, Gaslighting

OUTPUT:
Generate ONE scenario in JSON with 4 tone options."""
    }

    private fun buildTabooPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") { card ->
            "✅ Word: \"${card.word}\" | Forbidden: ${card.forbidden} (score: ${card.quality_score}/10)"
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
✓ MODERN - Current vocabulary

TOP-TIER EXAMPLES:
$exampleText

OUTPUT:
Generate ONE Taboo card in JSON format."""
    }

    private fun buildTitleFightPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
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
✓ CREATIVE - Unexpected combinations

TOP-TIER EXAMPLES:
$exampleText

OUTPUT:
Generate ONE challenge in JSON format."""
    }

    private fun buildAlibiPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ Words: ${it.words} | \"${it.text}\" (score: ${it.quality_score}/10)"
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
✓ UNEXPECTED - Surprising combinations

TOP-TIER EXAMPLES:
$exampleText

OUTPUT:
Generate ONE challenge with 3 random words in JSON format."""
    }

    private fun buildScatterPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ Category: \"${it.category}\", Letter: ${it.letter} (score: ${it.quality_score}/10)"
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
✓ FUN - Interesting to think about

TOP-TIER EXAMPLES:
$exampleText

OUTPUT:
Generate ONE challenge in JSON format."""
    }

    private fun buildConfessPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.take(5).joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
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
✓ SPECIFIC - Concrete details

TOP-TIER EXAMPLES:
$exampleText

OUTPUT:
Generate ONE confession in JSON format."""
    }

    // New game prompt builders
    private fun buildUnifyingTheoryPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
        }

        return """Generate a Unifying Theory challenge:

FORMAT:
{
  "text": "Item1, Item2, Item3"
}

QUALITY CRITERIA:
✓ UNRELATED ITEMS - Three completely different things
✓ CONNECTION POSSIBLE - Can find creative links
✓ VISUAL/MEMORABLE - Easy to picture all three
✓ SPICE AWARE - At Spice 4+, allow inappropriate connections

TOP-TIER EXAMPLES:
$exampleText

EXAMPLES:
- "A Priest, A Referee, A Zebra" → "They all wear black and white"
- "Your Ex, The IRS, A Magician" → "They all make things disappear"
- "A Vampire, A Sponge, Your Mom" → "They all suck things in"

OUTPUT:
Generate ONE trio of unrelated items in JSON format."""
    }

    private fun buildRealityCheckPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
        }

        return """Generate a Reality Check rating prompt:

FORMAT:
{
  "text": "Rating: Your [TRAIT OR ABILITY]"
}

QUALITY CRITERIA:
✓ SUBJECTIVE - No objective right answer
✓ EGO-PRONE - People overestimate this trait
✓ OBSERVABLE - Group has seen evidence
✓ EMBARRASSING - Gap between self/reality is funny

TOP-TIER EXAMPLES:
$exampleText

AVOID:
- Objective traits ("your height")
- Too serious ("how good of a person are you")
- Too niche (skills only some have)

OUTPUT:
Generate ONE rating question in JSON format."""
    }

    private fun buildOverUnderPrompt(examples: List<GoldCardsLoader.GoldCard>, attempt: Int): String {
        val exampleText = examples.joinToString("\n") {
            "✅ \"${it.text}\" (score: ${it.quality_score}/10)"
        }

        return """Generate an Over/Under prediction question:

FORMAT:
{
  "text": "Number of [VERIFIABLE QUANTITY ABOUT TARGET PLAYER]"
}

QUALITY CRITERIA:
✓ VERIFIABLE - Can prove the answer (phone check, counting, etc.)
✓ SURPRISING - Answer often higher/lower than expected
✓ PERSONAL - Reveals something about the person
✓ TESTABLE - Can check immediately if needed

TOP-TIER EXAMPLES:
$exampleText

CATEGORIES:
- Digital: "unread emails", "photos in camera roll", "screen time yesterday"
- Physical: "push-ups you can do right now", "cash in your wallet"
- Historical: "number of jobs you've had", "countries you've visited"

OUTPUT:
Generate ONE numerical question in JSON format."""
    }

    private fun parseAndValidateResponse(response: String, request: GenerationRequest): GenerationResult? {
        return try {
            val cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleaned)
            val text = json.optString("text", "")

            if (text.isBlank() || text.length < 10) return null

            val card = FilledCard(
                id = "llm_v2_${request.gameId}_${System.currentTimeMillis()}",
                game = request.gameId,
                text = text,
                family = "llm_generated_v2",
                spice = request.spiceMax,
                locality = 1,
                metadata = mapOf(
                    "generated_by" to "llm_v2",
                    "model" to (llm?.modelId ?: "unknown"),
                    "timestamp" to System.currentTimeMillis(),
                    "prompt_version" to "quality_first_v1",
                ),
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

    private fun validateQuality(result: GenerationResult): Boolean {
        // Basic quality checks
        val text = result.filledCard.text

        // Minimum quality score
        if (result.qualityScore < 0.6) {
            Logger.d("Quality score too low: ${result.qualityScore}")
            return false
        }

        // Check for common clichés (game-specific)
        val badPhrases = when (result.filledCard.game) {
            GameIds.ROAST_CONS -> listOf("be late", "eat pizza", "eat all", "be the one")
            GameIds.POISON_PITCH -> listOf("would you rather have", "or would you")
            else -> emptyList()
        }

        if (badPhrases.any { text.contains(it, ignoreCase = true) }) {
            Logger.d("Contains cliché phrase")
            return false
        }

        // Check minimum length
        if (text.length < 15) {
            Logger.d("Text too short: ${text.length} chars")
            return false
        }

        return true
    }

    private fun estimateQuality(card: FilledCard, options: GameOptions): Double {
        var score = 0.7 // Base score

        // Length check (not too short, not too long)
        val wordCount = card.text.split("\\s+".toRegex()).size
        score += when {
            wordCount < 5 -> -0.3
            wordCount > 30 -> -0.2
            wordCount in 10..20 -> 0.2
            else -> 0.0
        }

        // Specificity (contains numbers, names, specific details)
        if (card.text.contains(Regex("\\d"))) score += 0.1
        if (card.text.contains(Regex("[A-Z][a-z]+"))) score += 0.05

        // Options quality (for applicable games)
        when (options) {
            is GameOptions.AB -> {
                if (options.optionA.isNotBlank() && options.optionB.isNotBlank()) score += 0.1
            }
            is GameOptions.Taboo -> {
                if (options.forbidden.size == 3) score += 0.1
            }
            else -> {}
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun parseOptionsFromJson(json: JSONObject, request: GenerationRequest): GameOptions {
        return when (request.gameId) {
            GameIds.ROAST_CONS -> GameOptions.PlayerVote(request.players)

            GameIds.POISON_PITCH, GameIds.RED_FLAG -> {
                val a = json.optString("optionA", "Option A")
                val b = json.optString("optionB", "Option B")
                if (request.gameId == GameIds.RED_FLAG) {
                    GameOptions.AB("SMASH", "PASS")
                } else {
                    GameOptions.AB(a, b)
                }
            }

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

            // New games - use existing GameOptions types
            GameIds.UNIFYING_THEORY -> {
                val items = json.optString("text", "A, B, C").split(", ")
                GameOptions.Challenge("Find what connects: ${items.joinToString(", ")}")
            }

            GameIds.REALITY_CHECK -> GameOptions.Challenge("Rate yourself on this trait")

            GameIds.OVER_UNDER -> {
                val question = json.optString("text", "Number of something")
                GameOptions.Challenge("Predict: $question")
            }

            else -> GameOptions.None
        }
    }

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

        // New games
        GameIds.UNIFYING_THEORY -> InteractionType.JUDGE_PICK // Explaining the connection
        GameIds.REALITY_CHECK -> InteractionType.TARGET_SELECT // Rating comparison
        GameIds.OVER_UNDER -> InteractionType.PREDICT_VOTE // Over/Under betting

        else -> InteractionType.NONE
    }

    private fun fallbackToGold(request: GenerationRequest): GenerationResult? {
        val goldCard = GoldCardsLoader.getRandomFallback(context, request.gameId) ?: return null

        val card = FilledCard(
            id = "gold_${request.gameId}_${System.currentTimeMillis()}",
            game = request.gameId,
            text = goldCard.text,
            family = "gold_v2",
            spice = goldCard.spice,
            locality = 1,
            metadata = mapOf(
                "generated_by" to "gold_v2",
                "quality_score" to goldCard.quality_score,
            ),
        )

        val options = when (request.gameId) {
            GameIds.POISON_PITCH -> {
                GameOptions.AB(goldCard.optionA ?: "A", goldCard.optionB ?: "B")
            }
            GameIds.TABOO -> {
                GameOptions.Taboo(goldCard.word ?: "word", goldCard.forbidden ?: listOf("1", "2", "3"))
            }
            GameIds.ALIBI -> {
                GameOptions.HiddenWords(goldCard.words ?: listOf("word1", "word2"))
            }
            GameIds.SCATTER -> {
                GameOptions.Scatter(goldCard.category ?: "Things", goldCard.letter ?: "A")
            }
            GameIds.TEXT_TRAP -> {
                GameOptions.ReplyTone(goldCard.tones ?: listOf("Casual", "Formal", "Chaotic", "Petty"))
            }
            GameIds.ROAST_CONS -> GameOptions.PlayerVote(request.players)
            GameIds.CONFESS_CAP -> GameOptions.TrueFalse
            GameIds.RED_FLAG -> GameOptions.AB("SMASH", "PASS")

            // New games - need to handle these properly
            GameIds.UNIFYING_THEORY -> {
                val items = goldCard.text.split(", ")
                GameOptions.Challenge("Find the connection")
            }
            GameIds.REALITY_CHECK -> GameOptions.Challenge("Rate yourself")
            GameIds.OVER_UNDER -> GameOptions.Challenge("Predict the number")

            else -> GameOptions.None
        }

        return GenerationResult(
            card,
            options,
            getTimerForGame(request.gameId),
            getInteractionTypeForGame(request.gameId),
            usedLLM = false,
            qualityScore = goldCard.quality_score / 10.0,
        )
    }

    private fun fallbackToTemplates(request: GenerationRequest): GenerationResult? {
        val templateRequest = com.helldeck.content.engine.GameEngine.Request(
            sessionId = request.sessionId,
            gameId = request.gameId,
            players = request.players,
            spiceMax = request.spiceMax,
            roomHeat = request.roomHeat,
        )

        val rng = com.helldeck.content.util.SeededRng(request.sessionId.hashCode().toLong())

        return templateFallback.generate(templateRequest, rng)?.let {
            GenerationResult(
                filledCard = it.filledCard,
                options = it.options,
                timer = it.timer,
                interactionType = it.interactionType,
                usedLLM = false,
                qualityScore = 0.5,
            )
        }
    }
}
