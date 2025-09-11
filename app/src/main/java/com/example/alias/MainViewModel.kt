package com.example.alias

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.WordDao
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
) : ViewModel() {
    private val _engine = MutableStateFlow<GameEngine?>(null)
    val engine: StateFlow<GameEngine?> = _engine.asStateFlow()

    // Expose decks and enabled ids for Decks screen
    val decks = deckRepository.getDecks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val enabledDeckIds = settingsRepository.settings
        .map { it.enabledDeckIds }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    init {
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) {
                val content = context.assets.open("decks/sample_en.json").bufferedReader().use { it.readText() }
                deckRepository.importJson(content)
                // Ensure the bundled sample deck is enabled by default
                // If user has no enabled decks yet, enable the sample.
                // Non-blocking best-effort; ignore errors.
                try {
                    settingsRepository.setEnabledDeckIds(setOf("sample_en"))
                } catch (_: Throwable) {}
                wordDao.getWordTexts("sample_en")
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
}
