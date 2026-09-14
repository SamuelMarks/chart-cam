/**
 * @file DicomTag.kt
 * Standard DICOM Part 6 Data Dictionary tag definitions and UID constants.
 */
package io.healthplatform.chartcam.dicom

/**
 * Standard DICOM Part 6 Data Dictionary tag constants and standard UID identifiers.
 */
object DicomTag {
    /** Tag (0002,0000): File Meta Information Group Length. */
    const val FILE_META_INFORMATION_GROUP_LENGTH: Int = 0x00020000

    /** Tag (0002,0001): File Meta Information Version. */
    const val FILE_META_INFORMATION_VERSION: Int = 0x00020001

    /** Tag (0002,0002): Media Storage SOP Class UID. */
    const val MEDIA_STORAGE_SOP_CLASS_UID: Int = 0x00020002

    /** Tag (0002,0003): Media Storage SOP Instance UID. */
    const val MEDIA_STORAGE_SOP_INSTANCE_UID: Int = 0x00020003

    /** Tag (0002,0010): Transfer Syntax UID. */
    const val TRANSFER_SYNTAX_UID: Int = 0x00020010

    /** Tag (0002,0012): Implementation Class UID. */
    const val IMPLEMENTATION_CLASS_UID: Int = 0x00020012

    /** Tag (0002,0013): Implementation Version Name. */
    const val IMPLEMENTATION_VERSION_NAME: Int = 0x00020013

    /** Tag (0008,0016): SOP Class UID. */
    const val SOP_CLASS_UID: Int = 0x00080016

    /** Tag (0008,0018): SOP Instance UID. */
    const val SOP_INSTANCE_UID: Int = 0x00080018

    /** Tag (0008,0020): Study Date. */
    const val STUDY_DATE: Int = 0x00080020

    /** Tag (0008,0023): Content Date. */
    const val CONTENT_DATE: Int = 0x00080023

    /** Tag (0008,0030): Study Time. */
    const val STUDY_TIME: Int = 0x00080030

    /** Tag (0008,0033): Content Time. */
    const val CONTENT_TIME: Int = 0x00080033

    /** Tag (0008,0050): Accession Number. */
    const val ACCESSION_NUMBER: Int = 0x00080050

    /** Tag (0008,0060): Modality. */
    const val MODALITY: Int = 0x00080060

    /** Tag (0008,0070): Manufacturer. */
    const val MANUFACTURER: Int = 0x00080070

    /** Tag (0008,0080): Institution Name. */
    const val INSTITUTION_NAME: Int = 0x00080080

    /** Tag (0008,0090): Referring Physician's Name. */
    const val REFERRING_PHYSICIAN_NAME: Int = 0x00080090

    /** Tag (0008,1030): Study Description. */
    const val STUDY_DESCRIPTION: Int = 0x00081030

    /** Tag (0008,103E): Series Description. */
    const val SERIES_DESCRIPTION: Int = 0x0008103E

    /** Tag (0010,0010): Patient's Name. */
    const val PATIENT_NAME: Int = 0x00100010

    /** Tag (0010,0020): Patient ID (MRN). */
    const val PATIENT_ID: Int = 0x00100020

    /** Tag (0010,0030): Patient's Birth Date. */
    const val PATIENT_BIRTH_DATE: Int = 0x00100030

    /** Tag (0010,0040): Patient's Sex. */
    const val PATIENT_SEX: Int = 0x00100040

    /** Tag (0020,000D): Study Instance UID. */
    const val STUDY_INSTANCE_UID: Int = 0x0020000D

    /** Tag (0020,000E): Series Instance UID. */
    const val SERIES_INSTANCE_UID: Int = 0x0020000E

    /** Tag (0020,0011): Series Number. */
    const val SERIES_NUMBER: Int = 0x00200011

    /** Tag (0020,0013): Instance Number. */
    const val INSTANCE_NUMBER: Int = 0x00200013

    /** Tag (0028,0002): Samples per Pixel. */
    const val SAMPLES_PER_PIXEL: Int = 0x00280002

    /** Tag (0028,0004): Photometric Interpretation. */
    const val PHOTOMETRIC_INTERPRETATION: Int = 0x00280004

    /** Tag (0028,0010): Rows. */
    const val ROWS: Int = 0x00280010

    /** Tag (0028,0011): Columns. */
    const val COLUMNS: Int = 0x00280011

    /** Tag (0028,0100): Bits Allocated. */
    const val BITS_ALLOCATED: Int = 0x00280100

    /** Tag (0028,0101): Bits Stored. */
    const val BITS_STORED: Int = 0x00280101

    /** Tag (0028,0102): High Bit. */
    const val HIGH_BIT: Int = 0x00280102

    /** Tag (0028,0103): Pixel Representation. */
    const val PIXEL_REPRESENTATION: Int = 0x00280103

    /** Tag (0042,0010): Document Title. */
    const val DOCUMENT_TITLE: Int = 0x00420010

    /** Tag (0042,0011): Encapsulated Document. */
    const val ENCAPSULATED_DOCUMENT: Int = 0x00420011

    /** Tag (0042,0012): MIME Type of Encapsulated Document. */
    const val MIME_TYPE_OF_ENCAPSULATED_DOCUMENT: Int = 0x00420012

    /** Tag (7FE0,0010): Pixel Data. */
    const val PIXEL_DATA: Int = 0x7FE00010

    /** Explicit VR Little Endian Transfer Syntax UID. */
    const val UID_EXPLICIT_VR_LITTLE_ENDIAN: String = "1.2.840.10008.1.2.1"

    /** Implicit VR Little Endian Transfer Syntax UID. */
    const val UID_IMPLICIT_VR_LITTLE_ENDIAN: String = "1.2.840.10008.1.2"

    /** Encapsulated PDF Storage SOP Class UID. */
    const val UID_SOP_CLASS_ENCAPSULATED_PDF: String = "1.2.840.10008.5.1.4.1.1.104.1"

    /** Visible Light Photographic Image Storage SOP Class UID. */
    const val UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE: String = "1.2.840.10008.5.1.4.1.1.77.1.4"

    /** Secondary Capture Image Storage SOP Class UID. */
    const val UID_SOP_CLASS_SECONDARY_CAPTURE: String = "1.2.840.10008.5.1.4.1.1.7"

    /** ChartCam Implementation Class UID root. */
    const val UID_CHARTCAM_IMPLEMENTATION_CLASS: String = "1.3.6.1.4.1.59999.1.0"

    /** ChartCam Implementation Version Name. */
    const val IMPLEMENTATION_VERSION_STRING: String = "CHARTCAM_1_0"

    private const val SHIFT_16 = 16
    private const val MASK_16 = 0xFFFF

    /**
     * Extracts the 16-bit group number from a composite 32-bit tag integer.
     *
     * @param tag The 32-bit composite DICOM tag.
     * @return The 16-bit group number.
     */
    fun getGroup(tag: Int): Int = (tag ushr SHIFT_16) and MASK_16

    /**
     * Extracts the 16-bit element number from a composite 32-bit tag integer.
     *
     * @param tag The 32-bit composite DICOM tag.
     * @return The 16-bit element number.
     */
    fun getElement(tag: Int): Int = tag and MASK_16
}
