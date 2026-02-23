package io.healthplatform.chartcam.storage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.browser.localStorage

/**
 * Validates the JS implementation of SecureStorage using Obfuscation.
 */
class JsSecureStorageTest {

    private val storage = JsSecureStorage()
    private val key = "testKey"
    private val value = "testValue123"

    /**
     * Clears local storage before each test.
     */
    @BeforeTest
    fun setUp() {
        localStorage.clear()
    }

    /**
     * Clears local storage after each test.
     */
    @AfterTest
    fun tearDown() {
        localStorage.clear()
    }

    /**
     * Verifies that saving and retrieving a valid string works correctly.
     */
    @Test
    fun testSaveAndRetrieve() {
        storage.save(key, value)
        val retrieved = storage.getString(key)

        assertEquals(value, retrieved)
    }

    /**
     * Verifies that the string stored in [localStorage] is properly obfuscated
     * and not equal to the plain text value.
     */
    @Test
    fun testEncryption() {
        storage.save(key, value)
        
        // Ensure that what's written to localStorage is actually obfuscated
        val rawValue = localStorage.getItem(key)
        
        assertNotEquals(value, rawValue)
        assertEquals(true, rawValue != null && rawValue.isNotEmpty())
    }

    /**
     * Verifies that deleting a value correctly removes it from [localStorage].
     */
    @Test
    fun testDelete() {
        storage.save(key, value)
        storage.delete(key)

        assertNull(storage.getString(key))
    }

    /**
     * Verifies that fetching a non-existent key returns null.
     */
    @Test
    fun testGetNonExistentString() {
        assertNull(storage.getString("nonExistentKey"))
    }
    
    /**
     * Verifies that fallback mechanism handles invalid decoding.
     */
    @Test
    fun testInvalidDataDecryption() {
        // Put invalid base64 data directly
        localStorage.setItem(key, "InvalidDataNotBase64!!@@")
        
        // Should return the string itself as fallback
        assertEquals("InvalidDataNotBase64!!@@", storage.getString(key))
    }
}
