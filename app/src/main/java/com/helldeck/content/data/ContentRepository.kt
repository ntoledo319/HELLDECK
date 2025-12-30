package com.helldeck.content.data

import android.content.Context
import com.helldeck.content.db.HelldeckDb
import com.helldeck.content.db.TemplateExposureEntity
import com.helldeck.content.model.v2.TemplateV2
import com.helldeck.content.validation.TemplateCatalogueAuditor
import com.helldeck.utils.Logger
import kotlinx.coroutines.runBlocking

/**
 * ContentRepository provides access to all game content and data.
 *
 * This repository serves as the main data access layer for:
 * - TemplateV2 objects from assets
 * - Lexicon word lists
 * - Template exposure tracking
 * - Template statistics
 *
 * Key features:
 * - Asset-based content loading
 * - Template exposure tracking for diversity
 * - Lexicon aliasing for backward compatibility
 * - Database integration for persistence
 *
 * @param context Android application context for accessing assets and database
 */
class ContentRepository(
    private val context: Context,
) {
    private val assets = context.assets
    val db = HelldeckDb.get(context)

    private val lexicons = LexiconRepository(assets)
    private val templatesV2Repo = TemplateRepositoryV2(assets)

    // Lexicon alias mapping for backward compatibility
    private val aliasMap = mapOf(
        // Legacy singular keys -> pluralized lexicons
        "meme" to "memes",
        "perk" to "perks",
        "red_flag" to "red_flags",
        "friend" to "friends",
        "place" to "places",
        "letter" to "letters",
        "guilty_prompt" to "guilty_prompts",
        "inbound_text" to "inbound_texts",
        "social_disaster" to "social_disasters",
    )

    /**
     * Initializes the repository.
     *
     * Currently a no-op as assets are the source of truth.
     * Could be extended to handle database migrations or version updates.
     */
    fun initialize() {
        val templates = templatesV2Repo.loadAll()
        val issues = TemplateCatalogueAuditor.audit(templates, lexicons)
        if (issues.isNotEmpty()) {
            issues.forEach { issue ->
                Logger.w("Template audit: ${issue.templateId} -> ${issue.message}")
            }
        }
    }

    /**
     * Gets all available TemplateV2 objects.
     *
     * @return List of all templates loaded from assets
     */
    fun templatesV2(): List<TemplateV2> = templatesV2Repo.loadAll()

    /**
     * Gets words from a lexicon by key.
     *
     * Supports lexicon aliases for backward compatibility.
     *
     * @param key The lexicon key (supports aliases like "meme")
     * @return List of words from the specified lexicon
     */
    fun wordsFor(key: String): List<String> {
        val actualKey = aliasMap[key] ?: key
        return lexicons.wordsFor(actualKey)
    }

    /**
     * Gets recently used template IDs within a time horizon.
     *
     * Used for diversity tracking to avoid immediate repetition.
     *
     * @param horizon Time horizon in minutes to look back
     * @return Set of template IDs used within the horizon
     */
    fun recentHistoryIds(horizon: Int): Set<String> = runBlocking {
        val cutoff = System.currentTimeMillis() - (horizon * 60 * 1000L) // horizon in minutes
        db.templateExposureDao().getRecentIds(cutoff, horizon).toSet()
    }

    /**
     * Records exposure to a specific template.
     *
     * Tracks when templates are shown to enable diversity algorithms.
     *
     * @param templateId The template ID that was shown
     */
    fun addExposure(templateId: String) = runBlocking {
        db.templateExposureDao().insert(
            TemplateExposureEntity(
                templateId = templateId,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Gets access to template statistics DAO.
     *
     * @return TemplateStatDao for accessing template performance data
     */
    val statsDao get() = db.templateStatDao()
}
