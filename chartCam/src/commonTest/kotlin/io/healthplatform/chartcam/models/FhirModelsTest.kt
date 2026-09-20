/**
 * @file FhirModelsTest.kt
 * Contains tests for FhirModels.kt.
 */

package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.Patient
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for FHIR model creation.
 */
class FhirModelsTest {
    /**
     * Tests creation of a FHIR [Patient].
     */
    @Test
    fun testCreateFhirPatient() {
        val dob = LocalDate(1990, 1, 1)
        val patientResult =
            createFhirPatientCatching(
                id = "pat_123",
                firstName = "John",
                lastName = "Doe",
                dob = dob,
                mrnValue = "MRN-555",
            )
        assertTrue(patientResult.isSuccess)
        val patient = patientResult.getOrThrow()
        assertEquals("pat_123", patient.id)
        assertEquals("Doe", patient.name.first().familyName)
        assertEquals("John", patient.name.first().givenName)
        assertEquals("MRN-555", patient.mrn)
        assertEquals("1990-01-01", patient.customBirthDate)
        assertEquals(dob, (patient.fhirBirthDate as? dev.ohs.fhir.model.r4.FhirDate.Date)?.date)
        assertEquals("Doe, John", patient.fullName)

        val mrnIdentifier = buildMrnIdentifier("MRN-555").getOrThrow()
        assertEquals(
            "http://terminology.hl7.org/CodeSystem/v2-0203",
            mrnIdentifier.type
                ?.coding
                ?.first()
                ?.system
                ?.value,
        )
        assertEquals(
            "MR",
            mrnIdentifier.type
                ?.coding
                ?.first()
                ?.code
                ?.value,
        )
        assertEquals(
            dev.ohs.fhir.model.r4.terminologies.IdentifierTypeCodes.Mr.display,
            mrnIdentifier.type
                ?.coding
                ?.first()
                ?.display
                ?.value,
        )

        assertTrue(createFhirPatientCatching("", "A", "B", dob, "MRN").isFailure)
        assertTrue(createFhirPatientCatching("id", "A", "B", dob, "").isFailure)
    }

    /**
     * Tests creation of a FHIR Practitioner.
     */
    @Test
    fun testCreateFhirPractitioner() {
        val practitioner =
            createFhirPractitioner(
                id = "prac_456",
                lastName = "Smith",
                firstName = "Jane",
                isActive = true,
            )
        assertEquals("prac_456", practitioner.id)
        assertEquals("Smith", practitioner.name.first().familyName)
        assertEquals("Jane", practitioner.name.first().givenName)
        assertTrue(practitioner.active?.value == true)
        assertEquals("Smith, Jane", practitioner.fullName)
    }

    /**
     * Tests creation of a FHIR Encounter.
     */
    @Test
    fun testCreateFhirEncounter() {
        val encounter =
            createFhirEncounter(
                id = "enc_789",
                patientId = "Patient/pat_123",
                practitionerId = "Practitioner/prac_456",
                dateStr = "2023-10-27T10:00:00Z",
            )
        assertEquals("enc_789", encounter.id)
        assertEquals("Patient/pat_123", encounter.subject?.reference?.value)
        assertEquals(
            "Practitioner/prac_456",
            encounter.participant
                .first()
                .individual
                ?.reference
                ?.value,
        )
        assertTrue(encounter.encounterDate.contains("2023-10-27"))
    }

    /**
     * Tests creation of a FHIR DocumentReference.
     */
    @Test
    fun testCreateFhirDocumentReference() {
        val docRef =
            createFhirDocumentReference(
                DocumentReferenceCreationParams(
                    id = "doc_111",
                    patientId = "Patient/pat_123",
                    encounterId = "Encounter/enc_789",
                    dateStr = "2023-10-27T10:05:00Z",
                    desc = "Front View",
                    mime = "image/jpeg",
                    urlPath = "file:///path/to/image.jpg",
                ),
            )
        assertEquals("doc_111", docRef.id)
        assertEquals("Patient/pat_123", docRef.subject?.reference?.value)
        assertEquals(
            "Encounter/enc_789",
            docRef.context
                ?.encounter
                ?.first()
                ?.reference
                ?.value,
        )
        assertEquals("Front View", docRef.description?.value)
        assertEquals(
            "image/jpeg",
            docRef.content
                .first()
                .attachment
                .contentType
                ?.value,
        )
        assertEquals(
            "file:///path/to/image.jpg",
            docRef.content
                .first()
                .attachment
                .url
                ?.value,
        )
    }

