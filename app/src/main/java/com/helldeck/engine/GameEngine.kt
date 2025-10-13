package com.helldeck.engine

import kotlinx.coroutines.flow.first
import android.content.Context
import com.google.gson.Gson
import com.helldeck.AppCtx
import com.helldeck.data.*
import com.helldeck.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * Main game engine for HELLDECK
 * Orchestrates game flow, template selection, and learning
 */
class GameEngine(
    private val ctx: Context,
    internal val repo: Repository,
    private val templateEngine: TemplateEngine
) {

    private val gson = Gson()
    private val recentFamilies: ArrayDeque<String> = ArrayDeque()
    private val slotProvider = DefaultSlotProvider(ctx)

    var roundIdx: Int = 0
        private set

    var currentSessionId: Long? = null
        private set

    /**
     * Initialize the game engine
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            repo.initialize()
            currentSessionId = repo.startNewSession()
        }
    }

    /**
     * Generate next filled card for a game
     */
    suspend fun nextFilledCard(gameId: String): FilledCard {
        return try {
            withContext(Dispatchers.IO) {
                Logger.d("Generating card for game: $gameId, round: $roundIdx")

                val tdao = repo.db.templates()
                val candidates = tdao.getByGame(gameId).first()

                if (candidates.isEmpty()) {
                    Logger.w("No templates available for game: $gameId")
                    throw IllegalStateException("No templates available for game: $gameId")
                }

                val selected = Selection.pickNext(candidates, recentFamilies.toList(), roundIdx)
                val filled = templateEngine.fill(
                    TemplateDef(
                        selected.id,
                        selected.game,
                        selected.text,
                        selected.family,
                        selected.spice,
                        selected.locality,
                        selected.maxWords
                    ),
                    slotProvider::provideSlot
                )

                rememberFamily(selected.family)

                FilledCard(
                    templateId = selected.id,
                    game = selected.game,
                    text = filled,
                    options = getOptionsFor(gameId, filled),
                    meta = mapOf(
                        "family" to selected.family,
                        "spice" to selected.spice.toString(),
                        "locality" to selected.locality.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Logger.e("Failed to generate card for game: $gameId", e)
            throw e
        }
    }

    /**
     * Commit round results and update learning
     */
    suspend fun commitRound(
        card: FilledCard,
        feedback: Feedback,
        judgeWin: Boolean,
        points: Int,
        latencyMs: Int,
        notes: String? = null
    ): RoundResult {
        return withContext(Dispatchers.IO) {
            val activePlayers = repo.getActivePlayerCount()
            val roomHeat = Config.isRoomHeat(feedback.lol, activePlayers)
            val roomTrash = Config.isRoomTrash(feedback.trash, activePlayers)

            // Calculate final score
            val streakBonus = calculateStreakBonus()
            val fastLaugh = feedback.latencyMs < 1200 && feedback.lol > 0
            val roundScore = Learning.scoreCard(
                lol = if (roomHeat) 1 else 0,
                trash = if (roomTrash) 1 else 0,
                judgeWin = judgeWin,
                fastLaugh = fastLaugh,
                streakBonus = streakBonus,
                roomHeat = roomHeat,
                roomTrash = roomTrash
            )

            // Record the round
            val roundId = repo.recordRound(
                game = card.game,
                templateId = card.templateId,
                fillsJson = gson.toJson(card.meta),
                lol = feedback.lol,
                meh = feedback.meh,
                trash = feedback.trash,
                judgeWin = judgeWin,
                points = points,
                latencyMs = latencyMs,
                notes = notes,
                playerCount = activePlayers,
                roomHeat = if (roomHeat) 1.0 else 0.0
            )

            // Add comments if present
            if (notes?.isNotBlank() == true || feedback.tags.isNotEmpty()) {
                val tagsString = feedback.tags.joinToString(",")
                repo.addComment(roundId, notes ?: "", tagsString)
            }

            // Update template learning
            updateTemplateLearning(card, roundScore, judgeWin, roomHeat)

            // Update session
            currentSessionId?.let { sessionId ->
                // In a real implementation, you'd track rounds per session
            }

            roundIdx++

            RoundResult(
                points = points,
                judgeWin = if (judgeWin) 1 else 0,
                roundScore = roundScore,
                roomHeat = roomHeat,
                roomTrash = roomTrash,
                streakBonus = streakBonus
            )
        }
    }

    /**
     * Get options for specific game types
     */
    private fun getOptionsFor(gameId: String, filledText: String): List<String> {
        return when (gameId) {
            GameIds.POISON_PITCH, GameIds.MAJORITY -> listOf("A", "B")
            GameIds.TEXT_TRAP -> listOf("Deadpan", "Feral", "Chaotic", "Wholesome")
            GameIds.TABOO -> extractTabooWords(filledText)
            GameIds.ODD_ONE -> listOf("Option 1", "Option 2", "Option 3")
            else -> emptyList()
        }
    }

    /**
     * Extract taboo words from filled text
     */
    private fun extractTabooWords(text: String): List<String> {
        // Simple extraction - in practice, this would be more sophisticated
        val words = text.split(Regex("\\s+"))
        return words.filter { item -> item.length > 3 }.take(3)
    }

    /**
     * Remember template family for diversity
     */
    private fun rememberFamily(family: String) {
        recentFamilies.addFirst(family)
        while (recentFamilies?.size ?: 0 > Config.current.learning.diversity_window) {
            recentFamilies.removeLast()
        }
    }

    /**
     * Update template learning based on round results
     */
    private suspend fun updateTemplateLearning(
        card: FilledCard,
        roundScore: Double,
        judgeWin: Boolean,
        roomHeat: Boolean
    ) {
        val tdao = repo.db.templates()
        val template = tdao.getByGame(card.game).first().firstOrNull { it.id == card.templateId }

        if (template != null) {
            val updated = Learning.updateTemplateAfterRound(template, roundScore, judgeWin, roomHeat)
            tdao.update(updated)
        }
    }

    /**
     * Calculate current streak bonus
     */
    private suspend fun calculateStreakBonus(): Int {
        // Get recent rounds for this game
        val recentRounds = repo.db.rounds().getLastRounds(5)
        var streak = 0

        if (recentRounds != null && recentRounds.isNotEmpty()) {
            for (i in (recentRounds.size - 1) downTo 0) {
                val round = recentRounds[i]
                if (round.points > 0) {
                    streak++
                } else {
                    break
                }
            }
        }

        return minOf(streak, Config.current.scoring.streak_cap)
    }

    /**
     * Get game statistics
     */
    suspend fun getGameStats(): Map<String, Any?> {
        return withContext(Dispatchers.IO) {
            val rounds = repo.db.rounds().getLastRounds(100) // Last 100 rounds
            val templates = repo.db.templates().getAll().first()

            mapOf<String, Any?>(
                "totalRounds" to rounds.size,
                "totalTemplates" to templates.size,
                "averageScore" to (templates?.map { template -> template.score }?.average() ?: 0.0) as Any,
                "mostPlayedGame" to rounds.groupBy { it.game }.maxByOrNull { entry -> entry.value.size }?.key,
                "highestScoringTemplate" to templates?.maxByOrNull { it.score }?.id,
                "learningProgress" to calculateLearningProgress(rounds)
            )
        }
    }

    /**
     * Calculate learning progress over time
     */
    private fun calculateLearningProgress(rounds: List<RoundEntity>): Map<String, Double> {
        if (rounds.size < 20) return emptyMap()

        val firstHalf = rounds.take(rounds.size / 2)
        val secondHalf = rounds.takeLast(rounds.size / 2)

        return mapOf(
            "earlyAveragePoints" to firstHalf.map { item -> item.points }.average(),
            "lateAveragePoints" to secondHalf.map { item -> item.points }.average(),
            "improvement" to (secondHalf.map { item -> item.points }.average() - firstHalf.map { item -> item.points }.average())
        )
    }

    /**
     * Reset game state
     */
    suspend fun reset() {
        withContext(Dispatchers.IO) {
            roundIdx = 0
            recentFamilies.clear()
            currentSessionId = repo.startNewSession()
        }
    }

    /**
     * End current session
     */
    suspend fun endSession() {
        withContext(Dispatchers.IO) {
            currentSessionId?.let { sessionId ->
                // Calculate session statistics
                val rounds = repo.db.rounds().getLastRounds(50) // Rounds since session start
                var totalPoints = 0
                for (i in 0 until rounds.size) {
                    val round: RoundEntity = rounds[i]
                    totalPoints += round.points
                }

                repo.endSession(sessionId, rounds.size, totalPoints)
                currentSessionId = null
            }
        }
    }
}

/**
 * Game state data class
 */
data class GameState(
    val currentRound: Int,
    val currentGame: String?,
    val currentPhase: RoundPhase,
    val activePlayers: Int,
    val sessionId: Long?,
    val recentGames: List<String>
)

/**
 * Get current game state
 */
suspend fun GameEngine.getCurrentState(): GameState {
    return GameState(
        currentRound = roundIdx,
        currentGame = null, // Would be set from current card
        currentPhase = RoundPhase.IDLE,
        activePlayers = repo.getActivePlayerCount(),
        sessionId = currentSessionId,
        recentGames = listOf() // Would be populated from recent rounds
    )
}