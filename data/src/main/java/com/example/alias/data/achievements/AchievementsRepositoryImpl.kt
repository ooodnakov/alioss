package com.example.alias.data.achievements

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class AchievementsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val definitions: List<AchievementDefinition> = AchievementCatalog.definitions,
) : AchievementsRepository {

    companion object {
        private val TOTAL_CORRECT = intPreferencesKey("achievement_total_correct")
        private val FAST_WINS = intPreferencesKey("achievement_fast_wins")
        private val PERFECT_TURNS = intPreferencesKey("achievement_perfect_turns")
        private val SETTINGS_ADJUSTMENTS = intPreferencesKey("achievement_settings_adjustments")
        private val VISITED_SECTIONS = stringSetPreferencesKey("achievement_visited_sections")
    }

    private val unlockedKeys: Map<AchievementId, Preferences.Key<Long>> =
        definitions.associate {
            it.id to longPreferencesKey(
                "achievement_${it.id.name.lowercase(Locale.ROOT)}_unlocked_at",
            )
        }

    override val achievements: Flow<List<AchievementState>> = dataStore.data.map { prefs ->
        val stats = prefs.toStats()
        definitions.map { definition ->
            val progress = definition.condition.evaluate(stats)
            val unlockedAt = prefs[unlockedKeys.getValue(definition.id)]
            AchievementState(definition, progress, unlockedAt)
        }
    }

    override suspend fun recordCorrectGuesses(count: Int) {
        if (count <= 0) return
        updateStats { prefs ->
            prefs[TOTAL_CORRECT] = (prefs[TOTAL_CORRECT] ?: 0) + count
        }
    }

    override suspend fun recordPerfectTurn() {
        updateStats { prefs ->
            prefs[PERFECT_TURNS] = (prefs[PERFECT_TURNS] ?: 0) + 1
        }
    }

    override suspend fun recordFastMatchWin() {
        updateStats { prefs ->
            prefs[FAST_WINS] = (prefs[FAST_WINS] ?: 0) + 1
        }
    }

    override suspend fun recordSettingsAdjustment() {
        updateStats { prefs ->
            prefs[SETTINGS_ADJUSTMENTS] = (prefs[SETTINGS_ADJUSTMENTS] ?: 0) + 1
        }
    }

    override suspend fun recordSectionVisited(section: AchievementSection) {
        dataStore.edit { prefs ->
            val current = prefs[VISITED_SECTIONS]?.toMutableSet() ?: mutableSetOf()
            val changed = current.add(section.name)
            if (changed) {
                prefs[VISITED_SECTIONS] = current
                maybeUnlock(prefs)
            }
        }
    }

    private suspend fun updateStats(block: (MutablePreferences) -> Unit) {
        dataStore.edit { prefs ->
            block(prefs)
            maybeUnlock(prefs)
        }
    }

    private fun maybeUnlock(prefs: MutablePreferences) {
        val stats = prefs.toStats()
        val now = clock()
        for (definition in definitions) {
            val progress = definition.condition.evaluate(stats)
            if (progress.isUnlocked) {
                val key = unlockedKeys.getValue(definition.id)
                if (!prefs.contains(key)) {
                    prefs[key] = now
                }
            }
        }
    }

    private fun Preferences.toStats(): AchievementStats {
        val sections = this[VISITED_SECTIONS]
            ?.mapNotNull { value -> runCatching { AchievementSection.valueOf(value) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
        return AchievementStats(
            totalCorrectGuesses = this[TOTAL_CORRECT] ?: 0,
            fastMatchWins = this[FAST_WINS] ?: 0,
            perfectTurns = this[PERFECT_TURNS] ?: 0,
            settingsAdjustments = this[SETTINGS_ADJUSTMENTS] ?: 0,
            visitedSections = sections,
        )
    }
}
