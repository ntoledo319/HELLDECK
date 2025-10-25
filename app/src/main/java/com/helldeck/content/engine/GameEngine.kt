package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.engine.augment.Augmentor
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.model.Template
import com.helldeck.content.model.v2.OptionProvider
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import com.helldeck.engine.GameMetadata
import com.helldeck.engine.InteractionType

class GameEngine(
    private val repo: ContentRepository,
    private val rng: SeededRng,
    private val selector: ContextualSelector,
    private val augmentor: Augmentor?,
    private val modelId: String
) {
    data class Request(
        val sessionId: String,
        val gameId: String? = null,
        val players: List<String> = emptyList(),
        val activePlayer: String? = null,
        val roomHeat: Double = 0.6,
        val spiceMax: Int = 3,
        val recentFamilies: List<String> = emptyList(),
        val avoidTemplateIds: Set<String> = emptySet(),
        val tagAffinity: Map<String, Double> = emptyMap()
    )

    data class Result(
        val filledCard: FilledCard,
        val options: GameOptions,
        val timer: Int,
        val interactionType: InteractionType
    )

    suspend fun next(req: Request): Result {
        val pool = repo.templatesV2()
        val ctx = ContextualSelector.Context(
            players = req.players,
            activePlayer = req.activePlayer,
            roomHeat = req.roomHeat,
            spiceMax = req.spiceMax,
            wantedGameId = req.gameId,
            recentFamilies = req.recentFamilies,
            avoidIds = req.avoidTemplateIds,
            tagAffinity = req.tagAffinity
        )
        val chosen = selector.pick(ctx, pool)
        val filled = fill(chosen, req)
        val augmented = augment(chosen, filled, req)
        val options = compileOptions(chosen, augmented)
        val timer = timerFor(chosen.game)
        val interaction = interactionFor(chosen.game)
        return Result(augmented, options, timer, interaction)
    }

    private fun fill(t: TemplateV2, req: Request): FilledCard {
        return TemplateEngine(repo, rng)
            .fill(t, TemplateEngine.Context(
                players = req.players,
                spiceMax = req.spiceMax,
                localityMax = 3,
                inboundTexts = emptyList()
            ))
    }

    private suspend fun augment(t: TemplateV2, card: FilledCard, req: Request): FilledCard {
        val plan = Augmentor.Plan(
            allowParaphrase = true,
            maxWords = t.max_words ?: 18,
            spice = t.spice,
            tags = t.tags
        )
        val seed = (t.id + card.text + req.sessionId).hashCode()
        return augmentor?.maybeParaphrase(card, plan, seed, modelId) ?: card
    }

    private fun compileOptions(t: TemplateV2, card: FilledCard): GameOptions {
        return when (val p = t.options) {
            is OptionProvider.AB -> {
                val aWords = repo.wordsFor(p.a.from)
                val a = aWords.random(rng.random)
                val bWords = repo.wordsFor(p.b.from)
                var b = bWords.random(rng.random)
                if (p.b.avoid_same_as == "a" && b.equals(a, true)) {
                    b = bWords.shuffled(rng.random).first { !it.equals(a, true) }
                }
                GameOptions.AB(a, b)
            }
            is OptionProvider.PlayerVote -> GameOptions.PlayerVote(emptyList())
            is OptionProvider.Taboo -> {
                val words = repo.wordsFor(p.wordFrom)
                val w = words.random(rng.random)
                val forbidden = repo.wordsFor(p.forbiddenFrom)
                val f = forbidden.shuffled(rng.random).distinct().take(p.count)
                GameOptions.Taboo(w, f)
            }
            is OptionProvider.Scatter -> {
                val categories = repo.wordsFor(p.categoryFrom)
                val cat = categories.random(rng.random)
                val letters = repo.wordsFor(p.letterFrom)
                val letter = letters.random(rng.random)
                GameOptions.Scatter(cat, letter)
            }
            is OptionProvider.None -> GameOptions.None
        }
    }

    // Expose options for an already-filled card by looking up its template
    fun getOptionsFor(card: FilledCard, req: Request): GameOptions {
        // Try to find the originating template by ID
        val template = repo.templatesV2().firstOrNull { it.id == card.id }
        return if (template != null) compileOptions(template, card) else GameOptions.None
    }

    private fun timerFor(gameId: String): Int {
        return GameMetadata.getGameMetadata(gameId)?.timerSec ?: 10
    }

    private fun interactionFor(gameId: String): InteractionType {
        return GameMetadata.getGameMetadata(gameId)?.interactionType ?: InteractionType.NONE
    }

    fun recordOutcome(templateId: String, reward01: Double) {
        selector.update(templateId, reward01.coerceIn(0.0, 1.0))
    }
}
