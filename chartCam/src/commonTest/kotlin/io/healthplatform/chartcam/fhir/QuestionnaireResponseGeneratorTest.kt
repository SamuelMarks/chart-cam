/**
 * @file QuestionnaireResponseGeneratorTest.kt
 * Contains declarations for QuestionnaireResponseGeneratorTest.kt.
 */
package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.Boolean as FhirBoolean
import com.google.fhir.model.r4.String as FhirString

/**
 * Tests for [QuestionnaireResponseGenerator].
 */
class QuestionnaireResponseGeneratorTest {
    /**
     * Validates generation of a basic response.
     */
    @Test
    fun testBasicResponseGeneration() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q1"
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "name" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ).apply {
                                text = FhirString.Builder().apply { value = "Patient Name" }
                            },
                    )
                }.build()

        val answers = mapOf("name" to "Alice Smith")
        val result = QuestionnaireResponseGenerator.generateResult(q, answers)
        assertTrue(result.isSuccess)
        val qr = result.getOrNull()
        assertNotNull(qr)
        assertEquals(1, qr.item.size)
        assertEquals("name", qr.item[0].linkId.value)
    }

    /**
     * Validates generation of repeated question groups with hierarchical answers.
     */
    @Test
    fun testRepeatingGroupResponseGeneration() {
        val q =
            Questionnaire
                .Builder(status = Enumeration(value = PublicationStatus.Active))
                .apply {
                    id = "q_repeating"
                    item.add(
                        Questionnaire.Item
                            .Builder(
                                linkId = FhirString.Builder().apply { value = "lesion_group" },
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                            ).apply {
                                repeats = FhirBoolean.Builder().apply { value = true }
                                text = FhirString.Builder().apply { value = "Lesions" }
                                item.add(
                                    Questionnaire.Item.Builder(
                                        linkId = FhirString.Builder().apply { value = "location" },
                                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                                    ),
                                )
                            },
                    )
                }.build()

        val answers =
            mapOf(
                "lesion_group#0.location" to "Arm",
                "lesion_group#1.location" to "Leg",
            )

        val result = QuestionnaireResponseGenerator.generateResult(q, answers)
        assertTrue(result.isSuccess)
        val qr = result.getOrNull()
        assertNotNull(qr)
        assertEquals(2, qr.item.size)
        assertEquals("lesion_group", qr.item[0].linkId.value)
        assertEquals("lesion_group", qr.item[1].linkId.value)
    }
}
