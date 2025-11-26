package io.maxluxs.flagship.ui.components.theme

import androidx.compose.ui.graphics.Color

/**
 * Flagship brand colors from logo.
 * 
 * Reusable module for all Flagship UI clients.
 * 
 * Logo: docs/images/flagship_icon.svg
 * Main colors:
 * - FlagshipGreen (#00d687) - main green color
 * - FlagshipOrange (#ef7200) - orange accent
 */
object BrandColors {
    // Main brand colors from logo
    val FlagshipGreen = Color(0xFF00D687)  // #00d687 - main green
    val FlagshipOrange = Color(0xFFEF7200) // #ef7200 - orange accent
    
    // Additional green shades
    val FlagshipGreenLight = Color(0xFF4DE8A8)
    val FlagshipGreenDark = Color(0xFF00A869)
    val FlagshipGreenContainer = Color(0xFFE0F8F0)
    
    // Additional orange shades
    val FlagshipOrangeLight = Color(0xFFFF8F40)
    val FlagshipOrangeDark = Color(0xFFCC5A00)
    val FlagshipOrangeContainer = Color(0xFFFFE8D6)
    
    // Neutral colors for UI
    val Background = Color(0xFFFAFAFA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF5F5F5)
    val SurfaceElevated = Color(0xFFFFFFFF)
    
    // Text
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF666666)
    val TextTertiary = Color(0xFF999999)
    
    // Statuses
    val Success = FlagshipGreen
    val Warning = FlagshipOrange
    val Error = Color(0xFFDC3545)
    val Info = Color(0xFF0D6EFD)
    
    // Borders and dividers
    val Border = Color(0xFFE0E0E0)
    val Divider = Color(0xFFE5E5E5)
}

