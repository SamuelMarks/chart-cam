package io.healthplatform.chartcam.files

import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File

class JvmFileStorage : FileStorage {
    private val fileSystem = FileSystem.SYSTEM
    private val tempDir = System.getProperty("java.io.tmpdir").toPath()

    override fun saveImage(fileName: String, bytes: ByteArray): String {
        val path = tempDir / fileName
        fileSystem.write(path) {
            write(bytes)
        }
        return path.toString()
    }

    override fun readImage(path: String): ByteArray {
        return fileSystem.read(path.toPath()) {
            readByteArray()
        }
    }

    override fun clearCache() {}
}

actual fun createFileStorage(): FileStorage = JvmFileStorage()