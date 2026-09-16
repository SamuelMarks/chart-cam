/**
 * @file FileStorageWasmJsTest.kt
 * Contains declarations for FileStorageWasmJsTest.kt.
 */
package io.healthplatform.chartcam.files

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for FileStorage on WasmJS.
 */
class FileStorageWasmJsTest {
    /**
     * Test file storage instantiation on WasmJS.
     */
    @Test
    fun testFileStorageWasmJs() {
        val storage = createFileStorage()
        assertNotNull(storage)
    }
}
