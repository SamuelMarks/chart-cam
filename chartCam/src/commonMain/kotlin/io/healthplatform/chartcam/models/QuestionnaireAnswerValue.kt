/**
 * @file QuestionnaireAnswerValue.kt
 * Defines the strongly-typed sealed hierarchy for SDC/FHIR Questionnaire answer values,
 * along with domain enums and structured repeating-group answer keys.
 */
package io.healthplatform.chartcam.models

import androidx.compose.runtime.Immutable
import dev.ohs.fhir.model.r4.Coding
import kotlin.jvm.JvmInline

/**
 * Strongly-typed sealed hierarchy representing a single or multi-valued answer
 * to a FHIR [dev.ohs.fhir.model.r4.Questionnaire.Item].
 * Eliminates untyped `Any` casts by providing an explicit sum type with bidirectional
 * conversion to raw primitive values.
 */
@Immutable
sealed interface QuestionnaireAnswerValue {
    /**
     * Converts this strongly-typed answer value to its underlying runtime representation
     * for interoperability with legacy evaluators or serializers.
     *
     * @return The unwrapped value.
     */
    fun toRawValue(): Any

    /**
     * Text or string answer variant.
     *
     * @property text The string answer content.
     */
    @JvmInline
    value class TextValue(
        val text: String,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [text] string.
         *
         * @return The unwrapped string value.
         */
        override fun toRawValue(): Any = text
    }

    /**
     * Boolean answer variant.
     *
     * @property flag The boolean answer value.
     */
    @JvmInline
    value class BooleanValue(
        val flag: Boolean,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [flag] boolean.
         *
         * @return The unwrapped boolean value.
         */
        override fun toRawValue(): Any = flag
    }

    /**
     * Integer answer variant.
     *
     * @property number The integer answer value.
     */
    @JvmInline
    value class IntegerValue(
        val number: Int,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [number] integer.
         *
         * @return The unwrapped integer value.
         */
        override fun toRawValue(): Any = number
    }

    /**
     * Decimal / floating-point answer variant.
     *
     * @property number The double precision decimal answer value.
     */
    @JvmInline
    value class DecimalValue(
        val number: Double,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [number] double.
         *
         * @return The unwrapped double value.
         */
        override fun toRawValue(): Any = number
    }

    /**
     * ISO-8601 date answer variant (`YYYY-MM-DD`).
     *
     * @property isoDate The formatted ISO date string.
     */
    @JvmInline
    value class DateValue(
        val isoDate: String,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [isoDate] string.
         *
         * @return The unwrapped ISO date string.
         */
        override fun toRawValue(): Any = isoDate
    }

    /**
     * ISO-8601 date-time answer variant.
     *
     * @property isoDateTime The formatted ISO date-time string.
     */
    @JvmInline
    value class DateTimeValue(
        val isoDateTime: String,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [isoDateTime] string.
         *
         * @return The unwrapped ISO date-time string.
         */
        override fun toRawValue(): Any = isoDateTime
    }

    /**
     * FHIR [Coding] answer variant for coded choice questions.
     *
     * @property coding The selected FHIR [Coding].
     */
    @JvmInline
    value class CodingValue(
        val coding: Coding,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [coding] object.
         *
         * @return The unwrapped [Coding] value.
         */
        override fun toRawValue(): Any = coding
    }

    /**
     * Attachment URL/path answer variant for photo, video, or audio items.
     *
     * @property url The local or remote attachment URL string.
     */
    @JvmInline
    value class AttachmentUrlValue(
        val url: String,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [url] string.
         *
         * @return The unwrapped attachment URL string.
         */
        override fun toRawValue(): Any = url
    }

    /**
     * Fitzpatrick skin phototype answer variant.
     *
     * @property skinType The selected [FitzpatrickSkinType].
     */
    @JvmInline
    value class FitzpatrickValue(
        val skinType: FitzpatrickSkinType,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [skinType] enum.
         *
         * @return The unwrapped [FitzpatrickSkinType].
         */
        override fun toRawValue(): Any = skinType
    }

    /**
     * Anatomical body map coordinate answer variant.
     *
     * @property location The selected [BodyMapLocation].
     */
    @JvmInline
    value class BodyMapValue(
        val location: BodyMapLocation,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [location] object.
         *
         * @return The unwrapped [BodyMapLocation].
         */
        override fun toRawValue(): Any = location
    }

