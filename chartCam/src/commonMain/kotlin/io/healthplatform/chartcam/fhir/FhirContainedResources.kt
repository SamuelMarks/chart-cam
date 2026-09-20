/**
 * @file FhirContainedResources.kt
 * Multiplatform helper extensions for safely inspecting and extracting contained child resources from DomainResource.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.AuditEvent
import dev.ohs.fhir.model.r4.Basic
import dev.ohs.fhir.model.r4.CarePlan
import dev.ohs.fhir.model.r4.Condition
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DiagnosticReport
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.DomainResource
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Media
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Organization
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Safely resolves a strongly-typed contained resource from a [DomainResource] by local reference ID.
 *
 * Handles references formatted with or without the leading '#' symbol (e.g. "#child-1" or "child-1").
 *
 * @param T The expected concrete [Resource] subtype.
 * @param reference The local reference string.
 * @return A [Result] enclosing the matched resource instance, or null if not found.
 */
inline fun <reified T : Resource> DomainResource.getContainedResource(reference: String): Result<T?> =
    runCatching {
        val cleanRef = reference.trim().removePrefix("#")
        val found = contained.firstOrNull { it.id == cleanRef }
        found as? T
    }

/**
 * Resolves all contained child resources matching a specific concrete [Resource] type.
 *
 * @param T The expected [Resource] type.
 * @return A list of matching typed resources contained within this domain resource.
 */
inline fun <reified T : Resource> DomainResource.resolveAllContained(): List<T> =
    contained.filterIsInstance<T>()

/**
 * Creates a local internal FHIR [Reference] formatted with a '#' prefix.
 *
 * @param child The target [Resource].
 * @return A [Reference] pointing to the child resource ID locally.
 */
fun createLocalReference(child: Resource): Reference {
    val id = child.id
    val cleanId = if (id != null) id.removePrefix("#") else ""
    return Reference(reference = FhirString(value = "#$cleanId"))
}

/**
 * Attaches a child [Resource] to a [DomainResource]'s contained list in an immutable fashion.
 *
 * @param T The concrete [DomainResource] type.
 * @param parent The parent domain resource.
 * @param child The child resource to contain.
 * @return A [Result] enclosing the updated parent resource.
 */
@Suppress("UNCHECKED_CAST")
fun <T : DomainResource> addContainedResource(
    parent: T,
    child: Resource,
): Result<T> =
    runCatching {
        val updatedContained = parent.contained + child
        val updated =
            copyClinicalResource(parent, updatedContained)
                ?: copyAdministrativeResource(parent, updatedContained)
                ?: error("Unsupported DomainResource type for containment: ${parent::class.simpleName}")
        updated as T
    }

/**
 * Creates an updated copy of clinical domain resources with new contained items.
 *
 * @param parent The parent domain resource.
 * @param updatedContained The new list of contained resources.
 * @return The updated resource copy or null if not a matching clinical type.
 */
private fun copyClinicalResource(
    parent: DomainResource,
    updatedContained: List<Resource>,
): DomainResource? =
    when (parent) {
        is Patient -> parent.copy(contained = updatedContained)
        is Encounter -> parent.copy(contained = updatedContained)
        is DocumentReference -> parent.copy(contained = updatedContained)
        is QuestionnaireResponse -> parent.copy(contained = updatedContained)
        is Questionnaire -> parent.copy(contained = updatedContained)
        is Observation -> parent.copy(contained = updatedContained)
        is Condition -> parent.copy(contained = updatedContained)
        is CarePlan -> parent.copy(contained = updatedContained)
        is DiagnosticReport -> parent.copy(contained = updatedContained)
        else -> null
    }

/**
 * Creates an updated copy of administrative or media domain resources with new contained items.
 *
 * @param parent The parent domain resource.
 * @param updatedContained The new list of contained resources.
 * @return The updated resource copy or null if not a matching administrative type.
 */
private fun copyAdministrativeResource(
    parent: DomainResource,
    updatedContained: List<Resource>,
): DomainResource? =
    when (parent) {
        is Media -> parent.copy(contained = updatedContained)
        is Provenance -> parent.copy(contained = updatedContained)
        is AuditEvent -> parent.copy(contained = updatedContained)
        is Practitioner -> parent.copy(contained = updatedContained)
        is Device -> parent.copy(contained = updatedContained)
        is Organization -> parent.copy(contained = updatedContained)
        is Basic -> parent.copy(contained = updatedContained)
        else -> null
    }
