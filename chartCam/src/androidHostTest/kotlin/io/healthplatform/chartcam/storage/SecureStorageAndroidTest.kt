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
}
