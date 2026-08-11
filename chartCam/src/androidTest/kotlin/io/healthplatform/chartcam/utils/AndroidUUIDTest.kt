/**
 * @file AndroidUUIDTest.kt
 * Contains declarations for AndroidUUIDTest.kt.
 */
package io.healthplatform.chartcam.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the Android UUID utilities.
 */
@RunWith(AndroidJUnit4::class)
class AndroidUUIDTest {
    /**
     * Tests UUID random generation.
     */
    @Test
    fun testRandomUUID() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()

        assertTrue(uuid1.isNotEmpty())
        assertNotEquals(uuid1, uuid2)
    }
}
