/**
 * @file SecureStorageAndroidTest.kt
 * Contains declarations for SecureStorageAndroidTest.kt.
 */
package io.healthplatform.chartcam.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Android host tests for SecureStorage.
 */
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class SecureStorageAndroidTest {
    /**
     * Setup for tests.
     */
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    /**
     * Teardown for tests.
     */
    @After
    fun teardown() {
        val field = AndroidAppInit::class.java.getDeclaredField("context")
        field.isAccessible = true
        field.set(AndroidAppInit, null)
    }

    /**
     * Tests basic save, get, and delete operations on SecureStorage.
     */
    @Test
    fun testSecureStorageSaveGetDelete() {
        val storage = createSecureStorage()

        // Ensure initially null
        assertNull(storage.getString("myKey"))

        // Save
        storage.save("myKey", "myValue")

        // Get
        val retrieved = storage.getString("myKey")
        assertEquals("myValue", retrieved)

        // Delete
        storage.delete("myKey")

        // Ensure deleted
        assertNull(storage.getString("myKey"))
    }

    /**
     * Tests graceful recovery when stored data is malformed base64.
     */
    @Test
    fun testCorruptedBase64Handling() {
        val context = AndroidAppInit.getContext()
        val prefs = context.getSharedPreferences("secure_prefs_v2", Context.MODE_PRIVATE)
        prefs.edit().putString("corrupt_key", "not_valid_base64!@#$").apply()

        val storage = createSecureStorage()
        assertNull(storage.getString("corrupt_key"))
    }

    /**
     * Tests graceful recovery when stored ciphertext fails authentication or decryption.
     */
    @Test
    fun testCorruptedCiphertextHandling() {
        val context = AndroidAppInit.getContext()
        val prefs = context.getSharedPreferences("secure_prefs_v2", Context.MODE_PRIVATE)
        // Valid base64, but invalid AES-GCM ciphertext
        val invalidCiphertextBase64 =
            android.util.Base64.encodeToString(
                byteArrayOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 99),
                android.util.Base64.DEFAULT,
            )
        prefs.edit().putString("bad_cipher", invalidCiphertextBase64).apply()

        val storage = createSecureStorage()
        assertNull(storage.getString("bad_cipher"))
    }
}
