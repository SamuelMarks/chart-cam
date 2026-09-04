/**
 * @file ImageMetadataParser.kt
 * Contains declarations for ImageMetadataParser.kt.
 *
 * Provides safe parsing of image metadata and EXIF headers, resilient against
 * malformed headers, truncated streams, and corrupted binary blobs.
 */
package io.healthplatform.chartcam.files

/**
 * Parsed image metadata information.
 *
 * @property width The image width in pixels, if found.
 * @property height The image height in pixels, if found.
 * @property orientation The EXIF orientation tag, if present (1-8).
 * @property mimeType The detected MIME type of the image.
 * @property isCorrupted True if the binary header was malformed or incomplete.
 */
data class ImageMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val orientation: Int? = null,
    val mimeType: String = "image/jpeg",
    val isCorrupted: Boolean = false,
)

/**
 * Constants used in image header and EXIF parsing.
 */
private object HeaderMarkers {
    const val MIN_HEADER_LEN = 4
    const val PNG_HEADER_LEN = 8
    const val PNG_BYTE_0 = 0x89
    const val MASK_BYTE = 0xFF
    const val MARKER_PREFIX = 0xFF
    const val MARKER_SOI = 0xD8
    const val MARKER_APP1 = 0xE1
    const val MARKER_SOF0 = 0xC0
    const val MARKER_EOI = 0xD9
    const val MARKER_SOS = 0xDA
    const val SHIFT_8 = 8
    const val EXIF_MIN_LEN = 8
    const val SOF_MIN_LEN = 7
    const val DEFAULT_ORIENTATION = 1
    const val OFFSET_HEIGHT = 3
    const val OFFSET_WIDTH = 5
    const val MIN_SEGMENT_LEN = 2
    const val INDEX_3 = 3
}

/**
 * Parsed segment header descriptor.
 */
private data class SegmentInfo(
    val marker: Int,
    val length: Int,
    val nextOffset: Int,
    val isValid: Boolean,
    val isTerminal: Boolean,
)

/**
 * Safe EXIF and image header parser with resilience against corrupted blobs.
 */
