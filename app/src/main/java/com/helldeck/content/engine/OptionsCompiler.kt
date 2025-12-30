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
    private val rng: SeededRng
) {
    
    fun compile(template: TemplateV2, card: FilledCard, players: List<String>): GameOptions {
        return when (val provider = template.options) {
            is OptionProvider.AB -> compileAB(provider)
            is OptionProvider.PlayerVote -> GameOptions.PlayerVote(players)
            is OptionProvider.Taboo -> compileTaboo(provider)
            is OptionProvider.Scatter -> compileScatter(provider)
            is OptionProvider.None -> compileFallback(template.game, card)
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
    
    private fun compileFallback(gameId: String, card: FilledCard): GameOptions {
        return when (gameId) {
            GameIds.TEXT_TRAP -> compileTextTrap()
            GameIds.POISON_PITCH -> compilePoisonPitch(card)
            // Legacy games removed: ODD_ONE, MAJORITY
            else -> GameOptions.None
        }
    }
    
    private fun compileTextTrap(): GameOptions {
        val tones = try {
            repo.wordsFor("reply_tones")
        } catch (_: Exception) {
            emptyList()
        }
        val options = tones.ifEmpty {
            listOf("Deadpan", "Feral", "Chaotic", "Wholesome", "Petty", "Thirsty")
        }
        return GameOptions.ReplyTone(options)
    }
    
    // Legacy game methods removed: compileOddOneOut, compileMajority
    
    private fun compilePoisonPitch(card: FilledCard): GameOptions {
        val seq = card.metadata["slot_sequence"] as? List<*>
        val gross = findSlotValue(seq, "gross") ?: repo.wordsFor("gross").random(rng.random)
        val social = findSlotValue(seq, "social_disaster") ?: repo.wordsFor("social_disasters").random(rng.random)
        return GameOptions.AB(gross, social)
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