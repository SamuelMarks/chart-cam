/**
 * @file AndroidAppInitTest.kt
 * Contains declarations for AndroidAppInitTest.kt.
 */
package io.healthplatform.chartcam

import android.content.Context
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidAppInitTest {
    @Test
    fun testInitAndGetContext() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAppContext = Mockito.mock(Context::class.java)

        Mockito.`when`(mockContext.applicationContext).thenReturn(mockAppContext)

        AndroidAppInit.init(mockContext)

        val retrievedContext = AndroidAppInit.getContext()
        assertEquals(mockAppContext, retrievedContext)
    }

    @Test
    fun testGetContextBeforeInitThrowsException() {
        // We need to reset the context to null using reflection because it's a private var
        val field = AndroidAppInit::class.java.getDeclaredField("context")
        field.isAccessible = true
        field.set(AndroidAppInit, null)

        val exception =
            assertFailsWith<IllegalStateException> {
                AndroidAppInit.getContext()
            }
        assertEquals("AndroidAppInit.init(context) must be called before using platform features.", exception.message)
    }
}
