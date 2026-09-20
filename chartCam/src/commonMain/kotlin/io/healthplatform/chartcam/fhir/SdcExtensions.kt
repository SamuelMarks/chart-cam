/**
 * @file SdcExtensions.kt
 * Contains declarations for SdcExtensions.kt.
 */
package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.Questionnaire
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

    /** The questionnaire unit extension URL. */
    const val QUESTIONNAIRE_UNIT = "http://hl7.org/fhir/StructureDefinition/questionnaire-unit"

    /** The sdc itemWeight extension URL. */
    const val ITEM_WEIGHT = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemWeight"

    /** The choiceOrientation extension URL. */
    const val CHOICE_ORIENTATION = "http://hl7.org/fhir/StructureDefinition/questionnaire-choiceOrientation"

    /** SDC observation extract extension URL. */
    const val OBSERVATION_EXTRACT =
        "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-observationExtract"
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
 * Safe accessor for item control returning a [Result].
 */
val Questionnaire.Item.safeItemControl: Result<String?>
    get() = runCatching { getItemControl() }

/**
 * Safe accessor for min value returning a [Result].
 */
val Questionnaire.Item.safeMinValue: Result<dev.ohs.fhir.model.r4.FhirDecimal?>
    get() =
        runCatching {
            val ext = this.extension.firstOrNull { it.url == SdcExtensions.MIN_VALUE } ?: return@runCatching null
            when (val v = ext.value) {
                is Extension.Value.Decimal -> v.value.value
                is Extension.Value.Integer ->
                    v.value.value?.let {
                        dev.ohs.fhir.model.r4.FhirDecimal
                            .fromInt(it)
                    }
                else -> null
            }
        }

/**
 * Safe accessor for max value returning a [Result].
 */
val Questionnaire.Item.safeMaxValue: Result<dev.ohs.fhir.model.r4.FhirDecimal?>
    get() =
        runCatching {
            val ext = this.extension.firstOrNull { it.url == SdcExtensions.MAX_VALUE } ?: return@runCatching null
            when (val v = ext.value) {
                is Extension.Value.Decimal -> v.value.value
                is Extension.Value.Integer ->
                    v.value.value?.let {
                        dev.ohs.fhir.model.r4.FhirDecimal
                            .fromInt(it)
                    }
                else -> null
            }
        }

/**
 * Safe accessor for initial expression returning a [Result].
 */
val Questionnaire.Item.safeInitialExpression: Result<String?>
    get() = runCatching { getInitialExpression() }

/**
 * Safe accessor for questionnaire-unit returning a [Result].
 */
val Questionnaire.Item.safeUnit: Result<dev.ohs.fhir.model.r4.Coding?>
    get() =
        runCatching {
            val ext =
                this.extension.firstOrNull {
                    it.url == SdcExtensions.QUESTIONNAIRE_UNIT
                } ?: return@runCatching null
            when (val v = ext.value) {
                is Extension.Value.Coding -> v.value
                else -> null
            }
        }

/**
 * Safe accessor for sdc-questionnaire-itemWeight returning a [Result].
 */
val Questionnaire.Item.safeItemWeight: Result<dev.ohs.fhir.model.r4.FhirDecimal?>
    get() =
        runCatching {
            val ext =
                this.extension.firstOrNull {
                    it.url == SdcExtensions.ITEM_WEIGHT
                } ?: return@runCatching null
            when (val v = ext.value) {
                is Extension.Value.Decimal -> v.value.value
                else -> null
            }
        }

/**
 * Safe accessor for questionnaire-choiceOrientation returning a [Result].
 */
val Questionnaire.Item.safeChoiceOrientation: Result<String?>
    get() =
        runCatching {
            val ext =
                this.extension.firstOrNull {
                    it.url == SdcExtensions.CHOICE_ORIENTATION
                } ?: return@runCatching null
            when (val v = ext.value) {
                is Extension.Value.Code -> v.value.value
                is Extension.Value.String -> v.value.value
                else -> null
            }
        }

/**
 * Checks if a Questionnaire Item is hidden based on the SDC hidden extension.
 * @return True if hidden, false otherwise.
 */
fun Questionnaire.Item.isHidden(): Boolean {
    val hiddenExt = this.extension.firstOrNull { it.url == SdcExtensions.HIDDEN }
    return when (val v = hiddenExt?.value) {
        is Extension.Value.Boolean -> v.value.value == true
        else -> false
    }
}

/**
 * Retrieves the initialExpression extension expression from an item.
 * @return The initial expression string or null.
 */
