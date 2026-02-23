package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Patient
import kotlin.test.Test
import kotlin.test.assertEquals

class FhirJsonTest {
    @Test
    fun testJson() {
        val fhirJson = FhirR4Json()
        val p = Patient.Builder().apply { 
            id = "123"
        }.build()
        val str = fhirJson.encodeToString(p)
        val decoded = fhirJson.decodeFromString(str) as Patient
        assertEquals("123", decoded.id)
    }
}
