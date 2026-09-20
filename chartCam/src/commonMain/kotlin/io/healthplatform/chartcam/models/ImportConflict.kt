/**
 * @file ImportConflict.kt
 * Models and representations for conflict detection and staging resolution during bundle import.
 */
package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.Patient

/**
 * Types of conflicts that can occur when importing a resource.
 */
enum class ConflictType {
    /** Incoming resource is identical to local record. */
    EXACT_MATCH,

    /** Incoming resource shares an existing ID but has differing content. */
    ID_COLLISION_DIFFERENT_DATA,

    /** Incoming patient has identical Medical Record Number (MRN) but different UUID. */
    MRN_COLLISION_DIFFERENT_ID,

    /** Incoming encounter or note references a patient that does not exist locally or in import. */
    ORPHAN_ENCOUNTER,
}

/**
 * Strategies for resolving a detected conflict during import.
 */
enum class ConflictResolutionStrategy {
    /** Replace the local record with the incoming record. */
    OVERWRITE_LOCAL,

    /** Retain the local record and skip the incoming record. */
    KEEP_LOCAL,

    /** Merge the two records, combining encounters and non-conflicting fields. */
    MERGE_RECORDS,

    /** Re-key the incoming record with a new UUID and import as a distinct record. */
    CREATE_AS_NEW_ID,
}

/**
 * Staged patient item pending clinician selection and conflict arbitration.
 *
 * @property incomingPatient The parsed incoming FHIR Patient.
 * @property conflictType The conflict detected against local database state.
 * @property conflictingLocalPatient The existing local patient colliding with this candidate, if any.
 * @property encounterCount Number of associated visits/encounters staged with this patient.
 * @property isSelected Whether this patient is selected for import.
 * @property resolutionStrategy Chosen conflict resolution strategy if a collision exists.
 */
data class PatientStagingItem(
    val incomingPatient: Patient,
    val conflictType: ConflictType = ConflictType.EXACT_MATCH,
    val conflictingLocalPatient: Patient? = null,
    val encounterCount: Int = 0,
    val isSelected: Boolean = true,
    val resolutionStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.KEEP_LOCAL,
)

/**
 * Summary of staged data ready for preview, category filtering, and clinician confirmation.
 *
 * @property totalResources Total number of FHIR resources in the archive.
 * @property stagedPatients Staged patient entries with conflict metadata and selection state.
 * @property stagedEncounterCount Total number of staged encounters.
 * @property stagedPhotoCount Total number of staged binary images.
 * @property stagedFormCount Total number of staged custom questionnaires.
 * @property hasConflicts True if any staged patient or resource has an unresolved conflict.
 */
data class ImportPreviewSummary(
    val totalResources: Int,
    val stagedPatients: List<PatientStagingItem>,
    val stagedEncounterCount: Int,
    val stagedPhotoCount: Int,
    val stagedFormCount: Int,
    val hasConflicts: Boolean,
)
