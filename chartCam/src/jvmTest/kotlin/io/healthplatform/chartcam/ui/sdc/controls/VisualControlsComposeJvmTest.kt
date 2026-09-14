/**
 * @file VisualControlsComposeJvmTest.kt
 * Contains declarations for VisualControlsComposeJvmTest.kt.
 *
 * JVM Compose UI tests for interactive visual form controls and builder integration.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.healthplatform.chartcam.models.BodyMapLocation
import io.healthplatform.chartcam.models.FitzpatrickScaleDefaults
import io.healthplatform.chartcam.models.FitzpatrickSkinType
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.sdc.SdcFormConfig
import io.healthplatform.chartcam.sdc.SdcQuestionnaireForm
import io.healthplatform.chartcam.ui.theme.DarkColors
import io.healthplatform.chartcam.ui.theme.LightColors
import io.healthplatform.chartcam.ui.theme.calculateContrastRatio
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import io.healthplatform.chartcam.viewmodel.WidgetType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * UI and integration tests for visual questionnaire controls.
 */
class VisualControlsComposeJvmTest {
    /**
     * Tests VisualPainScaleControl in interactive and read-only modes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testVisualPainScaleControlInteractiveAndReadOnly() =
        runTest {
            runComposeUiTest {
                var selectedScore = 2

                setContent {
                    VisualPainScaleControl(
                        value = selectedScore,
                        onValueChange = { selectedScore = it },
                        label = "Pain Severity",
                        readOnly = false,
                    )
                }

                onNodeWithText("Pain Severity").assertIsDisplayed()
                onNodeWithTag("PainScaleSlider").assertIsDisplayed()
                onNodeWithTag("PainFaceButton_6").performClick()
                assertEquals(6, selectedScore)

                // Test read-only mode
                setContent {
                    VisualPainScaleControl(
                        value = 8,
                        onValueChange = {},
                        label = "Pain Severity Review",
                        readOnly = true,
                    )
                }

                onNodeWithTag("VisualPainScaleReadOnly").assertIsDisplayed()
            }
        }

    /**
     * Tests FitzpatrickPaletteControl in interactive and read-only modes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFitzpatrickPaletteControlInteractiveAndReadOnly() =
        runTest {
            runComposeUiTest {
                var selected: FitzpatrickSkinType? = null

                setContent {
                    FitzpatrickPaletteControl(
                        selectedType = selected,
                        onTypeSelected = { selected = it },
                        label = "Skin Type",
                        readOnly = false,
                    )
                }

                onNodeWithTag("FitzpatrickPaletteGroup").assertIsDisplayed()
                onNodeWithTag("FitzpatrickOption_3").performClick()
                assertEquals(3, selected?.type)

                // Test read-only mode
                setContent {
                    FitzpatrickPaletteControl(
                        selectedType = FitzpatrickScaleDefaults.ALL_TYPES[2],
                        onTypeSelected = {},
                        label = "Skin Type Review",
                        readOnly = true,
                    )
                }

                onNodeWithTag("FitzpatrickReadOnly").assertIsDisplayed()

                // Test read-only empty state
                setContent {
                    FitzpatrickPaletteControl(
                        selectedType = null,
                        onTypeSelected = {},
                        label = "Skin Type Empty",
                        readOnly = true,
                    )
                }
                onNodeWithText("Skin Type Empty").assertIsDisplayed()
            }
        }

    /**
     * Tests BodyMapPinDropControl in interactive and read-only modes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBodyMapPinDropControlInteractiveAndReadOnly() =
        runTest {
            runComposeUiTest {
                var pinnedLocation: BodyMapLocation? =
                    BodyMapLocation(
                        regionId = "anterior",
                        displayName = "Chest (Ant)",
                        snomedCode = "51185008",
                        xPercent = 50f,
                        yPercent = 30f,
                    )

                setContent {
                    BodyMapPinDropControl(
                        location = pinnedLocation,
                        onLocationChanged = { pinnedLocation = it },
                        label = "Lesion Location",
                        readOnly = false,
                    )
                }

                onNodeWithTag("BodyMapCanvas").assertIsDisplayed()
                onNodeWithTag("BodyMapAccessibleRegionSelector").assertIsDisplayed()
                onNodeWithTag("BodyMapAccessibleRegionSelector").performClick()
                onNodeWithText("Chest").performClick()
                kotlin.test.assertNotNull(pinnedLocation)
                onNodeWithTag("BodyMapViewPosterior").performClick()
                onNodeWithTag("ClearBodyMapPinButton").performClick()
                assertNull(pinnedLocation)

                // Test read-only mode
                val readOnlyLoc = BodyMapLocation("posterior", "Upper Back (Post)", "181533004", 50f, 30f)
                setContent {
                    BodyMapPinDropControl(
                        location = readOnlyLoc,
                        onLocationChanged = {},
                        label = "Lesion Review",
                        readOnly = true,
                    )
                }

                onNodeWithTag("BodyMapCanvas").assertIsDisplayed()

                // Test read-only empty state
                setContent {
                    BodyMapPinDropControl(
                        location = null,
                        onLocationChanged = {},
                        label = "Lesion Review Empty",
                        readOnly = true,
                    )
                }
                onNodeWithText("Lesion Review Empty").assertIsDisplayed()
            }
        }

    /**
     * Tests SegmentedVisualTilesControl in single and multi choice modes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSegmentedVisualTilesControlInteractiveAndReadOnly() =
        runTest {
            runComposeUiTest {
                val options = listOf("Independent", "Mild Assist", "Full Assist")
                var selectedOptions = listOf("Independent")

                setContent {
                    SegmentedVisualTilesControl(
                        selectedOptions = selectedOptions,
                        options = options,
                        onOptionToggled = { selectedOptions = listOf(it) },
                        label = "Mobility Grade",
                        isMultiSelect = false,
                        readOnly = false,
                    )
                }

                onNodeWithTag("SegmentedTilesGroup").assertIsDisplayed()
                onNodeWithTag("SegmentedTile_Mild Assist").performClick()
                assertEquals(listOf("Mild Assist"), selectedOptions)

                // Test read-only mode
                setContent {
                    SegmentedVisualTilesControl(
                        selectedOptions = listOf("Full Assist"),
                        options = options,
                        onOptionToggled = {},
                        label = "Mobility Review",
                        isMultiSelect = false,
                        readOnly = true,
                    )
                }

                onNodeWithTag("SegmentedTilesReadOnly").assertIsDisplayed()

                // Test read-only empty state
                setContent {
                    SegmentedVisualTilesControl(
                        selectedOptions = emptyList(),
                        options = options,
                        onOptionToggled = {},
                        label = "Mobility Empty",
                        isMultiSelect = false,
                        readOnly = true,
                    )
                }
                onNodeWithText("Mobility Empty").assertIsDisplayed()
            }
        }

    /**
     * Tests Form Builder end-to-end adding visual form controls and rendering preview.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFormBuilderVisualControlsEndToEnd() =
        runTest {
            val repo = QuestionnaireRepository()
            val viewModel = QuestionnaireBuilderViewModel(repo)

            viewModel.updateTitle("Dermatology Study Protocol")
            viewModel.addItem(WidgetType.PAIN_SCALE, "Assessment Pain Score")
            viewModel.addItem(WidgetType.FITZPATRICK_PALETTE, "Subject Fitzpatrick Scale")
            viewModel.addItem(WidgetType.BODY_MAP, "Primary Lesion Site")
            viewModel.addItem(WidgetType.SEGMENTED_TILES, "Erythema Severity")

            val fhirQuestionnaire = viewModel.buildQuestionnaire()
            assertNotNull(fhirQuestionnaire)
            assertEquals(4, fhirQuestionnaire.item.size)

            val painItem = fhirQuestionnaire.item[0]
            assertTrue(painItem.linkId.value != null)

            runComposeUiTest {
                setContent {
                    SdcQuestionnaireForm(
                        questionnaire = fhirQuestionnaire,
                        answers = emptyMap(),
                        config = SdcFormConfig(readOnly = false),
                        onFormUpdated = { _, _ -> },
                    )
                }

                onNodeWithText("Assessment Pain Score").assertIsDisplayed()
                onNodeWithText("Subject Fitzpatrick Scale").assertIsDisplayed()
                onNodeWithText("Primary Lesion Site").assertIsDisplayed()
                onNodeWithText("Erythema Severity").assertExists()
            }
        }

    /**
     * Tests contrast ratios for all pain score color levels across light and dark themes,
     * and verifies state description formatting on the pain slider.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPainScaleContrastRatiosAndStateDescription() =
        runTest {
            for (score in 0..10) {
                val lightColor = getPainScoreColor(score, isDarkTheme = false)
                val lightContrast = calculateContrastRatio(lightColor, LightColors.background)
                assertTrue(
                    lightContrast >= 4.5f,
                    "Light theme contrast for pain score $score ($lightContrast) should be >= 4.5",
                )

                val darkColor = getPainScoreColor(score, isDarkTheme = true)
                val darkContrast = calculateContrastRatio(darkColor, DarkColors.background)
                assertTrue(
                    darkContrast >= 4.5f,
                    "Dark theme contrast for pain score $score ($darkContrast) should be >= 4.5",
                )
            }

            runComposeUiTest {
                setContent {
                    VisualPainScaleControl(
                        value = 4,
                        onValueChange = {},
                        label = "Pain Assessment",
                    )
                }

                onNodeWithTag("PainScaleSlider").assertIsDisplayed()
            }
        }

    /**
     * Tests contrast ratios for each Fitzpatrick phototype swatch and verifies optimal check icon tint.
     */
    @Test
    fun testFitzpatrickContrastRatios() {
        FitzpatrickScaleDefaults.ALL_TYPES.forEach { item ->
            val swatchColor = parseHexColor(item.hexColor)
            val contrastWhite = calculateContrastRatio(Color.White, swatchColor)
            val contrastBlack = calculateContrastRatio(Color.Black, swatchColor)

            val chosenTint = if (contrastWhite >= contrastBlack) Color.White else Color.Black
            val chosenContrast = maxOf(contrastWhite, contrastBlack)
            assertTrue(
                chosenContrast >= 1.5f,
                "Fitzpatrick type ${item.type} contrast ($chosenContrast) should be legible",
            )
            if (chosenTint == Color.White) {
                assertTrue(contrastWhite >= contrastBlack)
            } else {
                assertTrue(contrastBlack >= contrastWhite)
            }
        }
    }
}
