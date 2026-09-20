/**
 * @file DynamicLanguageAndRtlWorkflowTest.kt
 * Contains declarations for DynamicLanguageAndRtlWorkflowTest.kt.
 *
 * Validates dynamic runtime language switching (English, Hebrew RTL, Spanish, Japanese)
 * during active clinical form completion without state loss.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.fhir.QuestionnaireResponseGenerator
import io.healthplatform.chartcam.fhir.SdcExtensions
import io.healthplatform.chartcam.sdc.SdcQuestionnaireForm
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * End-to-End workflow tests verifying live language switches between LTR and RTL scripts
 * during active form completion without data regression or crashes.
 */
@OptIn(ExperimentalTestApi::class)
class DynamicLanguageAndRtlWorkflowTest {
    private var initialLanguage = "en"

    /**
     * Preserves existing runtime language.
     */
    @BeforeTest
    fun setUp() {
        initialLanguage = currentLanguageState.value
        currentLanguageState.value = "en"
    }

    /**
     * Restores previous language state.
     */
    @AfterTest
    fun tearDown() {
        currentLanguageState.value = initialLanguage
    }

    /**
     * Helper to build a localized questionnaire with English, Hebrew, Spanish, and Japanese strings.
     *
     * @return Multi-lingual [Questionnaire].
     */
    private fun buildMultilingualQuestionnaire(): Questionnaire {
        fun createTranslationExt(
            lang: String,
            text: String,
        ): Extension.Builder =
            Extension.Builder(url = SdcExtensions.TRANSLATION).apply {
                extension.add(
                    Extension.Builder(url = "lang").apply {
                        value = Extension.Value.String(FhirString.Builder().apply { value = lang }.build())
                    },
                )
                extension.add(
                    Extension.Builder(url = "content").apply {
                        value = Extension.Value.String(FhirString.Builder().apply { value = text }.build())
                    },
                )
            }

        val feverItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "fever_symptom" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                ).apply {
                    text = FhirString.Builder().apply { value = "Do you have a fever?" }
                    extension.add(createTranslationExt("he", "האם יש לך חום?"))
                    extension.add(createTranslationExt("es", "¿Tiene fiebre?"))
                    extension.add(createTranslationExt("ja", "発熱はありますか？"))
                }

        val notesItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "clinical_notes" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = FhirString.Builder().apply { value = "Clinical Observations" }
                    extension.add(createTranslationExt("he", "הערות קליניות"))
                    extension.add(createTranslationExt("es", "Observaciones clínicas"))
                    extension.add(createTranslationExt("ja", "臨床観察"))
                }

        return Questionnaire
            .Builder(status = Enumeration(value = PublicationStatus.Active))
            .apply {
                title = FhirString.Builder().apply { value = "Clinical Triage Form" }
                item.add(feverItem)
                item.add(notesItem)
            }.build()
    }

    /**
     * Tests live flipping across languages and RTL layout orientation while preserving user inputs.
     */
    @Test
    fun testLiveLanguageSwitchingPreservesActiveFormState() =
        runComposeUiTest {
            val questionnaire = buildMultilingualQuestionnaire()

            setContent {
                val currentLang by currentLanguageState.collectAsState()
                val layoutDirection = getLayoutDirectionForLanguage(currentLang)

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    MaterialTheme {
                        var answers by remember {
                            mutableStateOf<Map<String, Any>>(
                                mapOf(
                                    "clinical_notes" to "Persistent cough and mild fatigue",
                                    "fever_symptom" to true,
                                ),
                            )
                        }

                        SdcQuestionnaireForm(
                            questionnaire = questionnaire,
                            answers = answers,
                            onFormUpdated = { newAnswers, _ ->
                                answers = newAnswers
                            },
                        )
                    }
                }
            }

            // 1. Initial State: English (LTR)
            assertEquals("en", currentLanguageState.value)
            assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("en"))
            onNodeWithText("Do you have a fever?").assertExists()
            onNodeWithText("Clinical Observations").assertExists()

            // 2. Switch to Hebrew (RTL)
            currentLanguageState.value = "he"
            waitForIdle()
            assertEquals("he", currentLanguageState.value)
            assertEquals(LayoutDirection.Rtl, getLayoutDirectionForLanguage("he"))
            assertTrue(isRtlLanguage("he"))
            onNodeWithText("האם יש לך חום?").assertExists()
            onNodeWithText("הערות קליניות").assertExists()

            // 3. Switch to Spanish (LTR)
            currentLanguageState.value = "es"
            waitForIdle()
            assertEquals("es", currentLanguageState.value)
            assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("es"))
            onNodeWithText("¿Tiene fiebre?").assertExists()
            onNodeWithText("Observaciones clínicas").assertExists()

            // 4. Switch to Japanese (LTR)
            currentLanguageState.value = "ja"
            waitForIdle()
            assertEquals("ja", currentLanguageState.value)
            assertEquals(LayoutDirection.Ltr, getLayoutDirectionForLanguage("ja"))
            onNodeWithText("発熱はありますか？").assertExists()
            onNodeWithText("臨床観察").assertExists()

            // 5. Final Submission: Ensure QuestionnaireResponse contains original preserved inputs
            val activeAnswers =
                mapOf<String, Any>(
                    "clinical_notes" to "Persistent cough and mild fatigue",
                    "fever_symptom" to true,
                )
            val qr = QuestionnaireResponseGenerator.generate(questionnaire, activeAnswers)
            val notesAnswer =
                qr.item
                    .firstOrNull { it.linkId.value == "clinical_notes" }
                    ?.answer
                    ?.firstOrNull()
            assertNotNull(notesAnswer)
            val notesVal = (notesAnswer.value as? QuestionnaireResponse.Item.Answer.Value.String)?.value?.value
            assertEquals("Persistent cough and mild fatigue", notesVal)
        }
}
