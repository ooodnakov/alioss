package com.example.alias.data.pack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
                { "text": "Director", "difficulty": 2, "category": "movies" },
                { "text": "Actor", "difficulty": 1 }
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)
        assertEquals("movies_en_v1", parsed.deck.id)
        assertEquals(2, parsed.words.size)
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
}

