/**
 * @file InternationalizedQuestionnaireBuilderWorkflowTest.kt
 * Contains declarations for InternationalizedQuestionnaireBuilderWorkflowTest.kt.
 *
 * Validates internationalized questionnaire title slug generation, Unicode stability,
 * canonical URI construction, and duplicate form name detection across multiple scripts (Japanese, Hebrew, Latin).
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import io.healthplatform.chartcam.viewmodel.WidgetType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Workflow tests ensuring internationalized forms generate robust slugs and handle duplicate names accurately.
 */
class InternationalizedQuestionnaireBuilderWorkflowTest {
    private lateinit var repo: QuestionnaireRepository

    /**
     * Initializes repository before each test.
     */
    @BeforeTest
    fun setUp() {
        repo = QuestionnaireRepository()
    }

    /**
     * Verifies Japanese Kanji and Kana titles generate deterministic non-empty IDs and valid canonical URIs.
     */
    @Test
    fun testJapaneseQuestionnaireSlugAndUriGeneration() {
        val vm = QuestionnaireBuilderViewModel(repo)
        vm.updateTitle("皮膚科スクリーニング検査")

        vm.addItem(WidgetType.SINGLE_SELECT)
        val selectId =
            vm.state.value.items
                .first()
                .linkId
        vm.updateItem(selectId, "皮疹の部位", listOf("顔", "体幹", "四肢"))

        vm.addItem(WidgetType.NUMERIC)
        val numId =
            vm.state.value.items
                .last()
                .linkId
        vm.updateItem(numId, "発症からの日数", emptyList())

        val savedId = vm.saveQuestionnaire()
        assertNotNull(savedId, "Japanese questionnaire should be saved successfully")
        assertTrue(savedId.startsWith("custom-i18n-"), "Non-Latin title should generate i18n prefix")
        assertFalse(savedId == "custom-", "ID must never collapse to bare custom- prefix")

        val q = repo.getQuestionnaire(savedId)
        assertNotNull(q)
        assertEquals("皮膚科スクリーニング検査", q.title?.value)
        assertEquals("http://healthplatform.io/fhir/Questionnaire/$savedId", q.url?.value)
    }

    /**
     * Verifies Hebrew RTL titles generate non-empty unique identifiers without collisions.
     */
    @Test
    fun testHebrewRtlQuestionnaireSlugAndUriGeneration() {
        val vm = QuestionnaireBuilderViewModel(repo)
        vm.updateTitle("שאלון בדיקת עיניים")

        vm.addItem(WidgetType.SWITCH)
        val switchId =
            vm.state.value.items
                .first()
                .linkId
        vm.updateItem(switchId, "האם יש כאב?", emptyList())

        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        val textId =
            vm.state.value.items
                .last()
                .linkId
        vm.updateItem(textId, "הערות נוספות", emptyList())

        val savedId = vm.saveQuestionnaire()
        assertNotNull(savedId)
        assertTrue(savedId.startsWith("custom-i18n-"))

        val q = repo.getQuestionnaire(savedId)
        assertNotNull(q)
        assertEquals("שאלון בדיקת עיניים", q.title?.value)
    }

    /**
     * Verifies mixed alphanumeric characters and symbols produce clean sanitized slugs.
     */
    @Test
    fun testMixedAlphanumericAndSymbolSlugSanitization() {
        val vm = QuestionnaireBuilderViewModel(repo)
        vm.updateTitle("COVID-19 & Flu Intake (2026!)")

        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        val itemId =
            vm.state.value.items
                .first()
                .linkId
        vm.updateItem(itemId, "Symptoms", emptyList())

        val savedId = vm.saveQuestionnaire()
        assertNotNull(savedId)
        assertEquals("custom-covid-19-flu-intake-2026", savedId)

        val q = repo.getQuestionnaire(savedId)
        assertNotNull(q)
        assertEquals("http://healthplatform.io/fhir/Questionnaire/custom-covid-19-flu-intake-2026", q.url?.value)
    }

    /**
     * Verifies that saving a second questionnaire with an identical non-ASCII title raises a duplicate error.
     */
    @Test
    fun testDuplicateNonAsciiTitleDetection() {
        val vm1 = QuestionnaireBuilderViewModel(repo)
        vm1.updateTitle("皮膚科スクリーニング検査")
        vm1.addItem(WidgetType.SINGLE_LINE_TEXT)
        vm1.updateItem(
            vm1.state.value.items
                .first()
                .linkId,
            "Question",
            emptyList(),
        )

        val id1 = vm1.saveQuestionnaire()
        assertNotNull(id1)
        assertFalse(vm1.state.value.isDuplicateNameError)

        // Attempt second save with identical title
        val vm2 = QuestionnaireBuilderViewModel(repo)
        vm2.updateTitle("皮膚科スクリーニング検査")
        vm2.addItem(WidgetType.SINGLE_LINE_TEXT)
        vm2.updateItem(
            vm2.state.value.items
                .first()
                .linkId,
            "Question",
            emptyList(),
        )

        val id2 = vm2.saveQuestionnaire()
        assertNull(id2, "Duplicate title save must be rejected")
        assertTrue(vm2.state.value.isDuplicateNameError, "isDuplicateNameError must be raised")
    }
}
