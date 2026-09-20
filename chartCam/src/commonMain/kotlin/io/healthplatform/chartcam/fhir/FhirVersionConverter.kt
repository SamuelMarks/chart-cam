/**
 * @file FhirVersionConverter.kt
 * Pure, deterministic in-memory converter between FHIR R4 and FHIR R5 models for offline exports and imports.
 */
package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Canonical as R4Canonical
import dev.ohs.fhir.model.r4.Code as R4Code
import dev.ohs.fhir.model.r4.Date as R4Date
import dev.ohs.fhir.model.r4.DateTime as R4DateTime
import dev.ohs.fhir.model.r4.Decimal as R4Decimal
import dev.ohs.fhir.model.r4.Encounter as R4Encounter
import dev.ohs.fhir.model.r4.Enumeration as R4Enumeration
import dev.ohs.fhir.model.r4.FhirDate as R4FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime as R4FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal as R4FhirDecimal
import dev.ohs.fhir.model.r4.HumanName as R4HumanName
import dev.ohs.fhir.model.r4.Identifier as R4Identifier
import dev.ohs.fhir.model.r4.Integer as R4Integer
import dev.ohs.fhir.model.r4.Observation as R4Observation
import dev.ohs.fhir.model.r4.Patient as R4Patient
import dev.ohs.fhir.model.r4.Period as R4Period
import dev.ohs.fhir.model.r4.Quantity as R4Quantity
import dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus as R4QrStatus
import dev.ohs.fhir.model.r4.Reference as R4Reference
import dev.ohs.fhir.model.r4.String as R4String
import dev.ohs.fhir.model.r4.Uri as R4Uri
import dev.ohs.fhir.model.r4.Url as R4Url
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender as R4Gender
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus as R4PubStatus
import dev.ohs.fhir.model.r4b.terminologies.AdministrativeGender as R4BGender
import dev.ohs.fhir.model.r5.Canonical as R5Canonical
import dev.ohs.fhir.model.r5.Code as R5Code
import dev.ohs.fhir.model.r5.CodeableConcept as R5CodeableConcept
import dev.ohs.fhir.model.r5.Coding as R5Coding
import dev.ohs.fhir.model.r5.Date as R5Date
import dev.ohs.fhir.model.r5.DateTime as R5DateTime
import dev.ohs.fhir.model.r5.Decimal as R5Decimal
import dev.ohs.fhir.model.r5.Encounter as R5Encounter
import dev.ohs.fhir.model.r5.Enumeration as R5Enumeration
import dev.ohs.fhir.model.r5.FhirDate as R5FhirDate
import dev.ohs.fhir.model.r5.FhirDateTime as R5FhirDateTime
import dev.ohs.fhir.model.r5.FhirDecimal as R5FhirDecimal
import dev.ohs.fhir.model.r5.HumanName as R5HumanName
import dev.ohs.fhir.model.r5.Identifier as R5Identifier
import dev.ohs.fhir.model.r5.Integer as R5Integer
import dev.ohs.fhir.model.r5.Observation as R5Observation
import dev.ohs.fhir.model.r5.Patient as R5Patient
import dev.ohs.fhir.model.r5.Period as R5Period
import dev.ohs.fhir.model.r5.Quantity as R5Quantity
import dev.ohs.fhir.model.r5.QuestionnaireResponse.QuestionnaireResponseStatus as R5QrStatus
import dev.ohs.fhir.model.r5.Reference as R5Reference
import dev.ohs.fhir.model.r5.String as R5String
import dev.ohs.fhir.model.r5.Uri as R5Uri
import dev.ohs.fhir.model.r5.Url as R5Url
import dev.ohs.fhir.model.r5.terminologies.AdministrativeGender as R5Gender
import dev.ohs.fhir.model.r5.terminologies.PublicationStatus as R5PubStatus

/**
 * Offline converter transforming core clinical resources between FHIR R4 and FHIR R5 schemas.
 */
@Suppress("LargeClass")
object FhirVersionConverter {
    /**
     * Maps an R4 String to an R5 String.
     *
     * @param s The R4 String.
     * @return The R5 String, or null.
     */
    private fun mapStringR4ToR5(s: R4String?): R5String? {
        val v = s?.value ?: return null
        return R5String(value = v)
    }

    /**
     * Maps an R5 String to an R4 String.
     *
     * @param s The R5 String.
     * @return The R4 String, or null.
     */
    private fun mapStringR5ToR4(s: R5String?): R4String? {
        val v = s?.value ?: return null
        return R4String(value = v)
    }

    /**
     * Maps an R4 Uri to an R5 Uri.
     *
     * @param u The R4 Uri.
     * @return The R5 Uri, or null.
     */
    private fun mapUriR4ToR5(u: R4Uri?): R5Uri? {
        val v = u?.value ?: return null
        return R5Uri(value = v)
    }

    /**
     * Maps an R5 Uri to an R4 Uri.
     *
     * @param u The R5 Uri.
     * @return The R4 Uri, or null.
     */
    private fun mapUriR5ToR4(u: R5Uri?): R4Uri? {
        val v = u?.value ?: return null
        return R4Uri(value = v)
    }

    /**
     * Maps an R4 Code to an R5 Code.
     *
     * @param c The R4 Code.
     * @return The R5 Code, or null.
     */
    private fun mapCodeR4ToR5(c: R4Code?): R5Code? {
        val v = c?.value ?: return null
        return R5Code(value = v)
    }

    /**
     * Maps an R5 Code to an R4 Code.
     *
     * @param c The R5 Code.
     * @return The R4 Code, or null.
     */
    private fun mapCodeR5ToR4(c: R5Code?): R4Code? {
        val v = c?.value ?: return null
        return R4Code(value = v)
    }

    /**
     * Maps an R4 Boolean to an R5 Boolean.
     *
     * @param b The R4 Boolean.
     * @return The R5 Boolean, or null.
     */
    private fun mapBooleanR4ToR5(b: dev.ohs.fhir.model.r4.Boolean?): dev.ohs.fhir.model.r5.Boolean? {
        val v = b?.value ?: return null
        return dev.ohs.fhir.model.r5
            .Boolean(value = v)
    }

