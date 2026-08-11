/**
 * @file SdcExtensionsTest.kt
 * Contains declarations for SdcExtensionsTest.kt.
 */
package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.CodeableConcept
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Integer
import com.google.fhir.model.r4.Questionnaire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.Boolean as FhirBoolean
import com.google.fhir.model.r4.String as FhirString

/**
 * Tests for SDC extension functions on FHIR resources.
 */
class SdcExtensionsTest {
    /** Helper for creating FHIR strings. */
    private fun str(s: String) = FhirString.Builder().apply { value = s }

    /**
     * Test the `isHidden` extension.
     */
    @Test
    fun testIsHidden() {
        val hiddenExt =
            Extension.Builder(url = SdcExtensions.HIDDEN).apply {
                value = Extension.Value.Boolean(FhirBoolean.Builder().apply { value = true }.build())
            }
        val item =
            Questionnaire.Item
                .Builder(
                    linkId = str("1"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(hiddenExt)
                }.build()
        assertTrue(item.isHidden())

        val visibleItem =
            Questionnaire.Item
                .Builder(
                    linkId = str("2"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()
        assertFalse(visibleItem.isHidden())
    }

    /**
     * Test getting the item control extension.
     */
    @Test
    fun testGetItemControl() {
        val coding = Coding.Builder().apply { code = Code.Builder().apply { value = "drop-down" } }
        val codeableConcept = CodeableConcept.Builder().apply { this.coding.add(coding) }.build()

        val controlExt =
            Extension.Builder(url = SdcExtensions.ITEM_CONTROL).apply {
                value = Extension.Value.CodeableConcept(codeableConcept)
            }

        val item =
            Questionnaire.Item
                .Builder(
                    linkId = str("1"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(controlExt)
                }.build()

        assertEquals("drop-down", item.getItemControl())

        val itemNoControl =
            Questionnaire.Item
                .Builder(
                    linkId = str("2"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()
        assertNull(itemNoControl.getItemControl())
    }

    /**
     * Test getting min/max value extensions.
     */
    @Test
    fun testGetMinValueAndMaxValue() {
        val minExt =
            Extension.Builder(url = SdcExtensions.MIN_VALUE).apply {
                value = Extension.Value.Integer(Integer.Builder().apply { value = 10 }.build())
            }
        val maxExt =
            Extension.Builder(url = SdcExtensions.MAX_VALUE).apply {
                value = Extension.Value.Integer(Integer.Builder().apply { value = 50 }.build())
            }

        val item =
            Questionnaire.Item
                .Builder(
                    linkId = str("1"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(minExt)
                    extension.add(maxExt)
                }.build()

        assertEquals(10f, item.getMinValue())
        assertEquals(50f, item.getMaxValue())

        val emptyItem =
            Questionnaire.Item
                .Builder(
                    linkId = str("2"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()
        assertNull(emptyItem.getMinValue())
        assertNull(emptyItem.getMaxValue())
    }
}
