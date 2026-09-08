/**
 * @file TutorialPreferencesTest.kt
 * Contains declarations for TutorialPreferencesTest.kt.
 *
 * Unit tests for [TutorialPreferences] and [DefaultTutorialPreferences].
 */
package io.healthplatform.chartcam.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validates the behavior of [DefaultTutorialPreferences] using a mock storage implementation.
 */
class TutorialPreferencesTest {
    /**
     * A mock storage for testing [TutorialPreferences].
     */
    private class MockStorage : SecureStorage {
        val map = mutableMapOf<String, String>()

        override fun save(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun getString(key: String): String? = map[key]

        override fun delete(key: String) {
            map.remove(key)
        }
    }

    /**
     * Tests that initial state shows tutorial and indicates uncompleted status.
     */
    @Test
    fun testInitialState() {
        val storage = MockStorage()
        val prefs = DefaultTutorialPreferences(storage)

        assertFalse(prefs.hasSeenTutorial())
        assertEquals(0, prefs.getTutorialVersion())
        assertTrue(prefs.shouldShowTutorial())
        assertTrue(prefs.shouldShowTutorial(1))
    }

    /**
     * Tests marking tutorial completed with default and custom version.
     */
    @Test
    fun testMarkTutorialCompleted() {
        val storage = MockStorage()
        val prefs = DefaultTutorialPreferences(storage)

        prefs.markTutorialCompleted()
        assertTrue(prefs.hasSeenTutorial())
        assertEquals(TutorialPreferences.CURRENT_TUTORIAL_VERSION, prefs.getTutorialVersion())
        assertFalse(prefs.shouldShowTutorial())
        assertFalse(prefs.shouldShowTutorial(TutorialPreferences.CURRENT_TUTORIAL_VERSION))

        // Newer version requested
        assertTrue(prefs.shouldShowTutorial(TutorialPreferences.CURRENT_TUTORIAL_VERSION + 1))

        // Custom version completion
        prefs.markTutorialCompleted(5)
        assertEquals(5, prefs.getTutorialVersion())
        assertFalse(prefs.shouldShowTutorial(5))
    }

    /**
     * Tests resetting tutorial state.
     */
    @Test
    fun testResetTutorialState() {
        val storage = MockStorage()
        val prefs = DefaultTutorialPreferences(storage)

        prefs.markTutorialCompleted(1)
        assertTrue(prefs.hasSeenTutorial())

        prefs.resetTutorialState()
        assertFalse(prefs.hasSeenTutorial())
        assertEquals(0, prefs.getTutorialVersion())
        assertTrue(prefs.shouldShowTutorial(1))
    }

    /**
     * Tests handling corrupted or non-numeric stored version strings.
     */
    @Test
    fun testCorruptedVersionStorageFallback() {
        val storage = MockStorage()
        storage.save(TutorialPreferences.KEY_TUTORIAL_VERSION, "corrupted_version_string")
        val prefs = DefaultTutorialPreferences(storage)

        assertEquals(0, prefs.getTutorialVersion())
    }
}
