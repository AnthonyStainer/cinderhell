package dev.cinderhell

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LauncherActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<LauncherActivity>()

    @Test
    fun firstRunAndNormalHomeExposeThePrimaryProductActions() {
        waitForHome()
        composeRule.onNodeWithText("One great Doom engine. Your games, one button away.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Play Freedoom").assertIsDisplayed()
        composeRule.onNodeWithText("Import game or mod").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Add mod set").assertIsDisplayed()
    }

    @Test
    fun stablePlayFocusAndDpadNavigationWorkWithoutTouch() {
        waitForHome()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag("play").assertIsFocused()
            }.isSuccess
        }
        composeRule.onNodeWithTag("play").assertIsFocused()
        composeRule.onNodeWithTag("play").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("play").assertDoesNotExistOrIsNotFocused()
    }

    @Test
    fun profileLibraryAndAdvancedRoutesRemainControllerFocusable() {
        waitForHome()
        composeRule.onNodeWithTag("add-profile").performScrollTo().performClick()
        composeRule.onNodeWithText("Create mod profile").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("save-profile").assertIsFocused()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithTag("library").performScrollTo().performClick()
        composeRule.onNodeWithText("Library").assertIsDisplayed()
        composeRule.onNodeWithTag("library-import").assertIsFocused()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.onNodeWithTag("advanced").performScrollTo().performClick()
        composeRule.onNodeWithText("More supported settings").assertIsDisplayed()
        composeRule.onNodeWithTag("apply-handheld").assertIsFocused()
    }

    private fun waitForHome() {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(hasText("Your games")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistOrIsNotFocused() {
    runCatching { assertIsFocused() }.onSuccess {
        throw AssertionError("Node unexpectedly remained focused")
    }
}
