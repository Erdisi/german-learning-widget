package com.germanleraningwidget.widget

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.core.graphics.toColorInt
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.germanleraningwidget.data.model.WidgetCustomization
import com.germanleraningwidget.data.model.WidgetTextContrast
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.repository.WidgetCustomizationRepository
import com.germanleraningwidget.util.AutoTextSizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Helper class for applying widget customizations to RemoteViews.
 * 
 * Handles automatic text sizing, background colors, and text contrast
 * based on content length and widget constraints.
 */
object WidgetCustomizationHelper {
    
    /**
     * Apply all customizations to a widget and return the customization used.
     * Uses cached customization to avoid blocking operations.
     */
    fun applyCustomizations(
        context: Context,
        views: RemoteViews,
        widgetType: WidgetType,
        containerViewId: Int
    ): WidgetCustomization {
        return try {
            // Get customization from repository - use cached version to avoid blocking
            val repository = WidgetCustomizationRepository.getInstance(context)
            
            // Try to get cached customization first, fallback to default if needed
            val customization = try {
                // Only use runBlocking as a last resort - this should be minimal and fast
                // since WidgetCustomizationRepository uses DataStore with caching
                runBlocking {
                    repository.getWidgetCustomization(widgetType).first()
                }
            } catch (e: Exception) {
                android.util.Log.w("WidgetCustomizationHelper", "Failed to get customization from cache: ${e.message}")
                WidgetCustomization.createDefault(widgetType)
            }
            
            // Apply background customization
            applyBackgroundCustomization(views, customization, containerViewId)
            
            customization
        } catch (e: Exception) {
            android.util.Log.w("WidgetCustomizationHelper", "Could not apply customizations: ${e.message}")
            // Return default customization as fallback
            WidgetCustomization.createDefault(widgetType)
        }
    }
    
    /**
     * Apply background color customization.
     */
    private fun applyBackgroundCustomization(
        views: RemoteViews,
        customization: WidgetCustomization,
        containerViewId: Int
    ) {
        try {
            // Use center color as solid background - simple and reliable
            val backgroundColor = customization.backgroundColor.argbCenterColor
            views.setInt(containerViewId, "setBackgroundColor", backgroundColor)
        } catch (e: Exception) {
            android.util.Log.w("WidgetCustomizationHelper", "Could not apply background color: ${e.message}")
            // Fallback to default color
            views.setInt(containerViewId, "setBackgroundColor", "#764ba2".toColorInt())
        }
    }
    
    /**
     * Apply automatic text customizations based on content length.
     */
    fun applyAutoTextCustomizations(
        views: RemoteViews,
        customization: WidgetCustomization,
        germanTextViewId: Int,
        translationTextViewId: Int,
        germanText: String,
        translationText: String,
        isHeroWidget: Boolean = false
    ) {
        try {
            // Calculate automatic text sizes based on content
            val textSizes = if (isHeroWidget) {
                AutoTextSizer.calculateHeroTextSizes(germanText, translationText)
            } else {
                AutoTextSizer.calculateAdvancedTextSizes(germanText, translationText, isHeroWidget)
            }
            
            // Apply calculated text sizes
            views.setTextViewTextSize(germanTextViewId, TypedValue.COMPLEX_UNIT_SP, textSizes.germanSize)
            views.setTextViewTextSize(translationTextViewId, TypedValue.COMPLEX_UNIT_SP, textSizes.translationSize)
            
            // Apply text colors based on background color pairing
            val baseTextColor = customization.backgroundColor.textColor.toArgb()
            val textColor = getTextColor(baseTextColor, customization.textContrast)
            val translationColor = getTranslationTextColor(baseTextColor, customization.textContrast)
            
            views.setTextColor(germanTextViewId, textColor)
            views.setTextColor(translationTextViewId, translationColor)
            
            android.util.Log.d("WidgetCustomizationHelper", 
                "Applied auto text sizes - German: ${textSizes.germanSize}sp, Translation: ${textSizes.translationSize}sp")
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetCustomizationHelper", "Could not apply auto text customizations: ${e.message}")
            // Fallback to default sizes
            applyFallbackTextSizes(views, customization, germanTextViewId, translationTextViewId, isHeroWidget)
        }
    }
    
    /**
     * Legacy method for compatibility - redirects to auto text sizing with default sample text.
     */
    @Deprecated("Use applyAutoTextCustomizations with actual text content instead")
    fun applyTextCustomizations(
        views: RemoteViews,
        customization: WidgetCustomization,
        germanTextViewId: Int,
        translationTextViewId: Int,
        baseGermanSize: Float = 18f,
        baseTranslationSize: Float = 14f
    ) {
        // Use default sample text for sizing when actual content is not available
        val sampleGermanText = "Guten Morgen!"
        val sampleTranslationText = "Good morning!"
        val isHeroWidget = baseGermanSize > 20f
        
        applyAutoTextCustomizations(
            views, customization, 
            germanTextViewId, translationTextViewId,
            sampleGermanText, sampleTranslationText,
            isHeroWidget
        )
        
        android.util.Log.w("WidgetCustomizationHelper", 
            "Using deprecated applyTextCustomizations - consider updating to applyAutoTextCustomizations")
    }
    
