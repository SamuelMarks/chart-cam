/**
 * @file FhirModels.kt
 * Contains declarations for FhirModels.kt.
 *
 * Contains models and utility extensions for interacting with FHIR resources.
 */
package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.Attachment
import dev.ohs.fhir.model.r4.Base64Binary
import dev.ohs.fhir.model.r4.Binary
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Device
import dev.ohs.fhir.model.r4.DocumentReference
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.Instant
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Period
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Provenance
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.Url
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus
import dev.ohs.fhir.model.r4.terminologies.IdentifierTypeCodes
import io.healthplatform.chartcam.terminology.TerminologyService
import io.healthplatform.chartcam.ui.currentLanguageState
import kotlinx.datetime.LocalDate
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Safely parses a string gender descriptor into a typed FHIR [Enumeration] of [AdministrativeGender].
 *
 * @param raw The raw gender string (e.g. "male", "female", "other", "unknown").
 * @return A [Result] enclosing the resolved [Enumeration] of [AdministrativeGender].
 */
fun parseAdministrativeGender(raw: String): Result<Enumeration<AdministrativeGender>> =
    runCatching {
        val gender = AdministrativeGender.fromCode(raw.trim().lowercase())
        Enumeration(value = gender)
    }

/**
 * Maps a string gender descriptor to a FHIR AdministrativeGender.
 *
 * @param gender The raw gender string.
 * @return The resolved AdministrativeGender.
 */
private fun mapAdministrativeGender(gender: String): AdministrativeGender =
    runCatching {
        when (gender.lowercase().trim()) {
            "male", "m" -> AdministrativeGender.Male
            "female", "f" -> AdministrativeGender.Female
            "other", "o" -> AdministrativeGender.Other
            "unknown", "u" -> AdministrativeGender.Unknown
            else -> AdministrativeGender.fromCode(gender.lowercase().trim())
        }
    }.getOrDefault(AdministrativeGender.Unknown)

/**
 * Resolves the typed [Encounter.EncounterStatus] wrapped in a [Result].
 */
val Encounter.typedStatus: Result<Encounter.EncounterStatus>
    get() {
        val s = status.value
        return if (s != null) {
            Result.success(s)
        } else {
            Result.failure(IllegalStateException("Encounter.status is missing or invalid"))
        }
    }

/**
 * Resolves the typed [DocumentReferenceStatus] wrapped in a [Result].
 */
val DocumentReference.typedStatus: Result<DocumentReferenceStatus>
    get() {
        val s = status.value
        return if (s != null) {
            Result.success(s)
        } else {
            Result.failure(IllegalStateException("DocumentReference.status is missing or invalid"))
        }
    }

/**
 * Builds a FHIR HumanName.
 *
 * @param firstName The first name.
 * @param lastName The last name.
 * @return The populated HumanName.
 */
private fun buildHumanName(
    firstName: String,
    lastName: String,
): HumanName =
    HumanName(
        family = FhirString(value = lastName),
        given = listOf(FhirString(value = firstName)),
    )

/**
 * Directly constructs an MRN [Identifier] for a non-blank MRN value.
 *
 * @param mrnValue The Medical Record Number value.
 * @return An [Identifier] configured for MRN.
 */
internal fun buildMrnIdentifierDirect(mrnValue: String): Identifier =
    Identifier(
        type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri(value = IdentifierTypeCodes.Mr.system),
                            code = Code(value = IdentifierTypeCodes.Mr.code),
                            display = FhirString(value = IdentifierTypeCodes.Mr.display),
                        ),
                    ),
            ),
        system = Uri(value = "urn:oid:1.2.36.146.595.217.0.1"),
        value = FhirString(value = mrnValue),
    )

/**
 * Builds an MRN [Identifier] element safely wrapped in a [Result].
 *
 * @param mrnValue The Medical Record Number value.
 * @return A [Result] enclosing the [Identifier] configured for MRN.
 */
fun buildMrnIdentifier(mrnValue: String): Result<Identifier> {
    if (mrnValue.isBlank()) {
        return Result.failure(IllegalArgumentException("MRN value cannot be blank"))
    }
    return Result.success(buildMrnIdentifierDirect(mrnValue))
}

/**
 * Safely creates a FHIR Patient resource wrapped in a [Result].
 *
 * @param id The unique identifier of the patient.
 * @param firstName The first name of the patient.
 * @param lastName The last name of the patient.
 * @param dob The date of birth of the patient.
 * @param mrnValue The Medical Record Number (MRN) of the patient.
 * @param organizationId An optional managing organization or practitioner ID for data scoping.
 * @param gender The administrative gender of the patient (e.g., male, female, other, unknown).
 * @return A [Result] enclosing the populated FHIR [Patient] object.
 */