    /**
     * Multi-select list of string option values.
     *
     * @property values The selected option strings.
     */
    @Immutable
    data class MultiSelectValue(
        val values: List<String>,
    ) : QuestionnaireAnswerValue {
        /**
         * Returns the underlying [values] list.
         *
         * @return The unwrapped list of option strings.
         */
        override fun toRawValue(): Any = values
    }

    /**
     * Companion factory methods for converting between raw values and [QuestionnaireAnswerValue].
     */
    companion object {
        /**
         * Wraps an arbitrary runtime answer object into a strongly-typed [QuestionnaireAnswerValue].
         *
         * @param raw The raw answer object.
         * @return The corresponding [QuestionnaireAnswerValue].
         */
        fun fromAny(raw: Any): QuestionnaireAnswerValue =
            when (raw) {
                is QuestionnaireAnswerValue -> raw
                is Boolean -> BooleanValue(raw)
                is Int -> IntegerValue(raw)
                is Long -> IntegerValue(raw.toInt())
                is Float -> DecimalValue(raw.toDouble())
                is Double -> DecimalValue(raw)
                is FitzpatrickSkinType -> FitzpatrickValue(raw)
                is BodyMapLocation -> BodyMapValue(raw)
                is Coding -> CodingValue(raw)
                is List<*> -> MultiSelectValue(raw.mapNotNull { it?.toString() })
                is String -> TextValue(raw)
                else -> TextValue(raw.toString())
            }

        /**
         * Converts an untyped answer map into a strongly-typed [QuestionnaireAnswerValue] map.
         *
         * @param rawMap The untyped answer map.
         * @return A map of linkIds to [QuestionnaireAnswerValue] instances.
         */
        fun fromRawMap(rawMap: Map<String, Any>): Map<String, QuestionnaireAnswerValue> =
            rawMap.mapValues { (_, v) -> fromAny(v) }

        /**
         * Converts a strongly-typed [QuestionnaireAnswerValue] map into a raw answer map.
         *
         * @param typedMap The strongly-typed answer map.
         * @return A map of linkIds to unwrapped values.
         */
        fun toRawMap(typedMap: Map<String, QuestionnaireAnswerValue>): Map<String, Any> =
            typedMap.mapValues { (_, v) -> v.toRawValue() }
    }
}

/**
 * Structured representation of a questionnaire answer key, supporting both top-level `linkId`s
 * and repeating group keys formatted as `"$groupLinkId#$repeatIndex.$childLinkId"`.
 *
 * @property linkId The root or group question linkId.
 * @property repeatIndex Optional zero-based index for repeating groups.
 * @property childLinkId Optional nested child question linkId within a repeating group.
 */
@Immutable
data class AnswerKey(
    val linkId: String,
    val repeatIndex: Int? = null,
    val childLinkId: String? = null,
) {
    /**
     * Formats this [AnswerKey] into its canonical string key representation.
     *
     * @return The canonical key string.
     */
    fun format(): String =
        when {
            repeatIndex != null && !childLinkId.isNullOrEmpty() -> "$linkId#$repeatIndex.$childLinkId"
            repeatIndex != null -> "$linkId#$repeatIndex"
            else -> linkId
        }

    /**
     * Companion parser for [AnswerKey].
     */
    companion object {
        /**
         * Parses a raw key string into a structured [AnswerKey].
         *
         * @param rawKey The raw key string (e.g. `"lesion_group#0.lesion_size"` or `"notes"`).
         * @return The parsed [AnswerKey].
         */
        fun parse(rawKey: String): AnswerKey {
            val hashIdx = rawKey.indexOf('#')
            if (hashIdx <= 0) return AnswerKey(linkId = rawKey)
            val rootId = rawKey.substring(0, hashIdx)
            val remainder = rawKey.substring(hashIdx + 1)
            val dotIdx = remainder.indexOf('.')
            return if (dotIdx < 0) {
                val idx = remainder.toIntOrNull()
                if (idx != null) AnswerKey(linkId = rootId, repeatIndex = idx) else AnswerKey(linkId = rawKey)
            } else {
                val idx = remainder.substring(0, dotIdx).toIntOrNull()
                val child = remainder.substring(dotIdx + 1)
                if (idx != null && child.isNotEmpty()) {
                    AnswerKey(linkId = rootId, repeatIndex = idx, childLinkId = child)
                } else {
                    AnswerKey(linkId = rawKey)
                }
            }
        }
    }
}

