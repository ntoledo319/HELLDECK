package com.helldeck.content.tools

import android.content.Context
import com.helldeck.utils.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Template lint tool for V3 blueprints.
 * Validates templates_v3 JSON files for correctness.
 */
object TemplateLint {

    data class LintResult(
        val file: String,
        val errors: List<String>,
        val warnings: List<String>
    ) {
        val isClean: Boolean get() = errors.isEmpty() && warnings.isEmpty()
    }

    /**
     * Lint all V3 templates in assets/templates_v3/
     */
    fun lintAll(context: Context): List<LintResult> {
        val results = mutableListOf<LintResult>()

        try {
            val templateFiles = context.assets.list("templates_v3") ?: emptyArray()

            templateFiles.filter { it.endsWith(".json") }.forEach { filename ->
                results.add(lintFile(context, "templates_v3/$filename"))
            }
        } catch (e: Exception) {
            Logger.e("TemplateLint: Failed to list templates", e)
        }

        return results
    }

    /**
     * Lint a single template file
     */
    fun lintFile(context: Context, path: String): LintResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            val templates = json.parseToJsonElement(jsonString).jsonArray

            templates.forEachIndexed { index, templateElement ->
                val template = templateElement.jsonObject
                val templateId = template["id"]?.jsonPrimitive?.content ?: "unknown_$index"

                // Required fields
                if (!template.containsKey("id")) {
                    errors.add("[${templateId}] Missing required field: id")
                }
                if (!template.containsKey("game")) {
                    errors.add("[${templateId}] Missing required field: game")
                }
                if (!template.containsKey("blueprint")) {
                    errors.add("[${templateId}] Missing required field: blueprint")
                }

                // Validate blueprint
                template["blueprint"]?.jsonArray?.let { blueprint ->
                    if (blueprint.isEmpty()) {
                        errors.add("[${templateId}] Empty blueprint")
                    }

                    blueprint.forEach { segment ->
                        val segmentObj = segment.jsonObject
                        val type = segmentObj["type"]?.jsonPrimitive?.content

                        when (type) {
                            "text" -> {
                                val value = segmentObj["value"]?.jsonPrimitive?.content
                                if (value.isNullOrBlank()) {
                                    warnings.add("[${templateId}] Empty text segment")
                                }
                                // Check for unresolved placeholders
                                if (value?.contains("{") == true || value?.contains("}") == true) {
                                    errors.add("[${templateId}] Text segment contains placeholder: $value")
                                }
                            }
                            "slot" -> {
                                if (!segmentObj.containsKey("slot_type")) {
                                    errors.add("[${templateId}] Slot missing slot_type")
                                }
                                val slotType = segmentObj["slot_type"]?.jsonPrimitive?.content
                                if (slotType.isNullOrBlank()) {
                                    errors.add("[${templateId}] Empty slot_type")
                                }
                            }
                            null -> {
                                errors.add("[${templateId}] Blueprint segment missing type")
                            }
                        }
                    }
                }

                // Validate constraints
                template["constraints"]?.jsonObject?.let { constraints ->
                    val maxWords = constraints["max_words"]?.jsonPrimitive?.content?.toIntOrNull()
                    if (maxWords != null && maxWords > 100) {
                        warnings.add("[${templateId}] Very high max_words: $maxWords")
                    }
                    if (maxWords != null && maxWords < 5) {
                        warnings.add("[${templateId}] Very low max_words: $maxWords")
                    }
                }

                // Validate spice level
                val spiceMax = template["spice_max"]?.jsonPrimitive?.content?.toIntOrNull()
                if (spiceMax != null && (spiceMax < 1 || spiceMax > 3)) {
                    warnings.add("[${templateId}] Invalid spice_max: $spiceMax (should be 1-3)")
                }

                // Validate weight
                val weight = template["weight"]?.jsonPrimitive?.content?.toDoubleOrNull()
                if (weight != null && weight <= 0) {
                    warnings.add("[${templateId}] Invalid weight: $weight (should be > 0)")
                }

                // Check for game ID existence (basic check)
                val gameId = template["game"]?.jsonPrimitive?.content
                if (gameId != null && !isValidGameId(gameId)) {
                    errors.add("[${templateId}] Unknown game ID: $gameId")
                }
            }

        } catch (e: Exception) {
            errors.add("Failed to parse JSON: ${e.message}")
        }

        return LintResult(path, errors, warnings)
    }

    /**
     * Basic game ID validation (would check against GameMetadata in real implementation)
     */
    private fun isValidGameId(gameId: String): Boolean {
        val knownGameIds = setOf(
            "ROAST_CONSENSUS", "CONFESSION_OR_CAP", "POISON_PITCH", "FILL_IN_FINISHER",
            "RED_FLAG_RALLY", "HOT_SEAT_IMPOSTER", "TEXT_THREAD_TRAP", "TABOO_TIMER",
            "ODD_ONE_OUT", "TITLE_FIGHT", "ALIBI_DROP", "HYPE_OR_YIKE",
            "SCATTERBLAST", "MAJORITY_REPORT"
        )
        return knownGameIds.contains(gameId)
    }

    /**
     * Generate a human-readable report
     */
    fun generateReport(results: List<LintResult>): String {
        val sb = StringBuilder()
        sb.appendLine("=== Template Lint Report ===")
        sb.appendLine()

        val totalErrors = results.sumOf { it.errors.size }
        val totalWarnings = results.sumOf { it.warnings.size }
        val cleanFiles = results.count { it.isClean }

        sb.appendLine("Summary:")
        sb.appendLine("  Total files: ${results.size}")
        sb.appendLine("  Clean files: $cleanFiles")
        sb.appendLine("  Total errors: $totalErrors")
        sb.appendLine("  Total warnings: $totalWarnings")
        sb.appendLine()

        results.forEach { result ->
            if (!result.isClean) {
                sb.appendLine("File: ${result.file}")

                if (result.errors.isNotEmpty()) {
                    sb.appendLine("  Errors:")
                    result.errors.forEach { error ->
                        sb.appendLine("    ❌ $error")
                    }
                }

                if (result.warnings.isNotEmpty()) {
                    sb.appendLine("  Warnings:")
                    result.warnings.forEach { warning ->
                        sb.appendLine("    ⚠️  $warning")
                    }
                }

                sb.appendLine()
            }
        }

        if (totalErrors == 0 && totalWarnings == 0) {
            sb.appendLine("✅ All templates are valid!")
        }

        return sb.toString()
    }
}
