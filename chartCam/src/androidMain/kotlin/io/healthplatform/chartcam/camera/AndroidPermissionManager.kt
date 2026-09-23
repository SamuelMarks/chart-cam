/**
 * @file AndroidPermissionManager.kt
 * Contains declarations for AndroidPermissionManager.kt.
 *
 * File defining the Android-specific implementation of [PermissionManager] and its composable factory.
 */
package io.healthplatform.chartcam.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [PermissionManager] using the Activity Result API.
 *
 * Handles checking, requesting, and routing to system settings for camera permissions.
 *
 * @param context The Android [android.content.Context] used to check permissions and start activities.
 * @param requestLauncher A lambda function responsible for initiating the permission
 * request via the Activity Result API.
 */
class AndroidPermissionManager(
    private val context: android.content.Context,
    internal val requestLauncher: (String) -> Unit,
) : PermissionManager {
    /**
     * Helper property to store the continuation for the suspending permission request.
     * This callback is invoked when the Activity Result returns.
     */
    var callback: ((Boolean) -> Unit)? = null

    /**
     * Retrieves the current camera permission status.
     *
     * @return [PermissionStatus.GRANTED] if permission is granted, otherwise [PermissionStatus.DENIED].
     */
    override fun getCameraPermissionStatus(): PermissionStatus {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        return if (status == PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.GRANTED
        } else {
            // Simplified logic: Android doesn't explicitly have "Not Determined" vs "Denied"
            // without checking rationale/preferences, defaulting to Denied/NotDetermined behavior.
            PermissionStatus.DENIED
        }
    }

    /**
     * Requests the camera permission from the user asynchronously.
     *
     * @return A [Result] indicating success if granted, or [PermissionDeniedException] if denied.
     */
    override suspend fun requestCameraPermission(): Result<Unit> {
        if (getCameraPermissionStatus() == PermissionStatus.GRANTED) return Result.success(Unit)

        return suspendCancellableCoroutine { cont ->
            callback = { isGranted ->
                if (isGranted) {
                    cont.resume(Result.success(Unit))
                } else {
                    val ex = PermissionDeniedException(message = "Android camera permission was denied")
                    cont.resume(Result.failure(ex))
                }
            }
            requestLauncher(Manifest.permission.CAMERA)
        }
    }

    /**
     * Opens the application's detailed settings screen in the Android system UI,
     * allowing the user to manually grant permissions if they previously permanently denied them.
     */
    override fun openSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    /**
     * Callback method invoked by the permission launcher when the result is available.
     * Resumes the suspended [requestCameraPermission] coroutine.
     *
     * @param isGranted `true` if the permission was granted, `false` otherwise.
     */
    fun onPermissionResult(isGranted: Boolean) {
        callback?.invoke(isGranted)
        callback = null
    }
}

/**
 * A composable function that remembers an instance of [PermissionManager] tailored for the Android platform.
 * Hooks into the Activity Result API to handle permission requests cleanly within the Compose lifecycle.
 *
 * @return An instance of [PermissionManager] (specifically [AndroidPermissionManager]).
 */
@Composable
actual fun rememberPermissionManager(): PermissionManager = rememberPermissionManagerInternal()

/**
 * Internal composable function that instantiates [AndroidPermissionManager] with optional custom launcher hook.
 *
 * @param launcherFactory Optional factory providing a custom permission request action for testing.
 * @return An instance of [PermissionManager].
 */
@Composable
internal fun rememberPermissionManagerInternal(
    launcherFactory: ((onResult: (Boolean) -> Unit) -> ((String) -> Unit))? = null,
): PermissionManager {
    val context = LocalContext.current

    var manager by remember { mutableStateOf<AndroidPermissionManager?>(null) }

    val onResultAction: (Boolean) -> Unit = { isGranted ->
        manager?.onPermissionResult(isGranted)
    }

    val defaultLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
            onResultAction,
        )

    val launchAction: (String) -> Unit =
        if (launcherFactory != null) {
            launcherFactory.invoke(onResultAction)
        } else {
            { permission -> defaultLauncher.launch(permission) }
        }

    val currentManager =
        remember {
            AndroidPermissionManager(context) { permission ->
                launchAction.invoke(permission)
            }
        }

    manager = currentManager
    return currentManager
}
