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
 * JVM (Desktop) реализация получения размера окна.
 * 
 * Для desktop используем фиксированный размер Expanded по умолчанию,
 * так как desktop приложения обычно имеют большие окна.
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    // Для desktop по умолчанию используем Expanded размер
    // В реальном приложении можно использовать WindowState для получения реального размера
    return remember {
        WindowSize(
            width = 1200.dp, // Expanded по умолчанию для desktop
            height = 800.dp
        )
    }
}

