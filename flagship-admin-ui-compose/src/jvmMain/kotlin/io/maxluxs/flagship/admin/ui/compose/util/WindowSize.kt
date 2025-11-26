@file:JvmName("WindowSizeJvm")

package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState

/**
 * JVM (Desktop) implementation for getting window size.
 * 
 * For desktop, we use fixed Expanded size by default,
 * as desktop applications usually have large windows.
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    // For desktop, use Expanded size by default
    // In a real application, can use WindowState to get actual size
    return remember {
        WindowSize(
            width = 1200.dp, // Expanded by default for desktop
            height = 800.dp
        )
    }
}

