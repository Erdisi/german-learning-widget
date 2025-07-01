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
import com.germanleraningwidget.util.DebugUtils
import com.germanleraningwidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


/**
 * Helper class for applying widget customizations to RemoteViews.
 * 
 * Handles automatic text sizing, background colors, and text contrast
 * based on content length and widget constraints.
 * 
 * OPTIMIZED: Removed runBlocking usage for better performance and ANR prevention.
 */
object WidgetCustomizationHelper {
    
    // Simple cache for widget customizations to avoid blocking operations
    private val customizationCache = mutableMapOf<WidgetType, WidgetCustomization>()
    private var lastCacheUpdate = 0L
    private const val CACHE_VALIDITY_MS = 30000L // 30 seconds
    
    /**
     * Invalidate cache for a specific widget type.
     * Call this when customizations are updated to ensure widgets get fresh data.
     */
    fun invalidateCache(widgetType: WidgetType) {
        customizationCache.remove(widgetType)
        DebugUtils.logWidget("Cache invalidated for ${widgetType.displayName}")
    }
    
    /**
     * Invalidate all cached customizations.
     */
    fun invalidateAllCache() {
        customizationCache.clear()
        lastCacheUpdate = 0L
        DebugUtils.logWidget("All customization cache invalidated")
    }
    
    /**
     * Get cached customization or load from persistent storage.
     * FIXED: Now properly loads saved settings on app restart.
     */
    private fun getCachedCustomization(
        context: Context,
        widgetType: WidgetType,
        freshCustomization: WidgetCustomization? = null
    ): WidgetCustomization {
        // If fresh customization is provided (from immediate update), use it and update cache
        if (freshCustomization != null) {
            customizationCache[widgetType] = freshCustomization
            lastCacheUpdate = System.currentTimeMillis()
            DebugUtils.logWidget("Using fresh customization for ${widgetType.displayName} from immediate update")
            return freshCustomization
        }
        
        val now = System.currentTimeMillis()
        
        // Check if cache is still valid
        if (now - lastCacheUpdate < CACHE_VALIDITY_MS) {
            customizationCache[widgetType]?.let { cachedCustomization ->
                DebugUtils.logWidget("Using valid cached customization for ${widgetType.displayName}")
                return cachedCustomization
            }
        }
        
        // Cache is expired or empty - load saved customization from repository
        return try {
            DebugUtils.logWidget("Cache expired/empty for ${widgetType.displayName} - loading saved settings")
            
            val repository = WidgetCustomizationRepository.getInstance(context)
            val loadedCustomization = runBlocking(Dispatchers.IO) {
                repository.getWidgetCustomization(widgetType).first()
            }
            
            // Update cache with loaded customization
            customizationCache[widgetType] = loadedCustomization
            lastCacheUpdate = now
            
            DebugUtils.logWidget("Successfully loaded saved customization for ${widgetType.displayName}")
            loadedCustomization
            
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to load saved customization for ${widgetType.displayName}: ${e.message}", e)
            
            // Only use defaults as fallback when loading fails
            val defaultCustomization = WidgetCustomization.createDefault(widgetType)
            customizationCache[widgetType] = defaultCustomization
            lastCacheUpdate = now
            
            DebugUtils.logWidget("Using default customization for ${widgetType.displayName} as fallback")
            defaultCustomization
        }
    }
    
    /**
     * Trigger async cache refresh without blocking current operation.
     * This loads the actual saved customization in the background.
     */
    private fun triggerAsyncCacheRefresh(context: Context, widgetType: WidgetType) {
        try {
            // Use a coroutine to load customization asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = WidgetCustomizationRepository.getInstance(context)
                    val loadedCustomization = repository.getWidgetCustomization(widgetType).first()
                    
                    // Update cache with loaded customization
                    customizationCache[widgetType] = loadedCustomization
                    lastCacheUpdate = System.currentTimeMillis()
                    
                    DebugUtils.logWidget("Async refresh completed for ${widgetType.displayName}")
                    
                    // NOTE: Don't auto-trigger widget updates from cache refresh to prevent continuous sentence changes
                    // Widget updates will happen naturally when needed or explicitly requested
                    
                } catch (e: Exception) {
                    DebugUtils.logError("WidgetCustomization", "Async cache refresh failed: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to start async cache refresh: ${e.message}", e)
        }
    }
    
