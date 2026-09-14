/**
 * @file VisualFormModels.kt
 * Contains declarations for VisualFormModels.kt.
 *
 * Defines domain models and structures for visual form controls in FHIR SDC questionnaires.
 */
package io.healthplatform.chartcam.models

/**
 * Represents a pinned anatomical region on the body map.
 *
 * @property regionId The unique identifier of the anatomical region.
 * @property displayName The user-facing display name of the anatomical region.
 * @property snomedCode The SNOMED CT concept code for this anatomical site, or null.
 * @property xPercent The horizontal relative coordinate (0.0 to 100.0).
 * @property yPercent The vertical relative coordinate (0.0 to 100.0).
 */
data class BodyMapLocation(
    val regionId: String,
    val displayName: String,
    val snomedCode: String? = null,
    val xPercent: Float = 50f,
    val yPercent: Float = 50f,
) {
    /**
     * Serializes this pinned location into a standardized string representation.
     *
     * @return Formatted string containing site name, SNOMED code, and coordinate percentages.
     */
    fun toSerializedString(): String {
        val snomedPart = if (snomedCode != null) " [$snomedCode]" else ""
        return "$displayName$snomedPart (${xPercent.toInt()}%, ${yPercent.toInt()}%)"
    }
}

/**
 * Represents a Fitzpatrick skin phototype classification swatch.
 *
 * @property type The numerical rating from 1 to 6.
 * @property romanNumeral The Roman numeral identifier (I through VI).
 * @property hexColor The representative hexadecimal RGB color code.
 * @property titleKey The resource key for the phototype title.
 * @property descriptionKey The resource key for the phototype clinical description.
 */
data class FitzpatrickSkinType(
    val type: Int,
    val romanNumeral: String,
    val hexColor: String,
    val titleKey: String,
    val descriptionKey: String,
)

/**
 * Standard predefined Fitzpatrick scale definitions (Types I through VI).
 */
object FitzpatrickScaleDefaults {
    /**
     * Default list of the six canonical Fitzpatrick phototypes.
     */
    val ALL_TYPES: List<FitzpatrickSkinType> =
        listOf(
            FitzpatrickSkinType(1, "I", "#F8D9C8", "fitzpatrick_type_1", "fitzpatrick_desc_1"),
            FitzpatrickSkinType(2, "II", "#E6B89C", "fitzpatrick_type_2", "fitzpatrick_desc_2"),
            FitzpatrickSkinType(3, "III", "#D49B72", "fitzpatrick_type_3", "fitzpatrick_desc_3"),
            FitzpatrickSkinType(4, "IV", "#B87342", "fitzpatrick_type_4", "fitzpatrick_desc_4"),
            FitzpatrickSkinType(5, "V", "#8C4824", "fitzpatrick_type_5", "fitzpatrick_desc_5"),
            FitzpatrickSkinType(6, "VI", "#3D2214", "fitzpatrick_type_6", "fitzpatrick_desc_6"),
        )
}

/**
 * Represents a point on the Wong-Baker FACES or Visual Analogue Scale (VAS).
 *
 * @property score The integer severity score (0 to 10).
 * @property hexColor The representative color for this pain severity tier.
 * @property labelKey The resource key for the localized descriptive severity label.
 */
data class PainScaleLevel(
    val score: Int,
    val hexColor: String,
    val labelKey: String,
)

/**
 * Standard predefined Wong-Baker / VAS severity levels.
 */
object PainScaleDefaults {
    /**
     * Predefined scale points for Wong-Baker FACES (0, 2, 4, 6, 8, 10).
     */
    val LEVELS: List<PainScaleLevel> =
        listOf(
            PainScaleLevel(0, "#4CAF50", "pain_level_0"),
            PainScaleLevel(2, "#8BC34A", "pain_level_2"),
            PainScaleLevel(4, "#FFC107", "pain_level_4"),
            PainScaleLevel(6, "#FF9800", "pain_level_6"),
            PainScaleLevel(8, "#FF5722", "pain_level_8"),
            PainScaleLevel(10, "#F44336", "pain_level_10"),
        )
}
