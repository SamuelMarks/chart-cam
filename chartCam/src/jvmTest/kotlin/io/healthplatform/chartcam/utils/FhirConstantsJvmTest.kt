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
        val instance = FhirConstants
        assertEquals("application/fhir+json", instance.CONTENT_TYPE_FHIR_JSON)
        assertEquals("application/fhir+json", FhirConstants.CONTENT_TYPE_FHIR_JSON)
        assertEquals(true, FhirConstants.isFhirJsonContentType("application/fhir+json"))
        assertEquals(false, FhirConstants.isFhirJsonContentType("application/json"))
    }
}
