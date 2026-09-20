/**
 * @file DicomExportService.kt
 * Service for exporting clinical photos, reports, and full patient PACS archives as standard DICOM (.dcm) files.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.dicom.FhirToDicomMapper
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.utils.runSuspendCatching
import okio.Buffer

private const val ZIP_LOCAL_HEADER_MAGIC = 0x04034b50L
private const val ZIP_CENTRAL_HEADER_MAGIC = 0x02014b50L
private const val ZIP_END_RECORD_MAGIC = 0x06054b50L
private const val ZIP_VERSION = 20
private const val BYTE_MASK = 0xFF
private const val SHIFT_8 = 8
private const val SHIFT_16 = 16
private const val SHIFT_24 = 24
private const val CRC_POLY = 0xEDB88320L
private const val CRC_INIT = 0xFFFFFFFFL
private const val BITS_PER_BYTE = 8

/**
 * Service for converting FHIR encounters, clinical media, and reports into standard DICOM Part 10 payloads.
 *
 * @param database The application's encrypted SQLDelight database.
 * @param fileStorage Storage manager used to read captured photos.
 */
open class DicomExportService(
    val database: ChartCamDatabase,
    private val fileStorage: FileStorage,
) {
    private val fhirRepo = FhirRepository(database)

    /**
     * Extracts local image file path from a DocumentReference.
     *
     * @param docRef The FHIR DocumentReference.
     * @return Path string or null.
     */
    private fun extractFilePath(docRef: DocumentReference): String? {
        val contents = docRef.content
        val url = if (contents.isNotEmpty()) contents[0].attachment.url else null
        return if (url != null) url.value else null
    }

    /**
     * Extracts referenced Encounter ID from DocumentReference.
     *
     * @param docRef The FHIR DocumentReference.
     * @return Clean Encounter ID or null.
     */
    private fun extractEncounterId(docRef: DocumentReference): String? {
        val ctx = docRef.context ?: return null
        val encounters = ctx.encounter
        val ref = if (encounters.isNotEmpty()) encounters[0].reference else null
        val v = if (ref != null) ref.value else null
        return if (v != null) v.removePrefix("Encounter/") else null
    }

    /**
     * Extracts referenced Patient ID from DocumentReference.
     *
     * @param docRef The FHIR DocumentReference.
     * @return Clean Patient ID or null.
     */
    private fun extractPatientId(docRef: DocumentReference): String? {
        val subj = docRef.subject ?: return null
        val ref = subj.reference
        val v = if (ref != null) ref.value else null
        return if (v != null) v.removePrefix("Patient/") else null
    }

    /**
     * Extracts referenced Patient ID from Encounter.
     *
     * @param encounter The FHIR Encounter.
     * @return Clean Patient ID or null.
     */
    private fun extractEncounterPatientId(encounter: Encounter?): String? {
        val subj = encounter?.subject
        val ref = if (subj != null) subj.reference else null
        val v = if (ref != null) ref.value else null
        return if (v != null) v.removePrefix("Patient/") else null
    }

    /**
     * Extracts referenced Practitioner ID from Encounter.
     *
     * @param encounter The FHIR Encounter.
     * @return Clean Practitioner ID or null.
     */
    private fun extractPractitionerId(encounter: Encounter?): String? {
        val participants = encounter?.participant
        val ind = if (!participants.isNullOrEmpty()) participants[0].individual else null
        val ref = if (ind != null) ind.reference else null
        val v = if (ref != null) ref.value else null
        return if (v != null) v.removePrefix("Practitioner/") else null
    }

    /**
     * Exports a single captured photo as a standard DICOM Visible Light Photographic Image.
     *
     * @param documentRefId The ID of the FHIR DocumentReference representing the clinical image.
     * @param anonymize True to de-identify patient demographic fields.
     * @return A [Result] containing the complete DICOM file bytes, or null if the photo or
     * document reference was not found.
     */
    open suspend fun exportPhotoAsDicom(
        documentRefId: String,
        anonymize: Boolean = false,
    ): Result<ByteArray?> {
        val docRef = fhirRepo.getDocumentReference(documentRefId)
        val filePath = if (docRef != null) extractFilePath(docRef) else null
        val imageBytes = if (filePath != null) fileStorage.readImage(filePath) else ByteArray(0)
        if (docRef == null || imageBytes.isEmpty()) {
            return Result.success(null)
        }

        val encounterId = extractEncounterId(docRef)
        val encounter = if (encounterId != null) fhirRepo.getEncounter(encounterId) else null

        val patientId = extractPatientId(docRef)
        val patient = if (patientId != null) fhirRepo.getPatient(patientId) else null

        val practitionerId = extractPractitionerId(encounter)
        val practitioner = if (practitionerId != null) fhirRepo.getPractitioner(practitionerId) else null

        return FhirToDicomMapper
            .createVisibleLightImageDicom(
                imageBytes = imageBytes,
                imageId = documentRefId,
                patient = patient,
                encounter = encounter,
                practitioner = practitioner,
                anonymize = anonymize,
            )
    }

    /**
     * Encapsulates a clinical PDF report into a standard DICOM Part 10 Encapsulated PDF document.
     *
     * @param encounterId The ID of the clinical Encounter.
     * @param pdfBytes The raw PDF bytes.
     * @param title The report title.
     * @param anonymize True to de-identify patient demographic fields.
     * @return A [Result] enclosing the complete DICOM file bytes.
     */
    open suspend fun exportEncounterReportAsDicomPdf(
        encounterId: String,
        pdfBytes: ByteArray,
        title: String = "Clinical Encounter Report",
        anonymize: Boolean = false,
    ): Result<ByteArray> {
        val cleanEncounterId = encounterId.removePrefix("Encounter/")
        val encounter = fhirRepo.getEncounter(cleanEncounterId)
        val patientId = extractEncounterPatientId(encounter)
        val patient = if (patientId != null) fhirRepo.getPatient(patientId) else null

        val practitionerId = extractPractitionerId(encounter)
        val practitioner = if (practitionerId != null) fhirRepo.getPractitioner(practitionerId) else null

        return FhirToDicomMapper.createEncapsulatedPdfDicom(
            pdfBytes = pdfBytes,
            title = title,
            patient = patient,
            encounter = encounter,
            practitioner = practitioner,
            anonymize = anonymize,
        )
    }

    /**
     * Compiles all clinical photos and documents for a patient into a standardized ZIP archive of DICOM files.
     *
     * @param patientId The FHIR ID of the target patient.
     * @param anonymize True to de-identify patient demographic fields.
     * @return A [Result] enclosing the ZIP archive binary payload containing all generated .dcm files.
     */
    open suspend fun exportPatientDicomArchive(
        patientId: String,
        anonymize: Boolean = false,
    ): Result<ByteArray> =
        runSuspendCatching {
            val files = mutableMapOf<String, ByteArray>()
            val cleanPatientId = patientId.removePrefix("Patient/")
            val encounters =
                fhirRepo.getAllEncounters().filter {
                    val ref = extractEncounterPatientId(it)
                    ref == cleanPatientId
                }
            val encIds = encounters.mapNotNull { it.id?.removePrefix("Encounter/") }.toSet()

            val docRefs =
                fhirRepo.getAllDocumentReferences().filter { doc ->
                    val refEnc = extractEncounterId(doc) ?: return@filter false
                    encIds.contains(refEnc)
                }

            docRefs.forEach { doc ->
                val docId = doc.id.orEmpty()
                val dicomBytes = exportPhotoAsDicom(docId, anonymize).getOrElse { null }
                if (dicomBytes != null) {
                    files["photo_$docId.dcm"] = dicomBytes
                }
            }

            createZipArchive(files)
        }

    /**
     * Creates a standard uncompressed ZIP archive containing the specified files.
     *
     * @param entries Map of filename to file content bytes.
     * @return A valid ZIP archive as a ByteArray.
     */
    fun createZipArchive(entries: Map<String, ByteArray>): ByteArray {
        val zipBuffer = Buffer()
        val centralDirBuffer = Buffer()
        var offset = 0L

        entries.forEach { (name, data) ->
            val nameBytes = name.encodeToByteArray()
            val crc = computeCrc32(data)
            val size = data.size.toLong()

            // Local File Header
            writeLongLe(zipBuffer, ZIP_LOCAL_HEADER_MAGIC)
            writeShortLe(zipBuffer, ZIP_VERSION)
            writeShortLe(zipBuffer, 0) // Flags
            writeShortLe(zipBuffer, 0) // Compression: 0 (Stored)
            writeShortLe(zipBuffer, 0) // Time
            writeShortLe(zipBuffer, 0) // Date
            writeLongLe(zipBuffer, crc)
            writeLongLe(zipBuffer, size)
            writeLongLe(zipBuffer, size)
            writeShortLe(zipBuffer, nameBytes.size)
            writeShortLe(zipBuffer, 0) // Extra field length
            zipBuffer.write(nameBytes)
            zipBuffer.write(data)

            // Central Directory Header
            writeLongLe(centralDirBuffer, ZIP_CENTRAL_HEADER_MAGIC)
            writeShortLe(centralDirBuffer, ZIP_VERSION) // Version made by
            writeShortLe(centralDirBuffer, ZIP_VERSION) // Version needed
            writeShortLe(centralDirBuffer, 0) // Flags
            writeShortLe(centralDirBuffer, 0) // Compression
            writeShortLe(centralDirBuffer, 0) // Time
            writeShortLe(centralDirBuffer, 0) // Date
            writeLongLe(centralDirBuffer, crc)
            writeLongLe(centralDirBuffer, size)
            writeLongLe(centralDirBuffer, size)
            writeShortLe(centralDirBuffer, nameBytes.size)
            writeShortLe(centralDirBuffer, 0) // Extra field length
            writeShortLe(centralDirBuffer, 0) // Comment length
            writeShortLe(centralDirBuffer, 0) // Disk start
            writeShortLe(centralDirBuffer, 0) // Internal attributes
            writeLongLe(centralDirBuffer, 0) // External attributes
            writeLongLe(centralDirBuffer, offset) // Relative offset of local header
            centralDirBuffer.write(nameBytes)

            offset = zipBuffer.size
        }

        val centralDirSize = centralDirBuffer.size
        val centralDirOffset = offset

        // Append Central Directory to zipBuffer
        zipBuffer.write(centralDirBuffer.readByteArray())

        // End of Central Directory Record
        writeLongLe(zipBuffer, ZIP_END_RECORD_MAGIC)
        writeShortLe(zipBuffer, 0) // Disk number
        writeShortLe(zipBuffer, 0) // Central dir disk
        writeShortLe(zipBuffer, entries.size) // Number of central dir records on this disk
        writeShortLe(zipBuffer, entries.size) // Total number of central dir records
        writeLongLe(zipBuffer, centralDirSize)
        writeLongLe(zipBuffer, centralDirOffset)
        writeShortLe(zipBuffer, 0) // Comment length

        return zipBuffer.readByteArray()
    }

    /**
     * Computes the CRC-32 checksum of a byte array.
     *
     * @param data The input bytes.
     * @return The 32-bit CRC checksum as an unsigned Long.
     */
    fun computeCrc32(data: ByteArray): Long {
        var crc = CRC_INIT
        for (b in data) {
            val byteVal = (b.toInt() and BYTE_MASK).toLong()
            var temp = (crc xor byteVal) and BYTE_MASK.toLong()
            repeat(BITS_PER_BYTE) {
                temp = if ((temp and 1L) == 1L) (temp ushr 1) xor CRC_POLY else (temp ushr 1)
            }
            crc = (crc ushr BITS_PER_BYTE) xor temp
        }
        return crc xor CRC_INIT
    }

    /**
     * Writes a 16-bit integer in little-endian order to the buffer.
     *
     * @param buffer The target buffer.
     * @param value The integer value.
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
     * @param value The long value.
     */
    private fun writeLongLe(
        buffer: Buffer,
        value: Long,
    ) {
        buffer.writeByte((value and BYTE_MASK.toLong()).toInt())
        buffer.writeByte(((value ushr SHIFT_8) and BYTE_MASK.toLong()).toInt())
        buffer.writeByte(((value ushr SHIFT_16) and BYTE_MASK.toLong()).toInt())
        buffer.writeByte(((value ushr SHIFT_24) and BYTE_MASK.toLong()).toInt())
    }
}