    /**
     * Maps an R5 Boolean to an R4 Boolean.
     *
     * @param b The R5 Boolean.
     * @return The R4 Boolean, or null.
     */
    private fun mapBooleanR5ToR4(b: dev.ohs.fhir.model.r5.Boolean?): dev.ohs.fhir.model.r4.Boolean? {
        val v = b?.value ?: return null
        return dev.ohs.fhir.model.r4
            .Boolean(value = v)
    }

    /**
     * Maps an R4 Reference to an R5 Reference.
     *
     * @param ref The R4 Reference.
     * @return The R5 Reference, or null.
     */
    private fun mapReferenceR4ToR5(ref: R4Reference?): R5Reference? {
        val v = ref?.reference?.value ?: return null
        return R5Reference(reference = R5String(value = v))
    }

    /**
     * Maps an R5 Reference to an R4 Reference.
     *
     * @param ref The R5 Reference.
     * @return The R4 Reference, or null.
     */
    private fun mapReferenceR5ToR4(ref: R5Reference?): R4Reference? {
        val v = ref?.reference?.value ?: return null
        return R4Reference(reference = R4String(value = v))
    }

    /**
     * Maps an R4 Url to an R5 Url.
     *
     * @param u The R4 Url.
     * @return The R5 Url, or null.
     */
    private fun mapUrlR4ToR5(u: R4Url?): R5Url? {
        val v = u?.value ?: return null
        return R5Url(value = v)
    }

    /**
     * Maps an R5 Url to an R4 Url.
     *
     * @param u The R5 Url.
     * @return The R4 Url, or null.
     */
    private fun mapUrlR5ToR4(u: R5Url?): R4Url? {
        val v = u?.value ?: return null
        return R4Url(value = v)
    }

    /**
     * Maps R4 AdministrativeGender to R5 AdministrativeGender.
     *
     * @param g The R4 gender.
     * @return The R5 gender enumeration, or null.
     */
    private fun mapGenderR4ToR5(g: R4Gender?): R5Enumeration<R5Gender>? =
        if (g == R4Gender.Male) {
            R5Enumeration(value = R5Gender.Male)
        } else if (g == R4Gender.Female) {
            R5Enumeration(value = R5Gender.Female)
        } else if (g == R4Gender.Other) {
            R5Enumeration(value = R5Gender.Other)
        } else if (g == R4Gender.Unknown) {
            R5Enumeration(value = R5Gender.Unknown)
        } else {
            null
        }

    /**
     * Maps R5 AdministrativeGender to R4 AdministrativeGender.
     *
     * @param g The R5 gender.
     * @return The R4 gender enumeration, or null.
     */
    private fun mapGenderR5ToR4(g: R5Gender?): R4Enumeration<R4Gender>? =
        if (g == R5Gender.Male) {
            R4Enumeration(value = R4Gender.Male)
        } else if (g == R5Gender.Female) {
            R4Enumeration(value = R4Gender.Female)
        } else if (g == R5Gender.Other) {
            R4Enumeration(value = R4Gender.Other)
        } else if (g == R5Gender.Unknown) {
            R4Enumeration(value = R4Gender.Unknown)
        } else {
            null
        }

    /**
     * Converts a FHIR R4 [R4Patient] into a FHIR R5 [R5Patient].
     *
     * @param r4 The R4 Patient resource.
     * @return A [Result] enclosing the converted R5 Patient resource.
     */
    fun convertPatientR4ToR5(r4: R4Patient): Result<R5Patient> =
        runCatching {
            val genderR5 = mapGenderR4ToR5(r4.gender?.value)

            val namesR5 =
                r4.name.map { n ->
                    val fam = n.family?.value
                    val givens = n.given.mapNotNull { it.value }.map { R5String(value = it) }
                    R5HumanName(
                        family = if (fam != null) R5String(value = fam) else null,
                        given = givens,
                    )
                }

            val identifiersR5 =
                r4.identifier.map { id ->
                    R5Identifier(
                        system = mapUriR4ToR5(id.system),
                        value = mapStringR4ToR5(id.value),
                    )
                }

            val bd = r4.birthDate?.value
            val birthDateR5 =
                when (bd) {
                    is R4FhirDate.Date -> R5Date(value = R5FhirDate.Date(bd.date))
                    is R4FhirDate.Year -> R5Date(value = R5FhirDate.Year(bd.value))
                    is R4FhirDate.YearMonth -> R5Date(value = R5FhirDate.YearMonth(bd.value))
                    null -> null
                }

            R5Patient(
                id = r4.id,
                gender = genderR5,
                name = namesR5,
                active = mapBooleanR4ToR5(r4.active),
                birthDate = birthDateR5,
                identifier = identifiersR5,
                managingOrganization = mapReferenceR4ToR5(r4.managingOrganization),
            )
        }

    /**
     * Converts a FHIR R5 [R5Patient] into a FHIR R4 [R4Patient].
     *
     * @param r5 The R5 Patient resource.
     * @return A [Result] enclosing the converted R4 Patient resource.
     */
    fun convertPatientR5ToR4(r5: R5Patient): Result<R4Patient> =
        runCatching {
            val genderR4 = mapGenderR5ToR4(r5.gender?.value)

            val namesR4 =
                r5.name.map { n ->
                    val fam = n.family?.value
                    val givens = n.given.mapNotNull { it.value }.map { R4String(value = it) }
                    R4HumanName(
                        family = if (fam != null) R4String(value = fam) else null,
                        given = givens,
                    )
                }

            val identifiersR4 =
                r5.identifier.map { id ->
                    R4Identifier(
                        system = mapUriR5ToR4(id.system),
                        value = mapStringR5ToR4(id.value),
                    )
                }

            val bd = r5.birthDate?.value
            val birthDateR4 =
                when (bd) {
                    is R5FhirDate.Date -> R4Date(value = R4FhirDate.Date(bd.date))
                    is R5FhirDate.Year -> R4Date(value = R4FhirDate.Year(bd.value))
                    is R5FhirDate.YearMonth -> R4Date(value = R4FhirDate.YearMonth(bd.value))
                    null -> null
                }

            R4Patient(
                id = r5.id,
                gender = genderR4,
                name = namesR4,
                active = mapBooleanR5ToR4(r5.active),
                birthDate = birthDateR4,
                identifier = identifiersR4,
                managingOrganization = mapReferenceR5ToR4(r5.managingOrganization),
            )
        }

