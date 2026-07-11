/**
 * Contains models and utility extensions for interacting with FHIR resources.
 */
package io.healthplatform.chartcam.models

import com.google.fhir.model.r4.Attachment
import com.google.fhir.model.r4.Base64Binary
import com.google.fhir.model.r4.Binary
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.Device
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Identifier
import com.google.fhir.model.r4.Instant
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Period
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Provenance
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.Uri
import com.google.fhir.model.r4.Url
import com.google.fhir.model.r4.terminologies.DocumentReferenceStatus
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Represents the response received when authenticating, containing tokens.
 *
 * @property accessToken The token used to access protected resources.
 * @property refreshToken The token used to refresh the access token.
 * @property expiresIn The duration in seconds until the access token expires.
 * @property tokenType The type of the token, usually "Bearer".
 */
@Serializable
data class TokenResponse(
    /** The token used to access protected resources. */
    val accessToken: kotlin.String,
    /** The token used to refresh the access token. */
    val refreshToken: kotlin.String,
    /** The duration in seconds until the access token expires. */
    val expiresIn: Int,
    /** The type of the token, usually "Bearer". */
    val tokenType: kotlin.String,
)

/**
 * Creates a FHIR Patient resource.
 *
 * @param id The unique identifier of the patient.
 * @param firstName The first name of the patient.
 * @param lastName The last name of the patient.
 * @param dob The date of birth of the patient.
 * @param mrnValue The Medical Record Number (MRN) of the patient.
 * @param genderStr The gender of the patient as a string.
 * @return A populated FHIR [Patient] object.
 */
fun createFhirPatient(
    id: kotlin.String,
    firstName: kotlin.String,
    lastName: kotlin.String,
    dob: LocalDate,
    mrnValue: kotlin.String,
    genderStr: kotlin.String,
): Patient =
    Patient
        .Builder()
        .apply {
            this.id = id
            name.add(
                HumanName.Builder().apply {
                    family = String.Builder().apply { value = lastName }
                    given.add(String.Builder().apply { value = firstName })
                },
            )
            birthDate = Date.Builder().apply { value = FhirDate.fromString(dob.toString()) }
            identifier.add(
                Identifier.Builder().apply {
                    system = Uri.Builder().apply { value = "urn:oid:1.2.36.146.595.217.0.1" }
                    value = String.Builder().apply { value = mrnValue }
                },
            )
        }.build()

/**
 * Extension property to get the Medical Record Number (MRN) from a [Patient].
 */
val Patient.mrn: kotlin.String
    get() = identifier.firstOrNull()?.value?.value ?: ""

/**
 * Extension property to get the birth date from a [Patient] as a string.
 */
val Patient.customBirthDate: kotlin.String
    get() = birthDate?.value?.toString() ?: ""

/**
 * Extension property to get the family name from a [HumanName].
 */
val HumanName.familyName: kotlin.String
    get() = family?.value ?: "Unknown"

/**
 * Extension property to get the given name from a [HumanName].
 */
val HumanName.givenName: kotlin.String
    get() = given.firstOrNull()?.value ?: "Unknown"

/**
 * Extension property to get the full formatted name from a [Patient].
 */
val Patient.fullName: kotlin.String
    get() {
        val n = name.firstOrNull()
        return if (n != null) "${n.familyName}, ${n.givenName}" else "Unknown, Unknown"
    }

/**
 * Extension property to get the full formatted name from a [Practitioner].
 */
val Practitioner.fullName: kotlin.String
    get() {
        val n = name.firstOrNull()
        return if (n != null) "${n.familyName}, ${n.givenName}" else "Unknown, Unknown"
    }

/**
 * Creates a FHIR Practitioner resource.
 *
 * @param id The unique identifier of the practitioner.
 * @param lastName The last name of the practitioner.
 * @param firstName The first name of the practitioner.
 * @param isActive Whether the practitioner is active.
 * @return A populated FHIR [Practitioner] object.
 */
fun createFhirPractitioner(
    id: kotlin.String,
    lastName: kotlin.String,
    firstName: kotlin.String,
    isActive: kotlin.Boolean,
): Practitioner =
    Practitioner
        .Builder()
        .apply {
            this.id = id
            active = Boolean.Builder().apply { value = isActive }
            name.add(
                HumanName.Builder().apply {
                    family = String.Builder().apply { value = lastName }
                    given.add(String.Builder().apply { value = firstName })
                },
            )
        }.build()

