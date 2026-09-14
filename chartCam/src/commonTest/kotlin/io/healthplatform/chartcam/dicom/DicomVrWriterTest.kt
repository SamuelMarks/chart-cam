/**
 * @file DicomVrWriterTest.kt
 * Unit tests for DICOM Value Representation encoding and element creation.
 */
package io.healthplatform.chartcam.dicom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for DICOM Value Representations and Element binary conversions.
 */
class DicomVrWriterTest {
    /**
     * Tests string element creation with standard space padding and UI null byte padding.
     */
    @Test
    fun testCreateStringPadding() {
        // Odd length string (5 chars) should be padded with space (0x20) to 6 bytes
        val elem = DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, "SMITH")
        assertEquals(6, elem.value.size)
        assertEquals(0x20.toByte(), elem.value[5])

        // Even length string (4 chars) should remain 4 bytes
        val evenElem = DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, "JOHN")
        assertEquals(4, evenElem.value.size)

        // UI VR with odd length must be padded with null byte (0x00)
        val uiElem = DicomElement.createString(DicomTag.SOP_CLASS_UID, DicomVR.UI, "1.2.3")
        assertEquals(6, uiElem.value.size)
        assertEquals(0x00.toByte(), uiElem.value[5])
    }

    /**
     * Tests Date and Time formatting into DICOM DA and TM representations.
     */
    @Test
    fun testCreateDateTime() {
        val dateElem = DicomElement.createDate(DicomTag.STUDY_DATE, "2026-09-11")
        assertEquals(DicomVR.DA, dateElem.vr)
        assertEquals("20260911", dateElem.value.decodeToString())

        val timeElem = DicomElement.createTime(DicomTag.STUDY_TIME, "14:30:15")
        assertEquals(DicomVR.TM, timeElem.vr)
        assertEquals("143015", timeElem.value.decodeToString())
    }

    /**
     * Tests Unsigned Short (16-bit) and Unsigned Long (32-bit) integer serialization in Little Endian.
     */
    @Test
    fun testCreateIntegers() {
        // 512 = 0x0200 -> bytes: [0x00, 0x02]
        val usElem = DicomElement.createUS(DicomTag.ROWS, 512)
        assertEquals(DicomVR.US, usElem.vr)
        assertEquals(2, usElem.value.size)
        assertEquals(0x00.toByte(), usElem.value[0])
        assertEquals(0x02.toByte(), usElem.value[1])

        // 65538 = 0x00010002 -> bytes: [0x02, 0x00, 0x01, 0x00]
        val ulElem = DicomElement.createUL(DicomTag.FILE_META_INFORMATION_GROUP_LENGTH, 65538L)
        assertEquals(DicomVR.UL, ulElem.vr)
        assertEquals(4, ulElem.value.size)
        assertEquals(0x02.toByte(), ulElem.value[0])
        assertEquals(0x00.toByte(), ulElem.value[1])
        assertEquals(0x01.toByte(), ulElem.value[2])
        assertEquals(0x00.toByte(), ulElem.value[3])
    }

    /**
     * Tests binary element creation and even-length null byte padding.
     */
    @Test
    fun testCreateBinary() {
        val oddBytes = byteArrayOf(0x01, 0x02, 0x03)
        val binElem = DicomElement.createBinary(DicomTag.ENCAPSULATED_DOCUMENT, DicomVR.OB, oddBytes)
        assertEquals(4, binElem.value.size)
        assertEquals(0x00.toByte(), binElem.value[3])

        val evenBytes = byteArrayOf(0x01, 0x02)
        val evenBin = DicomElement.createBinary(DicomTag.PIXEL_DATA, DicomVR.OB, evenBytes)
        assertEquals(2, evenBin.value.size)
    }

    /**
     * Tests equals, hashCode, and DicomVR extended flags.
     */
    @Test
    fun testElementEqualityAndVrProperties() {
        val elem1 = DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, "DOE")
        val elem2 = DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, "DOE")
        val elemDiffTag = DicomElement.createString(DicomTag.PATIENT_ID, DicomVR.LO, "DOE")
        val elemDiffVr = DicomElement(DicomTag.PATIENT_NAME, DicomVR.LO, elem1.value)
        val elemDiffVal = DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, "SMITH")

        assertEquals(elem1, elem2)
        assertEquals(elem1.hashCode(), elem2.hashCode())
        assertNotEquals(elem1, elemDiffTag)
        assertNotEquals(elem1, elemDiffVr)
        assertNotEquals(elem1, elemDiffVal)
        assertFalse(elem1.equals("not an element"))
        assertTrue(elem1.equals(elem1))

        assertTrue(DicomVR.OB.isExtended)
        assertTrue(DicomVR.OW.isExtended)
        assertTrue(DicomVR.UN.isExtended)
        assertFalse(DicomVR.PN.isExtended)
        assertFalse(DicomVR.UI.isExtended)
    }
}
