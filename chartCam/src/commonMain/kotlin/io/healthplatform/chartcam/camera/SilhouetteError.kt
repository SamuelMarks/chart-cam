/**
 * @file SilhouetteError.kt
 * Contains declarations for SilhouetteError.kt.
 *
 * Defines structured, type-safe errors that can occur during silhouette positioning,
 * scale calibration, and silhouette geometry resolution.
 */
package io.healthplatform.chartcam.camera

/**
 * Represents structured errors encountered in the on-screen silhouette and clinical guidance subsystem.
 */
sealed interface SilhouetteError {
    /**
     * Corneal apex is occluded or not detected within the profile frame.
     *
     * @property detail Human-readable explanation of why the cornea could not be resolved.
     */
    data class CorneaOccluded(
        val detail: String,
    ) : SilhouetteError

    /**
     * The scale or working distance of the patient is outside clinical tolerance.
     *
     * @property scaleFactor The computed scale factor that violated acceptable boundaries.
     */
    data class InvalidScale(
        val scaleFactor: Float,
    ) : SilhouetteError

    /**
     * Sensor or spatial calibration failed to establish the leveling baseline.
     *
     * @property reason Description of why calibration failed.
     */
    data class CalibrationFailed(
        val reason: String,
    ) : SilhouetteError

    /**
     * The requested silhouette code or shape identifier is not supported by the engine.
     *
     * @property code The unsupported silhouette identifier.
     */
    data class SilhouetteNotSupported(
        val code: String,
    ) : SilhouetteError
}

/**
 * Maps any [Throwable] to a strongly-typed [SilhouetteError].
 *
 * @receiver The throwable to map.
 * @return The corresponding [SilhouetteError].
 */
fun Throwable.toSilhouetteError(): SilhouetteError {
    val message = this.message ?: "Unknown error"
    return when {
        message.contains("cornea", ignoreCase = true) -> SilhouetteError.CorneaOccluded(message)
        message.contains("scale", ignoreCase = true) -> SilhouetteError.InvalidScale(0f)
        message.contains("calibration", ignoreCase = true) -> SilhouetteError.CalibrationFailed(message)
        else -> SilhouetteError.SilhouetteNotSupported(message)
    }
}
