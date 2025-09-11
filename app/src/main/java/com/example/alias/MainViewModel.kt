package com.example.alias

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.domain.DefaultGameEngine
import com.example.alias.domain.GameEngine
import com.example.alias.domain.MatchConfig
import com.example.alias.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckRepository: DeckRepository,
    private val wordDao: WordDao,
    private val settingsRepository: SettingsRepository,
    private val downloader: PackDownloader,
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

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    init {
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) {
                val content = context.assets.open("decks/sample_en.json").bufferedReader().use { it.readText() }
                deckRepository.importJson(content)
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

    fun setDeckEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first().enabledDeckIds.toMutableSet()
            if (enabled) current += id else current -= id
            settingsRepository.setEnabledDeckIds(current)
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
            _downloadStatus.value = "Downloading…"
            try {
                val bytes = withContext(Dispatchers.IO) { downloader.download(url.trim(), expectedSha256?.trim().takeUnless { it.isNullOrEmpty() }) }
                // Try JSON first
                val text = bytes.toString(Charsets.UTF_8)
                withContext(Dispatchers.IO) { deckRepository.importJson(text) }
                _downloadStatus.value = "Imported deck from URL"
            } catch (t: Throwable) {
                _downloadStatus.value = "Failed: ${t.message}"
            }
        }
    }
}
