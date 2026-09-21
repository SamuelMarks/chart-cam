/**
 * @file ScreenshotGeneratorE2E.kt
 * Contains the [ScreenshotGeneratorE2E] test class.
 */
package io.healthplatform.chartcam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.camera.SilhouetteType
import io.healthplatform.chartcam.database.DatabaseDriverFactory
import io.healthplatform.chartcam.dicom.DicomDataset
import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.models.BodyMapLocation
import io.healthplatform.chartcam.models.FitzpatrickScaleDefaults
import io.healthplatform.chartcam.models.FitzpatrickSkinType
import io.healthplatform.chartcam.navigation.PhotoSessionManager
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.DemoDataSeeder
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.BiometricSecurityManager
import io.healthplatform.chartcam.storage.JvmSecureStorage
import io.healthplatform.chartcam.ui.ControlsLayer
import io.healthplatform.chartcam.ui.ControlsState
import io.healthplatform.chartcam.ui.EncounterDetailActions
import io.healthplatform.chartcam.ui.EncounterDetailDependencies
import io.healthplatform.chartcam.ui.EncounterDetailScreen
import io.healthplatform.chartcam.ui.LoginScreen
import io.healthplatform.chartcam.ui.PatientDetailScreen
import io.healthplatform.chartcam.ui.PatientListActions
import io.healthplatform.chartcam.ui.PatientListDependencies
import io.healthplatform.chartcam.ui.PatientListScreen
import io.healthplatform.chartcam.ui.QuestionnaireBuilderScreen
import io.healthplatform.chartcam.ui.QuestionnaireListScreen
import io.healthplatform.chartcam.ui.TriageScreen
import io.healthplatform.chartcam.ui.components.CreatePatientDialog
import io.healthplatform.chartcam.ui.components.DicomViewerComponent
import io.healthplatform.chartcam.ui.components.LevelerOverlay
import io.healthplatform.chartcam.ui.components.SilhouetteOverlay
import io.healthplatform.chartcam.ui.currentLanguageState
import io.healthplatform.chartcam.ui.sdc.controls.BodyMapPinDropControl
import io.healthplatform.chartcam.ui.sdc.controls.FacialSeriesCardControl
import io.healthplatform.chartcam.ui.sdc.controls.FitzpatrickPaletteControl
import io.healthplatform.chartcam.ui.sdc.controls.VisualPainScaleControl
import io.healthplatform.chartcam.ui.theme.AppSpacing
import io.healthplatform.chartcam.ui.theme.AppTheme
import io.healthplatform.chartcam.viewmodel.LoginViewModel
import io.healthplatform.chartcam.viewmodel.QuestionnaireBuilderViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Test harness dependencies container.
 *
 * @property fhirRepository FHIR persistence repository.
 * @property questionnaireRepository SDC questionnaire repository.
 * @property authRepository Clinician authentication repository.
 * @property exportImportService Dataset import and export service.
 * @property biometricSecurityManager Hardware biometric security manager.
 */
data class ScreenshotTestDependencies(
    val fhirRepository: FhirRepository,
    val questionnaireRepository: QuestionnaireRepository,
    val authRepository: AuthRepository,
    val exportImportService: ExportImportService,
    val biometricSecurityManager: BiometricSecurityManager,
)

