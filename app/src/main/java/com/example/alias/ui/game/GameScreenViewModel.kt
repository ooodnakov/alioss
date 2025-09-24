package com.example.alias.ui.game

import com.example.alias.WordInfo
import kotlinx.coroutines.flow.StateFlow

/** Contract required by [gameScreen] to render state and dispatch actions. */
interface GameScreenViewModel {
    val showTutorialOnFirstTurn: StateFlow<Boolean>
    val wordInfoByText: StateFlow<Map<String, WordInfo>>

    fun dismissTutorialOnFirstTurn()
    fun updateSeenTutorial(value: Boolean)
    fun restartMatch()
    fun startTurn()
    fun nextTurn()
    fun overrideOutcome(index: Int, correct: Boolean)
}
