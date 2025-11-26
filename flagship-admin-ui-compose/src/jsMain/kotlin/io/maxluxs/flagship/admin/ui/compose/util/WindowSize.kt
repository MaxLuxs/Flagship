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
 * JS (Web) реализация получения размера окна.
 * 
 * Для web используем размер окна браузера.
 * По умолчанию считаем Expanded, если ширина > 840px.
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    // Для web используем размер окна браузера
    // Конвертируем пиксели в dp (примерно 1px = 1dp для web)
    val widthPx = window.innerWidth
    val heightPx = window.innerHeight
    
    return remember {
        WindowSize(
            width = widthPx.dp,
            height = heightPx.dp
        )
    }
}

