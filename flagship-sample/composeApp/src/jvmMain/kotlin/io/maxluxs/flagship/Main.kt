package io.maxluxs.flagship

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    // Initialize Flagship before showing UI
    FlagshipDesktopInitializer.initialize()
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Flagship Sample",
        state = WindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}

