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
    val textContrast: WidgetTextContrast = WidgetTextContrast.NORMAL,
    val sentencesPerDay: Int = DEFAULT_SENTENCES_PER_DAY
) {
    
    companion object {
        const val MIN_SENTENCES_PER_DAY = 1
        const val MAX_SENTENCES_PER_DAY = 10
        const val DEFAULT_SENTENCES_PER_DAY = 3
        
        /**
         * Get recommended update intervals in minutes for sentences per day.
         * Distributes updates throughout a 16-hour active day (6 AM to 10 PM).
         */
        fun getUpdateIntervalMinutes(sentencesPerDay: Int): Int {
            val activeHours = 16 // 6 AM to 10 PM
            val activeMinutes = activeHours * 60
            return (activeMinutes / sentencesPerDay.coerceIn(MIN_SENTENCES_PER_DAY, MAX_SENTENCES_PER_DAY))
                .coerceAtLeast(15) // Respect WorkManager 15-minute minimum
        }
        
        /**
         * Get daily schedule for sentence delivery.
         * Returns list of hour offsets from 6 AM for when to deliver sentences.
         */
        fun getDailySchedule(sentencesPerDay: Int): List<Int> {
            val validCount = sentencesPerDay.coerceIn(MIN_SENTENCES_PER_DAY, MAX_SENTENCES_PER_DAY)
            val baseHour = 6 // Start at 6 AM
            val activeHours = 16 // Until 10 PM
            
            return when (validCount) {
                1 -> listOf(6) // 12 PM (noon)
                2 -> listOf(2, 10) // 8 AM, 4 PM
                3 -> listOf(1, 6, 11) // 7 AM, 12 PM, 5 PM
                4 -> listOf(1, 4, 8, 12) // 7 AM, 10 AM, 2 PM, 6 PM
                5 -> listOf(0, 3, 6, 9, 13) // 6 AM, 9 AM, 12 PM, 3 PM, 7 PM
                6 -> listOf(0, 2, 5, 8, 11, 14) // 6 AM, 8 AM, 11 AM, 2 PM, 5 PM, 8 PM
                7 -> listOf(0, 2, 4, 6, 9, 12, 15) // Every 2-3 hours
                8 -> listOf(0, 2, 4, 6, 8, 10, 13, 16) // Every 2 hours
                9 -> listOf(0, 1, 3, 5, 7, 9, 11, 14, 16) // Frequent updates
                10 -> listOf(0, 1, 2, 4, 6, 8, 10, 12, 14, 16) // Maximum frequency
                else -> listOf(6) // Fallback
            }.map { it + baseHour } // Convert to actual hours (6 AM + offset)
        }
        
        /**
         * Create default customization for a widget type.
         */
        fun createDefault(widgetType: WidgetType): WidgetCustomization {
            return WidgetCustomization(
                widgetType = widgetType,
                backgroundColor = WidgetBackgroundColor.getDefaultForWidget(widgetType),
                sentencesPerDay = DEFAULT_SENTENCES_PER_DAY
            )
        }
        
        /**
         * Create high contrast customization for accessibility.
         */
        fun createHighContrast(widgetType: WidgetType): WidgetCustomization {
            return WidgetCustomization(
                widgetType = widgetType,
                backgroundColor = WidgetBackgroundColor.STONE,
                germanTextSize = WidgetTextSize.LARGE,
                translatedTextSize = WidgetTextSize.LARGE,
                textContrast = WidgetTextContrast.HIGH,
                sentencesPerDay = DEFAULT_SENTENCES_PER_DAY
            )
        }
    }
    
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
            
            // Validate sentences per day
            if (sentencesPerDay < MIN_SENTENCES_PER_DAY || sentencesPerDay > MAX_SENTENCES_PER_DAY) {
                return ValidationResult.Error("Sentences per day must be between $MIN_SENTENCES_PER_DAY and $MAX_SENTENCES_PER_DAY")
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
    fun withSafeDefaults(): WidgetCustomization {
        return copy(
            sentencesPerDay = sentencesPerDay.coerceIn(MIN_SENTENCES_PER_DAY, MAX_SENTENCES_PER_DAY)
        )
    }
    
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
        append(" • ${sentencesPerDay}/day")
    }
    
    /**
     * Get update interval in minutes for this widget.
     */
    val updateIntervalMinutes: Int get() = getUpdateIntervalMinutes(sentencesPerDay)
    
    /**
     * Get daily delivery schedule for this widget.
     */
    val dailySchedule: List<Int> get() = getDailySchedule(sentencesPerDay)


    
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
    val textColor: Color,
    val description: String
) {
    CREAM("cream", "Cream", 
        Color(0xFFFFFCF2), Color(0xFFFFFCF2), Color(0xFFFFFCF2), Color(0xFF2B2675),
        "Clean cream background"),
    LIME("lime", "Lime Green", 
        Color(0xFFD1D84E), Color(0xFFD1D84E), Color(0xFFD1D84E), Color(0xFF2B2675),
        "Bright lime green"),
    LAVENDER("lavender", "Lavender", 
        Color(0xFFE6EAFF), Color(0xFFE6EAFF), Color(0xFFE6EAFF), Color(0xFF2B2675),
        "Soft lavender blue"),
    PURPLE("purple", "Purple", 
        Color(0xFFA99BF7), Color(0xFFA99BF7), Color(0xFFA99BF7), Color(0xFFFFFFFF),
        "Rich purple"),
    ORANGE("orange", "Orange", 
        Color(0xFFE88E3D), Color(0xFFE88E3D), Color(0xFFE88E3D), Color(0xFFFFFFFF),
        "Vibrant orange"),
    NAVY("navy", "Navy Blue", 
        Color(0xFF2B2675), Color(0xFF2B2675), Color(0xFF2B2675), Color(0xFFFFFFFF),
        "Deep navy blue"),
    CORAL("coral", "Coral", 
        Color(0xFFFF7B7B), Color(0xFFFF7B7B), Color(0xFFFF7B7B), Color(0xFFFFFFFF),
        "Warm coral"),
    SAGE("sage", "Sage Green", 
        Color(0xFF87A96B), Color(0xFF87A96B), Color(0xFF87A96B), Color(0xFFFFFFFF),
        "Natural sage green"),
    STONE("stone", "Stone Gray", 
        Color(0xFF8B8680), Color(0xFF8B8680), Color(0xFF8B8680), Color(0xFFFFFFFF),
        "Neutral stone gray"),
    PEACH("peach", "Peach", 
        Color(0xFFFFB085), Color(0xFFFFB085), Color(0xFFFFB085), Color(0xFF2B2675),
        "Soft peach");
    
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
            return values().find { it.key == key } ?: CREAM
        }
        
        fun getAllColors(): List<WidgetBackgroundColor> = values().toList()
        
        fun getDefaultForWidget(widgetType: WidgetType): WidgetBackgroundColor {
            return when (widgetType) {
                WidgetType.MAIN -> CREAM
                WidgetType.BOOKMARKS -> ORANGE
                WidgetType.HERO -> NAVY
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