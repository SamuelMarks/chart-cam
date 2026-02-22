package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.utils.CryptoService
import io.healthplatform.chartcam.models.createFhirBinary
import com.google.fhir.model.r4.*
import com.google.fhir.model.r4.String as FhirString
import com.google.fhir.model.r4.Boolean as FhirBoolean
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import okio.ByteString.Companion.decodeBase64

/**
 * Service to export and import clinical data securely in FHIR standard format.
 * Generates a FHIR Bundle containing all patients, practitioners, encounters,
 * document references, questionnaire responses, provenances, devices, and binaries.
 */
class ExportImportService(
    val database: ChartCamDatabase,
    private val fileStorage: FileStorage,
    private val cryptoService: CryptoService = CryptoService()
) {
    /** The JSON configured for FHIR R4 standard serialization */
    private val fhirJson = FhirR4Json()

    /**
     * Exports all the data in the local database to an encrypted FHIR Bundle.
     * @param password Password to encrypt the bundle.
     * @return Encrypted string.
     */
    suspend fun exportData(password: kotlin.String): kotlin.String {
        val queries = database.chartCamQueries
        
        val bundleBuilder = Bundle.Builder(Enumeration(value = Bundle.BundleType.Collection))

        // 1. Devices
        queries.getAllDevices().awaitAsList().forEach { entity ->
            val resource = fhirJson.decodeFromString(entity.serializedResource) as Device
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        
        // 2. Practitioners
        queries.getAllPractitioners().awaitAsList().forEach { entity ->
            val resource = fhirJson.decodeFromString(entity.serializedResource) as Practitioner
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        
        // 3. Patients
        queries.getAllPatients().awaitAsList().forEach { entity ->
            val resource = fhirJson.decodeFromString(entity.serializedResource) as Patient
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        
        // 4. Encounters
        queries.getAllEncounters().awaitAsList().forEach { entity ->
            val resource = fhirJson.decodeFromString(entity.serializedResource) as Encounter
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        
        // 5. DocumentReferences & Binaries
        queries.getAllDocumentReferences().awaitAsList().forEach { entity ->
            val docRef = fhirJson.decodeFromString(entity.serializedResource) as DocumentReference
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = docRef.toBuilder() })
            
            try {
                val bytes = fileStorage.readImage(entity.filePath)
                val base64Data = bytes.toByteString().base64()
                val fileName = entity.filePath.substringAfterLast("/")
                val binary = createFhirBinary(
                    id = fileName, // We use fileName as ID so we know which file it is
                    contentTypeStr = entity.mimeType,
                    base64Data = base64Data
                )
                bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = binary.toBuilder() })
            } catch (e: Exception) {
                // Ignore missing files for now
            }
        }
        
        // 6. Questionnaire Responses
        queries.getAllQuestionnaireResponses().awaitAsList().forEach { entity ->
            val resource = fhirJson.decodeFromString(entity.serializedResource) as QuestionnaireResponse
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }

        // 7. Provenances
        queries.getAllProvenances().awaitAsList().forEach { entity ->
            val resource = fhirJson.decodeFromString(entity.serializedResource) as Provenance
            bundleBuilder.entry.add(Bundle.Entry.Builder().apply { this.resource = resource.toBuilder() })
        }
        
        val bundle = bundleBuilder.build()
        val jsonData = fhirJson.encodeToString(bundle)
        return cryptoService.encrypt(jsonData, password)
    }

    /**
     * Imports data from an encrypted FHIR Bundle.
     * @param encryptedData The encrypted data payload.
     * @param password Password to decrypt.
     */
    suspend fun importData(encryptedData: kotlin.String, password: kotlin.String) {
        val jsonData = cryptoService.decrypt(encryptedData, password)
        if (jsonData.isEmpty()) {
            throw IllegalArgumentException("Decryption failed or data is empty.")
        }
        val bundle = fhirJson.decodeFromString(jsonData) as Bundle
        val queries = database.chartCamQueries
        
        for (entry in bundle.entry) {
            val resource = entry.resource ?: continue
            val serialized = fhirJson.encodeToString(resource)
            
            when (resource) {
                is Device -> queries.insertDevice(resource.id!!, serialized)
                is Practitioner -> queries.insertPractitioner(resource.id!!, resource.name.firstOrNull()?.family?.value ?: "", resource.name.firstOrNull()?.given?.firstOrNull()?.value ?: "", resource.active?.value ?: true, serialized)
                is Patient -> {
                    val family = resource.name.firstOrNull()?.family?.value ?: ""
                    val given = resource.name.firstOrNull()?.given?.firstOrNull()?.value ?: ""
                    val dob = resource.birthDate?.value?.toString() ?: ""
                    val mrn = resource.identifier.firstOrNull()?.value?.value ?: ""
                    val gender = resource.gender?.value?.name ?: ""
                    queries.insertPatient(resource.id!!, family, given, dob, mrn, gender, null, serialized)
                }
                is Encounter -> {
                    val patientId = resource.subject?.reference?.value ?: ""
                    val practitionerId = resource.participant.firstOrNull()?.individual?.reference?.value ?: ""
                    val dateStr = resource.period?.start?.value?.toString() ?: ""
                    val status = resource.status?.value?.name ?: ""
                    queries.insertEncounter(resource.id!!, patientId, practitionerId, dateStr, status, serialized)
                }
                is DocumentReference -> {
                    val encounterId = resource.context?.encounter?.firstOrNull()?.reference?.value ?: ""
                    val patientId = resource.subject?.reference?.value ?: ""
                    val type = "photo"
                    val category = "clinical-photography"
                    val date = resource.date?.value?.toString() ?: ""
                    val filePath = resource.content.firstOrNull()?.attachment?.url?.value ?: ""
                    val mimeType = resource.content.firstOrNull()?.attachment?.contentType?.value ?: "image/jpeg"
                    queries.insertDocumentReference(resource.id!!, encounterId, patientId, type, category, date, null, filePath, mimeType, serialized)
                }
                is QuestionnaireResponse -> {
                    val encounterId = resource.encounter?.reference?.value?.replace("Encounter/", "") ?: ""
                    val patientId = resource.subject?.reference?.value?.replace("Patient/", "") ?: ""
                    val questionnaireId = resource.questionnaire?.value?.replace("Questionnaire/", "") ?: ""
                    val date = resource.authored?.value?.toString() ?: ""
                    queries.insertQuestionnaireResponse(resource.id!!, encounterId, patientId, questionnaireId, date, serialized)
                }
                is Provenance -> {
                    val targetId = resource.target.firstOrNull()?.reference?.value ?: ""
                    val dateStr = resource.recorded?.value?.toString() ?: ""
                    queries.insertProvenance(resource.id!!, targetId, null, dateStr, serialized)
                }
                is Binary -> {
                    // Extract and save binary file
                    val bytes = resource.data?.value?.decodeBase64()?.toByteArray()
                    if (bytes != null) {
                        fileStorage.saveImage(resource.id!!, bytes)
                    }
                }
                else -> { /* Ignore other resources for now */ }
            }
        }
    }
}
