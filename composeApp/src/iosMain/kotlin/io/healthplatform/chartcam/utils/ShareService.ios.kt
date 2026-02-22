package io.healthplatform.chartcam.utils

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * iOS implementation for sharing files and text.
 */
class IosShareService : ShareService {
    override fun shareFile(filePath: String) {
        val url = NSURL.fileURLWithPath(filePath)
        shareItems(listOf(url))
    }

    override fun shareText(text: String) {
        shareItems(listOf(text))
    }

    private fun shareItems(items: List<Any>) {
        val window = UIApplication.sharedApplication.keyWindow ?: return
        val rootViewController = window.rootViewController ?: return

        val activityVC = UIActivityViewController(
            activityItems = items,
            applicationActivities = null
        )

        rootViewController.presentViewController(activityVC, animated = true, completion = null)
    }
}

/**
 * Creates the IosShareService.
 */
actual fun createShareService(): ShareService = IosShareService()
