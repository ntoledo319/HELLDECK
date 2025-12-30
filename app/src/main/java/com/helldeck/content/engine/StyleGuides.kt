package com.helldeck.content.engine

import com.helldeck.engine.GameIds

/**
 * Style guides for LLM augmentation per game type.
 * Extracted from GameEngine to reduce bloat and improve maintainability.
 */
object StyleGuides {
    
    private val guides = mapOf(
        GameIds.ROAST_CONS to "Roasty, playful, one sentence. Sounds like a group roast prompt. No direct harassment; keep it light.",
        
        GameIds.POISON_PITCH to "Would-you-rather tone. Two balanced gross vs social disaster options; witty but clear.",
        GameIds.FILLIN to "Finish-the-sentence setup with a clear blank (____). Teases a punchline.",
        GameIds.RED_FLAG to "Dating scenario: present a perk vs red flag succinctly; ends with \"Smash or pass?\"",
        GameIds.HOTSEAT_IMP to "Answer-as-character vibe; instruct as if speaking as target. Keep it playful, not mean.",
        GameIds.TEXT_TRAP to "Text message scenario with quotes intact; prompt to pick a reply vibe.",
        GameIds.TABOO to "Taboo rules: one target word with 3 forbiddens. Keep formatting clear.",
        
        GameIds.TITLE_FIGHT to "Short, hype-y duel title; competitive energy, minimal words.",
        GameIds.ALIBI to "Story smuggling: clearly list the words to include in the alibi.",
        
        GameIds.SCATTER to "Category + letter; fast-paced. Make the task crystal clear and brief.",
        GameIds.UNIFYING_THEORY to "List three unrelated items; ask to explain why they're the same.",
        GameIds.REALITY_CHECK to "Self-rating prompt: ask to rate a trait 1-10. Keep it specific and personal.",
        GameIds.OVER_UNDER to "Betting prompt: ask for a specific number about the subject. Make it verifiable."
    )
    
    private const val DEFAULT_GUIDE = "Punchy, social party-game tone. Keep one sentence, clear task, no extra fluff."
    
    fun getForGame(gameId: String): String = guides[gameId] ?: DEFAULT_GUIDE
}