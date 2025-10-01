package com.example.alioss.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.alioss.MainActivity
import com.example.alioss.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_displaysQuickPlayAndDecksCard() {
        val quickPlay = composeRule.activity.getString(R.string.quick_play)
        val decksTitle = composeRule.activity.getString(R.string.title_decks)

        waitForText(quickPlay)
        composeRule.onNodeWithText(quickPlay, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(decksTitle, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun navigateToGame_showsStartTurnButton() {
        val quickPlay = composeRule.activity.getString(R.string.quick_play)
        val startTurn = composeRule.activity.getString(R.string.start_turn)

        waitForText(quickPlay)
        composeRule.onNodeWithText(quickPlay, useUnmergedTree = true).performClick()

        waitForText(startTurn)
        composeRule.onNodeWithText(startTurn, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun navigateToDecks_showsBundledDeck() {
        val decksTitle = composeRule.activity.getString(R.string.title_decks)

        waitForText(decksTitle)
        composeRule.onNodeWithText(decksTitle, useUnmergedTree = true).performClick()

        waitForText("Sample (EN)")
        composeRule.onNodeWithText("Sample (EN)", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun waitForText(value: String, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodes(hasText(value), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