    /**
     * Apply fallback text sizes when automatic sizing fails.
     */
    private fun applyFallbackTextSizes(
        views: RemoteViews,
        customization: WidgetCustomization,
        germanTextViewId: Int,
        translationTextViewId: Int,
        isHeroWidget: Boolean
    ) {
        try {
            val germanSize = if (isHeroWidget) 22f else 18f
            val translationSize = if (isHeroWidget) 16f else 14f
            
            views.setTextViewTextSize(germanTextViewId, TypedValue.COMPLEX_UNIT_SP, germanSize)
            views.setTextViewTextSize(translationTextViewId, TypedValue.COMPLEX_UNIT_SP, translationSize)
            
            // Apply text colors
            val baseTextColor = customization.backgroundColor.textColor.toArgb()
            val textColor = getTextColor(baseTextColor, customization.textContrast)
            val translationColor = getTranslationTextColor(baseTextColor, customization.textContrast)
            
            views.setTextColor(germanTextViewId, textColor)
            views.setTextColor(translationTextViewId, translationColor)
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetCustomizationHelper", "Failed to apply fallback text sizes: ${e.message}")
        }
    }
    
    /**
     * Get text color based on base color and contrast setting.
     * Since RemoteViews don't support text shadows/strokes directly, we enhance contrast
     * by adjusting brightness and saturation for better readability.
     */
    private fun getTextColor(baseTextColor: Int, contrast: WidgetTextContrast): Int {
        return when (contrast) {
            WidgetTextContrast.NORMAL -> baseTextColor
            WidgetTextContrast.HIGH -> enhanceContrast(baseTextColor, 1.2f) // 20% brighter
            WidgetTextContrast.MAXIMUM -> enhanceContrast(baseTextColor, 1.4f) // 40% brighter
        }
    }
    
    /**
     * Get translation text color with enhanced contrast support.
     */
    private fun getTranslationTextColor(baseTextColor: Int, contrast: WidgetTextContrast): Int {
        val alpha = when (contrast) {
            WidgetTextContrast.NORMAL -> 0.8f
            WidgetTextContrast.HIGH -> 0.9f   // Higher opacity for better readability
            WidgetTextContrast.MAXIMUM -> 0.95f // Highest opacity for maximum contrast
        }
        
        // Get enhanced base color for high/maximum contrast
        val enhancedBaseColor = when (contrast) {
            WidgetTextContrast.NORMAL -> baseTextColor
            WidgetTextContrast.HIGH -> enhanceContrast(baseTextColor, 1.1f)
            WidgetTextContrast.MAXIMUM -> enhanceContrast(baseTextColor, 1.3f)
        }
        
        // Extract RGB components and apply alpha
        val red = (enhancedBaseColor shr 16) and 0xFF
        val green = (enhancedBaseColor shr 8) and 0xFF
        val blue = enhancedBaseColor and 0xFF
        val newAlpha = (255 * alpha).toInt()
        
        return (newAlpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
    
    /**
     * Enhance text contrast by increasing brightness while preserving color tone.
     * This simulates the effect of shadows/outlines for better readability.
     */
    private fun enhanceContrast(color: Int, factor: Float): Int {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val alpha = (color shr 24) and 0xFF
        
        // Calculate luminance to determine if color is light or dark
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
        
        val newRed: Int
        val newGreen: Int
        val newBlue: Int
        
        if (luminance > 0.5) {
            // Light color - make it brighter (closer to white)
            newRed = (red + (255 - red) * (factor - 1.0f)).toInt().coerceAtMost(255)
            newGreen = (green + (255 - green) * (factor - 1.0f)).toInt().coerceAtMost(255)
            newBlue = (blue + (255 - blue) * (factor - 1.0f)).toInt().coerceAtMost(255)
        } else {
            // Dark color - make it darker but more contrasted (adjust towards extremes)
            newRed = (red * factor).toInt().coerceAtMost(255)
            newGreen = (green * factor).toInt().coerceAtMost(255)
            newBlue = (blue * factor).toInt().coerceAtMost(255)
        }
        
        return (alpha shl 24) or (newRed shl 16) or (newGreen shl 8) or newBlue
    }
    
    /**
     * Get preview text sizes for customization screens.
     */
    fun getPreviewTextSizes(
        germanText: String = "Guten Morgen!",
        translationText: String = "Good morning!",
        isHeroWidget: Boolean = false
    ): AutoTextSizer.TextSizes {
        return if (isHeroWidget) {
            AutoTextSizer.calculateHeroTextSizes(germanText, translationText)
        } else {
            AutoTextSizer.calculateAdvancedTextSizes(germanText, translationText, isHeroWidget)
        }
    }
    
    /**
     * Simple widget update trigger - uses AppWidgetManager to refresh widgets.
     */
    fun triggerWidgetUpdate(context: Context, widgetType: WidgetType) {
        try {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val (componentName, widgetClass) = when (widgetType) {
                WidgetType.MAIN -> Pair(
                    android.content.ComponentName(context, GermanLearningWidget::class.java),
                    GermanLearningWidget::class.java
                )
                WidgetType.BOOKMARKS -> Pair(
                    android.content.ComponentName(context, BookmarksWidget::class.java),
                    BookmarksWidget::class.java
                )
                WidgetType.HERO -> Pair(
                    android.content.ComponentName(context, BookmarksHeroWidget::class.java),
                    BookmarksHeroWidget::class.java
                )
            }
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf() // Handle potential null return
            
            if (widgetIds.isNotEmpty()) {
                // Trigger update for all instances of this widget type
                val intent = android.content.Intent(context, widgetClass)
                intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                context.sendBroadcast(intent)
                
                android.util.Log.d("WidgetCustomizationHelper", "Triggered update for ${widgetIds.size} ${widgetType.displayName} widgets")
            } else {
                android.util.Log.d("WidgetCustomizationHelper", "No ${widgetType.displayName} widgets found to update")
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetCustomizationHelper", "Failed to trigger widget update: ${e.message}")
        }
    }
} 