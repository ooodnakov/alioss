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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: MainViewModel = hiltViewModel()
                    val engine by vm.engine.collectAsState()
                    if (engine == null) {
                        Text("Loading…")
                    } else {
                        GameScreen(engine!!)
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
