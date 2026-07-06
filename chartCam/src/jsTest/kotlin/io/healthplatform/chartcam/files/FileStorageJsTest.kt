/**
 * Provides tests for the JavaScript-specific implementation of File Storage.
 */
package io.healthplatform.chartcam.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for [JsFileStorage] and related factory functions.
 *
 * Verifies that file saving, reading, and cache clearing operate correctly
 * in the JS-based in-memory storage environment.
 */
class FileStorageJsTest {
    /**
     * Tests the core functionality of the [JsFileStorage] implementation.
     *
     * Validates saving an image byte array to an in-memory path, reading it back,
     * attempting to read missing files, and clearing the file cache.
     */
    @Test
    fun testJsFileStorageImplementation() {
        val storage = JsFileStorage()

        // Test save
        val fileName = "test.jpg"
        val bytes = ByteArray(5) { 1 }
        val resultPath = storage.saveImage(fileName, bytes)
        assertEquals("mem://path/test.jpg", resultPath)

        // Test read
        val readBytes = storage.readImage(resultPath)
        assertFalse(readBytes.isEmpty())
        assertEquals(5, readBytes.size)
        assertEquals(1, readBytes[0])

        // Test read missing
        val missingBytes = storage.readImage("mem://path/missing.jpg")
        assertTrue(missingBytes.isEmpty())

        // Test clear
        storage.clearCache()
        val clearedBytes = storage.readImage(resultPath)
        assertTrue(clearedBytes.isEmpty())

        // Test factory
        val factoryStorage = createFileStorage()
        assertTrue(factoryStorage is JsFileStorage)
    }
}
