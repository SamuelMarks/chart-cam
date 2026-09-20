/**
 * @file VisualFormModelsTest.kt
 * Unit tests verifying VisualFormModels data classes, defaults, serialization, and equality.
 */

package io.healthplatform.chartcam.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test suite for [BodyMapLocation], [FitzpatrickSkinType], [FitzpatrickScaleDefaults],
 * [PainScaleLevel], and [PainScaleDefaults].
 */
class VisualFormModelsTest {
    /**
     * Verifies BodyMapLocation construction, defaults, serialization, and data class operations.
     */
    @Test
    fun testBodyMapLocation() {
        val defaultLoc = BodyMapLocation("r1", "Region 1")
        assertEquals("r1", defaultLoc.regionId)
        assertEquals("Region 1", defaultLoc.displayName)
        assertNull(defaultLoc.snomedCode)
        assertEquals(50f, defaultLoc.xPercent)
        assertEquals(50f, defaultLoc.yPercent)
        assertEquals("Region 1 (50%, 50%)", defaultLoc.toSerializedString())

        val customLoc =
            BodyMapLocation(
                regionId = "r2",
                displayName = "Left Shoulder",
                snomedCode = "12345",
                xPercent = 20.5f,
                yPercent = 30.5f,
            )
        assertEquals("Left Shoulder [12345] (20%, 30%)", customLoc.toSerializedString())

        val (id, name, snomed, x, y) = customLoc
        assertEquals("r2", id)
        assertEquals("Left Shoulder", name)
        assertEquals("12345", snomed)
        assertEquals(20.5f, x)
        assertEquals(30.5f, y)

        val copy1 = customLoc.copy(xPercent = 40f)
        assertEquals(40f, copy1.xPercent)
        assertNotEquals(customLoc, copy1)
        assertEquals(customLoc, customLoc)
        assertFalse(customLoc.equals(null))
        assertFalse(customLoc.equals("other"))
        assertEquals(customLoc.hashCode(), customLoc.hashCode())
        assertTrue(customLoc.toString().contains("Left Shoulder"))
    }

    /**
     * Verifies FitzpatrickSkinType and FitzpatrickScaleDefaults.
     */
    @Test
    fun testFitzpatrickSkinTypeAndDefaults() {
        val types = FitzpatrickScaleDefaults.ALL_TYPES
        assertEquals(6, types.size)

        val type1 = types[0]
        assertEquals(1, type1.type)
        assertEquals("I", type1.romanNumeral)
        assertEquals("#F8D9C8", type1.hexColor)
        assertEquals("fitzpatrick_type_1", type1.titleKey)
        assertEquals("fitzpatrick_desc_1", type1.descriptionKey)

        val (num, roman, hex, title, desc) = type1
        assertEquals(1, num)
        assertEquals("I", roman)
        assertEquals("#F8D9C8", hex)
        assertEquals("fitzpatrick_type_1", title)
        assertEquals("fitzpatrick_desc_1", desc)

        val customType = FitzpatrickSkinType(7, "VII", "#000000", "t7", "d7")
        val copyCustom = customType.copy(type = 8)
        assertEquals(8, copyCustom.type)
        assertNotEquals(customType, copyCustom)
        assertEquals(customType, customType)
        assertFalse(customType.equals(null))
        assertFalse(customType.equals("test"))
        assertEquals(customType.hashCode(), customType.hashCode())
        assertTrue(customType.toString().contains("VII"))
    }

    /**
     * Verifies PainScaleLevel and PainScaleDefaults.
     */
    @Test
    fun testPainScaleLevelAndDefaults() {
        val levels = PainScaleDefaults.LEVELS
        assertEquals(6, levels.size)

        val firstLevel = levels[0]
        assertEquals(0, firstLevel.score)
        assertEquals("#4CAF50", firstLevel.hexColor)
        assertEquals("pain_level_0", firstLevel.labelKey)

        val (score, hex, label) = firstLevel
        assertEquals(0, score)
        assertEquals("#4CAF50", hex)
        assertEquals("pain_level_0", label)

        val customLevel = PainScaleLevel(5, "#112233", "pain_level_5")
        val copyCustom = customLevel.copy(score = 7)
        assertEquals(7, copyCustom.score)
        assertNotEquals(customLevel, copyCustom)
        assertEquals(customLevel, customLevel)
        assertFalse(customLevel.equals(null))
        assertFalse(customLevel.equals(123))
        assertEquals(customLevel.hashCode(), customLevel.hashCode())
        assertTrue(customLevel.toString().contains("pain_level_5"))
    }
}
