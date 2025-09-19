package com.example.alias.ui.decks

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val DIFFICULTY_LEVELS = listOf(1, 2, 3, 4, 5)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(vm: MainViewModel, onDeckSelected: (DeckEntity) -> Unit) {
    val decks by vm.decks.collectAsState()
    val enabled by vm.enabledDeckIds.collectAsState()
    val trusted by vm.trustedSources.collectAsState()
    val settings by vm.settings.collectAsState()
    val downloadProgress by vm.deckDownloadProgress.collectAsState()
    val availableCategories by vm.availableCategories.collectAsState()
    val availableWordClasses by vm.availableWordClasses.collectAsState()

    var url by rememberSaveable { mutableStateOf("") }
    var sha by rememberSaveable { mutableStateOf("") }
    var newTrusted by rememberSaveable { mutableStateOf("") }

    var difficultyRange by rememberSaveable(settings) {
        mutableStateOf(
            normalizeDifficultyRange(settings.minDifficulty, settings.maxDifficulty)
        )
    }
    var selectedCategories by rememberSaveable(settings) { mutableStateOf(settings.selectedCategories) }
    var selectedWordClasses by rememberSaveable(settings) { mutableStateOf(settings.selectedWordClasses) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importDeckFromFile(it) }
    }

    var activeSheet by rememberSaveable { mutableStateOf<DeckSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheet = activeSheet
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState
        ) {
            when (sheet) {
                DeckSheet.FILTERS -> DeckFiltersSheet(
                    state = DeckFiltersSheetState(
                        difficulty = DifficultyFilterState(
                            selectedLevels = difficultyRange.toDifficultyRange().toSet()
                        ),
                        categories = FilterSelectionState(
                            available = availableCategories,
                            selected = selectedCategories
                        ),
                        wordClasses = FilterSelectionState(
                            available = availableWordClasses,
                            selected = selectedWordClasses
                        )
                    ),
                    callbacks = DeckFiltersSheetCallbacks(
                        onDifficultyToggle = { level ->
                            difficultyRange = adjustDifficultyRange(difficultyRange, level)
                        },
                        onCategoriesChange = { selectedCategories = it },
                        onWordClassesChange = { selectedWordClasses = it },
                        onApply = {
                            val range = difficultyRange.toDifficultyRange()
                            vm.updateDifficultyFilter(range.first, range.last)
                            vm.updateCategoriesFilter(selectedCategories)
                            vm.updateWordClassesFilter(selectedWordClasses)
                            activeSheet = null
                        }
                    )
                )

                DeckSheet.IMPORT -> DeckImportSheet(
                    state = DeckImportSheetState(
                        url = url,
                        sha256 = sha
                    ),
                    callbacks = DeckImportSheetCallbacks(
                        onUrlChange = { url = it },
                        onShaChange = { sha = it },
                        onPickFile = { filePicker.launch(arrayOf("application/json")) },
                        onDownload = {
                            vm.downloadPackFromUrl(url, sha)
                            activeSheet = null
                        },
                        onOpenTrusted = { activeSheet = DeckSheet.TRUSTED }
                    )
                )

                DeckSheet.TRUSTED -> DeckTrustedSourcesSheet(
                    trustedSources = trusted.toList(),
                    newSource = newTrusted,
                    onNewSourceChange = { newTrusted = it },
                    onRemove = { vm.removeTrustedSource(it) },
                    onAdd = {
                        val trimmed = newTrusted.trim()
                        if (trimmed.isNotEmpty()) {
                            vm.addTrustedSource(trimmed)
                            newTrusted = ""
                        }
                    }
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DecksHeroSummary(
                    state = DecksHeroSummaryState(
                        decks = decks,
                        enabledDeckIds = enabled
                    ),
                    actions = DecksHeroSummaryActions(
                        onFiltersClick = { activeSheet = DeckSheet.FILTERS },
                        onEnableAll = { vm.setAllDecksEnabled(true) },
                        onDisableAll = { vm.setAllDecksEnabled(false) },
                        onManageSources = { activeSheet = DeckSheet.TRUSTED }
                    )
                )
            }
            downloadProgress?.let { progress ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DeckDownloadCard(progress)
                }
            }
            if (decks.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyDecksState(onImportClick = { activeSheet = DeckSheet.IMPORT })
                }
            } else {
                items(decks, key = { it.id }) { deck ->
                    DeckCard(
                        deck = deck,
                        enabled = enabled.contains(deck.id),
                        onToggle = { toggled -> vm.setDeckEnabled(deck.id, toggled) },
                        onClick = { onDeckSelected(deck) }
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { activeSheet = DeckSheet.IMPORT },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.import_decks_action))
        }
    }
}

private enum class DeckSheet { FILTERS, IMPORT, TRUSTED }

private data class DecksHeroSummaryState(
    val decks: List<DeckEntity>,
    val enabledDeckIds: Set<String>,
)

