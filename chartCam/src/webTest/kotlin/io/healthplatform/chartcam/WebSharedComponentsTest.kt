/**
 * @file WebSharedComponentsTest.kt
 * Shared Web unit tests for [WebFileStorage], [WebKeystoreHardwareProvider], [resolvePlatformColorScheme],
 * and [SetupBrowserHistory] across JS and WasmJS targets.
 */
package io.healthplatform.chartcam

import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.navigation.Routes
import io.healthplatform.chartcam.storage.BiometricHardwareStatus
import io.healthplatform.chartcam.storage.createKeystoreHardwareProvider
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Shared Web unit tests exercising `webMain` components on both JS and WasmJS targets.
 */
class WebSharedComponentsTest {
    /**
     * Verifies [createFileStorage] round-trip save, read, delete, and cache clearing on Web.
     */
    @Test
    fun testWebFileStorageRoundTrip() {
        val storage = createFileStorage()
        assertNotNull(storage)
        storage.clearCache()

        val payload = byteArrayOf(1, 2, 3, 4)
        val path = storage.saveImage("web_shared_test.bin", payload)
        assertEquals("mem://path/web_shared_test.bin", path)
        assertContentEquals(payload, storage.readImage(path))

        assertTrue(storage.deleteImage(path).isSuccess)
        assertTrue(storage.deleteImage(path).isFailure)
        assertEquals(0, storage.readImage(path).size)

        storage.saveImage("cached_1.bin", payload)
        storage.clearCache()
        assertEquals(0, storage.readImage("mem://path/cached_1.bin").size)
    }

    /**
     * Verifies [createKeystoreHardwareProvider] returns [BiometricHardwareStatus.NO_HARDWARE] on Web.
     */
    @Test
    fun testWebKeystoreHardwareProvider() {
        val provider = createKeystoreHardwareProvider()
        assertNotNull(provider)
        assertTrue(provider.checkHardwareBacked().isFailure)
        assertEquals(BiometricHardwareStatus.NO_HARDWARE, provider.getHardwareStatus())
    }

    /**
     * Verifies web browser history route constants.
     */
    @Test
    fun testWebRoutes() {
        assertEquals("/auth/login", Routes.LOGIN)
    }
}
