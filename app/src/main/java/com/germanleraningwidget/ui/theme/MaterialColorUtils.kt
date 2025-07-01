package com.germanleraningwidget.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Material Design 3 Color Utilities for German Learning Widget
 * 
 * This file provides comprehensive utilities for:
 * - Programmatic color access
 * - ColorStateList creation
 * - Gradient generation
 * - Theme color resolution
 * - Usage examples
 */

/**
 * Utility class for accessing Material Design 3 colors programmatically
 */
object MaterialColorUtils {
    
    /**
     * Get color from theme attribute
     */
    fun getThemeColor(context: Context, @AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
    
    /**
     * Get primary color from current theme
     */
    fun getPrimaryColor(context: Context): Int {
        return getThemeColor(context, com.google.android.material.R.attr.colorPrimary)
    }
    
    /**
     * Get secondary color from current theme
     */
    fun getSecondaryColor(context: Context): Int {
        return getThemeColor(context, com.google.android.material.R.attr.colorSecondary)
    }
    
    /**
     * Get tertiary color from current theme
     */
    fun getTertiaryColor(context: Context): Int {
        return getThemeColor(context, com.google.android.material.R.attr.colorTertiary)
    }
    
    /**
     * Get surface color from current theme
     */
    fun getSurfaceColor(context: Context): Int {
        return getThemeColor(context, com.google.android.material.R.attr.colorSurface)
    }
    
    /**
     * Get error color from current theme
     */
    fun getErrorColor(context: Context): Int {
        return getThemeColor(context, com.google.android.material.R.attr.colorError)
    }
    
    /**
     * Create ColorStateList for button states
     */
    fun createButtonColorStateList(context: Context): ColorStateList {
        val primaryColor = getPrimaryColor(context)
        val disabledColor = Color(primaryColor).copy(alpha = 0.38f).toArgb()
        val pressedColor = Color(primaryColor).copy(alpha = 0.8f).toArgb()
        
        return ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf()
            ),
            intArrayOf(disabledColor, pressedColor, primaryColor)
        )
    }
    
    /**
     * Create ColorStateList for text states
     */
    fun createTextColorStateList(context: Context): ColorStateList {
        val primaryTextColor = getThemeColor(context, com.google.android.material.R.attr.colorOnSurface)
        val secondaryTextColor = getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant)
        val disabledTextColor = Color(primaryTextColor).copy(alpha = 0.38f).toArgb()
        
        return ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(disabledTextColor, primaryTextColor, secondaryTextColor)
        )
    }
    
    /**
     * Create gradient drawable with theme colors
     */
    fun createGradientDrawable(
        context: Context,
        startColorAttr: Int = com.google.android.material.R.attr.colorPrimary,
        endColorAttr: Int = com.google.android.material.R.attr.colorSecondary,
        orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT,
        cornerRadius: Float = 16f
    ): GradientDrawable {
        val startColor = getThemeColor(context, startColorAttr)
        val endColor = getThemeColor(context, endColorAttr)
        
        return GradientDrawable(orientation, intArrayOf(startColor, endColor)).apply {
            this.cornerRadius = cornerRadius
        }
    }
    
    /**
     * Get learning-specific colors
     */
    object LearningColors {
        fun getGermanTextColor(context: Context): Int = getPrimaryColor(context)
        fun getTranslationTextColor(context: Context): Int = getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant)
        fun getTopicBackgroundColor(context: Context): Int = getThemeColor(context, com.google.android.material.R.attr.colorSecondaryContainer)
        fun getLevelIndicatorColor(context: Context): Int = getTertiaryColor(context)
        fun getBookmarkActiveColor(context: Context): Int = getTertiaryColor(context)
        fun getBookmarkInactiveColor(context: Context): Int = getThemeColor(context, com.google.android.material.R.attr.colorOutline)
    }
}

/**
 * Compose utilities for Material Design 3 colors
 */
object ComposeColorUtils {
    
