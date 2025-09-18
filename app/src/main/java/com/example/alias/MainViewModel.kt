package com.example.alias

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.WordClassCount
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.data.TurnHistoryRepository
import com.example.alias.data.db.TurnHistoryEntity
import com.example.alias.data.pack.PackParser
import com.example.alias.data.pack.ParsedPack
import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.domain.DefaultGameEngine
import com.example.alias.domain.GameEngine
import com.example.alias.domain.MatchConfig
import com.example.alias.domain.word.WordClassCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import org.json.JSONObject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckRepository: DeckRepository,
    private val wordDao: WordDao,
    private val deckDao: com.example.alias.data.db.DeckDao,
    private val turnHistoryDao: com.example.alias.data.db.TurnHistoryDao,
    private val settingsRepository: SettingsRepository,
    private val downloader: PackDownloader,
    private val historyRepository: TurnHistoryRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _engine = MutableStateFlow<GameEngine?>(null)
    val engine: StateFlow<GameEngine?> = _engine.asStateFlow()

    enum class DeckDownloadStep { DOWNLOADING, IMPORTING }

    data class DeckDownloadProgress(
        val step: DeckDownloadStep,
        val bytesRead: Long = 0L,
        val totalBytes: Long? = null,
    )

    private val _deckDownloadProgress = MutableStateFlow<DeckDownloadProgress?>(null)
    val deckDownloadProgress: StateFlow<DeckDownloadProgress?> = _deckDownloadProgress.asStateFlow()

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

    fun recentHistory(limit: Int): Flow<List<TurnHistoryEntity>> =
        historyRepository.getRecent(limit)

    

    // General UI events (e.g., snackbars)
    data class UiEvent(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val isError: Boolean = false,
        val dismissCurrent: Boolean = false,
        val onAction: (suspend () -> Unit)? = null,
    )
    private val _uiEvents = MutableSharedFlow<UiEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    private fun showCoverImageErrorSnackbar() {
        _uiEvents.tryEmit(
            UiEvent(
                message = "Deck cover image couldn't be processed",
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long,
                isError = true,
            )
        )
    }

    private data class WordQueryFilters(
        val deckIds: List<String>,
        val language: String,
        val allowNSFW: Boolean,
        val minDifficulty: Int,
        val maxDifficulty: Int,
        val categories: List<String>,
        val categoryFilterEnabled: Int,
        val wordClasses: List<String>,
        val wordClassFilterEnabled: Int,
    )

    private data class InitialLoadResult(
        val words: List<String>,
        val settings: Settings,
    )

    private data class WordClassAvailabilityKey(
        val deckIds: Set<String>,
        val language: String,
        val allowNSFW: Boolean,
    )

    private data class SanitizedPack(
        val pack: ParsedPack,
        val coverImageError: Throwable?,
    )

    private fun canonicalizeWordClassFilters(raw: Collection<String>): List<String> {
        val normalized = raw
            .asSequence()
            .mapNotNull { value ->
                val trimmed = value.trim()
                if (trimmed.isEmpty()) {
                    null
                } else {
                    trimmed.uppercase(Locale.ROOT)
                }
            }
            .toList()
        if (normalized.isEmpty()) return emptyList()
        val orderedKnown = WordClassCatalog.order(normalized)
        val knownSet = orderedKnown.toSet()
        val extras = normalized
            .asSequence()
            .filterNot { knownSet.contains(it) }
            .distinct()
            .sorted()
            .toList()
        return orderedKnown + extras
    }

    private suspend fun sanitizeCoverImage(pack: ParsedPack): SanitizedPack {
        val coverError = withContext(Dispatchers.IO) {
            pack.deck.coverImageBase64?.let { encoded ->
                try {
                    val decoded = Base64.decode(encoded, Base64.DEFAULT)
                    require(decoded.isNotEmpty()) { "Cover image is empty" }
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size, opts)
                    require(opts.outWidth > 0 && opts.outHeight > 0) { "Cover image has invalid dimensions" }
                    null
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to decode deck cover image for ${pack.deck.id}", t)
                    t
                }
            }
        }
        val sanitized = if (coverError != null) {
            pack.copy(deck = pack.deck.copy(coverImageBase64 = null))
        } else {
            pack
        }
        return SanitizedPack(sanitized, coverError)
    }

    private suspend fun parseAndSanitizePack(content: String): SanitizedPack {
        return try {
            val parsed = PackParser.fromJson(content)
            sanitizeCoverImage(parsed)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (!error.isCoverImageError()) throw error
            val sanitizedJson = removeCoverImageField(content) ?: throw error
            val parsed = PackParser.fromJson(sanitizedJson)
            val sanitized = sanitizeCoverImage(parsed)
            sanitized.copy(coverImageError = sanitized.coverImageError ?: error)
        }
    }

    private fun removeCoverImageField(content: String): String? {
        return try {
            val root = JSONObject(content)
            val deck = root.optJSONObject("deck") ?: return null
            if (!deck.has("coverImage")) {
                null
            } else {
                deck.remove("coverImage")
                root.toString()
            }
        } catch (error: Exception) {
            null
        }
    }

    private fun Throwable.isCoverImageError(): Boolean {
        if (this is IllegalArgumentException && message?.contains("cover image", ignoreCase = true) == true) {
            return true
        }
        val cause = cause
        return cause != null && cause !== this && cause.isCoverImageError()
    }

    private fun Settings.toWordQueryFilters(deckIdsOverride: Set<String>? = null): WordQueryFilters {
        val deckIds = (deckIdsOverride ?: enabledDeckIds).toList()
        val categories = selectedCategories.toList()
        val classes = canonicalizeWordClassFilters(selectedWordClasses)
        return WordQueryFilters(
            deckIds = deckIds,
            language = languagePreference,
            allowNSFW = allowNSFW,
            minDifficulty = minDifficulty,
            maxDifficulty = maxDifficulty,
            categories = categories,
            categoryFilterEnabled = if (categories.isEmpty()) 0 else 1,
            wordClasses = classes,
            wordClassFilterEnabled = if (classes.isEmpty()) 0 else 1,
        )
    }

    private suspend fun loadWords(filters: WordQueryFilters): List<String> {
        if (filters.deckIds.isEmpty()) return emptyList()
        val words = wordDao.getWordTextsForDecks(
            filters.deckIds,
            filters.language,
            filters.allowNSFW,
            filters.minDifficulty,
            filters.maxDifficulty,
            filters.categories,
            filters.categoryFilterEnabled,
            filters.wordClasses,
            filters.wordClassFilterEnabled,
        )
        return words
    }

    init {
        viewModelScope.launch {
            val initial = withContext(Dispatchers.IO) {
                // Import bundled JSON decks from assets/decks/ when changed (by checksum) or on first run
                val assetFiles = context.assets.list("decks")?.filter { it.endsWith(".json") } ?: emptyList()
                val prev = settingsRepository.readBundledDeckHashes()
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                fun sha256(bytes: ByteArray): String = digest.digest(bytes).joinToString("") { b -> "%02x".format(b) }
                val currentEntries = mutableSetOf<String>()
                val toImport = mutableListOf<String>()
                for (f in assetFiles) {
                    val bytes = runCatching { context.assets.open("decks/$f").use { it.readBytes() } }.getOrNull()
                    if (bytes != null) {
                        val h = sha256(bytes)
                        val entry = "$f:$h"
                        currentEntries += entry
                        if (prev.none { it.startsWith("$f:") } || !prev.contains(entry)) {
                            toImport += f
                        }
                    }
                }
                if (prev.isEmpty() && currentEntries.isEmpty()) {
                    // No bundled content found; nothing to import
                } else if (prev.isEmpty() && currentEntries.isNotEmpty()) {
                    // First run: import all
                    toImport.clear()
                    toImport.addAll(assetFiles)
                }
                val hadDecks = deckRepository.getDecks().first().isNotEmpty()
                if (!hadDecks && assetFiles.isNotEmpty()) {
                    toImport.clear()
                    toImport.addAll(assetFiles)
                }
                for (f in toImport) {
                    try {
                        val content = context.assets.open("decks/$f").bufferedReader().use { it.readText() }
                        val (sanitizedPack, coverError) = parseAndSanitizePack(content)
                        deckRepository.importPack(sanitizedPack)
                        if (coverError != null) {
                            Log.w(TAG, "Bundled deck $f cover art discarded due to decode failure", coverError)
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to import bundled deck $f", t)
                    }
                }
                // Persist current checksums if any
                runCatching { settingsRepository.writeBundledDeckHashes(currentEntries) }
                // Resolve enabled deck ids; if none set, pick available decks matching language preference
                val baseSettings = settingsRepository.settings.first()
                val allDecks = deckRepository.getDecks().first()
                val preferredIds = allDecks.filter { it.language == baseSettings.languagePreference }.map { it.id }.toSet()
                val fallbackIds = allDecks.map { it.id }.toSet()
                val resolvedEnabled = if (baseSettings.enabledDeckIds.isEmpty()) {
                    if (preferredIds.isNotEmpty()) preferredIds else fallbackIds
                } else baseSettings.enabledDeckIds
                if (baseSettings.enabledDeckIds.isEmpty()) {
                    try {
                        settingsRepository.setEnabledDeckIds(resolvedEnabled)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to persist enabled deck ids", t)
                    }
                }
                // Fetch words for enabled decks in preferred language
                val filters = baseSettings.toWordQueryFilters(resolvedEnabled)
                val words = loadWords(filters)
                InitialLoadResult(
                    words = words,
                    settings = baseSettings.copy(enabledDeckIds = resolvedEnabled)
                )
            }
            // Also prepare word metadata and supporting filters for the same snapshot
            withContext(Dispatchers.IO) {
                val filters = initial.settings.toWordQueryFilters()
                if (filters.deckIds.isEmpty()) {
                    _wordInfo.value = emptyMap()
                    _availableCategories.value = emptyList()
                    _availableWordClasses.value = emptyList()
                } else {
                    coroutineScope {
                        val briefsDeferred = async {
                            wordDao.getWordBriefsForDecks(
                                filters.deckIds,
                                filters.language,
                                filters.allowNSFW,
                                filters.minDifficulty,
                                filters.maxDifficulty,
                                filters.categories,
                                filters.categoryFilterEnabled,
                                filters.wordClasses,
                                filters.wordClassFilterEnabled
                            )
                        }
                        val categoriesDeferred = async {
                            wordDao.getAvailableCategories(filters.deckIds, filters.language, filters.allowNSFW)
                        }
                        val classesDeferred = async {
                            wordDao.getAvailableWordClasses(filters.deckIds, filters.language, filters.allowNSFW)
                        }
                        val briefs = briefsDeferred.await()
                        val categories = categoriesDeferred.await().sorted()
                        val classes = classesDeferred.await()
                        val map = briefs.associateBy({ it.text }) {
                            WordInfo(it.difficulty, it.category, parseClass(it.wordClass))
                        }
                        _wordInfo.value = map
                        _availableCategories.value = categories
                        _availableWordClasses.value = canonicalizeWordClassFilters(classes)
                    }
                }
            }
            val e = DefaultGameEngine(initial.words, viewModelScope)
            _engine.value = e
            val config = MatchConfig(
                targetWords = initial.settings.targetWords,
                maxSkips = initial.settings.maxSkips,
                penaltyPerSkip = if (initial.settings.punishSkips) initial.settings.penaltyPerSkip else 0,
                roundSeconds = initial.settings.roundSeconds
            )
            val seed = java.security.SecureRandom().nextLong()
            e.startMatch(config, teams = initial.settings.teams, seed = seed)
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { settings ->
                    WordClassAvailabilityKey(
                        deckIds = settings.enabledDeckIds,
                        language = settings.languagePreference,
                        allowNSFW = settings.allowNSFW,
                    )
                }
                .distinctUntilChanged()
                .collectLatest { key ->
                    val ids = key.deckIds.toList()
                    val classes = if (ids.isEmpty()) {
                        emptyList()
                    } else {
                        withContext(Dispatchers.IO) {
                            wordDao.getAvailableWordClasses(ids, key.language, key.allowNSFW)
                        }
                    }
                    _availableWordClasses.value = canonicalizeWordClassFilters(classes)
                }
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

    fun setAllDecksEnabled(enableAll: Boolean) {
        viewModelScope.launch {
            val all = deckRepository.getDecks().first().map { it.id }.toSet()
            val target = if (enableAll) all else emptySet()
            settingsRepository.setEnabledDeckIds(target)
            val msg = if (enableAll) "Enabled all decks" else "Disabled all decks"
            _uiEvents.tryEmit(UiEvent(message = msg, actionLabel = "OK"))
        }
    }

    fun addTrustedSource(originOrHost: String) {
        viewModelScope.launch {
            val input = originOrHost.trim().lowercase()
            val normalized = when {
                input.startsWith("https://") -> input.removeSuffix("/")
                "://" in input -> null // reject non-https schemes
                else -> input // bare host
            }
            if (normalized.isNullOrBlank()) {
                _uiEvents.tryEmit(UiEvent(message = "Invalid host/origin", actionLabel = "Dismiss", duration = SnackbarDuration.Short, isError = true))
                return@launch
            }
            val cur = settingsRepository.settings.first().trustedSources.toMutableSet()
            cur += normalized
            settingsRepository.setTrustedSources(cur)
        }
    }

    fun updateDifficultyFilter(min: Int, max: Int) {
        viewModelScope.launch {
            settingsRepository.updateDifficultyFilter(min, max)
        }
    }

    fun updateCategoriesFilter(categories: Set<String>) {
        viewModelScope.launch {
            settingsRepository.setCategoriesFilter(categories)
        }
    }

    fun updateWordClassesFilter(classes: Set<String>) {
        viewModelScope.launch {
            settingsRepository.setWordClassesFilter(classes)
        }
    }

    fun removeTrustedSource(entry: String) {
        viewModelScope.launch {
            val cur = settingsRepository.settings.first().trustedSources.toMutableSet()
            cur -= entry
            settingsRepository.setTrustedSources(cur)
        }
    }

    suspend fun getWordCount(deckId: String): Int = deckRepository.getWordCount(deckId)

    suspend fun getDeckCategories(deckId: String): List<String> = withContext(Dispatchers.IO) {
        wordDao.getDeckCategories(deckId)
    }

    suspend fun getDeckWordSamples(deckId: String, limit: Int = 5): List<String> = withContext(Dispatchers.IO) {
        wordDao.getRandomWordSamples(deckId, limit)
    }

    suspend fun getDeckDifficultyHistogram(deckId: String): List<DifficultyBucket> =
        withContext(Dispatchers.IO) { deckRepository.getDifficultyHistogram(deckId) }

    suspend fun getDeckRecentWords(deckId: String, limit: Int = 8): List<String> =
        withContext(Dispatchers.IO) { deckRepository.getRecentWords(deckId, limit) }

    suspend fun getDeckWordClassCounts(deckId: String): List<WordClassCount> =
        withContext(Dispatchers.IO) {
            val counts = wordDao.getWordClassCounts(deckId)
            val knownOrder = WordClassCatalog.allowed.withIndex().associate { it.value to it.index }
            counts.sortedWith(
                compareBy(
                    { knownOrder[it.wordClass] ?: Int.MAX_VALUE },
                    { it.wordClass }
                )
            )
        }

    fun updateSeenTutorial(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateSeenTutorial(value) }
    }

    fun downloadPackFromUrl(url: String, expectedSha256: String?) {
        viewModelScope.launch {
            _deckDownloadProgress.value = DeckDownloadProgress(step = DeckDownloadStep.DOWNLOADING)
            _uiEvents.tryEmit(
                UiEvent(
                    message = "Downloading…",
                    duration = SnackbarDuration.Short,
                    dismissCurrent = true
                )
            )
            try {
                val bytes = withContext(Dispatchers.IO) {
                    var lastUpdate = 0L
                    downloader.download(
                        url.trim(),
                        expectedSha256?.trim().takeUnless { it.isNullOrEmpty() }
                    ) { bytesRead, totalBytes ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 100 || (totalBytes != null && bytesRead == totalBytes)) {
                            _deckDownloadProgress.value = DeckDownloadProgress(
                                step = DeckDownloadStep.DOWNLOADING,
                                bytesRead = bytesRead,
                                totalBytes = totalBytes
                            )
                            lastUpdate = now
                        }
                    }
                }
                // Try JSON first
                val text = bytes.toString(Charsets.UTF_8)
                _deckDownloadProgress.value = DeckDownloadProgress(step = DeckDownloadStep.IMPORTING)
                val (sanitizedPack, imageError) = withContext(Dispatchers.IO) { parseAndSanitizePack(text) }
                withContext(Dispatchers.IO) { deckRepository.importPack(sanitizedPack) }
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Imported deck from URL",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short,
                        dismissCurrent = true
                    )
                )
                if (imageError != null) {
                    showCoverImageErrorSnackbar()
                }
            } catch (t: Throwable) {
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed: ${t.message}",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true,
                        dismissCurrent = true
                    )
                )
            } finally {
                _deckDownloadProgress.value = null
            }
        }
    }

    fun importDeckFromFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Empty file")
                }
                val (sanitizedPack, imageError) = withContext(Dispatchers.IO) { parseAndSanitizePack(text) }
                withContext(Dispatchers.IO) { deckRepository.importPack(sanitizedPack) }
                runCatching {
                    val s = settingsRepository.settings.first()
                    if (sanitizedPack.deck.language == s.languagePreference) {
                        val ids = s.enabledDeckIds.toMutableSet()
                        if (ids.add(sanitizedPack.deck.id)) {
                            settingsRepository.setEnabledDeckIds(ids)
                        }
                    }
                }
                _uiEvents.tryEmit(UiEvent(message = "Imported deck", actionLabel = "OK", duration = SnackbarDuration.Short))
                if (imageError != null) {
                    showCoverImageErrorSnackbar()
                }
            } catch (t: Throwable) {
                _uiEvents.tryEmit(
                    UiEvent(
                        message = "Failed: ${t.message}",
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Long,
                        isError = true
                    )
                )
            }
        }
    }


    suspend fun updateSettings(
        roundSeconds: Int,
        targetWords: Int,
        maxSkips: Int,
        penaltyPerSkip: Int,
        punishSkips: Boolean,
        language: String,
        uiLanguage: String,
        allowNSFW: Boolean,
        haptics: Boolean,
        sound: Boolean,
        oneHanded: Boolean,
        verticalSwipes: Boolean,
        orientation: String,
        teams: List<String>,
    ) {
        // Persist all settings synchronously so callers can chain actions reliably (e.g., Save & Restart)
        val before = settingsRepository.settings.first()
        settingsRepository.updateRoundSeconds(roundSeconds)
        settingsRepository.updateTargetWords(targetWords)
        settingsRepository.updateSkipPolicy(maxSkips, penaltyPerSkip)
        settingsRepository.updatePunishSkips(punishSkips)
        settingsRepository.updateAllowNSFW(allowNSFW)
        settingsRepository.updateHapticsEnabled(haptics)
        settingsRepository.updateSoundEnabled(sound)
        settingsRepository.updateOneHandedLayout(oneHanded)
        settingsRepository.updateVerticalSwipes(verticalSwipes)
        settingsRepository.updateOrientation(orientation)
        settingsRepository.updateUiLanguage(canonicalizeLocalePreference(uiLanguage))
        // Language validation may fail; keep others applied regardless
        val langResult = runCatching { settingsRepository.updateLanguagePreference(language) }
        if (langResult.isFailure) {
            _uiEvents.tryEmit(UiEvent(message = langResult.exceptionOrNull()?.message ?: "Invalid language", duration = SnackbarDuration.Short, isError = true))
        } else {
            val newLang = language.trim().lowercase()
            if (!newLang.equals(before.languagePreference, ignoreCase = true)) {
                val decksAll = deckRepository.getDecks().first()
                val preferred = decksAll.filter { it.language.equals(newLang, ignoreCase = true) }.map { it.id }.toSet()
                if (preferred.isNotEmpty()) {
                    val prevEnabled = before.enabledDeckIds
                    if (prevEnabled != preferred) {
                        settingsRepository.setEnabledDeckIds(preferred)
                        _uiEvents.tryEmit(
                            UiEvent(
                                message = "Enabled ${preferred.size} deck(s) for $newLang",
                                actionLabel = "Undo",
                                onAction = { settingsRepository.setEnabledDeckIds(prevEnabled) }
                            )
                        )
                    }
                }
            }
        }
        // Ensure teams are saved
        settingsRepository.setTeams(teams)
        _uiEvents.tryEmit(UiEvent(message = "Settings updated", actionLabel = "Dismiss"))
    }

    data class WordInfo(val difficulty: Int, val category: String?, val wordClass: String?)
    private val _wordInfo = MutableStateFlow<Map<String, WordInfo>>(emptyMap())
    val wordInfoByText: StateFlow<Map<String, WordInfo>> = _wordInfo.asStateFlow()
    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()
    private val _availableWordClasses = MutableStateFlow<List<String>>(emptyList())
    val availableWordClasses: StateFlow<List<String>> = _availableWordClasses.asStateFlow()

    private fun parseClass(raw: String?): String? {
        return raw
            ?.split(',')
            ?.asSequence()
            ?.mapNotNull { WordClassCatalog.normalizeOrNull(it) }
            ?.firstOrNull()
    }

    fun resetLocalData(onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Clear DB tables
                turnHistoryDao.deleteAll()
                deckDao.deleteAll() // cascades to words via FK
                // Clear preferences
                settingsRepository.clearAll()
            }
            _uiEvents.tryEmit(UiEvent(message = "Local data cleared", actionLabel = "OK"))
            onDone?.invoke()
        }
    }

    fun restartMatch() {
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            val filters = s.toWordQueryFilters()
            val words = withContext(Dispatchers.IO) { loadWords(filters) }
            // Update word info cache for current filters
            viewModelScope.launch(Dispatchers.IO) {
                val briefs = if (filters.deckIds.isEmpty()) emptyList() else wordDao.getWordBriefsForDecks(
                    filters.deckIds,
                    filters.language,
                    filters.allowNSFW,
                    filters.minDifficulty,
                    filters.maxDifficulty,
                    filters.categories,
                    filters.categoryFilterEnabled,
                    filters.wordClasses,
                    filters.wordClassFilterEnabled
                )
                val map = briefs.associateBy({ it.text }) {
                    WordInfo(it.difficulty, it.category, parseClass(it.wordClass))
                }
                _wordInfo.value = map
            }
            // Update available categories
            viewModelScope.launch(Dispatchers.IO) {
                val list = if (filters.deckIds.isEmpty()) emptyList() else wordDao.getAvailableCategories(
                    filters.deckIds, filters.language, filters.allowNSFW
                ).sorted()
                _availableCategories.value = list
            }
            // Update available word classes
            viewModelScope.launch(Dispatchers.IO) {
                val list = if (filters.deckIds.isEmpty()) emptyList() else wordDao.getAvailableWordClasses(
                    filters.deckIds, filters.language, filters.allowNSFW
                )
                _availableWordClasses.value = canonicalizeWordClassFilters(list)
            }
            val e = DefaultGameEngine(words, viewModelScope)
            _engine.value = e
            val config = MatchConfig(
                targetWords = s.targetWords,
                maxSkips = s.maxSkips,
                penaltyPerSkip = if (s.punishSkips) s.penaltyPerSkip else 0,
                roundSeconds = s.roundSeconds
            )
            val seed = java.security.SecureRandom().nextLong()
            e.startMatch(config, teams = s.teams, seed = seed)
            _uiEvents.tryEmit(UiEvent(message = "Match restarted", actionLabel = "Dismiss"))
        }
    }

    fun nextTurn() {
        val e = _engine.value ?: return
        val current = e.state.value
        if (current is com.example.alias.domain.GameState.TurnFinished) {
            viewModelScope.launch {
                val infoByWord = _wordInfo.value
                val entries = current.outcomes.map {
                    com.example.alias.data.db.TurnHistoryEntity(
                        0L,
                        current.team,
                        it.word,
                        it.correct,
                        it.skipped,
                        infoByWord[it.word]?.difficulty,
                        it.timestamp
                    )
                }
                historyRepository.save(entries)
                e.nextTurn()
            }
        }
    }

    fun startTurn() {
        val e = _engine.value ?: return
        viewModelScope.launch { e.startTurn() }
    }

    fun overrideOutcome(index: Int, correct: Boolean) {
        val e = _engine.value ?: return
        viewModelScope.launch { e.overrideOutcome(index, correct) }
    }

    fun setOrientation(value: String) {
        viewModelScope.launch {
            settingsRepository.updateOrientation(value)
        }
    }
}
