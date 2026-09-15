/**
 * @file TriageViewModelTest.kt
 * Unit tests for TriageViewModel.
 */
package io.healthplatform.chartcam.viewmodel

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.healthplatform.chartcam.database.ChartCamDatabase
import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.FhirRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests verifying photo batch triage, multi-selection, and partial assignment.
 */
class TriageViewModelTest {
    private lateinit var fhirRepo: FhirRepository

    /**
     * Sets up test database and repository.
     */
    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        val db = ChartCamDatabase(driver)
        fhirRepo = FhirRepository(db)
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

        vm.togglePhotoSelection("p1")
        assertEquals(setOf("p1"), vm.uiState.value.selectedPhotoKeys)

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

        vm.togglePhotoSelection("p1")
        val result = vm.deleteSelectedPhotos(mockStorage)
        assertTrue(result.isSuccess)

        assertEquals(listOf("path1.jpg"), deletedFiles)
        assertEquals(mapOf("p2" to "path2.jpg"), vm.uiState.value.capturedPhotoPaths)
        assertEquals(0, vm.uiState.value.selectedPhotoKeys.size)
    }

    /**
     * Tests confirming association of a subset of photos, leaving remaining photos in the batch.
     */
    @Test
    fun testPartialBatchSplitting() {
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
    }
}
