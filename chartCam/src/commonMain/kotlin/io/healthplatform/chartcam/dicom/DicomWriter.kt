/**
 * @file DicomWriter.kt
 * Serializer for standard DICOM Part 10 binary files with Explicit VR Little Endian.
 */
package io.healthplatform.chartcam.dicom

import okio.Buffer

private const val PREAMBLE_SIZE = 128
private const val BYTE_MASK = 0xFF
private const val SHIFT_8 = 8
private const val SHIFT_16 = 16
private const val SHIFT_24 = 24
private const val GROUP_FILE_META = 0x0002

/**
 * Serializes DICOM elements into standard DICOM Part 10 binary format.
 */
object DicomWriter {
    /**
     * Serializes a collection of DICOM elements into a compliant Part 10 byte stream.
     *
     * @param elements The list of DICOM elements to encode.
     * @param sopClassUid The SOP Class UID to ensure in File Meta Information.
     * @param sopInstanceUid The SOP Instance UID to ensure in File Meta Information.
     * @return The complete Part 10 DICOM file as a ByteArray.
     */
    fun write(
        elements: List<DicomElement>,
        sopClassUid: String,
        sopInstanceUid: String,
    ): ByteArray {
        val out = Buffer()

        // 1. 128-byte preamble of zeros
        out.write(ByteArray(PREAMBLE_SIZE))

        // 2. 4-byte ASCII "DICM" magic header
        out.write("DICM".encodeToByteArray())

        // 3. Partition elements into Group 0002 (File Meta) and Dataset (Group > 0002)
        val fileMetaElements = prepareFileMetaElements(elements, sopClassUid, sopInstanceUid)
        val datasetElements =
            elements
                .filter { DicomTag.getGroup(it.tag) > GROUP_FILE_META }
                .sortedBy { it.tag }

        // 4. Serialize File Meta Elements into a temporary buffer to compute Group Length (0002,0000)
        val metaBuffer = Buffer()
        fileMetaElements.forEach { encodeElement(metaBuffer, it) }

        // 5. Write File Meta Information Group Length (0002,0000)
        val groupLengthElement =
            DicomElement.createUL(
                DicomTag.FILE_META_INFORMATION_GROUP_LENGTH,
                metaBuffer.size,
            )
        encodeElement(out, groupLengthElement)

        // 6. Write the rest of Group 0002 elements
        out.write(metaBuffer.readByteArray())

        // 7. Write Dataset elements (Group > 0002)
        datasetElements.forEach { encodeElement(out, it) }

        return out.readByteArray()
    }

