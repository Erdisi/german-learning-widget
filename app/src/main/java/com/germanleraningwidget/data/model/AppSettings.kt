package com.germanleraningwidget.data.model

/**
 * App-wide settings data class for the German Learning Widget app.
 * Separate from UserPreferences to handle general app configuration.
 * 
 * Features:
 * - Notification preferences
 * - UI preferences
 * - Performance settings
 */
data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val learningRemindersEnabled: Boolean = true,
    val isDarkModeEnabled: Boolean? = null, // null = follow system
    val isFirstLaunch: Boolean = true,
    val lastVersionCode: Int = 0
) {
    
    /**
     * Validates app settings.
     */
    fun validate(): ValidationResult {
        // All current settings are boolean or nullable boolean, no validation needed
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
        // All current properties have safe defaults, no modifications needed
        return this
    }
    
    companion object {
        /**
         * Create default app settings.
         */
        fun createDefault(): AppSettings = AppSettings()
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