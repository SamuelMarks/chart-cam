/**
 * @file ExportImportService.kt
 * Service responsible for exporting and importing the entire application database state.
 * The exported JSON structure is defined by the AppData wrapper class, which encrypts
 * internal states. Imported JSON must conform to this expected AppData schema.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Binary
import com.google.fhir.model.r4.Bundle
import com.google.fhir.model.r4.Device
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Provenance
import com.google.fhir.model.r4.QuestionnaireResponse
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirBinary
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.serialization.encodeToString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * Service responsible for exporting and importing ChartCam data as a secure, encrypted FHIR Bundle.
 *
 * @param database The ChartCam database containing the local resources.
 * @param fileStorage Storage mechanism used for reading and writing binary data (like images).
 * @param cryptoService Service used to encrypt and decrypt the FHIR bundle JSON string.
 */
open class ExportImportService(
    val database: ChartCamDatabase,
    private val fileStorage: FileStorage,
    private val cryptoService: CryptoService = CryptoService(),
) {
    private val fhirJson = FhirR4Json()
    private val fhirRepo = FhirRepository(database)

    /**
     * Exports local FHIR data into an encrypted FHIR Bundle.
     *
     * @param password The password used to encrypt the resulting string.
     * @param exportAll Whether to export all data, or limit it to the data associated with [practitionerId].
     * @param practitionerId An optional practitioner ID to filter the exported data.
     * @return A password-encrypted JSON string representing the exported FHIR Bundle.
     */
    open suspend fun exportData(
        password: String,
        exportAll: Boolean = true,
        practitionerId: String? = null,
    ): String {
        val bundleBuilder = Bundle.Builder(Enumeration(value = Bundle.BundleType.Collection))

        fhirRepo.getAllDevices().forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        fhirRepo.getAllPractitioners().forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        fhirRepo.getAllPatients(exportAll, practitionerId).forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        val encounters =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllEncounters()
            } else {
                fhirRepo.getAllEncounters().filter {
                    it.participant.any { p -> p.individual?.reference?.value == practitionerId }
                }
            }
        encounters.forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }

        val documentReferences =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllDocumentReferences()
            } else {
                val encIds = encounters.mapNotNull { it.id }
                fhirRepo.getAllDocumentReferences().filter { doc ->
                    encIds.contains(
                        doc.context
                            ?.encounter
                            ?.firstOrNull()
                            ?.reference
                            ?.value,
                    )
                }
            }
        documentReferences.forEach { docRef ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = docRef.toBuilder() })
            try {
                val filePath =
                    docRef.content
                        .firstOrNull()
                        ?.attachment
                        ?.url
                        ?.value ?: return@forEach
                val mimeType =
                    docRef.content
                        .firstOrNull()
                        ?.attachment
                        ?.contentType
                        ?.value ?: "image/jpeg"
                val bytes = fileStorage.readImage(filePath)
                val base64Data = bytes.toByteString().base64()
                val fileName = filePath.substringAfterLast("/")
                val binary = createFhirBinary(id = fileName, contentTypeStr = mimeType, base64Data = base64Data)
                bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = binary.toBuilder() })
            } catch (e: Exception) {
            }
        }

        val questionnaireResponses =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllQuestionnaireResponses()
            } else {
                val encIds = encounters.mapNotNull { it.id }
                fhirRepo.getAllQuestionnaireResponses().filter { qr ->
                    encIds.contains(
                        qr.encounter
                            ?.reference
                            ?.value
                            ?.replace("Encounter/", ""),
                    )
                }
            }
        questionnaireResponses.forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }

        val provenances =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllProvenances()
            } else {
                // Very simplified for export
                fhirRepo.getAllProvenances()
            }
        provenances.forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }

        val bundle = bundleBuilder.build()
        val jsonData = fhirJson.encodeToString(bundle)
        return cryptoService.encrypt(jsonData, password)
    }

    /**
     * Imports FHIR data from an encrypted JSON string and saves the resources to the local database.
     *
     * Expected Schema:
     * The decrypted string MUST be a valid FHIR R4 `Bundle` JSON object of type `collection` (or similar).
     * The `entry` array should contain `resource` objects. Supported resources for import are:
     * `Device`, `Practitioner`, `Patient`, `Encounter`, `DocumentReference`, `QuestionnaireResponse`,
     * `Provenance`, and `Binary`.
     * `Binary` resources are specifically used to carry raw image bytes (Base64 encoded) and will be
     * decoded and written to the local [FileStorage].
     *
     * @param encryptedData The password-encrypted JSON string representing a FHIR Bundle.
     * @param password The password used to decrypt the data.
     * @throws IllegalArgumentException if the decryption fails, the JSON is malformed, or it is not a valid FHIR Bundle.
     */
    open suspend fun importData(
        encryptedData: String,
        password: String,
    ) {
        val jsonData = cryptoService.decrypt(encryptedData, password)
        if (jsonData.isEmpty()) throw IllegalArgumentException("Decryption failed or data is empty.")
        val bundle = fhirJson.decodeFromString(jsonData) as Bundle

        for (entry in bundle.entry) {
            val resource = entry.resource ?: continue
            when (resource) {
                is Device -> fhirRepo.saveDevice(resource)
                is Practitioner -> fhirRepo.savePractitioner(resource)
                is Patient -> fhirRepo.savePatient(resource)
                is Encounter -> fhirRepo.saveEncounter(resource)
                is DocumentReference -> fhirRepo.saveDocumentReference(resource)
                is QuestionnaireResponse -> fhirRepo.saveQuestionnaireResponse(resource)
                is Provenance -> fhirRepo.saveProvenance(resource)
                is Binary -> {
                    val bytes =
                        resource.data
                            ?.value
                            ?.decodeBase64()
                            ?.toByteArray()
                    if (bytes != null) fileStorage.saveImage(resource.id!!, bytes)
                }
            }
        }
    }
}
