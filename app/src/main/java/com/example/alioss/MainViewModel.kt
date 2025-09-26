package com.example.alioss

import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alioss.achievements.AchievementsManager
import com.example.alioss.data.achievements.AchievementSection
import com.example.alioss.data.achievements.AchievementState
import com.example.alioss.data.db.DeckEntity
import com.example.alioss.data.db.DifficultyBucket
import com.example.alioss.data.db.TurnHistoryEntity
import com.example.alioss.data.db.WordClassCount
import com.example.alioss.data.settings.Settings
import com.example.alioss.domain.GameEngine
import com.example.alioss.ui.decks.DeckDownloadProgress
import com.example.alioss.ui.decks.DeckDownloadStep
import com.example.alioss.ui.decks.DecksScreenViewModel
import com.example.alioss.ui.game.GameScreenViewModel
import com.example.alioss.ui.settings.SettingsScreenViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.Charsets

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val deckManager: DeckManager,
    private val settingsController: SettingsController,
    private val gameController: GameController,
    private val achievementsManager: AchievementsManager,
) : ViewModel(),
    GameScreenViewModel,
    DecksScreenViewModel,
    SettingsScreenViewModel {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _engine = MutableStateFlow<GameEngine?>(null)
    val engine: StateFlow<GameEngine?> = _engine.asStateFlow()

    private val _deckDownloadProgress = MutableStateFlow<DeckDownloadProgress?>(null)
    override val deckDownloadProgress: StateFlow<DeckDownloadProgress?> =
        _deckDownloadProgress.asStateFlow()

    override val decks = deckManager.observeDecks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    override val enabledDeckIds = settingsController.enabledDeckIds
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    override val trustedSources = settingsController.trustedSources
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    override val settings = settingsController.settings
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    val achievements: StateFlow<List<AchievementState>> = achievementsManager.achievements
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun recentHistory(limit: Int): Flow<List<TurnHistoryEntity>> = gameController.recentHistory(limit)

    data class UiEvent(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val isError: Boolean = false,
        val dismissCurrent: Boolean = true,
        val onAction: (suspend () -> Unit)? = null,
    )

    private data class WordMetadataKey(
        val deckIds: Set<String>,
        val allowNSFW: Boolean,
        val minDifficulty: Int,
        val maxDifficulty: Int,
        val categories: Set<String>,
        val categoryFilterEnabled: Int,
        val wordClasses: Set<String>,
        val wordClassFilterEnabled: Int,
        val languages: Set<String>,
        val languageFilterEnabled: Int,
    )

    private val _uiEvents = MutableSharedFlow<UiEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    private fun showCoverImageErrorSnackbar() {
        _uiEvents.tryEmit(
            UiEvent(
                message = "Deck cover image couldn't be processed",
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long,
                isError = true,
            ),
        )
    }

    private val _wordInfo = MutableStateFlow<Map<String, WordInfo>>(emptyMap())
    override val wordInfoByText: StateFlow<Map<String, WordInfo>> = _wordInfo.asStateFlow()
    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    override val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()
    private val _availableWordClasses = MutableStateFlow<List<String>>(emptyList())
    override val availableWordClasses: StateFlow<List<String>> = _availableWordClasses.asStateFlow()
    private val _showTutorialOnFirstTurn = MutableStateFlow(true)
    override val showTutorialOnFirstTurn: StateFlow<Boolean> = _showTutorialOnFirstTurn.asStateFlow()

    init {
        viewModelScope.launch {
            Log.i(TAG, "Starting MainViewModel init block")
            val initial = deckManager.prepareInitialLoad()
            val engineInstance = gameController.createEngine(initial.words, viewModelScope)
            _engine.value = engineInstance
            gameController.startMatch(engineInstance, initial.settings)
            Log.i(TAG, "MainViewModel init complete")
        }

        viewModelScope.launch {
            settingsController.settings
                .map { deckManager.buildWordClassAvailabilityKey(it) }
                .distinctUntilChanged()
                .collectLatest { key ->
                    val classes = deckManager.loadAvailableWordClasses(key)
                    _availableWordClasses.value = classes
                }
        }

        viewModelScope.launch {
            settingsController.settings
                .map { deckManager.buildWordQueryFilters(it) }
                .distinctUntilChanged { previous, current ->
                    previous.toMetadataKey() == current.toMetadataKey()
                }
                .collectLatest { filters ->
                    val metadata = deckManager.loadWordMetadata(filters)
                    _wordInfo.value = metadata.infoByWord
                    _availableCategories.value = metadata.categories
                }
        }
    }

    private fun DeckManager.WordQueryFilters.toMetadataKey(): WordMetadataKey {
        return WordMetadataKey(
            deckIds = deckIds.toSet(),
            allowNSFW = allowNSFW,
            minDifficulty = minDifficulty,
            maxDifficulty = maxDifficulty,
            categories = categories?.toSet() ?: emptySet(),
            categoryFilterEnabled = categoryFilterEnabled,
            wordClasses = wordClasses?.toSet() ?: emptySet(),
            wordClassFilterEnabled = wordClassFilterEnabled,
            languages = languages.toSet(),
            languageFilterEnabled = languageFilterEnabled,
        )
    }

    override fun setDeckEnabled(id: String, enabled: Boolean) {
        setDeckEnabledInternal(id, enabled, fromUndo = false)
    }

    private fun setDeckEnabledInternal(id: String, enabled: Boolean, fromUndo: Boolean) {
        viewModelScope.launch {
            deckManager.setDeckEnabled(id, enabled)
            if (!fromUndo) {
                val msg = if (enabled) "Enabled deck: $id" else "Disabled deck: $id"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = msg,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                        onAction = { setDeckEnabledInternal(id, !enabled, fromUndo = true) },
                    ),
                )
            }
        }
    }

    override fun setAllDecksEnabled(enableAll: Boolean) {
        viewModelScope.launch {
            deckManager.setAllDecksEnabled(enableAll)
            val msg = if (enableAll) "Enabled all decks" else "Disabled all decks"
            _uiEvents.tryEmit(UiEvent(message = msg, actionLabel = "OK"))
        }
    }

    override fun deleteDeck(deck: DeckEntity) {
        viewModelScope.launch {
            when (val result = deckManager.deleteDeck(deck)) {
                is DeckManager.DeleteDeckResult.Success -> {
                    _uiEvents.tryEmit(
                        UiEvent(
                            message = result.message,
                            actionLabel = "OK",
                            dismissCurrent = true,
                        ),
                    )
                }
                is DeckManager.DeleteDeckResult.Failure -> {
                    _uiEvents.tryEmit(
                        UiEvent(
                            message = "Failed to delete deck: ${result.errorMessage}",
                            actionLabel = "Dismiss",
                            duration = SnackbarDuration.Long,
                            isError = true,
                            dismissCurrent = true,
                        ),
                    )
                }
            }
        }
    }

    override fun permanentlyDeleteImportedDeck(deck: DeckEntity) {
        viewModelScope.launch {
            val result = deckManager.permanentlyDeleteImportedDeck(deck)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed to permanently delete deck: $error",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
                return@launch
            }
            _uiEvents.tryEmit(
                UiEvent(
                    message = "Permanently deleted deck: ${deck.name}",
                    actionLabel = "OK",
                    dismissCurrent = true,
                ),
            )
        }
    }

    override fun restoreDeletedBundledDeck(deckId: String) {
        viewModelScope.launch {
            val result = deckManager.restoreDeletedBundledDeck(deckId)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed to restore deck: $error",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
                return@launch
            }
            _uiEvents.tryEmit(
                UiEvent(
                    message = "Restored deck",
                    actionLabel = "OK",
                    dismissCurrent = true,
                ),
            )
        }
    }

    override fun restoreDeletedImportedDeck(deckId: String) {
        viewModelScope.launch {
            val result = deckManager.restoreDeletedImportedDeck(deckId)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed to restore deck: $error",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
                return@launch
            }
            _uiEvents.tryEmit(
                UiEvent(
                    message = "Restored deck",
                    actionLabel = "OK",
                    dismissCurrent = true,
                ),
            )
        }
    }

    override fun permanentlyDeleteImportedDeck(deckId: String) {
        viewModelScope.launch {
            val result = deckManager.permanentlyDeleteImportedDeck(deckId)
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed to permanently delete deck: $error",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
                return@launch
            }
            _uiEvents.tryEmit(
                UiEvent(
                    message = "Permanently deleted deck",
                    actionLabel = "OK",
                    dismissCurrent = true,
                ),
            )
        }
    }

    override fun addTrustedSource(originOrHost: String) {
        viewModelScope.launch {
            when (settingsController.addTrustedSource(originOrHost)) {
                SettingsController.TrustedSourceResult.Invalid -> {
                    _uiEvents.tryEmit(
                        UiEvent(
                            message = "Invalid host/origin",
                            actionLabel = "Dismiss",
                            duration = SnackbarDuration.Short,
                            isError = true,
                        ),
                    )
                }
                is SettingsController.TrustedSourceResult.Added,
                is SettingsController.TrustedSourceResult.Unchanged,
                -> Unit
            }
        }
    }

    override fun updateDifficultyFilter(min: Int, max: Int) {
        viewModelScope.launch { settingsController.updateDifficultyFilter(min, max) }
    }

    override fun updateCategoriesFilter(categories: Set<String>) {
        viewModelScope.launch { settingsController.updateCategoriesFilter(categories) }
    }

    override fun updateDeckLanguagesFilter(languages: Set<String>) {
        viewModelScope.launch { settingsController.updateDeckLanguagesFilter(languages) }
    }

    override fun updateWordClassesFilter(classes: Set<String>) {
        viewModelScope.launch { settingsController.updateWordClassesFilter(classes) }
    }

    override fun removeTrustedSource(entry: String) {
        viewModelScope.launch { settingsController.removeTrustedSource(entry) }
    }

    override suspend fun getWordCount(deckId: String): Int = deckManager.getWordCount(deckId)

    override suspend fun getDeckCategories(deckId: String): List<String> =
        deckManager.getDeckCategories(deckId)

    override suspend fun getDeckWordSamples(deckId: String, limit: Int): List<String> =
        deckManager.getDeckWordSamples(deckId, limit)

    override suspend fun getDeckDifficultyHistogram(deckId: String): List<DifficultyBucket> =
        deckManager.getDeckDifficultyHistogram(deckId)

    override suspend fun getDeckRecentWords(deckId: String, limit: Int): List<String> =
        deckManager.getDeckRecentWords(deckId, limit)

    override suspend fun getDeckWordClassCounts(deckId: String): List<WordClassCount> =
        deckManager.getDeckWordClassCounts(deckId)

    override fun updateSeenTutorial(value: Boolean) {
        viewModelScope.launch {
            settingsController.updateSeenTutorial(value)
            _showTutorialOnFirstTurn.value = false
        }
    }

    override fun dismissTutorialOnFirstTurn() {
        _showTutorialOnFirstTurn.value = false
    }

    override fun downloadPackFromUrl(url: String, expectedSha256: String?) {
        viewModelScope.launch {
            _deckDownloadProgress.value = DeckDownloadProgress(step = DeckDownloadStep.DOWNLOADING)
            _uiEvents.tryEmit(
                UiEvent(
                    message = "Downloading…",
                    duration = SnackbarDuration.Short,
                    dismissCurrent = true,
                ),
            )
            try {
                var lastUpdate = 0L
                val bytes = deckManager.downloadPack(url, expectedSha256) { bytesRead, totalBytes ->
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 100 || (totalBytes != null && bytesRead == totalBytes)) {
                        _deckDownloadProgress.value = DeckDownloadProgress(
                            step = DeckDownloadStep.DOWNLOADING,
                            bytesRead = bytesRead,
                            totalBytes = totalBytes,
                        )
                        lastUpdate = now
                    }
                }
                _deckDownloadProgress.value = DeckDownloadProgress(step = DeckDownloadStep.IMPORTING)
                val text = bytes.toString(Charsets.UTF_8)
                val result = deckManager.importPackFromJson(text)
                runCatching {
                    val s = settingsController.settings.first()
                    val canEnable = !result.isNsfw || s.allowNSFW
                    if (canEnable) {
                        val ids = s.enabledDeckIds.toMutableSet()
                        if (ids.add(result.deckId)) {
                            settingsController.setEnabledDeckIds(ids)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Failed to auto-enable deck after URL import", it)
                }
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Imported deck from URL",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short,
                        dismissCurrent = true,
                    ),
                )
                if (result.coverImageError != null) {
                    showCoverImageErrorSnackbar()
                }
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to download or import pack", e)
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed: ${e.message}",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to download or import pack", e)
                val message = e.message?.takeUnless { it.isBlank() } ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to download or import pack", e)
                val message = e.message?.takeUnless { it.isBlank() } ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred", e)
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "An unexpected error occurred",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true,
                    ),
                )
            } finally {
                _deckDownloadProgress.value = null
            }
        }
    }

    override fun importDeckFromFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val result = deckManager.importDeckFromUri(uri)
                runCatching {
                    val s = settingsController.settings.first()
                    val canEnable = !result.isNsfw || s.allowNSFW
                    if (canEnable) {
                        val ids = s.enabledDeckIds.toMutableSet()
                        if (ids.add(result.deckId)) {
                            settingsController.setEnabledDeckIds(ids)
                        }
                    }
                }
                _uiEvents.tryEmit(
                    UiEvent(message = "Imported deck", actionLabel = "OK", duration = SnackbarDuration.Short),
                )
                if (result.coverImageError != null) {
                    showCoverImageErrorSnackbar()
                }
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to import deck from file", e)
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed: ${e.message}",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                    ),
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to import deck from file", e)
                val message = e.message?.takeUnless { it.isBlank() } ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                    ),
                )
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to import deck from file", e)
                val message = e.message?.takeUnless { it.isBlank() } ?: "Unknown error"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred", e)
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "An unexpected error occurred",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                    ),
                )
            }
        }
    }

    override suspend fun updateSettings(request: SettingsUpdateRequest) {
        settingsController.applySettingsUpdate(request)
        _uiEvents.tryEmit(UiEvent(message = "Settings updated", actionLabel = "Dismiss"))
    }

    override fun resetLocalData(onDone: (() -> Unit)?) {
        viewModelScope.launch {
            deckManager.resetLocalData()
            _uiEvents.tryEmit(UiEvent(message = "Local data cleared", actionLabel = "OK"))
            onDone?.invoke()
        }
    }

    fun resetHistory() {
        viewModelScope.launch {
            try {
                gameController.clearHistory()
                _uiEvents.tryEmit(UiEvent(message = "History cleared", actionLabel = "OK"))
            } catch (e: android.database.SQLException) {
                Log.e(TAG, "Failed to clear history", e)
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed to clear history: ${e.message ?: "Unknown error"}",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred", e)
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "An unexpected error occurred",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                    ),
                )
            }
        }
    }

    override fun restartMatch() {
        viewModelScope.launch {
            val currentSettings = settingsController.settings.first()
            val filters = deckManager.buildWordQueryFilters(currentSettings)
            val words = deckManager.loadWords(filters)
            val metadata = deckManager.loadWordMetadata(filters)
            _wordInfo.value = metadata.infoByWord
            _availableCategories.value = metadata.categories
            _availableWordClasses.value = metadata.wordClasses
            val engineInstance = gameController.createEngine(words, viewModelScope)
            _engine.value = engineInstance
            gameController.startMatch(engineInstance, currentSettings)
            _uiEvents.tryEmit(UiEvent(message = "Match restarted", actionLabel = "Dismiss"))
        }
    }

    override fun nextTurn() {
        val e = _engine.value ?: return
        viewModelScope.launch { gameController.completeTurn(e, _wordInfo.value) }
    }

    override fun startTurn() {
        val e = _engine.value ?: return
        viewModelScope.launch { gameController.startTurn(e) }
    }

    override fun overrideOutcome(index: Int, correct: Boolean) {
        val e = _engine.value ?: return
        viewModelScope.launch { gameController.overrideOutcome(e, index, correct) }
    }

    fun setOrientation(value: String) {
        viewModelScope.launch { settingsController.setOrientation(value) }
    }

    fun onSectionVisited(section: AchievementSection) {
        viewModelScope.launch { achievementsManager.onSectionVisited(section) }
    }
}
