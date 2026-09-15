/**
 * @file DicomReader.kt
 * Parser for DICOM Part 10 binary files.
 */
package io.healthplatform.chartcam.dicom

import okio.Buffer

private const val PREAMBLE_LEN = 128
private const val MAGIC_LEN = 4
private const val MAGIC_DICM = "DICM"
private const val MIN_HEADER_BYTES = 6
private const val VR_CODE_LEN = 2
private const val EXTENDED_RESERVED_BYTES = 2
private const val ITEM_HEADER_BYTES = 8
private const val ITEM_DELIM_SKIP_BYTES = 8L
private const val ITEM_TAG_GROUP = 0xFFFE
private const val ITEM_TAG_ELEMENT = 0xE000
private const val ITEM_DELIM_ELEMENT = 0xE0DD
private const val ITEM_NESTED_DELIM_ELEMENT = 0xE00D
private const val BYTE_MASK = 0xFF
private const val SHIFT_8 = 8
private const val SHIFT_16 = 16
private const val SHIFT_24 = 24
private const val MASK_16_BIT = 0xFFFFL
private const val MASK_32_BIT = 0xFFFFFFFFL
private const val JPEG_HEADER_CHECK_LIMIT = 256
private const val JPEG_SOI_0 = 0xFF.toByte()
private const val JPEG_SOI_1 = 0xD8.toByte()

/**
 * Parsed in-memory representation of a DICOM dataset.
 *
 * @property elements Map of 32-bit tag integer to DicomElement.
 * @property patientName Patient's Name (0010,0010).
 * @property patientId Patient ID (0010,0020).
 * @property patientSex Patient's Sex (0010,0040).
 * @property modality Modality (0008,0060).
 * @property sopClassUid SOP Class UID (0008,0016).
 * @property transferSyntaxUid Transfer Syntax UID (0002,0010).
 * @property pixelData Extracted pixel data bytes (e.g. JPEG fragment or raw raster).
 * @property encapsulatedPdf Extracted PDF payload bytes, if this is an Encapsulated PDF document.
 * @property width Image width in columns, if present.
 * @property height Image height in rows, if present.
 */
data class DicomDataset(
    val elements: Map<Int, DicomElement>,
    val patientName: String? = null,
    val patientId: String? = null,
    val patientSex: String? = null,
    val modality: String? = null,
    val sopClassUid: String? = null,
    val transferSyntaxUid: String? = null,
    val pixelData: ByteArray? = null,
    val encapsulatedPdf: ByteArray? = null,
    val width: Int? = null,
    val height: Int? = null,
)

/**
 * Parser for decoding standard DICOM Part 10 streams into [DicomDataset].
 */
object DicomReader {
    /**
     * Parses a DICOM Part 10 binary payload.
     *
     * @param bytes Raw DICOM Part 10 byte array.
     * @return A [Result] enclosing the decoded [DicomDataset], or failure if invalid.
     */
    fun read(bytes: ByteArray): Result<DicomDataset> =
        runCatching {
            require(bytes.size >= PREAMBLE_LEN + MAGIC_LEN) { "Byte stream too short for DICOM Part 10 file" }
            val magic = bytes.decodeToString(PREAMBLE_LEN, PREAMBLE_LEN + MAGIC_LEN)
            require(magic == MAGIC_DICM) { "Missing DICM prefix at byte 128: found '$magic'" }

            val offset = PREAMBLE_LEN + MAGIC_LEN
            val buffer = Buffer()
            buffer.write(bytes, offset, bytes.size - offset)

            val elements = parseAllElements(buffer)
            buildDataset(elements)
        }

    /**
     * Parses all DICOM elements from the payload buffer until exhausted.
     *
     * @param buffer The Okio buffer positioned at the first DICOM element.
     * @return Map of element tag to DicomElement.
     */
    private fun parseAllElements(buffer: Buffer): Map<Int, DicomElement> {
        val elements = mutableMapOf<Int, DicomElement>()
        while (buffer.size >= MIN_HEADER_BYTES) {
            parseNextElement(buffer, elements)
        }
        return elements
    }

