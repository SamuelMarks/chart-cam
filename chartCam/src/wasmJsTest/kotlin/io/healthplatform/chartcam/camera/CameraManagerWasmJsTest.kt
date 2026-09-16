/**
 * @file CameraManagerWasmJsTest.kt
 * Contains declarations for CameraManagerWasmJsTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for CameraManager on WasmJS.
 */
class CameraManagerWasmJsTest {
    /**
     * Test camera container creation on WasmJS.
     */
    @Test
    fun testCameraManagerWasmJs() {
        val container = CameraManager.createMinimalMp4Container()
        assertNotNull(container)
        assertTrue(container.isNotEmpty())
    }
}
