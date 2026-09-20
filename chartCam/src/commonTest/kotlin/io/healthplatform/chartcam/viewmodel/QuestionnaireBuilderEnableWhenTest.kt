/**
 * @file QuestionnaireBuilderEnableWhenTest.kt
 * Unit tests for QuestionnaireBuilderViewModel verifying enableWhen conditional logic configuration and FHIR mapping.
 */
package io.healthplatform.chartcam.viewmodel

import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for configuring and exporting enableWhen conditional logic in the QuestionnaireBuilder.
 */
class QuestionnaireBuilderEnableWhenTest {
    /**
     * Tests adding and updating enableWhen conditions on builder items and verifying FHIR output.
     */
    @Test
    fun testBuilderEnableWhenMapping() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repository = repo)

        viewModel.updateTitle("Triage Assessment")
        viewModel.addItem(WidgetType.SWITCH) // item_1
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT) // item_2

        val conditions =
            listOf(
                BuilderEnableWhen(
                    question = "item_1",
                    operator = Questionnaire.QuestionnaireItemOperator.EqualTo,
                    answerBoolean = true,
                ),
            )

        viewModel.updateItemEnableWhen(
            linkId = "item_2",
            enableWhen = conditions,
            enableBehavior = Questionnaire.EnableWhenBehavior.All,
        )

        val state = viewModel.state.value
        val item2 = state.items.first { it.linkId == "item_2" }
        assertEquals(1, item2.enableWhen.size)
        assertEquals("item_1", item2.enableWhen[0].question)
        assertEquals(true, item2.enableWhen[0].answerBoolean)
        assertEquals(Questionnaire.EnableWhenBehavior.All, item2.enableBehavior)

        // Generate FHIR Questionnaire
        val fhir = viewModel.buildQuestionnaire()
        val fhirItem2 = fhir.item.first { it.linkId.value == "item_2" }
        assertEquals(1, fhirItem2.enableWhen.size)
        assertEquals("item_1", fhirItem2.enableWhen[0].question.value)
        assertEquals(Questionnaire.QuestionnaireItemOperator.EqualTo, fhirItem2.enableWhen[0].operator.value)
        assertEquals(
            true,
            fhirItem2.enableWhen[0]
                .answer
                .asBoolean()
                ?.value
                ?.value,
        )
        assertEquals(Questionnaire.EnableWhenBehavior.All, fhirItem2.enableBehavior?.value)
    }

    /**
     * Tests all answer variant serializations: Integer, Decimal, String, Boolean.
     */
    @Test
    fun testAllAnswerVariantsSerialization() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repository = repo)

        viewModel.updateTitle("Multi-Condition Form")
        viewModel.addItem(WidgetType.NUMERIC) // item_1
        viewModel.addItem(WidgetType.RANGE) // item_2
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT) // item_3
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT) // item_4 (dependent)

        val conditions =
            listOf(
                BuilderEnableWhen(
                    question = "item_1",
                    operator = Questionnaire.QuestionnaireItemOperator.GreaterThan,
                    answerDecimal = 37.5,
                ),
                BuilderEnableWhen(
                    question = "item_2",
                    operator = Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo,
                    answerInteger = 18,
                ),
                BuilderEnableWhen(
                    question = "item_3",
                    operator = Questionnaire.QuestionnaireItemOperator.EqualTo,
                    answerString = "fever",
                ),
            )

        viewModel.updateItemEnableWhen(
            linkId = "item_4",
            enableWhen = conditions,
            enableBehavior = Questionnaire.EnableWhenBehavior.Any,
        )

        val fhir = viewModel.buildQuestionnaire()
        val fhirItem4 = fhir.item.first { it.linkId.value == "item_4" }
        assertEquals(3, fhirItem4.enableWhen.size)
        assertEquals(Questionnaire.EnableWhenBehavior.Any, fhirItem4.enableBehavior?.value)

        // Verify Decimal
        assertNotNull(fhirItem4.enableWhen[0].answer.asDecimal())
        assertEquals(
            37.5,
            fhirItem4.enableWhen[0]
                .answer
                .asDecimal()
                ?.value
                ?.value
                ?.asBigDecimal()
                ?.doubleValue(exactRequired = false),
        )

        // Verify Integer
        assertNotNull(fhirItem4.enableWhen[1].answer.asInteger())
        assertEquals(
            18,
            fhirItem4.enableWhen[1]
                .answer
                .asInteger()
                ?.value
                ?.value,
        )

        // Verify String
        assertNotNull(fhirItem4.enableWhen[2].answer.asString())
        assertEquals(
            "fever",
            fhirItem4.enableWhen[2]
                .answer
                .asString()
                ?.value
                ?.value,
        )
    }

    /**
     * Tests round-trip importing/duplicating of a Questionnaire containing enableWhen rules.
     */
    @Test
    fun testDuplicateQuestionnairePreservesEnableWhen() {
        val repo = QuestionnaireRepository()
        val originalVm = QuestionnaireBuilderViewModel(repository = repo)

        originalVm.updateTitle("Source Form")
        originalVm.addItem(WidgetType.SWITCH)
        originalVm.addItem(WidgetType.SINGLE_LINE_TEXT)

        originalVm.updateItemEnableWhen(
            linkId = "item_2",
            enableWhen =
                listOf(
                    BuilderEnableWhen(
                        question = "item_1",
                        operator = Questionnaire.QuestionnaireItemOperator.EqualTo,
                        answerBoolean = true,
                    ),
                ),
            enableBehavior = Questionnaire.EnableWhenBehavior.All,
        )

        val originalFhir = originalVm.buildQuestionnaire()
        repo.saveQuestionnaire(originalFhir)

        // Create new ViewModel duplicating from originalFhir
        val duplicateVm =
            QuestionnaireBuilderViewModel(
                repository = repo,
                duplicateFromId = originalFhir.id,
            )

        val dupState = duplicateVm.state.value
        val dupItem2 = dupState.items.first { it.linkId == "item_2" }
        assertEquals(1, dupItem2.enableWhen.size)
        assertEquals("item_1", dupItem2.enableWhen[0].question)
        assertEquals(true, dupItem2.enableWhen[0].answerBoolean)
        assertEquals(Questionnaire.EnableWhenBehavior.All, dupItem2.enableBehavior)
    }
}
