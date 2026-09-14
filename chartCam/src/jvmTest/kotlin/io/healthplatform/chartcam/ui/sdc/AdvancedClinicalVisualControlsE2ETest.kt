/**
 * @file AdvancedClinicalVisualControlsE2ETest.kt
 * Contains declarations for AdvancedClinicalVisualControlsE2ETest.kt.
 *
 * End-to-End workflow tests for rich clinical visual form controls:
 * Wong-Baker / VAS Pain Scale, Fitzpatrick Phototype Swatch Palette, and Anatomical Body Map.
 */
package io.healthplatform.chartcam.ui.sdc

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.fhir.model.r4.Enumeration
import com.google.fhir.model.r4.Extension
import com.google.fhir.model.r4.Questionnaire
import com.google.fhir.model.r4.QuestionnaireResponse
import com.google.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.fhir.QuestionnaireResponseGenerator
import io.healthplatform.chartcam.models.BodyMapLocation
import io.healthplatform.chartcam.models.FitzpatrickSkinType
import io.healthplatform.chartcam.ui.sdc.controls.BodyMapPinDropControl
import io.healthplatform.chartcam.ui.sdc.controls.FitzpatrickPaletteControl
import io.healthplatform.chartcam.ui.sdc.controls.VisualPainScaleControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.google.fhir.model.r4.String as FhirString

/**
 * End-to-End test suite verifying that advanced visual clinical controls
 * interact properly in the Compose UI and serialize accurately to FHIR QuestionnaireResponse.
 */
@OptIn(ExperimentalTestApi::class)
class AdvancedClinicalVisualControlsE2ETest {
    /**
     * Builds a questionnaire with Pain Scale, Fitzpatrick, and Body Map items.
     *
     * @return Fully populated [Questionnaire] with SDC itemControl extensions.
     */
    private fun buildVisualControlsQuestionnaire(): Questionnaire {
        fun createItemControlExt(controlName: String): Extension.Builder =
            Extension.Builder(url = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl").apply {
                value = Extension.Value.String(FhirString.Builder().apply { value = controlName }.build())
            }

        val painItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "pain_scale" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                ).apply {
                    text = FhirString.Builder().apply { value = "Patient Reported Pain Level" }
                    extension.add(createItemControlExt("pain-vas"))
                }

        val fitzpatrickItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "fitzpatrick_skin" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                ).apply {
                    text = FhirString.Builder().apply { value = "Fitzpatrick Skin Phototype" }
                    extension.add(createItemControlExt("fitzpatrick"))
                }

        val bodyMapItem =
            Questionnaire.Item
                .Builder(
                    linkId = FhirString.Builder().apply { value = "lesion_location" },
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = FhirString.Builder().apply { value = "Lesion Anatomical Site" }
                    extension.add(createItemControlExt("body-map"))
                }

        return Questionnaire
            .Builder(status = Enumeration(value = PublicationStatus.Active))
            .apply {
                title = FhirString.Builder().apply { value = "Dermatological Visual Assessment" }
                item.add(painItem)
                item.add(fitzpatrickItem)
                item.add(bodyMapItem)
            }.build()
    }

    /**
     * Tests interactive selection of all three visual controls, verifying UI state and FHIR response serialization.
     */
    @Test
    fun testVisualControlsUiInteractionAndSerialization() =
        runComposeUiTest {
            var selectedPain: Int? = null
            var selectedFitzpatrick: FitzpatrickSkinType? = null
            var selectedLocation by mutableStateOf<BodyMapLocation?>(null)

            setContent {
                MaterialTheme {
                    androidx.compose.foundation.layout.Column {
                        var internalPain by remember { mutableStateOf<Int?>(null) }
                        var internalFitz by remember { mutableStateOf<FitzpatrickSkinType?>(null) }

                        VisualPainScaleControl(
                            value = internalPain,
                            onValueChange = {
                                internalPain = it
                                selectedPain = it
                            },
                            label = "Pain Level (0-10)",
                        )

                        FitzpatrickPaletteControl(
                            selectedType = internalFitz,
                            onTypeSelected = {
                                internalFitz = it
                                selectedFitzpatrick = it
                            },
                            label = "Fitzpatrick Phototyping",
                        )

                        BodyMapPinDropControl(
                            location = selectedLocation,
                            onLocationChanged = {
                                selectedLocation = it
                            },
                            label = "Anatomical Location",
                        )
                    }
                }
            }

            // 1. Select Pain level 6 ("Hurts Even More")
            onNodeWithTag("PainFaceButton_6", useUnmergedTree = true).assertExists().performClick()
            waitForIdle()
            assertEquals(6, selectedPain, "Pain level 6 should be selected")

            // 2. Select Fitzpatrick Type IV (Mediterranean / Olive)
            onNodeWithTag("FitzpatrickOption_4").assertExists().performClick()
            waitForIdle()
            assertNotNull(selectedFitzpatrick)
            assertEquals("IV", selectedFitzpatrick.romanNumeral)

            // 3. Select Body Map location programmatically or via pin
            selectedLocation =
                BodyMapLocation(
                    regionId = "anterior",
                    displayName = "Right Arm",
                    snomedCode = "368209003",
                    xPercent = 20.0f,
                    yPercent = 30.0f,
                )
            waitForIdle()
            onNodeWithText("Pinned: Right Arm", substring = true).assertExists()

            // 4. Generate FHIR QuestionnaireResponse from gathered answers
            val questionnaire = buildVisualControlsQuestionnaire()
            val answersMap =
                mapOf<String, Any>(
                    "pain_scale" to (selectedPain ?: 0),
                    "fitzpatrick_skin" to selectedFitzpatrick,
                    "lesion_location" to (selectedLocation ?: ""),
                )

            val qr = QuestionnaireResponseGenerator.generate(questionnaire, answersMap)

            // 5. Verify answers in FHIR resource
            val painAnswer =
                qr.item
                    .firstOrNull { it.linkId.value == "pain_scale" }
                    ?.answer
                    ?.firstOrNull()
            val painVal = (painAnswer?.value as? QuestionnaireResponse.Item.Answer.Value.Integer)?.value?.value
            assertEquals(6, painVal)

            val fitzAnswer =
                qr.item
                    .firstOrNull { it.linkId.value == "fitzpatrick_skin" }
                    ?.answer
                    ?.firstOrNull()
            val fitzVal = (fitzAnswer?.value as? QuestionnaireResponse.Item.Answer.Value.String)?.value?.value
            assertEquals("Type IV", fitzVal)

            val bodyMapAnswer =
                qr.item
                    .firstOrNull { it.linkId.value == "lesion_location" }
                    ?.answer
                    ?.firstOrNull()
            val serializedLocation = (bodyMapAnswer?.value as? QuestionnaireResponse.Item.Answer.Value.String)?.value?.value ?: ""
            assertTrue(serializedLocation.contains("Right Arm"))
            assertTrue(serializedLocation.contains("368209003"))
        }
}
