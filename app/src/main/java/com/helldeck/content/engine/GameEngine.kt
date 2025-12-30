package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.augment.Augmentor
import com.helldeck.content.generator.CardGeneratorV3
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import com.helldeck.content.validation.CardQualityInspector
import com.helldeck.engine.Config
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.InteractionType
import com.helldeck.utils.Logger

class GameEngine(
    private val repo: ContentRepository,
    private val rng: SeededRng,
    private val selector: ContextualSelector,
    private val augmentor: Augmentor?,
    private val modelId: String,
    private val cardGeneratorV3: CardGeneratorV3?,
    private val llmCardGeneratorV2: com.helldeck.content.generator.LLMCardGeneratorV2? = null,
) {
    private val optionsCompiler = OptionsCompiler(repo, rng)

    data class Request(
        val sessionId: String,
        val gameId: String? = null,
        val players: List<String> = emptyList(),
        val activePlayer: String? = null,
        val roomHeat: Double = 0.6,
        val spiceMax: Int = 3,
        val recentFamilies: List<String> = emptyList(),
        val avoidTemplateIds: Set<String> = emptySet(),
        val tagAffinity: Map<String, Double> = emptyMap(),
        val localityMax: Int = 3,
    )

    data class Result(
        val filledCard: FilledCard,
        val options: GameOptions,
        val timer: Int,
        val interactionType: InteractionType,
    )

    suspend fun next(req: Request): Result {
        val maxContractAttempts = 15 // Bounded retry for contract validation

        // PRIORITY 1: Try LLM Card Generator V2 (quality-first with gold examples)
        llmCardGeneratorV2?.let { llmGen ->
            val llmRequest = com.helldeck.content.generator.LLMCardGeneratorV2.GenerationRequest(
                gameId = req.gameId ?: return@let,
                players = req.players,
                spiceMax = req.spiceMax,
                sessionId = req.sessionId,
                roomHeat = req.roomHeat,
            )

            llmGen.generate(llmRequest)?.let { llmResult ->
                val result = Result(
                    filledCard = llmResult.filledCard,
                    options = llmResult.options,
                    timer = llmResult.timer,
                    interactionType = llmResult.interactionType,
                )
                if (validateContract(result, req)) {
                    Logger.d("LLM V2 generated card with quality score: ${llmResult.qualityScore}")
                    return result
                } else {
                    Logger.w("LLM V2 card failed contract validation, falling back")
                }
            }
        }

        // PRIORITY 2: Fall back to Card Generator V3 (template system)
        cardGeneratorV3?.let { generator ->
            val cfg = Config.current.generator
            if (cfg.safe_mode_gold_only) {
                generator.goldOnly(req, rng)?.let {
                    val result = convert(it)
                    if (validateContract(result, req)) {
                        return result
                    } else {
                        Logger.w("Gold card failed contract validation: ${result.filledCard.id}")
                    }
                }
            }
            if (cfg.enable_v3_generator) {
                // Try generating with contract validation
                repeat(maxContractAttempts) { attempt ->
                    generator.generate(req, rng)?.let { generationResult ->
                        val result = convert(generationResult)
                        val contractCheck = com.helldeck.content.validation.GameContractValidator.validate(
                            gameId = result.filledCard.game,
                            interactionType = result.interactionType,
                            options = result.options,
                            filledCard = result.filledCard,
                            playersCount = req.players.size,
                        )
                        if (contractCheck.isValid) {
                            return result
                        } else {
                            Logger.d(
                                "Contract validation failed (attempt ${attempt + 1}): ${contractCheck.reasons.joinToString(
                                    ", ",
                                )}",
                            )
                        }
                    }
                }
                Logger.w("All V3 generation attempts failed contract validation")
            }
        }

        val pool = repo.templatesV2()
        var avoid = req.avoidTemplateIds.toMutableSet()

        repeat(4) { _ ->
            val ctx = ContextualSelector.Context(
                players = req.players,
                activePlayer = req.activePlayer,
                roomHeat = req.roomHeat,
                spiceMax = req.spiceMax,
                wantedGameId = req.gameId,
                recentFamilies = req.recentFamilies,
                avoidIds = avoid,
                tagAffinity = req.tagAffinity,
            )

            val chosen = selector.pick(ctx, pool)
            val filled = fill(chosen, req)
            val augmented = augment(chosen, filled, req)
            var options = optionsCompiler.compile(chosen, augmented, req.players)

            if (isSensible(augmented, options)) {
                val timer = timerFor(chosen.game)
                val interaction = interactionFor(chosen.game)
                return Result(augmented, options, timer, interaction)
            }

            // If augmentation made it worse, fallback to original fill once
            options = optionsCompiler.compile(chosen, filled, req.players)
            if (isSensible(filled, options)) {
                val timer = timerFor(chosen.game)
                val interaction = interactionFor(chosen.game)
                return Result(filled, options, timer, interaction)
            }

            // Avoid this template on next try
            avoid.add(chosen.id)
        }

        // Fallback: last attempt without quality check
        val ctx = ContextualSelector.Context(
            players = req.players,
            activePlayer = req.activePlayer,
            roomHeat = req.roomHeat,
            spiceMax = req.spiceMax,
            wantedGameId = req.gameId,
            recentFamilies = req.recentFamilies,
            avoidIds = avoid,
            tagAffinity = req.tagAffinity,
        )
        val chosen = selector.pick(ctx, pool)
        val filled = fill(chosen, req)
        val augmented = augment(chosen, filled, req)
        val options = optionsCompiler.compile(chosen, augmented, req.players)
        val timer = timerFor(chosen.game)
        val interaction = interactionFor(chosen.game)
        val lastAttempt = Result(augmented, options, timer, interaction)

        // Validate last attempt; if it fails, use gold fallback
        if (validateContract(lastAttempt, req)) {
            return lastAttempt
        } else {
            Logger.w("V2 generation failed contract; using gold fallback")
            return createGoldFallback(req)
        }
    }

    private fun convert(result: CardGeneratorV3.GenerationResult): Result =
        Result(result.filledCard, result.options, result.timer, result.interactionType)

    /**
     * Validates that a generated result satisfies game contract
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

    private fun isSensible(card: FilledCard, options: GameOptions): Boolean {
        val issues = CardQualityInspector.evaluate(card, options)
        if (issues.isEmpty()) return true
        if (Config.current.debug.enable_template_selection_logging) {
            Logger.d("Rejecting card '${card.id}': $issues | text='${card.text.trim()}'")
        }
        return false
    }

    private fun fill(t: TemplateV2, req: Request): FilledCard {
        return TemplateEngine(repo, rng)
            .fill(
                t,
                TemplateEngine.Context(
                    players = req.players,
                    spiceMax = req.spiceMax,
                    localityMax = req.localityMax,
                    inboundTexts = emptyList(),
                ),
            )
    }

    private suspend fun augment(t: TemplateV2, card: FilledCard, req: Request): FilledCard {
        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = t.max_words ?: 18,
            spice = t.spice,
            tags = t.tags,
            gameId = t.game,
            styleGuide = StyleGuides.getForGame(t.game),
        )
        val seed = (t.id + card.text + req.sessionId).hashCode()
        return augmentor?.maybeParaphrase(card, plan, seed, modelId) ?: card
    }

    fun getOptionsFor(card: FilledCard, req: Request): GameOptions {
        val template = repo.templatesV2().firstOrNull { it.id == card.id }
        return if (template != null) optionsCompiler.compile(template, card, req.players) else GameOptions.None
    }

    private fun timerFor(gameId: String): Int {
        return GameMetadata.getGameMetadata(gameId)?.timerSec ?: 10
    }

    private fun interactionFor(gameId: String): InteractionType {
        return GameMetadata.getGameMetadata(gameId)?.interactionType ?: InteractionType.NONE
    }

    fun recordOutcome(templateId: String, reward01: Double) {
        val r = reward01.coerceIn(0.0, 1.0)
        selector.update(templateId, r)
        // Persist basic stats so selection improves over time across sessions
        try {
            kotlinx.coroutines.runBlocking {
                val dao = repo.statsDao
                val current = dao.get(templateId)
                val visits = (current?.visits ?: 0) + 1
                val rewardSum = (current?.rewardSum ?: 0.0) + r
                dao.upsert(com.helldeck.content.db.TemplateStatEntity(templateId, visits, rewardSum))
            }
        } catch (e: Exception) {
            Logger.e("Failed to record outcome for template $templateId", e)
        }
    }

    /**
     * Creates a guaranteed-valid fallback card for when generation fails.
     * Each interaction type has a safe fallback.
     */
    private fun createGoldFallback(req: Request): Result {
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
            id = "gold_fallback_${interactionType.name}",
            game = gameId,
            text = text,
            family = "gold_fallback",
            spice = 1,
            locality = 1,
            metadata = mapOf("fallback" to true, "interactionType" to interactionType.name),
        )

        return Result(filledCard, options, timer, interactionType)
    }
}
