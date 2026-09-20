/**
 * @file FhirResourceTypes.kt
 * Reflection-free canonical resource type resolution for FHIR R4 resources.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.AuditEvent
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
import dev.ohs.fhir.model.r4.terminologies.ResourceType

/**
 * Resolves the canonical FHIR resourceType string for a reified [Resource] type without reflection.
 *
 * This protects against minification or obfuscation name mangling on Kotlin/JS and Kotlin/Wasm platforms.
 *
 * @param R The concrete FHIR [Resource] type.
 * @return A [Result] enclosing the canonical FHIR resourceType string.
 */
inline fun <reified R : Resource> resolveCanonicalResourceType(): Result<String> =
    runCatching {
        val rawSerialName =
            kotlinx.serialization
                .serializer<R>()
                .descriptor.serialName
                .substringAfterLast('.')
        ResourceType.fromCode(rawSerialName).code
    }

/**
 * Resolves the canonical FHIR resourceType string for any runtime [Resource] instance.
 *
 * @param resource The target resource.
 * @return The canonical resourceType name.
 */
fun resolveResourceTypeName(resource: Resource): String =
    when (resource) {
        is Patient -> "Patient"
        is Encounter -> "Encounter"
        is Observation -> "Observation"
        is DocumentReference -> "DocumentReference"
        is Questionnaire -> "Questionnaire"
        is QuestionnaireResponse -> "QuestionnaireResponse"
        is Practitioner -> "Practitioner"
        is Device -> "Device"
        is Provenance -> "Provenance"
        is Media -> "Media"
        is AuditEvent -> "AuditEvent"
        is Bundle -> "Bundle"
        else -> resolveDynamicResourceTypeName(resource)
    }

/**
 * Fallback dynamic resolution of resource type name for unlisted resource instances.
 *
 * @param resource The target resource.
 * @return The resolved canonical name.
 */
internal fun resolveDynamicResourceTypeName(resource: Resource): String {
    val name = resource::class.simpleName ?: ""
    return runCatching {
        ResourceType.fromCode(name).code
    }.getOrDefault("Resource")
}
