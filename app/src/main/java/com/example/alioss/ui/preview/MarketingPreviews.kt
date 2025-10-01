@file:Suppress("UnusedPrivateMember")

package com.example.alioss.ui.preview

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.alioss.SettingsUpdateRequest
import com.example.alioss.WordInfo
import com.example.alioss.aliossAppTheme
import com.example.alioss.data.db.DeckEntity
import com.example.alioss.data.db.DifficultyBucket
import com.example.alioss.data.db.TurnHistoryEntity
import com.example.alioss.data.db.WordClassCount
import com.example.alioss.data.settings.Settings
import com.example.alioss.domain.GameEngine
import com.example.alioss.data.achievements.AchievementCatalog
import com.example.alioss.data.achievements.AchievementId
import com.example.alioss.data.achievements.AchievementProgress
import com.example.alioss.data.achievements.AchievementState
import com.example.alioss.domain.GameState
import com.example.alioss.domain.MatchGoal
import com.example.alioss.domain.MatchGoalType
import com.example.alioss.domain.TurnOutcome
import com.example.alioss.ui.about.aboutScreen
import com.example.alioss.ui.appScaffold
import com.example.alioss.ui.decks.DeckDownloadProgress
import com.example.alioss.ui.decks.DeckDownloadStep
import com.example.alioss.ui.decks.DecksScreenViewModel
import com.example.alioss.ui.decks.deckDetailScreen
import com.example.alioss.ui.decks.decksScreen
import com.example.alioss.ui.game.GameScreenViewModel
import com.example.alioss.ui.game.gameScreen
import com.example.alioss.ui.historyScreen
import com.example.alioss.ui.home.HomeActions
import com.example.alioss.ui.home.HomeViewState
import com.example.alioss.ui.home.homeScreen
import com.example.alioss.ui.settings.SettingsScreenViewModel
import com.example.alioss.ui.settings.settingsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val SAMPLE_NOW = 1_720_000_000_000L
private const val TEAM_CRIMSON_COMETS = "Crimson Comets"
private const val TEAM_AZURE_OWLS = "Azure Owls"
private const val TEAM_GOLDEN_FOXES = "Golden Foxes"
private const val MATCH_ID_820 = "match-820"
private const val MATCH_ID_821 = "match-821"
private const val MARKETING_PREVIEW_WIDTH_DP = 414
private const val MARKETING_PREVIEW_HEIGHT_DP = 897

internal object MarketingPreviewIds {
    const val HOME = "home"
    const val GAME_TURN_PENDING = "game_turn_pending"
    const val GAME_TURN_ACTIVE = "game_turn_active"
    const val GAME_TURN_FINISHED = "game_turn_finished"
    const val DECKS = "decks"
    const val DECK_DETAIL = "deck_detail"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val ABOUT = "about"
}

internal object MarketingPreviewNames {
    const val HOME = "Home – Marketing"
    const val GAME_TURN_PENDING = "Game – Turn Pending"
    const val GAME_TURN_ACTIVE = "Game – Turn Active"
    const val GAME_TURN_FINISHED = "Game – Turn Finished"
    const val DECKS = "Decks – Marketing"
    const val DECK_DETAIL = "Deck Detail – Marketing"
    const val SETTINGS = "Settings – Marketing"
    const val HISTORY = "History – Marketing"
    const val ABOUT = "About – Marketing"
}

private val SampleDecks = listOf(
    DeckEntity(
        id = "official-classics",
        name = "Classic Party Pack",
        language = "en",
        isOfficial = true,
        isNSFW = false,
        version = 3,
        updatedAt = SAMPLE_NOW - 864_000_000L,
        coverImageBase64 = null,
        author = "Alioss Studio",
    ),
    DeckEntity(
        id = "global-giggles",
        name = "Global Giggles",
        language = "es",
        isOfficial = false,
        isNSFW = false,
        version = 1,
        updatedAt = SAMPLE_NOW - 604_800_000L,
        coverImageBase64 = null,
        author = "Luna Morales",
    ),
    DeckEntity(
        id = "midnight-madness",
        name = "Midnight Madness",
        language = "en",
        isOfficial = false,
        isNSFW = true,
        version = 5,
        updatedAt = SAMPLE_NOW - 259_200_000L,
        coverImageBase64 = null,
        author = "Night Owls Collective",
    ),
)