fun createFhirPatientCatching(
    id: String,
    firstName: String,
    lastName: String,
    dob: LocalDate,
    mrnValue: String,
    organizationId: String? = null,
    gender: String = "unknown",
): Result<Patient> {
    if (id.isBlank() || mrnValue.isBlank()) {
        val msg = if (id.isBlank()) "Patient id cannot be blank" else "Patient mrnValue cannot be blank"
        return Result.failure(IllegalArgumentException(msg))
    }
    val managingOrg =
        organizationId?.let {
            Reference(reference = FhirString(value = it))
        }

    val mrnId = buildMrnIdentifierDirect(mrnValue)

    val patient =
        Patient(
            id = id,
            gender = Enumeration(value = mapAdministrativeGender(gender)),
            name = listOf(buildHumanName(firstName, lastName)),
            birthDate = Date(value = FhirDate.Date(dob)),
            identifier = listOf(mrnId),
            managingOrganization = managingOrg,
        )
    return Result.success(patient)
}

/**
 * Creates a FHIR Patient resource.
 *
 * @param id The unique identifier of the patient.
 * @param firstName The first name of the patient.
 * @param lastName The last name of the patient.
 * @param dob The date of birth of the patient.
 * @param mrnValue The Medical Record Number (MRN) of the patient.
 * @param organizationId An optional managing organization or practitioner ID for data scoping.
 * @param gender The administrative gender of the patient (e.g., male, female, other, unknown).
 * @return A populated FHIR [Patient] object.
 */
fun createFhirPatient(
    id: String,
    firstName: String,
    lastName: String,
    dob: LocalDate,
    mrnValue: String,
    organizationId: String? = null,
    gender: String = "unknown",
): Patient =
    createFhirPatientCatching(id, firstName, lastName, dob, mrnValue, organizationId, gender).getOrElse {
        val managingOrg =
            organizationId?.let {
                Reference(reference = FhirString(value = it))
            }
        Patient(
            id = id,
            gender = Enumeration(value = mapAdministrativeGender(gender)),
            name = listOf(buildHumanName(firstName, lastName)),
            birthDate = Date(value = FhirDate.Date(dob)),
            identifier =
                listOf(
                    Identifier(
                        system = Uri(value = "urn:oid:1.2.36.146.595.217.0.1"),
                        value = FhirString(value = mrnValue),
                    ),
                ),
            managingOrganization = managingOrg,
        )
    }

/**
 * Extension property to get the Medical Record Number (MRN) from a [Patient].
 */
val Patient.mrn: String
    get() {
        for (id in identifier) {
            val codings = id.type?.coding
            if (codings != null) {
                for (coding in codings) {
                    val code = coding.code?.value
                    if (code != null && code.equals(IdentifierTypeCodes.Mr.code, ignoreCase = true)) {
                        val v = id.value?.value
                        if (v != null) return v
                    }
                }
            }
        }
        for (id in identifier) {
            val v = id.value?.value
            if (v != null) return v
        }
        return ""
    }

/**
 * Extension property to get the birth date from a [Patient] as a string.
 */
val Patient.customBirthDate: String
    get() {
        val d = birthDate?.value ?: return ""
        return d.toString()
    }

/**
 * Extension property to retrieve the typed [FhirDate] representing the patient's birth date.
 */
val Patient.fhirBirthDate: FhirDate?
    get() = birthDate?.value

/**
 * Formats the patient's birth date using the user's localized date format.
 *
 * @param language The BCP-47 language tag to use for formatting.
 * @return A [Result] enclosing the localized birth date string, or null if no birth date is set.
 */
fun Patient.formatLocalizedBirthDate(
    language: String = io.healthplatform.chartcam.ui.currentLanguageState.value,
): Result<String?> =
    runCatching {
        birthDate?.value?.let {
            io.healthplatform.chartcam.utils
                .formatLocalizedDate(it.toString(), language)
        }
    }

/**
 * Extension property to get the family name from a [HumanName].
 */
val HumanName.familyName: String
    get() = family?.value ?: "Unknown"

/**
 * Extension property to get the given name from a [HumanName].
 */
val HumanName.givenName: String
    get() = given.firstOrNull()?.value ?: "Unknown"

/**
 * Resolves the localized fallback string for an unknown person name.
 *
 * @param language The IETF BCP-47 language tag or ISO-639 code.
 * @return The localized unknown string placeholder.
 */
