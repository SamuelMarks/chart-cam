/**
 * @file ShareService.ios.kt
 * Contains declarations for ShareService.ios.kt.
 *
 * iOS implementation of the sharing service.
 */
package io.healthplatform.chartcam.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

/**
 * iOS-specific implementation for sharing files and text with other applications.
 *
 * This class uses [UIActivityViewController] to present a system share sheet,
 * allowing the user to select how they want to share the provided content.
 */
class IosShareService : ShareService {
    /**
     * Shares a file located at the specified path.
     *
     * It converts the file path into a local [NSURL] and presents it in the
     * iOS share sheet.
     *
     * @param filePath The absolute path to the file to be shared.
     * @return A [Result] indicating success or failure.
     */
    override fun shareFile(filePath: String): Result<Unit> {
        if (filePath.isBlank()) {
            return Result.failure(ExportFileNotFoundException(filePath))
        }
        val url = NSURL.fileURLWithPath(filePath)
        return shareItems(listOf(url))
    }

    /**
     * Shares plain text content.
     *
     * It presents the text in the iOS share sheet.
     *
     * @param text The text string to be shared.
     * @return A [Result] indicating success or failure.
     */
    override fun shareText(text: String): Result<Unit> {
        if (text.isBlank()) {
            return Result.success(Unit)
        }
        return shareItems(listOf(text))
    }

    /**
     * Internal helper to present the [UIActivityViewController] with the given items.
     *
     * Finds the current root view controller and presents the share sheet modally.
     *
     * @param items A list of items (e.g., [NSURL], [String]) to be shared.
     * @return A [Result] indicating whether the share sheet was successfully presented.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun shareItems(items: List<Any>): Result<Unit> {
        val rootViewController =
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: return Result.failure(PlatformShareException("iOS", "No active root view controller"))

        val activityVC =
            UIActivityViewController(
                activityItems = items,
                applicationActivities = null,
            )

        activityVC.popoverPresentationController?.sourceView = rootViewController.view

        rootViewController.presentViewController(activityVC, animated = true, completion = null)
        return Result.success(Unit)
    }
}

/**
 * Factory function to create an iOS-specific [ShareService].
 *
 * @return An instance of [IosShareService].
 */
actual fun createShareService(): ShareService = IosShareService()
