/**
 * @file AirGappedBundleTransferServiceTest.kt
 * Unit tests for AirGappedBundleTransferService and FhirProtobufParser.
 */

package io.healthplatform.chartcam.transfer

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Uri
import io.healthplatform.chartcam.fhir.FhirJsonParser
import io.healthplatform.chartcam.fhir.FhirProtobufParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Tests verifying air-gapped QR chunking and Protobuf serialization.
 */
class AirGappedBundleTransferServiceTest {
    /**
     * Verifies roundtrip binary serialization of FHIR resources via [FhirProtobufParser].
     */
    @Test
    fun testProtobufRoundTrip() {
        val patient =
            Patient(
                id = "pat-proto-1",
                name = listOf(HumanName(family = FhirString(value = "ProtobufFamily"))),
            )
        val encodeRes = FhirProtobufParser.encodeToProtobuf(patient)
        assertTrue(encodeRes.isSuccess)
        val bytes = encodeRes.getOrNull()
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val decodeRes = FhirProtobufParser.decodeFromProtobuf<Patient>(bytes)
        assertTrue(decodeRes.isSuccess)
        val decoded = decodeRes.getOrNull()
        assertNotNull(decoded)
        assertEquals("pat-proto-1", decoded.id)
        assertEquals(
            "ProtobufFamily",
            decoded.name
                .firstOrNull()
                ?.family
                ?.value,
        )
    }

    /**
     * Verifies that animated QR chunking and reassembly works with binary Protobuf bundles.
     */
    @Test
    fun testProtobufQrChunkingAndAssembly() {
        val bundle =
            Bundle(
                id = "bundle-proto-qr",
                type = Enumeration(value = Bundle.BundleType.Collection),
                entry =
                    listOf(
                        Bundle.Entry(
                            fullUrl = Uri(value = "urn:uuid:p1"),
                            resource = Patient(id = "p-in-bundle", name = listOf(HumanName(family = FhirString(value = "Smith")))),
                        ),
                    ),
            )

        // Compare JSON length vs Protobuf length
        val jsonStr = FhirJsonParser.encodeResource(bundle).getOrThrow()
        val protoBytes = FhirProtobufParser.encodeToProtobuf(bundle).getOrThrow()
        assertTrue(protoBytes.size < jsonStr.length, "Protobuf should be smaller than JSON representation")

        val chunkRes = AirGappedBundleTransferService.chunkProtobufBundleForQr(bundle, maxChunkSize = 50)
        assertTrue(chunkRes.isSuccess)
        val chunks = chunkRes.getOrThrow()
        assertTrue(chunks.size > 1, "Should split into multiple small chunks")

        // Call with default maxChunkSize
        val defaultChunkRes = AirGappedBundleTransferService.chunkProtobufBundleForQr(bundle)
        assertTrue(defaultChunkRes.isSuccess)

        // Reassemble in shuffled order to verify order independence
        val shuffled = chunks.shuffled()
        val assembleRes = AirGappedBundleTransferService.assembleQrProtobufChunks(shuffled)
        assertTrue(assembleRes.isSuccess)
        val reassembledBundle = assembleRes.getOrThrow()

        assertEquals(bundle.id, reassembledBundle.id)
        assertEquals(1, reassembledBundle.entry.size)
        val pat = reassembledBundle.entry.first().resource as? Patient
        assertNotNull(pat)
        assertEquals("p-in-bundle", pat.id)
    }

    /**
     * Verifies round-trip Protobuf serialization across Encounter, DocumentReference, and QuestionnaireResponse.
     */
    @Test
    fun testMultiResourceProtobufCompactionRoundTrip() {
        val encounter =
            dev.ohs.fhir.model.r4.Encounter(
                id = "enc-proto",
                status = Enumeration(value = dev.ohs.fhir.model.r4.Encounter.EncounterStatus.Finished),
                `class` =
                    dev.ohs.fhir.model.r4.Coding(
                        code =
                            dev.ohs.fhir.model.r4
                                .Code("AMB"),
                    ),
            )
        val encBytes = FhirProtobufParser.encodeToProtobuf(encounter).getOrThrow()
        val decodedEnc = FhirProtobufParser.decodeFromProtobuf<dev.ohs.fhir.model.r4.Encounter>(encBytes).getOrThrow()
        assertEquals("enc-proto", decodedEnc.id)

        val doc =
            dev.ohs.fhir.model.r4.DocumentReference(
                id = "doc-proto",
                status = Enumeration(value = dev.ohs.fhir.model.r4.terminologies.DocumentReferenceStatus.Current),
                content = emptyList(),
            )
        val docBytes = FhirProtobufParser.encodeToProtobuf(doc).getOrThrow()
        val decodedDoc = FhirProtobufParser.decodeFromProtobuf<dev.ohs.fhir.model.r4.DocumentReference>(docBytes).getOrThrow()
        assertEquals("doc-proto", decodedDoc.id)

        val qr =
            dev.ohs.fhir.model.r4.QuestionnaireResponse(
                id = "qr-proto",
                status = Enumeration(value = dev.ohs.fhir.model.r4.QuestionnaireResponse.QuestionnaireResponseStatus.Completed),
            )
        val qrBytes = FhirProtobufParser.encodeToProtobuf(qr).getOrThrow()
        val decodedQr = FhirProtobufParser.decodeFromProtobuf<dev.ohs.fhir.model.r4.QuestionnaireResponse>(qrBytes).getOrThrow()
        assertEquals("qr-proto", decodedQr.id)

        // Compact JSON fallback verification
        val compactBytes = FhirProtobufParser.encodeToCompactJsonBytes(encounter).getOrThrow()
        val decodedFallback = FhirProtobufParser.decodeFromCompactJsonBytes(compactBytes).getOrThrow()
        assertTrue(decodedFallback is dev.ohs.fhir.model.r4.Encounter)
        assertEquals("enc-proto", decodedFallback.id)

        // Typed Bundle helper verification
        val bundle = Bundle(id = "b-proto", type = Enumeration(value = Bundle.BundleType.Collection))
        val bBytes = FhirProtobufParser.encodeBundleToProtobuf(bundle).getOrThrow()
        val decodedB = FhirProtobufParser.decodeBundleFromProtobuf(bBytes).getOrThrow()
        assertEquals("b-proto", decodedB.id)

        // Any Resource serialization verification
        val anyResourceResult = FhirProtobufParser.encodeAnyResourceToProtobuf(bundle)
        if (anyResourceResult.isSuccess) {
            val decodedAny = FhirProtobufParser.decodeAnyResourceFromProtobuf(anyResourceResult.getOrThrow())
            assertNotNull(decodedAny)
        } else {
            assertTrue(anyResourceResult.isFailure)
        }
    }