fun Questionnaire.Item.getInitialExpression(): String? {
    val initExt =
        this.extension.firstOrNull {
            it.url == SdcExtensions.INITIAL_EXPRESSION
        } ?: return null
    val exprExt = initExt.extension.firstOrNull { it.url == "expression" }
    val exprVal = exprExt?.value
    return if (exprVal is Extension.Value.String) {
        exprVal.value.value
    } else {
        when (val v = initExt.value) {
            is Extension.Value.String -> v.value.value
            else -> null
        }
    }
}

/**
 * Retrieves the ItemControl code from the item, or null if not present.
 * @return The item control code, or null.
 */
fun Questionnaire.Item.getItemControl(): String? {
    val ext = this.extension.firstOrNull { it.url == SdcExtensions.ITEM_CONTROL } ?: return null
    return when (val v = ext.value) {
        is Extension.Value.CodeableConcept ->
            v.value.coding
                .firstOrNull()
                ?.code
                ?.value
        is Extension.Value.Code -> v.value.value
        is Extension.Value.String -> v.value.value
        else -> null
    }
}

/**
 * Safe accessor for decimal min value returning a [Result].
 *
 * @return A [Result] enclosing the [dev.ohs.fhir.model.r4.FhirDecimal] minimum value.
 */
fun Questionnaire.Item.getDecimalMinValue(): Result<dev.ohs.fhir.model.r4.FhirDecimal?> = safeMinValue

/**
 * Safe accessor for decimal max value returning a [Result].
 *
 * @return A [Result] enclosing the [dev.ohs.fhir.model.r4.FhirDecimal] maximum value.
 */
fun Questionnaire.Item.getDecimalMaxValue(): Result<dev.ohs.fhir.model.r4.FhirDecimal?> = safeMaxValue

/**
 * Retrieves the minimum value extension for numeric inputs.
 * @return The minimum value, or null.
 */
fun Questionnaire.Item.getMinValue(): Float? {
    val ext = this.extension.firstOrNull { it.url == SdcExtensions.MIN_VALUE }
    val dec =
        when (val v = ext?.value) {
            is Extension.Value.Decimal -> v.value.value
            is Extension.Value.Integer ->
                v.value.value?.let {
                    dev.ohs.fhir.model.r4.FhirDecimal
                        .fromInt(it)
                }
            else -> null
        }
    val str = dec?.toString()
    return if (str != null) str.toFloatOrNull() else null
}

/**
 * Retrieves the maximum value extension for numeric inputs.
 * @return The maximum value, or null.
 */
fun Questionnaire.Item.getMaxValue(): Float? {
    val ext = this.extension.firstOrNull { it.url == SdcExtensions.MAX_VALUE }
    val dec =
        when (val v = ext?.value) {
            is Extension.Value.Decimal -> v.value.value
            is Extension.Value.Integer ->
                v.value.value?.let {
                    dev.ohs.fhir.model.r4.FhirDecimal
                        .fromInt(it)
                }
            else -> null
        }
    val str = dec?.toString()
    return if (str != null) str.toFloatOrNull() else null
}

/**
 * Helper to find localized content in translation extensions.
 *
 * @param extensions List of extensions to search.
 * @param langPrefix Language prefix.
 * @return Localized string or null.
 */
private fun findTranslation(
    extensions: List<Extension>,
    langPrefix: String,
): String? {
    for (ext in extensions) {
        val langExt = ext.extension.firstOrNull { it.url == "lang" }
        val langCode =
            when (val langVal = langExt?.value) {
                is Extension.Value.Code -> langVal.value.value
                is Extension.Value.String -> langVal.value.value
                else -> null
            }
        if (langCode != null && langCode.lowercase().startsWith(langPrefix)) {
            val contentExt = ext.extension.firstOrNull { it.url == "content" }
            val v = contentExt?.value
            val content = if (v is Extension.Value.String) v.value.value else null
            if (!content.isNullOrBlank()) {
                return content
            }
        }
    }
    return null
}

/**
 * Retrieves the localized text for a questionnaire item using the FHIR translation extension.
 *
 * @param language The language tag to search for (e.g. "es", "ja", "he", "zh").
 * @return The localized text string if present, or the item's default text.
 */
fun Questionnaire.Item.getLocalizedText(language: String = currentLanguageState.value): String {
    val langPrefix = language.lowercase().split("-", "_").first()
    val transExt = this.extension.filter { it.url == SdcExtensions.TRANSLATION }
    return findTranslation(transExt, langPrefix) ?: (this.text?.value ?: "")
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
    val titleExt = this.title
    val titleTransExt =
        if (titleExt != null) {
            titleExt.extension.filter { it.url == SdcExtensions.TRANSLATION }
        } else {
            emptyList()
        }
    return findTranslation(titleTransExt + directTransExt, langPrefix) ?: (this.title?.value ?: "")
}
