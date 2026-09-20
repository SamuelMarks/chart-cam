/**
 * @file FhirJsonParser.kt
 * Centralized multiplatform JSON serializer for FHIR R4 resources.
 *
 * Provides type-safe, polymorphic serialization and deserialization for FHIR resources
 * using standard kotlinx.serialization and ResourcePolymorphicSerializer.
 */

package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Resource
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Multiplatform JSON serializer for FHIR R4 resources.
 * Wraps kotlinx.serialization JSON engine configured for clinical FHIR payloads.
 */
object FhirJsonParser {
    /**
     * Compact [Json] instance for high-throughput database persistence
     * and offline wire transfer without whitespace overhead.
     */
    val compactJson: Json =
        Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            isLenient = true
        }

    /**
     * Formatted [Json] instance for human-readable diagnostic exports and debug inspections.
     */
    val prettyJson: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }

    /**
     * Default [Json] instance for encoding and decoding FHIR payloads, defaulting to [compactJson].
     */
    val json: Json
        get() = compactJson

    /**
     * Encodes a FHIR resource to a compact JSON string suitable for local persistence.
     *
     * @param resource The FHIR [Resource] to serialize.
     * @return A [Result] containing the compact JSON string, or a failure if serialization fails.
     */
    fun encodeResource(resource: Resource): Result<String> =
        runCatching {
            compactJson.encodeToString<Resource>(resource)
        }

    /**
     * Encodes a FHIR resource to a pretty-printed, human-readable JSON string.
     *
     * @param resource The FHIR [Resource] to serialize.
     * @return A [Result] containing the formatted JSON string, or a failure if serialization fails.
     */
    fun encodeResourcePretty(resource: Resource): Result<String> =
        runCatching {
            prettyJson.encodeToString<Resource>(resource)
        }

    /**
     * Encodes a typed FHIR resource using its specific serializer in compact format.
     *
     * @param T The concrete FHIR resource type.
     * @param serializer The serializer for type [T].
     * @param resource The resource instance to encode.
     * @return A [Result] containing the compact JSON string, or a failure if serialization fails.
     */
    fun <T : Resource> encodeTypedResource(
        serializer: KSerializer<T>,
        resource: T,
    ): Result<String> =
        runCatching {
            compactJson.encodeToString(serializer, resource)
        }

    /**
     * Encodes a typed FHIR resource using its specific serializer in formatted, human-readable format.
     *
     * @param T The concrete FHIR resource type.
     * @param serializer The serializer for type [T].
     * @param resource The resource instance to encode.
     * @return A [Result] containing the formatted JSON string, or a failure if serialization fails.
     */
    fun <T : Resource> encodeTypedResourcePretty(
        serializer: KSerializer<T>,
        resource: T,
    ): Result<String> =
        runCatching {
            prettyJson.encodeToString(serializer, resource)
        }

    /**
     * Decodes a JSON string into a specific FHIR resource type.
     *
     * @param T The concrete FHIR resource type.
     * @param serializer The serializer for type [T].
     * @param jsonString The raw JSON payload to decode.
     * @return A [Result] containing the decoded resource, or a failure if deserialization fails.
     */
    fun <T : Resource> decodeTypedResource(
        serializer: KSerializer<T>,
        jsonString: String,
    ): Result<T> =
        runCatching {
            compactJson.decodeFromString(serializer, jsonString)
        }

    /**
     * Decodes a JSON string into a specific FHIR resource type using reified serialization.
     *
     * @param T The concrete FHIR resource type.
     * @param jsonString The raw JSON payload to decode.
     * @return A [Result] containing the decoded resource, or a failure if deserialization fails.
     */
    inline fun <reified T : Resource> decodeFromStringCatching(jsonString: String): Result<T> =
        runCatching {
            compactJson.decodeFromString<T>(jsonString)
        }

    /**
     * Encodes a typed FHIR resource using reified serialization in compact format.
     *
     * @param T The concrete FHIR resource type.
     * @param resource The resource instance to encode.
     * @return A [Result] containing the compact JSON string, or a failure if serialization fails.
     */
    inline fun <reified T : Resource> encodeToStringCatching(resource: T): Result<String> =
        runCatching {
            compactJson.encodeToString<T>(resource)
        }

    /**
     * Encodes a typed FHIR resource using reified serialization in pretty-printed format.
     *
     * @param T The concrete FHIR resource type.
     * @param resource The resource instance to encode.
     * @return A [Result] containing the formatted JSON string, or a failure if serialization fails.
     */
    inline fun <reified T : Resource> encodeToStringPrettyCatching(resource: T): Result<String> =
        runCatching {
            prettyJson.encodeToString<T>(resource)
        }

    /**
     * Decodes a JSON string polymorphically into a generic FHIR [Resource].
     * Utilizes the `resourceType` discriminator to instantiate the correct resource subclass.
     *
     * @param jsonString The raw JSON string representing a FHIR resource.
     * @return A [Result] containing the deserialized [Resource], or a failure if deserialization fails.
     */
    fun decodeAnyResource(jsonString: String): Result<Resource> =
        runCatching {
            compactJson.decodeFromString<Resource>(jsonString)
        }

    /**
     * Decodes a JSON string polymorphically into a generic FHIR R4 [Resource].
     *
     * @param jsonString The raw JSON string representing an R4 resource.
     * @return A [Result] containing the deserialized R4 [Resource].
     */
    fun decodeR4Resource(jsonString: String): Result<dev.ohs.fhir.model.r4.Resource> =
        runCatching {
            compactJson.decodeFromString<dev.ohs.fhir.model.r4.Resource>(jsonString)
        }

    /**
     * Decodes a JSON string polymorphically into a generic FHIR R4B [dev.ohs.fhir.model.r4b.Resource].
     *
     * @param jsonString The raw JSON string representing an R4B resource.
     * @return A [Result] containing the deserialized R4B [dev.ohs.fhir.model.r4b.Resource].
     */
    fun decodeR4BResource(jsonString: String): Result<dev.ohs.fhir.model.r4b.Resource> =
        runCatching {
            compactJson.decodeFromString<dev.ohs.fhir.model.r4b.Resource>(jsonString)
        }

    /**
     * Decodes a JSON string polymorphically into a generic FHIR R5 [dev.ohs.fhir.model.r5.Resource].
     *
     * @param jsonString The raw JSON string representing an R5 resource.
     * @return A [Result] containing the deserialized R5 [dev.ohs.fhir.model.r5.Resource].
     */
    fun decodeR5Resource(jsonString: String): Result<dev.ohs.fhir.model.r5.Resource> =
        runCatching {
            compactJson.decodeFromString<dev.ohs.fhir.model.r5.Resource>(jsonString)
        }
}
