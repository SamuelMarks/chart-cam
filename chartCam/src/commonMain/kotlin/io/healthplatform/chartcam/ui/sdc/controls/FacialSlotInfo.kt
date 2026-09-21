/**
 * @file FacialSlotInfo.kt
 * Contains declarations for FacialSlotInfo.kt.
 *
 * Data holder representing individual camera slots in a guided multi-angle series.
 */
package io.healthplatform.chartcam.ui.sdc.controls

/**
 * Data holder for individual view slots in the 3-angle series.
 *
 * @property linkId The unique FHIR item identifier for this view.
 * @property title The localized label for this view angle.
 * @property isCaptured True if an image attachment exists for this slot.
 */
data class FacialSlotInfo(
    val linkId: String,
    val title: String,
    val isCaptured: Boolean,
)
