package com.example.alias.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val DEFAULT_TEAMS = listOf("Red", "Blue")

private fun normalizeWordClasses(values: Set<String>): Set<String> {
    return values
        .mapNotNull { value ->
            val trimmed = value.trim()
            if (trimmed.isEmpty()) {
                null
            } else {
                trimmed.uppercase(Locale.ROOT)
            }
        }
        .toSet()
}

/**
 * Local settings persisted via Preferences DataStore.
 */
interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun updateRoundSeconds(value: Int)
    suspend fun updateTargetWords(value: Int)
    suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int)
    suspend fun updatePunishSkips(value: Boolean)
    suspend fun updateLanguagePreference(language: String)
    suspend fun setEnabledDeckIds(ids: Set<String>)
    suspend fun updateAllowNSFW(value: Boolean)
    suspend fun updateStemmingEnabled(value: Boolean)
    suspend fun updateHapticsEnabled(value: Boolean)
    suspend fun updateSoundEnabled(value: Boolean)
    suspend fun updateOneHandedLayout(value: Boolean)
    suspend fun updateOrientation(value: String)
    suspend fun updateUiLanguage(language: String)
    suspend fun updateDifficultyFilter(min: Int, max: Int)
    suspend fun setCategoriesFilter(categories: Set<String>)
    suspend fun setWordClassesFilter(classes: Set<String>)
    suspend fun setTeams(teams: List<String>)
    suspend fun updateVerticalSwipes(value: Boolean)

    // Trusted pack sources (hosts or origins) for manual downloads
    suspend fun setTrustedSources(origins: Set<String>)

    // Bundled deck asset hash tracking (filename:sha256)
    suspend fun readBundledDeckHashes(): Set<String>
    suspend fun writeBundledDeckHashes(entries: Set<String>)

    suspend fun updateSeenTutorial(value: Boolean)
    suspend fun clearAll()

    companion object {
        const val MIN_TEAMS = 2
        const val MAX_TEAMS = 6
    }
}

