/**
 * @file MainActivityTest.kt
 * Unit tests for MainActivity configuration and state on Android.
 */
package io.healthplatform.chartcam.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests verifying MainActivity companion flags and configuration.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    /**
     * Tests forceLightMode toggle flag in MainActivity companion object.
     */
    @Test
    fun testForceLightModeToggle() {
        val initial = MainActivity.forceLightMode
        MainActivity.forceLightMode = true
        assertTrue(MainActivity.forceLightMode)

        MainActivity.forceLightMode = false
        assertFalse(MainActivity.forceLightMode)

        MainActivity.forceLightMode = initial

        val activity = MainActivity()
        assertNotNull(activity)
    }
}
