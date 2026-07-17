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
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.Resource
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.models.mrn

/**
 * Repository responsible for CRUD operations on FHIR resources persisted locally.
 * Uses a generic Resource table and Index tables mimicking the FHIR Engine SDK.
 *
 * @property database The database instance used by this repository for executing queries.
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
            is Practitioner -> {
                val name = resource.name.firstOrNull()
                if (name != null) {
                    dbQuery.insertStringIndex(resourceType, resourceId, "family", name.familyName ?: "")
                    dbQuery.insertStringIndex(resourceType, resourceId, "given", name.givenName ?: "")
                }
                dbQuery.insertTokenIndex(resourceType, resourceId, "active", null, (resource.active?.value ?: true).toString())
            }
            is Patient -> {
                val name = resource.name.firstOrNull()
                if (name != null) {
                    dbQuery.insertStringIndex(resourceType, resourceId, "family", name.familyName ?: "")
                    dbQuery.insertStringIndex(resourceType, resourceId, "given", name.givenName ?: "")
                    dbQuery.insertStringIndex(resourceType, resourceId, "name", "${name.givenName} ${name.familyName}")
                }
                dbQuery.insertTokenIndex(resourceType, resourceId, "mrn", null, resource.mrn)
                dbQuery.insertTokenIndex(resourceType, resourceId, "gender", null, resource.gender?.value?.name ?: "")
                resource.birthDate?.value?.toString()?.let {
                    dbQuery.insertDateIndex(resourceType, resourceId, "birthdate", it)
                }
                resource.managingOrganization?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "organization", it)
                }
            }
            is Encounter -> {
                resource.subject?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "subject", it)
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "patient", it)
                }
                resource.participant.firstOrNull()?.individual?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "practitioner", it)
                }
                dbQuery.insertTokenIndex(resourceType, resourceId, "status", null, resource.status.value?.name ?: "")
                resource.period?.start?.value?.toString()?.let {
                    dbQuery.insertDateIndex(resourceType, resourceId, "date", it)
                }
            }
            is DocumentReference -> {
                resource.context?.encounter?.firstOrNull()?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "encounter", it)
                }
                resource.subject?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "subject", it)
                }
                resource.date?.value?.toString()?.let {
                    dbQuery.insertDateIndex(resourceType, resourceId, "date", it)
                }
            }
            is QuestionnaireResponse -> {
                resource.encounter?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "encounter", it)
                }
                resource.subject?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "subject", it)
                }
                resource.questionnaire?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "questionnaire", it)
                }
                resource.authored?.value?.toString()?.let {
                    dbQuery.insertDateIndex(resourceType, resourceId, "authored", it)
                }
            }
            is Provenance -> {
                resource.target.firstOrNull()?.reference?.value?.let {
                    dbQuery.insertReferenceIndex(resourceType, resourceId, "target", it)
                }
            }
        }
    }

/**
     * Saves a generic FHIR resource to the local database.
     *
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @param resource The FHIR Resource.
     * @param isLocalChange Whether this save is a local user mutation (default true).
     * @throws kotlinx.serialization.SerializationException if the resource cannot be serialized.
     * @throws Exception if a database operation fails.
     */
    @Suppress("ktlint:standard:function-signature")
    open suspend fun saveResource(
        resourceType: String,
        resourceId: String,
        resource: Resource,
        isLocalChange: Boolean = true,
    ) {
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
     */
    open suspend fun saveResourceFromSync(
        resourceType: String,
        resourceId: String,
        resource: Resource,
    ) {
        saveResource(resourceType, resourceId, resource, isLocalChange = false)
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
        return fhirJson.decodeFromString(entity.serializedResource) as Resource
    }

    /**
     * Deletes a FHIR Resource by type and ID.
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @param isLocalChange Whether this delete is a local user mutation (default true).
     */
    open suspend fun deleteResource(
        resourceType: String,
        resourceId: String,
        isLocalChange: Boolean = true,
    ) {
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
     * Saves a Practitioner.
     * @param practitioner The Practitioner resource to persist.
     */
    open suspend fun savePractitioner(practitioner: Practitioner) {
        saveResource("Practitioner", practitioner.id ?: "", practitioner)
    }

    /**
     * Retrieves a Practitioner.
     * @param id The unique identifier of the Practitioner to retrieve.
     * @return The Practitioner resource if found, or null otherwise.
     */
    open suspend fun getPractitioner(id: String): Practitioner? = getResource("Practitioner", id) as? Practitioner

    /**
     * Deletes a Practitioner.
     * @param id The unique identifier of the Practitioner to delete.
     */
    open suspend fun deletePractitioner(id: String) {
        deleteResource("Practitioner", id)
    }

    /**
     * Saves a Patient.
     * @param patient The Patient resource to persist.
     */
    open suspend fun savePatient(patient: Patient) {
        saveResource("Patient", patient.id ?: "", patient)
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
            val encounters = dbQuery.searchResourcesByReference("Encounter", "practitioner", practitionerId).awaitAsList()
            val patientIds =
                encounters
                    .mapNotNull {
                        val enc = fhirJson.decodeFromString(it.serializedResource) as Encounter
                        enc.subject?.reference?.value
                    }.distinct()
            patientIds.mapNotNull { getPatient(it) }
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
        val allEntities = dbQuery.searchResourcesByString("Patient", "name", query).awaitAsList()
        var patients = allEntities.map { fhirJson.decodeFromString(it.serializedResource) as Patient }
        if (!showAll && practitionerId != null) {
            val encounters = dbQuery.searchResourcesByReference("Encounter", "practitioner", practitionerId).awaitAsList()
            val patientIds =
                encounters
                    .mapNotNull {
                        (fhirJson.decodeFromString(it.serializedResource) as Encounter).subject?.reference?.value
                    }.distinct()
            patients = patients.filter { patientIds.contains(it.id) }
        }
        return patients
    }

    /**
     * Deletes a Patient.
     * @param id The unique identifier of the Patient to delete.
     */
    open suspend fun deletePatient(id: String) {
        deleteResource("Patient", id)
    }

    /**
     * Saves an Encounter.
     * @param encounter The Encounter resource to persist.
     */
    open suspend fun saveEncounter(encounter: Encounter) {
        saveResource("Encounter", encounter.id ?: "", encounter)
    }

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
    open suspend fun getEncountersForPatient(patientId: String): List<Encounter> =
        dbQuery.searchResourcesByReferenceDesc("Encounter", "patient", patientId).awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as Encounter
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
                                    status = Enumeration(value = com.google.fhir.model.r4.Narrative.NarrativeStatus.Generated),
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
     */
    open suspend fun deleteEncounter(id: String) {
        deleteResource("Encounter", id)
    }

    /**
     * Saves a DocumentReference (photo).
     * @param doc The DocumentReference resource to persist.
     */
    open suspend fun saveDocumentReference(doc: DocumentReference) {
        saveResource("DocumentReference", doc.id ?: "", doc)
    }

    /**
     * Retrieves photos (DocumentReferences) for an Encounter.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of DocumentReference resources.
     */
    open suspend fun getPhotosForEncounter(encounterId: String): List<DocumentReference> =
        dbQuery.searchResourcesByReference("DocumentReference", "encounter", encounterId).awaitAsList().map {
            fhirJson.decodeFromString(it.serializedResource) as DocumentReference
        }

    /**
     * Saves a QuestionnaireResponse.
     * @param qr The QuestionnaireResponse resource to persist.
     */
    open suspend fun saveQuestionnaireResponse(qr: QuestionnaireResponse) {
        saveResource("QuestionnaireResponse", qr.id ?: "", qr)
    }

    /**
     * Retrieves QuestionnaireResponses for an Encounter.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of QuestionnaireResponse resources.
     */
    open suspend fun getQuestionnaireResponsesForEncounter(encounterId: String): List<QuestionnaireResponse> {
        val ref = if (encounterId.startsWith("Encounter/")) encounterId else "Encounter/$encounterId"
        val refNoPrefix = if (encounterId.startsWith("Encounter/")) encounterId.removePrefix("Encounter/") else encounterId

        val withPrefix = dbQuery.searchResourcesByReferenceDesc("QuestionnaireResponse", "encounter", ref).awaitAsList()
        val withoutPrefix = dbQuery.searchResourcesByReferenceDesc("QuestionnaireResponse", "encounter", refNoPrefix).awaitAsList()

        val all = (withPrefix + withoutPrefix).distinctBy { it.resourceId }

        return all.map {
            fhirJson.decodeFromString(it.serializedResource) as QuestionnaireResponse
        }
    }

    /**
     * Saves a Device.
     * @param device The Device resource to persist.
     */
    open suspend fun saveDevice(device: Device) {
        saveResource("Device", device.id ?: "", device)
    }

    /**
     * Retrieves a Device.
     * @param id The unique identifier of the Device to retrieve.
     * @return The Device resource if found, or null otherwise.
     */
    open suspend fun getDevice(id: String): Device? = getResource("Device", id) as? Device

    /**
     * Saves a Provenance.
     * @param provenance The Provenance resource to persist.
     * @param encounterId Optional unique identifier of the Encounter.
     */
    open suspend fun saveProvenance(
        provenance: Provenance,
        encounterId: String? = null,
    ) {
        saveResource("Provenance", provenance.id ?: "", provenance)
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
