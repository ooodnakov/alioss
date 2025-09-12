package com.example.alias

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.data.TurnHistoryRepository
import com.example.alias.domain.DefaultGameEngine
import com.example.alias.domain.GameEngine
import com.example.alias.domain.MatchConfig
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.data.settings.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckRepository: DeckRepository,
    private val wordDao: WordDao,
    private val settingsRepository: SettingsRepository,
    private val downloader: PackDownloader,
    private val historyRepository: TurnHistoryRepository,
) : ViewModel() {
    private val _engine = MutableStateFlow<GameEngine?>(null)
    val engine: StateFlow<GameEngine?> = _engine.asStateFlow()

    // Expose decks and enabled ids for Decks screen
    val decks = deckRepository.getDecks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val enabledDeckIds = settingsRepository.settings
        .map { it.enabledDeckIds }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    val trustedSources = settingsRepository.settings
        .map { it.trustedSources }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    

    // General UI events (e.g., snackbars)
    data class UiEvent(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val isError: Boolean = false,
        val onAction: (suspend () -> Unit)? = null,
    )
    private val _uiEvents = MutableSharedFlow<UiEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    init {
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) {
                // Import all bundled JSON decks from assets/decks/
                val assetFiles = context.assets.list("decks")?.filter { it.endsWith(".json") } ?: emptyList()
                for (f in assetFiles) {
                    runCatching {
                        val content = context.assets.open("decks/$f").bufferedReader().use { it.readText() }
                        deckRepository.importJson(content)
                    }
                }
                // Prepare enabled deck ids
                val s0 = settingsRepository.settings.first()
                val initialEnabled = if (s0.enabledDeckIds.isEmpty()) setOf("sample_en") else s0.enabledDeckIds
                if (s0.enabledDeckIds.isEmpty()) {
                    try { settingsRepository.setEnabledDeckIds(initialEnabled) } catch (_: Throwable) {}
                }
                // Fetch words for enabled decks in preferred language
                val language = s0.languagePreference
                val allowNSFW = s0.allowNSFW
                if (initialEnabled.isEmpty()) emptyList() else wordDao.getWordTextsForDecks(initialEnabled.toList(), language, allowNSFW)
            }
            val e = DefaultGameEngine(words, viewModelScope)
            _engine.value = e
            // Resolve initial settings
            val s = settingsRepository.settings.first()
            val config = MatchConfig(
                targetWords = s.targetWords,
                maxSkips = s.maxSkips,
                penaltyPerSkip = s.penaltyPerSkip,
                roundSeconds = s.roundSeconds
            )
            val seed = java.security.SecureRandom().nextLong()
            e.startMatch(config, teams = listOf("Red", "Blue"), seed = seed)
        }
    }

    fun setDeckEnabled(id: String, enabled: Boolean, fromUndo: Boolean = false) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first().enabledDeckIds.toMutableSet()
            if (enabled) current += id else current -= id
            settingsRepository.setEnabledDeckIds(current)
            if (!fromUndo) {
                val msg = if (enabled) "Enabled deck: $id" else "Disabled deck: $id"
                _uiEvents.tryEmit(
                    UiEvent(
                        message = msg,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                        onAction = { setDeckEnabled(id, !enabled, fromUndo = true) }
                    )
                )
            }
        }
    }

    fun addTrustedSource(originOrHost: String) {
        viewModelScope.launch {
            val cur = settingsRepository.settings.first().trustedSources.toMutableSet()
            cur += originOrHost.trim()
            settingsRepository.setTrustedSources(cur)
        }
    }

    fun removeTrustedSource(entry: String) {
        viewModelScope.launch {
            val cur = settingsRepository.settings.first().trustedSources.toMutableSet()
            cur -= entry
            settingsRepository.setTrustedSources(cur)
        }
    }

    fun downloadPackFromUrl(url: String, expectedSha256: String?) {
        viewModelScope.launch {
            _uiEvents.tryEmit(UiEvent(message = "Downloading…", duration = SnackbarDuration.Indefinite))
            try {
                val bytes = withContext(Dispatchers.IO) { downloader.download(url.trim(), expectedSha256?.trim().takeUnless { it.isNullOrEmpty() }) }
                // Try JSON first
                val text = bytes.toString(Charsets.UTF_8)
                withContext(Dispatchers.IO) { deckRepository.importJson(text) }
                _uiEvents.tryEmit(UiEvent(message = "Imported deck from URL", actionLabel = "OK", duration = SnackbarDuration.Short))
            } catch (t: Throwable) {
                _uiEvents.tryEmit(UiEvent(message = "Failed: ${t.message}", actionLabel = "Dismiss", duration = SnackbarDuration.Long, isError = true))
            }
        }
    }


    fun updateSettings(
        roundSeconds: Int,
        targetWords: Int,
        maxSkips: Int,
        penaltyPerSkip: Int,
        language: String,
        haptics: Boolean,
        oneHanded: Boolean,
        orientation: String,
    ) {
        viewModelScope.launch {
            settingsRepository.updateRoundSeconds(roundSeconds)
            settingsRepository.updateTargetWords(targetWords)
            settingsRepository.updateSkipPolicy(maxSkips, penaltyPerSkip)
            settingsRepository.updateHapticsEnabled(haptics)
            settingsRepository.updateOneHandedLayout(oneHanded)
            settingsRepository.updateOrientation(orientation)
            runCatching { settingsRepository.updateLanguagePreference(language) }
            _uiEvents.tryEmit(UiEvent(message = "Settings updated", actionLabel = "Dismiss"))
        }
    }

    fun restartMatch() {
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            val words = withContext(Dispatchers.IO) {
                val enabled = s.enabledDeckIds
                if (enabled.isEmpty()) emptyList() else wordDao.getWordTextsForDecks(enabled.toList(), s.languagePreference, s.allowNSFW)
            }
            val e = DefaultGameEngine(words, viewModelScope)
            _engine.value = e
            val config = MatchConfig(
                targetWords = s.targetWords,
                maxSkips = s.maxSkips,
                penaltyPerSkip = s.penaltyPerSkip,
                roundSeconds = s.roundSeconds
            )
            val seed = java.security.SecureRandom().nextLong()
            e.startMatch(config, teams = listOf("Red", "Blue"), seed = seed)
            _uiEvents.tryEmit(UiEvent(message = "Match restarted", actionLabel = "Dismiss"))
        }
    }

    fun nextTurn() {
        val e = _engine.value ?: return
        val current = e.state.value
        if (current is com.example.alias.domain.GameState.TurnFinished) {
            viewModelScope.launch {
                val entries = current.outcomes.map {
                    com.example.alias.data.db.TurnHistoryEntity(
                        team = current.team,
                        word = it.word,
                        correct = it.correct,
                        timestamp = it.timestamp,
                    )
                }
                withContext(Dispatchers.IO) { historyRepository.save(entries) }
                e.nextTurn()
            }
        }
    }

    fun overrideOutcome(index: Int, correct: Boolean) {
        _engine.value?.overrideOutcome(index, correct)
    }
}
