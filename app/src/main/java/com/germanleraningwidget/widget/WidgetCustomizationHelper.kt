package com.germanleraningwidget.widget

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.widget.RemoteViews
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
            
            // Apply text contrast (colors)
            val textColor = getTextColor(customization.textContrast)
            val translationColor = getTranslationTextColor(customization.textContrast)
            
            views.setTextColor(germanTextViewId, textColor)
            views.setTextColor(translationTextViewId, translationColor)
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetCustomizationHelper", "Could not apply text customizations: ${e.message}")
        }
    }
    
    /**
     * Get text color based on contrast setting.
     */
    private fun getTextColor(contrast: WidgetTextContrast): Int {
        return when (contrast) {
            WidgetTextContrast.NORMAL -> Color.WHITE
            WidgetTextContrast.HIGH -> Color.WHITE // Could add shadow in future
            WidgetTextContrast.MAXIMUM -> Color.WHITE // Could add outline in future
        }
    }
    
    /**
     * Get translation text color (slightly transparent).
     */
    private fun getTranslationTextColor(contrast: WidgetTextContrast): Int {
        return when (contrast) {
            WidgetTextContrast.NORMAL -> 0xE6FFFFFF.toInt() // 90% opacity
            WidgetTextContrast.HIGH -> 0xF0FFFFFF.toInt() // 94% opacity
            WidgetTextContrast.MAXIMUM -> Color.WHITE // 100% opacity
        }
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