/**
 * Creates a FHIR Encounter resource.
 *
 * @param id The unique identifier of the encounter.
 * @param patientId The reference ID of the associated patient.
 * @param practitionerId The reference ID of the associated practitioner.
 * @param dateStr The date and time of the encounter as a string.
 * @param statusStr The status of the encounter as a string.
 * @return A populated FHIR [Encounter] object.
 */
fun createFhirEncounter(
    id: kotlin.String,
    patientId: kotlin.String,
    practitionerId: kotlin.String,
    dateStr: kotlin.String,
    statusStr: kotlin.String,
): Encounter =
    Encounter
        .Builder(
            status = Enumeration(value = Encounter.EncounterStatus.In_Progress),
            `class` =
                Coding.Builder().apply {
                    system = Uri.Builder().apply { value = "http://terminology.hl7.org/CodeSystem/v3-ActCode" }
                    code = Code.Builder().apply { value = "AMB" }
                },
        ).apply {
            this.id = id
            subject = Reference.Builder().apply { reference = String.Builder().apply { value = patientId } }
            participant.add(
                Encounter.Participant.Builder().apply {
                    individual = Reference.Builder().apply { reference = String.Builder().apply { value = practitionerId } }
                },
            )
            period =
                Period.Builder().apply {
                    start = DateTime.Builder().apply { value = FhirDateTime.fromString(dateStr) }
                }
        }.build()

/**
 * Extension property to get the encounter start date from an [Encounter] as a string.
 */
val Encounter.encounterDate: kotlin.String
    get() = period?.start?.value?.toString() ?: ""

/**
 * Creates a FHIR DocumentReference resource for an attachment or generic document.
 *
 * @param id The unique identifier of the document reference.
 * @param patientId The reference ID of the associated patient.
 * @param encounterId The reference ID of the associated encounter.
 * @param dateStr The date and time of the document creation as a string.
 * @param desc An optional description of the document.
 * @param mime The MIME type of the document content.
 * @param urlPath The URL or path to access the document content.
 * @return A populated FHIR [DocumentReference] object.
 */
fun createFhirDocumentReference(
    id: kotlin.String,
    patientId: kotlin.String,
    encounterId: kotlin.String,
    dateStr: kotlin.String,
    desc: kotlin.String?,
    mime: kotlin.String,
    urlPath: kotlin.String,
): DocumentReference =
    DocumentReference
        .Builder(
            status = Enumeration(value = DocumentReferenceStatus.Current),
            content =
                mutableListOf(
                    DocumentReference.Content.Builder(
                        attachment =
                            Attachment.Builder().apply {
                                contentType = Code.Builder().apply { value = mime }
                                url = Url.Builder().apply { value = urlPath }
                            },
                    ),
                ),
        ).apply {
            this.id = id
            subject = Reference.Builder().apply { reference = String.Builder().apply { value = patientId } }
            context =
                DocumentReference.Context.Builder().apply {
                    encounter.add(Reference.Builder().apply { reference = String.Builder().apply { value = encounterId } })
                }
            try {
                // we ignore the date parse error just in case dateStr is wrong format
                date = Instant.Builder().apply { value = FhirDateTime.fromString(dateStr) }
            } catch (e: Exception) {
            }
            if (!desc.isNullOrBlank()) {
                description = String.Builder().apply { value = desc }
            }
        }.build()

/**
 * Creates a FHIR DocumentReference resource specifically for a clinical note.
 *
 * @param id The unique identifier of the document reference.
 * @param patientId The reference ID of the associated patient.
 * @param encounterId The reference ID of the associated encounter.
 * @param dateStr The date and time of the document creation as a string.
 * @param notesText The textual content of the clinical note.
 * @return A populated FHIR [DocumentReference] object representing a clinical note.
 */
