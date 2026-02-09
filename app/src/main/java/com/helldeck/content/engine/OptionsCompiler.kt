package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.model.v2.OptionProvider
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.util.SeededRng
import com.helldeck.engine.GameIds

/**
 * Compiles game-specific options from templates and filled cards.
 * Implements strategy pattern to reduce GameEngine complexity.
 */
class OptionsCompiler(
    private val repo: ContentRepository,
    private val rng: SeededRng,
) {

    fun compile(template: TemplateV2, card: FilledCard, players: List<String>): GameOptions {
        return when (val provider = template.options) {
            is OptionProvider.AB -> compileAB(provider)
            is OptionProvider.PlayerVote -> GameOptions.SeatVote((1..players.size).toList())
            is OptionProvider.Taboo -> compileTaboo(provider)
            is OptionProvider.Scatter -> compileScatter(provider)
            is OptionProvider.None -> compileFallback(template.game, card, players)
        }
    }

    private fun compileAB(provider: OptionProvider.AB): GameOptions {
        val aWords = repo.wordsFor(provider.a.from)
        val a = aWords.random(rng.random)
        val bWords = repo.wordsFor(provider.b.from)
        var b = bWords.random(rng.random)

        if (provider.b.avoid_same_as == "a" && b.equals(a, true)) {
            b = bWords.shuffled(rng.random).first { !it.equals(a, true) }
        }

        return GameOptions.AB(a, b)
    }

    private fun compileTaboo(provider: OptionProvider.Taboo): GameOptions {
        val words = repo.wordsFor(provider.wordFrom)
        val word = words.random(rng.random)
        val forbidden = repo.wordsFor(provider.forbiddenFrom)
        val forbiddenList = forbidden.shuffled(rng.random).distinct().take(provider.count)
        return GameOptions.Taboo(word, forbiddenList)
    }

    private fun compileScatter(provider: OptionProvider.Scatter): GameOptions {
        val categories = repo.wordsFor(provider.categoryFrom)
        val category = categories.random(rng.random)
        val letters = repo.wordsFor(provider.letterFrom)
        val letter = letters.random(rng.random)
        return GameOptions.Scatter(category, letter)
    }

    private fun compileFallback(gameId: String, card: FilledCard, players: List<String>): GameOptions {
        val seatCount = if (players.size >= 2) players.size else 2
        return when (gameId) {
            GameIds.ROAST_CONS -> GameOptions.SeatVote((1..seatCount).toList())
            GameIds.CONFESS_CAP -> GameOptions.TrueFalse
            GameIds.RED_FLAG -> GameOptions.AB("SMASH", "PASS")
            GameIds.TEXT_TRAP -> compileTextTrap()
            GameIds.POISON_PITCH -> compilePoisonPitch(card)
            GameIds.TABOO -> GameOptions.Taboo("Secret word", listOf("Forbidden 1", "Forbidden 2", "Forbidden 3"))
            GameIds.UNIFYING_THEORY,
            GameIds.TITLE_FIGHT,
            GameIds.HOTSEAT_IMP -> GameOptions.Challenge("Freestyle")
            GameIds.REALITY_CHECK -> GameOptions.SeatSelect((1..seatCount).toList(), null)
            GameIds.ALIBI -> GameOptions.HiddenWords(listOf("alibi one", "alibi two", "alibi three"))
            GameIds.SCATTER -> GameOptions.Scatter("Category", "A")
            GameIds.OVER_UNDER -> GameOptions.AB("Over", "Under")
            // Legacy games removed: ODD_ONE, MAJORITY
            else -> GameOptions.None
        }
    }

    private fun compileTextTrap(): GameOptions {
        val tones = try {
            repo.wordsFor("mandatory_tones")
        } catch (_: Exception) {
            emptyList()
        }
        val options = tones.ifEmpty {
            listOf(
                "The Seductive Whisper", "The Raging Karen", "The 1920s News Anchor",
                "The Malfunctioning Robot", "The Noir Detective", "The Weeping Soap Star",
                "The Aggressive Drill Sergeant", "The Stoned Philosopher",
                "The Paralyzingly Shy", "The Used Car Salesman",
                "The Shakespearean Actor", "The Conspiracy Theorist",
                "The Valley Girl", "The Medieval Peasant", "The Bond Villain",
                "The Manic Squirrel", "The Dying Breath", "The Condescending IT Guy",
                "The Possessed", "The Confused Time Traveler",
                "The Golf Commentator", "The Frantic 911 Caller",
            )
        }
        // Shuffle and pick 4 tone options for the player to choose from
        return GameOptions.ReplyTone(options.shuffled(rng.random).take(4))
    }

    // Legacy game methods removed: compileOddOneOut, compileMajority

    private fun compilePoisonPitch(card: FilledCard): GameOptions {
        val seq = card.metadata["slot_sequence"] as? List<*>
        // Try multiple slot name patterns from both base and enhanced templates
        val optionA = findSlotValue(seq, "cost_a")
            ?: findSlotValue(seq, "gross")
            ?: findSlotValue(seq, "vice_a")
            ?: findSlotValue(seq, "bodily")
            ?: findSlotValue(seq, "selfish")
            ?: findSlotValue(seq, "innuendo")
            ?: repo.wordsFor("would_you_rather_costs").random(rng.random)
        val optionB = findSlotValue(seq, "cost_b")
            ?: findSlotValue(seq, "cost")
            ?: findSlotValue(seq, "red_flag")
            ?: findSlotValue(seq, "taboo")
            ?: findSlotValue(seq, "reason")
            ?: repo.wordsFor("gross_problem").random(rng.random)
        return GameOptions.AB(optionA, optionB)
    }

    private fun extractSlotValues(card: FilledCard): List<String> {
        val seq = card.metadata["slot_sequence"] as? List<*> ?: return emptyList()
        return seq.mapNotNull { item ->
            when (item) {
                is Pair<*, *> -> item.second as? String
                is Map<*, *> -> item.values.firstOrNull() as? String
                else -> null
            }
        }.filterNot { it.isBlank() }
    }

    private fun findSlotValue(seq: List<*>?, slotName: String): String? {
        return seq?.mapNotNull { item ->
            when (item) {
                is Pair<*, *> -> if (item.first == slotName) item.second as? String else null
                is Map<*, *> -> if (item.keys.firstOrNull() == slotName) item.values.firstOrNull() as? String else null
                else -> null
            }
        }?.lastOrNull()
    }

    private fun findSlotValues(seq: List<*>?, slotName: String): List<String> {
        return seq?.mapNotNull { item ->
            when (item) {
                is Pair<*, *> -> if (item.first == slotName) item.second as? String else null
                is Map<*, *> -> if (item.keys.firstOrNull() == slotName) item.values.firstOrNull() as? String else null
                else -> null
            }
        } ?: emptyList()
    }
}
