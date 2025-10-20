package com.helldeck.engine

import com.helldeck.data.TemplateEntity
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Template selection system for HELLDECK
 * Uses multi-armed bandit algorithms and learning data to select optimal templates
 */
object Selection {

    /**
     * Selection choice data class
     */
    data class Choice(
        val template: TemplateEntity,
        val choiceScore: Double,
        val explorationBonus: Double = 0.0,
        val diversityBonus: Double = 0.0,
        val recencyBonus: Double = 0.0
    ) {
        val components: Map<String, Double> = mapOf(
            "baseScore" to template.score,
            "explorationBonus" to explorationBonus,
            "diversityBonus" to diversityBonus,
            "recencyBonus" to recencyBonus,
            "totalScore" to choiceScore
        )
    }

    /**
     * Calculate epsilon for epsilon-greedy algorithm
     */
    internal fun calculateEpsilon(
        roundIdx: Int,
        start: Double = Config.current.learning.epsilon_start,
        end: Double = Config.current.learning.epsilon_end,
        decayRounds: Int = Config.current.learning.decay_rounds
    ): Double {
        val t = (roundIdx.toDouble() / decayRounds).coerceIn(0.0, 1.0)
        return start + (end - start) * t
    }

    /**
     * Calculate Upper Confidence Bound for a template
     */
    internal fun calculateUCB(
        template: TemplateEntity,
        totalDraws: Int,
        roundIdx: Int,
        c: Double = 1.0
    ): Double {
        if (template.draws == 0) return Double.MAX_VALUE // Encourage exploration of new templates

        val averageReward = template.score
        val confidence = c * sqrt(ln(roundIdx.toDouble()) / template.draws)

        return averageReward + confidence
    }

    /**
     * Calculate Thompson Sampling probability for a template
     */
    internal fun calculateThompsonProbability(
        template: TemplateEntity,
        alpha: Double = 1.0,
        beta: Double = 1.0
    ): Double {
        // Use Beta distribution parameters based on wins and losses
        val wins = template.wins.toDouble()
        val losses = (template.draws - template.wins).toDouble()

        // Sample from Beta distribution
        val a = wins + alpha
        val b = losses + beta

        // Simplified sampling - in practice you'd use proper Beta sampling
        return a / (a + b)
    }

    /**
     * Select next template using epsilon-greedy algorithm
     */
    fun pickNextEpsilonGreedy(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundIdx: Int
    ): TemplateEntity {
        val epsilon = calculateEpsilon(roundIdx)

        return if (Random.nextDouble() < epsilon) {
            // Exploration: pick random template
            candidates.filter { it.family !in recentFamilies }
                .ifEmpty { candidates }
                .random()
        } else {
            // Exploitation: pick best template
            pickBestTemplate(candidates, recentFamilies, roundIdx)
        }
    }

    /**
     * Select next template using Upper Confidence Bound algorithm
     */
    fun pickNextUCB(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundIdx: Int
    ): TemplateEntity {
        val totalDraws = candidates.sumOf { it.draws }
        val c = 1.0 // Exploration parameter

        val scored = candidates.map { template ->
            val baseScore = template.score
            val ucbBonus = calculateUCB(template, totalDraws, roundIdx, c)
            val diversityBonus = Learning.calculateDiversityBonus(template, recentFamilies)

            Choice(
                template = template,
                choiceScore = baseScore + ucbBonus + diversityBonus,
                explorationBonus = ucbBonus,
                diversityBonus = diversityBonus
            )
        }

        return scored.maxByOrNull { it.choiceScore }?.template
            ?: candidates.random()
    }

    /**
     * Select next template using Thompson Sampling
     */
    fun pickNextThompson(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundIdx: Int
    ): TemplateEntity {
        val scored = candidates.map { template ->
            val thompsonProb = calculateThompsonProbability(template)
            val diversityBonus = Learning.calculateDiversityBonus(template, recentFamilies)
            val explorationBonus = Learning.calculateExplorationBonus(template.draws)

            Choice(
                template = template,
                choiceScore = thompsonProb + diversityBonus + explorationBonus,
                explorationBonus = explorationBonus,
                diversityBonus = diversityBonus
            )
        }

        return scored.maxByOrNull { it.choiceScore }?.template
            ?: candidates.random()
    }

