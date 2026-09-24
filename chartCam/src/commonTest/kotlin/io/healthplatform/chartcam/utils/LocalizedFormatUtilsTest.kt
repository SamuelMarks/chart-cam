/**
 * @file LocalizedFormatUtilsTest.kt
 * Contains declarations for LocalizedFormatUtilsTest.kt.
 *
 * Comprehensive tests for safe localized template formatting, date parsing, and conflict type resolution.
 */
package io.healthplatform.chartcam.utils

import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.conflict_type_exact_match
import chartcam.chartcam.generated.resources.conflict_type_id_collision
import chartcam.chartcam.generated.resources.conflict_type_mrn_collision
import chartcam.chartcam.generated.resources.conflict_type_orphan
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import io.healthplatform.chartcam.models.ConflictType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Unit test suite verifying safe Result-percolating localization and format operations.
 */
class LocalizedFormatUtilsTest {
    /**
     * Verifies that the LocalizedFormatUtils namespace object is present and FormatError preserves message.
     */
    @Test
    fun testLocalizedFormatUtilsObjectAndError() {
        assertNotNull(LocalizedFormatUtils)
        val err = FormatError("custom error message")
        assertEquals("custom error message", err.message)
    }

    /**
     * Verifies that formatTemplateSafely formats strings with indexed placeholders successfully.
     */
    @Test
    fun testFormatTemplateSafelyIndexed() {
        val result = formatTemplateSafely("Hello %1\$s, you have %2\$d items", "Alice", 5)
        assertTrue(result.isSuccess)
        assertEquals("Hello Alice, you have 5 items", result.getOrNull())
    }

    /**
     * Verifies that formatTemplateSafely formats strings with sequential %s and %d placeholders.
     */
    @Test
    fun testFormatTemplateSafelySequential() {
        val result = formatTemplateSafely("Patient: %s, Score: %d", "Bob", 42)
        assertTrue(result.isSuccess)
        assertEquals("Patient: Bob, Score: 42", result.getOrNull())
    }

    /**
     * Verifies that formatTemplateSafely returns failure when arguments exceed template placeholders.
     */
    @Test
    fun testFormatTemplateSafelyMismatch() {
        val result = formatTemplateSafely("No placeholders here", "extra")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FormatError)
    }

    /**
     * Verifies safe parsing of valid flexible date inputs across languages and with default language.
     */
    @Test
    fun testSafeParseFlexibleDateValid() {
        val defaultResult = safeParseFlexibleDate("1995-05-20")
        assertTrue(defaultResult.isSuccess)
        assertEquals(LocalDate(1995, 5, 20), defaultResult.getOrNull())

        val isoResult = safeParseFlexibleDate("1995-05-20", "en")
        assertTrue(isoResult.isSuccess)
        assertEquals(LocalDate(1995, 5, 20), isoResult.getOrNull())

        val slashResult = safeParseFlexibleDate("12/31/1999", "en")
        assertTrue(slashResult.isSuccess)
        assertEquals(LocalDate(1999, 12, 31), slashResult.getOrNull())
    }

    /**
     * Verifies safe parsing of invalid flexible date inputs returns failure.
     */
    @Test
    fun testSafeParseFlexibleDateInvalid() {
        val result = safeParseFlexibleDate("not-a-date", "en")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FormatError)
    }

    /**
     * Verifies safe resolution of valid conflict type strings.
     */
    @Test
    fun testSafeResolveConflictTypeValid() {
        val exact = safeResolveConflictType("EXACT_MATCH")
        assertTrue(exact.isSuccess)
        assertEquals(ConflictType.EXACT_MATCH, exact.getOrNull())

        val mrn = safeResolveConflictType("mrn_collision_different_id")
        assertTrue(mrn.isSuccess)
        assertEquals(ConflictType.MRN_COLLISION_DIFFERENT_ID, mrn.getOrNull())
    }

    /**
     * Verifies safe resolution of invalid conflict type strings returns failure.
     */
    @Test
    fun testSafeResolveConflictTypeInvalid() {
        val result = safeResolveConflictType("UNKNOWN_CONFLICT")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FormatError)
    }

    /**
     * Verifies exhaustive mapping of ConflictType to StringResource.
     */
    @Test
    fun testConflictTypeToLocalizedResource() {
        assertEquals(Res.string.conflict_type_exact_match, ConflictType.EXACT_MATCH.toLocalizedResource())
        assertEquals(Res.string.conflict_type_id_collision, ConflictType.ID_COLLISION_DIFFERENT_DATA.toLocalizedResource())
        assertEquals(Res.string.conflict_type_mrn_collision, ConflictType.MRN_COLLISION_DIFFERENT_ID.toLocalizedResource())
        assertEquals(Res.string.conflict_type_orphan, ConflictType.ORPHAN_ENCOUNTER.toLocalizedResource())
    }

    /**
     * Verifies getLocalizedSlotLabel finds an item and returns its resolved label.
     */
    @Test
    fun testGetLocalizedSlotLabelFound() {
        val item =
            Questionnaire.Item(
                linkId = FhirString(value = "item_cornea_left"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "Cornea Profile Left"),
            )
        val q =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Draft),
                item = listOf(item),
            )

        val labelResult = getLocalizedSlotLabel("item_cornea_left", q)
        assertTrue(labelResult.isSuccess)
        assertEquals("Cornea Profile Left", labelResult.getOrNull())
    }

    /**
     * Verifies getLocalizedSlotLabel returns failure when item is missing, questionnaire is null, or label is blank.
     */
    @Test
    fun testGetLocalizedSlotLabelNotFound() {
        val nullResult = getLocalizedSlotLabel("unknown_key", null)
        assertTrue(nullResult.isFailure)

        val emptyQ =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Draft),
                item = emptyList(),
            )
        val missingInEmptyResult = getLocalizedSlotLabel("missing_key", emptyQ)
        assertTrue(missingInEmptyResult.isFailure)

        val validItem =
            Questionnaire.Item(
                linkId = FhirString(value = "item_valid"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "Valid Label"),
            )
        val itemNoText =
            Questionnaire.Item(
                linkId = FhirString(value = "item_no_text"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = null,
            )
        val itemNullValue =
            Questionnaire.Item(
                linkId = FhirString(value = "item_null_value"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = null),
            )
        val blankItem =
            Questionnaire.Item(
                linkId = FhirString(value = "blank_key"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "   "),
            )
        val multiQ =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Draft),
                item = listOf(validItem, itemNoText, itemNullValue, blankItem),
            )

        // Missing item in non-empty list
        val missingInNonEmpty = getLocalizedSlotLabel("non_existent_key", multiQ)
        assertTrue(missingInNonEmpty.isFailure)

        // Item with null text
        val noTextResult = getLocalizedSlotLabel("item_no_text", multiQ)
        assertTrue(noTextResult.isFailure)

        // Item with null text value
        val nullValueResult = getLocalizedSlotLabel("item_null_value", multiQ)
        assertTrue(nullValueResult.isFailure)

        // Item with blank text value
        val blankResult = getLocalizedSlotLabel("blank_key", multiQ)
        assertTrue(blankResult.isFailure)
    }
}
