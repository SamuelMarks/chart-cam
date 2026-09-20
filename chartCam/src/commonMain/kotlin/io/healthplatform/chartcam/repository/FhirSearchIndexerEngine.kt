/**
 * @file FhirSearchIndexerEngine.kt
 * Generic multiplatform SearchParam extractor and indexing engine for SQLite persistence.
 */
package io.healthplatform.chartcam.repository

import dev.ohs.fhir.model.r4.Address
import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.ExtensibleEnumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Period
import dev.ohs.fhir.model.r4.Quantity
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.search.SearchParam
import dev.ohs.fhir.model.r4.search.extract
import dev.ohs.fhir.model.r4.terminologies.SearchParamType
import io.healthplatform.chartcam.database.ChartCamQueries
import io.healthplatform.chartcam.models.familyName
import io.healthplatform.chartcam.models.givenName
import io.healthplatform.chartcam.utils.runSuspendCatching
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.Integer as FhirInteger
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Context holding database query handles and index deduplication tracking sets.
 *
 * @property dbQuery Database queries interface.
 * @property resourceType FHIR resource type name.
 * @property resourceId Target resource unique identifier.
 */
internal class ResourceIndexingContext(
    val dbQuery: ChartCamQueries,
    val resourceType: String,
    val resourceId: String,
) {
    val insertedStrings = mutableSetOf<Pair<String, String>>()
    val insertedTokens = mutableSetOf<Triple<String, String?, String>>()
    val insertedReferences = mutableSetOf<Pair<String, String>>()
    val insertedDates = mutableSetOf<Pair<String, String>>()

    /**
     * Routes a single extracted value to the appropriate type-specific indexer.
     *
     * @param paramName Search parameter name.
     * @param paramType Search parameter type enum.
     * @param value Extracted value object.
     * @param targets Target resource classes for reference parameters.
     */
    suspend fun indexValue(
        paramName: String,
        paramType: SearchParamType,
        value: Any,
        targets: List<kotlin.reflect.KClass<out Resource>> = emptyList(),
    ) {
        when (paramType) {
            SearchParamType.String -> indexString(paramName, value)
            SearchParamType.Token -> indexToken(paramName, value)
            SearchParamType.Reference -> indexReference(paramName, value, targets)
            SearchParamType.Date -> indexDate(paramName, value)
            SearchParamType.Number -> indexNumber(paramName, value)
            SearchParamType.Quantity -> indexQuantity(paramName, value)
            SearchParamType.Uri,
            SearchParamType.Composite,
            SearchParamType.Special,
            -> indexFallback(paramName, value)
        }
    }

    /**
     * Indexes string-like values.
     *
     * @param paramName Search parameter name.
     * @param value Extracted value.
     */
    private suspend fun indexString(paramName: String, value: Any) {
        when (value) {
            is HumanName -> indexHumanName(paramName, value)
            is Address -> indexAddress(paramName, value)
            is FhirString -> {
                val strVal = value.value
                if (strVal != null) {
                    insertString(paramName, strVal)
                }
            }
            else -> insertString(paramName, value.toString().trim())
        }
    }

    /**
     * Indexes HumanName components.
     *
     * @param paramName Search parameter name.
     * @param name The HumanName element.
     */
    private suspend fun indexHumanName(paramName: String, name: HumanName) {
        val family = name.familyName
        val given = name.givenName
        insertString("family", family)
        insertString("given", given)
        insertString(paramName, "$given $family".trim())
    }

    /**
     * Indexes Address components.
     *
     * @param paramName Search parameter name.
     * @param address The Address element.
     */
    private suspend fun indexAddress(paramName: String, address: Address) {
        val lineParts = mutableListOf<String>()
        for (item in address.line) {
            val itemVal = item.value
            if (itemVal != null) {
                lineParts.add(itemVal)
            }
        }
        val line = lineParts.joinToString(" ")
        val cityObj = address.city
        val city =
            if (cityObj != null) {
                val c = cityObj.value
                if (c != null) c else ""
            } else {
                ""
            }
        insertString(paramName, "$line $city".trim())
    }

    /**
     * Safely inserts a deduplicated string index record.
     *
     * @param name Index key name.
     * @param value String value to index.
     */
    private suspend fun insertString(name: String, value: String) {
        if (value.isNotBlank() && insertedStrings.add(name to value)) {
            dbQuery.insertStringIndex(resourceType, resourceId, name, value)
        }
    }

    /**
     * Indexes token-like values.
     *
     * @param paramName Search parameter name.
     * @param value Extracted value.
     */
    private suspend fun indexToken(paramName: String, value: Any) {
        when (value) {
            is Identifier -> indexIdentifier(paramName, value)
            is Coding -> indexCoding(paramName, value)
            is CodeableConcept -> indexCodeableConcept(paramName, value)
            is Code -> {
                val c = value.value
                if (c != null) insertToken(paramName, null, c)
            }
            is Enumeration<*> -> indexEnumeration(paramName, value)
            is ExtensibleEnumeration<*> -> indexExtensibleEnumeration(paramName, value)
            is FhirBoolean -> {
                val b = value.value
                if (b != null) insertToken(paramName, null, b.toString())
            }
            else -> insertToken(paramName, null, value.toString().trim())
        }
    }

    /**
     * Indexes a single Coding element.
     *
     * @param paramName Search parameter name.
     * @param coding The Coding element.
     */
    private suspend fun indexCoding(paramName: String, coding: Coding) {
        val codeObj = coding.code
        val c = if (codeObj != null) codeObj.value else null
        if (c != null && c.isNotBlank()) {
            val sysObj = coding.system
            val sys = if (sysObj != null) sysObj.value else null
            insertToken(paramName, sys, c)
            if (sys != null) insertToken(paramName, null, c)
        }
    }

    /**
     * Indexes ExtensibleEnumeration values.
     *
     * @param paramName Search parameter name.
     * @param extEnum The ExtensibleEnumeration element.
     */
    private suspend fun indexExtensibleEnumeration(paramName: String, extEnum: ExtensibleEnumeration<*>) {
        val code = extEnum.code
        val system =
            if (extEnum is ExtensibleEnumeration.Predefined<*>) {
                extEnum.value.system
            } else {
                null
            }
        if (code.isNotBlank()) {
            insertToken(paramName, system, code)
            if (system != null) insertToken(paramName, null, code)
        }
    }

    /**
     * Indexes patient and general identifiers.
     *
     * @param paramName Search parameter name.
     * @param id The Identifier element.
     */
    private suspend fun indexIdentifier(paramName: String, id: Identifier) {
        val fhirStr = id.value
        val tokenVal = if (fhirStr != null) fhirStr.value else null
        if (tokenVal != null && tokenVal.isNotBlank()) {
            val sysObj = id.system
            val system = if (sysObj != null) sysObj.value else null
            insertToken(paramName, system, tokenVal)
            if (resourceType == "Patient") {
                insertToken("mrn", system, tokenVal)
                insertString("mrn", tokenVal)
            }
        }
    }

    /**
     * Indexes CodeableConcept codings.
     *
     * @param paramName Search parameter name.
     * @param concept The CodeableConcept element.
     */
    private suspend fun indexCodeableConcept(paramName: String, concept: CodeableConcept) {
        for (c in concept.coding) {
            indexCoding(paramName, c)
        }
    }

    /**
     * Indexes FHIR Enumeration values.
     *
     * @param paramName Search parameter name.
     * @param enumElement The Enumeration element.
     */
    private suspend fun indexEnumeration(paramName: String, enumElement: Enumeration<*>) {
        val enumVal = enumElement.value ?: return
        val code = enumVal.code
        val system = enumVal.system
        insertToken(paramName, system, code)
        insertToken(paramName, null, code)
        val name = (enumVal as? Enum<*>)?.name
        if (name != null && name != code) {
            insertToken(paramName, system, name)
            insertToken(paramName, null, name)
        }
    }

    /**
     * Safely inserts a deduplicated token index record.
     *
     * @param name Index key name.
     * @param system Optional terminology system URI.
     * @param value Token code value.
     */
    private suspend fun insertToken(name: String, system: String?, value: String) {
        if (value.isNotBlank() && insertedTokens.add(Triple(name, system, value))) {
            dbQuery.insertTokenIndex(resourceType, resourceId, name, system, value)
        }
    }

    /**
     * Indexes reference-like values with support for declared target types.
     *
     * @param paramName Search parameter name.
     * @param value Extracted value.
     * @param targets Target resource classes for reference parameters.
     */
    private suspend fun indexReference(
        paramName: String,
        value: Any,
        targets: List<kotlin.reflect.KClass<out Resource>>,
    ) {
        val refRaw =
            when (value) {
                is Reference -> {
                    val r = value.reference
                    if (r != null) r.value else null
                }
                is Canonical -> value.value
                else -> value.toString()
            }
        if (refRaw == null) return
        val refStr = refRaw.trim()
        if (refStr.isBlank()) return
        val bareId = refStr.substringAfter('/')
        insertRef(paramName, refStr)
        if (bareId != refStr) insertRef(paramName, bareId)

        for (targetClass in targets) {
            val targetName = targetClass.simpleName
            if (targetName != null) {
                val fullTargetRef = "$targetName/$bareId"
                insertRef(paramName, fullTargetRef)
                when (targetName) {
                    "Patient" -> {
                        insertRef("patient", fullTargetRef)
                        insertRef("patient", bareId)
                        insertRef("subject", fullTargetRef)
                        insertRef("subject", bareId)
                    }
                    "Encounter" -> {
                        insertRef("encounter", fullTargetRef)
                        insertRef("encounter", bareId)
                    }
                    "Practitioner" -> {
                        insertRef("practitioner", fullTargetRef)
                        insertRef("practitioner", bareId)
                    }
                    else -> Unit
                }
            }
        }

        indexCrossReferences(paramName, refStr, bareId)
    }

    /**
     * Cross-indexes subject, encounter, and practitioner references.
     *
     * @param paramName Search parameter name.
     * @param refStr Full reference string.
     * @param bareId Stripped resource ID.
     */
    private suspend fun indexCrossReferences(paramName: String, refStr: String, bareId: String) {
        if (paramName == "subject" || paramName == "patient") {
            val fullPatient = if (refStr.startsWith("Patient/")) refStr else "Patient/$bareId"
            insertRef("patient", fullPatient)
            insertRef("patient", bareId)
            insertRef("subject", fullPatient)
            insertRef("subject", bareId)
        }
        if (paramName == "encounter") {
            val fullEnc = if (refStr.startsWith("Encounter/")) refStr else "Encounter/$bareId"
            insertRef("encounter", fullEnc)
            insertRef("encounter", bareId)
        }
        if (paramName == "practitioner") {
            val fullPrac = if (refStr.startsWith("Practitioner/")) refStr else "Practitioner/$bareId"
            insertRef("practitioner", fullPrac)
            insertRef("practitioner", bareId)
        }
        if (paramName == "target") {
            insertRef("target", refStr)
            insertRef("target", bareId)
            if (refStr.startsWith("Encounter/")) {
                insertRef("encounter", refStr)
                insertRef("encounter", bareId)
            }
            if (refStr.startsWith("Patient/")) {
                insertRef("patient", refStr)
                insertRef("patient", bareId)
            }
        }
    }

    /**
     * Safely inserts a deduplicated reference index record.
     *
     * @param name Index key name.
     * @param ref Target reference URI or bare ID.
     */
    private suspend fun insertRef(name: String, ref: String) {
        if (ref.isNotBlank() && insertedReferences.add(name to ref)) {
            dbQuery.insertReferenceIndex(resourceType, resourceId, name, ref)
        }
    }

    /**
     * Safely inserts a deduplicated date index record.
     *
     * @param name Index key name.
     * @param date Date value to index.
     */
    private suspend fun insertDate(name: String, date: String?) {
        if (date != null && date.isNotBlank() && insertedDates.add(name to date)) {
            dbQuery.insertDateIndex(resourceType, resourceId, name, date)
        }
    }

    /**
     * Indexes date-like values.
     *
     * @param paramName Search parameter name.
     * @param value Extracted value.
     */
    private suspend fun indexDate(paramName: String, value: Any) {
        if (value is Period) {
            indexPeriod(paramName, value)
            return
        }
        val dateRaw =
            when (value) {
                is Date -> {
                    val v = value.value
                    if (v != null) v.toString() else null
                }
                is DateTime -> {
                    val v = value.value
                    if (v != null) v.toString() else null
                }
                is Instant -> {
                    val v = value.value
                    if (v != null) v.toString() else null
                }
                is FhirDate -> value.toString()
                is FhirDateTime -> value.toString()
                else -> value.toString()
            }
        val dateStr = if (dateRaw != null) dateRaw.trim() else null
        insertDate(paramName, dateStr)
    }

    /**
     * Indexes both start and end boundaries of a Period element.
     *
     * @param paramName Search parameter name.
     * @param period Period to index.
     */
    private suspend fun indexPeriod(paramName: String, period: Period) {
        val s = period.start
        val startVal =
            if (s != null) {
                val sv = s.value
                if (sv != null) sv.toString().trim() else null
            } else {
                null
            }
        insertDate(paramName, startVal)

        val e = period.end
        val endVal =
            if (e != null) {
                val ev = e.value
                if (ev != null) ev.toString().trim() else null
            } else {
                null
            }
        insertDate(paramName, endVal)
    }

    /**
     * Indexes quantity values into token indices with optional system/unit matching.
     *
     * @param paramName Search parameter name.
     * @param value Extracted quantity value.
     */
    private suspend fun indexQuantity(paramName: String, value: Any) {
        if (value is Quantity) {
            val fhirVal = value.value
            val numStr =
                if (fhirVal != null) {
                    val d = fhirVal.value
                    if (d != null) d.toString() else ""
                } else {
                    ""
                }
            val sysObj = value.system
            val system = if (sysObj != null) sysObj.value else null
            val codeObj = value.code
            val unitObj = value.unit
            val unit =
                if (codeObj != null && codeObj.value != null) {
                    codeObj.value
                } else if (unitObj != null) {
                    unitObj.value
                } else {
                    null
                }
            if (numStr.isNotBlank()) {
                insertToken(paramName, system, numStr)
                insertToken(paramName, null, numStr)
                if (unit != null) {
                    insertToken(paramName, system, "$numStr|$unit")
                }
            }
        } else {
            indexFallback(paramName, value)
        }
    }

    /**
     * Indexes numeric values into token indices.
     *
     * @param paramName Search parameter name.
     * @param value Extracted value.
     */
    private suspend fun indexNumber(paramName: String, value: Any) {
        val numStr =
            when (value) {
                is FhirInteger -> {
                    val v = value.value
                    if (v != null) v.toString() else ""
                }
                is FhirDecimal -> value.toString()
                is Number -> value.toString()
                else -> value.toString()
            }.trim()

        insertToken(paramName, null, numStr)
    }

    /**
     * Fallback for special or URI parameters.
     *
     * @param paramName Search parameter name.
     * @param value Extracted value.
     */
    private suspend fun indexFallback(paramName: String, value: Any) {
        insertToken(paramName, null, value.toString().trim())
    }
}

