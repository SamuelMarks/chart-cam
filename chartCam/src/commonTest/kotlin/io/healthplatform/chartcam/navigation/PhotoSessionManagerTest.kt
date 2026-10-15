/**
 * @file PhotoSessionManagerTest.kt
 * Unit tests for PhotoSessionManager managing temporary photo session navigation state.
 */

package io.healthplatform.chartcam.navigation

import io.healthplatform.chartcam.utils.ChartCamError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests verifying [PhotoSessionManager] functionality including addition, removal,
 * clearing, capacity enforcement, retrieval, and state flow synchronization.
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
     * Verifies adding, retrieving, and setting multiple photos as well as capacity limits.
     */
    @Test
    fun testAddGetAndSetPhotos() {
        val manager = PhotoSessionManager()

        assertTrue(manager.addPhoto("p1", "/path/1.jpg").isSuccess)
        assertEquals(1, manager.photoCount())
        assertEquals("/path/1.jpg", manager.getPhoto("p1"))
        assertNull(manager.getPhoto("non-existent"))
        assertEquals(mapOf("p1" to "/path/1.jpg"), manager.get())

        assertTrue(manager.addPhoto("p2", "/path/2.jpg").isSuccess)
        assertEquals(2, manager.photoCount())
        assertEquals("/path/2.jpg", manager.getPhoto("p2"))

        val bulkPhotos = mapOf("a" to "/a.jpg", "b" to "/b.jpg", "c" to "/c.jpg")
        assertTrue(manager.setPhotos(bulkPhotos).isSuccess)
        assertEquals(3, manager.photoCount())
        assertEquals(bulkPhotos, manager.get())
        assertEquals(bulkPhotos, manager.pendingPhotos.value)

        val oversized = (1..1001).associate { "id_$it" to "/path/$it.jpg" }
        val setOversizedResult = manager.setPhotos(oversized)
        assertTrue(setOversizedResult.isFailure)
        assertTrue(setOversizedResult.exceptionOrNull() is ChartCamError.StateViolation)

        val atLimit = (1..1000).associate { "id_$it" to "/path/$it.jpg" }
        assertTrue(manager.setPhotos(atLimit).isSuccess)
        assertTrue(manager.addPhoto("id_1", "/path/updated.jpg").isSuccess)
        val addOverLimitResult = manager.addPhoto("id_1001", "/path/1001.jpg")
        assertTrue(addOverLimitResult.isFailure)
        assertTrue(addOverLimitResult.exceptionOrNull() is ChartCamError.StateViolation)
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
