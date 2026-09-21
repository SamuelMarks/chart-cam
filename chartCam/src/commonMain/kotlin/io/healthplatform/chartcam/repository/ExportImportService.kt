@file:Suppress("TooGenericExceptionCaught", "LargeClass", "ReturnCount")
/**
 * @file ExportImportService.kt
 * Contains declarations for ExportImportService.kt.
 *
 * Provides functionality to export and import FHIR resources and associated binaries
 * (like photos) to and from a password-encrypted JSON payload.
 */

package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Binary
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.ConflictResolutionStrategy
import io.healthplatform.chartcam.models.ConflictType
import io.healthplatform.chartcam.models.ImportCategory
import io.healthplatform.chartcam.models.ImportFilterOptions
import io.healthplatform.chartcam.models.ImportPreviewSummary
import io.healthplatform.chartcam.models.PatientStagingItem
import io.healthplatform.chartcam.models.createFhirBinary
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.utils.CryptoService
import io.healthplatform.chartcam.utils.UUID
import io.healthplatform.chartcam.utils.decryptCatching
import io.healthplatform.chartcam.utils.runSuspendCatching
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

    /**
     * Exports local FHIR resources and associated binaries to an encrypted JSON string.
     *
     * @param password The password used to encrypt the resulting string.
     * @param exportAll Whether to export all data, or limit it to the data associated with [practitionerId].
     * @param practitionerId An optional practitioner ID to filter the exported data.
     * @return A [Result] enclosing the password-encrypted JSON string representing the exported FHIR Bundle.
     */
    open suspend fun exportData(
        password: String,
        exportAll: Boolean = true,
        practitionerId: String? = null,
    ): Result<String> {
        if (!isValidPassword(password)) {
            return Result.failure(
                IllegalArgumentException("Encryption password must not be empty or weak (minimum 6 characters)."),
            )
        }
        return runSuspendCatching {
            val entries = mutableListOf<Bundle.Entry>()
            addBaseResources(entries)
            addQuestionnaires(entries)
            val encounters = addEncounters(entries, exportAll, practitionerId)
            addPatients(entries, exportAll, practitionerId, encounters)
            addDocumentReferences(entries, exportAll, practitionerId, encounters)
            addQuestionnaireResponses(entries, exportAll, practitionerId, encounters)
            addProvenances(entries, exportAll, practitionerId)
            val bundle =
                Bundle(
                    type = Enumeration(value = Bundle.BundleType.Collection),
                    entry = entries,
                )

            val jsonData =
                io.healthplatform.chartcam.fhir.FhirJsonParser.compactJson
                    .encodeToString(Bundle.serializer(), bundle)

            cryptoService.encrypt(jsonData, password)
        }
    }

    /**
     * Helper for exporting Questionnaires.
     * @param entries The destination entry list.
     */
    private suspend fun addQuestionnaires(entries: MutableList<Bundle.Entry>) {
        fhirRepo.getAllQuestionnaires().forEach { resource ->
            entries.add(Bundle.Entry(resource = resource))
        }
    }

    /**
     * Helper for exporting.
     * @param entries The destination entry list.
     */
    private suspend fun addBaseResources(entries: MutableList<Bundle.Entry>) {
        fhirRepo.getAllDevices().forEach { resource ->
            entries.add(Bundle.Entry(resource = resource))
        }
        fhirRepo.getAllPractitioners().forEach { resource ->
            entries.add(Bundle.Entry(resource = resource))
        }
    }

    /**
     * Extracts a bare Encounter ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Encounter ID or an empty string.
     */
    private fun extractEncounterReferenceId(ref: Reference?): String {
        val v =
            if (ref != null) {
                val r = ref.reference
                if (r != null) r.value else null
            } else {
                null
            }
        return if (v != null) v.removePrefix("Encounter/") else ""
    }

    /**
     * Extracts a bare Practitioner ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Practitioner ID or an empty string.
     */
    private fun extractPractitionerReferenceId(ref: Reference?): String {
        val v =
            if (ref != null) {
                val r = ref.reference
                if (r != null) r.value else null
            } else {
                null
            }
        return if (v != null) v.removePrefix("Practitioner/") else ""
    }

    /**
     * Checks if managing organization reference contains the specified practitioner ID.
     *
     * @param orgRef The organization reference element.
     * @param practitionerId The practitioner identifier to match.
     * @return True if the reference matches the practitioner ID, false otherwise.
     */
    private fun organizationMatchesPractitioner(orgRef: Reference?, practitionerId: String): Boolean {
        val v =
            if (orgRef != null) {
                val r = orgRef.reference
                if (r != null) r.value else null
            } else {
                null
            }
        return v != null && v.contains(practitionerId)
    }

    /**
     * Helper for exporting.
     * @param entries The destination entry list.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     * @param encounters The list of encounters for the practitioner.
     */
    private suspend fun addPatients(
        entries: MutableList<Bundle.Entry>,
        exportAll: Boolean,
        practitionerId: String?,
        encounters: List<Encounter>,
    ) {
        val patients =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllPatients()
            } else {
                val encPatientIds =
                    encounters
                        .mapNotNull { enc ->
                            extractPatientReferenceId(enc.subject).ifEmpty { null }
                        }.toSet()
                fhirRepo.getAllPatients().filter { p ->
                    val rawId = p.id
                    val pid = if (rawId != null) rawId.removePrefix("Patient/") else ""
                    encPatientIds.contains(pid) ||
                        organizationMatchesPractitioner(p.managingOrganization, practitionerId)
                }
            }
        patients.forEach { resource ->
            entries.add(Bundle.Entry(resource = resource))
        }
    }

    /**
     * Helper for exporting.
     * @param entries The destination entry list.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     * @return The result.
     */
    private suspend fun addEncounters(
        entries: MutableList<Bundle.Entry>,
        exportAll: Boolean,
        practitionerId: String?,
    ): List<Encounter> {
        val cleanPrac = practitionerId?.removePrefix("Practitioner/")
        val encounters =
            if (exportAll || cleanPrac == null) {
                fhirRepo.getAllEncounters()
            } else {
                fhirRepo.getAllEncounters().filter { enc ->
                    enc.participant.any { p ->
                        extractPractitionerReferenceId(p.individual) == cleanPrac
                    }
                }
            }
        encounters.forEach { resource ->
            entries.add(Bundle.Entry(resource = resource))
        }
        return encounters
    }

    /**
     * Helper for exporting.
     * @param entries The destination entry list.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     * @param encounters The encounters.
     */
    private suspend fun addDocumentReferences(
        entries: MutableList<Bundle.Entry>,
        exportAll: Boolean,
        practitionerId: String?,
        encounters: List<Encounter>,
    ) {
        val documentReferences =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllDocumentReferences()
            } else {
                val encIds = encounters.mapNotNull { it.id?.removePrefix("Encounter/") }.toSet()
                fhirRepo.getAllDocumentReferences().filter { doc ->
                    val encList = doc.context?.encounter
                    val ref =
                        if (!encList.isNullOrEmpty()) {
                            extractEncounterReferenceId(encList[0]).ifEmpty { null }
                        } else {
                            null
                        }
                    ref != null && encIds.contains(ref)
                }
            }
        documentReferences.forEach { resource ->
            entries.add(Bundle.Entry(resource = resource))
            runCatching {
                val contentList = resource.content
                if (contentList.isEmpty()) return@forEach
                val att = contentList[0].attachment
                val urlObj = att.url
                val urlVal = if (urlObj != null) urlObj.value else null
                if (urlVal.isNullOrBlank()) return@forEach
                val filePath = urlVal
                val bytes = fileStorage.readImage(filePath)
                val base64Data = bytes.toByteString().base64()
                val ctObj = att.contentType
                val ctVal = if (ctObj != null) ctObj.value else null
                val mimeType = if (ctVal != null && ctVal.isNotBlank()) ctVal else "image/jpeg"
                val fileName = filePath.substringAfterLast("/")
                val binary = createFhirBinary(id = fileName, contentTypeStr = mimeType, base64Data = base64Data)
                entries.add(Bundle.Entry(resource = binary))
            }.onFailure { e ->
                println("Failed to export binary: ${e.message}")
            }
        }
    }

    /**
     * Helper for exporting.
     * @param entries The destination entry list.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     * @param encounters The encounters.
     */
    private suspend fun addQuestionnaireResponses(
        entries: MutableList<Bundle.Entry>,
        exportAll: Boolean,
        practitionerId: String?,
        encounters: List<Encounter>,
    ) {
        val questionnaireResponses =
            if (exportAll || practitionerId == null) {
                fhirRepo.getAllQuestionnaireResponses()
            } else {
                val encIds = encounters.mapNotNull { it.id?.removePrefix("Encounter/") }.toSet()
                fhirRepo.getAllQuestionnaireResponses().filter { qr ->
                    val ref = extractEncounterReferenceId(qr.encounter).ifEmpty { null }
                    ref != null && encIds.contains(ref)
                }
            }
        questionnaireResponses.forEach { resource ->
            entries.add(Bundle.Entry(resource = resource))
        }
    }

    /**
     * Helper for exporting.
     * @param entries The destination entry list.
     * @param exportAll The exportAll.
     * @param practitionerId The practitionerId.
     */
    private suspend fun addProvenances(
        entries: MutableList<Bundle.Entry>,
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
            entries.add(Bundle.Entry(resource = resource))
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
     * Builds a staged patient item identifying conflict metadata and associated visit count.
     *
     * @param incomingPatient The patient candidate from the incoming archive.
     * @param encounters All encounters present in the incoming bundle.
     * @return The constructed [PatientStagingItem].
     */
    private suspend fun buildStagingItem(
        incomingPatient: Patient,
        encounters: List<Encounter>,
    ): PatientStagingItem {
        val pid = incomingPatient.id ?: ""
        val localPatientWithId = fhirRepo.getPatient(pid)
        val mrn = incomingPatient.mrn
        val localPatientWithMrn = if (mrn.isNotBlank()) fhirRepo.getPatientByMrn(mrn) else null

        val conflictType =
            if (localPatientWithId != null) {
                if (incomingPatient == localPatientWithId) {
                    ConflictType.EXACT_MATCH
                } else {
                    ConflictType.ID_COLLISION_DIFFERENT_DATA
                }
            } else if (localPatientWithMrn != null) {
                ConflictType.MRN_COLLISION_DIFFERENT_ID
            } else {
                ConflictType.EXACT_MATCH
            }

        val encCount = encounters.count { extractPatientReferenceId(it.subject) == pid }

        val resolution =
            if (conflictType == ConflictType.EXACT_MATCH) {
                ConflictResolutionStrategy.OVERWRITE_LOCAL
            } else {
                ConflictResolutionStrategy.KEEP_LOCAL
            }

        return PatientStagingItem(
            incomingPatient = incomingPatient,
            conflictType = conflictType,
            conflictingLocalPatient = localPatientWithId ?: localPatientWithMrn,
            encounterCount = encCount,
            isSelected = true,
            resolutionStrategy = resolution,
        )
    }

    /**
     * Inspects an encrypted archive without committing changes to the database.
     * Parses the FHIR bundle and evaluates each candidate patient against the local database
     * to identify collisions, MRN clashes, and encounter metrics.
     *
     * @param encryptedData The password-encrypted JSON string representing a FHIR Bundle.
     * @param password The password used to decrypt the data.
     * @return A [Result] enclosing the [ImportPreviewSummary] or an error.
     */
    open suspend fun inspectArchive(
        encryptedData: String,
        password: String,
    ): Result<ImportPreviewSummary> {
        if (!isValidPassword(password)) {
            return Result.failure(
                IllegalArgumentException("Decryption password must not be empty or weak (minimum 6 characters)."),
            )
        }
        val decryptResult = cryptoService.decryptCatching(encryptedData, password)
        val jsonData =
            decryptResult.fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) },
            )
        val decodeResult =
            io.healthplatform.chartcam.fhir.FhirJsonParser
                .decodeTypedResource(Bundle.serializer(), jsonData)
        val bundle =
            decodeResult.fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) },
            )

        val resources = bundle.entry.mapNotNull { it.resource }
        val patients = resources.filterIsInstance<Patient>()
        val encounters = resources.filterIsInstance<Encounter>()
        val docs = resources.filterIsInstance<DocumentReference>()
        val questionnaires = resources.filterIsInstance<Questionnaire>()

        val stagedPatients = patients.map { buildStagingItem(it, encounters) }
        val hasConflicts = stagedPatients.any { it.conflictType != ConflictType.EXACT_MATCH }

        return Result.success(
            ImportPreviewSummary(
                totalResources = resources.size,
                stagedPatients = stagedPatients,
                stagedEncounterCount = encounters.size,
                stagedPhotoCount = docs.size,
                stagedFormCount = questionnaires.size,
                hasConflicts = hasConflicts,
            ),
        )
    }

    /**
     * Ingests a single patient according to chosen conflict resolution strategy.
     *
     * @param incomingPatient The incoming patient resource.
     * @param strategy The conflict resolution strategy.
     * @param localConflict The conflicting local patient, if any.
     * @return The canonical ID to which child resources should be mapped, or null if skipped.
     */
    internal suspend fun ingestPatient(
        incomingPatient: Patient,
        strategy: ConflictResolutionStrategy,
        localConflict: Patient?,
    ): String? {
        val pid = incomingPatient.id ?: ""
        return when (strategy) {
            ConflictResolutionStrategy.KEEP_LOCAL -> {
                if (localConflict != null) {
                    null
                } else {
                    fhirRepo.savePatient(incomingPatient, isLocalChange = false)
                    pid
                }
            }
            ConflictResolutionStrategy.OVERWRITE_LOCAL -> {
                fhirRepo.savePatient(incomingPatient, isLocalChange = false)
                pid
            }
            ConflictResolutionStrategy.MERGE_RECORDS -> {
                if (localConflict != null) {
                    val merged = PatientMergeEngine().mergeDemographics(localConflict, incomingPatient)
                    fhirRepo.savePatient(merged, isLocalChange = false)
                    val localId = localConflict.id
                    if (localId != null) localId else pid
                } else {
                    fhirRepo.savePatient(incomingPatient, isLocalChange = false)
                    pid
                }
            }
            ConflictResolutionStrategy.CREATE_AS_NEW_ID -> {
                val newPatientId = UUID.randomUUID()
                val rekeyed =
                    incomingPatient
                        .toBuilder()
                        .apply {
                            id = newPatientId
                        }.build()
                fhirRepo.savePatient(rekeyed, isLocalChange = false)
                newPatientId
            }
        }
    }

    /**
     * Resolves incoming patient entries and maps original IDs to canonical destination IDs.
     *
     * @param patients The list of candidate incoming patients.
     * @param selectedPatientIds Optional set of patient IDs chosen for import.
     * @param resolutionMap Mapping of chosen conflict resolution strategies.
     * @return Map of incoming patient ID to persisted target ID, or null if skipped.
     */
    private suspend fun processPatientBatch(
        patients: List<Patient>,
        selectedPatientIds: Set<String>?,
        resolutionMap: Map<String, ConflictResolutionStrategy>,
    ): Map<String, String?> {
        val mapping = mutableMapOf<String, String?>()
        for (patient in patients) {
            val pid = patient.id ?: ""
            if (selectedPatientIds != null && !selectedPatientIds.contains(pid)) {
                mapping[pid] = null
            } else {
                val localWithId = fhirRepo.getPatient(pid)
                val mrn = patient.mrn
                val localWithMrn = if (mrn.isNotBlank()) fhirRepo.getPatientByMrn(mrn) else null
                val localConflict = localWithId ?: localWithMrn

                val defaultStrategy =
                    if (localConflict != null) {
                        ConflictResolutionStrategy.KEEP_LOCAL
                    } else {
                        ConflictResolutionStrategy.OVERWRITE_LOCAL
                    }
                val strategy = resolutionMap[pid] ?: defaultStrategy
                mapping[pid] = ingestPatient(patient, strategy, localConflict)
            }
        }
        return mapping
    }

    /**
     * Extracts a bare Patient ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Patient ID or an empty string.
     */
    private fun extractPatientReferenceId(ref: Reference?): String {
        val v =
            if (ref != null) {
                val r = ref.reference
                if (r != null) r.value else null
            } else {
                null
            }
        return if (v != null) v.removePrefix("Patient/") else ""
    }

    /**
     * Imports an encounter resource with patient re-parenting if permitted.
     *
     * @param encounter The encounter to import.
     * @param filterOptions Active categories to permit during ingestion.
     * @param patientIdMapping Map of original patient IDs to canonical IDs.
     */
    private suspend fun importEncounterResource(
        encounter: Encounter,
        filterOptions: ImportFilterOptions,
        patientIdMapping: Map<String, String?>,
    ) {
        if (!filterOptions.isCategoryEnabled(ImportCategory.ENCOUNTERS)) return
        val rawRef = extractPatientReferenceId(encounter.subject)
        val mappedId = patientIdMapping[rawRef]
        if (patientIdMapping.containsKey(rawRef) && mappedId == null) return
        val targetPid = if (mappedId != null) mappedId else rawRef
        val enc =
            if (targetPid != rawRef) {
                PatientMergeEngine().reparentEncounter(encounter, targetPid)
            } else {
                encounter
            }
        fhirRepo.saveEncounter(enc, isLocalChange = false)
    }

    /**
     * Imports a document reference resource with patient re-parenting if permitted.
     *
     * @param doc The document reference to import.
     * @param filterOptions Active categories to permit during ingestion.
     * @param patientIdMapping Map of original patient IDs to canonical IDs.
     */
    private suspend fun importDocumentResource(
        doc: DocumentReference,
        filterOptions: ImportFilterOptions,
        patientIdMapping: Map<String, String?>,
    ) {
        if (!filterOptions.isCategoryEnabled(ImportCategory.BINARY_PHOTOS)) return
        val rawRef = extractPatientReferenceId(doc.subject)
        val mappedId = patientIdMapping[rawRef]
        if (patientIdMapping.containsKey(rawRef) && mappedId == null) return
        val targetPid = if (mappedId != null) mappedId else rawRef
        val toSave =
            if (targetPid != rawRef) {
                PatientMergeEngine().reparentDocumentReference(doc, targetPid)
            } else {
                doc
            }
        fhirRepo.saveDocumentReference(toSave, isLocalChange = false)
    }

    /**
     * Imports a questionnaire response resource with patient re-parenting if permitted.
     *
     * @param qr The questionnaire response to import.
     * @param filterOptions Active categories to permit during ingestion.
     * @param patientIdMapping Map of original patient IDs to canonical IDs.
     */
    private suspend fun importNoteResource(
        qr: QuestionnaireResponse,
        filterOptions: ImportFilterOptions,
        patientIdMapping: Map<String, String?>,
    ) {
        if (!filterOptions.isCategoryEnabled(ImportCategory.CLINICAL_NOTES)) return
        val rawRef = extractPatientReferenceId(qr.subject)
        val mappedId = patientIdMapping[rawRef]
        if (patientIdMapping.containsKey(rawRef) && mappedId == null) return
        val targetPid = if (mappedId != null) mappedId else rawRef
        val toSave =
            if (targetPid != rawRef) {
                PatientMergeEngine().reparentQuestionnaireResponse(qr, targetPid)
            } else {
                qr
            }
        fhirRepo.saveQuestionnaireResponse(toSave, isLocalChange = false)
    }

    /**
     * Imports an image binary resource if binary photo category is enabled.
     *
     * @param binary The binary resource.
     * @param filterOptions Active categories to permit during ingestion.
     * @param savedImageFiles List tracking written image files for rollback.
     */
    private fun importBinaryResource(
        binary: Binary,
        filterOptions: ImportFilterOptions,
        savedImageFiles: MutableList<String>,
    ) {
        if (!filterOptions.isCategoryEnabled(ImportCategory.BINARY_PHOTOS)) return
        val dataObj = binary.data
        val dataVal = if (dataObj != null) dataObj.value else null
        val bytes =
            if (dataVal != null) {
                val bs = dataVal.decodeBase64()
                if (bs != null) bs.toByteArray() else null
            } else {
                null
            }
        val binaryId = binary.id
        if (bytes != null && !binaryId.isNullOrBlank()) {
            fileStorage.saveImage(binaryId, bytes)
            savedImageFiles.add(binaryId)
        }
    }

    /**
     * Imports an individual non-patient resource adhering to active category filters.
     *
     * @param resource The FHIR resource to import.
     * @param filterOptions Active categories to permit during ingestion.
     * @param patientIdMapping Map of original patient IDs to canonical IDs.
     * @param savedImageFiles List tracking written image files for rollback.
     */
    private suspend fun importEntryResource(
        resource: dev.ohs.fhir.model.r4.Resource,
        filterOptions: ImportFilterOptions,
        patientIdMapping: Map<String, String?>,
        savedImageFiles: MutableList<String>,
    ) {
        when (resource) {
            is Practitioner -> {
                if (filterOptions.isCategoryEnabled(ImportCategory.PRACTITIONERS)) {
                    fhirRepo.savePractitioner(resource, isLocalChange = false)
                }
            }
            is Device -> fhirRepo.saveDevice(resource, isLocalChange = false)
            is Questionnaire -> {
                if (filterOptions.isCategoryEnabled(ImportCategory.QUESTIONNAIRES)) {
                    fhirRepo.saveQuestionnaire(resource, isLocalChange = false)
                }
            }
            is Encounter -> importEncounterResource(resource, filterOptions, patientIdMapping)
            is DocumentReference -> importDocumentResource(resource, filterOptions, patientIdMapping)
            is QuestionnaireResponse -> importNoteResource(resource, filterOptions, patientIdMapping)
            is Provenance -> fhirRepo.saveProvenance(resource, isLocalChange = false)
            is Binary -> importBinaryResource(resource, filterOptions, savedImageFiles)
            else -> {}
        }
    }

    /**
     * Selectively imports data from an encrypted archive applying category filtering,
     * explicit patient selection, and interactive conflict resolution.
     *
     * @param encryptedData The encrypted archive payload.
     * @param password The decryption password.
     * @param filterOptions Active categories to permit during ingestion.
     * @param selectedPatientIds Optional set of specific incoming patient IDs to ingest.
     * @param resolutionMap Strategy mappings keyed by incoming patient ID.
     * @return A [Result] indicating success or failure of the selective import.
     */
    open suspend fun importDataSelective(
        encryptedData: String,
        password: String,
        filterOptions: ImportFilterOptions = ImportFilterOptions.all(),
        selectedPatientIds: Set<String>? = null,
        resolutionMap: Map<String, ConflictResolutionStrategy> = emptyMap(),
    ): Result<Unit> {
        if (!isValidPassword(password)) {
            return Result.failure(
                IllegalArgumentException("Decryption password must not be empty or weak (minimum 6 characters)."),
            )
        }
        val decryptResult = cryptoService.decryptCatching(encryptedData, password)
        val jsonData =
            decryptResult.fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) },
            )
        val decodeResult =
            io.healthplatform.chartcam.fhir.FhirJsonParser
                .decodeTypedResource(Bundle.serializer(), jsonData)
        val bundle =
            decodeResult.fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) },
            )

        val savedImageFiles = mutableListOf<String>()
        val importBatchResult =
            runSuspendCatching {
                val patientMapping =
                    if (filterOptions.isCategoryEnabled(ImportCategory.PATIENTS)) {
                        val patients = bundle.entry.mapNotNull { it.resource as? Patient }
                        processPatientBatch(patients, selectedPatientIds, resolutionMap)
                    } else {
                        emptyMap()
                    }

                for (entry in bundle.entry) {
                    val res = entry.resource ?: continue
                    if (res !is Patient) {
                        importEntryResource(res, filterOptions, patientMapping, savedImageFiles)
                    }
                }
            }
        val failureEx = importBatchResult.exceptionOrNull()
        return if (failureEx != null) {
            for (savedFile in savedImageFiles) {
                runCatching { fileStorage.deleteImage(savedFile) }
            }
            Result.failure(failureEx)
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Decrypts the provided JSON string and imports the contained FHIR Bundle into the local database.
     * Supported resources include `Device`, `Practitioner`, `Patient`, `Encounter`, `DocumentReference`,
     * `Questionnaire`, `QuestionnaireResponse`, `Provenance`, and `Binary`.
     * `Binary` resources are specifically used to carry raw image bytes (Base64 encoded) and will be
     * decoded and written to the local [FileStorage].
     *
     * @param encryptedData The password-encrypted JSON string representing a FHIR Bundle.
     * @param password The password used to decrypt the data.
     * @return A [Result] indicating success or failure of the import.
     */
    open suspend fun importData(
        encryptedData: String,
        password: String,
    ): Result<Unit> =
        importDataSelective(
            encryptedData = encryptedData,
            password = password,
            filterOptions = ImportFilterOptions.all(),
        )

    /**
     * Exports data to a temporary encrypted payload and executes an operation,
     * guaranteeing complete cleanup of temporary cache files in both success and failure execution paths.
     *
     * @param T The return type of the executed block.
     * @param password The password for encryption.
     * @param block The block to execute with the encrypted data.
     * @return A [Result] enclosing the result of the block execution or an error.
     */
    open suspend fun <T> executeWithTemporaryCleanup(
        password: String,
        block: suspend (encryptedData: String) -> T,
    ): Result<T> {
        val exportResult = exportData(password)
        val data =
            exportResult.getOrElse {
                fileStorage.clearCache()
                return Result.failure(it)
            }
        val result = runSuspendCatching { block(data) }
        fileStorage.clearCache()
        return result
    }

    /**
     * Imports data from an encrypted archive, guaranteeing complete cleanup of
     * temporary cache files on completion or error.
     *
     * @param encryptedData The encrypted archive string.
     * @param password The decryption password.
     * @return A [Result] indicating success or failure of the import.
     */
    open suspend fun importDataWithCleanup(
        encryptedData: String,
        password: String,
    ): Result<Unit> {
        val result = importData(encryptedData, password)
        fileStorage.clearCache()
        return result
    }
}
