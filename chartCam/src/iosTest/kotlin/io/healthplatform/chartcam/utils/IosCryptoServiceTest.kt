/**
 * @file IosCryptoServiceTest.kt
 * Contains declarations for IosCryptoServiceTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Validates Argon2 key derivation directly on iOS.
 */
class IosCryptoServiceTest {
    /**
     * Verifies Argon2 key derivation produces the expected 32-byte key.
     */
    @Test
    fun testArgon2KeyDerivation() =
        runTest {
            val service = CryptoService()
            val salt = ByteArray(16) { it.toByte() }
            val key = service.deriveKeyArgon2("doctor_pass", salt)
            assertEquals(32, key.size, "Argon2 key should be exactly 32 bytes")
        }
}

/**
 * Tests for iOS date formatting utilities.
 */
class IosDateFormatterTest {
    /**
     * Verifies date formatting does not crash on iOS.
     */
    @Test
    fun testDateFormattingOnIos() {
        val dateStr = formatLocalizedDate("2026-09-16", "en")
        assertTrue(dateStr.isNotEmpty(), "Formatted date string must not be empty")

        val dateTimeStr = formatLocalizedDateTime("2026-09-16T14:30:00Z", "en")
        assertTrue(dateTimeStr.isNotEmpty(), "Formatted datetime string must not be empty")
    }
}

/**
 * Tests for iOS ShareService factory and behaviors.
 */
class IosShareServiceTest {
    /**
     * Verifies ShareService creation.
     */
    @Test
    fun testShareServiceCreation() {
        val shareService = createShareService()
        assertNotNull(shareService, "IosShareService must be successfully created")
    }
}

/**
 * Checks iOS UUID generation.
 */
class IosUUIDTest {
    /**
     * Verifies randomUUID generates non-empty distinct UUID strings.
     */
    @Test
    fun testUUIDGeneration() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        assertTrue(uuid1.isNotEmpty(), "UUID must not be empty")
        assertTrue(uuid2.isNotEmpty(), "UUID must not be empty")
        assertTrue(uuid1 != uuid2, "UUIDs must be distinct")
    }
}
