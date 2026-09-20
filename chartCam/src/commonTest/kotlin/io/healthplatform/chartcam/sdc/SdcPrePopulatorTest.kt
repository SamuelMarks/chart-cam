/**
 * @file SdcPrePopulatorTest.kt
 * Unit tests for SdcPrePopulator verifying form initialization from local patient data.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test suite for [SdcPrePopulator].
 */
class SdcPrePopulatorTest {
    /**
     * Verifies demographic pre-population of name, birthdate, and gender fields.
     */
    @Test
    fun testPrePopulateDemographics() {
        val patient =
            Patient(
                id = "p-prepop-1",
                name =
                    listOf(
                        HumanName(
                            family = FhirString(value = "Doe"),
                            given = listOf(FhirString(value = "Jane")),
                        ),
                    ),
                gender = Enumeration(value = AdministrativeGender.Female),
                birthDate = Date(value = FhirDate.fromString("1995-11-23")),
            )

        val questionnaire =
            Questionnaire(
                id = "q-intake",
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = FhirString(value = "patient_name"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        Questionnaire.Item(
                            linkId = FhirString(value = "patient_dob"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                        ),
                        Questionnaire.Item(
                            linkId = FhirString(value = "patient_gender"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                        ),
                        Questionnaire.Item(
                            linkId = FhirString(value = "unrelated_question"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value.String(
                                                FhirString(value = "DefaultAnswer"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val result = SdcPrePopulator.populateCatching(questionnaire, patient)
        assertTrue(result.isSuccess)
        val answers = result.getOrThrow()

        assertEquals("Jane Doe", answers["patient_name"])
        assertEquals("1995-11-23", answers["patient_dob"])
        assertEquals("Female", answers["patient_gender"])
        assertEquals("DefaultAnswer", answers["unrelated_question"])
    }

    /**
     * Verifies initial value variants (Boolean, Decimal, Integer, Date, DateTime, Uri, Coding) and null/blank linkIds.
     */
    @Test
    fun testExhaustiveInitialValuesAndDemographicBranches() {
        val patientEmpty = Patient(id = "p-empty")
        val patientWithSexAndFullname =
            Patient(
                id = "p-full",
                name = listOf(HumanName(given = listOf(FhirString(value = "John")))), // family is null
                gender = Enumeration(value = AdministrativeGender.Male),
                birthDate = Date(value = FhirDate.fromString("2000-01-01")),
            )

        val questionnaire =
            Questionnaire(
                id = "q-exhaustive",
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        // LinkId with sex
                        Questionnaire.Item(
                            linkId = FhirString(value = "patient_sex"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                        ),
                        // LinkId with fullname
                        Questionnaire.Item(
                            linkId = FhirString(value = "fullname"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // LinkId with name only
                        Questionnaire.Item(
                            linkId = FhirString(value = "name"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // LinkId with birth
                        Questionnaire.Item(
                            linkId = FhirString(value = "birth_date"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                        ),
                        // Initial with Boolean
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_bool"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .Boolean(
                                                    dev.ohs.fhir.model.r4
                                                        .Boolean(value = true),
                                                ),
                                    ),
                                ),
                        ),
                        // Initial with Decimal
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_dec"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value.Decimal(
                                                dev.ohs.fhir.model.r4
                                                    .Decimal(
                                                        value =
                                                            dev.ohs.fhir.model.r4.FhirDecimal
                                                                .fromString("4.5"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                        // Initial with Integer
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_int"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .Integer(
                                                    dev.ohs.fhir.model.r4
                                                        .Integer(value = 10),
                                                ),
                                    ),
                                ),
                        ),
                        // Initial with Date
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_date"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .Date(Date(value = FhirDate.fromString("2026-09-18"))),
                                    ),
                                ),
                        ),
                        // Initial with DateTime
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_datetime"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value.DateTime(
                                                dev.ohs.fhir.model.r4.DateTime(
                                                    value =
                                                        dev.ohs.fhir.model.r4.FhirDateTime
                                                            .fromString("2026-09-18T12:00:00Z"),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        // Initial with Uri
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_uri"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Url),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .Uri(
                                                    dev.ohs.fhir.model.r4
                                                        .Uri(value = "https://health.org"),
                                                ),
                                    ),
                                ),
                        ),
                        // Initial with Coding
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_coding"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value.Coding(
                                                dev.ohs.fhir.model.r4
                                                    .Coding(
                                                        code =
                                                            dev.ohs.fhir.model.r4
                                                                .Code(value = "CODE1"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                        // Initial with unsupported type (e.g. Attachment)
                        Questionnaire.Item(
                            linkId = FhirString(value = "init_attachment"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .Attachment(
                                                    dev.ohs.fhir.model.r4
                                                        .Attachment(),
                                                ),
                                    ),
                                ),
                        ),
                        // Item with blank linkId
                        Questionnaire.Item(
                            linkId = FhirString(value = "   "),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // Item with null linkId.value and null linkId.id
                        Questionnaire.Item(
                            linkId = FhirString(id = null, value = null),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        // Item with null linkId.value but linkId.id set
                        Questionnaire.Item(
                            linkId = FhirString(id = "fallback_id", value = null),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .String(FhirString(value = "id_val")),
                                    ),
                                ),
                        ),
                        // Null value in Date initial
                        Questionnaire.Item(
                            linkId = FhirString(value = "null_init_date"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .Date(Date(value = null)),
                                    ),
                                ),
                        ),
                        // Null value in DateTime initial
                        Questionnaire.Item(
                            linkId = FhirString(value = "null_init_datetime"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .DateTime(
                                                    dev.ohs.fhir.model.r4
                                                        .DateTime(value = null),
                                                ),
                                    ),
                                ),
                        ),
                        // Null code in Coding initial
                        Questionnaire.Item(
                            linkId = FhirString(value = "null_init_coding1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value
                                                .Coding(
                                                    dev.ohs.fhir.model.r4
                                                        .Coding(code = null),
                                                ),
                                    ),
                                ),
                        ),
                        // Null code value in Coding initial
                        Questionnaire.Item(
                            linkId = FhirString(value = "null_init_coding2"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            initial =
                                listOf(
                                    Questionnaire.Item.Initial(
                                        value =
                                            Questionnaire.Item.Initial.Value.Coding(
                                                dev.ohs.fhir.model.r4
                                                    .Coding(
                                                        code =
                                                            dev.ohs.fhir.model.r4
                                                                .Code(value = null),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val res = SdcPrePopulator.populateCatching(questionnaire, patientWithSexAndFullname).getOrThrow()
        assertEquals("Male", res["patient_sex"])
        assertEquals("John", res["fullname"])
        assertEquals("John", res["name"])
        assertEquals("2000-01-01", res["birth_date"])
        assertEquals(true, res["init_bool"])
        assertEquals("4.5", res["init_dec"].toString())
        assertEquals(10, res["init_int"])
        assertEquals("2026-09-18", res["init_date"])
        assertEquals("2026-09-18T12:00:00Z", res["init_datetime"])
        assertEquals("https://health.org", res["init_uri"])
        assertEquals("CODE1", res["init_coding"])
        assertEquals("id_val", res["fallback_id"])
        kotlin.test.assertNull(res["init_attachment"])
        kotlin.test.assertNull(res["null_init_date"])
        kotlin.test.assertNull(res["null_init_datetime"])
        kotlin.test.assertNull(res["null_init_coding1"])
        kotlin.test.assertNull(res["null_init_coding2"])

        // Test with empty patient
        val emptyRes = SdcPrePopulator.populateCatching(questionnaire, patientEmpty).getOrThrow()
        kotlin.test.assertNull(emptyRes["patient_sex"])
        kotlin.test.assertNull(emptyRes["fullname"])
        kotlin.test.assertNull(emptyRes["birth_date"])

        // Test with patient having blank name and null enum values
        val patientWithBlankNamesAndNulls =
            Patient(
                id = "p-nulls",
                name = listOf(HumanName(given = listOf(FhirString(value = null)), family = FhirString(value = null))),
                gender = Enumeration(value = null),
                birthDate = Date(value = null),
            )
        val nullsRes = SdcPrePopulator.populateCatching(questionnaire, patientWithBlankNamesAndNulls).getOrThrow()
        kotlin.test.assertNull(nullsRes["patient_sex"])
        kotlin.test.assertNull(nullsRes["fullname"])
        kotlin.test.assertNull(nullsRes["birth_date"])
    }
}
