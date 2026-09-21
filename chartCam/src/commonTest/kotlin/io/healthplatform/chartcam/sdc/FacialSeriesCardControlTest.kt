/**
 * @file FacialSeriesCardControlTest.kt
 * Contains tests for FacialSlotInfo and facial series SDC helpers.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.camera.SilhouetteType
import io.healthplatform.chartcam.fhir.SdcExtensions
import io.healthplatform.chartcam.fhir.getSilhouetteType
import io.healthplatform.chartcam.fhir.isFacialProfileSeries
import io.healthplatform.chartcam.ui.sdc.controls.FacialSlotInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Tests verifying the facial series SDC helper logic and slot state modeling.
 */
class FacialSeriesCardControlTest {
    private fun str(s: String) = FhirString.Builder().apply { value = s }

    @Test
    fun testFacialSlotInfo() {
        val slot1 =
            FacialSlotInfo(
                linkId = "slot_1",
                title = "Left Profile",
                isCaptured = true,
            )
        val slot2 =
            FacialSlotInfo(
                linkId = "slot_1",
                title = "Left Profile",
                isCaptured = true,
            )
        val slotDiff =
            FacialSlotInfo(
                linkId = "slot_2",
                title = "Right Profile",
                isCaptured = false,
            )

        assertEquals("slot_1", slot1.linkId)
        assertEquals("Left Profile", slot1.title)
        assertTrue(slot1.isCaptured)

        // Test equality and hashCode
        assertEquals(slot1, slot2)
        assertEquals(slot1.hashCode(), slot2.hashCode())
        assertFalse(slot1 == slotDiff)
        assertFalse(slot1.equals("other"))

        // Test copy
        val slotCopied = slot1.copy(isCaptured = false)
        assertEquals("slot_1", slotCopied.linkId)
        assertEquals("Left Profile", slotCopied.title)
        assertFalse(slotCopied.isCaptured)

        // Test destructuring components
        val (id, title, captured) = slot1
        assertEquals("slot_1", id)
        assertEquals("Left Profile", title)
        assertTrue(captured)

        // Test toString
        assertTrue(slot1.toString().contains("slot_1"))
        assertTrue(slot1.toString().contains("Left Profile"))
    }

    @Test
    fun testIsFacialProfileSeries() {
        val itemWithControl =
            Questionnaire.Item
                .Builder(
                    linkId = str("facial_group"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).apply {
                    extension.add(
                        Extension
                            .Builder(url = SdcExtensions.ITEM_CONTROL)
                            .apply {
                                value =
                                    Extension.Value.CodeableConcept(
                                        CodeableConcept(
                                            coding =
                                                listOf(
                                                    Coding(
                                                        code =
                                                            Code(
                                                                value =
                                                                    SdcExtensions
                                                                        .ITEM_CONTROL_FACIAL_PROFILE_SERIES,
                                                            ),
                                                    ),
                                                ),
                                        ),
                                    )
                            },
                    )
                }.build()
        assertTrue(itemWithControl.isFacialProfileSeries())

        val regularItem =
            Questionnaire.Item
                .Builder(
                    linkId = str("regular_group"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                ).build()
        assertFalse(regularItem.isFacialProfileSeries())
    }

    @Test
    fun testGetSilhouetteTypeFromItem() {
        val itemWithSilhouette =
            Questionnaire.Item
                .Builder(
                    linkId = str("left_photo"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                ).apply {
                    extension.add(
                        Extension
                            .Builder(url = SdcExtensions.CAMERA_SILHOUETTE)
                            .apply {
                                value = Extension.Value.Code(Code(value = "profile-cornea-left"))
                            },
                    )
                }.build()
        val resolved = itemWithSilhouette.getSilhouetteType()
        assertTrue(resolved.isSuccess)
        assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_LEFT, resolved.getOrNull())

        val plainItem =
            Questionnaire.Item
                .Builder(
                    linkId = str("plain_photo"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                ).build()
        assertEquals(SilhouetteType.NONE, plainItem.getSilhouetteType().getOrNull())

        val itemWithStringVal =
            Questionnaire.Item
                .Builder(
                    linkId = str("str_photo"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                ).apply {
                    extension.add(
                        Extension
                            .Builder(url = SdcExtensions.CAMERA_SILHOUETTE)
                            .apply {
                                value = Extension.Value.String(str("frontal-face").build())
                            },
                    )
                }.build()
        val strResolved = itemWithStringVal.getSilhouetteType()
        assertTrue(strResolved.isSuccess)
        assertEquals(SilhouetteType.FRONTAL_FACE, strResolved.getOrNull())

        val itemWithInvalidVal =
            Questionnaire.Item
                .Builder(
                    linkId = str("inv_photo"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                ).apply {
                    extension.add(
                        Extension
                            .Builder(url = SdcExtensions.CAMERA_SILHOUETTE)
                            .apply {
                                value =
                                    Extension.Value.Boolean(
                                        dev.ohs.fhir.model.r4
                                            .Boolean(value = true),
                                    )
                            },
                    )
                }.build()
        val invResolved = itemWithInvalidVal.getSilhouetteType()
        assertTrue(invResolved.isSuccess)
        assertEquals(SilhouetteType.NONE, invResolved.getOrNull())
    }
}
