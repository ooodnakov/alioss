package com.example.alias.data.pack

import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.WordEntity
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
    val difficulty: Int = 1,
    val category: String? = null,
    @SerialName("isNSFW") val isNsfw: Boolean = false,
    val tabooStems: List<String>? = null
)

/** Result of parsing a pack: a [DeckEntity] and its [WordEntity] list. */
data class ParsedPack(val deck: DeckEntity, val words: List<WordEntity>)

/** Simple JSON pack parser based on kotlinx.serialization. */
object PackParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJson(content: String): ParsedPack {
        val dto = json.decodeFromString<PackDto>(content)
        val deckEntity = DeckEntity(
            id = dto.deck.id,
            name = dto.deck.name,
            language = dto.deck.language,
            isOfficial = dto.deck.isOfficial,
            isNSFW = dto.deck.isNsfw,
            version = dto.deck.version,
            updatedAt = dto.deck.updatedAt
        )
        val wordEntities = dto.words.map { word ->
            WordEntity(
                deckId = dto.deck.id,
                text = word.text,
                language = dto.deck.language,
                stems = null,
                category = word.category,
                difficulty = word.difficulty,
                tabooStems = word.tabooStems?.joinToString(";"),
                isNSFW = word.isNsfw
            )
        }
        return ParsedPack(deckEntity, wordEntities)
    }
}
