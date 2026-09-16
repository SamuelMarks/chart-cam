/**
 * @file EncounterDetailViewModelTest.kt
 * Contains declarations for EncounterDetailViewModelTest.kt.
 */
package io.healthplatform.chartcam.viewmodel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the [EncounterDetailViewModel].
 */
class EncounterDetailViewModelTest {
    /**
     * Verifies EncounterUiState construction and default fields.
     */
    @Test
    fun testEncounterUiStateDefaults() {
        val state = EncounterUiState()
        assertNotNull(state)
        assertTrue(state.isLoading)
        assertFalse(state.isFinalized)
        assertNull(state.patient)
        assertNull(state.encounter)
        assertTrue(state.photos.isEmpty())
        assertTrue(state.audioMemos.isEmpty())
    }
}