fun getLocalizedUnknownName(language: String = currentLanguageState.value): String {
    val lang = language.lowercase().split("-", "_").first()
    return when (lang) {
        "es" -> "Desconocido"
        "ja" -> "不明"
        "he", "iw" -> "לא ידוע"
        "zh" -> "未知"
        else -> "Unknown"
    }
}

/**
 * Formats a person's family and given names according to cultural conventions of the specified locale.
 *
 * @param familyName The family name or surname.
 * @param givenName The given name or first name.
 * @param language The language tag to determine naming order.
 * @return The culturally formatted full name.
 */
fun formatPersonName(
    familyName: String?,
    givenName: String?,
    language: String = currentLanguageState.value,
): String {
    val fallback = getLocalizedUnknownName(language)
    val family = familyName?.takeIf { it.isNotBlank() }
    val given = givenName?.takeIf { it.isNotBlank() }

    val lang = language.lowercase().split("-", "_").first()
    val isEastAsian = lang == "zh" || lang == "ja"

    return when {
        family != null && given != null -> if (isEastAsian) "$family$given" else "$family, $given"
        family != null -> family
        given != null -> given
        else -> fallback
    }
}

/**
 * Extension function to get the culturally formatted full name from a [Patient] for a specific language.
 *
 * @param language The language tag to use for formatting.
 * @return The formatted full name.
 */
fun Patient.getFullName(language: String = currentLanguageState.value): String {
    val n = name.firstOrNull() ?: return getLocalizedUnknownName(language)
    val fam = n.family?.value
    val giv = n.given.firstOrNull()?.value
    return formatPersonName(fam, giv, language)
}

/**
 * Extension property to get the full formatted name from a [Patient] using the active application language.
 */
val Patient.fullName: String
    get() = getFullName()

/**
 * Extension function to get the culturally formatted full name from a [Practitioner] for a specific language.
 *
 * @param language The language tag to use for formatting.
 * @return The formatted full name.
 */
fun Practitioner.getFullName(language: String = currentLanguageState.value): String {
    val n = name.firstOrNull() ?: return getLocalizedUnknownName(language)
    val fam = n.family?.value
    val giv = n.given.firstOrNull()?.value
    return formatPersonName(fam, giv, language)
}

/**
 * Extension property to get the full formatted name from a [Practitioner] using the active application language.
 */
val Practitioner.fullName: String
    get() = getFullName()

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
    id: String,
    lastName: String,
    firstName: String,
    isActive: Boolean,
): Practitioner =
    Practitioner(
        id = id,
        active = FhirBoolean(value = isActive),
        name =
            listOf(
                HumanName(
                    family = FhirString(value = lastName),
                    given = listOf(FhirString(value = firstName)),
                ),
            ),
    )

/**
 * Creates a FHIR Encounter resource.
 *
 * @param id The unique identifier of the encounter.
 * @param patientId The reference ID of the associated patient.
 * @param practitionerId The reference ID of the associated practitioner.
 * @param dateStr The date and time of the encounter as a string.
 * @return A populated FHIR [Encounter] object.
 */
