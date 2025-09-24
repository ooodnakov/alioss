package com.example.alias.data.pack

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PackParserTest {

    @BeforeTest
    fun setUpDecoder() {
        PackValidator.imageMetadataDecoder = TestCoverImages.fakeDecoder
    }

    @AfterTest
    fun tearDownDecoder() {
        PackValidator.resetImageMetadataDecoder()
    }

    @Test
    fun parses_valid_json_pack() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "movies_en_v1",
                "name": "Movies (EN)",
                "language": "en",
                "author": "Movie Buffs",
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
        assertEquals("Movie Buffs", parsed.deck.author)
        assertEquals(2, parsed.words.size)
        assertEquals(2, parsed.wordClasses.size)
        assertEquals(setOf("NOUN"), parsed.wordClasses.map { it.wordClass }.toSet())
        assertEquals(null, parsed.deck.coverImageBase64)
    }

    @Test
    fun parses_cover_image_and_normalizes_data_uri() {
        val base64Image = "data:image/png;base64," + TestCoverImages.TWO_BY_TWO_PNG_BASE64
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "cover_test",
                "name": "Cover Test",
                "language": "en",
                "author": "Cover Test",
                "version": 1,
                "coverImage": "$base64Image"
              },
              "words": [
                { "text": "One" }
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)

        val expected = TestCoverImages.base64Encoder.encodeToString(TestCoverImages.twoByTwoPngBytes)
        assertEquals(expected, parsed.deck.coverImageBase64)
        assertEquals("Cover Test", parsed.deck.author)
        assertNull(parsed.coverImageUrl)
    }

    @Test
    fun trims_author_and_allows_absence() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "cover_url",
                "name": "Cover URL",
                "language": "en",
                "version": 1,
                "author": "  Jane Doe  "
              },
              "words": [
                { "text": "One" }
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)

        assertEquals("Jane Doe", parsed.deck.author)
        assertNull(parsed.deck.coverImageBase64)

        val withoutAuthor = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "no_author",
                "name": "No Author",
                "language": "en",
                "version": 1
              },
              "words": [
                { "text": "One" }
              ]
            }
        """.trimIndent()

        val parsedWithoutAuthor = PackParser.fromJson(withoutAuthor)

        assertNull(parsedWithoutAuthor.deck.author)
    }

    @Test
    fun parses_cover_image_url() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "cover_url",
                "name": "Cover URL",
                "language": "en",
                "version": 1,
                "coverImageUrl": "https://example.com/cover.png"
              },
              "words": [
                { "text": "One" }
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)

        assertEquals("https://example.com/cover.png", parsed.coverImageUrl)
        assertEquals(null, parsed.deck.coverImageBase64)
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
    fun rejects_cover_image_with_both_inline_and_url() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "x",
                "name": "X",
                "language": "en",
                "version": 1,
                "coverImage": "data:image/png;base64,aGVsbG8=",
                "coverImageUrl": "https://example.com/cover.png"
              },
              "words": [{"text": "Ok", "difficulty": 1}]
            }
        """.trimIndent()

        val error = assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
        assertTrue(error.message?.contains("Cover image") == true)
    }

    @Test
    fun rejects_cover_image_url_when_not_https() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "x",
                "name": "X",
                "language": "en",
                "version": 1,
                "coverImageUrl": "http://example.com/cover.png"
              },
              "words": [{"text": "Ok", "difficulty": 1}]
            }
        """.trimIndent()

        val error = assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
        assertTrue(error.message?.contains("Cover image") == true)
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

    @Test
    fun parses_multi_language_pack_with_per_word_languages() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {"id": "multi", "name": "Multi", "language": "mul", "version": 1},
              "words": [
                {"text": "Hello", "language": "en"},
                {"text": "Privet", "language": "ru"}
              ]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)

        assertEquals(setOf("en", "ru"), parsed.words.map { it.language }.toSet())
        assertEquals("mul", parsed.deck.language)
    }

    @Test
    fun rejects_multi_language_pack_without_word_languages() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {"id": "multi", "name": "Multi", "language": "mul", "version": 1},
              "words": [
                {"text": "Hello"}
              ]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
    }

    @Test
    fun rejects_word_language_mismatch_for_single_language_pack() {
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {"id": "x", "name": "X", "language": "en", "version": 1},
              "words": [
                {"text": "Hola", "language": "es"}
              ]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> { PackParser.fromJson(json) }
    }

    @Test
    fun allows_cover_image_with_large_dimensions() {
        val encoded = TestCoverImages.base64Encoder.encodeToString(TestCoverImages.oversizedImageBytes)
        val json = """
            {
              "format": "alias-deck@1",
              "deck": {
                "id": "x",
                "name": "X",
                "language": "en",
                "version": 1,
                "coverImage": "$encoded"
              },
              "words": [{"text": "Ok", "difficulty": 1}]
            }
        """.trimIndent()

        val parsed = PackParser.fromJson(json)
        assertEquals(encoded, parsed.deck.coverImageBase64)
    }
}
