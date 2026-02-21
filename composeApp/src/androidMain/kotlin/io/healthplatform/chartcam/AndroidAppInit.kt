package io.healthplatform.chartcam

import android.annotation.SuppressLint
import android.content.Context

/**
 * A singleton object to hold the Application Context.
 * specific usage: enabling platform-dependent KMP implementations (like SecureStorage)
 * to access Context without complex DI frameworks in the prompt constraints.
 */
@SuppressLint("StaticFieldLeak")
object AndroidAppInit {
    private var context: Context? = null

    /**
     * Initializes the holder with the application context.
     * Should be called in MainActivity.onCreate or Application.onCreate.
     *
     * @param ctx The context to hold.
     */
    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    /**
     * Retrieves the stored context.
     *
     * @throws IllegalStateException if init() has not been called.
     * @return The captured Application Context.
     */
    fun getContext(): Context {
        return context ?: throw IllegalStateException("AndroidAppInit.init(context) must be called before using platform features.")
    }
}