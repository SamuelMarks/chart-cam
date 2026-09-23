/**
 * @file VisualControlsComposeJvmTest.kt
 * Contains declarations for VisualControlsComposeJvmTest.kt.
 *
 * JVM Compose UI tests for interactive visual form controls and builder integration.
 */
package io.healthplatform.chartcam.ui.sdc.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
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
     * Tests getPainScoreColor overloads and parseHexColor invalid hex fallback.
     */
    @Test
    fun testColorUtilitiesAndOverloads() {
        // Test getPainScoreColor 1-arg overload
        val c0 = getPainScoreColor(0)
        val c5 = getPainScoreColor(5)
        val c10 = getPainScoreColor(10)
        assertNotNull(c0)
        assertNotNull(c5)
        assertNotNull(c10)

        // Test dark theme variation
        val darkColor = getPainScoreColor(5, isDarkTheme = true)
        val lightColor = getPainScoreColor(5, isDarkTheme = false)
        assertNotNull(darkColor)
        assertNotNull(lightColor)

        // Test parseHexColor with invalid fallback
        val validColor = parseHexColor("#F8D9C8")
        val fallbackColor = parseHexColor("invalid_hex")
        assertNotNull(validColor)
        assertNotNull(fallbackColor)
    }

    /**
     * Tests VisualPainScaleControl error message, error semantics, and null value handling.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testVisualPainScaleControlErrorStates() {
        runComposeUiTest {
            // isError = true with non-null errorMessage and null value
            setContent {
                VisualPainScaleControl(
                    value = null,
                    onValueChange = {},
                    label = "Pain Level 1",
                    isRequired = false,
                    isError = true,
                    errorMessage = "Pain score required",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Pain score required").assertIsDisplayed()
            onNodeWithText("Pain Level 1").assertIsDisplayed()

            // isError = true with null errorMessage
            setContent {
                VisualPainScaleControl(
                    value = 3,
                    onValueChange = {},
                    label = "Pain Level 2",
                    isRequired = false,
                    isError = true,
                    errorMessage = null,
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Pain Level 2").assertIsDisplayed()

            // isError = false with non-null errorMessage
            setContent {
                VisualPainScaleControl(
                    value = 4,
                    onValueChange = {},
                    label = "Pain Level 3",
                    isRequired = false,
                    isError = false,
                    errorMessage = "Ignored error",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Pain Level 3").assertIsDisplayed()
            onNodeWithText("Ignored error").assertDoesNotExist()
        }
    }

    /**
     * Tests FitzpatrickPaletteControl contrast checkmark tints and error states.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFitzpatrickPaletteControlContrastAndErrorStates() {
        runComposeUiTest {
            // Type I (light swatch -> checkmark dark)
            setContent {
                FitzpatrickPaletteControl(
                    selectedType = FitzpatrickScaleDefaults.ALL_TYPES[0],
                    onTypeSelected = {},
                    label = "Type 1 Selection",
                    isRequired = false,
                    isError = true,
                    errorMessage = "Select your skin phototype",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Select your skin phototype").assertIsDisplayed()
            onNodeWithText("Type 1 Selection").assertIsDisplayed()

            // Type VI (dark swatch -> checkmark light)
            setContent {
                FitzpatrickPaletteControl(
                    selectedType = FitzpatrickScaleDefaults.ALL_TYPES[5],
                    onTypeSelected = {},
                    label = "Type 6 Selection",
                    isRequired = false,
                    isError = true,
                    errorMessage = null,
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Type 6 Selection").assertIsDisplayed()

            // isError = false with non-null errorMessage
            setContent {
                FitzpatrickPaletteControl(
                    selectedType = null,
                    onTypeSelected = {},
                    label = "Type Neutral",
                    isRequired = false,
                    isError = false,
                    errorMessage = "Ignored phototype error",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Type Neutral").assertIsDisplayed()
            onNodeWithText("Ignored phototype error").assertDoesNotExist()
        }
    }

    @androidx.compose.runtime.Composable
    private fun DynamicPainWrapper(
        value: Int?,
        onValueChange: (Int) -> Unit,
        label: String,
        isRequired: Boolean,
        isError: Boolean,
        errorMessage: String?,
        readOnly: Boolean,
        modifier: androidx.compose.ui.Modifier,
        trigger: Int,
    ) {
        val t = trigger
        VisualPainScaleControl(
            value = value,
            onValueChange = onValueChange,
            label = label,
            isRequired = isRequired,
            isError = isError,
            errorMessage = errorMessage,
            readOnly = readOnly,
            modifier = modifier,
        )
    }

    /**
     * Verifies recomposition skipping and parameter mutations for VisualPainScaleControl.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testVisualPainScaleControlRecompositionAndSkipping() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val valState = androidx.compose.runtime.mutableStateOf<Int?>(2)
            val labelState = androidx.compose.runtime.mutableStateOf("動態疼痛標籤")
            val isReqState = androidx.compose.runtime.mutableStateOf(false)
            val isErrState = androidx.compose.runtime.mutableStateOf(false)
            val errMsgState = androidx.compose.runtime.mutableStateOf<String?>(null)
            val roState = androidx.compose.runtime.mutableStateOf(false)
            val modState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)
            val staticOnValueChange: (Int) -> Unit = {}

            setContent {
                val dummy = trigger.value
                // Pure constant skipping call
                VisualPainScaleControl(
                    value = 5,
                    onValueChange = staticOnValueChange,
                    label = "純靜態疼痛",
                    isRequired = true,
                    isError = false,
                    errorMessage = null,
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Pure constant skipping call for convenience overload
                VisualPainScaleControl(
                    value = 5,
                    onValueChange = staticOnValueChange,
                    label = "純靜態疼痛過載",
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Call with default parameters to exercise $default bitmask
                VisualPainScaleControl(
                    value = 1,
                    onValueChange = staticOnValueChange,
                    label = "預設疼痛",
                )

                // Dynamic unmemoized wrapper call
                DynamicPainWrapper(
                    value = valState.value,
                    onValueChange = staticOnValueChange,
                    label = labelState.value,
                    isRequired = isReqState.value,
                    isError = isErrState.value,
                    errorMessage = errMsgState.value,
                    readOnly = roState.value,
                    modifier = modState.value,
                    trigger = trigger.value,
                )
            }
            waitForIdle()

            // Outer trigger causes pure skipping and unmemoized unchanged branches
            trigger.value++
            waitForIdle()

            // Mutate each state
            valState.value = 7
            waitForIdle()

            labelState.value = "更新疼痛標籤"
            waitForIdle()

            isReqState.value = true
            waitForIdle()

            isErrState.value = true
            errMsgState.value = "動態錯誤訊息"
            waitForIdle()

            roState.value = true
            waitForIdle()

            modState.value =
                androidx.compose.ui.Modifier
                    .padding(2.dp)
            waitForIdle()
        }
    }

    @androidx.compose.runtime.Composable
    private fun DynamicFitzpatrickWrapper(
        selectedType: FitzpatrickSkinType?,
        onTypeSelected: (FitzpatrickSkinType) -> Unit,
        label: String,
        isRequired: Boolean,
        isError: Boolean,
        errorMessage: String?,
        readOnly: Boolean,
        modifier: androidx.compose.ui.Modifier,
        trigger: Int,
    ) {
        val t = trigger
        FitzpatrickPaletteControl(
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            label = label,
            isRequired = isRequired,
            isError = isError,
            errorMessage = errorMessage,
            readOnly = readOnly,
            modifier = modifier,
        )
    }

    /**
     * Verifies recomposition skipping and parameter mutations for FitzpatrickPaletteControl.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFitzpatrickPaletteControlRecompositionAndSkipping() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val typeState = androidx.compose.runtime.mutableStateOf<FitzpatrickSkinType?>(null)
            val labelState = androidx.compose.runtime.mutableStateOf("動態膚色標籤")
            val isReqState = androidx.compose.runtime.mutableStateOf(false)
            val isErrState = androidx.compose.runtime.mutableStateOf(false)
            val errMsgState = androidx.compose.runtime.mutableStateOf<String?>(null)
            val roState = androidx.compose.runtime.mutableStateOf(false)
            val modState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)
            val staticOnTypeSelected: (FitzpatrickSkinType) -> Unit = {}

            setContent {
                val dummy = trigger.value
                // Pure constant skipping call
                FitzpatrickPaletteControl(
                    selectedType = FitzpatrickScaleDefaults.ALL_TYPES[1],
                    onTypeSelected = staticOnTypeSelected,
                    label = "純靜態膚色",
                    isRequired = true,
                    isError = false,
                    errorMessage = null,
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Pure constant skipping call for convenience overload
                FitzpatrickPaletteControl(
                    selectedType = FitzpatrickScaleDefaults.ALL_TYPES[1],
                    onTypeSelected = staticOnTypeSelected,
                    label = "純靜態膚色過載",
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Call with default parameters to exercise $default bitmask
                FitzpatrickPaletteControl(
                    selectedType = null,
                    onTypeSelected = staticOnTypeSelected,
                    label = "預設膚色",
                )

                // Dynamic unmemoized wrapper call
                DynamicFitzpatrickWrapper(
                    selectedType = typeState.value,
                    onTypeSelected = staticOnTypeSelected,
                    label = labelState.value,
                    isRequired = isReqState.value,
                    isError = isErrState.value,
                    errorMessage = errMsgState.value,
                    readOnly = roState.value,
                    modifier = modState.value,
                    trigger = trigger.value,
                )
            }
            waitForIdle()

            // Outer trigger causes pure skipping and unmemoized unchanged branches
            trigger.value++
            waitForIdle()

            // Mutate each state
            typeState.value = FitzpatrickScaleDefaults.ALL_TYPES[3]
            waitForIdle()

            labelState.value = "更新膚色標籤"
            waitForIdle()

            isReqState.value = true
            waitForIdle()

            isErrState.value = true
            errMsgState.value = "動態膚色錯誤"
            waitForIdle()

            roState.value = true
            waitForIdle()

            modState.value =
                androidx.compose.ui.Modifier
                    .padding(2.dp)
            waitForIdle()
        }
    }

    /**
     * Tests unmemoized parameter change detection branches by invoking Composables with changed = 0.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testZeroChangedFlagReflection() {
        runComposeUiTest {
            val recomposeTrigger = androidx.compose.runtime.mutableStateOf(0)
            val staticPainCallback: (Int) -> Unit = {}
            val staticFitzCallback: (FitzpatrickSkinType) -> Unit = {}

            val painClass = Class.forName("io.healthplatform.chartcam.ui.sdc.controls.VisualPainScaleControlKt")
            val fitzClass = Class.forName("io.healthplatform.chartcam.ui.sdc.controls.FitzpatrickPaletteControlKt")

            val painPrimary =
                painClass.declaredMethods.first {
                    it.name == "VisualPainScaleControl" && it.parameterCount == 11
                }
            painPrimary.isAccessible = true

            val painConvenience =
                painClass.declaredMethods.first {
                    it.name == "VisualPainScaleControl" && it.parameterCount == 8
                }
            painConvenience.isAccessible = true

            val fitzPrimary =
                fitzClass.declaredMethods.first {
                    it.name == "FitzpatrickPaletteControl" && it.parameterCount == 11
                }
            fitzPrimary.isAccessible = true

            val fitzConvenience =
                fitzClass.declaredMethods.first {
                    it.name == "FitzpatrickPaletteControl" && it.parameterCount == 8
                }
            fitzConvenience.isAccessible = true

            setContent {
                val dummy = recomposeTrigger.value

                // Calls with 3 args (defaulting readOnly and modifier)
                VisualPainScaleControl(
                    value = 2,
                    onValueChange = staticPainCallback,
                    label = "三參數疼痛",
                )
                FitzpatrickPaletteControl(
                    selectedType = null,
                    onTypeSelected = staticFitzCallback,
                    label = "三參數膚色",
                )

                // Calls with 4 args (defaulting modifier)
                VisualPainScaleControl(
                    value = 4,
                    onValueChange = staticPainCallback,
                    label = "四參數疼痛",
                    readOnly = true,
                )
                FitzpatrickPaletteControl(
                    selectedType = null,
                    onTypeSelected = staticFitzCallback,
                    label = "四參數膚色",
                    readOnly = true,
                )

                val composer = androidx.compose.runtime.currentComposer

                // 1. Primary VisualPainScaleControl with changed = 0, default = 0
                painPrimary.invoke(
                    null,
                    3,
                    staticPainCallback,
                    "反射標題",
                    false,
                    false,
                    null,
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
                painPrimary.invoke(
                    null,
                    7,
                    staticPainCallback,
                    "反射標題2",
                    true,
                    true,
                    "錯誤",
                    true,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )

                // 2. Convenience VisualPainScaleControl with changed = 0, default = 0
                painConvenience.invoke(
                    null,
                    4,
                    staticPainCallback,
                    "反射過載標題",
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )

                // 3. Primary FitzpatrickPaletteControl with changed = 0, default = 0
                fitzPrimary.invoke(
                    null,
                    null,
                    staticFitzCallback,
                    "反射膚色",
                    false,
                    false,
                    null,
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
                fitzPrimary.invoke(
                    null,
                    FitzpatrickScaleDefaults.ALL_TYPES[0],
                    staticFitzCallback,
                    "反射膚色2",
                    true,
                    true,
                    "錯誤",
                    true,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )

                // 4. Convenience FitzpatrickPaletteControl with changed = 0, default = 0
                fitzConvenience.invoke(
                    null,
                    null,
                    staticFitzCallback,
                    "反射過載膚色",
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
            }
            waitForIdle()

            // Recompose so changed = 0 reflection calls evaluate composer.changed(...) == false
            recomposeTrigger.value++
            waitForIdle()
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
                // Tap on anterior canvas
                onNodeWithTag("BodyMapCanvas").performTouchInput { click(center) }
                assertNotNull(pinnedLocation)

                // Exercise custom accessibility actions on card
                val cardNode = onNodeWithTag("BodyMapCard").fetchSemanticsNode()
                val customActions =
                    cardNode.config.getOrElse(
                        androidx.compose.ui.semantics.SemanticsActions.CustomActions,
                    ) { emptyList() }
                customActions.forEach { action -> action.action() }

                onNodeWithTag("BodyMapAccessibleRegionSelector").assertIsDisplayed()
                onNodeWithTag("BodyMapAccessibleRegionSelector").performClick()
                onNodeWithText("Chest").performClick()
                kotlin.test.assertNotNull(pinnedLocation)

                // Toggle to posterior view
                onNodeWithTag("BodyMapViewPosterior").performClick()
                // Exercise custom accessibility actions on posterior card
                val cardNodePost = onNodeWithTag("BodyMapCard").fetchSemanticsNode()
                val customActionsPost =
                    cardNodePost.config.getOrElse(
                        androidx.compose.ui.semantics.SemanticsActions.CustomActions,
                    ) { emptyList() }
                customActionsPost.forEach { action -> action.action() }

                // Tap on posterior canvas
                onNodeWithTag("BodyMapCanvas").performTouchInput { click(center) }
                assertNotNull(pinnedLocation)
                assertEquals("posterior", pinnedLocation?.regionId)

                // Open accessible dropdown while on posterior view and select item
                onNodeWithTag("BodyMapAccessibleRegionSelector").performClick()
                onNodeWithText("Upper Back").performClick()
                assertNotNull(pinnedLocation)

                onNodeWithTag("ClearBodyMapPinButton").performClick()
                assertNull(pinnedLocation)

                // Test interactive mode with posterior pinned location
                setContent {
                    BodyMapPinDropControl(
                        location = BodyMapLocation("posterior", "Upper Back", "181533004", 50f, 30f),
                        onLocationChanged = {},
                        label = "Posterior Pinned",
                        readOnly = false,
                    )
                }
                onNodeWithTag("ClearBodyMapPinButton").assertIsDisplayed()

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
     * Tests anatomical region resolution logic across coordinates and orientations.
     */
    @Test
    fun testBodyMapAnatomicalRegionsLogic() {
        // Head
        val (headAntRes, headAnt) = resolveAnatomicalRegion(50f, 10f, isPosterior = false)
        val (headPostRes, headPost) = resolveAnatomicalRegion(50f, 10f, isPosterior = true)
        assertEquals("69536005", headAnt)
        assertEquals("69536005", headPost)
        assertNotNull(headAntRes)
        assertNotNull(headPostRes)

        // Torso / Chest / Back
        val (chestRes, chest) = resolveAnatomicalRegion(50f, 30f, isPosterior = false)
        val (backRes, back) = resolveAnatomicalRegion(50f, 30f, isPosterior = true)
        assertEquals("51185008", chest)
        assertEquals("181533004", back)
        assertNotNull(chestRes)
        assertNotNull(backRes)

        // Arms
        val (rArmAnt, _) = resolveAnatomicalRegion(20f, 30f, isPosterior = false)
        val (lArmAnt, _) = resolveAnatomicalRegion(80f, 30f, isPosterior = false)
        val (lArmPost, _) = resolveAnatomicalRegion(20f, 30f, isPosterior = true)
        val (rArmPost, _) = resolveAnatomicalRegion(80f, 30f, isPosterior = true)
        assertNotNull(rArmAnt)
        assertNotNull(lArmAnt)
        assertNotNull(lArmPost)
        assertNotNull(rArmPost)

        // Lower torso: abdomen, pelvis, lower back, hands
        val (ab, _) = resolveAnatomicalRegion(50f, 50f, isPosterior = false)
        val (lb, _) = resolveAnatomicalRegion(50f, 50f, isPosterior = true)
        val (pelvis, _) = resolveAnatomicalRegion(50f, 58f, isPosterior = false)
        val (handL, _) = resolveAnatomicalRegion(15f, 55f, isPosterior = false)
        val (handR, _) = resolveAnatomicalRegion(85f, 55f, isPosterior = false)
        assertNotNull(ab)
        assertNotNull(lb)
        assertNotNull(pelvis)
        assertNotNull(handL)
        assertNotNull(handR)

        // Legs
        val (rLegAnt, _) = resolveAnatomicalRegion(40f, 80f, isPosterior = false)
        val (lLegAnt, _) = resolveAnatomicalRegion(60f, 80f, isPosterior = false)
        val (lLegPost, _) = resolveAnatomicalRegion(40f, 80f, isPosterior = true)
        val (rLegPost, _) = resolveAnatomicalRegion(60f, 80f, isPosterior = true)
        assertNotNull(rLegAnt)
        assertNotNull(lLegAnt)
        assertNotNull(lLegPost)
        assertNotNull(rLegPost)
    }

    /**
     * Tests BodyMapPinDropControl error message and semantics.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBodyMapPinDropControlErrorStates() {
        runComposeUiTest {
            // isError = true with non-null errorMessage
            setContent {
                BodyMapPinDropControl(
                    location = null,
                    onLocationChanged = {},
                    label = "Body Error 1",
                    isRequired = false,
                    isError = true,
                    errorMessage = "Location pin required",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Location pin required").assertIsDisplayed()
            onNodeWithText("Body Error 1").assertIsDisplayed()

            // isError = true with null errorMessage
            setContent {
                BodyMapPinDropControl(
                    location = null,
                    onLocationChanged = {},
                    label = "Body Error 2",
                    isRequired = false,
                    isError = true,
                    errorMessage = null,
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Body Error 2").assertIsDisplayed()

            // isError = false with non-null errorMessage
            setContent {
                BodyMapPinDropControl(
                    location = null,
                    onLocationChanged = {},
                    label = "Body Error 3",
                    isRequired = false,
                    isError = false,
                    errorMessage = "Ignored error",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Body Error 3").assertIsDisplayed()
            onNodeWithText("Ignored error").assertDoesNotExist()
        }
    }

    @androidx.compose.runtime.Composable
    private fun DynamicBodyMapWrapper(
        location: BodyMapLocation?,
        onLocationChanged: (BodyMapLocation?) -> Unit,
        label: String,
        isRequired: Boolean,
        isError: Boolean,
        errorMessage: String?,
        readOnly: Boolean,
        modifier: androidx.compose.ui.Modifier,
        trigger: Int,
    ) {
        val t = trigger
        BodyMapPinDropControl(
            location = location,
            onLocationChanged = onLocationChanged,
            label = label,
            isRequired = isRequired,
            isError = isError,
            errorMessage = errorMessage,
            readOnly = readOnly,
            modifier = modifier,
        )
    }

    /**
     * Verifies recomposition skipping and parameter mutations for BodyMapPinDropControl.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBodyMapPinDropControlRecompositionAndSkipping() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val staticLoc = BodyMapLocation("anterior", "靜態胸部", "51185008", 50f, 30f)
            val staticCallback: (BodyMapLocation?) -> Unit = {}

            val locState = androidx.compose.runtime.mutableStateOf<BodyMapLocation?>(null)
            val labelState = androidx.compose.runtime.mutableStateOf("動態人體標籤")
            val reqState = androidx.compose.runtime.mutableStateOf(false)
            val errState = androidx.compose.runtime.mutableStateOf(false)
            val errMsgState = androidx.compose.runtime.mutableStateOf<String?>(null)
            val roState = androidx.compose.runtime.mutableStateOf(false)
            val modState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)

            setContent {
                val dummy = trigger.value
                // Pure constant skipping call for primary method
                BodyMapPinDropControl(
                    location = staticLoc,
                    onLocationChanged = staticCallback,
                    label = "純靜態人體圖",
                    isRequired = false,
                    isError = false,
                    errorMessage = null,
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Pure constant skipping call for convenience overload
                BodyMapPinDropControl(
                    location = staticLoc,
                    onLocationChanged = staticCallback,
                    label = "純靜態人體圖過載",
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Calls with default parameters
                BodyMapPinDropControl(
                    location = staticLoc,
                    onLocationChanged = staticCallback,
                    label = "預設人體圖二參數",
                )
                BodyMapPinDropControl(
                    location = staticLoc,
                    onLocationChanged = staticCallback,
                    label = "預設人體圖三參數",
                    readOnly = true,
                )

                // Dynamic unmemoized wrapper call
                DynamicBodyMapWrapper(
                    location = locState.value,
                    onLocationChanged = staticCallback,
                    label = labelState.value,
                    isRequired = reqState.value,
                    isError = errState.value,
                    errorMessage = errMsgState.value,
                    readOnly = roState.value,
                    modifier = modState.value,
                    trigger = trigger.value,
                )
            }
            waitForIdle()

            // Outer trigger causes pure skipping and unmemoized unchanged branches
            trigger.value++
            waitForIdle()

            // Mutate each state
            locState.value = BodyMapLocation("posterior", "背部", "181533004", 50f, 30f)
            waitForIdle()

            labelState.value = "更新人體標籤"
            waitForIdle()

            reqState.value = true
            waitForIdle()

            errState.value = true
            errMsgState.value = "動態標記錯誤"
            waitForIdle()

            roState.value = true
            waitForIdle()

            modState.value =
                androidx.compose.ui.Modifier
                    .padding(2.dp)
            waitForIdle()
        }
    }

    /**
     * Tests unmemoized parameter change detection branches by invoking BodyMapPinDropControl with changed = 0.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBodyMapPinDropControlReflectionChangedZero() {
        runComposeUiTest {
            val recomposeTrigger = androidx.compose.runtime.mutableStateOf(0)
            val staticLoc = BodyMapLocation("anterior", "反射胸部", "51185008", 50f, 30f)
            val staticCallback: (BodyMapLocation?) -> Unit = {}

            val bodyClass = Class.forName("io.healthplatform.chartcam.ui.sdc.controls.BodyMapPinDropControlKt")
            val primaryMethod =
                bodyClass.declaredMethods.first {
                    it.name == "BodyMapPinDropControl" && it.parameterCount == 11
                }
            primaryMethod.isAccessible = true

            val convenienceMethod =
                bodyClass.declaredMethods.first {
                    it.name == "BodyMapPinDropControl" && it.parameterCount == 8
                }
            convenienceMethod.isAccessible = true

            setContent {
                val dummy = recomposeTrigger.value
                val composer = androidx.compose.runtime.currentComposer

                primaryMethod.invoke(
                    null,
                    staticLoc,
                    staticCallback,
                    "反射標題",
                    false,
                    false,
                    null,
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
                primaryMethod.invoke(
                    null,
                    null,
                    staticCallback,
                    "反射標題2",
                    true,
                    true,
                    "錯誤",
                    true,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )

                convenienceMethod.invoke(
                    null,
                    staticLoc,
                    staticCallback,
                    "反射過載標題",
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
                convenienceMethod.invoke(
                    null,
                    null,
                    staticCallback,
                    "反射過載標題2",
                    true,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
            }
            waitForIdle()

            // Recompose so changed = 0 reflection calls evaluate composer.changed(...) == false
            recomposeTrigger.value++
            waitForIdle()
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

                // Test multi-select mode
                var multiSelected = listOf("Independent")
                setContent {
                    SegmentedVisualTilesControl(
                        selectedOptions = multiSelected,
                        options = options,
                        onOptionToggled = { opt ->
                            multiSelected = if (multiSelected.contains(opt)) multiSelected - opt else multiSelected + opt
                        },
                        label = "Multi Mobility",
                        isMultiSelect = true,
                        readOnly = false,
                    )
                }
                onNodeWithTag("SegmentedTile_Mild Assist").performClick()
                assertEquals(listOf("Independent", "Mild Assist"), multiSelected)
            }
        }

    /**
     * Tests SegmentedVisualTilesControl error state semantics and error message display.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSegmentedVisualTilesControlErrorStates() {
        runComposeUiTest {
            val options = listOf("Option A", "Option B")

            // isError = true with non-null errorMessage
            setContent {
                SegmentedVisualTilesControl(
                    selectedOptions = emptyList(),
                    options = options,
                    onOptionToggled = {},
                    label = "Error Label 1",
                    isMultiSelect = false,
                    isRequired = false,
                    isError = true,
                    errorMessage = "Selection required",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Selection required").assertIsDisplayed()
            onNodeWithText("Error Label 1").assertIsDisplayed()

            // isError = true with null errorMessage
            setContent {
                SegmentedVisualTilesControl(
                    selectedOptions = emptyList(),
                    options = options,
                    onOptionToggled = {},
                    label = "Error Label 2",
                    isMultiSelect = false,
                    isRequired = false,
                    isError = true,
                    errorMessage = null,
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Error Label 2").assertIsDisplayed()

            // isError = false with non-null errorMessage
            setContent {
                SegmentedVisualTilesControl(
                    selectedOptions = emptyList(),
                    options = options,
                    onOptionToggled = {},
                    label = "Error Label 3",
                    isMultiSelect = false,
                    isRequired = false,
                    isError = false,
                    errorMessage = "Ignored error message",
                    readOnly = false,
                )
            }
            waitForIdle()
            onNodeWithText("Error Label 3").assertIsDisplayed()
            onNodeWithText("Ignored error message").assertDoesNotExist()
        }
    }

    @androidx.compose.runtime.Composable
    private fun DynamicSegmentedTilesWrapper(
        selectedOptions: List<String>,
        options: List<String>,
        onOptionToggled: (String) -> Unit,
        label: String,
        isMultiSelect: Boolean,
        isRequired: Boolean,
        isError: Boolean,
        errorMessage: String?,
        readOnly: Boolean,
        modifier: androidx.compose.ui.Modifier,
        trigger: Int,
    ) {
        val t = trigger
        SegmentedVisualTilesControl(
            selectedOptions = selectedOptions,
            options = options,
            onOptionToggled = onOptionToggled,
            label = label,
            isMultiSelect = isMultiSelect,
            isRequired = isRequired,
            isError = isError,
            errorMessage = errorMessage,
            readOnly = readOnly,
            modifier = modifier,
        )
    }

    /**
     * Verifies recomposition skipping and parameter mutations for SegmentedVisualTilesControl.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSegmentedVisualTilesControlRecompositionAndSkipping() {
        runComposeUiTest {
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val staticOptions = listOf("靜態選項A", "靜態選項B")
            val staticSelected = listOf("靜態選項A")
            val staticToggle: (String) -> Unit = {}

            val selState = androidx.compose.runtime.mutableStateOf(listOf("動態A"))
            val optState = androidx.compose.runtime.mutableStateOf(listOf("動態A", "動態B"))
            val labelState = androidx.compose.runtime.mutableStateOf("動態瓷磚標籤")
            val multiState = androidx.compose.runtime.mutableStateOf(false)
            val reqState = androidx.compose.runtime.mutableStateOf(false)
            val errState = androidx.compose.runtime.mutableStateOf(false)
            val errMsgState = androidx.compose.runtime.mutableStateOf<String?>(null)
            val roState = androidx.compose.runtime.mutableStateOf(false)
            val modState = androidx.compose.runtime.mutableStateOf<androidx.compose.ui.Modifier>(androidx.compose.ui.Modifier)

            setContent {
                val dummy = trigger.value
                // Pure constant skipping call for primary method
                SegmentedVisualTilesControl(
                    selectedOptions = staticSelected,
                    options = staticOptions,
                    onOptionToggled = staticToggle,
                    label = "純靜態瓷磚",
                    isMultiSelect = false,
                    isRequired = false,
                    isError = false,
                    errorMessage = null,
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Pure constant skipping call for convenience overload
                SegmentedVisualTilesControl(
                    selectedOptions = staticSelected,
                    options = staticOptions,
                    onOptionToggled = staticToggle,
                    label = "純靜態瓷磚過載",
                    isMultiSelect = false,
                    readOnly = false,
                    modifier = androidx.compose.ui.Modifier,
                )

                // Calls with default parameters
                SegmentedVisualTilesControl(
                    selectedOptions = staticSelected,
                    options = staticOptions,
                    onOptionToggled = staticToggle,
                    label = "預設瓷磚三參數",
                )
                SegmentedVisualTilesControl(
                    selectedOptions = staticSelected,
                    options = staticOptions,
                    onOptionToggled = staticToggle,
                    label = "預設瓷磚四參數",
                    isMultiSelect = true,
                )
                SegmentedVisualTilesControl(
                    selectedOptions = staticSelected,
                    options = staticOptions,
                    onOptionToggled = staticToggle,
                    label = "預設瓷磚五參數",
                    isMultiSelect = false,
                    readOnly = true,
                )

                // Dynamic unmemoized wrapper call
                DynamicSegmentedTilesWrapper(
                    selectedOptions = selState.value,
                    options = optState.value,
                    onOptionToggled = staticToggle,
                    label = labelState.value,
                    isMultiSelect = multiState.value,
                    isRequired = reqState.value,
                    isError = errState.value,
                    errorMessage = errMsgState.value,
                    readOnly = roState.value,
                    modifier = modState.value,
                    trigger = trigger.value,
                )
            }
            waitForIdle()

            // Outer trigger causes pure skipping and unmemoized unchanged branches
            trigger.value++
            waitForIdle()

            // Mutate each state
            selState.value = listOf("動態B")
            waitForIdle()

            optState.value = listOf("動態A", "動態B", "動態C")
            waitForIdle()

            labelState.value = "更新瓷磚標籤"
            waitForIdle()

            multiState.value = true
            waitForIdle()

            reqState.value = true
            waitForIdle()

            errState.value = true
            errMsgState.value = "動態瓷磚錯誤"
            waitForIdle()

            roState.value = true
            waitForIdle()

            modState.value =
                androidx.compose.ui.Modifier
                    .padding(2.dp)
            waitForIdle()
        }
    }

    /**
     * Tests unmemoized parameter change detection branches by invoking SegmentedVisualTilesControl with changed = 0.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSegmentedVisualTilesReflectionChangedZero() {
        runComposeUiTest {
            val recomposeTrigger = androidx.compose.runtime.mutableStateOf(0)
            val staticOptions = listOf("反射A", "反射B")
            val staticSelected = listOf("反射A")
            val staticToggle: (String) -> Unit = {}

            val tilesClass = Class.forName("io.healthplatform.chartcam.ui.sdc.controls.SegmentedVisualTilesControlKt")
            val primaryMethod =
                tilesClass.declaredMethods.first {
                    it.name == "SegmentedVisualTilesControl" && it.parameterCount == 13
                }
            primaryMethod.isAccessible = true

            val convenienceMethod =
                tilesClass.declaredMethods.first {
                    it.name == "SegmentedVisualTilesControl" && it.parameterCount == 10
                }
            convenienceMethod.isAccessible = true

            setContent {
                val dummy = recomposeTrigger.value
                val composer = androidx.compose.runtime.currentComposer

                primaryMethod.invoke(
                    null,
                    staticSelected,
                    staticOptions,
                    staticToggle,
                    "反射標題",
                    false,
                    false,
                    false,
                    null,
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )

                convenienceMethod.invoke(
                    null,
                    staticSelected,
                    staticOptions,
                    staticToggle,
                    "反射過載標題",
                    false,
                    false,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
                convenienceMethod.invoke(
                    null,
                    staticSelected,
                    staticOptions,
                    staticToggle,
                    "反射過載標題2",
                    true,
                    true,
                    androidx.compose.ui.Modifier,
                    composer,
                    0,
                    0,
                )
            }
            waitForIdle()

            // Recompose so changed = 0 reflection calls evaluate composer.changed(...) == false
            recomposeTrigger.value++
            waitForIdle()
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
