/**
 * @file SdcExtensionsJvmTest.kt
 * Contains declarations for SdcExtensionsJvmTest.kt.
 */
package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Boolean
import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.CodeableConcept
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Integer
import com.google.fhir.model.r4.Questionnaire
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for SdcExtensions on JVM.
 */
class SdcExtensionsJvmTest {
    /**
     * Helper method to create a Questionnaire.Item.Builder.
     * @return the builder instance
     */
    private fun createBuilder() =
        Questionnaire.Item.Builder(
            linkId =
                com.google.fhir.model.r4.String
                    .Builder()
                    .apply { value = "test_link" },
            type =
                com.google.fhir.model.r4
                    .Enumeration(value = Questionnaire.QuestionnaireItemType.String),
        )

    /**
     * Tests isHidden extension.
     */
    @Test
    fun `test isHidden`() {
        val itemWithoutHidden = createBuilder().build()
        assertFalse(itemWithoutHidden.isHidden())

        val itemWithHiddenTrue =
            createBuilder()
                .apply {
                    extension.add(
                        Extension.Builder(url = SdcExtensions.HIDDEN).apply {
                            value = Extension.Value.Boolean(Boolean.Builder().apply { value = true }.build())
                        },
                    )
                }.build()
        assertTrue(itemWithHiddenTrue.isHidden())

        val itemWithHiddenFalse =
            createBuilder()
                .apply {
                    extension.add(
                        Extension.Builder(url = SdcExtensions.HIDDEN).apply {
                            value = Extension.Value.Boolean(Boolean.Builder().apply { value = false }.build())
                        },
                    )
                }.build()
        assertFalse(itemWithHiddenFalse.isHidden())

        // Edge case: extension present but wrong type or null value
        val itemWithBadHidden =
            createBuilder()
                .apply {
                    extension.add(Extension.Builder(url = SdcExtensions.HIDDEN))
                }.build()
        assertFalse(itemWithBadHidden.isHidden())
    }

    /**
     * Tests getItemControl extension.
     */
    @Test
    fun `test getItemControl`() {
        val itemNoControl = createBuilder().build()
        assertNull(itemNoControl.getItemControl())

        val itemWithControl =
            createBuilder()
                .apply {
                    extension.add(
                        Extension.Builder(url = SdcExtensions.ITEM_CONTROL).apply {
                            value =
                                Extension.Value.CodeableConcept(
                                    CodeableConcept
                                        .Builder()
                                        .apply {
                                            coding.add(
                                                Coding.Builder().apply {
                                                    code = Code.Builder().apply { value = "drop-down" }
                                                },
                                            )
                                        }.build(),
                                )
                        },
                    )
                }.build()
        assertEquals("drop-down", itemWithControl.getItemControl())

        // Edge case: empty coding list
        val itemEmptyControl =
            createBuilder()
                .apply {
                    extension.add(
                        Extension.Builder(url = SdcExtensions.ITEM_CONTROL).apply {
                            value =
                                Extension.Value.CodeableConcept(
                                    CodeableConcept.Builder().build(),
                                )
                        },
                    )
                }.build()
        assertNull(itemEmptyControl.getItemControl())
    }

    /**
     * Tests getMinValue and getMaxValue extensions.
     */
    @Test
    fun `test getMinValue and getMaxValue`() {
        val itemNoMinMax = createBuilder().build()
        assertNull(itemNoMinMax.getMinValue())
        assertNull(itemNoMinMax.getMaxValue())

        val itemWithMinMax =
            createBuilder()
                .apply {
                    extension.add(
                        Extension.Builder(url = SdcExtensions.MIN_VALUE).apply {
                            value = Extension.Value.Integer(Integer.Builder().apply { value = 10 }.build())
                        },
                    )
                    extension.add(
                        Extension.Builder(url = SdcExtensions.MAX_VALUE).apply {
                            value = Extension.Value.Integer(Integer.Builder().apply { value = 100 }.build())
                        },
                    )
                }.build()
        assertEquals(10f, itemWithMinMax.getMinValue())
        assertEquals(100f, itemWithMinMax.getMaxValue())
    }
}
