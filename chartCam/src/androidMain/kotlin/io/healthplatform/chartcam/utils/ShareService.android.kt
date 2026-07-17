/**
 * @file ShareService.android.kt
 * Contains declarations for ShareService.android.kt.
 *
 * File defining the Android-specific implementation for the [ShareService] interface.
 */
package io.healthplatform.chartcam.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.healthplatform.chartcam.AndroidAppInit
import java.io.File

/**
 * Android implementation for sharing files and text via system intents.
 * Utilizes [FileProvider] to grant external apps secure access to files.
 *
 * @param context The application [Context] used to generate URIs and launch intents.
 */
class AndroidShareService(
    private val context: Context,
) : ShareService {
    /**
     * Shares a file to other applications using Android's [Intent.ACTION_SEND].
     *
     * @param filePath The absolute path to the file to be shared. If the file does not exist, the operation is aborted.
     */
    override fun shareFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val chooser =
            Intent.createChooser(intent, "Share Export").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooser)
    }

    /**
     * Shares plain text to other applications using Android's [Intent.ACTION_SEND].
     *
     * @param text The plain text content to be shared.
     */
    override fun shareText(text: String) {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val chooser =
            Intent.createChooser(intent, "Share Password").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooser)
    }
}

/**
 * Creates and returns an instance of [AndroidShareService] tailored for the Android platform.
 *
 * @return A new instance of [ShareService].
 */
actual fun createShareService(): ShareService = AndroidShareService(AndroidAppInit.getContext())
