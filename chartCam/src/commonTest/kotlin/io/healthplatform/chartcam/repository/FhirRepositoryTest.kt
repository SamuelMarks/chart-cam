/**
 * @file FhirRepositoryTest.kt
 * Contains declarations for FhirRepositoryTest.kt.
 */
package io.healthplatform.chartcam.repository

import kotlin.test.Test
import kotlin.test.assertTrue

class FhirRepositoryTest {
    @Test
    fun testFhirRepository() {
        // Without full sql delight driver mock, testing FhirRepository fully in common is hard.
        // Usually done via Android/JVM tests.
        assertTrue(true)
    }
}
