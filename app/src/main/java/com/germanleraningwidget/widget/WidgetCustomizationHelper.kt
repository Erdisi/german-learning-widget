package com.germanleraningwidget.widget

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.germanleraningwidget.data.model.WidgetCustomization
import com.germanleraningwidget.data.model.WidgetTextContrast
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.repository.WidgetCustomizationRepository
import kotlinx.coroutines.flow.first

/**
 * Centralized widget customization helper.
 * Provides simple, consistent methods for applying user customizations to widgets.
 */
object WidgetCustomizationHelper {
    
    /**
     * Apply all customizations to a widget RemoteViews.
     * This is the single entry point for widget customization.
     */
    suspend fun applyCustomizations(
        context: Context,
        views: RemoteViews,
        widgetType: WidgetType,
        containerViewId: Int
    ): WidgetCustomization {
        val customizationRepository = WidgetCustomizationRepository.getInstance(context)
        val customization = customizationRepository.getWidgetCustomization(widgetType).first()
        
        // Apply background color (solid color from center color)
        applyBackgroundColor(views, customization, containerViewId)
        
        return customization
    }
    
    /**
     * Apply background color using simple solid color approach.
     * Uses the center color from the gradient as a solid background.
     */
    private fun applyBackgroundColor(
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
            views.setInt(containerViewId, "setBackgroundColor", Color.parseColor("#764ba2"))
        }
    }
    
    /**
     * Apply text customizations to specific text views.
     */
    fun applyTextCustomizations(
        views: RemoteViews,
        customization: WidgetCustomization,
        germanTextViewId: Int,
        translationTextViewId: Int,
        baseGermanSize: Float = 18f,
        baseTranslationSize: Float = 14f
    ) {
        try {
            // Apply text sizes
            val germanTextSize = baseGermanSize * customization.germanTextSize.scaleFactor
            val translationTextSize = baseTranslationSize * customization.translatedTextSize.scaleFactor
            
            views.setTextViewTextSize(germanTextViewId, TypedValue.COMPLEX_UNIT_SP, germanTextSize)
            views.setTextViewTextSize(translationTextViewId, TypedValue.COMPLEX_UNIT_SP, translationTextSize)
            
            // Apply text colors based on background color pairing
            val baseTextColor = customization.backgroundColor.textColor.toArgb()
            val textColor = getTextColor(baseTextColor, customization.textContrast)
            val translationColor = getTranslationTextColor(baseTextColor, customization.textContrast)
            
            views.setTextColor(germanTextViewId, textColor)
            views.setTextColor(translationTextViewId, translationColor)
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetCustomizationHelper", "Could not apply text customizations: ${e.message}")
        }
    }
    
    /**
     * Get text color based on base color and contrast setting.
     */
    private fun getTextColor(baseTextColor: Int, contrast: WidgetTextContrast): Int {
        return when (contrast) {
            WidgetTextContrast.NORMAL -> baseTextColor
            WidgetTextContrast.HIGH -> baseTextColor // Could add shadow in future
            WidgetTextContrast.MAXIMUM -> baseTextColor // Could add outline in future
        }
    }
    
    /**
     * Get translation text color (slightly transparent version of base color).
     */
    private fun getTranslationTextColor(baseTextColor: Int, contrast: WidgetTextContrast): Int {
        return when (contrast) {
            WidgetTextContrast.NORMAL -> applyAlpha(baseTextColor, 0.9f) // 90% opacity
            WidgetTextContrast.HIGH -> applyAlpha(baseTextColor, 0.94f) // 94% opacity
            WidgetTextContrast.MAXIMUM -> baseTextColor // 100% opacity
        }
    }
    
    /**
     * Apply alpha to a color.
     */
    private fun applyAlpha(color: Int, alpha: Float): Int {
        val alphaValue = (255 * alpha).toInt()
        return (color and 0x00FFFFFF) or (alphaValue shl 24)
    }
    
    /**
     * Simple widget update trigger - just sends broadcast to refresh widgets.
     */
    fun triggerWidgetUpdate(context: Context, widgetType: WidgetType) {
        try {
            when (widgetType) {
                WidgetType.MAIN -> GermanLearningWidget.updateAllWidgets(context)
                WidgetType.BOOKMARKS -> BookmarksWidget.updateAllWidgets(context)
                WidgetType.HERO -> BookmarksHeroWidget.updateAllWidgets(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetCustomizationHelper", "Failed to trigger widget update: ${e.message}")
        }
    }
} 