    /**
     * Parses a single DICOM element and stores it into the elements map.
     *
     * @param buffer The input buffer.
     * @param elements The accumulator map of elements.
     */
    private fun parseNextElement(
        buffer: Buffer,
        elements: MutableMap<Int, DicomElement>,
    ) {
        val group = readShortLe(buffer)
        val elementNum = readShortLe(buffer)
        val tag = (group shl SHIFT_16) or elementNum

        val vrCode = buffer.readByteArray(VR_CODE_LEN.toLong()).decodeToString()
        val vr = DicomVR.entries.firstOrNull { it.code == vrCode } ?: DicomVR.UN

        val (length, isUndefined) = resolveElementLength(buffer, vr)
        if (isUndefined) {
            val elem = parseUndefinedSequence(buffer, tag, vr)
            elements[tag] = elem
        } else if (buffer.size >= length) {
            val valBytes = buffer.readByteArray(length)
            elements[tag] = DicomElement(tag, vr, valBytes)
        }
    }

    /**
     * Resolves length and undefined status for the current element.
     *
     * @param buffer The input buffer.
     * @param vr The value representation of the element.
     * @return Pair of resolved length and whether length is undefined.
     */
    private fun resolveElementLength(
        buffer: Buffer,
        vr: DicomVR,
    ): Pair<Long, Boolean> =
        if (vr.isExtended) {
            resolveExtendedLength(buffer)
        } else {
            resolveStandardLength(buffer)
        }

    /**
     * Resolves extended 32-bit element length.
     *
     * @param buffer The input buffer.
     * @return Pair of resolved length and whether length is undefined.
     */
    private fun resolveExtendedLength(buffer: Buffer): Pair<Long, Boolean> {
        if (buffer.size < MIN_HEADER_BYTES) return Pair(0L, false)
        buffer.skip(EXTENDED_RESERVED_BYTES.toLong())
        val len32 = readIntLe(buffer)
        val isUndef = len32 == -1
        val len = if (isUndef) -1L else len32.toLong() and MASK_32_BIT
        return Pair(len, isUndef)
    }

    /**
     * Resolves standard 16-bit element length.
     *
     * @param buffer The input buffer.
     * @return Pair of resolved length and false.
     */
    private fun resolveStandardLength(buffer: Buffer): Pair<Long, Boolean> {
        if (buffer.size < VR_CODE_LEN) return Pair(0L, false)
        val len16 = readShortLe(buffer)
        return Pair(len16.toLong() and MASK_16_BIT, false)
    }

    /**
     * Parses undefined length sequences, extracting fragments and delimiters.
     *
     * @param buffer The input buffer.
     * @param tag The element tag.
     * @param vr The element VR.
     * @return The constructed DicomElement.
     */
    private fun parseUndefinedSequence(
        buffer: Buffer,
        tag: Int,
        vr: DicomVR,
    ): DicomElement {
        val seqBuffer = Buffer()
        var extractedFragment: ByteArray? = null
        var isFirstItem = true

        while (buffer.size >= ITEM_HEADER_BYTES) {
            val itemGroup = readShortLe(buffer)
            val itemElem = readShortLe(buffer)
            val itemLen = readIntLe(buffer)

            if (itemGroup == ITEM_TAG_GROUP && itemElem == ITEM_DELIM_ELEMENT) break
            if (itemGroup == ITEM_TAG_GROUP && itemElem == ITEM_TAG_ELEMENT) {
                val itemBytes = readItemContent(buffer, itemLen)
                seqBuffer.write(itemBytes)
                val isJpg = isJpeg(itemBytes)
                val (newFrag, nextFirst) = resolveFragment(itemBytes, isJpg, isFirstItem, extractedFragment)
                extractedFragment = newFrag
                isFirstItem = nextFirst
            } else if (itemLen > 0) {
                buffer.skip(itemLen.toLong())
            }
        }
        val finalBytes = extractedFragment ?: seqBuffer.readByteArray()
        return DicomElement(tag, vr, finalBytes, isUndefinedLength = true)
    }

