/**
 * @file AndroidAppInit.kt
 * Contains declarations for AndroidAppInit.kt.
 *
 * File defining [AndroidAppInit] which acts as a holder for the application context.
 */
package io.healthplatform.chartcam

import android.annotation.SuppressLint
import android.content.Context

/**
 * A singleton object to hold the Android Application [Context].
 * Specific usage: enabling platform-dependent Kotlin Multiplatform (KMP) implementations
 * (like SecureStorage, CameraManager, etc.) to access the [Context] globally without
 * relying on complex Dependency Injection frameworks.
 */
@SuppressLint("StaticFieldLeak")
object AndroidAppInit {
    /**
     * The stored application [Context] reference.
     */
    private var context: Context? = null

    /**
     * Initializes the singleton with the provided context.
     * Should be called as early as possible, e.g., in `MainActivity.onCreate` or a custom `Application.onCreate`.
     *
     * @param ctx The [Context] to capture. The `applicationContext` will be extracted from it.
     */
    fun init(ctx: Context) {
        context = ctx.applicationContext
        migratePhotosFromCache(ctx.applicationContext)
    }

    /**
     * Migrates existing photos from the temporary cache directory to the persistent
     * internal files directory to prevent unexpected data loss by the OS.
     *
     * @param ctx The application context.
     */
    private fun migratePhotosFromCache(ctx: Context) {
        val cacheDir = ctx.cacheDir
        val filesDir = ctx.filesDir

        cacheDir.listFiles()?.forEach { file ->
            // Assuming photos or related encrypted data are the only things we store here
            // or specific patterns like ".enc" or image names.
            // Move files to filesDir.
            val destFile = java.io.File(filesDir, file.name)
            if (!destFile.exists()) {
                file.copyTo(destFile)
            }
            file.delete()
        }
    }

    /**
     * Retrieves the stored application [Context].
     *
     * @throws IllegalStateException if [init] has not been called prior to accessing the context.
     * @return The previously captured Application [Context].
     */
    fun getContext(): Context =
        checkNotNull(context) {
            "AndroidAppInit.init(context) must be called before using platform features."
        }
}
