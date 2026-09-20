/**
 * @file FhirSearchIndexer.kt
 * Contains declarations for FhirSearchIndexer.kt.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Media
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.search.BundleSearchParams
import dev.ohs.fhir.model.r4.search.DeviceSearchParams
import dev.ohs.fhir.model.r4.search.DocumentReferenceSearchParams
import dev.ohs.fhir.model.r4.search.EncounterSearchParams
import dev.ohs.fhir.model.r4.search.MediaSearchParams
import dev.ohs.fhir.model.r4.search.ObservationSearchParams
import dev.ohs.fhir.model.r4.search.PatientSearchParams
import dev.ohs.fhir.model.r4.search.PractitionerSearchParams
import dev.ohs.fhir.model.r4.search.ProvenanceSearchParams
import dev.ohs.fhir.model.r4.search.QuestionnaireResponseSearchParams
import dev.ohs.fhir.model.r4.search.QuestionnaireSearchParams
import io.healthplatform.chartcam.database.ChartCamQueries
import io.healthplatform.chartcam.utils.flatMap
import io.healthplatform.chartcam.utils.runSuspendCatching

/**
 * Orchestrates FHIR Search Parameter extraction and indexing.
 * Delegates reflection and type traversal to [FhirSearchIndexerEngine].
 */
