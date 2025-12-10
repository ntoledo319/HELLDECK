package com.helldeck.content.engine

import com.helldeck.content.data.ContentRepository
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.engine.Config
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * ContextualSelector implements Thompson Sampling algorithm for intelligent template selection.
 * 
 * This class uses a Bayesian approach to balance exploration and exploitation when selecting
 * game templates. It maintains alpha and beta parameters for each template and updates
 * them based on user feedback to improve selection quality over time.
 * 
 * Key features:
 * - Thompson Sampling for exploration/exploitation balance
 * - Diversity penalties to avoid repetitive content
 * - Tag affinity bonuses for personalized content
 * - Recent history tracking to avoid immediate repetition
 * 
 * @param repo Content repository for accessing template history and statistics
 * @param rng Random number generator for stochastic sampling
 */
class ContextualSelector(
    private val repo: ContentRepository,
    private val rng: Random
) {
    // Rough round counter to adapt exploration over time
    private var rounds: Int = 0
    
    /**
     * Context data required for template selection
     * 
     * @param players List of player names participating in the current session
     * @param activePlayer ID of the currently active player (null if no active player)
     * @param roomHeat Current room engagement level (0.0-1.0, higher = more engaged)
     * @param spiceMax Maximum spice level allowed for template selection
     * @param wantedGameId Specific game ID to force selection (null for any game)
     * @param recentFamilies List of template families recently shown to avoid repetition
     * @param avoidIds Set of template IDs to explicitly exclude from selection
     * @param tagAffinity Map of tag names to affinity scores for personalization
     */
    data class Context(
        val players: List<String>,
        val activePlayer: String? = null,
        val roomHeat: Double = 0.0,
        val spiceMax: Int = 3,
        val wantedGameId: String? = null,
        val recentFamilies: List<String> = emptyList(),
        val avoidIds: Set<String> = emptySet(),
        val tagAffinity: Map<String, Double> = emptyMap()
    )

    // Alpha and beta parameters for Thompson Sampling (one pair per template)
    private val alpha = mutableMapOf<String, Double>()
    private val beta = mutableMapOf<String, Double>()

    /**
     * Initializes the selector with prior distributions for templates.
     * 
     * This should be called once during app initialization to set up the Bayesian
     * parameters for all known templates.
     * 
     * @param priors Map of template IDs to (alpha, beta) parameter pairs
     */
    fun seed(priors: Map<String, Pair<Double, Double>>) {
        priors.forEach { (id, ab) ->
            alpha[id] = max(1e-3, ab.first)
            beta[id] = max(1e-3, ab.second)
        }
    }

    /**
     * Updates the Thompson Sampling parameters based on observed rewards.
     * 
     * Uses a learning rate to gradually update the posterior distribution.
     * Alpha represents success count, beta represents failure count.
     * 
     * @param templateId The template ID that was used
     * @param reward01 Normalized reward value (0.0-1.0) from user feedback
     */
    fun update(templateId: String, reward01: Double) {
        val a = alpha.getOrDefault(templateId, 1.0)
        val b = beta.getOrDefault(templateId, 1.0)
        val gain = Config.current.learning.alpha
        alpha[templateId] = a + reward01 * gain
        beta[templateId] = b + (1 - reward01) * gain
    }

    /**
     * Selects the best template from the available pool using Thompson Sampling.
     * 
     * Selection process:
     * 1. Filter templates based on context constraints
     * 2. Apply diversity penalties and affinity bonuses
     * 3. Sample from posterior distributions
     * 4. Select highest-scoring template
     * 
     * @param ctx Selection context containing all relevant constraints and preferences
     * @param pool Available templates to choose from
     * @return Selected template that best matches the context
     */
    fun pick(ctx: Context, pool: List<TemplateV2>): TemplateV2 {
        val filtered = pool.filter {
            it.spice <= ctx.spiceMax &&
            (ctx.wantedGameId == null || it.game == ctx.wantedGameId) &&
            it.id !in ctx.avoidIds &&
            (it.min_players == null || ctx.players.size >= it.min_players)
        }
        
        val recent = repo.recentHistoryIds(horizon = 10)
        
        fun diversityPenalty(t: TemplateV2): Double {
            var p = 0.0
            if (t.id in recent) p += 0.5 // Increased penalty for recently shown templates
            if (t.family in ctx.recentFamilies) p += 0.4 // Increased penalty for same family
            // Add penalty for same game repetition
            val recentGames = repo.recentHistoryIds(horizon = 5)
                .mapNotNull { id -> repo.templatesV2().find { it.id == id }?.game }
                .distinct()
            if (t.game in recentGames) p += 0.3
            return p
        }
        
        fun affinityBonus(t: TemplateV2): Double {
            if (ctx.tagAffinity.isEmpty()) return 0.0
            val s = t.tags.sumOf { tag -> ctx.tagAffinity[tag] ?: 0.0 }
            return (s / t.tags.size.coerceAtLeast(1)) * 0.4 // Increased weight bonus for matching tags
        }
        
        data class Scored(val t: TemplateV2, val score: Double)
        
        val scored = filtered.map { t ->
            val a = alpha.getOrDefault(t.id, 1.0)
            val b = beta.getOrDefault(t.id, 1.0)
            val sample = sampleBeta(a, b)
            val novelty = 0.1 + (t.weight - 1.0) * 0.1 // Increased bonus for variety
            Scored(t, sample + novelty + affinityBonus(t) - diversityPenalty(t))
        }.sortedByDescending { it.score }
        
        val epsilon = Config.getEpsilonForRound(rounds)
        val picked = if (filtered.isNotEmpty() && rng.nextDouble() < epsilon) {
            // Explore: pick from top-k or filtered pool
            val topK = scored.take(maxOf(1, (scored.size * 0.25).toInt())).map { it.t }
            (topK.ifEmpty { filtered }).random(rng)
        } else {
            // Exploit: pick best scored
            scored.firstOrNull()?.t ?: filtered.random(rng)
        }
        rounds++
        repo.addExposure(picked.id)
        return picked
    }

    /**
     * Samples from a Beta distribution using Thompson Sampling.
     * 
     * @param a Alpha parameter (success count)
     * @param b Beta parameter (failure count)
     * @return Sampled value from Beta(a,b) distribution
     */
    private fun sampleBeta(a: Double, b: Double): Double {
        val x = sampleGamma(a)
        val y = sampleGamma(b)
        return x / (x + y)
    }
    
    /**
     * Samples from a Gamma distribution using Marsaglia & Tsang method.
     * 
     * @param k Shape parameter
     * @return Sampled value from Gamma(k,1) distribution
     */
    private fun sampleGamma(k: Double): Double {
        if (k < 1) {
            val u = rng.nextDouble()
            return sampleGamma(k + 1) * u.pow(1.0 / k)
        }
        val d = k - 1.0 / 3.0
        val c = 1.0 / sqrt(9 * d)
        while (true) {
            var x: Double
            var v: Double
            do {
                x = rng.nextGaussian()
                v = 1 + c * x
            } while (v <= 0)
            v *= v * v
            val u = rng.nextDouble()
            if (u < 1 - 0.0331 * x * x * x) return d * v
            if (ln(u) < 0.5 * x * x + d * (1 - v + ln(v))) return d * v
        }
    }
}

/**
 * Extension function to generate Gaussian-distributed random numbers.
 * Uses the Box-Muller transform for normal distribution generation.
 * 
 * @return Normally distributed random number with mean 0 and standard deviation 1
 */
private fun Random.nextGaussian(): Double {
    var u: Double
    var v: Double
    var s: Double
    do {
        u = 2.0 * nextDouble() - 1.0
        v = 2.0 * nextDouble() - 1.0
        s = u * u + v * v
    } while (s >= 1.0 || s == 0.0)
    return u * sqrt(-2.0 * ln(s) / s)
}
