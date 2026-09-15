/**
 * @file DicomReaderTest.kt
 * Tests for DicomReader parsing DICOM Part 10 files.
 */
package io.healthplatform.chartcam.dicom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests parsing of DICOM Part 10 byte payloads into DicomDataset.
 */
class DicomReaderTest {
    /**
     * Tests parsing a Visible Light DICOM file.
     */
    @Test
    fun testReadVisibleLightDicom() {
        val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val dcmBytes = FhirToDicomMapper.createVisibleLightImageDicom(jpegBytes).getOrThrow()

        val readResult = DicomReader.read(dcmBytes)
        assertTrue(readResult.isSuccess)

        val dataset = readResult.getOrNull()
        assertNotNull(dataset)
        assertEquals("XC", dataset.modality)
        assertEquals(DicomTag.UID_JPEG_BASELINE, dataset.transferSyntaxUid)
        assertNotNull(dataset.pixelData)
    }

    /**
     * Tests parsing an Encapsulated PDF DICOM file.
     */
    @Test
    fun testReadEncapsulatedPdfDicom() {
        val pdfBytes = "%PDF-1.4 sample content".encodeToByteArray()
        val dcmBytes = FhirToDicomMapper.createEncapsulatedPdfDicom(pdfBytes, title = "Clinical Summary").getOrThrow()

        val readResult = DicomReader.read(dcmBytes)
        assertTrue(readResult.isSuccess)

        val dataset = readResult.getOrNull()
        assertNotNull(dataset)
        assertEquals("DOC", dataset.modality)
        assertNotNull(dataset.encapsulatedPdf)
    }

    /**
     * Tests invalid byte streams fail parsing gracefully.
     */
    @Test
    fun testReadInvalidDicomBytes() {
        val invalidBytes = byteArrayOf(1, 2, 3, 4, 5)
        val readResult = DicomReader.read(invalidBytes)
        assertTrue(readResult.isFailure)

        val randomBytes = ByteArray(140)
        val readRandomResult = DicomReader.read(randomBytes)
        assertTrue(readRandomResult.isFailure)
    }

    /**
     * Tests cleanJpegPayload with various valid and invalid inputs.
     */
    @Test
    fun testCleanJpegPayload() {
        val nullResult = DicomReader.cleanJpegPayload(null)
        assertTrue(nullResult.isFailure)

        val emptyResult = DicomReader.cleanJpegPayload(ByteArray(0))
        assertTrue(emptyResult.isFailure)

        val validJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02)
        val validResult = DicomReader.cleanJpegPayload(validJpeg)
        assertTrue(validResult.isSuccess)

