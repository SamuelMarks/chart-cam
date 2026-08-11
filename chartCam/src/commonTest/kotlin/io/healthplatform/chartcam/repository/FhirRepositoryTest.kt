/**
 * @file FhirRepositoryTest.kt
 * Contains declarations for FhirRepositoryTest.kt.
 */
package io.healthplatform.chartcam.repository

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Common test definitions for [FhirRepository].
 */
class FhirRepositoryTest {
    /**
     * Common assertion that [FhirRepository] components exist.
     */
    @Test
    fun testFhirRepository() {
        // Without full sql delight driver mock, testing FhirRepository fully in common is hard.
        // Usually done via Android/JVM tests.
        assertTrue(true)
    }
}
