package io.maxluxs.flagship.admin.ui.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS реализация получения размера окна.
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    
    return WindowSize(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
}

