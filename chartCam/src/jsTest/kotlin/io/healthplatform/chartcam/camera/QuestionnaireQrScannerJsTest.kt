/**
 * @file QuestionnaireQrScannerJsTest.kt
 * Contains declarations for QuestionnaireQrScannerJsTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for QuestionnaireQrScanner on JS.
 */
class QuestionnaireQrScannerJsTest {
    /**
     * Test scanner instantiation and scan invocation on JS.
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
