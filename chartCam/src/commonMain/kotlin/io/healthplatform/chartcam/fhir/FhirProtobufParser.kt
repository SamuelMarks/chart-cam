/**
 * @file FhirProtobufParser.kt
 * Pure multiplatform Protobuf serializer and deserializer for FHIR R4 resources.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Resource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * High-density binary serializer for FHIR R4 resources using Kotlinx Protobuf.
 *
 * Provides extreme payload compaction for air-gapped transfers (animated QR codes, offline files,
 * and peer-to-peer transmissions) without relying on network or remote FHIR infrastructure.
 */
@OptIn(ExperimentalSerializationApi::class)
object FhirProtobufParser {
    /**
     * Reusable [ProtoBuf] serializer instance configured for clinical FHIR payloads.
     */
    val protobuf: ProtoBuf =
        ProtoBuf {
            encodeDefaults = true
        }

    /**
     * Serializes a typed FHIR [Resource] into a compact Protobuf byte array.
     *
     * @param T The concrete FHIR resource type.
     * @param resource The FHIR resource to serialize.
     * @return A [Result] enclosing the serialized Protobuf byte array.
     */
    inline fun <reified T : Resource> encodeToProtobuf(resource: T): Result<ByteArray> =
        runCatching {
            protobuf.encodeToByteArray<T>(resource)
        }

    /**
     * Polymorphically serializes a generic FHIR [Resource] into a compact Protobuf byte array.
     *
     * @param resource The generic FHIR [Resource] to serialize.
     * @return A [Result] enclosing the serialized Protobuf byte array.
     */
    fun encodeAnyResourceToProtobuf(resource: Resource): Result<ByteArray> =
        runCatching {
            protobuf.encodeToByteArray<Resource>(resource)
        }

    /**
     * Deserializes a Protobuf byte array into a typed FHIR [Resource].
     *
     * @param T The concrete FHIR [Resource] type to decode.
     * @param bytes The raw Protobuf byte array.
     * @return A [Result] enclosing the decoded resource instance.
     */
    inline fun <reified T : Resource> decodeFromProtobuf(bytes: ByteArray): Result<T> =
        runCatching {
            protobuf.decodeFromByteArray<T>(bytes)
        }

    /**
     * Serializes a FHIR [dev.ohs.fhir.model.r4.Bundle] into a deterministic, compact Protobuf byte array.
     *
     * Standardizes air-gapped peer-to-peer transmissions across JVM, Android, iOS, and Web.
     *
     * @param bundle The Bundle resource to serialize.
     * @return A [Result] enclosing the Protobuf bytes.
     */
    fun encodeBundleToProtobuf(bundle: dev.ohs.fhir.model.r4.Bundle): Result<ByteArray> =
        encodeToProtobuf(bundle)

    /**
     * Deserializes a Protobuf byte array into a typed FHIR [dev.ohs.fhir.model.r4.Bundle].
     *
     * @param bytes The raw Protobuf byte array.
     * @return A [Result] enclosing the decoded [dev.ohs.fhir.model.r4.Bundle].
     */
    fun decodeBundleFromProtobuf(bytes: ByteArray): Result<dev.ohs.fhir.model.r4.Bundle> =
        decodeFromProtobuf(bytes)

    /**
     * Polymorphically deserializes a Protobuf byte array into a generic FHIR [Resource].
     *
     * @param bytes The raw Protobuf byte array.
     * @return A [Result] enclosing the decoded generic [Resource].
     */
    fun decodeAnyResourceFromProtobuf(bytes: ByteArray): Result<Resource> =
        runCatching {
            protobuf.decodeFromByteArray<Resource>(bytes)
        }

    /**
     * Serializes a generic FHIR [Resource] into minified UTF-8 byte array as a fallback mechanism.
     *
     * @param resource The FHIR [Resource] to serialize.
     * @return A [Result] enclosing the UTF-8 encoded compact JSON bytes.
     */
    fun encodeToCompactJsonBytes(resource: Resource): Result<ByteArray> =
        FhirJsonParser.encodeResource(resource).map { it.encodeToByteArray() }

    /**
     * Deserializes a minified UTF-8 byte array into a generic FHIR [Resource].
     *
     * @param bytes The raw JSON byte array.
     * @return A [Result] enclosing the decoded generic [Resource].
     */
    fun decodeFromCompactJsonBytes(bytes: ByteArray): Result<Resource> =
        FhirJsonParser.decodeAnyResource(bytes.decodeToString())
}
