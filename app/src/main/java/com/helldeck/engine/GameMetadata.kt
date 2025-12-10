package com.helldeck.engine

import kotlin.random.Random

object GameMetadata {
    // Maintain insertion order for stable UI presentation
    private val GameDefinitions: LinkedHashMap<String, GameInfo> = linkedMapOf(
        // 1. Roast Consensus
        GameIds.ROAST_CONS to GameInfo(
            id = GameIds.ROAST_CONS,
            title = "Roast Consensus",
            description = "Room votes which player fits the roast prompt best.",
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 8,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.VOTE_AVATAR,
            interactionType = InteractionType.VOTE_PLAYER,
            tags = setOf("roast", "vote", "avatar"),
            spice = 2
        ),

        // 2. Confession or Cap
        GameIds.CONFESS_CAP to GameInfo(
            id = GameIds.CONFESS_CAP,
            title = "Confession or Cap",
            description = "Speaker sets TRUTH/BLUFF; room votes T/F; match majority for points.",
            category = GameCategory.MAIN,
            difficulty = GameDifficulty.EASY,
            timerSec = 6,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.TRUE_FALSE,
            interactionType = InteractionType.TRUE_FALSE,
            tags = setOf("confession", "true_false", "prepick"),
            spice = 1
        ),

        // 3. Poison Pitch
        GameIds.POISON_PITCH to GameInfo(
            id = GameIds.POISON_PITCH,
            title = "Poison Pitch",
            description = "Would you rather A or B? Active pre-picks; pitch to sway the room.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 6,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.AB_VOTE,
            interactionType = InteractionType.A_B_CHOICE,
            tags = setOf("wyr", "ab", "pitch"),
            spice = 2
        ),

        // 4. Fill-In Finisher
        GameIds.FILLIN to GameInfo(
            id = GameIds.FILLIN,
            title = "Fill-In Finisher",
            description = "Complete the prompt with the funniest punchline; judge picks the best.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 4,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.JUDGE_PICK,
            interactionType = InteractionType.JUDGE_PICK,
            tags = setOf("fill_in", "judge"),
            spice = 1
        ),

        // 5. Red Flag Rally
        GameIds.RED_FLAG to GameInfo(
            id = GameIds.RED_FLAG,
            title = "Red Flag Rally",
            description = "Perk vs red flag; room votes SMASH or PASS; majority SMASH rewards.",
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 6,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.SMASH_PASS,
            interactionType = InteractionType.SMASH_PASS,
            tags = setOf("dating", "smash_pass"),
            spice = 2
        ),

        // 6. Hot Seat Imposter
        GameIds.HOTSEAT_IMP to GameInfo(
            id = GameIds.HOTSEAT_IMP,
            title = "Hot Seat Imposter",
            description = "Answer as the target player; judge picks best impersonation.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 6,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.JUDGE_PICK,
            interactionType = InteractionType.JUDGE_PICK,
            tags = setOf("imposter", "target", "judge"),
            spice = 1
        ),

        // 7. Text Thread Trap
        GameIds.TEXT_TRAP to GameInfo(
            id = GameIds.TEXT_TRAP,
            title = "Text Thread Trap",
            description = "Pick the perfect reply vibe to an incoming text.",
            category = GameCategory.MAIN,
            difficulty = GameDifficulty.EASY,
            timerSec = 6,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.REPLY_TONE,
            interactionType = InteractionType.REPLY_TONE,
            tags = setOf("text", "tone", "judge"),
            spice = 1
        ),

        // 8. Taboo Timer
        GameIds.TABOO to GameInfo(
            id = GameIds.TABOO,
            title = "Taboo Timer",
            description = "Give clues so your team guesses the word; avoid forbidden terms.",
            category = GameCategory.WORD,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 8,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.TABOO_CLUE,
            interactionType = InteractionType.TABOO_GUESS,
            tags = setOf("taboo", "team", "timer"),
            spice = 1
        ),

        // 9. Odd One Out
        GameIds.ODD_ONE to GameInfo(
            id = GameIds.ODD_ONE,
            title = "Odd One Out",
            description = "From three options, choose the misfit and explain why.",
            category = GameCategory.MAIN,
            difficulty = GameDifficulty.EASY,
            timerSec = 8,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.ODD_REASON,
            interactionType = InteractionType.ODD_EXPLAIN,
            tags = setOf("odd_one", "explain", "judge"),
            spice = 1
        ),

        // 10. Title Fight
        GameIds.TITLE_FIGHT to GameInfo(
            id = GameIds.TITLE_FIGHT,
            title = "Title Fight",
            description = "Quick duel mini-challenge; pick who won to keep or steal the crown.",
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

        // 11. Alibi Drop
        GameIds.ALIBI to GameInfo(
            id = GameIds.ALIBI,
            title = "Alibi Drop",
            description = "Weave secret words into an alibi without detection.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 3,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.SMUGGLE,
            interactionType = InteractionType.HIDE_WORDS,
            tags = setOf("smuggle", "story", "judge"),
            spice = 1
        ),

        // 12. Hype or Yike
        GameIds.HYPE_YIKE to GameInfo(
            id = GameIds.HYPE_YIKE,
            title = "Hype or Yike",
            description = "Pitch a straight-faced product for a ridiculous problem; lock when done.",
            category = GameCategory.CREATIVE,
            difficulty = GameDifficulty.MEDIUM,
            timerSec = 15,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.PITCH,
            interactionType = InteractionType.SALES_PITCH,
            tags = setOf("pitch", "sell", "vote"),
            spice = 1
        ),

        // 13. Scatterblast
        GameIds.SCATTER to GameInfo(
            id = GameIds.SCATTER,
            title = "Scatterblast",
            description = "Given a category and letter, say three valid items fast.",
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

        // 14. Majority Report
        GameIds.MAJORITY to GameInfo(
            id = GameIds.MAJORITY,
            title = "Majority Report",
            description = "Predict the room’s A vs B before they vote; earn if you read the room.",
            category = GameCategory.VOTING,
            difficulty = GameDifficulty.EASY,
            timerSec = 6,
            minPlayers = 3,
            maxPlayers = 16,
            interaction = Interaction.AB_VOTE,
            interactionType = InteractionType.PREDICT_VOTE,
            tags = setOf("predict", "ab", "vote"),
            spice = 1
        )
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
