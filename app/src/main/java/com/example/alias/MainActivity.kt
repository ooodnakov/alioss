package com.example.alias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
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
import androidx.compose.material3.SnackbarHostState
import com.example.alias.MainViewModel.UiEvent
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.launch
import com.example.alias.ui.WordCard
import com.example.alias.ui.WordCardAction
import com.example.alias.data.settings.SettingsRepository
private const val MIN_TEAMS = SettingsRepository.MIN_TEAMS
private const val MAX_TEAMS = SettingsRepository.MAX_TEAMS



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = hiltViewModel()
                val snack = remember { SnackbarHostState() }

                // Collect general UI events and show snackbars
                LaunchedEffect(Unit) {
                    vm.uiEvents.collect { ev: UiEvent ->
                        val result = snack.showSnackbar(
                            message = ev.message,
                            actionLabel = ev.actionLabel,
                            withDismissAction = ev.actionLabel == null,
                            duration = ev.duration
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            ev.onAction?.invoke()
                        }
                    }
                }
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        AppScaffold(title = "Alias", snackbarHostState = snack) {
                            HomeScreen(
                                onQuickPlay = { vm.restartMatch(); nav.navigate("game") },
                                onDecks = { nav.navigate("decks") },
                                onSettings = { nav.navigate("settings") }
                            )
                        }
                    }
                    composable("game") {
                        val engine by vm.engine.collectAsState()
                        val settings by vm.settings.collectAsState()
                        AppScaffold(title = "Game", onBack = { nav.popBackStack() }, snackbarHostState = snack) {
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
                        AppScaffold(title = "Decks", onBack = { nav.popBackStack() }, snackbarHostState = snack) {
                            DecksScreen(vm = vm)
                        }
                    }
                    composable("settings") {
                        AppScaffold(title = "Settings", onBack = { nav.popBackStack() }, snackbarHostState = snack) {
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
            var isProcessing by remember { mutableStateOf(false) }
            var committing by remember { mutableStateOf(false) }
            var frozenNext by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(s.word) {
                // Word advanced: re-enable actions and unfreeze preview
                isProcessing = false
                committing = false
                frozenNext = null
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(progress = { progress }, color = barColor, modifier = Modifier.fillMaxWidth())
                Text("${s.timeRemaining}s", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text("Team: ${s.team}", style = MaterialTheme.typography.titleMedium)
                        val computedNext = engine.peekNextWord()
                        val nextWord = frozenNext ?: computedNext
                        Box(Modifier.fillMaxWidth().height(200.dp)) {
                            // Pre-render the next card underneath to avoid flicker when advancing
                            if (nextWord != null) {
                                WordCard(
                                    word = nextWord,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .zIndex(0f)
                                        .alpha(if (committing) 1f else 0f),
                                    enabled = false,
                                    vibrator = null,
                                    hapticsEnabled = false,
                                    onActionStart = {},
                                    onAction = {},
                                    animateAppear = false,
                                    allowSkip = s.skipsRemaining > 0,
                                )
                            }
                            WordCard(
                                word = s.word,
                                modifier = Modifier.matchParentSize().zIndex(1f),
                                enabled = true,
                                vibrator = vibrator,
                                hapticsEnabled = settings.hapticsEnabled,
                                onActionStart = {
                                    // Freeze the preview so it doesn't swap to next-next mid-animation
                                    if (!committing) {
                                        frozenNext = computedNext
                                        committing = true
                                    }
                                    isProcessing = true
                                },
                                onAction = {
                                    when (it) {
                                        WordCardAction.Correct -> {
                                            engine.correct()
                                            isProcessing = false
                                        }
                                        WordCardAction.Skip -> if (s.skipsRemaining > 0) {
                                            engine.skip()
                                            isProcessing = false
                                        } else {
                                            // No skips left: ignore gesture and keep card
                                            isProcessing = false
                                        }
                                    }
                                },
                                allowSkip = s.skipsRemaining > 0,
                                animateAppear = false,
                            )
                        }
                Text("Remaining: ${s.remaining} • Score: ${s.score} • Skips: ${s.skipsRemaining}")
                if (settings.oneHandedLayout) {
                    val onCorrect = {
                        if (!isProcessing) {
                            isProcessing = true
                            engine.correct()
                        }
                    }
                    val onSkip = {
                        if (!isProcessing) {
                            if (s.skipsRemaining > 0) {
                                isProcessing = true
                                engine.skip()
                            } else {
                                // No skips left; ignore without blocking input
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onCorrect,
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) { Text("Correct") }
                        Button(
                            onClick = onSkip,
                            enabled = !isProcessing && s.skipsRemaining > 0,
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) { Text("Skip") }
                    }
                } else {
                    val onCorrect = {
                        if (!isProcessing) {
                            isProcessing = true
                            engine.correct()
                        }
                    }
                    val onSkip = {
                        if (!isProcessing) {
                            if (s.skipsRemaining > 0) {
                                isProcessing = true
                                engine.skip()
                            } else {
                                // No skips left; ignore without blocking input
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onCorrect,
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f).height(60.dp)
                        ) { Text("Correct") }
                        Button(
                            onClick = onSkip,
                            enabled = !isProcessing && s.skipsRemaining > 0,
                            modifier = Modifier.weight(1f).height(60.dp)
                        ) { Text("Skip") }
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
    // Status snackbars are handled globally via vm.uiEvents
    var url by rememberSaveable { mutableStateOf("") }
    var sha by rememberSaveable { mutableStateOf("") }
    var newTrusted by rememberSaveable { mutableStateOf("") }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importDeckFromFile(it) }
    }
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
            decks.forEachIndexed { index, deck ->
                val isEnabled = enabled.contains(deck.id)
                ListItem(
                    headlineContent = { Text(deck.name) },
                    supportingContent = {
                        val info = buildString {
                            append(deck.language)
                            if (deck.isNSFW) append(" • NSFW")
                        }
                        Text(info, style = MaterialTheme.typography.bodySmall)
                    },
                    trailingContent = {
                        Switch(checked = isEnabled, onCheckedChange = { vm.setDeckEnabled(deck.id, it) })
                    }
                )
                if (index < decks.lastIndex) HorizontalDivider()
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { filePicker.launch(arrayOf("application/json")) }) { Text("Import from file") }

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
        // No inline status; global snackbar will show download progress/results

        Spacer(Modifier.height(24.dp))
        Text("Trusted Sources", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        trusted.forEachIndexed { i, entry ->
            ListItem(
                headlineContent = { Text(entry) },
                trailingContent = {
                    IconButton(onClick = { vm.removeTrustedSource(entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
            )
            if (i < trusted.size - 1) HorizontalDivider()
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
    var punishSkips by rememberSaveable(s) { mutableStateOf(s.punishSkips) }
    var nsfw by rememberSaveable(s) { mutableStateOf(s.allowNSFW) }
    var haptics by rememberSaveable(s) { mutableStateOf(s.hapticsEnabled) }
    var oneHand by rememberSaveable(s) { mutableStateOf(s.oneHandedLayout) }
    var orientation by rememberSaveable(s) { mutableStateOf(s.orientation) }
    val scope = rememberCoroutineScope()
    var teams by rememberSaveable(s) { mutableStateOf(s.teams) }


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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Punish skips", modifier = Modifier.weight(1f))
            Switch(checked = punishSkips, onCheckedChange = { punishSkips = it })
        }
        OutlinedTextField(value = lang, onValueChange = { lang = it }, label = { Text("Language (e.g., en, ru)") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Allow NSFW", modifier = Modifier.weight(1f))
            Switch(checked = nsfw, onCheckedChange = { nsfw = it })
        }
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

        Button(onClick = {
            scope.launch {
                vm.updateSettings(
                    roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                    targetWords = target.toIntOrNull() ?: s.targetWords,
                    maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                    penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                    punishSkips = punishSkips,
                    language = lang.ifBlank { s.languagePreference },
                    allowNSFW = nsfw,
                    haptics = haptics,
                    oneHanded = oneHand,
                    orientation = orientation,
                    teams = teams,
                )
                vm.restartMatch()
                onBack()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        Text("Teams", style = MaterialTheme.typography.titleMedium)
        teams.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { new ->
                        teams = teams.toMutableList().also { it[index] = new }
                    },
                    label = { Text("Team ${index + 1}") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { teams = teams.toMutableList().also { it.removeAt(index) } },
                    enabled = teams.size > MIN_TEAMS
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove team")
                }
            }
        }
        if (teams.size < MAX_TEAMS) {
            OutlinedButton(onClick = { teams = teams + "Team ${teams.size + 1}" }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add Team", modifier = Modifier.padding(start = 4.dp))
            }
        }
        val canSave = teams.count { it.isNotBlank() } >= MIN_TEAMS
        val applySettings = {
            scope.launch {
                vm.updateSettings(
                    roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                    targetWords = target.toIntOrNull() ?: s.targetWords,
                    maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                    penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                    punishSkips = punishSkips,
                    language = lang.ifBlank { s.languagePreference },
                    allowNSFW = nsfw,
                    haptics = haptics,
                    oneHanded = oneHand,
                    orientation = orientation,
                    teams = teams,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { applySettings() }, enabled = canSave, modifier = Modifier.weight(1f)) { Text("Save") }
            FilledTonalButton(onClick = {
                applySettings()
                vm.restartMatch()
                onBack()
            }, enabled = canSave, modifier = Modifier.weight(1f)) { Text("Save & Restart") }
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
                ListItem(
                    leadingContent = {
                        Icon(
                            if (o.correct) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (o.correct) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    },
                    headlineContent = { Text(o.word) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { vm.overrideOutcome(index, true) }) {
                                Icon(Icons.Filled.Check, contentDescription = "Mark correct")
                            }
                            IconButton(onClick = { vm.overrideOutcome(index, false) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Mark incorrect")
                            }
                        }
                    }
                )
                if (index < s.outcomes.lastIndex) HorizontalDivider()
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
