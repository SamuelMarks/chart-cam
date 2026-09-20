/**
 * @file JvmQuestionnaireQrScannerTest.kt
 * Tests for JvmQuestionnaireQrScanner on Desktop JVM.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit test suite verifying JvmQuestionnaireQrScanner behavior.
 */
class JvmQuestionnaireQrScannerTest {
    /**
     * Verifies that JvmQuestionnaireQrScanner initializes and returns failure Result when no QR frame is present.
     */
    @Test
    fun testJvmQuestionnaireQrScannerScan() =
        runTest {
            val scanner = JvmQuestionnaireQrScanner()
            assertNotNull(scanner)
            val result = scanner.scanQuestionnaireQrCode()
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        }

    /**
     * Verifies that createQuestionnaireQrScanner on JVM returns a JvmQuestionnaireQrScanner.
     */
    @Test
    fun testCreateQuestionnaireQrScannerJvm() =
        runTest {
            val scanner = createQuestionnaireQrScanner()
            assertTrue(scanner is JvmQuestionnaireQrScanner)
        }
}
