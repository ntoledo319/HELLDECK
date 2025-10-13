package com.helldeck.fixtures

import com.helldeck.data.*
import com.helldeck.engine.Feedback
import kotlin.random.Random

/**
 * Factory class for creating test data entities
 */
object TestDataFactory {

    fun createTemplateEntity(
        id: String = "test_template_${Random.nextInt()}",
        game: String = "ROAST_CONSENSUS",
        text: String = "Test template with {slot1} and {slot2}",
        family: String = "test_family",
        spice: Int = 1,
        locality: Int = 1,
        maxWords: Int = 16
    ) = TemplateEntity(
        id = id,
        game = game,
        text = text,
        family = family,
        spice = spice,
        locality = locality,
        maxWords = maxWords
    )

    fun createPlayerEntity(
        id: String = "player_${Random.nextInt()}",
        name: String = "Test Player",
        avatar: String = "😀",
        sessionPoints: Int = 0
    ) = PlayerEntity(
        id = id,
        name = name,
        avatar = avatar,
        sessionPoints = sessionPoints
    )

    fun createRoundEntity(
        id: Long = 0L,
        sessionId: Long = 1L,
        templateId: String = "test_template",
        game: String = "ROAST_CONSENSUS",
        filledText: String = "Test filled text",
        feedback: Feedback = createFeedback(),
        points: Int = 2,
        timestamp: Long = System.currentTimeMillis()
    ) = RoundEntity(
        id = id,
        sessionId = sessionId,
        templateId = templateId,
        game = game,
        filledText = filledText,
        feedbackJson = com.google.gson.Gson().toJson(feedback),
        points = points,
        timestamp = timestamp
    )

    fun createFeedback(
        lol: Int = 1,
        meh: Int = 0,
        trash: Int = 0,
        latencyMs: Long = 1000L
    ) = Feedback(
        lol = lol,
        meh = meh,
        trash = trash,
        latencyMs = latencyMs
    )

    fun createGameSessionEntity(
        id: Long = 0L,
        startTime: Long = System.currentTimeMillis(),
        endTime: Long? = null,
        playerCount: Int = 2,
        roundsPlayed: Int = 0,
        gamesPlayed: String = "[]",
        totalPoints: Int = 0,
        brainpackExported: Int = 0
    ) = GameSessionEntity(
        id = id,
        startTime = startTime,
        endTime = endTime,
        playerCount = playerCount,
        roundsPlayed = roundsPlayed,
        gamesPlayed = gamesPlayed,
        totalPoints = totalPoints,
        brainpackExported = brainpackExported
    )

    fun createCommentEntity(
        id: Long = 0L,
        roundId: Long = 1L,
        text: String = "Test comment",
        tags: String = "test,comment",
        createdAt: Long = System.currentTimeMillis()
    ) = CommentEntity(
        id = id,
        roundId = roundId,
        text = text,
        tags = tags,
        createdAt = createdAt
    )

    fun createLexiconEntity(
        id: Long = 0L,
        name: String = "test_lexicon",
        words: String = "[\"word1\",\"word2\",\"word3\"]",
        updatedTs: Long = System.currentTimeMillis()
    ) = LexiconEntity(
        id = id,
        name = name,
        words = words,
        updatedTs = updatedTs
    )

    /**
     * Create a list of template entities for testing
     */
    fun createTemplateEntityList(count: Int = 5, game: String = "ROAST_CONSENSUS"): List<TemplateEntity> {
        return (1..count).map { i ->
            createTemplateEntity(
                id = "template_$i",
                game = game,
                text = "Template $i with {slot$i}",
                family = "family_${i % 3}"
            )
        }
    }

    /**
     * Create a list of player entities for testing
     */
    fun createPlayerEntityList(count: Int = 3): List<PlayerEntity> {
        val avatars = listOf("😀", "😎", "🤠", "🥳", "🤖")
        return (1..count).map { i ->
            createPlayerEntity(
                id = "player_$i",
                name = "Player $i",
                avatar = avatars[i % avatars.size],
                sessionPoints = i * 10
            )
        }
    }

    /**
     * Create a complete test scenario with players, templates, and rounds
     */
    fun createTestScenario(
        playerCount: Int = 3,
        templateCount: Int = 5,
        roundCount: Int = 2
    ): TestScenario {
        val players = createPlayerEntityList(playerCount)
        val templates = createTemplateEntityList(templateCount)
        val rounds = (1..roundCount).map { i ->
            createRoundEntity(
                id = i.toLong(),
                templateId = templates[i % templates.size].id,
                filledText = "Filled text for round $i",
                points = i * 2
            )
        }

        return TestScenario(players, templates, rounds)
    }

    /**
     * Data class to hold complete test scenarios
     */
    data class TestScenario(
        val players: List<PlayerEntity>,
        val templates: List<TemplateEntity>,
        val rounds: List<RoundEntity>
    )
}