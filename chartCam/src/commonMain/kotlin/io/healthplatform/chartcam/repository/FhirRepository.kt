/**
 * @file FhirRepository.kt
 * Repository for storing and retrieving FHIR resources (Patients and Encounters).
 * This repository handles bidirectional conversion between FHIR objects and local database models.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.google.fhir.model.r4.Device
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Provenance
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.Resource
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.utils.runSuspendCatching

/**
 * Repository responsible for CRUD operations on FHIR resources persisted locally.
 * Uses a generic Resource table and Index tables mimicking the FHIR Engine SDK.
 *
 * @param database The database instance used by this repository for executing queries.
 */
open class FhirRepository(
    val database: ChartCamDatabase,
) {
    /**
     * Primary constructor for Application usage.
     * @param databaseFactory Factory to create the SqlDriver.
     */
    constructor(databaseFactory: DatabaseDriverFactory) : this(
        ChartCamDatabase(databaseFactory.createDriver()),
    )

    /**
     * Helper constructor for Testing with raw SqlDriver.
     * @param driver The raw SqlDriver to use.
     */
    constructor(driver: SqlDriver) : this(ChartCamDatabase(driver))

    private val dbQuery by lazy { database.chartCamQueries }
    private val fhirJson = FhirR4Json()

    /**
     * Generates and saves SearchParam indices for a given resource.
     * @param resource The FHIR Resource to index.
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     */

    private suspend fun indexResource(
        resource: Resource,
        resourceType: String,
        resourceId: String,
    ) {
        dbQuery.deleteStringIndices(resourceType, resourceId)
        dbQuery.deleteTokenIndices(resourceType, resourceId)
        dbQuery.deleteReferenceIndices(resourceType, resourceId)
        dbQuery.deleteDateIndices(resourceType, resourceId)

        when (resource) {
            is Practitioner -> indexPractitioner(resource, resourceType, resourceId)
            is Patient -> indexPatient(resource, resourceType, resourceId)
            is Encounter -> indexEncounter(resource, resourceType, resourceId)
            is DocumentReference -> indexDocumentReference(resource, resourceType, resourceId)
            is QuestionnaireResponse -> indexQuestionnaireResponse(resource, resourceType, resourceId)
            is Questionnaire -> indexQuestionnaire(resource, resourceType, resourceId)
            else -> {
                // Not indexed
            }
        }
    }

    /**
     * Helper for indexing.
     * @param resource The resource.
     * @param resourceType The resourceType.
     * @param resourceId The resourceId.
     */
    private suspend fun indexPractitioner(
        resource: Practitioner,
        resourceType: String,
        resourceId: String,
    ) {
        val name = resource.name.firstOrNull()
        if (name != null) {
            dbQuery.insertStringIndex(resourceType, resourceId, "family", name.familyName)
            dbQuery.insertStringIndex(resourceType, resourceId, "given", name.givenName)
        }
        dbQuery.insertTokenIndex(resourceType, resourceId, "active", null, (resource.active?.value ?: true).toString())
    }

    /**
     * Helper for indexing.
     * @param resource The resource.
     * @param resourceType The resourceType.
     * @param resourceId The resourceId.
     */
    private suspend fun indexPatient(
        resource: Patient,
        resourceType: String,
        resourceId: String,
    ) {
        val name = resource.name.firstOrNull()
        if (name != null) {
            dbQuery.insertStringIndex(resourceType, resourceId, "family", name.familyName)
            dbQuery.insertStringIndex(resourceType, resourceId, "given", name.givenName)
            dbQuery.insertStringIndex(resourceType, resourceId, "name", "${name.givenName} ${name.familyName}")
        }
        dbQuery.insertTokenIndex(resourceType, resourceId, "mrn", null, resource.mrn)
        if (resource.mrn.isNotBlank()) {
            dbQuery.insertStringIndex(resourceType, resourceId, "mrn", resource.mrn)
        }
        dbQuery.insertTokenIndex(resourceType, resourceId, "gender", null, resource.gender?.value?.name ?: "")
        resource.birthDate?.value?.toString()?.let {
            dbQuery.insertDateIndex(resourceType, resourceId, "birthdate", it)
        }
        resource.managingOrganization?.reference?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "organization", it)
        }
    }

    /**
     * Helper for indexing.
     * @param resource The resource.
     * @param resourceType The resourceType.
     * @param resourceId The resourceId.
     */
    private suspend fun indexEncounter(
        resource: Encounter,
        resourceType: String,
        resourceId: String,
    ) {
        resource.subject?.reference?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "subject", it)
            dbQuery.insertReferenceIndex(resourceType, resourceId, "patient", it)
        }
        resource.participant.firstOrNull()?.individual?.reference?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "practitioner", it)
        }
        dbQuery.insertTokenIndex(resourceType, resourceId, "status", null, resource.status.value?.name ?: "")
    }

    /**
     * Helper for indexing.
     * @param resource The resource.
     * @param resourceType The resourceType.
     * @param resourceId The resourceId.
     */
    private suspend fun indexDocumentReference(
        resource: DocumentReference,
        resourceType: String,
        resourceId: String,
    ) {
        resource.subject?.reference?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "subject", it)
            dbQuery.insertReferenceIndex(resourceType, resourceId, "patient", it)
        }
        resource.context?.encounter?.firstOrNull()?.reference?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "encounter", it)
        }
        dbQuery.insertTokenIndex(resourceType, resourceId, "status", null, resource.status.value?.name ?: "")
    }

    /**
     * Helper for indexing.
     * @param resource The resource.
     * @param resourceType The resourceType.
     * @param resourceId The resourceId.
     */
    private suspend fun indexQuestionnaireResponse(
        resource: QuestionnaireResponse,
        resourceType: String,
        resourceId: String,
    ) {
        resource.subject?.reference?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "subject", it)
            dbQuery.insertReferenceIndex(resourceType, resourceId, "patient", it)
        }
        resource.encounter?.reference?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "encounter", it)
        }
        resource.questionnaire?.value?.let {
            dbQuery.insertReferenceIndex(resourceType, resourceId, "questionnaire", it)
        }
        dbQuery.insertTokenIndex(resourceType, resourceId, "status", null, resource.status.value?.name ?: "")
    }

    /**
     * Helper for indexing.
     * @param resource The resource.
     * @param resourceType The resourceType.
     * @param resourceId The resourceId.
     */
    private suspend fun indexQuestionnaire(
        resource: Questionnaire,
        resourceType: String,
        resourceId: String,
    ) {
        resource.title?.value?.let {
            dbQuery.insertStringIndex(resourceType, resourceId, "title", it)
        }
        dbQuery.insertTokenIndex(resourceType, resourceId, "status", null, resource.status.value?.name ?: "")
    }

    /**
     * Saves resource
     * @param resourceType The resourceType.
     * @param resourceId The resourceId.
     * @param resource The resource.
     * @param isLocalChange The isLocalChange.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveResource(
        resourceType: String,
        resourceId: String,
        resource: Resource,
        isLocalChange: Boolean = true,
    ): Result<Unit> =
        runSuspendCatching {
            val serialized = fhirJson.encodeToString(resource)
            val now =
                kotlin.time.Clock.System
                    .now()
                    .toString()
            dbQuery.insertResource(resourceId, resourceType, serialized, now)
            indexResource(resource, resourceType, resourceId)

            if (isLocalChange) {
                val versionId = resource.meta?.versionId?.value
                dbQuery.insertLocalChange(resourceType, resourceId, now, "UPDATE", serialized, versionId)
            }
        }

    /**
     * Saves a FHIR Resource during a sync operation without creating a local change record.
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @param resource The FHIR Resource.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveResourceFromSync(
        resourceType: String,
        resourceId: String,
        resource: Resource,
    ): Result<Unit> =
        runSuspendCatching {
            saveResource(resourceType, resourceId, resource, isLocalChange = false).getOrThrow()
            // Ensure any pending local changes for this resource are cleared to prevent overwriting server state
            dbQuery.deleteLocalChangesForResource(resourceType, resourceId)
        }

    /**
     * Retrieves a FHIR Resource by type and ID.
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @return The resource, or null if not found.
     */
    open suspend fun getResource(
        resourceType: String,
        resourceId: String,
    ): Resource? {
        val entity = dbQuery.getResourceById(resourceType, resourceId).awaitAsOneOrNull() ?: return null
        return fhirJson.decodeFromString(entity.serializedResource)
    }

    /**
     * Deletes a FHIR Resource by type and ID.
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @param isLocalChange Whether this delete is a local user mutation (default true).
     * @return A [Result] indicating success or failure.
     */
    open suspend fun deleteResource(
        resourceType: String,
        resourceId: String,
        isLocalChange: Boolean = true,
    ): Result<Unit> =
        runSuspendCatching {
            dbQuery.deleteResourceById(resourceType, resourceId)

            if (isLocalChange) {
                val now =
                    kotlin.time.Clock.System
                        .now()
                        .toString()
                dbQuery.insertLocalChange(resourceType, resourceId, now, "DELETE", "", null)
            }
        }

    /**
     * Retrieves all pending local changes for synchronization.
     * @return List of LocalChangeEntity.
     */
    open suspend fun getAllLocalChanges() = dbQuery.getAllLocalChanges().awaitAsList()

    /**
     * Retrieves the count of pending local changes.
     * @return Number of pending changes.
     */
    open suspend fun getPendingLocalChangesCount(): Int = dbQuery.getAllLocalChanges().awaitAsList().size

    /**
     * Deletes a local change record after successful sync.
     * @param id The ID of the local change.
     */
    open suspend fun deleteLocalChange(id: Long) {
        dbQuery.deleteLocalChange(id)
    }

    /**
     * Saves a Practitioner with default local change tracking.
     * @param practitioner The Practitioner resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun savePractitioner(practitioner: Practitioner): Result<Unit> = savePractitioner(practitioner, isLocalChange = true)

    /**
     * Saves a Practitioner.
     * @param practitioner The Practitioner resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun savePractitioner(
        practitioner: Practitioner,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResource("Practitioner", practitioner.id ?: "", practitioner, isLocalChange)

    /**
     * Retrieves a Practitioner.
     * @param id The unique identifier of the Practitioner to retrieve.
     * @return The Practitioner resource if found, or null otherwise.
     */
    open suspend fun getPractitioner(id: String): Practitioner? = getResource("Practitioner", id) as? Practitioner

    /**
     * Deletes a Practitioner.
     * @param id The unique identifier of the Practitioner to delete.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun deletePractitioner(id: String): Result<Unit> = deleteResource("Practitioner", id)

    /**
     * Saves a Patient with default local change tracking.
     * @param patient The Patient resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun savePatient(patient: Patient): Result<Unit> = savePatient(patient, isLocalChange = true)

    /**
     * Saves a Patient.
     * @param patient The Patient resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun savePatient(
        patient: Patient,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResource("Patient", patient.id ?: "", patient, isLocalChange)

    /**
     * Retrieves a Patient by Medical Record Number (MRN).
     *
     * @param mrn The Medical Record Number to match.
     * @return The matching [Patient], or null if not found.
     */
    open suspend fun getPatientByMrn(mrn: String): Patient? {
        if (mrn.isBlank()) return null
        val entity =
            dbQuery.searchResourcesByToken("Patient", "mrn", null, mrn).awaitAsList().firstOrNull()
                ?: dbQuery.searchResourcesByString("Patient", "mrn", mrn).awaitAsList().firstOrNull()
        return entity?.let { fhirJson.decodeFromString(it.serializedResource) as? Patient }
    }

    /**
     * Retrieves a Patient.
     * @param id The unique identifier of the Patient to retrieve.
     * @return The Patient resource if found, or null otherwise.
     */
    open suspend fun getPatient(id: String): Patient? = getResource("Patient", id) as? Patient

    /**
     * Retrieves all Patients.
     * @param showAll If true, returns all patients regardless of the practitioner.
     * @param practitionerId The Practitioner to filter by, if showAll is false.
     * @return A list containing all matching Patient resources.
     */
    open suspend fun getAllPatients(
        showAll: Boolean = true,
        practitionerId: String? = null,
    ): List<Patient> =
        if (showAll || practitionerId == null) {
            dbQuery.getAllResourcesByType("Patient").awaitAsList().map {
                fhirJson.decodeFromString(it.serializedResource) as Patient
            }
        } else {
            val encounters =
                dbQuery
                    .searchResourcesByReference(
                        "Encounter",
                        "practitioner",
                        practitionerId,
                    ).awaitAsList()
            val patientIds =
                encounters
                    .mapNotNull {
                        val enc = fhirJson.decodeFromString(it.serializedResource) as Encounter
                        enc.subject
                            ?.reference
                            ?.value
                            ?.removePrefix("Patient/")
                    }.distinct()
            val all =
                dbQuery.getAllResourcesByType("Patient").awaitAsList().map {
                    fhirJson.decodeFromString(it.serializedResource) as Patient
                }
            all.filter { p ->
                val pid = p.id?.removePrefix("Patient/") ?: ""
                patientIds.contains(pid) ||
                    p.managingOrganization
                        ?.reference
                        ?.value
                        ?.contains(practitionerId) == true
            }
        }

    /**
     * Searches Patients by query string.
     * @param query The search query string.
     * @param showAll If true, searches across all patients.
     * @param practitionerId The Practitioner to filter by, if showAll is false.
     * @return A list of matching Patient resources.
     */
    open suspend fun searchPatients(
        query: String,
        showAll: Boolean = true,
        practitionerId: String? = null,
    ): List<Patient> {
        val trimmedQuery = query.trim()
        val nameEntities = dbQuery.searchResourcesByString("Patient", "name", trimmedQuery).awaitAsList()
        val mrnEntities = dbQuery.searchResourcesByString("Patient", "mrn", trimmedQuery).awaitAsList()
        val allEntities = (nameEntities + mrnEntities).distinctBy { it.resourceId }
        var patients = allEntities.map { fhirJson.decodeFromString(it.serializedResource) as Patient }
        if (!showAll && practitionerId != null) {
            val encounters =
                dbQuery
                    .searchResourcesByReference(
                        "Encounter",
                        "practitioner",
                        practitionerId,
                    ).awaitAsList()
            val patientIds =
                encounters
                    .mapNotNull {
                        (fhirJson.decodeFromString(it.serializedResource) as Encounter)
                            .subject
                            ?.reference
                            ?.value
                            ?.removePrefix("Patient/")
                    }.distinct()
            patients =
                patients.filter { p ->
                    val pid = p.id?.removePrefix("Patient/") ?: ""
                    patientIds.contains(pid) ||
                        p.managingOrganization
                            ?.reference
                            ?.value
                            ?.contains(practitionerId) == true
                }
        }
        return patients
    }

    /**
     * Safely retrieves a Resource, wrapping the operation in a [Result].
     *
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @return A [Result] enclosing the Resource or null if not found, or failure.
     */
    open suspend fun getResourceCatching(
        resourceType: String,
        resourceId: String,
    ): Result<Resource?> = runSuspendCatching { getResource(resourceType, resourceId) }

    /**
     * Safely retrieves a Patient by ID, wrapping the operation in a [Result].
     *
     * @param id The ID of the patient.
     * @return A [Result] enclosing the Patient or null if not found, or failure.
     */
    open suspend fun getPatientCatching(id: String): Result<Patient?> = runSuspendCatching { getPatient(id) }

    /**
     * Safely retrieves all Patients, wrapping the query in a [Result].
     *
     * @param showAll True to retrieve all patients regardless of practitioner scoping.
     * @param practitionerId Optional practitioner ID to scope the results.
     * @return A [Result] enclosing the list of matching Patients.
     */
    open suspend fun getAllPatientsCatching(
        showAll: Boolean = false,
        practitionerId: String? = null,
    ): Result<List<Patient>> = runSuspendCatching { getAllPatients(showAll, practitionerId) }

    /**
     * Deletes a Patient.
     * @param id The unique identifier of the Patient to delete.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun deletePatient(id: String): Result<Unit> = deletePatient(id, null)

    /**
     * Deletes a Patient.
     * @param id The unique identifier of the Patient to delete.
     * @param fileStorage Optional FileStorage to delete associated media.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun deletePatient(
        id: String,
        fileStorage: io.healthplatform.chartcam.files.FileStorage?,
    ): Result<Unit> =
        runSuspendCatching {
            val cleanId = id.removePrefix("Patient/")
            val encounters = getEncountersForPatient(cleanId)
            encounters.forEach { enc ->
                enc.id?.let { deleteEncounter(it, fileStorage) }
            }
            deleteResource("Patient", cleanId).getOrThrow()
        }

    /**
     * Saves an Encounter with default local change tracking.
     * @param encounter The Encounter resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveEncounter(encounter: Encounter): Result<Unit> = saveEncounter(encounter, isLocalChange = true)

    /**
     * Saves an Encounter.
     * @param encounter The Encounter resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveEncounter(
        encounter: Encounter,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResource("Encounter", encounter.id ?: "", encounter, isLocalChange)

    /**
     * Retrieves an Encounter.
     * @param id The unique identifier of the Encounter to retrieve.
     * @return The Encounter resource if found, or null otherwise.
     */
    open suspend fun getEncounter(id: String): Encounter? = getResource("Encounter", id) as? Encounter

    /**
     * Retrieves Encounters for a specific Patient.
     * @param patientId The unique identifier of the Patient.
     * @return A list of Encounter resources.
     */
    open suspend fun getEncountersForPatient(patientId: String): List<Encounter> {
        val cleanId = patientId.removePrefix("Patient/")
        val withPrefix =
            dbQuery.searchResourcesByReferenceDesc("Encounter", "patient", "Patient/$cleanId").awaitAsList()
        val withoutPrefix =
            dbQuery.searchResourcesByReferenceDesc("Encounter", "patient", cleanId).awaitAsList()
        return (withPrefix + withoutPrefix).distinctBy { it.resourceId }.map {
            fhirJson.decodeFromString(it.serializedResource) as Encounter
        }
    }

    /**
     * Updates Encounter status.
     * @param id The Encounter to update.
     * @param status The new status.
     * @param notes Optional notes.
     */
    open suspend fun updateEncounterStatus(
        id: String,
        status: String,
        notes: String? = null,
    ) {
        val encounter = getEncounter(id)
        if (encounter != null) {
            val mappedStatus =
                when (status.lowercase()) {
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
            val updatedEncounter =
                encounter
                    .toBuilder()
                    .apply {
                        this.status = updatedStatus
                        if (notes != null) {
                            this.text =
                                com.google.fhir.model.r4.Narrative.Builder(
                                    status =
                                        Enumeration(
                                            value = com.google.fhir.model.r4.Narrative.NarrativeStatus.Generated,
                                        ),
                                    div =
                                        com.google.fhir.model.r4.Xhtml
                                            .Builder(value = "<div>$notes</div>"),
                                )
                        }
                    }.build()
            saveEncounter(updatedEncounter)
        }
    }

    /**
     * Deletes an Encounter.
     * @param id The unique identifier of the Encounter to delete.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun deleteEncounter(id: String): Result<Unit> = deleteEncounter(id, null)

    /**
     * Deletes an Encounter and cascades deletion to linked photo references and questionnaire responses.
     * @param id The unique identifier of the Encounter to delete.
     * @param fileStorage Optional storage to delete associated image files from disk.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun deleteEncounter(
        id: String,
        fileStorage: io.healthplatform.chartcam.files.FileStorage?,
    ): Result<Unit> =
        runSuspendCatching {
            val cleanId = id.removePrefix("Encounter/")
            val photos = getPhotosForEncounter(cleanId)
            photos.forEach { doc ->
                doc.id?.let { deleteResource("DocumentReference", it) }
                val path =
                    doc.content
                        .firstOrNull()
                        ?.attachment
                        ?.url
                        ?.value
                if (path != null && fileStorage != null) {
                    fileStorage.deleteImage(path)
                }
            }
            val responses = getQuestionnaireResponsesForEncounter(cleanId)
            responses.forEach { qr ->
                qr.id?.let { deleteResource("QuestionnaireResponse", it) }
            }
            deleteResource("Encounter", cleanId).getOrThrow()
        }

    /**
     * Saves a DocumentReference (photo) with default local change tracking.
     * @param doc The DocumentReference resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveDocumentReference(doc: DocumentReference): Result<Unit> = saveDocumentReference(doc, isLocalChange = true)

    /**
     * Saves a DocumentReference (photo).
     * @param doc The DocumentReference resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveDocumentReference(
        doc: DocumentReference,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResource("DocumentReference", doc.id ?: "", doc, isLocalChange)

    /**
     * Retrieves photos (DocumentReferences) for an Encounter.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of DocumentReference resources.
     */
    open suspend fun getPhotosForEncounter(encounterId: String): List<DocumentReference> {
        val cleanId = encounterId.removePrefix("Encounter/")
        val withPrefix =
            dbQuery.searchResourcesByReference("DocumentReference", "encounter", "Encounter/$cleanId").awaitAsList()
        val withoutPrefix =
            dbQuery.searchResourcesByReference("DocumentReference", "encounter", cleanId).awaitAsList()
        return (withPrefix + withoutPrefix).distinctBy { it.resourceId }.map {
            fhirJson.decodeFromString(it.serializedResource) as DocumentReference
        }
    }

    /**
     * Retrieves a single DocumentReference (photo) by ID.
     * @param id The unique identifier of the DocumentReference.
     * @return The DocumentReference resource, or null if not found.
     */
    open suspend fun getDocumentReference(id: String): DocumentReference? = getResource("DocumentReference", id) as? DocumentReference

    /**
     * Saves a QuestionnaireResponse with default local change tracking.
     * @param qr The QuestionnaireResponse resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveQuestionnaireResponse(qr: QuestionnaireResponse): Result<Unit> =
        saveQuestionnaireResponse(qr, isLocalChange = true)

    /**
     * Saves a QuestionnaireResponse.
     * @param qr The QuestionnaireResponse resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveQuestionnaireResponse(
        qr: QuestionnaireResponse,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResource("QuestionnaireResponse", qr.id ?: "", qr, isLocalChange)

    /**
     * Retrieves QuestionnaireResponses for an Encounter.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of QuestionnaireResponse resources.
     */
    open suspend fun getQuestionnaireResponsesForEncounter(encounterId: String): List<QuestionnaireResponse> {
        val ref = if (encounterId.startsWith("Encounter/")) encounterId else "Encounter/$encounterId"
        val refNoPrefix =
            if (encounterId.startsWith("Encounter/")) encounterId.removePrefix("Encounter/") else encounterId

        val withPrefix = dbQuery.searchResourcesByReferenceDesc("QuestionnaireResponse", "encounter", ref).awaitAsList()
        val withoutPrefix =
            dbQuery.searchResourcesByReferenceDesc("QuestionnaireResponse", "encounter", refNoPrefix).awaitAsList()

        val all = (withPrefix + withoutPrefix).distinctBy { it.resourceId }

        return all.map {
            fhirJson.decodeFromString(it.serializedResource) as QuestionnaireResponse
        }
    }

    /**
     * Saves a Device with default local change tracking.
     * @param device The Device resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveDevice(device: Device): Result<Unit> = saveDevice(device, isLocalChange = true)

    /**
     * Saves a Device.
     * @param device The Device resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveDevice(
        device: Device,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResource("Device", device.id ?: "", device, isLocalChange)

    /**
     * Retrieves a Device.
     * @param id The unique identifier of the Device to retrieve.
     * @return The Device resource if found, or null otherwise.
     */
    open suspend fun getDevice(id: String): Device? = getResource("Device", id) as? Device

    /**
     * Saves a Provenance with default local change tracking.
     * @param provenance The Provenance resource to persist.
     * @param encounterId Optional unique identifier of the Encounter.
     */
    open suspend fun saveProvenance(
        provenance: Provenance,
        encounterId: String? = null,
    ) {
        saveProvenance(provenance, encounterId, isLocalChange = true)
    }

    /**
     * Saves a Provenance.
     * @param provenance The Provenance resource to persist.
     * @param encounterId Optional unique identifier of the Encounter.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     */
    open suspend fun saveProvenance(
        provenance: Provenance,
        encounterId: String? = null,
        isLocalChange: Boolean,
    ) {
        saveResource("Provenance", provenance.id ?: "", provenance, isLocalChange)
        if (encounterId != null) {
            dbQuery.insertReferenceIndex("Provenance", provenance.id!!, "encounter", encounterId)
        }
    }

    /**
     * Retrieves Provenances for an Encounter.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of Provenance resources.
     */
    open suspend fun getProvenancesForEncounter(encounterId: String): List<Provenance> =
        dbQuery.searchResourcesByReferenceDesc("Provenance", "encounter", encounterId).awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as Provenance
        }

    /**
     * Retrieves all Practitioner resources.
     * @return A list of Practitioner resources.
     */
    open suspend fun getAllPractitioners() =
        dbQuery.getAllResourcesByType("Practitioner").awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as Practitioner
        }

    /**
     * Retrieves all Encounter resources.
     * @return A list of Encounter resources.
     */
    open suspend fun getAllEncounters() =
        dbQuery.getAllResourcesByType("Encounter").awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as Encounter
        }

    /**
     * Retrieves all DocumentReference resources.
     * @return A list of DocumentReference resources.
     */
    open suspend fun getAllDocumentReferences() =
        dbQuery.getAllResourcesByType("DocumentReference").awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as DocumentReference
        }

    /**
     * Saves a Questionnaire resource with default local change tracking.
     *
     * @param questionnaire The Questionnaire resource to persist.
     */
    open suspend fun saveQuestionnaire(questionnaire: Questionnaire) {
        saveQuestionnaire(questionnaire, isLocalChange = true)
    }

    /**
     * Saves a Questionnaire resource.
     *
     * @param questionnaire The Questionnaire resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     */
    open suspend fun saveQuestionnaire(
        questionnaire: Questionnaire,
        isLocalChange: Boolean,
    ) {
        saveResource("Questionnaire", questionnaire.id ?: "", questionnaire, isLocalChange)
    }

    /**
     * Retrieves a Questionnaire resource by ID.
     *
     * @param id The unique identifier of the Questionnaire.
     * @return The Questionnaire resource, or null if not found.
     */
    open suspend fun getQuestionnaire(id: String): Questionnaire? = getResource("Questionnaire", id) as? Questionnaire

    /**
     * Retrieves all Questionnaire resources.
     *
     * @return A list of Questionnaire resources.
     */
    open suspend fun getAllQuestionnaires(): List<Questionnaire> =
        dbQuery.getAllResourcesByType("Questionnaire").awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as Questionnaire
        }

    /**
     * Retrieves all QuestionnaireResponse resources.
     * @return A list of QuestionnaireResponse resources.
     */
    open suspend fun getAllQuestionnaireResponses() =
        dbQuery.getAllResourcesByType("QuestionnaireResponse").awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as QuestionnaireResponse
        }

    /**
     * Retrieves all Provenance resources.
     * @return A list of Provenance resources.
     */
    open suspend fun getAllProvenances() =
        dbQuery.getAllResourcesByType("Provenance").awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as Provenance
        }

    /**
     * Retrieves all Device resources.
     * @return A list of Device resources.
     */
    open suspend fun getAllDevices() =
        dbQuery.getAllResourcesByType("Device").awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as Device
        }
}
