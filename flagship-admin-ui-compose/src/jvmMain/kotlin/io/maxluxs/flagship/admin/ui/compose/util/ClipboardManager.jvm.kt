package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual class ClipboardManager {
    actual fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
    }
}

@Composable
actual fun createClipboardManager(context: Any?): ClipboardManager {
    return ClipboardManager()
}

