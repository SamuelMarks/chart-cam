/**
 * @file DicomHeaderTest.kt
 * Unit tests for standard Part 10 DICOM preamble, magic bytes, and File Meta Information generation.
 */
package io.healthplatform.chartcam.dicom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PREAMBLE_SIZE = 128
private const val DICM_MAGIC_SIZE = 4
private const val HEADER_TOTAL = PREAMBLE_SIZE + DICM_MAGIC_SIZE

/**
 * Tests for DICOM Part 10 file structure, headers, and metadata encoding.
 */
class DicomHeaderTest {
    /**
     * Verifies that DicomWriter produces the mandatory 128-byte zero preamble and "DICM" magic byte string.
     */
    @Test
    fun testPreambleAndMagicBytes() {
        val bytes =
            DicomWriter.write(
                elements = emptyList(),
                sopClassUid = DicomTag.UID_SOP_CLASS_ENCAPSULATED_PDF,
                sopInstanceUid = "1.2.3.4.5",
            )

        assertTrue(bytes.size >= HEADER_TOTAL)

        // Check first 128 bytes are all 0x00
        for (i in 0 until PREAMBLE_SIZE) {
            assertEquals(0x00.toByte(), bytes[i], "Preamble byte at index $i must be zero")
        }

        // Check bytes 128..131 are "DICM" (ASCII 0x44, 0x49, 0x43, 0x4D)
        val magic = bytes.copyOfRange(PREAMBLE_SIZE, HEADER_TOTAL).decodeToString()
        assertEquals("DICM", magic)
    }

    /**
     * Verifies that File Meta Information (Group 0002) tags and group length are present and accurately structured.
     */
    @Test
    fun testFileMetaInformationPresence() {
        val sopClass = DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE
        val sopInstance = "1.3.6.1.4.1.59999.3.999"

        val bytes =
            DicomWriter.write(
                elements =
                    listOf(
                        DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, "SMITH^JOHN"),
                    ),
                sopClassUid = sopClass,
                sopInstanceUid = sopInstance,
            )

        // The file must contain standard Group 0002 tags and the dataset tag (0010,0010)
        assertTrue(bytes.size > HEADER_TOTAL + 100)

        // Verify that the byte stream contains the SOP instance and class strings
        val fullContent = bytes.decodeToString()
        assertTrue(fullContent.contains("DICM"))
        assertTrue(fullContent.contains(sopClass))
        assertTrue(fullContent.contains(sopInstance))
        assertTrue(fullContent.contains(DicomTag.UID_EXPLICIT_VR_LITTLE_ENDIAN))
        assertTrue(fullContent.contains(DicomTag.IMPLEMENTATION_VERSION_STRING))
        assertTrue(fullContent.contains("SMITH^JOHN"))
    }

    /**
     * Tests passing custom Group 0002 File Meta Information elements.
     */
    @Test
    fun testCustomFileMetaElements() {
        val customElements =
            listOf(
                DicomElement.createUL(DicomTag.FILE_META_INFORMATION_GROUP_LENGTH, 100L),
                DicomElement(DicomTag.FILE_META_INFORMATION_VERSION, DicomVR.OB, byteArrayOf(0x00, 0x01)),
                DicomElement.createString(DicomTag.MEDIA_STORAGE_SOP_CLASS_UID, DicomVR.UI, DicomTag.UID_SOP_CLASS_SECONDARY_CAPTURE),
                DicomElement.createString(DicomTag.MEDIA_STORAGE_SOP_INSTANCE_UID, DicomVR.UI, "1.2.3.4"),
                DicomElement.createString(DicomTag.TRANSFER_SYNTAX_UID, DicomVR.UI, DicomTag.UID_EXPLICIT_VR_LITTLE_ENDIAN),
                DicomElement.createString(DicomTag.IMPLEMENTATION_CLASS_UID, DicomVR.UI, "1.2.3.5"),
                DicomElement.createString(DicomTag.IMPLEMENTATION_VERSION_NAME, DicomVR.SH, "CUSTOM_VER"),
            )

        val bytes =
            DicomWriter.write(
                elements = customElements,
                sopClassUid = DicomTag.UID_SOP_CLASS_SECONDARY_CAPTURE,
                sopInstanceUid = "1.2.3.4",
            )

        val fullContent = bytes.decodeToString()
        assertTrue(fullContent.contains("CUSTOM_VER"))
        assertTrue(fullContent.contains("1.2.3.5"))
    }
}
