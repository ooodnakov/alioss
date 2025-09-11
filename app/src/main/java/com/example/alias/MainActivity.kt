package com.example.alias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val vm: MainViewModel = hiltViewModel()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onQuickPlay = { nav.navigate("game") },
                                onDecks = { nav.navigate("decks") }
                            )
                        }
                        composable("game") {
                            val engine by vm.engine.collectAsState()
                            if (engine == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Loading…")
                                }
                            } else {
                                GameScreen(engine!!)
                            }
                        }
                        composable("decks") {
                            DecksScreen(vm = vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onQuickPlay: () -> Unit, onDecks: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onQuickPlay, modifier = Modifier.fillMaxWidth()) { Text("Quick Play") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDecks, modifier = Modifier.fillMaxWidth()) { Text("Decks") }
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

@Composable
private fun DecksScreen(vm: MainViewModel) {
    val decks by vm.decks.collectAsState()
    val enabled by vm.enabledDeckIds.collectAsState()
    val trusted by vm.trustedSources.collectAsState()
    val status by vm.downloadStatus.collectAsState()
    var url by rememberSaveable { mutableStateOf("") }
    var sha by rememberSaveable { mutableStateOf("") }
    var newTrusted by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Decks", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        if (decks.isEmpty()) {
            Text("No decks installed")
        } else {
            decks.forEach { deck ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(deck.name)
                        Text(deck.language, style = MaterialTheme.typography.bodySmall)
                    }
                    val isEnabled = enabled.contains(deck.id)
                    Button(onClick = { vm.setDeckEnabled(deck.id, !isEnabled) }) {
                        Text(if (isEnabled) "Disable" else "Enable")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Download Pack", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        TextField(value = url, onValueChange = { url = it }, label = { Text("HTTPS URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextField(value = sha, onValueChange = { sha = it }, label = { Text("Expected SHA-256 (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.downloadPackFromUrl(url, sha) }) { Text("Download & Import") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = {
                // Add host of URL to trusted list for convenience
                runCatching {
                    val host = java.net.URI(url).host ?: ""
                    if (host.isNotBlank()) vm.addTrustedSource(host)
                }
            }) { Text("Trust Host") }
        }
        if (!status.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(status!!, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))
        Text("Trusted Sources", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        trusted.forEach { entry ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(entry, modifier = Modifier.weight(1f))
                Button(onClick = { vm.removeTrustedSource(entry) }) { Text("Remove") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = newTrusted, onValueChange = { newTrusted = it }, label = { Text("Add host/origin") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newTrusted.isNotBlank()) { vm.addTrustedSource(newTrusted.trim()); newTrusted = "" }
            }) { Text("Add") }
        }
    }
}