    /**
     * Tests creation of a FHIR clinical note DocumentReference.
     */
    @Test
    fun testCreateFhirClinicalNote() {
        val clinicalNote =
            createFhirClinicalNote(
                id = "note_222",
                patientId = "Patient/pat_123",
                encounterId = "Encounter/enc_789",
                dateStr = "2023-10-27T10:10:00Z",
                notesText = "Patient seems fine.",
            )
        assertEquals("note_222", clinicalNote.id)
        assertEquals("Patient/pat_123", clinicalNote.subject?.reference?.value)
        assertEquals(
            "text/plain",
            clinicalNote.content
                .first()
                .attachment
                .contentType
                ?.value,
        )
        assertTrue(
            clinicalNote.content
                .first()
                .attachment
                .url
                ?.value
                ?.contains("Patient seems fine.") == true,
        )
    }

    /**
     * Tests creation of a FHIR Device.
     */
    @Test
    fun testCreateFhirDevice() {
        val device =
            createFhirDevice(
                id = "dev_333",
                modelName = "Pixel 7",
                manufacturerName = "Google",
            )
        assertEquals("dev_333", device.id)
        assertEquals(
            "Pixel 7",
            device.deviceName
                .first()
                .name
                .value,
        )
        assertEquals("Google", device.manufacturer?.value)
    }

    /**
     * Tests creation of a FHIR Provenance.
     */
    @Test
    fun testCreateFhirProvenance() {
        val provenance =
            createFhirProvenance(
                id = "prov_444",
                targetResourceId = "DocumentReference/doc_111",
                practitionerId = "Practitioner/prac_456",
                dateStr = "2023-10-27T10:06:00Z",
            )
        assertEquals("prov_444", provenance.id)
        assertEquals(
            "DocumentReference/doc_111",
            provenance.target
                .first()
                .reference
                ?.value,
        )
        assertEquals(
            "Practitioner/prac_456",
            provenance.agent
                .first()
                .who
                .reference
                ?.value,
        )
    }

    /**
     * Tests creation of a FHIR Binary.
     */
    @Test
    fun testCreateFhirBinary() {
        val binary =
            createFhirBinary(
                id = "bin_555",
                contentTypeStr = "image/png",
                base64Data = "iVBORw0KGgo=",
            )
        assertEquals("bin_555", binary.id)
        assertEquals("image/png", binary.contentType.value)
        assertEquals("iVBORw0KGgo=", binary.data?.value)
    }

    /**
     * Tests parsing of empty FHIR extensions.
     */
    @Test
    fun testEmptyPatientExtensions() {
        val emptyPatient = Patient.Builder().build()
        assertEquals("", emptyPatient.mrn)
        assertEquals("", emptyPatient.customBirthDate)
        assertEquals("Unknown", emptyPatient.fullName)
    }

    /**
     * Tests culturally sensitive person name formatting across Western and East Asian locales.
     */
    @Test
    fun testCulturalPersonNameFormatting() {
        val patient = createFhirPatient("p1", "Wei", "Chen", kotlinx.datetime.LocalDate(1990, 1, 1), "123")
        assertEquals("Chen, Wei", patient.getFullName("en"))
        assertEquals("Chen, Wei", patient.getFullName("es"))
        assertEquals("Chen, Wei", patient.getFullName("he"))
        assertEquals("ChenWei", patient.getFullName("zh"))
        assertEquals("ChenWei", patient.getFullName("zh-TW"))
        assertEquals("ChenWei", patient.getFullName("ja"))

        val cjkPatient = createFhirPatient("p2", "偉", "陳", kotlinx.datetime.LocalDate(1990, 1, 1), "124")
        assertEquals("陳偉", cjkPatient.getFullName("zh"))
        assertEquals("陳偉", cjkPatient.getFullName("ja"))

        val emptyPatient = Patient.Builder().build()
        assertEquals("Unknown", emptyPatient.getFullName("en"))
        assertEquals("Desconocido", emptyPatient.getFullName("es"))
        assertEquals("לא ידוע", emptyPatient.getFullName("he"))
        assertEquals("未知", emptyPatient.getFullName("zh"))
        assertEquals("不明", emptyPatient.getFullName("ja"))

        val practitioner = createFhirPractitioner("pr1", "Tanaka", "Ken", true)
        assertEquals("Tanaka, Ken", practitioner.getFullName("en"))
        assertEquals("TanakaKen", practitioner.getFullName("ja"))
        assertEquals("TanakaKen", practitioner.getFullName("zh"))

        val partialNameOnlyFamily = formatPersonName("Smith", null, "en")
        assertEquals("Smith", partialNameOnlyFamily)
        val partialNameOnlyGiven = formatPersonName(null, "John", "en")
        assertEquals("John", partialNameOnlyGiven)
        val partialNameBothNull = formatPersonName(null, null, "en")
        assertEquals("Unknown", partialNameBothNull)
    }

