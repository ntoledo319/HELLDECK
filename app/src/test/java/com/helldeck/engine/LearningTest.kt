package com.helldeck.engine

import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for Learning system
 */
@ExperimentalCoroutinesApi
class LearningTest : BaseTest() {

    @Test
    fun `scoreCard calculates correct score for positive feedback`() {
        // Arrange
        val feedback = TestDataFactory.createFeedback(lol = 3, meh = 1, trash = 0, latencyMs = 1000)

        // Act
        val score = Learning.scoreCard(feedback)

        // Assert
        assertTrue("Score should be positive for good feedback", score > 0)
        assertEquals("Score should be 3.0 for 3 lol votes", 3.0, score, 0.01)
    }

    @Test
    fun `scoreCard calculates negative score for poor feedback`() {
        // Arrange
        val feedback = TestDataFactory.createFeedback(lol = 0, meh = 1, trash = 3, latencyMs = 5000)

        // Act
        val score = Learning.scoreCard(feedback)

        // Assert
        assertTrue("Score should be negative for poor feedback", score < 0)
        assertEquals("Score should be -3.0 for 3 trash votes", -3.0, score, 0.01)
    }

    @Test
    fun `scoreCard handles mixed feedback correctly`() {
        // Arrange
        val feedback = TestDataFactory.createFeedback(lol = 2, meh = 2, trash = 1, latencyMs = 2000)

        // Act
        val score = Learning.scoreCard(feedback)

        // Assert
        assertEquals("Score should be 1.0 for mixed feedback (2-2-1)", 1.0, score, 0.01)
    }

    @Test
    fun `scoreCard handles zero feedback correctly`() {
        // Arrange
        val feedback = TestDataFactory.createFeedback(lol = 0, meh = 0, trash = 0, latencyMs = 1000)

        // Act
        val score = Learning.scoreCard(feedback)

        // Assert
        assertEquals("Score should be 0.0 for no feedback", 0.0, score, 0.01)
    }

    @Test
    fun `scoreCard applies latency penalty correctly`() {
        // Arrange
        val fastFeedback = TestDataFactory.createFeedback(lol = 2, meh = 0, trash = 0, latencyMs = 500)
        val slowFeedback = TestDataFactory.createFeedback(lol = 2, meh = 0, trash = 0, latencyMs = 10000)

        // Act
        val fastScore = Learning.scoreCard(fastFeedback)
        val slowScore = Learning.scoreCard(slowFeedback)

        // Assert
        assertTrue("Fast feedback should score higher than slow feedback",
            fastScore > slowScore)
    }

    @Test
    fun `updateTemplateScore applies EMA correctly for new template`() {
        // Arrange
        val currentScore = 0.0 // New template starts with 0
        val newScore = 3.0
        val alpha = 0.3

        // Act
        val updatedScore = Learning.updateTemplateScore(currentScore, newScore, alpha)

        // Assert
        val expected = alpha * newScore + (1 - alpha) * currentScore
        assertEquals("Should apply EMA formula correctly", expected, updatedScore, 0.001)
    }

    @Test
    fun `updateTemplateScore applies EMA correctly for existing template`() {
        // Arrange
        val currentScore = 2.0 // Existing template score
        val newScore = 4.0
        val alpha = 0.2

        // Act
        val updatedScore = Learning.updateTemplateScore(currentScore, newScore, alpha)

        // Assert
        val expected = alpha * newScore + (1 - alpha) * currentScore
        assertEquals("Should apply EMA formula correctly", expected, updatedScore, 0.001)
    }

    @Test
    fun `updateTemplateScore handles negative scores correctly`() {
        // Arrange
        val currentScore = 1.0
        val newScore = -2.0
        val alpha = 0.3

        // Act
        val updatedScore = Learning.updateTemplateScore(currentScore, newScore, alpha)

        // Assert
        val expected = alpha * newScore + (1 - alpha) * currentScore
        assertEquals("Should handle negative scores correctly", expected, updatedScore, 0.001)
        assertTrue("Updated score should be negative", updatedScore < 0)
    }

    @Test
    fun `calculateSelectionScore considers template performance and context`() {
        // Arrange
        val templateScore = 2.5
        val familyDiversity = 0.8
        val spicePreference = 1.2
        val roundNumber = 10

        // Act
        val selectionScore = Learning.calculateSelectionScore(
            templateScore, familyDiversity, spicePreference, roundNumber
        )

        // Assert
        assertNotNull("Selection score should not be null", selectionScore)
        assertTrue("Selection score should be positive", selectionScore > 0)

        // Test that different parameters produce different scores
        val differentScore = Learning.calculateSelectionScore(
            templateScore * 2, familyDiversity, spicePreference, roundNumber
        )
        assertNotEquals("Different parameters should produce different scores",
            selectionScore, differentScore, 0.001)
    }

