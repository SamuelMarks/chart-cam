package io.healthplatform.chartcam.repository

import app.cash.sqldelight.db.SqlDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.google.fhir.model.r4.Practitioner

import com.google.fhir.model.r4.Device
import com.google.fhir.model.r4.Provenance

import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Enumeration
import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.models.mrn

/**
 * Repository responsible for CRUD operations on FHIR resources persisted locally.
 * It manages [Practitioner], [Patient], [Encounter], [DocumentReference], and [QuestionnaireResponse] resources.
 * Each resource is stored locally inside a SQL database with key fields extracted for searchability
 * and the entire resource serialized as a JSON string to ensure no data loss.
 *
 * @property database The database instance (injected for testing, or created via factory).
 */
class FhirRepository(val database: ChartCamDatabase) {

    /**
     * Primary constructor for Application usage.
     * @param databaseFactory Factory to create the [SqlDriver] for [ChartCamDatabase].
     */
    constructor(databaseFactory: DatabaseDriverFactory) : this(
        ChartCamDatabase(databaseFactory.createDriver())
    )

    /**
     * Helper constructor for Testing with raw SqlDriver (e.g. In-Memory).
     * @param driver The raw [SqlDriver] to use for the [ChartCamDatabase].
     */
    constructor(driver: SqlDriver) : this(ChartCamDatabase(driver))

    private val dbQuery = database.chartCamQueries
    private val fhirJson = FhirR4Json()

    // --- Practitioner ---

    /**
     * Saves a FHIR [Practitioner] resource into the local database.
     * @param practitioner The Practitioner resource to persist.
     */
    suspend fun savePractitioner(practitioner: Practitioner) {
        val name = practitioner.name.firstOrNull()
        val serialized = fhirJson.encodeToString(practitioner)
        dbQuery.insertPractitioner(
            id = practitioner.id ?: "",
            family = name?.familyName ?: "Unknown",
            given = name?.givenName ?: "Unknown",
            active = practitioner.active?.value ?: true,
            serializedResource = serialized
        )
    
    // --- Device ---
    suspend fun saveDevice(device: Device) {
        val serialized = fhirJson.encodeToString(device)
        dbQuery.insertDevice(device.id!!, serialized)
    }

    suspend fun getDevice(id: String): Device? {
        val entity = dbQuery.getDevice(id).awaitAsOneOrNull() ?: return null
        return fhirJson.decodeFromString(entity.serializedResource) as Device
    }

    // --- Provenance ---
    suspend fun saveProvenance(provenance: Provenance, encounterId: String? = null) {
        val serialized = fhirJson.encodeToString(provenance)
        val targetId = provenance.target.firstOrNull()?.reference?.value ?: ""
        dbQuery.insertProvenance(provenance.id!!, targetId, encounterId, provenance.recorded?.value.toString(), serialized)
    }

    suspend fun getProvenancesForEncounter(encounterId: String): List<Provenance> {
        return dbQuery.getProvenancesForEncounter(encounterId).awaitAsList().map { entity ->
            fhirJson.decodeFromString(entity.serializedResource) as Provenance
        }
    }

}

    /**
     * Retrieves a FHIR [Practitioner] resource by its ID.
     * @param id The unique identifier of the Practitioner.
     * @return The Practitioner resource if found, or null otherwise.
     */
    suspend fun getPractitioner(id: String): Practitioner? {
        val entity = dbQuery.getPractitionerById(id).awaitAsOneOrNull() ?: return null
        return fhirJson.decodeFromString(entity.serializedResource) as Practitioner
    }

    // --- Patient ---

    /**
     * Saves a FHIR [Patient] resource into the local database.
     * @param patient The Patient resource to persist.
     */
    suspend fun savePatient(patient: Patient) {
        val name = patient.name.firstOrNull()
        val serialized = fhirJson.encodeToString(patient)
        dbQuery.insertPatient(
            id = patient.id ?: "",
            family = name?.familyName ?: "Unknown",
            given = name?.givenName ?: "Unknown",
            birthDate = patient.birthDate?.value?.toString() ?: "",
            mrn = patient.mrn,
            gender = patient.gender?.value?.name ?: "unknown",
            managingOrganization = patient.managingOrganization?.reference?.value ?: "",
            serializedResource = serialized
        )
    }

