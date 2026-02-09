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
                    val candidate = withTimeout(6000) { // 6 sec timeout
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
            maxSpice = request.spiceMax, // Filter examples by requested spice level
        )

        val spiceGuidance = when (request.spiceMax) {
            1 -> "wholesome and PG-13 (family-friendly, no dating/relationship humor)"
            2 -> "fun and playful with light edge (mild awkwardness OK)"
            3 -> "edgy and provocative but not mean-spirited"
            4 -> "wild and unhinged while avoiding slurs"
            else -> "maximum chaos (keep it funny, not cruel)"
        }

        val avoidList = when (request.spiceMax) {
            1 -> "dating, exes, alcohol, parties, anything remotely adult"
            2 -> "explicit content, heavy drinking, relationship drama"
            else -> "slurs, protected groups, genuinely cruel content"
        }

        val system = """${ComedySciencePrompts.COMEDY_SCIENCE_SYSTEM}

CURRENT SPICE LEVEL: $spiceGuidance

CRITICAL OUTPUT RULES:
1. Generate EXACTLY ONE card in valid JSON format
2. Output ONLY the JSON - no markdown, no explanation, no preamble
3. Every card must be UNIQUE - never repeat patterns
4. AVOID these topics at spice level ${request.spiceMax}: $avoidList

NEVER USE:
- Slurs or attacks on protected groups
- Genuine cruelty (roast behavior, not people)
- These clichés: "be late", "eat pizza", "Netflix and chill", "your ex""""

        val user = ComedySciencePrompts.getPromptForGame(
            gameId = request.gameId,
            examples = goldExamples,
            spiceLevel = request.spiceMax
        )

        return Prompt(system, user)
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
        val text = result.filledCard.text
        val gameId = result.filledCard.game
        
        // Basic quality score check
        if (result.qualityScore < 0.65) {
            Logger.d("Quality score too low: ${result.qualityScore}")
            return false
        }
        
        // Extract game-specific options for validation
        val optionA: String?
        val optionB: String?
        val category: String?
        val forbidden: List<String>?
        val words: List<String>?
        val tones: List<String>?
        
        when (val opts = result.options) {
            is GameOptions.AB -> {
                optionA = opts.optionA
                optionB = opts.optionB
                category = null
                forbidden = null
                words = null
                tones = null
            }
            is GameOptions.Taboo -> {
                optionA = null
                optionB = null
                category = null
                forbidden = opts.forbidden
                words = null
                tones = null
            }
            is GameOptions.HiddenWords -> {
                optionA = null
                optionB = null
                category = null
                forbidden = null
                words = opts.words
                tones = null
            }
            is GameOptions.Scatter -> {
                optionA = null
                optionB = null
                category = opts.category
                forbidden = null
                words = null
                tones = null
            }
            is GameOptions.ReplyTone -> {
                optionA = null
                optionB = null
                category = null
                forbidden = null
                words = null
                tones = opts.tones
            }
            else -> {
                optionA = null
                optionB = null
                category = null
                forbidden = null
                words = null
                tones = null
            }
        }
        
        // Use ComedyScienceValidator for comprehensive validation
        val validation = ComedyScienceValidator.validate(
            text = text,
            gameId = gameId,
            optionA = optionA,
            optionB = optionB,
            category = category,
            forbidden = forbidden,
            words = words,
            tones = tones
        )
        
        if (!validation.isValid) {
            Logger.d("ComedyScience rejected: ${validation.failureReasons.joinToString(", ")}")
            return false
        }
        
        // Log successful validation with scores
        Logger.d("ComedyScience passed: specificity=${validation.specificityScore}, visual=${validation.visualImageryScore}, total=${validation.totalScore}")
        
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
            GameIds.ROAST_CONS -> GameOptions.SeatVote((1..request.players.size.coerceAtLeast(2)).toList())

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

            GameIds.REALITY_CHECK -> GameOptions.SeatSelect((1..request.players.size.coerceAtLeast(2)).toList(), null)

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
        val goldCard = GoldCardsLoader.getRandomFallback(context, request.gameId, request.spiceMax) ?: return null

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
            GameIds.ROAST_CONS -> GameOptions.SeatVote((1..request.players.size.coerceAtLeast(2)).toList())
            GameIds.CONFESS_CAP -> GameOptions.TrueFalse
            GameIds.RED_FLAG -> GameOptions.AB("SMASH", "PASS")

            // New games - need to handle these properly
            GameIds.UNIFYING_THEORY -> {
                val items = goldCard.text.split(", ")
                GameOptions.Challenge("Find the connection between ${items.joinToString(", ")}")
            }
            GameIds.REALITY_CHECK -> GameOptions.SeatSelect((1..request.players.size.coerceAtLeast(2)).toList(), null)
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
            val score = ComedyScienceValidator.scoreCard(
                it.filledCard.text, request.gameId
            ) / 10.0
            GenerationResult(
                filledCard = it.filledCard,
                options = it.options,
                timer = it.timer,
                interactionType = it.interactionType,
                usedLLM = false,
                qualityScore = score,
            )
        }
    }
}
