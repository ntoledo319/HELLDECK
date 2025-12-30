package com.helldeck.engine

/**
 * Game ID constants for all HELLDECK games
 */
object GameIds {
    const val ROAST_CONS = "ROAST_CONSENSUS"
    const val CONFESS_CAP = "CONFESSION_OR_CAP"
    const val POISON_PITCH = "POISON_PITCH"
    const val FILLIN = "FILL_IN_FINISHER"

    // Back-compat alias for older callers
    const val FILL_IN = FILLIN
    const val RED_FLAG = "RED_FLAG_RALLY"
    const val HOTSEAT_IMP = "HOT_SEAT_IMPOSTER"
    const val TEXT_TRAP = "TEXT_THREAD_TRAP"
    const val TABOO = "TABOO_TIMER"
    const val TITLE_FIGHT = "TITLE_FIGHT"
    const val ALIBI = "ALIBI_DROP"
    const val SCATTER = "SCATTERBLAST"
    const val UNIFYING_THEORY = "THE_UNIFYING_THEORY"
    const val REALITY_CHECK = "REALITY_CHECK"
    const val OVER_UNDER = "OVER_UNDER"

    // REMOVED - Not part of official 14 games per HDRealRules.md:
    // MAJORITY_REPORT, HYPE_OR_YIKE, ODD_ONE_OUT are legacy and excluded
}

/**
 * Interaction types for different game mechanics
 */
enum class Interaction(
    val description: String,
    val requiresTimer: Boolean = true,
    val supportsPreChoice: Boolean = false,
    val supportsOptions: Boolean = false,
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
    DUEL("Quick duel between players", true, false, false),
    SMUGGLE("Hide words in a story", true, false, false),
    PITCH("Give a sales pitch", true, false, false),
    SPEED_LIST("Quickly list items", true, false, false),
    ;

    companion object {
        fun fromGameId(gameId: String): Interaction {
            return GameMetadata.getGameMetadata(gameId)?.interaction ?: JUDGE_PICK
        }
    }
}

/**
 * Interaction types for UI/UX handling
 */
enum class InteractionType {
    NONE,
    VOTE_PLAYER,
    TRUE_FALSE,
    A_B_CHOICE,
    JUDGE_PICK,
    SMASH_PASS,
    TARGET_SELECT,
    REPLY_TONE,
    TABOO_GUESS,
    ODD_EXPLAIN,
    MINI_DUEL,
    HIDE_WORDS,
    SALES_PITCH,
    SPEED_LIST,
    PREDICT_VOTE,
}

/**
 * Game categories for organization
 */
enum class GameCategory(val displayName: String) {
    MAIN("Main Games"),
    DUEL("Duel Games"),
    WORD("Word Games"),
    VOTING("Voting Games"),
    CREATIVE("Creative Games"),
    QUICK("Quick Games"),
}

/**
 * Game difficulty levels
 */
enum class GameDifficulty(val displayName: String, val multiplier: Double) {
    EASY("Easy", 0.8),
    MEDIUM("Medium", 1.0),
    HARD("Hard", 1.2),
    EXPERT("Expert", 1.5),
}

/**
 * Registry of all available games - now using unified GameMetadata
 */
object GameRegistry {

    /**
     * Get game spec by ID
     */
    fun getGameById(id: String): GameSpec? {
        val metadata = GameMetadata.getGameMetadata(id) ?: return null
        return GameSpec(
            id = metadata.id,
            title = metadata.title,
            interaction = metadata.interaction,
            timerSec = metadata.timerSec,
            description = metadata.description,
            minPlayers = metadata.minPlayers,
            maxPlayers = metadata.maxPlayers,
            category = metadata.category,
            difficulty = metadata.difficulty,
            tags = metadata.tags,
        )
    }

    /**
     * Get all games for a category
     */
    fun getGamesByCategory(category: GameCategory): List<GameSpec> {
        return GameMetadata.getGamesByCategory(category).map { metadata ->
            GameSpec(
                id = metadata.id,
                title = metadata.title,
                interaction = metadata.interaction,
                timerSec = metadata.timerSec,
                description = metadata.description,
                minPlayers = metadata.minPlayers,
                maxPlayers = metadata.maxPlayers,
                category = metadata.category,
                difficulty = metadata.difficulty,
                tags = metadata.tags,
            )
        }
    }

