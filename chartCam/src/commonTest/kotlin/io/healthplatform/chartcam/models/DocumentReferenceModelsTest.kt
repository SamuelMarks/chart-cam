/**
 * @file DocumentReferenceModelsTest.kt
 * Tests for DocumentReferenceModels and ExtensibleEnumeration language parsing.
 */

package io.healthplatform.chartcam.models

import dev.ohs.fhir.model.r4.ExtensibleEnumeration
import dev.ohs.fhir.model.r4.terminologies.CommonLanguages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests verifying DocumentReference models, context, and extensible language parsing.
 */
class DocumentReferenceModelsTest {
    /**
     * Tests standard BCP-47 language tag parsing to predefined enum.
     */
    @Test
    fun testPredefinedLanguageParsing() {
        val resultEn = parseExtensibleLanguage("en")
        assertTrue(resultEn.isSuccess)
        val enumEn = resultEn.getOrThrow()
        assertTrue(enumEn is ExtensibleEnumeration.Predefined)
        assertEquals(CommonLanguages.En, enumEn.value)

        val resultEs = parseExtensibleLanguage("es")
        assertTrue(resultEs.isSuccess)
        val enumEs = resultEs.getOrThrow()
        assertTrue(enumEs is ExtensibleEnumeration.Predefined)
        assertEquals(CommonLanguages.Es, enumEs.value)

        val resultWhitespace = parseExtensibleLanguage("  EN  ")
        assertTrue(resultWhitespace.isSuccess)
        assertEquals(CommonLanguages.En, (resultWhitespace.getOrThrow() as ExtensibleEnumeration.Predefined).value)

        val resultEmpty = parseExtensibleLanguage("   ")
        assertTrue(resultEmpty.isFailure)
    }

    /**
     * Tests custom dialect parsing to Custom extensible enumeration.
     */
    @Test
    fun testCustomDialectLanguageParsing() {
        val result = parseExtensibleLanguage("custom-clinic-dialect-xyz")
        assertTrue(result.isSuccess)
        val enumCustom = result.getOrThrow()
        assertTrue(enumCustom is ExtensibleEnumeration.Custom)
        assertEquals("custom-clinic-dialect-xyz", enumCustom.code)
    }

    /**
     * Tests building document reference content with extensible language.
     */
    @Test
    fun testBuildDocumentReferenceContentWithLanguage() {
        val contentResult = buildDocumentReferenceContent("image/jpeg", "photos/test.jpg", "en")
        assertTrue(contentResult.isSuccess)
        val contentList = contentResult.getOrThrow()
        assertEquals(1, contentList.size)
        val item = contentList[0]
        assertEquals("image/jpeg", item.attachment.contentType?.value)
        assertEquals("photos/test.jpg", item.attachment.url?.value)
        assertNotNull(item.attachment.language)
        assertTrue(item.attachment.language is ExtensibleEnumeration.Predefined)
    }

    /**
     * Tests building document reference context with encounter and answer code.
     */
    @Test
    fun testBuildDocumentReferenceContext() {
        val contextResult = buildDocumentReferenceContext("enc-123", "ans-456")
        assertTrue(contextResult.isSuccess)
        val context = contextResult.getOrThrow()
        assertEquals(
            "enc-123",
            context.encounter
                .firstOrNull()
                ?.reference
                ?.value,
        )
        assertEquals(
            "ans-456",
            context.related
                .firstOrNull()
                ?.identifier
                ?.value
                ?.value,
        )
        val contextNoAns = buildDocumentReferenceContext("enc-no-ans").getOrThrow()
        assertEquals(
            "enc-no-ans",
            contextNoAns.encounter
                .firstOrNull()
                ?.reference
                ?.value,
        )
        assertTrue(contextNoAns.related.isEmpty())

        val contentNoLang = buildDocumentReferenceContent("image/png", "photos/no-lang.png").getOrThrow()
        assertEquals(1, contentNoLang.size)
        kotlin.test.assertNull(contentNoLang[0].attachment.language)

        val contentBlankLang = buildDocumentReferenceContent("image/png", "photos/blank.png", "   ").getOrThrow()
        assertEquals(1, contentBlankLang.size)
        kotlin.test.assertNull(contentBlankLang[0].attachment.language)
    }

    /**
     * Tests building clinical note content and LOINC type concept.
     */
    @Test
    fun testBuildClinicalNoteContentAndType() {
        val noteContent = buildClinicalNoteContent("Patient is recovering well.", "es").getOrThrow()
        assertEquals(1, noteContent.size)
        assertEquals("text/plain", noteContent[0].attachment.contentType?.value)
        assertEquals("data:text/plain;charset=utf-8,Patient is recovering well.", noteContent[0].attachment.url?.value)

        val noteContentNoLang = buildClinicalNoteContent("Raw text note").getOrThrow()
        assertEquals(1, noteContentNoLang.size)
        kotlin.test.assertNull(noteContentNoLang[0].attachment.language)

        val noteBlankLang = buildClinicalNoteContent("Raw text note", "   ").getOrThrow()
        assertEquals(1, noteBlankLang.size)
        kotlin.test.assertNull(noteBlankLang[0].attachment.language)

        val noteType = buildClinicalNoteType().getOrThrow()
        assertEquals(1, noteType.coding.size)
        assertEquals("11488-4", noteType.coding[0].code?.value)
        assertEquals("Consultation note", noteType.coding[0].display?.value)
    }
}