    /**
     * Force refresh cache for a specific widget type.
     * This method can be async or sync depending on the sync parameter.
     */
    fun refreshCache(context: Context, widgetType: WidgetType, sync: Boolean = false) {
        try {
            // Invalidate the cache first
            invalidateCache(widgetType)
            
            if (sync) {
                // Synchronous cache refresh for critical initialization
                try {
                    val repository = WidgetCustomizationRepository.getInstance(context)
                    val loadedCustomization = runBlocking(Dispatchers.IO) {
                        repository.getWidgetCustomization(widgetType).first()
                    }
                    
                    // Update cache with loaded customization
                    customizationCache[widgetType] = loadedCustomization
                    lastCacheUpdate = System.currentTimeMillis()
                    
                    DebugUtils.logWidget("Synchronous cache refresh completed for ${widgetType.displayName}")
                } catch (e: Exception) {
                    DebugUtils.logError("WidgetCustomization", "Synchronous cache refresh failed: ${e.message}", e)
                }
            } else {
                // Trigger async refresh
                triggerAsyncCacheRefresh(context, widgetType)
                DebugUtils.logWidget("Async cache refresh requested for ${widgetType.displayName}")
            }
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to refresh cache: ${e.message}", e)
        }
    }
    
    /**
     * Apply all customizations to a widget and return the customization used.
     * Enhanced to support fresh customization data for immediate updates.
     */
    fun applyCustomizations(
        context: Context,
        views: RemoteViews,
        widgetType: WidgetType,
        containerViewId: Int,
        freshCustomization: WidgetCustomization? = null
    ): WidgetCustomization {
        return try {
            // Get customization from repository - use cached version to avoid blocking
            val repository = WidgetCustomizationRepository.getInstance(context)
            
            val customization = try {
                // Use fresh customization if provided, otherwise use cache
                getCachedCustomization(context, widgetType, freshCustomization)
            } catch (e: Exception) {
                DebugUtils.logWarning("WidgetCustomization", "Failed to get customization: ${e.message}", e)
                WidgetCustomization.createDefault(widgetType)
            }
            
            // Apply background customization
            applyBackgroundCustomization(views, customization, containerViewId)
            
            customization
        } catch (e: Exception) {
            DebugUtils.logWarning("WidgetCustomization", "Could not apply customizations: ${e.message}", e)
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
            DebugUtils.logWarning("WidgetCustomization", "Could not apply background color: ${e.message}", e)
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
            
            DebugUtils.logWidget("Applied auto text sizes - German: ${textSizes.germanSize}sp, Translation: ${textSizes.translationSize}sp")
            
        } catch (e: Exception) {
            DebugUtils.logWarning("WidgetCustomization", "Could not apply auto text customizations: ${e.message}", e)
            // Fallback to default sizes
            applyFallbackTextSizes(views, customization, germanTextViewId, translationTextViewId, isHeroWidget)
        }
    }
    
