package com.example.alias.ui.decks

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.db.DeckEntity
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val DIFFICULTY_LEVELS = listOf(1, 2, 3, 4, 5)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun decksScreen(vm: MainViewModel, onDeckSelected: (DeckEntity) -> Unit) {
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

    var minDifficulty by rememberSaveable(settings) { mutableStateOf(settings.minDifficulty) }
    var maxDifficulty by rememberSaveable(settings) { mutableStateOf(settings.maxDifficulty) }

    var selectedCategories by rememberSaveable(settings) { mutableStateOf(settings.selectedCategories) }
    var selectedWordClasses by rememberSaveable(settings) { mutableStateOf(settings.selectedWordClasses) }
    var selectedLanguages by rememberSaveable(settings) { mutableStateOf(settings.selectedDeckLanguages) }

    val normalizedLanguageFilter = remember(selectedLanguages) {
        selectedLanguages.map { it.lowercase(Locale.ROOT) }.toSet()
    }
    val filteredDecks = remember(decks, normalizedLanguageFilter) {
        if (normalizedLanguageFilter.isEmpty()) {
            decks
        } else {
            decks.filter { deck ->
                normalizedLanguageFilter.contains(deck.language.lowercase(Locale.ROOT))
            }
        }
    }
    val availableLanguages = remember(decks) {
        decks.map { it.language.lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .toSet()
            .sorted()
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importDeckFromFile(it) }
    }

    var activeSheet by rememberSaveable { mutableStateOf<DeckSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var deckPendingDelete by remember { mutableStateOf<DeckEntity?>(null) }
    var deckPendingPermanentDelete by remember { mutableStateOf<DeckEntity?>(null) }

    val sheet = activeSheet
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
        ) {
            when (sheet) {
                DeckSheet.FILTERS -> deckFiltersSheet(
                    state = DeckFiltersSheetState(
                        difficulty = DifficultyFilterState(
                            selectedLevels = IntRange(minDifficulty, maxDifficulty).toSet(),
                        ),
                        languages = FilterSelectionState(
                            available = availableLanguages,
                            selected = selectedLanguages,
                        ),
                        categories = FilterSelectionState(
                            available = availableCategories,
                            selected = selectedCategories,
                        ),
                        wordClasses = FilterSelectionState(
                            available = availableWordClasses,
                            selected = selectedWordClasses,
                        ),
                    ),
                    callbacks = DeckFiltersSheetCallbacks(
                        onDifficultyToggle = { level ->
                            val currentRange = IntRange(minDifficulty, maxDifficulty)
                            val newRange = adjustDifficultyRange(currentRange, level)
                            minDifficulty = newRange.first
                            maxDifficulty = newRange.last
                        },
                        onLanguagesChange = { selectedLanguages = it },
                        onCategoriesChange = { selectedCategories = it },
                        onWordClassesChange = { selectedWordClasses = it },
                        onApply = {
                            vm.updateDifficultyFilter(minDifficulty, maxDifficulty)
                            vm.updateDeckLanguagesFilter(selectedLanguages)
                            vm.updateCategoriesFilter(selectedCategories)
                            vm.updateWordClassesFilter(selectedWordClasses)
                            activeSheet = null
                        },
                    ),
                )

                DeckSheet.IMPORT -> deckImportSheet(
                    state = DeckImportSheetState(
                        url = url,
                        sha256 = sha,
                    ),
                    callbacks = DeckImportSheetCallbacks(
                        onUrlChange = { url = it },
                        onShaChange = { sha = it },
                        onPickFile = { filePicker.launch(arrayOf("application/json")) },
                        onDownload = {
                            vm.downloadPackFromUrl(url, sha)
                            activeSheet = null
                        },
                        onOpenTrusted = { activeSheet = DeckSheet.TRUSTED },
                    ),
                )

                DeckSheet.TRUSTED -> deckTrustedSourcesSheet(
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
                    },
                )

                DeckSheet.DELETED_DECKS -> deckDeletedDecksSheet(
                    deletedBundledDeckIds = settings.deletedBundledDeckIds,
                    onRestoreDeck = { deckId -> vm.restoreDeletedBundledDeck(deckId) },
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                decksHeroSummary(
                    state = DecksHeroSummaryState(
                        decks = filteredDecks,
                        enabledDeckIds = enabled,
                    ),
                    actions = DecksHeroSummaryActions(
                        onFiltersClick = { activeSheet = DeckSheet.FILTERS },
                        onEnableAll = { vm.setAllDecksEnabled(true) },
                        onDisableAll = { vm.setAllDecksEnabled(false) },
                        onManageSources = { activeSheet = DeckSheet.TRUSTED },
                        onManageDeleted = { activeSheet = DeckSheet.DELETED_DECKS },
                    ),
                )
            }
            downloadProgress?.let { progress ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    deckDownloadCard(progress)
                }
            }
            if (decks.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    emptyDecksState(onImportClick = { activeSheet = DeckSheet.IMPORT })
                }
            } else if (filteredDecks.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    filteredDecksEmptyState(onAdjustFilters = { activeSheet = DeckSheet.FILTERS })
                }
            } else {
                items(filteredDecks, key = { it.id }) { deck ->
                    deckCard(
                        deck = deck,
                        enabled = enabled.contains(deck.id),
                        onToggle = { toggled -> vm.setDeckEnabled(deck.id, toggled) },
                        onClick = { onDeckSelected(deck) },
                        onDelete = { deckPendingDelete = deck },
                        onPermanentDelete = if (!deck.isOfficial) ({ deckPendingPermanentDelete = deck }) else null,
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { activeSheet = DeckSheet.IMPORT },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.import_decks_action))
        }

        val deckToDelete = deckPendingDelete
        if (deckToDelete != null) {
            AlertDialog(
                onDismissRequest = { deckPendingDelete = null },
                title = { Text(stringResource(R.string.deck_delete_dialog_title)) },
                text = {
                    Text(stringResource(R.string.deck_delete_dialog_message, deckToDelete.name))
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.deleteDeck(deckToDelete)
                        deckPendingDelete = null
                    }) {
                        Text(stringResource(R.string.deck_delete_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deckPendingDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        val deckToPermanentlyDelete = deckPendingPermanentDelete
        if (deckToPermanentlyDelete != null) {
            AlertDialog(
                onDismissRequest = { deckPendingPermanentDelete = null },
                title = { Text(stringResource(R.string.deck_permanent_delete_dialog_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.deck_permanent_delete_dialog_message,
                            deckToPermanentlyDelete.name,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.permanentlyDeleteImportedDeck(deckToPermanentlyDelete)
                        deckPendingPermanentDelete = null
                    }) {
                        Text(stringResource(R.string.deck_delete_permanently_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deckPendingPermanentDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

private enum class DeckSheet { FILTERS, IMPORT, TRUSTED, DELETED_DECKS }

private data class DecksHeroSummaryState(
    val decks: List<DeckEntity>,
    val enabledDeckIds: Set<String>,
)

private data class DecksHeroSummaryActions(
    val onFiltersClick: () -> Unit,
    val onEnableAll: () -> Unit,
    val onDisableAll: () -> Unit,
    val onManageSources: () -> Unit,
    val onManageDeleted: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun decksHeroSummary(state: DecksHeroSummaryState, actions: DecksHeroSummaryActions) {
    val activeCount = state.decks.count { state.enabledDeckIds.contains(it.id) }
    val languages = remember(state.decks, state.enabledDeckIds) {
        state.decks
            .filter { state.enabledDeckIds.contains(it.id) }
            .map { it.language.uppercase(Locale.getDefault()) }
            .toSet()
            .sorted()
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.title_decks),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.deck_active_summary, activeCount, state.decks.size),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                deckLanguagesSummary(languages = languages)
                FilledTonalButton(onClick = actions.onFiltersClick) {
                    Icon(Icons.Filled.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.open_filters))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = actions.onManageSources) {
                    Icon(Icons.Filled.Verified, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.manage_trusted_sources))
                }
                OutlinedButton(onClick = actions.onManageDeleted) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.deleted_decks))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = actions.onEnableAll) { Text(stringResource(R.string.enable_all)) }
                TextButton(onClick = actions.onDisableAll) { Text(stringResource(R.string.disable_all)) }
            }
            Text(
                text = stringResource(R.string.filters_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun deckLanguagesSummary(languages: List<String>, modifier: Modifier = Modifier) {
    if (languages.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { language ->
                AssistChip(onClick = {}, enabled = false, label = { Text(language) })
            }
        }
    } else {
        Text(
            text = stringResource(R.string.deck_languages_none),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun deckCard(
    deck: DeckEntity,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onPermanentDelete: (() -> Unit)? = null,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            deckCoverArt(deck = deck)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = deck.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (onDelete != null) {
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = stringResource(R.string.deck_more_actions),
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    if (deck.isOfficial) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.deck_hide_action)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onDelete()
                                            },
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.deck_delete_permanently_action))
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onPermanentDelete?.invoke()
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Switch(checked = enabled, onCheckedChange = onToggle)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(deck.language.uppercase(Locale.getDefault())) },
                    )
                    if (deck.isOfficial) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(R.string.deck_official_label)) },
                        )
                    }
                    if (deck.isNSFW) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(R.string.deck_nsfw_label)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun deckCoverArt(deck: DeckEntity, modifier: Modifier = Modifier) {
    val gradient = rememberDeckCoverBrush(deck.id)
    val coverImage = rememberDeckCoverImage(deck.coverImageBase64)
    val initial = remember(deck.id, deck.name) {
        deck.name.firstOrNull()?.uppercaseChar()?.toString()
            ?: deck.language.uppercase(Locale.getDefault())
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 1f)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(gradient),
    ) {
        coverImage?.let { image ->
            androidx.compose.foundation.Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
            )
        } ?: Text(
            text = initial,
            style = MaterialTheme.typography.displayLarge,
            color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            text = stringResource(R.string.deck_cover_language, deck.language.uppercase(Locale.getDefault())),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
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
private fun emptyDecksState(onImportClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.no_decks_installed), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.no_decks_call_to_action), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onImportClick) { Text(stringResource(R.string.import_decks_action)) }
        }
    }
}

