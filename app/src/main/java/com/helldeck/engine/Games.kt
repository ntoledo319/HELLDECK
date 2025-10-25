package com.helldeck.engine

// Minimal Games registry to satisfy UI references
object Games {
    private val all: List<GameSpec> = listOf(
        GameSpec(
            id = GameIds.ROAST_CONS,
            title = "Roast Consensus",
            interaction = Interaction.VOTE_AVATAR,
            timerSec = 10,
            description = "Vote who fits the prompt best.",
            minPlayers = 3,
            maxPlayers = 16,
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.MEDIUM
        ),
        GameSpec(
            id = GameIds.CONFESS_CAP,
            title = "Confession or Cap",
            interaction = Interaction.TRUE_FALSE,
            timerSec = 8,
            description = "Truth or bluff? Room guesses.",
            minPlayers = 3,
            maxPlayers = 16,
            category = GameCategory.MAIN,
            difficulty = GameDifficulty.EASY
        ),
        GameSpec(
            id = GameIds.TABOO,
            title = "Taboo Timer",
            interaction = Interaction.TABOO_CLUE,
            timerSec = 10,
            description = "Clue without forbidden words.",
            minPlayers = 3,
            maxPlayers = 16,
            category = GameCategory.WORD,
            difficulty = GameDifficulty.MEDIUM
        )
    )

    val size: Int get() = all.size
    operator fun get(index: Int): GameSpec = all[index]
    fun forEach(action: (GameSpec) -> Unit) = all.forEach(action)
}
