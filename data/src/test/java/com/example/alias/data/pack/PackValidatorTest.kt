package com.example.alias.data.pack

import com.example.alias.testing.fakePngBytes
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PackValidatorTest {

    @BeforeTest
    fun setUpDecoder() {
        PackValidator.imageMetadataDecoder = TestCoverImages.fakeDecoder
    }

    @AfterTest
    fun tearDownDecoder() {
        PackValidator.resetImageMetadataDecoder()
    }

    @Test
    fun `normalize language tag trims and lowercases`() {
        assertEquals("en-us", PackValidator.normalizeLanguageTag(" En-US "))
        assertEquals("mul", PackValidator.normalizeLanguageTag("mul"))
        assertFailsWith<IllegalArgumentException> { PackValidator.normalizeLanguageTag("bad tag!") }
    }

    @Test
    fun `normalize cover image url enforces https`() {
        assertNull(PackValidator.normalizeCoverImageUrl(null))
        assertNull(PackValidator.normalizeCoverImageUrl("   "))

        val normalized = PackValidator.normalizeCoverImageUrl(" https://example.com/images/cover.png ")
        assertEquals("https://example.com/images/cover.png", normalized)

        assertFailsWith<CoverImageException> { PackValidator.normalizeCoverImageUrl("ftp://example.com/image.png") }
        assertFailsWith<CoverImageException> {
            PackValidator.normalizeCoverImageUrl(
                "***example.com/image.png",
            )
        }
        assertFailsWith<CoverImageException> {
            PackValidator.normalizeCoverImageUrl(
                "https://user:pass@example.com/image.png",
            )
        }
        assertFailsWith<CoverImageException> { PackValidator.normalizeCoverImageUrl("/relative/path.png") }
    }

    @Test
    fun `validate deck normalizes embedded cover image`() {
        val normalized = PackValidator.validateDeck(
            id = "deck_1",
            language = "en",
            name = "Deck One",
            version = 1,
            isNSFW = false,
            coverImageBase64 = " data:image/png;base64,${TestCoverImages.TWO_BY_TWO_PNG_BASE64} ",
        )
        assertEquals(TestCoverImages.TWO_BY_TWO_PNG_BASE64, normalized)
        assertEquals("Deck Author", normalized.author)

        val blankAuthor = PackValidator.validateDeck(
            id = "deck_blank",
            language = "en",
            name = "Deck Blank",
            version = 1,
            isNSFW = false,
            coverImageBase64 = null,
            author = "   ",
        )
        assertNull(blankAuthor.author)

        assertNull(
            PackValidator.validateDeck(
                id = "deck_2",
                language = "en",
                name = "Deck Two",
                version = 1,
                isNSFW = false,
                coverImageBase64 = "   ",
                author = null,
            ).coverImageBase64,
        )

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateDeck(
                id = "bad id",
                language = "en",
                name = "Deck",
                version = 1,
                isNSFW = false,
                coverImageBase64 = null,
                author = null,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateDeck(
                id = "deck_3",
                language = "en",
                name = "Deck Three",
                version = 1,
                isNSFW = false,
                coverImageBase64 = null,
                author = "a".repeat(101),
            )
        }
    }

    @Test
    fun `validate cover image bytes accepts common formats`() {
        TestCoverImages.supportedFormatBytes.forEach { (label, bytes) ->
            val normalized = PackValidator.validateCoverImageBytes(bytes)
            assertEquals(TestCoverImages.base64Encoder.encodeToString(bytes), normalized, label)
        }
    }

    @Test
    fun `validate cover image bytes rejects invalid data`() {
        assertFailsWith<CoverImageException> { PackValidator.validateCoverImageBytes(ByteArray(0)) }

        assertFailsWith<CoverImageException> {
            PackValidator.validateCoverImageBytes(TestCoverImages.zeroDimensionPngBytes)
        }

        assertFailsWith<CoverImageException> {
            PackValidator.validateCoverImageBytes(ByteArray(1024) { 0x55 })
        }
    }

    @Test
    fun `validate cover image bytes allows oversized payloads`() {
        val oversized = TestCoverImages.oversizedImageBytes
        assertTrue(oversized.size > PackValidator.MAX_COVER_IMAGE_BYTES)

        val normalized = PackValidator.validateCoverImageBytes(oversized)
        assertEquals(TestCoverImages.base64Encoder.encodeToString(oversized), normalized)
    }

    @Test
    fun `validate cover image bytes accepts large png under limit`() {
        val largeBytes = fakePngBytes(width = 512, height = 512, totalSize = 3 * 1024 * 1024)
        assertTrue(largeBytes.size > 1_000_000)

        val normalized = PackValidator.validateCoverImageBytes(largeBytes)
        assertEquals(Base64.getEncoder().encodeToString(largeBytes), normalized)
    }

    @Test
    fun `validate word enforces language and metadata`() {
        PackValidator.validateWord(
            text = "Director",
            deckLanguage = "en",
            wordLanguage = "en",
            difficulty = 3,
            category = "movies",
            tabooStems = listOf("direct"),
            wordClass = "noun",
        )

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Hola",
                deckLanguage = "en",
                wordLanguage = "es",
                difficulty = 2,
                category = null,
                tabooStems = null,
                wordClass = null,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Bonjour",
                deckLanguage = PackValidator.MULTI_LANGUAGE_TAG,
                wordLanguage = null,
                difficulty = 2,
                category = null,
                tabooStems = null,
                wordClass = null,
            )
        }

        val longCategory = "a".repeat(65)
        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Overflow",
                deckLanguage = "en",
                wordLanguage = "en",
                difficulty = 2,
                category = longCategory,
                tabooStems = null,
                wordClass = null,
            )
        }

        val stems = List(11) { "stem$it" }
        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "TooMany",
                deckLanguage = "en",
                wordLanguage = "en",
                difficulty = 2,
                category = null,
                tabooStems = stems,
                wordClass = null,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Verb",
                deckLanguage = "en",
                wordLanguage = "en",
                difficulty = 2,
                category = null,
                tabooStems = null,
                wordClass = "invalid",
            )
        }
    }

    @Test
    fun `validate word count enforces bounds`() {
        PackValidator.validateWordCount(1)
        PackValidator.validateWordCount(200_000)
        assertFailsWith<IllegalArgumentException> { PackValidator.validateWordCount(0) }
        assertFailsWith<IllegalArgumentException> { PackValidator.validateWordCount(200_001) }
    }

    @Test
    fun `validate multi language content requires multiple languages`() {
        PackValidator.validateMultiLanguageContent(setOf("en", "ru"))
        assertFailsWith<IllegalArgumentException> { PackValidator.validateMultiLanguageContent(emptySet()) }
        assertFailsWith<IllegalArgumentException> { PackValidator.validateMultiLanguageContent(setOf("en")) }
    }

}
