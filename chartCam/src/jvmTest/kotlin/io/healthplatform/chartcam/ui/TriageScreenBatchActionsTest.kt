/**
 * @file TriageScreenBatchActionsTest.kt
 * Unit tests for TriageViewModel multi-selection, batch deletion, and patient assignment.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.files.FileStorage
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.viewmodel.TriageViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite verifying photo batch management actions in Triage.
 */
class TriageScreenBatchActionsTest {
    private val mockFileStorage =
        object : FileStorage {
            val deleted = mutableListOf<String>()

            override fun saveImage(
                fileName: String,
                bytes: ByteArray,
            ): String = fileName

            override fun readImage(path: String): ByteArray = ByteArray(0)

            override fun deleteImage(path: String): Result<Unit> {
                deleted.add(path)
                return Result.success(Unit)
            }

            override fun clearCache() {}
        }

    /**
     * Tests selection toggling and select-all/clear logic.
     */
    @Test
    fun testPhotoSelectionLifecycle() {
        val fhirRepo =
            FhirRepository(
                io.healthplatform.chartcam.database
                    .DatabaseDriverFactory()
                    .createDriver(),
            )
        val vm = TriageViewModel(fhirRepo)

        val samplePhotos =
            mapOf(
                "step1" to "file1.jpg",
                "step2" to "file2.jpg",
                "step3" to "file3.jpg",
            )
        vm.setPaths(samplePhotos)

        assertEquals(0, vm.uiState.value.selectedPhotoKeys.size)

        vm.togglePhotoSelection("step1")
        assertEquals(setOf("step1"), vm.uiState.value.selectedPhotoKeys)

        vm.togglePhotoSelection("step1")
        assertEquals(emptySet(), vm.uiState.value.selectedPhotoKeys)

        vm.selectAllPhotos()
        assertEquals(setOf("step1", "step2", "step3"), vm.uiState.value.selectedPhotoKeys)

        vm.clearSelection()
        assertEquals(emptySet(), vm.uiState.value.selectedPhotoKeys)
    }

    /**
     * Tests batch deletion of selected photos with storage purging.
     */
    @Test
    fun testBatchDeleteSelectedPhotos() =
        runTest {
            val fhirRepo =
                FhirRepository(
                    io.healthplatform.chartcam.database
                        .DatabaseDriverFactory()
                        .createDriver(),
                )
            val vm = TriageViewModel(fhirRepo)

            val samplePhotos =
                mapOf(
                    "face" to "face.jpg",
                    "lesion" to "lesion.jpg",
                )
            vm.setPaths(samplePhotos)
            vm.togglePhotoSelection("face")

            val deleteRes = vm.deleteSelectedPhotos(mockFileStorage)
            assertTrue(deleteRes.isSuccess)

            assertEquals(1, vm.uiState.value.capturedPhotoPaths.size)
            assertFalse(
                vm.uiState.value.capturedPhotoPaths
                    .containsKey("face"),
            )
            assertTrue(
                vm.uiState.value.capturedPhotoPaths
                    .containsKey("lesion"),
            )
            assertTrue(mockFileStorage.deleted.contains("face.jpg"))
        }
}
