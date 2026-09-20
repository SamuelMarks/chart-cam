/**
 * @file QuestionnaireUtilsTest.kt
 * Contains unit tests for QuestionnaireUtils helper functions.
 */
package io.healthplatform.chartcam.utils

import dev.ohs.fhir.model.r4.Attachment
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.Integer
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.QuestionnaireResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Unit tests verifying QuestionnaireUtils behavior.
 */
class QuestionnaireUtilsTest {
    /**
     * Tests stripNarrativeDiv with null and empty inputs.
     */
    @Test
    fun testStripNarrativeDiv_nullAndEmpty() {
        assertNull(QuestionnaireUtils.stripNarrativeDiv(null))
        assertEquals("", QuestionnaireUtils.stripNarrativeDiv(""))
        assertEquals("", QuestionnaireUtils.stripNarrativeDiv("   "))
    }

    /**
     * Tests stripNarrativeDiv with standard div tags.
     */
    @Test
    fun testStripNarrativeDiv_standardDiv() {
        val input = "<div>Patient examination completed.</div>"
        assertEquals("Patient examination completed.", QuestionnaireUtils.stripNarrativeDiv(input))
    }

    /**
     * Tests stripNarrativeDiv with FHIR XHTML namespaced div.
     */
    @Test
    fun testStripNarrativeDiv_namespacedDiv() {
        val input = """<div xmlns="http://www.w3.org/1999/xhtml">Follow-up in 2 weeks.</div>"""
        assertEquals("Follow-up in 2 weeks.", QuestionnaireUtils.stripNarrativeDiv(input))
    }

    /**
     * Tests stripNarrativeDiv with nested tags.
     */
    @Test
    fun testStripNarrativeDiv_nestedContent() {
        val input = """<div xmlns="http://www.w3.org/1999/xhtml"><p>Vitals normal.</p></div>"""
        assertEquals("<p>Vitals normal.</p>", QuestionnaireUtils.stripNarrativeDiv(input))
    }

    /**
     * Tests stripNarrativeDiv when no outer div is present.
     */
    @Test
    fun testStripNarrativeDiv_plainText() {
        val input = "Just plain clinical notes."
        assertEquals("Just plain clinical notes.", QuestionnaireUtils.stripNarrativeDiv(input))
    }

