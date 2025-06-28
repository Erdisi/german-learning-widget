package com.germanleraningwidget.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb


/**
 * Widget customization settings for individual widget types.
 * Each widget can have its own unique customization settings.
 */
data class WidgetCustomization(
    val widgetType: WidgetType,
    val backgroundColor: WidgetBackgroundColor = WidgetBackgroundColor.getDefaultForWidget(widgetType),
    val germanTextSize: WidgetTextSize = WidgetTextSize.MEDIUM,
    val translatedTextSize: WidgetTextSize = WidgetTextSize.MEDIUM,
    val textContrast: WidgetTextContrast = WidgetTextContrast.NORMAL
) {
    
    /**
     * Validates widget customization settings.
     */
    fun validate(): ValidationResult {
        return try {
            // Basic validation
            if (germanTextSize.scaleFactor < 0.5f || germanTextSize.scaleFactor > 2.0f) {
                return ValidationResult.Error("German text size scale factor must be between 0.5 and 2.0")
            }
            
            if (translatedTextSize.scaleFactor < 0.5f || translatedTextSize.scaleFactor > 2.0f) {
                return ValidationResult.Error("Translated text size scale factor must be between 0.5 and 2.0")
            }
            
            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error("Invalid customization: ${e.message}")
        }
    }
    
    /**
     * Check if customization is valid.
     */
    fun isValid(): Boolean = validate().isSuccess
    
    /**
     * Create a copy with safe defaults.
     */
    fun withSafeDefaults(): WidgetCustomization = this
    
    /**
     * Get display name for the customization.
     */
    val displayName: String get() = widgetType.displayName
    
    /**
     * Get customization summary for UI.
     */
    val customizationSummary: String get() = buildString {
        append("Background: ${backgroundColor.displayName}")
        append(" • German: ${germanTextSize.displayName}")
        append(" • Translation: ${translatedTextSize.displayName}")
        append(" • Contrast: ${textContrast.displayName}")
    }
    

    
    companion object {
        /**
         * Create default customization for a widget type.
         */
        fun createDefault(widgetType: WidgetType): WidgetCustomization {
            return WidgetCustomization(
                widgetType = widgetType,
                backgroundColor = WidgetBackgroundColor.getDefaultForWidget(widgetType)
            )
        }
        
        /**
         * Create high contrast customization for accessibility.
         */
        fun createHighContrast(widgetType: WidgetType): WidgetCustomization {
            return WidgetCustomization(
                widgetType = widgetType,
                backgroundColor = WidgetBackgroundColor.DARK,
                germanTextSize = WidgetTextSize.LARGE,
                translatedTextSize = WidgetTextSize.LARGE,
                textContrast = WidgetTextContrast.HIGH
            )
        }
        

    }
    
    /**
     * Validation result for widget customization.
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val errorText: String) : ValidationResult()
        
        val isSuccess: Boolean get() = this is Success
        val errorMessage: String? get() = (this as? Error)?.errorText
    }
}

/**
 * Widget types available for customization.
 */
enum class WidgetType(
    val key: String,
    val displayName: String,
    val description: String
) {
    MAIN("main_widget", "Main Learning Widget", "Shows current German sentences with translations"),
    BOOKMARKS("bookmarks_widget", "Bookmarks Widget", "Browse through your saved sentences"),
    HERO("hero_widget", "Hero Bookmarks Widget", "Material Design 3 carousel view of bookmarks");
    
    companion object {
        fun fromKey(key: String): WidgetType {
            return values().find { it.key == key } ?: MAIN
        }
        
        fun getAllTypes(): List<WidgetType> = values().toList()
    }
}

/**
 * Background color options for widgets.
 */
enum class WidgetBackgroundColor(
    val key: String,
    val displayName: String,
    val startColor: Color,
    val centerColor: Color,
    val endColor: Color,
    val description: String
) {
    DEFAULT("default", "Default Blue", 
        Color(0xFF667eea), Color(0xFF764ba2), Color(0xFFf093fb), 
        "Original gradient (Purple-Pink)"),
    BOOKMARKS("bookmarks", "Bookmarks Orange", 
        Color(0xFFFF6B35), Color(0xFFF7931E), Color(0xFFFFD23F), 
        "Orange sunset gradient"),
    HERO("hero", "Hero Blue", 
        Color(0xFF1A237E), Color(0xFF3949AB), Color(0xFF5C6BC0), 
        "Deep blue professional gradient"),
    OCEAN("ocean", "Ocean Blue", 
        Color(0xFF0575E6), Color(0xFF021B79), Color(0xFF005aa7), 
        "Deep ocean blue gradient"),
    FOREST("forest", "Forest Green", 
        Color(0xFF134E5E), Color(0xFF71B280), Color(0xFF8BC34A), 
        "Nature-inspired green gradient"),
    SUNSET("sunset", "Sunset Red", 
        Color(0xFFe65c00), Color(0xFFFF5722), Color(0xFFFF8A65), 
        "Warm sunset gradient"),
    PURPLE("purple", "Royal Purple", 
        Color(0xFF667eea), Color(0xFF764ba2), Color(0xFF9575CD), 
        "Royal purple gradient"),
    TEAL("teal", "Modern Teal", 
        Color(0xFF11998e), Color(0xFF38ef7d), Color(0xFF4CAF50), 
        "Fresh teal gradient"),
    DARK("dark", "Dark Professional", 
        Color(0xFF2C3E50), Color(0xFF4A6741), Color(0xFF34495E), 
        "Professional dark gradient"),
    LIGHT("light", "Light Minimal", 
        Color(0xFFF7F7F7), Color(0xFFE8E8E8), Color(0xFFDDDDDD), 
        "Clean minimal light gradient");
    
    /**
     * Get primary color (start color) as ARGB integer for widget usage.
     */
    val argbStartColor: Int get() = startColor.toArgb()
    val argbCenterColor: Int get() = centerColor.toArgb()
    val argbEndColor: Int get() = endColor.toArgb()
    
    /**
     * Get primary color for UI preview.
     */
    val primaryColor: Color get() = centerColor
    
    companion object {
        fun fromKey(key: String): WidgetBackgroundColor {
            return values().find { it.key == key } ?: DEFAULT
        }
        
        fun getAllColors(): List<WidgetBackgroundColor> = values().toList()
        
        fun getDefaultForWidget(widgetType: WidgetType): WidgetBackgroundColor {
            return when (widgetType) {
                WidgetType.MAIN -> DEFAULT
                WidgetType.BOOKMARKS -> BOOKMARKS
                WidgetType.HERO -> HERO
            }
        }
    }
}

/**
 * Text size options for widget text.
 */
enum class WidgetTextSize(
    val key: String,
    val displayName: String,
    val scaleFactor: Float,
    val description: String
) {
    SMALL("small", "Small", 0.8f, "Compact text for more content"),
    MEDIUM("medium", "Medium", 1.0f, "Standard readable text size"),
    LARGE("large", "Large", 1.2f, "Larger text for better readability"),
    EXTRA_LARGE("extra_large", "Extra Large", 1.4f, "Maximum text size for accessibility");
    
    companion object {
        fun fromKey(key: String): WidgetTextSize {
            return values().find { it.key == key } ?: MEDIUM
        }
        
        fun getAllSizes(): List<WidgetTextSize> = values().toList()
    }
}

/**
 * Text contrast options for widget text.
 */
enum class WidgetTextContrast(
    val key: String,
    val displayName: String,
    val description: String,
    val shadowEnabled: Boolean,
    val strokeEnabled: Boolean
) {
    NORMAL("normal", "Normal", "Standard text contrast", false, false),
    HIGH("high", "High", "Enhanced contrast with shadow", true, false),
    MAXIMUM("maximum", "Maximum", "Maximum contrast with outline", true, true);
    
    companion object {
        fun fromKey(key: String): WidgetTextContrast {
            return values().find { it.key == key } ?: NORMAL
        }
        
        fun getAllContrasts(): List<WidgetTextContrast> = values().toList()
    }
}



/**
 * Complete widget customization container for all widgets.
 */
data class AllWidgetCustomizations(
    val mainWidget: WidgetCustomization = WidgetCustomization.createDefault(WidgetType.MAIN),
    val bookmarksWidget: WidgetCustomization = WidgetCustomization.createDefault(WidgetType.BOOKMARKS),
    val heroWidget: WidgetCustomization = WidgetCustomization.createDefault(WidgetType.HERO)
) {
    
    /**
     * Get customization for specific widget type.
     */
    fun getCustomization(widgetType: WidgetType): WidgetCustomization {
        return when (widgetType) {
            WidgetType.MAIN -> mainWidget
            WidgetType.BOOKMARKS -> bookmarksWidget
            WidgetType.HERO -> heroWidget
        }
    }
    
    /**
     * Update customization for specific widget type.
     */
    fun updateCustomization(customization: WidgetCustomization): AllWidgetCustomizations {
        return when (customization.widgetType) {
            WidgetType.MAIN -> copy(mainWidget = customization)
            WidgetType.BOOKMARKS -> copy(bookmarksWidget = customization)
            WidgetType.HERO -> copy(heroWidget = customization)
        }
    }
    
    /**
     * Validate all customizations.
     */
    fun validate(): ValidationResult {
        val results = listOf(
            mainWidget.validate(),
            bookmarksWidget.validate(),
            heroWidget.validate()
        )
        
        val errors = results.filterIsInstance<WidgetCustomization.ValidationResult.Error>()
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error("Invalid customizations: ${errors.joinToString(", ") { it.errorText }}")
        }
    }
    
    /**
     * Check if all customizations are valid.
     */
    fun isValid(): Boolean = validate().isSuccess
    
    /**
     * Get all widget types.
     */
    val allWidgetTypes: List<WidgetType> get() = WidgetType.getAllTypes()
    
    /**
     * Get customization summary for all widgets.
     */
    val allCustomizationsSummary: String get() = buildString {
        append("Main: ${mainWidget.backgroundColor.displayName}")
        append(" • Bookmarks: ${bookmarksWidget.backgroundColor.displayName}")
        append(" • Hero: ${heroWidget.backgroundColor.displayName}")
    }
    
    companion object {
        /**
         * Create default customizations for all widgets.
         */
        fun createDefault(): AllWidgetCustomizations = AllWidgetCustomizations()
        
        /**
         * Create high contrast customizations for all widgets.
         */
        fun createHighContrast(): AllWidgetCustomizations = AllWidgetCustomizations(
            mainWidget = WidgetCustomization.createHighContrast(WidgetType.MAIN),
            bookmarksWidget = WidgetCustomization.createHighContrast(WidgetType.BOOKMARKS),
            heroWidget = WidgetCustomization.createHighContrast(WidgetType.HERO)
        )
    }
    
    /**
     * Validation result for all widget customizations.
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val errorText: String) : ValidationResult()
        
        val isSuccess: Boolean get() = this is Success
        val errorMessage: String? get() = (this as? Error)?.errorText
    }
}