private data class DecksHeroSummaryActions(
    val onFiltersClick: () -> Unit,
    val onEnableAll: () -> Unit,
    val onDisableAll: () -> Unit,
    val onManageSources: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecksHeroSummary(state: DecksHeroSummaryState, actions: DecksHeroSummaryActions) {
    val activeCount = state.decks.count { state.enabledDeckIds.contains(it.id) }
    val languages = remember(state.decks) {
        state.decks.map { it.language.uppercase(Locale.getDefault()) }
            .toSet()
            .sorted()
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.title_decks),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.deck_active_summary, activeCount, state.decks.size),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            DeckLanguagesSummary(languages = languages)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = actions.onFiltersClick) {
                    Icon(Icons.Filled.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.open_filters))
                }
                OutlinedButton(onClick = actions.onManageSources) {
                    Icon(Icons.Filled.Verified, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.manage_trusted_sources))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = actions.onEnableAll) { Text(stringResource(R.string.enable_all)) }
                TextButton(onClick = actions.onDisableAll) { Text(stringResource(R.string.disable_all)) }
            }
            Text(
                text = stringResource(R.string.filters_hint),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckLanguagesSummary(languages: List<String>, modifier: Modifier = Modifier) {
    if (languages.isNotEmpty()) {
        Text(
            text = stringResource(R.string.deck_languages_summary, languages.joinToString(" • ")),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { language ->
                AssistChip(onClick = {}, enabled = false, label = { Text(language) })
            }
        }
    } else {
        Text(
            text = stringResource(R.string.deck_languages_none),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckCard(
    deck: DeckEntity,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            DeckCoverArt(deck = deck)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(deck.language.uppercase(Locale.getDefault())) })
                    if (deck.isOfficial) {
                        AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.deck_official_label)) })
                    }
                    if (deck.isNSFW) {
                        AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.deck_nsfw_label)) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (enabled) stringResource(R.string.deck_card_enabled) else stringResource(R.string.deck_card_disabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = enabled, onCheckedChange = onToggle)
                }
                Text(
                    text = stringResource(R.string.deck_card_view_details),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DeckCoverArt(deck: DeckEntity, modifier: Modifier = Modifier) {
    val gradient = rememberDeckCoverBrush(deck.id)
    val coverImage by produceState<ImageBitmap?>(initialValue = null, deck.coverImageBase64) {
        value = deck.coverImageBase64?.let { encoded ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    if (bytes.isEmpty()) {
                        null
                    } else {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    val initial = remember(deck.id, deck.name) {
        deck.name.firstOrNull()?.uppercaseChar()?.toString()
            ?: deck.language.uppercase(Locale.getDefault())
    }
    val image = coverImage
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(gradient)
    ) {
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Text(
            text = stringResource(R.string.deck_cover_language, deck.language.uppercase(Locale.getDefault())),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

private val DeckCoverPalette = listOf(
    Color(0xFF6C63FF),
    Color(0xFF00BFA5),
    Color(0xFFFF7043),
    Color(0xFF7E57C2),
    Color(0xFF26C6DA),
    Color(0xFFF06292),
)

@Composable
fun rememberDeckCoverBrush(deckId: String): Brush {
    val colors = remember(deckId) {
        val baseIndex = deckId.hashCode().absoluteValue % DeckCoverPalette.size
        val nextIndex = (baseIndex + 1) % DeckCoverPalette.size
        listOf(DeckCoverPalette[baseIndex], DeckCoverPalette[nextIndex])
    }
    return remember(colors) { Brush.linearGradient(colors) }
}

@Composable
private fun EmptyDecksState(onImportClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.no_decks_installed), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.no_decks_call_to_action), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onImportClick) { Text(stringResource(R.string.import_decks_action)) }
        }
    }
}

@Composable
private fun DeckDownloadCard(progress: MainViewModel.DeckDownloadProgress) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.deck_download_in_progress), style = MaterialTheme.typography.titleMedium)
            DeckDownloadProgressIndicator(progress)
        }
    }
}