    /**
     * Converts a FHIR R4 [R4Encounter] into a FHIR R5 [R5Encounter].
     *
     * @param r4 The R4 Encounter resource.
     * @return A [Result] enclosing the converted R5 Encounter resource.
     */
    fun convertEncounterR4ToR5(r4: R4Encounter): Result<R5Encounter> =
        runCatching {
            val s = r4.status.value
            val statusR5 =
                if (s == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Planned) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Planned)
                } else if (s == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Completed)
                } else if (s == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Cancelled) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Cancelled)
                } else if (s == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Entered_In_Error) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Entered_In_Error)
                } else if (s == dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Unknown) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Unknown)
                } else {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Encounter.EncounterStatus.In_Progress)
                }

            val p = r4.period
            val periodR5 =
                if (p != null) {
                    val st = p.start?.value
                    val en = p.end?.value
                    R5Period(
                        start = if (st != null) R5DateTime(value = R5FhirDateTime.fromString(st.toString())) else null,
                        end = if (en != null) R5DateTime(value = R5FhirDateTime.fromString(en.toString())) else null,
                    )
                } else {
                    null
                }

            R5Encounter(
                id = r4.id,
                status = statusR5,
                `class` = convertEncounterClassR4ToR5(r4.`class`),
                subject = mapReferenceR4ToR5(r4.subject),
                actualPeriod = periodR5,
                serviceProvider = mapReferenceR4ToR5(r4.serviceProvider),
            )
        }

    /**
     * Converts R4 encounter coding into R5 class concept list.
     *
     * @param coding The R4 coding.
     * @return The list of R5 CodeableConcepts.
     */
    private fun convertEncounterClassR4ToR5(coding: dev.ohs.fhir.model.r4.Coding): List<R5CodeableConcept> =
        listOf(
            R5CodeableConcept(
                coding =
                    listOf(
                        R5Coding(
                            system = mapUriR4ToR5(coding.system),
                            code = mapCodeR4ToR5(coding.code),
                            display = mapStringR4ToR5(coding.display),
                        ),
                    ),
            ),
        )

    /**
     * Converts a FHIR R5 [R5Encounter] into a FHIR R4 [R4Encounter].
     *
     * @param r5 The R5 Encounter resource.
     * @return A [Result] enclosing the converted R4 Encounter resource.
     */
    fun convertEncounterR5ToR4(r5: R5Encounter): Result<R4Encounter> =
        runCatching {
            val s = r5.status.value
            val statusR4 =
                if (s == dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Planned) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Planned)
                } else if (s == dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Completed) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished)
                } else if (s == dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Cancelled) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Cancelled)
                } else if (s == dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Entered_In_Error) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Entered_In_Error)
                } else if (s == dev.ohs.fhir.model.r5.Encounter.EncounterStatus.Unknown) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Unknown)
                } else {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress)
                }

            val p = r5.actualPeriod
            val periodR4 =
                if (p != null) {
                    val st = p.start?.value
                    val en = p.end?.value
                    R4Period(
                        start = if (st != null) R4DateTime(value = R4FhirDateTime.fromString(st.toString())) else null,
                        end = if (en != null) R4DateTime(value = R4FhirDateTime.fromString(en.toString())) else null,
                    )
                } else {
                    null
                }

            R4Encounter(
                id = r5.id,
                status = statusR4,
                `class` = convertEncounterClassR5ToR4(r5.`class`),
                subject = mapReferenceR5ToR4(r5.subject),
                period = periodR4,
                serviceProvider = mapReferenceR5ToR4(r5.serviceProvider),
            )
        }

    /**
     * Converts R5 encounter class concept list into an R4 encounter Coding.
     *
     * @param classes The list of R5 concepts.
     * @return The converted R4 Coding.
     */
    private fun convertEncounterClassR5ToR4(classes: List<R5CodeableConcept>): dev.ohs.fhir.model.r4.Coding {
        val defaultSystem = "http://terminology.hl7.org/CodeSystem/v3-ActCode"
        val defaultCode = "AMB"
        val c = if (classes.isNotEmpty()) classes[0].coding.firstOrNull() else null
        if (c == null) {
            return dev.ohs.fhir.model.r4.Coding(
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = defaultSystem),
                code =
                    dev.ohs.fhir.model.r4
                        .Code(value = defaultCode),
            )
        }
        val sysVal = c.system?.value
        val codVal = c.code?.value
        val sys = if (!sysVal.isNullOrBlank()) sysVal else defaultSystem
        val cod = if (!codVal.isNullOrBlank()) codVal else defaultCode
        return dev.ohs.fhir.model.r4.Coding(
            system =
                dev.ohs.fhir.model.r4
                    .Uri(value = sys),
            code =
                dev.ohs.fhir.model.r4
                    .Code(value = cod),
            display = mapStringR5ToR4(c.display),
        )
    }

    /**
     * Converts a FHIR R4 [R4Observation] into a FHIR R5 [R5Observation].
     *
     * @param r4 The R4 Observation resource.
     * @return A [Result] enclosing the converted R5 Observation resource.
     */
    fun convertObservationR4ToR5(r4: R4Observation): Result<R5Observation> =
        runCatching {
            val s = r4.status.value
            val statusR5 =
                if (s == dev.ohs.fhir.model.r4.Observation.ObservationStatus.Registered) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Registered)
                } else if (s == dev.ohs.fhir.model.r4.Observation.ObservationStatus.Preliminary) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Preliminary)
                } else if (s == dev.ohs.fhir.model.r4.Observation.ObservationStatus.Amended) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Amended)
                } else if (s == dev.ohs.fhir.model.r4.Observation.ObservationStatus.Corrected) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Corrected)
                } else if (s == dev.ohs.fhir.model.r4.Observation.ObservationStatus.Cancelled) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Cancelled)
                } else if (s == dev.ohs.fhir.model.r4.Observation.ObservationStatus.Entered_In_Error) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Entered_In_Error)
                } else if (s == dev.ohs.fhir.model.r4.Observation.ObservationStatus.Unknown) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Unknown)
                } else {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Observation.ObservationStatus.Final)
                }

            val codeR5 =
                R5CodeableConcept(
                    coding =
                        r4.code.coding.map { c ->
                            R5Coding(
                                system = mapUriR4ToR5(c.system),
                                code = mapCodeR4ToR5(c.code),
                                display = mapStringR4ToR5(c.display),
                            )
                        },
                    text = mapStringR4ToR5(r4.code.text),
                )

            val valueR5 = mapR4ObservationValue(r4.value)

            R5Observation(
                id = r4.id,
                status = statusR5,
                code = codeR5,
                subject = mapReferenceR4ToR5(r4.subject),
                encounter = mapReferenceR4ToR5(r4.encounter),
                value = valueR5,
            )
        }

    /**
     * Converts a FHIR R5 [R5Observation] into a FHIR R4 [R4Observation].
     *
     * @param r5 The R5 Observation resource.
     * @return A [Result] enclosing the converted R4 Observation resource.
     */
    fun convertObservationR5ToR4(r5: R5Observation): Result<R4Observation> =
        runCatching {
            val s = r5.status.value
            val statusR4 =
                if (s == dev.ohs.fhir.model.r5.Observation.ObservationStatus.Registered) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Registered)
                } else if (s == dev.ohs.fhir.model.r5.Observation.ObservationStatus.Preliminary) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Preliminary)
                } else if (s == dev.ohs.fhir.model.r5.Observation.ObservationStatus.Amended) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Amended)
                } else if (s == dev.ohs.fhir.model.r5.Observation.ObservationStatus.Corrected) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Corrected)
                } else if (s == dev.ohs.fhir.model.r5.Observation.ObservationStatus.Cancelled) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Cancelled)
                } else if (s == dev.ohs.fhir.model.r5.Observation.ObservationStatus.Entered_In_Error) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Entered_In_Error)
                } else if (s == dev.ohs.fhir.model.r5.Observation.ObservationStatus.Unknown) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Unknown)
                } else {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final)
                }

            val codeR4 =
                dev.ohs.fhir.model.r4.CodeableConcept(
                    coding =
                        r5.code.coding.map { c ->
                            dev.ohs.fhir.model.r4.Coding(
                                system = mapUriR5ToR4(c.system),
                                code = mapCodeR5ToR4(c.code),
                                display = mapStringR5ToR4(c.display),
                            )
                        },
                    text = mapStringR5ToR4(r5.code.text),
                )

            val valueR4 = mapR5ObservationValue(r5.value)

            R4Observation(
                id = r5.id,
                status = statusR4,
                code = codeR4,
                subject = mapReferenceR5ToR4(r5.subject),
                encounter = mapReferenceR5ToR4(r5.encounter),
                value = valueR4,
            )
        }

    /**
     * Maps R4 Observation.Value to R5 Observation.Value.
     *
     * @param v The R4 Observation.Value.
     * @return The converted R5 Observation.Value, or null.
     */
    private fun mapR4ObservationValue(v: R4Observation.Value?): R5Observation.Value? =
        when (v) {
            is R4Observation.Value.Quantity -> {
                val dec = v.value.value?.value
                if (dec != null) {
                    R5Observation.Value.Quantity(
                        R5Quantity(
                            value = R5Decimal(value = R5FhirDecimal.fromString(dec.toString())),
                            unit = mapStringR4ToR5(v.value.unit),
                        ),
                    )
                } else {
                    null
                }
            }
            is R4Observation.Value.String -> {
                val str = v.value.value
                if (str != null) R5Observation.Value.String(R5String(value = str)) else null
            }
            is R4Observation.Value.Integer -> {
                val i = v.value.value
                if (i != null) R5Observation.Value.Integer(R5Integer(value = i)) else null
            }
            else -> null
        }

    /**
     * Maps R5 Observation.Value to R4 Observation.Value.
     *
     * @param v The R5 Observation.Value.
     * @return The converted R4 Observation.Value, or null.
     */
    private fun mapR5ObservationValue(v: R5Observation.Value?): R4Observation.Value? =
        when (v) {
            is R5Observation.Value.Quantity -> {
                val dec = v.value.value?.value
                if (dec != null) {
                    R4Observation.Value.Quantity(
                        R4Quantity(
                            value = R4Decimal(value = R4FhirDecimal.fromString(dec.toString())),
                            unit = mapStringR5ToR4(v.value.unit),
                        ),
                    )
                } else {
                    null
                }
            }
            is R5Observation.Value.String -> {
                val str = v.value.value
                if (str != null) R4Observation.Value.String(R4String(value = str)) else null
            }
            is R5Observation.Value.Integer -> {
                val i = v.value.value
                if (i != null) R4Observation.Value.Integer(R4Integer(value = i)) else null
            }
            else -> null
        }

    /**
     * Converts a FHIR R4 Questionnaire into a FHIR R5 Questionnaire.
     *
     * @param r4 The R4 Questionnaire resource.
     * @return A [Result] enclosing the converted R5 Questionnaire.
     */
    fun convertQuestionnaireR4ToR5(
        r4: dev.ohs.fhir.model.r4.Questionnaire,
    ): Result<dev.ohs.fhir.model.r5.Questionnaire> =
        runCatching {
            val s = r4.status.value
            val statusR5 =
                if (s == R4PubStatus.Draft) {
                    R5Enumeration(value = R5PubStatus.Draft)
                } else if (s == R4PubStatus.Retired) {
                    R5Enumeration(value = R5PubStatus.Retired)
                } else if (s == R4PubStatus.Unknown) {
                    R5Enumeration(value = R5PubStatus.Unknown)
                } else {
                    R5Enumeration(value = R5PubStatus.Active)
                }

            val itemsR5 = r4.item.map { convertQuestionnaireItemR4ToR5(it).getOrThrow() }

            dev.ohs.fhir.model.r5.Questionnaire(
                id = r4.id,
                url = mapUriR4ToR5(r4.url),
                title = mapStringR4ToR5(r4.title),
                status = statusR5,
                item = itemsR5,
            )
        }

    /**
     * Recursively converts an R4 Questionnaire item into an R5 Questionnaire item.
     *
     * @param item The source R4 item.
     * @return A [Result] enclosing the converted R5 item.
     */
    fun convertQuestionnaireItemR4ToR5(
        item: dev.ohs.fhir.model.r4.Questionnaire.Item,
    ): Result<dev.ohs.fhir.model.r5.Questionnaire.Item> =
        runCatching {
            val t = item.type.value
            val typeR5 =
                if (t != null) {
                    runCatching {
                        dev.ohs.fhir.model.r5.Questionnaire.QuestionnaireItemType
                            .fromCode(t.code)
                    }.getOrDefault(dev.ohs.fhir.model.r5.Questionnaire.QuestionnaireItemType.String)
                } else {
                    dev.ohs.fhir.model.r5.Questionnaire.QuestionnaireItemType.String
                }

            val subItemsR5 = item.item.map { convertQuestionnaireItemR4ToR5(it).getOrThrow() }
            val optionsR5 = convertAnswerOptionsR4ToR5(item.answerOption)

            dev.ohs.fhir.model.r5.Questionnaire.Item(
                linkId = mapStringR4ToR5(item.linkId) ?: R5String(value = ""),
                text = mapStringR4ToR5(item.text),
                type =
                    dev.ohs.fhir.model.r5
                        .Enumeration(value = typeR5),
                required = mapBooleanR4ToR5(item.required),
                repeats = mapBooleanR4ToR5(item.repeats),
                answerOption = optionsR5,
                item = subItemsR5,
            )
        }

    /**
     * Converts R4 answer options into R5 answer options.
     *
     * @param options The source R4 answer options.
     * @return Converted R5 answer options.
     */
    private fun convertAnswerOptionsR4ToR5(
        options: List<dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption>,
    ): List<dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption> =
        options.mapNotNull { opt ->
            when (val v = opt.value) {
                is dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value.String -> {
                    val str = v.value.value
                    if (str != null) {
                        dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value
                                    .String(R5String(value = str)),
                        )
                    } else {
                        null
                    }
                }
                is dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value.Coding -> {
                    dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption(
                        value =
                            dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.Coding(
                                R5Coding(
                                    system = mapUriR4ToR5(v.value.system),
                                    code = mapCodeR4ToR5(v.value.code),
                                    display = mapStringR4ToR5(v.value.display),
                                ),
                            ),
                    )
                }
                else -> null
            }
        }

    /**
     * Converts a FHIR R5 Questionnaire into a FHIR R4 Questionnaire.
     *
     * @param r5 The R5 Questionnaire resource.
     * @return A [Result] enclosing the converted R4 Questionnaire.
     */
    fun convertQuestionnaireR5ToR4(
        r5: dev.ohs.fhir.model.r5.Questionnaire,
    ): Result<dev.ohs.fhir.model.r4.Questionnaire> =
        runCatching {
            val s = r5.status.value
            val statusR4 =
                if (s == R5PubStatus.Draft) {
                    R4Enumeration(value = R4PubStatus.Draft)
                } else if (s == R5PubStatus.Retired) {
                    R4Enumeration(value = R4PubStatus.Retired)
                } else if (s == R5PubStatus.Unknown) {
                    R4Enumeration(value = R4PubStatus.Unknown)
                } else {
                    R4Enumeration(value = R4PubStatus.Active)
                }

            val itemsR4 = r5.item.map { convertQuestionnaireItemR5ToR4(it).getOrThrow() }

            dev.ohs.fhir.model.r4.Questionnaire(
                id = r5.id,
                url = mapUriR5ToR4(r5.url),
                title = mapStringR5ToR4(r5.title),
                status = statusR4,
                item = itemsR4,
            )
        }

    /**
     * Recursively converts an R5 Questionnaire item into an R4 Questionnaire item.
     *
     * @param item The source R5 item.
     * @return A [Result] enclosing the converted R4 item.
     */
    fun convertQuestionnaireItemR5ToR4(
        item: dev.ohs.fhir.model.r5.Questionnaire.Item,
    ): Result<dev.ohs.fhir.model.r4.Questionnaire.Item> =
        runCatching {
            val t = item.type.value
            val typeR4 =
                if (t != null) {
                    runCatching {
                        dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType
                            .fromCode(t.code)
                    }.getOrDefault(dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType.String)
                } else {
                    dev.ohs.fhir.model.r4.Questionnaire.QuestionnaireItemType.String
                }

            val subItemsR4 = item.item.map { convertQuestionnaireItemR5ToR4(it).getOrThrow() }
            val optionsR4 = convertAnswerOptionsR5ToR4(item.answerOption)

            dev.ohs.fhir.model.r4.Questionnaire.Item(
                linkId = mapStringR5ToR4(item.linkId) ?: R4String(value = ""),
                text = mapStringR5ToR4(item.text),
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = typeR4),
                required = mapBooleanR5ToR4(item.required),
                repeats = mapBooleanR5ToR4(item.repeats),
                answerOption = optionsR4,
                item = subItemsR4,
            )
        }

    /**
     * Converts R5 answer options into R4 answer options.
     *
     * @param options The source R5 answer options.
     * @return Converted R4 answer options.
     */
    private fun convertAnswerOptionsR5ToR4(
        options: List<dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption>,
    ): List<dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption> =
        options.mapNotNull { opt ->
            when (val v = opt.value) {
                is dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.String -> {
                    val str = v.value.value
                    if (str != null) {
                        dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption(
                            value =
                                dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value
                                    .String(R4String(value = str)),
                        )
                    } else {
                        null
                    }
                }
                is dev.ohs.fhir.model.r5.Questionnaire.Item.AnswerOption.Value.Coding -> {
                    dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption(
                        value =
                            dev.ohs.fhir.model.r4.Questionnaire.Item.AnswerOption.Value.Coding(
                                dev.ohs.fhir.model.r4.Coding(
                                    system = mapUriR5ToR4(v.value.system),
                                    code = mapCodeR5ToR4(v.value.code),
                                    display = mapStringR5ToR4(v.value.display),
                                ),
                            ),
                    )
                }
                else -> null
            }
        }

    /**
     * Converts a FHIR R4 QuestionnaireResponse into a FHIR R5 QuestionnaireResponse.
     *
     * @param r4 The R4 QuestionnaireResponse resource.
     * @return A [Result] enclosing the converted R5 QuestionnaireResponse.
     */
    fun convertQuestionnaireResponseR4ToR5(
        r4: dev.ohs.fhir.model.r4.QuestionnaireResponse,
    ): Result<dev.ohs.fhir.model.r5.QuestionnaireResponse> =
        runCatching {
            val s = r4.status.value
            val statusR5 =
                if (s == R4QrStatus.In_Progress) {
                    R5Enumeration(value = R5QrStatus.In_Progress)
                } else if (s == R4QrStatus.Amended) {
                    R5Enumeration(value = R5QrStatus.Amended)
                } else if (s == R4QrStatus.Entered_In_Error) {
                    R5Enumeration(value = R5QrStatus.Entered_In_Error)
                } else if (s == R4QrStatus.Stopped) {
                    R5Enumeration(value = R5QrStatus.Stopped)
                } else {
                    R5Enumeration(value = R5QrStatus.Completed)
                }

            val canonicalUri =
                valStrOrEmpty(r4.questionnaire?.value).let {
                    R5Canonical(value = it)
                }

            dev.ohs.fhir.model.r5.QuestionnaireResponse(
                id = r4.id,
                questionnaire = canonicalUri,
                status = statusR5,
                subject = mapReferenceR4ToR5(r4.subject),
                encounter = mapReferenceR4ToR5(r4.encounter),
                author = mapReferenceR4ToR5(r4.author),
            )
        }

    /**
     * Converts a FHIR R5 QuestionnaireResponse into a FHIR R4 QuestionnaireResponse.
     *
     * @param r5 The R5 QuestionnaireResponse resource.
     * @return A [Result] enclosing the converted R4 QuestionnaireResponse.
     */
    fun convertQuestionnaireResponseR5ToR4(
        r5: dev.ohs.fhir.model.r5.QuestionnaireResponse,
    ): Result<dev.ohs.fhir.model.r4.QuestionnaireResponse> =
        runCatching {
            val s = r5.status.value
            val statusR4 =
                if (s == R5QrStatus.In_Progress) {
                    R4Enumeration(value = R4QrStatus.In_Progress)
                } else if (s == R5QrStatus.Amended) {
                    R4Enumeration(value = R4QrStatus.Amended)
                } else if (s == R5QrStatus.Entered_In_Error) {
                    R4Enumeration(value = R4QrStatus.Entered_In_Error)
                } else if (s == R5QrStatus.Stopped) {
                    R4Enumeration(value = R4QrStatus.Stopped)
                } else {
                    R4Enumeration(value = R4QrStatus.Completed)
                }

            val canonicalUri =
                valStrOrEmpty(r5.questionnaire.value).let {
                    R4Canonical(value = it)
                }

            dev.ohs.fhir.model.r4.QuestionnaireResponse(
                id = r5.id,
                questionnaire = canonicalUri,
                status = statusR4,
                subject = mapReferenceR5ToR4(r5.subject),
                encounter = mapReferenceR5ToR4(r5.encounter),
                author = mapReferenceR5ToR4(r5.author),
            )
        }

    /**
     * Helper to return non-null string or empty.
     *
     * @param str Nullable string.
     * @return Non-null string.
     */
    private fun valStrOrEmpty(str: String?): String = str ?: ""

    /**
     * Converts a FHIR R4 DocumentReference into a FHIR R5 DocumentReference.
     *
     * @param r4 The R4 DocumentReference resource.
     * @return A [Result] enclosing the converted R5 DocumentReference.
     */
    fun convertDocumentReferenceR4ToR5(
        r4: dev.ohs.fhir.model.r4.DocumentReference,
    ): Result<dev.ohs.fhir.model.r5.DocumentReference> =
        runCatching {
            val s = r4.status.value
            val statusR5 =
                if (s == dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Superseded) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Superseded)
                } else if (s == dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Entered_In_Error) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Entered_In_Error)
                } else {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Current)
                }

            val contentsR5 =
                r4.content.map { c ->
                    dev.ohs.fhir.model.r5.DocumentReference.Content(
                        attachment =
                            dev.ohs.fhir.model.r5.Attachment(
                                contentType = mapCodeR4ToR5(c.attachment.contentType),
                                url = mapUrlR4ToR5(c.attachment.url),
                            ),
                    )
                }

            dev.ohs.fhir.model.r5.DocumentReference(
                id = r4.id,
                status = statusR5,
                content = contentsR5,
                subject = mapReferenceR4ToR5(r4.subject),
            )
        }

    /**
     * Converts a FHIR R5 DocumentReference into a FHIR R4 DocumentReference.
     *
     * @param r5 The R5 DocumentReference resource.
     * @return A [Result] enclosing the converted R4 DocumentReference.
     */
    fun convertDocumentReferenceR5ToR4(
        r5: dev.ohs.fhir.model.r5.DocumentReference,
    ): Result<dev.ohs.fhir.model.r4.DocumentReference> =
        runCatching {
            val s = r5.status.value
            val statusR4 =
                if (s == dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Superseded) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Superseded)
                } else if (s == dev.ohs.fhir.model.r5.terminologies.DocumentReferenceStatus.Entered_In_Error) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Entered_In_Error)
                } else {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current)
                }

            val contentsR4 =
                r5.content.map { c ->
                    dev.ohs.fhir.model.r4.DocumentReference.Content(
                        attachment =
                            dev.ohs.fhir.model.r4.Attachment(
                                contentType = mapCodeR5ToR4(c.attachment.contentType),
                                url = mapUrlR5ToR4(c.attachment.url),
                            ),
                    )
                }

            dev.ohs.fhir.model.r4.DocumentReference(
                id = r5.id,
                status = statusR4,
                content = contentsR4,
                subject = mapReferenceR5ToR4(r5.subject),
            )
        }

    /**
     * Converts a FHIR R4 Provenance into a FHIR R5 Provenance.
     *
     * @param r4 The R4 Provenance resource.
     * @return A [Result] enclosing the converted R5 Provenance.
     */
    fun convertProvenanceR4ToR5(r4: dev.ohs.fhir.model.r4.Provenance): Result<dev.ohs.fhir.model.r5.Provenance> =
        runCatching {
            val targetR5 = r4.target.mapNotNull { mapReferenceR4ToR5(it) }
            val rec = r4.recorded.value
            val recordedR5 =
                if (rec != null) {
                    dev.ohs.fhir.model.r5.Instant(
                        value =
                            dev.ohs.fhir.model.r5.FhirDateTime
                                .fromString(rec.toString()),
                    )
                } else {
                    null
                }
            val agentsR5 =
                r4.agent
                    .map { a ->
                        val ref = a.who.reference
                        val v = ref?.value
                        val refStr = if (!v.isNullOrBlank()) v else "Practitioner/unknown"
                        dev.ohs.fhir.model.r5.Provenance.Agent(
                            who =
                                dev.ohs.fhir.model.r5
                                    .Reference(reference = R5String(value = refStr)),
                        )
                    }.ifEmpty {
                        listOf(
                            dev.ohs.fhir.model.r5.Provenance.Agent(
                                who =
                                    dev.ohs.fhir.model.r5.Reference(
                                        reference = R5String(value = "Practitioner/chartcam-device"),
                                    ),
                            ),
                        )
                    }
            dev.ohs.fhir.model.r5.Provenance(
                id = r4.id,
                target = targetR5,
                recorded = recordedR5,
                agent = agentsR5,
            )
        }

    /**
     * Converts a FHIR R5 Provenance into a FHIR R4 Provenance.
     *
     * @param r5 The R5 Provenance resource.
     * @return A [Result] enclosing the converted R4 Provenance.
     */
    fun convertProvenanceR5ToR4(r5: dev.ohs.fhir.model.r5.Provenance): Result<dev.ohs.fhir.model.r4.Provenance> =
        runCatching {
            val targetR4 = r5.target.mapNotNull { mapReferenceR5ToR4(it) }
            val rec = r5.recorded?.value
            val recordedR4 =
                if (rec != null) {
                    dev.ohs.fhir.model.r4.Instant(
                        value =
                            dev.ohs.fhir.model.r4.FhirDateTime
                                .fromString(rec.toString()),
                    )
                } else {
                    dev.ohs.fhir.model.r4.Instant(
                        value =
                            dev.ohs.fhir.model.r4.FhirDateTime
                                .fromString("2026-09-17T00:00:00Z"),
                    )
                }
            val agentsR4 =
                r5.agent
                    .map { a ->
                        val ref = a.who.reference
                        val v = ref?.value
                        val refStr = if (!v.isNullOrBlank()) v else "Practitioner/unknown"
                        dev.ohs.fhir.model.r4.Provenance.Agent(
                            who =
                                dev.ohs.fhir.model.r4
                                    .Reference(reference = R4String(value = refStr)),
                        )
                    }.ifEmpty {
                        listOf(
                            dev.ohs.fhir.model.r4.Provenance.Agent(
                                who =
                                    dev.ohs.fhir.model.r4.Reference(
                                        reference = R4String(value = "Practitioner/chartcam-device"),
                                    ),
                            ),
                        )
                    }
            dev.ohs.fhir.model.r4.Provenance(
                id = r5.id,
                target = targetR4,
                recorded = recordedR4,
                agent = agentsR4,
            )
        }

    /**
     * Converts a FHIR R4 Bundle into a FHIR R5 Bundle.
     *
     * @param r4 The R4 Bundle resource.
     * @return A [Result] enclosing the converted R5 Bundle.
     */
    fun convertBundleR4ToR5(r4: dev.ohs.fhir.model.r4.Bundle): Result<dev.ohs.fhir.model.r5.Bundle> =
        runCatching {
            val t = r4.type.value
            val typeR5 =
                if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.Document) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Document)
                } else if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.Message) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Message)
                } else if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.Transaction) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Transaction)
                } else if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.Transaction_Response) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Transaction_Response)
                } else if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.Batch) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Batch)
                } else if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.Batch_Response) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Batch_Response)
                } else if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.History) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.History)
                } else if (t == dev.ohs.fhir.model.r4.Bundle.BundleType.Searchset) {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Searchset)
                } else {
                    R5Enumeration(value = dev.ohs.fhir.model.r5.Bundle.BundleType.Collection)
                }

            val entriesR5 =
                r4.entry.map { e ->
                    val resR5 = e.resource?.let { convertResourceR4ToR5(it).getOrNull() }
                    dev.ohs.fhir.model.r5.Bundle.Entry(
                        fullUrl = mapUriR4ToR5(e.fullUrl),
                        resource = resR5,
                    )
                }

            dev.ohs.fhir.model.r5.Bundle(
                id = r4.id,
                type = typeR5,
                entry = entriesR5,
            )
        }

    /**
     * Converts a FHIR R5 Bundle into a FHIR R4 Bundle.
     *
     * @param r5 The R5 Bundle resource.
     * @return A [Result] enclosing the converted R4 Bundle.
     */
    fun convertBundleR5ToR4(r5: dev.ohs.fhir.model.r5.Bundle): Result<dev.ohs.fhir.model.r4.Bundle> =
        runCatching {
            val t = r5.type.value
            val typeR4 =
                if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.Document) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Document)
                } else if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.Message) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Message)
                } else if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.Transaction) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Transaction)
                } else if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.Transaction_Response) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Transaction_Response)
                } else if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.Batch) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Batch)
                } else if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.Batch_Response) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Batch_Response)
                } else if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.History) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.History)
                } else if (t == dev.ohs.fhir.model.r5.Bundle.BundleType.Searchset) {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Searchset)
                } else {
                    R4Enumeration(value = dev.ohs.fhir.model.r4.Bundle.BundleType.Collection)
                }

            val entriesR4 =
                r5.entry.map { e ->
                    val resR4 = e.resource?.let { convertResourceR5ToR4(it).getOrNull() }
                    dev.ohs.fhir.model.r4.Bundle.Entry(
                        fullUrl = mapUriR5ToR4(e.fullUrl),
                        resource = resR4,
                    )
                }

            dev.ohs.fhir.model.r4.Bundle(
                id = r5.id,
                type = typeR4,
                entry = entriesR4,
            )
        }

    /**
     * Converts a generic FHIR R4 Resource into its FHIR R5 equivalent.
     *
     * @param resource The R4 resource.
     * @return A [Result] enclosing the converted R5 resource.
     */
    fun convertResourceR4ToR5(resource: dev.ohs.fhir.model.r4.Resource): Result<dev.ohs.fhir.model.r5.Resource> =
        when (resource) {
            is R4Patient -> convertPatientR4ToR5(resource)
            is R4Encounter -> convertEncounterR4ToR5(resource)
            is R4Observation -> convertObservationR4ToR5(resource)
            is dev.ohs.fhir.model.r4.Questionnaire -> convertQuestionnaireR4ToR5(resource)
            is dev.ohs.fhir.model.r4.QuestionnaireResponse ->
                convertQuestionnaireResponseR4ToR5(resource)
            is dev.ohs.fhir.model.r4.DocumentReference ->
                convertDocumentReferenceR4ToR5(resource)
            is dev.ohs.fhir.model.r4.Provenance -> convertProvenanceR4ToR5(resource)
            else ->
                Result.failure(
                    IllegalArgumentException("Unsupported R4 resource type: ${resource::class.simpleName}"),
                )
        }

    /**
     * Converts a generic FHIR R5 Resource into its FHIR R4 equivalent.
     *
     * @param resource The R5 resource.
     * @return A [Result] enclosing the converted R4 resource.
     */
    fun convertResourceR5ToR4(resource: dev.ohs.fhir.model.r5.Resource): Result<dev.ohs.fhir.model.r4.Resource> =
        when (resource) {
            is R5Patient -> convertPatientR5ToR4(resource)
            is R5Encounter -> convertEncounterR5ToR4(resource)
            is R5Observation -> convertObservationR5ToR4(resource)
            is dev.ohs.fhir.model.r5.Questionnaire -> convertQuestionnaireR5ToR4(resource)
            is dev.ohs.fhir.model.r5.QuestionnaireResponse ->
                convertQuestionnaireResponseR5ToR4(resource)
            is dev.ohs.fhir.model.r5.DocumentReference ->
                convertDocumentReferenceR5ToR4(resource)
            is dev.ohs.fhir.model.r5.Provenance -> convertProvenanceR5ToR4(resource)
            else -> Result.failure(IllegalArgumentException("Unsupported R5 resource type"))
        }

    /**
     * Converts a FHIR R4 Patient into a FHIR R4B Patient.
     *
     * @param r4 The R4 Patient resource.
     * @return A [Result] enclosing the converted R4B Patient.
     */
    fun convertPatientR4ToR4B(r4: R4Patient): Result<dev.ohs.fhir.model.r4b.Patient> =
        runCatching {
            val g = r4.gender?.value
            val genderR4B =
                if (g == R4Gender.Male) {
                    dev.ohs.fhir.model.r4b
                        .Enumeration(value = R4BGender.Male)
                } else if (g == R4Gender.Female) {
                    dev.ohs.fhir.model.r4b
                        .Enumeration(value = R4BGender.Female)
                } else if (g == R4Gender.Other) {
                    dev.ohs.fhir.model.r4b
                        .Enumeration(value = R4BGender.Other)
                } else if (g == R4Gender.Unknown) {
                    dev.ohs.fhir.model.r4b
                        .Enumeration(value = R4BGender.Unknown)
                } else {
                    null
                }

            val act = r4.active?.value
            dev.ohs.fhir.model.r4b.Patient(
                id = r4.id,
                gender = genderR4B,
                active =
                    if (act != null) {
                        dev.ohs.fhir.model.r4b
                            .Boolean(value = act)
                    } else {
                        null
                    },
                name =
                    r4.name.map { n ->
                        val fam = n.family?.value
                        dev.ohs.fhir.model.r4b.HumanName(
                            family =
                                if (fam != null) {
                                    dev.ohs.fhir.model.r4b
                                        .String(value = fam)
                                } else {
                                    null
                                },
                            given =
                                n.given.mapNotNull { it.value }.map {
                                    dev.ohs.fhir.model.r4b
                                        .String(value = it)
                                },
                        )
                    },
            )
        }

    /**
     * Converts a FHIR R4B Patient into a FHIR R4 Patient.
     *
     * @param r4b The R4B Patient resource.
     * @return A [Result] enclosing the converted R4 Patient.
     */
    fun convertPatientR4BToR4(r4b: dev.ohs.fhir.model.r4b.Patient): Result<R4Patient> =
        runCatching {
            val g = r4b.gender?.value
            val genderR4 =
                if (g == R4BGender.Male) {
                    R4Enumeration(value = R4Gender.Male)
                } else if (g == R4BGender.Female) {
                    R4Enumeration(value = R4Gender.Female)
                } else if (g == R4BGender.Other) {
                    R4Enumeration(value = R4Gender.Other)
                } else if (g == R4BGender.Unknown) {
                    R4Enumeration(value = R4Gender.Unknown)
                } else {
                    null
                }

            val act = r4b.active?.value
            R4Patient(
                id = r4b.id,
                gender = genderR4,
                active =
                    if (act != null) {
                        dev.ohs.fhir.model.r4
                            .Boolean(value = act)
                    } else {
                        null
                    },
                name =
                    r4b.name.map { n ->
                        val fam = n.family?.value
                        R4HumanName(
                            family = if (fam != null) R4String(value = fam) else null,
                            given = n.given.mapNotNull { it.value }.map { R4String(value = it) },
                        )
                    },
            )
        }
}
