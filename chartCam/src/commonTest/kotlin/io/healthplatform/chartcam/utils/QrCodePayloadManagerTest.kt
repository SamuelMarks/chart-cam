/**
 * @file QrCodePayloadManagerTest.kt
 * Contains declarations for QrCodePayloadManagerTest.kt.
 */
package io.healthplatform.chartcam.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for QR code payload chunking, reassembly, and matrix generation.
 */
class QrCodePayloadManagerTest {
    /**
     * Verifies splitting payloads into single and multiple chunks.
     */
    @Test
    fun testChunking() {
        val emptyChunks = QrCodePayloadManager.splitIntoChunks("")
        assertEquals(1, emptyChunks.size)
        assertEquals("1/1:", emptyChunks[0])

        val payload = "A".repeat(50)
        val chunks = QrCodePayloadManager.splitIntoChunks(payload, maxChunkSize = 20)
        assertEquals(3, chunks.size)
        assertEquals("1/3:" + "A".repeat(20), chunks[0])
        assertEquals("2/3:" + "A".repeat(20), chunks[1])
        assertEquals("3/3:" + "A".repeat(10), chunks[2])
    }

    /**
     * Verifies reassembling chunks in order and out of order.
     */
    @Test
    fun testReassembly() {
        val reassembler = QrChunkReassembler()
        val payload = "Hello Decentralized Clinical FHIR Questionnaire"
        val chunks = QrCodePayloadManager.splitIntoChunks(payload, maxChunkSize = 10)

        // Process first chunk
        val r1 = reassembler.processChunk(chunks[0])
        assertTrue(r1.isSuccess)
        assertNull(r1.getOrNull())

        // Process remaining chunks in reverse order (out of order arrival)
        for (i in (chunks.size - 1) downTo 1) {
            val res = reassembler.processChunk(chunks[i])
            assertTrue(res.isSuccess)
            if (i == 1) {
                assertEquals(payload, res.getOrNull())
            } else {
                assertNull(res.getOrNull())
            }
        }
    }

    /**
     * Verifies error handling for malformed chunk headers.
     */
    @Test
    fun testReassemblyErrors() {
        val reassembler = QrChunkReassembler()

        // Missing colon
        assertTrue(reassembler.processChunk("1/2").isFailure)

        // Invalid parts count
        assertTrue(reassembler.processChunk("1/2/3:data").isFailure)

        // Non-numeric index
        assertTrue(reassembler.processChunk("abc/2:data").isFailure)

        // Non-numeric total
        assertTrue(reassembler.processChunk("1/xyz:data").isFailure)

        // Out of bounds index or total
        assertTrue(reassembler.processChunk("0/2:data").isFailure)
        assertTrue(reassembler.processChunk("1/0:data").isFailure)
        assertTrue(reassembler.processChunk("3/2:data").isFailure)

        // Total mismatch
        assertTrue(reassembler.processChunk("1/2:first").isSuccess)
        assertTrue(reassembler.processChunk("2/3:mismatch").isFailure)

        // Progress check
        val progress = reassembler.getProgress()
        assertNotNull(progress)

        // Reset
        reassembler.reset()
        assertEquals(0 to 0, reassembler.getProgress())
    }

    /**
     * Verifies QR matrix generation with content and with empty data.
     */
    @Test
    fun testMatrixGeneration() {
        val matrixResult = QrCodePayloadManager.generateQrMatrix("TestFHIRPayload123")
        assertTrue(matrixResult.isSuccess)
        val matrix = matrixResult.getOrNull()
        assertNotNull(matrix)
        assertEquals(25, matrix.size)
        assertEquals(25, matrix[0].size)

        // Top-left finder corner (0,0) must be true (black)
        assertTrue(matrix[0][0])
        // Top-left inner center (3,3) must be true (black)
        assertTrue(matrix[3][3])
        // Top-left finder border (1,1) must be false (white)
        assertTrue(!matrix[1][1])

        // Empty data matrix generation
        val emptyMatrixResult = QrCodePayloadManager.generateQrMatrix("")
        assertTrue(emptyMatrixResult.isSuccess)
    }
}
