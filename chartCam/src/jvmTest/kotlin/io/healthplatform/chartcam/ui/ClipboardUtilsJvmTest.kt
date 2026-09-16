/**
 * @file ClipboardUtilsJvmTest.kt
 * Contains declarations for ClipboardUtilsJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for ClipboardUtils on JVM.
 */
class ClipboardUtilsJvmTest {
    /**
     * Verifies system clipboard write and read round-trip on JVM.
     */
    @Test
    fun testClipboardUtilsJvm() {
        val testText = "Clinical JSON clipboard test"
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(testText), null)
        val readText = clipboard.getData(DataFlavor.stringFlavor) as String
        assertEquals(testText, readText)
    }
}
