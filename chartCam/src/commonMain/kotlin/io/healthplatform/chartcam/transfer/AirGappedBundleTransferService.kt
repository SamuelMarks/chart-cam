/**
 * @file AirGappedBundleTransferService.kt
 * Service for chunking, transferring, and assembling FHIR bundles
 * across air-gapped devices via animated QR codes or offline files.
 */

package io.healthplatform.chartcam.transfer

import dev.ohs.fhir.model.r4.Bundle
import io.healthplatform.chartcam.fhir.FhirBundleOrchestrator
import io.healthplatform.chartcam.fhir.FhirJsonParser
import io.healthplatform.chartcam.fhir.FhirProtobufParser
import io.healthplatform.chartcam.repository.FhirRepository
import io.healthplatform.chartcam.utils.flatMap
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

private const val INITIAL_HASH_SEED = 1125899906842597L
private const val HASH_MULTIPLIER = 31L

/**
 * Service facilitating decentralized offline transfer of FHIR bundles between air-gapped devices.
 */
object AirGappedBundleTransferService {
    private const val PREFIX = "CHARTCAM_PART:"
    private const val PROTO_PREFIX = "CHARTCAM_PROTO_PART:"
    private const val CHUNK_PARTS_COUNT = 4

    /**
     * Serializes a FHIR [Bundle] to binary Protobuf, encodes to Base64, and splits into QR chunks.
     *
     * @param bundle The FHIR [Bundle] to chunk.
     * @param maxChunkSize Maximum character payload per chunk.
     * @return A [Result] enclosing the list of QR chunk strings.
     */
    fun chunkProtobufBundleForQr(
        bundle: Bundle,
        maxChunkSize: Int = 800,
    ): Result<List<String>> =
        runCatching {
            val protoBytes = FhirProtobufParser.encodeToProtobuf(bundle).getOrDefault(ByteArray(0))
            val base64Payload = protoBytes.toByteString().base64()
            val totalLength = base64Payload.length
            val numChunks = (totalLength + maxChunkSize - 1) / maxChunkSize
            val checksum = calculateSimpleChecksum(base64Payload)

            (0 until numChunks).map { index ->
                val start = index * maxChunkSize
                val end = minOf(start + maxChunkSize, totalLength)
                val partData = base64Payload.substring(start, end)
                "$PROTO_PREFIX$index:$numChunks:$checksum:$partData"
            }
        }

    /**
     * Assembles a collection of scanned Protobuf QR chunk strings into the original [Bundle].
     *
     * @param chunks The received chunk strings (order does not matter).
     * @return A [Result] enclosing the reassembled and decoded [Bundle].
     */
    fun assembleQrProtobufChunks(chunks: List<String>): Result<Bundle> =
        runCatching {
            if (chunks.isEmpty()) error("Chunk list cannot be empty")
            val parsed =
                chunks.map { chunk ->
                    if (!chunk.startsWith(PROTO_PREFIX)) error("Invalid chunk prefix: $chunk")
                    val body = chunk.removePrefix(PROTO_PREFIX)
                    val parts = body.split(":", limit = CHUNK_PARTS_COUNT)
                    if (parts.size != CHUNK_PARTS_COUNT) error("Malformed chunk structure: $chunk")
                    val index = parts[0].toIntOrNull() ?: error("Invalid index: ${parts[0]}")
                    val total = parts[1].toIntOrNull() ?: error("Invalid total: ${parts[1]}")
                    val checksum = parts[2].toLongOrNull() ?: error("Invalid checksum: ${parts[2]}")
                    val data = parts[3]
                    ChunkMeta(index, total, checksum, data)
                }

            val total = parsed.first().total
            val checksum = parsed.first().checksum
            if (parsed.size < total) {
                error("Incomplete chunk set: received ${parsed.size} of $total")
            }

            val sorted = parsed.distinctBy { it.index }.sortedBy { it.index }
            if (sorted.size != total) {
                error("Missing chunks: expected $total unique indices, found ${sorted.size}")
            }

            val reassembled = sorted.joinToString("") { it.data }
            val reassembledChecksum = calculateSimpleChecksum(reassembled)
            if (reassembledChecksum != checksum) {
                error("Checksum mismatch: expected $checksum but calculated $reassembledChecksum")
            }

            val decodedBase64 =
                reassembled.decodeBase64()
                    ?: error("Failed to decode base64 protobuf payload")
            decodedBase64.toByteArray()
        }.flatMap { rawBytes ->
            FhirProtobufParser.decodeFromProtobuf<Bundle>(rawBytes)
        }

    /**
     * Ingests a binary Protobuf bundle payload directly into the local repository offline.
     *
     * @param repository The local FHIR repository.
     * @param bytes The raw Protobuf byte array.
     * @param isLocalChange Whether to record sync change tracking.
     * @return A [Result] enclosing the count of ingested resources.
     */
    suspend fun importProtobufBundlePayload(
        repository: FhirRepository,
        bytes: ByteArray,
        isLocalChange: Boolean = true,
    ): Result<Int> {
        val bundleResult = FhirProtobufParser.decodeFromProtobuf<Bundle>(bytes)
        val bundle = bundleResult.getOrElse { return Result.failure(it) }
        return FhirBundleOrchestrator.ingestBundle(repository, bundle, isLocalChange)
    }

