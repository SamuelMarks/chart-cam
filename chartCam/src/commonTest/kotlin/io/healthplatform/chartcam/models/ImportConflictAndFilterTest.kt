/**
 * @file ImportConflictAndFilterTest.kt
 * Unit tests verifying ImportConflict and ImportFilter models, enums, strategies, and options.
 */

package io.healthplatform.chartcam.models

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test suite for [ConflictType], [ConflictResolutionStrategy], [PatientStagingItem],
 * [ImportPreviewSummary], [ImportCategory], and [ImportFilterOptions].
 */
class ImportConflictAndFilterTest {
    /**
     * Verifies all entries of ConflictType and ConflictResolutionStrategy enums.
     */
    @Test
    fun testConflictEnums() {
        val types = ConflictType.entries
        assertTrue(types.contains(ConflictType.EXACT_MATCH))
        assertTrue(types.contains(ConflictType.ID_COLLISION_DIFFERENT_DATA))
        assertTrue(types.contains(ConflictType.MRN_COLLISION_DIFFERENT_ID))
        assertTrue(types.contains(ConflictType.ORPHAN_ENCOUNTER))
        assertEquals(ConflictType.EXACT_MATCH, ConflictType.valueOf("EXACT_MATCH"))

        val strategies = ConflictResolutionStrategy.entries
        assertTrue(strategies.contains(ConflictResolutionStrategy.OVERWRITE_LOCAL))
        assertTrue(strategies.contains(ConflictResolutionStrategy.KEEP_LOCAL))
        assertTrue(strategies.contains(ConflictResolutionStrategy.MERGE_RECORDS))
        assertTrue(strategies.contains(ConflictResolutionStrategy.CREATE_AS_NEW_ID))
        assertEquals(ConflictResolutionStrategy.KEEP_LOCAL, ConflictResolutionStrategy.valueOf("KEEP_LOCAL"))
    }

    /**
     * Verifies PatientStagingItem instantiation, defaults, copy, equality, and destructuring.
     */
    @Test
    fun testPatientStagingItem() {
        val patient = createFhirPatient("p-1", "John", "Doe", LocalDate(1980, 1, 1), "MRN-101", gender = "male")

        val itemDefault = PatientStagingItem(incomingPatient = patient)
        assertEquals(patient, itemDefault.incomingPatient)
        assertEquals(ConflictType.EXACT_MATCH, itemDefault.conflictType)
        assertNull(itemDefault.conflictingLocalPatient)
        assertEquals(0, itemDefault.encounterCount)
        assertTrue(itemDefault.isSelected)
        assertEquals(ConflictResolutionStrategy.KEEP_LOCAL, itemDefault.resolutionStrategy)

        val localPatient = createFhirPatient("p-2", "Jane", "Doe", LocalDate(1985, 2, 2), "MRN-102", gender = "female")

        val itemCustom =
            PatientStagingItem(
                incomingPatient = patient,
                conflictType = ConflictType.ID_COLLISION_DIFFERENT_DATA,
                conflictingLocalPatient = localPatient,
                encounterCount = 3,
                isSelected = false,
                resolutionStrategy = ConflictResolutionStrategy.MERGE_RECORDS,
            )

        assertEquals(ConflictType.ID_COLLISION_DIFFERENT_DATA, itemCustom.conflictType)
        assertEquals(localPatient, itemCustom.conflictingLocalPatient)
        assertEquals(3, itemCustom.encounterCount)
        assertFalse(itemCustom.isSelected)
        assertEquals(ConflictResolutionStrategy.MERGE_RECORDS, itemCustom.resolutionStrategy)

        val (inc, cType, confLocal, encCnt, sel, strat) = itemCustom
        assertEquals(patient, inc)
        assertEquals(ConflictType.ID_COLLISION_DIFFERENT_DATA, cType)
        assertEquals(localPatient, confLocal)
        assertEquals(3, encCnt)
        assertFalse(sel)
        assertEquals(ConflictResolutionStrategy.MERGE_RECORDS, strat)

        val copied = itemCustom.copy(isSelected = true)
        assertTrue(copied.isSelected)
        assertNotEquals(itemCustom, copied)
        assertEquals(itemCustom, itemCustom)
        assertNotEquals(itemCustom, itemDefault)
        assertFalse(itemCustom.equals(null))
        assertFalse(itemCustom.equals("string"))
        assertEquals(itemCustom.hashCode(), itemCustom.hashCode())
        assertTrue(itemCustom.toString().contains("p-1"))
    }

