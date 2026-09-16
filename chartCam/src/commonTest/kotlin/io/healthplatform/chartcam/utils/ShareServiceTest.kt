/**
 * @file ShareServiceTest.kt
 * Contains declarations for ShareServiceTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Common test suite for [ShareService] and [ExportException] hierarchy.
 */
class ShareServiceTest {
    /**
     * Verifies that the platform share service instance initializes cleanly or fails with documented state.
     */
    @Test
    fun testShareService() {
        runCatching {
            val service = createShareService()
            assertNotNull(service)
        }.onFailure {
            assertTrue(it is IllegalStateException)
        }
    }

    /**
     * Tests the domain exception hierarchy for decentralized export failures.
     */
    @Test
    fun testExportExceptionHierarchy() {
        val passExDefault = InvalidExportPasswordException()
        assertEquals("Export password must not be empty or weak (minimum 6 characters).", passExDefault.message)
        val passExCustom = InvalidExportPasswordException("custom message")
        assertEquals("custom message", passExCustom.message)

        val fileEx = ExportFileNotFoundException("missing.enc")
        assertTrue(fileEx.message?.contains("missing.enc") == true)

        val storageExDefault = FileStorageSaveException("patient.dcm")
        assertTrue(storageExDefault.message?.contains("patient.dcm") == true)
        assertEquals(null, storageExDefault.cause)

        val storageExWithCause = FileStorageSaveException("patient.dcm", RuntimeException("disk full"))
        assertTrue(storageExWithCause.message?.contains("patient.dcm") == true)
        assertEquals("disk full", storageExWithCause.cause?.message)

        val platformExDefault = PlatformShareException("Android", "No activity found")
        assertTrue(platformExDefault.message?.contains("Android") == true)
        assertEquals(null, platformExDefault.cause)

        val platformExWithCause = PlatformShareException("Android", "No activity found", RuntimeException("intent failed"))
        assertTrue(platformExWithCause.message?.contains("Android") == true)
        assertEquals("intent failed", platformExWithCause.cause?.message)

        val pacsExDefault = PacsExportException("Corrupt tag dictionary")
        assertEquals("Corrupt tag dictionary", pacsExDefault.message)
        assertEquals(null, pacsExDefault.cause)

        val pacsExWithCause = PacsExportException("Corrupt tag dictionary", RuntimeException("parse error"))
        assertEquals("Corrupt tag dictionary", pacsExWithCause.message)
        assertEquals("parse error", pacsExWithCause.cause?.message)
    }

    /**
     * Tests custom mock implementation of [ShareService] to ensure contract conformance.
     */
    @Test
    fun testMockShareServiceContract() {
        val mockService =
            object : ShareService {
                var sharedFile: String? = null
                var sharedText: String? = null

                override fun shareFile(filePath: String): Result<Unit> =
                    if (filePath.endsWith(".enc") || filePath.endsWith(".json")) {
                        sharedFile = filePath
                        Result.success(Unit)
                    } else {
                        Result.failure(ExportFileNotFoundException(filePath))
                    }

                override fun shareText(text: String): Result<Unit> =
                    if (text.isNotBlank()) {
                        sharedText = text
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalArgumentException("Blank text"))
                    }
            }

        assertTrue(mockService.shareFile("export.enc").isSuccess)
        assertEquals("export.enc", mockService.sharedFile)

        assertTrue(mockService.shareFile("invalid.xyz").isFailure)

        assertTrue(mockService.shareText("secret-pass-123").isSuccess)
        assertEquals("secret-pass-123", mockService.sharedText)

        assertTrue(mockService.shareText("").isFailure)
    }
}
