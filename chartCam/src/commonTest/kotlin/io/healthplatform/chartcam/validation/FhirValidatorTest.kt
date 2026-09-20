/**
 * @file FhirValidatorTest.kt
 * Contains tests for [FhirValidator].
 */
package io.healthplatform.chartcam.validation

import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.OperationOutcome
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.String
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [FhirValidator] profiles including structural FHIR rules.
 */
class FhirValidatorTest {
    /**
     * Verifies that Patient validation correctly rejects patients missing required profile fields.
     */
    @Test
    fun testPatientValidationProfile() {
        val validPatient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName.Builder().apply {
                            given.add(String.Builder().apply { value = "John" })
                            family = String.Builder().apply { value = "Doe" }
                        },
                    )
                    identifier.add(
                        Identifier.Builder().apply {
                            value = String.Builder().apply { value = "MRN-1234" }
                        },
                    )
                }.build()

        assertTrue(FhirValidator.validate(validPatient).isSuccess, "Patient with given, family, and identifier should be valid")

        val missingIdentifierPatient =
            Patient
                .Builder()
                .apply {
                    name.add(
                        HumanName.Builder().apply {
                            given.add(String.Builder().apply { value = "John" })
                            family = String.Builder().apply { value = "Doe" }
                        },
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingIdentifierPatient).isFailure, "Patient missing identifier should be invalid")

        val missingNamePatient =
            Patient
                .Builder()
                .apply {
                    identifier.add(
                        Identifier.Builder().apply {
                            value = String.Builder().apply { value = "MRN-1234" }
                        },
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingNamePatient).isFailure, "Patient missing name should be invalid")
    }

    /**
     * Verifies that Questionnaire validation correctly rejects questionnaires missing required profile fields.
     */
    @Test
    fun testQuestionnaireValidationProfile() {
        val validQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "General Questionnaire" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "What is your age?" }
                            },
                    )
                }.build()

        assertTrue(FhirValidator.validate(validQuestionnaire).isSuccess, "Questionnaire with title and valid items should be valid")

        val missingTitleQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "What is your age?" }
                            },
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingTitleQuestionnaire).isFailure, "Questionnaire missing title should be invalid")

        val missingItemTextQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "General Questionnaire" }
                    item.add(
                        Questionnaire.Item.Builder(
                            linkId = String.Builder().apply { value = "item-1" },
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                        ),
                    )
                }.build()

        assertTrue(FhirValidator.validate(missingItemTextQuestionnaire).isFailure, "Questionnaire item missing text should be invalid")

        val duplicateLinkIdQuestionnaire =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "General Questionnaire" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "First item?" }
                            },
                    )
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                            ).apply {
                                text = String.Builder().apply { value = "Second item?" }
                            },
                    )
                }.build()

        assertTrue(FhirValidator.validate(duplicateLinkIdQuestionnaire).isFailure, "Questionnaire with duplicate linkIds should be invalid")
    }

    /**
     * Tests that a Questionnaire with an enableWhen condition pointing to a non-existent linkId fails validation.
     */
    @Test
    fun testDanglingEnableWhenReference() {
        val ewAnswer =
            Questionnaire.Item.EnableWhen.Answer.Boolean(
                dev.ohs.fhir.model.r4.Boolean
                    .Builder()
                    .apply { value = true }
                    .build(),
            )
        val qWithDangling =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    title = String.Builder().apply { value = "Dangling Condition Questionnaire" }
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = String.Builder().apply { value = "item-1" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                            ).apply {
                                text = String.Builder().apply { value = "Do you have symptoms?" }
                                enableWhen.add(
                                    Questionnaire.Item.EnableWhen.Builder(
                                        answer = ewAnswer,
                                        operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                                        question = String.Builder().apply { value = "non-existent-item" },
                                    ),
                                )
                            },
                    )
                }.build()

        val result = FhirValidator.validate(qWithDangling)
        assertTrue(result.isFailure, "Questionnaire with dangling enableWhen condition must fail validation")
        assertTrue(result.exceptionOrNull() is FhirValidationException.DanglingEnableWhenReferenceException)
    }

    /**
     * Verifies validateToOutcome returns OperationOutcome with issues for invalid resources and null for valid ones.
     */
    @Test
    fun testValidateToOutcome() {
        val invalidPatient = Patient()
        val outcomeResult = FhirValidator.validateToOutcome(invalidPatient)
        assertTrue(outcomeResult.isSuccess)
        val outcome = outcomeResult.getOrNull()
        kotlin.test.assertNotNull(outcome)
        assertTrue(outcome.issue.isNotEmpty())
        assertTrue(outcome.issue.any { it.expression.any { e -> e.value == "Patient.name" } })

        val validPatient =
            io.healthplatform.chartcam.models.createFhirPatient(
                id = "pat-outcome-valid",
                firstName = "Alice",
                lastName = "Smith",
                dob = kotlinx.datetime.LocalDate(1995, 5, 5),
                mrnValue = "MRN-OUTCOME-1",
            )
        val validOutcomeResult = FhirValidator.validateToOutcome(validPatient)
        assertTrue(validOutcomeResult.isSuccess)
        kotlin.test.assertNull(validOutcomeResult.getOrNull())
    }

    /**
     * Verifies OfflineValueSetValidator checks Coding system and permitted values offline.
     */
    @Test
    fun testOfflineValueSetValidatorCoding() {
        val validCoding =
            dev.ohs.fhir.model.r4.Coding(
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org"),
                code =
                    dev.ohs.fhir.model.r4
                        .Code(value = "8867-4"),
            )
        val outcomeValid = OfflineValueSetValidator.validateCoding(validCoding, setOf("8867-4", "85353-1"), "http://loinc.org")
        assertTrue(outcomeValid.isSuccess)
        kotlin.test.assertNull(outcomeValid.getOrNull())

        val invalidCode =
            dev.ohs.fhir.model.r4.Coding(
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://loinc.org"),
                code =
                    dev.ohs.fhir.model.r4
                        .Code(value = "9999-9"),
            )
        val outcomeInvalid = OfflineValueSetValidator.validateCoding(invalidCode, setOf("8867-4", "85353-1"), "http://loinc.org")
        assertTrue(outcomeInvalid.isSuccess)
        val outcome = outcomeInvalid.getOrNull()
        kotlin.test.assertNotNull(outcome)
        assertTrue(outcome.issue.any { it.code.value == OperationOutcome.IssueType.Code_Invalid })
    }

    /**
     * Verifies OfflineValueSetValidator checks QuestionnaireResponse choice options.
     */
    @Test
    fun testOfflineValueSetValidatorResponseOptions() {
        val q =
            Questionnaire(
                id = "q-choice-test",
                status = Enumeration(value = PublicationStatus.Active),
                title =
                    dev.ohs.fhir.model.r4
                        .String(value = "Triage"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId =
                                dev.ohs.fhir.model.r4
                                    .String(value = "severity"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            text =
                                dev.ohs.fhir.model.r4
                                    .String(value = "Severity"),
                            answerOption =
                                listOf(
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value.String(
                                                dev.ohs.fhir.model.r4
                                                    .String(value = "Mild"),
                                            ),
                                    ),
                                    Questionnaire.Item.AnswerOption(
                                        value =
                                            Questionnaire.Item.AnswerOption.Value.String(
                                                dev.ohs.fhir.model.r4
                                                    .String(value = "Severe"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val validResp =
            QuestionnaireResponse(
                id = "qr-valid-choice",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        QuestionnaireResponse.Item(
                            linkId =
                                dev.ohs.fhir.model.r4
                                    .String(value = "severity"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.String(
                                                dev.ohs.fhir.model.r4
                                                    .String(value = "Mild"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val validRes = OfflineValueSetValidator.validateResponseOptions(q, validResp)
        assertTrue(validRes.isSuccess)
        kotlin.test.assertNull(validRes.getOrNull())

        val invalidResp =
            QuestionnaireResponse(
                id = "qr-invalid-choice",
                status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
                item =
                    listOf(
                        QuestionnaireResponse.Item(
                            linkId =
                                dev.ohs.fhir.model.r4
                                    .String(value = "severity"),
                            answer =
                                listOf(
                                    QuestionnaireResponse.Item.Answer(
                                        value =
                                            QuestionnaireResponse.Item.Answer.Value.String(
                                                dev.ohs.fhir.model.r4
                                                    .String(value = "Catastrophic"),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val invalidRes = OfflineValueSetValidator.validateResponseOptions(q, invalidResp)
        assertTrue(invalidRes.isSuccess)
        val outcome = invalidRes.getOrNull()
        kotlin.test.assertNotNull(outcome)
        assertTrue(outcome.issue.any { it.diagnostics?.value?.contains("Catastrophic") == true })
    }

    /**
     * Verifies that validate and validateToOutcome handle non-profiled resource types like Observation.
     */
    @Test
    fun testValidateOtherResource() {
        val observation =
            dev.ohs.fhir.model.r4.Observation(
                id = "obs-1",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Observation.ObservationStatus.Final),
                code =
                    dev.ohs.fhir.model.r4
                        .CodeableConcept(),
            )
        val validateRes = FhirValidator.validate(observation)
        assertTrue(validateRes.isSuccess)

        val outcomeRes = FhirValidator.validateToOutcome(observation)
        assertTrue(outcomeRes.isSuccess)
        kotlin.test.assertNull(outcomeRes.getOrNull())
    }

    /**
     * Verifies that Patient validation covers missing given name, missing family name, and empty family name string.
     */
    @Test
    fun testValidatePatientDetailedErrors() {
        val missingGiven =
            Patient(
                name = listOf(HumanName(family = String(value = "Doe"))),
                identifier = listOf(Identifier(value = String(value = "MRN-1"))),
            )
        val resGiven = FhirValidator.validatePatient(missingGiven)
        assertTrue(resGiven.isFailure)
        assertTrue(resGiven.exceptionOrNull() is FhirValidationException.MissingRequiredFieldException)

        val missingFamily =
            Patient(
                name = listOf(HumanName(given = listOf(String(value = "John")), family = null)),
                identifier = listOf(Identifier(value = String(value = "MRN-1"))),
            )
        val resFamily = FhirValidator.validatePatient(missingFamily)
        assertTrue(resFamily.isFailure)
        assertTrue(resFamily.exceptionOrNull() is FhirValidationException.MissingRequiredFieldException)

        val emptyFamily =
            Patient(
                name = listOf(HumanName(given = listOf(String(value = "John")), family = String(value = ""))),
                identifier = listOf(Identifier(value = String(value = "MRN-1"))),
            )
        val resEmptyFamily = FhirValidator.validatePatient(emptyFamily)
        assertTrue(resEmptyFamily.isFailure)
    }

    /**
     * Verifies that QuestionnaireResponse validation covers null and present status values.
     */
    @Test
    fun testValidateQuestionnaireResponse() {
        val nullStatusResp = QuestionnaireResponse(status = Enumeration(value = null))
        val resNull = FhirValidator.validateQuestionnaireResponse(nullStatusResp)
        assertTrue(resNull.isFailure)
        assertTrue(resNull.exceptionOrNull() is FhirValidationException.MissingRequiredFieldException)

        val validResp =
            QuestionnaireResponse(status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
        val resValid = FhirValidator.validateQuestionnaireResponse(validResp)
        assertTrue(resValid.isSuccess)

        // validate dispatcher for QuestionnaireResponse
        val resDispatch = FhirValidator.validate(validResp)
        assertTrue(resDispatch.isSuccess)
    }

    /**
     * Verifies that Questionnaire validation covers empty items, missing linkId, empty choice options, and null enableWhen question.
     */
    @Test
    fun testValidateQuestionnaireDetailedErrors() {
        // Empty items list
        val emptyItemsQ =
            Questionnaire(
                title = String(value = "Title"),
                item = emptyList(),
                status = Enumeration(value = PublicationStatus.Active),
            )
        val resEmptyItems = FhirValidator.validateQuestionnaire(emptyItemsQ)
        assertTrue(resEmptyItems.isFailure)
        assertTrue(resEmptyItems.exceptionOrNull() is FhirValidationException.MissingRequiredFieldException)

        // Item missing linkId
        val missingLinkIdQ =
            Questionnaire(
                title = String(value = "Title"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = null),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            text = String(value = "Text"),
                        ),
                    ),
                status = Enumeration(value = PublicationStatus.Active),
            )
        val resMissingLinkId = FhirValidator.validateQuestionnaire(missingLinkIdQ)
        assertTrue(resMissingLinkId.isFailure)

        // Item is Choice but has no answer options
        val emptyChoiceQ =
            Questionnaire(
                title = String(value = "Title"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "choice-1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                            text = String(value = "Select one"),
                            answerOption = emptyList(),
                        ),
                    ),
                status = Enumeration(value = PublicationStatus.Active),
            )
        val resChoice = FhirValidator.validateQuestionnaire(emptyChoiceQ)
        assertTrue(resChoice.isFailure)
        assertTrue(resChoice.exceptionOrNull() is FhirValidationException.EmptyChoiceOptionsException)

        // enableWhen clause where target question value is null, or item linkId is null
        val nullEwTargetQ =
            Questionnaire(
                title = String(value = "Title"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                            text = String(value = "Q1"),
                            enableWhen =
                                listOf(
                                    Questionnaire.Item.EnableWhen(
                                        question = String(value = null),
                                        operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                                        answer =
                                            Questionnaire.Item.EnableWhen.Answer
                                                .Boolean(
                                                    dev.ohs.fhir.model.r4
                                                        .Boolean(value = true),
                                                ),
                                    ),
                                ),
                        ),
                    ),
                status = Enumeration(value = PublicationStatus.Active),
            )
        // linkId value is null, so validateSingleItem fails on item.linkId
        val resNullEw = FhirValidator.validateQuestionnaire(nullEwTargetQ)
        assertTrue(resNullEw.isFailure)

        // enableWhen clause pointing to existing question
        val validEwQ =
            Questionnaire(
                title = String(value = "Title"),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "q1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                            text = String(value = "Q1"),
                        ),
                        Questionnaire.Item(
                            linkId = String(value = "q2"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            text = String(value = "Q2"),
                            enableWhen =
                                listOf(
                                    Questionnaire.Item.EnableWhen(
                                        question = String(value = "q1"),
                                        operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                                        answer =
                                            Questionnaire.Item.EnableWhen.Answer
                                                .Boolean(
                                                    dev.ohs.fhir.model.r4
                                                        .Boolean(value = true),
                                                ),
                                    ),
                                    Questionnaire.Item.EnableWhen(
                                        question = String(value = null),
                                        operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                                        answer =
                                            Questionnaire.Item.EnableWhen.Answer
                                                .Boolean(
                                                    dev.ohs.fhir.model.r4
                                                        .Boolean(value = true),
                                                ),
                                    ),
                                ),
                        ),
                    ),
                status = Enumeration(value = PublicationStatus.Active),
            )
        val resValidEw = FhirValidator.validateQuestionnaire(validEwQ)
        assertTrue(resValidEw.isSuccess)
        // Patient with multiple name entries with null/empty families and valid family
        val pMultipleNames =
            Patient(
                name =
                    listOf(
                        HumanName(given = listOf(String(value = "John")), family = null),
                        HumanName(given = listOf(String(value = "John")), family = String(value = null)),
                        HumanName(given = listOf(String(value = "John")), family = String(value = "")),
                        HumanName(given = listOf(String(value = "John")), family = String(value = "Doe")),
                    ),
                identifier = listOf(Identifier(value = String(value = "MRN-1"))),
            )
        val resMultiNames = FhirValidator.validatePatient(pMultipleNames)
        assertTrue(resMultiNames.isSuccess)

        // Directly verify validateEnableWhenClauses with null source linkId
        val clauseItems =
            listOf(
                Questionnaire.Item(
                    linkId = String(value = null),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                    enableWhen =
                        listOf(
                            Questionnaire.Item.EnableWhen(
                                question = String(value = "missing-target"),
                                operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.Exists),
                                answer =
                                    Questionnaire.Item.EnableWhen.Answer
                                        .Boolean(
                                            dev.ohs.fhir.model.r4
                                                .Boolean(value = true),
                                        ),
                            ),
                        ),
                ),
            )
        val dangling = FhirValidator.validateEnableWhenClauses(clauseItems, setOf("q1"))
        kotlin.test.assertNotNull(dangling)
        assertTrue(dangling is FhirValidationException.DanglingEnableWhenReferenceException)
    }

    /**
     * Verifies validateToOutcome issues for all Patient, Questionnaire, and QuestionnaireResponse edge cases.
     */
    @Test
    fun testValidateToOutcomeComprehensive() {
        // Patient with missing given and missing family
        val pMissingFields =
            Patient(
                name =
                    listOf(
                        HumanName(given = emptyList(), family = null),
                        HumanName(given = emptyList(), family = String(value = null)),
                        HumanName(given = emptyList(), family = String(value = "")),
                    ),
                identifier = emptyList(),
            )
        val pOutcome = FhirValidator.validateToOutcome(pMissingFields).getOrNull()
        kotlin.test.assertNotNull(pOutcome)
        assertTrue(pOutcome.issue.any { it.expression.any { e -> e.value == "Patient.name.given" } })
        assertTrue(pOutcome.issue.any { it.expression.any { e -> e.value == "Patient.name.family" } })
        assertTrue(pOutcome.issue.any { it.expression.any { e -> e.value == "Patient.identifier" } })

        // Questionnaire with blank title, missing item linkId, id-only linkId, blank linkId, and duplicate linkId
        val qIssues =
            Questionnaire(
                title = String(value = "   "),
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = null),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        Questionnaire.Item(
                            linkId = String(id = "id-only"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        Questionnaire.Item(
                            linkId = String(value = "   "),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        Questionnaire.Item(
                            linkId = String(value = "item-dup"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                        Questionnaire.Item(
                            linkId = String(value = "item-dup"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )
        val qOutcome = FhirValidator.validateToOutcome(qIssues).getOrNull()
        kotlin.test.assertNotNull(qOutcome)
        assertEquals(5, qOutcome.issue.size)
        assertTrue(qOutcome.issue.any { it.expression.any { e -> e.value == "Questionnaire.title" } })
        assertTrue(qOutcome.issue.any { it.expression.any { e -> e.value == "Questionnaire.item.linkId" } })
        assertTrue(qOutcome.issue.any { it.expression.any { e -> e.value?.contains("item-dup") == true } })

        // Additional title variations for checkQuestionnaireOutcomeIssues branch coverage
        val qNullTitle =
            Questionnaire(
                title = null,
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "q1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )
        val outNullTitle = FhirValidator.validateToOutcome(qNullTitle).getOrNull()
        kotlin.test.assertNotNull(outNullTitle)

        val qNullTitleVal =
            Questionnaire(
                title = String(value = null),
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "q1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )
        val outNullTitleVal = FhirValidator.validateToOutcome(qNullTitleVal).getOrNull()
        kotlin.test.assertNotNull(outNullTitleVal)

        val qEmptyTitleVal =
            Questionnaire(
                title = String(value = ""),
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "q1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )
        val outEmptyTitleVal = FhirValidator.validateToOutcome(qEmptyTitleVal).getOrNull()
        kotlin.test.assertNotNull(outEmptyTitleVal)

        val qValidTitle =
            Questionnaire(
                title = String(value = "Clinical Screening"),
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = String(value = "q1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )
        val outValidTitle = FhirValidator.validateToOutcome(qValidTitle).getOrNull()
        kotlin.test.assertNull(outValidTitle)

        // QuestionnaireResponse with null status
        val qrNullStatus = QuestionnaireResponse(status = Enumeration(value = null))
        val qrOutcome = FhirValidator.validateToOutcome(qrNullStatus).getOrNull()
        kotlin.test.assertNotNull(qrOutcome)
        assertTrue(qrOutcome.issue.any { it.expression.any { e -> e.value == "QuestionnaireResponse.status" } })

        // Valid QuestionnaireResponse outcome is null
        val qrValid =
            QuestionnaireResponse(status = Enumeration(value = QuestionnaireResponse.QuestionnaireResponseStatus.Completed))
        val qrValidOutcome = FhirValidator.validateToOutcome(qrValid).getOrNull()
        kotlin.test.assertNull(qrValidOutcome)
    }
}
