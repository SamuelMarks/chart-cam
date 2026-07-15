package io.healthplatform.chartcam

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field

/**
 * Tests for [AndroidAppInit].
 */
@RunWith(AndroidJUnit4::class)
class AndroidAppInitTest {
    @Before
    fun setup() {
        // Reset the singleton using reflection for isolated tests
        val contextField: Field = AndroidAppInit::class.java.getDeclaredField("context")
        contextField.isAccessible = true
        contextField.set(AndroidAppInit, null)
    }

    /**
     * Test successful context retrieval after init.
     */
    @Test
    fun testInitAndGetContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
        val retrieved = AndroidAppInit.getContext()
        assertEquals(context, retrieved)
    }

    /**
     * Test getting context before init throws exception.
     */
    @Test
    fun testGetContextBeforeInitThrows() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                AndroidAppInit.getContext()
            }
        assertTrue(exception.message!!.contains("AndroidAppInit.init(context) must be called"))
    }
}
