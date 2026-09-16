/**
 * @file CameraManagerJsTest.kt
 * Contains declarations for CameraManagerJsTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for CameraManager on JS.
 */
class CameraManagerJsTest {
    /**
     * Test camera container creation on JS.
     */
    @Test
    fun testCameraManagerJs() {
        val container = CameraManager.createMinimalMp4Container()
        assertNotNull(container)
        assertTrue(container.isNotEmpty())
    }
}
