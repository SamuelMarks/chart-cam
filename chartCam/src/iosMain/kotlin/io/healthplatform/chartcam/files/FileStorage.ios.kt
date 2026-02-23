package io.healthplatform.chartcam.files

import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosFileStorage : FileStorage {
    private val fileSystem = FileSystem.SYSTEM
    
    private val documentDir by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val docDir = paths.first() as String
        docDir.toPath()
    }

    override fun saveImage(fileName: String, bytes: ByteArray): String {
        val path = documentDir / fileName
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
