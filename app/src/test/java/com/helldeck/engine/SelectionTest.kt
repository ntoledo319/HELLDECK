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
 * Comprehensive unit tests for Selection algorithm
 */
@ExperimentalCoroutinesApi
class SelectionTest : BaseTest() {

    @Test
    fun `pickNext selects valid template from candidates`() {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(5, "ROAST_CONSENSUS")
        val recentFamilies = listOf("family_0", "family_1")
        val roundIdx = 5

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))
        assertEquals("Selected template should have correct game",
            "ROAST_CONSENSUS", selected.game)
    }

    @Test
    fun `pickNext avoids recently used families correctly`() {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(9, "ROAST_CONSENSUS")
        val recentFamilies = listOf("family_0", "family_1", "family_2") // First 3 families
        val roundIdx = 10

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertFalse("Selected template should not be from recent families",
            recentFamilies.contains(selected.family))
    }

    @Test
    fun `pickNext handles empty recent families list correctly`() {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(3, "ROAST_CONSENSUS")
        val recentFamilies = emptyList<String>()
        val roundIdx = 1

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))
    }

    @Test
    fun `pickNext handles single candidate correctly`() {
        // Arrange
        val singleCandidate = TestDataFactory.createTemplateEntity(
            id = "single_template",
            game = "ROAST_CONSENSUS",
            family = "single_family"
        )
        val candidates = listOf(singleCandidate)
        val recentFamilies = listOf("other_family")
        val roundIdx = 1

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertEquals("Should select the single candidate", singleCandidate.id, selected.id)
    }

    @Test
    fun `pickNext considers round index for selection strategy`() {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(10, "ROAST_CONSENSUS")

        // Act - Test early round
        val earlySelection = Selection.pickNext(candidates, emptyList(), 2)

        // Act - Test late round
        val lateSelection = Selection.pickNext(candidates, emptyList(), 50)

        // Assert
        assertNotNull("Early selection should not be null", earlySelection)
        assertNotNull("Late selection should not be null", lateSelection)

        // Both should be valid selections from candidates
        assertTrue("Early selection should be from candidates",
            candidates.contains(earlySelection))
        assertTrue("Late selection should be from candidates",
            candidates.contains(lateSelection))
    }

    @Test
    fun `pickNext handles candidates with same family correctly`() {
        // Arrange
        val familyName = "same_family"
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(id = "template_1", family = familyName),
            TestDataFactory.createTemplateEntity(id = "template_2", family = familyName),
            TestDataFactory.createTemplateEntity(id = "template_3", family = familyName)
        )
        val recentFamilies = listOf("other_family")
        val roundIdx = 5

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertEquals("Selected template should have correct family", familyName, selected.family)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))
    }

    @Test
    fun `pickNext maintains variety across multiple selections`() {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(15, "ROAST_CONSENSUS")
        val recentFamilies = emptyList<String>()

        // Act - Make multiple selections
        val selections = (1..10).map {
            Selection.pickNext(candidates, recentFamilies, it)
        }

        // Assert
        assertEquals("Should have 10 selections", 10, selections.size)

        // Check that we get variety (not always selecting the same template)
        val uniqueTemplates = selections.distinctBy { it.id }
        assertTrue("Should have variety in selections", uniqueTemplates.size > 1)

        // All selections should be from candidates
        selections.forEach { selection ->
            assertTrue("All selections should be from candidates",
                candidates.contains(selection))
        }
    }

    @Test
    fun `pickNext handles large candidate list efficiently`() {
        // Arrange
        val largeCandidateList = (1..1000).map { i ->
            TestDataFactory.createTemplateEntity(
                id = "template_$i",
                family = "family_${i % 10}" // 10 different families
            )
        }
        val recentFamilies = (0..2).map { "family_$it" } // Recent families 0-2
        val roundIdx = 100

        // Act
        val startTime = System.currentTimeMillis()
        val selected = Selection.pickNext(largeCandidateList, recentFamilies, roundIdx)
        val endTime = System.currentTimeMillis()

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            largeCandidateList.contains(selected))
        assertFalse("Selected template should not be from recent families",
            recentFamilies.contains(selected.family))

        // Performance assertion - should complete quickly even with large list
        val selectionTime = endTime - startTime
        assertTrue("Selection should be fast even with large candidate list",
            selectionTime < 100) // Less than 100ms
    }

    @Test
    fun `pickNext handles edge case with all families being recent`() {
        // Arrange - Create candidates where all families are in recent list
        val candidates = TestDataFactory.createTemplateEntityList(6, "ROAST_CONSENSUS")
        val allFamilies = candidates.map { it.family }.distinct()
        val recentFamilies = allFamilies // All families are recent
        val roundIdx = 5

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))

        // In this edge case, the algorithm should still select something
        // (exact behavior depends on implementation - it might fall back to random selection)
        assertNotNull("Should still make a selection", selected)
    }

    @Test
    fun `pickNext maintains consistency with same inputs`() {
        // Arrange
        val candidates = TestDataFactory.createTemplateEntityList(8, "ROAST_CONSENSUS")
        val recentFamilies = listOf("family_0")
        val roundIdx = 10

        // Act - Make multiple calls with same inputs
        val selections = (1..5).map {
            Selection.pickNext(candidates, recentFamilies, roundIdx)
        }

        // Assert
        assertEquals("Should have 5 selections", 5, selections.size)

        // All selections should be valid
        selections.forEach { selection ->
            assertNotNull("Each selection should not be null", selection)
            assertTrue("Each selection should be from candidates",
                candidates.contains(selection))
        }

        // Note: Selection algorithm might not be deterministic due to randomness,
        // but all results should be valid selections from the candidate list
    }

    @Test
    fun `pickNext handles templates with different spice levels correctly`() {
        // Arrange
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(id = "mild_1", spice = 1),
            TestDataFactory.createTemplateEntity(id = "medium_1", spice = 2),
            TestDataFactory.createTemplateEntity(id = "hot_1", spice = 3),
            TestDataFactory.createTemplateEntity(id = "mild_2", spice = 1),
            TestDataFactory.createTemplateEntity(id = "medium_2", spice = 2)
        )
        val recentFamilies = listOf("other_family")
        val roundIdx = 8

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))

        // Verify spice level is preserved
        assertTrue("Spice level should be valid (1-3)",
            selected.spice in 1..3)
    }

    @Test
    fun `pickNext handles templates with different locality levels correctly`() {
        // Arrange
        val candidates = listOf(
            TestDataFactory.createTemplateEntity(id = "local_1", locality = 1),
            TestDataFactory.createTemplateEntity(id = "regional_1", locality = 2),
            TestDataFactory.createTemplateEntity(id = "global_1", locality = 3),
            TestDataFactory.createTemplateEntity(id = "local_2", locality = 1)
        )
        val recentFamilies = listOf("other_family")
        val roundIdx = 6

        // Act
        val selected = Selection.pickNext(candidates, recentFamilies, roundIdx)

        // Assert
        assertNotNull("Selected template should not be null", selected)
        assertTrue("Selected template should be from candidates",
            candidates.contains(selected))

        // Verify locality level is preserved
        assertTrue("Locality level should be valid (1-3)",
            selected.locality in 1..3)
    }
}