    /**
     * Select next template using hybrid approach (default)
     */
    fun pickNext(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundIdx: Int,
        algorithm: SelectionAlgorithm = SelectionAlgorithm.HYBRID
    ): TemplateEntity {
        if (!Config.learningEnabled) {
            // When learning is off, prefer simple random with diversity
            return candidates.filter { it.family !in recentFamilies }
                .ifEmpty { candidates }
                .random()
        }
        val filteredCandidates = candidates.filter { it.family !in recentFamilies }
            .ifEmpty { candidates }

        return when (algorithm) {
            SelectionAlgorithm.EPSILON_GREEDY -> pickNextEpsilonGreedy(filteredCandidates, recentFamilies, roundIdx)
            SelectionAlgorithm.UCB -> pickNextUCB(filteredCandidates, recentFamilies, roundIdx)
            SelectionAlgorithm.THOMPSON -> pickNextThompson(filteredCandidates, recentFamilies, roundIdx)
            SelectionAlgorithm.HYBRID -> pickNextHybrid(filteredCandidates, recentFamilies, roundIdx)
        }
    }

    /**
     * Hybrid selection algorithm combining multiple approaches
     */
    private fun pickNextHybrid(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundIdx: Int
    ): TemplateEntity {
        val epsilon = calculateEpsilon(roundIdx)

        return if (Random.nextDouble() < epsilon) {
            // Exploration phase: use UCB for smarter exploration
            pickNextUCB(candidates, recentFamilies, roundIdx)
        } else {
            // Exploitation phase: use comprehensive scoring
            pickBestTemplate(candidates, recentFamilies, roundIdx)
        }
    }

    /**
     * Pick the best template using comprehensive scoring
     */
    private fun pickBestTemplate(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundIdx: Int
    ): TemplateEntity {
        val scored = candidates.map { template ->
            val selectionScore = Learning.calculateSelectionScore(template, recentFamilies, roundIdx)
            val diversityBonus = Learning.calculateDiversityBonus(template, recentFamilies)
            val explorationBonus = Learning.calculateExplorationBonus(template.draws)

            Choice(
                template = template,
                choiceScore = selectionScore + diversityBonus + explorationBonus,
                explorationBonus = explorationBonus,
                diversityBonus = diversityBonus
            )
        }

        return scored.maxByOrNull { it.choiceScore }?.template
            ?: candidates.random()
    }

    /**
     * Get selection statistics for debugging
     */
    fun getSelectionStats(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundIdx: Int
    ): Map<String, Any> {
        val scored = candidates.map { template ->
            val selectionScore = Learning.calculateSelectionScore(template, recentFamilies, roundIdx)
            val diversityBonus = Learning.calculateDiversityBonus(template, recentFamilies)
            val explorationBonus = Learning.calculateExplorationBonus(template.draws)

            Choice(
                template = template,
                choiceScore = selectionScore + diversityBonus + explorationBonus,
                explorationBonus = explorationBonus,
                diversityBonus = diversityBonus
            )
        }

        val best = scored.maxByOrNull { it.choiceScore }
        val worst = scored.minByOrNull { it.choiceScore }

        return mapOf(
            "totalCandidates" to candidates.size,
            "filteredCandidates" to scored.size,
            "bestTemplate" to (best?.template?.id ?: "none"),
            "bestScore" to (best?.choiceScore ?: 0.0),
            "worstScore" to (worst?.choiceScore ?: 0.0),
            "averageScore" to scored.map { it.choiceScore }.average(),
            "scoreDistribution" to scored.groupBy {
                when {
                    it.choiceScore >= 3.0 -> "excellent"
                    it.choiceScore >= 2.0 -> "good"
                    it.choiceScore >= 1.0 -> "average"
                    else -> "poor"
                }
            }.mapValues { it.value.size },
            "epsilon" to calculateEpsilon(roundIdx)
        )
    }

    /**
     * Batch selection for multiple rounds
     */
    fun selectBatch(
        candidates: List<TemplateEntity>,
        recentFamilies: List<String>,
        roundCount: Int,
        algorithm: SelectionAlgorithm = SelectionAlgorithm.HYBRID
    ): List<TemplateEntity> {
        val selected = mutableListOf<TemplateEntity>()
        var currentFamilies = recentFamilies.toMutableList()

        for (i in 0 until roundCount) {
            val next = pickNext(candidates, currentFamilies, i, algorithm)

            selected.add(next)
            currentFamilies.add(next.family)

            // Maintain diversity window
            if (currentFamilies.size > Config.current.learning.diversity_window) {
                currentFamilies = currentFamilies.takeLast(Config.current.learning.diversity_window).toMutableList()
            }
        }

        return selected
    }

