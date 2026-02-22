package io.healthplatform.chartcam.utils

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JOptionPane

/**
 * JVM implementation for sharing files and text.
 */
class JvmShareService : ShareService {
    override fun shareFile(filePath: String) {
        val file = File(filePath)
        if (file.exists() && Desktop.isDesktopSupported()) {
            try {
                // Just open the file directory or the file itself as "sharing"
                Desktop.getDesktop().open(file.parentFile)
                JOptionPane.showMessageDialog(null, "File saved to: ${file.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun shareText(text: String) {
        val selection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
        JOptionPane.showMessageDialog(null, "Text copied to clipboard")
    }
}

/**
 * Creates the JvmShareService.
 */
actual fun createShareService(): ShareService = JvmShareService()
