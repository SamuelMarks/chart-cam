/**
 * @file AndroidCameraManagerTest.kt
 * Contains declarations for AndroidCameraManagerTest.kt.
 */
package io.healthplatform.chartcam.camera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field

/**
 * Tests for [AndroidCameraManager].
 */
@RunWith(AndroidJUnit4::class)
class AndroidCameraManagerTest {
    private lateinit var context: Context
    private lateinit var manager: AndroidCameraManager

    /**
     * Setup for tests.
     */
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        manager = AndroidCameraManager(context)
    }

    /**
     * Test capture without binding returns null.
     */
    @Test
    fun testCaptureWithoutBindingReturnsNull() =
        runBlocking {
            val bytes = manager.captureImage()
            assertNull(bytes)
        }

    /**
     * Test flash toggle doesn't crash even if not bound.
     */
    @Test
    fun testSetFlash() {
        manager.setFlash(true)
        manager.setFlash(false)
    }

    /**
     * Test toggle lens changes facing.
     */
    @Test
    fun testToggleLens() {
        val lensField: Field = AndroidCameraManager::class.java.getDeclaredField("lensFacing")
        lensField.isAccessible = true

        val initial = lensField.get(manager) as Int
        assertNotNull(initial)

        manager.toggleLens()
        val next = lensField.get(manager) as Int

        manager.toggleLens()
        val restored = lensField.get(manager) as Int

        org.junit.Assert.assertNotEquals(initial, next)
        org.junit.Assert.assertEquals(initial, restored)
    }

    /**
     * Test release doesn't crash.
     */
    @Test
    fun testRelease() {
        manager.release()
    }
}
