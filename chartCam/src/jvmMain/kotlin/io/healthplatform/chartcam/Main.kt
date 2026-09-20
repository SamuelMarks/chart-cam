/**
 * @file Main.kt
 * Main entry point for the JVM (Desktop) application.
 */
package io.healthplatform.chartcam

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Taskbar
import javax.imageio.ImageIO

/**
 * The main execution function for the ChartCam Desktop application.
 * It sets up platform-specific configurations like the macOS dock icon
 * and launches the Compose multiplatform application window.
 */

fun main() {
    System.setProperty("apple.awt.application.name", "ChartCam")
    // Set the macOS dock icon when running via gradle run
    runCatching {
        if (System.getProperty("os.name").contains("Mac")) {
            val classLoader = Thread.currentThread().contextClassLoader
            classLoader.getResourceAsStream("icon.png")?.use { inputStream ->
                val image = ImageIO.read(inputStream)
                val taskbar = Taskbar.getTaskbar()
                taskbar.iconImage = image
            }
        }
    }.onFailure { println(it.message) }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ChartCam",
            icon = painterResource(Res.drawable.icon),
        ) {
            androidx.compose.runtime.DisposableEffect(Unit) {
                val listener =
                    object : java.awt.event.WindowFocusListener {
                        /**
                         * Un-obscures UI and validates session timeout when desktop window gains focus.
                         *
                         * @param e The window event.
                         */
                        override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                            io.healthplatform.chartcam.ui.currentAppPrivacyManager
                                .onAppMovedToForeground()
                        }

                        /**
                         * Obscures UI and tracks background duration when desktop window loses focus.
                         *
                         * @param e The window event.
                         */
                        override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                            io.healthplatform.chartcam.ui.currentAppPrivacyManager
                                .onAppMovedToBackground()
                        }
                    }
                window.addWindowFocusListener(listener)
                onDispose {
                    window.removeWindowFocusListener(listener)
                }
            }
            App()
        }
    }
}
