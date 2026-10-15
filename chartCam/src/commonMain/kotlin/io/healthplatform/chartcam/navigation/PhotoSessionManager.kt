/**
 * @file PhotoSessionManager.kt
 * Contains declarations for PhotoSessionManager.kt.
 */
package io.healthplatform.chartcam.navigation

import io.healthplatform.chartcam.utils.ChartCamError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A manager class responsible for handling temporary photo session data during navigation.
 */
class PhotoSessionManager {
    /**
     * Companion object defining constants for [PhotoSessionManager].
     */
    companion object {
        /**
         * Maximum recommended photo count per session.
         */
        const val MAX_SUPPORTED_PHOTOS = 1000
    }

    /**
     * A private mutable state flow holding the pending photos as a map of photo IDs to file paths.
     */
    private val _pendingPhotos = MutableStateFlow<Map<String, String>>(emptyMap())

    /**
     * A public read-only state flow representing the current pending photos.
     */
    val pendingPhotos = _pendingPhotos.asStateFlow()

    /**
     * Sets the pending photos for the current session, validating against [MAX_SUPPORTED_PHOTOS].
     *
     * @param photos A map of photo IDs to their corresponding file paths.
     * @return A [Result] indicating success or a [ChartCamError.StateViolation] if capacity is exceeded.
     */
    fun setPhotos(photos: Map<String, String>): Result<Unit> =
        if (photos.size > MAX_SUPPORTED_PHOTOS) {
            Result.failure(
                ChartCamError.StateViolation(
                    "Photo session capacity exceeded: ${photos.size} > $MAX_SUPPORTED_PHOTOS",
                ),
            )
        } else {
            _pendingPhotos.value = photos
            Result.success(Unit)
        }

    /**
     * Atomically adds a photo to the active session, validating against [MAX_SUPPORTED_PHOTOS].
     *
     * @param id The unique identifier for the photo.
     * @param path The file path of the captured photo.
     * @return A [Result] indicating success or a [ChartCamError.StateViolation] if capacity is exceeded.
     */
    fun addPhoto(
        id: String,
        path: String,
    ): Result<Unit> {
        var limitExceeded = false
        _pendingPhotos.update { current ->
            if (!current.containsKey(id) && current.size >= MAX_SUPPORTED_PHOTOS) {
                limitExceeded = true
                current
            } else {
                current + (id to path)
            }
        }
        return if (limitExceeded) {
            Result.failure(
                ChartCamError.StateViolation(
                    "Cannot add photo '$id': maximum session limit ($MAX_SUPPORTED_PHOTOS) reached",
                ),
            )
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Atomically removes a photo from the active session by ID.
     *
     * @param id The identifier of the photo to remove.
     * @return The removed file path or null if not found.
     */
    fun removePhoto(id: String): String? {
        var removedPath: String? = null
        _pendingPhotos.update { current ->
            removedPath = current[id]
            if (current.containsKey(id)) {
                current - id
            } else {
                current
            }
        }
        return removedPath
    }

    /**
     * Clears all pending photo session references.
     */
    fun clear() {
        _pendingPhotos.value = emptyMap()
    }

    /**
     * Resets the entire photo session state by delegating to [clear].
     */
    fun reset() {
        clear()
    }

    /**
     * Retrieves a single photo file path by its ID.
     *
     * @param id The identifier of the photo.
     * @return The file path of the photo or null if not found.
     */
    fun getPhoto(id: String): String? = _pendingPhotos.value[id]

    /**
     * Returns the total count of photos in the current session.
     *
     * @return The number of stored photos.
     */
    fun photoCount(): Int = _pendingPhotos.value.size

    /**
     * Retrieves the current pending photos and clears the session state.
     *
     * @return A map of photo IDs to their file paths that were previously set.
     */
    fun getAndClear(): Map<String, String> {
        var previous: Map<String, String> = emptyMap()
        _pendingPhotos.update { current ->
            previous = current
            emptyMap()
        }
        return previous
    }

    /**
     * Retrieves the current pending photos without clearing the session state.
     *
     * @return A map of photo IDs to their file paths.
     */
    fun get(): Map<String, String> = _pendingPhotos.value
}
