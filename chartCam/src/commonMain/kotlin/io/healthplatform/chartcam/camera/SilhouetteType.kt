/**
 * @file SilhouetteType.kt
 * Contains declarations for SilhouetteType.kt.
 *
 * Defines the supported clinical silhouette overlays and guidance state for the camera viewport.
 */
package io.healthplatform.chartcam.camera

/**
 * Supported clinical photography silhouette guidance types.
 * Used to project on-screen vector guidance overlays during patient image capture.
 *
 * @property code The FHIR extension code identifier for this silhouette preset.
 */
enum class SilhouetteType(
    val code: String,
) {
    /** No silhouette guidance displayed. */
    NONE("none"),

    /** Left profile view highlighting the corneal apex tangent and nasal tip box. */
    PROFILE_CORNEA_NOSE_LEFT("profile-cornea-left"),

    /** Front-on facial view with bipupillary horizontal line and vertical facial axis. */
    FRONTAL_FACE("frontal-face"),

    /** Right profile view highlighting the corneal apex tangent and nasal tip box (horizontally mirrored). */
    PROFILE_CORNEA_NOSE_RIGHT("profile-cornea-right"),
    ;

    /** Companion object for resolving silhouette types from code strings. */
    companion object {
        /**
         * Resolves a [SilhouetteType] from its FHIR extension code string.
         *
         * @param code The silhouette code from the FHIR camera-silhouette extension.
         * @return A [Result] containing the matching [SilhouetteType] or a failure if unsupported.
         */
        fun fromCode(code: String?): Result<SilhouetteType> {
            if (code == null || code.isBlank() || code == "none") {
                return Result.success(NONE)
            }
            val match = entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
            return if (match != null) {
                Result.success(match)
            } else {
                Result.failure(IllegalArgumentException("Unsupported silhouette code: $code"))
            }
        }
    }
}

/**
 * Represents the real-time guidance state for a silhouette overlay.
 *
 * @property scale The current zoom or scale factor applied to the silhouette.
 * @property isLevel True if the device is within pitch and roll level tolerance.
 * @property pitch Device pitch in degrees.
 * @property roll Device roll in degrees.
 * @property hasPreviousGhost True if a ghost image from a preceding profile step is available for onion-skinning.
 */
data class SilhouetteGuideState(
    val scale: Float = 1.0f,
    val isLevel: Boolean = false,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val hasPreviousGhost: Boolean = false,
)
