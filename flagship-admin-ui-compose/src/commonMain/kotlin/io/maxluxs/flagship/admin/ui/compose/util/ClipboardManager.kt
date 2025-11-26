package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable

/**
 * Platform-specific clipboard manager for copying text to clipboard.
 */
expect class ClipboardManager {
    fun copyToClipboard(text: String)
}

@Composable
expect fun createClipboardManager(context: Any? = null): ClipboardManager

