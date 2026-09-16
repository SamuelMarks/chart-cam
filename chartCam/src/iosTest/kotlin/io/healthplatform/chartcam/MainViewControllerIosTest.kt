/**
 * @file MainViewControllerIosTest.kt
 * Contains declarations for MainViewControllerIosTest.kt.
 */
package io.healthplatform.chartcam

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Platform-specific tests for MainViewController on iOS.
 */
class MainViewControllerIosTest {
    /**
     * Verifies MainViewController instantiation on iOS.
     */
    @Test
    fun testMainViewController() {
        val vc = mainViewController()
        assertNotNull(vc)
    }
}
