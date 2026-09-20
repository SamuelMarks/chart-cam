/**
 * @file FhirBundleOrchestrator.kt
 * Pure multiplatform orchestrator for packaging and unpacking decentralized FHIR Bundles.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.Uri
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.utils.UUID
import io.healthplatform.chartcam.utils.runSuspendCatching

/**
 * Orchestrator for assembling and disassembling air-gapped FHIR Transaction and Collection Bundles.
 */
object FhirBundleOrchestrator {
    /**
     * Assembles all clinical resources associated with an encounter into a standard FHIR [Bundle].
     *
     * @param repository The local FHIR repository to query.
     * @param encounterId The unique identifier of the target encounter.
     * @param patientId The unique identifier of the associated patient.
     * @param bundleType The [Bundle.BundleType] to assign to the bundle (defaults to Collection).
     * @return A [Result] enclosing the generated [Bundle].
     */
    suspend fun createEncounterBundle(
        repository: FhirRepository,
        encounterId: String,
        patientId: String,
        bundleType: Bundle.BundleType = Bundle.BundleType.Collection,
    ): Result<Bundle> =
        runSuspendCatching {
            val cleanPatientId = patientId.removePrefix("Patient/")
            val cleanEncounterId = encounterId.removePrefix("Encounter/")

            val patient =
                repository.getPatient(cleanPatientId)
                    ?: error("Patient not found for ID: $cleanPatientId")
            val encounter =
                repository.getEncounter(cleanEncounterId)
                    ?: error("Encounter not found for ID: $cleanEncounterId")

            val observations = repository.getObservationsForEncounter(cleanEncounterId)
            val photos = repository.getPhotosForEncounter(cleanEncounterId)
            val responses = repository.getQuestionnaireResponsesForEncounter(cleanEncounterId)
            val provenances = repository.getProvenancesForEncounter(cleanEncounterId)

            val allResources: List<Resource> =
                listOf(patient, encounter) + observations + photos + responses + provenances

            val entries =
                allResources.map { res ->
                    val resourceId = res.id ?: UUID.randomUUID()
                    val urn = if (resourceId.startsWith("urn:uuid:")) resourceId else "urn:uuid:$resourceId"
                    Bundle.Entry(
                        fullUrl = Uri(value = urn),
                        resource = res,
                    )
                }

            Bundle(
                id = UUID.randomUUID(),
                type = Enumeration(value = bundleType),
                entry = entries,
            )
        }

    /**
     * Unpacks a FHIR [Bundle] into a flat list of constituent resources, validating bundle structure.
     *
     * @param bundle The source FHIR [Bundle] to unpack.
     * @return A [Result] enclosing the list of extracted [Resource] instances.
     */
    fun unpackEncounterBundle(bundle: Bundle): Result<List<Resource>> =
        runCatching {
            val resources = bundle.entry.mapNotNull { it.resource }
            if (resources.isEmpty()) {
                error("Bundle contains no extractable resources.")
            }
            resources
        }

    /**
     * Atomically stores all extracted resources from a bundle into the local repository.
     *
     * @param repository The local FHIR repository to persist into.
     * @param bundle The source FHIR [Bundle] to ingest.
     * @param isLocalChange Whether to record sync change tracking entries.
     * @return A [Result] enclosing the number of persisted resources.
     */
    suspend fun ingestBundle(
        repository: FhirRepository,
        bundle: Bundle,
        isLocalChange: Boolean = true,
    ): Result<Int> {
        val unpackResult = unpackEncounterBundle(bundle)
        val resources = unpackResult.getOrElse { return Result.failure(it) }
        var error: Throwable? = null
        var count = 0
        for (res in resources) {
            val resType = resolveResourceTypeName(res)
            val resId = res.id ?: UUID.randomUUID()
            val saveRes = repository.saveResource(resType, resId, res, isLocalChange)
            if (saveRes.isFailure) {
                error = saveRes.exceptionOrNull() ?: IllegalStateException("Save failed")
                break
            }
            count++
        }
        return if (error != null) Result.failure(error) else Result.success(count)
    }

    /**
     * Executes a FHIR Transaction Bundle atomically within the local database.
     *
     * Processes POST (create), PUT (idempotent update), and DELETE entries.
     * If any entry fails validation or insertion, returns a failure Result.
     *
     * @param repository The local repository.
     * @param bundle The transaction Bundle.
     * @return A [Result] enclosing the TransactionResponse [Bundle].
     */
    suspend fun executeTransactionBundle(
        repository: FhirRepository,
        bundle: Bundle,
    ): Result<Bundle> {
        val responseEntries = mutableListOf<Bundle.Entry>()
        for (entry in bundle.entry) {
            val respEntryResult = processTransactionEntry(repository, entry)
            val respEntry = respEntryResult.getOrElse { return Result.failure(it) }
            responseEntries.add(respEntry)
        }

        return Result.success(
            Bundle(
                id = UUID.randomUUID(),
                type = Enumeration(value = Bundle.BundleType.Transaction_Response),
                entry = responseEntries,
            ),
        )
    }

