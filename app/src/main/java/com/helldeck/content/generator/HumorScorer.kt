package com.helldeck.content.generator

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * HumorScorer evaluates cards for comedic quality using multiple metrics.
 * Inspired by Cards Against Humanity, Bad Choices, and Bad People humor styles.
 */
class HumorScorer(
    private val lexiconRepository: LexiconRepositoryV2,
    private val pairings: Map<String, Map<String, Double>>
) {
    
    data class HumorScore(
        val absurdity: Double,
        val shockValue: Double,
        val relatability: Double,
        val cringeFactor: Double,
        val benignViolation: Double,
        val surprise: Double,
        val timing: Double,
        val specificity: Double,
        val overallScore: Double
    ) {
        companion object {
            private const val ABSURDITY_WEIGHT = 0.20
            private const val SHOCK_WEIGHT = 0.15
            private const val RELATABLE_WEIGHT = 0.20
            private const val CRINGE_WEIGHT = 0.10
            private const val BENIGN_VIOLATION_WEIGHT = 0.15
            private const val SURPRISE_WEIGHT = 0.10
            private const val TIMING_WEIGHT = 0.05
            private const val SPECIFICITY_WEIGHT = 0.05
            
            fun calculate(
                absurdity: Double,
                shock: Double,
                relatable: Double,
                cringe: Double,
                benignViolation: Double,
                surprise: Double,
                timing: Double,
                specificity: Double
            ): HumorScore {
                val overall = (absurdity * ABSURDITY_WEIGHT) +
                             (shock * SHOCK_WEIGHT) +
                             (relatable * RELATABLE_WEIGHT) +
                             (cringe * CRINGE_WEIGHT) +
                             (benignViolation * BENIGN_VIOLATION_WEIGHT) +
                             (surprise * SURPRISE_WEIGHT) +
                             (timing * TIMING_WEIGHT) +
                             (specificity * SPECIFICITY_WEIGHT)
                
                return HumorScore(
                    absurdity = absurdity,
                    shockValue = shock,
                    relatability = relatable,
                    cringeFactor = cringe,
                    benignViolation = benignViolation,
                    surprise = surprise,
                    timing = timing,
                    specificity = specificity,
                    overallScore = overall.coerceIn(0.0, 1.0)
                )
            }
        }
    }
    
    /**
     * Evaluate humor quality of a filled card
     */
    fun evaluate(
        text: String,
        blueprint: TemplateBlueprint,
        slots: Map<String, SlotData>
    ): HumorScore {
        val absurdity = calculateAbsurdity(slots, blueprint)
        val shock = calculateShockValue(slots, blueprint)
        val relatable = calculateRelatability(slots, blueprint)
        val cringe = calculateCringe(slots)
        val benignViolation = calculateBenignViolation(slots, shock)
        val surprise = calculateSurprise(slots, text)
        val timing = calculateTiming(text, slots, blueprint)
        val specificity = calculateSpecificity(slots)
        
        return HumorScore.calculate(
            absurdity, shock, relatable, cringe, benignViolation,
            surprise, timing, specificity
        )
    }
    
    /**
     * Absurdity Score: Measures unexpectedness from incompatible slot combinations
     */
    @Suppress("UNUSED_PARAMETER")
    private fun calculateAbsurdity(slots: Map<String, SlotData>, blueprint: TemplateBlueprint): Double {
        if (slots.size < 2) return 0.3 // Single slot = low absurdity
        
        var absurdityScore = 0.0
        val slotTypes = slots.values.map { it.slotType }.distinct()
        
        // Check semantic distance between slot types via pairings
        for (i in slotTypes.indices) {
            for (j in i + 1 until slotTypes.size) {
                val typeA = slotTypes[i]
                val typeB = slotTypes[j]
                
                // Low or negative pairing = high absurdity (unexpected combo)
                val pairScore = pairings[typeA]?.get(typeB) ?: 0.0
                if (pairScore < 0.2) {
                    absurdityScore += 0.3
                } else if (pairScore > 0.8) {
                    absurdityScore -= 0.1 // Too expected
                }
            }
        }
        
        // Bonus for mixing innocent + taboo
        val hasInnocent = slotTypes.any { it in INNOCENT_TYPES }
        val hasTaboo = slotTypes.any { it in TABOO_TYPES }
        if (hasInnocent && hasTaboo) absurdityScore += 0.4
        
        return absurdityScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Shock Value: Measures taboo element presence and spice levels
     * Updated to use max spice with decay instead of average
     */
    private fun calculateShockValue(slots: Map<String, SlotData>, blueprint: TemplateBlueprint): Double {
        var shockScore = 0.0
        
        // Taboo slot types contribute to shock
        val tabooCount = slots.values.count { it.slotType in TABOO_TYPES }
        shockScore += (tabooCount * 0.25)
        
        // Use max spice with decay, not average (one extreme element matters more)
        val spices = slots.values.map { it.spice }.sortedDescending()
        val maxSpice = spices.firstOrNull() ?: 0
        shockScore += (maxSpice / 5.0) * 0.4
        
        // Add decayed contribution from other spicy elements
        val spiceDecay = spices.drop(1).mapIndexed { i, s ->
            s * kotlin.math.exp(-0.3 * i)
        }.sum()
        shockScore += (spiceDecay / (5.0 * slots.size.coerceAtLeast(1))) * 0.2
        
        // Blueprint spice max indicates intended shock level
        shockScore += (blueprint.spice_max / 5.0) * 0.15
        
        return shockScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Relatability: Identifies common social situations and relatable contexts
     */
    private fun calculateRelatability(slots: Map<String, SlotData>, blueprint: TemplateBlueprint): Double {
        var relateScore = 0.0
        
        // Relatable slot types
        val relateCount = slots.values.count { it.slotType in RELATABLE_TYPES }
        relateScore += (relateCount * 0.3)
        
        // Player-targeting games are highly relatable
        if (blueprint.game in SOCIAL_GAMES) relateScore += 0.3
        
        // Everyday contexts boost relatability
        val hasEveryday = slots.values.any { it.slotType in EVERYDAY_TYPES }
        if (hasEveryday) relateScore += 0.2
        
        // Medium spice (2-3) hits sweet spot of relatability
        val avgSpice = slots.values.map { it.spice }.average().let { if (it.isFinite()) it else 0.0 }
        if (avgSpice in 2.0..3.5) relateScore += 0.2
        
        return relateScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Cringe Factor: Detects awkward scenarios and embarrassing situations
     */
    private fun calculateCringe(slots: Map<String, SlotData>): Double {
        var cringeScore = 0.0
        
        // Awkward context slots are pure cringe
        val awkwardCount = slots.values.count { 
            it.slotType == "awkward_contexts" || 
            it.slotType == "relationship_fails" ||
            it.slotType == "selfish_behaviors"
        }
        cringeScore += (awkwardCount * 0.4)
        
        // Sexual + bodily combos = maximum cringe
        val hasSexual = slots.values.any { it.slotType == "sexual_innuendo" }
        val hasBodily = slots.values.any { it.slotType == "bodily_functions" }
        if (hasSexual && hasBodily) cringeScore += 0.3
        
        return cringeScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Benign Violation: Boundary-crossing humor that's taboo but playful
     * McGraw's Benign Violation Theory: humor occurs when something seems wrong but safe
     * Updated to require playful framing for high scores
     */
    private fun calculateBenignViolation(slots: Map<String, SlotData>, shockValue: Double): Double {
        // Need taboo elements (violation)
        val hasTaboo = slots.values.any { it.slotType in TABOO_TYPES }
        if (!hasTaboo) return 0.2 // No violation
        
        // Check for playful tone markers (benign framing)
        val hasPlayful = slots.values.any { it.tone in PLAYFUL_TONES }
        
        // Calculate average spice
        val avgSpice = slots.values.map { it.spice }.average()
        
        // Benign violation requires BOTH taboo content AND playful framing
        val benignScore = when {
            !hasPlayful -> 0.3 // Violation without benign framing (dark, not funny)
            hasTaboo && hasPlayful && avgSpice in 2.0..4.0 -> 0.9 // Perfect benign violation
            hasTaboo && hasPlayful && avgSpice < 2.0 -> 0.5 // Too tame
            hasTaboo && hasPlayful -> 0.6 // Too extreme but has playful framing
            else -> 0.4
        }
        
        // Bonus for mixing taboo with innocent (classic benign violation)
        val hasInnocent = slots.values.any { it.slotType in INNOCENT_TYPES }
        val mixBonus = if (hasTaboo && hasInnocent && hasPlayful) 0.1 else 0.0
        
        return (benignScore + mixBonus).coerceIn(0.0, 1.0)
    }
    
    /**
     * Surprise Metric: Measures novelty and unexpectedness of combinations.
     */
    private fun calculateSurprise(slots: Map<String, SlotData>, text: String): Double {
        var surpriseScore = 0.5 // Base score
        
        // Check for unexpected word combinations
        val words = text.lowercase().split(Regex("\\s+"))
        val hasUnexpectedPair = words.zipWithNext().any { (w1, w2) ->
            // Simple heuristic: words from different semantic domains
            val isUnexpected = (w1.length > 4 && w2.length > 4) && 
                              !w1.startsWith(w2.take(3)) && 
                              !w2.startsWith(w1.take(3))
            isUnexpected
        }
        
        if (hasUnexpectedPair) surpriseScore += 0.2
        
        // Bonus for mixing incompatible slot types (controlled absurdity)
        val slotTypes = slots.values.map { it.slotType }.distinct()
        if (slotTypes.size >= 3) surpriseScore += 0.2
        
        // Bonus for specific, concrete details (more surprising than vague)
        val hasNumbers = text.contains(Regex("\\d+"))
        val hasProperNoun = text.split(" ").any { it.firstOrNull()?.isUpperCase() == true }
        if (hasNumbers || hasProperNoun) surpriseScore += 0.1
        
        return surpriseScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Timing Metric: Validates punchline position and comedic pacing.
     */
    private fun calculateTiming(text: String, slots: Map<String, SlotData>, blueprint: TemplateBlueprint): Double {
        val words = text.split(Regex("\\s+"))
        if (words.size < 5) return 0.5 // Too short to evaluate timing
        
        var timingScore = 0.5
        
        // Check if punchline is at the end (last 30% of text)
        val lastSlotText = slots.values.lastOrNull()?.text
        if (lastSlotText != null) {
            val punchlinePosition = text.lastIndexOf(lastSlotText)
            val relativePosition = punchlinePosition.toDouble() / text.length
            
            if (relativePosition > 0.7) {
                timingScore = 1.0 // Perfect punchline placement
            } else if (relativePosition > 0.5) {
                timingScore = 0.7 // Acceptable placement
            } else {
                timingScore = 0.3 // Punchline too early
            }
        }
        
        // Bonus for ending with punctuation that adds emphasis
        if (text.endsWith("!") || text.endsWith("?")) {
            timingScore += 0.1
        }
        
        return timingScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Specificity Metric: Rewards concrete details over vague descriptions.
     */
    private fun calculateSpecificity(slots: Map<String, SlotData>): Double {
        val specificityScores = slots.values.map { slot ->
            val text = slot.text
            var score = 0.5
            
            // Multi-word entries are more specific
            val wordCount = text.split(Regex("\\s+")).size
            if (wordCount > 3) score += 0.2
            else if (wordCount > 1) score += 0.1
            
            // Numbers add specificity
            if (text.contains(Regex("\\d+"))) score += 0.2
            
            // Proper nouns are specific
            if (text.firstOrNull()?.isUpperCase() == true) score += 0.1
            
            // Possessives and contractions indicate specificity
            if (text.contains("'")) score += 0.1
            
            // Longer text tends to be more specific
            if (text.length > 30) score += 0.1
            
            score.coerceIn(0.0, 1.0)
        }
        
        return if (specificityScores.isNotEmpty()) {
            specificityScores.average()
        } else {
            0.5
        }
    }
    
    data class SlotData(
        val slotType: String,
        val text: String,
        val spice: Int,
        val tone: String
    )
    
    companion object {
        // Slot type categories for humor analysis
        private val TABOO_TYPES = setOf(
            "sexual_innuendo", "bodily_functions", "taboo_topics",
            "vices_and_indulgences", "relationship_fails"
        )
        
        private val INNOCENT_TYPES = setOf(
            "dating_green_flags", "perks_plus", "categories", "letters"
        )
        
        private val RELATABLE_TYPES = setOf(
            "awkward_contexts", "selfish_behaviors", "relationship_fails",
            "internet_slang", "meme_references", "social_reason", "receipts"
        )
        
        private val EVERYDAY_TYPES = setOf(
            "awkward_contexts", "product_item", "meme_item",
            "vices_and_indulgences", "internet_slang"
        )
        
        private val SOCIAL_GAMES = setOf(
            "ROAST_CONSENSUS", "MAJORITY_REPORT", "HOT_SEAT_IMPOSTER",
            "TEXT_THREAD_TRAP"
        )
        
        private val PLAYFUL_TONES = setOf(
            "playful", "witty", "wild", "dry"
        )
    }
}