    /**
     * Retrieves a FHIR [Patient] resource by its ID.
     * @param id The unique identifier of the Patient.
     * @return The Patient resource if found, or null otherwise.
     */
    suspend fun getPatient(id: String): Patient? {
        val entity = dbQuery.getPatientById(id).awaitAsOneOrNull() ?: return null
        return fhirJson.decodeFromString(entity.serializedResource) as Patient
    }

    /**
     * Retrieves all FHIR [Patient] resources stored locally.
     * @return A list containing all Patient resources.
     */
    suspend fun getAllPatients(): List<Patient> {
        return dbQuery.getAllPatients().awaitAsList().map { entity ->
            fhirJson.decodeFromString(entity.serializedResource) as Patient
        }
    }

    /**
     * Searches for FHIR [Patient] resources that match the given query string.
     * The search runs against family name, given name, or MRN.
     * @param query The search query string.
     * @return A list of matching Patient resources.
     */
    suspend fun searchPatients(query: String): List<Patient> {
        return dbQuery.searchPatients(query).awaitAsList().map { entity ->
            fhirJson.decodeFromString(entity.serializedResource) as Patient
        }
    }

    // --- Encounter ---

    /**
     * Saves a FHIR [Encounter] resource into the local database, along with clinical notes.
     * @param encounter The Encounter resource to persist.
     * @param notes Optional notes associated with the Encounter.
     */
    suspend fun saveEncounter(encounter: Encounter) {
        val serialized = fhirJson.encodeToString(encounter)
        dbQuery.insertEncounter(
            id = encounter.id ?: "",
            patientId = encounter.subject?.reference?.value ?: "",
            practitionerId = encounter.participant.firstOrNull()?.individual?.reference?.value ?: "",
            date = encounter.period?.start?.value?.toString() ?: "",
            status = encounter.status?.value?.name ?: "in-progress",
            serializedResource = serialized
        )
    }

    /**
     * Retrieves a FHIR [Encounter] resource by its ID.
     * @param id The unique identifier of the Encounter.
     * @return The Encounter resource if found, or null otherwise.
     */
    suspend fun getEncounter(id: String): Encounter? {
        val entity = dbQuery.getEncounterById(id).awaitAsOneOrNull() ?: return null
        return fhirJson.decodeFromString(entity.serializedResource) as Encounter
    }

    /**
     * Retrieves all FHIR [Encounter] resources associated with a specific Patient ID.
     * @param patientId The unique identifier of the Patient.
     * @return A list of Encounter resources for the specified Patient.
     */
    suspend fun getEncountersForPatient(patientId: String): List<Encounter> {
        return dbQuery.getEncountersForPatient(patientId).awaitAsList().map { entity ->
            fhirJson.decodeFromString(entity.serializedResource) as Encounter
        }
    }

    /**
     * Retrieves the clinical notes associated with an Encounter.
     * @param id The unique identifier of the Encounter.
     * @return The clinical notes string, or empty string if not found.
     */
    /**
     * @param status The new status (e.g., "finished", "in-progress").
     */
    suspend fun updateEncounterStatus(id: String, status: String) {
        val encounter = getEncounter(id)
        if (encounter != null) {
            val mappedStatus = when (status.lowercase()) {
                "finished" -> Encounter.EncounterStatus.Finished
                "in-progress" -> Encounter.EncounterStatus.In_Progress
                "planned" -> Encounter.EncounterStatus.Planned
                "arrived" -> Encounter.EncounterStatus.Arrived
                "triaged" -> Encounter.EncounterStatus.Triaged
                "onleave" -> Encounter.EncounterStatus.Onleave
                "cancelled" -> Encounter.EncounterStatus.Cancelled
                else -> Encounter.EncounterStatus.Unknown
            }
            val updatedStatus = Enumeration(value = mappedStatus)
            val updatedEncounter = encounter.toBuilder().apply {
                this.status = updatedStatus
            }.build()
            val serialized = fhirJson.encodeToString(updatedEncounter)
            dbQuery.updateEncounterStatus(status, serialized, id)
        }
    }

