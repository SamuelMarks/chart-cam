/**
 * @file EncounterDetailScreenTest.kt
 * Contains declarations for EncounterDetailScreenTest.kt.
 */
package io.healthplatform.chartcam.ui

import io.healthplatform.chartcam.viewmodel.EncounterUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Common test for [EncounterDetailScreen] state.
 */
class EncounterDetailScreenTest {
    /**
     * Verifies default EncounterUiState properties.
     */
    @Test
    fun testEncounterUiState() {
        val state = EncounterUiState()
        assertNotNull(state)
        assertTrue(state.isLoading)
        assertFalse(state.isFinalized)
        assertNull(state.encounter)
        assertTrue(state.photos.isEmpty())
        assertTrue(state.audioMemos.isEmpty())
    }
}
