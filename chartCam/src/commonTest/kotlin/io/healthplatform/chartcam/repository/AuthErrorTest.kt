package io.healthplatform.chartcam.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for [AuthError] variants.
 */
class AuthErrorTest {
    @Test
    fun testInvalidCredentials() {
        val error = AuthError.InvalidCredentials()
        assertEquals("Invalid Credentials", error.message)
        assertNull(error.cause)

        val customError = AuthError.InvalidCredentials("Custom message")
        assertEquals("Custom message", customError.message)
    }

    @Test
    fun testCryptoError() {
        val cause = RuntimeException("Crypto failed")
        val error = AuthError.CryptoError("Error decrypting", cause)
        assertEquals("Error decrypting", error.message)
        assertSame(cause, error.cause)

        val errorNoCause = AuthError.CryptoError("Error decrypting no cause")
        assertEquals("Error decrypting no cause", errorNoCause.message)
        assertNull(errorNoCause.cause)
    }

    @Test
    fun testNoActiveSession() {
        val error = AuthError.NoActiveSession()
        assertEquals("No active session", error.message)
        assertNull(error.cause)

        val customError = AuthError.NoActiveSession("Custom no session")
        assertEquals("Custom no session", customError.message)
    }

    @Test
    fun testStorageError() {
        val cause = RuntimeException("Storage full")
        val error = AuthError.StorageError("Error saving", cause)
        assertEquals("Error saving", error.message)
        assertSame(cause, error.cause)

        val errorNoCause = AuthError.StorageError("Error saving no cause")
        assertEquals("Error saving no cause", errorNoCause.message)
        assertNull(errorNoCause.cause)
    }
}
