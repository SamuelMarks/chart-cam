/**
 * @file FhirToDicomMapper.kt
 * Maps HL7 FHIR R4 clinical resources (Patient, Encounter, DocumentReference) to standard DICOM Part 10 datasets.
 */
package io.healthplatform.chartcam.dicom

import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import io.healthplatform.chartcam.files.ImageMetadataParser
import okio.Buffer
import kotlin.math.abs

private const val DEFAULT_IMAGE_WIDTH = 1920
private const val DEFAULT_IMAGE_HEIGHT = 1080
private const val BITS_8 = 8
private const val HIGH_BIT_7 = 7
private const val SAMPLES_3 = 3
private const val ANON_ID_LEN = 8
private const val BYTE_TAG_ITEM_0 = 0xFE
private const val BYTE_TAG_ITEM_1 = 0xFF
private const val BYTE_TAG_ITEM_2 = 0x00
private const val BYTE_TAG_ITEM_3 = 0xE0
private const val BYTE_TAG_DELIM_2 = 0xDD
private const val BYTE_ZERO = 0x00
private const val BYTE_MASK = 0xFF
private const val SHIFT_16 = 16
private const val SHIFT_24 = 24

/**
 * Mapper for converting FHIR R4 clinical entities to DICOM elements.
 */
object FhirToDicomMapper {
    /**
     * Joins a list of FHIR string tokens into a single space-separated string.
     *
     * @param names The list of FHIR string wrappers.
     * @return Space-separated combined string.
     */
    fun joinNames(names: List<com.google.fhir.model.r4.String>): kotlin.String {
        val sb = StringBuilder()
        for (n in names) {
            val v = n.value
            if (v != null && v.isNotEmpty()) {
                if (sb.isNotEmpty()) {
                    sb.append(' ')
                }
                sb.append(v)
            }
        }
        return sb.toString()
    }

    /**
     * Extracts MRN identifier string from a Patient resource.
     *
     * @param patient The Patient resource.
     * @return Extracted identifier, or patient ID, or "NO_MRN".
     */
    fun extractMrn(patient: Patient): kotlin.String {
        val found = patient.identifier.firstNotNullOfOrNull { it.value?.value }
        return found ?: patient.id ?: "NO_MRN"
    }

    /**
     * Extracts formatted date of birth from Patient resource.
     *
     * @param patient The Patient resource.
     * @return Extracted birth date string or empty string.
     */
    fun extractBirthDate(patient: Patient): kotlin.String {
        val b = patient.birthDate
        if (b != null) {
            val d = b.value
            if (d != null) {
                return d.toString()
            }
        }
        return ""
    }

    /**
     * Extracts standard administrative gender code from Patient resource.
     *
     * @param patient The Patient resource.
     * @return "M", "F", or "O".
     */
    fun extractGender(patient: Patient): kotlin.String {
        val genderName = patient.gender?.value?.name
        return when (genderName?.lowercase()) {
            "male" -> "M"
            "female" -> "F"
            else -> "O"
        }
    }

    /**
     * Extracts period start time from Encounter resource.
     *
     * @param encounter The Encounter resource.
     * @return Extracted ISO datetime string or empty string.
     */
    fun extractPeriodStart(encounter: Encounter?): kotlin.String {
        val startVal = encounter?.period?.start?.value
        return if (startVal != null) startVal.toString() else ""
    }

    /**
     * Extracts family name from a HumanName record.
     *
     * @param docName The HumanName record.
     * @return Extracted family name or empty string.
     */
    fun extractFamilyName(docName: HumanName?): kotlin.String {
        if (docName != null) {
            val fam = docName.family
            if (fam != null) {
                val v = fam.value
                if (v != null) return v
            }
        }
        return ""
    }

    /**
     * Extracts given names combined into a space-separated string from a HumanName record.
     *
     * @param docName The HumanName record.
     * @return Space-separated given names or empty string.
     */
    fun extractGivenName(docName: HumanName?): kotlin.String {
        if (docName != null) {
            return joinNames(docName.given)
        }
        return ""
    }

