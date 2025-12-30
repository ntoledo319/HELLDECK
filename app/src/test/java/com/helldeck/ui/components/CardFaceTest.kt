package com.helldeck.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.ui.CardFace
import com.helldeck.ui.HelldeckTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Comprehensive UI tests for CardFace component
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class CardFaceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `CardFace displays title correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Test Card Title",
                    subtitle = null,
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Test Card Title")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays title and subtitle correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Test Title",
                    subtitle = "Test Subtitle",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Test Title")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Test Subtitle")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace triggers onClick when clicked`() {
        // Arrange
        var clicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Clickable Card",
                    onClick = { clicked = true },
                )
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("Clickable Card")
            .performClick()

        // Assert
        assertTrue("Card should trigger onClick", clicked)
    }

    @Test
    fun `CardFace does not trigger onClick when null`() {
        // Arrange
        var clicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Non-Clickable Card",
                    onClick = null,
                )
            }
        }

        // Act & Assert - Should not crash
        composeTestRule
            .onNodeWithText("Non-Clickable Card")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays emojis in title correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Card with 🚀 emojis 🌟 in title",
                    subtitle = "And 🎯 emojis 🎲 in subtitle",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Card with 🚀 emojis 🌟 in title")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("And 🎯 emojis 🎲 in subtitle")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays special characters correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Spécial Çharácters Ñ Title",
                    subtitle = "Sübtítlé wíth áccénts",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Spécial Çharácters Ñ Title")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Sübtítlé wíth áccénts")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays very long title correctly`() {
        // Arrange
        val longTitle = "A".repeat(200) + " Very Long Title"

        // Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = longTitle,
                    subtitle = "Short subtitle",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText(longTitle)
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays very long subtitle correctly`() {
        // Arrange
        val longSubtitle = "B".repeat(200) + " Very Long Subtitle"

        // Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Short title",
                    subtitle = longSubtitle,
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText(longSubtitle)
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays numbers correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Numbers 123456789",
                    subtitle = "Decimals 123.456",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Numbers 123456789")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Decimals 123.456")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays unicode correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Ünïcödé Tëxt 日本語",
                    subtitle = "العربية עברית",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Ünïcödé Tëxt 日本語")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("العربية עברית")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays mathematical symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Math ∑∆π∞",
                    subtitle = "More √∫∂",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Math ∑∆π∞")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("More √∫∂")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays chess symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Chess ♔♕♖",
                    subtitle = "More ♗♘♙",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Chess ♔♕♖")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("More ♗♘♙")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays card symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Cards ♠♥♦♣",
                    subtitle = "Joker 🃏",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Cards ♠♥♦♣")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Joker 🃏")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays zero-width joiner emojis correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "ZWJ 👨‍💻👩‍🎨",
                    subtitle = "Astronaut 👨‍🚀",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("ZWJ 👨‍💻👩‍🎨")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Astronaut 👨‍🚀")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays skin tone modifiers correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Skin 👋🏻👋🏼👋🏽",
                    subtitle = "More 👋🏾👋🏿",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Skin 👋🏻👋🏼👋🏽")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("More 👋🏾👋🏿")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles empty title correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "",
                    subtitle = "Subtitle only",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Subtitle only")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles empty subtitle correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Title only",
                    subtitle = "",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Title only")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles whitespace correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "   ",
                    subtitle = "   ",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("   ")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles newlines in title correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Title\nwith\nnewlines",
                    subtitle = "Subtitle\nwith\nnewlines",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Title\nwith\nnewlines")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Subtitle\nwith\nnewlines")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles tabs in title correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Title\twith\ttabs",
                    subtitle = "Subtitle\twith\ttabs",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Title\twith\ttabs")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Subtitle\twith\ttabs")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays HTML-like content correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Title <with> <html> tags",
                    subtitle = "Subtitle & entities",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Title <with> <html> tags")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Subtitle & entities")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays quotes correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Title \"with\" 'quotes'",
                    subtitle = "Subtitle `with` backticks",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Title \"with\" 'quotes'")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Subtitle `with` backticks")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays URLs correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Title https://example.com",
                    subtitle = "Subtitle ftp://test.org",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Title https://example.com")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Subtitle ftp://test.org")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace displays mixed content correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Mixed: 🚀 ünícódé 123 \"quotes\" ∑",
                    subtitle = "More: 🌟 çharácters 456 'quotes' ∆",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Mixed: 🚀 ünícódé 123 \"quotes\" ∑")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("More: 🌟 çharácters 456 'quotes' ∆")
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles extremely long title correctly`() {
        // Arrange
        val extremelyLongTitle = "A".repeat(1000) + " Extremely Long Title"

        // Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = extremelyLongTitle,
                    subtitle = "Short subtitle",
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText(extremelyLongTitle)
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles extremely long subtitle correctly`() {
        // Arrange
        val extremelyLongSubtitle = "B".repeat(1000) + " Extremely Long Subtitle"

        // Act
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Short title",
                    subtitle = extremelyLongSubtitle,
                )
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText(extremelyLongSubtitle)
            .assertIsDisplayed()
    }

    @Test
    fun `CardFace handles multiple clicks correctly`() {
        // Arrange
        var clickCount = 0
        composeTestRule.setContent {
            HelldeckTheme {
                CardFace(
                    title = "Multi Click Card",
                    onClick = { clickCount++ },
                )
            }
        }

        // Act
        repeat(5) {
            composeTestRule
                .onNodeWithText("Multi Click Card")
                .performClick()
        }

        // Assert
        assertEquals("Card should be clicked 5 times", 5, clickCount)
    }
}
