/**
 * @file DicomElement.kt
 * Value Representations (VR) and Data Elements for DICOM Part 10 encoding.
 */
package io.healthplatform.chartcam.dicom

/**
 * Standard DICOM Value Representations (VR).
 *
 * @property code 2-character ASCII mnemonic code for the VR.
 * @property isExtended True if this VR uses a 4-byte extended header (2 reserved bytes + 32-bit length).
 */
enum class DicomVR(
    val code: String,
    val isExtended: Boolean = false,
) {
    /** Application Entity. */
    AE("AE"),

    /** Age String. */
    AS("AS"),

    /** Code String. */
    CS("CS"),

    /** Date (YYYYMMDD). */
    DA("DA"),

    /** Decimal String. */
    DS("DS"),

    /** Date Time. */
    DT("DT"),

    /** Floating Point Single. */
    FL("FL"),

    /** Floating Point Double. */
    FD("FD"),

    /** Integer String. */
    IS("IS"),

    /** Long String. */
    LO("LO"),

    /** Long Text. */
    LT("LT"),

    /** Other Byte String (extended 32-bit length). */
    OB("OB", isExtended = true),

    /** Other Float String (extended 32-bit length). */
    OF("OF", isExtended = true),

    /** Other Word String (extended 32-bit length). */
    OW("OW", isExtended = true),

    /** Person Name (e.g. Last^First^Middle). */
    PN("PN"),

    /** Short String. */
    SH("SH"),

    /** Signed Long. */
    SL("SL"),

    /** Sequence of Items (extended 32-bit length). */
    SQ("SQ", isExtended = true),

    /** Signed Short. */
    SS("SS"),

    /** Short Text. */
    ST("ST"),

    /** Time (HHMMSS). */
    TM("TM"),

    /** Unique Identifier (UID). */
    UI("UI"),

    /** Unsigned Long. */
    UL("UL"),

    /** Unknown (extended 32-bit length). */
    UN("UN", isExtended = true),

    /** Unsigned Short. */
    US("US"),

    /** Unlimited Text (extended 32-bit length). */
    UT("UT", isExtended = true),
}

private const val BYTE_MASK = 0xFF
private const val SHIFT_8 = 8
private const val SHIFT_16 = 16
private const val SHIFT_24 = 24
private const val SPACE_PAD_BYTE: Byte = 0x20
private const val DATE_STR_LEN = 8
private const val TIME_STR_LEN = 6

/**
 * Represents a single DICOM Data Element consisting of a 32-bit Tag, Value Representation, and byte payload.
 *
 * @property tag The 32-bit composite DICOM tag (group shl 16 or element).
 * @property vr The Value Representation specifying how the data is encoded.
 * @property value The raw binary payload of the element, aligned to an even byte boundary.
 * @property isUndefinedLength True if the element is serialized with 0xFFFFFFFF undefined length.
 */
