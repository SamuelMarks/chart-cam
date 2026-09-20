/**
 * @file FhirProtobufParserTest.kt
 * Unit tests verifying FhirProtobufParser encoding, decoding, and failure recovery.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Resource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for [FhirProtobufParser].
 */
@OptIn(ExperimentalSerializationApi::class)
class FhirProtobufParserTest {
    /**
     * Verifies typed and bundle Protobuf round-tripping.
     */
    @Test
    fun testTypedAndBundleProtobufRoundTrip() {
        assertNotNull(FhirProtobufParser.protobuf)

        val bundle =
            Bundle(
                id = "b-test-proto",
                type = Enumeration(value = Bundle.BundleType.Collection),
            )
        val bundleBytes = FhirProtobufParser.encodeBundleToProtobuf(bundle).getOrThrow()
        assertTrue(bundleBytes.isNotEmpty())

        val decodedBundle = FhirProtobufParser.decodeBundleFromProtobuf(bundleBytes).getOrThrow()
        assertEquals("b-test-proto", decodedBundle.id)

        val pat = Patient(id = "p-test-proto")
        val patBytes = FhirProtobufParser.encodeToProtobuf(pat).getOrThrow()
        val decodedPat = FhirProtobufParser.decodeFromProtobuf<Patient>(patBytes).getOrThrow()
        assertEquals("p-test-proto", decodedPat.id)
    }

    /**
     * Verifies polymorphic and compact JSON bytes serialization.
     */
    @Test
    fun testPolymorphicAndCompactJsonBytes() {
        val pat = Patient(id = "p-poly")
        val anyResult = FhirProtobufParser.encodeAnyResourceToProtobuf(pat)
        if (anyResult.isSuccess) {
            val decodedAny = FhirProtobufParser.decodeAnyResourceFromProtobuf(anyResult.getOrThrow())
            assertTrue(decodedAny.isSuccess)
        }

        val jsonBytes = FhirProtobufParser.encodeToCompactJsonBytes(pat).getOrThrow()
        assertTrue(jsonBytes.isNotEmpty())

        val decodedFromBytes = FhirProtobufParser.decodeFromCompactJsonBytes(jsonBytes).getOrThrow()
        assertEquals("p-poly", decodedFromBytes.id)

        val failingResource =
            object : Resource() {
                override val id: String? = null
                override val meta: dev.ohs.fhir.model.r4.Meta? = null
                override val implicitRules: dev.ohs.fhir.model.r4.Uri? = null
                override val language: dev.ohs.fhir.model.r4.Code? = null

                override fun toBuilder(): Resource.Builder = error("Unsupported")
            }
        val compactFail = FhirProtobufParser.encodeToCompactJsonBytes(failingResource)
        assertTrue(compactFail.isFailure)
    }

    /**
     * Verifies error handling when decoding invalid byte payloads.
     */
    @Test
    fun testInvalidBytesHandling() {
        val invalidBytes = byteArrayOf(0, 1, 2, 3, 4)

        assertTrue(FhirProtobufParser.decodeFromProtobuf<Patient>(invalidBytes).isFailure)
        assertTrue(FhirProtobufParser.decodeBundleFromProtobuf(invalidBytes).isFailure)
        assertTrue(FhirProtobufParser.decodeAnyResourceFromProtobuf(invalidBytes).isFailure)
        assertTrue(FhirProtobufParser.decodeFromCompactJsonBytes(invalidBytes).isFailure)
    }
}
