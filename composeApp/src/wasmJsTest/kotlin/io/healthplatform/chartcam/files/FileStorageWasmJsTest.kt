package io.healthplatform.chartcam.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FileStorageWasmJsTest {

    @Test
    fun testWasmJsFileStorageImplementation() {
        val storage = WasmJsFileStorage()
        
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
        assertTrue(factoryStorage is WasmJsFileStorage)
    }
}
