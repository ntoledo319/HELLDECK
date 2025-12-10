package com.helldeck.engine

// Games list now derives from the canonical GameRegistry (GameMetadata)
object Games {
    private val all: List<GameSpec>
        get() = GameRegistry.getAllGameIds().mapNotNull { GameRegistry.getGameById(it) }

    val size: Int get() = all.size
    operator fun get(index: Int): GameSpec = all[index]
    fun forEach(action: (GameSpec) -> Unit) = all.forEach(action)
}
