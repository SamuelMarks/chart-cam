package io.healthplatform.chartcam.files

/**
 * JS (Web) implementation of [FileStorage].
 * Note: File I/O uses an in-memory cache to handle file storage temporarily for JS targets.
 */
class JsFileStorage : FileStorage {

    private val cache = mutableMapOf<String, ByteArray>()

    /**
     * Saves an image to the in-memory cache.
     *
     * @param fileName The name of the file to save.
     * @param bytes The data to write.
     * @return A virtual path representing the file in cache.
     */
    override fun saveImage(fileName: String, bytes: ByteArray): String {
        val virtualPath = "mem://path/$fileName"
        cache[virtualPath] = bytes
        return virtualPath
    }

    /**
     * Reads an image from the in-memory cache.
     *
     * @param path The path to read from.
     * @return The cached byte array, or an empty byte array if not found.
     */
    override fun readImage(path: String): ByteArray {
        return cache[path] ?: ByteArray(0)
    }

    /**
     * Clears the in-memory cache.
     */
    override fun clearCache() {
        cache.clear()
    }
}

/**
 * Factory to create [FileStorage] for JS Web.
 *
 * @return An in-memory [FileStorage] implementation.
 */
actual fun createFileStorage(): FileStorage = JsFileStorage()
