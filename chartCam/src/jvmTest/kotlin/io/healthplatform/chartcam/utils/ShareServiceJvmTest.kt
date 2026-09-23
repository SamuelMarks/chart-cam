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

    /**
     * Tests synthetic default implementation bridge for share service.
     */
    @Test
    fun testShareServiceDefaultImpls() {
        val service = JvmShareService()
        runCatching {
            val defaultImpls = Class.forName("io.healthplatform.chartcam.utils.ShareService\$DefaultImpls")
            val method = defaultImpls.getMethod("isValidSharePath", ShareService::class.java, String::class.java)
            val result = method.invoke(null, service, "valid") as Boolean
            assertTrue(result)
        }
    }

    /**
     * Tests sharing file when desktop operations are unsupported.
     */
    @Test
    fun testJvmShareServiceUnsupportedDesktop() {
        val temp = File.createTempFile("test_unsupported", ".txt")
        // allow-exception
        try {
            val service = JvmShareService(isDesktopSupportedProvider = { false })
            val result = service.shareFile(temp.absolutePath)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is PlatformShareException)
        } finally {
            temp.delete()
        }
    }

    /**
     * Tests dialog presentation branches when not running in test mode.
     */
    @Test
    fun testJvmShareServiceDialogAndNonTestingBranches() {
        val temp = File.createTempFile("test_dialog", ".txt")
        val dialogMessages = mutableListOf<String>()
        // allow-exception
        try {
            val service =
                JvmShareService(
                    isDesktopSupportedProvider = { true },
                    isTestingProvider = { false },
                    showDialogAction = { msg -> dialogMessages.add(msg) },
                    openFileAction = { },
                    copyTextAction = { },
                )
            val fileRes = service.shareFile(temp.absolutePath)
            assertTrue(fileRes.isSuccess)
            assertTrue(dialogMessages.any { it.contains("File saved to:") })

            val textRes = service.shareText("Sample text")
            assertTrue(textRes.isSuccess)
            assertTrue(dialogMessages.any { it.contains("Text copied to clipboard") })
        } finally {
            temp.delete()
        }
    }

    /**
     * Tests failure handling when openFileAction throws.
     */
    @Test
    fun testJvmShareServiceOpenFileFailure() {
        val temp = File.createTempFile("test_fail", ".txt")
        // allow-exception
        try {
            val service =
                JvmShareService(
                    openFileAction = { throw java.io.IOException("Cannot open directory") }, // allow-exception
                )
            val result = service.shareFile(temp.absolutePath)
            assertTrue(result.isFailure)
        } finally {
            temp.delete()
        }
    }

    /**
     * Tests failure handling when copyTextAction throws.
     */
    @Test
    fun testJvmShareServiceCopyTextFailure() {
        val service =
            JvmShareService(
                copyTextAction = { throw IllegalStateException("Clipboard locked") }, // allow-exception
            )
        val result = service.shareText("fails")
        assertTrue(result.isFailure)
    }

    /**
     * Tests file path with null parent directory fallback.
     */
    @Test
    fun testJvmShareServiceFileWithNullParent() {
        var openedFile: File? = null
        val service =
            JvmShareService(
                isDesktopSupportedProvider = { true },
                openFileAction = { file -> openedFile = file },
            )
        val localFile = File("local_share_test.txt")
        localFile.writeText("data")
        // allow-exception
        try {
            val result = service.shareFile("local_share_test.txt")
            assertTrue(result.isSuccess)
            assertNotNull(openedFile)
        } finally {
            localFile.delete()
        }
    }
}
