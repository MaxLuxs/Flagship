package io.maxluxs.flagship.admin.ui.compose.theme

import androidx.compose.runtime.Composable
import io.maxluxs.flagship.ui.components.theme.FlagshipTheme

/**
 * Admin Theme с брендовыми цветами Flagship.
 * 
 * Алиас для FlagshipTheme из flagship-ui-components.
 * Использует переиспользуемую дизайн-систему.
 * 
 * @param useDarkTheme Использовать темную тему (по умолчанию: системная настройка)
 * @param colorScheme Кастомная цветовая схема (опционально)
 * @param content Контент для отображения
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

