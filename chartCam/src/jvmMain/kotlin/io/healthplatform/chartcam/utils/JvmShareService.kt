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
     * @return A [Result] indicating success or failure.
     */
    override fun shareFile(filePath: String): Result<Unit> {
        val file = File(filePath)
        val failure =
            when {
                !file.exists() -> ExportFileNotFoundException(filePath)
                !Desktop.isDesktopSupported() ->
                    PlatformShareException(
                        platform = "JVM",
                        reason = "Desktop operations are unsupported in this environment",
                    )
                else -> null
            }
        if (failure != null) {
            return Result.failure(failure)
        }

        return runCatching {
            Desktop.getDesktop().open(file.parentFile ?: file)
            if (!isTesting()) {
                JOptionPane.showMessageDialog(null, "File saved to: ${file.absolutePath}")
            }
        }.mapCatching { }
    }

    /**
     * Shares text by copying it to the system clipboard and showing a
     * confirmation dialog to the user.
     *
     * @param text The text string to be copied to the clipboard.
     * @return A [Result] indicating success or failure.
     */
    override fun shareText(text: String): Result<Unit> =
        runCatching {
            val selection = StringSelection(text)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)
            if (!isTesting()) {
                JOptionPane.showMessageDialog(null, "Text copied to clipboard")
            }
        }.mapCatching { }
}

/**
 * Creates and returns a new instance of the [ShareService] for the JVM platform.
 *
 * @return A new [JvmShareService] instance.
 */
actual fun createShareService(): ShareService = JvmShareService()
