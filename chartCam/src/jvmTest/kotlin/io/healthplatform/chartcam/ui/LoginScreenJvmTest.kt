/**
 * @file LoginScreenJvmTest.kt
 * Contains declarations for LoginScreenJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.viewmodel.LoginUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Test class for LoginScreen on JVM.
 */
class LoginScreenJvmTest {
    /**
     * Verifies default LoginUiState properties.
     */
    @Test
    fun testLoginScreenJvm() {
        val state = LoginUiState()
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }
}