    /**
     * Resolves the extracted fragment based on item order and format.
     *
     * @param itemBytes The byte contents of the item.
     * @param isJpg Whether the item bytes represent a JPEG image.
     * @param isFirstItem Whether this is the first item in the sequence.
     * @param current The current extracted fragment bytes, if any.
     * @return Pair of updated fragment bytes and false.
     */
    private fun resolveFragment(
        itemBytes: ByteArray,
        isJpg: Boolean,
        isFirstItem: Boolean,
        current: ByteArray?,
    ): Pair<ByteArray?, Boolean> {
        val frag =
            if (isFirstItem) {
                if (isJpg) itemBytes else current
            } else {
                if (current == null || isJpg) itemBytes else current
            }
        return Pair(frag, false)
    }

    /**
     * Reads the byte contents of an individual sequence item.
     *
     * @param buffer The source buffer.
     * @param itemLen The parsed length of the item (-1 if undefined).
     * @return The item content byte array.
     */
    private fun readItemContent(
        buffer: Buffer,
        itemLen: Int,
    ): ByteArray {
        if (itemLen == -1) {
            val itemBuf = Buffer()
            while (buffer.size >= ITEM_HEADER_BYTES) {
                val g = (buffer[0].toInt() and BYTE_MASK) or ((buffer[1].toInt() and BYTE_MASK) shl SHIFT_8)
                val e = (buffer[2].toInt() and BYTE_MASK) or ((buffer[3].toInt() and BYTE_MASK) shl SHIFT_8)
                if (g == ITEM_TAG_GROUP && e == ITEM_NESTED_DELIM_ELEMENT) {
                    buffer.skip(ITEM_DELIM_SKIP_BYTES)
                    break
                }
                itemBuf.writeByte(buffer.readByte().toInt())
            }
            return itemBuf.readByteArray()
        }
        return if (itemLen > 0) buffer.readByteArray(itemLen.toLong()) else ByteArray(0)
    }

    /**
     * Builds a [DicomDataset] from a map of parsed elements.
     *
     * @param elements Map of parsed DICOM elements.
     * @return The synthesized [DicomDataset].
     */
    private fun buildDataset(elements: Map<Int, DicomElement>): DicomDataset {
        val patientName = extractStringTag(elements, DicomTag.PATIENT_NAME)
        val patientId = extractStringTag(elements, DicomTag.PATIENT_ID)
        val patientSex = extractStringTag(elements, DicomTag.PATIENT_SEX)
        val modality = extractStringTag(elements, DicomTag.MODALITY)
        val sopClassUid = extractStringTag(elements, DicomTag.SOP_CLASS_UID)
        val transferSyntaxUid = extractStringTag(elements, DicomTag.TRANSFER_SYNTAX_UID)

        val rows = extractDimensionTag(elements, DicomTag.ROWS)
        val cols = extractDimensionTag(elements, DicomTag.COLUMNS)

        val pixelElem = elements[DicomTag.PIXEL_DATA]
        val pixelData = if (pixelElem != null) extractRawPixelPayload(pixelElem) else null
        val pdfElem = elements[DicomTag.ENCAPSULATED_DOCUMENT]
        val pdfData = pdfElem?.value

        return DicomDataset(
            elements = elements,
            patientName = patientName,
            patientId = patientId,
            patientSex = patientSex,
            modality = modality,
            sopClassUid = sopClassUid,
            transferSyntaxUid = transferSyntaxUid,
            pixelData = pixelData,
            encapsulatedPdf = pdfData,
            width = cols,
            height = rows,
        )
    }

    /**
     * Extracts trimmed string value from a DICOM tag.
     *
     * @param elements Map of elements.
     * @param tag Tag integer.
     * @return Trimmed string or null.
     */
    private fun extractStringTag(
        elements: Map<Int, DicomElement>,
        tag: Int,
    ): String? {
        val elem = elements[tag] ?: return null
        return elem.value.decodeToString().trim()
    }

