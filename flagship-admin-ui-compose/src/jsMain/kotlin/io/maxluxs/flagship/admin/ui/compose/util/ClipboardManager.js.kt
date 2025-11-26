package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable

actual class ClipboardManager {
    actual fun copyToClipboard(text: String) {
        // Use Clipboard API if available
        if (js("typeof navigator !== 'undefined' && navigator.clipboard")) {
            js("navigator.clipboard.writeText(text)").unsafeCast<kotlin.js.Promise<Unit>>()
        } else {
            // Fallback for older browsers
            val textArea = js("document.createElement('textarea')").unsafeCast<org.w3c.dom.HTMLTextAreaElement>()
            textArea.value = text
            textArea.style.position = "fixed"
            textArea.style.opacity = "0"
            js("document.body.appendChild(textArea)")
            textArea.select()
            js("document.execCommand('copy')")
            js("document.body.removeChild(textArea)")
        }
    }
}

@Composable
actual fun createClipboardManager(context: Any?): ClipboardManager {
    return ClipboardManager()
}

