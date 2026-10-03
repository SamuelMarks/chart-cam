package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.utils.CryptoService
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertTrue

class AuthRepositoryJvmTest {
    @Test
    fun testLoginCryptoFailure() =
        runTest {
            val storage = InMemorySecureStorage()
            val repo = AuthRepository(storage)

            // Use reflection to set the cryptoService field
            val field = AuthRepository::class.java.getDeclaredField("cryptoService")
            field.isAccessible = true

            val mockCrypto = Mockito.mock(CryptoService::class.java)
            // Make the mock throw an exception on encrypt
            Mockito
                .`when`(mockCrypto.encrypt(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(RuntimeException("Simulated crypto failure"))

            field.set(repo, mockCrypto)

            val result = repo.login("user_fail_crypto", "password")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AuthError.CryptoError)
        }

    @Test
    fun testLoginDecryptionReturnsWrongPayload() =
        runTest {
            val storage = InMemorySecureStorage()
            storage.save("auth_payload_user_wrong_payload", "dummy_encrypted")

            val repo = AuthRepository(storage)

            val field = AuthRepository::class.java.getDeclaredField("cryptoService")
            field.isAccessible = true

            val mockCrypto = Mockito.mock(CryptoService::class.java)
            // Succeeds but returns wrong string
            Mockito
                .`when`(mockCrypto.decrypt(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("WRONG_PAYLOAD")

            field.set(repo, mockCrypto)

            val result = repo.login("user_wrong_payload", "password")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AuthError.InvalidCredentials)
        }
}
