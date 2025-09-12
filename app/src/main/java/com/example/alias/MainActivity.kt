package com.example.alias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import android.os.VibrationEffect
import com.example.alias.data.settings.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.TurnOutcome
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alias.ui.AppScaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
 

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = hiltViewModel()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        AppScaffold(title = "Alias") {
                            HomeScreen(
                                onQuickPlay = { nav.navigate("game") },
                                onDecks = { nav.navigate("decks") },
                                onSettings = { nav.navigate("settings") }
                            )
                        }
                    }
                    composable("game") {
                        val engine by vm.engine.collectAsState()
                        val settings by vm.settings.collectAsState()
                        AppScaffold(title = "Game", onBack = { nav.popBackStack() }) {
                            if (engine == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Loading…")
                                }
                            } else {
                                GameScreen(vm, engine!!, settings)
                            }
                        }
                    }
                    composable("decks") {
                        AppScaffold(title = "Decks", onBack = { nav.popBackStack() }) {
                            DecksScreen(vm = vm)
                        }
                    }
                    composable("settings") {
                        AppScaffold(title = "Settings", onBack = { nav.popBackStack() }) {
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onQuickPlay, modifier = Modifier.fillMaxWidth()) { Text("Quick Play") }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onDecks, modifier = Modifier.fillMaxWidth()) { Text("Decks") }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
    }
}

@Composable
fun GameScreen(vm: MainViewModel, engine: GameEngine, settings: Settings) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    DisposableEffect(settings.orientation) {
        val original = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = when (settings.orientation) {
            "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose { activity?.requestedOrientation = original }
    }
    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    val state by engine.state.collectAsState()
    when (val s = state) {
        GameState.Idle -> Text("Idle")
        is GameState.TurnActive -> {
            val rawProgress = if (s.totalSeconds > 0) s.timeRemaining.toFloat() / s.totalSeconds else 0f
            val progress by animateFloatAsState(rawProgress, label = "timerProgress")
            val targetColor = if (rawProgress > 0.5f) {
                val t = (1 - rawProgress) * 2f
                lerp(Color(0xFF4CAF50), Color(0xFFFFC107), t)
            } else {
                val t = rawProgress * 2f
                lerp(Color(0xFFFFC107), Color(0xFFF44336), 1 - t)
            }
            val barColor by animateColorAsState(targetColor, label = "timerColor")
            LaunchedEffect(s.timeRemaining) {
                if (settings.hapticsEnabled && (s.timeRemaining == 10 || s.timeRemaining == 3)) {
                    val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator?.vibrate(effect)
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(progress = progress, color = barColor, modifier = Modifier.fillMaxWidth())
                Text("${s.timeRemaining}s", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text("Team: ${s.team}", style = MaterialTheme.typography.titleMedium)
                Text(s.word, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                Text("Remaining: ${s.remaining} • Score: ${s.score} • Skips: ${s.skipsRemaining}")
                if (settings.oneHandedLayout) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { engine.correct() }, modifier = Modifier.fillMaxWidth().height(80.dp)) { Text("Correct") }
                        Button(onClick = { engine.skip() }, modifier = Modifier.fillMaxWidth().height(80.dp)) { Text("Skip") }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { engine.correct() }, modifier = Modifier.weight(1f).height(60.dp)) { Text("Correct") }
                        Button(onClick = { engine.skip() }, modifier = Modifier.weight(1f).height(60.dp)) { Text("Skip") }
                    }
                }
                Button(onClick = { vm.restartMatch() }, modifier = Modifier.fillMaxWidth()) { Text("Restart Match") }
            }
        }
        is GameState.TurnFinished -> {
            RoundSummaryScreen(vm = vm, s = s)
        }
        is GameState.MatchFinished -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉 Match over 🎉", style = MaterialTheme.typography.headlineSmall)
                Scoreboard(s.scores)
                Text("Start a new match from Settings or Restart.")
                Button(onClick = { vm.restartMatch() }) { Text("Restart Match") }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Decks", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        if (decks.isEmpty()) {
            Text("No decks installed")
        } else {
            decks.forEach { deck ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(deck.name)
                        Text(deck.language, style = MaterialTheme.typography.bodySmall)
                    }
                    val isEnabled = enabled.contains(deck.id)
                    FilledTonalButton(onClick = { vm.setDeckEnabled(deck.id, !isEnabled) }) {
                        Text(if (isEnabled) "Disable" else "Enable")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Download Pack", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("HTTPS URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = sha, onValueChange = { sha = it }, label = { Text("Expected SHA-256 (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.downloadPackFromUrl(url, sha) }) { Text("Download & Import") }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = {
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { vm.removeTrustedSource(entry) }) { Text("Remove") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newTrusted, onValueChange = { newTrusted = it }, label = { Text("Add host/origin") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = {
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
    var haptics by rememberSaveable(s) { mutableStateOf(s.hapticsEnabled) }
    var oneHand by rememberSaveable(s) { mutableStateOf(s.oneHandedLayout) }
    var orientation by rememberSaveable(s) { mutableStateOf(s.orientation) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = round, onValueChange = { round = it }, label = { Text("Round seconds") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target words") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = maxSkips, onValueChange = { maxSkips = it }, label = { Text("Max skips") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = penalty, onValueChange = { penalty = it }, label = { Text("Penalty/skip") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = lang, onValueChange = { lang = it }, label = { Text("Language (e.g., en, ru)") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Haptics", modifier = Modifier.weight(1f))
            Switch(checked = haptics, onCheckedChange = { haptics = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("One-hand layout", modifier = Modifier.weight(1f))
            Switch(checked = oneHand, onCheckedChange = { oneHand = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Orientation", modifier = Modifier.weight(1f))
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }) { Text(orientation) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("system", "portrait", "landscape").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { orientation = it; expanded = false })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                vm.updateSettings(
                    roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                    targetWords = target.toIntOrNull() ?: s.targetWords,
                    maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                    penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                    language = lang.ifBlank { s.languagePreference },
                    haptics = haptics,
                    oneHanded = oneHand,
                    orientation = orientation,
                )
            }, modifier = Modifier.weight(1f)) { Text("Save") }
            FilledTonalButton(onClick = { vm.restartMatch(); onBack() }, modifier = Modifier.weight(1f)) { Text("Save & Restart") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun RoundSummaryScreen(vm: MainViewModel, s: GameState.TurnFinished) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
    ) {
        Text("Turn summary for ${s.team}", style = MaterialTheme.typography.headlineSmall)
        Text("Score change: ${s.deltaScore}")
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(s.outcomes) { index, o ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(o.word, modifier = Modifier.weight(1f))
                    Text(if (o.correct) "✓" else "✗")
                    Row {
                        Button(onClick = { vm.overrideOutcome(index, true) }) { Text("+") }
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = { vm.overrideOutcome(index, false) }) { Text("-") }
                    }
                }
            }
        }
        Scoreboard(s.scores)
        Button(onClick = { vm.nextTurn() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (s.matchOver) "End Match" else "Next Team")
        }
    }
}

@Composable
private fun Scoreboard(scores: Map<String, Int>) {
    Column(Modifier.fillMaxWidth()) {
        Text("Scoreboard", style = MaterialTheme.typography.titleMedium)
        val max = scores.values.maxOrNull() ?: 0
        val leaders = scores.filterValues { it == max }.keys
        scores.forEach { (team, score) ->
            val isLeader = leaders.contains(team)
            val suffix = if (leaders.size > 1 && isLeader) " (tie)" else if (isLeader) " ←" else ""
            Text("$team: $score$suffix")
        }
    }
}