fun createFhirEncounter(
    id: String? = null,
    patientId: String,
    practitionerId: String,
    dateStr: String,
): Encounter =
    Encounter(
        id = id,
        status = Enumeration(value = Encounter.EncounterStatus.In_Progress),
        `class` =
            Coding(
                system = Uri(value = "http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                code = Code(value = "AMB"),
            ),
        subject = Reference(reference = FhirString(value = patientId)),
        participant =
            listOf(
                Encounter.Participant(
                    individual = Reference(reference = FhirString(value = practitionerId)),
                ),
            ),
        period =
            Period(
                start = DateTime(value = FhirDateTime.fromString(dateStr)),
            ),
    )

/**
 * Extension property to get the encounter start date from an [Encounter] as a string.
 */
val Encounter.encounterDate: String
    get() {
        val s = period?.start?.value ?: return ""
        return s.toString()
    }

/**
 * Parameters for creating a FHIR DocumentReference.
 *
 * @property id The unique identifier.
 * @property patientId The patient ID.
 * @property encounterId The encounter ID.
 * @property dateStr The date string.
 * @property desc The description.
 * @property mime The MIME type.
 * @property urlPath The URL path.
 * @property answerCode The optional answer code.
 */
data class DocumentReferenceCreationParams(
    val id: String,
    val patientId: String,
    val encounterId: String,
    val dateStr: String,
    val desc: String?,
    val mime: String,
    val urlPath: String,
    val answerCode: String? = null,
)

/**
 * Creates a FHIR DocumentReference using immutable data class constructors.
 *
 * @param params The creation parameters.
 * @return A populated FHIR [DocumentReference].
 */
fun createFhirDocumentReference(params: DocumentReferenceCreationParams): DocumentReference {
    val parsedDate = runCatching { Instant(value = FhirDateTime.fromString(params.dateStr)) }.getOrNull()
    return DocumentReference(
        id = params.id,
        status = Enumeration(value = DocumentReferenceStatus.Current),
        content =
            listOf(
                DocumentReference.Content(
                    attachment =
                        Attachment(
                            contentType = Code(value = params.mime),
                            url = Url(value = params.urlPath),
                        ),
                ),
            ),
        subject = Reference(reference = FhirString(value = params.patientId)),
        context =
            DocumentReference.Context(
                encounter = listOf(Reference(reference = FhirString(value = params.encounterId))),
                related =
                    if (params.answerCode != null) {
                        listOf(Reference(identifier = Identifier(value = FhirString(value = params.answerCode))))
                    } else {
                        emptyList()
                    },
            ),
        date = parsedDate,
        description = if (!params.desc.isNullOrBlank()) FhirString(value = params.desc) else null,
    )
}

/**
 * Creates a FHIR DocumentReference resource specifically for a clinical note using immutable data classes.
 *
 * @param id The unique identifier of the document reference.
 * @param patientId The reference ID of the associated patient.
 * @param encounterId The reference ID of the associated encounter.
 * @param dateStr The date and time of the document creation as a string.
 * @param notesText The textual content of the clinical note.
 * @return A populated FHIR [DocumentReference] object representing a clinical note.
 */
fun createFhirClinicalNote(
    id: String,
    patientId: String,
    encounterId: String,
    dateStr: String,
    notesText: String,
): DocumentReference {
    val parsedDate = runCatching { Instant(value = FhirDateTime.fromString(dateStr)) }.getOrNull()
    return DocumentReference(
        id = id,
        status = Enumeration(value = DocumentReferenceStatus.Current),
        type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri(value = TerminologyService.LOINC_URI),
                            code = Code(value = "11488-4"),
                            display = FhirString(value = "Consultation note"),
                        ),
                    ),
            ),
        content =
            listOf(
                DocumentReference.Content(
                    attachment =
                        Attachment(
                            contentType = Code(value = "text/plain"),
                            url = Url(value = "data:text/plain;charset=utf-8,$notesText"),
                        ),
                ),
            ),
        subject = Reference(reference = FhirString(value = patientId)),
        context =
            DocumentReference.Context(
                encounter = listOf(Reference(reference = FhirString(value = encounterId))),
            ),
        date = parsedDate,
    )
}

/**
 * Creates a FHIR Device resource.
 *
 * @param id The unique identifier of the device.
 * @param modelName The model name of the device.
 * @param manufacturerName The name of the device manufacturer.
 * @return A populated FHIR [Device] object.
 */
fun createFhirDevice(
    id: String,
    modelName: String,
    manufacturerName: String,
): Device =
    Device(
        id = id,
        deviceName =
            listOf(
                Device.DeviceName(
                    name = FhirString(value = modelName),
                    type = Enumeration(value = Device.DeviceNameType.Model_Name),
                ),
            ),
        manufacturer = FhirString(value = manufacturerName),
    )

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
    id: String,
    targetResourceId: String,
    practitionerId: String,
    dateStr: String,
): Provenance =
    Provenance(
        id = id,
        target = listOf(Reference(reference = FhirString(value = targetResourceId))),
        recorded = Instant(value = FhirDateTime.fromString(dateStr)),
        agent =
            listOf(
                Provenance.Agent(
                    who = Reference(reference = FhirString(value = practitionerId)),
                    type =
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system =
                                            Uri(
                                                value =
                                                    "http://terminology.hl7.org/CodeSystem/" +
                                                        "provenance-participant-type",
                                            ),
                                        code = Code(value = "author"),
                                    ),
                                ),
                        ),
                ),
            ),
    )

/**
 * Creates a FHIR Binary resource to encapsulate raw binary data.
 *
 * @param id The unique identifier of the binary resource.
 * @param contentTypeStr The MIME type of the binary content.
 * @param base64Data The binary data encoded as a base64 string.
 * @return A populated FHIR [Binary] object.
 */
fun createFhirBinary(
    id: String,
    contentTypeStr: String,
    base64Data: String,
): Binary =
    Binary(
        id = id,
        contentType = Code(value = contentTypeStr),
        data = Base64Binary(value = base64Data),
    )
