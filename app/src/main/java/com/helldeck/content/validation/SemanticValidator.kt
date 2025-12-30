package com.helldeck.content.validation

import android.content.res.AssetManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Validates semantic coherence of slot combinations to prevent nonsensical cards.
 * Checks compatibility between slot types and semantic distance between entries.
 */
class SemanticValidator(assetManager: AssetManager) {

    private val compatibility: SemanticCompatibilityMatrix

    init {
        val jsonString = assetManager.open("model/semantic_compatibility.json")
            .bufferedReader()
            .use { it.readText() }

        compatibility = Json { ignoreUnknownKeys = true }.decodeFromString(jsonString)
    }

    /**
     * Validates coherence of slot combinations.
     * Returns score 0.0-1.0 where 1.0 is perfectly coherent.
     * FIX: Now uses slot TYPES from values, not slot NAMES from keys.
     */
    fun validateCoherence(slots: Map<String, SlotFill>): Double {
        if (slots.size < 2) return 1.0 // Single slot always coherent

        var score = 1.0

        // Extract slot types (NOT slot names)
        val slotTypes = slots.values.map { it.slotType }
        val typePairs = slotTypes.pairs()

        // Check forbidden pairs (hard fail)
        for ((typeA, typeB) in typePairs) {
            if (isForbidden(typeA, typeB)) {
                return 0.0 // Immediate rejection
            }
        }

        // Check compatibility (soft penalty)
        for ((typeA, typeB) in typePairs) {
            if (!isCompatible(typeA, typeB)) {
                score *= 0.7 // Penalize incompatible pairs
            }
        }

        // Check domain mixing (bonus for good cross-domain pairs)
        // FIX: Use slot types, not names
        val domains = slotTypes.mapNotNull { getDomain(it) }.distinct()
        if (domains.size >= 2) {
            // Multiple domains can be good (absurdity) or bad (nonsensical)
            val hasSocialAndTaboo = domains.contains("social") && domains.contains("taboo")
            val hasWholesomeAndTaboo = domains.contains("wholesome") && domains.contains("taboo")

            if (hasSocialAndTaboo) {
                score *= 1.1 // Bonus for social + taboo (good absurdity)
            } else if (hasWholesomeAndTaboo) {
                score *= 0.6 // Penalty for wholesome + taboo (bad mismatch)
            }
        }

        // Check semantic distance using simple heuristics
        // (In production, could use word embeddings for better accuracy)
        for ((slotA, slotB) in slots.values.toList().pairs()) {
            val distance = calculateSimpleSemanticDistance(slotA.originalText, slotB.originalText)

            // Penalize if too similar (boring) or too different (nonsensical)
            if (distance < 0.2) {
                score *= 0.8 // Too similar
            } else if (distance > 0.9) {
                score *= 0.85 // Too different
            }
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Checks if two slot types are explicitly forbidden.
     */
    private fun isForbidden(typeA: String, typeB: String): Boolean {
        return compatibility.forbidden_pairs.any { pair ->
            (pair[0] == typeA && pair[1] == typeB) ||
                (pair[0] == typeB && pair[1] == typeA)
        }
    }

    /**
     * Checks if two slot types are compatible.
     */
    private fun isCompatible(typeA: String, typeB: String): Boolean {
        val pairInfo = compatibility.action_reason_pairs[typeA] ?: return true

        // Check if explicitly incompatible
        if (pairInfo.incompatible.contains(typeB)) {
            return false
        }

        // Check if explicitly compatible
        if (pairInfo.compatible.contains(typeB)) {
            return true
        }

        // If not specified, assume neutral compatibility
        return true
    }

    /**
     * Gets the domain category for a slot type.
     */
    private fun getDomain(slotType: String): String? {
        return compatibility.domain_categories.entries
            .find { (_, types) -> types.contains(slotType) }
            ?.key
    }

    /**
     * Calculates simple semantic distance between two texts.
     * Uses word overlap and length difference as heuristics.
     * Returns 0.0 (identical) to 1.0 (completely different).
     */
    private fun calculateSimpleSemanticDistance(textA: String, textB: String): Double {
        val wordsA = textA.lowercase().split(Regex("\\s+")).toSet()
        val wordsB = textB.lowercase().split(Regex("\\s+")).toSet()

        // Jaccard distance: 1 - (intersection / union)
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size

        if (union == 0) return 0.5

        val jaccardSimilarity = intersection.toDouble() / union
        val jaccardDistance = 1.0 - jaccardSimilarity

        // Also consider length difference
        val lengthDiff = kotlin.math.abs(textA.length - textB.length).toDouble()
        val maxLength = kotlin.math.max(textA.length, textB.length).toDouble()
        val lengthDistance = if (maxLength > 0) lengthDiff / maxLength else 0.0

        // Combine both metrics
        return (jaccardDistance * 0.7 + lengthDistance * 0.3).coerceIn(0.0, 1.0)
    }

    /**
     * Extension function to get all pairs from a collection.
     */
    private fun <T> List<T>.pairs(): List<Pair<T, T>> {
        return this.indices.flatMap { i ->
            (i + 1 until this.size).map { j ->
                this[i] to this[j]
            }
        }
    }
}

/**
 * Data classes for semantic compatibility configuration.
 */
@Serializable
data class SemanticCompatibilityMatrix(
    val version: Int,
    val action_reason_pairs: Map<String, PairInfo>,
    val forbidden_pairs: List<List<String>>,
    val domain_categories: Map<String, List<String>>,
)

@Serializable
data class PairInfo(
    val compatible: List<String>,
    val incompatible: List<String>,
)

/**
 * Slot fill data for semantic validation.
 */
data class SlotFill(
    val slotType: String,
    val originalText: String,
    val displayText: String,
)
