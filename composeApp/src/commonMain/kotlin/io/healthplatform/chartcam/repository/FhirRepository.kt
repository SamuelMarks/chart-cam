package io.healthplatform.chartcam.repository

import app.cash.sqldelight.db.SqlDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import io.healthplatform.chartcam.models.Attachment
import io.healthplatform.chartcam.models.DocumentReference
import io.healthplatform.chartcam.models.Encounter
import io.healthplatform.chartcam.models.HumanName
import io.healthplatform.chartcam.models.Patient
import io.healthplatform.chartcam.models.Period
import io.healthplatform.chartcam.models.Practitioner
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Repository responsible for CRUD operations on FHIR resources persisted locally.
 *
 * @property database The database instance (injected for testing, or created via factory).
 */
class FhirRepository(val database: ChartCamDatabase) {

    /**
     * Primary constructor for Application usage.
     */
    constructor(databaseFactory: DatabaseDriverFactory) : this(
        ChartCamDatabase(databaseFactory.createDriver())
    )

    /**
     * Helper constructor for Testing with raw SqlDriver (e.g. In-Memory).
     */
    constructor(driver: SqlDriver) : this(ChartCamDatabase(driver))

    private val dbQuery = database.chartCamQueries

    // --- Practitioner ---
    suspend fun savePractitioner(practitioner: Practitioner) {
        val name = practitioner.name.firstOrNull()
        dbQuery.insertPractitioner(
            id = practitioner.id,
            family = name?.family ?: "Unknown",
            given = name?.given?.joinToString(" ") ?: "Unknown",
            active = practitioner.active
        )
    }

    suspend fun getPractitioner(id: String): Practitioner? {
        val entity = dbQuery.getPractitionerById(id).awaitAsOneOrNull() ?: return null
        return Practitioner(
            id = entity.id,
            active = entity.active,
            name = listOf(HumanName(family = entity.family, given = entity.given.split(" ")))
        )
    }

    // --- Patient ---
    suspend fun savePatient(patient: Patient) {
        val name = patient.name.firstOrNull()
        dbQuery.insertPatient(
            id = patient.id,
            family = name?.family ?: "Unknown",
            given = name?.given?.joinToString(" ") ?: "Unknown",
            birthDate = patient.birthDate.toString(),
            mrn = patient.mrn,
            gender = patient.gender,
            managingOrganization = patient.managingOrganization
        )
    }

    suspend fun getPatient(id: String): Patient? {
        val entity = dbQuery.getPatientById(id).awaitAsOneOrNull() ?: return null
        return Patient(
            id = entity.id,
            name = listOf(HumanName(family = entity.family, given = entity.given.split(" "))),
            birthDate = LocalDate.parse(entity.birthDate),
            mrn = entity.mrn,
            gender = entity.gender,
            managingOrganization = entity.managingOrganization
        )
    }

    suspend fun getAllPatients(): List<Patient> {
        return dbQuery.getAllPatients().awaitAsList().map { entity ->
            Patient(
                id = entity.id,
                name = listOf(HumanName(family = entity.family, given = entity.given.split(" "))),
                birthDate = LocalDate.parse(entity.birthDate),
                mrn = entity.mrn,
                gender = entity.gender,
                managingOrganization = entity.managingOrganization
            )
        }
    }

    suspend fun searchPatients(query: String): List<Patient> {
        return dbQuery.searchPatients(query).awaitAsList().map { entity ->
            Patient(
                id = entity.id,
                name = listOf(HumanName(family = entity.family, given = entity.given.split(" "))),
                birthDate = LocalDate.parse(entity.birthDate),
                mrn = entity.mrn,
                gender = entity.gender,
                managingOrganization = entity.managingOrganization
            )
        }
    }

    // --- Encounter ---
    suspend fun saveEncounter(encounter: Encounter) {
        dbQuery.insertEncounter(
            id = encounter.id,
            patientId = encounter.subjectReference,
            practitionerId = encounter.participantReference,
            date = encounter.period.start.toString(),
            status = encounter.status,
            notes = encounter.text
        )
    }

    suspend fun getEncounter(id: String): Encounter? {
        val entity = dbQuery.getEncounterById(id).awaitAsOneOrNull() ?: return null
        return Encounter(
            id = entity.id,
            status = entity.status,
            type = entity.type,
            subjectReference = entity.patientId,
            participantReference = entity.practitionerId,
            period = Period(start = LocalDateTime.parse(entity.date)),
            text = entity.notes
        )
    }

    suspend fun getEncountersForPatient(patientId: String): List<Encounter> {
        return dbQuery.getEncountersForPatient(patientId).awaitAsList().map { entity ->
            Encounter(
                id = entity.id,
                status = entity.status,
                type = entity.type,
                subjectReference = entity.patientId,
                participantReference = entity.practitionerId,
                period = Period(start = LocalDateTime.parse(entity.date)),
                text = entity.notes
            )
        }
    }

    suspend fun updateEncounterNotes(id: String, notes: String) {
        dbQuery.updateEncounterNotes(notes, id)
    }

    suspend fun updateEncounterStatus(id: String, status: String) {
        dbQuery.updateEncounterStatus(status, id)
    }

    // --- DocumentReference (Photos) ---
    suspend fun saveDocumentReference(doc: DocumentReference) {
        dbQuery.insertDocumentReference(
            id = doc.id,
            encounterId = doc.contextReference,
            patientId = doc.subjectReference,
            type = "photo",
            category = "clinical-photography",
            date = doc.date.toString(),
            title = doc.description,
            filePath = doc.content.url,
            mimeType = doc.content.contentType
        )
    }

    suspend fun getPhotosForEncounter(encounterId: String): List<DocumentReference> {
        return dbQuery.getPhotosForEncounter(encounterId).awaitAsList().map { entity ->
            DocumentReference(
                id = entity.id,
                subjectReference = entity.patientId,
                contextReference = entity.encounterId,
                date = LocalDateTime.parse(entity.date),
                description = entity.title,
                content = Attachment(
                    contentType = entity.mimeType,
                    url = entity.filePath
                )
            )
        }
    }
}