    // --- DocumentReference (Photos) ---

    /**
     * Saves a FHIR [DocumentReference] resource (typically a clinical photo) into the local database.
     * @param doc The DocumentReference resource to persist.
     */
    suspend fun saveDocumentReference(doc: DocumentReference) {
        val serialized = fhirJson.encodeToString(doc)
        dbQuery.insertDocumentReference(
            id = doc.id ?: "",
            encounterId = doc.context?.encounter?.firstOrNull()?.reference?.value ?: "",
            patientId = doc.subject?.reference?.value ?: "",
            type = "photo",
            category = "clinical-photography",
            date = doc.date?.value?.toString() ?: "",
            title = doc.description?.value ?: "",
            filePath = doc.content.firstOrNull()?.attachment?.url?.value ?: "",
            mimeType = doc.content.firstOrNull()?.attachment?.contentType?.value ?: "",
            serializedResource = serialized
        )
    }

    /**
     * Retrieves all FHIR [DocumentReference] resources (photos) associated with a specific Encounter ID.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of DocumentReference resources.
     */
    suspend fun getPhotosForEncounter(encounterId: String): List<DocumentReference> {
        return dbQuery.getPhotosForEncounter(encounterId).awaitAsList().map { entity ->
            fhirJson.decodeFromString(entity.serializedResource) as DocumentReference
        }
    }

    // --- QuestionnaireResponse (Forms and Notes) ---

    /**
     * Saves a FHIR [QuestionnaireResponse] resource into the local database.
     * @param qr The QuestionnaireResponse resource to persist.
     */
    suspend fun saveQuestionnaireResponse(qr: QuestionnaireResponse) {
        val serialized = fhirJson.encodeToString(qr)
        dbQuery.insertQuestionnaireResponse(
            id = qr.id ?: "",
            encounterId = qr.encounter?.reference?.value ?: "",
            patientId = qr.subject?.reference?.value ?: "",
            questionnaireId = qr.questionnaire?.value ?: "",
            date = qr.authored?.value?.toString() ?: "",
            serializedResource = serialized
        )
    }

    /**
     * Retrieves all FHIR [QuestionnaireResponse] resources associated with a specific Encounter ID.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of QuestionnaireResponse resources.
     */
    suspend fun getQuestionnaireResponsesForEncounter(encounterId: String): List<QuestionnaireResponse> {
        return dbQuery.getQuestionnaireResponsesForEncounter(encounterId).awaitAsList().map { entity ->
            fhirJson.decodeFromString(entity.serializedResource) as QuestionnaireResponse
        }
    }

    // --- Device ---
    suspend fun saveDevice(device: Device) {
        val serialized = fhirJson.encodeToString(device)
        dbQuery.insertDevice(device.id!!, serialized)
    }

    suspend fun getDevice(id: String): Device? {
        val entity = dbQuery.getDevice(id).awaitAsOneOrNull() ?: return null
        return fhirJson.decodeFromString(entity.serializedResource) as Device
    }

    // --- Provenance ---
    suspend fun saveProvenance(provenance: Provenance, encounterId: String? = null) {
        val serialized = fhirJson.encodeToString(provenance)
        val targetId = provenance.target.firstOrNull()?.reference?.value ?: ""
        dbQuery.insertProvenance(provenance.id!!, targetId, encounterId, provenance.recorded?.value.toString(), serialized)
    }

    suspend fun getProvenancesForEncounter(encounterId: String): List<Provenance> {
        return dbQuery.getProvenancesForEncounter(encounterId).awaitAsList().map { entity ->
            fhirJson.decodeFromString(entity.serializedResource) as Provenance
        }
    }

}