    /**
     * Selection algorithm enumeration
     */
    enum class SelectionAlgorithm(val description: String) {
        EPSILON_GREEDY("Epsilon-greedy exploration"),
        UCB("Upper Confidence Bound"),
        THOMPSON("Thompson Sampling"),
        HYBRID("Hybrid approach combining multiple algorithms")
    }

    /**
     * Selection strategy configuration
     */
    data class SelectionStrategy(
        val algorithm: SelectionAlgorithm,
        val epsilonStart: Double = 0.25,
        val epsilonEnd: Double = 0.05,
        val decayRounds: Int = 20,
        val ucbC: Double = 1.0,
        val thompsonAlpha: Double = 1.0,
        val thompsonBeta: Double = 1.0
    )

    /**
     * Create selection strategy from config
     */
    fun createStrategyFromConfig(): SelectionStrategy {
        val config = Config.current.learning
        return SelectionStrategy(
            algorithm = SelectionAlgorithm.HYBRID,
            epsilonStart = config.epsilon_start,
            epsilonEnd = config.epsilon_end,
            decayRounds = config.decay_rounds
        )
    }
}

/**
 * Template selection result for detailed analysis
 */
data class SelectionResult(
    val selectedTemplate: TemplateEntity,
    val algorithm: Selection.SelectionAlgorithm,
    val choice: Selection.Choice,
    val alternatives: List<Selection.Choice>,
    val selectionReason: String,
    val roundIdx: Int
)

/**
 * Advanced selection with detailed result
 */
fun Selection.selectWithResult(
    candidates: List<TemplateEntity>,
    recentFamilies: List<String>,
    roundIdx: Int,
    algorithm: Selection.SelectionAlgorithm = Selection.SelectionAlgorithm.HYBRID
): SelectionResult {
    val filteredCandidates = candidates.filter { it.family !in recentFamilies }
        .ifEmpty { candidates }

    val scored = filteredCandidates.map { template ->
        val selectionScore = Learning.calculateSelectionScore(template, recentFamilies, roundIdx)
        val diversityBonus = Learning.calculateDiversityBonus(template, recentFamilies)
        val explorationBonus = Learning.calculateExplorationBonus(template.draws)

        Selection.Choice(
            template = template,
            choiceScore = selectionScore + diversityBonus + explorationBonus,
            explorationBonus = explorationBonus,
            diversityBonus = diversityBonus
        )
    }

    val selectedChoice = when (algorithm) {
        Selection.SelectionAlgorithm.EPSILON_GREEDY -> {
            val epsilon = calculateEpsilon(roundIdx)
            if (Random.nextDouble() < epsilon) {
                scored.random()
            } else {
                scored.maxByOrNull { it.choiceScore } ?: scored.random()
            }
        }
        Selection.SelectionAlgorithm.UCB -> {
            val totalDraws = filteredCandidates.sumOf { it.draws }
            scored.maxByOrNull { calculateUCB(it.template, totalDraws, roundIdx) + it.choiceScore }
                ?: scored.random()
        }
        Selection.SelectionAlgorithm.THOMPSON -> {
            scored.maxByOrNull { calculateThompsonProbability(it.template) + it.choiceScore }
                ?: scored.random()
        }
        Selection.SelectionAlgorithm.HYBRID -> {
            val epsilon = calculateEpsilon(roundIdx)
            if (Random.nextDouble() < epsilon) {
                val totalDraws = filteredCandidates.sumOf { it.draws }
                scored.maxByOrNull { calculateUCB(it.template, totalDraws, roundIdx) + it.choiceScore }
                    ?: scored.random()
            } else {
                scored.maxByOrNull { it.choiceScore } ?: scored.random()
            }
        }
    }

    val reason = when (algorithm) {
        Selection.SelectionAlgorithm.EPSILON_GREEDY -> {
            val epsilon = calculateEpsilon(roundIdx)
            if (Random.nextDouble() < epsilon) "Exploration (ε=$epsilon)" else "Exploitation"
        }
        Selection.SelectionAlgorithm.UCB -> "UCB with confidence bonus"
        Selection.SelectionAlgorithm.THOMPSON -> "Thompson Sampling"
        Selection.SelectionAlgorithm.HYBRID -> {
            val epsilon = calculateEpsilon(roundIdx)
            if (Random.nextDouble() < epsilon) "Hybrid exploration" else "Hybrid exploitation"
        }
    }

    return SelectionResult(
        selectedTemplate = selectedChoice.template,
        algorithm = algorithm,
        choice = selectedChoice,
        alternatives = scored.filter { it != selectedChoice },
        selectionReason = reason,
        roundIdx = roundIdx
    )
}
