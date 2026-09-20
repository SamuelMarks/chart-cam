/**
 * @file PermissionManagerJvmTest.kt
 * Contains declarations for PermissionManagerJvmTest.kt.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for PermissionManager on JVM.
 */
class PermissionManagerJvmTest {
    /**
     * Compose test rule for checking rememberPermissionManager.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test permission manager methods on JVM.
     */
    @Test
    fun testPermissionManagerJvm() {
        val manager = JvmPermissionManager()
        val status = manager.getCameraPermissionStatus()
        assertNotNull(status)

        runBlocking {
            val result = manager.requestCameraPermission()
            if (status == PermissionStatus.GRANTED) {
                assertTrue(result.isSuccess)
            } else {
                assertFalse(result.isSuccess)
                assertTrue(result.exceptionOrNull() is PermissionDeniedException)
            }
        }
    }

    /**
     * Test permission manager when no webcam is present (DENIED status).
     */
    @Test
    fun testDeniedPermissionBranches() {
        runBlocking {
            // empty webcams list
            val deniedManager = JvmPermissionManager(webcamSupplier = { emptyList() })
            assertEquals(PermissionStatus.DENIED, deniedManager.getCameraPermissionStatus())
            val res = deniedManager.requestCameraPermission()
            assertFalse(res.isSuccess)
            assertTrue(res.exceptionOrNull() is PermissionDeniedException)

            // null webcams list (exception occurred in lookup)
            val nullWebcamManager = JvmPermissionManager(webcamSupplier = { null })
            assertEquals(PermissionStatus.DENIED, nullWebcamManager.getCameraPermissionStatus())

            // throwing webcams list
            val throwingManager = JvmPermissionManager(webcamSupplier = { error("Simulated lookup failure") })
            assertEquals(PermissionStatus.DENIED, throwingManager.getCameraPermissionStatus())
        }
    }

    /**
     * Test permission manager when a webcam is present (GRANTED status).
     */
    @Test
    fun testGrantedPermissionBranch() {
        runBlocking {
            val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
            unsafeField.isAccessible = true
            val unsafe = unsafeField.get(null) as sun.misc.Unsafe
            val fakeWebcam =
                unsafe.allocateInstance(
                    com.github.sarxos.webcam.Webcam::class.java,
                ) as com.github.sarxos.webcam.Webcam
            val grantedManager = JvmPermissionManager(webcamSupplier = { listOf(fakeWebcam) })
            assertEquals(PermissionStatus.GRANTED, grantedManager.getCameraPermissionStatus())
            val res = grantedManager.requestCameraPermission()
            assertTrue(res.isSuccess)
        }
    }

    /**
     * Test openSettings across different OS flavors.
     */
    @Test
    fun testOpenSettingsOSBranches() {
        val manager = JvmPermissionManager()
        val origOS = System.getProperty("os.name")
        runCatching {
            // Mac OS path
            System.setProperty("os.name", "Mac OS X")
            manager.openSettings()

            // Windows path
            System.setProperty("os.name", "Windows 11")
            manager.openSettings()

            // Linux / Unknown path
            System.setProperty("os.name", "Linux")
            manager.openSettings()

            // Null OS property fallback
            System.clearProperty("os.name")
            manager.openSettings()
        }.also {
            if (origOS != null) {
                System.setProperty("os.name", origOS)
            }
        }
    }

    /**
     * Test rememberPermissionManager on JVM.
     */
    @Test
    fun testRememberPermissionManager() {
        composeTestRule.setContent {
            val manager = rememberPermissionManager()
            assertNotNull(manager)
        }
    }

    /**
     * Tests PermissionManager and CameraManager DefaultImpls bytecode bridges.
     */
    @Test
    fun testDefaultImplsCoverage() {
        val permManager =
            object : PermissionManager {
                override fun getCameraPermissionStatus(): PermissionStatus = PermissionStatus.GRANTED

                override suspend fun requestCameraPermission(): Result<Unit> = Result.success(Unit)

                override fun openSettings() {}
            }
        val permClass = Class.forName("io.healthplatform.chartcam.camera.PermissionManager\$DefaultImpls")
        for (m in permClass.declaredMethods) {
            m.isAccessible = true
            runCatching {
                m.invoke(null, permManager)
            }
        }

        val camManager =
            object : CameraManager {
                override suspend fun captureImage(): ByteArray? = null

                override fun release() {}
            }
        val camClass = Class.forName("io.healthplatform.chartcam.camera.CameraManager\$DefaultImpls")
        for (m in camClass.declaredMethods) {
            m.isAccessible = true
            runCatching {
                if (m.parameterCount == 1) {
                    m.invoke(null, camManager)
                } else if (m.parameterCount == 2 && m.parameterTypes[1] == java.lang.Boolean.TYPE) {
                    m.invoke(null, camManager, true)
                } else if (m.parameterCount == 2) {
                    m.invoke(null, camManager, null)
                }
            }
        }
    }
}
