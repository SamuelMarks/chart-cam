/**
 * @file PhotoSessionManagerTest.kt
 * Unit tests for PhotoSessionManager managing temporary photo session navigation state.
 */

package io.healthplatform.chartcam.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests verifying [PhotoSessionManager] functionality including addition, removal,
 * clearing, retrieval, and state flow synchronization.
 */
class PhotoSessionManagerTest {
    /**
     * Verifies constant definition and initial empty state.
     */
    @Test
    fun testInitialStateAndConstants() {
        val manager = PhotoSessionManager()
        assertEquals(1000, PhotoSessionManager.MAX_SUPPORTED_PHOTOS)
        assertEquals(0, manager.photoCount())
        assertTrue(manager.get().isEmpty())
        assertTrue(manager.pendingPhotos.value.isEmpty())
    }

    /**
     * Verifies adding, retrieving, and setting multiple photos.
     */
    @Test
    fun testAddGetAndSetPhotos() {
        val manager = PhotoSessionManager()

        manager.addPhoto("p1", "/path/1.jpg")
        assertEquals(1, manager.photoCount())
        assertEquals("/path/1.jpg", manager.getPhoto("p1"))
        assertNull(manager.getPhoto("non-existent"))
        assertEquals(mapOf("p1" to "/path/1.jpg"), manager.get())

        manager.addPhoto("p2", "/path/2.jpg")
        assertEquals(2, manager.photoCount())
        assertEquals("/path/2.jpg", manager.getPhoto("p2"))

        val bulkPhotos = mapOf("a" to "/a.jpg", "b" to "/b.jpg", "c" to "/c.jpg")
        manager.setPhotos(bulkPhotos)
        assertEquals(3, manager.photoCount())
        assertEquals(bulkPhotos, manager.get())
        assertEquals(bulkPhotos, manager.pendingPhotos.value)
    }

    /**
     * Verifies removing photo by key for both existing and missing keys.
     */
    @Test
    fun testRemovePhoto() {
        val manager = PhotoSessionManager()
        manager.setPhotos(mapOf("p1" to "/path/1.jpg", "p2" to "/path/2.jpg"))

        // Remove existing
        val removed = manager.removePhoto("p1")
        assertEquals("/path/1.jpg", removed)
        assertEquals(1, manager.photoCount())
        assertNull(manager.getPhoto("p1"))

        // Remove non-existing key
        val nonExisting = manager.removePhoto("p3")
        assertNull(nonExisting)
        assertEquals(1, manager.photoCount())
        assertEquals("/path/2.jpg", manager.getPhoto("p2"))
    }

    /**
     * Verifies clear, reset, and getAndClear lifecycle methods.
     */
    @Test
    fun testClearResetAndGetAndClear() {
        val manager = PhotoSessionManager()
        manager.setPhotos(mapOf("p1" to "/path/1.jpg"))

        val extracted = manager.getAndClear()
        assertEquals(mapOf("p1" to "/path/1.jpg"), extracted)
        assertTrue(manager.get().isEmpty())
        assertEquals(0, manager.photoCount())

        manager.addPhoto("p2", "/path/2.jpg")
        assertEquals(1, manager.photoCount())
        manager.clear()
        assertTrue(manager.get().isEmpty())

        manager.addPhoto("p3", "/path/3.jpg")
        assertEquals(1, manager.photoCount())
        manager.reset()
        assertTrue(manager.get().isEmpty())
    }
}
