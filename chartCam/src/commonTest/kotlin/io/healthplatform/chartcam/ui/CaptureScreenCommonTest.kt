/**
 * @file CaptureScreenCommonTest.kt
 * Contains declarations for CaptureScreenCommonTest.kt.
 */
package io.healthplatform.chartcam.ui

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.fhir.SdcExtensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.google.fhir.model.r4.String as FhirString

/**
 * Common test wrapper for the Capture Screen logic.
 */
class CaptureScreenCommonTest {
    /**
     * Placeholder test.
     */
    @Test
    fun dummyTest() {
        assertNotNull(this)
    }

    /**
     * Verifies that [extractSteps] correctly extracts attachment steps and localizes their titles.
     */
    @Test
    fun testExtractStepsLocalization() {
        val langExtEs =
            Extension.Builder(url = "lang").apply {
                value = Extension.Value.String(FhirString.Builder().apply { value = "es" }.build())
            }
        val contentExtEs =
            Extension.Builder(url = "content").apply {
                value = Extension.Value.String(FhirString.Builder().apply { value = "Foto Frontal" }.build())
            }
        val transExtEs =
            Extension.Builder(url = SdcExtensions.TRANSLATION).apply {
                extension.add(langExtEs)
                extension.add(contentExtEs)
            }

        val item1 =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "step1" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                ).apply {
                    text = FhirString.Builder().apply { value = "Front Photo" }
                    extension.add(transExtEs)
                }.build()

        val item2 =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "group1" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    val nestedAttachment =
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "step2" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                            ).apply {
                                text = FhirString.Builder().apply { value = "Side Photo" }
                            }
                    item.add(nestedAttachment)
                }.build()

        val items = listOf(item1, item2)

        val esSteps = extractSteps(items, "es")
        assertEquals(2, esSteps.size)
        assertEquals("step1", esSteps[0].id)
        assertEquals("Foto Frontal", esSteps[0].title)
        assertEquals("step2", esSteps[1].id)
        assertEquals("Side Photo", esSteps[1].title)

        val enSteps = extractSteps(items, "en")
        assertEquals("Front Photo", enSteps[0].title)
    }
}
