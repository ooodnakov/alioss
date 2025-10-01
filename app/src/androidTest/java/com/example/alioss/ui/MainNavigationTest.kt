package com.example.alioss.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.alioss.MainActivity
import com.example.alioss.R
import com.example.alioss.data.DeckRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@HiltAndroidTest
class MainNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var deckRepository: DeckRepository

    private lateinit var sampleDeckName: String

    @Before
    fun setUp() {
        hiltRule.inject()
        sampleDeckName = runBlocking {
            deckRepository.getDecks()
                .first { it.isNotEmpty() }
                .first()
                .name
        }
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

        waitForText(sampleDeckName)
        composeRule.onNodeWithText(sampleDeckName, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun waitForText(value: String, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodes(hasText(value), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
