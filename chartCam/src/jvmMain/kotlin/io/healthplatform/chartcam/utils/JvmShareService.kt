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
 *
 * @param isDesktopSupportedProvider Checks if Desktop operations are supported.
 * @param openFileAction Action to open a file or directory in Desktop file manager.
 * @param showDialogAction Action to show a confirmation dialog.
 * @param copyTextAction Action to set text to the system clipboard.
 * @param isTestingProvider Checks if current environment is in test mode.
 */
class JvmShareService(
    private val isDesktopSupportedProvider: () -> Boolean = { Desktop.isDesktopSupported() },
    private val openFileAction: (File) -> Unit = { target -> Desktop.getDesktop().open(target) },
    private val showDialogAction: (String) -> Unit = { msg -> JOptionPane.showMessageDialog(null, msg) },
    private val copyTextAction: (String) -> Unit = { text ->
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    },
    private val isTestingProvider: () -> Boolean = {
        Thread.currentThread().stackTrace.any {
            it.className.startsWith("org.junit.") || it.className.startsWith("kotlin.test.")
        }
    },
) : ShareService {
    /**
     * Checks if the current code is running in a testing environment.
     *
     * @return True if in a testing environment, false otherwise.
     */
    private fun isTesting(): Boolean = isTestingProvider.invoke()

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
                !isDesktopSupportedProvider.invoke() ->
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
            openFileAction.invoke(file.parentFile ?: file)
            if (!isTesting()) {
                showDialogAction.invoke("File saved to: ${file.absolutePath}")
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
            copyTextAction.invoke(text)
            if (!isTesting()) {
                showDialogAction.invoke("Text copied to clipboard")
            }
        }.mapCatching { }
}

/**
 * Creates and returns a new instance of the [ShareService] for the JVM platform.
 *
 * @return A new [JvmShareService] instance.
 */
actual fun createShareService(): ShareService = JvmShareService()
