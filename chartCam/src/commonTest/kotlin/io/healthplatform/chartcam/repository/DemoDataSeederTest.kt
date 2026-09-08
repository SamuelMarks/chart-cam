/**
 * @file DemoDataSeederTest.kt
 * Contains declarations for DemoDataSeederTest.kt.
 *
 * Unit tests validating synthetic demo data constants, IDs, and integrity.
 */
package io.healthplatform.chartcam.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the metadata, constant collections, and ID uniqueness of [DemoDataSeeder].
 */
class DemoDataSeederTest {
    /**
     * Verifies that demo patient, encounter, and QR ID lists are non-empty, unique, and matched.
     */
    @Test
    fun testDemoIdCollectionsIntegrity() {
        assertEquals(3, DemoDataSeeder.ALL_DEMO_PATIENT_IDS.size)
        assertEquals(3, DemoDataSeeder.ALL_DEMO_ENCOUNTER_IDS.size)
        assertEquals(3, DemoDataSeeder.ALL_DEMO_QR_IDS.size)

        // Ensure uniqueness
        assertEquals(3, DemoDataSeeder.ALL_DEMO_PATIENT_IDS.toSet().size)
        assertEquals(3, DemoDataSeeder.ALL_DEMO_ENCOUNTER_IDS.toSet().size)
        assertEquals(3, DemoDataSeeder.ALL_DEMO_QR_IDS.toSet().size)

        assertTrue(DemoDataSeeder.ALL_DEMO_PATIENT_IDS.contains(DemoDataSeeder.DEMO_PATIENT_PEDIATRIC_ID))
        assertTrue(DemoDataSeeder.ALL_DEMO_PATIENT_IDS.contains(DemoDataSeeder.DEMO_PATIENT_ADULT_ID))
        assertTrue(DemoDataSeeder.ALL_DEMO_PATIENT_IDS.contains(DemoDataSeeder.DEMO_PATIENT_GERIATRIC_ID))

        assertTrue(DemoDataSeeder.ALL_DEMO_ENCOUNTER_IDS.contains(DemoDataSeeder.DEMO_ENCOUNTER_PEDIATRIC_ID))
        assertTrue(DemoDataSeeder.ALL_DEMO_ENCOUNTER_IDS.contains(DemoDataSeeder.DEMO_ENCOUNTER_ADULT_ID))
        assertTrue(DemoDataSeeder.ALL_DEMO_ENCOUNTER_IDS.contains(DemoDataSeeder.DEMO_ENCOUNTER_GERIATRIC_ID))

        assertTrue(DemoDataSeeder.ALL_DEMO_QR_IDS.contains(DemoDataSeeder.DEMO_QR_PEDIATRIC_ID))
        assertTrue(DemoDataSeeder.ALL_DEMO_QR_IDS.contains(DemoDataSeeder.DEMO_QR_ADULT_ID))
        assertTrue(DemoDataSeeder.ALL_DEMO_QR_IDS.contains(DemoDataSeeder.DEMO_QR_GERIATRIC_ID))
    }
}
