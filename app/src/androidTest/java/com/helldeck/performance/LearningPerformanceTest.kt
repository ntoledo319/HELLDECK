package com.helldeck.performance

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.engine.Selection
import com.helldeck.engine.SmartLearning
import com.helldeck.fixtures.TestDataFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance tests for learning algorithms
 */
@RunWith(AndroidJUnit4::class)
class LearningPerformanceTest {

    @Test
    fun `templateSelectionPerformance with large dataset completes quickly`() {
        // Arrange
        val candidates = (1..1000).map { i ->
            TestDataFactory.createTemplateEntity(
                id = "template_$i",
                family = "family_${i % 20}"
            )
        }
        val gameType = "ROAST_CONSENSUS"

        // Act
        val startTime = System.currentTimeMillis()
        
        runBlocking {
            repeat(100) {
                val selected = SmartLearning.selectTemplate(candidates, gameType)
                assertNotNull("Selected template should not be null", selected)
            }
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Assert - Performance should be acceptable
        assertTrue("Template selection with 1000 candidates should complete in <2s (actual: ${totalTime}ms)",
            totalTime < 2000)
    }

    @Test
    fun `selectionAlgorithmPerformance with varied datasets`() {
        // Arrange
        val smallDataset = TestDataFactory.createTemplateEntityList(10)
        val mediumDataset = TestDataFactory.createTemplateEntityList(100)
        val largeDataset = TestDataFactory.createTemplateEntityList(500)

        // Act & Assert - Small dataset
        val smallStartTime = System.currentTimeMillis()
        repeat(100) {
            val selected = Selection.pickNext(smallDataset, emptyList(), it)
            assertNotNull("Selected template should not be null", selected)
        }
        val smallTime = System.currentTimeMillis() - smallStartTime
        assertTrue("Small dataset selection should be very fast (<100ms): ${smallTime}ms", smallTime < 100)

        // Medium dataset
        val mediumStartTime = System.currentTimeMillis()
        repeat(100) {
            val selected = Selection.pickNext(mediumDataset, emptyList(), it)
            assertNotNull("Selected template should not be null", selected)
        }
        val mediumTime = System.currentTimeMillis() - mediumStartTime
        assertTrue("Medium dataset selection should be fast (<500ms): ${mediumTime}ms", mediumTime < 500)

        // Large dataset
        val largeStartTime = System.currentTimeMillis()
        repeat(100) {
            val selected = Selection.pickNext(largeDataset, emptyList(), it)
            assertNotNull("Selected template should not be null", selected)
        }
        val largeTime = System.currentTimeMillis() - largeStartTime
        assertTrue("Large dataset selection should be acceptable (<1000ms): ${largeTime}ms", largeTime < 1000)

        // Performance should scale reasonably
        assertTrue("Performance should scale sub-linearly", largeTime < mediumTime * 10)
    }

    @Test
    fun `selectionAlgorithmMemoryUsage is acceptable`() {
        // Arrange
        val largeDataset = (1..2000).map { i ->
            TestDataFactory.createTemplateEntity(id = "template_$i")
        }

        val runtime = Runtime.getRuntime()
        runtime.gc() // Force garbage collection
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        // Act - Perform many selections
        repeat(500) {
            val selected = Selection.pickNext(largeDataset, emptyList(), it)
            assertNotNull("Selected template should not be null", selected)
        }

        runtime.gc() // Force garbage collection
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()

        // Assert
        val memoryIncrease = afterMemory - beforeMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)

        assertTrue("Memory increase should be reasonable (<50MB): ${memoryIncreaseMB}MB",
            memoryIncreaseMB < 50)
    }

    @Test
    fun `smartLearningPerformance with complex scenarios`() = runBlocking {
        // Arrange
        val complexDataset = (1..500).map { i ->
            TestDataFactory.createTemplateEntity(
                id = "template_$i",
                spice = (i % 3) + 1,
                locality = (i % 3) + 1,
                family = "family_${i % 10}"
            )
        }

        // Act
        val startTime = System.currentTimeMillis()

        repeat(200) {
            val selected = SmartLearning.selectTemplate(complexDataset, "ROAST_CONSENSUS")
            assertNotNull("Selected template should not be null", selected)
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Assert
        assertTrue("Complex scenario selection should complete in <3s (actual: ${totalTime}ms)",
            totalTime < 3000)
    }

    @Test
    fun `selectionConsistencyUnderLoad maintains correctness`() {
        // Arrange
        val dataset = TestDataFactory.createTemplateEntityList(100)
        val recentFamilies = listOf("family_0", "family_1")

        // Act - Perform many selections rapidly
        val selections = (1..1000).map {
            Selection.pickNext(dataset, recentFamilies, it)
        }

        // Assert - All selections should be valid
        assertEquals("Should have 1000 selections", 1000, selections.size)
        selections.forEach { selection ->
            assertTrue("All selections should be from dataset", dataset.contains(selection))
            assertFalse("Selected templates should not be from recent families",
                recentFamilies.contains(selection.family))
        }

        // Should have reasonable variety
        val uniqueSelections = selections.distinctBy { it.id }
        assertTrue("Should have reasonable variety (>10 unique): ${uniqueSelections.size}",
            uniqueSelections.size > 10)
    }
}