/**
 * @file FhirRepository.kt
 * Repository for storing and retrieving FHIR resources (Patients and Encounters).
 * This repository handles bidirectional conversion between FHIR objects and local database models.
 */
package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Media
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Resource
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.fhir.FhirJsonParser
import io.healthplatform.chartcam.models.mrn
import io.healthplatform.chartcam.utils.runSuspendCatching

/**
 * FHIR date and numeric search prefix operators.
 *
 * @property prefix The two-letter FHIR search prefix code.
 */
enum class SearchPrefix(
    val prefix: String,
) {
    /** Equal to (default). */
    EQ("eq"),

    /** Not equal to. */
    NE("ne"),

    /** Greater than. */
    GT("gt"),

    /** Less than. */
    LT("lt"),

    /** Greater than or equal to. */
    GE("ge"),

    /** Less than or equal to. */
    LE("le"),

    /** Starts after. */
    SA("sa"),

    /** Ends before. */
    EB("eb"),
    ;

    /**
     * Utilities for extracting prefix operators from query parameters.
     */
    companion object {
        /**
         * Extracts the prefix operator from a raw query value, defaulting to [EQ].
         *
         * @param raw The raw query string.
         * @return A [Pair] containing the resolved [SearchPrefix] and clean query value.
         */
        fun fromValue(raw: String): Pair<SearchPrefix, String> {
            for (p in entries) {
                if (raw.startsWith(p.prefix, ignoreCase = true) && raw.length > p.prefix.length) {
                    return p to raw.substring(p.prefix.length)
                }
            }
            return EQ to raw
        }
    }
}

/**
 * Criterion definition for multi-parameter compound FHIR search queries.
 *
 * @param R The concrete FHIR [Resource] type.
 * @property param The strongly-typed search parameter.
 * @property value The query value or prefixed value.
 */
data class SearchCriterion<R : Resource>(
    val param: dev.ohs.fhir.model.r4.search.SearchParam<R, *>,
    val value: String,
)

/**
 * Repository responsible for CRUD operations on FHIR resources persisted locally.
 * Uses a generic Resource table and Index tables mimicking the FHIR Engine SDK.
 *
 * @param database The database instance used by this repository for executing queries.
 */
