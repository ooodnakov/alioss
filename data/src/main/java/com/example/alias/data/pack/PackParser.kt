package com.example.alias.data.pack

import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.WordClassEntity
import com.example.alias.data.db.WordEntity
import com.example.alias.domain.word.WordClassCatalog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Serializable representation of a deck pack file. */
@Serializable
private data class PackDto(
    val format: String,
    val deck: DeckDto,
    val words: List<WordDto>,
)

@Serializable
private data class DeckDto(
    val id: String,
    val name: String,
    val language: String,
    @SerialName("isNSFW") val isNsfw: Boolean = false,
    val version: Int = 1,
    val updatedAt: Long = 0L,
    val isOfficial: Boolean = false,
    @SerialName("coverImage") val coverImageBase64: String? = null,
)

@Serializable
private data class WordDto(
    val text: String,
    val difficulty: Int? = null,
    val language: String? = null,
    val category: String? = null,
    @SerialName("wordClass") val wordClass: String? = null,
    @SerialName("wordClasses") val legacyWordClasses: List<String>? = null,
    @SerialName("isNSFW") val isNsfw: Boolean = false,
    val tabooStems: List<String>? = null,
) {
    fun normalizedDifficulty(): Int = difficulty ?: DEFAULT_DIFFICULTY

    fun normalizedCategory(): String? {
        return category?.takeIf { it.isNotBlank() }
    }

    fun resolvedWordClass(): String? {
        val normalized = wordClass?.let { WordClassCatalog.normalizeOrNull(it) }
        if (normalized != null) {
            return normalized
        }
        if (wordClass != null && wordClass.isNotBlank()) {
            throw IllegalArgumentException("Unsupported word class: $wordClass")
        }
        return legacyWordClasses
            ?.asSequence()
            ?.mapNotNull { WordClassCatalog.normalizeOrNull(it) }
            ?.firstOrNull()
    }

    companion object {
        private const val DEFAULT_DIFFICULTY = 1
    }
}

/** Result of parsing a pack: a [DeckEntity] and its [WordEntity] list. */
data class ParsedPack(
    val deck: DeckEntity,
    val words: List<WordEntity>,
    val wordClasses: List<WordClassEntity>,
)

/** Simple JSON pack parser based on kotlinx.serialization. */
object PackParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJson(content: String, isBundledAsset: Boolean = false): ParsedPack {
        val dto = json.decodeFromString<PackDto>(content)
        // Basic input validation to avoid malformed or oversized packs.
        PackValidator.validateFormat(dto.format)
        val normalizedDeckLanguage = PackValidator.normalizeLanguageTag(dto.deck.language)
        val coverImageBase64 = PackValidator.validateDeck(
            id = dto.deck.id,
            language = normalizedDeckLanguage,
            name = dto.deck.name,
            version = dto.deck.version,
            isNSFW = dto.deck.isNsfw,
            coverImageBase64 = dto.deck.coverImageBase64,
        )
        PackValidator.validateWordCount(dto.words.size)
        val deckEntity = DeckEntity(
            id = dto.deck.id,
            name = dto.deck.name,
            language = normalizedDeckLanguage,
            isOfficial = isBundledAsset || dto.deck.isOfficial,
            isNSFW = dto.deck.isNsfw,
            version = dto.deck.version,
            updatedAt = dto.deck.updatedAt,
            coverImageBase64 = coverImageBase64,
        )
        val wordEntities = mutableListOf<WordEntity>()
        val classEntities = mutableListOf<WordClassEntity>()
        val languagesEncountered = mutableSetOf<String>()
        dto.words.forEach { word ->
            val difficulty = word.normalizedDifficulty()
            val category = word.normalizedCategory()
            val normalizedClass = word.resolvedWordClass()
            val normalizedWordLanguage = word.language?.let { PackValidator.normalizeLanguageTag(it) }
            PackValidator.validateWord(
                text = word.text,
                deckLanguage = normalizedDeckLanguage,
                wordLanguage = normalizedWordLanguage,
                difficulty = difficulty,
                category = category,
                tabooStems = word.tabooStems,
                wordClass = normalizedClass,
            )
            val storedLanguage = normalizedWordLanguage ?: normalizedDeckLanguage
            if (normalizedDeckLanguage == PackValidator.MULTI_LANGUAGE_TAG) {
                languagesEncountered += storedLanguage
            }
            wordEntities += WordEntity(
                deckId = dto.deck.id,
                text = word.text,
                language = storedLanguage,
                stems = null,
                category = category,
                difficulty = difficulty,
                tabooStems = word.tabooStems?.joinToString(";"),
                isNSFW = word.isNsfw,
            )
            normalizedClass?.let { cls ->
                classEntities += WordClassEntity(
                    deckId = dto.deck.id,
                    wordText = word.text,
                    wordClass = cls,
                )
            }
        }
        if (normalizedDeckLanguage == PackValidator.MULTI_LANGUAGE_TAG) {
            PackValidator.validateMultiLanguageContent(languagesEncountered)
        }
        return ParsedPack(deckEntity, wordEntities, classEntities)
    }
}