/**
 * End-to-end UI tests that traverse the application on JVM to generate Android-sized screenshots.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class ScreenshotGeneratorE2E {
    companion object {
        /** Viewport width in pixels for compact phone screenshot generation. */
        private const val DEVICE_WIDTH = 360

        /** Viewport height in pixels for compact phone screenshot generation. */
        private const val DEVICE_HEIGHT = 640
    }

    /**
     * Captures the root UI node and saves it as an image file.
     *
     * @param composeTestRule The active compose UI test rule.
     * @param filename The path and filename to save the image.
     */
    private fun takeScreenshot(
        composeTestRule: DesktopComposeUiTest,
        filename: String,
    ) {
        val f = File(filename)
        f.parentFile.mkdirs()
        val img =
            composeTestRule
                .onAllNodes(isRoot())
                .onFirst()
                .captureToImage()
                .toAwtImage()
        ImageIO.write(img, "png", f)
        println("WROTE " + f.absolutePath)
    }

    /**
     * Captures the active test screen and saves it under the Android naming convention and optionally legacy iPhone name.
     *
     * @param composeTestRule The active compose UI test rule.
     * @param androidName The Android screenshot name (without extension).
     * @param iphoneName The legacy iPhone screenshot name (without extension), or null.
     */
    private fun captureAndroidAndIphone(
        composeTestRule: DesktopComposeUiTest,
        androidName: String,
        iphoneName: String? = null,
    ) {
        takeScreenshot(composeTestRule, "../fastlane/screenshots/en-US/$androidName.png")
        if (iphoneName != null) {
            takeScreenshot(composeTestRule, "../fastlane/screenshots/en-US/$iphoneName.png")
        }
    }

    /**
     * Sets up the database and repository dependencies for the test.
     *
     * @return Initialized [ScreenshotTestDependencies].
     */
    private fun setupDeps(): ScreenshotTestDependencies {
        val dbFactory = DatabaseDriverFactory()
        val driver = dbFactory.createDriver()
        val fhirRepository = FhirRepository(driver)
        val questionnaireRepository = QuestionnaireRepository(fhirRepository)
        val storage = JvmSecureStorage("test_screenshots")
        val authRepository = AuthRepository(storage)
        val fileStorage = createFileStorage()
        val exportImportService = ExportImportService(fhirRepository.database, fileStorage)
        val biometricSecurityManager = BiometricSecurityManager(storage)

        runBlocking {
            io.healthplatform.chartcam.initDatabase(driver)
            questionnaireRepository.loadDefaultForms()
            DemoDataSeeder.seedDemoData(fhirRepository)
            authRepository.loginAsDemo()
        }
        return ScreenshotTestDependencies(
            fhirRepository = fhirRepository,
            questionnaireRepository = questionnaireRepository,
            authRepository = authRepository,
            exportImportService = exportImportService,
            biometricSecurityManager = biometricSecurityManager,
        )
    }

    /**
     * Creates a synthetic ophthalmic image byte array for DICOM preview.
     *
     * @return PNG-encoded byte array representing a synthetic medical scan.
     */
    private fun createSyntheticScanBytes(): ByteArray {
        val bi = BufferedImage(360, 270, BufferedImage.TYPE_INT_RGB)
        val g = bi.createGraphics()
        g.color = java.awt.Color(20, 25, 30)
        g.fillRect(0, 0, 360, 270)
        g.color = java.awt.Color(10, 147, 150)
        g.drawOval(40, 20, 220, 220)
        g.color = java.awt.Color(148, 210, 189)
        g.drawOval(80, 60, 140, 140)
        g.color = java.awt.Color(238, 155, 0)
        g.fillOval(130, 110, 40, 40)
        g.color = java.awt.Color.WHITE
        g.drawString("OPHTHALMIC RETINAL SCAN - MGH", 25, 255)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(bi, "png", baos)
        return baos.toByteArray()
    }

    /**
     * Generates all end-to-end Android size screenshots covering the full clinical suite.
     */
    @Test
    fun generateAllAndroidScreenshots() {
        File("chartcam_desktop.db").delete()
        val deps = setupDeps()
        val patientListDeps =
            PatientListDependencies(
                deps.fhirRepository,
                deps.exportImportService,
                deps.authRepository,
            )
        val patientListActs = PatientListActions({}, {}, {})

        // 00: Authentication Gateway
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val loginVm = LoginViewModel(deps.authRepository, deps.biometricSecurityManager)
                        LoginScreen(
                            viewModel = loginVm,
                            onLoginSuccess = {},
                        )
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-00-login", "iphone-00-login")
        }

        // 01: Primary Viewfinder with Leveler Overlay
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val gridColor = Color.White.copy(alpha = 0.12f)
                                drawLine(gridColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 2f)
                                drawLine(gridColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 2f)
                                drawLine(gridColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 2f)
                                drawLine(gridColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 2f)
                            }
                            LevelerOverlay(pitch = 0.8f, roll = -0.5f)
                            ControlsLayer(
                                state =
                                    ControlsState(
                                        stepName = "Step 1 of 2: Left Eye (Anterior Segment)",
                                        count = 1,
                                        total = 2,
                                        isCapturing = false,
                                        hasMultipleCameras = true,
                                    ),
                                onCapture = {},
                                onToggleLens = {},
                                onCancel = {},
                            )
                        }
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-01-capture-leveler")
        }

        // 02: Media Attribution & Triage
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        TriageScreen(
                            capturedPhotoPaths =
                                mapOf(
                                    "Left Eye" to "mock_left_eye.jpg",
                                    "Right Eye" to "mock_right_eye.jpg",
                                ),
                            fhirRepository = deps.fhirRepository,
                            onProceedToEncounter = { _, _ -> },
                            onBack = {},
                        )
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-02-triage")
        }

        // 03: Patient Provisioning Dialog
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            PatientListScreen(
                                dependencies = patientListDeps,
                                actions = patientListActs,
                            )
                            CreatePatientDialog(
                                onDismissRequest = {},
                                onConfirm = { _, _, _, _, _ -> },
                            )
                        }
                    }
                }
            }
            waitForIdle()
            onNodeWithText("First Name").performTextInput("Jane")
            onNodeWithText("Last Name").performTextInput("Smith")
            onNodeWithText("MRN").performTextInput("MRN-9876")
            onNodeWithText("DOB (MM/DD/YYYY)").performTextInput("05/15/1985")
            waitForIdle()
            captureAndroidAndIphone(this, "android-03-create-patient", "iphone-01-create-patient")
        }

        // 04: Patient Directory & Dashboard
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PatientListScreen(
                            dependencies = patientListDeps,
                            actions = patientListActs,
                        )
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-04-list-patients", "iphone-02-list-patients")
        }

        // Burger dropdown menu from patient list screen
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PatientListScreen(
                            dependencies = patientListDeps,
                            actions = patientListActs,
                        )
                    }
                }
            }
            waitForIdle()
            onAllNodesWithContentDescription("More options", substring = true, ignoreCase = true).onFirst().performClick()
            waitForIdle()
            takeScreenshot(this, "../fastlane/screenshots/en-US/iphone-03-burger-dropdown.png")
        }

        // 05: SDC Form Builder
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val builderVm = QuestionnaireBuilderViewModel(deps.questionnaireRepository)
                        QuestionnaireBuilderScreen(
                            viewModel = builderVm,
                            onBack = {},
                            onSaved = {},
                        )
                    }
                }
            }
            waitForIdle()
            onNodeWithText("Questionnaire Title").performTextInput("Mass Eye Clinical Protocol")
            waitForIdle()
            captureAndroidAndIphone(this, "android-05-create-questionnaire", "iphone-04-create-questionnaire")
        }

        // 06: Specialized Clinical Controls (Body Map, FACES Pain, Fitzpatrick)
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Clinical Assessment Protocol") },
                                navigationIcon = {
                                    IconButton(onClick = {}) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                            )
                        },
                    ) { padding ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            var bodyLocation by remember {
                                mutableStateOf<BodyMapLocation?>(
                                    BodyMapLocation(
                                        regionId = "left_arm",
                                        displayName = "Left Arm (Forearm Lesion)",
                                        snomedCode = "368208006",
                                        xPercent = 28f,
                                        yPercent = 42f,
                                    ),
                                )
                            }
                            BodyMapPinDropControl(
                                location = bodyLocation,
                                onLocationChanged = { bodyLocation = it },
                                label = "1. Anatomical Lesion / Wound Location",
                            )

                            var painScore by remember { mutableStateOf<Int?>(4) }
                            VisualPainScaleControl(
                                value = painScore,
                                onValueChange = { painScore = it },
                                label = "2. Wong-Baker FACES® Pain Assessment",
                            )

                            var skinType by remember {
                                mutableStateOf<FitzpatrickSkinType?>(FitzpatrickScaleDefaults.ALL_TYPES[2])
                            }
                            FitzpatrickPaletteControl(
                                selectedType = skinType,
                                onTypeSelected = { skinType = it },
                                label = "3. Fitzpatrick Skin Phototyping",
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-06-clinical-widgets", "iphone-05-fill-questionnaire")
        }

        // 07: Longitudinal Encounters History
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PatientDetailScreen(
                            patientId = DemoDataSeeder.DEMO_PATIENT_ADULT_ID,
                            fhirRepository = deps.fhirRepository,
                            onBack = {},
                            onNewVisit = {},
                            onVisitSelected = {},
                        )
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-07-view-patient-questionnaires", "iphone-06-view-patient-questionnaires")
        }

        // 08: Encounter & Questionnaire Response Detail
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        EncounterDetailScreen(
                            patientId = DemoDataSeeder.DEMO_PATIENT_ADULT_ID,
                            visitId = DemoDataSeeder.DEMO_ENCOUNTER_ADULT_ID,
                            dependencies =
                                EncounterDetailDependencies(
                                    photoSessionManager = PhotoSessionManager(),
                                    fhirRepository = deps.fhirRepository,
                                    authRepository = deps.authRepository,
                                    questionnaireRepository = deps.questionnaireRepository,
                                ),
                            actions =
                                EncounterDetailActions(
                                    onBack = {},
                                    onTakePhotos = { _, _ -> },
                                    onFinalized = {},
                                ),
                        )
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-08-view-specific-questionnaire", "iphone-07-view-specific-questionnaire")
        }

        // 09: Air-Gapped Protocol Sharing via QR Code
        runBlocking {
            deps.questionnaireRepository.createQuestionnaire("Burn Assessment Protocol", 2, "Left Arm, Right Arm")
        }
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        QuestionnaireListScreen(
                            questionnaireRepository = deps.questionnaireRepository,
                            onBack = {},
                            onNavigateToBuilder = {},
                        )
                    }
                }
            }
            waitForIdle()
            onAllNodesWithContentDescription("Share Questionnaire", substring = true, ignoreCase = true).onFirst().performClick()
            waitForIdle()
            onNodeWithText("Display QR Code").performClick()
            waitForIdle()
            captureAndroidAndIphone(this, "android-09-qr-code-share", "iphone-08-export-questionnaire-view")
        }

        // 10: Encrypted Dataset Export (Argon2id + AES-256)
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PatientListScreen(
                            dependencies = patientListDeps,
                            actions = patientListActs,
                        )
                    }
                }
            }
            waitForIdle()
            onAllNodesWithContentDescription("More options", substring = true, ignoreCase = true).onFirst().performClick()
            waitForIdle()
            onNodeWithText("Export Data").performClick()
            waitForIdle()
            onAllNodes(hasSetTextAction()).onLast().performTextInput("secure123")
            waitForIdle()
            captureAndroidAndIphone(this, "android-10-export-dataset", "iphone-09-export-dataset")
        }

        // 11: DICOM Part 10 Inspector & PACS Viewer
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("DICOM Part 10 Inspector") },
                                navigationIcon = {
                                    IconButton(onClick = {}) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                            )
                        },
                    ) { padding ->
                        val syntheticBytes = createSyntheticScanBytes()
                        val dataset =
                            DicomDataset(
                                elements = emptyMap(),
                                patientName = "Jenkins^Sarah",
                                patientId = "DEMO-ADULT-002",
                                patientSex = "F",
                                modality = "XC",
                                sopClassUid = "1.2.840.10008.5.1.4.1.1.77.1.4",
                                transferSyntaxUid = "1.2.840.10008.1.2.4.50",
                                width = 1920,
                                height = 1080,
                                pixelData = syntheticBytes,
                            )
                        DicomViewerComponent(
                            dataset = dataset,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-11-dicom-viewer")
        }

        // 12: Hardware Security Shield & Lockout
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = false) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(AppSpacing.xl),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Session Locked",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Session Locked Due to Inactivity",
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Biometric authentication required to decrypt clinical records.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = {}) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlock with Biometrics")
                                }
                            }
                        }
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-12-security-shield")
        }

        // 13: Global Localization (Hebrew RTL)
        currentLanguageState.value = "he"
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppTheme(darkTheme = false) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            PatientListScreen(
                                dependencies = patientListDeps,
                                actions = patientListActs,
                            )
                        }
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-13-i18n-rtl")
        }
        currentLanguageState.value = "en"

        // 14: Guided Craniofacial & Cornea/Nose Silhouette Viewfinder
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            setContent {
                AppTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val gridColor = Color.White.copy(alpha = 0.08f)
                                drawLine(gridColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1.5f)
                                drawLine(gridColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 1.5f)
                                drawLine(gridColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1.5f)
                                drawLine(gridColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 1.5f)
                            }
                            SilhouetteOverlay(
                                silhouetteType = SilhouetteType.PROFILE_CORNEA_NOSE_LEFT,
                                isVisible = true,
                            )
                            LevelerOverlay(pitch = 0.5f, roll = -0.2f)
                            ControlsLayer(
                                state =
                                    ControlsState(
                                        stepName = "Step 1 of 3: Left Profile (Cornea & Nose)",
                                        count = 1,
                                        total = 3,
                                        isCapturing = false,
                                        hasMultipleCameras = true,
                                    ),
                                onCapture = {},
                                onToggleLens = {},
                                onCancel = {},
                            )
                        }
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-14-silhouette-capture", "iphone-10-silhouette-capture")
        }

        // 15: 3-Angle Facial & Cornea Series SDC Protocol Widget
        runDesktopComposeUiTest(width = DEVICE_WIDTH, height = DEVICE_HEIGHT) {
            val items =
                listOf(
                    Questionnaire.Item
                        .Builder(
                            linkId =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "profile_left" },
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                        ).apply {
                            text =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "Left Profile (Cornea & Nose)" }
                        }.build(),
                    Questionnaire.Item
                        .Builder(
                            linkId =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "front_view" },
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                        ).apply {
                            text =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "Front View" }
                        }.build(),
                    Questionnaire.Item
                        .Builder(
                            linkId =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "profile_right" },
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.Attachment),
                        ).apply {
                            text =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "Right Profile (Cornea & Nose)" }
                        }.build(),
                )

            setContent {
                AppTheme(darkTheme = false) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Facial Examination") },
                                navigationIcon = {
                                    IconButton(onClick = {}) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                },
                            )
                        },
                    ) { padding ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Standard Craniofacial & Ophthalmic Series",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text =
                                    "Standardized multi-angle photographic protocol capturing corneal curvature, nasal projection, " +
                                        "and frontal symmetry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FacialSeriesCardControl(
                                title = "3-Angle Facial & Cornea Series",
                                items = items,
                                answers = mapOf("profile_left" to "mock_left.jpg"),
                                existingAttachments = emptyList(),
                                readOnly = false,
                                onCaptureSeries = {},
                                onCaptureSingle = {},
                            )
                        }
                    }
                }
            }
            waitForIdle()
            captureAndroidAndIphone(this, "android-15-facial-series", "iphone-11-facial-series")
        }
    }
}
