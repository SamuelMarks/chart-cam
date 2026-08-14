package io.healthplatform.chartcam

import android.os.Bundle
import androidx.activity.ComponentActivity

class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidAppInit.init(this)
    }
}
