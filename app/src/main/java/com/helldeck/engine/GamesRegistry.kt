package com.helldeck.engine

/**
 * Game ID constants for all HELLDECK mini-games
 */
object GameIds {
    const val ROAST_CONS = "ROAST_CONSENSUS"
    const val CONFESS_CAP = "CONFESSION_OR_CAP"
    const val POISON_PITCH = "POISON_PITCH"
    const val FILLIN = "FILL_IN_FINISHER"
    const val RED_FLAG = "RED_FLAG_RALLY"
    const val HOTSEAT_IMP = "HOT_SEAT_IMPOSTER"
    const val TEXT_TRAP = "TEXT_THREAD_TRAP"
    const val TABOO = "TABOO_TIMER"
    const val ODD_ONE = "ODD_ONE_OUT"
    const val TITLE_FIGHT = "TITLE_FIGHT"
    const val ALIBI = "ALIBI_DROP"
    const val HYPE_YIKE = "HYPE_OR_YIKE"
    const val SCATTER = "SCATTERBLAST"
    const val MAJORITY = "MAJORITY_REPORT"
}

/**
 * Interaction types for different game mechanics
 */
enum class Interaction(
    val description: String,
    val requiresTimer: Boolean = true,
    val supportsPreChoice: Boolean = false,
    val supportsOptions: Boolean = false
) {
    // Voting interactions
    VOTE_AVATAR("Vote for a player avatar", true, false, false),
    TRUE_FALSE("Choose between two options", true, false, true),
    AB_VOTE("Choose between A or B", true, true, true),
    JUDGE_PICK("Judge selects from options", true, false, true),

    // Special interactions
    SMASH_PASS("Choose to smash or pass", true, false, true),
    TARGET_PICK("Pick a target player", false, false, false),
    REPLY_TONE("Choose reply tone/style", true, false, true),
    TABOO_CLUE("Give clues without forbidden words", true, false, false),
    ODD_REASON("Explain why something is odd", true, false, false),

    // Duel interactions
    DUEL("Mini-game duel between players", true, false, false),
    SMUGGLE("Hide words in a story", true, false, false),
    PITCH("Give a sales pitch", true, false, false),
    SPEED_LIST("Quickly list items", true, false, false);

    companion object {
        fun fromGameId(gameId: String): Interaction {
            return when (gameId) {
                GameIds.ROAST_CONS -> VOTE_AVATAR
                GameIds.CONFESS_CAP -> TRUE_FALSE
                GameIds.POISON_PITCH -> AB_VOTE
                GameIds.FILLIN -> JUDGE_PICK
                GameIds.RED_FLAG -> SMASH_PASS
                GameIds.HOTSEAT_IMP -> TARGET_PICK
                GameIds.TEXT_TRAP -> REPLY_TONE
                GameIds.TABOO -> TABOO_CLUE
                GameIds.ODD_ONE -> ODD_REASON
                GameIds.TITLE_FIGHT -> DUEL
                GameIds.ALIBI -> SMUGGLE
                GameIds.HYPE_YIKE -> PITCH
                GameIds.SCATTER -> SPEED_LIST
                GameIds.MAJORITY -> AB_VOTE
                else -> JUDGE_PICK // Default fallback
            }
        }
    }
}

/**
 * Game specification data class
 */
data class GameSpec(
    val id: String,
    val title: String,
    val interaction: Interaction,
    val timerSec: Int = 15,
    val description: String = "",
    val minPlayers: Int = 3,
    val maxPlayers: Int = 16,
    val category: GameCategory = GameCategory.MAIN,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val tags: Set<String> = emptySet()
)

/**
 * Game categories for organization
 */
enum class GameCategory(val displayName: String) {
    MAIN("Main Games"),
    DUEL("Duel Games"),
    WORD("Word Games"),
    VOTING("Voting Games"),
    CREATIVE("Creative Games"),
    QUICK("Quick Games")
}

/**
 * Game difficulty levels
 */
enum class GameDifficulty(val displayName: String, val multiplier: Double) {
    EASY("Easy", 0.8),
    MEDIUM("Medium", 1.0),
    HARD("Hard", 1.2),
    EXPERT("Expert", 1.5)
}

/**
 * Registry of all available games
 */
