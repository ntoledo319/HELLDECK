package com.helldeck.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.fixtures.TestDataFactory
import com.helldeck.testutil.DatabaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for LexiconDao
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class LexiconDaoTest : DatabaseTest() {

    @Test
    fun `insert inserts lexicon correctly`() = runTest {
        // Arrange
        val lexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "test_lexicon",
            words = "[\"word1\",\"word2\",\"word3\"]",
            updatedTs = 1000L
        )

        // Act
        database.lexicons().insert(lexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("test_lexicon")
        assertNotNull("Lexicon should be retrievable", retrieved)
        assertEquals("Lexicon ID should match", lexicon.id, retrieved?.id)
        assertEquals("Lexicon name should match", lexicon.name, retrieved?.name)
        assertEquals("Lexicon words should match", lexicon.words, retrieved?.words)
        assertEquals("Lexicon updatedTs should match", lexicon.updatedTs, retrieved?.updatedTs)
    }

    @Test
    fun `insertAll inserts multiple lexicons correctly`() = runTest {
        // Arrange
        val lexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "lexicon1"),
            TestDataFactory.createLexiconEntity(id = 2L, name = "lexicon2"),
            TestDataFactory.createLexiconEntity(id = 3L, name = "lexicon3")
        )

        // Act
        database.lexicons().insertAll(lexicons)

        // Assert
        val allLexicons = database.lexicons().getAllLexicons()
        assertEquals("Should have all lexicons", 3, allLexicons.size)

        lexicons.forEach { expectedLexicon ->
            val found = allLexicons.find { it.id == expectedLexicon.id }
            assertNotNull("Lexicon should be found: ${expectedLexicon.name}", found)
            assertEquals("Lexicon name should match", expectedLexicon.name, found?.name)
        }
    }

    @Test
    fun `getLexicon returns null for non-existent lexicon`() = runTest {
        // Act
        val lexicon = database.lexicons().getLexicon("non_existent_lexicon")

        // Assert
        assertNull("Should return null for non-existent lexicon", lexicon)
    }

    @Test
    fun `getAllLexicons returns all lexicons correctly`() = runTest {
        // Arrange
        val lexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "lexicon1"),
            TestDataFactory.createLexiconEntity(id = 2L, name = "lexicon2"),
            TestDataFactory.createLexiconEntity(id = 3L, name = "lexicon3")
        )
        database.lexicons().insertAll(lexicons)

        // Act
        val allLexicons = database.lexicons().getAllLexicons()

        // Assert
        assertEquals("Should return all lexicons", 3, allLexicons.size)
        lexicons.forEach { expectedLexicon ->
            assertTrue("All lexicons should be present",
                allLexicons.any { it.id == expectedLexicon.id })
        }
    }

    @Test
    fun `getAllLexicons returns empty list when no lexicons exist`() = runTest {
        // Act
        val allLexicons = database.lexicons().getAllLexicons()

        // Assert
        assertNotNull("Results should not be null", allLexicons)
        assertTrue("Results should be empty when no lexicons exist", allLexicons.isEmpty())
    }

    @Test
    fun `getAllLexiconNames returns correct names`() = runTest {
        // Arrange
        val lexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "lexicon1"),
            TestDataFactory.createLexiconEntity(id = 2L, name = "lexicon2"),
            TestDataFactory.createLexiconEntity(id = 3L, name = "lexicon3")
        )
        database.lexicons().insertAll(lexicons)

        // Act
        val allNames = database.lexicons().getAllLexiconNames()

        // Assert
        assertEquals("Should return all lexicon names", 3, allNames.size)
        assertTrue("Should contain lexicon1", allNames.contains("lexicon1"))
        assertTrue("Should contain lexicon2", allNames.contains("lexicon2"))
        assertTrue("Should contain lexicon3", allNames.contains("lexicon3"))
    }

    @Test
    fun `getAllLexiconNames returns empty list when no lexicons exist`() = runTest {
        // Act
        val allNames = database.lexicons().getAllLexiconNames()

        // Assert
        assertNotNull("Results should not be null", allNames)
        assertTrue("Results should be empty when no lexicons exist", allNames.isEmpty())
    }

    @Test
    fun `getLexiconCount returns correct count`() = runTest {
        // Arrange
        val lexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "lexicon1"),
            TestDataFactory.createLexiconEntity(id = 2L, name = "lexicon2"),
            TestDataFactory.createLexiconEntity(id = 3L, name = "lexicon3")
        )
        database.lexicons().insertAll(lexicons)

        // Act
        val count = database.lexicons().getLexiconCount()

        // Assert
        assertEquals("Count should match inserted lexicons", 3, count)
    }

    @Test
    fun `getLexiconCount returns zero when no lexicons exist`() = runTest {
        // Act
        val count = database.lexicons().getLexiconCount()

        // Assert
        assertEquals("Count should be zero when no lexicons exist", 0, count)
    }

    @Test
    fun `update updates lexicon correctly`() = runTest {
        // Arrange
        val originalLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "original_name",
            words = "[\"original\"]",
            updatedTs = 1000L
        )
        database.lexicons().insert(originalLexicon)

        val updatedLexicon = originalLexicon.copy(
            name = "updated_name",
            words = "[\"updated\"]",
            updatedTs = 2000L
        )

        // Act
        database.lexicons().update(updatedLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("updated_name")
        assertNotNull("Lexicon should still exist", retrieved)
        assertEquals("Name should be updated", "updated_name", retrieved?.name)
        assertEquals("Words should be updated", "[\"updated\"]", retrieved?.words)
        assertEquals("UpdatedTs should be updated", 2000L, retrieved?.updatedTs)
    }

    @Test
    fun `delete removes lexicon correctly`() = runTest {
        // Arrange
        val lexiconToDelete = TestDataFactory.createLexiconEntity(id = 1L, name = "delete_test")
        val lexiconToKeep = TestDataFactory.createLexiconEntity(id = 2L, name = "keep_test")

        database.lexicons().insert(lexiconToDelete)
        database.lexicons().insert(lexiconToKeep)

        // Act
        database.lexicons().delete(lexiconToDelete)

        // Assert
        val deletedLexicon = database.lexicons().getLexicon("delete_test")
        val keptLexicon = database.lexicons().getLexicon("keep_test")

        assertNull("Deleted lexicon should not exist", deletedLexicon)
        assertNotNull("Kept lexicon should still exist", keptLexicon)
    }

    @Test
    fun `deleteByName removes lexicon by name correctly`() = runTest {
        // Arrange
        val lexicon = TestDataFactory.createLexiconEntity(id = 1L, name = "delete_by_name_test")
        database.lexicons().insert(lexicon)

        // Act
        database.lexicons().deleteByName("delete_by_name_test")

        // Assert
        val retrieved = database.lexicons().getLexicon("delete_by_name_test")
        assertNull("Lexicon should be deleted", retrieved)
    }

    @Test
    fun `upsert inserts new lexicon correctly`() = runTest {
        // Arrange
        val lexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "upsert_test",
            words = "[\"test\"]",
            updatedTs = 1000L
        )

        // Act
        database.lexicons().upsert(lexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("upsert_test")
        assertNotNull("Lexicon should be inserted", retrieved)
        assertEquals("Words should match", "[\"test\"]", retrieved?.words)
    }

    @Test
    fun `upsert updates existing lexicon correctly`() = runTest {
        // Arrange
        val originalLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "upsert_update_test",
            words = "[\"original\"]",
            updatedTs = 1000L
        )
        database.lexicons().insert(originalLexicon)

        val updatedLexicon = originalLexicon.copy(
            words = "[\"updated\"]",
            updatedTs = 2000L
        )

        // Act
        database.lexicons().upsert(updatedLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("upsert_update_test")
        assertNotNull("Lexicon should still exist", retrieved)
        assertEquals("Words should be updated", "[\"updated\"]", retrieved?.words)
        assertEquals("UpdatedTs should be updated", 2000L, retrieved?.updatedTs)
    }

    @Test
    fun `insert handles lexicons with special characters correctly`() = runTest {
        // Arrange
        val specialLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "spécial_léxicon_ñ",
            words = "[\"wörd1\",\"wörd2\",\"émojis 🚀\"]"
        )

        // Act
        database.lexicons().insert(specialLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("spécial_léxicon_ñ")
        assertNotNull("Lexicon with special characters should be stored", retrieved)
        assertEquals("Special characters should be preserved",
            "[\"wörd1\",\"wörd2\",\"émojis 🚀\"]", retrieved?.words)
    }

    @Test
    fun `insert handles very long words array correctly`() = runTest {
        // Arrange
        val longWords = List(1000) { i -> "\"word$i\"" }.joinToString(",", prefix = "[", postfix = "]")
        val longLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "long_words_test",
            words = longWords
        )

        // Act
        database.lexicons().insert(longLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("long_words_test")
        assertNotNull("Lexicon with long words should be stored", retrieved)
        assertEquals("Long words should be preserved", longWords, retrieved?.words)
    }

    @Test
    fun `insert handles lexicons with empty words array correctly`() = runTest {
        // Arrange
        val emptyWordsLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "empty_words_test",
            words = "[]"
        )

        // Act
        database.lexicons().insert(emptyWordsLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("empty_words_test")
        assertNotNull("Lexicon with empty words should be stored", retrieved)
        assertEquals("Empty words should be preserved", "[]", retrieved?.words)
    }

    @Test
    fun `insert handles lexicons with single word correctly`() = runTest {
        // Arrange
        val singleWordLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "single_word_test",
            words = "[\"single\"]"
        )

        // Act
        database.lexicons().insert(singleWordLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("single_word_test")
        assertNotNull("Lexicon with single word should be stored", retrieved)
        assertEquals("Single word should be preserved", "[\"single\"]", retrieved?.words)
    }

    @Test
    fun `insert handles lexicons with minimum valid values correctly`() = runTest {
        // Arrange
        val minimalLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "a",
            words = "[\"a\"]",
            updatedTs = 0L
        )

        // Act
        database.lexicons().insert(minimalLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("a")
        assertNotNull("Minimal lexicon should be stored", retrieved)
        assertEquals("Minimal name should be preserved", "a", retrieved?.name)
        assertEquals("Minimal words should be preserved", "[\"a\"]", retrieved?.words)
        assertEquals("Minimal updatedTs should be preserved", 0L, retrieved?.updatedTs)
    }

    @Test
    fun `insert handles lexicons with maximum valid values correctly`() = runTest {
        // Arrange
        val maximalLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "A".repeat(100),
            words = "[\"${"A".repeat(1000)}\"]",
            updatedTs = Long.MAX_VALUE
        )

        // Act
        database.lexicons().insert(maximalLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("A".repeat(100))
        assertNotNull("Maximal lexicon should be stored", retrieved)
        assertEquals("Maximal name should be preserved", "A".repeat(100), retrieved?.name)
        assertEquals("Maximal words should be preserved", "[\"${"A".repeat(1000)}\"]", retrieved?.words)
        assertEquals("Maximal updatedTs should be preserved", Long.MAX_VALUE, retrieved?.updatedTs)
    }

    @Test
    fun `getStaleLexicons returns correct lexicons based on timestamp`() = runTest {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - 1000

        val freshLexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "fresh1", updatedTs = currentTime),
            TestDataFactory.createLexiconEntity(id = 2L, name = "fresh2", updatedTs = currentTime - 500)
        )

        val staleLexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 3L, name = "stale1", updatedTs = cutoffTime - 1000),
            TestDataFactory.createLexiconEntity(id = 4L, name = "stale2", updatedTs = cutoffTime - 500)
        )

        val allLexicons = freshLexicons + staleLexicons
        database.lexicons().insertAll(allLexicons)

        // Act
        val staleResults = database.lexicons().getStaleLexicons(cutoffTime)

        // Assert
        assertEquals("Should return correct number of stale lexicons", 2, staleResults.size)
        staleResults.forEach { lexicon ->
            assertTrue("All lexicons should be stale",
                lexicon.updatedTs < cutoffTime)
        }

        // Verify no fresh lexicons are included
        staleResults.forEach { staleLexicon ->
            assertFalse("Fresh lexicons should not be included",
                freshLexicons.any { it.id == staleLexicon.id })
        }
    }

    @Test
    fun `getStaleLexicons returns empty list when all lexicons are fresh`() = runTest {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - 1000

        val freshLexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "fresh1", updatedTs = currentTime),
            TestDataFactory.createLexiconEntity(id = 2L, name = "fresh2", updatedTs = currentTime - 500)
        )

        database.lexicons().insertAll(freshLexicons)

        // Act
        val staleResults = database.lexicons().getStaleLexicons(cutoffTime)

        // Assert
        assertNotNull("Results should not be null", staleResults)
        assertTrue("Results should be empty when all lexicons are fresh", staleResults.isEmpty())
    }

    @Test
    fun `getStaleLexicons returns all lexicons when cutoff is very old`() = runTest {
        // Arrange
        val lexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "lexicon1", updatedTs = 1000L),
            TestDataFactory.createLexiconEntity(id = 2L, name = "lexicon2", updatedTs = 2000L),
            TestDataFactory.createLexiconEntity(id = 3L, name = "lexicon3", updatedTs = 3000L)
        )
        database.lexicons().insertAll(lexicons)

        // Act
        val staleResults = database.lexicons().getStaleLexicons(500L) // Very old cutoff

        // Assert
        assertEquals("Should return all lexicons when cutoff is very old", 3, staleResults.size)
    }

    @Test
    fun `insert handles lexicons with malformed JSON correctly`() = runTest {
        // Arrange
        val malformedJsonLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "malformed_json_test",
            words = "[\"word1\",\"word2\",]" // Trailing comma
        )

        // Act
        database.lexicons().insert(malformedJsonLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("malformed_json_test")
        assertNotNull("Lexicon with malformed JSON should be stored", retrieved)
        assertEquals("Malformed JSON should be preserved", "[\"word1\",\"word2\",]", retrieved?.words)
    }

    @Test
    fun `insert handles lexicons with nested quotes correctly`() = runTest {
        // Arrange
        val nestedQuotesLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "nested_quotes_test",
            words = "[\"word with \\\"quotes\\\"\",\"another word\"]"
        )

        // Act
        database.lexicons().insert(nestedQuotesLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("nested_quotes_test")
        assertNotNull("Lexicon with nested quotes should be stored", retrieved)
        assertEquals("Nested quotes should be preserved",
            "[\"word with \\\"quotes\\\"\",\"another word\"]", retrieved?.words)
    }

    @Test
    fun `insert handles lexicons with unicode characters correctly`() = runTest {
        // Arrange
        val unicodeLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "ünícódé_tëst_ñ",
            words = "[\"wörd1\",\"wörd2\",\"émojis 🚀 🌟\"]"
        )

        // Act
        database.lexicons().insert(unicodeLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("ünícódé_tëst_ñ")
        assertNotNull("Lexicon with unicode characters should be stored", retrieved)
        assertEquals("Unicode characters should be preserved",
            "[\"wörd1\",\"wörd2\",\"émojis 🚀 🌟\"]", retrieved?.words)
    }

    @Test
    fun `insert handles lexicons with very long names correctly`() = runTest {
        // Arrange
        val longName = "A".repeat(1000) + "_lexicon_name_" + "B".repeat(1000)
        val longNameLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = longName,
            words = "[\"test\"]"
        )

        // Act
        database.lexicons().insert(longNameLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon(longName)
        assertNotNull("Lexicon with long name should be stored", retrieved)
        assertEquals("Long name should be preserved", longName, retrieved?.name)
    }

    @Test
    fun `insert handles lexicons with empty name correctly`() = runTest {
        // Arrange
        val emptyNameLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "",
            words = "[\"test\"]"
        )

        // Act
        database.lexicons().insert(emptyNameLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("")
        assertNotNull("Lexicon with empty name should be stored", retrieved)
        assertEquals("Empty name should be preserved", "", retrieved?.name)
    }

    @Test
    fun `insert handles lexicons with null words correctly`() = runTest {
        // Arrange
        val nullWordsLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "null_words_test",
            words = "null"
        )

        // Act
        database.lexicons().insert(nullWordsLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("null_words_test")
        assertNotNull("Lexicon with null words should be stored", retrieved)
        assertEquals("Null words should be preserved", "null", retrieved?.words)
    }

    @Test
    fun `insert handles lexicons with zero updated timestamp correctly`() = runTest {
        // Arrange
        val zeroTimestampLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "zero_timestamp_test",
            words = "[\"test\"]",
            updatedTs = 0L
        )

        // Act
        database.lexicons().insert(zeroTimestampLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("zero_timestamp_test")
        assertNotNull("Lexicon with zero timestamp should be stored", retrieved)
        assertEquals("Zero timestamp should be preserved", 0L, retrieved?.updatedTs)
    }

    @Test
    fun `insert handles lexicons with future timestamp correctly`() = runTest {
        // Arrange
        val futureTimestamp = System.currentTimeMillis() + 1000000
        val futureTimestampLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "future_timestamp_test",
            words = "[\"test\"]",
            updatedTs = futureTimestamp
        )

        // Act
        database.lexicons().insert(futureTimestampLexicon)

        // Assert
        val retrieved = database.lexicons().getLexicon("future_timestamp_test")
        assertNotNull("Lexicon with future timestamp should be stored", retrieved)
        assertEquals("Future timestamp should be preserved", futureTimestamp, retrieved?.updatedTs)
    }

    @Test
    fun `insert handles lexicons with duplicate names correctly with REPLACE strategy`() = runTest {
        // Arrange
        val originalLexicon = TestDataFactory.createLexiconEntity(
            id = 1L,
            name = "duplicate_name_test",
            words = "[\"original\"]",
            updatedTs = 1000L
        )

        val duplicateLexicon = TestDataFactory.createLexiconEntity(
            id = 2L,
            name = "duplicate_name_test",
            words = "[\"duplicate\"]",
            updatedTs = 2000L
        )

        // Act
        database.lexicons().insert(originalLexicon)
        database.lexicons().insert(duplicateLexicon) // Should replace

        // Assert
        val retrieved = database.lexicons().getLexicon("duplicate_name_test")
        assertNotNull("Lexicon should exist", retrieved)
        assertEquals("Should have duplicate lexicon data", "[\"duplicate\"]", retrieved?.words)
        assertEquals("Should have duplicate lexicon updatedTs", 2000L, retrieved?.updatedTs)
    }

    @Test
    fun `getStaleLexicons handles edge case timestamps correctly`() = runTest {
        // Arrange
        val currentTime = System.currentTimeMillis()

        val edgeCaseLexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "exactly_cutoff", updatedTs = currentTime - 1000),
            TestDataFactory.createLexiconEntity(id = 2L, name = "just_after_cutoff", updatedTs = currentTime - 999),
            TestDataFactory.createLexiconEntity(id = 3L, name = "much_older", updatedTs = currentTime - 10000)
        )

        database.lexicons().insertAll(edgeCaseLexicons)

        // Act
        val staleResults = database.lexicons().getStaleLexicons(currentTime - 1000)

        // Assert
        assertEquals("Should return lexicons exactly at or before cutoff", 2, staleResults.size)
        assertTrue("Should include exactly cutoff lexicon",
            staleResults.any { it.name == "exactly_cutoff" })
        assertTrue("Should include much older lexicon",
            staleResults.any { it.name == "much_older" })
        assertFalse("Should not include just after cutoff lexicon",
            staleResults.any { it.name == "just_after_cutoff" })
    }

    @Test
    fun `getStaleLexicons handles future cutoff timestamp correctly`() = runTest {
        // Arrange
        val lexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "lexicon1", updatedTs = 1000L),
            TestDataFactory.createLexiconEntity(id = 2L, name = "lexicon2", updatedTs = 2000L)
        )
        database.lexicons().insertAll(lexicons)

        // Act
        val staleResults = database.lexicons().getStaleLexicons(3000L) // Future timestamp

        // Assert
        assertEquals("Should return all lexicons when cutoff is in future", 2, staleResults.size)
    }

    @Test
    fun `getStaleLexicons handles zero cutoff timestamp correctly`() = runTest {
        // Arrange
        val lexicons = listOf(
            TestDataFactory.createLexiconEntity(id = 1L, name = "zero_ts", updatedTs = 0L),
            TestDataFactory.createLexiconEntity(id = 2L, name = "positive_ts", updatedTs = 1000L)
        )
        database.lexicons().insertAll(lexicons)

        // Act
        val staleResults = database.lexicons().getStaleLexicons(500L)

        // Assert
        assertEquals("Should return lexicons with timestamp <= cutoff", 1, staleResults.size)
        assertEquals("Should return zero timestamp lexicon", "zero_ts", staleResults.first().name)
    }
}