data class DicomElement(
    val tag: Int,
    val vr: DicomVR,
    val value: ByteArray,
    val isUndefinedLength: Boolean = false,
) {
    /**
     * Checks equality based on tag, VR, and byte array content.
     *
     * @param other The other object to compare with.
     * @return True if tag, VR, and content match.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DicomElement) return false
        if (tag != other.tag) return false
        if (vr != other.vr) return false
        return value.contentEquals(other.value)
    }

    /**
     * Computes hash code incorporating tag, VR, and value array content.
     *
     * @return The hash code.
     */
    override fun hashCode(): Int {
        var result = tag
        result = 31 * result + vr.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }

    /**
     * Factory methods for creating standard DICOM data elements.
     */
    companion object {
        /**
         * Creates a string element with appropriate DICOM even-byte padding.
         *
         * @param tag The DICOM tag.
         * @param vr The string-compatible Value Representation (e.g. CS, SH, LO, PN, UI).
         * @param text The string text value.
         * @return A padded DicomElement.
         */
        fun createString(
            tag: Int,
            vr: DicomVR,
            text: String,
        ): DicomElement {
            val rawBytes = text.encodeToByteArray()
            val padByte: Byte = if (vr == DicomVR.UI) 0x00 else SPACE_PAD_BYTE // UI pads with null, others with space
            val paddedBytes =
                if (rawBytes.size % 2 != 0) {
                    val padded = ByteArray(rawBytes.size + 1)
                    rawBytes.copyInto(padded)
                    padded[rawBytes.size] = padByte
                    padded
                } else {
                    rawBytes
                }
            return DicomElement(tag, vr, paddedBytes)
        }

        /**
         * Creates a Date element in DICOM format (YYYYMMDD).
         *
         * @param tag The DICOM tag (usually VR = DA).
         * @param dateText The date string in format YYYY-MM-DD, YYYYMMDD, or ISO-8601 datetime.
         * @return A DicomElement with VR.DA.
         */
        fun createDate(
            tag: Int,
            dateText: String,
        ): DicomElement {
            val dateOnly = if (dateText.contains("T")) dateText.substringBefore("T") else dateText
            val sanitized = dateOnly.replace("-", "").take(DATE_STR_LEN)
            return createString(tag, DicomVR.DA, sanitized)
        }

        /**
         * Creates a Time element in DICOM format (HHMMSS).
         *
         * @param tag The DICOM tag (usually VR = TM).
         * @param timeText The time string in format HH:MM:SS, HHMMSS, or ISO-8601 datetime.
         * @return A DicomElement with VR.TM.
         */
        fun createTime(
            tag: Int,
            timeText: String,
        ): DicomElement {
            val rawTime = if (timeText.contains("T")) timeText.substringAfter("T") else timeText
            val timePart = rawTime.takeWhile { it.isDigit() || it == ':' }
            val sanitized = timePart.replace(":", "").take(TIME_STR_LEN)
            val padded = sanitized.padEnd(TIME_STR_LEN, '0')
            return createString(tag, DicomVR.TM, padded)
        }

        /**
         * Creates an Unsigned Short (US) 16-bit integer element in Little Endian.
         *
         * @param tag The DICOM tag.
         * @param intValue The 16-bit unsigned integer value.
         * @return A 2-byte DicomElement with VR.US.
         */
        fun createUS(
            tag: Int,
            intValue: Int,
        ): DicomElement {
            val bytes =
                byteArrayOf(
                    (intValue and BYTE_MASK).toByte(),
                    ((intValue ushr SHIFT_8) and BYTE_MASK).toByte(),
                )
            return DicomElement(tag, DicomVR.US, bytes)
        }

        /**
         * Creates an Unsigned Long (UL) 32-bit integer element in Little Endian.
         *
         * @param tag The DICOM tag.
         * @param longValue The 32-bit unsigned integer value.
         * @return A 4-byte DicomElement with VR.UL.
         */
        fun createUL(
            tag: Int,
            longValue: Long,
        ): DicomElement {
            val bytes =
                byteArrayOf(
                    (longValue and BYTE_MASK.toLong()).toByte(),
                    ((longValue ushr SHIFT_8) and BYTE_MASK.toLong()).toByte(),
                    ((longValue ushr SHIFT_16) and BYTE_MASK.toLong()).toByte(),
                    ((longValue ushr SHIFT_24) and BYTE_MASK.toLong()).toByte(),
                )
            return DicomElement(tag, DicomVR.UL, bytes)
        }

        /**
         * Creates a binary byte element (OB or OW), padded with a trailing zero if length is odd.
         *
         * @param tag The DICOM tag.
         * @param vr The binary Value Representation (OB, OW, UN).
         * @param bytes The raw binary payload.
         * @return An even-padded DicomElement.
         */
        fun createBinary(
            tag: Int,
            vr: DicomVR,
            bytes: ByteArray,
        ): DicomElement {
            val padded =
                if (bytes.size % 2 != 0) {
                    val p = ByteArray(bytes.size + 1)
                    bytes.copyInto(p)
                    p[bytes.size] = 0x00
                    p
                } else {
                    bytes
                }
            return DicomElement(tag, vr, padded)
        }

        /**
         * Creates an encapsulated pixel data element serialized with 0xFFFFFFFF undefined length.
         *
         * @param tag The DICOM tag.
         * @param sequenceBytes The serialized item sequence buffer.
         * @return A DicomElement with isUndefinedLength set to true.
         */
        fun createEncapsulatedPixelData(
            tag: Int,
            sequenceBytes: ByteArray,
        ): DicomElement = DicomElement(tag, DicomVR.OB, sequenceBytes, isUndefinedLength = true)
    }
}