private val SampleSettings = Settings(
    roundSeconds = 75,
    targetWords = 25,
    scoreTargetEnabled = true,
    targetScore = 60,
    maxSkips = 2,
    penaltyPerSkip = 1,
    punishSkips = true,
    uiLanguage = "en",
    enabledDeckIds = SampleDecks.take(2).map { it.id }.toSet(),
    selectedDeckLanguages = setOf("en", "es"),
    teams = listOf(TEAM_CRIMSON_COMETS, TEAM_AZURE_OWLS, TEAM_GOLDEN_FOXES),
    allowNSFW = true,
    stemmingEnabled = true,
    hapticsEnabled = true,
    soundEnabled = true,
    oneHandedLayout = false,
    minDifficulty = 1,
    maxDifficulty = 5,
    verticalSwipes = true,
    selectedCategories = setOf("Party", "Movies"),
    selectedWordClasses = setOf("Noun", "Verb"),
    orientation = "landscape",
    trustedSources = setOf("alioss.example.com", "party.local"),
    deletedBundledDeckIds = setOf("bundled-classics"),
    deletedImportedDeckIds = setOf("indie-pack"),
    seenTutorial = true,
)

private val SampleAchievements = listOf(
    sampleAchievementState(
        id = AchievementId.WORD_CHAMPION,
        current = 320,
        target = 500,
    ),
    sampleAchievementState(
        id = AchievementId.SPEED_RUNNER,
        current = 1,
        target = 1,
        unlockedAt = SAMPLE_NOW - 48_000L,
    ),
    sampleAchievementState(
        id = AchievementId.SETTINGS_TINKERER,
        current = 6,
        target = 10,
    ),
    sampleAchievementState(
        id = AchievementId.APP_EXPLORER,
        current = 4,
        target = 4,
        unlockedAt = SAMPLE_NOW - 120_000L,
    ),
)

private val SampleHistory = listOf(
    TurnHistoryEntity(
        id = 1,
        team = TEAM_CRIMSON_COMETS,
        word = "Aurora",
        correct = true,
        skipped = false,
        difficulty = 2,
        timestamp = SAMPLE_NOW - 120_000L,
        matchId = MATCH_ID_820,
    ),
    TurnHistoryEntity(
        id = 2,
        team = TEAM_AZURE_OWLS,
        word = "Momentum",
        correct = true,
        skipped = false,
        difficulty = 3,
        timestamp = SAMPLE_NOW - 90_000L,
        matchId = MATCH_ID_820,
    ),
    TurnHistoryEntity(
        id = 3,
        team = TEAM_GOLDEN_FOXES,
        word = "Nebula",
        correct = false,
        skipped = true,
        difficulty = 4,
        timestamp = SAMPLE_NOW - 60_000L,
        matchId = MATCH_ID_820,
    ),
    TurnHistoryEntity(
        id = 4,
        team = TEAM_CRIMSON_COMETS,
        word = "Catalyst",
        correct = true,
        skipped = false,
        difficulty = 3,
        timestamp = SAMPLE_NOW - 30_000L,
        matchId = MATCH_ID_821,
    ),
    TurnHistoryEntity(
        id = 5,
        team = TEAM_AZURE_OWLS,
        word = "Quasar",
        correct = false,
        skipped = false,
        difficulty = 5,
        timestamp = SAMPLE_NOW - 10_000L,
        matchId = MATCH_ID_821,
    ),
    TurnHistoryEntity(
        id = 6,
        team = TEAM_GOLDEN_FOXES,
        word = "Mirage",
        correct = true,
        skipped = false,
        difficulty = 2,
        timestamp = SAMPLE_NOW,
        matchId = MATCH_ID_821,
    ),
)

private val SampleWordInfo = mapOf(
    "Momentum" to WordInfo(difficulty = 3, category = "Science", wordClass = "Noun"),
    "Catalyst" to WordInfo(difficulty = 4, category = "Science", wordClass = "Noun"),
    "Nebula" to WordInfo(difficulty = 2, category = "Space", wordClass = "Noun"),
    "Quasar" to WordInfo(difficulty = 5, category = "Space", wordClass = "Noun"),
    "Mirage" to WordInfo(difficulty = 3, category = "Travel", wordClass = "Noun"),
    "Velocity" to WordInfo(difficulty = 4, category = "Science", wordClass = "Noun"),
)