/**
 * Generic SearchParam-driven indexing engine for local persistence.
 */
object FhirSearchIndexerEngine {
    /**
     * Dynamically indexes a resource using a list of strongly-typed SearchParams.
     *
     * @param R The resource type.
     * @param dbQuery The queries handle.
     * @param resource The resource to index.
     * @param resourceType The resource type name.
     * @param resourceId The resource ID.
     * @param params The list of SearchParams to extract and persist.
     * @return A [Result] indicating success or failure.
     */
    suspend fun <R : Resource> indexResourceGeneric(
        dbQuery: ChartCamQueries,
        resource: R,
        resourceType: String,
        resourceId: String,
        params: List<SearchParam<R, *>>,
    ): Result<Unit> =
        runSuspendCatching {
            val ctx = ResourceIndexingContext(dbQuery, resourceType, resourceId)
            indexUniversalBaseParameters(ctx, resource)

            for (param in params) {
                val values = runCatching { resource.extract(param) }.getOrDefault(emptyList())
                if (values.isEmpty()) continue

                for (value in values) {
                    if (value == null) continue
                    ctx.indexValue(param.name, param.type, value, param.target)
                }
            }
        }

    /**
     * Indexes standard base Resource metadata elements (_id, _lastUpdated, _tag, _profile, _security).
     *
     * @param ctx Indexing context.
     * @param resource Target resource.
     */
    private suspend fun indexUniversalBaseParameters(
        ctx: ResourceIndexingContext,
        resource: Resource,
    ) {
        val idVal = resource.id
        if (idVal != null) {
            ctx.indexValue("_id", SearchParamType.Token, idVal)
            ctx.indexValue("_id", SearchParamType.String, idVal)
        }
        val meta = resource.meta
        if (meta != null) {
            val lu = meta.lastUpdated?.value
            if (lu != null) {
                ctx.indexValue("_lastUpdated", SearchParamType.Date, lu)
            }
            for (tag in meta.tag) {
                val codeVal = tag.code?.value
                if (codeVal != null) {
                    ctx.indexValue("_tag", SearchParamType.Token, tag)
                }
            }
            for (prof in meta.profile) {
                val profVal = prof.value
                if (profVal != null) {
                    ctx.indexValue("_profile", SearchParamType.Reference, profVal)
                }
            }
            for (sec in meta.security) {
                val secVal = sec.code?.value
                if (secVal != null) {
                    ctx.indexValue("_security", SearchParamType.Token, sec)
                }
            }
        }
    }
}
