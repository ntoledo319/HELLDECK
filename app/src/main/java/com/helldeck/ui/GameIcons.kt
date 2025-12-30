package com.helldeck.ui

import com.helldeck.engine.GameIds

/**
 * Game icon mappings for the 14 official HELLDECK games
 * Based on HDRealRules.md
 */
fun gameIconFor(id: String): String = when (id) {
    GameIds.ROAST_CONS -> "🎯" // 1. Roast Consensus
    GameIds.CONFESS_CAP -> "🤥" // 2. Confession or Cap
    GameIds.POISON_PITCH -> "💀" // 3. Poison Pitch
    GameIds.FILLIN -> "✍️" // 4. Fill-In Finisher
    GameIds.RED_FLAG -> "🚩" // 5. Red Flag Rally
    GameIds.HOTSEAT_IMP -> "🎭" // 6. Hot Seat Imposter
    GameIds.TEXT_TRAP -> "📱" // 7. Text Thread Trap
    GameIds.TABOO -> "⏱️" // 8. Taboo Timer
    GameIds.UNIFYING_THEORY -> "📐" // 9. The Unifying Theory
    GameIds.TITLE_FIGHT -> "🥊" // 10. Title Fight
    GameIds.ALIBI -> "🕵️" // 11. Alibi Drop
    GameIds.REALITY_CHECK -> "🪞" // 12. Reality Check
    GameIds.SCATTER -> "💣" // 13. Scatterblast
    GameIds.OVER_UNDER -> "📉" // 14. Over / Under
    else -> "🎮"
}
