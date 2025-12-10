package com.helldeck.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import com.helldeck.ui.GiantButton
import com.helldeck.ui.HelldeckTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive UI tests for GiantButton component
 */
class GiantButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GiantButton displays text content correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Test Button")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Test Button")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton triggers onClick when clicked`() {
        // Arrange
        var clicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = { clicked = true }) {
                    androidx.compose.material3.Text("Click Me")
                }
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("Click Me")
            .performClick()

        // Assert
        assertTrue("Button should trigger onClick", clicked)
    }

    @Test
    fun `GiantButton is disabled when enabled is false`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(
                    onClick = {},
                    enabled = false
                ) {
                    androidx.compose.material3.Text("Disabled Button")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Disabled Button")
            .assertIsNotEnabled()
    }

    @Test
    fun `GiantButton is enabled when enabled is true`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(
                    onClick = {},
                    enabled = true
                ) {
                    androidx.compose.material3.Text("Enabled Button")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Enabled Button")
            .assertIsEnabled()
    }

    @Test
    fun `GiantButton does not trigger onClick when disabled`() {
        // Arrange
        var clicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(
                    onClick = { clicked = true },
                    enabled = false
                ) {
                    androidx.compose.material3.Text("Disabled Button")
                }
            }
        }

        // Act
        composeTestRule
            .onNodeWithText("Disabled Button")
            .performClick()

        // Assert
        assertFalse("Disabled button should not trigger onClick", clicked)
    }

    @Test
    fun `GiantButton displays complex content correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.foundation.layout.Row {
                        androidx.compose.material3.Text("Icon")
                        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.width(4.dp))
                        androidx.compose.material3.Text("Text")
                    }
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Icon")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Text")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays emojis correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("🚀 Rocket Button 🌟")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("🚀 Rocket Button 🌟")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays special characters correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Spécial Çharácters Ñ")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Spécial Çharácters Ñ")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays very long text correctly`() {
        // Arrange
        val longText = "A".repeat(100) + " Very Long Button Text"

        // Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text(longText)
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText(longText)
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton handles multiple clicks correctly`() {
        // Arrange
        var clickCount = 0
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = { clickCount++ }) {
                    androidx.compose.material3.Text("Multi Click Button")
                }
            }
        }

        // Act
        repeat(5) {
            composeTestRule
                .onNodeWithText("Multi Click Button")
                .performClick()
        }

        // Assert
        assertEquals("Button should be clicked 5 times", 5, clickCount)
    }

    @Test
    fun `GiantButton displays empty text correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays numbers correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("123456789")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("123456789")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays unicode correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Ünïcödé Tëxt 日本語")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Ünïcödé Tëxt 日本語")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays mathematical symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Math ∑∆π∞√∫∂")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Math ∑∆π∞√∫∂")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays chess symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Chess ♔♕♖♗♘♙")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Chess ♔♕♖♗♘♙")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays card symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Cards ♠♥♦♣🃏")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Cards ♠♥♦♣🃏")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays weather symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Weather ☀️🌤️⛅🌦️🌧️")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Weather ☀️🌤️⛅🌦️🌧️")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays food symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Food 🍕🍔🍟🌭🍿")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Food 🍕🍔🍟🌭🍿")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays activity symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Activity ⚽🏀🏈⚾🎾")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Activity ⚽🏀🏈⚾🎾")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays travel symbols correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Travel 🚗✈️🚲⛵🏠")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Travel 🚗✈️🚲⛵🏠")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays country flags correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Flags 🇺🇸🇬🇧🇫🇷🇩🇪🇯🇵")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Flags 🇺🇸🇬🇧🇫🇷🇩🇪🇯🇵")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays zero-width joiner emojis correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("ZWJ 👨‍💻👩‍🎨👨‍🚀")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("ZWJ 👨‍💻👩‍🎨👨‍🚀")
            .assertIsDisplayed()
    }

    @Test
    fun `GiantButton displays skin tone modifiers correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                GiantButton(onClick = {}) {
                    androidx.compose.material3.Text("Skin 👋🏻👋🏼👋🏽👋🏾👋🏿")
                }
            }
        }

        // Assert
        composeTestRule
            .onNodeWithText("Skin 👋🏻👋🏼👋🏽👋🏾👋🏿")
            .assertIsDisplayed()
    }
}
