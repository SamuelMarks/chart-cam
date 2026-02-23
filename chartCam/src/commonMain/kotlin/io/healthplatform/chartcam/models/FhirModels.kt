package io.healthplatform.chartcam.models

import kotlinx.serialization.Serializable
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.DocumentReference
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.DateTime
import com.google.fhir.model.r4.Instant
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.FhirDateTime
import com.google.fhir.model.r4.Identifier
import com.google.fhir.model.r4.Uri
import com.google.fhir.model.r4.Url
import com.google.fhir.model.r4.Reference
import com.google.fhir.model.r4.Period
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.Attachment
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Base64Binary
import com.google.fhir.model.r4.terminologies.DocumentReferenceStatus
import com.google.fhir.model.r4.Device
import com.google.fhir.model.r4.Provenance
import com.google.fhir.model.r4.Binary

import kotlinx.datetime.LocalDate

@Serializable
data class TokenResponse(
    val accessToken: kotlin.String,
    val refreshToken: kotlin.String,
    val expiresIn: Int,
    val tokenType: kotlin.String
)

fun createFhirPatient(
    id: kotlin.String,
    firstName: kotlin.String,
    lastName: kotlin.String,
    dob: LocalDate,
    mrnValue: kotlin.String,
    genderStr: kotlin.String
): Patient {
    return Patient.Builder().apply {
        this.id = id
        name.add(
            HumanName.Builder().apply {
                family = String.Builder().apply { value = lastName }
                given.add(String.Builder().apply { value = firstName })
            }
        )
        birthDate = Date.Builder().apply { value = FhirDate.fromString(dob.toString()) }
        identifier.add(
            Identifier.Builder().apply {
                system = Uri.Builder().apply { value = "urn:oid:1.2.36.146.595.217.0.1" }
                value = String.Builder().apply { value = mrnValue }
            }
        )
    }.build()
}

val Patient.mrn: kotlin.String
    get() = identifier.firstOrNull()?.value?.value ?: ""

val Patient.customBirthDate: kotlin.String
    get() = birthDate?.value?.toString() ?: ""

val HumanName.familyName: kotlin.String
    get() = family?.value ?: "Unknown"

val HumanName.givenName: kotlin.String
    get() = given.firstOrNull()?.value ?: "Unknown"

val Patient.fullName: kotlin.String
    get() = "${name.firstOrNull()?.familyName}, ${name.firstOrNull()?.givenName}"

val Practitioner.fullName: kotlin.String
    get() = "${name.firstOrNull()?.familyName}, ${name.firstOrNull()?.givenName}"

fun createFhirPractitioner(
    id: kotlin.String,
    lastName: kotlin.String,
    firstName: kotlin.String,
    isActive: kotlin.Boolean
): Practitioner {
    return Practitioner.Builder().apply {
        this.id = id
        active = Boolean.Builder().apply { value = isActive }
        name.add(
            HumanName.Builder().apply {
                family = String.Builder().apply { value = lastName }
                given.add(String.Builder().apply { value = firstName })
            }
        )
    }.build()
}

fun createFhirEncounter(
    id: kotlin.String,
    patientId: kotlin.String,
    practitionerId: kotlin.String,
    dateStr: kotlin.String,
    statusStr: kotlin.String
): Encounter {
    return Encounter.Builder(
        status = Enumeration(value = Encounter.EncounterStatus.In_Progress),
        `class` = Coding.Builder().apply {
            system = Uri.Builder().apply { value = "http://terminology.hl7.org/CodeSystem/v3-ActCode" }
            code = Code.Builder().apply { value = "AMB" }
        }
    ).apply {
        this.id = id
        subject = Reference.Builder().apply { reference = String.Builder().apply { value = patientId } }
        participant.add(
            Encounter.Participant.Builder().apply {
                individual = Reference.Builder().apply { reference = String.Builder().apply { value = practitionerId } }
            }
        )
        period = Period.Builder().apply {
            start = DateTime.Builder().apply { value = FhirDateTime.fromString(dateStr) }
        }
    }.build()
}

val Encounter.encounterDate: kotlin.String
    get() = period?.start?.value?.toString() ?: ""

