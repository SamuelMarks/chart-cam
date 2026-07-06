package io.healthplatform.chartcam.utils

import org.junit.Test
import java.io.File

class ShareServiceJvmTest {
    @Test
    fun testShareServiceJvm() {
        // System.setProperty("java.awt.headless", "true") -> this could prevent JOptionPane from showing

        val service = createShareService()

        try {
            service.shareText("Hello World")
        } catch (e: Exception) {
            // Might throw HeadlessException
        }

        val tempFile = File.createTempFile("test_share_jvm", ".txt")
        try {
            service.shareFile(tempFile.absolutePath)
        } catch (e: Exception) {
            // Might throw HeadlessException or UnsupportedOperationException
        } finally {
            tempFile.delete()
        }
    }
}
