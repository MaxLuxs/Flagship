package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable
import platform.UIKit.UIPasteboard
import platform.UIKit.generalPasteboard

actual class ClipboardManager {
    actual fun copyToClipboard(text: String) {
        val pasteboard = UIPasteboard.generalPasteboard
        pasteboard.setString(text)
    }
}

@Composable
actual fun createClipboardManager(context: Any?): ClipboardManager {
    return ClipboardManager()
}

