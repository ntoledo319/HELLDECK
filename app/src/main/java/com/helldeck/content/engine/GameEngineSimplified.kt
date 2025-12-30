package com.helldeck.content.engine

import com.helldeck.content.generator.LLMCardGenerator
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.InteractionType
import com.helldeck.utils.Logger

/**
 * SIMPLIFIED Game Engine - Streamlined for single LLM path
 *
 * Architecture:
 * 1. LLMCardGenerator (primary, with gold fallback built-in)
 * 2. Hardcoded safe fallback (last resort)
 *
 * Removed:
 * - CardGeneratorV3 (template system)
 * - Multiple fallback layers
 * - TemplateEngine, ContextualSelector, Augmentor
 * - Complex validation chains
 */
class GameEngineSimplified(
    private val llmCardGenerator: LLMCardGenerator,
) {

    data class Request(
        val sessionId: String,
        val gameId: String? = null,
        val players: List<String> = emptyList(),
        val activePlayer: String? = null,
        val roomHeat: Double = 0.6,
        val spiceMax: Int = 3,
        val localityMax: Int = 3,
    )

    data class Result(
        val filledCard: FilledCard,
        val options: GameOptions,
        val timer: Int,
        val interactionType: InteractionType,
    )

    /**
     * Generate next card - simplified single path
     */
    suspend fun next(req: Request): Result {
        val gameId = req.gameId ?: GameMetadata.getAllGameIds().random()

        // Try LLM generation (includes automatic gold fallback)
        val llmRequest = LLMCardGenerator.GenerationRequest(
            gameId = gameId,
            players = req.players,
            spiceMax = req.spiceMax,
            sessionId = req.sessionId,
            roomHeat = req.roomHeat,
        )

        llmCardGenerator.generate(llmRequest)?.let { result ->
            val finalResult = Result(
                filledCard = result.filledCard,
                options = result.options,
                timer = result.timer,
                interactionType = result.interactionType,
            )

            // Validate contract
            if (validateContract(finalResult, req)) {
                Logger.d("Card generated successfully (LLM: ${result.usedLLM}, quality: ${result.qualityScore})")
                return finalResult
            } else {
                Logger.w("Generated card failed contract validation")
            }
        }

        // Last resort: hardcoded safe fallback
        Logger.w("All generation failed, using safe fallback")
        return createSafeFallback(req)
    }

    /**
     * Validate that a generated result satisfies game contract
     */
    private fun validateContract(result: Result, req: Request): Boolean {
        val contractResult = com.helldeck.content.validation.GameContractValidator.validate(
            gameId = result.filledCard.game,
            interactionType = result.interactionType,
            options = result.options,
            filledCard = result.filledCard,
            playersCount = req.players.size,
        )
        if (!contractResult.isValid) {
            Logger.w("Contract validation failed: ${contractResult.reasons.joinToString(", ")}")
        }
        return contractResult.isValid
    }

    /**
     * Creates a guaranteed-valid fallback card for when all generation fails
     */
    private fun createSafeFallback(req: Request): Result {
        val gameId = req.gameId ?: GameMetadata.getAllGameIds().random()
        val metadata = GameMetadata.getGameMetadata(gameId)
        val interactionType = metadata?.interactionType ?: InteractionType.NONE
        val timer = metadata?.timerSec ?: 15

        val (text, options) = when (interactionType) {
            InteractionType.A_B_CHOICE -> {
                "Would you rather have unlimited coffee or unlimited pizza?" to
                    GameOptions.AB("Unlimited Coffee", "Unlimited Pizza")
            }
            InteractionType.VOTE_PLAYER -> {
                "Who is most likely to survive a zombie apocalypse?" to
                    GameOptions.PlayerVote(req.players.ifEmpty { listOf("Player 1", "Player 2", "Player 3") })
            }
            InteractionType.TRUE_FALSE -> {
                "I once convinced someone I could speak three languages (I can't)." to
                    GameOptions.TrueFalse
            }
            InteractionType.SMASH_PASS -> {
                "A partner who's always 10 minutes late but brings snacks." to
                    GameOptions.AB("SMASH", "PASS")
            }
            InteractionType.TABOO_GUESS -> {
                "Get your team to guess this word without using forbidden terms!" to
                    GameOptions.Taboo("Password", listOf("computer", "login", "security"))
            }
            InteractionType.JUDGE_PICK -> {
                "Complete this: The worst superpower would be..." to
                    GameOptions.None
            }
            InteractionType.REPLY_TONE -> {
                "Your ex texts: 'Hey, you up?' Pick your vibe:" to
                    GameOptions.ReplyTone(listOf("Petty", "Wholesome", "Chaotic", "Deadpan"))
            }
            InteractionType.ODD_EXPLAIN -> {
                "Which doesn't belong?" to
                    GameOptions.OddOneOut(listOf("Dolphins", "Bats", "Penguins"))
            }
            InteractionType.HIDE_WORDS -> {
                "Sneak these words into your story!" to
                    GameOptions.HiddenWords(listOf("rubber duck", "midnight"))
            }
            InteractionType.SALES_PITCH -> {
                "Pitch this product with a straight face:" to
                    GameOptions.Product("Edible socks")
            }
            InteractionType.SPEED_LIST -> {
                "Name three things fast!" to
                    GameOptions.Scatter("Animals", "S")
            }
            InteractionType.MINI_DUEL -> {
                "Rock-paper-scissors showdown! Best of three." to
                    GameOptions.Challenge("Duel!")
            }
            InteractionType.TARGET_SELECT -> {
                "Pick someone to answer this: What's your secret talent?" to
                    GameOptions.PlayerSelect(req.players, null)
            }
            InteractionType.PREDICT_VOTE -> {
                "Predict what the room will choose: Tacos vs Pizza?" to
                    GameOptions.AB("Tacos", "Pizza")
            }
            else -> {
                "Everyone: share your most embarrassing moment from this week!" to
                    GameOptions.None
            }
        }

        val filledCard = FilledCard(
            id = "safe_fallback_${interactionType.name}",
            game = gameId,
            text = text,
            family = "safe_fallback",
            spice = 1,
            locality = 1,
            metadata = mapOf("fallback" to true, "interactionType" to interactionType.name),
        )

        return Result(filledCard, options, timer, interactionType)
    }
}
