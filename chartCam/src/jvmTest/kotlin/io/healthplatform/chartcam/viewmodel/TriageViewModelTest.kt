/**
 * @file TriageViewModelTest.kt
 * Unit tests for TriageViewModel.
 */
package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.models.createFhirPatient
import io.healthplatform.chartcam.repository.FhirRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests verifying photo batch triage, multi-selection, and partial assignment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TriageViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fhirRepo: FhirRepository

    /**
     * Sets up test database and repository.
     */
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        val db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
    }

    /**
     * Resets the coroutines main dispatcher.
     */
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests selecting and deselecting photos in a triage batch.
     */
    @Test
    fun testPhotoSelectionAndToggling() {
        val vm = TriageViewModel(fhirRepo)
        val photos = mapOf("p1" to "path1.jpg", "p2" to "path2.jpg", "p3" to "path3.jpg")
        vm.setPaths(photos)

        assertEquals(0, vm.uiState.value.selectedPhotoKeys.size)

        // Select p1
        vm.togglePhotoSelection("p1")
        assertEquals(setOf("p1"), vm.uiState.value.selectedPhotoKeys)

        // Deselect p1 (exercises branch when key is already selected)
        vm.togglePhotoSelection("p1")
        assertEquals(0, vm.uiState.value.selectedPhotoKeys.size)

        vm.selectAllPhotos()
        assertEquals(setOf("p1", "p2", "p3"), vm.uiState.value.selectedPhotoKeys)

        vm.clearSelection()
        assertEquals(0, vm.uiState.value.selectedPhotoKeys.size)
    }

    /**
     * Tests deleting selected photos from the active batch and disk storage.
     */
    @Test
    fun testDeleteSelectedPhotos() {
        val vm = TriageViewModel(fhirRepo)
        val photos = mapOf("p1" to "path1.jpg", "p2" to "path2.jpg")
        vm.setPaths(photos)

        // Empty selection deletion branch with default argument
        val emptyResult = vm.deleteSelectedPhotos()
        assertTrue(emptyResult.isSuccess)
        assertEquals(photos, vm.uiState.value.capturedPhotoPaths)

        val deletedFiles = mutableListOf<String>()
        val mockStorage =
            object : FileStorage {
                override fun saveImage(
                    fileName: String,
                    bytes: ByteArray,
                ): String = fileName

                override fun readImage(path: String): ByteArray = ByteArray(0)

                override fun deleteImage(path: String): Result<Unit> {
                    deletedFiles.add(path)
                    return Result.success(Unit)
                }

                override fun clearCache() {}
            }

        // Delete with storage, including a non-existent key to test path == null branch
        vm.togglePhotoSelection("p1")
        vm.togglePhotoSelection("missingKey")
        val result = vm.deleteSelectedPhotos(mockStorage)
        assertTrue(result.isSuccess)

        assertEquals(listOf("path1.jpg"), deletedFiles)
        assertEquals(mapOf("p2" to "path2.jpg"), vm.uiState.value.capturedPhotoPaths)
        assertEquals(0, vm.uiState.value.selectedPhotoKeys.size)

        // Delete without storage (fileStorage == null branch)
        vm.togglePhotoSelection("p2")
        val resultNoStorage = vm.deleteSelectedPhotos(null)
        assertTrue(resultNoStorage.isSuccess)
        assertTrue(
            vm.uiState.value.capturedPhotoPaths
                .isEmpty(),
        )
    }

    /**
     * Tests confirming association of a subset of photos, leaving remaining photos in the batch,
     * as well as associating all photos when no selection is active.
     */
    @Test
    fun testPartialBatchSplittingAndFullAssociation() {
        val vm = TriageViewModel(fhirRepo)
        val photos = mapOf("p1" to "path1.jpg", "p2" to "path2.jpg", "p3" to "path3.jpg")
        vm.setPaths(photos)

        vm.togglePhotoSelection("p1")
        vm.togglePhotoSelection("p2")

        var assigned = emptyMap<String, String>()
        val result = vm.confirmAssociation { assigned = it }
        assertTrue(result.isSuccess)

        assertEquals(mapOf("p1" to "path1.jpg", "p2" to "path2.jpg"), assigned)
        assertEquals(mapOf("p3" to "path3.jpg"), vm.uiState.value.capturedPhotoPaths)

        // Now confirm association without any selection -> assigns ALL remaining photos
        var remainingAssigned = emptyMap<String, String>()
        val fullResult = vm.confirmAssociation { remainingAssigned = it }
        assertTrue(fullResult.isSuccess)
        assertEquals(mapOf("p3" to "path3.jpg"), remainingAssigned)
        assertTrue(
            vm.uiState.value.capturedPhotoPaths
                .isEmpty(),
        )
    }

    /**
     * Tests search queries, blank query clearing, patient selection, and patient creation.
     */
    @Test
    fun testSearchAndPatientCreation() =
        runTest {
            val vm = TriageViewModel(fhirRepo)

            // Save a patient to search for
            val patient = createFhirPatient("pat-1", "Alice", "Smith", LocalDate(1985, 5, 12), "MRN-101")
            fhirRepo.savePatient(patient)

            // Blank search query returns empty list
            vm.onSearchQueryChanged("   ")
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(
                vm.uiState.value.searchResults
                    .isEmpty(),
            )

            // Non-blank query finds patient
            vm.onSearchQueryChanged("Alice")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, vm.uiState.value.searchResults.size)
            assertEquals(
                "pat-1",
                vm.uiState.value.searchResults
                    .first()
                    .id,
            )

            // Select patient
            vm.selectPatient(patient)
            assertEquals(patient, vm.uiState.value.selectedPatient)

            // Dialog visibility toggle
            vm.showCreatePatient(true)
            assertTrue(vm.uiState.value.isCreatingPatient)
            vm.showCreatePatient(false)
            assertFalse(vm.uiState.value.isCreatingPatient)

            // Create patient
            vm.createPatient(
                firstName = "Bob",
                lastName = "Jones",
                mrn = "MRN-202",
                dob = LocalDate(1992, 3, 4),
                gender = "male",
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(vm.uiState.value.selectedPatient)
            assertFalse(vm.uiState.value.isCreatingPatient)
        }

    /**
     * Tests data class contract of TriageUiState.
     */
    @Test
    fun testTriageUiStateContract() {
        val s1 = TriageUiState()
        val s2 = TriageUiState()
        val s3 = TriageUiState(searchQuery = "test", isCreatingPatient = true)

        assertEquals(s1, s2)
        assertNotEquals(s1, s3)
        assertFalse(s1.equals(null))
        assertFalse(s1.equals("diff"))
        assertEquals(s1.hashCode(), s2.hashCode())
        assertTrue(s3.toString().contains("test"))

        val copy1 = s3.copy(isCreatingPatient = false)
        assertFalse(copy1.isCreatingPatient)

        assertEquals(emptyMap(), s1.component1())
        assertEquals("", s1.component2())
        assertEquals(emptyList(), s1.component3())
        assertFalse(s1.component4())
        assertEquals(null, s1.component5())
        assertEquals(emptySet(), s1.component6())
    }
}
