/**
 * @file FacialSeriesItemHelperTest.kt
 * Unit tests for [applyFacialSeriesItems] and [createFacialSeriesChildItem].
 */
package io.healthplatform.chartcam.viewmodel

import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Tests for facial series item helper functions.
 */
class FacialSeriesItemHelperTest {
    /**
     * Verifies that [applyFacialSeriesItems] returns success and appends the 3 expected child items.
     */
    @Test
    fun `applyFacialSeriesItems returns success and adds 3 items when inputs are valid`() {
        val groupBuilder =
            Questionnaire.Item.Builder(
                linkId = FhirString(value = "group1").toBuilder(),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
            )

        val result =
            applyFacialSeriesItems(
                groupBuilder = groupBuilder,
                baseLinkId = "base_1",
                leftProfileText = "Left",
                frontViewText = "Front",
                rightProfileText = "Right",
            )

        assertTrue(result.isSuccess)
        val builtGroup = groupBuilder.build()
        assertEquals(3, builtGroup.item.size)

        assertEquals("base_1_left", builtGroup.item[0].linkId.value)
        assertEquals("Left", builtGroup.item[0].text?.value)

        assertEquals("base_1_front", builtGroup.item[1].linkId.value)
        assertEquals("Front", builtGroup.item[1].text?.value)

        assertEquals("base_1_right", builtGroup.item[2].linkId.value)
        assertEquals("Right", builtGroup.item[2].text?.value)
    }

    /**
     * Verifies that [applyFacialSeriesItems] fails when given a blank baseLinkId.
     */
    @Test
    fun `applyFacialSeriesItems returns failure when baseLinkId is blank`() {
        val groupBuilder =
            Questionnaire.Item.Builder(
                linkId = FhirString(value = "group1").toBuilder(),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
            )

        val result =
            applyFacialSeriesItems(
                groupBuilder = groupBuilder,
                baseLinkId = "  ",
                leftProfileText = "Left",
                frontViewText = "Front",
                rightProfileText = "Right",
            )

        assertTrue(result.isFailure)
    }

    /**
     * Verifies that [createFacialSeriesChildItem] succeeds when all required parameters are valid.
     */
    @Test
    fun `createFacialSeriesChildItem returns success for valid inputs`() {
        val result =
            createFacialSeriesChildItem(
                linkId = "link_1",
                text = "Test Text",
                silhouetteCode = "sil_code",
                snomedCode = "sno_code",
                snomedDisplay = "sno_display",
            )

        assertTrue(result.isSuccess)
        val item = result.getOrThrow().build()
        assertEquals("link_1", item.linkId.value)
        assertEquals("Test Text", item.text?.value)
    }

    /**
     * Verifies that [createFacialSeriesChildItem] fails when linkId is blank.
     */
    @Test
    fun `createFacialSeriesChildItem returns failure when linkId is blank`() {
        val result =
            createFacialSeriesChildItem(
                linkId = "",
                text = "Test Text",
                silhouetteCode = "sil_code",
                snomedCode = "sno_code",
                snomedDisplay = "sno_display",
            )
        assertTrue(result.isFailure)
    }

    /**
     * Verifies that [createFacialSeriesChildItem] fails when silhouetteCode is blank.
     */
    @Test
    fun `createFacialSeriesChildItem returns failure when silhouetteCode is blank`() {
        val result =
            createFacialSeriesChildItem(
                linkId = "link_1",
                text = "Test Text",
                silhouetteCode = "  ",
                snomedCode = "sno_code",
                snomedDisplay = "sno_display",
            )
        assertTrue(result.isFailure)
    }

    /**
     * Verifies that [createFacialSeriesChildItem] fails when snomedCode is blank.
     */
    @Test
    fun `createFacialSeriesChildItem returns failure when snomedCode is blank`() {
        val result =
            createFacialSeriesChildItem(
                linkId = "link_1",
                text = "Test Text",
                silhouetteCode = "sil_code",
                snomedCode = "",
                snomedDisplay = "sno_display",
            )
        assertTrue(result.isFailure)
    }
}
