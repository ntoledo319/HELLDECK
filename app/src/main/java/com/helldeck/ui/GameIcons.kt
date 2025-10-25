package com.helldeck.ui

import com.helldeck.engine.GameIds

fun gameIconFor(id: String): String = when (id) {
    GameIds.ROAST_CONS -> "🔥"
    GameIds.CONFESS_CAP -> "🕵️"
    GameIds.POISON_PITCH -> "⚖️"
    GameIds.FILLIN -> "✍️"
    GameIds.RED_FLAG -> "🚩"
    GameIds.HOTSEAT_IMP -> "🎭"
    GameIds.TEXT_TRAP -> "💬"
    GameIds.TABOO -> "⛔️"
    GameIds.ODD_ONE -> "🧩"
    GameIds.TITLE_FIGHT -> "👑"
    GameIds.ALIBI -> "🕶️"
    GameIds.HYPE_YIKE -> "📣"
    GameIds.SCATTER -> "🔤"
    GameIds.MAJORITY -> "📊"
    else -> "🎮"
}

