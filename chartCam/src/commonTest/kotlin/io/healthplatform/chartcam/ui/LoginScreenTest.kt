/**
 * @file LoginScreenTest.kt
 * Contains declarations for LoginScreenTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.viewmodel.LoginUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Common test for [LoginScreen] and [LoginUiState].
 */
class LoginScreenTest {
    /**
     * Verifies default LoginUiState creation and initial flags.
     */
    @Test
    fun testLoginUiStateDefaults() {
        val state = LoginUiState()
        assertNotNull(state)
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }
}
