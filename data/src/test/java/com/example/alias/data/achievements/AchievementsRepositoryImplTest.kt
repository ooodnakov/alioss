package com.example.alias.data.achievements

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPathApi::class, ExperimentalCoroutinesApi::class)
class AchievementsRepositoryImplTest {
    private lateinit var scope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AchievementsRepository
    private lateinit var tempDir: Path

    @Before
    fun setUp() {
        scope = TestScope(UnconfinedTestDispatcher())
        tempDir = createTempDirectory("achievements-test")
        dataStore = PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope.backgroundScope,
            produceFile = { tempDir.resolve("store.preferences_pb").toString().toPath() },
        )
        repository = AchievementsRepositoryImpl(dataStore, clock = { 1_234L })
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
    }

    @Test
    fun recordCorrectGuessesUnlocksChampion() = scope.runTest {
        repository.recordCorrectGuesses(500)
        advanceUntilIdle()

        val state = repository.achievements.first { states ->
            states.any { it.definition.id == AchievementId.WORD_CHAMPION }
        }
        val champion = state.first { it.definition.id == AchievementId.WORD_CHAMPION }
        assertTrue(champion.progress.isUnlocked)
        assertEquals(1_234L, champion.unlockedAtMillis)
    }

    @Test
    fun visitingSectionsUnlocksExplorer() = scope.runTest {
        repository.recordSectionVisited(AchievementSection.HOME)
        repository.recordSectionVisited(AchievementSection.GAME)
        repository.recordSectionVisited(AchievementSection.DECKS)
        repository.recordSectionVisited(AchievementSection.SETTINGS)
        advanceUntilIdle()

        val state = repository.achievements.first { states ->
            states.any { it.definition.id == AchievementId.APP_EXPLORER && it.progress.isUnlocked }
        }
        val explorer = state.first { it.definition.id == AchievementId.APP_EXPLORER }
        assertTrue(explorer.progress.isUnlocked)
    }
}
