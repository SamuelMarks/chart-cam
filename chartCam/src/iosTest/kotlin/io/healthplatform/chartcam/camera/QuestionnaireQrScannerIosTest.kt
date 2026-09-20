/**
 * @file QuestionnaireQrScannerIosTest.kt
 * Contains declarations for QuestionnaireQrScannerIosTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for QuestionnaireQrScanner on iOS.
 */
class QuestionnaireQrScannerIosTest {
    /**
     * Tests scanner creation and invocation on iOS.
     */
    @Test
    fun testScannerInvocation() =
        runTest {
            val scanner = createQuestionnaireQrScanner()
            assertNotNull(scanner)
            val res = scanner.scanQuestionnaireQrCode()
            assertTrue(res.isFailure)
        }
}
