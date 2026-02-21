package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import okio.ByteString.Companion.decodeBase64

@Serializable
data class DatabaseExport(
    val practitioners: List<PractitionerExport>,
    val patients: List<PatientExport>,
    val encounters: List<EncounterExport>,
    val documentReferences: List<DocumentReferenceExport>,
    val files: List<FileExport>
)

@Serializable
data class PractitionerExport(
    val id: String,
    val family: String,
    val given: String,
    val active: Boolean
)

@Serializable
data class PatientExport(
    val id: String,
    val family: String,
    val given: String,
    val birthDate: String,
    val mrn: String,
    val gender: String,
    val managingOrganization: String?
)

@Serializable
data class EncounterExport(
    val id: String,
    val patientId: String,
    val practitionerId: String,
    val date: String,
    val status: String,
    val type: String,
    val notes: String?
)

@Serializable
data class DocumentReferenceExport(
    val id: String,
    val encounterId: String,
    val patientId: String,
    val type: String,
    val category: String,
    val date: String,
    val title: String?,
    val filePath: String,
    val mimeType: String
)

@Serializable
data class FileExport(
    val name: String,
    val base64Data: String
)

class ExportImportService(
    private val database: ChartCamDatabase,
    private val fileStorage: FileStorage
) {
    suspend fun exportData(): String {
        val queries = database.chartCamQueries
        
        val practitioners = queries.getAllPractitioners().awaitAsList().map {
            PractitionerExport(it.id, it.family, it.given, it.active)
        }
        val patients = queries.getAllPatients().awaitAsList().map {
            PatientExport(it.id, it.family, it.given, it.birthDate, it.mrn, it.gender, it.managingOrganization)
        }
        val encounters = queries.getAllEncounters().awaitAsList().map {
            EncounterExport(it.id, it.patientId, it.practitionerId, it.date, it.status, it.type, it.notes)
        }
        val documentReferences = queries.getAllDocumentReferences().awaitAsList().map {
            DocumentReferenceExport(it.id, it.encounterId, it.patientId, it.type, it.category, it.date, it.title, it.filePath, it.mimeType)
        }
        
        val files = documentReferences.mapNotNull { doc ->
            try {
                val bytes = fileStorage.readImage(doc.filePath)
                val fileName = doc.filePath.substringAfterLast("/")
                FileExport(fileName, bytes.toByteString().base64())
            } catch (e: Exception) {
                null
            }
        }
        
        val export = DatabaseExport(
            practitioners,
            patients,
            encounters,
            documentReferences,
            files
        )
        
        return Json.encodeToString(export)
    }

    suspend fun importData(jsonData: String) {
        val export = Json.decodeFromString<DatabaseExport>(jsonData)
        val queries = database.chartCamQueries
        
        val pathMap = mutableMapOf<String, String>()
        for (file in export.files) {
            val bytes = file.base64Data.decodeBase64()?.toByteArray()
            if (bytes != null) {
                val newPath = fileStorage.saveImage(file.name, bytes)
                pathMap[file.name] = newPath
            }
        }
        
        for (p in export.practitioners) {
            queries.insertPractitioner(p.id, p.family, p.given, p.active)
        }
        for (p in export.patients) {
            queries.insertPatient(p.id, p.family, p.given, p.birthDate, p.mrn, p.gender, p.managingOrganization)
        }
        for (e in export.encounters) {
            queries.insertEncounter(e.id, e.patientId, e.practitionerId, e.date, e.status, e.notes)
        }
        for (d in export.documentReferences) {
            val oldName = d.filePath.substringAfterLast("/")
            val newPath = pathMap[oldName] ?: d.filePath
            queries.insertDocumentReference(d.id, d.encounterId, d.patientId, d.type, d.category, d.date, d.title, newPath, d.mimeType)
        }
    }
}