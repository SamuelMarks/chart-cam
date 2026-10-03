/**
 * @file SilhouetteErrorTest.kt
 * Tests for SilhouetteError mapping and data classes.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SilhouetteError.
 */
class SilhouetteErrorTest {
    /**
     * Verifies mapping from Throwable to SilhouetteError.
     */
    @Test
    fun testToSilhouetteError() {
        val cornea = Exception("The cornea is not visible").toSilhouetteError()
        assertTrue(cornea is SilhouetteError.CorneaOccluded)
        assertEquals("The cornea is not visible", cornea.detail)

        val scale = Exception("Invalid scale detected").toSilhouetteError()
        assertTrue(scale is SilhouetteError.InvalidScale)
        assertEquals(0f, scale.scaleFactor)

        val calibration = Exception("calibration failed").toSilhouetteError()
        assertTrue(calibration is SilhouetteError.CalibrationFailed)
        assertEquals("calibration failed", calibration.reason)

        val unknown = Exception("random code").toSilhouetteError()
        assertTrue(unknown is SilhouetteError.SilhouetteNotSupported)
        assertEquals("random code", unknown.code)
    }
}
