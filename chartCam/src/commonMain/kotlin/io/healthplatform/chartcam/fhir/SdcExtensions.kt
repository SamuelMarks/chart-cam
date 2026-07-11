package io.healthplatform.chartcam.fhir

import com.google.fhir.model.r4.Questionnaire

/**
 * Structured Data Capture (SDC) Extension definitions and helpers.
 * https://hl7.org/fhir/uv/sdc/
 */
object SdcExtensions {
    const val ITEM_CONTROL = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"
    const val MIN_VALUE = "http://hl7.org/fhir/StructureDefinition/minValue"
    const val MAX_VALUE = "http://hl7.org/fhir/StructureDefinition/maxValue"
    const val HIDDEN = "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden"
}

/**
 * Checks if a Questionnaire Item is hidden based on the SDC hidden extension.
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
 * Retrieves the ItemControl code from the item, or null if not present.
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
 */
fun Questionnaire.Item.getMaxValue(): Float? =
    this.extension
        .firstOrNull { it.url == SdcExtensions.MAX_VALUE }
        ?.value
        ?.asInteger()
        ?.value
        ?.value
        ?.toFloat()
