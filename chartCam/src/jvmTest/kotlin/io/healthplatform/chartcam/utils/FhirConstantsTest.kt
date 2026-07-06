package io.healthplatform.chartcam.utils

import org.junit.Test
import kotlin.test.assertEquals

class FhirConstantsTest {
    @Test
    fun testFhirConstants() {
        assertEquals("application/fhir+json", FhirConstants.ContentTypeFhirJson.toString())
    }
}