    /**
     * Create primary gradient brush
     */
    @Composable
    fun createPrimaryGradient(): Brush {
        return Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            )
        )
    }
    
    /**
     * Create surface gradient brush
     */
    @Composable
    fun createSurfaceGradient(): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    
    /**
     * Create learning-themed gradient
     */
    @Composable
    fun createLearningGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer
            )
        )
    }
    
    /**
     * Get color with elevation overlay for dark theme
     */
    @Composable
    fun getElevatedSurfaceColor(elevation: Int): Color {
        return if (ThemeUtils.isDarkTheme() && elevation > 0) {
            // Apply elevation overlay calculation
            val alpha = ((elevation * 0.05f).coerceAtMost(0.15f))
            MaterialTheme.colorScheme.surface.copy(
                red = (MaterialTheme.colorScheme.surface.red + alpha).coerceAtMost(1f),
                green = (MaterialTheme.colorScheme.surface.green + alpha).coerceAtMost(1f),
                blue = (MaterialTheme.colorScheme.surface.blue + alpha).coerceAtMost(1f)
            )
        } else {
            MaterialTheme.colorScheme.surface
        }
    }
}

/**
 * Usage examples for Material Design 3 colors in Compose
 */
object ColorUsageExamples {
    
    /**
     * Example: Primary action button
     */
    @Composable
    fun PrimaryActionButton(
        onClick: () -> Unit,
        text: String,
        enabled: Boolean = true
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
            Text(text)
        }
    }
    
    /**
     * Example: Secondary action button
     */
    @Composable
    fun SecondaryActionButton(
        onClick: () -> Unit,
        text: String,
        enabled: Boolean = true
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
        ) {
            Text(text)
        }
    }
    
    /**
     * Example: Learning card with gradient background
     */
    @Composable
    fun LearningCard(
        germanText: String,
        translationText: String,
        topic: String,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeColorUtils.createLearningGradient())
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = germanText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = ThemePresets.Learning.getGermanTextColor()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = translationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemePresets.Learning.getTranslationTextColor()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = ThemePresets.Learning.getTopicBackgroundColor(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = topic,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ThemePresets.Learning.getTopicTextColor()
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Example: Status indicator with color coding
     */
    @Composable
    fun StatusIndicator(
        status: LearningStatus,
        modifier: Modifier = Modifier
    ) {
        val (color, text) = when (status) {
            LearningStatus.COMPLETED -> Pair(ThemePresets.Learning.getSuccessColor(), "Completed")
            LearningStatus.IN_PROGRESS -> Pair(MaterialTheme.colorScheme.primary, "In Progress")
            LearningStatus.NOT_STARTED -> Pair(MaterialTheme.colorScheme.outline, "Not Started")
            LearningStatus.ERROR -> Pair(ThemePresets.Learning.getWarningColor(), "Error")
        }
        
        Surface(
            modifier = modifier,
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
    
    /**
     * Example: Themed progress indicator
     */
    @Composable
    fun ThemedProgressIndicator(
        progress: Float,
        modifier: Modifier = Modifier
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = ThemePresets.Learning.getProgressColor(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
    
    /**
     * Example: Widget preview with theme colors
     */
    @Composable
    fun WidgetPreview(
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeColorUtils.createPrimaryGradient())
                .border(
                    1.dp,
                    ThemePresets.Widget.overlay,
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "🎓 Learn German",
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemePresets.Widget.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Guten Morgen!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ThemePresets.Widget.textPrimary
                )
                Text(
                    text = "Good morning!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemePresets.Widget.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Greetings",
                    style = MaterialTheme.typography.labelMedium,
                    color = ThemePresets.Widget.textTertiary
                )
            }
        }
    }
}

/**
 * Learning status enum for color coding
 */
enum class LearningStatus {
    COMPLETED,
    IN_PROGRESS,
    NOT_STARTED,
    ERROR
}

/**
 * Extension functions for color manipulation
 */
fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha)

fun Color.lighten(factor: Float = 0.1f): Color {
    return Color(
        red = (red + factor).coerceAtMost(1f),
        green = (green + factor).coerceAtMost(1f),
        blue = (blue + factor).coerceAtMost(1f),
        alpha = alpha
    )
}

fun Color.darken(factor: Float = 0.1f): Color {
    return Color(
        red = (red - factor).coerceAtLeast(0f),
        green = (green - factor).coerceAtLeast(0f),
        blue = (blue - factor).coerceAtLeast(0f),
        alpha = alpha
    )
} 