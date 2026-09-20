/**
 * @file TestActivityTest.kt
 * Contains declarations for TestActivityTest.kt.
 */
package io.healthplatform.chartcam

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [TestActivity].
 */
@RunWith(RobolectricTestRunner::class)
class TestActivityTest {
    /**
     * Verifies that TestActivity can be instantiated.
     */
    @Test
    fun testTestActivityInstantiation() {
        val activity = TestActivity()
        assertNotNull(activity)
    }
}
