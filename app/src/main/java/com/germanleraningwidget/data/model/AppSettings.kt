package com.germanleraningwidget.data.model

/**
 * App-wide settings data class for the German Learning Widget app.
 * Separate from UserPreferences to handle general app configuration.
 * 
 * Features:
 * - Notification preferences
 * - Accessibility settings
 * - UI preferences
 * - Performance settings
 */
data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val learningRemindersEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val textSizeScale: Float = 1.0f, // 0.8f to 1.5f range
    val isDarkModeEnabled: Boolean? = null, // null = follow system
    val isFirstLaunch: Boolean = true,
    val lastVersionCode: Int = 0
) {
    
    /**
     * Validates app settings.
     */
    fun validate(): ValidationResult {
        if (textSizeScale < 0.5f || textSizeScale > 2.0f) {
            return ValidationResult.Error("Text size scale must be between 0.5 and 2.0")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Check if settings are valid.
     */
    fun isValid(): Boolean = validate().isSuccess
    
    /**
     * Create a copy with safe defaults.
     */
    fun withSafeDefaults(): AppSettings {
        val safeTextScale = textSizeScale.coerceIn(0.5f, 2.0f)
        
        return if (safeTextScale == textSizeScale) {
            this
        } else {
            copy(textSizeScale = safeTextScale)
        }
    }
    
    /**
     * Get text size description for UI.
     */
    val textSizeDescription: String get() = when {
        textSizeScale <= 0.8f -> "Small"
        textSizeScale <= 1.0f -> "Default"
        textSizeScale <= 1.2f -> "Large"
        else -> "Extra Large"
    }
    
    companion object {
        /**
         * Create default app settings.
         */
        fun createDefault(): AppSettings = AppSettings()
        
        /**
         * Create settings for accessibility needs.
         */
        fun createAccessible(): AppSettings = AppSettings(
            hapticFeedbackEnabled = true,
            textSizeScale = 1.2f
        )
    }
    
    /**
     * Validation result for app settings.
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val errorText: String) : ValidationResult()
        
        val isSuccess: Boolean get() = this is Success
        val errorMessage: String? get() = (this as? Error)?.errorText
    }
} 