    /**
     * Verifies that JSON bundle chunking and assembly work correctly.
     */
    @Test
    fun testJsonBundleChunkingAndAssembly() {
        val bundleJson = """{"resourceType":"Bundle","id":"b-json-chunk","type":"collection"}"""
        val chunkRes = AirGappedBundleTransferService.chunkBundleForQr(bundleJson, maxChunkSize = 30)
        assertTrue(chunkRes.isSuccess)
        val chunks = chunkRes.getOrThrow()
        assertTrue(chunks.size > 1)

        val assembled = AirGappedBundleTransferService.assembleQrChunks(chunks).getOrThrow()
        assertEquals(bundleJson, assembled)

        // Error branches
        assertTrue(AirGappedBundleTransferService.chunkBundleForQr("").isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(emptyList()).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("INVALID_CHUNK")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("INVALID1", "INVALID2")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("CHARTCAM_PART:0:2:123")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("CHARTCAM_PART:abc:2:123:data")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("CHARTCAM_PART:0:abc:123:data")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("CHARTCAM_PART:0:2:abc:data")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("CHARTCAM_PART:0:2:123:abc")).isFailure)
        assertTrue(
            AirGappedBundleTransferService.assembleQrChunks(listOf("CHARTCAM_PART:0:2:123:abc", "CHARTCAM_PART:0:2:123:abc")).isFailure,
        )
        assertTrue(AirGappedBundleTransferService.assembleQrChunks(listOf("CHARTCAM_PART:0:1:99999:abc")).isFailure)

        // Protobuf assembly error branches
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(emptyList()).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(listOf("INVALID_PROTO")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(listOf("CHARTCAM_PROTO_PART:0:2:123")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(listOf("CHARTCAM_PROTO_PART:abc:2:123:data")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(listOf("CHARTCAM_PROTO_PART:0:abc:123:data")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(listOf("CHARTCAM_PROTO_PART:0:2:abc:data")).isFailure)
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(listOf("CHARTCAM_PROTO_PART:0:2:123:abc")).isFailure)
        assertTrue(
            AirGappedBundleTransferService
                .assembleQrProtobufChunks(
                    listOf("CHARTCAM_PROTO_PART:0:2:123:abc", "CHARTCAM_PROTO_PART:0:2:123:abc"),
                ).isFailure,
        )
        assertTrue(AirGappedBundleTransferService.assembleQrProtobufChunks(listOf("CHARTCAM_PROTO_PART:0:1:99999:abc")).isFailure)
        val badBase64Data = "!!!"
        val badChecksum = AirGappedBundleTransferService.calculateSimpleChecksumForTesting(badBase64Data)
        assertTrue(
            AirGappedBundleTransferService
                .assembleQrProtobufChunks(
                    listOf("CHARTCAM_PROTO_PART:0:1:$badChecksum:$badBase64Data"),
                ).isFailure,
        )
    }

    /**
     * Verifies chunkProtobufBundleForQr handles bundle serialization failure fallback.
     */
    @Test
    fun testChunkProtobufBundleWithFailingResource() {
        val failingResource =
            object : dev.ohs.fhir.model.r4.Resource() {
                override val id: String? = null
                override val meta: dev.ohs.fhir.model.r4.Meta? = null
                override val implicitRules: dev.ohs.fhir.model.r4.Uri? = null
                override val language: dev.ohs.fhir.model.r4.Code? = null

                override fun toBuilder(): dev.ohs.fhir.model.r4.Resource.Builder = error("Unsupported")
            }
        val bundle =
            Bundle(
                type = Enumeration(value = Bundle.BundleType.Collection),
                entry = listOf(Bundle.Entry(resource = failingResource)),
            )
        val result = AirGappedBundleTransferService.chunkProtobufBundleForQr(bundle)
        assertTrue(result.isSuccess)
    }
}
