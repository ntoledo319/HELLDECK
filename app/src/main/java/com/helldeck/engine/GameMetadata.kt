package com.helldeck.engine

import kotlin.random.Random

object GameMetadata {
    // Maintain insertion order for stable UI presentation
    private val GameDefinitions: LinkedHashMap<String, GameInfo> = linkedMapOf(
        // 1. Roast Consensus 🎯
        GameIds.ROAST_CONS to GameInfo(
            id = GameIds.ROAST_CONS,
            title = "Roast Consensus",
            description = "A roast card appears. Everyone secretly picks one victim. 20 seconds to lock in. All votes drop simultaneously. Whoever got the most votes takes the heat.",
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 20,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.VOTE_AVATAR,
            interactionType = InteractionType.VOTE_PLAYER,
            tags = setOf("roast", "vote", "avatar"),
            spice = 2
        ),

        // 2. Confession or Cap 🤥
        GameIds.CONFESS_CAP to GameInfo(
            id = GameIds.CONFESS_CAP,
            title = "Confession or Cap",
            description = "One player receives a potentially embarrassing prompt. They answer TRUE or FALSE. Everyone else votes on whether they believe them. The truth is revealed.",
            category = GameCategory.MAIN,
            difficulty = GameDifficulty.EASY,
            timerSec = 15,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.TRUE_FALSE,
            interactionType = InteractionType.TRUE_FALSE,
            tags = setOf("confession", "true_false", "prepick"),
            spice = 1
        ),

        // 3. Poison Pitch 💀
        GameIds.POISON_PITCH to GameInfo(
            id = GameIds.POISON_PITCH,
            title = "Poison Pitch",
            description = "A 'Would You Rather' card with two horrifying options. One player defends Option A, another defends Option B. Each has 30 seconds to argue. The group votes for the most convincing pitch.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 30,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.AB_VOTE,
            interactionType = InteractionType.A_B_CHOICE,
            tags = setOf("wyr", "ab", "pitch"),
            spice = 2
        ),

        // 4. Fill-In Finisher ✍️
        GameIds.FILLIN to GameInfo(
            id = GameIds.FILLIN,
            title = "Fill-In Finisher",
            description = "The Judge draws a card with two blanks. They fill in the first blank verbally. All other players have 60 seconds to write their answer for the second blank. The Judge picks their favorite.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 60,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.JUDGE_PICK,
            interactionType = InteractionType.JUDGE_PICK,
            tags = setOf("fill_in", "judge", "write"),
            spice = 1
        ),

        // 5. Red Flag Rally 🚩
        GameIds.RED_FLAG to GameInfo(
            id = GameIds.RED_FLAG,
            title = "Red Flag Rally",
            description = "A dating card with a Perk and a Red Flag. One player defends this person and argues why they're still dateable. 45 seconds to make the case. Everyone votes SMASH or PASS.",
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 45,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.SMASH_PASS,
            interactionType = InteractionType.SMASH_PASS,
            tags = setOf("dating", "smash_pass"),
            spice = 2
        ),

        // 6. Hot Seat Imposter 🎭
        GameIds.HOTSEAT_IMP to GameInfo(
            id = GameIds.HOTSEAT_IMP,
            title = "Hot Seat Imposter",
            description = "One player is the Target. Another is secretly chosen to impersonate them. The group asks 3-5 personal questions. The Imposter answers as if they were the Target. The group votes: REAL or FAKE.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 15,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.JUDGE_PICK,
            interactionType = InteractionType.JUDGE_PICK,
            tags = setOf("imposter", "target", "judge"),
            spice = 1
        ),

        // 7. Text Thread Trap 📱
        GameIds.TEXT_TRAP to GameInfo(
            id = GameIds.TEXT_TRAP,
            title = "Text Thread Trap",
            description = "A card displays an awkward or high-stakes received text message. A Mandatory Tone is generated (1-22). The player must verbally improvise the text reply while acting out that specific Tone. The group votes on whether they survived.",
            category = GameCategory.MAIN,
            difficulty = GameDifficulty.EASY,
            timerSec = 15,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.REPLY_TONE,
            interactionType = InteractionType.REPLY_TONE,
            tags = setOf("text", "tone", "judge"),
            spice = 1
        ),

        // 8. Taboo Timer ⏱️
        GameIds.TABOO to GameInfo(
            id = GameIds.TABOO,
            title = "Taboo Timer",
            description = "The Clue-Giver draws a card with a target word and 3-5 forbidden words. 60 seconds on the timer. Describe the target word without using any forbidden words. Guessers shout out answers.",
            category = GameCategory.WORD,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 60,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.TABOO_CLUE,
            interactionType = InteractionType.TABOO_GUESS,
            tags = setOf("taboo", "team", "timer"),
            spice = 1
        ),

        // 9. The Unifying Theory 📐
        GameIds.UNIFYING_THEORY to GameInfo(
            id = GameIds.UNIFYING_THEORY,
            title = "The Unifying Theory",
            description = "A card reveals three completely unrelated items. The player must explain exactly why these three things are The Same. Find the single thread that connects all three. The group votes if your theory holds water.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 30,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.ODD_REASON,
            interactionType = InteractionType.ODD_EXPLAIN,
            tags = setOf("unifying", "explain", "judge"),
            spice = 1
        ),

        // 10. Title Fight 🥊
        GameIds.TITLE_FIGHT to GameInfo(
            id = GameIds.TITLE_FIGHT,
            title = "Title Fight",
            description = "Draw a card. Immediately point at another player and yell FIGHT! Read the challenge. You and that player compete instantly. The first person to mess up, pause, or quit is the Loser.",
            category = GameCategory.DUEL,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 15,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.DUEL,
            interactionType = InteractionType.MINI_DUEL,
            tags = setOf("duel", "mini_game", "judge"),
            spice = 1
        ),

        // 11. Alibi Drop 🕵️
        GameIds.ALIBI to GameInfo(
            id = GameIds.ALIBI,
            title = "Alibi Drop",
            description = "Draw a card that accuses you of a crime. The card lists 3 Mandatory Words. You have 30 seconds to explain yourself. You must weave all three words into your story naturally. The group acts as the Jury.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 30,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.SMUGGLE,
            interactionType = InteractionType.HIDE_WORDS,
            tags = setOf("smuggle", "story", "judge"),
            spice = 1
        ),

        // 12. Reality Check 🪞
        GameIds.REALITY_CHECK to GameInfo(
            id = GameIds.REALITY_CHECK,
            title = "Reality Check",
            description = "A brutal game of self-awareness. The Subject draws a card with a specific trait. They secretly write down a rating from 1 to 10. The group discusses and agrees on a single rating. Both numbers are revealed simultaneously.",
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 20,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.TARGET_PICK,
            interactionType = InteractionType.TARGET_SELECT,
            tags = setOf("rating", "self-awareness", "vote"),
            spice = 2
        ),

        // 13. Scatterblast 💣
        GameIds.SCATTER to GameInfo(
            id = GameIds.SCATTER,
            title = "Scatterblast",
            description = "A high-speed category elimination game. The phone acts as a bomb with a hidden timer. A card reveals a Category + Letter. Players take turns shouting valid answers. If the bomb explodes on your turn, you lose.",
            category = GameCategory.WORD,
            difficulty = GameDifficulty.EASY,
            timerSec = 10,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.SPEED_LIST,
            interactionType = InteractionType.SPEED_LIST,
            tags = setOf("scatter", "speed", "timer"),
            spice = 1
        ),

        // 14. Over / Under 📉
        GameIds.OVER_UNDER to GameInfo(
            id = GameIds.OVER_UNDER,
            title = "Over / Under",
            description = "A social betting game. One player is the Subject. A card asks for a number about them. The group sets a Betting Line. Everyone votes OVER or UNDER. The Subject immediately reveals the exact number.",
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.EASY,
            timerSec = 20,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.AB_VOTE,
            interactionType = InteractionType.PREDICT_VOTE,
            tags = setOf("betting", "numbers", "vote"),
            spice = 1
        )

        // NOTE: HYPE_YIKE, MAJORITY, and ODD_ONE are NOT in HDRealRules.md and are excluded from the 14 official games.
        // They may exist in legacy content but are not part of the official game collection.
    )

