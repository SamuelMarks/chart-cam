/**
 * @file SetupPlatformPrivacy.wasmJs.kt
 * Contains declarations for SetupPlatformPrivacy.wasmJs.kt.
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.healthplatform.chartcam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val IS_DOCUMENT_HIDDEN_JS =
    "() => (typeof document !== 'undefined' && document.hidden) ? true : false"

@JsFun(IS_DOCUMENT_HIDDEN_JS)
private external fun isDocumentHiddenJs(): Boolean

/**
 * WasmJs implementation of [SetupPlatformPrivacy] monitoring document visibility.
 */
@Composable
actual fun SetupPlatformPrivacy() {
    LaunchedEffect(Unit) {
        var lastHidden = isDocumentHiddenJs()
        while (isActive) {
            val hidden = isDocumentHiddenJs()
            if (hidden != lastHidden) {
                lastHidden = hidden
                if (hidden) {
                    currentAppPrivacyManager.onAppMovedToBackground()
                } else {
                    currentAppPrivacyManager.onAppMovedToForeground()
                }
            }
            delay(200L)
        }
    }
}
