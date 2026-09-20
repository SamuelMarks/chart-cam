/**
 * @file TestExpressionTest.kt
 * Contains declarations for TestExpressionTest.kt.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Common tests for SDC expressions.
 */
class TestExpressionTest {
    /**
     * Verifies calculated expression evaluation logic.
     */
    @Test
    fun testExp() {
        val q =
            Questionnaire
                .Builder(Enumeration(value = PublicationStatus.Active))
                .apply { id = "test-q" }
                .build()
        val answers = mapOf("val1" to 10, "val2" to 20)
        val result = SdcEvaluator.evaluateCalculatedExpressions(q, answers)
        assertEquals(10, result["val1"])
        assertEquals(20, result["val2"])
    }
}
