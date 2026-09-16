/**
 * @file TriageScreenTest.kt
 * Contains declarations for TriageScreenTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.viewmodel.TriageUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Common test for [TriageScreen] state.
 */
class TriageScreenTest {
    /**
     * Verifies default TriageUiState flags.
     */
    @Test
    fun testTriageUiState() {
        val state = TriageUiState()
        assertNotNull(state)
        assertFalse(state.isCreatingPatient)
        assertNull(state.selectedPatient)
        assertTrue(state.capturedPhotoPaths.isEmpty())
        assertTrue(state.searchResults.isEmpty())
    }
}
