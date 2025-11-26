package io.maxluxs.flagship.admin.ui.compose.theme

import androidx.compose.runtime.Composable
import io.maxluxs.flagship.ui.components.theme.FlagshipTheme

/**
 * Admin Theme with Flagship brand colors.
 * 
 * Alias for FlagshipTheme from flagship-ui-components.
 * Uses reusable design system.
 * 
 * @param useDarkTheme Use dark theme (default: system setting)
 * @param colorScheme Custom color scheme (optional)
 * @param content Content to display
 */
@Composable
fun AdminTheme(
    useDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    colorScheme: androidx.compose.material3.ColorScheme? = null,
    content: @Composable () -> Unit
) {
    FlagshipTheme(
        useDarkTheme = useDarkTheme,
        colorScheme = colorScheme,
        content = content
    )
}

