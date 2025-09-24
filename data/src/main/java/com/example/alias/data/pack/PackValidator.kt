package com.example.alias.data.pack

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.example.alias.domain.word.WordClassCatalog
import java.nio.ByteBuffer
import java.util.Base64
import java.util.Locale

/**
 * Validates pack metadata and words to enforce basic constraints.
 */
object PackValidator {
    const val MULTI_LANGUAGE_TAG = "mul"
    private const val MAX_WORDS = 200_000
    internal const val MAX_COVER_IMAGE_BYTES = 40L * 1024 * 1024
    private const val MAX_COVER_IMAGE_DIMENSION = 2_048
    private const val MAX_COVER_IMAGE_URL_LENGTH = 2_048
    private const val MAX_AUTHOR_LENGTH = 100
    private val ID_REGEX = Regex("^[a-z0-9_-]{1,64}$")
    private val LANG_REGEX = Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$")
    private val base64Encoder = Base64.getEncoder()
    private val base64MimeDecoder = Base64.getMimeDecoder()

    data class DeckValidationResult(
        val coverImageBase64: String?,
        val author: String?,
    )

    fun validateFormat(format: String) {
        require(format == "alias-deck@1") { "Unsupported pack format: $format" }
    }

    fun validateDeck(
        id: String,
        language: String,
        name: String,
        version: Int,
        @Suppress("UNUSED_PARAMETER") isNSFW: Boolean,
        coverImageBase64: String?,
        author: String?,
    ): DeckValidationResult {
        require(ID_REGEX.matches(id)) { "Invalid deck id" }
        require(name.isNotBlank() && name.length <= 100) { "Invalid deck name" }
        require(LANG_REGEX.matches(language)) { "Invalid language tag" }
        require(version >= 1) { "Invalid version" }
        // isNSFW: no constraint (boolean)
        val normalizedAuthor = author?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedAuthor != null) {
            require(normalizedAuthor.length <= MAX_AUTHOR_LENGTH) { "Invalid deck author" }
        }
        return DeckValidationResult(
            coverImageBase64 = normalizeCoverImage(coverImageBase64),
            author = normalizedAuthor,
        )
    }

    fun normalizeCoverImageUrl(coverImageUrl: String?): String? {
        if (coverImageUrl == null) {
            return null
        }
        val trimmed = coverImageUrl.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        if (trimmed.length > MAX_COVER_IMAGE_URL_LENGTH) {
            throw CoverImageException("Cover image URL too long")
        }
        val uri = try {
            java.net.URI(trimmed)
        } catch (error: java.net.URISyntaxException) {
            throw CoverImageException("Invalid cover image URL", error)
        }
        if (!uri.isAbsolute) {
            throw CoverImageException("Cover image URL must be absolute")
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw CoverImageException("Cover image URL must use HTTPS")
        }
        if (uri.host.isNullOrBlank()) {
            throw CoverImageException("Cover image URL missing host")
        }
        if (uri.userInfo != null) {
            throw CoverImageException("Cover image URL must not contain credentials")
        }
        return uri.toString()
    }

    fun normalizeLanguageTag(language: String): String {
        val trimmed = language.trim()
        require(LANG_REGEX.matches(trimmed)) { "Invalid language tag" }
        return trimmed.lowercase(Locale.ROOT)
    }

    private fun normalizeCoverImage(coverImageBase64: String?): String? {
        if (coverImageBase64 == null) {
            return null
        }
        val trimmed = coverImageBase64.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val dataPortion = trimmed.substringAfter(',', trimmed)
        val decoded = try {
            base64MimeDecoder.decode(dataPortion)
        } catch (error: IllegalArgumentException) {
            throw CoverImageException("Invalid cover image encoding", error)
        }
        return validateCoverImageBytes(decoded)
    }

    fun validateCoverImageBytes(data: ByteArray): String {
        if (data.isEmpty()) {
            throw CoverImageException("Cover image is empty")
        }
        val (width, height) = decodeImageSize(data)
            ?: throw CoverImageException("Cover image has invalid data")
        if (width <= 0 || height <= 0) {
            throw CoverImageException("Cover image has invalid dimensions")
        }
        return base64Encoder.encodeToString(data)
    }

    internal fun resetImageMetadataDecoder() {
        imageMetadataDecoder = AndroidImageMetadataDecoder
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmStatic
    fun overrideImageMetadataDecoderForTesting(
        decoder: (ByteArray) -> Pair<Int, Int>?,
    ) {
        imageMetadataDecoder = object : ImageMetadataDecoder {
            override fun decodeSize(data: ByteArray): Pair<Int, Int>? = decoder(data)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmStatic
    fun resetImageMetadataDecoderForTesting() {
        resetImageMetadataDecoder()
    }

    private fun decodeImageSize(data: ByteArray): Pair<Int, Int>? {
        return imageMetadataDecoder.decodeSize(data)
    }

    internal interface ImageMetadataDecoder {
        fun decodeSize(data: ByteArray): Pair<Int, Int>?
    }

    internal var imageMetadataDecoder: ImageMetadataDecoder = AndroidImageMetadataDecoder

    private object AndroidImageMetadataDecoder : ImageMetadataDecoder {
        override fun decodeSize(data: ByteArray): Pair<Int, Int>? {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            val width = options.outWidth
            val height = options.outHeight
            if (width > 0 && height > 0) {
                return width to height
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return ImageDecoderDelegate.decodeSize(data)
            }
            return null
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private object ImageDecoderDelegate {
            fun decodeSize(data: ByteArray): Pair<Int, Int>? {
                return try {
                    val source = ImageDecoder.createSource(ByteBuffer.wrap(data))
                    ImageDecoder.decodeDrawable(source) { _, info, _ ->
                        throw ImageSizeCaptured(info.size.width, info.size.height)
                    }
                    null
                } catch (captured: ImageSizeCaptured) {
                    captured.width to captured.height
                } catch (_: Exception) {
                    null
                }
            }

            private class ImageSizeCaptured(
                val width: Int,
                val height: Int,
            ) : RuntimeException(null, null, false, false)
        }
    }

    fun validateWordCount(count: Int) {
        require(count in 1..MAX_WORDS) { "Invalid word count: $count" }
    }

    fun validateWord(
        text: String,
        deckLanguage: String,
        wordLanguage: String?,
        difficulty: Int,
        category: String?,
        tabooStems: List<String>?,
        wordClass: String?,
    ) {
        require(text.isNotBlank() && text.trim().length <= 120) { "Invalid word text" }
        require(difficulty in 1..5) { "Invalid difficulty: $difficulty" }
        if (deckLanguage == MULTI_LANGUAGE_TAG) {
            require(!wordLanguage.isNullOrBlank()) { "Multi-language decks require per-word language" }
        } else if (wordLanguage != null) {
            require(wordLanguage == deckLanguage) { "Word language must match deck language" }
        }
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

    fun validateMultiLanguageContent(languages: Set<String>) {
        require(languages.isNotEmpty()) { "Multi-language deck must include languages" }
        require(languages.size >= 2) { "Multi-language deck must include at least two languages" }
    }
}
