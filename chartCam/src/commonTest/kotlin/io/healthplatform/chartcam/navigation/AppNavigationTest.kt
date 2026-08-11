/**
 * @file AppNavigationTest.kt
 * Contains declarations for AppNavigationTest.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for Application Navigation utilities.
 */
class AppNavigationTest {
    /**
     * Tests the behavior of the [PhotoSessionManager].
     */
    @Test
    fun testPhotoSessionManager() {
        val manager = PhotoSessionManager()
        val photos = mapOf("id1" to "path1", "id2" to "path2")

        manager.setPhotos(photos)

        val retrieved = manager.get()
        assertEquals(2, retrieved.size)
        assertEquals("path1", retrieved["id1"])

        val cleared = manager.getAndClear()
        assertEquals(2, cleared.size)

        val empty = manager.get()
        assertTrue(empty.isEmpty())
    }
}