    /**
     * Splits a serialized FHIR bundle into structured QR chunk payloads.
     *
     * @param bundleJson The serialized FHIR bundle JSON string.
     * @param maxChunkSize Maximum character payload per chunk.
     * @return A [Result] enclosing the ordered list of QR chunk strings.
     */
    fun chunkBundleForQr(
        bundleJson: String,
        maxChunkSize: Int = 800,
    ): Result<List<String>> =
        runCatching {
            if (bundleJson.isBlank()) error("Bundle payload cannot be blank")
            val totalLength = bundleJson.length
            val numChunks = (totalLength + maxChunkSize - 1) / maxChunkSize
            val checksum = calculateSimpleChecksum(bundleJson)

            (0 until numChunks).map { index ->
                val start = index * maxChunkSize
                val end = minOf(start + maxChunkSize, totalLength)
                val partData = bundleJson.substring(start, end)
                "$PREFIX$index:$numChunks:$checksum:$partData"
            }
        }

    /**
     * Assembles a collection of scanned QR chunk strings into the original serialized bundle JSON.
     *
     * @param chunks The received chunk strings (order does not matter).
     * @return A [Result] enclosing the reassembled JSON string.
     */
    fun assembleQrChunks(chunks: List<String>): Result<String> =
        runCatching {
            if (chunks.isEmpty()) error("Chunk list cannot be empty")
            val parsed =
                chunks.map { chunk ->
                    if (!chunk.startsWith(PREFIX)) error("Invalid chunk prefix: $chunk")
                    val body = chunk.removePrefix(PREFIX)
                    val parts = body.split(":", limit = CHUNK_PARTS_COUNT)
                    if (parts.size != CHUNK_PARTS_COUNT) error("Malformed chunk structure: $chunk")
                    val index = parts[0].toIntOrNull() ?: error("Invalid index: ${parts[0]}")
                    val total = parts[1].toIntOrNull() ?: error("Invalid total: ${parts[1]}")
                    val checksum = parts[2].toLongOrNull() ?: error("Invalid checksum: ${parts[2]}")
                    val data = parts[3]
                    ChunkMeta(index, total, checksum, data)
                }

            val total = parsed.first().total
            val checksum = parsed.first().checksum
            if (parsed.size < total) {
                error("Incomplete chunk set: received ${parsed.size} of $total")
            }

            val sorted = parsed.distinctBy { it.index }.sortedBy { it.index }
            if (sorted.size != total) {
                error("Missing chunks: expected $total unique indices, found ${sorted.size}")
            }

            val reassembled = sorted.joinToString("") { it.data }
            val reassembledChecksum = calculateSimpleChecksum(reassembled)
            if (reassembledChecksum != checksum) {
                error("Checksum mismatch: expected $checksum but calculated $reassembledChecksum")
            }

            reassembled
        }

    /**
     * Ingests a serialized bundle payload directly into the local repository offline.
     *
     * @param repository The local FHIR repository.
     * @param bundleJson The raw JSON bundle string.
     * @param isLocalChange Whether to record sync change tracking.
     * @return A [Result] enclosing the count of ingested resources.
     */
    suspend fun importBundlePayload(
        repository: FhirRepository,
        bundleJson: String,
        isLocalChange: Boolean = true,
    ): Result<Int> {
        val bundleResult = FhirJsonParser.decodeFromStringCatching<Bundle>(bundleJson)
        val bundle = bundleResult.getOrElse { return Result.failure(it) }
        return FhirBundleOrchestrator.ingestBundle(repository, bundle, isLocalChange)
    }

    /**
     * Calculates a deterministic CRC-like 64-bit checksum for integrity validation.
     *
     * @param input The raw input string.
     * @return A 64-bit checksum value.
     */
    fun calculateSimpleChecksumForTesting(input: String): Long = calculateSimpleChecksum(input)

    /**
     * Calculates a deterministic CRC-like 64-bit checksum for integrity validation.
     *
     * @param input The raw input string.
     * @return A 64-bit checksum value.
     */
    private fun calculateSimpleChecksum(input: String): Long {
        var hash = INITIAL_HASH_SEED
        for (ch in input) {
            hash = HASH_MULTIPLIER * hash + ch.code.toLong()
        }
        return hash
    }

    /**
     * Internal metadata for a single payload chunk.
     *
     * @property index The zero-based chunk index.
     * @property total The total number of expected chunks.
     * @property checksum The checksum value for the chunk.
     * @property data The base64 payload slice contained in this chunk.
     */
    private data class ChunkMeta(
        val index: Int,
        val total: Int,
        val checksum: Long,
        val data: String,
    )
}
