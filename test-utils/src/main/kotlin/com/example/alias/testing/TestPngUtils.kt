package com.example.alias.testing

private const val CHUNK_LENGTH_BYTES = Int.SIZE_BYTES
private const val CHUNK_TYPE_BYTES = 4
private const val CHUNK_CRC_BYTES = Int.SIZE_BYTES
private const val PNG_SIGNATURE_BYTES = 8

private const val IHDR_CHUNK_LENGTH = 13
private const val IHDR_CHUNK_TOTAL_BYTES = CHUNK_LENGTH_BYTES + CHUNK_TYPE_BYTES + IHDR_CHUNK_LENGTH + CHUNK_CRC_BYTES
private const val IHDR_LENGTH_OFFSET = PNG_SIGNATURE_BYTES
private const val IHDR_TYPE_OFFSET = IHDR_LENGTH_OFFSET + CHUNK_LENGTH_BYTES
private const val IHDR_DATA_OFFSET = IHDR_TYPE_OFFSET + CHUNK_TYPE_BYTES
private const val IHDR_WIDTH_OFFSET = IHDR_DATA_OFFSET
private const val IHDR_HEIGHT_OFFSET = IHDR_WIDTH_OFFSET + Int.SIZE_BYTES
private const val IHDR_BIT_DEPTH_OFFSET = IHDR_HEIGHT_OFFSET + Int.SIZE_BYTES
private const val IHDR_COLOR_TYPE_OFFSET = IHDR_BIT_DEPTH_OFFSET + 1
private const val IHDR_COMPRESSION_OFFSET = IHDR_COLOR_TYPE_OFFSET + 1
private const val IHDR_FILTER_OFFSET = IHDR_COMPRESSION_OFFSET + 1
private const val IHDR_INTERLACE_OFFSET = IHDR_FILTER_OFFSET + 1

private const val IEND_CHUNK_TOTAL_BYTES = CHUNK_LENGTH_BYTES + CHUNK_TYPE_BYTES + CHUNK_CRC_BYTES
private const val MIN_PNG_SIZE = PNG_SIGNATURE_BYTES + IHDR_CHUNK_TOTAL_BYTES + IEND_CHUNK_TOTAL_BYTES

private const val BIT_DEPTH_8 = 8
private const val COLOR_TYPE_RGB = 2
private const val NO_COMPRESSION = 0
private const val NO_FILTER = 0
private const val NO_INTERLACE = 0

private val PNG_SIGNATURE = byteArrayOf(
    137.toByte(),
    80,
    78,
    71,
    13,
    10,
    26,
    10,
)

private val IHDR_CHUNK_TYPE = byteArrayOf('I'.code.toByte(), 'H'.code.toByte(), 'D'.code.toByte(), 'R'.code.toByte())
private val IEND_CHUNK_TYPE = byteArrayOf('I'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'D'.code.toByte())

/**
 * Generates a fake PNG payload with the provided [width], [height], and [totalSize].
 *
 * The image data is intentionally empty, but the PNG signature, IHDR chunk, and IEND chunk
 * are populated so that downstream validators treat the payload as structurally valid.
 */
fun fakePngBytes(width: Int, height: Int, totalSize: Int): ByteArray {
    require(totalSize >= MIN_PNG_SIZE) {
        "PNG must be large enough to hold the signature, IHDR, and IEND chunks"
    }
    val bytes = ByteArray(totalSize)
    PNG_SIGNATURE.copyInto(bytes)
    writeInt(bytes, IHDR_LENGTH_OFFSET, IHDR_CHUNK_LENGTH)
    IHDR_CHUNK_TYPE.copyInto(bytes, IHDR_TYPE_OFFSET)
    writeInt(bytes, IHDR_WIDTH_OFFSET, width)
    writeInt(bytes, IHDR_HEIGHT_OFFSET, height)
    bytes[IHDR_BIT_DEPTH_OFFSET] = BIT_DEPTH_8.toByte()
    bytes[IHDR_COLOR_TYPE_OFFSET] = COLOR_TYPE_RGB.toByte()
    bytes[IHDR_COMPRESSION_OFFSET] = NO_COMPRESSION.toByte()
    bytes[IHDR_FILTER_OFFSET] = NO_FILTER.toByte()
    bytes[IHDR_INTERLACE_OFFSET] = NO_INTERLACE.toByte()

    val iendOffset = totalSize - IEND_CHUNK_TOTAL_BYTES
    writeInt(bytes, iendOffset, 0)
    IEND_CHUNK_TYPE.copyInto(bytes, iendOffset + CHUNK_LENGTH_BYTES)
    return bytes
}

private fun writeInt(target: ByteArray, offset: Int, value: Int) {
    target[offset] = ((value shr 24) and 0xFF).toByte()
    target[offset + 1] = ((value shr 16) and 0xFF).toByte()
    target[offset + 2] = ((value shr 8) and 0xFF).toByte()
    target[offset + 3] = (value and 0xFF).toByte()
}
