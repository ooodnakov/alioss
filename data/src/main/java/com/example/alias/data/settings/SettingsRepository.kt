package com.example.alias.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local settings persisted via Preferences DataStore.
 */
interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun updateRoundSeconds(value: Int)
    suspend fun updateTargetWords(value: Int)
    suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int)
    suspend fun updateLanguagePreference(language: String)
    suspend fun setEnabledDeckIds(ids: Set<String>)
    suspend fun updateAllowNSFW(value: Boolean)
    suspend fun updateStemmingEnabled(value: Boolean)

    // Trusted pack sources (hosts or origins) for manual downloads
    suspend fun setTrustedSources(origins: Set<String>)
}

data class Settings(
    val roundSeconds: Int = 60,
    val targetWords: Int = 20,
    val maxSkips: Int = 3,
    val penaltyPerSkip: Int = 1,
    val languagePreference: String = "en",
    val enabledDeckIds: Set<String> = emptySet(),
    val allowNSFW: Boolean = false,
    val stemmingEnabled: Boolean = false,
    val trustedSources: Set<String> = emptySet(),
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
            languagePreference = p[Keys.LANGUAGE] ?: "en",
            enabledDeckIds = p[Keys.ENABLED_DECK_IDS] ?: emptySet(),
            allowNSFW = p[Keys.ALLOW_NSFW] ?: false,
            stemmingEnabled = p[Keys.STEMMING_ENABLED] ?: false,
            trustedSources = p[Keys.TRUSTED_SOURCES] ?: emptySet(),
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

    override suspend fun setTrustedSources(origins: Set<String>) {
        // Store as provided; downloader applies normalization when checking.
        dataStore.edit { it[Keys.TRUSTED_SOURCES] = origins }
    }

    private object Keys {
        val ROUND_SECONDS = intPreferencesKey("round_seconds")
        val TARGET_WORDS = intPreferencesKey("target_words")
        val MAX_SKIPS = intPreferencesKey("max_skips")
        val PENALTY_PER_SKIP = intPreferencesKey("penalty_per_skip")
        val LANGUAGE = stringPreferencesKey("language_preference")
        val ENABLED_DECK_IDS = stringSetPreferencesKey("enabled_deck_ids")
        val ALLOW_NSFW = booleanPreferencesKey("allow_nsfw")
        val STEMMING_ENABLED = booleanPreferencesKey("stemming_enabled")
        val TRUSTED_SOURCES = stringSetPreferencesKey("trusted_sources")
    }
}
