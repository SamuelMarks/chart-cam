/**
 * @file SdcExtensions.kt
 * Contains declarations for SdcExtensions.kt.
 */
package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.ui.currentLanguageState

/**
 * Structured Data Capture (SDC) Extension definitions and helpers.
 * https://hl7.org/fhir/uv/sdc/
 */
object SdcExtensions {
    /** The itemControl extension URL. */
    const val ITEM_CONTROL = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"

    /** The minValue extension URL. */
    const val MIN_VALUE = "http://hl7.org/fhir/StructureDefinition/minValue"

    /** The maxValue extension URL. */
    const val MAX_VALUE = "http://hl7.org/fhir/StructureDefinition/maxValue"

    /** The hidden extension URL. */
    const val HIDDEN = "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden"

    /** The calculatedExpression extension URL. */
    const val CALCULATED_EXPRESSION =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression"

    /** The initialExpression extension URL. */
    const val INITIAL_EXPRESSION =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"

    /** The translation extension URL. */
    const val TRANSLATION = "http://hl7.org/fhir/StructureDefinition/translation"

    /** Item control code for pain VAS / Wong-Baker faces slider. */
    const val ITEM_CONTROL_PAIN_VAS = "pain-vas"

    /** Item control code for color / phototyping palette. */
    const val ITEM_CONTROL_PALETTE = "palette"

    /** Item control code for anatomical body map pin drop. */
    const val ITEM_CONTROL_BODY_MAP = "body-map"

    /** Item control code for segmented visual choice control. */
    const val ITEM_CONTROL_SEGMENTED_CONTROL = "segmented-control"

    /** LOINC code for pain severity score. */
    const val LOINC_PAIN_SEVERITY = "72514-3"

    /** Alternative LOINC code for pain score. */
    const val LOINC_PAIN_SCORE = "38208-5"

    /** Extension URL for coordinate coordinates (x, y percentages) on body map. */
    const val BODY_MAP_COORDINATES =
        "http://chartcam.local/fhir/StructureDefinition/body-map-coordinates"
}

/**
 * Checks if the Questionnaire Item is configured as a visual pain scale control.
 *
 * @return True if itemControl is pain-vas or slider with a pain LOINC code.
 */
fun Questionnaire.Item.isVisualPainControl(): Boolean {
    val control = getItemControl()
    if (control == SdcExtensions.ITEM_CONTROL_PAIN_VAS || control == "wong-baker") {
        return true
    }
    val codeMatch =
        code.any { coding ->
            val c = coding.code?.value
            c == SdcExtensions.LOINC_PAIN_SEVERITY || c == SdcExtensions.LOINC_PAIN_SCORE
        }
    return (control == "slider" || control == null) && codeMatch
}

/**
 * Checks if the Questionnaire Item is configured as a Fitzpatrick / color palette control.
 *
 * @return True if itemControl is palette or fitzpatrick.
 */
fun Questionnaire.Item.isFitzpatrickPalette(): Boolean {
    val control = getItemControl()
    return control in listOf(SdcExtensions.ITEM_CONTROL_PALETTE, "color-palette", "fitzpatrick")
}

/**
 * Checks if the Questionnaire Item is configured as an anatomical body map control.
 *
 * @return True if itemControl is body-map.
 */
fun Questionnaire.Item.isBodyMap(): Boolean {
    val control = getItemControl()
    return control == SdcExtensions.ITEM_CONTROL_BODY_MAP
}

/**
 * Checks if the Questionnaire Item is configured as a segmented control / choice tiles.
 *
 * @return True if itemControl is segmented-control or choice-cards.
 */
fun Questionnaire.Item.isSegmentedControl(): Boolean {
    val control = getItemControl()
    return control in listOf(SdcExtensions.ITEM_CONTROL_SEGMENTED_CONTROL, "choice-cards")
}

/**
 * Checks if a Questionnaire Item is hidden based on the SDC hidden extension.
 * @return True if hidden, false otherwise.
 */
fun Questionnaire.Item.isHidden(): Boolean {
    val hiddenExt = this.extension.firstOrNull { it.url == SdcExtensions.HIDDEN }
    return hiddenExt
        ?.value
        ?.asBoolean()
        ?.value
        ?.value == true
}

