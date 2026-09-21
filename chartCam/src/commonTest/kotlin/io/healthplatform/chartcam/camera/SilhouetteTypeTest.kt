/**
 * @file SilhouetteTypeTest.kt
 * Contains tests for SilhouetteType.kt and SilhouetteError.kt.
 */
package io.healthplatform.chartcam.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests verifying the behavior of silhouette types and error mappings.
 */
class SilhouetteTypeTest {
    @Test
    fun testSilhouetteTypeFromCode() {
        assertEquals(SilhouetteType.NONE, SilhouetteType.fromCode(null).getOrNull())
        assertEquals(SilhouetteType.NONE, SilhouetteType.fromCode("").getOrNull())
        assertEquals(SilhouetteType.NONE, SilhouetteType.fromCode("   ").getOrNull())
        assertEquals(SilhouetteType.NONE, SilhouetteType.fromCode("none").getOrNull())
        assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_LEFT, SilhouetteType.fromCode("profile-cornea-left").getOrNull())
        assertEquals(SilhouetteType.FRONTAL_FACE, SilhouetteType.fromCode("frontal-face").getOrNull())
        assertEquals(SilhouetteType.PROFILE_CORNEA_NOSE_RIGHT, SilhouetteType.fromCode("profile-cornea-right").getOrNull())

        val invalid = SilhouetteType.fromCode("unsupported-preset")
        assertTrue(invalid.isFailure)
        assertNotNull(invalid.exceptionOrNull())
    }

    @Test
    fun testSilhouetteGuideState() {
        val state =
            SilhouetteGuideState(
                scale = 1.2f,
                isLevel = true,
                pitch = 1.5f,
                roll = -0.5f,
                hasPreviousGhost = true,
            )
        assertEquals(1.2f, state.scale)
        assertTrue(state.isLevel)
        assertEquals(1.5f, state.pitch)
        assertEquals(-0.5f, state.roll)
        assertTrue(state.hasPreviousGhost)
    }

    @Test
    fun testSilhouetteErrorMapping() {
        val corneaEx = IllegalArgumentException("Cornea was obscured by hair")
        val corneaError = corneaEx.toSilhouetteError()
        assertTrue(corneaError is SilhouetteError.CorneaOccluded)
        assertEquals("Cornea was obscured by hair", (corneaError as? SilhouetteError.CorneaOccluded)?.detail)

        val scaleEx = IllegalStateException("Invalid scale factor computed")
        val scaleError = scaleEx.toSilhouetteError()
        assertTrue(scaleError is SilhouetteError.InvalidScale)

        val calibEx = RuntimeException("Sensor calibration unavailable")
        val calibError = calibEx.toSilhouetteError()
        assertTrue(calibError is SilhouetteError.CalibrationFailed)

        val genericEx = Exception("Some arbitrary error")
        val genericError = genericEx.toSilhouetteError()
        assertTrue(genericError is SilhouetteError.SilhouetteNotSupported)

        val nullMsgEx = Exception(null as String?)
        val nullMsgError = nullMsgEx.toSilhouetteError()
        assertTrue(nullMsgError is SilhouetteError.SilhouetteNotSupported)
        assertEquals("Unknown error", (nullMsgError as SilhouetteError.SilhouetteNotSupported).code)
    }
}