val Games = listOf(
    GameSpec(
        id = GameIds.ROAST_CONS,
        title = "Roast Consensus",
        interaction = Interaction.VOTE_AVATAR,
        timerSec = 10,
        description = "Vote for the player most likely to do something embarrassing",
        category = GameCategory.VOTING,
        difficulty = GameDifficulty.EASY,
        tags = setOf("voting", "social", "roast")
    ),

    GameSpec(
        id = GameIds.CONFESS_CAP,
        title = "Confession or Cap",
        interaction = Interaction.TRUE_FALSE,
        timerSec = 8,
        description = "Speaker shares a prompt; room bets truth or lie",
        category = GameCategory.VOTING,
        difficulty = GameDifficulty.MEDIUM,
        tags = setOf("bluffing", "voting", "social")
    ),

    GameSpec(
        id = GameIds.POISON_PITCH,
        title = "Poison Pitch",
        interaction = Interaction.AB_VOTE,
        timerSec = 10,
        description = "Sell your side of a Would You Rather scenario",
        category = GameCategory.CREATIVE,
        difficulty = GameDifficulty.MEDIUM,
        tags = setOf("persuasion", "creative", "debate")
    ),

    GameSpec(
        id = GameIds.FILLIN,
        title = "Fill-In Finisher",
        interaction = Interaction.JUDGE_PICK,
        timerSec = 6,
        description = "Complete a prompt with the punchiest finisher",
        category = GameCategory.CREATIVE,
        difficulty = GameDifficulty.MEDIUM,
        tags = setOf("creative", "writing", "humor")
    ),

    GameSpec(
        id = GameIds.RED_FLAG,
        title = "Red Flag Rally",
        interaction = Interaction.SMASH_PASS,
        timerSec = 10,
        description = "Defend a dating scenario despite obvious red flags",
        category = GameCategory.CREATIVE,
        difficulty = GameDifficulty.HARD,
        tags = setOf("creative", "defense", "dating")
    ),

    GameSpec(
        id = GameIds.HOTSEAT_IMP,
        title = "Hot Seat Imposter",
        interaction = Interaction.TARGET_PICK,
        timerSec = 0,
        description = "Everyone answers as the target; target picks most believable",
        category = GameCategory.CREATIVE,
        difficulty = GameDifficulty.HARD,
        tags = setOf("impersonation", "creative", "social")
    ),

    GameSpec(
        id = GameIds.TEXT_TRAP,
        title = "Text Thread Trap",
        interaction = Interaction.REPLY_TONE,
        timerSec = 8,
        description = "Choose the perfect reply tone for a message",
        category = GameCategory.CREATIVE,
        difficulty = GameDifficulty.MEDIUM,
        tags = setOf("communication", "creative", "social")
    ),

    GameSpec(
        id = GameIds.TABOO,
        title = "Taboo Timer",
        interaction = Interaction.TABOO_CLUE,
        timerSec = 10,
        description = "Get team to guess word without saying forbidden terms",
        category = GameCategory.WORD,
        difficulty = GameDifficulty.HARD,
        tags = setOf("word", "team", "communication")
    ),

    GameSpec(
        id = GameIds.ODD_ONE,
        title = "Odd One Out",
        interaction = Interaction.ODD_REASON,
        timerSec = 10,
        description = "Pick the misfit from three options and explain why",
        category = GameCategory.WORD,
        difficulty = GameDifficulty.MEDIUM,
        tags = setOf("reasoning", "explanation", "logic")
    ),

    GameSpec(
        id = GameIds.TITLE_FIGHT,
        title = "Title Fight",
        interaction = Interaction.DUEL,
        timerSec = 15,
        description = "Mini-duel challenge against the current champion",
        category = GameCategory.DUEL,
        difficulty = GameDifficulty.HARD,
        tags = setOf("duel", "challenge", "skill")
    ),

    GameSpec(
        id = GameIds.ALIBI,
        title = "Alibi Drop",
        interaction = Interaction.SMUGGLE,
        timerSec = 15,
        description = "Hide secret words in an excuse without detection",
        category = GameCategory.CREATIVE,
        difficulty = GameDifficulty.EXPERT,
        tags = setOf("creative", "storytelling", "subtle")
    ),

    GameSpec(
        id = GameIds.HYPE_YIKE,
        title = "Hype or Yike",
        interaction = Interaction.PITCH,
        timerSec = 15,
        description = "Dead-serious pitch for an absurd product",
        category = GameCategory.CREATIVE,
        difficulty = GameDifficulty.HARD,
        tags = setOf("sales", "creative", "absurd")
    ),

    GameSpec(
        id = GameIds.SCATTER,
        title = "Scatterblast",
        interaction = Interaction.SPEED_LIST,
        timerSec = 10,
        description = "Name 3 things in a category starting with a letter",
        category = GameCategory.QUICK,
        difficulty = GameDifficulty.EASY,
        tags = setOf("quick", "word", "categories")
    ),

    GameSpec(
        id = GameIds.MAJORITY,
        title = "Majority Report",
        interaction = Interaction.AB_VOTE,
        timerSec = 8,
        description = "Predict how the room will vote before they do",
        category = GameCategory.VOTING,
        difficulty = GameDifficulty.MEDIUM,
        tags = setOf("prediction", "voting", "psychology")
    )
)