/**
 * Retrieves the initialExpression extension expression from an item.
 * @return The initial expression string or null.
 */
fun Questionnaire.Item.getInitialExpression(): String? {
    val initExt = this.extension.firstOrNull { it.url == SdcExtensions.INITIAL_EXPRESSION } ?: return null
    val exprExt = initExt.extension.firstOrNull { it.url == "expression" }
    return exprExt
        ?.value
        ?.asString()
        ?.value
        ?.value
        ?: initExt.value
            ?.asString()
            ?.value
            ?.value
}

/**
 * Retrieves the ItemControl code from the item, or null if not present.
 * @return The item control code, or null.
 */
fun Questionnaire.Item.getItemControl(): String? =
    this.extension
        .firstOrNull { it.url == SdcExtensions.ITEM_CONTROL }
        ?.value
        ?.asCodeableConcept()
        ?.value
        ?.coding
        ?.firstOrNull()
        ?.code
        ?.value

/**
 * Retrieves the minimum value extension for numeric inputs.
 * @return The minimum value, or null.
 */
fun Questionnaire.Item.getMinValue(): Float? =
    this.extension
        .firstOrNull { it.url == SdcExtensions.MIN_VALUE }
        ?.value
        ?.asInteger()
        ?.value
        ?.value
        ?.toFloat()

/**
 * Retrieves the maximum value extension for numeric inputs.
 * @return The maximum value, or null.
 */
fun Questionnaire.Item.getMaxValue(): Float? =
    this.extension
        .firstOrNull { it.url == SdcExtensions.MAX_VALUE }
        ?.value
        ?.asInteger()
        ?.value
        ?.value
        ?.toFloat()

/**
 * Retrieves the localized text for a questionnaire item using the FHIR translation extension.
 *
 * @param language The language tag to search for (e.g. "es", "ja", "he", "zh").
 * @return The localized text string if present, or the item's default text.
 */
fun Questionnaire.Item.getLocalizedText(language: String = currentLanguageState.value): String {
    val langPrefix = language.lowercase().split("-", "_").first()
    val transExt = this.extension.filter { it.url == SdcExtensions.TRANSLATION }
    for (ext in transExt) {
        val langExt = ext.extension.firstOrNull { it.url == "lang" }
        val langCode =
            langExt
                ?.value
                ?.asCode()
                ?.value
                ?.value
                ?: langExt
                    ?.value
                    ?.asString()
                    ?.value
                    ?.value
        if (langCode?.lowercase()?.startsWith(langPrefix) == true) {
            val contentExt = ext.extension.firstOrNull { it.url == "content" }
            val content =
                contentExt
                    ?.value
                    ?.asString()
                    ?.value
                    ?.value
            if (!content.isNullOrBlank()) {
                return content
            }
        }
    }
    return this.text?.value ?: ""
}

/**
 * Retrieves the localized title for a questionnaire using the FHIR translation extension.
 *
 * @param language The language tag to search for (e.g. "es", "ja", "he", "zh").
 * @return The localized title string if present, or the questionnaire's default title, or empty string.
 */
fun Questionnaire.getLocalizedTitle(language: String = currentLanguageState.value): String {
    val langPrefix = language.lowercase().split("-", "_").first()
    val directTransExt = this.extension.filter { it.url == SdcExtensions.TRANSLATION }
    val titleTransExt = this.title?.extension?.filter { it.url == SdcExtensions.TRANSLATION } ?: emptyList()
    val allTransExt = titleTransExt + directTransExt

    for (ext in allTransExt) {
        val langExt = ext.extension.firstOrNull { it.url == "lang" }
        val langCode =
            langExt
                ?.value
                ?.asCode()
                ?.value
                ?.value
                ?: langExt
                    ?.value
                    ?.asString()
                    ?.value
                    ?.value
        if (langCode?.lowercase()?.startsWith(langPrefix) == true) {
            val contentExt = ext.extension.firstOrNull { it.url == "content" }
            val content =
                contentExt
                    ?.value
                    ?.asString()
                    ?.value
                    ?.value
            if (!content.isNullOrBlank()) {
                return content
            }
        }
    }
    return this.title?.value ?: ""
}