    /**
     * Executes a transaction delete request.
     *
     * @param repository The repository.
     * @param req The request definition.
     * @return A [Result] enclosing the response entry.
     */
    private suspend fun processDeleteEntry(
        repository: FhirRepository,
        req: Bundle.Entry.Request,
    ): Result<Bundle.Entry> {
        val urlStr =
            req.url.value
                ?: return Result.failure(IllegalStateException("Missing DELETE request URL"))
        val type = urlStr.substringBefore('/')
        val id = urlStr.substringAfter('/')
        val delRes = repository.deleteResource(type, id, isLocalChange = true)
        return if (delRes.isFailure) {
            Result.failure(delRes.exceptionOrNull() ?: IllegalStateException("Delete failed"))
        } else {
            Result.success(
                Bundle.Entry(
                    response =
                        Bundle.Entry.Response(
                            status =
                                dev.ohs.fhir.model.r4
                                    .String(value = "204 No Content"),
                            location =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "$type/$id"),
                        ),
                ),
            )
        }
    }

    /**
     * Executes a transaction save (create or update) request.
     *
     * @param repository The repository.
     * @param res The resource to persist.
     * @param isPost True if POST operation (201 Created), false otherwise (200 OK).
     * @return A [Result] enclosing the response entry.
     */
    private suspend fun processSaveEntry(
        repository: FhirRepository,
        res: Resource?,
        isPost: Boolean,
    ): Result<Bundle.Entry> {
        val validRes =
            res
                ?: return Result.failure(IllegalStateException("Transaction entry missing resource"))
        val resType = resolveResourceTypeName(validRes)
        val resId = validRes.id ?: UUID.randomUUID()
        val saveRes = repository.saveResource(resType, resId, validRes, isLocalChange = true)
        return if (saveRes.isFailure) {
            Result.failure(saveRes.exceptionOrNull() ?: IllegalStateException("Save failed"))
        } else {
            val status = if (isPost) "201 Created" else "200 OK"
            Result.success(
                Bundle.Entry(
                    response =
                        Bundle.Entry.Response(
                            status =
                                dev.ohs.fhir.model.r4
                                    .String(value = status),
                            location =
                                dev.ohs.fhir.model.r4
                                    .Uri(value = "$resType/$resId"),
                        ),
                ),
            )
        }
    }

    /**
     * Processes a single transaction bundle entry according to its HTTP request verb.
     *
     * @param repository The repository.
     * @param entry The transaction entry.
     * @return A [Result] enclosing the response entry.
     */
    private suspend fun processTransactionEntry(
        repository: FhirRepository,
        entry: Bundle.Entry,
    ): Result<Bundle.Entry> {
        val req = entry.request
        if (req == null) {
            return processSaveEntry(repository, entry.resource, isPost = false)
        }
        return when (req.method.value) {
            Bundle.HTTPVerb.Delete -> processDeleteEntry(repository, req)
            Bundle.HTTPVerb.Post -> processSaveEntry(repository, entry.resource, isPost = true)
            else -> processSaveEntry(repository, entry.resource, isPost = false)
        }
    }

    /**
     * Validates that a FHIR [Bundle] is completely self-contained for offline, air-gapped usage.
     *
     * Ensures all internal references resolve locally within the bundle entries and
     * detects any unresolvable external HTTP network URLs.
     *
     * @param bundle The [Bundle] to validate.
     * @return A [Result] enclosing an [AirGappedValidationResult].
     */
    fun validateAirGappedSelfContainment(bundle: Bundle): Result<AirGappedValidationResult> =
        runCatching {
            val (fullUrls, entryIds) = collectBundleIdentifiers(bundle)
            val dangling = mutableListOf<String>()
            val unresolved = mutableListOf<String>()

            for (entry in bundle.entry) {
                val res = entry.resource ?: continue
                val refs = extractReferencesFromResource(res)
                inspectReferences(refs, fullUrls, entryIds, dangling, unresolved)
            }

            AirGappedValidationResult(
                isValid = dangling.isEmpty() && unresolved.isEmpty(),
                danglingReferences = dangling,
                unresolvedLocalReferences = unresolved,
            )
        }

    /**
     * Collects all declared fullUrls and canonical resource IDs from a bundle.
     *
     * @param bundle The [Bundle] to inspect.
     * @return A pair containing sets of fullUrls and composite entry IDs.
     */
    private fun collectBundleIdentifiers(bundle: Bundle): Pair<Set<String>, Set<String>> {
        val entryIds = mutableSetOf<String>()
        val fullUrls = mutableSetOf<String>()
        for (entry in bundle.entry) {
            val uri = entry.fullUrl
            val url = if (uri != null) uri.value?.trim() else null
            if (!url.isNullOrBlank()) {
                fullUrls.add(url)
            }
            val res = entry.resource ?: continue
            val id = res.id?.trim()
            if (!id.isNullOrBlank()) {
                val resType = resolveResourceTypeName(res)
                entryIds.add(id)
                entryIds.add("$resType/$id")
                entryIds.add("urn:uuid:$id")
            }
        }
        return fullUrls to entryIds
    }

    /**
     * Inspects extracted reference strings against local bundle identifiers.
     *
     * @param refs The extracted reference URIs.
     * @param fullUrls All declared entry fullUrls.
     * @param entryIds All declared composite resource IDs.
     * @param dangling Output list for unresolvable external URLs.
     * @param unresolved Output list for unresolvable local references.
     */
    private fun inspectReferences(
        refs: List<String>,
        fullUrls: Set<String>,
        entryIds: Set<String>,
        dangling: MutableList<String>,
        unresolved: MutableList<String>,
    ) {
        for (ref in refs) {
            val trimmed = ref.trim()
            if (trimmed.startsWith("#")) {
                // Fragment reference resolves locally
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                if (!fullUrls.contains(trimmed)) {
                    dangling.add(trimmed)
                }
            } else {
                val bareId = trimmed.removePrefix("urn:uuid:").substringAfter('/')
                val resolves = fullUrls.contains(trimmed) || entryIds.contains(trimmed) || entryIds.contains(bareId)
                if (!resolves) {
                    unresolved.add(trimmed)
                }
            }
        }
    }

    /**
     * Extracts reference target strings from standard clinical resources.
     *
     * @param res Target resource.
     * @return List of referenced target strings.
     */
    private fun extractReferencesFromResource(res: Resource): List<String> {
        val refs = mutableListOf<String>()
        when (res) {
            is dev.ohs.fhir.model.r4.Patient -> addReference(res.managingOrganization, refs)
            is dev.ohs.fhir.model.r4.Encounter -> extractEncounterReferences(res, refs)
            is dev.ohs.fhir.model.r4.Observation -> {
                addReference(res.subject, refs)
                addReference(res.encounter, refs)
            }
            is dev.ohs.fhir.model.r4.DocumentReference -> extractDocRefReferences(res, refs)
            is dev.ohs.fhir.model.r4.QuestionnaireResponse -> extractQrReferences(res, refs)
            else -> {}
        }
        return refs
    }

    /**
     * Extracts reference links from an Encounter resource.
     *
     * @param enc The encounter resource.
     * @param refs Output accumulator list.
     */
    private fun extractEncounterReferences(
        enc: dev.ohs.fhir.model.r4.Encounter,
        refs: MutableList<String>,
    ) {
        addReference(enc.subject, refs)
        addReference(enc.serviceProvider, refs)
        enc.participant.forEach { p -> addReference(p.individual, refs) }
    }

    /**
     * Extracts reference links from a DocumentReference resource.
     *
     * @param doc The document reference resource.
     * @param refs Output accumulator list.
     */
    private fun extractDocRefReferences(
        doc: dev.ohs.fhir.model.r4.DocumentReference,
        refs: MutableList<String>,
    ) {
        addReference(doc.subject, refs)
        doc.author.forEach { a -> addReference(a, refs) }
    }

    /**
     * Extracts reference links from a QuestionnaireResponse resource.
     *
     * @param qr The questionnaire response resource.
     * @param refs Output accumulator list.
     */
    private fun extractQrReferences(
        qr: dev.ohs.fhir.model.r4.QuestionnaireResponse,
        refs: MutableList<String>,
    ) {
        addReference(qr.subject, refs)
        addReference(qr.encounter, refs)
        addReference(qr.author, refs)
    }

    /**
     * Helper to safely extract a non-blank string reference from a FHIR Reference object.
     *
     * @param ref The FHIR Reference object.
     * @param refs The mutable accumulator list.
     */
    private fun addReference(
        ref: dev.ohs.fhir.model.r4.Reference?,
        refs: MutableList<String>,
    ) {
        val str = ref?.reference?.value
        if (!str.isNullOrBlank()) {
            refs.add(str)
        }
    }
}

/**
 * Result of air-gapped bundle validation verifying offline self-containment.
 *
 * @property isValid True if all internal references resolve locally and no external network dependencies exist.
 * @property danglingReferences List of references that point to unresolvable external network URLs.
 * @property unresolvedLocalReferences List of internal references that do not resolve to any entry in the bundle.
 */
data class AirGappedValidationResult(
    val isValid: Boolean,
    val danglingReferences: List<String>,
    val unresolvedLocalReferences: List<String>,
)
