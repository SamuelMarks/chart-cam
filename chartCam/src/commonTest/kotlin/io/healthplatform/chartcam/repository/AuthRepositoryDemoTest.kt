/**
 * @file AuthRepositoryDemoTest.kt
 * Contains declarations for AuthRepositoryDemoTest.kt.
 *
 * Unit tests validating demo mode authentication and session lifecycle in [AuthRepository].
 */
package io.healthplatform.chartcam.repository

import io.healthplatform.chartcam.storage.SecureStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Validates demo session state transitions, token management, and logout teardown in [AuthRepository].
 */
class AuthRepositoryDemoTest {
    /**
     * In-memory mock storage implementation for testing session tokens.
     */
    class MockStorage : SecureStorage {
        /** Map holding key-value pairs. */
        val map = mutableMapOf<String, String>()

        /**
         * Saves key value pair.
         * @param key The key.
         * @param value The value.
         */
        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        /**
         * Gets value for key.
         * @param key The key.
         * @return The value or null.
         */
        override fun getString(key: String): String? = map[key]

        /**
         * Deletes value for key.
         * @param key The key.
         */
        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /**
     * Verifies that loginAsDemo provisions the demo practitioner and sets isDemoSession to true.
     */
    @Test
    fun testLoginAsDemo() =
        runTest {
            val storage = MockStorage()
            val repository = AuthRepository(storage)

            assertFalse(repository.isDemoSession.value)
            assertNull(repository.currentUser.value)

            val result = repository.loginAsDemo()
            assertTrue(result.isSuccess)

            val practitioner = result.getOrNull()
            assertNotNull(practitioner)
            assertEquals(AuthRepository.DEMO_PRACTITIONER_ID, practitioner.id)
            assertEquals(
                "Clinician",
                practitioner.name
                    .first()
                    .family
                    ?.value,
            )
            assertEquals(
                "Dr. Demo",
                practitioner.name
                    .first()
                    .given
                    .first()
                    .value,
            )

            assertTrue(repository.isDemoSession.value)
            assertEquals(practitioner, repository.currentUser.value)

            // Storage verification
            assertEquals("true", storage.getString(AuthRepository.KEY_IS_DEMO))
            assertEquals(AuthRepository.DEMO_USERNAME, storage.getString(AuthRepository.KEY_CURRENT_USERNAME))
            assertNotNull(storage.getString("access_token"))
        }

    /**
     * Verifies checkSession properly restores demo session state.
     */
    @Test
    fun testCheckSessionDemo() =
        runTest {
            val storage = MockStorage()
            val repository = AuthRepository(storage)

            repository.loginAsDemo()
            assertTrue(repository.isDemoSession.value)

            // Create new repository instance pointing to same storage
            val newRepo = AuthRepository(storage)
            val restored = newRepo.checkSession()
            assertTrue(restored)
            assertTrue(newRepo.isDemoSession.value)
            assertEquals(AuthRepository.DEMO_PRACTITIONER_ID, newRepo.currentUser.value?.id)
        }

    /**
     * Verifies checkSession with normal non-demo user sets isDemoSession to false.
     */
    @Test
    fun testCheckSessionNormalUser() =
        runTest {
            val storage = MockStorage()
            val repository = AuthRepository(storage)

            storage.save("access_token", "normal_token")
            storage.save(AuthRepository.KEY_CURRENT_USERNAME, "dr_smith")

            val restored = repository.checkSession()
            assertTrue(restored)
            assertFalse(repository.isDemoSession.value)
            assertEquals(
                "dr_smith",
                repository.currentUser.value
                    ?.name
                    ?.first()
                    ?.family
                    ?.value,
            )
        }

    /**
     * Verifies checkSession with no tokens returns false and isDemoSession is false.
     */
    @Test
    fun testCheckSessionNoTokens() =
        runTest {
            val storage = MockStorage()
            val repository = AuthRepository(storage)

            val restored = repository.checkSession()
            assertFalse(restored)
            assertFalse(repository.isDemoSession.value)
            assertNull(repository.currentUser.value)
        }

    /**
     * Verifies logout clears demo session flag and user profile.
     */
    @Test
    fun testLogoutClearsDemoSession() =
        runTest {
            val storage = MockStorage()
            val repository = AuthRepository(storage)

            repository.loginAsDemo()
            assertTrue(repository.isDemoSession.value)

            repository.logout()
            assertFalse(repository.isDemoSession.value)
            assertNull(repository.currentUser.value)
            assertNull(storage.getString(AuthRepository.KEY_IS_DEMO))
            assertNull(storage.getString(AuthRepository.KEY_CURRENT_USERNAME))
        }

    /**
     * Verifies deleteAccount also clears demo state.
     */
    @Test
    fun testDeleteAccountClearsDemoSession() =
        runTest {
            val storage = MockStorage()
            val repository = AuthRepository(storage)

            repository.loginAsDemo()
            repository.deleteAccount(AuthRepository.DEMO_USERNAME)

            assertFalse(repository.isDemoSession.value)
            assertNull(repository.currentUser.value)
        }
}