    /**
     * Builds a list of standard Patient and Study module DICOM elements from FHIR models.
     *
     * @param patient The FHIR Patient resource, if available.
     * @param encounter The FHIR Encounter resource, if available.
     * @param practitioner The attending FHIR Practitioner, if available.
     * @param anonymize Whether to de-identify patient demographic information per HIPAA safe harbor.
     * @return A list of populated DICOM elements.
     */
    fun buildCommonElements(
        patient: Patient?,
        encounter: Encounter?,
        practitioner: Practitioner?,
        anonymize: Boolean = false,
    ): MutableList<DicomElement> {
        val elements = mutableListOf<DicomElement>()

        // 1. Patient Demographics Module
        if (anonymize || patient == null) {
            val patId = patient?.id
            val anonId =
                if (patId != null) {
                    "ANON-" + abs(patId.hashCode()).toString().take(ANON_ID_LEN)
                } else {
                    "ANONYMOUS"
                }
            elements.add(DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, "ANONYMOUS^PATIENT"))
            elements.add(DicomElement.createString(DicomTag.PATIENT_ID, DicomVR.LO, anonId))
            elements.add(DicomElement.createDate(DicomTag.PATIENT_BIRTH_DATE, "19000101"))
            elements.add(DicomElement.createString(DicomTag.PATIENT_SEX, DicomVR.CS, "O"))
        } else {
            val nameRecord = patient.name.firstOrNull()
            val family = extractFamilyName(nameRecord)
            val given = extractGivenName(nameRecord)
            val formattedName = if (family.isNotEmpty() || given.isNotEmpty()) "$family^$given" else "UNKNOWN"

            val mrn = extractMrn(patient)
            val birthDate = extractBirthDate(patient)
            val genderStr = extractGender(patient)

            elements.add(DicomElement.createString(DicomTag.PATIENT_NAME, DicomVR.PN, formattedName))
            elements.add(DicomElement.createString(DicomTag.PATIENT_ID, DicomVR.LO, mrn))
            if (birthDate.isNotEmpty()) {
                elements.add(DicomElement.createDate(DicomTag.PATIENT_BIRTH_DATE, birthDate))
            }
            elements.add(DicomElement.createString(DicomTag.PATIENT_SEX, DicomVR.CS, genderStr))
        }

        // 2. Study Module (Encounter)
        val encounterId = encounter?.id ?: "ENC_DEFAULT"
        val studyUid = "${DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS}.1.${abs(encounterId.hashCode())}"
        elements.add(DicomElement.createString(DicomTag.STUDY_INSTANCE_UID, DicomVR.UI, studyUid))

        val periodStart = extractPeriodStart(encounter)
        if (periodStart.isNotEmpty()) {
            elements.add(DicomElement.createDate(DicomTag.STUDY_DATE, periodStart))
            elements.add(DicomElement.createTime(DicomTag.STUDY_TIME, periodStart))
            elements.add(DicomElement.createDate(DicomTag.CONTENT_DATE, periodStart))
            elements.add(DicomElement.createTime(DicomTag.CONTENT_TIME, periodStart))
        }

        val docName = if (practitioner != null) practitioner.name.firstOrNull() else null
        val referringDoc =
            if (docName != null) {
                val f = extractFamilyName(docName)
                val g = extractGivenName(docName)
                "$f^$g"
            } else {
                "ChartCam^Provider"
            }
        elements.add(DicomElement.createString(DicomTag.REFERRING_PHYSICIAN_NAME, DicomVR.PN, referringDoc))
        elements.add(DicomElement.createString(DicomTag.MANUFACTURER, DicomVR.LO, "ChartCam Health"))
        elements.add(DicomElement.createString(DicomTag.INSTITUTION_NAME, DicomVR.LO, "Harvard MGH Mass Eye & Ear"))