    /**
     * Get all games for a difficulty level
     */
    fun getGamesByDifficulty(difficulty: GameDifficulty): List<GameSpec> {
        return GameMetadata.getGamesByDifficulty(difficulty).map { metadata ->
            GameSpec(
                id = metadata.id,
                title = metadata.title,
                interaction = metadata.interaction,
                timerSec = metadata.timerSec,
                description = metadata.description,
                minPlayers = metadata.minPlayers,
                maxPlayers = metadata.maxPlayers,
                category = metadata.category,
                difficulty = metadata.difficulty,
                tags = metadata.tags,
            )
        }
    }

    /**
     * Get games suitable for player count
     */
    fun getGamesForPlayerCount(playerCount: Int): List<GameSpec> {
        return GameMetadata.getGamesForPlayerCount(playerCount).map { metadata ->
            GameSpec(
                id = metadata.id,
                title = metadata.title,
                interaction = metadata.interaction,
                timerSec = metadata.timerSec,
                description = metadata.description,
                minPlayers = metadata.minPlayers,
                maxPlayers = metadata.maxPlayers,
                category = metadata.category,
                difficulty = metadata.difficulty,
                tags = metadata.tags,
            )
        }
    }

    /**
     * Get games with specific tags
     */
    fun getGamesWithTags(tags: Set<String>): List<GameSpec> {
        return GameMetadata.getGamesWithTags(tags).map { metadata ->
            GameSpec(
                id = metadata.id,
                title = metadata.title,
                interaction = metadata.interaction,
                timerSec = metadata.timerSec,
                description = metadata.description,
                minPlayers = metadata.minPlayers,
                maxPlayers = metadata.maxPlayers,
                category = metadata.category,
                difficulty = metadata.difficulty,
                tags = metadata.tags,
            )
        }
    }

    /**
     * Get random game suitable for player count
     */
    fun getRandomGame(playerCount: Int): GameSpec {
        val metadata = GameMetadata.getRandomGame(playerCount)
        return GameSpec(
            id = metadata.id,
            title = metadata.title,
            interaction = metadata.interaction,
            timerSec = metadata.timerSec,
            description = metadata.description,
            minPlayers = metadata.minPlayers,
            maxPlayers = metadata.maxPlayers,
            category = metadata.category,
            difficulty = metadata.difficulty,
            tags = metadata.tags,
        )
    }

    /**
     * Get games that require specific interaction type
     */
    fun getGamesByInteraction(interaction: Interaction): List<GameSpec> {
        return GameMetadata.getAllGames().filter { it.interaction == interaction }.map { metadata ->
            GameSpec(
                id = metadata.id,
                title = metadata.title,
                interaction = metadata.interaction,
                timerSec = metadata.timerSec,
                description = metadata.description,
                minPlayers = metadata.minPlayers,
                maxPlayers = metadata.maxPlayers,
                category = metadata.category,
                difficulty = metadata.difficulty,
                tags = metadata.tags,
            )
        }
    }

    /**
     * Get all available game IDs
     */
    fun getAllGameIds(): List<String> {
        return GameMetadata.getAllGameIds()
    }

    /**
     * Get game statistics
     */
    fun getGameStats(): Map<String, Any> {
        return GameMetadata.getGameStats()
    }

    /**
     * Validate game configuration
     */
    fun validateGameConfiguration(): List<String> {
        return GameMetadata.validateAllGames()
    }

    /**
     * Get game progression suggestions
     */
    fun suggestGameProgression(currentGameId: String, playerCount: Int): List<GameSpec> {
        return GameMetadata.suggestGameProgression(currentGameId, playerCount).map { metadata ->
            GameSpec(
                id = metadata.id,
                title = metadata.title,
                interaction = metadata.interaction,
                timerSec = metadata.timerSec,
                description = metadata.description,
                minPlayers = metadata.minPlayers,
                maxPlayers = metadata.maxPlayers,
                category = metadata.category,
                difficulty = metadata.difficulty,
                tags = metadata.tags,
            )
        }
    }
}

/**
 * Game specification data class - maintained for backward compatibility
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
    val tags: Set<String> = emptySet(),
)