    /**
     * Verifies ImportPreviewSummary properties, copy, equality, and destructuring.
     */
    @Test
    fun testImportPreviewSummary() {
        val patient = createFhirPatient("p-10", "Alice", "Smith", LocalDate(1992, 5, 10), "MRN-200", gender = "female")
        val stagingItem = PatientStagingItem(incomingPatient = patient)

        val summary =
            ImportPreviewSummary(
                totalResources = 10,
                stagedPatients = listOf(stagingItem),
                stagedEncounterCount = 2,
                stagedPhotoCount = 3,
                stagedFormCount = 1,
                hasConflicts = false,
            )

        assertEquals(10, summary.totalResources)
        assertEquals(1, summary.stagedPatients.size)
        assertEquals(2, summary.stagedEncounterCount)
        assertEquals(3, summary.stagedPhotoCount)
        assertEquals(1, summary.stagedFormCount)
        assertFalse(summary.hasConflicts)

        val (tot, pts, encs, photos, forms, conf) = summary
        assertEquals(10, tot)
        assertEquals(1, pts.size)
        assertEquals(2, encs)
        assertEquals(3, photos)
        assertEquals(1, forms)
        assertFalse(conf)

        val copied = summary.copy(hasConflicts = true)
        assertTrue(copied.hasConflicts)
        assertNotEquals(summary, copied)
        assertEquals(summary, summary)
        assertFalse(summary.equals(null))
        assertFalse(summary.equals("other"))
        assertEquals(summary.hashCode(), summary.hashCode())
        assertTrue(summary.toString().contains("totalResources=10"))
    }

    /**
     * Verifies ImportCategory and ImportFilterOptions behaviour.
     */
    @Test
    fun testImportFilterOptions() {
        val categories = ImportCategory.entries
        assertEquals(6, categories.size)
        assertTrue(categories.contains(ImportCategory.PRACTITIONERS))
        assertTrue(categories.contains(ImportCategory.PATIENTS))
        assertTrue(categories.contains(ImportCategory.ENCOUNTERS))
        assertTrue(categories.contains(ImportCategory.CLINICAL_NOTES))
        assertTrue(categories.contains(ImportCategory.BINARY_PHOTOS))
        assertTrue(categories.contains(ImportCategory.QUESTIONNAIRES))
        assertEquals(ImportCategory.PATIENTS, ImportCategory.valueOf("PATIENTS"))

        val allOptions = ImportFilterOptions.all()
        assertEquals(6, allOptions.enabledCategories.size)
        assertNull(allOptions.targetPractitionerId)
        assertTrue(allOptions.isCategoryEnabled(ImportCategory.PATIENTS))
        assertTrue(allOptions.isCategoryEnabled(ImportCategory.QUESTIONNAIRES))

        val scopedOptions =
            ImportFilterOptions(
                enabledCategories = setOf(ImportCategory.PATIENTS, ImportCategory.ENCOUNTERS),
                targetPractitionerId = "practitioner-42",
            )
        assertEquals(2, scopedOptions.enabledCategories.size)
        assertEquals("practitioner-42", scopedOptions.targetPractitionerId)
        assertTrue(scopedOptions.isCategoryEnabled(ImportCategory.PATIENTS))
        assertTrue(scopedOptions.isCategoryEnabled(ImportCategory.ENCOUNTERS))
        assertFalse(scopedOptions.isCategoryEnabled(ImportCategory.BINARY_PHOTOS))

        val (cats, practId) = scopedOptions
        assertEquals(2, cats.size)
        assertEquals("practitioner-42", practId)

        val copied = scopedOptions.copy(targetPractitionerId = null)
        assertNull(copied.targetPractitionerId)
        assertNotEquals(scopedOptions, copied)
        assertEquals(scopedOptions, scopedOptions)
        assertFalse(scopedOptions.equals(null))
        assertFalse(scopedOptions.equals(42))
        assertEquals(scopedOptions.hashCode(), scopedOptions.hashCode())
        assertTrue(scopedOptions.toString().contains("practitioner-42"))
    }
}
