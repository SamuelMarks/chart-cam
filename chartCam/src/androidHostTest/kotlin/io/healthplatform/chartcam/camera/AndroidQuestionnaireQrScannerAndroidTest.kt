/**
 * @file AndroidQuestionnaireQrScannerAndroidTest.kt
 * Contains declarations for AndroidQuestionnaireQrScannerAndroidTest.kt.
 */
package io.healthplatform.chartcam.camera

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for AndroidQuestionnaireQrScanner on Android host.
 */
class AndroidQuestionnaireQrScannerAndroidTest {
    /**
     * Tests questionnaire QR scanner instantiation and execution on Android.
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
