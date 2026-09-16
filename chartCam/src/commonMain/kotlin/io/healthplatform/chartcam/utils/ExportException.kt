/**
 * @file ExportException.kt
 * Defines the sealed domain exception hierarchy for decentralized export and sharing operations.
 */
package io.healthplatform.chartcam.utils

/**
 * Base sealed exception for all decentralized export, packaging, and platform sharing failures.
 *
 * @param message Human-readable error description explaining the cause of the failure.
 * @param cause Optional underlying throwable cause for chained diagnostic context.
 */
sealed class ExportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Exception thrown or returned when an encryption password provided for archive export fails validation.
 *
 * @param message Human-readable description of the password validation failure.
 */
class InvalidExportPasswordException(
    message: String = "Export password must not be empty or weak (minimum 6 characters).",
) : ExportException(message)

/**
 * Exception returned when a requested file cannot be located on disk for sharing or export.
 *
 * @param path The absolute or relative target file path that could not be resolved.
 */
class ExportFileNotFoundException(
    path: String,
) : ExportException("Target export file not found on local storage: $path")

/**
 * Exception returned when persistent or cache file storage fails during export payload generation.
 *
 * @param fileName The name of the file that could not be saved to local storage.
 * @param cause Optional underlying throwable explaining the storage error.
 */
class FileStorageSaveException(
    fileName: String,
    cause: Throwable? = null,
) : ExportException("Failed to save export file to local storage: $fileName", cause)

/**
 * Exception returned when the native host platform fails to present a system share sheet or complete sharing.
 *
 * @param platform The host platform name (e.g., "Android", "iOS", "JVM", "JS", "WasmJS").
 * @param reason Detailed explanation of the platform-specific failure.
 * @param cause Optional underlying throwable from the host platform subsystem.
 */
class PlatformShareException(
    platform: String,
    reason: String,
    cause: Throwable? = null,
) : ExportException("Local share sheet operation failed on $platform: $reason", cause)

/**
 * Exception returned when DICOM PACS packaging or serialization encounters an unrecoverable error.
 *
 * @param message Human-readable description of the DICOM PACS export failure.
 * @param cause Optional underlying throwable from the DICOM serialization layer.
 */
class PacsExportException(
    message: String,
    cause: Throwable? = null,
) : ExportException(message, cause)
