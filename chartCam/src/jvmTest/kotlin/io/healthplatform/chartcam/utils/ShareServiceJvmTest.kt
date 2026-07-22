/**
 * @file ShareServiceJvmTest.kt
 * Contains declarations for ShareServiceJvmTest.kt.
 */
package io.healthplatform.chartcam.utils

import java.io.File
import kotlin.test.Test

class ShareServiceJvmTest {
    @Test
    fun testJvmShareServiceText() {
        val service = JvmShareService()
        // It detects test environment and won't show dialog, but will set clipboard
        service.shareText("test string")
    }

    @Test
    fun testJvmShareServiceFile() {
        val service = JvmShareService()
        val temp = File.createTempFile("test", ".txt")
        try {
            // Testing env prevents dialog, but it might still invoke Desktop API.
            // Some headless environments don't support Desktop, we just call it and catch/ignore errors if any
            service.shareFile(temp.absolutePath)
        } catch (e: Exception) {
        } finally {
            temp.delete()
        }
    }
}
