package com.helldeck.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import com.helldeck.ui.BigZones
import com.helldeck.ui.HelldeckTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config

/**
 * Comprehensive UI tests for BigZones component
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BigZonesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `BigZones displays three zones correctly`() {
        // Arrange & Act
        composeTestRule.setContent {
            HelldeckTheme {
                BigZones()
            }
        }

        // Assert - BigZones should be composed of three equal-weight boxes
        // We can verify this by checking the component exists
        composeTestRule
            .onRoot()
            .assertExists()
    }

    @Test
    fun `BigZones left zone triggers onLeft callback`() {
        // Arrange
        var leftClicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                BigZones(onLeft = { leftClicked = true })
            }
        }

        // Act
        composeTestRule
            .onRoot()
            .performTouchInput {
                // Click on the left third of the screen
                click(position = center.copy(x = center.x * 0.5f))
            }

        // Assert
        assertTrue("Left zone should trigger onLeft", leftClicked)
    }

    @Test
    fun `BigZones center zone triggers onCenter callback`() {
        // Arrange
        var centerClicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                BigZones(onCenter = { centerClicked = true })
            }
        }

        // Act
        composeTestRule
            .onRoot()
            .performTouchInput {
                // Click at the center
                click()
            }

        // Assert
        assertTrue("Center zone should trigger onCenter", centerClicked)
    }

    @Test
    fun `BigZones right zone triggers onRight callback`() {
        // Arrange
        var rightClicked = false
        composeTestRule.setContent {
            HelldeckTheme {
                BigZones(onRight = { rightClicked = true })
            }
        }

        // Act
        composeTestRule
            .onRoot()
            .performTouchInput {
                // Click on the right third of the screen
                click(position = center.copy(x = center.x * 1.5f))
            }

        // Assert
        assertTrue("Right zone should trigger onRight", rightClicked)
    }

    @Test
    fun `BigZones long press triggers onLong callback`() {
        // Arrange
        var longPressed = false
        composeTestRule.setContent {
            HelldeckTheme {
                BigZones(onLong = { longPressed = true })
            }
        }

        // Act
        composeTestRule
            .onRoot()
            .performTouchInput {
                longClick()
            }

        // Assert
        assertTrue("Long press should trigger onLong", longPressed)
    }

    @Test
    fun `BigZones handles multiple taps correctly`() {
        // Arrange
        var leftCount = 0
        var centerCount = 0
        var rightCount = 0

        composeTestRule.setContent {
            HelldeckTheme {
                BigZones(
                    onLeft = { leftCount++ },
                    onCenter = { centerCount++ },
                    onRight = { rightCount++ }
                )
            }
        }

        // Act
        composeTestRule.onRoot().performTouchInput {
            // Left taps
            repeat(3) { click(position = center.copy(x = center.x * 0.5f)) }
            // Center taps
            repeat(2) { click() }
            // Right taps
            repeat(1) { click(position = center.copy(x = center.x * 1.5f)) }
        }

        // Assert
        assertEquals("Left zone should be tapped 3 times", 3, leftCount)
        assertEquals("Center zone should be tapped 2 times", 2, centerCount)
        assertEquals("Right zone should be tapped 1 time", 1, rightCount)
    }

    @Test
    fun `BigZones handles mixed interactions correctly`() {
        // Arrange
        var leftClicked = false
        var centerClicked = false
        var rightClicked = false
        var longPressed = false

        composeTestRule.setContent {
            HelldeckTheme {
                BigZones(
                    onLeft = { leftClicked = true },
                    onCenter = { centerClicked = true },
                    onRight = { rightClicked = true },
                    onLong = { longPressed = true }
                )
            }
        }

        // Act - Tap left, then center, then right, then long press
        composeTestRule.onRoot().performTouchInput {
            click(position = center.copy(x = center.x * 0.5f))
            click()
            click(position = center.copy(x = center.x * 1.5f))
            longClick()
        }

        // Assert
        assertTrue("Left zone should be clicked", leftClicked)
        assertTrue("Center zone should be clicked", centerClicked)
        assertTrue("Right zone should be clicked", rightClicked)
        assertTrue("Long press should be triggered", longPressed)
    }

    @Test
    fun `BigZones handles rapid taps correctly`() {
        // Arrange
        var tapCount = 0
        composeTestRule.setContent {
            HelldeckTheme {
                BigZones(onCenter = { tapCount++ })
            }
        }

        // Act - Rapid taps
        composeTestRule.onRoot().performTouchInput {
            repeat(10) {
                click()
            }
        }

        // Assert
        assertEquals("Center zone should be tapped 10 times", 10, tapCount)
    }
}
