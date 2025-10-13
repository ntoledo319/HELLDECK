package com.helldeck.engine

import com.helldeck.data.TemplateEntity
import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/**
 * Comprehensive unit tests for SmartLearning system
 */
@ExperimentalCoroutinesApi
class SmartLearningTest : BaseTest() {

    @Test
    fun `selectTemplate with valid candidates returns template`() = runTest {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(5, "ROAST_CONSENSUS")
        val gameType = "ROAST_CONSENSUS"

        // Act
        val selected = SmartLearning.selectTemplate(candidates, gameType)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))
        assertEquals("Selected template should have correct game",
            gameType, selected.game)
    }

    @Test
    fun `selectTemplate with empty candidates throws exception`() = runTest {
        // Arrange
        val candidates = emptyList<TemplateEntity>()
        val gameType = "ROAST_CONSENSUS"

        // Act & Assert
        try {
            SmartLearning.selectTemplate(candidates, gameType)
            fail("Should have thrown exception for empty candidates")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention candidates",
                e.message?.contains("No template candidates available") == true)
        }
    }

    @Test
    fun `selectTemplate with single candidate returns that candidate`() = runTest {
        // Arrange
        val singleCandidate = TestDataFactory.createTemplateEntity(
            id = "single_template",
            game = "ROAST_CONSENSUS",
            family = "single_family"
        )
        val candidates = listOf(singleCandidate)
        val gameType = "ROAST_CONSENSUS"

        // Act
        val selected = SmartLearning.selectTemplate(candidates, gameType)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertEquals("Should select the single candidate", singleCandidate.id, selected.id)
    }

    @Test
    fun `selectTemplate balances exploration and exploitation over time`() = runTest {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(10, "ROAST_CONSENSUS")
        val gameType = "ROAST_CONSENSUS"

        // Act - Make multiple selections to test exploration/exploitation balance
        val selections = (1..50).map {
            SmartLearning.selectTemplate(candidates, gameType)
        }

        // Assert
        assertEquals("Should have 50 selections", 50, selections.size)

        // Check for variety (exploration)
        val uniqueTemplates = selections.distinctBy { it.id }
        assertTrue("Should have variety through exploration",
            uniqueTemplates.size > 1)

        // Check that some templates are selected more often (exploitation)
        val selectionCounts = selections.groupingBy { it.id }.eachCount()
        val maxSelections = selectionCounts.values.maxOrNull() ?: 0
        val minSelections = selectionCounts.values.minOrNull() ?: 0

        // There should be some imbalance due to exploitation
        assertTrue("Should have some exploitation (imbalanced selection)",
            maxSelections > minSelections)
    }

    @Test
    fun `selectTemplate handles different game types correctly`() = runTest {
        // Arrange
        val roastCandidates = TestDataFactory.createTemplateEntityList(3, "ROAST_CONSENSUS")
        val confessionCandidates = TestDataFactory.createTemplateEntityList(3, "CONFESSION_OR_CAP")

        // Act
        val roastSelection = SmartLearning.selectTemplate(roastCandidates, "ROAST_CONSENSUS")
        val confessionSelection = SmartLearning.selectTemplate(confessionCandidates, "CONFESSION_OR_CAP")

        // Assert
        assertNotNull("Roast selection should not be null", roastSelection)
        assertNotNull("Confession selection should not be null", confessionSelection)

        assertEquals("Roast selection should have correct game",
            "ROAST_CONSENSUS", roastSelection.game)
        assertEquals("Confession selection should have correct game",
            "CONFESSION_OR_CAP", confessionSelection.game)

        assertTrue("Roast selection should be from roast candidates",
            roastCandidates.contains(roastSelection))
        assertTrue("Confession selection should be from confession candidates",
            confessionCandidates.contains(confessionSelection))
    }

    @Test
    fun `selectTemplate maintains performance with large candidate lists`() = runTest {
        // Arrange
        val largeCandidateList = (1..500).map { i ->
            TestDataFactory.createTemplateEntity(
                id = "template_$i",
                family = "family_${i % 20}" // 20 different families
            )
        }
        val gameType = "ROAST_CONSENSUS"

        // Act
        val startTime = System.currentTimeMillis()
        val selected = SmartLearning.selectTemplate(largeCandidateList, gameType)
        val endTime = System.currentTimeMillis()

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            largeCandidateList.contains(selected))

        // Performance assertion
        val selectionTime = endTime - startTime
        assertTrue("Selection should be fast even with large candidate list",
            selectionTime < 200) // Less than 200ms for 500 candidates
    }

    @Test
    fun `selectTemplate handles templates with varying performance scores correctly`() = runTest {
        // Arrange - Create templates with different characteristics
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(id = "high_performer", spice = 3, locality = 3),
            TestDataFactory.createTemplateEntity(id = "medium_performer", spice = 2, locality = 2),
            TestDataFactory.createTemplateEntity(id = "low_performer", spice = 1, locality = 1),
            TestDataFactory.createTemplateEntity(id = "wildcard", spice = 2, locality = 3)
        )
        val gameType = "ROAST_CONSENSUS"

        // Act - Make multiple selections to see if algorithm favors better performers
        val selections = (1..30).map {
            SmartLearning.selectTemplate(candidates, gameType)
        }

        // Assert
        assertEquals("Should have 30 selections", 30, selections.size)

        // All selections should be from candidates
        selections.forEach { selection ->
            assertTrue("All selections should be from candidates",
                candidates.contains(selection))
        }

        // Check that we get a good distribution
        val uniqueSelections = selections.distinctBy { it.id }
        assertTrue("Should explore different templates",
            uniqueSelections.size >= 2)
    }

    @Test
    fun `selectTemplate handles family diversity correctly over multiple rounds`() = runTest {
        // Arrange - Create candidates with limited family diversity
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(id = "family1_t1", family = "family1"),
            TestDataFactory.createTemplateEntity(id = "family1_t2", family = "family1"),
            TestDataFactory.createTemplateEntity(id = "family2_t1", family = "family2"),
            TestDataFactory.createTemplateEntity(id = "family3_t1", family = "family3")
        )
        val gameType = "ROAST_CONSENSUS"

        // Act - Make selections that would trigger family diversity concerns
        val selections = (1..20).map {
            SmartLearning.selectTemplate(candidates, gameType)
        }

        // Assert
        assertEquals("Should have 20 selections", 20, selections.size)

        // Check family distribution
        val familyDistribution = selections.groupingBy { it.family }.eachCount()

        // Should have explored multiple families (not stuck on one)
        assertTrue("Should explore multiple families",
            familyDistribution.size > 1)

        // All selections should be from candidates
        selections.forEach { selection ->
            assertTrue("All selections should be from candidates",
                candidates.contains(selection))
        }
    }

    @Test
    fun `selectTemplate handles edge case with very similar templates correctly`() = runTest {
        // Arrange - Create very similar templates
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(
                id = "similar_1",
                text = "Template {slot} one",
                family = "similar",
                spice = 1,
                locality = 1
            ),
            TestDataFactory.createTemplateEntity(
                id = "similar_2",
                text = "Template {slot} two",
                family = "similar",
                spice = 1,
                locality = 1
            ),
            TestDataFactory.createTemplateEntity(
                id = "similar_3",
                text = "Template {slot} three",
                family = "similar",
                spice = 1,
                locality = 1
            )
        )
        val gameType = "ROAST_CONSENSUS"

        // Act
        val selected = SmartLearning.selectTemplate(candidates, gameType)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))
        assertEquals("Selected template should have correct family",
            "similar", selected.family)
    }

    @Test
    fun `selectTemplate maintains reasonable selection frequency distribution`() = runTest {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(8, "ROAST_CONSENSUS")
        val gameType = "ROAST_CONSENSUS"

        // Act - Make many selections
        val selections = (1..100).map {
            SmartLearning.selectTemplate(candidates, gameType)
        }

        // Assert
        assertEquals("Should have 100 selections", 100, selections.size)

        // Analyze selection distribution
        val selectionCounts = selections.groupingBy { it.id }.eachCount()

        // Every template should be selected at least once (exploration)
        assertEquals("All templates should be selected at least once",
            candidates.size, selectionCounts.size)

        // Check that no template dominates completely (should be some balance)
        val maxSelections = selectionCounts.values.maxOrNull() ?: 0
        val totalSelections = selections.size

        // No single template should be selected more than 50% of the time
        // (allowing for some exploitation but maintaining diversity)
        assertTrue("No template should dominate completely",
            maxSelections < totalSelections * 0.5)
    }

    @Test
    fun `selectTemplate handles game type filtering correctly`() = runTest {
        // Arrange - Mix of different game types
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(id = "roast_1", game = "ROAST_CONSENSUS"),
            TestDataFactory.createTemplateEntity(id = "confession_1", game = "CONFESSION_OR_CAP"),
            TestDataFactory.createTemplateEntity(id = "roast_2", game = "ROAST_CONSENSUS"),
            TestDataFactory.createTemplateEntity(id = "poison_1", game = "POISON_PITCH")
        )

        // Act
        val roastSelection = SmartLearning.selectTemplate(candidates, "ROAST_CONSENSUS")
        val confessionSelection = SmartLearning.selectTemplate(candidates, "CONFESSION_OR_CAP")

        // Assert
        assertNotNull("Roast selection should not be null", roastSelection)
        assertNotNull("Confession selection should not be null", confessionSelection)

        assertEquals("Roast selection should have correct game",
            "ROAST_CONSENSUS", roastSelection.game)
        assertEquals("Confession selection should have correct game",
            "CONFESSION_OR_CAP", confessionSelection.game)

        // Verify selections come from correct candidate pools
        val roastCandidates = candidates.filter { it.game == "ROAST_CONSENSUS" }
        val confessionCandidates = candidates.filter { it.game == "CONFESSION_OR_CAP" }

        assertTrue("Roast selection should be from roast candidates",
            roastCandidates.contains(roastSelection))
        assertTrue("Confession selection should be from confession candidates",
            confessionCandidates.contains(confessionSelection))
    }

    @Test
    fun `selectTemplate handles performance with extreme template characteristics`() = runTest {
        // Arrange - Templates with extreme characteristics
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(id = "extreme_1", spice = 3, locality = 3, maxWords = 50),
            TestDataFactory.createTemplateEntity(id = "extreme_2", spice = 1, locality = 1, maxWords = 5),
            TestDataFactory.createTemplateEntity(id = "normal_1", spice = 2, locality = 2, maxWords = 16)
        )
        val gameType = "ROAST_CONSENSUS"

        // Act
        val selected = SmartLearning.selectTemplate(candidates, gameType)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))

        // Verify characteristics are preserved
        assertTrue("Spice should be valid", selected.spice in 1..3)
        assertTrue("Locality should be valid", selected.locality in 1..3)
        assertTrue("MaxWords should be valid", selected.maxWords > 0)
    }
}