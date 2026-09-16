/**
 * @file PermissionManagerJvmTest.kt
 * Contains declarations for PermissionManagerJvmTest.kt.
 */
package io.healthplatform.chartcam.camera

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test
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

        kotlinx.coroutines.runBlocking {
            val result = manager.requestCameraPermission()
            assertTrue(result.isSuccess)
        }

        // Test openSettings does not throw
        manager.openSettings()
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
}
