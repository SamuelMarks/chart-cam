package io.healthplatform.chartcam.files

import io.healthplatform.chartcam.AndroidAppInit
import okio.FileSystem
import okio.Path.Companion.toPath

class AndroidFileStorage : FileStorage {
    private val context = AndroidAppInit.getContext()
    private val fileSystem = FileSystem.SYSTEM
    private val cacheDir = context.cacheDir.absolutePath.toPath()

    override fun saveImage(fileName: String, bytes: ByteArray): String {
        val path = cacheDir / fileName
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
        // Implementation note: Be careful not to delete other cache items in production.
        // For this phase, we assume a dedicated subfolder usually, but simple cacheDir clean for now.
        // fileSystem.deleteRecursively(cacheDir) // Too dangerous for shared cache
        
        // No-op for safety in this snippet, normally would target a sub-folder.
    }
}

actual fun createFileStorage(): FileStorage = AndroidFileStorage()