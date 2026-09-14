/**
 * @file VisualFormControlsTest.kt
 * Contains declarations for VisualFormControlsTest.kt.
 *
 * Unit tests for visual form control data models, SDC extensions, and response generator.
 */
package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Code
import com.google.fhir.model.r4.CodeableConcept
import com.google.fhir.model.r4.Coding
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.models.BodyMapLocation
import io.healthplatform.chartcam.models.FitzpatrickScaleDefaults
import io.healthplatform.chartcam.models.FitzpatrickSkinType
import io.healthplatform.chartcam.models.PainScaleDefaults
import io.healthplatform.chartcam.ui.sdc.controls.getFitzpatrickDescResource
import io.healthplatform.chartcam.ui.sdc.controls.getFitzpatrickTitleResource
import io.healthplatform.chartcam.ui.sdc.controls.getPainScoreColor
import io.healthplatform.chartcam.ui.sdc.controls.getPainScoreResource
import io.healthplatform.chartcam.ui.sdc.controls.parseHexColor
import io.healthplatform.chartcam.ui.sdc.controls.resolveAnatomicalRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.String as FhirString

/**
 * Unit tests for visual form controls SDC extensions, serializers, and helpers.
 */
class VisualFormControlsTest {
    /**
     * Builds a dummy FHIR string.
     *
     * @param value The raw string.
     * @return The FHIR string builder.
     */
    private fun fhirStr(value: String) = FhirString.Builder().apply { this.value = value }

    /**
     * Helper to create an item builder with an itemControl extension.
     *
     * @param linkId The linkId of the item.
     * @param type The QuestionnaireItemType.
     * @param itemControlCode The itemControl code to attach.
     * @return The constructed Questionnaire Item Builder.
     */
    private fun createItemBuilderWithControl(
        linkId: String,
        type: Questionnaire.QuestionnaireItemType,
        itemControlCode: String,
    ): Questionnaire.Item.Builder {
        val ext =
            Extension
                .Builder(url = SdcExtensions.ITEM_CONTROL)
                .apply {
                    value =
                        Extension.Value.CodeableConcept(
                            CodeableConcept
                                .Builder()
                                .apply {
                                    coding.add(
                                        Coding
                                            .Builder()
                                            .apply {
                                                code = Code.Builder().apply { value = itemControlCode }
                                            },
                                    )
                                }.build(),
                        )
                }

        return Questionnaire.Item
            .Builder(
                linkId = fhirStr(linkId),
                type = Enumeration(value = type),
            ).apply {
                extension.add(ext)
            }
    }

    /**
     * Tests BodyMapLocation creation and serialization.
     */
    @Test
    fun testBodyMapLocationSerialization() {
        val locWithSnomed =
            BodyMapLocation(
                regionId = "anterior_arm",
                displayName = "Right Arm",
                snomedCode = "368209003",
                xPercent = 25.5f,
                yPercent = 35.0f,
            )
        assertEquals("Right Arm [368209003] (25%, 35%)", locWithSnomed.toSerializedString())

        val locWithoutSnomed =
            BodyMapLocation(
                regionId = "head",
                displayName = "Head",
                snomedCode = null,
                xPercent = 50.0f,
                yPercent = 10.0f,
            )
        assertEquals("Head (50%, 10%)", locWithoutSnomed.toSerializedString())
    }

    /**
     * Tests FitzpatrickScaleDefaults constants and parsing.
     */
    @Test
    fun testFitzpatrickScaleDefaults() {
        val types = FitzpatrickScaleDefaults.ALL_TYPES
        assertEquals(6, types.size)
        assertEquals("I", types[0].romanNumeral)
        assertEquals("VI", types[5].romanNumeral)

        for (t in 1..6) {
            assertNotNull(getFitzpatrickTitleResource(t))
            assertNotNull(getFitzpatrickDescResource(t))
        }

        val parsedColor = parseHexColor("#F8D9C8")
        assertNotNull(parsedColor)
    }

    /**
     * Tests PainScaleDefaults constants and resource mapping.
     */
    @Test
    fun testPainScaleDefaults() {
        val levels = PainScaleDefaults.LEVELS
        assertEquals(6, levels.size)
        assertEquals(0, levels.first().score)
        assertEquals(10, levels.last().score)

        for (score in 0..10) {
            assertNotNull(getPainScoreResource(score))
            assertNotNull(getPainScoreColor(score))
        }
    }

