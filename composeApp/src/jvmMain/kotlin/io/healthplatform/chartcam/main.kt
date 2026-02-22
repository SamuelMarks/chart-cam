package io.healthplatform.chartcam

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() {
    // Set the macOS dock icon when running via gradle run
    try {
        if (System.getProperty("os.name").contains("Mac")) {
            val classLoader = Thread.currentThread().contextClassLoader
            classLoader.getResourceAsStream("icon.png")?.use { inputStream ->
                val image = ImageIO.read(inputStream)
                val taskbar = Taskbar.getTaskbar()
                taskbar.iconImage = image
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ChartCam",
            icon = painterResource("icon.png")
        ) {
            App()
        }
    }
}