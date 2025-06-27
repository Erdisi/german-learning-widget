package com.germanleraningwidget.data.model

import android.util.Log

/**
 * Immutable data class representing user preferences for the German learning app.
 * Contains all user-configurable settings for personalized learning experience.
 * 
 * All validation is performed during construction and through helper methods.
 */
data class UserPreferences(
    val germanLevel: GermanLevel = GermanLevel.A1,
    val nativeLanguage: String = DEFAULT_NATIVE_LANGUAGE,
    val selectedTopics: Set<String> = emptySet(),
    val deliveryFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
    val isOnboardingCompleted: Boolean = false
) {
    
    /**
     * Validates the user preferences to ensure they are valid.
     * @return ValidationResult containing success status and error message if invalid
     */
    fun validate(): ValidationResult {
        if (nativeLanguage.isBlank()) {
            return ValidationResult.Error("Native language cannot be blank")
        }
        
        if (selectedTopics.isEmpty()) {
            return ValidationResult.Error("At least one topic must be selected")
        }
        
        val invalidTopics = selectedTopics.filter { it.isBlank() }
        if (invalidTopics.isNotEmpty()) {
            return ValidationResult.Error("Topics cannot be blank")
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Checks if preferences are valid.
     */
    fun isValid(): Boolean = validate().isSuccess
    
    /**
     * Creates a copy with safe defaults if current values are invalid.
     * This method ensures the returned preferences are always valid.
     */
    fun withSafeDefaults(): UserPreferences {
        val safeLang = nativeLanguage.takeIf { it.isNotBlank() } ?: DEFAULT_NATIVE_LANGUAGE
        val safeTopics = if (selectedTopics.isEmpty() || selectedTopics.any { it.isBlank() }) {
            DEFAULT_TOPICS
        } else {
            selectedTopics.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
        
        return copy(
            nativeLanguage = safeLang.trim(),
            selectedTopics = safeTopics
        )
    }
    
    /**
     * Creates a normalized copy with trimmed strings and validated data.
     */
    fun normalized(): UserPreferences {
        return copy(
            nativeLanguage = nativeLanguage.trim(),
            selectedTopics = selectedTopics.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        )
    }
    
    companion object {
        private const val DEFAULT_NATIVE_LANGUAGE = "English"
        private val DEFAULT_TOPICS = setOf("Daily Life")
        
        /**
         * Creates UserPreferences with validation.
         * @return Valid UserPreferences or null if input is invalid and cannot be corrected
         */
        fun createSafe(
            germanLevel: GermanLevel = GermanLevel.A1,
            nativeLanguage: String = DEFAULT_NATIVE_LANGUAGE,
            selectedTopics: Set<String> = emptySet(),
            deliveryFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
            isOnboardingCompleted: Boolean = false
        ): UserPreferences {
            return UserPreferences(
                germanLevel = germanLevel,
                nativeLanguage = nativeLanguage,
                selectedTopics = selectedTopics,
                deliveryFrequency = deliveryFrequency,
                isOnboardingCompleted = isOnboardingCompleted
            ).withSafeDefaults()
        }
    }
    
    /**
     * Sealed class representing validation results.
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
        
        val isSuccess: Boolean get() = this is Success
        val errorMessage: String? get() = (this as? Error)?.message
    }
}

/**
 * Enum representing different German proficiency levels.
 * Ordered from beginner to advanced for easy comparison.
 */
enum class GermanLevel(val displayName: String, val order: Int) {
    A1("A1 - Beginner", 1),
    A2("A2 - Elementary", 2),
    B1("B1 - Intermediate", 3),
    B2("B2 - Upper Intermediate", 4),
    C1("C1 - Advanced", 5),
    C2("C2 - Mastery", 6);
    
    /**
     * Checks if this level is at least the specified minimum level.
     */
    fun isAtLeast(minimumLevel: GermanLevel): Boolean = order >= minimumLevel.order
    
    /**
     * Gets the next level, or null if already at maximum.
     */
    fun nextLevel(): GermanLevel? = values().find { it.order == order + 1 }
    
    /**
     * Gets the previous level, or null if already at minimum.
     */
    fun previousLevel(): GermanLevel? = values().find { it.order == order - 1 }
    
    companion object {
        /**
         * Safely converts a string to GermanLevel with proper error handling.
         */
        fun fromString(value: String?): GermanLevel {
            if (value.isNullOrBlank()) return A1
            
            return try {
                valueOf(value.uppercase().trim())
            } catch (e: IllegalArgumentException) {
                Log.w("GermanLevel", "Invalid level string: '$value', defaulting to A1")
                A1
            }
        }
        
        /**
         * Gets all levels as a list for UI display.
         */
        fun getAllLevels(): List<GermanLevel> = values().toList()
    }
}

/**
 * Enum representing different delivery frequencies for German sentences.
 * Contains both display information and timing data.
 */
enum class DeliveryFrequency(
    val displayName: String, 
    val hours: Long,
    val minutes: Long = 0
) {
    EVERY_30_MINUTES("Every 30 minutes", 0, 30),
    EVERY_HOUR("Every hour", 1),
    EVERY_2_HOURS("Every 2 hours", 2),
    EVERY_4_HOURS("Every 4 hours", 4),
    EVERY_6_HOURS("Every 6 hours", 6),
    EVERY_12_HOURS("Every 12 hours", 12),
    DAILY("Daily", 24);
    
    /**
     * Gets the total duration in minutes.
     */
    val totalMinutes: Long get() = hours * 60 + minutes
    
    /**
     * Gets the total duration in milliseconds.
     */
    val totalMilliseconds: Long get() = totalMinutes * 60 * 1000
    
    /**
     * Checks if this is a high-frequency delivery (less than 2 hours).
     */
    val isHighFrequency: Boolean get() = totalMinutes < 120
    
    companion object {
        /**
         * Safely converts display name to DeliveryFrequency.
         */
        fun fromDisplayName(displayName: String?): DeliveryFrequency {
            if (displayName.isNullOrBlank()) return DAILY
            
            return values().find { it.displayName.equals(displayName.trim(), ignoreCase = true) }
                ?: run {
                    Log.w("DeliveryFrequency", "Invalid display name: '$displayName', defaulting to DAILY")
                    DAILY
                }
        }
        
        /**
         * Safely converts string to DeliveryFrequency with proper error handling.
         */
        fun fromString(value: String?): DeliveryFrequency {
            if (value.isNullOrBlank()) return DAILY
            
            return try {
                valueOf(value.uppercase().trim())
            } catch (e: IllegalArgumentException) {
                Log.w("DeliveryFrequency", "Invalid frequency string: '$value', defaulting to DAILY")
                DAILY
            }
        }
        
        /**
         * Gets all frequencies as a list for UI display.
         */
        fun getAllFrequencies(): List<DeliveryFrequency> = values().toList()
        
        /**
         * Gets recommended frequencies for beginners.
         */
        fun getRecommendedForBeginners(): List<DeliveryFrequency> = listOf(
            EVERY_4_HOURS, EVERY_6_HOURS, EVERY_12_HOURS, DAILY
        )
    }
} 