    /**
     * Prepares and ensures mandatory Group 0002 File Meta Information elements.
     *
     * @param elements The input elements.
     * @param sopClassUid The SOP Class UID.
     * @param sopInstanceUid The SOP Instance UID.
     * @return A sorted list of Group 0002 elements (excluding 0002,0000).
     */
    private fun prepareFileMetaElements(
        elements: List<DicomElement>,
        sopClassUid: String,
        sopInstanceUid: String,
    ): List<DicomElement> {
        val metaMap = mutableMapOf<Int, DicomElement>()

        // Copy any existing Group 0002 elements (except group length 0002,0000)
        elements
            .filter {
                DicomTag.getGroup(it.tag) == GROUP_FILE_META &&
                    it.tag != DicomTag.FILE_META_INFORMATION_GROUP_LENGTH
            }.forEach { metaMap[it.tag] = it }

        // Ensure (0002,0001) File Meta Information Version
        if (!metaMap.containsKey(DicomTag.FILE_META_INFORMATION_VERSION)) {
            metaMap[DicomTag.FILE_META_INFORMATION_VERSION] =
                DicomElement(
                    DicomTag.FILE_META_INFORMATION_VERSION,
                    DicomVR.OB,
                    byteArrayOf(0x00, 0x01),
                )
        }

        // Ensure (0002,0002) Media Storage SOP Class UID
        if (!metaMap.containsKey(DicomTag.MEDIA_STORAGE_SOP_CLASS_UID)) {
            metaMap[DicomTag.MEDIA_STORAGE_SOP_CLASS_UID] =
                DicomElement.createString(
                    DicomTag.MEDIA_STORAGE_SOP_CLASS_UID,
                    DicomVR.UI,
                    sopClassUid,
                )
        }

        // Ensure (0002,0003) Media Storage SOP Instance UID
        if (!metaMap.containsKey(DicomTag.MEDIA_STORAGE_SOP_INSTANCE_UID)) {
            metaMap[DicomTag.MEDIA_STORAGE_SOP_INSTANCE_UID] =
                DicomElement.createString(
                    DicomTag.MEDIA_STORAGE_SOP_INSTANCE_UID,
                    DicomVR.UI,
                    sopInstanceUid,
                )
        }

        // Ensure (0002,0010) Transfer Syntax UID (Explicit VR Little Endian)
        if (!metaMap.containsKey(DicomTag.TRANSFER_SYNTAX_UID)) {
            metaMap[DicomTag.TRANSFER_SYNTAX_UID] =
                DicomElement.createString(
                    DicomTag.TRANSFER_SYNTAX_UID,
                    DicomVR.UI,
                    DicomTag.UID_EXPLICIT_VR_LITTLE_ENDIAN,
                )
        }

        // Ensure (0002,0012) Implementation Class UID
        if (!metaMap.containsKey(DicomTag.IMPLEMENTATION_CLASS_UID)) {
            metaMap[DicomTag.IMPLEMENTATION_CLASS_UID] =
                DicomElement.createString(
                    DicomTag.IMPLEMENTATION_CLASS_UID,
                    DicomVR.UI,
                    DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS,
                )
        }

        // Ensure (0002,0013) Implementation Version Name
        if (!metaMap.containsKey(DicomTag.IMPLEMENTATION_VERSION_NAME)) {
            metaMap[DicomTag.IMPLEMENTATION_VERSION_NAME] =
                DicomElement.createString(
                    DicomTag.IMPLEMENTATION_VERSION_NAME,
                    DicomVR.SH,
                    DicomTag.IMPLEMENTATION_VERSION_STRING,
                )
        }

        return metaMap.values.sortedBy { it.tag }
    }

    /**
     * Encodes a single DicomElement in Explicit VR Little Endian format into the target buffer.
     *
     * @param buffer The destination buffer.
     * @param element The DICOM element to serialize.
     */
    fun encodeElement(
        buffer: Buffer,
        element: DicomElement,
    ) {
        val group = DicomTag.getGroup(element.tag)
        val elem = DicomTag.getElement(element.tag)

        // Write Tag Group and Element (16-bit integers in Little Endian)
        writeShortLe(buffer, group)
        writeShortLe(buffer, elem)

        // Write 2-character ASCII VR
        buffer.write(element.vr.code.encodeToByteArray())

        if (element.vr.isExtended) {
            // Extended VR (OB, OW, OF, SQ, UN, UT): 2 reserved null bytes + 32-bit length
            writeShortLe(buffer, 0x0000)
            writeIntLe(buffer, element.value.size)
        } else {
            // Standard VR: 16-bit length
            writeShortLe(buffer, element.value.size)
        }

        // Write Value payload
        buffer.write(element.value)
    }

    /**
     * Writes a 16-bit integer in little-endian order to the buffer.
     *
     * @param buffer The target buffer.
     * @param value The integer value to write.
     */
    private fun writeShortLe(
        buffer: Buffer,
        value: Int,
    ) {
        buffer.writeByte(value and BYTE_MASK)
        buffer.writeByte((value ushr SHIFT_8) and BYTE_MASK)
    }

    /**
     * Writes a 32-bit integer in little-endian order to the buffer.
     *
     * @param buffer The target buffer.
     * @param value The integer value to write.
     */
    private fun writeIntLe(
        buffer: Buffer,
        value: Int,
    ) {
        buffer.writeByte(value and BYTE_MASK)
        buffer.writeByte((value ushr SHIFT_8) and BYTE_MASK)
        buffer.writeByte((value ushr SHIFT_16) and BYTE_MASK)
        buffer.writeByte((value ushr SHIFT_24) and BYTE_MASK)
    }
}
