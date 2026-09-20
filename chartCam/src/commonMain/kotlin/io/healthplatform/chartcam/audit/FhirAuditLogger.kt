/**
 * @file FhirAuditLogger.kt
 * Air-gapped security and HIPAA/GDPR audit logger generating standard FHIR AuditEvent resources.
 */

package io.healthplatform.chartcam.audit

import dev.ohs.fhir.model.r4.AuditEvent
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.utils.UUID
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Security audit action categories for decentralized clinical compliance.
 */
enum class SecurityAuditAction {
    /** Creation of a clinical entity. */
    CREATE,

    /** Access or inspection of a clinical entity. */
    READ,

    /** Modification of a clinical entity. */
    UPDATE,

    /** Deletion of a clinical entity. */
    DELETE,

    /** Export of encrypted dataset bundle. */
    EXPORT,

    /** Ingestion of peer-to-peer dataset bundle. */
    IMPORT,

    /** Air-gapped cryptographic keystore unlock. */
    KEY_UNLOCK,
}

/**
 * Security audit outcome classifications.
 */
enum class SecurityAuditOutcome {
    /** Operation succeeded completely. */
    SUCCESS,

    /** Minor operational warning or recoverable failure. */
    MINOR_FAILURE,

    /** Serious failure or authorization defect. */
    SERIOUS_FAILURE,

    /** Major failure or security exception. */
    MAJOR_FAILURE,
}

/**
 * Air-gapped FHIR AuditEvent generator and persistence logger.
 */
object FhirAuditLogger {
    /**
     * Records a local security audit event into the local FHIR repository as an [AuditEvent].
     *
     * @param repository The local FHIR repository.
     * @param action The security action being performed.
     * @param resourceType The target clinical resource type.
     * @param resourceId The target clinical resource ID.
     * @param practitionerId The optional ID of the acting practitioner.
     * @param outcome The outcome classification of the action.
     * @param outcomeDesc Optional diagnostic explanation of the outcome.
     * @return A [Result] enclosing the persisted [AuditEvent] resource.
     */
    suspend fun logAuditEvent(
        repository: FhirRepository,
        action: SecurityAuditAction,
        resourceType: String,
        resourceId: String,
        practitionerId: String? = null,
        outcome: SecurityAuditOutcome = SecurityAuditOutcome.SUCCESS,
        outcomeDesc: String? = null,
    ): Result<AuditEvent> {
        val fhirAction = mapAuditAction(action)
        val fhirOutcome = mapAuditOutcome(outcome)
        val auditId = UUID.randomUUID()
        val nowStr =
            kotlin.time.Clock.System
                .now()
                .toString()

        val auditEvent =
            AuditEvent(
                id = auditId,
                type =
                    Coding(
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/audit-event-type"),
                        code = Code(value = "rest"),
                        display = FhirString(value = "Restful Operation"),
                    ),
                action = Enumeration(value = fhirAction),
                recorded = Instant(value = FhirDateTime.fromString(nowStr)),
                outcome = Enumeration(value = fhirOutcome),
                outcomeDesc = outcomeDesc?.let { FhirString(value = it) },
                agent =
                    listOf(
                        AuditEvent.Agent(
                            requestor = FhirBoolean(value = true),
                            who =
                                practitionerId?.let {
                                    Reference(reference = FhirString(value = "Practitioner/$it"))
                                },
                        ),
                    ),
                source =
                    AuditEvent.Source(
                        observer = Reference(reference = FhirString(value = "Device/local-chartcam-app")),
                    ),
                entity =
                    listOf(
                        AuditEvent.Entity(
                            what = Reference(reference = FhirString(value = "$resourceType/$resourceId")),
                        ),
                    ),
            )

        return repository.saveResource("AuditEvent", auditId, auditEvent, isLocalChange = false).map { auditEvent }
    }

    /**
     * Maps security audit action to FHIR AuditEventAction enum.
     *
     * @param action Source security audit action.
     * @return FHIR [AuditEvent.AuditEventAction].
     */
    private fun mapAuditAction(action: SecurityAuditAction): AuditEvent.AuditEventAction =
        when (action) {
            SecurityAuditAction.CREATE -> AuditEvent.AuditEventAction.C
            SecurityAuditAction.READ -> AuditEvent.AuditEventAction.R
            SecurityAuditAction.UPDATE -> AuditEvent.AuditEventAction.U
            SecurityAuditAction.DELETE -> AuditEvent.AuditEventAction.D
            SecurityAuditAction.EXPORT,
            SecurityAuditAction.IMPORT,
            SecurityAuditAction.KEY_UNLOCK,
            -> AuditEvent.AuditEventAction.E
        }

    /**
     * Maps security audit outcome to FHIR AuditEventOutcome enum.
     *
     * @param outcome Source security audit outcome.
     * @return FHIR [AuditEvent.AuditEventOutcome].
     */
    private fun mapAuditOutcome(outcome: SecurityAuditOutcome): AuditEvent.AuditEventOutcome =
        when (outcome) {
            SecurityAuditOutcome.SUCCESS -> AuditEvent.AuditEventOutcome._0
            SecurityAuditOutcome.MINOR_FAILURE -> AuditEvent.AuditEventOutcome._4
            SecurityAuditOutcome.SERIOUS_FAILURE -> AuditEvent.AuditEventOutcome._8
            SecurityAuditOutcome.MAJOR_FAILURE -> AuditEvent.AuditEventOutcome._12
        }
}
