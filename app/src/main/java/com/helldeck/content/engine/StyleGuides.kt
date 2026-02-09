package com.helldeck.content.engine

import com.helldeck.engine.GameIds

/**
 * Style guides for LLM augmentation per game type.
 * Extracted from GameEngine to reduce bloat and improve maintainability.
 */
object StyleGuides {

    private val guides = mapOf(
        GameIds.ROAST_CONS to "Format: 'Who would [SPECIFIC VISUAL SCENARIO]?' Roast BEHAVIOR not people. Use vivid details: time of day, location, specific objects. Avoid generic ('be late') or appearance attacks.",

        GameIds.POISON_PITCH to "Format: 'Would you rather [BAD OPTION A] or [BAD OPTION B]?' Both options equally terrible. Be specific and visceral. Never pair a perk with a problem — both must be bad.",
        GameIds.FILL_IN to "Finish-the-sentence with _____ blank at punchline position. Setup creates expectation, blank allows surprise. Multiple funny answers must be possible.",
        GameIds.RED_FLAG to "Format: 'They're [GREEN FLAG], but [RED FLAG].' Green flag genuinely tempting, red flag dealbreaker-absurd. Never use actual abuse/crime. Use 'but' as separator.",
        GameIds.HOTSEAT_IMP to "Personal question that trips up fakers but is obvious to real friends. Must end with '?'. Avoid yes/no, too common, or too traumatic questions.",
        GameIds.TEXT_TRAP to "Format: '[Sender] texts: \"[anxiety-inducing message]\"'. Create tension through the text itself. Player must reply in a mandatory character tone.",
        GameIds.TABOO to "One common target word with 3 forbidden words that block the most obvious clues. Word must be well-known. Forbidden words = direct synonyms + category words.",

        GameIds.TITLE_FIGHT to "Format: 'Who would win: [ABSURD THING A] vs [ABSURD THING B]?' Both sides must be debatable. Use scale mismatches, category clashes, or absurd specificity.",
        GameIds.ALIBI to "Three COMPLETELY UNRELATED specific words to sneak into a story. Use concrete nouns from different categories: an animal, a brand, a location. Never use vague words.",

        GameIds.SCATTER to "Creative/absurd category (not generic trivia). Format: 'Things that would [X]' or 'Reasons [Y]'. Category itself should be amusing. Pair with a random letter.",
        GameIds.UNIFYING_THEORY to "Three items from DIFFERENT categories (profession, animal, object, relative) with a surprising hidden connection. No obvious link at first glance.",
        GameIds.REALITY_CHECK to "Format: 'Rate: [TRAIT PEOPLE OVERESTIMATE]'. Target Dunning-Kruger goldmine traits: humor, intelligence, driving, social skills. Subjective, ego-linked, observable.",
        GameIds.OVER_UNDER to "Format: 'Number of [REVEALING QUANTITY]'. Must be verifiable (phone check, memory). The number should reveal personality. Avoid same-for-everyone questions.",
        GameIds.CONFESS_CAP to "Format: 'I once [SPECIFIC BORDERLINE-BELIEVABLE CONFESSION]'. First-person, Goldilocks zone of believability. Use specific locations, objects, consequences.",
    )

    private const val DEFAULT_GUIDE = "Punchy, social party-game tone. Keep one sentence, clear task, no extra fluff."

    fun getForGame(gameId: String): String = guides[gameId] ?: DEFAULT_GUIDE
}
