package com.example.alias

import com.example.alias.data.settings.Settings

/**
 * Data transfer object bundling all inputs required to persist the settings update.
 */
data class SettingsUpdateRequest(
    val roundSeconds: Int,
    val targetWords: Int,
    val targetScore: Int,
    val scoreTargetEnabled: Boolean,
    val maxSkips: Int,
    val penaltyPerSkip: Int,
    val punishSkips: Boolean,
    val uiLanguage: String,
    val allowNSFW: Boolean,
    val haptics: Boolean,
    val sound: Boolean,
    val oneHanded: Boolean,
    val verticalSwipes: Boolean,
    val orientation: String,
    val teams: List<String>,
) {
    companion object {
        fun from(settings: Settings): SettingsUpdateRequest =
            SettingsUpdateRequest(
                roundSeconds = settings.roundSeconds,
                targetWords = settings.targetWords,
                targetScore = settings.targetScore,
                scoreTargetEnabled = settings.scoreTargetEnabled,
                maxSkips = settings.maxSkips,
                penaltyPerSkip = settings.penaltyPerSkip,
                punishSkips = settings.punishSkips,
                uiLanguage = settings.uiLanguage,
                allowNSFW = settings.allowNSFW,
                haptics = settings.hapticsEnabled,
                sound = settings.soundEnabled,
                oneHanded = settings.oneHandedLayout,
                verticalSwipes = settings.verticalSwipes,
                orientation = settings.orientation,
                teams = settings.teams.toList(),
            )
    }
}