@Composable
private fun filteredDecksEmptyState(onAdjustFilters: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.deck_filters_no_results), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.filters_hint), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAdjustFilters) { Text(stringResource(R.string.open_filters)) }
        }
    }
}

@Composable
private fun deckDownloadCard(progress: MainViewModel.DeckDownloadProgress) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.deck_download_in_progress), style = MaterialTheme.typography.titleMedium)
            deckDownloadProgressIndicator(progress)
        }
    }
}

@Composable
private fun deckTrustedSourcesSheet(
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    },
                )
                if (index < trustedSources.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newSource,
                onValueChange = onNewSourceChange,
                label = { Text(stringResource(R.string.add_host_origin)) },
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = onAdd, enabled = newSource.isNotBlank()) {
                Text(stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun deckDownloadProgressIndicator(
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
private fun deckFiltersSheet(
    state: DeckFiltersSheetState,
    callbacks: DeckFiltersSheetCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.filters_label), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.deck_filters_description), style = MaterialTheme.typography.bodyMedium)
        difficultyFilter(state.difficulty, callbacks.onDifficultyToggle)
        filterChipGroup(
            title = stringResource(R.string.languages_label),
            items = state.languages.available,
            selectedItems = state.languages.selected,
            onSelectionChanged = callbacks.onLanguagesChange,
            labelMapper = { it.uppercase(Locale.getDefault()) },
        )
        filterChipGroup(
            title = stringResource(R.string.categories_label),
            items = state.categories.available,
            selectedItems = state.categories.selected,
            onSelectionChanged = callbacks.onCategoriesChange,
        )
        filterChipGroup(
            title = stringResource(R.string.word_classes_label),
            items = state.wordClasses.available,
            selectedItems = state.wordClasses.selected,
            onSelectionChanged = callbacks.onWordClassesChange,
        )
        Text(stringResource(R.string.filters_hint), style = MaterialTheme.typography.bodySmall)
        Button(onClick = callbacks.onApply, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.apply_label))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun difficultyFilter(state: DifficultyFilterState, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.difficulty_filter_label), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DIFFICULTY_LEVELS.forEach { level ->
                val selected = state.selectedLevels.contains(level)
                FilterChip(
                    selected = selected,
                    onClick = { onToggle(level) },
                    label = { Text(level.toString()) },
                )
            }
        }
    }
}

