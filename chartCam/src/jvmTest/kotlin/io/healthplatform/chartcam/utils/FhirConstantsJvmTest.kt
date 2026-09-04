/**
 * @file FhirConstantsJvmTest.kt
 * Contains declarations for FhirConstantsJvmTest.kt.
 */
package io.healthplatform.chartcam.utils

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Test class for FhirConstants on JVM.
 */
class FhirConstantsJvmTest {
    /**
     * Tests FhirConstants on JVM.
     */
    @Test
    fun testFhirConstantsJvm() {
        assertEquals("application/fhir+json", FhirConstants.CONTENT_TYPE_FHIR_JSON)
    }
}
