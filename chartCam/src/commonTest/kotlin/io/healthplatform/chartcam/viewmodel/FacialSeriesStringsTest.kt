package io.healthplatform.chartcam.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Unit tests for [FacialSeriesStrings].
 * Ensures all data class properties and generated methods (like equals, hashCode, copy) are covered.
 */
class FacialSeriesStringsTest {
    @Test
    fun testDefaultValues() {
        val defaultStrings = FacialSeriesStrings()
        assertEquals("Left Profile (Cornea & Nose)", defaultStrings.leftProfile)
        assertEquals("Front View", defaultStrings.frontView)
        assertEquals("Right Profile (Cornea & Nose)", defaultStrings.rightProfile)
    }

    @Test
    fun testCustomValues() {
        val customStrings =
            FacialSeriesStrings(
                leftProfile = "Left",
                frontView = "Front",
                rightProfile = "Right",
            )
        assertEquals("Left", customStrings.leftProfile)
        assertEquals("Front", customStrings.frontView)
        assertEquals("Right", customStrings.rightProfile)
    }

    @Test
    fun testCopy() {
        val original = FacialSeriesStrings()
        val copied = original.copy(frontView = "New Front View")

        assertEquals("New Front View", copied.frontView)
        assertEquals(original.leftProfile, copied.leftProfile)
        assertEquals(original.rightProfile, copied.rightProfile)
    }

    @Test
    fun testEqualsAndHashCode() {
        val first = FacialSeriesStrings()
        val second = FacialSeriesStrings()
        val different = FacialSeriesStrings(leftProfile = "Different")

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())

        assertNotEquals(first, different)
        assertNotEquals(first.hashCode(), different.hashCode())
    }

    @Test
    fun testToString() {
        val strings = FacialSeriesStrings()
        val toStringResult = strings.toString()

        assertEquals(
            "FacialSeriesStrings(leftProfile=Left Profile (Cornea & Nose), frontView=Front View, rightProfile=Right Profile (Cornea & Nose))",
            toStringResult,
        )
    }
}
