/**
 * @file PhotoSessionManager.kt
 * Contains declarations for PhotoSessionManager.kt.
 */
package io.healthplatform.chartcam.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A manager class responsible for handling temporary photo session data during navigation.
 */
class PhotoSessionManager {
    /**
     * A private mutable state flow holding the pending photos as a map of photo IDs to file paths.
     */
    private val _pendingPhotos = MutableStateFlow<Map<String, String>>(emptyMap())

    /**
     * A public read-only state flow representing the current pending photos.
     */
    val pendingPhotos = _pendingPhotos.asStateFlow()

    /**
     * Sets the pending photos for the current session.
     *
     * @param photos A map of photo IDs to their corresponding file paths.
     */
    fun setPhotos(photos: Map<String, String>) {
        _pendingPhotos.value = photos
    }

    /**
     * Retrieves the current pending photos and clears the session state.
     *
     * @return A map of photo IDs to their file paths that were previously set.
     */
    fun getAndClear(): Map<String, String> {
        val p = _pendingPhotos.value
        _pendingPhotos.value = emptyMap()
        return p
    }

    /**
     * Retrieves the current pending photos without clearing the session state.
     *
     * @return A map of photo IDs to their file paths.
     */
    fun get(): Map<String, String> = _pendingPhotos.value
}
