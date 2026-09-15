/**
 * @file FhirToDicomMapperTest.kt
 * Unit tests for mapping FHIR R4 clinical entities (Patient, Encounter, Practitioner) to standard DICOM elements.
 */
package io.healthplatform.chartcam.dicom

import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.Encounter
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.FhirDate
import com.google.fhir.model.r4.HumanName
import com.google.fhir.model.r4.Identifier
import com.google.fhir.model.r4.Patient
import com.google.fhir.model.r4.Practitioner
import com.google.fhir.model.r4.String
import com.google.fhir.model.r4.terminologies.AdministrativeGender
import io.healthplatform.chartcam.models.createFhirEncounter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for FhirToDicomMapper covering demographic extraction, UID generation, and PDF/Image encapsulation.
 */
class FhirToDicomMapperTest {
    /**
     * Helper to create a standard sample FHIR Patient.
     * @return A populated Patient resource.
     */
    private fun createSamplePatient(gender: AdministrativeGender = AdministrativeGender.Female): Patient =
        Patient
            .Builder()
            .apply {
                id = "pat-123"
                name.add(
                    HumanName
                        .Builder()
                        .apply {
                            family = String.Builder().apply { value = "Williams" }
                            given.add(String.Builder().apply { value = "Clara" })
                        },
                )
                identifier.add(
                    Identifier
                        .Builder()
                        .apply {
                            value = String.Builder().apply { value = "MRN-998877" }
                        },
                )
                birthDate =
                    Date
                        .Builder()
                        .apply {
                            value = FhirDate.fromString("1988-04-25")
                        }
                this.gender = Enumeration(value = gender)
            }.build()

    /**
     * Helper to create a standard sample FHIR Encounter.
     * @return A populated Encounter resource.
     */
    private fun createSampleEncounter(): Encounter =
        createFhirEncounter(
            id = "enc-555",
            patientId = "Patient/pat-123",
            practitionerId = "Practitioner/prac-1",
            dateStr = "2026-09-11T10:15:30Z",
        )

    /**
     * Helper to create a standard sample FHIR Practitioner.
     * @return A populated Practitioner resource.
     */
    private fun createSamplePractitioner(): Practitioner =
        Practitioner
            .Builder()
            .apply {
                id = "prac-1"
                name.add(
                    HumanName
                        .Builder()
                        .apply {
                            family = String.Builder().apply { value = "Fauci" }
                            given.add(String.Builder().apply { value = "Anthony" })
                        },
                )
            }.build()

    /**
     * Tests standard demographic mapping from FHIR Patient and Encounter.
     */
    @Test
    fun testCommonElementsMapping() {
        val patient = createSamplePatient()
        val encounter = createSampleEncounter()
        val practitioner = createSamplePractitioner()

        val elements = FhirToDicomMapper.buildCommonElements(patient, encounter, practitioner, anonymize = false)
        val elemMap = elements.associateBy { it.tag }

        val nameElem = elemMap[DicomTag.PATIENT_NAME]
        assertEquals("Williams^Clara", nameElem?.value?.decodeToString()?.trim())

        val idElem = elemMap[DicomTag.PATIENT_ID]
        assertEquals("MRN-998877", idElem?.value?.decodeToString()?.trim())

        val birthElem = elemMap[DicomTag.PATIENT_BIRTH_DATE]
        assertEquals("19880425", birthElem?.value?.decodeToString()?.trim())

        val sexElem = elemMap[DicomTag.PATIENT_SEX]
        assertEquals("F", sexElem?.value?.decodeToString()?.trim())

        val docElem = elemMap[DicomTag.REFERRING_PHYSICIAN_NAME]
        assertEquals("Fauci^Anthony", docElem?.value?.decodeToString()?.trim())

        val studyUid = elemMap[DicomTag.STUDY_INSTANCE_UID]?.value?.decodeToString()?.trim()
        assertTrue(studyUid?.startsWith(DicomTag.UID_CHARTCAM_IMPLEMENTATION_CLASS) == true)
    }