fun createFhirClinicalNote(
    id: kotlin.String,
    patientId: kotlin.String,
    encounterId: kotlin.String,
    dateStr: kotlin.String,
    notesText: kotlin.String,
): DocumentReference =
    DocumentReference
        .Builder(
            status = Enumeration(value = DocumentReferenceStatus.Current),
            content =
                mutableListOf(
                    DocumentReference.Content.Builder(
                        attachment =
                            Attachment.Builder().apply {
                                contentType = Code.Builder().apply { value = "text/plain" }
                                // Using data URI for text content to avoid protobuf ByteString dependency in commonMain
                                url = Url.Builder().apply { value = "data:text/plain;charset=utf-8,$notesText" }
                            },
                    ),
                ),
        ).apply {
            this.id = id
            subject = Reference.Builder().apply { reference = String.Builder().apply { value = patientId } }
            context =
                DocumentReference.Context.Builder().apply {
                    encounter.add(Reference.Builder().apply { reference = String.Builder().apply { value = encounterId } })
                }
            type =
                com.google.fhir.model.r4.CodeableConcept.Builder().apply {
                    coding.add(
                        Coding.Builder().apply {
                            system = Uri.Builder().apply { value = io.healthplatform.chartcam.terminology.TerminologyService.loincUri }
                            code = Code.Builder().apply { value = "11488-4" }
                            display = String.Builder().apply { value = "Consultation note" }
                        },
                    )
                }
            try {
                date = Instant.Builder().apply { value = FhirDateTime.fromString(dateStr) }
            } catch (e: Exception) {
            }
        }.build()

/**
 * Creates a FHIR Device resource.
 *
 * @param id The unique identifier of the device.
 * @param modelName The model name of the device.
 * @param manufacturerName The name of the device manufacturer.
 * @return A populated FHIR [Device] object.
 */
fun createFhirDevice(
    id: kotlin.String,
    modelName: kotlin.String,
    manufacturerName: kotlin.String,
): Device =
    Device
        .Builder()
        .apply {
            this.id = id
            deviceName.add(
                Device.DeviceName.Builder(
                    name = String.Builder().apply { value = modelName },
                    type = Enumeration(value = Device.DeviceNameType.Model_Name),
                ),
            )
            manufacturer = String.Builder().apply { value = manufacturerName }
        }.build()

/**
 * Creates a FHIR Provenance resource to track the origin or history of a target resource.
 *
 * @param id The unique identifier of the provenance resource.
 * @param targetResourceId The reference ID of the target resource.
 * @param practitionerId The reference ID of the practitioner who acted as the agent.
 * @param dateStr The date and time of the provenance event as a string.
 * @return A populated FHIR [Provenance] object.
 */
fun createFhirProvenance(
    id: kotlin.String,
    targetResourceId: kotlin.String,
    practitionerId: kotlin.String,
    dateStr: kotlin.String,
): Provenance =
    Provenance
        .Builder(
            target = mutableListOf(Reference.Builder().apply { reference = String.Builder().apply { value = targetResourceId } }),
            recorded = Instant.Builder().apply { value = FhirDateTime.fromString(dateStr) },
            agent =
                mutableListOf(
                    Provenance.Agent
                        .Builder(
                            who = Reference.Builder().apply { reference = String.Builder().apply { value = practitionerId } },
                        ).apply {
                            type =
                                com.google.fhir.model.r4.CodeableConcept.Builder().apply {
                                    coding.add(
                                        Coding.Builder().apply {
                                            system =
                                                Uri.Builder().apply {
                                                    value =
                                                        "http://terminology.hl7.org/CodeSystem/provenance-participant-type"
                                                }
                                            code = Code.Builder().apply { value = "author" }
                                        },
                                    )
                                }
                        },
                ),
        ).apply {
            this.id = id
        }.build()

/**
 * Creates a FHIR Binary resource to encapsulate raw binary data.
 *
 * @param id The unique identifier of the binary resource.
 * @param contentTypeStr The MIME type of the binary content.
 * @param base64Data The binary data encoded as a base64 string.
 * @return A populated FHIR [Binary] object.
 */
fun createFhirBinary(
    id: kotlin.String,
    contentTypeStr: kotlin.String,
    base64Data: kotlin.String,
): Binary =
    Binary
        .Builder(
            contentType = Code.Builder().apply { value = contentTypeStr },
        ).apply {
            this.id = id
            this.data = Base64Binary.Builder().apply { value = base64Data }
        }.build()
