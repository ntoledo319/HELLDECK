package com.helldeck.content.engine

import com.helldeck.engine.GameIds

/**
 * Style guides for LLM augmentation per game type.
 * Extracted from GameEngine to reduce bloat and improve maintainability.
 */
object StyleGuides {
    
    private val guides = mapOf(
        GameIds.ROAST_CONS to "Roasty, playful, one sentence. Sounds like a group roast prompt. No direct harassment; keep it light.",
        GameIds.MAJORITY to "Neutral A/B phrasing for group vote. Balanced options; avoid leading language. Keep it short.",
        GameIds.POISON_PITCH to "Would-you-rather tone. Two balanced gross vs social disaster options; witty but clear.",
        GameIds.FILLIN to "Finish-the-sentence setup with a clear blank (____). Teases a punchline.",
        GameIds.RED_FLAG to "Dating scenario: present a perk vs red flag succinctly; ends with \"Smash or pass?\"",
        GameIds.HOTSEAT_IMP to "Answer-as-character vibe; instruct as if speaking as target. Keep it playful, not mean.",
        GameIds.TEXT_TRAP to "Text message scenario with quotes intact; prompt to pick a reply vibe.",
        GameIds.TABOO to "Taboo rules: one target word with 3 forbiddens. Keep formatting clear.",
        GameIds.ODD_ONE to "List three items, then ask which is the odd one out and why.",
        GameIds.TITLE_FIGHT to "Short, hype-y duel title; competitive energy, minimal words.",
        GameIds.ALIBI to "Story smuggling: clearly list the words to include in the alibi.",
        GameIds.HYPE_YIKE to "Deadpan product pitch vibe for a ridiculous problem; keep it straight-faced.",
        GameIds.SCATTER to "Category + letter; fast-paced. Make the task crystal clear and brief."
    )
    
    private const val DEFAULT_GUIDE = "Punchy, social party-game tone. Keep one sentence, clear task, no extra fluff."
    
    fun getForGame(gameId: String): String = guides[gameId] ?: DEFAULT_GUIDE
}