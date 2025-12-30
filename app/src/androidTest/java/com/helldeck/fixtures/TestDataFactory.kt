package com.helldeck.fixtures

import com.helldeck.content.db.TemplateStatEntity
import com.helldeck.engine.Feedback
import com.helldeck.data.PlayerEntity
import java.util.UUID

/**
 * Test data factory for creating test entities and data structures.
 * 
 * Provides helper methods to create test data for integration and unit tests.
 */
object TestDataFactory {
    
    /**
     * Creates a template stat entity for testing.
     */
    fun createTemplateStatEntity(
        templateId: String = "test_template_${UUID.randomUUID()}",
        visits: Int = 0,
        rewardSum: Double = 0.0,
    ): TemplateStatEntity {
        return TemplateStatEntity(
            templateId = templateId,
            visits = visits,
            rewardSum = rewardSum,
        )
    }
    
    /**
     * Creates a template entity list for testing.
     * Note: This creates TemplateStatEntity objects, not actual template entities.
     * For actual templates, use ContentRepository.
     */
    fun createTemplateEntityList(count: Int, game: String = "ROAST_CONSENSUS"): List<TemplateStatEntity> {
        return (1..count).map { i ->
            createTemplateStatEntity(
                templateId = "${game}_template_$i",
                visits = 0,
                rewardSum = 0.0,
            )
        }
    }
    
    /**
     * Creates a single template entity for testing.
     */
    fun createTemplateEntity(
        id: String = "test_template_${UUID.randomUUID()}",
        game: String = "ROAST_CONSENSUS",
    ): TemplateStatEntity {
        return createTemplateStatEntity(
            templateId = id,
            visits = 0,
            rewardSum = 0.0,
        )
    }
    
    /**
     * Creates a feedback object for testing.
     */
    fun createFeedback(
        lol: Int = 0,
        meh: Int = 0,
        trash: Int = 0,
        latencyMs: Long = 1000L,
        tags: Set<String> = emptySet(),
        comments: String = "",
    ): Feedback {
        return Feedback(
            lol = lol,
            meh = meh,
            trash = trash,
            latencyMs = latencyMs.toInt(),
            tags = tags,
            comments = comments,
        )
    }
    
    /**
     * Creates a player entity for testing.
     */
    fun createPlayerEntity(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Player",
        avatar: String = "😀",
        sessionPoints: Int = 0,
        totalPoints: Int = 0,
    ): PlayerEntity {
        return PlayerEntity(
            id = id,
            name = name,
            avatar = avatar,
            sessionPoints = sessionPoints,
            totalPoints = totalPoints,
            elo = 1000,
            gamesPlayed = 0,
            wins = 0,
            afk = 0,
            heatRounds = 0,
            quickLaughs = 0,
            lolCount = 0,
            mehCount = 0,
            trashCount = 0,
        )
    }
}

