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
        val absurdity: Double,           // 0.0-1.0: Unexpectedness of combinations
        val shockValue: Double,          // 0.0-1.0: Taboo element presence
        val relatability: Double,        // 0.0-1.0: Common social situations
        val cringeFactor: Double,        // 0.0-1.0: Awkward scenarios
        val benignViolation: Double,     // 0.0-1.0: Boundary-crossing without malice
        val overallScore: Double         // Weighted composite
    ) {
        companion object {
            // Weights for overall score calculation
            private const val ABSURDITY_WEIGHT = 0.25
            private const val SHOCK_WEIGHT = 0.20
            private const val RELATABLE_WEIGHT = 0.25
            private const val CRINGE_WEIGHT = 0.15
            private const val BENIGN_VIOLATION_WEIGHT = 0.15
            
            fun calculate(
                absurdity: Double,
                shock: Double,
                relatable: Double,
                cringe: Double,
                benignViolation: Double
            ): HumorScore {
                val overall = (absurdity * ABSURDITY_WEIGHT) +
                             (shock * SHOCK_WEIGHT) +
                             (relatable * RELATABLE_WEIGHT) +
                             (cringe * CRINGE_WEIGHT) +
                             (benignViolation * BENIGN_VIOLATION_WEIGHT)
                
                return HumorScore(
                    absurdity = absurdity,
                    shockValue = shock,
                    relatability = relatable,
                    cringeFactor = cringe,
                    benignViolation = benignViolation,
                    overallScore = overall.coerceIn(0.0, 1.0)
                )
            }
        }
    }
    
    /**
     * Evaluate humor quality of a filled card
     */
    @Suppress("UNUSED_PARAMETER")
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
        
        return HumorScore.calculate(absurdity, shock, relatable, cringe, benignViolation)
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
     */
    private fun calculateShockValue(slots: Map<String, SlotData>, blueprint: TemplateBlueprint): Double {
        var shockScore = 0.0
        
        // Taboo slot types contribute to shock
        val tabooCount = slots.values.count { it.slotType in TABOO_TYPES }
        shockScore += (tabooCount * 0.25)
        
        // High spice entries increase shock
        val avgSpice = slots.values.map { it.spice }.average().let { if (it.isFinite()) it else 0.0 }
        shockScore += (avgSpice / 5.0) * 0.5
        
        // Blueprint spice max indicates intended shock level
        shockScore += (blueprint.spice_max / 5.0) * 0.25
        
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
     */
    @Suppress("UNUSED_PARAMETER")
    private fun calculateBenignViolation(slots: Map<String, SlotData>, shockValue: Double): Double {
        // Need taboo elements (violation)
        val hasTaboo = slots.values.any { it.slotType in TABOO_TYPES }
        if (!hasTaboo) return 0.2
        
        // But not too extreme (needs benign framing)
        val avgSpice = slots.values.map { it.spice }.average()
        
        // Sweet spot: spice 2-4 with taboo content = benign violation
        val benignScore = when {
            avgSpice < 2.0 -> 0.3 // Too tame
            avgSpice in 2.0..4.0 -> 0.8 // Perfect balance
            else -> 0.4 // Too extreme, less funny
        }
        
        // Check for playful tone markers
        val hasPlayful = slots.values.any { it.tone in PLAYFUL_TONES }
        val bonus = if (hasPlayful) 0.2 else 0.0
        
        return (benignScore + bonus).coerceIn(0.0, 1.0)
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
            "internet_slang", "meme_references", "social_reason"
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
