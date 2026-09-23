/**
 * @file QuestionnaireDestinationsJvmTest.kt
 * Contains declarations for QuestionnaireDestinationsJvmTest.kt.
 *
 * JVM Compose UI tests for questionnaire navigation destinations in [QuestionnaireDestinations.kt].
 */
package io.healthplatform.chartcam.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.AuthRepository
import io.healthplatform.chartcam.repository.ExportImportService
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.storage.SecureStorage
import io.healthplatform.chartcam.ui.setAppLanguage
import io.healthplatform.chartcam.viewmodel.WidgetType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.ohs.fhir.model.r4.String as FhirString

private class QTestMemoryStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()

    override fun save(key: String, value: String) {
        map[key] = value
    }

    override fun getString(key: String): String? = map[key]

    override fun delete(key: String) {
        map.remove(key)
    }
}

private class QTestFileStorage : FileStorage {
    override fun saveImage(fileName: String, bytes: ByteArray): String = fileName

    override fun readImage(path: String): ByteArray = ByteArray(0)

    override fun deleteImage(path: String): Result<Unit> = Result.success(Unit)

    override fun clearCache() {}
}

private fun createTestDependencies(): AppDependencies {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ChartCamDatabase.Schema.synchronous().create(driver)
    val database = ChartCamDatabase(driver)
    val secureStorage = QTestMemoryStorage()
    val authRepo = AuthRepository(secureStorage)
    val fhirRepo = FhirRepository(driver)
    val qRepo = QuestionnaireRepository(fhirRepo)
    val fileStorage = QTestFileStorage()
    val eiService = ExportImportService(database, fileStorage)
    val photoMgr = PhotoSessionManager()

    return AppDependencies(
        authRepository = authRepo,
        fhirRepository = fhirRepo,
        questionnaireRepository = qRepo,
        exportImportService = eiService,
        photoSessionManager = photoMgr,
        fileStorage = fileStorage,
    )
}

/**
 * JVM test suite verifying [QuestionnaireDestinations.kt] resolvers, action factories, and Composable content.
 */
@OptIn(ExperimentalTestApi::class)
class QuestionnaireDestinationsJvmTest {
    /**
     * Verifies all branches of [resolveCopyTitle].
     */
    @Test
    fun testResolveCopyTitleBranches() {
        // Primary template with %1$s
        assertEquals("Copy of Triage", resolveCopyTitle("Copy of %1\$s", "Fallback %1\$s", "Triage"))
        // Primary template with %s
        assertEquals("Copy of Triage", resolveCopyTitle("Copy of %s", "Fallback %s", "Triage"))
        // Fallback template when primary contains neither
        assertEquals("Fallback Triage", resolveCopyTitle("NoSpecifier", "Fallback %1\$s", "Triage"))
        assertEquals("Fallback Triage", resolveCopyTitle("NoSpecifier", "Fallback %s", "Triage"))
    }

    /**
     * Verifies all branches of [resolveWidgetItemLabel].
     */
    @Test
    fun testResolveWidgetItemLabelBranches() {
        val names = mapOf(WidgetType.PHOTO_CAMERA to "Camera")

        // In map and template with %1$s
        assertEquals("Add Camera", resolveWidgetItemLabel("Add %1\$s", "Fallback %1\$s", "New Item", names, WidgetType.PHOTO_CAMERA))
        // In map and template with %s
        assertEquals("Add Camera", resolveWidgetItemLabel("Add %s", "Fallback %s", "New Item", names, WidgetType.PHOTO_CAMERA))
        // Fallback template when template contains neither
        assertEquals(
            "Fallback Camera (New Item)",
            resolveWidgetItemLabel("StaticText", "Fallback %1\$s (%2\$s)", "New Item", names, WidgetType.PHOTO_CAMERA),
        )
        // Not in map -> fallback to widgetType.name
        assertEquals("Add SWITCH", resolveWidgetItemLabel("Add %1\$s", "Fallback %1\$s", "New Item", names, WidgetType.SWITCH))
    }

