package com.example.alias

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.MatchConfig
import com.example.alias.data.settings.Settings

@RunWith(RobolectricTestRunner::class)
class GameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonsClickable() {
        val engine = FakeEngine()
        val settings = Settings()
        composeRule.setContent {
            GameScreen(engine, settings, onRestart = {})
        }
        composeRule.onNodeWithText("Correct").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Skip").assertIsDisplayed().performClick()
        assert(engine.correctCalled)
        assert(engine.skipCalled)
    }
}

private class FakeEngine : GameEngine {
    private val _state = MutableStateFlow<GameState>(
        GameState.TurnActive(
            team = "Red",
            word = "Test",
            remaining = 10,
            score = 0,
            skipsRemaining = 3,
            timeRemaining = 60,
            totalSeconds = 60,
        )
    )
    override val state = _state
    var correctCalled = false
    var skipCalled = false
    override fun startMatch(config: MatchConfig, teams: List<String>, seed: Long) {}
    override fun correct() { correctCalled = true }
    override fun skip() { skipCalled = true }
    override fun nextTurn() {}
}

