/**
 * @file SdcExtensions.kt
 * Contains declarations for SdcExtensions.kt.
 */
package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Questionnaire

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
