package io.maxluxs.flagship.admin.ui.compose.util

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual class ClipboardManager(private val context: Context) {
    actual fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
        val clip = ClipData.newPlainText("API Key", text)
        clipboard.setPrimaryClip(clip)
    }
}

@Composable
actual fun createClipboardManager(context: Any?): ClipboardManager {
    val ctx = LocalContext.current
    return ClipboardManager(ctx)
}