object ImageMetadataParser {
    /**
     * Checks if the header matches PNG signature.
     *
     * @param bytes Image byte array.
     * @return True if PNG.
     */
    private fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < HeaderMarkers.PNG_HEADER_LEN) return false
        val b0 = bytes[0].toInt() and HeaderMarkers.MASK_BYTE
        val b1 = bytes[1].toInt().toChar()
        val b2 = bytes[2].toInt().toChar()
        val b3 = bytes[HeaderMarkers.INDEX_3].toInt().toChar()
        return b0 == HeaderMarkers.PNG_BYTE_0 && b1 == 'P' && b2 == 'N' && b3 == 'G'
    }

    /**
     * Checks if the header begins with JPEG SOI marker.
     *
     * @param bytes Image byte array.
     * @return True if JPEG SOI.
     */
    private fun isJpeg(bytes: ByteArray): Boolean {
        val b0 = bytes[0].toInt() and HeaderMarkers.MASK_BYTE
        val b1 = bytes[1].toInt() and HeaderMarkers.MASK_BYTE
        return b0 == HeaderMarkers.MARKER_PREFIX && b1 == HeaderMarkers.MARKER_SOI
    }

    /**
     * Parses frame dimensions from an SOF0 segment.
     *
     * @param bytes Raw byte array.
     * @param offset Start offset of the SOF0 segment.
     * @return Pair of width to height.
     */
    private fun parseDimensions(
        bytes: ByteArray,
        offset: Int,
    ): Pair<Int, Int> {
        val h1 = bytes[offset + HeaderMarkers.OFFSET_HEIGHT].toInt() and HeaderMarkers.MASK_BYTE
        val h2 = bytes[offset + HeaderMarkers.OFFSET_HEIGHT + 1].toInt() and HeaderMarkers.MASK_BYTE
        val height = (h1 shl HeaderMarkers.SHIFT_8) or h2

        val w1 = bytes[offset + HeaderMarkers.OFFSET_WIDTH].toInt() and HeaderMarkers.MASK_BYTE
        val w2 = bytes[offset + HeaderMarkers.OFFSET_WIDTH + 1].toInt() and HeaderMarkers.MASK_BYTE
        val width = (w1 shl HeaderMarkers.SHIFT_8) or w2
        return Pair(width, height)
    }

    /**
     * Parses metadata from raw image bytes.
     * Guarantees never to throw unhandled exceptions on corrupted or truncated inputs.
     *
     * @param bytes The raw image byte array.
     * @return The parsed [ImageMetadata], marked as corrupted if the format is invalid.
     */
    fun parse(bytes: ByteArray?): ImageMetadata =
        when {
            bytes == null || bytes.size < HeaderMarkers.MIN_HEADER_LEN -> ImageMetadata(isCorrupted = true)
            isPng(bytes) -> ImageMetadata(mimeType = "image/png", isCorrupted = false)
            !isJpeg(bytes) -> ImageMetadata(isCorrupted = true)
            else -> scanJpeg(bytes)
        }

    /**
     * Reads and decodes a JPEG segment at the current offset.
     *
     * @param bytes Image byte buffer.
     * @param offset Current cursor offset.
     * @return Decoded [SegmentInfo].
     */
    private fun readSegment(
        bytes: ByteArray,
        offset: Int,
    ): SegmentInfo {
        val prefix = bytes[offset].toInt() and HeaderMarkers.MASK_BYTE
        if (prefix != HeaderMarkers.MARKER_PREFIX) {
            return SegmentInfo(0, 0, offset, isValid = false, isTerminal = false)
        }
        val marker = bytes[offset + 1].toInt() and HeaderMarkers.MASK_BYTE
        val bodyOffset = offset + 2

        return when {
            marker == HeaderMarkers.MARKER_EOI || marker == HeaderMarkers.MARKER_SOS ->
                SegmentInfo(marker, 0, bytes.size, isValid = true, isTerminal = true)
            bodyOffset + 2 > bytes.size ->
                SegmentInfo(marker, 0, offset, isValid = false, isTerminal = false)
            else -> {
                val l1 = bytes[bodyOffset].toInt() and HeaderMarkers.MASK_BYTE
                val l2 = bytes[bodyOffset + 1].toInt() and HeaderMarkers.MASK_BYTE
                val length = (l1 shl HeaderMarkers.SHIFT_8) or l2
                val validLen = length >= HeaderMarkers.MIN_SEGMENT_LEN && bodyOffset + length <= bytes.size
                SegmentInfo(marker, length, bodyOffset + length, isValid = validLen, isTerminal = false)
            }
        }
    }

    /**
     * Scans JPEG segments for dimensions and orientation.
     *
     * @param bytes JPEG image bytes.
     * @return Resulting ImageMetadata.
     */
    private fun scanJpeg(bytes: ByteArray): ImageMetadata {
        var offset = 2
        var orientation: Int? = null
        var dims: Pair<Int, Int>? = null
        var malformed = false

        while (offset < bytes.size - 1 && !malformed) {
            val seg = readSegment(bytes, offset)
            if (!seg.isValid) {
                malformed = true
            } else if (seg.isTerminal) {
                offset = bytes.size
            } else {
                if (seg.marker == HeaderMarkers.MARKER_APP1 && seg.length >= HeaderMarkers.EXIF_MIN_LEN) {
                    orientation = HeaderMarkers.DEFAULT_ORIENTATION
                }
                if (seg.marker == HeaderMarkers.MARKER_SOF0 && seg.length >= HeaderMarkers.SOF_MIN_LEN) {
                    dims = parseDimensions(bytes, offset + 2)
                }
                offset = seg.nextOffset
            }
        }

        return ImageMetadata(
            width = dims?.first,
            height = dims?.second,
            orientation = orientation,
            mimeType = "image/jpeg",
            isCorrupted = malformed,
        )
    }
}
