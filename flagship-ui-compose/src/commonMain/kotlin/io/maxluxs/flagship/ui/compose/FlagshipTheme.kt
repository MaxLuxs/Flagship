package io.maxluxs.flagship.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import io.maxluxs.flagship.ui.components.theme.FlagshipTheme as SharedFlagshipTheme

/**
 * Flagship Theme для SDK Debug UI.
 * 
 * Использует общую дизайн-систему из [flagship-ui-components] с брендовыми цветами Flagship.
 * 
 * Обеспечивает обратную совместимость с существующим API.
 * 
 * @param useDarkTheme Использовать темную тему (по умолчанию: системная настройка)
 * @param colorScheme Кастомная цветовая схема (опционально)
 * @param content Контент для отображения
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