    /**
     * Tests anatomical region resolution for anterior and posterior coordinates.
     */
    @Test
    fun testResolveAnatomicalRegion() {
        val (headRes, headCode) = resolveAnatomicalRegion(50f, 10f, false)
        assertEquals("69536005", headCode)

        val (chestRes, chestCode) = resolveAnatomicalRegion(50f, 30f, false)
        assertEquals("51185008", chestCode)

        val (backRes, backCode) = resolveAnatomicalRegion(50f, 30f, true)
        assertEquals("181533004", backCode)

        val (armRes, armCode) = resolveAnatomicalRegion(15f, 35f, false)
        assertEquals("368209003", armCode)

        val (legRes, legCode) = resolveAnatomicalRegion(70f, 80f, false)
        assertEquals("368214008", legCode)

        // Posterior: viewer left is patient left, viewer right is patient right
        val (postLeftArmRes, postLeftArmCode) = resolveAnatomicalRegion(15f, 35f, true)
        assertEquals("368208006", postLeftArmCode)

        val (postRightArmRes, postRightArmCode) = resolveAnatomicalRegion(85f, 35f, true)
        assertEquals("368209003", postRightArmCode)

        val (postLeftLegRes, postLeftLegCode) = resolveAnatomicalRegion(35f, 80f, true)
        assertEquals("368214008", postLeftLegCode)

        val (postRightLegRes, postRightLegCode) = resolveAnatomicalRegion(65f, 80f, true)
        assertEquals("368215009", postRightLegCode)
    }

    /**
     * Tests SDC extension item identification helpers.
     */
    @Test
    fun testItemControlExtensionHelpers() {
        val painItem = createItemBuilderWithControl("p1", Questionnaire.QuestionnaireItemType.Integer, "pain-vas").build()
        assertTrue(painItem.isVisualPainControl())
        assertFalse(painItem.isFitzpatrickPalette())
        assertFalse(painItem.isBodyMap())
        assertFalse(painItem.isSegmentedControl())

        val paletteItem = createItemBuilderWithControl("f1", Questionnaire.QuestionnaireItemType.Choice, "palette").build()
        assertTrue(paletteItem.isFitzpatrickPalette())
        assertFalse(paletteItem.isVisualPainControl())

        val bodyMapItem = createItemBuilderWithControl("b1", Questionnaire.QuestionnaireItemType.String, "body-map").build()
        assertTrue(bodyMapItem.isBodyMap())
        assertFalse(bodyMapItem.isSegmentedControl())

        val segmentedItem = createItemBuilderWithControl("s1", Questionnaire.QuestionnaireItemType.Choice, "segmented-control").build()
        assertTrue(segmentedItem.isSegmentedControl())
        assertFalse(segmentedItem.isBodyMap())
    }

    /**
     * Tests QuestionnaireResponseGenerator serialization for visual control answer models.
     */
    @Test
    fun testQuestionnaireResponseGeneratorWithVisualControls() {
        val painItemBuilder = createItemBuilderWithControl("pain1", Questionnaire.QuestionnaireItemType.Integer, "pain-vas")
        val fitzItemBuilder = createItemBuilderWithControl("fitz1", Questionnaire.QuestionnaireItemType.Choice, "palette")
        val bodyItemBuilder = createItemBuilderWithControl("body1", Questionnaire.QuestionnaireItemType.String, "body-map")

        val questionnaire =
            Questionnaire
                .Builder(
                    status = Enumeration(value = PublicationStatus.Active),
                ).apply {
                    id = "q-visual"
                    item.add(painItemBuilder)
                    item.add(fitzItemBuilder)
                    item.add(bodyItemBuilder)
                }.build()

        val fitzSwatch = FitzpatrickSkinType(3, "III", "#D49B72", "fitzpatrick_type_3", "fitzpatrick_desc_3")
        val bodyLoc = BodyMapLocation("chest", "Chest", "51185008", 50f, 32f)

        val answers =
            mapOf(
                "pain1" to 6,
                "fitz1" to fitzSwatch,
                "body1" to bodyLoc,
            )

        val response = QuestionnaireResponseGenerator.generate(questionnaire, answers)
        assertNotNull(response)
        assertEquals(3, response.item.size)

        val painAnswer =
            response.item
                .firstOrNull { it.linkId.value == "pain1" }
                ?.answer
                ?.firstOrNull()
        val painVal = (painAnswer?.value as? com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.Integer)?.value?.value
        assertEquals(6, painVal)

        val fitzAnswer =
            response.item
                .firstOrNull { it.linkId.value == "fitz1" }
                ?.answer
                ?.firstOrNull()
        val fitzVal = (fitzAnswer?.value as? com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String)?.value?.value
        assertEquals("Type III", fitzVal)

        val bodyAnswer =
            response.item
                .firstOrNull { it.linkId.value == "body1" }
                ?.answer
                ?.firstOrNull()
        val bodyVal = (bodyAnswer?.value as? com.google.fhir.model.r4.QuestionnaireResponse.Item.Answer.Value.String)?.value?.value
        assertEquals("Chest [51185008] (50%, 32%)", bodyVal)
    }
}