@Composable
private fun DeckTrustedSourcesSheet(
    trustedSources: List<String>,
    newSource: String,
    onNewSourceChange: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.trusted_sources), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.trusted_sources_sheet_hint), style = MaterialTheme.typography.bodyMedium)
        if (trustedSources.isEmpty()) {
            Text(stringResource(R.string.no_trusted_sources_yet))
        } else {
            trustedSources.forEachIndexed { index, entry ->
                ListItem(
                    headlineContent = { Text(entry) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(entry) }) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                )
                if (index < trustedSources.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newSource,
                onValueChange = onNewSourceChange,
                label = { Text(stringResource(R.string.add_host_origin)) },
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onAdd, enabled = newSource.isNotBlank()) {
                Text(stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun DeckDownloadProgressIndicator(
    progress: MainViewModel.DeckDownloadProgress,
    modifier: Modifier = Modifier,
) {
    val totalBytes = progress.totalBytes?.takeIf { it > 0L }
    val fraction = totalBytes?.let { bytesTotal ->
        val clamped = progress.bytesRead.coerceAtMost(bytesTotal)
        (clamped.toFloat() / bytesTotal.toFloat()).coerceIn(0f, 1f)
    }
    val statusText = when (progress.step) {
        MainViewModel.DeckDownloadStep.DOWNLOADING -> fraction?.let {
            stringResource(R.string.deck_download_percent, (it * 100).roundToInt())
        } ?: stringResource(R.string.deck_download_downloading)

        MainViewModel.DeckDownloadStep.IMPORTING -> stringResource(R.string.deck_download_importing)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(statusText, style = MaterialTheme.typography.bodyMedium)
        val indicatorModifier = Modifier.fillMaxWidth()
        if (fraction != null && progress.step == MainViewModel.DeckDownloadStep.DOWNLOADING) {
            LinearProgressIndicator(progress = { fraction }, modifier = indicatorModifier)
        } else {
            LinearProgressIndicator(modifier = indicatorModifier)
        }
    }
}

@Composable
private fun DeckFiltersSheet(
    state: DeckFiltersSheetState,
    callbacks: DeckFiltersSheetCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.filters_label), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.deck_filters_description), style = MaterialTheme.typography.bodyMedium)
        DifficultyFilter(state.difficulty, callbacks.onDifficultyToggle)
        FilterChipGroup(
            title = stringResource(R.string.categories_label),
            items = state.categories.available,
            selectedItems = state.categories.selected,
            onSelectionChanged = callbacks.onCategoriesChange
        )
        FilterChipGroup(
            title = stringResource(R.string.word_classes_label),
            items = state.wordClasses.available,
            selectedItems = state.wordClasses.selected,
            onSelectionChanged = callbacks.onWordClassesChange
        )
        Text(stringResource(R.string.filters_hint), style = MaterialTheme.typography.bodySmall)
        Button(onClick = callbacks.onApply, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.apply_label))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DifficultyFilter(state: DifficultyFilterState, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.difficulty_filter_label), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DIFFICULTY_LEVELS.forEach { level ->
                val selected = state.selectedLevels.contains(level)
                FilterChip(
                    selected = selected,
                    onClick = { onToggle(level) },
                    label = { Text(level.toString()) }
                )
            }
        }
    }
}

private data class DeckFiltersSheetState(
    val difficulty: DifficultyFilterState,
    val categories: FilterSelectionState,
    val wordClasses: FilterSelectionState,
)

private data class DifficultyFilterState(val selectedLevels: Set<Int>)

private data class FilterSelectionState(
    val available: List<String>,
    val selected: Set<String>,
)

private class DeckFiltersSheetCallbacks(
    val onDifficultyToggle: (Int) -> Unit,
    val onCategoriesChange: (Set<String>) -> Unit,
    val onWordClassesChange: (Set<String>) -> Unit,
    val onApply: () -> Unit,
)

@Composable
private fun DeckImportSheet(
    state: DeckImportSheetState,
    callbacks: DeckImportSheetCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.import_sheet_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.import_sheet_hint), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = state.url,
            onValueChange = callbacks.onUrlChange,
            label = { Text(stringResource(R.string.https_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.sha256,
            onValueChange = callbacks.onShaChange,
            label = { Text(stringResource(R.string.expected_sha256_optional)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = callbacks.onPickFile, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_file))
            }
            Button(onClick = callbacks.onDownload, enabled = state.url.isNotBlank(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.download_and_import))
            }
        }
        TextButton(onClick = callbacks.onOpenTrusted, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Filled.Verified, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.manage_trusted_sources))
        }
    }
}

private data class DeckImportSheetState(
    val url: String,
    val sha256: String,
)

private class DeckImportSheetCallbacks(
    val onUrlChange: (String) -> Unit,
    val onShaChange: (String) -> Unit,
    val onPickFile: () -> Unit,
    val onDownload: () -> Unit,
    val onOpenTrusted: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipGroup(
    title: String,
    items: List<String>,
    selectedItems: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val selected = selectedItems.contains(item)
                FilterChip(
                    selected = selected,
                    onClick = {
                        val updatedSelection = if (selected) selectedItems - item else selectedItems + item
                        onSelectionChanged(updatedSelection)
                    },
                    label = { Text(item) }
                )
            }
        }
    }
}

private fun normalizeDifficultyRange(min: Int, max: Int): IntRange {
    val boundedMin = min.coerceAtLeast(DIFFICULTY_LEVELS.first())
    val boundedMax = max.coerceAtMost(DIFFICULTY_LEVELS.last())
    return IntRange(boundedMin, boundedMax)
}

private fun IntRange.toDifficultyRange(): IntRange {
    val start = first.coerceAtLeast(DIFFICULTY_LEVELS.first())
    val end = last.coerceAtMost(DIFFICULTY_LEVELS.last())
    return IntRange(start, end)
}

private fun adjustDifficultyRange(range: IntRange, level: Int): IntRange {
    return if (range.contains(level)) {
        when {
            range.first == range.last -> range
            level == range.first -> IntRange(range.first + 1, range.last)
            level == range.last -> IntRange(range.first, range.last - 1)
            else -> range
        }
    } else {
        when {
            level < range.first -> IntRange(level, range.last)
            else -> IntRange(range.first, level)
        }
    }
}
