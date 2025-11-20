package io.maxluxs.flagship.ui.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Default yellow-cyan color scheme
private val YellowCyan = Color(0xFFFFC107)
private val YellowCyanVariant = Color(0xFFFFD54F)
private val CyanAccent = Color(0xFF00BCD4)
private val CyanLight = Color(0xFF4DD0E1)
private val CyanDark = Color(0xFF0097A7)

private val BackgroundLight = Color(0xFFFFFBF5)
private val SurfaceLight = Color(0xFFFFF9EF)
private val OnSurfaceLight = Color(0xFF1C1B1F)

private val BackgroundDark = Color(0xFF1C1B1F)
private val SurfaceDark = Color(0xFF2B2930)
private val OnSurfaceDark = Color(0xFFE6E1E5)

val DefaultLightColorScheme = lightColorScheme(
    primary = YellowCyan,
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFFFFF9C4),
    onPrimaryContainer = Color(0xFF3E2723),
    
    secondary = CyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363D),
    
    tertiary = Color(0xFF9C27B0),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1BEE7),
    onTertiaryContainer = Color(0xFF4A148C),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),
    
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFFFF3E0),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

val DefaultDarkColorScheme = darkColorScheme(
    primary = YellowCyan,
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFFFE082),
    
    secondary = CyanLight,
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFF004F5C),
    onSecondaryContainer = Color(0xFFB2EBF2),
    
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color(0xFF4A148C),
    tertiaryContainer = Color(0xFF6A1B9A),
    onTertiaryContainer = Color(0xFFF3E5F5),
    
    error = Color(0xFFEF5350),
    onError = Color(0xFF5F2120),
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),
    
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF3E3842),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

/**
 * Flagship Material 3 theme with customizable color scheme.
 * 
 * Default: Yellow + Cyan accent (no elevation shadows)
 */
@Composable
fun FlagshipTheme(
    useDarkTheme: Boolean = false,
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val colors = colorScheme ?: if (useDarkTheme) {
        DefaultDarkColorScheme
    } else {
        DefaultLightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

