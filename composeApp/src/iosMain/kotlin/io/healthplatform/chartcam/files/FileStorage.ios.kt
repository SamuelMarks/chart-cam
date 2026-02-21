package io.healthplatform.chartcam.files

import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

class IosFileStorage : FileStorage {
    private val fileSystem = FileSystem.SYSTEM
    // NSTemporaryDirectory returns a string ending with /
    private val tempDir = NSTemporaryDirectory().toPath()

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

    override fun clearCache() {
        // Implementation would clear logic specific files
    }
}

actual fun createFileStorage(): FileStorage = IosFileStorage()