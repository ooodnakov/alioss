package com.example.alias.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

private const val MIN_TEAMS = SettingsRepository.MIN_TEAMS
private const val MAX_TEAMS = SettingsRepository.MAX_TEAMS

@Composable
fun settingsScreen(vm: MainViewModel, onBack: () -> Unit, onAbout: () -> Unit) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var round by rememberSaveable(s) { mutableStateOf(s.roundSeconds.toString()) }
    var target by rememberSaveable(s) { mutableStateOf(s.targetWords.toString()) }
    var maxSkips by rememberSaveable(s) { mutableStateOf(s.maxSkips.toString()) }
    var penalty by rememberSaveable(s) { mutableStateOf(s.penaltyPerSkip.toString()) }
    var lang by rememberSaveable(s) { mutableStateOf(s.languagePreference) }
    var uiLang by rememberSaveable(s) { mutableStateOf(s.uiLanguage) }
    var punishSkips by rememberSaveable(s) { mutableStateOf(s.punishSkips) }
    var nsfw by rememberSaveable(s) { mutableStateOf(s.allowNSFW) }
    var haptics by rememberSaveable(s) { mutableStateOf(s.hapticsEnabled) }
    var sound by rememberSaveable(s) { mutableStateOf(s.soundEnabled) }
    var oneHand by rememberSaveable(s) { mutableStateOf(s.oneHandedLayout) }
    var verticalSwipes by rememberSaveable(s) { mutableStateOf(s.verticalSwipes) }
    var orientation by rememberSaveable(s) { mutableStateOf(s.orientation) }
    var teams by rememberSaveable(s, saver = TeamEditorEntryStateSaver) {
        mutableStateOf(s.teams.mapIndexed { index, name -> TeamEditorEntry(index.toLong(), name) })
    }
    var nextTeamId by rememberSaveable(s) { mutableStateOf(s.teams.size.toLong()) }
    val pagerState = rememberPagerState { SettingsTab.values().size }
    var selectedTab by rememberSaveable(pagerState) { mutableStateOf(SettingsTab.MATCH_RULES) }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = SettingsTab.values()[pagerState.currentPage]
    }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val teamSuggestions = stringArrayResource(R.array.team_name_suggestions).toList()

    val canSave = teams.count { it.name.isNotBlank() } >= MIN_TEAMS
    val applySettings: () -> Job = {
        scope.launch {
            vm.updateSettings(
                MainViewModel.SettingsUpdateRequest(
                    roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                    targetWords = target.toIntOrNull() ?: s.targetWords,
                    maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                    penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                    punishSkips = punishSkips,
                    language = lang.ifBlank { s.languagePreference },
                    uiLanguage = uiLang,
                    allowNSFW = nsfw,
                    haptics = haptics,
                    sound = sound,
                    oneHanded = oneHand,
                    verticalSwipes = verticalSwipes,
                    orientation = orientation,
                    teams = teams.map { it.name },
                ),
            )
        }
    }

    if (showResetDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showResetDialog = false }) {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.reset_confirm_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.reset_confirm_message))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showResetDialog = false }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(onClick = {
                            showResetDialog = false
                            vm.resetLocalData()
                        }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = pagerState.currentPage) {
            SettingsTab.values().forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(stringResource(tab.titleRes)) },
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (SettingsTab.values()[page]) {
                    SettingsTab.MATCH_RULES -> matchRulesTab(
                        round = round,
                        onRoundChange = { round = it },
                        target = target,
                        onTargetChange = { target = it },
                        maxSkips = maxSkips,
                        onMaxSkipsChange = { maxSkips = it },
                        penalty = penalty,
                        onPenaltyChange = { penalty = it },
                        punishSkips = punishSkips,
                        onPunishSkipsChange = { punishSkips = it },
                    )

                    SettingsTab.INPUT_FEEDBACK -> inputFeedbackTab(
                        haptics = haptics,
                        onHapticsChange = { haptics = it },
                        sound = sound,
                        onSoundChange = { sound = it },
                        oneHand = oneHand,
                        onOneHandChange = { oneHand = it },
                        verticalSwipes = verticalSwipes,
                        onVerticalSwipesChange = { verticalSwipes = it },
                        orientation = orientation,
                        onOrientationChange = { orientation = it },
                    )

                    SettingsTab.TEAMS -> teamsTab(
                        teams = teams,
                        canRemoveTeam = teams.size > MIN_TEAMS,
                        canAddTeam = teams.size < MAX_TEAMS,
                        onTeamNameChange = { index, value ->
                            teams = teams.toMutableList().also { list ->
                                list[index] = list[index].copy(name = value)
                            }
                        },
                        onTeamRemove = { index ->
                            teams = teams.toMutableList().also { it.removeAt(index) }
                        },
                        onTeamAdd = {
                            val defaultName = ctx.getString(R.string.team_default_name, teams.size + 1)
                            teams = teams + TeamEditorEntry(nextTeamId, defaultName)
                            nextTeamId += 1
                        },
                        onTeamMove = { from, to ->
                            if (from == to) return@teamsTab
                            if (from !in teams.indices || to !in 0..teams.size) return@teamsTab
                            val updated = teams.toMutableList()
                            val item = updated.removeAt(from)
                            val targetIndex = to.coerceIn(0, updated.size)
                            updated.add(targetIndex, item)
                            teams = updated
                        },
                        suggestions = teamSuggestions,
                        onApplySuggestion = { suggestion ->
                            if (teams.any { it.name.equals(suggestion, ignoreCase = true) }) return@teamsTab
                            val targetIndex = teams.indexOfFirst { it.name.isBlank() }
                            teams = teams.toMutableList().also { list ->
                                when {
                                    targetIndex >= 0 -> list[targetIndex] = list[targetIndex].copy(name = suggestion)
                                    list.size < MAX_TEAMS -> {
                                        list += TeamEditorEntry(nextTeamId, suggestion)
                                        nextTeamId += 1
                                    }
                                }
                            }
                        },
                    )

                    SettingsTab.ADVANCED -> advancedTab(
                        uiLanguage = uiLang,
                        onUiLanguageChange = { uiLang = it },
                        language = lang,
                        onLanguageChange = { lang = it },
                        allowNsfw = nsfw,
                        onAllowNsfwChange = { nsfw = it },
                        onShowTutorialAgain = { vm.updateSeenTutorial(false) },
                        onAbout = onAbout,
                        onReset = { showResetDialog = true },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { applySettings() }, enabled = canSave, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save_label))
            }
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        applySettings().join()
                        vm.restartMatch()
                        onBack()
                    }
                },
                enabled = canSave,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.save_and_restart_label))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}

