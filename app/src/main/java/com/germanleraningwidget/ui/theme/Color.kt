package com.germanleraningwidget.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material Design 3 Color System for German Learning Widget
 * 
 * This file defines the complete color palette following Material Design 3 guidelines
 * with custom branding colors for the German Learning Widget app.
 */

// Primary Colors (Blue)
val md_theme_light_primary = Color(0xFF2B2675)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFE6EAFF)
val md_theme_light_onPrimaryContainer = Color(0xFF1E1A52)

val md_theme_dark_primary = Color(0xFFB8C5FF)
val md_theme_dark_onPrimary = Color(0xFF1E1A52)
val md_theme_dark_primaryContainer = Color(0xFF2B2675)
val md_theme_dark_onPrimaryContainer = Color(0xFFE6EAFF)

// Secondary Colors (Lavender)
val md_theme_light_secondary = Color(0xFFA99BF7)
val md_theme_light_onSecondary = Color(0xFF2B2675)
val md_theme_light_secondaryContainer = Color(0xFFF0EDFF)
val md_theme_light_onSecondaryContainer = Color(0xFF4A3F7A)

val md_theme_dark_secondary = Color(0xFFA99BF7)
val md_theme_dark_onSecondary = Color(0xFF2B2675)
val md_theme_dark_secondaryContainer = Color(0xFF4A3F7A)
val md_theme_dark_onSecondaryContainer = Color(0xFFF0EDFF)

// Tertiary Colors (Apple Green)
val md_theme_light_tertiary = Color(0xFFD1D84E)
val md_theme_light_onTertiary = Color(0xFF2D3300)
val md_theme_light_tertiaryContainer = Color(0xFFF4F7CC)
val md_theme_light_onTertiaryContainer = Color(0xFF4A5200)

val md_theme_dark_tertiary = Color(0xFFD1D84E)
val md_theme_dark_onTertiary = Color(0xFF2D3300)
val md_theme_dark_tertiaryContainer = Color(0xFF4A5200)
val md_theme_dark_onTertiaryContainer = Color(0xFFF4F7CC)

// Error Colors (Orange)
val md_theme_light_error = Color(0xFFE88E3D)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFF0E6)
val md_theme_light_onErrorContainer = Color(0xFF8B4513)

val md_theme_dark_error = Color(0xFFE88E3D)
val md_theme_dark_onError = Color(0xFFFFFFFF)
val md_theme_dark_errorContainer = Color(0xFF8B4513)
val md_theme_dark_onErrorContainer = Color(0xFFFFF0E6)

// Surface Colors - Light Theme
val md_theme_light_background = Color(0xFFFFFCF2)
val md_theme_light_onBackground = Color(0xFF1C1B1F)
val md_theme_light_surface = Color(0xFFFFFCF2)
val md_theme_light_onSurface = Color(0xFF1C1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE6EAFF)
val md_theme_light_onSurfaceVariant = Color(0xFF46464F)
val md_theme_light_surfaceTint = Color(0xFF2B2675)
val md_theme_light_inverseSurface = Color(0xFF313033)
val md_theme_light_inverseOnSurface = Color(0xFFF4EFF4)
val md_theme_light_inversePrimary = Color(0xFFB8C5FF)
val md_theme_light_outline = Color(0xFF767680)
val md_theme_light_outlineVariant = Color(0xFFC4C6D0)

// Surface Colors - Dark Theme
val md_theme_dark_background = Color(0xFF141218)
val md_theme_dark_onBackground = Color(0xFFE6E1E5)
val md_theme_dark_surface = Color(0xFF1C1B1F)
val md_theme_dark_onSurface = Color(0xFFE6E1E5)
val md_theme_dark_surfaceVariant = Color(0xFF2B2930)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4C6D0)
val md_theme_dark_surfaceTint = Color(0xFFB8C5FF)
val md_theme_dark_inverseSurface = Color(0xFFE6E1E5)
val md_theme_dark_inverseOnSurface = Color(0xFF313033)
val md_theme_dark_inversePrimary = Color(0xFF2B2675)
val md_theme_dark_outline = Color(0xFF90909A)
val md_theme_dark_outlineVariant = Color(0xFF46464F)

// Surface Container Colors - Light Theme
val md_theme_light_surfaceContainer = Color(0xFFF0F0F7)
val md_theme_light_surfaceContainerHigh = Color(0xFFEAEAF2)
val md_theme_light_surfaceContainerHighest = Color(0xFFE4E4EC)
val md_theme_light_surfaceContainerLow = Color(0xFFF6F6FD)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)

// Surface Container Colors - Dark Theme
val md_theme_dark_surfaceContainer = Color(0xFF1F1E22)
val md_theme_dark_surfaceContainerHigh = Color(0xFF292831)
val md_theme_dark_surfaceContainerHighest = Color(0xFF34323A)
val md_theme_dark_surfaceContainerLow = Color(0xFF1C1B1F)
val md_theme_dark_surfaceContainerLowest = Color(0xFF0F0E13)

// Shadow and Scrim
val md_theme_light_shadow = Color(0xFF000000)
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_scrim = Color(0xFF000000)

// Legacy colors for backward compatibility
val Purple40 = md_theme_light_secondary
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

// Widget specific colors
val WidgetTextPrimary = Color(0xFFFFFFFF)
val WidgetTextSecondary = Color(0xE6FFFFFF)
val WidgetTextTertiary = Color(0xB3FFFFFF)
val WidgetOverlay = Color(0x33FFFFFF)

/**
 * Light Color Scheme for Material Design 3
 */
val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint
)

/**
 * Dark Color Scheme for Material Design 3
 */
val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint
)

/**
 * Utility object for accessing theme colors programmatically
 */
object ThemeColors {
    /**
     * Get primary color based on dark theme preference
     */
    fun getPrimaryColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) md_theme_dark_primary else md_theme_light_primary
    }
    
    /**
     * Get surface color based on dark theme preference
     */
    fun getSurfaceColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) md_theme_dark_surface else md_theme_light_surface
    }
    
    /**
     * Get accent color (tertiary) based on dark theme preference
     */
    fun getAccentColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) md_theme_dark_tertiary else md_theme_light_tertiary
    }
    
    /**
     * Get error color based on dark theme preference
     */
    fun getErrorColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) md_theme_dark_error else md_theme_light_error
    }
}