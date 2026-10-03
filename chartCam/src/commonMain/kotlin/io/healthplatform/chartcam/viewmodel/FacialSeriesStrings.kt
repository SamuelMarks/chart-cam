/**
 * @file FacialSeriesStrings.kt
 * Contains declarations for FacialSeriesStrings.kt.
 */
package io.healthplatform.chartcam.viewmodel

/**
 * Encapsulates the localized strings required for the Facial Profile Series widget.
 *
 * @property leftProfile The localized text for the Left Profile.
 * @property frontView The localized text for the Front View.
 * @property rightProfile The localized text for the Right Profile.
 */
data class FacialSeriesStrings(
    val leftProfile: String = "Left Profile (Cornea & Nose)",
    val frontView: String = "Front View",
    val rightProfile: String = "Right Profile (Cornea & Nose)",
)
