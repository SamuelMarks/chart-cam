package io.healthplatform.chartcam.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [AndroidSecureStorage].
 */
@RunWith(AndroidJUnit4::class)
class AndroidSecureStorageTest {
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    /**
     * Test create secure storage.
     */
    @Test
    fun testCreateSecureStorage() {
        val storage = createSecureStorage()
        assertTrue(storage is AndroidSecureStorage)
    }

    /**
     * Test save, get, and delete.
     */
    @Test
    fun testSaveGetDelete() {
        val storage = createSecureStorage()
        val key = "test_key"
        val value = "test_value"

        storage.save(key, value)
        val retrieved = storage.getString(key)
        assertEquals(value, retrieved)

        storage.delete(key)
        val retrievedAfterDelete = storage.getString(key)
        assertNull(retrievedAfterDelete)
    }
}
