/**
 * @file FileStorageIosTest.kt
 * Contains declarations for FileStorageIosTest.kt.
 */
package io.healthplatform.chartcam.files

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * iOS-specific tests for file storage logic.
 */
class FileStorageIosTest {
    /**
     * Verifies creation of FileStorage on iOS.
     */
    @Test
    fun testFileStorageIos() {
        val storage = createFileStorage()
        assertNotNull(storage)
    }
}