private fun sampleAchievementState(
    id: AchievementId,
    current: Int,
    target: Int,
    unlockedAt: Long? = null,
): AchievementState {
    val definition = AchievementCatalog.definitions.first { it.id == id }
    return AchievementState(
        definition = definition,
        progress = AchievementProgress(current = current, target = target),
        unlockedAtMillis = unlockedAt,
    )
}

@Composable
internal fun homeMarketingPreviewContent() {
    aliossAppTheme {
        appScaffold {
            homeScreen(
                state = HomeViewState(
                    gameState = GameState.MatchFinished(
                        scores = mapOf(
                            TEAM_CRIMSON_COMETS to 62,
                            TEAM_AZURE_OWLS to 58,
                            TEAM_GOLDEN_FOXES to 44,
                        ),
                    ),
                    settings = SampleSettings,
                    decks = SampleDecks,
                    recentHistory = SampleHistory,
                    achievements = SampleAchievements,
                ),
                actions = HomeActions(
                    onResumeMatch = {},
                    onStartNewMatch = {},
                    onHistory = {},
                    onSettings = {},
                    onDecks = {},
                    onAchievements = {},
                ),
            )
        }
    }
}

@Preview(
    name = MarketingPreviewNames.HOME,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun homeScreenMarketingPreview() {
    homeMarketingPreviewContent()
}

@Composable
internal fun gameTurnPendingMarketingPreviewContent() {
    aliossAppTheme {
        appScaffold {
            gameScreen(
                vm = PreviewGameViewModel(SampleWordInfo, tutorial = false),
                engine = PreviewGameEngine(
                    GameState.TurnPending(
                        team = TEAM_CRIMSON_COMETS,
                        scores = mapOf(
                            TEAM_CRIMSON_COMETS to 42,
                            TEAM_AZURE_OWLS to 38,
                            TEAM_GOLDEN_FOXES to 24,
                        ),
                        goal = MatchGoal(MatchGoalType.TARGET_SCORE, target = 60),
                        remainingToGoal = 18,
                    ),
                ),
                settings = SampleSettings,
                onNavigateHome = {},
            )
        }
    }
}

@Preview(
    name = MarketingPreviewNames.GAME_TURN_PENDING,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun gameTurnPendingMarketingPreview() {
    gameTurnPendingMarketingPreviewContent()
}

@Composable
internal fun gameTurnActiveMarketingPreviewContent() {
    aliossAppTheme {
        appScaffold {
            gameScreen(
                vm = PreviewGameViewModel(SampleWordInfo, tutorial = true),
                engine = PreviewGameEngine(
                    initialState = GameState.TurnActive(
                        team = TEAM_AZURE_OWLS,
                        word = "Momentum",
                        goal = MatchGoal(MatchGoalType.TARGET_SCORE, target = 60),
                        remainingToGoal = 15,
                        score = 12,
                        skipsRemaining = 1,
                        timeRemaining = 22,
                        totalSeconds = SampleSettings.roundSeconds,
                    ),
                    nextWord = "Catalyst",
                ),
                settings = SampleSettings,
                onNavigateHome = {},
            )
        }
    }
}

@Preview(
    name = MarketingPreviewNames.GAME_TURN_ACTIVE,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun gameTurnActiveMarketingPreview() {
    gameTurnActiveMarketingPreviewContent()
}

@Composable
internal fun gameTurnFinishedMarketingPreviewContent() {
    aliossAppTheme {
        appScaffold {
            gameScreen(
                vm = PreviewGameViewModel(SampleWordInfo),
                engine = PreviewGameEngine(
                    GameState.TurnFinished(
                        team = TEAM_GOLDEN_FOXES,
                        deltaScore = 7,
                        scores = mapOf(
                            TEAM_CRIMSON_COMETS to 49,
                            TEAM_AZURE_OWLS to 53,
                            TEAM_GOLDEN_FOXES to 51,
                        ),
                        outcomes = listOf(
                            TurnOutcome(word = "Mirage", correct = true, timestamp = SAMPLE_NOW - 8_000L),
                            TurnOutcome(word = "Quasar", correct = false, timestamp = SAMPLE_NOW - 6_000L),
                            TurnOutcome(word = "Catalyst", correct = true, timestamp = SAMPLE_NOW - 4_000L),
                        ),
                        matchOver = false,
                    ),
                ),
                settings = SampleSettings,
                onNavigateHome = {},
            )
        }
    }
}

@Preview(
    name = MarketingPreviewNames.GAME_TURN_FINISHED,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun gameTurnFinishedMarketingPreview() {
    gameTurnFinishedMarketingPreviewContent()
}

@Composable
internal fun decksMarketingPreviewContent() {
    aliossAppTheme {
        val vm = remember { PreviewDecksViewModel(SampleDecks, SampleSettings) }
        appScaffold {
            decksScreen(vm = vm, onDeckSelected = {})
        }
    }
}

@Preview(
    name = MarketingPreviewNames.DECKS,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun decksScreenMarketingPreview() {
    decksMarketingPreviewContent()
}

@Preview(
    name = MarketingPreviewNames.DECK_DETAIL,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun deckDetailMarketingPreview() {
    deckDetailMarketingPreviewContent()
}

@Composable
internal fun deckDetailMarketingPreviewContent() {
    aliossAppTheme {
        val vm = remember { PreviewDecksViewModel(SampleDecks, SampleSettings) }
        appScaffold {
            deckDetailScreen(vm = vm, deck = SampleDecks.first())
        }
    }
}

@Composable
internal fun settingsMarketingPreviewContent() {
    aliossAppTheme {
        val vm = remember { PreviewSettingsViewModel(SampleSettings) }
        appScaffold {
            settingsScreen(vm = vm, onBack = {}, onAbout = {})
        }
    }
}

@Preview(
    name = MarketingPreviewNames.SETTINGS,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun settingsScreenMarketingPreview() {
    settingsMarketingPreviewContent()
}

@Preview(
    name = MarketingPreviewNames.HISTORY,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun historyScreenMarketingPreview() {
    historyMarketingPreviewContent()
}

@Composable
internal fun historyMarketingPreviewContent() {
    aliossAppTheme {
        appScaffold {
            historyScreen(history = SampleHistory, onResetHistory = {})
        }
    }
}

@Composable
internal fun aboutMarketingPreviewContent() {
    aliossAppTheme {
        appScaffold { aboutScreen() }
    }
}

@Preview(
    name = MarketingPreviewNames.ABOUT,
    showBackground = true,
    widthDp = MARKETING_PREVIEW_WIDTH_DP,
    heightDp = MARKETING_PREVIEW_HEIGHT_DP,
)
@Composable
private fun aboutScreenMarketingPreview() {
    aboutMarketingPreviewContent()
}

internal data class MarketingPreviewSpec(
    val id: String,
    val displayName: String,
    val widthDp: Int = MARKETING_PREVIEW_WIDTH_DP,
    val heightDp: Int = MARKETING_PREVIEW_HEIGHT_DP,
    val content: @Composable () -> Unit,
)

internal val MarketingPreviewSpecs = listOf(
    MarketingPreviewSpec(
        id = MarketingPreviewIds.HOME,
        displayName = MarketingPreviewNames.HOME,
        content = { homeMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.GAME_TURN_PENDING,
        displayName = MarketingPreviewNames.GAME_TURN_PENDING,
        content = { gameTurnPendingMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.GAME_TURN_ACTIVE,
        displayName = MarketingPreviewNames.GAME_TURN_ACTIVE,
        content = { gameTurnActiveMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.GAME_TURN_FINISHED,
        displayName = MarketingPreviewNames.GAME_TURN_FINISHED,
        content = { gameTurnFinishedMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.DECKS,
        displayName = MarketingPreviewNames.DECKS,
        content = { decksMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.DECK_DETAIL,
        displayName = MarketingPreviewNames.DECK_DETAIL,
        content = { deckDetailMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.SETTINGS,
        displayName = MarketingPreviewNames.SETTINGS,
        content = { settingsMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.HISTORY,
        displayName = MarketingPreviewNames.HISTORY,
        content = { historyMarketingPreviewContent() },
    ),
    MarketingPreviewSpec(
        id = MarketingPreviewIds.ABOUT,
        displayName = MarketingPreviewNames.ABOUT,
        content = { aboutMarketingPreviewContent() },
    ),
)

private class PreviewGameViewModel(
    wordInfo: Map<String, WordInfo>,
    tutorial: Boolean = false,
) : GameScreenViewModel {
    private val tutorialFlow = MutableStateFlow(tutorial)
    private val wordInfoFlow = MutableStateFlow(wordInfo)

    override val showTutorialOnFirstTurn: StateFlow<Boolean> = tutorialFlow.asStateFlow()
    override val wordInfoByText: StateFlow<Map<String, WordInfo>> = wordInfoFlow.asStateFlow()

    override fun dismissTutorialOnFirstTurn() {
        tutorialFlow.value = false
    }

    override fun updateSeenTutorial(value: Boolean) {
        tutorialFlow.value = !value && tutorialFlow.value
    }

    override fun restartMatch() = Unit

    override fun startTurn() = Unit

    override fun nextTurn() = Unit

    override fun overrideOutcome(index: Int, correct: Boolean) = Unit
}

private class PreviewDecksViewModel(
    decks: List<DeckEntity>,
    settings: Settings,
) : DecksScreenViewModel {
    private val decksFlow = MutableStateFlow(decks)
    private val enabledFlow = MutableStateFlow(settings.enabledDeckIds)
    private val trustedFlow = MutableStateFlow(settings.trustedSources)
    private val settingsFlow = MutableStateFlow(settings)
    private val downloadFlow = MutableStateFlow<DeckDownloadProgress?>(
        DeckDownloadProgress(
            step = DeckDownloadStep.IMPORTING,
            bytesRead = 4_200_000L,
            totalBytes = 5_000_000L,
        ),
    )
    private val categoriesFlow = MutableStateFlow(listOf("Party", "Movies", "Travel", "Tech"))
    private val wordClassesFlow = MutableStateFlow(listOf("Noun", "Verb", "Adjective"))

    override val decks: StateFlow<List<DeckEntity>> = decksFlow
    override val enabledDeckIds: StateFlow<Set<String>> = enabledFlow
    override val trustedSources: StateFlow<Set<String>> = trustedFlow
    override val settings: StateFlow<Settings> = settingsFlow
    override val deckDownloadProgress: StateFlow<DeckDownloadProgress?> = downloadFlow
    override val availableCategories: StateFlow<List<String>> = categoriesFlow
    override val availableWordClasses: StateFlow<List<String>> = wordClassesFlow

    override fun importDeckFromFile(uri: Uri) = Unit

    override fun updateDifficultyFilter(min: Int, max: Int) {
        settingsFlow.value = settingsFlow.value.copy(minDifficulty = min, maxDifficulty = max)
    }

    override fun updateDeckLanguagesFilter(languages: Set<String>) {
        settingsFlow.value = settingsFlow.value.copy(selectedDeckLanguages = languages)
    }

    override fun updateCategoriesFilter(categories: Set<String>) {
        settingsFlow.value = settingsFlow.value.copy(selectedCategories = categories)
    }

    override fun updateWordClassesFilter(wordClasses: Set<String>) {
        settingsFlow.value = settingsFlow.value.copy(selectedWordClasses = wordClasses)
    }

    override fun downloadPackFromUrl(url: String, expectedSha256: String?) {
        downloadFlow.value = DeckDownloadProgress(
            step = DeckDownloadStep.DOWNLOADING,
            bytesRead = 2_500_000L,
            totalBytes = 5_000_000L,
        )
    }

    override fun removeTrustedSource(entry: String) {
        trustedFlow.value = trustedFlow.value - entry
    }

    override fun addTrustedSource(entry: String) {
        trustedFlow.value = trustedFlow.value + entry
    }

    override fun restoreDeletedBundledDeck(deckId: String) = Unit

    override fun restoreDeletedImportedDeck(deckId: String) = Unit

    override fun permanentlyDeleteImportedDeck(deck: DeckEntity) {
        decksFlow.value = decksFlow.value.filterNot { it.id == deck.id }
    }

    override fun permanentlyDeleteImportedDeck(deckId: String) {
        decksFlow.value = decksFlow.value.filterNot { it.id == deckId }
    }

    override fun setAllDecksEnabled(enableAll: Boolean) {
        val ids = if (enableAll) decksFlow.value.map { it.id }.toSet() else emptySet()
        enabledFlow.value = ids
        settingsFlow.value = settingsFlow.value.copy(enabledDeckIds = ids)
    }

    override fun setDeckEnabled(id: String, enabled: Boolean) {
        val updated = enabledFlow.value.toMutableSet()
        if (enabled) updated.add(id) else updated.remove(id)
        enabledFlow.value = updated
        settingsFlow.value = settingsFlow.value.copy(enabledDeckIds = updated)
    }

    override fun deleteDeck(deck: DeckEntity) {
        decksFlow.value = decksFlow.value.filterNot { it.id == deck.id }
    }

    override suspend fun getWordCount(deckId: String): Int = 420

    override suspend fun getDeckCategories(deckId: String): List<String> =
        listOf("Party", "Movies", "Travel")

    override suspend fun getDeckWordClassCounts(deckId: String): List<WordClassCount> = listOf(
        WordClassCount(wordClass = "Noun", count = 128),
        WordClassCount(wordClass = "Verb", count = 96),
        WordClassCount(wordClass = "Adjective", count = 72),
    )

    override suspend fun getDeckDifficultyHistogram(deckId: String): List<DifficultyBucket> = listOf(
        DifficultyBucket(difficulty = 1, count = 32),
        DifficultyBucket(difficulty = 2, count = 48),
        DifficultyBucket(difficulty = 3, count = 54),
        DifficultyBucket(difficulty = 4, count = 26),
        DifficultyBucket(difficulty = 5, count = 12),
    )

    override suspend fun getDeckRecentWords(deckId: String, limit: Int): List<String> =
        listOf("Aurora", "Catalyst", "Nimbus", "Velocity", "Serendipity").take(limit)

    override suspend fun getDeckWordSamples(deckId: String, limit: Int): List<String> =
        listOf("Pulse", "Maverick", "Cascade", "Quasar", "Mirage", "Halo").take(limit)
}

private class PreviewSettingsViewModel(initial: Settings) : SettingsScreenViewModel {
    private val settingsFlow = MutableStateFlow(initial)

    override val settings: StateFlow<Settings> = settingsFlow.asStateFlow()

    override fun updateSeenTutorial(value: Boolean) {
        settingsFlow.value = settingsFlow.value.copy(seenTutorial = value)
    }

    override suspend fun updateSettings(request: SettingsUpdateRequest) {
        settingsFlow.value = settingsFlow.value.copy(
            roundSeconds = request.roundSeconds,
            targetWords = request.targetWords,
            targetScore = request.targetScore,
            scoreTargetEnabled = request.scoreTargetEnabled,
            maxSkips = request.maxSkips,
            penaltyPerSkip = request.penaltyPerSkip,
            punishSkips = request.punishSkips,
            uiLanguage = request.uiLanguage,
            allowNSFW = request.allowNSFW,
            hapticsEnabled = request.haptics,
            soundEnabled = request.sound,
            oneHandedLayout = request.oneHanded,
            verticalSwipes = request.verticalSwipes,
            orientation = request.orientation,
            teams = request.teams,
        )
    }

    override fun resetLocalData(onDone: (() -> Unit)?) {
        onDone?.invoke()
    }

    override fun restartMatch() = Unit
}

private class PreviewGameEngine(
    initialState: GameState,
    private val nextWord: String? = null,
) : GameEngine {
    private val stateFlow = MutableStateFlow(initialState)

    override val state: StateFlow<GameState> = stateFlow.asStateFlow()

    override suspend fun startMatch(
        config: com.example.alioss.domain.MatchConfig,
        teams: List<String>,
        seed: Long,
    ) = Unit

    override suspend fun correct() = Unit

    override suspend fun skip() = Unit

    override suspend fun nextTurn() = Unit

    override suspend fun startTurn() = Unit

    override suspend fun overrideOutcome(index: Int, correct: Boolean) = Unit

    override suspend fun peekNextWord(): String? = nextWord
}
