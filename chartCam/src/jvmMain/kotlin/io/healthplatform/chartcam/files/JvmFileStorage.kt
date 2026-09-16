/**
 * @file FileStorage.jvm.kt
 * File storage implementation for the JVM platform using Okio.
 */
package io.healthplatform.chartcam.files

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

private const val MIN_WINDOWS_PATH_LENGTH = 2

/**
 * JVM-specific implementation of [FileStorage].
 * Utilizes Okio for efficient and idiomatic file I/O operations
 * against a dedicated user application media storage directory.
 *
 * @param baseDirectory The base directory path for persisting clinical media files.
 */
class JvmFileStorage(
    private val baseDirectory: Path = (System.getProperty("user.home") + "/.chartcam/media").toPath(),
) : FileStorage {
    /**
     * The Okio [FileSystem] instance representing the host operating system's local file system.
     */
    private val fileSystem = FileSystem.SYSTEM

    init {
        if (!fileSystem.exists(baseDirectory)) {
            fileSystem.createDirectories(baseDirectory)
            runCatching {
                val perms =
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    )
                Files.setPosixFilePermissions(baseDirectory.toNioPath(), perms)
            }
        }
    }

    /**
     * Checks whether the path string represents an absolute path.
     *
     * @param p The path string.
     * @return True if absolute, false otherwise.
     */
    private fun isAbsolutePath(p: String): Boolean {
        val isUnixRoot = p.startsWith('/')
        val isWindowsRoot = p.startsWith('\\')
        val isWindowsDrive = p.length > MIN_WINDOWS_PATH_LENGTH && p[1] == ':'
        return isUnixRoot || isWindowsRoot || isWindowsDrive
    }

    /**
     * Saves a byte array as an image file in the secure media directory.
     *
     * @param fileName The desired name for the file (e.g., "image.png").
     * @param bytes The binary contents of the image to be saved.
     * @return The absolute path to the newly saved file as a string.
     */
    override fun saveImage(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val path = if (isAbsolutePath(fileName)) fileName.toPath() else baseDirectory / fileName
        val parent = path.parent
        if (parent != null && !fileSystem.exists(parent)) {
            fileSystem.createDirectories(parent)
        }
        fileSystem.write(path) {
            write(bytes)
        }
        runCatching {
            val perms =
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                )
            Files.setPosixFilePermissions(path.toNioPath(), perms)
        }
        return path.toString()
    }

    /**
     * Reads the binary contents of an image file from the specified path.
     *
     * @param path The absolute or relative path to the file to be read.
     * @return A [ByteArray] containing the raw bytes of the file.
     */
    override fun readImage(path: String): ByteArray {
        val okPath = resolvePath(path)
        if (!fileSystem.exists(okPath)) return ByteArray(0)

        return fileSystem.read(okPath) {
            readByteArray()
        }
    }

    /**
     * Deletes the specified file from storage.
     *
     * @param path The absolute or relative path to the file to be deleted.
     * @return A [Result] indicating success or failure of the deletion.
     */
    override fun deleteImage(path: String): Result<Unit> {
        val okPath = resolvePath(path)
        return if (fileSystem.exists(okPath)) {
            fileSystem.delete(okPath)
            Result.success(Unit)
        } else {
            Result.failure(java.io.FileNotFoundException("File not found: $path"))
        }
    }

    /**
     * Clears cached temporary files in the storage directory.
     */
    override fun clearCache() {
        if (fileSystem.exists(baseDirectory)) {
            val list = fileSystem.list(baseDirectory)
            for (file in list) {
                if (file.name.endsWith(".tmp") || file.name.startsWith("temp_")) {
                    runCatching { fileSystem.delete(file) }
                }
            }
        }
    }

    /**
     * Resolves an input path string to a canonical Okio [Path].
     *
     * @param rawPath The path string.
     * @return The resolved [Path].
     */
    private fun resolvePath(rawPath: String): Path {
        if (isAbsolutePath(rawPath)) return rawPath.toPath()
        val scoped = baseDirectory / rawPath
        return if (fileSystem.exists(scoped)) scoped else rawPath.toPath()
    }
}

/**
 * Creates and returns a new instance of [FileStorage] for the JVM platform.
 *
 * @return A new [JvmFileStorage] instance.
 */
actual fun createFileStorage(): FileStorage = JvmFileStorage()
