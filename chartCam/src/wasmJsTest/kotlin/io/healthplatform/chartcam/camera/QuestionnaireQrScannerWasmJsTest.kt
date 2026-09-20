/**
 * @file QuestionnaireQrScannerWasmJsTest.kt
 * Contains declarations for QuestionnaireQrScannerWasmJsTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for QuestionnaireQrScanner on WasmJS.
 */
class QuestionnaireQrScannerWasmJsTest {
    /**
     * Test scanner instantiation and scan invocation on WasmJS.
     */
    @Test
    fun testScannerInvocation() =
        runTest {
            val scanner = createQuestionnaireQrScanner()
            assertNotNull(scanner)
            val result = scanner.scanQuestionnaireQrCode()
            assertTrue(result.isFailure)
        }
}
