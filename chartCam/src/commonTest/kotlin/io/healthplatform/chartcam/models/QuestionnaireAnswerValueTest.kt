/**
 * @file QuestionnaireAnswerValueTest.kt
 * Unit tests for [QuestionnaireAnswerValue], [AnswerKey], [SdcItemControlCode], and [FhirResourceType].
 */
package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Coding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [QuestionnaireAnswerValue], [AnswerKey], [SdcItemControlCode], and [FhirResourceType].
 */
class QuestionnaireAnswerValueTest {
    /**
     * Verifies all [QuestionnaireAnswerValue] subtypes and `fromAny` / `fromRawMap` / `toRawMap` conversions.
     */
    @Test
    fun testQuestionnaireAnswerValueSubtypesAndConversions() {
        val textVal = QuestionnaireAnswerValue.TextValue("note")
        assertEquals("note", textVal.text)
        assertEquals("note", textVal.toRawValue())
        assertEquals(textVal, QuestionnaireAnswerValue.fromAny(textVal))

        val boolVal = QuestionnaireAnswerValue.BooleanValue(true)
        assertEquals(true, boolVal.flag)
        assertEquals(true, boolVal.toRawValue())
        assertEquals(boolVal, QuestionnaireAnswerValue.fromAny(true))

        val intVal = QuestionnaireAnswerValue.IntegerValue(42)
        assertEquals(42, intVal.number)
        assertEquals(42, intVal.toRawValue())
        assertEquals(intVal, QuestionnaireAnswerValue.fromAny(42))
        assertEquals(intVal, QuestionnaireAnswerValue.fromAny(42L))

        val decVal = QuestionnaireAnswerValue.DecimalValue(3.5)
        assertEquals(3.5, decVal.number)
        assertEquals(3.5, decVal.toRawValue())
        assertEquals(decVal, QuestionnaireAnswerValue.fromAny(3.5))
        assertEquals(decVal, QuestionnaireAnswerValue.fromAny(3.5f))

        val dateVal = QuestionnaireAnswerValue.DateValue("2026-10-15")
        assertEquals("2026-10-15", dateVal.isoDate)
        assertEquals("2026-10-15", dateVal.toRawValue())

        val dateTimeVal = QuestionnaireAnswerValue.DateTimeValue("2026-10-15T12:00:00Z")
        assertEquals("2026-10-15T12:00:00Z", dateTimeVal.isoDateTime)
        assertEquals("2026-10-15T12:00:00Z", dateTimeVal.toRawValue())

        val coding = Coding(code = Code(value = "routine"))
        val codingVal = QuestionnaireAnswerValue.CodingValue(coding)
        assertEquals(coding, codingVal.coding)
        assertEquals(coding, codingVal.toRawValue())
        assertEquals(codingVal, QuestionnaireAnswerValue.fromAny(coding))

        val attachVal = QuestionnaireAnswerValue.AttachmentUrlValue("file:///img.jpg")
        assertEquals("file:///img.jpg", attachVal.url)
        assertEquals("file:///img.jpg", attachVal.toRawValue())

        val fitzType3 = FitzpatrickScaleDefaults.ALL_TYPES[2]
        val fitzVal = QuestionnaireAnswerValue.FitzpatrickValue(fitzType3)
        assertEquals(fitzType3, fitzVal.skinType)
        assertEquals(fitzType3, fitzVal.toRawValue())
        assertEquals(fitzVal, QuestionnaireAnswerValue.fromAny(fitzType3))

        val loc = BodyMapLocation(regionId = "head", displayName = "Head", xPercent = 50f, yPercent = 50f)
        val bodyVal = QuestionnaireAnswerValue.BodyMapValue(loc)
        assertEquals(loc, bodyVal.location)
        assertEquals(loc, bodyVal.toRawValue())
        assertEquals(bodyVal, QuestionnaireAnswerValue.fromAny(loc))

        val multiVal = QuestionnaireAnswerValue.MultiSelectValue(listOf("A", "B"))
        assertEquals(listOf("A", "B"), multiVal.values)
        assertEquals(listOf("A", "B"), multiVal.toRawValue())
        assertEquals(multiVal, QuestionnaireAnswerValue.fromAny(listOf("A", null, "B")))

        assertEquals(QuestionnaireAnswerValue.TextValue("hello"), QuestionnaireAnswerValue.fromAny("hello"))
        val customObj =
            object {
                override fun toString(): String = "custom_obj"
            }
        assertEquals(QuestionnaireAnswerValue.TextValue("custom_obj"), QuestionnaireAnswerValue.fromAny(customObj))

        val rawMap = mapOf("k1" to "v1", "k2" to 10, "k3" to true)
        val typedMap = QuestionnaireAnswerValue.fromRawMap(rawMap)
        assertEquals(3, typedMap.size)
        val roundTrip = QuestionnaireAnswerValue.toRawMap(typedMap)
        assertEquals(rawMap, roundTrip)
    }

    /**
     * Verifies [AnswerKey] parsing and formatting for simple, group-indexed, and child keys.
     */
    @Test
    fun testAnswerKeyParseAndFormat() {
        val simple = AnswerKey.parse("notes")
        assertEquals(AnswerKey("notes", null, null), simple)
        assertEquals("notes", simple.format())

        val groupOnly = AnswerKey.parse("lesion#2")
        assertEquals(AnswerKey("lesion", 2, null), groupOnly)
        assertEquals("lesion#2", groupOnly.format())

        val groupChild = AnswerKey.parse("lesion#2.diameter")
        assertEquals(AnswerKey("lesion", 2, "diameter"), groupChild)
        assertEquals("lesion#2.diameter", groupChild.format())

        val emptyChildKey = AnswerKey("lesion", 2, "")
        assertEquals("lesion#2", emptyChildKey.format())

        // Malformed keys fall back safely to simple linkId
        assertEquals(AnswerKey("#invalid"), AnswerKey.parse("#invalid"))
        assertEquals(AnswerKey("lesion#abc"), AnswerKey.parse("lesion#abc"))
        assertEquals(AnswerKey("lesion#abc.child"), AnswerKey.parse("lesion#abc.child"))
        assertEquals(AnswerKey("lesion#1."), AnswerKey.parse("lesion#1."))
    }

    /**
     * Verifies [SdcItemControlCode] and [FhirResourceType] lookup helpers.
     */
    @Test
    fun testEnumsLookup() {
        assertNull(SdcItemControlCode.fromCode(null))
        assertNull(SdcItemControlCode.fromCode("   "))
        assertNull(SdcItemControlCode.fromCode("unknown-control"))
        for (entry in SdcItemControlCode.entries) {
            assertEquals(entry, SdcItemControlCode.fromCode(entry.code.uppercase()))
        }

        assertNull(FhirResourceType.fromTypeName(null))
        assertNull(FhirResourceType.fromTypeName("UnknownResource"))
        for (entry in FhirResourceType.entries) {
            assertEquals(entry, FhirResourceType.fromTypeName(entry.typeName))
            assertTrue(entry.typeName.isNotEmpty())
        }
    }
}
