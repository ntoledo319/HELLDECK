package com.helldeck.content.generator

import android.content.res.AssetManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.yaml.snakeyaml.Yaml
import kotlin.math.exp

class GeneratorArtifacts(assetManager: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }

    val priors: Map<String, PriorStats>
    val pairings: Map<String, Map<String, Double>>
    val logisticModel: LogisticModel?
    val bannedTokens: Set<String>
    val bannedPhrases: Set<String>
    val rules: GeneratorRules

    init {
        priors = loadPriors(assetManager)
        pairings = loadPairings(assetManager)
        logisticModel = loadLogit(assetManager)
        val banned = loadBanned(assetManager)
        bannedTokens = banned.first
        bannedPhrases = banned.second
        rules = loadRules(assetManager)
    }

    private fun loadPriors(assetManager: AssetManager): Map<String, PriorStats> {
        return runCatching {
            val text = assetManager.open("model/priors.json").bufferedReader().use { it.readText() }
            val file = json.decodeFromString<PriorFile>(text)
            file.blueprints.associate { it.id to PriorStats(it.alpha, it.beta) }
        }.getOrDefault(emptyMap())
    }

    private fun loadPairings(assetManager: AssetManager): Map<String, Map<String, Double>> {
        return runCatching {
            val text = assetManager.open("model/pairings.json").bufferedReader().use { it.readText() }
            val file = json.decodeFromString<PairingsFile>(text)
            file.pairs.associate { it.slotType to it.compatibility }
        }.getOrDefault(emptyMap())
    }

    private fun loadLogit(assetManager: AssetManager): LogisticModel? {
        return runCatching {
            val text = assetManager.open("model/logit.json").bufferedReader().use { it.readText() }
            val file = json.decodeFromString<LogitFile>(text)
            LogisticModel(file.features, file.threshold)
        }.getOrNull()
    }

    private fun loadBanned(assetManager: AssetManager): Pair<Set<String>, Set<String>> {
        return runCatching {
            val text = assetManager.open("model/banned.json").bufferedReader().use { it.readText() }
            val file = json.decodeFromString<BannedFile>(text)
            file.tokens.toSet() to file.phrases.toSet()
        }.getOrDefault(emptySet<String>() to emptySet())
    }

    private fun loadRules(assetManager: AssetManager): GeneratorRules {
        return runCatching {
            val text = assetManager.open("model/rules.yaml").bufferedReader().use { it.readText() }
            val yaml = Yaml().load<Map<String, Any>>(text)
            GeneratorRules(
                threshold = (yaml["coherence_threshold"] as? Number)?.toDouble() ?: 0.0,
                maxAttempts = (yaml["max_attempts"] as? Number)?.toInt() ?: 3,
                maxRepetitionRatio = (yaml["max_repetition_ratio"] as? Number)?.toDouble() ?: 0.4,
                minWordCount = (yaml["min_word_count"] as? Number)?.toInt() ?: 5,
                maxWordCount = (yaml["max_word_count"] as? Number)?.toInt() ?: 30,
                softRepetitionMargin = (yaml["soft_repetition_margin"] as? Number)?.toDouble() ?: 0.2,
                nearWordLimitMargin = (yaml["near_word_limit_margin"] as? Number)?.toDouble() ?: 0.1,
                attemptsByGame = (yaml["attempts_by_game"] as? Map<*, *>)
                    ?.mapNotNull { (k, v) -> (k as? String)?.let { it to ((v as? Number)?.toInt() ?: 0) } }
                    ?.filter { it.second > 0 }
                    ?.toMap()
                    ?: emptyMap(),
                tonePreferenceLow = (yaml["tone_preference_low"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                tonePreferenceHigh = (yaml["tone_preference_high"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                humorThreshold = (yaml["humor_threshold"] as? Number)?.toDouble() ?: 0.35,
                enableHumorScoring = (yaml["enable_humor_scoring"] as? Boolean) ?: true,
                enableSemanticValidation = (yaml["enable_semantic_validation"] as? Boolean) ?: true,
                semanticThreshold = (yaml["semantic_threshold"] as? Number)?.toDouble() ?: 0.50,
                spiceRampPerRound = (yaml["spice_ramp_per_round"] as? Number)?.toDouble() ?: 0.0,
                spiceRampCap = (yaml["spice_ramp_cap"] as? Number)?.toDouble() ?: 5.0,
            )
        }.getOrElse {
            GeneratorRules()
        }
    }
}

data class PriorStats(val alpha: Double, val beta: Double) {
    fun mean(): Double = alpha / (alpha + beta)
}

data class LogisticModel(
    private val features: Map<String, Double>,
    private val threshold: Double,
) {
    private val bias = features["bias"] ?: 0.0

    fun passes(featureNames: Collection<String>): Boolean {
        val sum = featureNames.sumOf { features[it] ?: 0.0 } + bias
        val logistic = 1.0 / (1.0 + exp(-sum))
        return logistic >= threshold
    }
}

data class GeneratorRules(
    val threshold: Double = 0.15,
    val maxAttempts: Int = 3,
    val maxRepetitionRatio: Double = 0.4,
    val minWordCount: Int = 5,
    val maxWordCount: Int = 30,
    val softRepetitionMargin: Double = 0.2,
    val nearWordLimitMargin: Double = 0.1,
    val attemptsByGame: Map<String, Int> = emptyMap(),
    val tonePreferenceLow: List<String> = emptyList(),
    val tonePreferenceHigh: List<String> = emptyList(),
    val humorThreshold: Double = 0.35,
    val enableHumorScoring: Boolean = true,
    val enableSemanticValidation: Boolean = true,
    val semanticThreshold: Double = 0.50,
    val spiceRampPerRound: Double = 0.0,
    val spiceRampCap: Double = 5.0,
)

@Serializable
private data class PriorFile(
    val version: Int,
    val blueprints: List<PriorEntry>,
)

@Serializable
private data class PriorEntry(
    val id: String,
    val alpha: Double,
    val beta: Double,
)

@Serializable
private data class PairingsFile(
    val version: Int,
    val pairs: List<PairEntry>,
)

@Serializable
private data class PairEntry(
    @SerialName("slot_type") val slotType: String,
    val compatibility: Map<String, Double>,
)

@Serializable
private data class LogitFile(
    val version: Int,
    val features: Map<String, Double>,
    val threshold: Double,
)

@Serializable
private data class BannedFile(
    val version: Int,
    val tokens: List<String> = emptyList(),
    val phrases: List<String> = emptyList(),
)
