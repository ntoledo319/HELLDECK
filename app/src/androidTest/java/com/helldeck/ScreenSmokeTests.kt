package com.helldeck

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.helldeck.ui.screens.*
import com.helldeck.ui.vm.GameNightViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen smoke tests.
 * Ensures all screens can render without crashing.
 */
@RunWith(AndroidJUnit4::class)
class ScreenSmokeTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun houseRulesScreen_renders() {
        composeTestRule.setContent {
            HouseRulesScreen(vm = GameNightViewModel(), onClose = {})
        }
        composeTestRule.onNodeWithText("House Rules").assertExists()
    }

    @Test
    fun groupDnaScreen_renders() {
        composeTestRule.setContent {
            GroupDnaScreen(vm = GameNightViewModel(), onClose = {})
        }
        composeTestRule.onNodeWithText("Group DNA").assertExists()
    }

    @Test
    fun packsScreen_renders() {
        composeTestRule.setContent {
            PacksScreen(vm = GameNightViewModel(), onClose = {})
        }
        composeTestRule.onNodeWithText("Card Packs").assertExists()
    }
}
