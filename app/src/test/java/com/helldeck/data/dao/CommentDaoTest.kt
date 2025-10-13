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
 * Comprehensive unit tests for CommentDao
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CommentDaoTest : DatabaseTest() {

    @Test
    fun `insert inserts comment correctly`() = runTest {
        // Arrange
        val comment = TestDataFactory.createCommentEntity(
            id = 1L,
            roundId = 1L,
            text = "Test comment",
            tags = "test,comment",
            createdAt = 1000L
        )

        // Act
        database.comments().insert(comment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Comment ID should match", comment.id, retrieved.first().id)
        assertEquals("Comment text should match", comment.text, retrieved.first().text)
        assertEquals("Comment tags should match", comment.tags, retrieved.first().tags)
        assertEquals("Comment createdAt should match", comment.createdAt, retrieved.first().createdAt)
    }

    @Test
    fun `insertAll inserts multiple comments correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, roundId = 1L, text = "Comment 1"),
            TestDataFactory.createCommentEntity(id = 2L, roundId = 1L, text = "Comment 2"),
            TestDataFactory.createCommentEntity(id = 3L, roundId = 2L, text = "Comment 3")
        )

        // Act
        database.comments().insertComments(comments)

        // Assert
        val allComments = database.comments().getAll()
        assertEquals("Should have all comments", 3, allComments.size)

        comments.forEach { expectedComment ->
            val found = allComments.find { it.id == expectedComment.id }
            assertNotNull("Comment should be found: ${expectedComment.id}", found)
            assertEquals("Comment text should match", expectedComment.text, found?.text)
        }
    }

    @Test
    fun `getCommentsForRound returns correct comments for round`() = runTest {
        // Arrange
        val round1Comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, roundId = 1L, text = "Round 1 Comment 1"),
            TestDataFactory.createCommentEntity(id = 2L, roundId = 1L, text = "Round 1 Comment 2"),
            TestDataFactory.createCommentEntity(id = 3L, roundId = 1L, text = "Round 1 Comment 3")
        )

        val round2Comments = listOf(
            TestDataFactory.createCommentEntity(id = 4L, roundId = 2L, text = "Round 2 Comment 1"),
            TestDataFactory.createCommentEntity(id = 5L, roundId = 2L, text = "Round 2 Comment 2")
        )

        val allComments = round1Comments + round2Comments
        database.comments().insertComments(allComments)

        // Act
        val round1Results = database.comments().getCommentsForRound(1L)
        val round2Results = database.comments().getCommentsForRound(2L)

        // Assert
        assertEquals("Round 1 should have 3 comments", 3, round1Results.size)
        assertEquals("Round 2 should have 2 comments", 2, round2Results.size)

        round1Results.forEach { comment ->
            assertEquals("All comments should be for round 1", 1L, comment.roundId)
        }

        round2Results.forEach { comment ->
            assertEquals("All comments should be for round 2", 2L, comment.roundId)
        }
    }

    @Test
    fun `getCommentsForRound returns empty list for round with no comments`() = runTest {
        // Act
        val comments = database.comments().getCommentsForRound(999L)

        // Assert
        assertNotNull("Results should not be null", comments)
        assertTrue("Results should be empty for round with no comments", comments.isEmpty())
    }

    @Test
    fun `getCommentsWithTag returns correct comments for tag`() = runTest {
        // Arrange
        val funnyComments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, text = "Funny comment 1", tags = "funny,humor"),
            TestDataFactory.createCommentEntity(id = 2L, text = "Funny comment 2", tags = "funny,joke")
        )

        val seriousComments = listOf(
            TestDataFactory.createCommentEntity(id = 3L, text = "Serious comment 1", tags = "serious,analysis"),
            TestDataFactory.createCommentEntity(id = 4L, text = "Serious comment 2", tags = "serious,thoughtful")
        )

        val allComments = funnyComments + seriousComments
        database.comments().insertComments(allComments)

        // Act
        val funnyResults = database.comments().getCommentsWithTag("funny")
        val seriousResults = database.comments().getCommentsWithTag("serious")

        // Assert
        assertEquals("Should return correct number of funny comments", 2, funnyResults.size)
        assertEquals("Should return correct number of serious comments", 2, seriousResults.size)

        funnyResults.forEach { comment ->
            assertTrue("All comments should contain funny tag",
                comment.tags.contains("funny"))
        }

        seriousResults.forEach { comment ->
            assertTrue("All comments should contain serious tag",
                comment.tags.contains("serious"))
        }
    }

    @Test
    fun `getCommentsWithTag returns empty list for tag with no comments`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "serious,analysis")
        )
        database.comments().insertComments(comments)

        // Act
        val results = database.comments().getCommentsWithTag("non_existent_tag")

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty for non-existent tag", results.isEmpty())
    }

    @Test
    fun `getAllTags returns all unique tags correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor,joke"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "serious,analysis,funny"),
            TestDataFactory.createCommentEntity(id = 3L, tags = "creative,artistic"),
            TestDataFactory.createCommentEntity(id = 4L, tags = "")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return all unique tags", 6, allTags.size)
        assertTrue("Should contain funny tag", allTags.contains("funny"))
        assertTrue("Should contain humor tag", allTags.contains("humor"))
        assertTrue("Should contain joke tag", allTags.contains("joke"))
        assertTrue("Should contain serious tag", allTags.contains("serious"))
        assertTrue("Should contain analysis tag", allTags.contains("analysis"))
        assertTrue("Should contain creative tag", allTags.contains("creative"))
    }

    @Test
    fun `getAllTags returns empty list when no comments exist`() = runTest {
        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertNotNull("Results should not be null", allTags)
        assertTrue("Results should be empty when no comments exist", allTags.isEmpty())
    }

    @Test
    fun `getAllTags handles comments with no tags correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = ""),
            TestDataFactory.createCommentEntity(id = 2L, tags = ""),
            TestDataFactory.createCommentEntity(id = 3L, tags = "")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertNotNull("Results should not be null", allTags)
        assertTrue("Results should be empty when no comments have tags", allTags.isEmpty())
    }

    @Test
    fun `update updates comment correctly`() = runTest {
        // Arrange
        val originalComment = TestDataFactory.createCommentEntity(
            id = 1L,
            text = "Original text",
            tags = "original",
            createdAt = 1000L
        )
        database.comments().insert(originalComment)

        val updatedComment = originalComment.copy(
            text = "Updated text",
            tags = "updated,new"
        )

        // Act
        database.comments().update(updatedComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Text should be updated", "Updated text", retrieved.first().text)
        assertEquals("Tags should be updated", "updated,new", retrieved.first().tags)
        assertEquals("CreatedAt should remain unchanged", 1000L, retrieved.first().createdAt)
    }

    @Test
    fun `delete removes comment correctly`() = runTest {
        // Arrange
        val commentToDelete = TestDataFactory.createCommentEntity(id = 1L, roundId = 1L)
        val commentToKeep = TestDataFactory.createCommentEntity(id = 2L, roundId = 1L)

        database.comments().insert(commentToDelete)
        database.comments().insert(commentToKeep)

        // Act
        database.comments().delete(commentToDelete)

        // Assert
        val roundComments = database.comments().getCommentsForRound(1L)
        assertEquals("Should have one comment left", 1, roundComments.size)
        assertEquals("Kept comment should still exist", 2L, roundComments.first().id)
    }

    @Test
    fun `deleteCommentsForRound removes all comments for round correctly`() = runTest {
        // Arrange
        val round1Comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, roundId = 1L),
            TestDataFactory.createCommentEntity(id = 2L, roundId = 1L),
            TestDataFactory.createCommentEntity(id = 3L, roundId = 1L)
        )

        val round2Comments = listOf(
            TestDataFactory.createCommentEntity(id = 4L, roundId = 2L),
            TestDataFactory.createCommentEntity(id = 5L, roundId = 2L)
        )

        val allComments = round1Comments + round2Comments
        database.comments().insertComments(allComments)

        // Act
        database.comments().deleteCommentsForRound(1L)

        // Assert
        val round1Results = database.comments().getCommentsForRound(1L)
        val round2Results = database.comments().getCommentsForRound(2L)

        assertEquals("Round 1 should have no comments", 0, round1Results.size)
        assertEquals("Round 2 should still have comments", 2, round2Results.size)
    }

    @Test
    fun `insert handles comments with special characters correctly`() = runTest {
        // Arrange
        val specialComment = TestDataFactory.createCommentEntity(
            id = 1L,
            text = "Comment with spécial çharácters and émojis 🚀!",
            tags = "spécial,émojis,tëst"
        )

        // Act
        database.comments().insert(specialComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Special characters should be preserved",
            "Comment with spécial çharácters and émojis 🚀!", retrieved.first().text)
        assertEquals("Special tags should be preserved",
            "spécial,émojis,tëst", retrieved.first().tags)
    }

    @Test
    fun `insert handles very long comment text correctly`() = runTest {
        // Arrange
        val longText = "A".repeat(1000) + " Very Long Comment Text " + "B".repeat(1000)
        val longComment = TestDataFactory.createCommentEntity(
            id = 1L,
            text = longText
        )

        // Act
        database.comments().insert(longComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Long text should be preserved", longText, retrieved.first().text)
    }

    @Test
    fun `insert handles comments with very long tags correctly`() = runTest {
        // Arrange
        val longTags = List(50) { i -> "tag$i" }.joinToString(",")
        val longTagsComment = TestDataFactory.createCommentEntity(
            id = 1L,
            tags = longTags
        )

        // Act
        database.comments().insert(longTagsComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Long tags should be preserved", longTags, retrieved.first().tags)
    }

    @Test
    fun `insert handles comments with empty text correctly`() = runTest {
        // Arrange
        val emptyTextComment = TestDataFactory.createCommentEntity(
            id = 1L,
            text = ""
        )

        // Act
        database.comments().insert(emptyTextComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Empty text should be preserved", "", retrieved.first().text)
    }

    @Test
    fun `insert handles comments with empty tags correctly`() = runTest {
        // Arrange
        val emptyTagsComment = TestDataFactory.createCommentEntity(
            id = 1L,
            tags = ""
        )

        // Act
        database.comments().insert(emptyTagsComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Empty tags should be preserved", "", retrieved.first().tags)
    }

    @Test
    fun `insert handles comments with single tag correctly`() = runTest {
        // Arrange
        val singleTagComment = TestDataFactory.createCommentEntity(
            id = 1L,
            tags = "single"
        )

        // Act
        database.comments().insert(singleTagComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Single tag should be preserved", "single", retrieved.first().tags)
    }

    @Test
    fun `insert handles comments with minimum valid values correctly`() = runTest {
        // Arrange
        val minimalComment = TestDataFactory.createCommentEntity(
            id = 1L,
            roundId = 1L,
            text = "A",
            tags = "a",
            createdAt = 0L
        )

        // Act
        database.comments().insert(minimalComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Minimal text should be preserved", "A", retrieved.first().text)
        assertEquals("Minimal tags should be preserved", "a", retrieved.first().tags)
        assertEquals("Minimal createdAt should be preserved", 0L, retrieved.first().createdAt)
    }

    @Test
    fun `insert handles comments with maximum valid values correctly`() = runTest {
        // Arrange
        val maximalComment = TestDataFactory.createCommentEntity(
            id = 1L,
            roundId = Long.MAX_VALUE,
            text = "A".repeat(1000),
            tags = List(100) { "tag$it" }.joinToString(","),
            createdAt = Long.MAX_VALUE
        )

        // Act
        database.comments().insert(maximalComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(Long.MAX_VALUE).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Maximal text should be preserved", "A".repeat(1000), retrieved.first().text)
        assertEquals("Maximal tags should be preserved", List(100) { "tag$it" }.joinToString(","), retrieved.first().tags)
        assertEquals("Maximal createdAt should be preserved", Long.MAX_VALUE, retrieved.first().createdAt)
    }

    @Test
    fun `getCommentsWithTag handles multiple tags correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor,joke"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "funny,clever"),
            TestDataFactory.createCommentEntity(id = 3L, tags = "serious,analysis"),
            TestDataFactory.createCommentEntity(id = 4L, tags = "creative,funny")
        )
        database.comments().insertComments(comments)

        // Act
        val funnyResults = database.comments().getCommentsWithTag("funny")

        // Assert
        assertEquals("Should return comments with funny tag", 3, funnyResults.size)
        funnyResults.forEach { comment ->
            assertTrue("All comments should contain funny tag",
                comment.tags.contains("funny"))
        }
    }

    @Test
    fun `getCommentsWithTag handles case sensitivity correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "FUNNY,joke"),
            TestDataFactory.createCommentEntity(id = 3L, tags = "Funny,clever")
        )
        database.comments().insertComments(comments)

        // Act
        val funnyResults = database.comments().getCommentsWithTag("funny")
        val funnyUpperResults = database.comments().getCommentsWithTag("FUNNY")
        val funnyMixedResults = database.comments().getCommentsWithTag("Funny")

        // Assert
        assertEquals("Should find lowercase funny", 3, funnyResults.size)
        assertEquals("Should find uppercase FUNNY", 1, funnyUpperResults.size)
        assertEquals("Should find mixed case Funny", 1, funnyMixedResults.size)
    }

    @Test
    fun `getCommentsWithTag handles partial tag matches correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "fun,funny"),
            TestDataFactory.createCommentEntity(id = 3L, tags = "unfunny,serious")
        )
        database.comments().insertComments(comments)

        // Act
        val funResults = database.comments().getCommentsWithTag("fun")

        // Assert
        assertEquals("Should find comments with partial tag matches", 2, funResults.size)
        funResults.forEach { comment ->
            assertTrue("All comments should contain 'fun' in tags",
                comment.tags.contains("fun"))
        }
    }

    @Test
    fun `getAllTags handles duplicate tags correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor,funny"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "funny,serious"),
            TestDataFactory.createCommentEntity(id = 3L, tags = "funny,humor")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return unique tags only", 3, allTags.size)
        assertTrue("Should contain funny tag", allTags.contains("funny"))
        assertTrue("Should contain humor tag", allTags.contains("humor"))
        assertTrue("Should contain serious tag", allTags.contains("serious"))
    }

    @Test
    fun `getAllTags handles tags with spaces correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "tag with spaces,another tag"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "normal tag, tag with spaces ")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return all unique tags including those with spaces", 3, allTags.size)
        assertTrue("Should contain 'tag with spaces'", allTags.contains("tag with spaces"))
        assertTrue("Should contain 'another tag'", allTags.contains("another tag"))
        assertTrue("Should contain 'normal tag'", allTags.contains("normal tag"))
    }

    @Test
    fun `getAllTags handles tags with special characters correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "spécial,émojis,tëst"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "ünícódé,ñormal")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return all unique special character tags", 5, allTags.size)
        assertTrue("Should contain spécial", allTags.contains("spécial"))
        assertTrue("Should contain émojis", allTags.contains("émojis"))
        assertTrue("Should contain tëst", allTags.contains("tëst"))
        assertTrue("Should contain ünícódé", allTags.contains("ünícódé"))
        assertTrue("Should contain ñormal", allTags.contains("ñormal"))
    }

    @Test
    fun `getCommentsForRound returns comments in chronological order`() = runTest {
        // Arrange
        val baseTime = 1000L
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, roundId = 1L, text = "First comment", createdAt = baseTime + 3000),
            TestDataFactory.createCommentEntity(id = 2L, roundId = 1L, text = "Second comment", createdAt = baseTime + 1000),
            TestDataFactory.createCommentEntity(id = 3L, roundId = 1L, text = "Third comment", createdAt = baseTime + 2000)
        )
        database.comments().insertComments(comments)

        // Act
        val roundComments = database.comments().getCommentsForRound(1L)

        // Assert
        assertEquals("Should return all comments", 3, roundComments.size)
        assertEquals("First comment should be earliest", "Second comment", roundComments[0].text)
        assertEquals("Second comment should be middle", "Third comment", roundComments[1].text)
        assertEquals("Third comment should be latest", "First comment", roundComments[2].text)
    }

    @Test
    fun `getCommentsForRound handles comments with same timestamp correctly`() = runTest {
        // Arrange
        val sameTime = 1000L
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, roundId = 1L, text = "Comment A", createdAt = sameTime),
            TestDataFactory.createCommentEntity(id = 2L, roundId = 1L, text = "Comment B", createdAt = sameTime),
            TestDataFactory.createCommentEntity(id = 3L, roundId = 1L, text = "Comment C", createdAt = sameTime)
        )
        database.comments().insertComments(comments)

        // Act
        val roundComments = database.comments().getCommentsForRound(1L)

        // Assert
        assertEquals("Should return all comments", 3, roundComments.size)
        // Order might vary for same timestamp, but all should be present
        val texts = roundComments.map { it.text }
        assertTrue("Should contain Comment A", texts.contains("Comment A"))
        assertTrue("Should contain Comment B", texts.contains("Comment B"))
        assertTrue("Should contain Comment C", texts.contains("Comment C"))
    }

    @Test
    fun `deleteCommentsForRound handles non-existent round correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, roundId = 1L),
            TestDataFactory.createCommentEntity(id = 2L, roundId = 2L)
        )
        database.comments().insertComments(comments)

        // Act
        database.comments().deleteCommentsForRound(999L) // Non-existent round

        // Assert
        val round1Comments = database.comments().getCommentsForRound(1L)
        val round2Comments = database.comments().getCommentsForRound(2L)

        assertEquals("Round 1 should still have comments", 1, round1Comments.size)
        assertEquals("Round 2 should still have comments", 1, round2Comments.size)
    }

    @Test
    fun `getAll returns all comments correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, roundId = 1L),
            TestDataFactory.createCommentEntity(id = 2L, roundId = 2L),
            TestDataFactory.createCommentEntity(id = 3L, roundId = 3L)
        )
        database.comments().insertComments(comments)

        // Act
        val allComments = database.comments().getAll()

        // Assert
        assertEquals("Should return all comments", 3, allComments.size)
        comments.forEach { expectedComment ->
            assertTrue("All comments should be present",
                allComments.any { it.id == expectedComment.id })
        }
    }

    @Test
    fun `getAll returns empty list when no comments exist`() = runTest {
        // Act
        val allComments = database.comments().getAll()

        // Assert
        assertNotNull("Results should not be null", allComments)
        assertTrue("Results should be empty when no comments exist", allComments.isEmpty())
    }

    @Test
    fun `insert handles comments with null round ID correctly`() = runTest {
        // Arrange
        val nullRoundComment = TestDataFactory.createCommentEntity(
            id = 1L,
            roundId = 0L, // Assuming 0 represents null in some contexts
            text = "Null round comment"
        )

        // Act
        database.comments().insert(nullRoundComment)

        // Assert
        val allComments = database.comments().getAll()
        assertEquals("Should have one comment", 1, allComments.size)
        assertEquals("Comment should be stored with null round ID", 0L, allComments.first().roundId)
    }

    @Test
    fun `getCommentsWithTag handles empty tag correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = ""),
            TestDataFactory.createCommentEntity(id = 2L, tags = "normal"),
            TestDataFactory.createCommentEntity(id = 3L, tags = "")
        )
        database.comments().insertComments(comments)

        // Act
        val emptyTagResults = database.comments().getCommentsWithTag("")

        // Assert
        assertEquals("Should return comments with empty tags", 2, emptyTagResults.size)
        emptyTagResults.forEach { comment ->
            assertEquals("All comments should have empty tags", "", comment.tags)
        }
    }

    @Test
    fun `getCommentsWithTag handles whitespace in tags correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = " funny , humor , joke "),
            TestDataFactory.createCommentEntity(id = 2L, tags = "funny,humor"),
            TestDataFactory.createCommentEntity(id = 3L, tags = " serious , analysis ")
        )
        database.comments().insertComments(comments)

        // Act
        val funnyResults = database.comments().getCommentsWithTag("funny")

        // Assert
        assertEquals("Should find comments with whitespace in tags", 2, funnyResults.size)
        funnyResults.forEach { comment ->
            assertTrue("All comments should contain funny tag",
                comment.tags.contains("funny"))
        }
    }

    @Test
    fun `getAllTags handles empty and whitespace tags correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor"),
            TestDataFactory.createCommentEntity(id = 2L, tags = ""),
            TestDataFactory.createCommentEntity(id = 3L, tags = "   "),
            TestDataFactory.createCommentEntity(id = 4L, tags = "serious,")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return only non-empty tags", 3, allTags.size)
        assertTrue("Should contain funny tag", allTags.contains("funny"))
        assertTrue("Should contain humor tag", allTags.contains("humor"))
        assertTrue("Should contain serious tag", allTags.contains("serious"))
    }

    @Test
    fun `insert handles comments with future timestamp correctly`() = runTest {
        // Arrange
        val futureTime = System.currentTimeMillis() + 1000000
        val futureComment = TestDataFactory.createCommentEntity(
            id = 1L,
            createdAt = futureTime
        )

        // Act
        database.comments().insert(futureComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Future timestamp should be preserved", futureTime, retrieved.first().createdAt)
    }

    @Test
    fun `insert handles comments with zero timestamp correctly`() = runTest {
        // Arrange
        val zeroTimeComment = TestDataFactory.createCommentEntity(
            id = 1L,
            createdAt = 0L
        )

        // Act
        database.comments().insert(zeroTimeComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Zero timestamp should be preserved", 0L, retrieved.first().createdAt)
    }

    @Test
    fun `getCommentsForRound handles large number of comments correctly`() = runTest {
        // Arrange
        val largeNumberOfComments = (1..1000).map { i ->
            TestDataFactory.createCommentEntity(id = i.toLong(), roundId = 1L, text = "Comment $i")
        }
        database.comments().insertComments(largeNumberOfComments)

        // Act
        val roundComments = database.comments().getCommentsForRound(1L)

        // Assert
        assertEquals("Should return all comments", 1000, roundComments.size)
        assertEquals("First comment should be correct", "Comment 1", roundComments.first().text)
        assertEquals("Last comment should be correct", "Comment 1000", roundComments.last().text)
    }

    @Test
    fun `getCommentsWithTag handles large number of comments correctly`() = runTest {
        // Arrange
        val funnyComments = (1..500).map { i ->
            TestDataFactory.createCommentEntity(id = i.toLong(), tags = "funny,humor")
        }
        val seriousComments = (501..1000).map { i ->
            TestDataFactory.createCommentEntity(id = i.toLong(), tags = "serious,analysis")
        }
        val allComments = funnyComments + seriousComments
        database.comments().insertComments(allComments)

        // Act
        val funnyResults = database.comments().getCommentsWithTag("funny")

        // Assert
        assertEquals("Should return all funny comments", 500, funnyResults.size)
        funnyResults.forEach { comment ->
            assertTrue("All comments should contain funny tag",
                comment.tags.contains("funny"))
        }
    }

    @Test
    fun `getAllTags handles large number of unique tags correctly`() = runTest {
        // Arrange
        val comments = (1..100).map { i ->
            TestDataFactory.createCommentEntity(id = i.toLong(), tags = "tag$i,common")
        }
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return all unique tags", 101, allTags.size) // 100 unique + 1 common
        assertTrue("Should contain common tag", allTags.contains("common"))
        assertTrue("Should contain tag1", allTags.contains("tag1"))
        assertTrue("Should contain tag100", allTags.contains("tag100"))
    }

    @Test
    fun `deleteCommentsForRound handles large number of comments correctly`() = runTest {
        // Arrange
        val round1Comments = (1..500).map { i ->
            TestDataFactory.createCommentEntity(id = i.toLong(), roundId = 1L)
        }
        val round2Comments = (501..1000).map { i ->
            TestDataFactory.createCommentEntity(id = i.toLong(), roundId = 2L)
        }
        val allComments = round1Comments + round2Comments
        database.comments().insertComments(allComments)

        // Act
        database.comments().deleteCommentsForRound(1L)

        // Assert
        val round1Results = database.comments().getCommentsForRound(1L)
        val round2Results = database.comments().getCommentsForRound(2L)

        assertEquals("Round 1 should have no comments", 0, round1Results.size)
        assertEquals("Round 2 should still have all comments", 500, round2Results.size)
    }

    @Test
    fun `insert handles comments with extremely long text correctly`() = runTest {
        // Arrange
        val extremelyLongText = "A".repeat(10000) + " Extremely Long Comment " + "B".repeat(10000)
        val extremelyLongComment = TestDataFactory.createCommentEntity(
            id = 1L,
            text = extremelyLongText
        )

        // Act
        database.comments().insert(extremelyLongComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Extremely long text should be preserved", extremelyLongText, retrieved.first().text)
    }

    @Test
    fun `insert handles comments with extremely long tags correctly`() = runTest {
        // Arrange
        val extremelyLongTags = List(1000) { i -> "verylongtagnamethatmightcauseissues$i" }.joinToString(",")
        val extremelyLongTagsComment = TestDataFactory.createCommentEntity(
            id = 1L,
            tags = extremelyLongTags
        )

        // Act
        database.comments().insert(extremelyLongTagsComment)

        // Assert
        val retrieved = database.comments().getCommentsForRound(1L).first()
        assertEquals("Should have one comment", 1, retrieved.size)
        assertEquals("Extremely long tags should be preserved", extremelyLongTags, retrieved.first().tags)
    }

    @Test
    fun `getCommentsWithTag handles multiple simultaneous tag searches correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "funny,humor,joke"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "funny,clever,witty"),
            TestDataFactory.createCommentEntity(id = 3L, tags = "serious,analysis,thoughtful"),
            TestDataFactory.createCommentEntity(id = 4L, tags = "creative,funny,artistic")
        )
        database.comments().insertComments(comments)

        // Act - Search for multiple tags simultaneously
        val funnyResults = database.comments().getCommentsWithTag("funny")
        val humorResults = database.comments().getCommentsWithTag("humor")
        val seriousResults = database.comments().getCommentsWithTag("serious")

        // Assert
        assertEquals("Funny tag should match 3 comments", 3, funnyResults.size)
        assertEquals("Humor tag should match 1 comment", 1, humorResults.size)
        assertEquals("Serious tag should match 1 comment", 1, seriousResults.size)

        // Verify no overlap issues
        val funnyIds = funnyResults.map { it.id }.toSet()
        val humorIds = humorResults.map { it.id }.toSet()
        val seriousIds = seriousResults.map { it.id }.toSet()

        assertTrue("Comment 1 should be in funny results", funnyIds.contains(1L))
        assertTrue("Comment 1 should be in humor results", humorIds.contains(1L))
        assertFalse("Comment 1 should not be in serious results", seriousIds.contains(1L))
    }

    @Test
    fun `getAllTags handles edge case with only commas in tags correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = ",,,"),
            TestDataFactory.createCommentEntity(id = 2L, tags = ",tag,,")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return only valid tags", 1, allTags.size)
        assertTrue("Should contain tag", allTags.contains("tag"))
    }

    @Test
    fun `getAllTags handles edge case with tags containing commas correctly`() = runTest {
        // Arrange
        val comments = listOf(
            TestDataFactory.createCommentEntity(id = 1L, tags = "tag,with,commas"),
            TestDataFactory.createCommentEntity(id = 2L, tags = "tag with, spaces and commas")
        )
        database.comments().insertComments(comments)

        // Act
        val allTags = database.comments().getAllTags()

        // Assert
        assertEquals("Should return all tags correctly", 5, allTags.size)
        assertTrue("Should contain 'tag'", allTags.contains("tag"))
        assertTrue("Should contain 'with'", allTags.contains("with"))
        assertTrue("Should contain 'commas'", allTags.contains("commas"))
        assertTrue("Should contain 'tag with'", allTags.contains("tag with"))
        assertTrue("Should contain 'spaces and commas'", allTags.contains("spaces and commas"))
    }
}