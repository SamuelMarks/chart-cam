/**
 * @file FileStorageJsTest.kt
 * Contains declarations for FileStorageJsTest.kt.
 */
package io.healthplatform.chartcam.files

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test class for FileStorage on JS.
 */
class FileStorageJsTest {
    /**
     * Test file storage instantiation on JS.
     */
    @Test
    fun testFileStorageJs() {
        val storage = createFileStorage()
        assertNotNull(storage)
    }
}
