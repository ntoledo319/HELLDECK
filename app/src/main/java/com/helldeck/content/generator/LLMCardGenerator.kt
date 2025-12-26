package com.helldeck.content.generator

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
 * LLM-first card generator that creates unique, unpredictable cards on-the-fly.
 * Falls back to template system when LLM is unavailable or slow.
 */
class LLMCardGenerator(
    private val llm: LocalLLM?,
    private val templateFallback: CardGeneratorV3
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
        val usedLLM: Boolean
    )

    suspend fun generate(request: GenerationRequest): GenerationResult? {
        var result: GenerationResult? = null

        if (llm?.isReady == true) {
            try {
                result = withTimeout(2000) {
                    generateWithLLM(request)
                }
            } catch (e: Exception) {
                Logger.w("LLM generation failed: ${e.message}, falling back to templates")
            }
        }

        return result ?: fallbackToTemplates(request)
    }

    private suspend fun generateWithLLM(request: GenerationRequest): GenerationResult? {
        val prompt = buildPromptForGame(request)
        val config = GenConfig(
            maxTokens = 128,
            temperature = if (request.spiceMax >= 3) 0.8f else 0.6f,
            topP = 0.9f,
            seed = (request.sessionId + System.currentTimeMillis()).hashCode()
        )

        val response = llm?.generate(prompt.system, prompt.user, config) ?: return null
        return parseResponse(response, request)
    }

    private data class Prompt(val system: String, val user: String)

    private fun buildPromptForGame(request: GenerationRequest): Prompt {
        val spiceLevel = when (request.spiceMax) {
            1 -> "keep it PG-13, wholesome"
            2 -> "keep it fun, slightly edgy"
            3 -> "can be edgy and provocative"
            4 -> "wild and unhinged, but not offensive"
            else -> "maximum chaos"
        }

        val system = """You are a party game card generator for HELLDECK.
Rules:
- Generate exactly ONE card in JSON format
- Be creative, unexpected, unpredictable
- Never repeat common phrases
- Spice level: $spiceLevel
- No slurs, targeted harassment, or extreme content
- Output ONLY valid JSON, no markdown or extra text"""

        val user = when (request.gameId) {
            GameIds.ROAST_CONS -> """Generate a roast card:
{
  "text": "Most likely to [funny action] because [absurd reason]",
  "targetType": "vote"
}
Examples:
- Most likely to become a professional cave dweller because they already live like one
- Most likely to argue with a toaster because they lost the argument with the microwave
Make it:
- Targeting (clearly about "someone")
- Funny through specificity and absurdity
- Fresh (avoid cliches like "most likely to eat everything")"""

            GameIds.POISON_PITCH -> """Generate a "would you rather" with two equally terrible options:
{
  "text": "Would you rather...",
  "optionA": "first terrible thing",
  "optionB": "second terrible thing"
}
Examples:
- Would you rather have hiccups every time you lie OR sneeze every time someone says your name?
- Would you rather fight one horse-sized duck OR explain Bitcoin to your grandma every day for a year?
Make both options:
- Equally bad/awkward/annoying
- Specific and vivid
- Genuinely difficult to choose"""

            GameIds.FILL_IN -> """Generate a prompt with a blank to fill in:
{
  "text": "_____ is the worst superpower.",
  "prompt_type": "funniest"
}
The blank should invite creative, funny completions. Examples:
- The best excuse for being late: _____
- If I had to eat only one food forever, I'd choose _____ because _____
- My Tinder bio should just say: _____"""

            GameIds.RED_FLAG -> """Generate a dating scenario with a MAJOR red flag:
{
  "text": "They're perfect: [attractive quality], but [HUGE red flag]",
  "defend": true
}
Examples:
- They're perfect: great cook, amazing smile, but they refer to their ex as 'the one that got away' daily
- They're perfect: fit, funny, rich, but they don't believe in cleaning dishes 'because germs build immunity'
The red flag should be:
- Dealbreaker-level absurd
- Specific and visual
- Defensible (players will defend it)"""

            GameIds.HOTSEAT_IMP -> """Generate a personal question for someone to answer:
{
  "text": "What would you do if you won the lottery tomorrow?",
  "targetType": "impersonate"
}
Everyone answers AS the target person. Make it:
- Personal but not too invasive
- Revealing of personality/habits
- Fun to impersonate
Examples:
- What's your signature dance move called?
- Describe your morning routine in 3 words
- What would your WWE entrance song be?"""

            GameIds.TEXT_TRAP -> """Generate a text message scenario:
{
  "text": "Your crush texts: [message]",
  "tones": ["Flirty", "Oblivious", "Petty", "Chaotic"]
}
Examples:
- Your crush texts: "what are you up to this weekend?"
- Your ex texts: "I miss us"
- Your boss texts at 11pm: "quick question..."
Make it:
- Relatable
- Open to multiple reply tones
- Slightly awkward or high-stakes"""

            GameIds.TABOO -> """Generate a Taboo card:
{
  "word": "the word to guess",
  "forbidden": ["word1", "word2", "word3"]
}
Examples:
- word: "Coffee", forbidden: ["drink", "caffeine", "Starbucks"]
- word: "TikTok", forbidden: ["app", "video", "dance"]
The forbidden words should make it challenging but possible."""

            GameIds.ODD_ONE -> """Generate 3 items where one doesn't fit:
{
  "items": ["item1", "item2", "item3"],
  "text": "Which one doesn't belong?"
}
Make it arguable - players defend their choice. Examples:
- ["Pizza", "Tacos", "Salad"] (all food, but salad is healthy)
- ["Dogs", "Cats", "Birds"] (all pets, but birds fly)
- ["Netflix", "Sleep", "Exercise"] (all activities, but which is the outlier?)"""

            GameIds.ALIBI -> """Generate secret words to sneak into an excuse/story:
{
  "text": "Sneak these words into your excuse for being late:",
  "words": ["rubber duck", "conspiracy", "3am"]
}
Words should be:
- Random and unrelated
- Challenging to work in naturally
- 2-3 words max"""

            GameIds.HYPE_YIKE -> """Generate a ridiculous product to pitch:
{
  "product": "absurd product name/description",
  "text": "Pitch this product with a straight face:"
}
Examples:
- Edible phone cases (tastes like your favorite app)
- Deodorant for your feet's feelings
- A pillow that judges your life choices
Make it absurd but pitch-able."""

            GameIds.SCATTER -> """Generate a Scattergories challenge:
{
  "category": "category name",
  "letter": "X",
  "text": "Name 3 [category] starting with [letter]"
}
Examples:
- Category: "Things you'd find in a villain's lair", Letter: "L"
- Category: "Excuses for missing work", Letter: "D"
Make categories creative, not generic."""

            GameIds.MAJORITY -> """Generate a binary choice to predict:
{
  "text": "What will the room choose?",
  "optionA": "first option",
  "optionB": "second option"
}
Examples:
- Tacos vs Pizza
- Cats vs Dogs
- Morning person vs Night owl
Make it divisive but not offensive."""

            GameIds.TITLE_FIGHT -> """Generate a comparative challenge:
{
  "text": "Who would win: [option A] vs [option B]?",
  "challenge": "defend your champion"
}
Examples:
- Who would win: A gorilla with a sword vs 50 angry geese?
- Better superpower: Flying but only 2 feet off the ground vs Invisibility but only when nobody's looking"""

            GameIds.CONFESS_CAP -> """Generate a confession (truth or lie):
{
  "text": "I once [confession statement]",
  "votable": true
}
Examples:
- I once convinced my family I was allergic to chores
- I've never actually finished a book I said I read
- I accidentally joined a cult for 3 days before realizing
Make it believable yet sus."""

            else -> """Generate a party game card:
{
  "text": "Generated card text",
  "type": "general"
}"""
        }

        return Prompt(system, user)
    }

    private fun parseResponse(response: String, request: GenerationRequest): GenerationResult? {
        return try {
            // Clean response (remove markdown if present)
            val cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleaned)
            val text = json.optString("text", "")

            if (text.isBlank()) return null

            // Create FilledCard
            val card = FilledCard(
                id = "llm_${request.gameId}_${System.currentTimeMillis()}",
                game = request.gameId,
                text = text,
                family = "llm_generated",
                spice = request.spiceMax,
                locality = 1,
                metadata = mapOf(
                    "generated_by" to "llm",
                    "model" to (llm?.modelId ?: "unknown"),
                    "timestamp" to System.currentTimeMillis()
                )
            )

            // Parse game-specific options
            val options = parseOptionsFromJson(json, request)
            val timer = getTimerForGame(request.gameId)
            val interactionType = getInteractionTypeForGame(request.gameId)

            GenerationResult(card, options, timer, interactionType, usedLLM = true)

        } catch (e: Exception) {
            Logger.w("Failed to parse LLM response: ${e.message}")
            null
        }
    }

    private fun parseOptionsFromJson(json: JSONObject, request: GenerationRequest): GameOptions {
        return when (request.gameId) {
            GameIds.ROAST_CONS -> GameOptions.PlayerVote(request.players)

            GameIds.POISON_PITCH, GameIds.MAJORITY -> {
                val a = json.optString("optionA", "Option A")
                val b = json.optString("optionB", "Option B")
                GameOptions.AB(a, b)
            }

            GameIds.TABOO -> {
                val word = json.optString("word", "word")
                val forbidden = json.optJSONArray("forbidden")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: listOf("forbidden1", "forbidden2", "forbidden3")
                GameOptions.Taboo(word, forbidden)
            }

            GameIds.ODD_ONE -> {
                val items = json.optJSONArray("items")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: listOf("A", "B", "C")
                GameOptions.OddOneOut(items)
            }

            GameIds.ALIBI -> {
                val words = json.optJSONArray("words")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: listOf("word1", "word2")
                GameOptions.HiddenWords(words)
            }

            GameIds.HYPE_YIKE -> {
                val product = json.optString("product", "A product")
                GameOptions.Product(product)
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

            GameIds.RED_FLAG -> {
                GameOptions.AB("SMASH", "PASS")
            }

            GameIds.HOTSEAT_IMP, GameIds.TITLE_FIGHT -> {
                GameOptions.Challenge(json.optString("challenge", "Pick the best"))
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
        GameIds.POISON_PITCH, GameIds.MAJORITY -> InteractionType.A_B_CHOICE
        GameIds.CONFESS_CAP -> InteractionType.TRUE_FALSE
        GameIds.RED_FLAG -> InteractionType.SMASH_PASS
        GameIds.TABOO -> InteractionType.TABOO_GUESS
        GameIds.ODD_ONE -> InteractionType.ODD_EXPLAIN
        GameIds.ALIBI -> InteractionType.HIDE_WORDS
        GameIds.HYPE_YIKE -> InteractionType.SALES_PITCH
        GameIds.SCATTER -> InteractionType.SPEED_LIST
        GameIds.TITLE_FIGHT -> InteractionType.MINI_DUEL
        GameIds.TEXT_TRAP -> InteractionType.REPLY_TONE
        GameIds.HOTSEAT_IMP -> InteractionType.JUDGE_PICK
        else -> InteractionType.NONE
    }

    private fun fallbackToTemplates(request: GenerationRequest): GenerationResult? {
        val templateRequest = com.helldeck.content.engine.GameEngine.Request(
            sessionId = request.sessionId,
            gameId = request.gameId,
            players = request.players,
            spiceMax = request.spiceMax,
            roomHeat = request.roomHeat
        )

        val rng = com.helldeck.content.util.SeededRng(request.sessionId.hashCode().toLong())

        return templateFallback.generate(templateRequest, rng)?.let {
            GenerationResult(
                filledCard = it.filledCard,
                options = it.options,
                timer = it.timer,
                interactionType = it.interactionType,
                usedLLM = false
            )
        }
    }
}
