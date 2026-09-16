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
     * Verifies construction of mock questionnaire resources.
     */
    @Test
    fun testMocks() {
        val q = FhirMocks.createMockQuestionnaire("q-test-1", "Test Questionnaire")
        assertNotNull(q)
        assertEquals("q-test-1", q.id)
        assertEquals("Test Questionnaire", q.title?.value)
    }
}