/**
 * Game registry utility functions
 */
object GameRegistry {

    /**
     * Get game spec by ID
     */
    fun getGameById(id: String): GameSpec? {
        return Games.find { it.id == id }
    }

    /**
     * Get all games for a category
     */
    fun getGamesByCategory(category: GameCategory): List<GameSpec> {
        return Games.filter { it.category == category }
    }

    /**
     * Get all games for a difficulty level
     */
    fun getGamesByDifficulty(difficulty: GameDifficulty): List<GameSpec> {
        return Games.filter { it.difficulty == difficulty }
    }

    /**
     * Get games suitable for player count
     */
    fun getGamesForPlayerCount(playerCount: Int): List<GameSpec> {
        return Games.filter { game ->
            playerCount >= game.minPlayers && playerCount <= game.maxPlayers
        }
    }

    /**
     * Get games with specific tags
     */
    fun getGamesWithTags(tags: Set<String>): List<GameSpec> {
        return Games.filter { game ->
            game.tags.intersect(tags).isNotEmpty()
        }
    }

    /**
     * Get random game suitable for player count
     */
    fun getRandomGame(playerCount: Int): GameSpec {
        val suitableGames = getGamesForPlayerCount(playerCount)
        return suitableGames.random()
    }

    /**
     * Get games that require specific interaction type
     */
    fun getGamesByInteraction(interaction: Interaction): List<GameSpec> {
        return Games.filter { it.interaction == interaction }
    }

    /**
     * Get all available game IDs
     */
    fun getAllGameIds(): List<String> {
        return Games.map { it.id }
    }

    /**
     * Get game statistics
     */
    fun getGameStats(): Map<String, Any> {
        return mapOf(
            "totalGames" to Games.size,
            "categories" to GameCategory.values().associateWith { category ->
                getGamesByCategory(category).size
            },
            "difficulties" to GameDifficulty.values().associateWith { difficulty ->
                getGamesByDifficulty(difficulty).size
            },
            "interactions" to Interaction.values().associateWith { interaction ->
                getGamesByInteraction(interaction).size
            }
        )
    }

    /**
     * Validate game configuration
     */
    fun validateGameConfiguration(): List<String> {
        val errors = mutableListOf<String>()

        Games.forEach { game ->
            if (game.timerSec < 0) {
                errors.add("Game ${game.id} has negative timer")
            }
            if (game.minPlayers > game.maxPlayers) {
                errors.add("Game ${game.id} has minPlayers > maxPlayers")
            }
            if (game.minPlayers < 2) {
                errors.add("Game ${game.id} has minPlayers < 2")
            }
        }

        return errors
    }

    /**
     * Get game progression suggestions
     */
    fun suggestGameProgression(currentGameId: String, playerCount: Int): List<GameSpec> {
        val currentGame = getGameById(currentGameId) ?: return emptyList()

        return Games.filter { game ->
            game.id != currentGameId &&
            playerCount >= game.minPlayers &&
            playerCount <= game.maxPlayers &&
            game.difficulty <= currentGame.difficulty // Don't suggest harder games
        }.take(3)
    }
}