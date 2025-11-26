package io.maxluxs.flagship.ui.components.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Light color scheme с брендовыми цветами Flagship.
 * Primary: FlagshipGreen (#00d687)
 * Secondary: FlagshipOrange (#ef7200)
 */
private val LightColorScheme = lightColorScheme(
    primary = BrandColors.FlagshipGreen,
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = BrandColors.FlagshipGreenContainer,
    onPrimaryContainer = BrandColors.FlagshipGreenDark,
    secondary = BrandColors.FlagshipOrange,
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = BrandColors.FlagshipOrangeContainer,
    onSecondaryContainer = BrandColors.FlagshipOrangeDark,
    tertiary = BrandColors.FlagshipOrangeLight,
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = BrandColors.FlagshipOrangeContainer,
    onTertiaryContainer = BrandColors.FlagshipOrangeDark,
    error = BrandColors.Error,
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer = BrandColors.Error,
    background = BrandColors.Background,
    onBackground = BrandColors.TextPrimary,
    surface = BrandColors.Surface,
    onSurface = BrandColors.TextPrimary,
    surfaceVariant = BrandColors.SurfaceVariant,
    onSurfaceVariant = BrandColors.TextSecondary,
    outline = BrandColors.Border,
    outlineVariant = BrandColors.Divider,
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
    inverseSurface = BrandColors.TextPrimary,
    inverseOnSurface = BrandColors.Surface,
    inversePrimary = BrandColors.FlagshipGreenLight,
    surfaceTint = BrandColors.FlagshipGreen
)

/**
 * Dark color scheme с брендовыми цветами Flagship.
 */
private val DarkColorScheme = darkColorScheme(
    primary = BrandColors.FlagshipGreenLight,
    onPrimary = BrandColors.FlagshipGreenDark,
    primaryContainer = BrandColors.FlagshipGreenDark,
    onPrimaryContainer = BrandColors.FlagshipGreenLight,
    secondary = BrandColors.FlagshipOrangeLight,
    onSecondary = BrandColors.FlagshipOrangeDark,
    secondaryContainer = BrandColors.FlagshipOrangeDark,
    onSecondaryContainer = BrandColors.FlagshipOrangeLight,
    tertiary = BrandColors.FlagshipOrange,
    onTertiary = BrandColors.FlagshipOrangeDark,
    tertiaryContainer = BrandColors.FlagshipOrangeDark,
    onTertiaryContainer = BrandColors.FlagshipOrangeLight,
    error = BrandColors.Error,
    onError = BrandColors.Error,
    errorContainer = BrandColors.Error,
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    background = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
    outline = androidx.compose.ui.graphics.Color(0xFF938F99),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF49454F),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF313033),
    inversePrimary = BrandColors.FlagshipGreen,
    surfaceTint = BrandColors.FlagshipGreenLight
)

/**
 * Flagship Theme с брендовыми цветами.
 * 
 * Переиспользуемый модуль для всех Flagship UI клиентов.
 * 
 * Использует:
 * - BrandColors для цветовой палитры
 * - FlagshipTypography для типографики
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
    val scheme = colorScheme ?: if (useDarkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = scheme,
        typography = FlagshipTypography,
        content = content
    )
}