private enum class SettingsTab(@StringRes val titleRes: Int) {
    MATCH_RULES(R.string.match_rules_tab),
    INPUT_FEEDBACK(R.string.input_feedback_tab),
    TEAMS(R.string.teams_tab),
    ADVANCED(R.string.advanced_tab),
}

private data class TeamEditorEntry(val id: Long, val name: String)

private val TeamEditorEntryStateSaver = listSaver<MutableState<List<TeamEditorEntry>>, Any?>(
    save = { state -> state.value.flatMap { entry -> listOf(entry.id, entry.name) } },
    restore = { restored ->
        val entries = restored.chunked(2).mapNotNull { chunk ->
            val id = (chunk.getOrNull(0) as? Number)?.toLong() ?: return@mapNotNull null
            val name = chunk.getOrNull(1) as? String ?: ""
            TeamEditorEntry(id, name)
        }
        mutableStateOf(entries)
    },
)

@Composable
private fun matchRulesTab(
    round: String,
    onRoundChange: (String) -> Unit,
    target: String,
    onTargetChange: (String) -> Unit,
    maxSkips: String,
    onMaxSkipsChange: (String) -> Unit,
    penalty: String,
    onPenaltyChange: (String) -> Unit,
    punishSkips: Boolean,
    onPunishSkipsChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.round_and_goals), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = round,
                        onValueChange = onRoundChange,
                        label = { Text(stringResource(R.string.round_seconds_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = onTargetChange,
                        label = { Text(stringResource(R.string.target_words_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.skips_section), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = maxSkips,
                            onValueChange = onMaxSkipsChange,
                            label = { Text(stringResource(R.string.max_skips_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = penalty,
                            onValueChange = onPenaltyChange,
                            label = { Text(stringResource(R.string.penalty_per_skip_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.punish_skips_label), modifier = Modifier.weight(1f))
                        Switch(checked = punishSkips, onCheckedChange = onPunishSkipsChange)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun inputFeedbackTab(
    haptics: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    sound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    oneHand: Boolean,
    onOneHandChange: (Boolean) -> Unit,
    verticalSwipes: Boolean,
    onVerticalSwipesChange: (Boolean) -> Unit,
    orientation: String,
    onOrientationChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.feedback_and_layout), style = MaterialTheme.typography.titleMedium)
                    settingsToggleRow(
                        label = stringResource(R.string.haptics_label),
                        checked = haptics,
                        onCheckedChange = onHapticsChange,
                    )
                    settingsToggleRow(
                        label = stringResource(R.string.sound_effects_label),
                        checked = sound,
                        onCheckedChange = onSoundChange,
                    )
                    settingsToggleRow(
                        label = stringResource(R.string.one_hand_layout_label),
                        checked = oneHand,
                        onCheckedChange = onOneHandChange,
                    )
                    settingsToggleRow(
                        label = stringResource(R.string.vertical_swipes_label),
                        checked = verticalSwipes,
                        onCheckedChange = onVerticalSwipesChange,
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.orientation_label), style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        orientationChip(
                            selected = orientation == "system",
                            label = stringResource(R.string.auto_label),
                            icon = Icons.Filled.ScreenRotation,
                            onClick = { onOrientationChange("system") },
                        )
                        orientationChip(
                            selected = orientation == "portrait",
                            label = stringResource(R.string.portrait_label),
                            icon = Icons.Filled.ScreenLockPortrait,
                            onClick = { onOrientationChange("portrait") },
                        )
                        orientationChip(
                            selected = orientation == "landscape",
                            label = stringResource(R.string.landscape_label),
                            icon = Icons.Filled.ScreenLockLandscape,
                            onClick = { onOrientationChange("landscape") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun settingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun orientationChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun teamsTab(
    teams: List<TeamEditorEntry>,
    canRemoveTeam: Boolean,
    canAddTeam: Boolean,
    onTeamNameChange: (Int, String) -> Unit,
    onTeamRemove: (Int) -> Unit,
    onTeamAdd: () -> Unit,
    onTeamMove: (Int, Int) -> Unit,
    suggestions: List<String>,
    onApplySuggestion: (String) -> Unit,
) {
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.teams_label), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.drag_to_reorder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(teams, key = { _, team -> team.id }) { index, team ->
            val isDragging = draggingIndex == index
            teamEditorCard(
                index = index,
                name = team.name,
                canRemove = canRemoveTeam,
                onNameChange = onTeamNameChange,
                onRemove = onTeamRemove,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> itemHeights[index] = coords.size.height }
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                handleModifier = Modifier.pointerInput(teams) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingIndex = index
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val current = draggingIndex ?: return@detectDragGesturesAfterLongPress
                            dragOffset += dragAmount.y
                            if (dragOffset > 0 && current < teams.lastIndex) {
                                val height = itemHeights[current + 1] ?: return@detectDragGesturesAfterLongPress
                                if (dragOffset > height * 0.6f) {
                                    onTeamMove(current, current + 1)
                                    draggingIndex = current + 1
                                    dragOffset = 0f
                                }
                            } else if (dragOffset < 0 && current > 0) {
                                val height = itemHeights[current - 1] ?: return@detectDragGesturesAfterLongPress
                                if (dragOffset < -height * 0.6f) {
                                    onTeamMove(current, current - 1)
                                    draggingIndex = current - 1
                                    dragOffset = 0f
                                }
                            }
                        },
                        onDragEnd = {
                            draggingIndex = null
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffset = 0f
                        },
                    )
                },
                isDragging = isDragging,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onTeamAdd, enabled = canAddTeam, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_team_label))
                }
                OutlinedButton(
                    onClick = { suggestions.randomOrNull()?.let(onApplySuggestion) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.team_suggestions_label))
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.team_suggestions_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.team_suggestions_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(onClick = { onApplySuggestion(suggestion) }, label = { Text(suggestion) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun teamEditorCard(
    index: Int,
    name: String,
    canRemove: Boolean,
    onNameChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
    isDragging: Boolean,
) {
    val elevation = if (isDragging) 8.dp else 2.dp
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: (index + 1).toString()
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(initial, style = MaterialTheme.typography.titleMedium)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(index, it) },
                    label = { Text(stringResource(R.string.team_default_name, index + 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                IconButton(onClick = { onRemove(index) }, enabled = canRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_team))
                }
                Box(
                    modifier = handleModifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.team_drag_handle_description),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun advancedTab(
    uiLanguage: String,
    onUiLanguageChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    allowNsfw: Boolean,
    onAllowNsfwChange: (Boolean) -> Unit,
    onShowTutorialAgain: () -> Unit,
    onAbout: () -> Unit,
    onReset: () -> Unit,
) {
    val selectedLanguage = remember(uiLanguage) { resolveUiLanguageSelection(uiLanguage) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.language_and_content), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.ui_language_label))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedLanguage == "system",
                            onClick = { onUiLanguageChange("system") },
                            label = { Text(stringResource(R.string.system_default_label)) },
                        )
                        FilterChip(
                            selected = selectedLanguage == "en",
                            onClick = { onUiLanguageChange("en") },
                            label = { Text(stringResource(R.string.english_label)) },
                        )
                        FilterChip(
                            selected = selectedLanguage == "ru",
                            onClick = { onUiLanguageChange("ru") },
                            label = { Text(stringResource(R.string.russian_label)) },
                        )
                    }
                    OutlinedTextField(
                        value = language,
                        onValueChange = onLanguageChange,
                        label = { Text(stringResource(R.string.language_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    settingsToggleRow(
                        label = stringResource(R.string.allow_nsfw_label),
                        checked = allowNsfw,
                        onCheckedChange = onAllowNsfwChange,
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.support_and_data_label), style = MaterialTheme.typography.titleMedium)
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.History, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.show_tutorial_again)) },
                        modifier = Modifier.clickable { onShowTutorialAgain() },
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.title_about)) },
                        modifier = Modifier.clickable(onClick = onAbout),
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.reset_local_data)) },
                        modifier = Modifier.clickable { onReset() },
                    )
                }
            }
        }
    }
}

private fun resolveUiLanguageSelection(raw: String): String {
    if (raw.equals("system", ignoreCase = true) || raw.isBlank()) {
        return "system"
    }
    val firstTag = raw.split(',').firstOrNull()?.trim().orEmpty()
    if (firstTag.isEmpty()) {
        return raw
    }
    val locale = Locale.forLanguageTag(firstTag)
    val language = locale.language
    return if (language.isNullOrEmpty()) raw else language
}
