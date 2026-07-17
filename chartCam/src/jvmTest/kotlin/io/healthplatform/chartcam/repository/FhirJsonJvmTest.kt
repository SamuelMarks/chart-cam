/**
 * @file FhirJsonJvmTest.kt
 * Contains declarations for FhirJsonJvmTest.kt.
 *
 * Ensures serialization functionality works for the FHIR resource representations.
 *
 * Acts as a sanity check to verify that underlying JSON serialization library mappings
 * for complicated external class hierarchies map and serialize as expected.
 */
package io.healthplatform.chartcam.repository

import com.google.fhir.model.r4.FhirR4Json
import com.google.fhir.model.r4.Patient
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests JSON parsing compatibility for R4 FHIR Models.
 *
 * Verifies encode-decode round tripping behaviors against basic FHIR model structures.
 */
class FhirJsonJvmTest {
    /**
     * Executes a serialization-deserialization round trip test.
     *
     * Populates a [Patient] class, serializes it to a string using [FhirR4Json],
     * deserializes it, and asserts identity matches the initial payload.
     */
    @Test
    fun testJson() {
        val fhirJson = FhirR4Json()
        val p =
            Patient
                .Builder()
                .apply {
                    id = "123"
                }.build()
        val str = fhirJson.encodeToString(p)
        val decoded = fhirJson.decodeFromString(str) as Patient
        assertEquals("123", decoded.id)
    }
}
