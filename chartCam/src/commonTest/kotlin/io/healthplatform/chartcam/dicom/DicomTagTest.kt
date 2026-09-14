/**
 * @file DicomTagTest.kt
 * Unit tests for standard DICOM tags and UID definitions.
 */
package io.healthplatform.chartcam.dicom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for DICOM Part 6 data dictionary tag constants and tag manipulation helpers.
 */
class DicomTagTest {
    /**
     * Verifies that Group and Element numbers are correctly extracted from composite 32-bit tag values.
     */
    @Test
    fun testGetGroupAndElement() {
        val patientNameTag = DicomTag.PATIENT_NAME
        assertEquals(0x0010, DicomTag.getGroup(patientNameTag))
        assertEquals(0x0010, DicomTag.getElement(patientNameTag))

        val pixelDataTag = DicomTag.PIXEL_DATA
        assertEquals(0x7FE0, DicomTag.getGroup(pixelDataTag))
        assertEquals(0x0010, DicomTag.getElement(pixelDataTag))

        val metaGroupLengthTag = DicomTag.FILE_META_INFORMATION_GROUP_LENGTH
        assertEquals(0x0002, DicomTag.getGroup(metaGroupLengthTag))
        assertEquals(0x0000, DicomTag.getElement(metaGroupLengthTag))
    }

    /**
     * Verifies standard SOP Class and Transfer Syntax UID strings.
     */
    @Test
    fun testStandardUids() {
        assertEquals("1.2.840.10008.1.2.1", DicomTag.UID_EXPLICIT_VR_LITTLE_ENDIAN)
        assertEquals("1.2.840.10008.1.2", DicomTag.UID_IMPLICIT_VR_LITTLE_ENDIAN)
        assertEquals("1.2.840.10008.5.1.4.1.1.104.1", DicomTag.UID_SOP_CLASS_ENCAPSULATED_PDF)
        assertEquals("1.2.840.10008.5.1.4.1.1.77.1.4", DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE)
        assertTrue(DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS.startsWith("1.3.6.1.4.1.59999"))
        assertEquals("CHARTCAM_1_0", DicomTag.IMPLEMENTATION_VERSION_STRING)
    }
}
