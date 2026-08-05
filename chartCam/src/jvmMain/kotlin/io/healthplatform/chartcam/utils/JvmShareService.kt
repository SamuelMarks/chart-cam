/**
 * @file ShareService.jvm.kt
 * Sharing service implementation for the JVM platform.
 */
package io.healthplatform.chartcam.utils

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JOptionPane

/**
 * JVM implementation for sharing files and text.
 * On Desktop, this typically opens the file location or copies text to the clipboard.
 */
class JvmShareService : ShareService {
    /**
     * Checks if the current code is running in a testing environment.
     *
     * @return True if in a testing environment, false otherwise.
     */
    private fun isTesting(): Boolean =
        Thread.currentThread().stackTrace.any {
            it.className.startsWith("org.junit.") || it.className.startsWith("kotlin.test.")
        }

    /**
     * Shares a file by opening its parent directory in the native file explorer
     * and showing a confirmation dialog to the user.
     *
     * @param filePath The absolute path of the file to be shared.
     */
    override fun shareFile(filePath: String) {
        val file = File(filePath)
        if (file.exists() && Desktop.isDesktopSupported()) {
            try {
                // Just open the file directory or the file itself as "sharing"
                Desktop.getDesktop().open(file.parentFile)
                if (!isTesting()) {
                    JOptionPane.showMessageDialog(null, "File saved to: ${file.absolutePath}")
                }
            } catch (e: IllegalStateException) {
                println(e.message)
            }
        }
    }

    /**
     * Shares text by copying it to the system clipboard and showing a
     * confirmation dialog to the user.
     *
     * @param text The text string to be copied to the clipboard.
     */
    override fun shareText(text: String) {
        try {
            val selection = StringSelection(text)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)
            if (!isTesting()) {
                JOptionPane.showMessageDialog(null, "Text copied to clipboard")
            }
        } catch (e: IllegalStateException) {
            println("Headless environment detected, cannot copy to clipboard: ${e.message}")
        } catch (e: java.awt.HeadlessException) {
            println("Headless environment detected, cannot copy to clipboard: ${e.message}")
        }
    }
}

/**
 * Creates and returns a new instance of the [ShareService] for the JVM platform.
 *
 * @return A new [JvmShareService] instance.
 */
actual fun createShareService(): ShareService = JvmShareService()
