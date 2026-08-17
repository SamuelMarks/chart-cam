/**
 * @file TestActivity.kt
 * Provides an isolated Activity for use in tests.
 */
package io.healthplatform.chartcam

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * A simple Activity used for Android tests.
 */
class TestActivity : ComponentActivity() {
    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidAppInit.init(this)
    }
}