    fun getGameMetadata(gameId: String): GameInfo? = GameDefinitions[gameId]
    fun getAllGames(): List<GameInfo> = GameDefinitions.values.toList()
    fun getGamesByCategory(category: GameCategory): List<GameInfo> = getAllGames().filter { it.category == category }
    fun getGamesByDifficulty(difficulty: GameDifficulty): List<GameInfo> = getAllGames().filter { it.difficulty == difficulty }
    fun getGamesForPlayerCount(playerCount: Int): List<GameInfo> = getAllGames().filter { playerCount in it.minPlayers..it.maxPlayers }
    fun getGamesWithTags(tags: Set<String>): List<GameInfo> = getAllGames().filter { it.tags.any(tags::contains) }
    fun getRandomGame(playerCount: Int): GameInfo = (getGamesForPlayerCount(playerCount).ifEmpty { getAllGames() }).random(Random)
    fun getAllGameIds(): List<String> = GameDefinitions.keys.toList()

    fun getGameStats(): Map<String, Any> = mapOf(
        "count" to GameDefinitions.size,
        "categories" to getAllGames().groupBy { it.category }.mapValues { it.value.size },
        "difficulties" to getAllGames().groupBy { it.difficulty }.mapValues { it.value.size }
    )

    fun validateAllGames(): List<String> {
        val errors = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        GameDefinitions.values.forEach { g ->
            if (g.id.isBlank()) errors.add("Blank id")
            if (!ids.add(g.id)) errors.add("Duplicate id: ${g.id}")
            if (g.title.isBlank()) errors.add("Blank title for ${g.id}")
            if (g.timerSec < 0) errors.add("Negative timer for ${g.id}")
            if (g.minPlayers > g.maxPlayers) errors.add("minPlayers>maxPlayers for ${g.id}")
        }
        return errors
    }

    fun suggestGameProgression(currentGameId: String, playerCount: Int): List<GameInfo> {
        val current = getGameMetadata(currentGameId) ?: return getGamesForPlayerCount(playerCount)
        // Suggestion: rotate category, keep difficulty +/- one step
        val cats = GameCategory.values().toList()
        val nextCat = cats[(cats.indexOf(current.category) + 1) % cats.size]
        val pool = getGamesForPlayerCount(playerCount)
        return pool.filter { it.category == nextCat && it.difficulty.multiplier in (current.difficulty.multiplier - 0.2)..(current.difficulty.multiplier + 0.2) }
            .ifEmpty { pool.filter { it.category == nextCat } }
            .ifEmpty { pool }
    }
}

data class GameInfo(
    val id: String,
    val title: String,
    val description: String,
    val category: GameCategory,
    val difficulty: GameDifficulty,
    val timerSec: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val interaction: Interaction,
    val interactionType: InteractionType,
    val tags: Set<String>,
    val spice: Int
)
