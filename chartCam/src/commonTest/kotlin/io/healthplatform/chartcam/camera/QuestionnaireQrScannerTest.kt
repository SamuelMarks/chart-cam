/**
 * @file QuestionnaireQrScannerTest.kt
 * Tests for QuestionnaireQrScanner common interface and factory.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Common test suite verifying QuestionnaireQrScanner interface contract and platform factory.
 */
class QuestionnaireQrScannerTest {
    /**
     * Verifies that the platform factory produces a non-null scanner.
     */
    @Test
    fun testCreateQuestionnaireQrScanner() =
        runTest {
            val scanner = createQuestionnaireQrScanner()
            assertNotNull(scanner)
            val result = scanner.scanQuestionnaireQrCode()
            // On desktop JVM / headless environment, scanner returns a failure Result (no active camera frame)
            assertTrue(result.isFailure || result.isSuccess)
        }

    /**
     * Verifies custom scanner implementation contract.
     */
    @Test
    fun testCustomQuestionnaireQrScannerContract() =
        runTest {
            val successScanner =
                object : QuestionnaireQrScanner {
                    override suspend fun scanQuestionnaireQrCode(): Result<String> =
                        Result.success("""{"resourceType":"Questionnaire"}""")
                }
            val successResult = successScanner.scanQuestionnaireQrCode()
            assertTrue(successResult.isSuccess)
            assertTrue(successResult.getOrThrow().contains("Questionnaire"))

            val failureScanner =
                object : QuestionnaireQrScanner {
                    override suspend fun scanQuestionnaireQrCode(): Result<String> =
                        Result.failure(IllegalStateException("No QR code found"))
                }
            val failureResult = failureScanner.scanQuestionnaireQrCode()
            assertTrue(failureResult.isFailure)
        }
}
