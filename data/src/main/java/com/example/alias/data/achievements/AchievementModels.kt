package com.example.alias.data.achievements

import kotlin.math.max

/** Identifiers for all supported achievements. */
enum class AchievementId {
    WORD_CHAMPION,
    SPEED_RUNNER,
    PERFECTIONIST,
    SETTINGS_TINKERER,
    APP_EXPLORER,
}

/** High-level sections of the app that can be tracked for achievements. */
enum class AchievementSection {
    HOME,
    GAME,
    DECKS,
    SETTINGS,
    HISTORY,
    ABOUT,
}

/** Snapshot of counters used to evaluate achievement progress. */
data class AchievementStats(
    val totalCorrectGuesses: Int = 0,
    val fastMatchWins: Int = 0,
    val perfectTurns: Int = 0,
    val settingsAdjustments: Int = 0,
    val visitedSections: Set<AchievementSection> = emptySet(),
)

/** Progress representation for a single achievement. */
data class AchievementProgress(
    val current: Int,
    val target: Int,
) {
    val isUnlocked: Boolean
        get() = current >= target
}

/** Strategy interface describing how to evaluate an achievement's condition. */
sealed interface AchievementCondition {
    fun evaluate(stats: AchievementStats): AchievementProgress

    /** Simple threshold-based achievement condition. */
    class Threshold(
        private val target: Int,
        private val selector: (AchievementStats) -> Int,
    ) : AchievementCondition {
        init {
            require(target > 0) { "Achievement targets must be positive" }
        }

        override fun evaluate(stats: AchievementStats): AchievementProgress {
            val current = max(0, selector(stats))
            return AchievementProgress(current = current, target = target)
        }
    }
}

/** Static metadata for an achievement. */
data class AchievementDefinition(
    val id: AchievementId,
    val title: String,
    val description: String,
    val condition: AchievementCondition,
)

/** Combined runtime state for an achievement. */
data class AchievementState(
    val definition: AchievementDefinition,
    val progress: AchievementProgress,
    val unlockedAtMillis: Long?,
)

/** Central catalogue describing all built-in achievements. */
object AchievementCatalog {
    val definitions: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = AchievementId.WORD_CHAMPION,
            title = "Word Champion",
            description = "Guess 500 words correctly across all matches.",
            condition = AchievementCondition.Threshold(target = 500) { it.totalCorrectGuesses },
        ),
        AchievementDefinition(
            id = AchievementId.SPEED_RUNNER,
            title = "Speed Runner",
            description = "Win a match in five turns or fewer.",
            condition = AchievementCondition.Threshold(target = 1) { it.fastMatchWins },
        ),
        AchievementDefinition(
            id = AchievementId.PERFECTIONIST,
            title = "Perfectionist",
            description = "Finish a turn without skips or mistakes.",
            condition = AchievementCondition.Threshold(target = 1) { it.perfectTurns },
        ),
        AchievementDefinition(
            id = AchievementId.SETTINGS_TINKERER,
            title = "Settings Tinkerer",
            description = "Adjust game settings ten different times.",
            condition = AchievementCondition.Threshold(target = 10) { it.settingsAdjustments },
        ),
        AchievementDefinition(
            id = AchievementId.APP_EXPLORER,
            title = "App Explorer",
            description = "Visit at least four sections of the app.",
            condition = AchievementCondition.Threshold(target = 4) { it.visitedSections.size },
        ),
    )
}