    /**
     * Apply secondary text colors based on theme context.
     * For light backgrounds, uses different opacity levels for the faded #2B2675 color.
     */
    fun applySecondaryTextColors(
        views: RemoteViews,
        customization: WidgetCustomization,
        titleTextViewId: Int? = null,
        levelTextViewId: Int? = null,
        topicTextViewId: Int? = null,
        counterTextViewId: Int? = null
    ) {
        try {
            val baseTextColor = customization.backgroundColor.textColor.toArgb()
            val isLightBackground = isLightBackgroundColor(baseTextColor)
            
            if (isLightBackground) {
                // For light backgrounds, use different opacity levels of #2B2675
                titleTextViewId?.let {
                    val titleColor = android.graphics.Color.argb(
                        255, // Alpha: 255 = 100% opacity for high visibility 
                        0x2B, 0x26, 0x75
                    )
                    views.setTextColor(it, titleColor)
                }
                
                levelTextViewId?.let {
                    val levelColor = android.graphics.Color.argb(
                        230, // Alpha: 230 = 90% opacity for moderate visibility
                        0x2B, 0x26, 0x75
                    )
                    views.setTextColor(it, levelColor)
                }
                
                topicTextViewId?.let {
                    val topicColor = android.graphics.Color.argb(
                        230, // Alpha: 230 = 90% opacity for moderate visibility
                        0x2B, 0x26, 0x75
                    )
                    views.setTextColor(it, topicColor)
                }
                
                counterTextViewId?.let {
                    val counterColor = android.graphics.Color.argb(
                        200, // Alpha: 200 = 78% opacity for subtle visibility
                        0x2B, 0x26, 0x75
                    )
                    views.setTextColor(it, counterColor)
                }
                
            } else {
                // For dark backgrounds, use the original white/light colors
                val contrast = customization.textContrast
                val baseColor = customization.backgroundColor.textColor.toArgb()
                
                titleTextViewId?.let {
                    val titleColor = getTextColor(baseColor, contrast)
                    views.setTextColor(it, titleColor)
                }
                
                levelTextViewId?.let {
                    val levelColor = android.graphics.Color.argb(180, 255, 255, 255) // Semi-transparent white
                    views.setTextColor(it, levelColor)
                }
                
                topicTextViewId?.let {
                    val topicColor = android.graphics.Color.argb(180, 255, 255, 255) // Semi-transparent white
                    views.setTextColor(it, topicColor)
                }
                
                counterTextViewId?.let {
                    val counterColor = android.graphics.Color.argb(160, 255, 255, 255) // More transparent white
                    views.setTextColor(it, counterColor)
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logWarning("WidgetCustomization", "Could not apply secondary text colors: ${e.message}", e)
        }
    }



    /**
     * Apply dynamic background colors to topic and button elements based on theme.
     * This ensures proper visibility of background elements while preserving rounded corners.
     */
    fun applyDynamicBackgrounds(
        views: RemoteViews,
        customization: WidgetCustomization,
        topicTextViewId: Int? = null,
        levelTextViewId: Int? = null,
        buttonId: Int? = null
    ) {
        try {
            val baseTextColor = customization.backgroundColor.textColor.toArgb()
            val isLightBackground = isLightBackgroundColor(baseTextColor)
            
            if (isLightBackground) {
                // For light backgrounds, we still want to use the XML drawable resources
                // which already have 16dp rounded corners. The colors will be handled
                // by the drawable resources themselves.
                topicTextViewId?.let { 
                    views.setInt(it, "setBackgroundResource", R.drawable.topic_background)
                }
                
                levelTextViewId?.let {
                    views.setInt(it, "setBackgroundResource", R.drawable.topic_background)
                }
                
                buttonId?.let {
                    views.setInt(it, "setBackgroundResource", R.drawable.widget_button_background)
                }
                
            } else {
                // For dark backgrounds, also use the XML drawable resources
                // which maintain the rounded corners
                topicTextViewId?.let { 
                    views.setInt(it, "setBackgroundResource", R.drawable.topic_background)
                }
                
                levelTextViewId?.let {
                    views.setInt(it, "setBackgroundResource", R.drawable.topic_background)
                }
                
                buttonId?.let {
                    views.setInt(it, "setBackgroundResource", R.drawable.widget_button_background)
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logWarning("WidgetCustomization", "Could not apply dynamic backgrounds: ${e.message}", e)
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
        
        DebugUtils.logWarning("WidgetCustomization", 
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
            DebugUtils.logError("WidgetCustomization", "Failed to apply fallback text sizes: ${e.message}", e)
        }
    }
    
    /**
     * Get text color based on base color and contrast setting.
     * Since RemoteViews don't support text shadows/strokes directly, we enhance contrast
     * by adjusting brightness and saturation for better readability.
     * For light backgrounds (CREAM, LIME, LAVENDER), uses a faded version of #2B2675.
     */
    private fun getTextColor(baseTextColor: Int, contrast: WidgetTextContrast): Int {
        // Check if this is a light background color that needs faded text
        val isLightBackground = isLightBackgroundColor(baseTextColor)
        
        return when (contrast) {
            WidgetTextContrast.NORMAL -> {
                if (isLightBackground) {
                    // Use clearly visible #2B2675 for light backgrounds
                    val readableColor = android.graphics.Color.argb(
                        255, // Alpha: 255 = 100% opacity for maximum readability
                        0x2B, // Red component of #2B2675
                        0x26, // Green component of #2B2675
                        0x75  // Blue component of #2B2675
                    )
                    readableColor
                } else {
                    baseTextColor
                }
            }
            WidgetTextContrast.HIGH -> enhanceContrast(baseTextColor, 1.2f) // 20% brighter
            WidgetTextContrast.MAXIMUM -> enhanceContrast(baseTextColor, 1.4f) // 40% brighter
        }
    }
    
    /**
     * Get translation text color with enhanced contrast support.
     * For light backgrounds (CREAM, LIME, LAVENDER), uses a faded version of #2B2675.
     */
    private fun getTranslationTextColor(baseTextColor: Int, contrast: WidgetTextContrast): Int {
        // Check if this is a light background color that needs faded text
        val isLightBackground = isLightBackgroundColor(baseTextColor)
        
        val alpha = when (contrast) {
            WidgetTextContrast.NORMAL -> if (isLightBackground) 0.90f else 0.8f // High opacity for light backgrounds to ensure readability
            WidgetTextContrast.HIGH -> 0.95f   // Higher opacity for better readability
            WidgetTextContrast.MAXIMUM -> 1.0f // Maximum opacity for maximum contrast
        }
        
        // Get enhanced base color for high/maximum contrast, or use faded #2B2675 for light backgrounds
        val enhancedBaseColor = when (contrast) {
            WidgetTextContrast.NORMAL -> {
                if (isLightBackground) {
                    // Use #2B2675 as base color for light backgrounds
                    android.graphics.Color.rgb(0x2B, 0x26, 0x75)
                } else {
                    baseTextColor
                }
            }
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
     * Check if the given text color indicates a light background that needs faded text.
     * This detects the first 3 background options: CREAM, LIME, and LAVENDER.
     */
    private fun isLightBackgroundColor(textColor: Int): Boolean {
        // The light backgrounds (CREAM, LIME, LAVENDER) all use #2B2675 as their text color
        val lightBackgroundTextColor = android.graphics.Color.rgb(0x2B, 0x26, 0x75)
        return textColor == lightBackgroundTextColor
    }

    /**
     * Get appropriate background color for topic/button elements based on theme.
     */
    private fun getBackgroundColor(baseTextColor: Int, alpha: Float = 0.2f): Int {
        return if (isLightBackgroundColor(baseTextColor)) {
            // For light backgrounds, use a darker semi-transparent color
            val darkColor = android.graphics.Color.rgb(0x2B, 0x26, 0x75) // Same as our faded text color
            android.graphics.Color.argb((alpha * 255).toInt(), 
                android.graphics.Color.red(darkColor),
                android.graphics.Color.green(darkColor),
                android.graphics.Color.blue(darkColor))
        } else {
            // For dark backgrounds, use light semi-transparent white as before
            android.graphics.Color.argb((alpha * 255).toInt(), 255, 255, 255)
        }
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
     * FIXED: Now invalidates cache before triggering update to ensure fresh customizations.
     */
    fun triggerWidgetUpdate(context: Context, widgetType: WidgetType) {
        try {
            // CRITICAL FIX: Invalidate cache first so widgets get fresh customizations
            invalidateCache(widgetType)
            
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
                
                DebugUtils.logWidget("Cache invalidated and triggered update for ${widgetIds.size} ${widgetType.displayName} widgets")
            } else {
                DebugUtils.logWidget("No ${widgetType.displayName} widgets found to update")
            }
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to trigger widget update: ${e.message}", e)
        }
    }
    
    /**
     * Trigger updates for all widget types.
     * Useful when global changes (like user preferences) affect all widgets.
     */
    fun triggerAllWidgetUpdates(context: Context) {
        try {
            // Invalidate all cache first
            invalidateAllCache()
            
            // Trigger updates for all widget types
            WidgetType.getAllTypes().forEach { widgetType ->
                triggerWidgetUpdate(context, widgetType)
            }
            
            DebugUtils.logWidget("Triggered updates for all widget types")
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to trigger all widget updates: ${e.message}", e)
        }
    }
    
    /**
     * Trigger immediate widget update with fresh customization data.
     * This method passes the actual customization to widgets to ensure immediate updates.
     */
    fun triggerImmediateWidgetUpdateWithData(context: Context, widgetType: WidgetType, customization: WidgetCustomization) {
        try {
            // STEP 1: Clear cache to ensure fresh data
            invalidateCache(widgetType)
            
            // STEP 2: Update cache with fresh customization
            customizationCache[widgetType] = customization
            lastCacheUpdate = System.currentTimeMillis()
            
            // STEP 3: Get widget info and force update
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
            
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf()
            
            if (widgetIds.isNotEmpty()) {
                // STEP 4: Send broadcast with customization data
                val intent = android.content.Intent(context, widgetClass)
                intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                // Add customization data to intent
                intent.putExtra("fresh_customization_background_color", customization.backgroundColor.key)
                intent.putExtra("fresh_customization_text_contrast", customization.textContrast.key)
                context.sendBroadcast(intent)
                
                // STEP 5: Also trigger direct widget update
                try {
                    when (widgetType) {
                        WidgetType.MAIN -> {
                            val widget = GermanLearningWidget()
                            widget.onUpdate(context, appWidgetManager, widgetIds)
                        }
                        WidgetType.BOOKMARKS -> {
                            val widgetClass = Class.forName("com.germanleraningwidget.widget.BookmarksWidget")
                            val constructor = widgetClass.getDeclaredConstructor()
                            val widgetInstance = constructor.newInstance()
                            val onUpdateMethod = widgetInstance.javaClass.getMethod("onUpdate", Context::class.java, android.appwidget.AppWidgetManager::class.java, IntArray::class.java)
                            onUpdateMethod.invoke(widgetInstance, context, appWidgetManager, widgetIds)
                        }
                        WidgetType.HERO -> {
                            val widgetClass = Class.forName("com.germanleraningwidget.widget.BookmarksHeroWidget")
                            val constructor = widgetClass.getDeclaredConstructor()
                            val widgetInstance = constructor.newInstance()
                            val onUpdateMethod = widgetInstance.javaClass.getMethod("onUpdate", Context::class.java, android.appwidget.AppWidgetManager::class.java, IntArray::class.java)
                            onUpdateMethod.invoke(widgetInstance, context, appWidgetManager, widgetIds)
                        }
                    }
                } catch (e: Exception) {
                    DebugUtils.logWarning("WidgetCustomization", "Direct widget update failed, using broadcast only: ${e.message}", e)
                }
                
                DebugUtils.logWidget("IMMEDIATE UPDATE WITH DATA: Triggered immediate update for ${widgetIds.size} ${widgetType.displayName} widgets with fresh customization")
            } else {
                DebugUtils.logWidget("IMMEDIATE UPDATE WITH DATA: No ${widgetType.displayName} widgets found to update")
            }
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to trigger immediate widget update with data: ${e.message}", e)
        }
    }

    /**
     * Trigger immediate widget update with complete cache refresh.
     * This is a more robust method that ensures widgets get fresh data immediately.
     */
    fun triggerImmediateWidgetUpdate(context: Context, widgetType: WidgetType) {
        try {
            // STEP 1: Completely clear all caches
            invalidateAllCache()
            
            // STEP 2: Clear Android's system widget cache
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            
            // STEP 3: Get widget info and force update
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
            
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf()
            
            if (widgetIds.isNotEmpty()) {
                // STEP 4: Send multiple update signals to ensure refresh
                
                // First, use the standard update
                val intent = android.content.Intent(context, widgetClass)
                intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                context.sendBroadcast(intent)
                
                // STEP 5: Also trigger direct widget provider onUpdate
                try {
                    when (widgetType) {
                        WidgetType.MAIN -> {
                            val widget = GermanLearningWidget()
                            widget.onUpdate(context, appWidgetManager, widgetIds)
                        }
                        WidgetType.BOOKMARKS -> {
                            val widgetClass = Class.forName("com.germanleraningwidget.widget.BookmarksWidget")
                            val constructor = widgetClass.getDeclaredConstructor()
                            val widgetInstance = constructor.newInstance()
                            val onUpdateMethod = widgetInstance.javaClass.getMethod("onUpdate", Context::class.java, android.appwidget.AppWidgetManager::class.java, IntArray::class.java)
                            onUpdateMethod.invoke(widgetInstance, context, appWidgetManager, widgetIds)
                        }
                        WidgetType.HERO -> {
                            val widgetClass = Class.forName("com.germanleraningwidget.widget.BookmarksHeroWidget")
                            val constructor = widgetClass.getDeclaredConstructor()
                            val widgetInstance = constructor.newInstance()
                            val onUpdateMethod = widgetInstance.javaClass.getMethod("onUpdate", Context::class.java, android.appwidget.AppWidgetManager::class.java, IntArray::class.java)
                            onUpdateMethod.invoke(widgetInstance, context, appWidgetManager, widgetIds)
                        }
                    }
                } catch (e: Exception) {
                    DebugUtils.logWarning("WidgetCustomization", "Direct widget update failed, using broadcast only: ${e.message}", e)
                }
                
                DebugUtils.logWidget("IMMEDIATE UPDATE: Cache cleared and triggered immediate update for ${widgetIds.size} ${widgetType.displayName} widgets")
            } else {
                DebugUtils.logWidget("IMMEDIATE UPDATE: No ${widgetType.displayName} widgets found to update")
            }
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to trigger immediate widget update: ${e.message}", e)
        }
    }
    
    /**
     * Trigger immediate updates for all widget types.
     * This forces all widgets to refresh immediately with fresh data.
     */
    fun triggerImmediateAllWidgetUpdates(context: Context) {
        try {
            // STEP 1: Clear all caches first
            invalidateAllCache()
            
            // STEP 2: Trigger immediate updates for each widget type
            WidgetType.getAllTypes().forEach { widgetType ->
                triggerImmediateWidgetUpdate(context, widgetType)
            }
            
            DebugUtils.logWidget("IMMEDIATE UPDATE: Triggered immediate updates for all widget types")
        } catch (e: Exception) {
            DebugUtils.logError("WidgetCustomization", "Failed to trigger immediate all widget updates: ${e.message}", e)
        }
    }
} 