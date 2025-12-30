package com.helldeck.content.validation

import android.content.Context
import android.content.res.AssetManager
import com.helldeck.content.generator.BlueprintOptionProvider
import com.helldeck.content.generator.BlueprintSegment
import com.helldeck.content.generator.LexiconFile
import com.helldeck.content.generator.TemplateBlueprint
import com.helldeck.utils.Logger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.yaml.snakeyaml.Yaml

/**
 * Validates all V3 generator assets at boot time.
 * Reports errors and optionally forces gold-only fallback on critical failures.
 */
object AssetValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<ValidationError>,
        val warnings: List<String>,
        val summary: String,
    )

    data class ValidationError(
        val asset: String,
        val severity: Severity,
        val message: String,
        val detail: String? = null,
    )

    enum class Severity {
        CRITICAL, // Breaks generation completely
        ERROR, // Degrades quality significantly
        WARNING, // Minor issue, safe to continue
    }

    /**
     * Validate all V3 assets and return comprehensive report.
     * @param context Application context
     * @param strictMode If true, treat errors as critical
     * @return ValidationResult with all findings
     */
    fun validateAll(context: Context, strictMode: Boolean = false): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<String>()

        // 1. Validate blueprints (templates_v3/*.json)
        validateBlueprints(context.assets, errors, warnings)

        // 2. Validate lexicons (lexicons_v2/*.json)
        validateLexicons(context.assets, errors, warnings)

        // 3. Validate model artifacts (model/*.{yaml,json})
        validateModelArtifacts(context.assets, errors, warnings)

        // 4. Validate gold bank (gold/gold_cards.json)
        validateGoldBank(context.assets, errors, warnings)

        // Determine overall status
        val criticalCount = errors.count { it.severity == Severity.CRITICAL }
        val errorCount = errors.count { it.severity == Severity.ERROR }
        val isValid = if (strictMode) errors.isEmpty() else criticalCount == 0

        val summary = buildString {
            append("Asset Validation: ")
            if (isValid) {
                append("✅ PASSED")
            } else {
                append("❌ FAILED")
            }
            append(" | Critical: $criticalCount, Errors: $errorCount, Warnings: ${warnings.size}")
        }

        return ValidationResult(isValid, errors, warnings, summary)
    }

    private fun validateBlueprints(assets: AssetManager, errors: MutableList<ValidationError>, warnings: MutableList<String>) {
        try {
            val blueprintFiles = assets.list("templates_v3") ?: emptyArray()
            if (blueprintFiles.isEmpty()) {
                errors.add(
                    ValidationError(
                        "templates_v3/",
                        Severity.CRITICAL,
                        "No blueprint files found",
                        "Generator V3 requires at least one blueprint file",
                    ),
                )
                return
            }

            val json = Json { ignoreUnknownKeys = true }
            var totalBlueprints = 0

            blueprintFiles.forEach { file ->
                if (!file.endsWith(".json")) return@forEach

                try {
                    val content = assets.open("templates_v3/$file").bufferedReader().use { it.readText() }
                    // Basic JSON validation
                    json.parseToJsonElement(content)

                    // Count blueprints
                    val count = content.count { it == '{' } - 1 // Approximate
                    totalBlueprints += count

                    // Check for required fields
                    val requiredFields = listOf("id", "game", "family", "blueprint", "constraints")
                    requiredFields.forEach { field ->
                        if (!content.contains("\"$field\"")) {
                            errors.add(
                                ValidationError(
                                    "templates_v3/$file",
                                    Severity.ERROR,
                                    "Missing required field: $field",
                                ),
                            )
                        }
                    }

                    // Deep validation: AB options refer to declared slots
                    runCatching {
                        val typed = json.decodeFromString(ListSerializer(TemplateBlueprint.serializer()), content)
                        typed.forEach { bp ->
                            val slotNames = bp.blueprint.mapNotNull { seg ->
                                when (seg) {
                                    is BlueprintSegment.Slot -> seg.name
                                    is BlueprintSegment.Text -> null
                                }
                            }.toSet()
                            val provider = bp.option_provider
                            if (provider != null && provider.type == BlueprintOptionProvider.OptionProviderType.AB) {
                                provider.options.forEachIndexed { idx, opt ->
                                    val src = opt.fromSlot
                                    if (src != null && src !in slotNames) {
                                        errors.add(
                                            ValidationError(
                                                "templates_v3/$file",
                                                Severity.ERROR,
                                                "Option ${idx + 1} refers to unknown slot '$src' in blueprint '${bp.id}'",
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }.onFailure { ex ->
                        warnings.add("Blueprint deep validation skipped for $file (${ex.message})")
                    }
                } catch (e: SerializationException) {
                    errors.add(
                        ValidationError(
                            "templates_v3/$file",
                            Severity.CRITICAL,
                            "Invalid JSON format",
                            e.message,
                        ),
                    )
                } catch (e: Exception) {
                    errors.add(
                        ValidationError(
                            "templates_v3/$file",
                            Severity.ERROR,
                            "Failed to parse blueprint file",
                            e.message,
                        ),
                    )
                }
            }

            if (totalBlueprints < 12) {
                warnings.add("Only $totalBlueprints blueprints found; recommend at least 12 for variety")
            }
        } catch (e: Exception) {
            errors.add(
                ValidationError(
                    "templates_v3/",
                    Severity.CRITICAL,
                    "Cannot access blueprint directory",
                    e.message,
                ),
            )
        }
    }

    private fun validateLexicons(assets: AssetManager, errors: MutableList<ValidationError>, warnings: MutableList<String>) {
        try {
            val lexiconFiles = assets.list("lexicons_v2") ?: emptyArray()
            if (lexiconFiles.isEmpty()) {
                errors.add(
                    ValidationError(
                        "lexicons_v2/",
                        Severity.CRITICAL,
                        "No lexicon files found",
                    ),
                )
                return
            }

            val json = Json { ignoreUnknownKeys = true }
            val criticalLexicons = setOf(
                "perks_plus.json",
                "gross_problem.json",
                "red_flag_issue.json",
                "meme_item.json",
                "categories.json",
                "letters.json",
                "secret_word.json",
            )

            lexiconFiles.forEach { file ->
                if (!file.endsWith(".json")) return@forEach

                try {
                    val content = assets.open("lexicons_v2/$file").bufferedReader().use { it.readText() }
                    json.parseToJsonElement(content)

                    // Check for slot_type and entries
                    if (!content.contains("\"slot_type\"")) {
                        errors.add(
                            ValidationError(
                                "lexicons_v2/$file",
                                Severity.ERROR,
                                "Missing slot_type field",
                            ),
                        )
                    }
                    if (!content.contains("\"entries\"")) {
                        errors.add(
                            ValidationError(
                                "lexicons_v2/$file",
                                Severity.CRITICAL,
                                "Missing entries array",
                            ),
                        )
                    }

                    // Count entries (approximate)
                    val entryCount = content.count { it == '{' } - 1
                    if (entryCount < 3 && file in criticalLexicons) {
                        warnings.add("Lexicon $file has only $entryCount entries; recommend at least 10")
                    }

                    // Deep validation of lexicon entries
                    runCatching {
                        val lex = json.decodeFromString(LexiconFile.serializer(), content)
                        val seen = mutableSetOf<String>()
                        lex.entries.forEachIndexed { idx, e ->
                            val key = e.text.trim().lowercase()
                            if (key.isEmpty()) {
                                errors.add(
                                    ValidationError(
                                        "lexicons_v2/$file",
                                        Severity.ERROR,
                                        "Empty entry text at index $idx",
                                    ),
                                )
                            }
                            val startsWithArticle = key.startsWith("a ") || key.startsWith("an ") || key.startsWith("the ") || key.startsWith("some ")
                            if (startsWithArticle && e.needs_article.lowercase() != "none") {
                                errors.add(
                                    ValidationError(
                                        "lexicons_v2/$file",
                                        Severity.ERROR,
                                        "Entry starts with an article but needs_article='${e.needs_article}'",
                                        e.text,
                                    ),
                                )
                            }
                            if (e.spice !in 0..3) {
                                errors.add(
                                    ValidationError(
                                        "lexicons_v2/$file",
                                        Severity.ERROR,
                                        "Spice out of range 0..3",
                                        e.text,
                                    ),
                                )
                            }
                            if (e.locality !in 1..3) {
                                errors.add(
                                    ValidationError(
                                        "lexicons_v2/$file",
                                        Severity.ERROR,
                                        "Locality out of range 1..3",
                                        e.text,
                                    ),
                                )
                            }
                            if (!seen.add(key)) {
                                warnings.add("Duplicate entry in $file: '${e.text}'")
                            }
                        }
                    }.onFailure { ex ->
                        warnings.add("Lexicon deep validation skipped for $file (${ex.message})")
                    }
                } catch (e: SerializationException) {
                    val severity = if (file in criticalLexicons) Severity.CRITICAL else Severity.ERROR
                    errors.add(
                        ValidationError(
                            "lexicons_v2/$file",
                            severity,
                            "Invalid JSON format",
                            e.message,
                        ),
                    )
                } catch (e: Exception) {
                    errors.add(
                        ValidationError(
                            "lexicons_v2/$file",
                            Severity.ERROR,
                            "Failed to parse lexicon file",
                            e.message,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            errors.add(
                ValidationError(
                    "lexicons_v2/",
                    Severity.CRITICAL,
                    "Cannot access lexicon directory",
                    e.message,
                ),
            )
        }
    }

    private fun validateModelArtifacts(assets: AssetManager, errors: MutableList<ValidationError>, warnings: MutableList<String>) {
        // Validate rules.yaml
        try {
            val rulesContent = assets.open("model/rules.yaml").bufferedReader().use { it.readText() }
            val yaml = Yaml()
            val data = yaml.load<Map<String, Any>>(rulesContent)

            val requiredKeys =
                listOf(
                    "version",
                    "coherence_threshold",
                    "max_attempts",
                    "max_repetition_ratio",
                    "min_word_count",
                    "max_word_count",
                )
            requiredKeys.forEach { key ->
                if (!data.containsKey(key)) {
                    errors.add(
                        ValidationError(
                            "model/rules.yaml",
                            Severity.CRITICAL,
                            "Missing required key: $key",
                        ),
                    )
                }
            }

            // Validate thresholds are reasonable
            (data["max_repetition_ratio"] as? Number)?.let { ratio ->
                if (ratio.toDouble() > 0.6) {
                    warnings.add("max_repetition_ratio > 0.6 may allow poor quality cards")
                }
            }
        } catch (e: Exception) {
            errors.add(
                ValidationError(
                    "model/rules.yaml",
                    Severity.CRITICAL,
                    "Failed to parse rules",
                    e.message,
                ),
            )
        }

        // Validate pairings.json
        try {
            val content = assets.open("model/pairings.json").bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            json.parseToJsonElement(content)

            if (!content.contains("\"pairs\"")) {
                errors.add(
                    ValidationError(
                        "model/pairings.json",
                        Severity.ERROR,
                        "Missing pairs array",
                    ),
                )
            }
        } catch (e: Exception) {
            errors.add(
                ValidationError(
                    "model/pairings.json",
                    Severity.ERROR,
                    "Failed to parse pairings",
                    e.message,
                ),
            )
        }

        // Validate priors.json
        try {
            val content = assets.open("model/priors.json").bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            json.parseToJsonElement(content)

            if (!content.contains("\"blueprints\"")) {
                warnings.add("priors.json missing blueprints array; will use defaults")
            }
        } catch (e: Exception) {
            warnings.add("Failed to parse priors.json: ${e.message}; will use defaults")
        }

        // Validate banned.json (optional)
        try {
            assets.open("model/banned.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            warnings.add("banned.json not found or invalid; no banned tokens will be filtered")
        }
    }

    private fun validateGoldBank(assets: AssetManager, errors: MutableList<ValidationError>, warnings: MutableList<String>) {
        try {
            val content = assets.open("gold/gold_cards.json").bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            json.parseToJsonElement(content)

            // Count gold cards (approximate)
            val cardCount = content.count { it == '{' } - 1
            if (cardCount < 20) {
                warnings.add("Gold bank has only $cardCount cards; recommend at least 50 for good coverage")
            }

            // Check for game coverage - 14 official games per HDRealRules.md
            val games = setOf(
                "ROAST_CONSENSUS", "CONFESSION_OR_CAP", "POISON_PITCH", "FILL_IN_FINISHER",
                "RED_FLAG_RALLY", "HOT_SEAT_IMPOSTER", "TEXT_THREAD_TRAP", "TABOO_TIMER",
                "THE_UNIFYING_THEORY", "TITLE_FIGHT", "ALIBI_DROP", "REALITY_CHECK",
                "SCATTERBLAST", "OVER_UNDER",
            )

            games.forEach { game ->
                if (!content.contains("\"$game\"")) {
                    warnings.add("No gold cards found for game: $game")
                }
            }
        } catch (e: SerializationException) {
            errors.add(
                ValidationError(
                    "gold/gold_cards.json",
                    Severity.CRITICAL,
                    "Invalid JSON format in gold bank",
                    e.message,
                ),
            )
        } catch (e: Exception) {
            errors.add(
                ValidationError(
                    "gold/gold_cards.json",
                    Severity.CRITICAL,
                    "Cannot load gold bank fallback",
                    e.message,
                ),
            )
        }
    }

    /**
     * Quick validation for essential assets only (minimal boot overhead).
     */
    fun validateEssentials(context: Context): Boolean {
        return try {
            // Just check critical files exist and are parseable
            context.assets.open("gold/gold_cards.json").close()
            context.assets.open("model/rules.yaml").close()
            true
        } catch (e: Exception) {
            Logger.e("Critical asset validation failed", e)
            false
        }
    }

    /**
     * Log validation result summary to console/logcat.
     */
    fun logValidationResult(result: ValidationResult) {
        if (result.isValid) {
            Logger.i("✅ ${result.summary}")
        } else {
            Logger.e("❌ ${result.summary}")
        }

        result.errors.forEach { error ->
            val prefix = when (error.severity) {
                Severity.CRITICAL -> "🚨 CRITICAL"
                Severity.ERROR -> "⚠️ ERROR"
                Severity.WARNING -> "⚠️ WARN"
            }
            Logger.w("$prefix [${error.asset}] ${error.message}${error.detail?.let { " | $it" } ?: ""}")
        }

        result.warnings.forEach { warning ->
            Logger.d("ℹ️ WARN: $warning")
        }
    }
}
