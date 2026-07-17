/**
 * @file AndroidFileStorageTest.kt
 * Contains declarations for AndroidFileStorageTest.kt.
 */
package io.healthplatform.chartcam.files

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.healthplatform.chartcam.AndroidAppInit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertContentEquals

/**
 * Tests for [AndroidFileStorage].
 */
@RunWith(AndroidJUnit4::class)
class AndroidFileStorageTest {
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidAppInit.init(context)
    }

    /**
     * Test create storage.
     */
    @Test
    fun testCreateFileStorage() {
        val storage = createFileStorage()
        assertTrue(storage is AndroidFileStorage)
    }

    /**
     * Test save and read.
     */
    @Test
    fun testSaveAndReadImage() {
        val storage = createFileStorage()
        val data = "test_data".toByteArray()
        val path = storage.saveImage("test.jpg", data)

        val file = File(path)
        assertTrue(file.exists())

        val readData = storage.readImage(path)
        assertContentEquals(data, readData)

        storage.clearCache()
        assertTrue(!file.exists() || storage.readImage(path).isEmpty())
    }
}