        return elements
    }

    /**
     * Creates a complete DICOM Part 10 byte stream for an Encapsulated PDF document.
     *
     * @param pdfBytes The raw bytes of the PDF file to encapsulate.
     * @param title The human-readable title of the document.
     * @param patient The FHIR Patient resource, if available.
     * @param encounter The FHIR Encounter resource, if available.
     * @param practitioner The attending FHIR Practitioner, if available.
     * @param anonymize Whether to de-identify patient information.
     * @return A [Result] enclosing the complete Part 10 DICOM file as a ByteArray.
     */
    fun createEncapsulatedPdfDicom(
        pdfBytes: ByteArray,
        title: String = "Clinical Encounter Report",
        patient: Patient? = null,
        encounter: Encounter? = null,
        practitioner: Practitioner? = null,
        anonymize: Boolean = false,
    ): Result<ByteArray> {
        val elements = buildCommonElements(patient, encounter, practitioner, anonymize)

        val encId = encounter?.id ?: "ENC_DEFAULT"
        val seriesUid = "${DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS}.2.${abs(encId.hashCode())}.104"
        val sopInstanceUid = "${DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS}.3.${abs(pdfBytes.contentHashCode())}"

        // Series Module
        elements.add(DicomElement.createString(DicomTag.SERIES_INSTANCE_UID, DicomVR.UI, seriesUid))
        elements.add(DicomElement.createString(DicomTag.MODALITY, DicomVR.CS, "DOC"))
        elements.add(DicomElement.createString(DicomTag.SERIES_DESCRIPTION, DicomVR.LO, "Encapsulated PDF Document"))
        elements.add(DicomElement.createUS(DicomTag.SERIES_NUMBER, 1))

        // SOP Common Module
        elements.add(
            DicomElement.createString(
                DicomTag.SOP_CLASS_UID,
                DicomVR.UI,
                DicomTag.UID_SOP_CLASS_ENCAPSULATED_PDF,
            ),
        )
        elements.add(DicomElement.createString(DicomTag.SOP_INSTANCE_UID, DicomVR.UI, sopInstanceUid))
        elements.add(DicomElement.createUS(DicomTag.INSTANCE_NUMBER, 1))

        // Encapsulated Document Module
        elements.add(DicomElement.createString(DicomTag.DOCUMENT_TITLE, DicomVR.ST, title))
        elements.add(
            DicomElement.createString(
                DicomTag.MIME_TYPE_OF_ENCAPSULATED_DOCUMENT,
                DicomVR.LO,
                "application/pdf",
            ),
        )
        elements.add(DicomElement.createBinary(DicomTag.ENCAPSULATED_DOCUMENT, DicomVR.OB, pdfBytes))

        return runCatching {
            DicomWriter.write(elements, DicomTag.UID_SOP_CLASS_ENCAPSULATED_PDF, sopInstanceUid)
        }
    }

    /**
     * Constructs a DICOM encapsulated sequence containing a basic offset table, fragment item, and sequence delimiter.
     *
     * @param jpegBytes The raw compressed JPEG byte stream.
     * @return Byte array formatted as an encapsulated sequence.
     */
    fun buildEncapsulatedJpegSequence(jpegBytes: ByteArray): ByteArray {
        val pad = if (jpegBytes.size % 2 != 0) 1 else 0
        val fragLen = jpegBytes.size + pad
        val buffer = Buffer()

        // 1. Basic Offset Table: Tag (FFFE,E000), Length 0
        buffer.writeByte(BYTE_TAG_ITEM_0)
        buffer.writeByte(BYTE_TAG_ITEM_1)
        buffer.writeByte(BYTE_TAG_ITEM_2)
        buffer.writeByte(BYTE_TAG_ITEM_3)
        buffer.writeByte(BYTE_ZERO)
        buffer.writeByte(BYTE_ZERO)
        buffer.writeByte(BYTE_ZERO)
        buffer.writeByte(BYTE_ZERO)

        // 2. Fragment Item: Tag (FFFE,E000), Length fragLen in Little Endian
        buffer.writeByte(BYTE_TAG_ITEM_0)
        buffer.writeByte(BYTE_TAG_ITEM_1)
        buffer.writeByte(BYTE_TAG_ITEM_2)
        buffer.writeByte(BYTE_TAG_ITEM_3)
        buffer.writeByte(fragLen and BYTE_MASK)
        buffer.writeByte((fragLen ushr BITS_8) and BYTE_MASK)
        buffer.writeByte((fragLen ushr SHIFT_16) and BYTE_MASK)
        buffer.writeByte((fragLen ushr SHIFT_24) and BYTE_MASK)
        buffer.write(jpegBytes)
        if (pad > 0) {
            buffer.writeByte(BYTE_ZERO)
        }

        // 3. Sequence Delimiter: Tag (FFFE,E0DD), Length 0
        buffer.writeByte(BYTE_TAG_ITEM_0)
        buffer.writeByte(BYTE_TAG_ITEM_1)
        buffer.writeByte(BYTE_TAG_DELIM_2)
        buffer.writeByte(BYTE_TAG_ITEM_3)
        buffer.writeByte(BYTE_ZERO)
        buffer.writeByte(BYTE_ZERO)
        buffer.writeByte(BYTE_ZERO)
        buffer.writeByte(BYTE_ZERO)

        return buffer.readByteArray()
    }

    /**
     * Creates a complete DICOM Part 10 byte stream for a Visible Light (VL) Photographic image.
     *
     * @param imageBytes The raw photo image bytes (JPEG or PNG).
     * @param imageId A unique identifier for the image or DocumentReference.
     * @param patient The FHIR Patient resource, if available.
     * @param encounter The FHIR Encounter resource, if available.
     * @param practitioner The attending FHIR Practitioner, if available.
     * @param anonymize Whether to de-identify patient information.
     * @return A [Result] enclosing the complete Part 10 DICOM file as a ByteArray.
     */
    fun createVisibleLightImageDicom(
        imageBytes: ByteArray,
        imageId: String = "PHOTO_1",
        patient: Patient? = null,
        encounter: Encounter? = null,
        practitioner: Practitioner? = null,
        anonymize: Boolean = false,
    ): Result<ByteArray> =
        runCatching {
            require(imageBytes.isNotEmpty()) { "Image bytes must not be empty" }
            val elements = buildCommonElements(patient, encounter, practitioner, anonymize)

            val encId = encounter?.id ?: "ENC_DEFAULT"
            val seriesUid = "${DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS}.2.${abs(encId.hashCode())}.77"
            val sopInstanceUid = "${DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS}.3.${abs(imageId.hashCode())}"

            val isJpeg =
                imageBytes.size >= 2 &&
                    imageBytes[0] == 0xFF.toByte() &&
                    imageBytes[1] == 0xD8.toByte()

            if (isJpeg) {
                elements.add(
                    DicomElement.createString(
                        DicomTag.TRANSFER_SYNTAX_UID,
                        DicomVR.UI,
                        DicomTag.UID_JPEG_BASELINE,
                    ),
                )
            }

            // Series Module
            elements.add(DicomElement.createString(DicomTag.SERIES_INSTANCE_UID, DicomVR.UI, seriesUid))
            elements.add(DicomElement.createString(DicomTag.MODALITY, DicomVR.CS, "XC")) // External Camera Photography
            elements.add(DicomElement.createString(DicomTag.SERIES_DESCRIPTION, DicomVR.LO, "Clinical Photography"))
            elements.add(DicomElement.createUS(DicomTag.SERIES_NUMBER, 1))

            // SOP Common Module
            elements.add(
                DicomElement.createString(
                    DicomTag.SOP_CLASS_UID,
                    DicomVR.UI,
                    DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE,
                ),
            )
            elements.add(DicomElement.createString(DicomTag.SOP_INSTANCE_UID, DicomVR.UI, sopInstanceUid))
            elements.add(DicomElement.createUS(DicomTag.INSTANCE_NUMBER, 1))

            // Image Metadata & Dimensions
            val metadata = ImageMetadataParser.parse(imageBytes)
            val rows = metadata.height ?: DEFAULT_IMAGE_HEIGHT
            val cols = metadata.width ?: DEFAULT_IMAGE_WIDTH

            // Image Pixel Module
            elements.add(DicomElement.createUS(DicomTag.SAMPLES_PER_PIXEL, SAMPLES_3))
            val photometric = if (isJpeg) "YBR_FULL_422" else "RGB"
            elements.add(DicomElement.createString(DicomTag.PHOTOMETRIC_INTERPRETATION, DicomVR.CS, photometric))
            elements.add(DicomElement.createUS(DicomTag.ROWS, rows))
            elements.add(DicomElement.createUS(DicomTag.COLUMNS, cols))
            elements.add(DicomElement.createUS(DicomTag.BITS_ALLOCATED, BITS_8))
            elements.add(DicomElement.createUS(DicomTag.BITS_STORED, BITS_8))
            elements.add(DicomElement.createUS(DicomTag.HIGH_BIT, HIGH_BIT_7))
            elements.add(DicomElement.createUS(DicomTag.PIXEL_REPRESENTATION, 0)) // 0 = unsigned

            if (isJpeg) {
                val encapsulatedSeq = buildEncapsulatedJpegSequence(imageBytes)
                elements.add(DicomElement.createEncapsulatedPixelData(DicomTag.PIXEL_DATA, encapsulatedSeq))
            } else {
                elements.add(DicomElement.createBinary(DicomTag.PIXEL_DATA, DicomVR.OB, imageBytes))
            }

            DicomWriter.write(elements, DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE, sopInstanceUid)
        }
}
