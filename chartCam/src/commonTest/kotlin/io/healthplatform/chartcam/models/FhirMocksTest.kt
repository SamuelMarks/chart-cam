/**
 * @file FhirMocksTest.kt
 * Contains declarations for FhirMocksTest.kt.
 */
package io.healthplatform.chartcam.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for FHIR mock data logic.
 */
class FhirMocksTest {
    /**
     * Verifies construction of mock questionnaire resources with both explicit and default parameters.
     */
    @Test
    fun testMocks() {
        val q = FhirMocks.createMockQuestionnaire("q-test-1", "Test Questionnaire")
        assertNotNull(q)
        assertEquals("q-test-1", q.id)
        assertEquals("Test Questionnaire", q.title?.value)

        val qDefault = FhirMocks.createMockQuestionnaire()
        assertNotNull(qDefault)
        assertEquals("mock-questionnaire-1", qDefault.id)
        assertEquals("Mock Questionnaire", qDefault.title?.value)

        val qrDefault = FhirMocks.createMockQuestionnaireResponse()
        assertNotNull(qrDefault)
        assertEquals("mock-response-1", qrDefault.id)
        assertEquals("Questionnaire/mock-questionnaire-1", qrDefault.questionnaire?.value)

        val qrCustom = FhirMocks.createMockQuestionnaireResponse("custom-qr", "Questionnaire/custom-1")
        assertNotNull(qrCustom)
        assertEquals("custom-qr", qrCustom.id)
        assertEquals("Questionnaire/custom-1", qrCustom.questionnaire?.value)
    }
}
