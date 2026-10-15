/**
 * @file EmptyJsTest.kt
 * Contains JS platform contract and UUID/storage sanity verification tests.
 */
package io.healthplatform.chartcam

import io.healthplatform.chartcam.files.createFileStorage
import io.healthplatform.chartcam.utils.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Platform contract verification tests for the JS target.
 */
class EmptyJsTest {
    /**
     * Verifies JS platform descriptor, UUID v4 formatting, and virtual file storage round-trip.
     */
    @Test
    fun testEmptyJs() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotBlank())

        val generatedUuid = UUID.randomUUID()
        assertEquals(36, generatedUuid.length)

        val storage = createFileStorage()
        val payload = "js_storage_payload".encodeToByteArray()
        val savedPath = storage.saveImage("js_contract.txt", payload)
        assertTrue(savedPath.endsWith("js_contract.txt"))
        assertEquals("js_storage_payload", storage.readImage(savedPath).decodeToString())
        assertTrue(storage.deleteImage(savedPath).isSuccess)
    }
}
