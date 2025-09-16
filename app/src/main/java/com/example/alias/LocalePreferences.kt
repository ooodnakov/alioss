package com.example.alias

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Normalizes locale preference strings and applies them via [AppCompatDelegate].
 */
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
    val locales = if (normalized == "system") {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(normalized)
    }
    val current = AppCompatDelegate.getApplicationLocales()
    if (current != locales) {
        AppCompatDelegate.setApplicationLocales(locales)
    }
    val effectiveList = if (locales.isEmpty) {
        LocaleListCompat.getAdjustedDefault()
    } else {
        locales
    }
    effectiveList.firstOrNull()?.let(Locale::setDefault)
}

private fun LocaleListCompat.firstOrNull(): Locale? = if (size() > 0) get(0) else null
