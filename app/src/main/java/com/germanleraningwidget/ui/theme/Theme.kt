package com.germanleraningwidget.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * German Learning Widget Theme System
 * 
 * Enhanced Material Design 3 theme implementation with:
 * - Custom brand colors for German Learning Widget
 * - Dynamic color support for Android 12+
 * - Proper system UI integration
 * - Theme utilities for programmatic access
 * - Accessibility considerations
 */

// LightColorScheme and DarkColorScheme are defined in Color.kt

@Composable
fun GermanLearningWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Configure system UI based on theme - Fixed: Use proper WindowInsetsControllerCompat
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            
            // Set transparent status/navigation bars for modern edge-to-edge experience
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Use WindowInsetsController for modern API levels, fallback for older versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Modern approach using WindowInsetsController (API 30+)
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                } else {
                    // Legacy approach for API 21-29
                    @Suppress("DEPRECATION")
                    window.statusBarColor = Color.Transparent.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = Color.Transparent.toArgb()
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Theme utilities for accessing current theme colors and properties
 */
object ThemeUtils {
    
    /**
     * Check if current theme is dark
     */
    @Composable
    fun isDarkTheme(): Boolean = isSystemInDarkTheme()
    
    /**
     * Get current color scheme
     */
    @Composable
    fun getColorScheme(): ColorScheme = MaterialTheme.colorScheme
    
    /**
     * Get brand primary color based on current theme
     */
    @Composable
    fun getBrandPrimary(): Color = MaterialTheme.colorScheme.primary
    
    /**
     * Get brand secondary color based on current theme
     */
    @Composable
    fun getBrandSecondary(): Color = MaterialTheme.colorScheme.secondary
    
    /**
     * Get brand tertiary (accent) color based on current theme
     */
    @Composable
    fun getBrandTertiary(): Color = MaterialTheme.colorScheme.tertiary
    
    /**
     * Get surface color with elevation overlay if needed
     */
    @Composable
    fun getSurfaceWithElevation(elevation: Int = 0): Color {
        return if (elevation > 0 && isDarkTheme()) {
            // Apply elevation overlay for dark theme
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface
        }
    }
    
    /**
     * Get appropriate text color for given background
     */
    @Composable
    fun getTextColorForBackground(backgroundColor: Color): Color {
        val luminance = backgroundColor.luminance()
        return if (luminance > 0.5) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.surface
        }
    }
}

/**
 * Extension function to calculate color luminance
 */
private fun Color.luminance(): Float {
    val red = red * 0.299f
    val green = green * 0.587f
    val blue = blue * 0.114f
    return red + green + blue
}

/**
 * Preset theme configurations for specific use cases
 */
object ThemePresets {
    
    /**
     * Widget theme colors - optimized for readability on various backgrounds
     */
    object Widget {
        val textPrimary = WidgetTextPrimary
        val textSecondary = WidgetTextSecondary
        val textTertiary = WidgetTextTertiary
        val overlay = WidgetOverlay
    }
    
    /**
     * Learning-specific color mappings
     */
    object Learning {
        @Composable
        fun getGermanTextColor(): Color = MaterialTheme.colorScheme.primary
        
        @Composable
        fun getTranslationTextColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
        
        @Composable
        fun getTopicBackgroundColor(): Color = MaterialTheme.colorScheme.secondaryContainer
        
        @Composable
        fun getTopicTextColor(): Color = MaterialTheme.colorScheme.onSecondaryContainer
        
        @Composable
        fun getLevelIndicatorColor(): Color = MaterialTheme.colorScheme.tertiary
        
        @Composable
        fun getBookmarkColor(): Color = MaterialTheme.colorScheme.tertiary
        
        @Composable
        fun getProgressColor(): Color = MaterialTheme.colorScheme.primary
        
        @Composable
        fun getSuccessColor(): Color = MaterialTheme.colorScheme.tertiary
        
        @Composable
        fun getWarningColor(): Color = MaterialTheme.colorScheme.error
    }
}

/**
 * Color state utilities for different component states
 */
object ColorStates {
    
    /**
     * Get button colors for different states
     */
    @Composable
    fun getButtonColors(
        enabled: Boolean = true,
        pressed: Boolean = false
    ): Color {
        return when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.primary
        }
    }
    
    /**
     * Get card colors for different states
     */
    @Composable
    fun getCardColors(
        elevated: Boolean = false,
        selected: Boolean = false
    ): Color {
        return when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            elevated -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        }
    }
    
    /**
     * Get text colors for different emphasis levels
     */
    @Composable
    fun getTextColors(emphasis: TextEmphasis = TextEmphasis.HIGH): Color {
        return when (emphasis) {
            TextEmphasis.HIGH -> MaterialTheme.colorScheme.onSurface
            TextEmphasis.MEDIUM -> MaterialTheme.colorScheme.onSurfaceVariant
            TextEmphasis.LOW -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            TextEmphasis.DISABLED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }
    }
}

/**
 * Text emphasis levels for consistent text styling
 */
enum class TextEmphasis {
    HIGH,    // Primary text
    MEDIUM,  // Secondary text
    LOW,     // Tertiary text
    DISABLED // Disabled text
}

/**
 * Accessibility utilities for theme
 */
object AccessibilityTheme {
    
    /**
     * Check if colors have sufficient contrast ratio
     */
    fun hasAcceptableContrast(foreground: Color, background: Color): Boolean {
        val contrastRatio = calculateContrastRatio(foreground, background)
        return contrastRatio >= 4.5 // WCAG AA standard
    }
    
    /**
     * Calculate contrast ratio between two colors
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val luminance1 = color1.luminance() + 0.05
        val luminance2 = color2.luminance() + 0.05
        return if (luminance1 > luminance2) {
            luminance1 / luminance2
        } else {
            luminance2 / luminance1
        }
    }
    
    /**
     * Get high contrast version of color if needed
     */
    @Composable
    fun getAccessibleColor(
        color: Color,
        background: Color = MaterialTheme.colorScheme.surface
    ): Color {
        return if (hasAcceptableContrast(color, background)) {
            color
        } else {
            // Return high contrast alternative
            if (background.luminance() > 0.5) {
                Color.Black
            } else {
                Color.White
            }
        }
    }
}

/**
 * Animation colors for smooth theme transitions
 */
object ThemeAnimations {
    
    /**
     * Get transition colors for theme switching
     */
    @Composable
    fun getTransitionColors(): Pair<Color, Color> {
        return Pair(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primary
        )
    }
    
    /**
     * Get ripple color for current theme
     */
    @Composable
    fun getRippleColor(): Color {
        return MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
}

