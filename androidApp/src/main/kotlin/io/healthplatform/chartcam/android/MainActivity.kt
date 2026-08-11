/**
 * @file MainActivity.kt
 * Contains the main Android entry point for the ChartCam application.
 */
package io.healthplatform.chartcam.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.healthplatform.chartcam.AndroidAppInit
import io.healthplatform.chartcam.App

/**
 * Main Entry point for the Android Application.
 */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Prevent screenshots and screen recording for security/HIPAA compliance
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Initialize the Context holder for KMP Android implementation
        // This object resides in the Shared Library
        AndroidAppInit.init(this)

        setContent {
            App()
        }
    }
}

/**
 * Preview for the main App composable on Android.
 */
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
