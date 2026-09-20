/**
 * @file ClipboardUtilsJvmTest.kt
 * Contains declarations for ClipboardUtilsJvmTest.kt.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.test.runTest
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test fake implementation of [Clipboard] for JVM tests.
 *
 * @property nativeClipboard The underlying native clipboard object.
 */
class FakeJvmClipboard(
    override val nativeClipboard: Any,
) : Clipboard {
    /**
     * Stub getClipEntry.
     *
     * @return Null.
     */
    override suspend fun getClipEntry(): androidx.compose.ui.platform.ClipEntry? = null

    /**
     * Stub setClipEntry.
     *
     * @param clipEntry The clip entry.
     */
    override suspend fun setClipEntry(clipEntry: androidx.compose.ui.platform.ClipEntry?) {}
}

/**
 * Fake implementation of AWT [java.awt.datatransfer.Clipboard].
 *
 * @param name The name of the clipboard.
 * @param contentsSupplier Optional supplier for clipboard contents.
 * @param onSetContents Optional callback when contents are set.
 */
class FakeAwtClipboard(
    name: String = "FakeClipboard",
    private val contentsSupplier: (() -> Transferable?)? = null,
    private val onSetContents: ((Transferable?) -> Unit)? = null,
) : java.awt.datatransfer.Clipboard(name) {
    /**
     * Gets contents from the fake clipboard.
     *
     * @param requestor Requestor object.
     * @return The transferable or null.
     */
    override fun getContents(requestor: Any?): Transferable? =
        contentsSupplier?.invoke() ?: super.getContents(requestor)

    /**
     * Sets contents in the fake clipboard.
     *
     * @param contents The transferable contents.
     * @param owner The clipboard owner.
     */
    override fun setContents(contents: Transferable?, owner: ClipboardOwner?) {
        onSetContents?.invoke(contents)
    }
}

/**
 * Fake implementation of [Transferable].
 *
 * @param supported Whether string flavor is supported.
 * @param dataSupplier Supplier for transfer data.
 */
class FakeTransferable(
    private val supported: Boolean = true,
    private val dataSupplier: (() -> Any?)? = null,
) : Transferable {
    /**
     * Returns supported data flavors.
     *
     * @return Array of flavors.
     */
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.stringFlavor)

    /**
     * Checks if given data flavor is supported.
     *
     * @param flavor Flavor to check.
     * @return True if supported.
     */
    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = supported

    /**
     * Returns transfer data for given flavor.
     *
     * @param flavor Requested flavor.
     * @return Data object.
     */
    override fun getTransferData(flavor: DataFlavor?): Any? =
        dataSupplier?.invoke() ?: "fake"
}

/**
 * Comprehensive unit test suite for [ClipboardUtils.jvm.kt].
 */
class ClipboardUtilsJvmTest {
    /**
     * Verifies setting plain text into the JVM clipboard.
     */
    @Test
    fun testSetPlainText() =
        runTest {
            var capturedTransferable: Transferable? = null
            val awtClipboard =
                FakeAwtClipboard(onSetContents = { t ->
                    capturedTransferable = t
                })
            val clipboard = FakeJvmClipboard(awtClipboard)

            clipboard.setPlainText("Hello JVM")

            assertTrue(capturedTransferable is StringSelection)
            val text = (capturedTransferable as StringSelection).getTransferData(DataFlavor.stringFlavor) as String
            assertEquals("Hello JVM", text)
        }

    /**
     * Verifies retrieving plain text when clipboard content is valid.
     */
    @Test
    fun testGetPlainTextSuccess() =
        runTest {
            val transferable = FakeTransferable(supported = true, dataSupplier = { "Patient Notes" })
            val awtClipboard = FakeAwtClipboard(contentsSupplier = { transferable })
            val clipboard = FakeJvmClipboard(awtClipboard)

            val text = clipboard.getPlainText()
            assertEquals("Patient Notes", text)
        }

    /**
     * Verifies retrieving plain text when clipboard contents are null.
     */
    @Test
    fun testGetPlainTextNullContents() =
        runTest {
            val awtClipboard = FakeAwtClipboard(contentsSupplier = { null })
            val clipboard = FakeJvmClipboard(awtClipboard)

            assertNull(clipboard.getPlainText())
        }

    /**
     * Verifies retrieving plain text when string flavor is not supported.
     */
    @Test
    fun testGetPlainTextUnsupportedFlavor() =
        runTest {
            val transferable = FakeTransferable(supported = false)
            val awtClipboard = FakeAwtClipboard(contentsSupplier = { transferable })
            val clipboard = FakeJvmClipboard(awtClipboard)

            assertNull(clipboard.getPlainText())
        }

    /**
     * Verifies retrieving plain text when transfer data is not a String.
     */
    @Test
    fun testGetPlainTextNonStringData() =
        runTest {
            val transferable = FakeTransferable(supported = true, dataSupplier = { 12345 })
            val awtClipboard = FakeAwtClipboard(contentsSupplier = { transferable })
            val clipboard = FakeJvmClipboard(awtClipboard)

            assertNull(clipboard.getPlainText())
        }

    /**
     * Verifies exception handling when UnsupportedFlavorException is thrown.
     */
    @Test
    fun testGetPlainTextCatchesUnsupportedFlavorException() =
        runTest {
            val transferable =
                FakeTransferable(supported = true, dataSupplier = {
                    throw UnsupportedFlavorException(DataFlavor.stringFlavor) // allow-exception
                })
            val awtClipboard = FakeAwtClipboard(contentsSupplier = { transferable })
            val clipboard = FakeJvmClipboard(awtClipboard)

            assertNull(clipboard.getPlainText())
        }

    /**
     * Verifies exception handling when IOException is thrown.
     */
    @Test
    fun testGetPlainTextCatchesIOException() =
        runTest {
            val transferable =
                FakeTransferable(supported = true, dataSupplier = {
                    throw IOException("IO error reading clipboard") // allow-exception
                })
            val awtClipboard = FakeAwtClipboard(contentsSupplier = { transferable })
            val clipboard = FakeJvmClipboard(awtClipboard)

            assertNull(clipboard.getPlainText())
        }

    /**
     * Verifies exception handling when IllegalStateException is thrown.
     */
    @Test
    fun testGetPlainTextCatchesIllegalStateException() =
        runTest {
            val awtClipboard =
                FakeAwtClipboard(contentsSupplier = {
                    throw IllegalStateException("Clipboard unavailable") // allow-exception
                })
            val clipboard = FakeJvmClipboard(awtClipboard)

            assertNull(clipboard.getPlainText())
        }
}
