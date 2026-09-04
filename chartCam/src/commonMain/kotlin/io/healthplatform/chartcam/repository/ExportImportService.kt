@file:Suppress("TooGenericExceptionCaught")
/**
 * @file ExportImportService.kt
 * Contains declarations for ExportImportService.kt.
 *
 * Provides functionality to export and import FHIR resources and associated binaries
 * (like photos) to and from a password-encrypted JSON payload.
 */

package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.Binary
import com.google.fhir.model.r4.Bundle
import com.google.fhir.model.r4.Device
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Provenance
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirBinary
import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.serialization.encodeToString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

private const val MIN_PASSWORD_LENGTH = 6

/**
 * Service responsible for creating a full or partial export of local FHIR data into
 * an encrypted Bundle, and conversely importing an encrypted Bundle into the local database.
 *
 */
open class ExportImportService(
    /** The underlying database. */
    val database: ChartCamDatabase,
    /** The storage manager for reading/writing raw image files. */
    private val fileStorage: FileStorage,
) {
    private val fhirRepo = FhirRepository(database)
    private val cryptoService = CryptoService()
    private val fhirJson =
        com.google.fhir.model.r4
            .FhirR4Json()

    /**
     * Exports local FHIR resources and associated binaries to an encrypted JSON string.
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
        addBaseResources(bundleBuilder)
        addQuestionnaires(bundleBuilder)
        addPatients(bundleBuilder, exportAll, practitionerId)
        val encounters = addEncounters(bundleBuilder, exportAll, practitionerId)
        addDocumentReferences(bundleBuilder, exportAll, practitionerId, encounters)
        addQuestionnaireResponses(bundleBuilder, exportAll, practitionerId, encounters)
        addProvenances(bundleBuilder, exportAll, practitionerId)

        val bundle = bundleBuilder.build()
        val jsonData = fhirJson.encodeToString(bundle)
        return cryptoService.encrypt(jsonData, password)
    }

    /**
     * Helper for exporting Questionnaires.
     * @param bundleBuilder The bundleBuilder.
     */
    private suspend fun addQuestionnaires(bundleBuilder: Bundle.Builder) {
        fhirRepo.getAllQuestionnaires().forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
    }

    /**
     * Helper for exporting.
     * @param bundleBuilder The bundleBuilder.
     */
    private suspend fun addBaseResources(bundleBuilder: Bundle.Builder) {
        fhirRepo.getAllDevices().forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        fhirRepo.getAllPractitioners().forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
    }

    /**
     * Helper for exporting.
     * @param bundleBuilder The bundleBuilder.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     */
    private suspend fun addPatients(
        bundleBuilder: Bundle.Builder,
        exportAll: Boolean,
        practitionerId: String?,
    ) {
        val patients =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllPatients()
            } else {
                fhirRepo.getAllPatients().filter {
                    it.managingOrganization
                        ?.reference
                        ?.value
                        ?.contains(practitionerId) == true
                }
            }
        patients.forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
    }

    /**
     * Helper for exporting.
     * @param bundleBuilder The bundleBuilder.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     * @return The result.
     */
    private suspend fun addEncounters(
        bundleBuilder: Bundle.Builder,
        exportAll: Boolean,
        practitionerId: String?,
    ): List<Encounter> {
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
        return encounters
    }

    /**
     * Helper for exporting.
     * @param bundleBuilder The bundleBuilder.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     * @param encounters The encounters.
     */
    private suspend fun addDocumentReferences(
        bundleBuilder: Bundle.Builder,
        exportAll: Boolean,
        practitionerId: String?,
        encounters: List<Encounter>,
    ) {
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
        documentReferences.forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
            try {
                val filePath =
                    resource.content
                        .firstOrNull()
                        ?.attachment
                        ?.url
                        ?.value ?: return@forEach
                val bytes = fileStorage.readImage(filePath)
                val base64Data = bytes.toByteString().base64()
                val mimeType =
                    resource.content
                        .firstOrNull()
                        ?.attachment
                        ?.contentType
                        ?.value ?: "image/jpeg"
                val fileName = filePath.substringAfterLast("/")
                val binary = createFhirBinary(id = fileName, contentTypeStr = mimeType, base64Data = base64Data)
                bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = binary.toBuilder() })
            } catch (e: Exception) {
                println("Failed to export binary: ${e.message}")
            }
        }
    }

    /**
     * Helper for exporting.
     * @param bundleBuilder The bundleBuilder.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     * @param encounters The encounters.
     */
    private suspend fun addQuestionnaireResponses(
        bundleBuilder: Bundle.Builder,
        exportAll: Boolean,
        practitionerId: String?,
        encounters: List<Encounter>,
    ) {
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
    }

    /**
     * Helper for exporting.
     * @param bundleBuilder The bundleBuilder.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     */
    private suspend fun addProvenances(
        bundleBuilder: Bundle.Builder,
        exportAll: Boolean,
        practitionerId: String?,
    ) {
        val provenances =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllProvenances()
            } else {
                fhirRepo.getAllProvenances()
            }
        provenances.forEach { resource ->
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
    }

    /**
     * Validates whether a password meets the required decryption strength criteria.
     *
     * @param password The password string to evaluate.
     * @return True if valid (non-blank and at least 6 characters), false otherwise.
     */
    fun isValidPassword(password: String): Boolean = password.isNotBlank() && password.length >= MIN_PASSWORD_LENGTH

    /**
     * Decrypts the provided JSON string and imports the contained FHIR Bundle into the local database.
     * Supported resources include `Device`, `Practitioner`, `Patient`, `Encounter`, `DocumentReference`,
     * `Questionnaire`, `QuestionnaireResponse`, `Provenance`, and `Binary`.
     * `Binary` resources are specifically used to carry raw image bytes (Base64 encoded) and will be
     * decoded and written to the local [FileStorage].
     *
     * @param encryptedData The password-encrypted JSON string representing a FHIR Bundle.
     * @param password The password used to decrypt the data.
     * @throws IllegalArgumentException if the decryption fails, the JSON is malformed,
     * or it is not a valid FHIR Bundle.
     */
    open suspend fun importData(
        encryptedData: String,
        password: String,
    ) {
        require(isValidPassword(password)) {
            "Decryption password must not be empty or weak (minimum 6 characters)."
        }
        val jsonData = cryptoService.decrypt(encryptedData, password)
        require(jsonData.isNotEmpty()) { "Decryption failed or data is empty." }
        val bundle = fhirJson.decodeFromString(jsonData) as Bundle

        for (entry in bundle.entry) {
            val resource = entry.resource ?: continue
            try {
                importResource(resource)
            } catch (e: Exception) {
                println("Skipping malformed resource ${resource.id ?: "unknown"}: ${e.message}")
            }
        }
    }

    /**
     * Exports data to a temporary encrypted payload and executes an operation,
     * guaranteeing complete cleanup of temporary cache files in both success and failure execution paths.
     *
     * @param T The return type of the executed block.
     * @param password The password for encryption.
     * @param block The block to execute with the encrypted data.
     * @return The result of the block execution.
     */
    open suspend fun <T> executeWithTemporaryCleanup(
        password: String,
        block: suspend (encryptedData: String) -> T,
    ): T =
        try {
            val data = exportData(password)
            block(data)
        } finally {
            fileStorage.clearCache()
        }

    /**
     * Imports data from an encrypted archive, guaranteeing complete cleanup of
     * temporary cache files on completion or error.
     *
     * @param encryptedData The encrypted archive string.
     * @param password The decryption password.
     */
    open suspend fun importDataWithCleanup(
        encryptedData: String,
        password: String,
    ) {
        try {
            importData(encryptedData, password)
        } finally {
            fileStorage.clearCache()
        }
    }

    /**
     * Helper for importing a specific resource.
     * @param resource The resource.
     */
    private suspend fun importResource(resource: com.google.fhir.model.r4.Resource) {
        when (resource) {
            is Device -> fhirRepo.saveDevice(resource)
            is Practitioner -> fhirRepo.savePractitioner(resource)
            is Patient -> fhirRepo.savePatient(resource)
            is Encounter -> fhirRepo.saveEncounter(resource)
            is DocumentReference -> fhirRepo.saveDocumentReference(resource)
            is Questionnaire -> fhirRepo.saveQuestionnaire(resource)
            is QuestionnaireResponse -> fhirRepo.saveQuestionnaireResponse(resource)
            is Provenance -> fhirRepo.saveProvenance(resource)
            is Binary -> {
                val bytes =
                    resource.data
                        ?.value
                        ?.let { it.decodeBase64()?.toByteArray() }
                val binaryId = resource.id
                if (bytes != null && !binaryId.isNullOrBlank()) fileStorage.saveImage(binaryId, bytes)
            }
            else -> {
                // Ignore unknown resources for now
            }
        }
    }
}