private data class DeckFiltersSheetState(
    val difficulty: DifficultyFilterState,
    val languages: FilterSelectionState,
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
    val onLanguagesChange: (Set<String>) -> Unit,
    val onCategoriesChange: (Set<String>) -> Unit,
    val onWordClassesChange: (Set<String>) -> Unit,
    val onApply: () -> Unit,
)

@Composable
private fun deckImportSheet(
    state: DeckImportSheetState,
    callbacks: DeckImportSheetCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.import_sheet_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.import_sheet_hint), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = state.url,
            onValueChange = callbacks.onUrlChange,
            label = { Text(stringResource(R.string.https_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.sha256,
            onValueChange = callbacks.onShaChange,
            label = { Text(stringResource(R.string.expected_sha256_optional)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
private fun filterChipGroup(
    title: String,
    items: List<String>,
    selectedItems: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    labelMapper: (String) -> String = { it },
) {
    if (items.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                val selected = selectedItems.contains(item)
                FilterChip(
                    selected = selected,
                    onClick = {
                        val updatedSelection = if (selected) selectedItems - item else selectedItems + item
                        onSelectionChanged(updatedSelection)
                    },
                    label = { Text(labelMapper(item)) },
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

@Composable
private fun deckDeletedDecksSheet(
    deletedBundledDeckIds: Set<String>,
    onRestoreDeck: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.deleted_decks),
            style = MaterialTheme.typography.titleLarge,
        )

        if (deletedBundledDeckIds.isEmpty()) {
            Text(
                text = stringResource(R.string.deck_deleted_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.deck_deleted_bundled_hint),
                style = MaterialTheme.typography.titleMedium,
            )

            deletedBundledDeckIds.forEach { deckId ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.deck_deleted_bundled_label, deckId))
                        },
                        trailingContent = {
                            TextButton(onClick = { onRestoreDeck(deckId) }) {
                                Text(stringResource(R.string.restore))
                            }
                        },
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.deck_deleted_imported_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