/**
 * Strongly-typed enum of HL7 SDC `questionnaire-itemControl` extension codes supported by ChartCam.
 *
 * @property code The wire-format FHIR code string.
 */
enum class SdcItemControlCode(
    val code: String,
) {
    /** Photo camera attachment capture control. */
    PHOTO("photo"),

    /** Video camera attachment capture control. */
    VIDEO("video"),

    /** Boolean toggle switch control. */
    SWITCH("switch"),

    /** Numeric horizontal slider control. */
    SLIDER("slider"),

    /** Visual Analog Scale (VAS) pain slider control. */
    PAIN_VAS("pain-vas"),

    /** Wong-Baker FACES pain rating control. */
    WONG_BAKER("wong-baker"),

    /** Color/skin tone palette picker control. */
    PALETTE("palette"),

    /** Alternative color palette picker code. */
    COLOR_PALETTE("color-palette"),

    /** Fitzpatrick skin phototype selector code. */
    FITZPATRICK("fitzpatrick"),

    /** Interactive anatomical body map SVG selector. */
    BODY_MAP("body-map"),

    /** Segmented choice pill/tile selector. */
    SEGMENTED_CONTROL("segmented-control"),

    /** Choice cards selector. */
    CHOICE_CARDS("choice-cards"),

    /** 3-step guided facial profile & cornea series capture. */
    FACIAL_PROFILE_SERIES("facial-profile-series"),

    /** Multi/single select check-box control. */
    CHECK_BOX("check-box"),

    /** Single select radio-button control. */
    RADIO_BUTTON("radio-button"),
    ;

    /**
     * Companion lookup methods for [SdcItemControlCode].
     */
    companion object {
        /**
         * Resolves a raw FHIR itemControl code string to its strongly-typed [SdcItemControlCode] enum.
         *
         * @param rawCode The wire-format code string.
         * @return The matching [SdcItemControlCode], or null if unrecognized.
         */
        fun fromCode(rawCode: String?): SdcItemControlCode? {
            if (rawCode.isNullOrBlank()) return null
            val normalized = rawCode.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
        }
    }
}

/**
 * Strongly-typed enum of FHIR R4 resource types persisted and indexed in ChartCam.
 *
 * @property typeName The canonical FHIR R4 `resourceType` string.
 */
enum class FhirResourceType(
    val typeName: String,
) {
    /** FHIR Patient resource. */
    PATIENT("Patient"),

    /** FHIR Encounter resource. */
    ENCOUNTER("Encounter"),

    /** FHIR DocumentReference resource. */
    DOCUMENT_REFERENCE("DocumentReference"),

    /** FHIR Questionnaire resource. */
    QUESTIONNAIRE("Questionnaire"),

    /** FHIR QuestionnaireResponse resource. */
    QUESTIONNAIRE_RESPONSE("QuestionnaireResponse"),

    /** FHIR Practitioner resource. */
    PRACTITIONER("Practitioner"),

    /** FHIR Device resource. */
    DEVICE("Device"),

    /** FHIR Provenance resource. */
    PROVENANCE("Provenance"),

    /** FHIR Observation resource. */
    OBSERVATION("Observation"),

    /** FHIR Media resource. */
    MEDIA("Media"),

    /** FHIR Bundle resource. */
    BUNDLE("Bundle"),

    /** FHIR AuditEvent resource. */
    AUDIT_EVENT("AuditEvent"),

    /** FHIR Basic resource. */
    BASIC("Basic"),
    ;

    /**
     * Companion lookup methods for [FhirResourceType].
     */
    companion object {
        /**
         * Resolves a canonical FHIR resource type string to a [FhirResourceType].
         *
         * @param rawType The canonical resource type string.
         * @return The matching [FhirResourceType], or null if unrecognized.
         */
        fun fromTypeName(rawType: String?): FhirResourceType? =
            entries.firstOrNull { it.typeName == rawType }
    }
}
