package com.example.alias.data.pack

import com.example.alias.domain.word.WordClassCatalog

/**
 * Validates pack metadata and words to enforce basic constraints.
 */
object PackValidator {
    private const val MAX_WORDS = 200_000
    private val ID_REGEX = Regex("^[a-z0-9_-]{1,64}$")
    private val LANG_REGEX = Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$")

    fun validateFormat(format: String) {
        require(format == "alias-deck@1") { "Unsupported pack format: $format" }
    }

    fun validateDeck(id: String, language: String, name: String, version: Int, isNSFW: Boolean) {
        require(ID_REGEX.matches(id)) { "Invalid deck id" }
        require(name.isNotBlank() && name.length <= 100) { "Invalid deck name" }
        require(LANG_REGEX.matches(language)) { "Invalid language tag" }
        require(version >= 1) { "Invalid version" }
        // isNSFW: no constraint (boolean)
    }

    fun validateWordCount(count: Int) {
        require(count in 1..MAX_WORDS) { "Invalid word count: $count" }
    }

    fun validateWord(
        text: String,
        difficulty: Int,
        category: String?,
        tabooStems: List<String>?,
        wordClass: String?,
    ) {
        require(text.isNotBlank() && text.trim().length <= 120) { "Invalid word text" }
        require(difficulty in 1..5) { "Invalid difficulty: $difficulty" }
        if (category != null) {
            require(category.trim().length <= 64) { "Invalid category length" }
        }
        if (tabooStems != null) {
            require(tabooStems.size <= 10) { "Too many taboo stems" }
            tabooStems.forEach { stem ->
                require(stem.isNotBlank() && stem.length <= 32) { "Invalid taboo stem" }
            }
        }
        if (wordClass != null) {
            val normalized = WordClassCatalog.normalizeOrNull(wordClass)
            require(normalized != null) { "Unsupported word class: $wordClass" }
        }
    }
}
