package io.healthplatform.chartcam.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of PermissionManager using Activity Result API.
 */
class AndroidPermissionManager(
    private val context: android.content.Context,
    private val requestLauncher: (String) -> Unit
) : PermissionManager {

    // Helper to store continuation for suspension
    var callback: ((Boolean) -> Unit)? = null

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

    override suspend fun requestCameraPermission(): Boolean {
        if (getCameraPermissionStatus() == PermissionStatus.GRANTED) return true
        
        return suspendCancellableCoroutine { cont ->
            callback = { isGranted ->
                cont.resume(isGranted)
            }
            requestLauncher(Manifest.permission.CAMERA)
        }
    }

    override fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun onPermissionResult(isGranted: Boolean) {
        callback?.invoke(isGranted)
        callback = null
    }
}

@Composable
actual fun rememberPermissionManager(): PermissionManager {
    val context = LocalContext.current
    // Use a mutable state or reference to hold the manager so we can update it with the launcher
    // However, the launcher must be created in composition.
    
    // Pattern: We create the manager, and inject the launcher trigger.
    // But the launcher callback needs to call back into the manager.
    
    var manager by remember { mutableStateOf<AndroidPermissionManager?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        manager?.onPermissionResult(isGranted)
    }
    
    val currentManager = remember {
        AndroidPermissionManager(context) { permission ->
            launcher.launch(permission)
        }
    }
    
    manager = currentManager
    return currentManager
}