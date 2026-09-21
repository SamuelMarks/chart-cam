/**
 * @file QrCodePayloadManager.kt
 * Contains declarations for QrCodePayloadManager.kt.
 *
 * Provides decentralized air-gapped QR code chunking, multi-part reconstruction,
 * and pure Kotlin matrix generation for offline clinical data exchange.
 */
package io.healthplatform.chartcam.utils

/**
 * Manages air-gapped chunking and reassembly of large data payloads for QR code exchange.
 */
@Suppress("MagicNumber")
object QrCodePayloadManager {
    /**
     * Splits an input string payload into sequential numbered chunks suitable for QR codes.
     *
     * @param payload The raw string payload to divide.
     * @param maxChunkSize The maximum character length per chunk body (excluding prefix).
     * @return A list of chunk strings formatted as "index/total:chunk_data".
     */
    fun splitIntoChunks(
        payload: String,
        maxChunkSize: Int = 400,
    ): List<String> {
        if (payload.isEmpty()) return listOf("1/1:")
        val effectiveSize = maxChunkSize.coerceAtLeast(1)
        val chunks = payload.chunked(effectiveSize)
        val total = chunks.size
        return chunks.mapIndexed { index, chunk ->
            "${index + 1}/$total:$chunk"
        }
    }

    /**
     * Generates a 2D boolean matrix representing a QR-style scannable pattern.
     * Includes finder patterns, timing patterns, and data bits.
     *
     * @param data The data string to encode into the matrix.
     * @return A [Result] enclosing the 2D boolean array (true = black, false = white).
     */
    fun generateQrMatrix(data: String): Result<Array<BooleanArray>> =
        runCatching {
            val bytes = data.encodeToByteArray()
            val baseSize = 25
            val matrix = Array(baseSize) { BooleanArray(baseSize) { false } }

            // 1. Draw Finder Patterns at three corners (Top-Left, Top-Right, Bottom-Left)
            drawFinderPattern(matrix, 0, 0)
            drawFinderPattern(matrix, baseSize - 7, 0)
            drawFinderPattern(matrix, 0, baseSize - 7)

            // 2. Draw Timing Patterns (alternating black/white on row 6 and column 6)
            for (i in 7 until (baseSize - 7)) {
                matrix[6][i] = (i % 2 == 0)
                matrix[i][6] = (i % 2 == 0)
            }

            // 3. Fill data bits deterministically into the available grid
            var byteIndex = 0
            var bitIndex = 0
            for (y in 0 until baseSize) {
                for (x in 0 until baseSize) {
                    // Skip finder and timing regions
                    if (isReservedRegion(x, y, baseSize)) continue

                    val bit =
                        if (bytes.isNotEmpty()) {
                            val currByte = bytes[byteIndex % bytes.size].toInt()
                            ((currByte ushr (7 - bitIndex)) and 1) == 1
                        } else {
                            false
                        }
                    matrix[y][x] = bit

                    bitIndex++
                    if (bitIndex >= 8) {
                        bitIndex = 0
                        byteIndex++
                    }
                }
            }
            matrix
        }

    /**
     * Draws a standard 7x7 finder pattern with a 3x3 inner square into the matrix.
     *
     * @param matrix The target 2D boolean matrix.
     * @param startX The top-left X coordinate of the 7x7 pattern.
     * @param startY The top-left Y coordinate of the 7x7 pattern.
     */
    private fun drawFinderPattern(
        matrix: Array<BooleanArray>,
        startX: Int,
        startY: Int,
    ) {
        for (y in 0 until 7) {
            for (x in 0 until 7) {
                val isBorder = x == 0 || x == 6 || y == 0 || y == 6
                val isCenter = x in 2..4 && y in 2..4
                matrix[startY + y][startX + x] = isBorder || isCenter
            }
        }
    }

    /**
     * Determines whether a given coordinate in the matrix is reserved for finder or timing patterns.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param size The total matrix width/height.
     * @return True if the cell is in a reserved region, false otherwise.
     */
    private fun isReservedRegion(
        x: Int,
        y: Int,
        size: Int,
    ): Boolean =
        (x < 8 && y < 8) ||
            (x >= size - 8 && y < 8) ||
            (x < 8 && y >= size - 8) ||
            (x == 6 || y == 6)
}

/**
 * State machine for reassembling multi-chunk QR code streams.
 */
class QrChunkReassembler {
    private val receivedChunks = mutableMapOf<Int, String>()
    private var expectedTotal: Int? = null

    /**
     * Parses and validates the "index/total" header from a raw chunk string.
     *
     * @param rawChunk The raw chunk string.
     * @return A [Result] enclosing (index, total) pair or failure.
     */
    private fun parseChunkHeader(rawChunk: String): Result<Pair<Int, Int>> {
        val colonIndex = rawChunk.indexOf(':')
        if (colonIndex <= 0) {
            return Result.failure(IllegalArgumentException("Invalid QR chunk header: missing colon separator"))
        }
        val header = rawChunk.substring(0, colonIndex)
        val parts = header.split('/')
        return when (parts.size) {
            2 -> {
                val idx = parts[0].toIntOrNull()
                val tot = parts[1].toIntOrNull()
                when {
                    idx == null -> Result.failure(IllegalArgumentException("Invalid chunk index: ${parts[0]}"))
                    tot == null -> Result.failure(IllegalArgumentException("Invalid chunk total: ${parts[1]}"))
                    idx < 1 || tot < 1 || idx > tot ->
                        Result.failure(IllegalArgumentException("Chunk index out of bounds: $idx of $tot"))
                    else -> Result.success(Pair(idx, tot))
                }
            }
            else -> Result.failure(IllegalArgumentException("Invalid QR chunk header format: expected index/total"))
        }
    }

    /**
     * Ingests a raw QR chunk and attempts reconstruction.
     *
     * @param rawChunk The chunk string formatted as "index/total:content".
     * @return A [Result] enclosing the full reconstructed payload if complete, null if awaiting more chunks,
     *         or a failure if the chunk format is invalid.
     */
    fun processChunk(rawChunk: String): Result<String?> {
        val parsed = parseChunkHeader(rawChunk)
        val failure =
            if (parsed.isFailure) {
                parsed.exceptionOrNull()
            } else {
                val (_, total) = parsed.getOrThrow()
                val currentTotal = expectedTotal
                if (currentTotal != null && currentTotal != total) {
                    IllegalStateException("Chunk total mismatch: expected $currentTotal but received $total")
                } else {
                    null
                }
            }

        if (failure != null) {
            return Result.failure(failure)
        }

        val (index, total) = parsed.getOrThrow()
        val colonIndex = rawChunk.indexOf(':')
        val content = rawChunk.substring(colonIndex + 1)
        expectedTotal = total
        receivedChunks[index] = content

        val reconstructed =
            if (receivedChunks.size == total) {
                val full = (1..total).joinToString("") { receivedChunks.getValue(it) }
                reset()
                full
            } else {
                null
            }
        return Result.success(reconstructed)
    }

    /**
     * Returns the current progress as a pair of (receivedChunksCount, totalChunksExpected).
     *
     * @return A [Pair] containing received count and total count.
     */
    fun getProgress(): Pair<Int, Int> = (receivedChunks.size) to (expectedTotal ?: 0)

    /**
     * Resets the reassembly buffer and state.
     */
    fun reset() {
        receivedChunks.clear()
        expectedTotal = null
    }
}
