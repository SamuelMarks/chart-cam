package io.healthplatform.chartcam.storage

import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Validates the JVM implementation of SecureStorage.
 * Ensures data is encrypted at rest and behaves identically to other platform implementations.
 */
class JvmSecureStorageTest {

    private val testNode = "io.healthplatform.chartcam.secure.test"
    private lateinit var storage: JvmSecureStorage

    /**
     * Initializes a fresh instance of the test storage node.
     */
    @BeforeTest
    fun setUp() {
        storage = JvmSecureStorage(testNode)
        storage.clearAll()
    }

    /**
     * Cleans up the test node after each test.
     */
    @AfterTest
    fun tearDown() {
        storage.clearAll()
    }

    /**
     * Tests saving and retrieving a valid string safely.
     */
    @Test
    fun testSaveAndRetrieve() {
        val key = "testKey"
        val value = "testValue123"

        storage.save(key, value)
        val retrieved = storage.getString(key)

        assertEquals(value, retrieved)
    }

    /**
     * Tests that values are effectively encrypted before being written to [Preferences].
     */
    @Test
    fun testEncryption() {
        val key = "testKey"
        val value = "testValue123"

        storage.save(key, value)
        
        // Ensure that what's written to Preferences is actually encrypted
        val rawPrefs = Preferences.userRoot().node(testNode)
        val rawValue = rawPrefs.get(key, null)
        
        assertNotEquals(value, rawValue)
        assertTrue(rawValue != null && rawValue.isNotEmpty())
    }

    /**
     * Tests deleting a stored key and verifying it no longer exists.
     */
    @Test
    fun testDelete() {
        val key = "testKey"
        val value = "testValue123"

        storage.save(key, value)
        storage.delete(key)

        assertNull(storage.getString(key))
    }

    /**
     * Tests fetching a non-existent key returns null.
     */
    @Test
    fun testGetNonExistentString() {
        assertNull(storage.getString("nonExistentKey"))
    }
    
    /**
     * Tests that fetching garbage/invalid Base64 data yields null and handles exceptions smoothly.
     */
    @Test
    fun testInvalidDataDecryption() {
        val key = "testKey"
        val rawPrefs = Preferences.userRoot().node(testNode)
        // Put invalid base64/encrypted data
        rawPrefs.put(key, "InvalidDataNotEncrypted")
        
        // Should return null and not crash
        assertNull(storage.getString(key))
    }
}