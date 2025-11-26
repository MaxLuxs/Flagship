package io.maxluxs.flagship.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import io.maxluxs.flagship.ui.components.theme.FlagshipTheme as SharedFlagshipTheme

/**
 * Flagship Theme for SDK Debug UI.
 * 
 * Uses shared design system from [flagship-ui-components] with Flagship brand colors.
 * 
 * Provides backward compatibility with existing API.
 * 
 * @param useDarkTheme Use dark theme (default: system setting)
 * @param colorScheme Custom color scheme (optional)
 * @param content Content to display
 */
@Composable
fun FlagshipTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    SharedFlagshipTheme(
        useDarkTheme = useDarkTheme,
        colorScheme = colorScheme,
        content = content
    )
}

