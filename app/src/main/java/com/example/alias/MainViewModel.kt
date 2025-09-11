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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckRepository: DeckRepository,
    private val wordDao: WordDao,
) : ViewModel() {
    private val _engine = MutableStateFlow<GameEngine?>(null)
    val engine: StateFlow<GameEngine?> = _engine.asStateFlow()

    init {
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) {
                val content = context.assets.open("decks/sample_en.json").bufferedReader().use { it.readText() }
                deckRepository.importJson(content)
                wordDao.getWordTexts("sample_en")
            }
            val e = DefaultGameEngine(words, viewModelScope)
            _engine.value = e
            val config = MatchConfig(targetWords = 10, maxSkips = 3, penaltyPerSkip = 1, roundSeconds = 30)
            e.startMatch(config, teams = listOf("Red", "Blue"), seed = 0L)
        }
    }
}