data class Settings(
    val roundSeconds: Int = 60,
    val targetWords: Int = 20,
    val maxSkips: Int = 3,
    val penaltyPerSkip: Int = 1,
    val punishSkips: Boolean = true,
    val languagePreference: String = "en",
    val uiLanguage: String = "system",
    val enabledDeckIds: Set<String> = emptySet(),
    val teams: List<String> = DEFAULT_TEAMS,
    val allowNSFW: Boolean = false,
    val stemmingEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val oneHandedLayout: Boolean = false,
    val minDifficulty: Int = 1,
    val maxDifficulty: Int = 5,
    val verticalSwipes: Boolean = false,
    val selectedCategories: Set<String> = emptySet(),
    val selectedWordClasses: Set<String> = emptySet(),
    val orientation: String = "system",
    val trustedSources: Set<String> = emptySet(),
    val seenTutorial: Boolean = false,
)

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<Settings> = dataStore.data.map { p ->
        Settings(
            roundSeconds = p[Keys.ROUND_SECONDS] ?: 60,
            targetWords = p[Keys.TARGET_WORDS] ?: 20,
            maxSkips = p[Keys.MAX_SKIPS] ?: 3,
            penaltyPerSkip = p[Keys.PENALTY_PER_SKIP] ?: 1,
            punishSkips = p[Keys.PUNISH_SKIPS] ?: true,
            languagePreference = p[Keys.LANGUAGE] ?: "en",
            uiLanguage = p[Keys.UI_LANGUAGE] ?: "system",
            enabledDeckIds = p[Keys.ENABLED_DECK_IDS] ?: emptySet(),
            teams = p[Keys.TEAMS]?.split("|")?.filter { it.isNotBlank() }?.take(SettingsRepository.MAX_TEAMS)?.let {
                if (it.size >= SettingsRepository.MIN_TEAMS) it else DEFAULT_TEAMS
            } ?: DEFAULT_TEAMS,
            allowNSFW = p[Keys.ALLOW_NSFW] ?: false,
            stemmingEnabled = p[Keys.STEMMING_ENABLED] ?: false,
            hapticsEnabled = p[Keys.HAPTICS_ENABLED] ?: true,
            soundEnabled = p[Keys.SOUND_ENABLED] ?: true,
            oneHandedLayout = p[Keys.ONE_HANDED] ?: false,
            minDifficulty = p[Keys.MIN_DIFFICULTY] ?: 1,
            maxDifficulty = p[Keys.MAX_DIFFICULTY] ?: 5,
            verticalSwipes = p[Keys.VERTICAL_SWIPES] ?: false,
            selectedCategories = p[Keys.CATEGORIES_FILTER] ?: emptySet(),
            selectedWordClasses = normalizeWordClasses(p[Keys.WORD_CLASSES_FILTER] ?: emptySet()),
            orientation = p[Keys.ORIENTATION] ?: "system",
            trustedSources = p[Keys.TRUSTED_SOURCES] ?: emptySet(),
            seenTutorial = p[Keys.SEEN_TUTORIAL] ?: false,
        )
    }

    override suspend fun updateRoundSeconds(value: Int) {
        dataStore.edit { it[Keys.ROUND_SECONDS] = value.coerceIn(10, 600) }
    }

    override suspend fun updateTargetWords(value: Int) {
        dataStore.edit { it[Keys.TARGET_WORDS] = value.coerceIn(5, 200) }
    }

    override suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int) {
        dataStore.edit {
            it[Keys.MAX_SKIPS] = maxSkips.coerceIn(0, 50)
            it[Keys.PENALTY_PER_SKIP] = penaltyPerSkip.coerceIn(0, 10)
        }
    }

    override suspend fun updatePunishSkips(value: Boolean) {
        dataStore.edit { it[Keys.PUNISH_SKIPS] = value }
    }

    override suspend fun updateLanguagePreference(language: String) {
        // Lightweight validation for BCP-47-ish tags
        val lc = language.trim()
        require(Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$").matches(lc)) { "Invalid language tag" }
        dataStore.edit { it[Keys.LANGUAGE] = lc }
    }

    override suspend fun setEnabledDeckIds(ids: Set<String>) {
        dataStore.edit { it[Keys.ENABLED_DECK_IDS] = ids }
    }

    override suspend fun updateAllowNSFW(value: Boolean) {
        dataStore.edit { it[Keys.ALLOW_NSFW] = value }
    }

    override suspend fun updateStemmingEnabled(value: Boolean) {
        dataStore.edit { it[Keys.STEMMING_ENABLED] = value }
    }

    override suspend fun updateHapticsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.HAPTICS_ENABLED] = value }
    }

    override suspend fun updateSoundEnabled(value: Boolean) {
        dataStore.edit { it[Keys.SOUND_ENABLED] = value }
    }

    override suspend fun updateOneHandedLayout(value: Boolean) {
        dataStore.edit { it[Keys.ONE_HANDED] = value }
    }

    override suspend fun updateOrientation(value: String) {
        val norm = when (value.lowercase()) {
            "portrait", "landscape", "system" -> value.lowercase()
            else -> "system"
        }
        dataStore.edit { it[Keys.ORIENTATION] = norm }
    }

    override suspend fun updateUiLanguage(language: String) {
        dataStore.edit { it[Keys.UI_LANGUAGE] = language }
    }

    override suspend fun updateDifficultyFilter(min: Int, max: Int) {
        val lo = min.coerceIn(1, 5)
        val hi = max.coerceIn(1, 5)
        val (mn, mx) = if (lo <= hi) lo to hi else hi to lo
        dataStore.edit {
            it[Keys.MIN_DIFFICULTY] = mn
            it[Keys.MAX_DIFFICULTY] = mx
        }
    }

    override suspend fun setCategoriesFilter(categories: Set<String>) {
        dataStore.edit { it[Keys.CATEGORIES_FILTER] = categories }
    }

    override suspend fun setWordClassesFilter(classes: Set<String>) {
        val normalized = normalizeWordClasses(classes)
        dataStore.edit { it[Keys.WORD_CLASSES_FILTER] = normalized }
    }

    override suspend fun setTeams(teams: List<String>) {
        val norm = teams.map { it.trim() }.filter { it.isNotEmpty() }.take(SettingsRepository.MAX_TEAMS)
        require(norm.size in SettingsRepository.MIN_TEAMS..SettingsRepository.MAX_TEAMS)
        dataStore.edit { it[Keys.TEAMS] = norm.joinToString("|") }
    }

    override suspend fun updateVerticalSwipes(value: Boolean) {
        dataStore.edit { it[Keys.VERTICAL_SWIPES] = value }
    }

    override suspend fun setTrustedSources(origins: Set<String>) {
        // Store as provided; downloader applies normalization when checking.
        dataStore.edit { it[Keys.TRUSTED_SOURCES] = origins }
    }

    override suspend fun updateSeenTutorial(value: Boolean) {
        dataStore.edit { it[Keys.SEEN_TUTORIAL] = value }
    }

    override suspend fun readBundledDeckHashes(): Set<String> =
        dataStore.data.first()[Keys.BUNDLED_DECK_HASHES] ?: emptySet()

    override suspend fun writeBundledDeckHashes(entries: Set<String>) {
        dataStore.edit { it[Keys.BUNDLED_DECK_HASHES] = entries }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val ROUND_SECONDS = intPreferencesKey("round_seconds")
        val TARGET_WORDS = intPreferencesKey("target_words")
        val MAX_SKIPS = intPreferencesKey("max_skips")
        val PENALTY_PER_SKIP = intPreferencesKey("penalty_per_skip")
        val PUNISH_SKIPS = booleanPreferencesKey("punish_skips")
        val LANGUAGE = stringPreferencesKey("language_preference")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val ENABLED_DECK_IDS = stringSetPreferencesKey("enabled_deck_ids")
        val ALLOW_NSFW = booleanPreferencesKey("allow_nsfw")
        val STEMMING_ENABLED = booleanPreferencesKey("stemming_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val ONE_HANDED = booleanPreferencesKey("one_handed_layout")
        val MIN_DIFFICULTY = intPreferencesKey("min_difficulty")
        val MAX_DIFFICULTY = intPreferencesKey("max_difficulty")
        val VERTICAL_SWIPES = booleanPreferencesKey("vertical_swipes")
        val CATEGORIES_FILTER = stringSetPreferencesKey("categories_filter")
        val WORD_CLASSES_FILTER = stringSetPreferencesKey("word_classes_filter")
        val ORIENTATION = stringPreferencesKey("orientation_mode")
        val TEAMS = stringPreferencesKey("teams")
        val TRUSTED_SOURCES = stringSetPreferencesKey("trusted_sources")
        val SEEN_TUTORIAL = booleanPreferencesKey("seen_tutorial")
        val BUNDLED_DECK_HASHES = stringSetPreferencesKey("bundled_deck_hashes")
    }
}
