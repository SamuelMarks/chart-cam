/**
 * @file AndroidShareService.kt
 * Contains declarations for AndroidShareService.kt.
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
 * @param uriProvider Provider function resolving a content [android.net.Uri] for a given file.
 */
class AndroidShareService(
    private val context: Context,
    private val uriProvider: (Context, String, File) -> android.net.Uri = { ctx, auth, file ->
        FileProvider.getUriForFile(ctx, auth, file)
    },
) : ShareService {
    /**
     * Resolves the target file on disk, attempting direct resolution followed by
     * lookups in internal persistent storage and cache directories.
     *
     * @param filePath The provided absolute or relative file path.
     * @return The resolved [File] if it exists, or null.
     */
    private fun resolveFile(filePath: String): File? {
        val directFile = File(filePath)
        val cleanName = directFile.name
        val candidates =
            listOf(
                directFile,
                File(context.filesDir, cleanName),
                File(context.cacheDir, cleanName),
            )
        return candidates.firstOrNull { it.exists() }
    }

    /**
     * Shares a file to other applications using Android's [Intent.ACTION_SEND].
     *
     * @param filePath The absolute or relative path to the file to be shared.
     * @return A [Result] indicating success or failure.
     */
    override fun shareFile(filePath: String): Result<Unit> {
        val file =
            resolveFile(filePath)
                ?: return Result.failure(ExportFileNotFoundException(filePath))

        return runCatching {
            val uri =
                uriProvider.invoke(
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
        }.mapCatching { }
    }

    /**
     * Shares plain text to other applications using Android's [Intent.ACTION_SEND].
     *
     * @param text The plain text content to be shared.
     * @return A [Result] indicating success or failure.
     */
    override fun shareText(text: String): Result<Unit> =
        runCatching {
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
        }.mapCatching { }
}

/**
 * Creates and returns an instance of [AndroidShareService] tailored for the Android platform.
 *
 * @return A new instance of [ShareService].
 */
actual fun createShareService(): ShareService = AndroidShareService(AndroidAppInit.getContext())