    /**
     * Verifies all branches of [resolveDefaultOptions].
     */
    @Test
    fun testResolveDefaultOptionsBranches() {
        val fitzpatrick = listOf("Type I", "Type II")
        val severity = listOf("Mild", "Moderate", "Severe")

        assertEquals(fitzpatrick, resolveDefaultOptions(fitzpatrick, severity, WidgetType.FITZPATRICK_PALETTE))
        assertEquals(severity, resolveDefaultOptions(fitzpatrick, severity, WidgetType.SEGMENTED_TILES))
        assertEquals(emptyList(), resolveDefaultOptions(fitzpatrick, severity, WidgetType.PHOTO_CAMERA))
        assertEquals(emptyList(), resolveDefaultOptions(fitzpatrick, severity, WidgetType.SWITCH))
    }

    /**
     * Verifies all callbacks built by [buildQuestionnaireBuilderActions].
     */
    @Test
    fun testBuildQuestionnaireBuilderActions() =
        runComposeUiTest {
            var capturedActions: QuestionnaireBuilderNavActions? = null
            var rootActions: QuestionnaireBuilderNavActions? = null

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "root") {
                    composable("root") {
                        rootActions = buildQuestionnaireBuilderActions(navController)
                        Text("Root")
                    }
                    composable<QuestionnaireBuilderRoute> {
                        capturedActions = buildQuestionnaireBuilderActions(navController)
                        Text("BuilderScreen")
                    }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(QuestionnaireBuilderRoute())
                }
            }

            waitForIdle()
            runOnIdle {
                val root = checkNotNull(rootActions)
                root.onSaved("q-root")

                val actions = checkNotNull(capturedActions)
                actions.onSaved("q-saved-999")
                actions.onBack()
            }
            waitForIdle()
        }

    /**
     * Verifies resolvers wired by [createQuestionnaireBuilderViewModel].
     */
    @Test
    fun testCreateQuestionnaireBuilderViewModel() {
        val deps = createTestDependencies()
        val names = mapOf(WidgetType.PHOTO_CAMERA to "Camera")
        val fitz = listOf("Type I", "Type II")
        val sev = listOf("Mild", "Moderate")

        // Non-duplicate flow
        val vm =
            createQuestionnaireBuilderViewModel(
                repository = deps.questionnaireRepository,
                duplicateFromId = null,
                copyTemplate = "Copy of %1\$s",
                copyFallbackTemplate = "Fallback %1\$s",
                newItemLabel = "New Item",
                newWidgetTemplate = "New %1\$s",
                newWidgetFallbackTemplate = "New %1\$s (%2\$s)",
                unknownLabel = "Unknown Title",
                widgetNames = names,
                fitzpatrickTypes = fitz,
                defaultSeverityOptions = sev,
            )
        vm.addItem(WidgetType.PHOTO_CAMERA)
        vm.addItem(WidgetType.FITZPATRICK_PALETTE)
        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        assertEquals(3, vm.state.value.items.size)

        // Duplicate flow with missing title and missing item text
        val blankQ =
            Questionnaire(
                id = "q-blank",
                status = Enumeration(value = PublicationStatus.Active),
                item =
                    listOf(
                        Questionnaire.Item(
                            linkId = FhirString(value = "item_1"),
                            type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                        ),
                    ),
            )
        runBlocking { deps.questionnaireRepository.saveQuestionnaire(blankQ) }

        val vmDup =
            createQuestionnaireBuilderViewModel(
                repository = deps.questionnaireRepository,
                duplicateFromId = "q-blank",
                copyTemplate = "Copy of %1\$s",
                copyFallbackTemplate = "Fallback %1\$s",
                newItemLabel = "Fallback Item Label",
                newWidgetTemplate = "New %1\$s",
                newWidgetFallbackTemplate = "New %1\$s (%2\$s)",
                unknownLabel = "Fallback Unknown",
                widgetNames = names,
                fitzpatrickTypes = fitz,
                defaultSeverityOptions = sev,
            )
        assertEquals("Copy of Fallback Unknown", vmDup.state.value.title)
        assertEquals(
            "Fallback Item Label",
            vmDup.state.value.items
                .first()
                .label,
        )
    }

    /**
     * Verifies recomposition and skipping for [QuestionnaireBuilderRouteScreen].
     */
    @Test
    fun testQuestionnaireBuilderRouteScreenRecomposition() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()
            val trigger = androidx.compose.runtime.mutableStateOf(0)
            val routeState =
                androidx.compose.runtime.mutableStateOf(
                    QuestionnaireBuilderRoute(duplicateFromId = null),
                )
            val langState = androidx.compose.runtime.mutableStateOf("en")
            val onBackState = androidx.compose.runtime.mutableStateOf<() -> Unit>({})
            val onSavedState = androidx.compose.runtime.mutableStateOf<(String) -> Unit>({})

            setContent {
                val dummy = trigger.value
                QuestionnaireBuilderRouteScreen(
                    route = routeState.value,
                    deps = deps,
                    currentLang = langState.value,
                    onBack = onBackState.value,
                    onSaved = onSavedState.value,
                )
            }
            waitForIdle()

            // Skipping
            trigger.value++
            waitForIdle()

            // Recomposition without route change
            onBackState.value = {}
            onSavedState.value = {}
            langState.value = "es"
            waitForIdle()

            // Recomposition with route change
            routeState.value = QuestionnaireBuilderRoute(duplicateFromId = "dup-rec")
            waitForIdle()
        }

    /**
     * Verifies all callbacks built by [buildQuestionnaireListActions].
     */
    @Test
    fun testBuildQuestionnaireListActions() =
        runComposeUiTest {
            var capturedActions: QuestionnaireListNavActions? = null

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Routes.QUESTIONNAIRE_LIST) {
                    composable(Routes.QUESTIONNAIRE_LIST) {
                        capturedActions = buildQuestionnaireListActions(navController)
                        Text("ListScreen")
                    }
                    composable<QuestionnaireBuilderRoute> { Text("Builder") }
                }
            }

            waitForIdle()
            runOnIdle {
                val actions = checkNotNull(capturedActions)
                actions.onNavigateToBuilder("dup-id-1")
                actions.onNavigateToBuilder(null)
                actions.onBack()
            }
            waitForIdle()
        }

    /**
     * Verifies [questionnaireListDestination] in NavHost rendering [QuestionnaireListScreen].
     */
    @Test
    fun testQuestionnaireListDestinationNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()

            setContent {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Routes.QUESTIONNAIRE_LIST) {
                    questionnaireListDestination(navController, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Questionnaires", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Questionnaires", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies [questionnaireBuilderDestination] in NavHost creating a new questionnaire.
     */
    @Test
    fun testQuestionnaireBuilderDestinationNewNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()

            setContent {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = QuestionnaireBuilderRoute(duplicateFromId = null),
                ) {
                    questionnaireBuilderDestination(navController, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Build Questionnaire", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Build Questionnaire", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }

    /**
     * Verifies [questionnaireBuilderDestination] in NavHost duplicating an existing questionnaire.
     */
    @Test
    fun testQuestionnaireBuilderDestinationDuplicateNavHost() =
        runComposeUiTest {
            setAppLanguage("en")
            val deps = createTestDependencies()
            val existing =
                Questionnaire(
                    id = "q-dup-source",
                    title = FhirString(value = "Source Assessment"),
                    status = Enumeration(value = PublicationStatus.Active),
                    item = emptyList(),
                )
            runBlocking { deps.questionnaireRepository.saveQuestionnaire(existing) }

            setContent {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = QuestionnaireBuilderRoute(duplicateFromId = "q-dup-source"),
                ) {
                    questionnaireBuilderDestination(navController, deps, "en")
                }
            }

            waitForIdle()
            waitUntil(timeoutMillis = 5000L) {
                runCatching {
                    onNodeWithText("Build Questionnaire", substring = true, useUnmergedTree = true).assertExists()
                }.isSuccess
            }
            onNodeWithText("Build Questionnaire", substring = true, useUnmergedTree = true).assertIsDisplayed()
        }
}
