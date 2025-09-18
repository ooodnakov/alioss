package com.example.alias.data.pack

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PackParserTest {

    @Test
    fun parses_valid_json_pack() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "movies_en_v1",
                "name": "Movies (EN)",
                "language": "en",
                "version": 1,
                "isNSFW": false
              },
              "words": [
                { "text": "Director", "difficulty": 2, "category": "movies", "wordClass": "NOUN" },
                { "text": "Actor", "difficulty": 1, "wordClasses": ["noun", "person"] }
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)
        assertEquals("movies_en_v1", parsed.deck.id)
        assertEquals(2, parsed.words.size)
        assertEquals(2, parsed.wordClasses.size)
        assertEquals(setOf("NOUN"), parsed.wordClasses.map { it.wordClass }.toSet())
        assertEquals(null, parsed.deck.coverImageBase64)
    }

    @Test
    fun parses_cover_image_and_normalizes_data_uri() {
        val base64Image = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII="
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "cover_test",
                "name": "Cover Test",
                "language": "en",
                "version": 1,
                "coverImage": "$base64Image"
              },
              "words": [
                { "text": "One" }
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)

        val expected = Base64.getEncoder().encodeToString(
            Base64.getMimeDecoder().decode(base64Image.substringAfter(','))
        )
        assertEquals(expected, parsed.deck.coverImageBase64)
    }

    @Test
    fun rejects_invalid_difficulty() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {"id": "x", "name": "X", "language": "en", "version": 1},
              "words": [{"text": "Bad", "difficulty": 0}]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
    }

    @Test
    fun rejects_wrong_format() {
        val json = """
            {
              "format": "alias-deck@2",
              "deck": {"id": "x", "name": "X", "language": "en", "version": 1},
              "words": [{"text": "Ok", "difficulty": 1}]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
    }

    @Test
    fun rejects_invalid_word_class() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {"id": "x", "name": "X", "language": "en", "version": 1},
              "words": [{"text": "Ok", "difficulty": 1, "wordClass": "invalid"}]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
    }

    @Test
    fun rejects_invalid_cover_image() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "x",
                "name": "X",
                "language": "en",
                "version": 1,
                "coverImage": "!!!"
              },
              "words": [{"text": "Ok", "difficulty": 1}]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
    }

    @Test
    fun allows_missing_optional_word_metadata() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {"id": "x", "name": "X", "language": "en", "version": 1},
              "words": [
                {"text": "Solo"},
                {"text": "Duo", "difficulty": null, "category": ""},
                {"text": "Trio", "wordClass": ""}
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)

        assertEquals(listOf(1, 1, 1), parsed.words.map { it.difficulty })
        assertEquals(listOf(null, null, null), parsed.words.map { it.category })
        assertTrue(parsed.wordClasses.isEmpty())
    }
}