    /**
     * Tests demographic edge cases including missing names, fallbacks, and male/other genders.
     */
    @Test
    fun testDemographicEdgeCases() {
        // Male patient with family only and no MRN identifier
        val malePatient =
            Patient
                .Builder()
                .apply {
                    id = "pat-male-42"
                    name.add(
                        HumanName
                            .Builder()
                            .apply {
                                family = String.Builder().apply { value = "Bond" }
                            },
                    )
                    gender = Enumeration(value = AdministrativeGender.Male)
                }.build()

        val maleElements = FhirToDicomMapper.buildCommonElements(malePatient, null, null)
        val maleMap = maleElements.associateBy { it.tag }
        assertEquals("Bond^", maleMap[DicomTag.PATIENT_NAME]?.value?.decodeToString()?.trim())
        assertEquals("pat-male-42", maleMap[DicomTag.PATIENT_ID]?.value?.decodeToString()?.trim())
        assertEquals("M", maleMap[DicomTag.PATIENT_SEX]?.value?.decodeToString()?.trim())
        assertNull(maleMap[DicomTag.PATIENT_BIRTH_DATE])
        assertEquals("ChartCam^Provider", maleMap[DicomTag.REFERRING_PHYSICIAN_NAME]?.value?.decodeToString()?.trim())

        // Patient with given name only and other gender
        val givenOnlyPatient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName
                            .Builder()
                            .apply {
                                given.add(String.Builder().apply { value = "Cher" })
                            },
                    )
                    gender = Enumeration(value = AdministrativeGender.Other)
                }.build()
        val givenMap = FhirToDicomMapper.buildCommonElements(givenOnlyPatient, null, null).associateBy { it.tag }
        assertEquals("^Cher", givenMap[DicomTag.PATIENT_NAME]?.value?.decodeToString()?.trim())
        assertEquals("NO_MRN", givenMap[DicomTag.PATIENT_ID]?.value?.decodeToString()?.trim())
        assertEquals("O", givenMap[DicomTag.PATIENT_SEX]?.value?.decodeToString()?.trim())

        // Patient with empty elements inside Name and Identifier
        val blankPatient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName
                            .Builder()
                            .apply {
                                family = String.Builder()
                                given.add(String.Builder())
                            },
                    )
                    identifier.add(Identifier.Builder().apply { value = String.Builder() })
                    birthDate = Date.Builder()
                }.build()
        val blankMap = FhirToDicomMapper.buildCommonElements(blankPatient, null, null).associateBy { it.tag }
        assertEquals("UNKNOWN", blankMap[DicomTag.PATIENT_NAME]?.value?.decodeToString()?.trim())
        assertEquals("NO_MRN", blankMap[DicomTag.PATIENT_ID]?.value?.decodeToString()?.trim())
        assertNull(blankMap[DicomTag.PATIENT_BIRTH_DATE])

        // Practitioner with blank name
        val blankPrac =
            Practitioner
                .Builder()
                .apply {
                    name.add(
                        HumanName
                            .Builder()
                            .apply {
                                family = String.Builder().apply { value = "Smith" }
                                given.add(String.Builder().apply { value = "John" })
                                given.add(String.Builder().apply { value = "A." })
                            },
                    )
                }.build()
        val pracMap = FhirToDicomMapper.buildCommonElements(null, null, blankPrac).associateBy { it.tag }
        assertEquals("Smith^John A.", pracMap[DicomTag.REFERRING_PHYSICIAN_NAME]?.value?.decodeToString()?.trim())

        // Patient with empty name list and no id and no gender
        val noIdNoGenderPatient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName
                            .Builder()
                            .apply {
                                family = String.Builder().apply { value = "AnonymousFamily" }
                            },
                    )
                }.build()
        val noIdNoGenderMap = FhirToDicomMapper.buildCommonElements(noIdNoGenderPatient, null, null).associateBy { it.tag }
        assertEquals("NO_MRN", noIdNoGenderMap[DicomTag.PATIENT_ID]?.value?.decodeToString()?.trim())
        assertEquals("O", noIdNoGenderMap[DicomTag.PATIENT_SEX]?.value?.decodeToString()?.trim())

        // Practitioner with empty name list
        val emptyPrac = Practitioner.Builder().build()
        val emptyPracMap = FhirToDicomMapper.buildCommonElements(null, null, emptyPrac).associateBy { it.tag }
        assertEquals("ChartCam^Provider", emptyPracMap[DicomTag.REFERRING_PHYSICIAN_NAME]?.value?.decodeToString()?.trim())

        // Practitioner with given name only (family = null)
        val givenOnlyPrac =
            Practitioner
                .Builder()
                .apply {
                    name.add(
                        HumanName
                            .Builder()
                            .apply {
                                given.add(String.Builder().apply { value = "Alexander" })
                            },
                    )
                }.build()
        val givenOnlyPracMap = FhirToDicomMapper.buildCommonElements(null, null, givenOnlyPrac).associateBy { it.tag }
        assertEquals("^Alexander", givenOnlyPracMap[DicomTag.REFERRING_PHYSICIAN_NAME]?.value?.decodeToString()?.trim())

        // Encounter without period
        val encNoPeriod =
            Encounter
                .Builder(
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` =
                        com.google.fhir.model.r4.Coding
                            .Builder()
                            .apply {
                                code =
                                    com.google.fhir.model.r4.Code
                                        .Builder()
                                        .apply { value = "AMB" }
                            },
                ).build()
        val noPeriodMap = FhirToDicomMapper.buildCommonElements(null, encNoPeriod, null).associateBy { it.tag }
        assertNull(noPeriodMap[DicomTag.STUDY_DATE])

        // Encounter with period but null start
        val encNullStart =
            Encounter
                .Builder(
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` =
                        com.google.fhir.model.r4.Coding
                            .Builder()
                            .apply {
                                code =
                                    com.google.fhir.model.r4.Code
                                        .Builder()
                                        .apply { value = "AMB" }
                            },
                ).apply {
                    period =
                        com.google.fhir.model.r4.Period
                            .Builder()
                }.build()
        val nullStartMap = FhirToDicomMapper.buildCommonElements(null, encNullStart, null).associateBy { it.tag }
        assertNull(nullStartMap[DicomTag.STUDY_DATE])

        // Encapsulated PDF and Image with encounter without id
        val expectedHash = kotlin.math.abs("ENC_DEFAULT".hashCode()).toString()
        val pdfEncNoId = FhirToDicomMapper.createEncapsulatedPdfDicom(byteArrayOf(1, 2), encounter = encNoPeriod).getOrThrow()
        assertTrue(pdfEncNoId.decodeToString().contains(expectedHash))

        val imgEncNoId = FhirToDicomMapper.createVisibleLightImageDicom(byteArrayOf(1, 2), encounter = encNoPeriod).getOrThrow()
        assertTrue(imgEncNoId.decodeToString().contains(expectedHash))
    }

    /**
     * Tests HIPAA anonymization scrubbing demographic identifiers.
     */
    @Test
    fun testAnonymization() {
        val patient = createSamplePatient()
        val encounter = createSampleEncounter()

        val elements = FhirToDicomMapper.buildCommonElements(patient, encounter, practitioner = null, anonymize = true)
        val elemMap = elements.associateBy { it.tag }

        val nameElem = elemMap[DicomTag.PATIENT_NAME]?.value?.decodeToString()?.trim()
        assertEquals("ANONYMOUS^PATIENT", nameElem)

        val idElem = elemMap[DicomTag.PATIENT_ID]?.value?.decodeToString()?.trim()
        assertTrue(idElem?.startsWith("ANON-") == true)

        val birthElem = elemMap[DicomTag.PATIENT_BIRTH_DATE]?.value?.decodeToString()?.trim()
        assertEquals("19000101", birthElem)

        val sexElem = elemMap[DicomTag.PATIENT_SEX]?.value?.decodeToString()?.trim()
        assertEquals("O", sexElem)

        // Anonymization with null patient id
        val noIdPatient = Patient.Builder().build()
        val anonNoId = FhirToDicomMapper.buildCommonElements(noIdPatient, null, null, anonymize = true).associateBy { it.tag }
        assertEquals("ANONYMOUS", anonNoId[DicomTag.PATIENT_ID]?.value?.decodeToString()?.trim())
    }

    /**
     * Tests creation of compliant DICOM Part 10 byte stream for Encapsulated PDF.
     */
    @Test
    fun testCreateEncapsulatedPdfDicom() {
        val pdfBytes = "%PDF-1.4 sample clinical content".encodeToByteArray()
        val dcmBytes =
            FhirToDicomMapper
                .createEncapsulatedPdfDicom(
                    pdfBytes = pdfBytes,
                    title = "Cardiology Discharge Summary",
                    patient = createSamplePatient(),
                    encounter = createSampleEncounter(),
                    practitioner = createSamplePractitioner(),
                    anonymize = false,
                ).getOrThrow()

        assertTrue(dcmBytes.size > pdfBytes.size)
        val stringContent = dcmBytes.decodeToString()
        assertTrue(stringContent.contains("DICM"))
        assertTrue(stringContent.contains(DicomTag.UID_SOP_CLASS_ENCAPSULATED_PDF))
        assertTrue(stringContent.contains("application/pdf"))
        assertTrue(stringContent.contains("DOC"))
        assertTrue(stringContent.contains("Cardiology Discharge Summary"))
    }

    /**
     * Tests creation of compliant DICOM Part 10 byte stream for Visible Light Photography.
     */
    @Test
    fun testCreateVisibleLightImageDicom() {
        // Valid minimal JPEG with SOF0 header (height = 256, width = 512)
        val jpegWithDims =
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(), // SOI
                0xFF.toByte(),
                0xC0.toByte(), // SOF0
                0x00.toByte(),
                0x11.toByte(), // Segment Length = 17
                0x08.toByte(), // Precision
                0x01.toByte(),
                0x00.toByte(), // Height = 256
                0x02.toByte(),
                0x00.toByte(), // Width = 512
                0x03.toByte(),
                0x01.toByte(),
                0x11.toByte(),
                0x00.toByte(), // Components
                0x02.toByte(),
                0x11.toByte(),
                0x01.toByte(),
                0x03.toByte(),
                0x11.toByte(),
                0x01.toByte(),
                0xFF.toByte(),
                0xD9.toByte(), // EOI
            )

        val dcmBytes =
            FhirToDicomMapper
                .createVisibleLightImageDicom(
                    imageBytes = jpegWithDims,
                    imageId = "PHOTO_SKIN_LESION_01",
                    patient = createSamplePatient(),
                    encounter = createSampleEncounter(),
                    practitioner = null,
                    anonymize = false,
                ).getOrThrow()

        assertTrue(dcmBytes.size > jpegWithDims.size)
        val stringContent = dcmBytes.decodeToString()
        assertTrue(stringContent.contains("DICM"))
        assertTrue(stringContent.contains(DicomTag.UID_SOP_CLASS_VL_PHOTOGRAPHIC_IMAGE))
        assertTrue(stringContent.contains("XC"))
        assertTrue(stringContent.contains(DicomTag.UID_JPEG_BASELINE))
        assertTrue(stringContent.contains("YBR_FULL_422"))
    }

    /**
     * Tests invocation of createEncapsulatedPdfDicom and createVisibleLightImageDicom with default parameters.
     */
    @Test
    fun testDefaultArguments() {
        val pdfBytes = "%PDF-1.4 payload".encodeToByteArray()
        val pdfDcm = FhirToDicomMapper.createEncapsulatedPdfDicom(pdfBytes).getOrThrow()
        assertTrue(pdfDcm.decodeToString().contains("Clinical Encounter Report"))

        val imageBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val imgDcm = FhirToDicomMapper.createVisibleLightImageDicom(imageBytes).getOrThrow()
        assertTrue(imgDcm.decodeToString().contains("XC"))

        // Odd length image triggers pad byte branch
        val oddBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val oddDcm = FhirToDicomMapper.createVisibleLightImageDicom(oddBytes).getOrThrow()
        assertTrue(oddDcm.isNotEmpty())

        // Female patient
        val femalePat = createSamplePatient(AdministrativeGender.Female)
        val femaleDcm = FhirToDicomMapper.createVisibleLightImageDicom(imageBytes, patient = femalePat).getOrThrow()
        assertTrue(femaleDcm.isNotEmpty())

        // Other gender patient
        val otherPat = createSamplePatient(AdministrativeGender.Other)
        val otherDcm = FhirToDicomMapper.createVisibleLightImageDicom(imageBytes, patient = otherPat).getOrThrow()
        assertTrue(otherDcm.isNotEmpty())

        // Unknown gender patient
        val unknownPat = createSamplePatient(AdministrativeGender.Unknown)
        val unknownDcm = FhirToDicomMapper.createVisibleLightImageDicom(imageBytes, patient = unknownPat).getOrThrow()
        assertTrue(unknownDcm.isNotEmpty())

        // 1-byte image payload (covers size < 2 branch)
        val singleByteDcm = FhirToDicomMapper.createVisibleLightImageDicom(byteArrayOf(0x42)).getOrThrow()
        assertTrue(singleByteDcm.isNotEmpty())

        // 2-byte payload starting with 0xFF but not 0xD8
        val notSoiDcm = FhirToDicomMapper.createVisibleLightImageDicom(byteArrayOf(0xFF.toByte(), 0x00)).getOrThrow()
        assertTrue(notSoiDcm.isNotEmpty())

        // Encounter with null id
        val noIdEncounter =
            createFhirEncounter(
                id = null,
                patientId = "pat-123",
                practitionerId = "prac-1",
                dateStr = "2026-09-01",
            )
        val noIdDcm = FhirToDicomMapper.createVisibleLightImageDicom(imageBytes, encounter = noIdEncounter).getOrThrow()
        assertTrue(noIdDcm.isNotEmpty())

        // Empty image bytes failure
        assertTrue(FhirToDicomMapper.createVisibleLightImageDicom(ByteArray(0)).isFailure)
    }

    /**
     * Tests helper extraction methods directly covering every null and empty branch.
     */
    @Test
    fun testHelperMethodsDirectly() {
        // joinNames
        val namesList =
            listOf(
                String.Builder().apply { value = "Alpha" }.build(),
                String.Builder().apply { value = "" }.build(), // empty string branch
                String.Builder().build(), // null string value branch
                String.Builder().apply { value = "Beta" }.build(),
            )
        val joined = FhirToDicomMapper.joinNames(namesList)
        assertEquals("Alpha Beta", joined)

        // extractMrn
        val p1 = Patient.Builder().apply { id = "p1" }.build()
        assertEquals("p1", FhirToDicomMapper.extractMrn(p1))

        val p2 = Patient.Builder().build()
        assertEquals("NO_MRN", FhirToDicomMapper.extractMrn(p2))

        val p3 =
            Patient
                .Builder()
                .apply {
                    identifier.add(Identifier.Builder().apply { value = String.Builder() })
                }.build()
        assertEquals("NO_MRN", FhirToDicomMapper.extractMrn(p3))

        val p4 =
            Patient
                .Builder()
                .apply {
                    identifier.add(Identifier.Builder().apply { value = String.Builder().apply { value = "MRN-1" } })
                }.build()
        assertEquals("MRN-1", FhirToDicomMapper.extractMrn(p4))

        val p5 =
            Patient
                .Builder()
                .apply {
                    identifier.add(Identifier.Builder())
                    identifier.add(Identifier.Builder().apply { value = String.Builder() })
                    identifier.add(Identifier.Builder().apply { value = String.Builder().apply { value = "MRN-SUCCESS" } })
                }.build()
        assertEquals("MRN-SUCCESS", FhirToDicomMapper.extractMrn(p5))

        // extractBirthDate
        assertEquals("", FhirToDicomMapper.extractBirthDate(p2))
        val pBirthNull = Patient.Builder().apply { birthDate = Date.Builder() }.build()
        assertEquals("", FhirToDicomMapper.extractBirthDate(pBirthNull))
        val pBirthValid = Patient.Builder().apply { birthDate = Date.Builder().apply { value = FhirDate.fromString("2020-01-01") } }.build()
        assertEquals("2020-01-01", FhirToDicomMapper.extractBirthDate(pBirthValid))

        // extractGender
        assertEquals("O", FhirToDicomMapper.extractGender(p2))
        val pGenderNull = Patient.Builder().apply { gender = Enumeration<AdministrativeGender>() }.build()
        assertEquals("O", FhirToDicomMapper.extractGender(pGenderNull))

        val pMaleCreated =
            io.healthplatform.chartcam.models.createFhirPatient(
                "p-m",
                "John",
                "Doe",
                kotlinx.datetime.LocalDate(1990, 1, 1),
                "MRN-M",
                gender = "male",
            )
        assertEquals("M", FhirToDicomMapper.extractGender(pMaleCreated))

        val pFemaleCreated =
            io.healthplatform.chartcam.models.createFhirPatient(
                "p-f",
                "Jane",
                "Doe",
                kotlinx.datetime.LocalDate(1992, 2, 2),
                "MRN-F",
                gender = "female",
            )
        assertEquals("F", FhirToDicomMapper.extractGender(pFemaleCreated))

        val pOtherCreated =
            io.healthplatform.chartcam.models.createFhirPatient(
                "p-o",
                "Alex",
                "Doe",
                kotlinx.datetime.LocalDate(1995, 5, 5),
                "MRN-O",
                gender = "other",
            )
        assertEquals("O", FhirToDicomMapper.extractGender(pOtherCreated))

        // extractPeriodStart
        assertEquals("", FhirToDicomMapper.extractPeriodStart(null))
        val encNullPeriod =
            Encounter
                .Builder(
                    status = Enumeration(value = Encounter.EncounterStatus.Finished),
                    `class` =
                        com.google.fhir.model.r4.Coding
                            .Builder()
                            .apply {
                                code =
                                    com.google.fhir.model.r4.Code
                                        .Builder()
                                        .apply { value = "AMB" }
                            },
                ).build()
        assertEquals("", FhirToDicomMapper.extractPeriodStart(encNullPeriod))
        val encNullStartVal =
            encNullPeriod
                .toBuilder()
                .apply {
                    period =
                        com.google.fhir.model.r4.Period
                            .Builder()
                            .apply {
                                start =
                                    com.google.fhir.model.r4.DateTime
                                        .Builder()
                            }
                }.build()
        assertEquals("", FhirToDicomMapper.extractPeriodStart(encNullStartVal))

        // extractFamilyName
        assertEquals("", FhirToDicomMapper.extractFamilyName(null))
        val hNameNullFam = HumanName.Builder().build()
        assertEquals("", FhirToDicomMapper.extractFamilyName(hNameNullFam))
        val hNameNullVal = HumanName.Builder().apply { family = String.Builder() }.build()
        assertEquals("", FhirToDicomMapper.extractFamilyName(hNameNullVal))
        val hNameValid = HumanName.Builder().apply { family = String.Builder().apply { value = "Curie" } }.build()
        assertEquals("Curie", FhirToDicomMapper.extractFamilyName(hNameValid))

        // extractGivenName
        assertEquals("", FhirToDicomMapper.extractGivenName(null))
    }

    /**
     * Tests fallback behavior when patient is completely null.
     */
    @Test
    fun testNullPatientFallback() {
        val elements = FhirToDicomMapper.buildCommonElements(patient = null, encounter = null, practitioner = null)
        val elemMap = elements.associateBy { it.tag }
        assertEquals("ANONYMOUS^PATIENT", elemMap[DicomTag.PATIENT_NAME]?.value?.decodeToString()?.trim())
        assertEquals("ANONYMOUS", elemMap[DicomTag.PATIENT_ID]?.value?.decodeToString()?.trim())
    }
}
