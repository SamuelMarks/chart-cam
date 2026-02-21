package io.healthplatform.chartcam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Main Entry point for the Android Application.
 */
class MainActivity : ComponentActivity() { 
    override fun onCreate(savedInstanceState: Bundle?) { 
        enableEdgeToEdge() 
        super.onCreate(savedInstanceState) 
        
        // Initialize the Context holder for KMP Android implementation
        AndroidAppInit.init(this)

        setContent { 
            App() 
        } 
    } 
} 

@Preview
@Composable
fun AppAndroidPreview() { 
    App() 
}