@Suppress("LargeClass")
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

    @PublishedApi
    internal val dbQuery by lazy { database.chartCamQueries }

    /**
     * Safely decodes a serialized FHIR resource string to the expected resource type using typed deserialization.
     *
     * @param T The concrete FHIR [Resource] type.
     * @param serialized The serialized JSON string.
     * @return The deserialized resource instance, or null if deserialization failed.
     */
    public inline fun <reified T : Resource> decodeResource(serialized: String): T? =
        decodeResourceCatching<T>(serialized).getOrNull()

    /**
     * Safely decodes a serialized FHIR resource string to the expected resource type wrapped in a [Result].
     *
     * @param T The concrete FHIR [Resource] type.
     * @param serialized The serialized JSON string.
     * @return A [Result] enclosing the deserialized resource instance.
     */
    public inline fun <reified T : Resource> decodeResourceCatching(serialized: String): Result<T> =
        FhirJsonParser.decodeFromStringCatching<T>(serialized)

    /**
     * Generates and saves SearchParam indices for a given resource.
     * @param resource The FHIR Resource to index.
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @return A [Result] indicating success or failure.
     */
    @PublishedApi
    internal suspend fun indexResource(
        resource: Resource,
        resourceType: String,
        resourceId: String,
    ): Result<Unit> =
        FhirSearchIndexer.indexResource(dbQuery, resource, resourceType, resourceId)

    /**
     * Saves a strongly typed FHIR Resource using direct typed serialization.
     *
     * @param T The concrete FHIR [Resource] type.
     * @param resourceType The type of the resource.
     * @param resourceId The unique ID of the resource.
     * @param resource The resource instance.
     * @param isLocalChange Whether to record a local update entry.
     * @return A [Result] indicating success or failure.
     */
    suspend inline fun <reified T : Resource> saveResourceTyped(
        resourceType: String,
        resourceId: String,
        resource: T,
        isLocalChange: Boolean,
    ): Result<Unit> {
        val serialized =
            FhirJsonParser
                .encodeToStringCatching<T>(resource)
                .getOrElse { error -> return Result.failure(error) }
        val dbResult =
            runSuspendCatching {
                val now =
                    kotlin.time.Clock.System
                        .now()
                        .toString()
                dbQuery.insertResource(resourceId, resourceType, serialized, now)
                if (isLocalChange) {
                    val meta = resource.meta
                    val vId = if (meta != null) meta.versionId else null
                    val versionId = if (vId != null) vId.value else null
                    dbQuery.insertLocalChange(resourceType, resourceId, now, "UPDATE", serialized, versionId)
                }
            }
        return if (dbResult.isFailure) dbResult else indexResource(resource, resourceType, resourceId)
    }

    /**
     * Safely retrieves a strongly typed FHIR Resource by type and ID using direct typed deserialization.
     *
     * @param T The concrete FHIR [Resource] type.
     * @param resourceType The type discriminator of the resource.
     * @param resourceId The unique ID of the resource.
     * @return A [Result] enclosing the deserialized resource instance, or null if not found.
     */
    suspend inline fun <reified T : Resource> getResourceTyped(
        resourceType: String,
        resourceId: String,
    ): Result<T?> {
        val queryResult =
            runSuspendCatching {
                dbQuery.getResourceById(resourceType, resourceId).awaitAsOneOrNull()
            }
        return queryResult.fold(
            onSuccess = { entity ->
                if (entity == null) {
                    Result.success(null)
                } else {
                    decodeResourceCatching<T>(entity.serializedResource)
                }
            },
            onFailure = { Result.failure(it) },
        )
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
    ): Result<Unit> {
        val serialized =
            FhirJsonParser
                .encodeResource(resource)
                .getOrElse { error -> return Result.failure(error) }
        val dbResult =
            runSuspendCatching {
                val now =
                    kotlin.time.Clock.System
                        .now()
                        .toString()
                dbQuery.insertResource(resourceId, resourceType, serialized, now)
                if (isLocalChange) {
                    val meta = resource.meta
                    val vId = if (meta != null) meta.versionId else null
                    val versionId = if (vId != null) vId.value else null
                    dbQuery.insertLocalChange(resourceType, resourceId, now, "UPDATE", serialized, versionId)
                }
            }
        return if (dbResult.isFailure) dbResult else indexResource(resource, resourceType, resourceId)
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
    ): Result<Unit> {
        saveResource(resourceType, resourceId, resource, isLocalChange = false).getOrElse { error ->
            return Result.failure(error)
        }
        return runSuspendCatching {
            // Ensure any pending local changes for this resource are cleared to prevent overwriting server state
            dbQuery.deleteLocalChangesForResource(resourceType, resourceId)
        }
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
        val entity =
            runSuspendCatching {
                dbQuery.getResourceById(resourceType, resourceId).awaitAsOneOrNull()
            }.getOrNull() ?: return null
        return FhirJsonParser.decodeAnyResource(entity.serializedResource).getOrNull()
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
    open suspend fun savePractitioner(practitioner: Practitioner): Result<Unit> =
        savePractitioner(practitioner, isLocalChange = true)

    /**
     * Saves a Practitioner.
     * @param practitioner The Practitioner resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun savePractitioner(
        practitioner: Practitioner,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResourceTyped("Practitioner", practitioner.id ?: "", practitioner, isLocalChange)

    /**
     * Safely retrieves a Practitioner by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Practitioner to retrieve.
     * @return A [Result] enclosing the Practitioner resource if found, or null otherwise.
     */
    open suspend fun getPractitionerCatching(id: String): Result<Practitioner?> =
        getResourceTyped<Practitioner>("Practitioner", id)

    /**
     * Retrieves a Practitioner.
     * @param id The unique identifier of the Practitioner to retrieve.
     * @return The Practitioner resource if found, or null otherwise.
     */
    open suspend fun getPractitioner(id: String): Practitioner? = getPractitionerCatching(id).getOrNull()

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
    ): Result<Unit> = saveResourceTyped("Patient", patient.id ?: "", patient, isLocalChange)

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
        return entity?.let { decodeResource<Patient>(it.serializedResource) }
    }

    /**
     * Safely retrieves a Patient by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Patient to retrieve.
     * @return A [Result] enclosing the Patient resource if found, or null otherwise.
     */
    open suspend fun getPatientCatching(id: String): Result<Patient?> =
        getResourceTyped<Patient>("Patient", id)

    /**
     * Retrieves a Patient.
     * @param id The unique identifier of the Patient to retrieve.
     * @return The Patient resource if found, or null otherwise.
     */
    open suspend fun getPatient(id: String): Patient? = getPatientCatching(id).getOrNull()

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
            dbQuery.getAllResourcesByType("Patient").awaitAsList().mapNotNull {
                decodeResource<Patient>(it.serializedResource)
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
                        val enc = decodeResource<Encounter>(it.serializedResource)
                        val subj = if (enc != null) enc.subject else null
                        val pid = extractPatientReferenceId(subj)
                        if (pid.isNotEmpty()) pid else null
                    }.distinct()
            val all =
                dbQuery.getAllResourcesByType("Patient").awaitAsList().mapNotNull {
                    decodeResource<Patient>(it.serializedResource)
                }
            all.filter { p ->
                val rawId = p.id
                val pid = if (rawId != null) rawId.removePrefix("Patient/") else ""
                patientIds.contains(pid) ||
                    organizationMatchesPractitioner(p.managingOrganization, practitionerId)
            }
        }

    /**
     * Extracts a bare Patient ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Patient ID or an empty string.
     */
    @PublishedApi
    internal fun extractPatientReferenceId(ref: dev.ohs.fhir.model.r4.Reference?): String {
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
     * Extracts a bare Encounter ID from an optional reference element.
     *
     * @param ref The reference element.
     * @return The bare Encounter ID or an empty string.
     */
    @PublishedApi
    internal fun extractEncounterReferenceId(ref: dev.ohs.fhir.model.r4.Reference?): String {
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
     * Checks if managing organization reference contains the specified practitioner ID.
     *
     * @param orgRef The organization reference element.
     * @param practitionerId The practitioner identifier to match.
     * @return True if the reference matches the practitioner ID, false otherwise.
     */
    @PublishedApi
    internal fun organizationMatchesPractitioner(
        orgRef: dev.ohs.fhir.model.r4.Reference?,
        practitionerId: String,
    ): Boolean {
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
        var patients = allEntities.mapNotNull { decodeResource<Patient>(it.serializedResource) }
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
                        val enc = decodeResource<Encounter>(it.serializedResource)
                        val subj = if (enc != null) enc.subject else null
                        val pid = extractPatientReferenceId(subj)
                        if (pid.isNotEmpty()) pid else null
                    }.distinct()
            patients =
                patients.filter { p ->
                    val rawId = p.id
                    val pid = if (rawId != null) rawId.removePrefix("Patient/") else ""
                    patientIds.contains(pid) ||
                        organizationMatchesPractitioner(p.managingOrganization, practitionerId)
                }
        }
        return patients
    }

    /**
     * Searches any FHIR Resource by a strongly-typed [dev.ohs.fhir.model.r4.search.SearchParam].
     *
     * @param R The concrete FHIR [Resource] type.
     * @param param The search parameter definition.
     * @param value The query value to match.
     * @return A [Result] enclosing matching resources.
     */
    suspend inline fun <reified R : Resource> searchByParam(
        param: dev.ohs.fhir.model.r4.search.SearchParam<R, *>,
        value: String,
    ): Result<List<R>> {
        val resourceType =
            io.healthplatform.chartcam.fhir
                .resolveCanonicalResourceType<R>()
                .getOrNull()
                .orEmpty()
        val (prefix, cleanVal) = SearchPrefix.fromValue(value)
        val isDate =
            param.type == dev.ohs.fhir.model.r4.terminologies.SearchParamType.Date
        val isQuantity =
            param.type == dev.ohs.fhir.model.r4.terminologies.SearchParamType.Quantity
        return if (isDate) {
            searchByDatePrefixCatching<R>(param.name, prefix, cleanVal)
        } else if (isQuantity) {
            searchByQuantityCatching<R>(param.name, prefix, cleanVal)
        } else {
            runSuspendCatching {
                val entities =
                    if (param.type == dev.ohs.fhir.model.r4.terminologies.SearchParamType.Reference) {
                        dbQuery.searchResourcesByReference(resourceType, param.name, value).awaitAsList()
                    } else if (param.type == dev.ohs.fhir.model.r4.terminologies.SearchParamType.String) {
                        dbQuery.searchResourcesByString(resourceType, param.name, value).awaitAsList()
                    } else {
                        dbQuery.searchResourcesByToken(resourceType, param.name, null, value).awaitAsList()
                    }
                entities.distinctBy { it.resourceId }.mapNotNull { decodeResource<R>(it.serializedResource) }
            }
        }
    }

    /**
     * Searches any FHIR Resource by a quantity search parameter supporting range prefix modifiers and units.
     *
     * @param R The concrete FHIR [Resource] type.
     * @param paramName The name of the quantity search parameter.
     * @param prefix The range comparison prefix (ge, le, gt, lt, ne, eq).
     * @param queryValue The quantity string (e.g. "140", "140|mm[Hg]", "140|http://unitsofmeasure.org|mm[Hg]").
     * @return A [Result] enclosing matching resources.
     */
    suspend inline fun <reified R : Resource> searchByQuantityCatching(
        paramName: String,
        prefix: SearchPrefix,
        queryValue: String,
    ): Result<List<R>> {
        val resourceType =
            io.healthplatform.chartcam.fhir
                .resolveCanonicalResourceType<R>()
                .getOrElse { return Result.failure(it) }
        return runSuspendCatching {
            val parts = queryValue.split('|')
            val numStr = parts[0].trim()
            val targetNum = numStr.toDoubleOrNull()
            val system = if (parts.size > 1 && parts[1].isNotBlank()) parts[1].trim() else null

            val allCandidates =
                if (prefix == SearchPrefix.EQ) {
                    if (system != null) {
                        dbQuery.searchResourcesByToken(resourceType, paramName, system, numStr).awaitAsList()
                    } else {
                        dbQuery.searchResourcesByToken(resourceType, paramName, null, numStr).awaitAsList()
                    }
                } else {
                    dbQuery.getAllResourcesByType(resourceType).awaitAsList()
                }

            val decoded =
                allCandidates
                    .distinctBy { it.resourceId }
                    .mapNotNull { decodeResource<R>(it.serializedResource) }

            if (targetNum == null || prefix == SearchPrefix.EQ) {
                decoded
            } else {
                decoded.filter { res ->
                    val pair = extractObservationQuantity(res)
                    if (pair != null && (system == null || system == pair.second)) {
                        matchesQuantityPrefix(prefix, pair.first, targetNum)
                    } else {
                        false
                    }
                }
            }
        }
    }

    /**
     * Extracts quantity value and system from a decoded FHIR Resource.
     *
     * @param res The decoded resource.
     * @return Pair of numeric quantity value and optional system URI, or null.
     */
    @PublishedApi
    internal fun extractObservationQuantity(res: Resource): Pair<Double, String?>? {
        val pair =
            if (res is Observation) {
                val v = res.value
                val qh = if (v != null) v.asQuantity() else null
                val q = if (qh != null) qh.value else null
                val d = if (q != null) q.value else null
                val rn = if (d != null) d.value else null
                val n = if (rn != null) rn.toString().toDoubleOrNull() else null
                val sysObj = if (q != null) q.system else null
                val s = if (sysObj != null) sysObj.value else null
                if (n != null) {
                    n to s
                } else {
                    null
                }
            } else {
                null
            }
        return pair
    }

    /**
     * Evaluates a numeric value against a comparison prefix and target number.
     *
     * @param prefix The comparison prefix.
     * @param value The candidate value.
     * @param target The target number.
     * @return True if the comparison holds.
     */
    fun matchesQuantityPrefix(
        prefix: SearchPrefix,
        value: Double,
        target: Double,
    ): Boolean =
        when (prefix) {
            SearchPrefix.GE -> value >= target
            SearchPrefix.LE -> value <= target
            SearchPrefix.GT -> value > target
            SearchPrefix.LT -> value < target
            SearchPrefix.NE -> value != target
            SearchPrefix.EQ -> value == target
            else -> true
        }

    /**
     * Executes a chained search parameter query locally in SQLite.
     *
     * Example: searching Encounters where 'subject.name' matches 'Smith'.
     *
     * @param R The primary resource type to search.
     * @param referenceParam The reference parameter on resource [R] (e.g. "subject" or "patient").
     * @param targetResourceType The target resource type being referenced (e.g. "Patient").
     * @param targetParamName The search parameter name on the target resource (e.g. "name", "family").
     * @param targetParamValue The query value to match on the target resource.
     * @return A [Result] enclosing matching primary resources.
     */
    suspend inline fun <reified R : Resource> searchByChainedParam(
        referenceParam: String,
        targetResourceType: String,
        targetParamName: String,
        targetParamValue: String,
    ): Result<List<R>> {
        val primaryType =
            io.healthplatform.chartcam.fhir
                .resolveCanonicalResourceType<R>()
                .getOrNull()
                .orEmpty()

        return runSuspendCatching {
            val targetEntities =
                dbQuery
                    .searchResourcesByString(targetResourceType, targetParamName, targetParamValue)
                    .awaitAsList()
            if (targetEntities.isEmpty()) return@runSuspendCatching emptyList()

            val results = mutableListOf<R>()
            val seenIds = mutableSetOf<String>()
            for (target in targetEntities) {
                val bareId = target.resourceId
                val fullRef = "$targetResourceType/$bareId"
                val primaryMatches =
                    dbQuery.searchResourcesByReference(primaryType, referenceParam, fullRef).awaitAsList() +
                        dbQuery.searchResourcesByReference(primaryType, referenceParam, bareId).awaitAsList()

                for (pm in primaryMatches) {
                    if (seenIds.add(pm.resourceId)) {
                        val decoded = decodeResource<R>(pm.serializedResource)
                        if (decoded != null) {
                            results.add(decoded)
                        }
                    }
                }
            }
            results
        }
    }

    /**
     * Executes a reverse-chaining query (_has parameter) locally in SQLite.
     *
     * Example: find Patients having an Observation with a specific code.
     *
     * @param R The primary resource type to retrieve (e.g. Patient).
     * @param targetResourceType The primary resource type to retrieve (e.g. "Patient").
     * @param sourceResourceType The referring resource type (e.g. "Observation").
     * @param sourceRefParam The reference parameter on the referring resource pointing to target.
     * @param sourceFilterParam The search parameter on the referring resource to filter by.
     * @param sourceFilterValue The query value to match.
     * @return A [Result] enclosing matching primary resources.
     */
    suspend inline fun <reified R : Resource> searchByReverseChain(
        targetResourceType: String,
        sourceResourceType: String,
        sourceRefParam: String,
        sourceFilterParam: String,
        sourceFilterValue: String,
    ): Result<List<R>> =
        runSuspendCatching {
            val tokenMatches =
                dbQuery
                    .searchResourcesByToken(sourceResourceType, sourceFilterParam, null, sourceFilterValue)
                    .awaitAsList()
            val stringMatches =
                dbQuery
                    .searchResourcesByString(sourceResourceType, sourceFilterParam, sourceFilterValue)
                    .awaitAsList()
            val referringEntities = tokenMatches + stringMatches
            if (referringEntities.isEmpty()) return@runSuspendCatching emptyList()

            val targetIds = mutableSetOf<String>()
            for (refEntity in referringEntities.distinctBy { it.resourceId }) {
                val refIndices =
                    dbQuery
                        .searchResourcesByReference(sourceResourceType, sourceRefParam, refEntity.resourceId)
                        .awaitAsList()
                for (ri in refIndices) {
                    targetIds.add(ri.resourceId)
                }

                val decoded = decodeResource<Resource>(refEntity.serializedResource)
                when (decoded) {
                    is Observation -> {
                        val v = extractPatientReferenceId(decoded.subject)
                        if (v.isNotBlank()) {
                            targetIds.add(v.removePrefix("$targetResourceType/"))
                        }
                    }
                    is Encounter -> {
                        val v = extractPatientReferenceId(decoded.subject)
                        if (v.isNotBlank()) {
                            targetIds.add(v.removePrefix("$targetResourceType/"))
                        }
                    }
                    else -> {}
                }
            }

            val results = mutableListOf<R>()
            for (tId in targetIds) {
                val entity = dbQuery.getResourceById(targetResourceType, tId).awaitAsOneOrNull()
                if (entity != null) {
                    val decoded = decodeResource<R>(entity.serializedResource)
                    if (decoded != null) {
                        results.add(decoded)
                    }
                }
            }
            results
        }

    /**
     * Searches any FHIR Resource by a date search parameter using range prefix modifiers.
     *
     * @param R The concrete FHIR [Resource] type.
     * @param paramName The name of the date search parameter.
     * @param prefix The range comparison prefix (ge, le, gt, lt, ne, eq).
     * @param dateValue The target ISO date string.
     * @return A [Result] enclosing matching resources.
     */
    suspend inline fun <reified R : Resource> searchByDatePrefixCatching(
        paramName: String,
        prefix: SearchPrefix,
        dateValue: String,
    ): Result<List<R>> {
        val resourceType =
            io.healthplatform.chartcam.fhir
                .resolveCanonicalResourceType<R>()
                .getOrElse { return Result.failure(it) }
        return runSuspendCatching {
            val entities =
                when (prefix) {
                    SearchPrefix.GE, SearchPrefix.SA ->
                        dbQuery.searchResourcesByDatePrefixGe(resourceType, paramName, dateValue).awaitAsList()
                    SearchPrefix.LE, SearchPrefix.EB ->
                        dbQuery.searchResourcesByDatePrefixLe(resourceType, paramName, dateValue).awaitAsList()
                    SearchPrefix.GT ->
                        dbQuery.searchResourcesByDatePrefixGt(resourceType, paramName, dateValue).awaitAsList()
                    SearchPrefix.LT ->
                        dbQuery.searchResourcesByDatePrefixLt(resourceType, paramName, dateValue).awaitAsList()
                    SearchPrefix.NE ->
                        dbQuery.searchResourcesByDatePrefixNe(resourceType, paramName, dateValue).awaitAsList()
                    SearchPrefix.EQ ->
                        dbQuery.searchResourcesByDate(resourceType, paramName, dateValue).awaitAsList()
                }
            entities.distinctBy { it.resourceId }.mapNotNull { decodeResource<R>(it.serializedResource) }
        }
    }

    /**
     * Executes a multi-parameter compound search with local set intersection (logical AND).
     *
     * @param R The concrete FHIR [Resource] type.
     * @param criteria The list of criteria to filter resources against.
     * @return A [Result] enclosing resources matching all criteria.
     */
    suspend inline fun <reified R : Resource> searchCompoundCatching(
        criteria: List<SearchCriterion<R>>,
    ): Result<List<R>> {
        var matchingIds = emptySet<String>()
        var primaryMatches: List<R> = emptyList()

        for ((index, criterion) in criteria.withIndex()) {
            val matches = searchByParam(criterion.param, criterion.value).getOrNull().orEmpty()
            val currentIds = matches.mapNotNull { it.id }.toSet()
            if (index == 0) {
                matchingIds = currentIds
                primaryMatches = matches
            } else {
                matchingIds = matchingIds.intersect(currentIds)
            }
        }

        return Result.success(primaryMatches.filter { it.id in matchingIds })
    }

    /**
     * Performs a local reverse-include resolving Encounters along with their associated Observations.
     *
     * @param patientId The target patient identifier.
     * @return A [Result] mapping each Encounter to its associated Observations.
     */
    open suspend fun searchEncountersWithObservationsCatching(
        patientId: String,
    ): Result<Map<Encounter, List<Observation>>> {
        val cleanPatientId = patientId.removePrefix("Patient/")
        val encounters =
            searchEncountersByParam(
                dev.ohs.fhir.model.r4.search.EncounterSearchParams.subject,
                cleanPatientId,
            ).getOrNull().orEmpty()

        val observations =
            searchByParam<Observation>(
                dev.ohs.fhir.model.r4.search.ObservationSearchParams.subject,
                cleanPatientId,
            ).getOrNull().orEmpty()

        val encMap = mutableMapOf<Encounter, MutableList<Observation>>()
        for (enc in encounters) {
            encMap[enc] = mutableListOf()
        }

        for (obs in observations) {
            val encRef = extractEncounterReferenceId(obs.encounter)
            val targetEnc = encounters.firstOrNull { it.id == encRef }
            if (targetEnc != null) {
                encMap.getValue(targetEnc).add(obs)
            }
        }
        return Result.success(encMap)
    }

    /**
     * Searches any FHIR Resource by a token search parameter with optional system matching.
     *
     * @param R The concrete FHIR [Resource] type.
     * @param paramName The name of the token search parameter (e.g., "code", "identifier").
     * @param system Optional terminology system URI to match.
     * @param code The token code value to match.
     * @return A [Result] enclosing matching resources.
     */
    suspend inline fun <reified R : Resource> searchByToken(
        paramName: String,
        system: String?,
        code: String,
    ): Result<List<R>> {
        val resourceType =
            io.healthplatform.chartcam.fhir
                .resolveCanonicalResourceType<R>()
                .getOrElse { return Result.failure(it) }
        return runSuspendCatching {
            val entities = dbQuery.searchResourcesByToken(resourceType, paramName, system, code).awaitAsList()
            entities.distinctBy { it.resourceId }.mapNotNull { decodeResource<R>(it.serializedResource) }
        }
    }

    /**
     * Searches any FHIR Resource by standard security or category tag.
     *
     * @param R The concrete FHIR [Resource] type.
     * @param code The tag code.
     * @param system Optional system URI.
     * @return A [Result] enclosing matching resources.
     */
    suspend inline fun <reified R : Resource> searchByTag(
        code: String,
        system: String? = null,
    ): Result<List<R>> = searchByToken<R>("_tag", system, code)

    /**
     * Searches any FHIR Resource conforming to a specific profile URI.
     *
     * @param R The concrete FHIR [Resource] type.
     * @param profileUri The profile canonical URI.
     * @return A [Result] enclosing matching resources.
     */
    suspend inline fun <reified R : Resource> searchByProfile(
        profileUri: String,
    ): Result<List<R>> {
        val resourceType =
            io.healthplatform.chartcam.fhir
                .resolveCanonicalResourceType<R>()
                .getOrElse { return Result.failure(it) }
        return runSuspendCatching {
            val entities = dbQuery.searchResourcesByReference(resourceType, "_profile", profileUri).awaitAsList()
            entities.distinctBy { it.resourceId }.mapNotNull { decodeResource<R>(it.serializedResource) }
        }
    }

    /**
     * Searches Patients by a strongly-typed [dev.ohs.fhir.model.r4.search.SearchParam].
     *
     * @param param The search parameter definition.
     * @param value The query value.
     * @return A [Result] enclosing matching [Patient] resources.
     */
    open suspend fun searchPatientsByParam(
        param: dev.ohs.fhir.model.r4.search.SearchParam<Patient, *>,
        value: String,
    ): Result<List<Patient>> = searchByParam(param, value)

    /**
     * Searches Encounters by a strongly-typed [dev.ohs.fhir.model.r4.search.SearchParam].
     *
     * @param param The search parameter definition.
     * @param value The query value.
     * @return A [Result] enclosing matching [Encounter] resources.
     */
    open suspend fun searchEncountersByParam(
        param: dev.ohs.fhir.model.r4.search.SearchParam<Encounter, *>,
        value: String,
    ): Result<List<Encounter>> = searchByParam(param, value)

    /**
     * Searches DocumentReferences by a strongly-typed [dev.ohs.fhir.model.r4.search.SearchParam].
     *
     * @param param The search parameter definition.
     * @param value The query value.
     * @return A [Result] enclosing matching [DocumentReference] resources.
     */
    open suspend fun searchDocumentReferencesByParam(
        param: dev.ohs.fhir.model.r4.search.SearchParam<DocumentReference, *>,
        value: String,
    ): Result<List<DocumentReference>> = searchByParam(param, value)

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
     * Searches Patients by query string safely returning a [Result].
     *
     * @param query The search query string.
     * @param showAll If true, searches across all patients.
     * @param practitionerId The Practitioner to filter by, if showAll is false.
     * @return A [Result] enclosing the list of matching Patient resources.
     */
    open suspend fun searchPatientsCatching(
        query: String,
        showAll: Boolean = true,
        practitionerId: String? = null,
    ): Result<List<Patient>> = runSuspendCatching { searchPatients(query, showAll, practitionerId) }

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
    ): Result<Unit> {
        val cleanId = id.removePrefix("Patient/")
        for (enc in getEncountersForPatient(cleanId)) {
            val encId = enc.id
            if (encId != null) {
                deleteEncounter(encId, fileStorage)
            }
        }
        for (qr in getAllQuestionnaireResponses().filter { extractPatientReferenceId(it.subject) == cleanId }) {
            val qrId = qr.id
            if (qrId != null) {
                deleteResource("QuestionnaireResponse", qrId)
            }
        }
        return deleteResource("Patient", cleanId)
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
    ): Result<Unit> = saveResourceTyped("Encounter", encounter.id ?: "", encounter, isLocalChange)

    /**
     * Safely retrieves an Encounter by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Encounter to retrieve.
     * @return A [Result] enclosing the Encounter resource if found, or null otherwise.
     */
    open suspend fun getEncounterCatching(id: String): Result<Encounter?> =
        getResourceTyped<Encounter>("Encounter", id)

    /**
     * Retrieves an Encounter.
     * @param id The unique identifier of the Encounter to retrieve.
     * @return The Encounter resource if found, or null otherwise.
     */
    open suspend fun getEncounter(id: String): Encounter? = getEncounterCatching(id).getOrNull()

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
        return (withPrefix + withoutPrefix).distinctBy { it.resourceId }.mapNotNull {
            decodeResource<Encounter>(it.serializedResource)
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
                                dev.ohs.fhir.model.r4.Narrative.Builder(
                                    status =
                                        Enumeration(
                                            value = dev.ohs.fhir.model.r4.Narrative.NarrativeStatus.Generated,
                                        ),
                                    div =
                                        dev.ohs.fhir.model.r4.Xhtml
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
    ): Result<Unit> {
        val cleanId = id.removePrefix("Encounter/")
        val photos = getPhotosForEncounter(cleanId)
        photos.forEach { doc ->
            val docId = doc.id
            if (docId != null) {
                deleteResource("DocumentReference", docId)
            }
            val contentList = doc.content
            val path =
                if (contentList.isNotEmpty()) {
                    val att = contentList[0].attachment
                    val urlObj = att.url
                    if (urlObj != null) urlObj.value else null
                } else {
                    null
                }
            if (path != null && fileStorage != null) {
                fileStorage.deleteImage(path)
            }
        }
        val responses = getQuestionnaireResponsesForEncounter(cleanId)
        responses.forEach { qr ->
            val qrId = qr.id
            if (qrId != null) {
                deleteResource("QuestionnaireResponse", qrId)
            }
        }
        return deleteResource("Encounter", cleanId)
    }

    /**
     * Saves a DocumentReference (photo) with default local change tracking.
     * @param doc The DocumentReference resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveDocumentReference(doc: DocumentReference): Result<Unit> =
        saveDocumentReference(doc, isLocalChange = true)

    /**
     * Saves a DocumentReference (photo).
     * @param doc The DocumentReference resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveDocumentReference(
        doc: DocumentReference,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResourceTyped("DocumentReference", doc.id ?: "", doc, isLocalChange)

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
        return (withPrefix + withoutPrefix).distinctBy { it.resourceId }.mapNotNull {
            decodeResource<DocumentReference>(it.serializedResource)
        }
    }

    /**
     * Safely retrieves a single DocumentReference (photo) by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the DocumentReference.
     * @return A [Result] enclosing the DocumentReference resource, or null if not found.
     */
    open suspend fun getDocumentReferenceCatching(id: String): Result<DocumentReference?> =
        getResourceTyped<DocumentReference>("DocumentReference", id)

    /**
     * Retrieves a single DocumentReference (photo) by ID.
     * @param id The unique identifier of the DocumentReference.
     * @return The DocumentReference resource, or null if not found.
     */
    open suspend fun getDocumentReference(id: String): DocumentReference? =
        getDocumentReferenceCatching(id).getOrNull()

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
    ): Result<Unit> = saveResourceTyped("QuestionnaireResponse", qr.id ?: "", qr, isLocalChange)

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

        return all.mapNotNull {
            decodeResource<QuestionnaireResponse>(it.serializedResource)
        }
    }

    /**
     * Safely retrieves a QuestionnaireResponse by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the QuestionnaireResponse.
     * @return A [Result] enclosing the QuestionnaireResponse resource, or null if not found.
     */
    open suspend fun getQuestionnaireResponseCatching(id: String): Result<QuestionnaireResponse?> =
        getResourceTyped<QuestionnaireResponse>("QuestionnaireResponse", id)

    /**
     * Retrieves a QuestionnaireResponse by ID.
     *
     * @param id The unique identifier of the QuestionnaireResponse.
     * @return The QuestionnaireResponse resource, or null if not found.
     */
    open suspend fun getQuestionnaireResponse(id: String): QuestionnaireResponse? =
        getQuestionnaireResponseCatching(id).getOrNull()

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
    ): Result<Unit> = saveResourceTyped("Device", device.id ?: "", device, isLocalChange)

    /**
     * Safely retrieves a Device by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Device to retrieve.
     * @return A [Result] enclosing the Device resource if found, or null otherwise.
     */
    open suspend fun getDeviceCatching(id: String): Result<Device?> =
        getResourceTyped<Device>("Device", id)

    /**
     * Retrieves a Device.
     * @param id The unique identifier of the Device to retrieve.
     * @return The Device resource if found, or null otherwise.
     */
    open suspend fun getDevice(id: String): Device? = getDeviceCatching(id).getOrNull()

    /**
     * Saves an [Observation] resource.
     *
     * @param observation The Observation resource to persist.
     * @param isLocalChange Whether to record a local change tracking entry.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveObservation(
        observation: Observation,
        isLocalChange: Boolean = true,
    ): Result<Unit> = saveResourceTyped("Observation", observation.id ?: "", observation, isLocalChange)

    /**
     * Safely retrieves an [Observation] by its ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Observation.
     * @return A [Result] enclosing the [Observation] if found, or null otherwise.
     */
    open suspend fun getObservationCatching(id: String): Result<Observation?> =
        getResourceTyped<Observation>("Observation", id)

    /**
     * Retrieves an [Observation] by its ID.
     *
     * @param id The unique identifier of the Observation.
     * @return The [Observation] if found, or null otherwise.
     */
    open suspend fun getObservation(id: String): Observation? =
        getObservationCatching(id).getOrNull()

    /**
     * Retrieves all [Observation] resources linked to a Patient.
     *
     * @param patientId The unique identifier of the Patient.
     * @return List of matching [Observation] resources.
     */
    open suspend fun getObservationsForPatient(patientId: String): List<Observation> {
        val cleanId = patientId.removePrefix("Patient/")
        val withPrefix =
            dbQuery.searchResourcesByReference("Observation", "patient", "Patient/$cleanId").awaitAsList()
        val withoutPrefix =
            dbQuery.searchResourcesByReference("Observation", "patient", cleanId).awaitAsList()
        return (withPrefix + withoutPrefix).distinctBy { it.resourceId }.mapNotNull {
            decodeResource<Observation>(it.serializedResource)
        }
    }

    /**
     * Retrieves all [Observation] resources linked to an Encounter.
     *
     * @param encounterId The unique identifier of the Encounter.
     * @return List of matching [Observation] resources.
     */
    open suspend fun getObservationsForEncounter(encounterId: String): List<Observation> {
        val cleanId = encounterId.removePrefix("Encounter/")
        val withPrefix =
            dbQuery.searchResourcesByReference("Observation", "encounter", "Encounter/$cleanId").awaitAsList()
        val withoutPrefix =
            dbQuery.searchResourcesByReference("Observation", "encounter", cleanId).awaitAsList()
        return (withPrefix + withoutPrefix).distinctBy { it.resourceId }.mapNotNull {
            decodeResource<Observation>(it.serializedResource)
        }
    }

    /**
     * Saves a Provenance with default local change tracking.
     * @param provenance The Provenance resource to persist.
     * @param encounterId Optional unique identifier of the Encounter.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveProvenance(
        provenance: Provenance,
        encounterId: String? = null,
    ): Result<Unit> = saveProvenance(provenance, encounterId, isLocalChange = true)

    /**
     * Saves a Provenance.
     * @param provenance The Provenance resource to persist.
     * @param encounterId Optional unique identifier of the Encounter.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveProvenance(
        provenance: Provenance,
        encounterId: String? = null,
        isLocalChange: Boolean,
    ): Result<Unit> {
        val saveResult = saveResourceTyped("Provenance", provenance.id ?: "", provenance, isLocalChange)
        if (saveResult.isFailure) return saveResult
        return runSuspendCatching {
            if (encounterId != null && provenance.id != null) {
                dbQuery.insertReferenceIndex("Provenance", provenance.id!!, "encounter", encounterId)
            }
        }
    }

    /**
     * Safely retrieves a Provenance by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Provenance to retrieve.
     * @return A [Result] enclosing the Provenance resource if found, or null otherwise.
     */
    open suspend fun getProvenanceCatching(id: String): Result<Provenance?> =
        getResourceTyped<Provenance>("Provenance", id)

    /**
     * Retrieves Provenances for an Encounter.
     * @param encounterId The unique identifier of the Encounter.
     * @return A list of Provenance resources.
     */
    open suspend fun getProvenancesForEncounter(encounterId: String): List<Provenance> =
        dbQuery.searchResourcesByReferenceDesc("Provenance", "encounter", encounterId).awaitAsList().mapNotNull {
            decodeResource<Provenance>(it.serializedResource)
        }

    /**
     * Retrieves all Practitioner resources.
     * @return A list of Practitioner resources.
     */
    open suspend fun getAllPractitioners() =
        dbQuery.getAllResourcesByType("Practitioner").awaitAsList().mapNotNull {
            decodeResource<Practitioner>(it.serializedResource)
        }

    /**
     * Retrieves all Encounter resources.
     * @return A list of Encounter resources.
     */
    open suspend fun getAllEncounters() =
        dbQuery.getAllResourcesByType("Encounter").awaitAsList().mapNotNull {
            decodeResource<Encounter>(it.serializedResource)
        }

    /**
     * Retrieves all DocumentReference resources.
     * @return A list of DocumentReference resources.
     */
    open suspend fun getAllDocumentReferences() =
        dbQuery.getAllResourcesByType("DocumentReference").awaitAsList().mapNotNull {
            decodeResource<DocumentReference>(it.serializedResource)
        }

    /**
     * Saves a Questionnaire resource with default local change tracking.
     *
     * @param questionnaire The Questionnaire resource to persist.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveQuestionnaire(questionnaire: Questionnaire): Result<Unit> =
        saveQuestionnaire(questionnaire, isLocalChange = true)

    /**
     * Saves a Questionnaire resource.
     *
     * @param questionnaire The Questionnaire resource to persist.
     * @param isLocalChange Whether this is a local change to record in sync tracking.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveQuestionnaire(
        questionnaire: Questionnaire,
        isLocalChange: Boolean,
    ): Result<Unit> = saveResourceTyped("Questionnaire", questionnaire.id ?: "", questionnaire, isLocalChange)

    /**
     * Safely retrieves a Questionnaire resource by ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Questionnaire.
     * @return A [Result] enclosing the Questionnaire resource, or null if not found.
     */
    open suspend fun getQuestionnaireCatching(id: String): Result<Questionnaire?> =
        getResourceTyped<Questionnaire>("Questionnaire", id)

    /**
     * Retrieves a Questionnaire resource by ID.
     *
     * @param id The unique identifier of the Questionnaire.
     * @return The Questionnaire resource, or null if not found.
     */
    open suspend fun getQuestionnaire(id: String): Questionnaire? =
        getQuestionnaireCatching(id).getOrNull()

    /**
     * Retrieves all Questionnaire resources.
     *
     * @return A list of Questionnaire resources.
     */
    open suspend fun getAllQuestionnaires(): List<Questionnaire> =
        dbQuery.getAllResourcesByType("Questionnaire").awaitAsList().mapNotNull {
            decodeResource<Questionnaire>(it.serializedResource)
        }

    /**
     * Retrieves all QuestionnaireResponse resources.
     * @return A list of QuestionnaireResponse resources.
     */
    open suspend fun getAllQuestionnaireResponses() =
        dbQuery.getAllResourcesByType("QuestionnaireResponse").awaitAsList().mapNotNull {
            decodeResource<QuestionnaireResponse>(it.serializedResource)
        }

    /**
     * Retrieves all Provenance resources.
     * @return A list of Provenance resources.
     */
    open suspend fun getAllProvenances() =
        dbQuery.getAllResourcesByType("Provenance").awaitAsList().mapNotNull {
            decodeResource<Provenance>(it.serializedResource)
        }

    /**
     * Retrieves all Device resources.
     * @return A list of Device resources.
     */
    open suspend fun getAllDevices() =
        dbQuery.getAllResourcesByType("Device").awaitAsList().mapNotNull {
            decodeResource<Device>(it.serializedResource)
        }

    /**
     * Saves a [Media] resource.
     *
     * @param media The Media resource to persist.
     * @param isLocalChange Whether to record a local change tracking entry.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun saveMedia(
        media: Media,
        isLocalChange: Boolean = true,
    ): Result<Unit> = saveResourceTyped("Media", media.id ?: "", media, isLocalChange)

    /**
     * Safely retrieves a [Media] resource by its ID using typed deserialization wrapped in a [Result].
     *
     * @param id The unique identifier of the Media resource.
     * @return A [Result] enclosing the [Media] if found, or null otherwise.
     */
    open suspend fun getMediaCatching(id: String): Result<Media?> =
        getResourceTyped<Media>("Media", id)

    /**
     * Retrieves a [Media] resource by its ID.
     *
     * @param id The unique identifier of the Media resource.
     * @return The [Media] resource if found, or null otherwise.
     */
    open suspend fun getMedia(id: String): Media? = getMediaCatching(id).getOrNull()

    /**
     * Deletes a [Media] resource by its ID.
     *
     * @param id The unique identifier of the Media resource to delete.
     * @param isLocalChange Whether to track this deletion as a local mutation.
     * @return A [Result] indicating success or failure.
     */
    open suspend fun deleteMedia(
        id: String,
        isLocalChange: Boolean = true,
    ): Result<Unit> = deleteResource("Media", id, isLocalChange)

    /**
     * Retrieves all [Media] resources associated with a specific Patient reference ID.
     *
     * @param patientId The patient ID (with or without 'Patient/' prefix).
     * @return A [Result] enclosing matching [Media] resources.
     */
    open suspend fun getMediaForPatient(patientId: String): Result<List<Media>> =
        searchByParam<Media>(
            dev.ohs.fhir.model.r4.search.MediaSearchParams.subject,
            if (patientId.startsWith("Patient/")) patientId else "Patient/$patientId",
        )

    /**
     * Retrieves all Media resources.
     * @return A list of Media resources.
     */
    open suspend fun getAllMedia() =
        dbQuery.getAllResourcesByType("Media").awaitAsList().mapNotNull {
            decodeResource<Media>(it.serializedResource)
        }
}
