/**
 * @file FhirConstantsJvmTest.kt
 * Contains declarations for FhirConstantsJvmTest.kt.
 */
package io.healthplatform.chartcam.utils

import org.junit.Test
import kotlin.test.assertEquals

class FhirConstantsJvmTest {
    @Test
    fun testFhirConstantsJvm() {
        assertEquals("application/fhir+json", FhirConstants.ContentTypeFhirJson.toString())
    }
}
