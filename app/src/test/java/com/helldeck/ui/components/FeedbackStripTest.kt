package com.helldeck.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.helldeck.ui.FeedbackStrip
import com.helldeck.ui.HelldeckTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive UI tests for FeedbackStrip component
 */
class FeedbackStripTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `FeedbackStrip displays all feedback buttons correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip()
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("😂 BANGER")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("😐 MEH")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("🚮 TRASH")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("✍️ WHY")
            .assertIsDisplayed()
    }

    @Test
    fun `FeedbackStrip LOL button triggers onLol callback`() {
        // Arrange
        var lolClicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(onLol = { lolClicked = true })
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("😂 BANGER")
            .performClick()

        // Assert
        assertTrue("LOL button should trigger onLol", lolClicked)
    }

    @Test
    fun `FeedbackStrip MEH button triggers onMeh callback`() {
        // Arrange
        var mehClicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(onMeh = { mehClicked = true })
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("😐 MEH")
            .performClick()

        // Assert
        assertTrue("MEH button should trigger onMeh", mehClicked)
    }

    @Test
    fun `FeedbackStrip TRASH button triggers onTrash callback`() {
        // Arrange
        var trashClicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(onTrash = { trashClicked = true })
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("🚮 TRASH")
            .performClick()

        // Assert
        assertTrue("TRASH button should trigger onTrash", trashClicked)
    }

    @Test
    fun `FeedbackStrip handles multiple feedback clicks correctly`() {
        // Arrange
        var lolCount = 0
        var mehCount = 0
        var trashCount = 0

        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(
                    onLol = { lolCount++ },
                    onMeh = { mehCount++ },
                    onTrash = { trashCount++ }
                )
            }
        }

        // Act
        repeat(3) {
            composeTestRule
                .onNodeWithText("😂 BANGER")
                .performClick()
        }
        repeat(2) {
            composeTestRule
                .onNodeWithText("😐 MEH")
                .performClick()
        }
        repeat(1) {
            composeTestRule
                .onNodeWithText("🚮 TRASH")
                .performClick()
        }

        // Assert
        assertEquals("LOL should be clicked 3 times", 3, lolCount)
        assertEquals("MEH should be clicked 2 times", 2, mehCount)
        assertEquals("TRASH should be clicked 1 time", 1, trashCount)
    }

    @Test
    fun `FeedbackStrip comment section displays when showComments is true`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(showComments = true)
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("optional note…")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Save Feedback")
            .assertIsDisplayed()
    }

    @Test
    fun `FeedbackStrip comment section hidden when showComments is false`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(showComments = false)
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("optional note…")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Save Feedback")
            .assertDoesNotExist()
    }

    @Test
    fun `FeedbackStrip displays available tags when showComments is true`() {
        // Arrange
        val testTags = listOf("funny", "clever", "witty", "creative", "random")

        // Act
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(
                    showComments = true,
                    availableTags = testTags
                )
            }
        }

        // Assert
        testTags.forEach { tag ->
            composeTestRule
                .onNodeWithText("□ $tag", substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `FeedbackStrip tag selection updates correctly`() {
        // Arrange
        var toggledTag: String? = null
        val testTags = listOf("funny", "clever", "witty")

        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(
                    showComments = true,
                    availableTags = testTags,
                    onTagToggle = { tag -> toggledTag = tag }
                )
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("□ funny", substring = true)
            .performClick()

        // Assert
        assertEquals("Tag toggle should pass correct tag", "funny", toggledTag)
    }

    @Test
    fun `FeedbackStrip comment text change updates correctly`() {
        // Arrange
        var commentText = ""
        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(
                    showComments = true,
                    commentText = commentText,
                    onCommentTextChange = { commentText = it }
                )
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("optional note…")
            .performTextInput("Test comment")

        // Assert
        assertEquals("Comment text should update", "Test comment", commentText)
    }

    @Test
    fun `FeedbackStrip save button triggers onComment callback`() {
        // Arrange
        var savedComment = ""
        var savedTags = emptySet<String>()

        composeTestRule.setContent {
            HelldeckTheme {
                FeedbackStrip(
                    showComments = true,
                    commentText = "Test comment",
                    selectedTags = setOf("funny", "clever"),
                    onComment = { text, tags ->
                        savedComment = text
                        savedTags = tags
                    }
                )
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("Save Feedback")
            .performClick()

        // Assert
        assertEquals("Comment text should be saved", "Test comment", savedComment)
        assertEquals("Tags should be saved", setOf("funny", "clever"), savedTags)
    }
}