        val offsetJpeg = byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0xD8.toByte(), 0x55)
        val offsetResult = DicomReader.cleanJpegPayload(offsetJpeg)
        assertTrue(offsetResult.isSuccess)
        assertEquals(3, offsetResult.getOrNull()?.size)

        val invalidPayload = byteArrayOf(0x10, 0x20, 0x30, 0x40)
        val invalidResult = DicomReader.cleanJpegPayload(invalidPayload)
        assertTrue(invalidResult.isFailure)

        val singleFf = byteArrayOf(0xFF.toByte())
        assertTrue(DicomReader.cleanJpegPayload(singleFf).isFailure)

        val ffZero = byteArrayOf(0xFF.toByte(), 0x00)
        assertTrue(DicomReader.cleanJpegPayload(ffZero).isFailure)
    }

    /**
     * Tests undefined length item parsing with delimiter tag 0xFFFE, 0xE00D.
     */
    @Test
    fun testUndefinedLengthItemSequence() {
        val buffer = okio.Buffer()
        // Preamble 128 bytes + DICM
        buffer.write(ByteArray(128))
        buffer.writeUtf8("DICM")

        // Pixel data tag (7FE0, 0010), VR OB (extended), undefined length (-1)
        buffer.writeShortLe(0x7FE0)
        buffer.writeShortLe(0x0010)
        buffer.writeUtf8("OB")
        buffer.writeShortLe(0) // reserved
        buffer.writeIntLe(-1) // undefined length

        // Item 1: undefined length item
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(-1) // undefined length item
        buffer.writeByte(0xFF)
        buffer.writeByte(0xD8)
        buffer.writeByte(0xAA)
        // Item delimiter tag (FFFE, E00D) with length 0
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE00D)
        buffer.writeIntLe(0)

        // Sequence delimiter tag (FFFE, E0DD) with length 0
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE0DD)
        buffer.writeIntLe(0)

        val dcmBytes = buffer.readByteArray()
        val res = DicomReader.read(dcmBytes)
        assertTrue(res.isSuccess)
        assertNotNull(res.getOrNull()?.pixelData)
    }

    /**
     * Tests basic offset table and multi-fragment sequence.
     */
    @Test
    fun testBasicOffsetTableSequence() {
        val buffer = okio.Buffer()
        buffer.write(ByteArray(128))
        buffer.writeUtf8("DICM")

        buffer.writeShortLe(0x7FE0)
        buffer.writeShortLe(0x0010)
        buffer.writeUtf8("OB")
        buffer.writeShortLe(0)
        buffer.writeIntLe(-1)

        // Item 1: non-JPEG Basic Offset Table (isFirstItem = true, isJpg = false)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(4)
        buffer.writeIntLe(0)

        // Item 2: non-JPEG Fragment (isFirstItem = false, isJpg = false, current == null)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(2)
        buffer.writeByte(0x10)
        buffer.writeByte(0x20)

        // Item 3: JPEG Fragment Item (isFirstItem = false, isJpg = true, current != null)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(3)
        buffer.writeByte(0xFF)
        buffer.writeByte(0xD8)
        buffer.writeByte(0xAA)

        // Item 4: non-JPEG trailing fragment (isFirstItem = false, isJpg = false, current != null)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(2)
        buffer.writeByte(0x11)
        buffer.writeByte(0x22)

        // Sequence delimiter tag (FFFE, E0DD) with length 0
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE0DD)
        buffer.writeIntLe(0)

        val dcmBytes = buffer.readByteArray()
        val res = DicomReader.read(dcmBytes)
        assertTrue(res.isSuccess)
        assertNotNull(res.getOrNull()?.pixelData)
    }

    /**
     * Tests undefined length sequence containing empty items and skipped non-item elements.
     */
    @Test
    fun testUndefinedSequenceWithSkippedItemsAndZeroLength() {
        val buffer = okio.Buffer()
        buffer.write(ByteArray(128))
        buffer.writeUtf8("DICM")

        // Rows tag (0028, 0010), VR US, 2 bytes length -> 512
        buffer.writeShortLe(0x0028)
        buffer.writeShortLe(0x0010)
        buffer.writeUtf8("US")
        buffer.writeShortLe(2)
        buffer.writeShortLe(512)

        // Pixel data tag (7FE0, 0010), VR OB, undefined length
        buffer.writeShortLe(0x7FE0)
        buffer.writeShortLe(0x0010)
        buffer.writeUtf8("OB")
        buffer.writeShortLe(0)
        buffer.writeIntLe(-1)

        // Item 1: length 0
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(0)

        // Unknown non-item element inside sequence: (0000, 0000) length 4 -> will be skipped
        buffer.writeShortLe(0x0000)
        buffer.writeShortLe(0x0000)
        buffer.writeIntLe(4)
        buffer.writeIntLe(1234)

        // Item 2: length 4 with JPEG SOI
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(4)
        buffer.writeByte(0xFF)
        buffer.writeByte(0xD8)
        buffer.writeByte(0x11)
        buffer.writeByte(0x22)

        // Sequence delimiter tag (FFFE, E0DD) with length 0
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE0DD)
        buffer.writeIntLe(0)

        val dcmBytes = buffer.readByteArray()
        val res = DicomReader.read(dcmBytes)
        assertTrue(res.isSuccess)
        assertEquals(512, res.getOrNull()?.height)
        assertNotNull(res.getOrNull()?.pixelData)
    }

    /**
     * Tests unknown VR codes fallback and truncated element handling.
     */
    @Test
    fun testUnknownVrAndTruncatedHeaders() {
        val buffer = okio.Buffer()
        buffer.write(ByteArray(128))
        buffer.writeUtf8("DICM")

        // Unknown VR: tag (0010, 0010), VR "XX" (resolves to UN extended), 2 reserved + 4 length, "OK"
        buffer.writeShortLe(0x0010)
        buffer.writeShortLe(0x0010)
        buffer.writeUtf8("XX")
        buffer.writeShortLe(0) // reserved
        buffer.writeIntLe(2) // length
        buffer.writeUtf8("OK")

        // Element with buffer size less than declared length (truncated payload)
        buffer.writeShortLe(0x0010)
        buffer.writeShortLe(0x0020)
        buffer.writeUtf8("CS")
        buffer.writeShortLe(100) // declared 100 bytes
        buffer.writeUtf8("SHORT") // only 5 bytes written

        // Trailing 3 bytes (fewer than MIN_HEADER_BYTES = 6)
        buffer.writeByte(0xAA)
        buffer.writeByte(0xBB)
        buffer.writeByte(0xCC)

        val dcmBytes = buffer.readByteArray()
        val res = DicomReader.read(dcmBytes)
        assertTrue(res.isSuccess)
        assertEquals("OK", res.getOrNull()?.patientName)
    }

    /**
     * Tests truncated extended and standard VR length headers.
     */
    @Test
    fun testTruncatedVrHeaders() {
        // Extended VR (OB) with fewer than 6 bytes remaining for length
        val bufferExtended = okio.Buffer()
        bufferExtended.write(ByteArray(128))
        bufferExtended.writeUtf8("DICM")
        bufferExtended.writeShortLe(0x7FE0)
        bufferExtended.writeShortLe(0x0010)
        bufferExtended.writeUtf8("OB")
        bufferExtended.writeByte(0x00) // only 1 reserved byte instead of 2 reserved + 4 len

        val resExt = DicomReader.read(bufferExtended.readByteArray())
        assertTrue(resExt.isSuccess)

        // Standard VR (CS) with fewer than 2 bytes remaining for length
        val bufferStd = okio.Buffer()
        bufferStd.write(ByteArray(128))
        bufferStd.writeUtf8("DICM")
        bufferStd.writeShortLe(0x0010)
        bufferStd.writeShortLe(0x0010)
        bufferStd.writeUtf8("CS")
        bufferStd.writeByte(0x01) // only 1 length byte

        val resStd = DicomReader.read(bufferStd.readByteArray())
        assertTrue(resStd.isSuccess)
    }

    /**
     * Tests undefined sequence ending prematurely without delimiter.
     */
    @Test
    fun testPrematureUndefinedSequence() {
        val buffer = okio.Buffer()
        buffer.write(ByteArray(128))
        buffer.writeUtf8("DICM")
        buffer.writeShortLe(0x7FE0)
        buffer.writeShortLe(0x0010)
        buffer.writeUtf8("OB")
        buffer.writeShortLe(0)
        buffer.writeIntLe(-1)
        // Trailing 4 bytes inside sequence (fewer than ITEM_HEADER_BYTES = 8)
        buffer.writeIntLe(1234)

        val res = DicomReader.read(buffer.readByteArray())
        assertTrue(res.isSuccess)
    }

    /**
     * Tests short dimension byte array returning 0.
     */
    @Test
    fun testShortDimensionTag() {
        val buf = okio.Buffer()
        buf.write(ByteArray(128))
        buf.writeUtf8("DICM")
        buf.writeShortLe(0x0028)
        buf.writeShortLe(0x0010)
        buf.writeUtf8("US")
        buf.writeShortLe(1)
        buf.writeByte(0x05)
        val dcm = buf.readByteArray()
        val r = DicomReader.read(dcm)
        assertTrue(r.isSuccess)
        assertEquals(0, r.getOrNull()?.height)
    }

    /**
     * Tests deep sequence branch edge cases including zero-length non-item tags and non-tag groups.
     */
    @Test
    fun testDeepUndefinedSequenceBranches() {
        val buffer = okio.Buffer()
        buffer.write(ByteArray(128))
        buffer.writeUtf8("DICM")

        buffer.writeShortLe(0x7FE0)
        buffer.writeShortLe(0x0010)
        buffer.writeUtf8("OB")
        buffer.writeShortLe(0)
        buffer.writeIntLe(-1)

        // Non-item tag with group 0xFFFE but element 0x5555 (covers itemGroup == ITEM_TAG_GROUP && itemElem != ITEM_TAG_ELEMENT)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0x5555)
        buffer.writeIntLe(2)
        buffer.writeShortLe(0x1111)

        // Non-item tag group (0x1234, 0x5678) with itemLen = 0 (covers else-if itemLen <= 0 branch)
        buffer.writeShortLe(0x1234)
        buffer.writeShortLe(0x5678)
        buffer.writeIntLe(0)

        // Undefined item with intermediate non-delimiter bytes (covers g != ITEM_TAG_GROUP and g == ITEM_TAG_GROUP && e != DELIM)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(-1)
        // False delimiter: 0xFFFE, 0x5555
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0x5555)
        buffer.writeIntLe(0)
        // Real nested item delimiter (FFFE, E00D)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE00D)
        buffer.writeIntLe(0)

        // Undefined item that ends abruptly without delimiter (covers buffer.size < ITEM_HEADER_BYTES)
        buffer.writeShortLe(0xFFFE)
        buffer.writeShortLe(0xE000)
        buffer.writeIntLe(-1)
        buffer.writeByte(0x42) // only 1 byte left in buffer

        val res = DicomReader.read(buffer.readByteArray())
        assertTrue(res.isSuccess)
    }
}