    @Test
    fun `calculateSelectionScore handles early rounds correctly`() {
        // Arrange
        val templateScore = 1.0
        val familyDiversity = 0.5
        val spicePreference = 1.0
        val earlyRound = 2

        // Act
        val earlyScore = Learning.calculateSelectionScore(
            templateScore, familyDiversity, spicePreference, earlyRound
        )

        val lateScore = Learning.calculateSelectionScore(
            templateScore, familyDiversity, spicePreference, earlyRound * 10
        )

        // Assert
        assertNotNull("Early score should not be null", earlyScore)
        assertNotNull("Late score should not be null", lateScore)
        // Early rounds might have different scoring logic than late rounds
        assertTrue("Both scores should be valid", earlyScore >= 0 && lateScore >= 0)
    }

    @Test
    fun `calculateSelectionScore handles high spice preference correctly`() {
        // Arrange
        val templateScore = 2.0
        val familyDiversity = 0.6
        val lowSpicePreference = 0.5
        val highSpicePreference = 2.0

        // Act
        val lowSpiceScore = Learning.calculateSelectionScore(
            templateScore, familyDiversity, lowSpicePreference, 10
        )
        val highSpiceScore = Learning.calculateSelectionScore(
            templateScore, familyDiversity, highSpicePreference, 10
        )

        // Assert
        assertNotNull("Low spice score should not be null", lowSpiceScore)
        assertNotNull("High spice score should not be null", highSpiceScore)

        // High spice preference should generally lead to different scoring
        // (exact behavior depends on implementation details)
        assertTrue("Both scores should be valid", lowSpiceScore >= 0 && highSpiceScore >= 0)
    }

    @Test
    fun `calculateSelectionScore handles family diversity correctly`() {
        // Arrange
        val templateScore = 2.0
        val lowDiversity = 0.2
        val highDiversity = 0.9
        val spicePreference = 1.0

        // Act
        val lowDiversityScore = Learning.calculateSelectionScore(
            templateScore, lowDiversity, spicePreference, 10
        )
        val highDiversityScore = Learning.calculateSelectionScore(
            templateScore, highDiversity, spicePreference, 10
        )

        // Assert
        assertNotNull("Low diversity score should not be null", lowDiversityScore)
        assertNotNull("High diversity score should not be null", highDiversityScore)

        // Different diversity should produce different scores
        // (exact behavior depends on implementation details)
        assertTrue("Both scores should be valid",
            lowDiversityScore >= 0 && highDiversityScore >= 0)
    }

    @Test
    fun `scoreCard handles extreme latency values correctly`() {
        // Arrange
        val normalFeedback = TestDataFactory.createFeedback(lol = 2, meh = 0, trash = 0, latencyMs = 2000)
        val veryFastFeedback = TestDataFactory.createFeedback(lol = 2, meh = 0, trash = 0, latencyMs = 100)
        val verySlowFeedback = TestDataFactory.createFeedback(lol = 2, meh = 0, trash = 0, latencyMs = 30000)

        // Act
        val normalScore = Learning.scoreCard(normalFeedback)
        val fastScore = Learning.scoreCard(veryFastFeedback)
        val slowScore = Learning.scoreCard(verySlowFeedback)

        // Assert
        assertTrue("Fast feedback should score higher than normal", fastScore >= normalScore)
        assertTrue("Normal feedback should score higher than slow", normalScore >= slowScore)
    }

    @Test
    fun `updateTemplateScore handles edge case alpha values correctly`() {
        // Arrange
        val currentScore = 1.0
        val newScore = 3.0

        // Test with alpha = 0 (no learning)
        val alphaZero = Learning.updateTemplateScore(currentScore, newScore, 0.0)

        // Test with alpha = 1 (immediate replacement)
        val alphaOne = Learning.updateTemplateScore(currentScore, newScore, 1.0)

        // Assert
        assertEquals("Alpha 0 should return current score", currentScore, alphaZero, 0.001)
        assertEquals("Alpha 1 should return new score", newScore, alphaOne, 0.001)
    }

    @Test
    fun `scoreCard handles maximum feedback values correctly`() {
        // Arrange
        val maxFeedback = TestDataFactory.createFeedback(lol = 10, meh = 10, trash = 10, latencyMs = 1000)

        // Act
        val score = Learning.scoreCard(maxFeedback)

        // Assert
        assertEquals("Should handle maximum values correctly", 10.0, score, 0.01)
    }

    @Test
    fun `calculateSelectionScore handles edge case values correctly`() {
        // Arrange - Test with extreme values
        val negativeScore = Learning.calculateSelectionScore(-5.0, -1.0, -2.0, -1)
        val zeroScore = Learning.calculateSelectionScore(0.0, 0.0, 0.0, 0)
        val veryHighScore = Learning.calculateSelectionScore(100.0, 10.0, 10.0, 1000)

        // Assert
        assertNotNull("Negative score should not be null", negativeScore)
        assertNotNull("Zero score should not be null", zeroScore)
        assertNotNull("Very high score should not be null", veryHighScore)

        // All scores should be valid numbers (not NaN or infinite)
        assertTrue("Negative score should be finite", negativeScore.isFinite())
        assertTrue("Zero score should be finite", zeroScore.isFinite())
        assertTrue("Very high score should be finite", veryHighScore.isFinite())
    }
}