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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                                onDecks = { nav.navigate("decks") },
                                onSettings = { nav.navigate("settings") }
                            )
                        }
                        composable("game") {
                            val engine by vm.engine.collectAsState()
                            if (engine == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Loading…")
                                }
                            } else {
                                GameScreen(engine!!, onRestart = { vm.restartMatch() })
                            }
                        }
                        composable("decks") {
                            DecksScreen(vm = vm)
                        }
                        composable("settings") {
                            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onQuickPlay: () -> Unit, onDecks: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onQuickPlay, modifier = Modifier.fillMaxWidth()) { Text("Quick Play") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDecks, modifier = Modifier.fillMaxWidth()) { Text("Decks") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
    }
}

@Composable
private fun GameScreen(engine: GameEngine, onRestart: () -> Unit) {
    val state by engine.state.collectAsState()
    when (val s = state) {
        GameState.Idle -> Text("Idle")
        is GameState.TurnActive -> {
            val progress = if (s.totalSeconds > 0) s.timeRemaining.toFloat() / s.totalSeconds else 0f
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                Text("${s.timeRemaining}s", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text("Team: ${s.team}", style = MaterialTheme.typography.titleMedium)
                Text(s.word, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                Text("Remaining: ${s.remaining} • Score: ${s.score} • Skips: ${s.skipsRemaining}")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { engine.correct() }, modifier = Modifier.weight(1f)) { Text("Correct") }
                    Button(onClick = { engine.skip() }, modifier = Modifier.weight(1f)) { Text("Skip") }
                }
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Restart Match") }
            }
        }
        is GameState.TurnFinished -> {
            RoundSummaryScreen(state = s, onNext = { engine.nextTurn() }, onAdjust = { engine.adjustScore(it) })
        }
        is GameState.MatchFinished -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Match over")
                Text("Scores: ${s.scores}")
                Text("Start a new match from Settings or Restart.")
                Button(onClick = onRestart) { Text("Restart Match") }
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

@Composable
private fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    var round by rememberSaveable(s) { mutableStateOf(s.roundSeconds.toString()) }
    var target by rememberSaveable(s) { mutableStateOf(s.targetWords.toString()) }
    var maxSkips by rememberSaveable(s) { mutableStateOf(s.maxSkips.toString()) }
    var penalty by rememberSaveable(s) { mutableStateOf(s.penaltyPerSkip.toString()) }
    var lang by rememberSaveable(s) { mutableStateOf(s.languagePreference) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        TextField(value = round, onValueChange = { round = it }, label = { Text("Round seconds") }, modifier = Modifier.fillMaxWidth())
        TextField(value = target, onValueChange = { target = it }, label = { Text("Target words") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(value = maxSkips, onValueChange = { maxSkips = it }, label = { Text("Max skips") }, modifier = Modifier.weight(1f))
            TextField(value = penalty, onValueChange = { penalty = it }, label = { Text("Penalty/skip") }, modifier = Modifier.weight(1f))
        }
        TextField(value = lang, onValueChange = { lang = it }, label = { Text("Language (e.g., en, ru)") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                vm.updateSettings(
                    roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                    targetWords = target.toIntOrNull() ?: s.targetWords,
                    maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                    penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                    language = lang.ifBlank { s.languagePreference }
                )
            }, modifier = Modifier.weight(1f)) { Text("Save") }
            Button(onClick = { vm.restartMatch(); onBack() }, modifier = Modifier.weight(1f)) { Text("Save & Restart") }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun RoundSummaryScreen(state: GameState.TurnFinished, onNext: () -> Unit, onAdjust: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
    ) {
        Text("Turn summary for ${state.team}", style = MaterialTheme.typography.headlineSmall)
        Text("Score change: ${state.deltaScore}")
        LazyColumn(Modifier.weight(1f)) {
            items(state.results) { r ->
                Text(text = (if (r.correct) "✅" else "❌") + " " + r.word)
            }
        }
        Text("Scores: ${state.scores}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAdjust(-1) }, modifier = Modifier.weight(1f)) { Text("-1") }
            Button(onClick = { onAdjust(1) }, modifier = Modifier.weight(1f)) { Text("+1") }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next team") }
    }
}