    /**
     * Tests parseAdministrativeGender with valid and invalid inputs.
     */
    @Test
    fun testParseAdministrativeGender() {
        val maleResult = parseAdministrativeGender("male")
        assertTrue(maleResult.isSuccess)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Male, maleResult.getOrNull()?.value)

        val femaleResult = parseAdministrativeGender("female")
        assertTrue(femaleResult.isSuccess)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Female, femaleResult.getOrNull()?.value)

        val otherResult = parseAdministrativeGender("other")
        assertTrue(otherResult.isSuccess)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Other, otherResult.getOrNull()?.value)

        val unknownResult = parseAdministrativeGender("unknown")
        assertTrue(unknownResult.isSuccess)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Unknown, unknownResult.getOrNull()?.value)

        val invalidResult = parseAdministrativeGender("invalid_gender")
        assertTrue(invalidResult.isFailure)
    }

    /**
     * Tests typedStatus extension properties for Encounter and DocumentReference.
     */
    @Test
    fun testTypedStatus() {
        val encounter =
            createFhirEncounter(
                id = "enc_1",
                patientId = "Patient/p1",
                practitionerId = "Practitioner/pr1",
                dateStr = "2023-10-27T10:00:00Z",
            )
        assertTrue(encounter.typedStatus.isSuccess)
        assertEquals(dev.ohs.fhir.model.r4.Encounter.EncounterStatus.In_Progress, encounter.typedStatus.getOrNull())

        val docRef =
            createFhirDocumentReference(
                DocumentReferenceCreationParams(
                    id = "doc_1",
                    patientId = "Patient/p1",
                    encounterId = "Encounter/enc_1",
                    dateStr = "2023-10-27T10:05:00Z",
                    desc = "Test Note",
                    mime = "application/pdf",
                    urlPath = "file:///tmp/test.pdf",
                ),
            )
        assertTrue(docRef.typedStatus.isSuccess)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current, docRef.typedStatus.getOrNull())
    }

    /**
     * Tests FhirToDicomMapper extractGender and extractGenderCatching.
     */
    @Test
    fun testDicomExtractGender() {
        val malePatient = createFhirPatient("p_m", "A", "B", LocalDate(1980, 1, 1), "1", gender = "male")
        assertEquals(
            "M",
            io.healthplatform.chartcam.dicom.FhirToDicomMapper
                .extractGender(malePatient),
        )
        assertEquals(
            "M",
            io.healthplatform.chartcam.dicom.FhirToDicomMapper
                .extractGenderCatching(malePatient)
                .getOrNull(),
        )

        val femalePatient = createFhirPatient("p_f", "A", "B", LocalDate(1980, 1, 1), "2", gender = "female")
        assertEquals(
            "F",
            io.healthplatform.chartcam.dicom.FhirToDicomMapper
                .extractGender(femalePatient),
        )
        assertEquals(
            "F",
            io.healthplatform.chartcam.dicom.FhirToDicomMapper
                .extractGenderCatching(femalePatient)
                .getOrNull(),
        )

        val otherPatient = createFhirPatient("p_o", "A", "B", LocalDate(1980, 1, 1), "3", gender = "other")
        assertEquals(
            "O",
            io.healthplatform.chartcam.dicom.FhirToDicomMapper
                .extractGender(otherPatient),
        )

        val unknownPatient = createFhirPatient("p_u", "A", "B", LocalDate(1980, 1, 1), "4", gender = "unknown")
        assertEquals(
            "O",
            io.healthplatform.chartcam.dicom.FhirToDicomMapper
                .extractGender(unknownPatient),
        )
    }

    /**
     * Verifies DocumentReferenceCreationParams data class semantics.
     */
    @Test
    fun testDocumentReferenceCreationParams() {
        val params =
            DocumentReferenceCreationParams(
                id = "doc-1",
                patientId = "Patient/p-1",
                encounterId = "Encounter/e-1",
                dateStr = "2023-01-01T12:00:00Z",
                desc = "Test Photo",
                mime = "image/jpeg",
                urlPath = "photos/photo.jpg",
                answerCode = "ans-99",
            )
        val (id, patId, encId, dt, desc, mime, url, ans) = params
        assertEquals("doc-1", id)
        assertEquals("Patient/p-1", patId)
        assertEquals("Encounter/e-1", encId)
        assertEquals("2023-01-01T12:00:00Z", dt)
        assertEquals("Test Photo", desc)
        assertEquals("image/jpeg", mime)
        assertEquals("photos/photo.jpg", url)
        assertEquals("ans-99", ans)

        val identical = params.copy()
        assertEquals(params, identical)
        assertEquals(params.hashCode(), identical.hashCode())
        assertTrue(params.toString().contains("doc-1"))

        val modified = params.copy(answerCode = null)
        kotlin.test.assertNotEquals(params, modified)
        kotlin.test.assertFalse(params.equals(null))
        kotlin.test.assertFalse(params.equals("doc-1"))
    }

    /**
     * Verifies edge cases and fallbacks across Patient, Practitioner, and Encounter models.
     */
    @Test
    fun testFhirModelsEdgeCases() {
        // Fallback branch in createFhirPatient when id or mrnValue is blank
        val fallbackPatient =
            createFhirPatient(
                id = "",
                firstName = "Fallback",
                lastName = "User",
                dob = LocalDate(2000, 1, 1),
                mrnValue = "",
                organizationId = "org-1",
                gender = "other",
            )
        assertEquals("", fallbackPatient.id)
        assertEquals("Fallback User", fallbackPatient.name.first().let { "${it.given.first().value} ${it.family?.value}" })
        assertEquals("org-1", fallbackPatient.managingOrganization?.reference?.value)

        // createFhirPatientCatching with organizationId
        val patWithOrg =
            createFhirPatientCatching(
                id = "p-org",
                firstName = "Org",
                lastName = "Patient",
                dob = LocalDate(1995, 3, 3),
                mrnValue = "MRN-ORG",
                organizationId = "org-main",
                gender = "female",
            ).getOrThrow()
        assertEquals("org-main", patWithOrg.managingOrganization?.reference?.value)

        // Patient.mrn branches: legacy MR code, non-coded MRN, and empty
        val legacyPatient =
            Patient
                .Builder()
                .apply {
                    identifier.add(
                        dev.ohs.fhir.model.r4.Identifier.Builder().apply {
                            value =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "LEGACY-MRN" }
                            type =
                                dev.ohs.fhir.model.r4.CodeableConcept.Builder().apply {
                                    coding.add(
                                        dev.ohs.fhir.model.r4.Coding.Builder().apply {
                                            code =
                                                dev.ohs.fhir.model.r4.Code
                                                    .Builder()
                                                    .apply { value = "MR" }
                                        },
                                    )
                                }
                        },
                    )
                }.build()
        assertEquals("LEGACY-MRN", legacyPatient.mrn)

        val rawMrnPatient =
            Patient
                .Builder()
                .apply {
                    identifier.add(
                        dev.ohs.fhir.model.r4.Identifier.Builder().apply {
                            value =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "RAW-123" }
                        },
                    )
                }.build()
        assertEquals("RAW-123", rawMrnPatient.mrn)

        val noMrnPatient = Patient.Builder().build()
        assertEquals("", noMrnPatient.mrn)

        // Patient.customBirthDate and formatLocalizedBirthDate
        assertEquals("", noMrnPatient.customBirthDate)
        val birthDateResultNull = noMrnPatient.formatLocalizedBirthDate("en")
        assertTrue(birthDateResultNull.isSuccess)
        kotlin.test.assertNull(birthDateResultNull.getOrThrow())

        val patWithDob = createFhirPatient("p-dob", "Bob", "Smith", LocalDate(1990, 5, 12), "MRN-DOB")
        val birthDateFormatted = patWithDob.formatLocalizedBirthDate("en")
        assertTrue(birthDateFormatted.isSuccess)
        kotlin.test.assertNotNull(birthDateFormatted.getOrThrow())

        // HumanName missing family or given
        val emptyHumanName =
            dev.ohs.fhir.model.r4.HumanName
                .Builder()
                .build()
        assertEquals("Unknown", emptyHumanName.familyName)
        assertEquals("Unknown", emptyHumanName.givenName)

        // getLocalizedUnknownName locales
        assertEquals("Desconocido", getLocalizedUnknownName("es-ES"))
        assertEquals("不明", getLocalizedUnknownName("ja-JP"))
        assertEquals("לא ידוע", getLocalizedUnknownName("he-IL"))
        assertEquals("לא ידוע", getLocalizedUnknownName("iw"))
        assertEquals("未知", getLocalizedUnknownName("zh-CN"))
        assertEquals("Unknown", getLocalizedUnknownName("fr-FR"))

        // formatPersonName branches
        assertEquals("Chen", formatPersonName("Chen", null, "zh"))
        assertEquals("Wei", formatPersonName(null, "Wei", "zh"))
        assertEquals("Chen", formatPersonName("Chen", "", "zh"))
        assertEquals("Wei", formatPersonName("", "Wei", "zh"))
        assertEquals("Unknown", formatPersonName("  ", "  ", "en"))
        assertEquals("未知", formatPersonName(null, null, "zh"))

        // Patient and Practitioner getFullName and fullName with empty names
        val emptyPractitioner =
            dev.ohs.fhir.model.r4.Practitioner
                .Builder()
                .build()
        assertEquals("Unknown", emptyPractitioner.getFullName("en"))
        assertEquals("Unknown", emptyPractitioner.fullName)
        assertEquals("Unknown", noMrnPatient.fullName)

        // Encounter with default id=null and encounterDate without period
        val encNoId = createFhirEncounter(patientId = "Patient/p-1", practitionerId = "Practitioner/pr-1", dateStr = "2023-11-01T08:00:00Z")
        kotlin.test.assertNull(encNoId.id)
        val emptyEncounter =
            dev.ohs.fhir.model.r4.Encounter(
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code(value = "AMB"),
                        ),
            )
        assertEquals("", emptyEncounter.encounterDate)

        // DocumentReference creation with blank/null description, answerCode, and invalid date
        val docWithAnswerAndInvalidDate =
            createFhirDocumentReference(
                DocumentReferenceCreationParams(
                    id = "doc-custom",
                    patientId = "Patient/p-1",
                    encounterId = "Encounter/e-1",
                    dateStr = "invalid-date-string",
                    desc = "  ",
                    mime = "image/png",
                    urlPath = "photos/photo.png",
                    answerCode = "ans-42",
                ),
            )
        kotlin.test.assertNull(docWithAnswerAndInvalidDate.date)
        kotlin.test.assertNull(docWithAnswerAndInvalidDate.description)
        assertEquals(
            "ans-42",
            docWithAnswerAndInvalidDate.context
                ?.related
                ?.firstOrNull()
                ?.identifier
                ?.value
                ?.value,
        )

        // Clinical note creation with invalid date
        val clinicalNoteInvalidDate =
            createFhirClinicalNote(
                id = "note-invalid",
                patientId = "Patient/p-1",
                encounterId = "Encounter/e-1",
                dateStr = "not-a-valid-date",
                notesText = "Short note text",
            )
        kotlin.test.assertNull(clinicalNoteInvalidDate.date)

        // typedStatus failure branches on empty resources
        assertTrue(emptyEncounter.typedStatus.isFailure)
        val emptyDocRef =
            dev.ohs.fhir.model.r4.DocumentReference(
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
                content = emptyList(),
            )
        assertTrue(emptyDocRef.typedStatus.isFailure)

        // Default argument invocations
        val defaultFormattedDob = patWithDob.formatLocalizedBirthDate()
        assertTrue(defaultFormattedDob.isSuccess)
        kotlin.test.assertNotNull(defaultFormattedDob.getOrThrow())

        val defaultUnknownName = getLocalizedUnknownName()
        assertTrue(defaultUnknownName.isNotEmpty())

        val defaultPersonName = formatPersonName("Doe", "Jane")
        assertEquals("Doe, Jane", defaultPersonName)

        val defaultPatFullName = patWithDob.getFullName()
        assertEquals("Smith, Bob", defaultPatFullName)

        val defaultPracFullName = createFhirPractitioner("pr-def", "Taylor", "Alex", true).getFullName()
        assertEquals("Taylor, Alex", defaultPracFullName)

        val blankMrnResult = buildMrnIdentifier("   ")
        assertTrue(blankMrnResult.isFailure)

        // DocumentReferenceCreationParams with default answerCode
        val paramsDefaultAns =
            DocumentReferenceCreationParams(
                id = "doc-def-ans",
                patientId = "Patient/p-def",
                encounterId = "Encounter/e-def",
                dateStr = "2023-02-02T10:00:00Z",
                desc = null,
                mime = "image/jpeg",
                urlPath = "photos/def.jpg",
            )
        kotlin.test.assertNull(paramsDefaultAns.answerCode)

        // createFhirPatient with default organizationId and gender
        val patDefaults = createFhirPatient("p-defaults-only", "Dan", "Evans", LocalDate(1991, 1, 1), "MRN-DEF")
        kotlin.test.assertNull(patDefaults.managingOrganization)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Unknown, patDefaults.gender?.value)

        // Patient.mrn with non-MR coding, and with null value
        val nonMrPatient =
            dev.ohs.fhir.model.r4.Patient(
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            type =
                                dev.ohs.fhir.model.r4.CodeableConcept(
                                    coding =
                                        listOf(
                                            dev.ohs.fhir.model.r4.Coding(
                                                code =
                                                    dev.ohs.fhir.model.r4
                                                        .Code(value = "OTHER"),
                                            ),
                                        ),
                                ),
                            value =
                                dev.ohs.fhir.model.r4
                                    .String(value = "NON-MR-VAL"),
                        ),
                    ),
            )
        assertEquals("NON-MR-VAL", nonMrPatient.mrn)

        val nullValPatient =
            dev.ohs.fhir.model.r4.Patient(
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            value = null,
                        ),
                    ),
            )
        assertEquals("", nullValPatient.mrn)

        // ja person naming with only family or only given
        assertEquals("Tanaka", formatPersonName("Tanaka", null, "ja"))
        assertEquals("Ken", formatPersonName(null, "Ken", "ja"))

        // Patient and Practitioner with blank names
        val blankNamePatient =
            dev.ohs.fhir.model.r4.Patient(
                name =
                    listOf(
                        dev.ohs.fhir.model.r4.HumanName(
                            family = null,
                            given = emptyList(),
                        ),
                    ),
            )
        assertEquals("Unknown", blankNamePatient.getFullName("en"))

        val blankNamePractitioner =
            dev.ohs.fhir.model.r4.Practitioner(
                name =
                    listOf(
                        dev.ohs.fhir.model.r4.HumanName(
                            family = null,
                            given = emptyList(),
                        ),
                    ),
            )
        assertEquals("Unknown", blankNamePractitioner.getFullName("en"))
    }

    /**
     * Verifies gender parsing fallback and MRN detection variants in Patient models.
     */
    @Test
    fun testPatientBranchEdgeCases() {
        val patUnknownGender = createFhirPatient("p-unk", "Jane", "Doe", LocalDate(2000, 1, 1), "MRN-1", gender = "not-a-gender")
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Unknown, patUnknownGender.gender?.value)

        val patWithOrg =
            createFhirPatient(
                "p-org",
                "Jane",
                "Doe",
                LocalDate(2000, 1, 1),
                "MRN-2",
                organizationId = "Organization/org-99",
                gender = "female",
            )
        assertEquals("Organization/org-99", patWithOrg.managingOrganization?.reference?.value)

        // MRN detection with "MR" code
        val patMrCode =
            dev.ohs.fhir.model.r4.Patient(
                id = "p-mr",
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            type =
                                dev.ohs.fhir.model.r4.CodeableConcept(
                                    coding =
                                        listOf(
                                            dev.ohs.fhir.model.r4.Coding(
                                                code =
                                                    dev.ohs.fhir.model.r4
                                                        .Code("MR"),
                                            ),
                                        ),
                                ),
                            value =
                                dev.ohs.fhir.model.r4
                                    .String(value = "MR-999"),
                        ),
                    ),
            )
        assertEquals("MR-999", patMrCode.mrn)

        // Patient with empty birthDate
        val patNoDob =
            dev.ohs.fhir.model.r4
                .Patient(id = "p-nodob")
        assertEquals("", patNoDob.customBirthDate)
        kotlin.test.assertNull(patNoDob.fhirBirthDate)
        kotlin.test.assertNull(patNoDob.formatLocalizedBirthDate().getOrThrow())

        // Given only name
        val patGivenOnly =
            dev.ohs.fhir.model.r4.Patient(
                id = "p-given",
                name =
                    listOf(
                        dev.ohs.fhir.model.r4.HumanName(
                            family = null,
                            given =
                                listOf(
                                    dev.ohs.fhir.model.r4
                                        .String(value = "Sam"),
                                ),
                        ),
                    ),
            )
        assertEquals("Sam", patGivenOnly.getFullName("en"))
        assertEquals("Sam", patGivenOnly.getFullName("ja"))
        assertEquals("Sam", patGivenOnly.fullName)
        assertEquals("Unknown", patGivenOnly.name.first().familyName)
        assertEquals("Sam", patGivenOnly.name.first().givenName)

        // Family only name
        val patFamilyOnly =
            dev.ohs.fhir.model.r4.Patient(
                id = "p-fam",
                name =
                    listOf(
                        dev.ohs.fhir.model.r4.HumanName(
                            family =
                                dev.ohs.fhir.model.r4
                                    .String(value = "Smith"),
                            given = emptyList(),
                        ),
                    ),
            )
        assertEquals("Smith", patFamilyOnly.getFullName("en"))
        assertEquals("Smith", patFamilyOnly.getFullName("ja"))
        assertEquals("Smith", patFamilyOnly.fullName)
        assertEquals("Smith", patFamilyOnly.name.first().familyName)
        assertEquals("Unknown", patFamilyOnly.name.first().givenName)
    }

    /**
     * Verifies Encounter and DocumentReference branch edge cases.
     */
    @Test
    fun testEncounterAndDocumentReferenceBranchEdgeCases() {
        val encNoPeriod =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-np",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code("AMB"),
                        ),
            )
        assertEquals("", encNoPeriod.encounterDate)

        val note =
            createFhirClinicalNote(
                id = "note-blank",
                patientId = "pat-1",
                encounterId = "enc-1",
                dateStr = "invalid-date",
                notesText = "clinical notes text",
            )
        kotlin.test.assertNotNull(note)
        assertEquals("DocumentReference/note-blank", "DocumentReference/${note.id}")

        // Test DocumentReferenceCreationParams data class and createFhirDocumentReference with invalid date
        val params1 =
            DocumentReferenceCreationParams(
                id = "doc-1",
                patientId = "pat-1",
                encounterId = "enc-1",
                dateStr = "2020-01-01T00:00:00Z",
                desc = "desc-1",
                mime = "image/jpeg",
                urlPath = "/photos/1.jpg",
                answerCode = "CODE1",
            )
        val params2 =
            DocumentReferenceCreationParams(
                id = "doc-1",
                patientId = "pat-1",
                encounterId = "enc-1",
                dateStr = "2020-01-01T00:00:00Z",
                desc = "desc-1",
                mime = "image/jpeg",
                urlPath = "/photos/1.jpg",
                answerCode = "CODE1",
            )
        val params3 = params1.copy(id = "doc-2")
        val paramsDefault =
            DocumentReferenceCreationParams(
                id = "doc-def",
                patientId = "pat-1",
                encounterId = "enc-1",
                dateStr = "invalid-date",
                desc = "",
                mime = "image/jpeg",
                urlPath = "/photos/def.jpg",
            )
        assertTrue(params1.equals(params1))
        assertEquals(params1, params2)
        assertEquals(params1.hashCode(), params2.hashCode())
        kotlin.test.assertNotEquals(params1, params3)
        kotlin.test.assertFalse(params1.equals(null))
        kotlin.test.assertFalse(params1.equals("other"))
        kotlin.test.assertNull(paramsDefault.answerCode)
        assertEquals(params1, params1.copy())
        assertEquals(params3, params1.copy(id = "doc-2"))
        val (pid, ppat, penc, pdate, pdesc, pmime, purl, pans) = params1
        assertEquals("doc-1", pid)
        assertEquals("CODE1", pans)
        assertTrue(params1.toString().contains("doc-1"))

        val docRefInvalidDate = createFhirDocumentReference(paramsDefault)
        kotlin.test.assertNull(docRefInvalidDate.date)

        // Test Patient.mrn branches: "MR" code, no-code coding, null-code coding, and fallback identifier
        val patMrCode =
            dev.ohs.fhir.model.r4.Patient(
                id = "p-mr",
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            type =
                                dev.ohs.fhir.model.r4.CodeableConcept(
                                    coding =
                                        listOf(
                                            dev.ohs.fhir.model.r4.Coding(
                                                code =
                                                    dev.ohs.fhir.model.r4
                                                        .Code(value = "MR"),
                                            ),
                                        ),
                                ),
                            value =
                                dev.ohs.fhir.model.r4
                                    .String(value = "MR-VAL-1"),
                        ),
                    ),
            )
        assertEquals("MR-VAL-1", patMrCode.mrn)

        val patOtherCodeFallback =
            dev.ohs.fhir.model.r4.Patient(
                id = "p-other",
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            type =
                                dev.ohs.fhir.model.r4.CodeableConcept(
                                    coding =
                                        listOf(
                                            dev.ohs.fhir.model.r4.Coding(
                                                code =
                                                    dev.ohs.fhir.model.r4
                                                        .Code(value = "OTHER"),
                                            ),
                                            dev.ohs.fhir.model.r4.Coding(
                                                code = null,
                                            ),
                                        ),
                                ),
                            value = null,
                        ),
                        dev.ohs.fhir.model.r4.Identifier(
                            type = null,
                            value =
                                dev.ohs.fhir.model.r4
                                    .String(value = "FALLBACK-VAL"),
                        ),
                    ),
            )
        assertEquals("FALLBACK-VAL", patOtherCodeFallback.mrn)

        val patNoIdVal =
            dev.ohs.fhir.model.r4.Patient(
                id = "p-empty-id",
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            type = null,
                            value = null,
                        ),
                    ),
            )
        assertEquals("", patNoIdVal.mrn)

        // Localized unknown name variants: es, ja, zh, he, iw
        assertEquals("לא ידוע", getLocalizedUnknownName("iw"))
        assertEquals("לא ידוע", getLocalizedUnknownName("he"))
        assertEquals("Desconocido", getLocalizedUnknownName("es"))
        assertEquals("不明", getLocalizedUnknownName("ja"))
        assertEquals("未知", getLocalizedUnknownName("zh"))

        // Format localized birth date when birthDate is null or birthDate.value is null
        val patNoBirthDate =
            dev.ohs.fhir.model.r4
                .Patient(id = "p-nbd")
        assertEquals(null, patNoBirthDate.formatLocalizedBirthDate().getOrThrow())
        val patNullBirthDateVal =
            dev.ohs.fhir.model.r4
                .Patient(
                    id = "p-nbdv",
                    birthDate =
                        dev.ohs.fhir.model.r4
                            .Date(value = null),
                )
        assertEquals("", patNullBirthDateVal.customBirthDate)
        assertEquals(null, patNullBirthDateVal.formatLocalizedBirthDate().getOrThrow())

        // HumanName familyName and givenName when String value is null
        val nullNameVals =
            dev.ohs.fhir.model.r4.HumanName(
                family =
                    dev.ohs.fhir.model.r4
                        .String(value = null),
                given =
                    listOf(
                        dev.ohs.fhir.model.r4
                            .String(value = null),
                    ),
            )
        assertEquals("Unknown", nullNameVals.familyName)
        assertEquals("Unknown", nullNameVals.givenName)

        // DocumentReference description null and blank branches
        val docNullDesc = createFhirDocumentReference(params1.copy(desc = null))
        kotlin.test.assertNotNull(docNullDesc.date)
        kotlin.test.assertNull(docNullDesc.description)
        val docBlankDesc = createFhirDocumentReference(params1.copy(desc = "   "))
        kotlin.test.assertNull(docBlankDesc.description)

        // Encounter encounterDate when period.start is null or period.start.value is null
        val encStartNull =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-sn",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code("AMB"),
                        ),
                period =
                    dev.ohs.fhir.model.r4
                        .Period(start = null),
            )
        assertEquals("", encStartNull.encounterDate)

        val encStartValNull =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-svn",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4
                        .Coding(
                            code =
                                dev.ohs.fhir.model.r4
                                    .Code("AMB"),
                        ),
                period =
                    dev.ohs.fhir.model.r4
                        .Period(
                            start =
                                dev.ohs.fhir.model.r4
                                    .DateTime(value = null),
                        ),
            )
        assertEquals("", encStartValNull.encounterDate)

        // createFhirPatient fallback via getOrElse with managingOrg and gender variants
        val fallbackPatOrg =
            createFhirPatient(
                id = "",
                firstName = "Fallback",
                lastName = "User",
                dob = LocalDate(1985, 5, 5),
                mrnValue = "FB-MRN",
                organizationId = "Organization/org-1",
                gender = "other",
            )
        assertEquals("Organization/org-1", fallbackPatOrg.managingOrganization?.reference?.value)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Other, fallbackPatOrg.gender?.value)

        val fallbackPatNoOrg =
            createFhirPatient(
                id = "",
                firstName = "Fallback",
                lastName = "User",
                dob = LocalDate(1985, 5, 5),
                mrnValue = "FB-MRN",
                organizationId = null,
                gender = "m",
            )
        kotlin.test.assertNull(fallbackPatNoOrg.managingOrganization)
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Male, fallbackPatNoOrg.gender?.value)

        val fallbackPatF = createFhirPatient("", "F", "U", LocalDate(1985, 5, 5), "MRN", null, "f")
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Female, fallbackPatF.gender?.value)

        val fallbackPatO = createFhirPatient("", "O", "U", LocalDate(1985, 5, 5), "MRN", null, "o")
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Other, fallbackPatO.gender?.value)

        val fallbackPatU = createFhirPatient("", "U", "U", LocalDate(1985, 5, 5), "MRN", null, "u")
        assertEquals(dev.ohs.fhir.model.r4.terminologies.AdministrativeGender.Unknown, fallbackPatU.gender?.value)

        // MRN coding with MR code but null id.value
        val patMrNullVal =
            dev.ohs.fhir.model.r4.Patient(
                id = "p-mr-null",
                identifier =
                    listOf(
                        dev.ohs.fhir.model.r4.Identifier(
                            type =
                                dev.ohs.fhir.model.r4.CodeableConcept(
                                    coding =
                                        listOf(
                                            dev.ohs.fhir.model.r4.Coding(
                                                code =
                                                    dev.ohs.fhir.model.r4
                                                        .Code(value = "MR"),
                                            ),
                                        ),
                                ),
                            value = null,
                        ),
                    ),
            )
        assertEquals("", patMrNullVal.mrn)
    }
}