    /**
     * Extracts unsigned dimension short from a DICOM tag.
     *
     * @param elements Map of elements.
     * @param tag Tag integer.
     * @return Dimension integer or null.
     */
    private fun extractDimensionTag(
        elements: Map<Int, DicomElement>,
        tag: Int,
    ): Int? {
        val elem = elements[tag] ?: return null
        return readUS(elem.value)
    }

    /**
     * Validates and cleans a JPEG byte array by checking standard SOI markers.
     *
     * @param bytes Candidate JPEG bytes.
     * @return A [Result] enclosing the valid JPEG bytes, or failure if invalid.
     */
    fun cleanJpegPayload(bytes: ByteArray?): Result<ByteArray> {
        if (bytes == null || bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("JPEG payload is null or empty"))
        }
        val match = findJpegStart(bytes)
        return if (match != null) {
            Result.success(match)
        } else {
            Result.failure(IllegalArgumentException("Invalid JPEG Start of Image (SOI) marker"))
        }
    }

    /**
     * Finds the JPEG SOI marker and returns bytes starting from that offset.
     *
     * @param bytes The raw byte array.
     * @return Byte array starting at SOI marker or null.
     */
    private fun findJpegStart(bytes: ByteArray): ByteArray? {
        if (isJpeg(bytes)) return bytes
        var foundIdx = -1
        for (i in 0 until minOf(bytes.size - 1, JPEG_HEADER_CHECK_LIMIT)) {
            if (bytes[i] == JPEG_SOI_0 && bytes[i + 1] == JPEG_SOI_1) {
                foundIdx = i
                break
            }
        }
        return if (foundIdx != -1) bytes.copyOfRange(foundIdx, bytes.size) else null
    }

    /**
     * Checks whether the byte array begins with standard JPEG SOI markers.
     *
     * @param bytes The byte array to check.
     * @return True if starting with SOI markers, false otherwise.
     */
    private fun isJpeg(bytes: ByteArray): Boolean = bytes.size >= 2 && bytes[0] == JPEG_SOI_0 && bytes[1] == JPEG_SOI_1

    /**
     * Extracts pixel payload from a Pixel Data DicomElement.
     *
     * @param elem The Pixel Data element.
     * @return The extracted pixel byte array.
     */
    private fun extractRawPixelPayload(elem: DicomElement): ByteArray = findJpegStart(elem.value) ?: elem.value

    /**
     * Reads a 16-bit little endian integer from the buffer.
     *
     * @param buffer The source buffer.
     * @return The 16-bit integer value.
     */
    private fun readShortLe(buffer: Buffer): Int {
        val b0 = buffer.readByte().toInt() and BYTE_MASK
        val b1 = buffer.readByte().toInt() and BYTE_MASK
        return (b1 shl SHIFT_8) or b0
    }

    /**
     * Reads a 32-bit little endian integer from the buffer.
     *
     * @param buffer The source buffer.
     * @return The 32-bit integer value.
     */
    private fun readIntLe(buffer: Buffer): Int {
        val b0 = buffer.readByte().toInt() and BYTE_MASK
        val b1 = buffer.readByte().toInt() and BYTE_MASK
        val b2 = buffer.readByte().toInt() and BYTE_MASK
        val b3 = buffer.readByte().toInt() and BYTE_MASK
        return (b3 shl SHIFT_24) or (b2 shl SHIFT_16) or (b1 shl SHIFT_8) or b0
    }

    /**
     * Reads an unsigned short from a byte array.
     *
     * @param bytes The raw bytes.
     * @return The integer value.
     */
    private fun readUS(bytes: ByteArray): Int {
        if (bytes.size < 2) return 0
        val b0 = bytes[0].toInt() and BYTE_MASK
        val b1 = bytes[1].toInt() and BYTE_MASK
        return (b1 shl SHIFT_8) or b0
    }
}
