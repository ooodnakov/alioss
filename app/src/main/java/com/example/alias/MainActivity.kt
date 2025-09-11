package com.example.alias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.WordDao
import com.example.alias.domain.DefaultGameEngine
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.MatchConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var deckRepository: DeckRepository
    @Inject lateinit var wordDao: WordDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    var engine by remember { mutableStateOf<GameEngine?>(null) }
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(Unit) {
                        val content = context.assets.open("decks/sample_en.json").bufferedReader().use { it.readText() }
                        deckRepository.importJson(content)
                        val words = wordDao.getWordTexts("sample_en")
                        val e = DefaultGameEngine(words, scope)
                        engine = e
                        val config = MatchConfig(targetWords = 10, maxSkips = 3, penaltyPerSkip = 1, roundSeconds = 30)
                        e.startMatch(config, teams = listOf("Red", "Blue"), seed = 0L)
                    }
                    val current = engine
                    if (current == null) {
                        Text("Loading…")
                    } else {
                        GameScreen(current)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameScreen(engine: GameEngine) {
    val state by engine.state.collectAsState()
    when (val s = state) {
        GameState.Idle -> Text("Idle")
        is GameState.TurnActive -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Team: ${s.team}")
                Text(s.word)
                Text("Remaining: ${s.remaining}")
                Text("Score: ${s.score}")
                Text("Skips left: ${s.skipsRemaining}")
                Text("Time left: ${s.timeRemaining}s")
                Button(onClick = { engine.correct() }) { Text("Correct") }
                Button(onClick = { engine.skip() }) { Text("Skip") }
            }
        }
        is GameState.TurnFinished -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Turn finished for ${s.team}")
                Text("Delta: ${s.deltaScore}")
                Text("Scores: ${s.scores}")
                Button(onClick = { engine.nextTurn() }) { Text("Next turn") }
            }
        }
        is GameState.MatchFinished -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Match over")
                Text("Scores: ${s.scores}")
            }
        }
    }
}