object FhirSearchIndexer {
    /**
     * Extracts and persists search indices for a FHIR [Resource] into SQLite.
     *
     * @param dbQuery The [ChartCamQueries] database queries handle.
     * @param resource The FHIR [Resource] to index.
     * @param resourceType The resource type name (e.g. "Patient", "Encounter").
     * @param resourceId The unique identifier of the resource.
     * @return A [Result] indicating success or failure.
     */
    suspend fun indexResource(
        dbQuery: ChartCamQueries,
        resource: Resource,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        runSuspendCatching {
            dbQuery.deleteStringIndices(resourceType, resourceId)
            dbQuery.deleteTokenIndices(resourceType, resourceId)
            dbQuery.deleteReferenceIndices(resourceType, resourceId)
            dbQuery.deleteDateIndices(resourceType, resourceId)
        }.flatMap {
            when (resource) {
                is Patient -> indexPatient(dbQuery, resource, resourceType, resourceId)
                is Encounter -> indexEncounter(dbQuery, resource, resourceType, resourceId)
                is DocumentReference -> indexDocumentReference(dbQuery, resource, resourceType, resourceId)
                is QuestionnaireResponse -> indexQuestionnaireResponse(dbQuery, resource, resourceType, resourceId)
                is Questionnaire -> indexQuestionnaire(dbQuery, resource, resourceType, resourceId)
                is Practitioner -> indexPractitioner(dbQuery, resource, resourceType, resourceId)
                is Device -> indexDevice(dbQuery, resource, resourceType, resourceId)
                is Provenance -> indexProvenance(dbQuery, resource, resourceType, resourceId)
                is Observation -> indexObservation(dbQuery, resource, resourceType, resourceId)
                is Media -> indexMedia(dbQuery, resource, resourceType, resourceId)
                is Bundle -> indexBundle(dbQuery, resource, resourceType, resourceId)
                else -> {
                    FhirSearchIndexerEngine
                        .indexResourceGeneric(
                            dbQuery,
                            resource,
                            resourceType,
                            resourceId,
                            emptyList(),
                        )
                }
            }
        }

    /**
     * Indexes a [Patient] resource using [PatientSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param patient The [Patient] resource.
     * @param resourceType The resource type name.
     * @param resourceId The patient ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexPatient(
        dbQuery: ChartCamQueries,
        patient: Patient,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                patient,
                resourceType,
                resourceId,
                PatientSearchParams.all,
            )

    /**
     * Indexes an [Encounter] resource using [EncounterSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param encounter The [Encounter] resource.
     * @param resourceType The resource type name.
     * @param resourceId The encounter ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexEncounter(
        dbQuery: ChartCamQueries,
        encounter: Encounter,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> {
        encounter.participant.mapNotNull { it.individual?.reference?.value }.forEach { ref ->
            if (!ref.startsWith("Practitioner/")) {
                val full = "Practitioner/$ref"
                dbQuery.insertReferenceIndex(resourceType, resourceId, "practitioner", full)
                dbQuery.insertReferenceIndex(resourceType, resourceId, "practitioner", ref)
            }
        }
        return FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                encounter,
                resourceType,
                resourceId,
                EncounterSearchParams.all,
            )
    }

    /**
     * Indexes a [DocumentReference] resource using [DocumentReferenceSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param doc The [DocumentReference] resource.
     * @param resourceType The resource type name.
     * @param resourceId The document reference ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexDocumentReference(
        dbQuery: ChartCamQueries,
        doc: DocumentReference,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                doc,
                resourceType,
                resourceId,
                DocumentReferenceSearchParams.all,
            )

    /**
     * Indexes a [QuestionnaireResponse] resource using [QuestionnaireResponseSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param qr The [QuestionnaireResponse] resource.
     * @param resourceType The resource type name.
     * @param resourceId The questionnaire response ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexQuestionnaireResponse(
        dbQuery: ChartCamQueries,
        qr: QuestionnaireResponse,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                qr,
                resourceType,
                resourceId,
                QuestionnaireResponseSearchParams.all,
            )

    /**
     * Indexes a [Questionnaire] resource using [QuestionnaireSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param q The [Questionnaire] resource.
     * @param resourceType The resource type name.
     * @param resourceId The questionnaire ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexQuestionnaire(
        dbQuery: ChartCamQueries,
        q: Questionnaire,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                q,
                resourceType,
                resourceId,
                QuestionnaireSearchParams.all,
            )

    /**
     * Indexes a [Practitioner] resource using [PractitionerSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param practitioner The [Practitioner] resource.
     * @param resourceType The resource type name.
     * @param resourceId The practitioner ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexPractitioner(
        dbQuery: ChartCamQueries,
        practitioner: Practitioner,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                practitioner,
                resourceType,
                resourceId,
                PractitionerSearchParams.all,
            )

    /**
     * Indexes a [Device] resource using [DeviceSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param device The [Device] resource.
     * @param resourceType The resource type name.
     * @param resourceId The device ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexDevice(
        dbQuery: ChartCamQueries,
        device: Device,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                device,
                resourceType,
                resourceId,
                DeviceSearchParams.all,
            )

    /**
     * Indexes a [Provenance] resource using [ProvenanceSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param provenance The [Provenance] resource.
     * @param resourceType The resource type name.
     * @param resourceId The provenance ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexProvenance(
        dbQuery: ChartCamQueries,
        provenance: Provenance,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> {
        provenance.target.mapNotNull { it.reference?.value }.forEach { ref ->
            if (!ref.startsWith("Encounter/")) {
                val full = "Encounter/$ref"
                dbQuery.insertReferenceIndex(resourceType, resourceId, "encounter", full)
                dbQuery.insertReferenceIndex(resourceType, resourceId, "encounter", ref)
            }
        }
        return FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                provenance,
                resourceType,
                resourceId,
                ProvenanceSearchParams.all,
            )
    }

    /**
     * Indexes an [Observation] resource using [ObservationSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param observation The [Observation] resource.
     * @param resourceType The resource type name.
     * @param resourceId The observation ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexObservation(
        dbQuery: ChartCamQueries,
        observation: Observation,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                observation,
                resourceType,
                resourceId,
                ObservationSearchParams.all,
            )

    /**
     * Indexes a [Media] resource using [MediaSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param media The [Media] resource.
     * @param resourceType The resource type name.
     * @param resourceId The media ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexMedia(
        dbQuery: ChartCamQueries,
        media: Media,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                media,
                resourceType,
                resourceId,
                MediaSearchParams.all,
            )

    /**
     * Indexes a [Bundle] resource using [BundleSearchParams].
     *
     * @param dbQuery The queries handle.
     * @param bundle The [Bundle] resource.
     * @param resourceType The resource type name.
     * @param resourceId The bundle ID.
     * @return A [Result] indicating success or failure.
     */
    private suspend fun indexBundle(
        dbQuery: ChartCamQueries,
        bundle: Bundle,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexerEngine
            .indexResourceGeneric(
                dbQuery,
                bundle,
                resourceType,
                resourceId,
                BundleSearchParams.all,
            )
}
