/**
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
    }

    /**
     * Retrieves the stored application [Context].
     *
     * @throws IllegalStateException if [init] has not been called prior to accessing the context.
     * @return The previously captured Application [Context].
     */
    fun getContext(): Context =
        context ?: throw IllegalStateException("AndroidAppInit.init(context) must be called before using platform features.")
}
