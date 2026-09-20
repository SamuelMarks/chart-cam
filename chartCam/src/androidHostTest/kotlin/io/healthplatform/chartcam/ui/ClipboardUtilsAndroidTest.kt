/**
 * @file ClipboardUtilsAndroidTest.kt
 * Contains declarations for ClipboardUtilsAndroidTest.kt.
 */
package io.healthplatform.chartcam.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as whenever

/**
 * Fake Android [Clipboard] for host unit tests.
 *
 * @property currentEntry The current in-memory clip entry.
 */
class FakeAndroidClipboard(
    var currentEntry: ClipEntry? = null,
) : Clipboard {
    /**
     * Retrieves current clip entry.
     *
     * @return The current clip entry.
     */
    override suspend fun getClipEntry(): ClipEntry? = currentEntry

    /**
     * Sets current clip entry.
     *
     * @param clipEntry The clip entry to set.
     */
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        currentEntry = clipEntry
    }

    /**
     * Native clipboard accessor.
     */
    override val nativeClipboard: ClipboardManager
        get() =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

/**
 * Android host tests for [ClipboardUtils.android.kt].
 */
@Config(manifest = Config.NONE, sdk = [33])
@RunWith(RobolectricTestRunner::class)
class ClipboardUtilsAndroidTest {
    /**
     * Verifies setting and getting plain text on Android clipboard across all nullable branches.
     */
    @Test
    fun testSetAndGetPlainText() =
        runTest {
            val clipboard = FakeAndroidClipboard()

            // 1. null clipEntry returns null
            assertNull(clipboard.getPlainText())

            // 2. setPlainText sets valid text and retrieves it
            clipboard.setPlainText("Patient-12345")
            val text = clipboard.getPlainText()
            assertNotNull(text)
            assertEquals("Patient-12345", text)

            // 3. clipData with item having null text returns null
            val nullTextItem = ClipData.Item(null as CharSequence?)
            val clipData = ClipData("label", arrayOf("text/plain"), nullTextItem)
            clipboard.currentEntry = ClipEntry(clipData)
            assertNull(clipboard.getPlainText())

            // 4. clipEntry with null clipData returns null
            val mockEntryWithNullData = mock(ClipEntry::class.java)
            whenever(mockEntryWithNullData.clipData).thenReturn(null)
            clipboard.currentEntry = mockEntryWithNullData
            assertNull(clipboard.getPlainText())

            // 5. clipData where getItemAt(0) returns null
            val mockClipData = mock(ClipData::class.java)
            whenever(mockClipData.getItemAt(0)).thenReturn(null)
            val mockEntryWithNullItem = mock(ClipEntry::class.java)
            whenever(mockEntryWithNullItem.clipData).thenReturn(mockClipData)
            clipboard.currentEntry = mockEntryWithNullItem
            assertNull(clipboard.getPlainText())
        }
}
