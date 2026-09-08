/**
 * @file TutorialPreferences.kt
 * Contains declarations for TutorialPreferences.kt.
 *
 * Interface and implementation for managing onboarding tutorial state and version persistence.
 */
package io.healthplatform.chartcam.storage

import io.healthplatform.chartcam.storage.TutorialPreferences.Companion.KEY_TUTORIAL_VERSION

/**
 * Contract for persisting and querying the user's onboarding tutorial completion status.
 */
interface TutorialPreferences {
    /**
     * Checks if the onboarding workflow tutorial has already been seen and completed.
     *
     * @return True if the tutorial has been completed or dismissed, false otherwise.
     */
    fun hasSeenTutorial(): Boolean

    /**
     * Gets the last completed tutorial schema version.
     *
     * @return The stored version number, or 0 if never completed.
     */
    fun getTutorialVersion(): Int

    /**
     * Marks the onboarding tutorial as completed for a specific version.
     *
     * @param version The tutorial version completed.
     */
    fun markTutorialCompleted(version: Int)

    /**
     * Resets the tutorial state, causing it to be eligible to be displayed again.
     */
    fun resetTutorialState()

    /**
     * Evaluates whether the onboarding tutorial should be shown to the user.
     *
     * @param currentVersion The current active tutorial version.
     * @return True if the tutorial has not been seen or is from an older version, false otherwise.
     */
    fun shouldShowTutorial(currentVersion: Int): Boolean

    /**
     * Companion constants for storage keys and version numbering.
     */
    companion object {
        /** Storage key for tutorial seen boolean flag. */
        const val KEY_HAS_SEEN_TUTORIAL = "tutorial_has_seen"

        /** Storage key for tutorial schema version integer. */
        const val KEY_TUTORIAL_VERSION = "tutorial_version"

        /** Current active tutorial version. */
        const val CURRENT_TUTORIAL_VERSION = 1
    }
}

/**
 * Default implementation of [TutorialPreferences] backed by [SecureStorage].
 *
 * @param storage The underlying storage engine used for persistence.
 */
class DefaultTutorialPreferences(
    private val storage: SecureStorage,
) : TutorialPreferences {
    /**
     * Checks if the onboarding workflow tutorial has already been seen and completed.
     *
     * @return True if the tutorial has been completed or dismissed, false otherwise.
     */
    override fun hasSeenTutorial(): Boolean = storage.getString(TutorialPreferences.KEY_HAS_SEEN_TUTORIAL) == "true"

    /**
     * Gets the last completed tutorial schema version.
     *
     * @return The stored version number, or 0 if never completed.
     */
    override fun getTutorialVersion(): Int = storage.getString(KEY_TUTORIAL_VERSION)?.toIntOrNull() ?: 0

    /**
     * Marks the onboarding tutorial as completed for a specific version.
     *
     * @param version The tutorial version completed.
     */
    override fun markTutorialCompleted(version: Int) {
        storage.save(TutorialPreferences.KEY_HAS_SEEN_TUTORIAL, "true")
        storage.save(TutorialPreferences.KEY_TUTORIAL_VERSION, version.toString())
    }

    /**
     * Marks the onboarding tutorial as completed using the default current version.
     */
    fun markTutorialCompleted() {
        markTutorialCompleted(TutorialPreferences.CURRENT_TUTORIAL_VERSION)
    }

    /**
     * Resets the tutorial state, causing it to be eligible to be displayed again.
     */
    override fun resetTutorialState() {
        storage.delete(TutorialPreferences.KEY_HAS_SEEN_TUTORIAL)
        storage.delete(TutorialPreferences.KEY_TUTORIAL_VERSION)
    }

    /**
     * Evaluates whether the onboarding tutorial should be shown for the default current version.
     *
     * @return True if tutorial should be shown, false otherwise.
     */
    fun shouldShowTutorial(): Boolean = shouldShowTutorial(TutorialPreferences.CURRENT_TUTORIAL_VERSION)

    /**
     * Evaluates whether the onboarding tutorial should be shown to the user.
     *
     * @param currentVersion The current active tutorial version.
     * @return True if the tutorial has not been seen or is from an older version, false otherwise.
     */
    override fun shouldShowTutorial(currentVersion: Int): Boolean {
        if (!hasSeenTutorial()) return true
        return getTutorialVersion() < currentVersion
    }
}
