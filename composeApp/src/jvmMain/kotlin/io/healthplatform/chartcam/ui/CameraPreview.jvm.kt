package io.healthplatform.chartcam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.healthplatform.chartcam.camera.CameraManager
import io.healthplatform.chartcam.camera.JvmCameraManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun CameraPreview(modifier: Modifier, cameraManager: CameraManager) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(cameraManager) {
        if (cameraManager is JvmCameraManager) {
            while (isActive) {
                val img = cameraManager.getPreviewImage()
                if (img != null) {
                    imageBitmap = img.toComposeImageBitmap()
                }
                delay(33) // ~30 fps
            }
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Camera Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("Initializing Camera...", color = Color.White)
        }
    }
}
