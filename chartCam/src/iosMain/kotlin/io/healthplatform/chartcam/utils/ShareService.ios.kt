/**
 * iOS implementation of the sharing service.
 */
package io.healthplatform.chartcam.utils

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

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
     */
    override fun shareFile(filePath: String) {
        val url = NSURL.fileURLWithPath(filePath)
        shareItems(listOf(url))
    }

    /**
     * Shares plain text content.
     *
     * It presents the text in the iOS share sheet.
     *
     * @param text The text string to be shared.
     */
    override fun shareText(text: String) {
        shareItems(listOf(text))
    }

    /**
     * Internal helper to present the [UIActivityViewController] with the given items.
     *
     * Finds the current root view controller and presents the share sheet modally.
     *
     * @param items A list of items (e.g., [NSURL], [String]) to be shared.
     */
    private fun shareItems(items: List<Any>) {
        val window = UIApplication.sharedApplication.keyWindow ?: return
        val rootViewController = window.rootViewController ?: return

        val activityVC =
            UIActivityViewController(
                activityItems = items,
                applicationActivities = null,
            )

        rootViewController.presentViewController(activityVC, animated = true, completion = null)
    }
}

/**
 * Factory function to create an iOS-specific [ShareService].
 *
 * @return An instance of [IosShareService].
 */
actual fun createShareService(): ShareService = IosShareService()
