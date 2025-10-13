package com.helldeck.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.data.TemplateEntity
import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.DatabaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for TemplateDao
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TemplateDaoTest : DatabaseTest() {

    @Test
    fun `insert inserts template correctly`() = runTest {
        // Arrange
        val template = TestDataFactory.createTemplateEntity(
            id = "test_template_1",
            game = "ROAST_CONSENSUS",
            text = "Test template {slot1}",
            family = "test_family",
            spice = 2,
            locality = 1,
            maxWords = 16
        )

        // Act
        database.templates().insert(template)

        // Assert
        val retrieved = database.templates().byId(template.id)
        assertNotNull("Template should be retrievable", retrieved)
        assertEquals("Template ID should match", template.id, retrieved?.id)
        assertEquals("Template game should match", template.game, retrieved?.game)
        assertEquals("Template text should match", template.text, retrieved?.text)
        assertEquals("Template family should match", template.family, retrieved?.family)
        assertEquals("Template spice should match", template.spice, retrieved?.spice)
        assertEquals("Template locality should match", template.locality, retrieved?.locality)
        assertEquals("Template maxWords should match", template.maxWords, retrieved?.maxWords)
    }

    @Test
    fun `insertAll inserts multiple templates correctly`() = runTest {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(5, "ROAST_CONSENSUS")

        // Act
        database.templates().insertAll(templates)

        // Assert
        val allTemplates = database.templates().getAll()
        assertEquals("Should have all templates", 5, allTemplates.size)

        templates.forEach { expectedTemplate ->
            val found = allTemplates.find { it.id == expectedTemplate.id }
            assertNotNull("Template should be found: ${expectedTemplate.id}", found)
            assertEquals("Template text should match", expectedTemplate.text, found?.text)
        }
    }

    @Test
    fun `byId returns null for non-existent template`() = runTest {
        // Act
        val template = database.templates().byId("non_existent_id")

        // Assert
        assertNull("Should return null for non-existent template", template)
    }

    @Test
    fun `byGame returns correct templates for specific game`() = runTest {
        // Arrange
        val roastTemplates = listOf(
            TestDataFactory.createTemplateEntity(id = "roast_1", game = "ROAST_CONSENSUS"),
            TestDataFactory.createTemplateEntity(id = "roast_2", game = "ROAST_CONSENSUS"),
            TestDataFactory.createTemplateEntity(id = "roast_3", game = "ROAST_CONSENSUS")
        )

        val confessionTemplates = listOf(
            TestDataFactory.createTemplateEntity(id = "confession_1", game = "CONFESSION_OR_CAP"),
            TestDataFactory.createTemplateEntity(id = "confession_2", game = "CONFESSION_OR_CAP")
        )

        val allTemplates = roastTemplates + confessionTemplates
        database.templates().insertAll(allTemplates)

        // Act
        val roastResults = database.templates().byGame("ROAST_CONSENSUS")
        val confessionResults = database.templates().byGame("CONFESSION_OR_CAP")

        // Assert
        assertEquals("Should return correct number of roast templates", 3, roastResults.size)
        assertEquals("Should return correct number of confession templates", 2, confessionResults.size)

        roastResults.forEach { template ->
            assertEquals("All returned templates should be roast consensus",
                "ROAST_CONSENSUS", template.game)
        }

        confessionResults.forEach { template ->
            assertEquals("All returned templates should be confession or cap",
                "CONFESSION_OR_CAP", template.game)
        }
    }

    @Test
    fun `byGame returns empty list for game with no templates`() = runTest {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(3, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act
        val results = database.templates().byGame("NON_EXISTENT_GAME")

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for non-existent game", results.isEmpty())
    }

    @Test
    fun `getAll returns all templates correctly`() = runTest {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(4, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act
        val allTemplates = database.templates().getAll()

        // Assert
        assertEquals("Should return all templates", 4, allTemplates.size)
        templates.forEach { expectedTemplate ->
            assertTrue("All templates should be present",
                allTemplates.any { it.id == expectedTemplate.id })
        }
    }

    @Test
    fun `getAll returns empty list when no templates exist`() = runTest {
        // Act
        val allTemplates = database.templates().getAll()

        // Assert
        assertNotNull("Results should not be null", allTemplates)
        assertTrue("Results should be empty when no templates exist", allTemplates.isEmpty())
    }

    @Test
    fun `update updates template correctly`() = runTest {
        // Arrange
        val originalTemplate = TestDataFactory.createTemplateEntity(
            id = "update_test",
            text = "Original text {slot}",
            spice = 1
        )
        database.templates().insert(originalTemplate)

        val updatedTemplate = originalTemplate.copy(
            text = "Updated text {slot}",
            spice = 3
        )

        // Act
        database.templates().update(updatedTemplate)

        // Assert
        val retrieved = database.templates().byId(originalTemplate.id)
        assertNotNull("Template should still exist", retrieved)
        assertEquals("Text should be updated", "Updated text {slot}", retrieved?.text)
        assertEquals("Spice should be updated", 3, retrieved?.spice)
        assertEquals("Other fields should remain unchanged",
            originalTemplate.game, retrieved?.game)
    }

    @Test
    fun `delete removes template correctly`() = runTest {
        // Arrange
        val templateToDelete = TestDataFactory.createTemplateEntity(id = "delete_test")
        val templateToKeep = TestDataFactory.createTemplateEntity(id = "keep_test")

        database.templates().insert(templateToDelete)
        database.templates().insert(templateToKeep)

        // Act
        database.templates().delete(templateToDelete)

        // Assert
        val deletedTemplate = database.templates().byId("delete_test")
        val keptTemplate = database.templates().byId("keep_test")

        assertNull("Deleted template should not exist", deletedTemplate)
        assertNotNull("Kept template should still exist", keptTemplate)
    }

    @Test
    fun `deleteById removes template by ID correctly`() = runTest {
        // Arrange
        val template = TestDataFactory.createTemplateEntity(id = "delete_by_id_test")
        database.templates().insert(template)

        // Act
        database.templates().deleteById(template.id)

        // Assert
        val retrieved = database.templates().byId(template.id)
        assertNull("Template should be deleted", retrieved)
    }

    @Test
    fun `insert handles duplicate IDs correctly with REPLACE strategy`() = runTest {
        // Arrange
        val originalTemplate = TestDataFactory.createTemplateEntity(
            id = "duplicate_test",
            text = "Original text",
            spice = 1
        )

        val duplicateTemplate = TestDataFactory.createTemplateEntity(
            id = "duplicate_test",
            text = "Duplicate text",
            spice = 2
        )

        // Act
        database.templates().insert(originalTemplate)
        database.templates().insert(duplicateTemplate) // Should replace

        // Assert
        val retrieved = database.templates().byId("duplicate_test")
        assertNotNull("Template should exist", retrieved)
        assertEquals("Should have duplicate template data", "Duplicate text", retrieved?.text)
        assertEquals("Should have duplicate template spice", 2, retrieved?.spice)
    }

    @Test
    fun `byGame handles case sensitivity correctly`() = runTest {
        // Arrange
        val lowercaseGame = TestDataFactory.createTemplateEntity(
            id = "lowercase_test",
            game = "roast_consensus"
        )
        val uppercaseGame = TestDataFactory.createTemplateEntity(
            id = "uppercase_test",
            game = "ROAST_CONSENSUS"
        )

        database.templates().insert(lowercaseGame)
        database.templates().insert(uppercaseGame)

        // Act
        val lowercaseResults = database.templates().byGame("roast_consensus")
        val uppercaseResults = database.templates().byGame("ROAST_CONSENSUS")

        // Assert
        assertEquals("Should find lowercase game template", 1, lowercaseResults.size)
        assertEquals("Should find uppercase game template", 1, uppercaseResults.size)
        assertEquals("Lowercase template should have correct game",
            "roast_consensus", lowercaseResults.first().game)
        assertEquals("Uppercase template should have correct game",
            "ROAST_CONSENSUS", uppercaseResults.first().game)
    }

    @Test
    fun `getTemplateCount returns correct count`() = runTest {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(7, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act
        val count = database.templates().getTemplateCount()

        // Assert
        assertEquals("Count should match inserted templates", 7, count)
    }

    @Test
    fun `getTemplateCount returns zero when no templates exist`() = runTest {
        // Act
        val count = database.templates().getTemplateCount()

        // Assert
        assertEquals("Count should be zero when no templates exist", 0, count)
    }

    @Test
    fun `byFamily returns correct templates for specific family`() = runTest {
        // Arrange
        val family1Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "family1_1", family = "family1"),
            TestDataFactory.createTemplateEntity(id = "family1_2", family = "family1")
        )

        val family2Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "family2_1", family = "family2"),
            TestDataFactory.createTemplateEntity(id = "family2_2", family = "family2"),
            TestDataFactory.createTemplateEntity(id = "family2_3", family = "family2")
        )

        val allTemplates = family1Templates + family2Templates
        database.templates().insertAll(allTemplates)

        // Act
        val family1Results = database.templates().byFamily("family1")
        val family2Results = database.templates().byFamily("family2")

        // Assert
        assertEquals("Should return correct number of family1 templates", 2, family1Results.size)
        assertEquals("Should return correct number of family2 templates", 3, family2Results.size)

        family1Results.forEach { template ->
            assertEquals("All returned templates should be family1", "family1", template.family)
        }

        family2Results.forEach { template ->
            assertEquals("All returned templates should be family2", "family2", template.family)
        }
    }

    @Test
    fun `byFamily returns empty list for family with no templates`() = runTest {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(3, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act
        val results = database.templates().byFamily("non_existent_family")

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for non-existent family", results.isEmpty())
    }

    @Test
    fun `bySpiceLevel returns correct templates for spice level`() = runTest {
        // Arrange
        val spice1Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "spice1_1", spice = 1),
            TestDataFactory.createTemplateEntity(id = "spice1_2", spice = 1)
        )

        val spice2Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "spice2_1", spice = 2),
            TestDataFactory.createTemplateEntity(id = "spice2_2", spice = 2),
            TestDataFactory.createTemplateEntity(id = "spice2_3", spice = 2)
        )

        val spice3Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "spice3_1", spice = 3)
        )

        val allTemplates = spice1Templates + spice2Templates + spice3Templates
        database.templates().insertAll(allTemplates)

        // Act
        val spice1Results = database.templates().bySpiceLevel(1)
        val spice2Results = database.templates().bySpiceLevel(2)
        val spice3Results = database.templates().bySpiceLevel(3)

        // Assert
        assertEquals("Should return correct number of spice 1 templates", 2, spice1Results.size)
        assertEquals("Should return correct number of spice 2 templates", 3, spice2Results.size)
        assertEquals("Should return correct number of spice 3 templates", 1, spice3Results.size)

        spice1Results.forEach { template ->
            assertEquals("All returned templates should have spice 1", 1, template.spice)
        }

        spice2Results.forEach { template ->
            assertEquals("All returned templates should have spice 2", 2, template.spice)
        }

        spice3Results.forEach { template ->
            assertEquals("All returned templates should have spice 3", 3, template.spice)
        }
    }

    @Test
    fun `bySpiceLevel returns empty list for spice level with no templates`() = runTest {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(3, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act
        val results = database.templates().bySpiceLevel(99)

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for non-existent spice level", results.isEmpty())
    }

    @Test
    fun `byLocalityLevel returns correct templates for locality level`() = runTest {
        // Arrange
        val locality1Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "local1_1", locality = 1),
            TestDataFactory.createTemplateEntity(id = "local1_2", locality = 1)
        )

        val locality2Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "local2_1", locality = 2)
        )

        val locality3Templates = listOf(
            TestDataFactory.createTemplateEntity(id = "local3_1", locality = 3),
            TestDataFactory.createTemplateEntity(id = "local3_2", locality = 3),
            TestDataFactory.createTemplateEntity(id = "local3_3", locality = 3)
        )

        val allTemplates = locality1Templates + locality2Templates + locality3Templates
        database.templates().insertAll(allTemplates)

        // Act
        val locality1Results = database.templates().byLocalityLevel(1)
        val locality2Results = database.templates().byLocalityLevel(2)
        val locality3Results = database.templates().byLocalityLevel(3)

        // Assert
        assertEquals("Should return correct number of locality 1 templates", 2, locality1Results.size)
        assertEquals("Should return correct number of locality 2 templates", 1, locality2Results.size)
        assertEquals("Should return correct number of locality 3 templates", 3, locality3Results.size)

        locality1Results.forEach { template ->
            assertEquals("All returned templates should have locality 1", 1, template.locality)
        }

        locality2Results.forEach { template ->
            assertEquals("All returned templates should have locality 2", 2, template.locality)
        }

        locality3Results.forEach { template ->
            assertEquals("All returned templates should have locality 3", 3, template.locality)
        }
    }

    @Test
    fun `byLocalityLevel returns empty list for locality level with no templates`() = runTest {
        // Arrange
        val templates = TestDataFactory.createTemplateEntityList(3, "ROAST_CONSENSUS")
        database.templates().insertAll(templates)

        // Act
        val results = database.templates().byLocalityLevel(99)

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for non-existent locality level", results.isEmpty())
    }

    @Test
    fun `complex queries work correctly with multiple filters`() = runTest {
        // Arrange
        val templates = listOf(
            TestDataFactory.createTemplateEntity(id = "complex1", game = "ROAST_CONSENSUS", family = "family1", spice = 1, locality = 1),
            TestDataFactory.createTemplateEntity(id = "complex2", game = "ROAST_CONSENSUS", family = "family1", spice = 2, locality = 1),
            TestDataFactory.createTemplateEntity(id = "complex3", game = "ROAST_CONSENSUS", family = "family2", spice = 1, locality = 2),
            TestDataFactory.createTemplateEntity(id = "complex4", game = "CONFESSION_OR_CAP", family = "family1", spice = 1, locality = 1)
        )
        database.templates().insertAll(templates)

        // Act - Test combination of game and family
        val roastFamily1Results = database.templates().byGame("ROAST_CONSENSUS")
            .filter { it.family == "family1" }

        // Assert
        assertEquals("Should return correct templates for game + family filter", 2, roastFamily1Results.size)
        roastFamily1Results.forEach { template ->
            assertEquals("Should all be ROAST_CONSENSUS", "ROAST_CONSENSUS", template.game)
            assertEquals("Should all be family1", "family1", template.family)
        }
    }

    @Test
    fun `insert handles templates with special characters correctly`() = runTest {
        // Arrange
        val specialTemplate = TestDataFactory.createTemplateEntity(
            id = "special_chars_test",
            text = "Template with spécial çharácters and émojis 🚀 and {slot}!",
            family = "special_family_ñ"
        )

        // Act
        database.templates().insert(specialTemplate)

        // Assert
        val retrieved = database.templates().byId("special_chars_test")
        assertNotNull("Template with special characters should be stored", retrieved)
        assertEquals("Special characters should be preserved",
            "Template with spécial çharácters and émojis 🚀 and {slot}!", retrieved?.text)
        assertEquals("Special family name should be preserved",
            "special_family_ñ", retrieved?.family)
    }

    @Test
    fun `insert handles very long template text correctly`() = runTest {
        // Arrange
        val longText = "A".repeat(1000) + " {slot} " + "B".repeat(1000)
        val longTemplate = TestDataFactory.createTemplateEntity(
            id = "long_text_test",
            text = longText,
            maxWords = 2000
        )

        // Act
        database.templates().insert(longTemplate)

        // Assert
        val retrieved = database.templates().byId("long_text_test")
        assertNotNull("Long template should be stored", retrieved)
        assertEquals("Long text should be preserved", longText, retrieved?.text)
        assertEquals("MaxWords should be preserved", 2000, retrieved?.maxWords)
    }

    @Test
    fun `insert handles templates with minimum valid values correctly`() = runTest {
        // Arrange
        val minimalTemplate = TestDataFactory.createTemplateEntity(
            id = "minimal_test",
            text = "{slot}",
            family = "a",
            spice = 1,
            locality = 1,
            maxWords = 1
        )

        // Act
        database.templates().insert(minimalTemplate)

        // Assert
        val retrieved = database.templates().byId("minimal_test")
        assertNotNull("Minimal template should be stored", retrieved)
        assertEquals("Minimal text should be preserved", "{slot}", retrieved?.text)
        assertEquals("Minimal family should be preserved", "a", retrieved?.family)
        assertEquals("Minimal spice should be preserved", 1, retrieved?.spice)
        assertEquals("Minimal locality should be preserved", 1, retrieved?.locality)
        assertEquals("Minimal maxWords should be preserved", 1, retrieved?.maxWords)
    }

    @Test
    fun `insert handles templates with maximum valid values correctly`() = runTest {
        // Arrange
        val maximalTemplate = TestDataFactory.createTemplateEntity(
            id = "maximal_test",
            text = "A".repeat(1000),
            family = "family_name_123",
            spice = 3,
            locality = 3,
            maxWords = 1000
        )

        // Act
        database.templates().insert(maximalTemplate)

        // Assert
        val retrieved = database.templates().byId("maximal_test")
        assertNotNull("Maximal template should be stored", retrieved)
        assertEquals("Maximal text should be preserved", "A".repeat(1000), retrieved?.text)
        assertEquals("Maximal family should be preserved", "family_name_123", retrieved?.family)
        assertEquals("Maximal spice should be preserved", 3, retrieved?.spice)
        assertEquals("Maximal locality should be preserved", 3, retrieved?.locality)
        assertEquals("Maximal maxWords should be preserved", 1000, retrieved?.maxWords)
    }
}