fun createFhirDocumentReference(
    id: kotlin.String,
    patientId: kotlin.String,
    encounterId: kotlin.String,
    dateStr: kotlin.String,
    desc: kotlin.String?,
    mime: kotlin.String,
    urlPath: kotlin.String
): DocumentReference {
    return DocumentReference.Builder(
        status = Enumeration(value = DocumentReferenceStatus.Current),
        content = mutableListOf(
            DocumentReference.Content.Builder(
                attachment = Attachment.Builder().apply {
                    contentType = Code.Builder().apply { value = mime }
                    url = Url.Builder().apply { value = urlPath }
                }
            )
        )
    ).apply {
        this.id = id
        subject = Reference.Builder().apply { reference = String.Builder().apply { value = patientId } }
        context = DocumentReference.Context.Builder().apply {
            encounter.add(Reference.Builder().apply { reference = String.Builder().apply { value = encounterId } })
        }
        try {
            // we ignore the date parse error just in case dateStr is wrong format
            date = Instant.Builder().apply { value = FhirDateTime.fromString(dateStr) }
        } catch (e: Exception) {}
        if (!desc.isNullOrBlank()) {
            description = String.Builder().apply { value = desc }
        }
    }.build()
}

fun createFhirClinicalNote(
    id: kotlin.String,
    patientId: kotlin.String,
    encounterId: kotlin.String,
    dateStr: kotlin.String,
    notesText: kotlin.String
): DocumentReference {
    return DocumentReference.Builder(
        status = Enumeration(value = DocumentReferenceStatus.Current),
        content = mutableListOf(
            DocumentReference.Content.Builder(
                attachment = Attachment.Builder().apply {
                    contentType = Code.Builder().apply { value = "text/plain" }
                    // Using data URI for text content to avoid protobuf ByteString dependency in commonMain
                    url = Url.Builder().apply { value = "data:text/plain;charset=utf-8,$notesText" }
                }
            )
        )
    ).apply {
        this.id = id
        subject = Reference.Builder().apply { reference = String.Builder().apply { value = patientId } }
        context = DocumentReference.Context.Builder().apply {
            encounter.add(Reference.Builder().apply { reference = String.Builder().apply { value = encounterId } })
        }
        type = com.google.fhir.model.r4.CodeableConcept.Builder().apply {
            coding.add(
                Coding.Builder().apply {
                    system = Uri.Builder().apply { value = "http://loinc.org" }
                    code = Code.Builder().apply { value = "11488-4" }
                    display = String.Builder().apply { value = "Consultation note" }
                }
            )
        }
        try {
            date = Instant.Builder().apply { value = FhirDateTime.fromString(dateStr) }
        } catch (e: Exception) {}
    }.build()
}






fun createFhirDevice(
    id: kotlin.String,
    modelName: kotlin.String,
    manufacturerName: kotlin.String
): Device {
    return Device.Builder().apply {
        this.id = id
        deviceName.add(
            Device.DeviceName.Builder(
                name = String.Builder().apply { value = modelName },
                type = Enumeration(value = Device.DeviceNameType.Model_Name)
            )
        )
        manufacturer = String.Builder().apply { value = manufacturerName }
    }.build()
}

fun createFhirProvenance(
    id: kotlin.String,
    targetResourceId: kotlin.String,
    practitionerId: kotlin.String,
    dateStr: kotlin.String
): Provenance {
    return Provenance.Builder(
        target = mutableListOf(Reference.Builder().apply { reference = String.Builder().apply { value = targetResourceId } }),
        recorded = Instant.Builder().apply { value = FhirDateTime.fromString(dateStr) },
        agent = mutableListOf(
            Provenance.Agent.Builder(
                who = Reference.Builder().apply { reference = String.Builder().apply { value = practitionerId } }
            ).apply {
                type = com.google.fhir.model.r4.CodeableConcept.Builder().apply {
                    coding.add(
                        Coding.Builder().apply {
                            system = Uri.Builder().apply { value = "http://terminology.hl7.org/CodeSystem/provenance-participant-type" }
                            code = Code.Builder().apply { value = "author" }
                        }
                    )
                }
            }
        )
    ).apply {
        this.id = id
    }.build()
}

fun createFhirBinary(
    id: kotlin.String,
    contentTypeStr: kotlin.String,
    base64Data: kotlin.String
): Binary {
    return Binary.Builder(
        contentType = Code.Builder().apply { value = contentTypeStr }
    ).apply {
        this.id = id
        this.data = Base64Binary.Builder().apply { value = base64Data }
    }.build()
}
