package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.augment.Augmentor
import com.helldeck.content.generator.CardGeneratorV3
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.model.Template
import com.helldeck.content.model.v2.OptionProvider
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.InteractionType
import com.helldeck.engine.Config
import com.helldeck.utils.Logger
import com.helldeck.content.validation.CardQualityInspector

class GameEngine(
    private val repo: ContentRepository,
    private val rng: SeededRng,
    private val selector: ContextualSelector,
    private val augmentor: Augmentor?,
    private val modelId: String,
    private val cardGeneratorV3: CardGeneratorV3?
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
        val localityMax: Int = 3
    )

    data class Result(
        val filledCard: FilledCard,
        val options: GameOptions,
        val timer: Int,
        val interactionType: InteractionType
    )

    suspend fun next(req: Request): Result {
        cardGeneratorV3?.let { generator ->
            val cfg = Config.current.generator
            if (cfg.safe_mode_gold_only) {
                generator.goldOnly(req, rng)?.let { return convert(it) }
            }
            if (cfg.enable_v3_generator) {
                generator.generate(req, rng)?.let { return convert(it) }
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
                tagAffinity = req.tagAffinity
            )

            val chosen = selector.pick(ctx, pool)
            val filled = fill(chosen, req)
            val augmented = augment(chosen, filled, req)
            var options = compileOptions(chosen, augmented, req)

            if (isSensible(augmented, options)) {
                val timer = timerFor(chosen.game)
                val interaction = interactionFor(chosen.game)
                return Result(augmented, options, timer, interaction)
            }

            // If augmentation made it worse, fallback to original fill once
            options = compileOptions(chosen, filled, req)
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
            tagAffinity = req.tagAffinity
        )
        val chosen = selector.pick(ctx, pool)
        val filled = fill(chosen, req)
        val augmented = augment(chosen, filled, req)
        val options = compileOptions(chosen, augmented, req)
        val timer = timerFor(chosen.game)
        val interaction = interactionFor(chosen.game)
        return Result(augmented, options, timer, interaction)
    }

    private fun convert(result: CardGeneratorV3.GenerationResult): Result =
        Result(result.filledCard, result.options, result.timer, result.interactionType)

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
            .fill(t, TemplateEngine.Context(
                players = req.players,
                spiceMax = req.spiceMax,
                localityMax = req.localityMax,
                inboundTexts = emptyList()
            ))
    }

    private suspend fun augment(t: TemplateV2, card: FilledCard, req: Request): FilledCard {
        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = t.max_words ?: 18,
            spice = t.spice,
            tags = t.tags,
            gameId = t.game,
            styleGuide = StyleGuides.getForGame(t.game)
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
        } catch (_: Exception) { }
    }
}
