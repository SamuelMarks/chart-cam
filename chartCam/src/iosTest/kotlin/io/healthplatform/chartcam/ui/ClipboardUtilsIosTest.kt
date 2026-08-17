/**
 * @file ClipboardUtilsIosTest.kt
 * Contains iOS-specific clipboard implementation tests.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.test.runTest
import platform.UIKit.UIPasteboard
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Validation tests for clipboard extension utilities on iOS.
 */
class ClipboardUtilsIosTest {
    /**
     * Validates that standard clipboard data storage and retrieval operate accurately with the iOS backing types.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testSetAndGetPlainText() =
        runTest {
            val fakeClipboard =
                object : Clipboard {
                    private var currentEntry: ClipEntry? = null

                    /**
                     * Get clip entry mock.
                     * @return The current entry.
                     */
                    override suspend fun getClipEntry(): ClipEntry? = currentEntry

                    /**
                     * Set clip entry mock.
                     * @param clipEntry The new entry to set.
                     */
                    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
                        currentEntry = clipEntry
                    }

                    /** Provide fake native clipboard. */
                    override val nativeClipboard: UIPasteboard
                        get() = UIPasteboard.generalPasteboard
                }

            val testText = "Hello, iOS Clipboard!"
            fakeClipboard.setPlainText(testText)

            val retrievedText = fakeClipboard.getPlainText()
            assertEquals(testText, retrievedText)
        }
}
