package com.example.alias

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** Normalizes locale preference strings and applies them via [AppCompatDelegate]. */
fun canonicalizeLocalePreference(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.equals("system", ignoreCase = true) || trimmed.isEmpty()) {
        return "system"
    }
    val locales = LocaleListCompat.forLanguageTags(trimmed)
    if (locales.isEmpty) {
        return "system"
    }
    return locales.toLanguageTags()
}

fun applyLocalePreference(tag: String) {
    val normalized = canonicalizeLocalePreference(tag)
    val locales =
            if (normalized == "system") {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(normalized)
            }
    val current = AppCompatDelegate.getApplicationLocales()
    if (current != locales) {
        AppCompatDelegate.setApplicationLocales(locales)
    }
    val effectiveList =
            if (locales.isEmpty) {
                LocaleListCompat.getAdjustedDefault()
            } else {
                locales
            }
    effectiveList.firstOrNull()?.let(Locale::setDefault)
}

fun resolveInitialLocalePreference(stored: String): String {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val defaultLocales = LocaleListCompat.getAdjustedDefault()
    val defaultTag = canonicalizeLocalePreference(defaultLocales.toLanguageTags())
    val canonicalAppTag = canonicalizeLocalePreference(appLocales.toLanguageTags())
    val appTag =
            when {
                appLocales.isEmpty -> "system"
                canonicalAppTag == defaultTag -> "system"
                else -> canonicalAppTag
            }
    return if (appTag != "system") appTag else stored
}

private fun LocaleListCompat.firstOrNull(): Locale? = if (size() > 0) get(0) else null
