/**
 * @file ShareServiceJvmTest.kt
 * Contains declarations for ShareServiceJvmTest.kt.
 */
package io.healthplatform.chartcam.utils

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for [JvmShareService] on Desktop JVM.
 */
class ShareServiceJvmTest {
    /**
     * Tests sharing text on JVM.
     */
    @Test
    fun testJvmShareServiceText() {
        val service = JvmShareService()
        val result = service.shareText("test string")
        assertNotNull(result)
    }

    /**
     * Tests sharing an existing temporary file on JVM.
     */
    @Test
    fun testJvmShareServiceFile() {
        val service = JvmShareService()
        val temp = File.createTempFile("test_export", ".txt")
        temp.writeText("sample export payload")
        val result = service.shareFile(temp.absolutePath)
        assertNotNull(result)
        temp.delete()
    }

    /**
     * Tests sharing a non-existent file path returns Result.failure with ExportFileNotFoundException.
     */
    @Test
    fun testJvmShareServiceMissingFile() {
        val service = JvmShareService()
        val result = service.shareFile("missing_archive_path_123.enc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ExportFileNotFoundException)
    }

    /**
     * Tests factory creation on JVM platform.
     */
    @Test
    fun testCreateShareServiceJvm() {
        val service = createShareService()
        assertTrue(service is JvmShareService)
    }
}
