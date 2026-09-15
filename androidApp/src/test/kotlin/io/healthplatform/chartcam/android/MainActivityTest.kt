/**
 * @file MainActivityTest.kt
 * Unit tests for MainActivity configuration and state on Android.
 */
package io.healthplatform.chartcam.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying MainActivity companion flags and configuration.
 */
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
    }
}
