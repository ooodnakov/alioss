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
    val words: List<WordDto>
)

@Serializable
private data class DeckDto(
    val id: String,
    val name: String,
    val language: String,
    @SerialName("isNSFW") val isNsfw: Boolean = false,
    val version: Int = 1,
    val updatedAt: Long = 0L,
    val isOfficial: Boolean = false
)

@Serializable
private data class WordDto(
    val text: String,
    val difficulty: Int? = null,
    val category: String? = null,
    @SerialName("wordClass") val wordClass: String? = null,
    @SerialName("wordClasses") val legacyWordClasses: List<String>? = null,
    @SerialName("isNSFW") val isNsfw: Boolean = false,
    val tabooStems: List<String>? = null
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

    fun fromJson(content: String): ParsedPack {
        val dto = json.decodeFromString<PackDto>(content)
        // Basic input validation to avoid malformed or oversized packs.
        PackValidator.validateFormat(dto.format)
        PackValidator.validateDeck(
            id = dto.deck.id,
            language = dto.deck.language,
            name = dto.deck.name,
            version = dto.deck.version,
            isNSFW = dto.deck.isNsfw
        )
        PackValidator.validateWordCount(dto.words.size)
        val deckEntity = DeckEntity(
            id = dto.deck.id,
            name = dto.deck.name,
            language = dto.deck.language,
            isOfficial = dto.deck.isOfficial,
            isNSFW = dto.deck.isNsfw,
            version = dto.deck.version,
            updatedAt = dto.deck.updatedAt
        )
        val wordEntities = mutableListOf<WordEntity>()
        val classEntities = mutableListOf<WordClassEntity>()
        dto.words.forEach { word ->
            val normalizedClass = word.resolvedWordClass()
            PackValidator.validateWord(
                text = word.text,
                difficulty = word.difficulty,
                category = word.category,
                tabooStems = word.tabooStems,
                wordClass = normalizedClass
            )
            wordEntities += WordEntity(
                deckId = dto.deck.id,
                text = word.text,
                language = dto.deck.language,
                stems = null,
                category = word.category,
                difficulty = word.difficulty,
                tabooStems = word.tabooStems?.joinToString(";"),
                isNSFW = word.isNsfw
            )
            normalizedClass?.let { cls ->
                classEntities += WordClassEntity(
                    deckId = dto.deck.id,
                    wordText = word.text,
                    wordClass = cls
                )
            }
        }
        return ParsedPack(deckEntity, wordEntities, classEntities)
    }
}
