package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.browser.window

/**
 * JS (Web) implementation for getting window size.
 * 
 * For web, we use browser window size.
 * By default, consider Expanded if width > 840px.
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    // For web, use browser window size
    // Convert pixels to dp (approximately 1px = 1dp for web)
    val widthPx = window.innerWidth
    val heightPx = window.innerHeight
    
    return remember {
        WindowSize(
            width = widthPx.dp,
            height = heightPx.dp
        )
    }
}

