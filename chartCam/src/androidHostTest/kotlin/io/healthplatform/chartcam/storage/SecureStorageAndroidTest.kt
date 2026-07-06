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

@RunWith(RobolectricTestRunner::class)
class SecureStorageAndroidTest {
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    @After
    fun teardown() {
        val field = AndroidAppInit::class.java.getDeclaredField("context")
        field.isAccessible = true
        field.set(AndroidAppInit, null)
    }

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