    /**
     * Tests findItemRecursively with empty items, blank linkId, and deep/cycle structures.
     */
    @Test
    fun testFindItemRecursively() {
        assertNull(QuestionnaireUtils.findItemRecursively(emptyList(), "item-1"))
        assertNull(
            QuestionnaireUtils.findItemRecursively(
                listOf(
                    Questionnaire.Item(
                        linkId = FhirString(value = "x"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                    ),
                ),
                "",
            ),
        )
        assertNull(
            QuestionnaireUtils.findItemRecursively(
                listOf(
                    Questionnaire.Item(
                        linkId = FhirString(value = "x"),
                        type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                    ),
                ),
                "   ",
            ),
        )

        val nestedItem =
            Questionnaire.Item(
                linkId = FhirString(value = "nested-1"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "Nested Question"),
            )
        val parentItem =
            Questionnaire.Item(
                linkId = FhirString(value = "parent-1"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                text = FhirString(value = "Parent Question"),
                item = listOf(nestedItem),
            )
        val duplicateItem =
            Questionnaire.Item(
                linkId = FhirString(value = "parent-1"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            )
        val nullLinkIdItem =
            Questionnaire.Item(
                linkId = FhirString(),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            )

        val items = listOf(parentItem, duplicateItem, nullLinkIdItem)

        // Find top-level
        val foundTop = QuestionnaireUtils.findItemRecursively(items, "parent-1")
        assertNotNull(foundTop)
        assertEquals("parent-1", foundTop.linkId.value)

        // Find nested
        val foundNested = QuestionnaireUtils.findItemRecursively(items, "nested-1")
        assertNotNull(foundNested)
        assertEquals("nested-1", foundNested.linkId.value)

        // Missing item
        assertNull(QuestionnaireUtils.findItemRecursively(items, "non-existent"))

        // Exceed recursion depth
        assertNull(
            QuestionnaireUtils.findItemRecursivelyInternal(
                items = listOf(nestedItem),
                linkId = "nested-1",
                visitedLinkIds = mutableSetOf(),
                depth = 51,
            ),
        )
    }

    /**
     * Tests resolveLabel covering empty items, blank and non-blank linkIds, and fallback behavior.
     */
    @Test
    fun testResolveLabel() {
        // Empty items list
        assertEquals("fallback-label", QuestionnaireUtils.resolveLabel(emptyList(), "", "fallback-label"))
        assertEquals("q1", QuestionnaireUtils.resolveLabel(emptyList(), "q1", "fallback-label"))

        val itemWithText =
            Questionnaire.Item(
                linkId = FhirString(value = "q-text"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "Question Text"),
            )
        val itemWithoutText =
            Questionnaire.Item(
                linkId = FhirString(value = "q-no-text"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "   "),
            )
        val itemNullText =
            Questionnaire.Item(
                linkId = FhirString(value = "q-null-text"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = null,
            )
        val itemBlankLinkId =
            Questionnaire.Item(
                linkId = FhirString(value = "   "),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = null,
            )

        val items = listOf(itemWithText, itemWithoutText, itemNullText, itemBlankLinkId)

        // Text present
        assertEquals("Question Text", QuestionnaireUtils.resolveLabel(items, "q-text"))
        // Text blank -> returns linkId
        assertEquals("q-no-text", QuestionnaireUtils.resolveLabel(items, "q-no-text"))
        // Text null -> returns linkId
        assertEquals("q-null-text", QuestionnaireUtils.resolveLabel(items, "q-null-text"))
        // Item not found, non-blank linkId -> returns linkId
        assertEquals("unknown-link", QuestionnaireUtils.resolveLabel(items, "unknown-link"))
        // Blank linkId with fallback
        assertEquals("custom-fallback", QuestionnaireUtils.resolveLabel(items, "", "custom-fallback"))
    }

    /**
     * Tests buildDummyItemsRecursively covering all answer types and nested group items.
     */
    @Test
    fun testBuildDummyItemsRecursively() {
        val qrItems =
            listOf(
                // 1. String answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "str-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .String(FhirString(value = "Text")),
                            ),
                        ),
                ),
                // 2. Boolean answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "bool-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Boolean(FhirBoolean(value = true)),
                            ),
                        ),
                ),
                // 3. Attachment answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "att-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Attachment(Attachment()),
                            ),
                        ),
                ),
                // 4. Decimal answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "dec-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Decimal(Decimal(value = FhirDecimal.fromString("3.14"))),
                            ),
                        ),
                ),
                // 5. Integer answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "int-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Integer(Integer(value = 42)),
                            ),
                        ),
                ),
                // 6. Date answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "date-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Date(Date(value = FhirDate.fromString("2026-01-01"))),
                            ),
                        ),
                ),
                // 7. DateTime answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "dt-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value.DateTime(
                                        DateTime(value = FhirDateTime.fromString("2026-01-01T12:00:00Z")),
                                    ),
                            ),
                        ),
                ),
                // 8. Other answer type (Coding)
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "coding-item"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Coding(Coding(code = Code(value = "c1"))),
                            ),
                        ),
                ),
                // 9. Null answer with child items (Group)
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "group-item"),
                    item =
                        listOf(
                            QuestionnaireResponse.Item(
                                linkId = FhirString(value = "child-item"),
                                answer =
                                    listOf(
                                        QuestionnaireResponse.Item.Answer(
                                            value =
                                                QuestionnaireResponse.Item.Answer.Value
                                                    .String(FhirString(value = "ChildVal")),
                                        ),
                                    ),
                            ),
                        ),
                ),
                // 10. Null answer without child items (Default String)
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "empty-item"),
                ),
                // 11. Item with null linkId value (skipped)
                QuestionnaireResponse.Item(
                    linkId = FhirString(),
                ),
                // 12. Item with empty string linkId
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = ""),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .String(FhirString(value = "Val")),
                            ),
                        ),
                ),
            )

        val builders = QuestionnaireUtils.buildDummyItemsRecursively(qrItems)
        // 11 valid items constructed
        assertEquals(11, builders.size)
        val items = builders.map { it.build() }
        assertEquals(Questionnaire.QuestionnaireItemType.String, items[0].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.Boolean, items[1].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.Attachment, items[2].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.Decimal, items[3].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.Integer, items[4].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.Date, items[5].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.DateTime, items[6].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.String, items[7].type.value)
        assertEquals(Questionnaire.QuestionnaireItemType.Group, items[8].type.value)
        assertEquals(1, items[8].item.size)
        assertEquals(Questionnaire.QuestionnaireItemType.String, items[9].type.value)
    }

    /**
     * Tests buildResponseItemsRecursively covering String, List, Boolean, Float, disabled, and nested answers.
     */
    @Test
    fun testBuildResponseItemsRecursively() {
        val qItems =
            listOf(
                Questionnaire.Item(
                    linkId = FhirString(value = "date-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                    text = FhirString(value = "Date Question"),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "dt-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "dec-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "int-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "int-invalid-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "str-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "blank-str-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "list-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "empty-list-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "bool-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Boolean),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "float-dec-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                ),
                Questionnaire.Item(
                    linkId = FhirString(value = "float-int-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                ),
                // Group item with nested child
                Questionnaire.Item(
                    linkId = FhirString(value = "group-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                    item =
                        listOf(
                            Questionnaire.Item(
                                linkId = FhirString(value = "nested-q"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ),
                        ),
                ),
                // Group item with empty nested children
                Questionnaire.Item(
                    linkId = FhirString(value = "empty-group-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.Group),
                    item =
                        listOf(
                            Questionnaire.Item(
                                linkId = FhirString(value = "unanswered-child"),
                                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                            ),
                        ),
                ),
                // Item with unsupported answer type
                Questionnaire.Item(
                    linkId = FhirString(value = "unsupported-ans-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ),
                // Disabled item via enableWhen
                Questionnaire.Item(
                    linkId = FhirString(value = "disabled-q"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                    enableWhen =
                        listOf(
                            Questionnaire.Item.EnableWhen(
                                question = FhirString(value = "bool-q"),
                                operator = Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo),
                                answer =
                                    Questionnaire.Item.EnableWhen.Answer
                                        .Boolean(FhirBoolean(value = false)),
                            ),
                        ),
                ),
                // Item with null linkId
                Questionnaire.Item(
                    linkId = FhirString(),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ),
                // Item with null type value
                Questionnaire.Item(
                    linkId = FhirString(value = "null-type-q"),
                    type = Enumeration(value = null),
                ),
            )

        val answers: Map<String, Any> =
            mapOf(
                "date-q" to "2026-05-12",
                "dt-q" to "2026-05-12T10:30:00Z",
                "dec-q" to "12.34",
                "int-q" to "42",
                "int-invalid-q" to "not-an-int",
                "str-q" to "Clinical Note",
                "blank-str-q" to "   ",
                "list-q" to listOf("Choice1", "Choice2", "  "),
                "empty-list-q" to listOf("   "),
                "bool-q" to true,
                "float-dec-q" to 99.5f,
                "float-int-q" to 15.0f,
                "nested-q" to "Nested answer value",
                "unsupported-ans-q" to 12345,
                "disabled-q" to "Should not be emitted",
                "null-type-q" to "Default String Answer",
            )

        val responseBuilders = QuestionnaireUtils.buildResponseItemsRecursively(qItems, answers)
        val responseItems = responseBuilders.map { it.build() }
        assertTrue(responseItems.isNotEmpty())

        val linkIdMap = responseItems.associateBy { it.linkId.value }
        assertEquals(
            "2026-05-12",
            (linkIdMap["date-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.Date)?.value?.value.toString(),
        )
        assertEquals(
            "2026-05-12T10:30:00Z",
            (linkIdMap["dt-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.DateTime)?.value?.value.toString(),
        )
        assertEquals(
            "12.34",
            (linkIdMap["dec-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.Decimal)?.value?.value.toString(),
        )
        assertEquals(42, (linkIdMap["int-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.Integer)?.value?.value)
        assertEquals(
            0,
            (linkIdMap["int-invalid-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.Integer)?.value?.value,
        )
        assertEquals(
            "Clinical Note",
            (linkIdMap["str-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.String)?.value?.value,
        )
        // blank-str-q and empty-list-q and unsupported-ans-q have no valid answer and are not Group, so omitted
        assertNull(linkIdMap["blank-str-q"])
        assertNull(linkIdMap["empty-list-q"])
        assertNull(linkIdMap["unsupported-ans-q"])
        assertNull(linkIdMap["disabled-q"])
        // list-q has 2 answers
        assertEquals(2, linkIdMap["list-q"]?.answer?.size)
        // bool-q
        assertEquals(true, (linkIdMap["bool-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.Boolean)?.value?.value)
        // float-dec-q
        assertEquals(
            "99.5",
            (linkIdMap["float-dec-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.Decimal)?.value?.value.toString(),
        )
        // float-int-q
        assertEquals(
            15,
            (linkIdMap["float-int-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.Integer)?.value?.value,
        )
        // group-q contains nested-q
        val group = linkIdMap["group-q"]
        assertNotNull(group)
        assertEquals(1, group.item.size)
        assertEquals(
            "nested-q",
            group.item
                .first()
                .linkId.value,
        )
        // empty-group-q emitted even though no children answered
        assertNotNull(linkIdMap["empty-group-q"])
        // null-type-q defaulted to String type
        assertEquals(
            "Default String Answer",
            (linkIdMap["null-type-q"]?.answer?.first()?.value as? QuestionnaireResponse.Item.Answer.Value.String)?.value?.value,
        )
    }

    /**
     * Tests extractAnswersRecursively covering multi-answers, single answers (String, Boolean, Decimal, Integer, Date, DateTime), and null values.
     */
    @Test
    fun testExtractAnswersRecursively() {
        val qrItems =
            listOf(
                // Multi-answer item
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "multi-ans"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .String(FhirString(value = "OptA")),
                            ),
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .String(FhirString(value = "OptB")),
                            ),
                            // non-string value in multi-answer filtered out
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Integer(Integer(value = 1)),
                            ),
                            // String value with null value
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .String(FhirString(value = null)),
                            ),
                        ),
                ),
                // Single String answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "single-str"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .String(FhirString(value = "Sample text")),
                            ),
                        ),
                ),
                // Single Boolean answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "single-bool"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Boolean(FhirBoolean(value = false)),
                            ),
                        ),
                ),
                // Single Decimal answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "single-dec"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Decimal(Decimal(value = FhirDecimal.fromString("12.5"))),
                            ),
                        ),
                ),
                // Single Decimal with invalid float fallback
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "invalid-dec"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Decimal(Decimal(value = null)),
                            ),
                        ),
                ),
                // Single Integer answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "single-int"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Integer(Integer(value = 8)),
                            ),
                        ),
                ),
                // Single Integer answer with null value
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "null-int"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Integer(Integer(value = null)),
                            ),
                        ),
                ),
                // Single Date answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "single-date"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Date(Date(value = FhirDate.fromString("2026-03-01"))),
                            ),
                        ),
                ),
                // Single DateTime answer
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "single-dt"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value.DateTime(
                                        DateTime(value = FhirDateTime.fromString("2026-03-01T08:00:00Z")),
                                    ),
                            ),
                        ),
                ),
                // Null value elements
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "null-str"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .String(FhirString(value = null)),
                            ),
                        ),
                ),
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "null-bool"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Boolean(FhirBoolean(value = null)),
                            ),
                        ),
                ),
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "null-date"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Date(Date(value = null)),
                            ),
                        ),
                ),
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "null-dt"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .DateTime(DateTime(value = null)),
                            ),
                        ),
                ),
                // Item with 1 answer but null value
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "null-answer-val"),
                    answer = listOf(QuestionnaireResponse.Item.Answer(value = null)),
                ),
                // Item with 0 answers
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "zero-answers"),
                    answer = emptyList(),
                ),
                // Item with Coding answer (unhandled in extractSingleAnswer, covers false branches)
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "coding-ans"),
                    answer =
                        listOf(
                            QuestionnaireResponse.Item.Answer(
                                value =
                                    QuestionnaireResponse.Item.Answer.Value
                                        .Coding(Coding(code = Code(value = "c1"))),
                            ),
                        ),
                ),
                // Item with null linkId (skipped)
                QuestionnaireResponse.Item(
                    linkId = FhirString(),
                ),
                // Item with nested child
                QuestionnaireResponse.Item(
                    linkId = FhirString(value = "parent-qr"),
                    item =
                        listOf(
                            QuestionnaireResponse.Item(
                                linkId = FhirString(value = "child-qr"),
                                answer =
                                    listOf(
                                        QuestionnaireResponse.Item.Answer(
                                            value =
                                                QuestionnaireResponse.Item.Answer.Value
                                                    .String(FhirString(value = "Child Text")),
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        val extracted = mutableMapOf<String, Any>()
        QuestionnaireUtils.extractAnswersRecursively(qrItems, extracted)

        assertEquals(listOf("OptA", "OptB"), extracted["multi-ans"])
        assertEquals("Sample text", extracted["single-str"])
        assertEquals(false, extracted["single-bool"])
        assertEquals(12.5f, extracted["single-dec"])
        assertNull(extracted["invalid-dec"])
        assertEquals(8.0f, extracted["single-int"])
        assertEquals(0.0f, extracted["null-int"])
        assertEquals("2026-03-01", extracted["single-date"])
        assertEquals("2026-03-01T08:00:00Z", extracted["single-dt"])
        assertEquals("Child Text", extracted["child-qr"])
        assertNull(extracted["null-str"])
        assertNull(extracted["null-bool"])
        assertNull(extracted["null-date"])
        assertNull(extracted["null-dt"])
        assertNull(extracted["null-answer-val"])
        assertNull(extracted["zero-answers"])
        assertNull(extracted["coding-ans"])
    }
}
