package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size class for adaptive navigation.
 */
enum class WindowSizeClass {
    Compact,    // < 600dp (mobile)
    Medium,     // 600dp - 840dp (tablet)
    Expanded    // > 840dp (desktop)
}

/**
 * Determines window size class based on width.
 */
fun getWindowSizeClass(width: Dp): WindowSizeClass {
    return when {
        width < 600.dp -> WindowSizeClass.Compact
        width < 840.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}

/**
 * Expect function to get window size on different platforms.
 */
@Composable
expect fun rememberWindowSize(): WindowSize

/**
 * Window size model.
 */
data class WindowSize(
    val width: Dp,
    val height: Dp
) {
    val sizeClass: WindowSizeClass
        get() = getWindowSizeClass(width)
    
    val isCompact: Boolean
        get() = sizeClass == WindowSizeClass.Compact
    
    val isMedium: Boolean
        get() = sizeClass == WindowSizeClass.Medium
    
    val isExpanded: Boolean
        get() = sizeClass == WindowSizeClass.Expanded
}

