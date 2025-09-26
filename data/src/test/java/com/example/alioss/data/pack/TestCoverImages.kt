@file:Suppress("MaxLineLength")

package com.example.alioss.data.pack

import java.nio.ByteBuffer
import java.util.Base64

internal object TestCoverImages {
    private val base64Decoder = Base64.getMimeDecoder()
    val base64Encoder: Base64.Encoder = Base64.getEncoder()

    const val TWO_BY_TWO_PNG_BASE64 =
        "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFUlEQVR4nAXBAQEAAACAEP9PF0JQGR7vBPykAXTzAAAAAElFTkSuQmCC"

    const val TWO_BY_TWO_JPEG_BASE64 =
        "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAACAAIDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD0LwrY2kvhDRJJLWB3ewgZmaMEkmNckmiiivynF/7xU9X+Z89X/iy9X+Z//9k="

    const val TWO_BY_TWO_WEBP_BASE64 =
        "UklGRkYAAABXRUJQVlA4IDoAAADQAQCdASoCAAIAAUAmJZgCdAEO+KkLAAD+68fy0Z8T/GL/+rQ3mHY2QAs0MmFD/4tD+3n/BrbSbQAA"

    const val TWO_BY_TWO_GIF_BASE64 =
        "R0lGODdhAgACAIEAAP//AAD/AP8AAAAA/ywAAAAAAgACAAAIBwAFBBgAICAAOw=="

    const val TWO_BY_TWO_BMP_BASE64 =
        "Qk1GAAAAAAAAADYAAAAoAAAAAgAAAAIAAAABABgAAAAAABAAAADEDgAAxA4AAAAAAAAAAAAA/wAAAP//AAAAAP8A/wAAAA=="

    const val TWO_BY_TWO_HEIF_BASE64 =
        "AAAAHGZ0eXBoZWljAAAAAG1pZjFoZWljbWlhZgAAAWltZXRhAAAAAAAAACFoZGxyAAAAAAAAAABwaWN0AAAAAAAAAAAAAAAAAAAAAA5waXRtAAAAAAABAAAAImlsb2MAAAAAREAAAQABAAAAAAGNAAEAAAAAAAAAQwAAACNpaW5mAAAAAAABAAAAFWluZmUCAAAAAAEAAGh2YzEAAAAA6WlwcnAAAADKaXBjbwAAAHZodmNDAQNwAAAAAAAAAAAAHvAA/P34+AAADwNgAAEAGEABDAH//wNwAAADAJAAAAMAAAMAHroCQGEAAQAqQgEBA3AAAAMAkAAAAwAAAwAeoCCBBZbqrprm4CGgwIAAAAMAgAAAAwCEYgABAAZEAcFzwYkAAAAUaXNwZQAAAAAAAABAAAAAQAAAAChjbGFwAAAAAgAAAAEAAAACAAAAAf///8IAAAAC////wgAAAAIAAAAQcGl4aQAAAAADCAgIAAAAF2lwbWEAAAAAAAAAAQABBIECBIMAAABLbWRhdAAAAD8oAa8TIaEgWFgcligACenbs6jP+j371qyPYrLT4+prx1AgKJjmas132RAywb9/bj6IjIDo24ElY9W1iDdVMlg="

    val twoByTwoPngBytes: ByteArray = base64Decoder.decode(TWO_BY_TWO_PNG_BASE64)
    val twoByTwoJpegBytes: ByteArray = base64Decoder.decode(TWO_BY_TWO_JPEG_BASE64)
    val twoByTwoWebpBytes: ByteArray = base64Decoder.decode(TWO_BY_TWO_WEBP_BASE64)
    val twoByTwoGifBytes: ByteArray = base64Decoder.decode(TWO_BY_TWO_GIF_BASE64)
    val twoByTwoBmpBytes: ByteArray = base64Decoder.decode(TWO_BY_TWO_BMP_BASE64)
    val twoByTwoHeifBytes: ByteArray = base64Decoder.decode(TWO_BY_TWO_HEIF_BASE64)

    val zeroDimensionPngBytes: ByteArray = twoByTwoPngBytes.copyOf().apply {
        this[16] = 0
        this[17] = 0
        this[18] = 0
        this[19] = 0
    }

    val oversizedImageBytes: ByteArray = ByteArray(
        (PackValidator.MAX_COVER_IMAGE_BYTES + 32).toInt(),
    ) { 0x42 }

    val supportedFormatBytes: Map<String, ByteArray> = mapOf(
        "png" to twoByTwoPngBytes,
        "jpeg" to twoByTwoJpegBytes,
        "webp" to twoByTwoWebpBytes,
        "gif" to twoByTwoGifBytes,
        "bmp" to twoByTwoBmpBytes,
        "heif" to twoByTwoHeifBytes,
    )

    private val pngSignature = byteArrayOf(
        0x89.toByte(),
        'P'.code.toByte(),
        'N'.code.toByte(),
        'G'.code.toByte(),
        0x0D,
        0x0A,
        0x1A,
        0x0A,
    )

    private fun ByteArray.hasPrefix(prefix: ByteArray): Boolean {
        if (size < prefix.size) {
            return false
        }
        return prefix.indices.all { this[it] == prefix[it] }
    }

    private fun ByteArray.decodeBigEndianInt(offset: Int): Int {
        return ByteBuffer.wrap(this, offset, Int.SIZE_BYTES).int
    }

    val fakeDecoder: PackValidator.ImageMetadataDecoder = object : PackValidator.ImageMetadataDecoder {
        override fun decodeSize(data: ByteArray): Pair<Int, Int>? {
            return when {
                data.contentEquals(oversizedImageBytes) -> 4_096 to 4_096
                data.contentEquals(zeroDimensionPngBytes) -> 0 to 0
                data.hasPrefix(pngSignature) -> data.decodeBigEndianInt(16) to data.decodeBigEndianInt(20)
                supportedFormatBytes.values.any { it.contentEquals(data) } -> 2 to 2
                else -> null
            }
        }
    }
}
